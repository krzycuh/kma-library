package pl.kmazurek.library.infrastructure.persistence

import org.springframework.stereotype.Component
import pl.kmazurek.library.domain.model.shelf.Library
import pl.kmazurek.library.domain.model.shelf.LibraryId
import pl.kmazurek.library.domain.model.shelf.LibraryMember
import pl.kmazurek.library.domain.model.shelf.LibraryRole
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.LibraryRepository
import pl.kmazurek.library.infrastructure.persistence.entity.LibraryMemberKey
import pl.kmazurek.library.infrastructure.persistence.mapper.LibraryMapper

@Component
class LibraryRepositoryImpl(
    private val jpaRepository: JpaLibraryRepository,
    private val memberRepository: JpaLibraryMemberRepository,
) : LibraryRepository {
    override fun findById(id: LibraryId): Library? = jpaRepository.findById(id.value).map { LibraryMapper.toDomain(it) }.orElse(null)

    override fun findByMember(userId: UserId): List<Library> = jpaRepository.findByMember(userId.value).map { LibraryMapper.toDomain(it) }

    override fun save(library: Library): Library = LibraryMapper.toDomain(jpaRepository.save(LibraryMapper.toJpa(library)))

    override fun findMemberRole(
        libraryId: LibraryId,
        userId: UserId,
    ): LibraryRole? =
        memberRepository.findById(LibraryMemberKey(libraryId.value, userId.value))
            .map { LibraryRole.valueOf(it.role) }
            .orElse(null)

    override fun addMember(member: LibraryMember) {
        memberRepository.save(LibraryMapper.memberToJpa(member))
    }
}
