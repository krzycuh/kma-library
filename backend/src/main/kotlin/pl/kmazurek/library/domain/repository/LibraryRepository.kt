package pl.kmazurek.library.domain.repository

import pl.kmazurek.library.domain.model.shelf.Library
import pl.kmazurek.library.domain.model.shelf.LibraryId
import pl.kmazurek.library.domain.model.shelf.LibraryMember
import pl.kmazurek.library.domain.model.shelf.LibraryRole
import pl.kmazurek.library.domain.model.user.UserId

/**
 * Repository interface (port) for the Library aggregate and its membership
 */
interface LibraryRepository {
    fun findById(id: LibraryId): Library?

    fun findByMember(userId: UserId): List<Library>

    fun save(library: Library): Library

    fun findMemberRole(
        libraryId: LibraryId,
        userId: UserId,
    ): LibraryRole?

    fun addMember(member: LibraryMember)

    fun findMembers(libraryId: LibraryId): List<LibraryMember>

    fun removeMember(
        libraryId: LibraryId,
        userId: UserId,
    )
}
