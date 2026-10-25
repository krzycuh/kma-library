package pl.kmazurek.library.application.usecase.shelf

import org.springframework.stereotype.Service
import pl.kmazurek.library.application.dto.LibraryDto
import pl.kmazurek.library.domain.model.shelf.Library
import pl.kmazurek.library.domain.model.shelf.LibraryId
import pl.kmazurek.library.domain.model.shelf.LibraryRole
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.LibraryRepository
import pl.kmazurek.library.domain.service.LibraryAccessPolicy

/**
 * Use Case: libraries the current user is a member of
 */
@Service
class ListMyLibraries(
    private val libraryRepository: LibraryRepository,
) {
    fun execute(userId: UserId): List<Library> = libraryRepository.findByMember(userId).sortedBy { it.createdAt }

    /**
     * Same libraries with the user's role and the member count; the user's own library first.
     */
    fun summaries(userId: UserId): List<LibraryDto> =
        execute(userId)
            .map { library ->
                val members = libraryRepository.findMembers(library.id)
                val myRole = members.firstOrNull { it.userId == userId }?.role ?: LibraryRole.MEMBER
                LibraryDto.fromDomain(library, myRole, members.size)
            }
            .sortedBy { if (it.myRole == LibraryRole.OWNER) 0 else 1 }
}

/**
 * Shared guard: loads a library the user may access, or fails with 404 / 403
 */
@Service
class LibraryAccessGuard(
    private val libraryRepository: LibraryRepository,
    private val accessPolicy: LibraryAccessPolicy,
) {
    fun requireAccess(
        userId: UserId,
        libraryId: LibraryId,
    ): Library {
        val library = libraryRepository.findById(libraryId) ?: throw LibraryNotFoundException("Library not found")
        if (!accessPolicy.canAccess(userId, libraryId)) {
            throw LibraryAccessDeniedException("You are not a member of this library")
        }
        return library
    }
}
