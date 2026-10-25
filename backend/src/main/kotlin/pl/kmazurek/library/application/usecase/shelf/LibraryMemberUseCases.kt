package pl.kmazurek.library.application.usecase.shelf

import org.springframework.stereotype.Service
import pl.kmazurek.library.application.dto.LibraryDto
import pl.kmazurek.library.application.dto.LibraryMemberDto
import pl.kmazurek.library.domain.model.shelf.Library
import pl.kmazurek.library.domain.model.shelf.LibraryId
import pl.kmazurek.library.domain.model.shelf.LibraryMember
import pl.kmazurek.library.domain.model.shelf.LibraryRole
import pl.kmazurek.library.domain.model.user.Email
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.LibraryRepository
import pl.kmazurek.library.domain.repository.UserRepository
import pl.kmazurek.library.domain.service.LibraryAccessPolicy

/**
 * Use Case: members of a library (visible to every member), owner first
 */
@Service
class ListLibraryMembers(
    private val libraryRepository: LibraryRepository,
    private val userRepository: UserRepository,
    private val libraryAccessGuard: LibraryAccessGuard,
) {
    fun execute(
        userId: UserId,
        libraryId: LibraryId,
    ): List<LibraryMemberDto> {
        libraryAccessGuard.requireAccess(userId, libraryId)
        return libraryRepository.findMembers(libraryId)
            .mapNotNull { member ->
                userRepository.findById(member.userId)?.let { user ->
                    LibraryMemberDto(user.id.toString(), user.name, user.email.value, member.role)
                }
            }
            .sortedWith(compareBy({ if (it.role == LibraryRole.OWNER) 0 else 1 }, { it.name.lowercase() }))
    }
}

/**
 * Use Case (owner): add an existing, active user to the library by e-mail
 */
@Service
class InviteLibraryMember(
    private val libraryRepository: LibraryRepository,
    private val userRepository: UserRepository,
    private val libraryAccessGuard: LibraryAccessGuard,
    private val accessPolicy: LibraryAccessPolicy,
) {
    fun execute(
        ownerId: UserId,
        libraryId: LibraryId,
        email: String,
    ): LibraryMemberDto {
        requireOwner(libraryAccessGuard, accessPolicy, ownerId, libraryId)
        val user =
            userRepository.findByEmail(Email(email.trim()))?.takeIf { it.isActive }
                ?: throw LibraryMemberNotFoundException("No active account with this e-mail")
        if (libraryRepository.findMemberRole(libraryId, user.id) != null) {
            throw AlreadyLibraryMemberException("${user.name} is already a member of this library")
        }
        libraryRepository.addMember(LibraryMember(libraryId, user.id, LibraryRole.MEMBER))
        return LibraryMemberDto(user.id.toString(), user.name, user.email.value, LibraryRole.MEMBER)
    }
}

/**
 * Use Case: the owner removes a member, or a member leaves. Their shelf items stay in the library.
 */
@Service
class RemoveLibraryMember(
    private val libraryRepository: LibraryRepository,
    private val libraryAccessGuard: LibraryAccessGuard,
    private val accessPolicy: LibraryAccessPolicy,
) {
    fun execute(
        requesterId: UserId,
        libraryId: LibraryId,
        memberId: UserId,
    ) {
        libraryAccessGuard.requireAccess(requesterId, libraryId)
        val memberRole =
            libraryRepository.findMemberRole(libraryId, memberId)
                ?: throw LibraryMemberNotFoundException("This user is not a member of the library")
        if (!accessPolicy.canRemoveMember(requesterId, libraryId, memberId)) {
            throw CannotRemoveLibraryMemberException(
                if (memberRole == LibraryRole.OWNER) "The owner cannot leave or be removed" else "Only the owner can remove other members",
            )
        }
        libraryRepository.removeMember(libraryId, memberId)
    }
}

/**
 * Use Case (owner): rename the library, e.g. "Biblioteczka rodzinna"
 */
@Service
class RenameLibrary(
    private val libraryRepository: LibraryRepository,
    private val libraryAccessGuard: LibraryAccessGuard,
    private val accessPolicy: LibraryAccessPolicy,
) {
    fun execute(
        ownerId: UserId,
        libraryId: LibraryId,
        name: String,
    ): LibraryDto {
        val library = requireOwner(libraryAccessGuard, accessPolicy, ownerId, libraryId)
        val saved = libraryRepository.save(library.rename(name))
        return LibraryDto.fromDomain(saved, LibraryRole.OWNER, libraryRepository.findMembers(libraryId).size)
    }
}

private fun requireOwner(
    guard: LibraryAccessGuard,
    accessPolicy: LibraryAccessPolicy,
    userId: UserId,
    libraryId: LibraryId,
): Library {
    val library = guard.requireAccess(userId, libraryId)
    if (!accessPolicy.canManageMembers(userId, libraryId)) {
        throw LibraryAccessDeniedException("Only the owner can manage this library")
    }
    return library
}
