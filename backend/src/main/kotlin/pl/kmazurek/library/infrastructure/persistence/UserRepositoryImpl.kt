package pl.kmazurek.library.infrastructure.persistence

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import pl.kmazurek.library.domain.model.user.Email
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.UserRepository
import pl.kmazurek.library.infrastructure.persistence.mapper.UserMapper

/**
 * Implementation of UserRepository (domain port) backed by JPA
 */
@Component
class UserRepositoryImpl(
    private val jpaRepository: JpaUserRepository,
) : UserRepository {
    override fun findById(id: UserId): User? {
        return jpaRepository.findById(id.value)
            .map { UserMapper.toDomain(it) }
            .orElse(null)
    }

    override fun findByEmail(email: Email): User? {
        return jpaRepository.findByEmail(email.value)
            ?.let { UserMapper.toDomain(it) }
    }

    override fun existsByEmail(email: Email): Boolean {
        return jpaRepository.existsByEmail(email.value)
    }

    override fun save(user: User): User {
        val saved = jpaRepository.save(UserMapper.toJpa(user))
        return UserMapper.toDomain(saved)
    }

    override fun deleteById(id: UserId) {
        jpaRepository.deleteById(id.value)
    }

    override fun findAll(
        searchTerm: String?,
        page: Int,
        pageSize: Int,
    ): Pair<List<User>, Long> {
        val searchPattern =
            searchTerm?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()?.let { "%$it%" }
        val pageable = PageRequest.of(page.coerceAtLeast(0), pageSize.coerceIn(1, 100))
        val result = jpaRepository.search(searchPattern, pageable)
        return result.content.map { UserMapper.toDomain(it) } to result.totalElements
    }
}
