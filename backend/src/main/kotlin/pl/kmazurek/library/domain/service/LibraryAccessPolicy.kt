package pl.kmazurek.library.domain.service

import pl.kmazurek.library.domain.model.shelf.LibraryId
import pl.kmazurek.library.domain.model.shelf.LibraryRole
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.LibraryRepository

/**
 * Domain service: the single place deciding who may read/modify a library's shelf.
 * Any member may use the shelf (add, edit, lend, return); only the owner manages members and the library itself.
 */
class LibraryAccessPolicy(
    private val libraryRepository: LibraryRepository,
) {
    fun canAccess(
        userId: UserId,
        libraryId: LibraryId,
    ): Boolean = libraryRepository.findMemberRole(libraryId, userId) != null

    fun canManageMembers(
        userId: UserId,
        libraryId: LibraryId,
    ): Boolean = libraryRepository.findMemberRole(libraryId, userId) == LibraryRole.OWNER

    /**
     * The owner may remove anyone but themselves; a member may only leave (remove themselves).
     * The owner can never be removed: the library would be left without anyone managing it.
     */
    fun canRemoveMember(
        requesterId: UserId,
        libraryId: LibraryId,
        memberId: UserId,
    ): Boolean {
        val memberRole = libraryRepository.findMemberRole(libraryId, memberId) ?: return false
        if (memberRole == LibraryRole.OWNER) return false
        return requesterId == memberId || canManageMembers(requesterId, libraryId)
    }
}
