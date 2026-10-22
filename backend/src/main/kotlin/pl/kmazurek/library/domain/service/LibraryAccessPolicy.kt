package pl.kmazurek.library.domain.service

import pl.kmazurek.library.domain.model.shelf.LibraryId
import pl.kmazurek.library.domain.model.shelf.LibraryRole
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.LibraryRepository

/**
 * Domain service: the single place deciding who may read/modify a library's shelf.
 * MVP: any member (owner). Family libraries only add members; this class stays the same.
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
}
