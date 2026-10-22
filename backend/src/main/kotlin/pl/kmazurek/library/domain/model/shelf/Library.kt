package pl.kmazurek.library.domain.model.shelf

import pl.kmazurek.library.domain.model.user.UserId
import java.time.LocalDateTime

/**
 * Library Aggregate Root (Shelf context)
 * A bookshelf that has an owner and members. In MVP every user owns exactly one library;
 * family libraries only add members.
 */
data class Library(
    val id: LibraryId,
    val name: String,
    val ownerId: UserId,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    init {
        require(name.isNotBlank()) { "Library name cannot be blank" }
        require(name.length <= 255) { "Library name cannot exceed 255 characters" }
    }

    fun rename(name: String): Library = copy(name = name.trim(), updatedAt = LocalDateTime.now())

    companion object {
        fun create(
            name: String,
            ownerId: UserId,
        ): Library {
            val now = LocalDateTime.now()
            return Library(
                id = LibraryId.generate(),
                name = name.trim(),
                ownerId = ownerId,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}

enum class LibraryRole {
    OWNER,
    MEMBER,
}

data class LibraryMember(
    val libraryId: LibraryId,
    val userId: UserId,
    val role: LibraryRole,
)
