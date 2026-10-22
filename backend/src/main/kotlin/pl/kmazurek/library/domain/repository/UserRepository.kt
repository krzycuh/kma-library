package pl.kmazurek.library.domain.repository

import pl.kmazurek.library.domain.model.user.Email
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.model.user.UserId

/**
 * Repository interface (port) for User aggregate
 */
interface UserRepository {
    fun findById(id: UserId): User?

    fun findByEmail(email: Email): User?

    fun existsByEmail(email: Email): Boolean

    fun save(user: User): User

    fun deleteById(id: UserId)

    /**
     * Paged listing for admin screens. Returns (users, totalCount).
     */
    fun findAll(
        searchTerm: String?,
        page: Int,
        pageSize: Int,
    ): Pair<List<User>, Long>
}
