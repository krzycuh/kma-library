package pl.kmazurek.library.application.usecase.shelf

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.library.domain.model.shelf.Library
import pl.kmazurek.library.domain.model.shelf.LibraryMember
import pl.kmazurek.library.domain.model.shelf.LibraryRole
import pl.kmazurek.library.domain.model.user.Email
import pl.kmazurek.library.domain.model.user.HashedPassword
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.model.user.UserId
import pl.kmazurek.library.domain.repository.LibraryRepository
import pl.kmazurek.library.domain.repository.UserRepository
import pl.kmazurek.library.domain.service.LibraryAccessPolicy

class LibraryMemberUseCasesTest {
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var userRepository: UserRepository
    private lateinit var guard: LibraryAccessGuard
    private lateinit var policy: LibraryAccessPolicy

    private val owner = User.create(Email("anna@example.com"), HashedPassword("h"), "Anna")
    private val member = User.create(Email("jan@example.com"), HashedPassword("h"), "Jan")
    private val outsider = User.create(Email("ola@example.com"), HashedPassword("h"), "Ola")
    private val blocked = User.create(Email("blocked@example.com"), HashedPassword("h"), "Zablokowany").deactivate()
    private val library = Library.create("Biblioteczka Anny", owner.id)

    @BeforeEach
    fun setup() {
        libraryRepository = mockk(relaxUnitFun = true)
        userRepository = mockk()
        policy = LibraryAccessPolicy(libraryRepository)
        guard = LibraryAccessGuard(libraryRepository, policy)
        every { libraryRepository.findById(library.id) } returns library
        every { libraryRepository.findMemberRole(library.id, any()) } returns null
        every { libraryRepository.findMemberRole(library.id, owner.id) } returns LibraryRole.OWNER
        every { libraryRepository.findMemberRole(library.id, member.id) } returns LibraryRole.MEMBER
        every { libraryRepository.findMembers(library.id) } returns
            listOf(LibraryMember(library.id, member.id, LibraryRole.MEMBER), LibraryMember(library.id, owner.id, LibraryRole.OWNER))
        listOf(owner, member, outsider, blocked).forEach { user ->
            every { userRepository.findById(user.id) } returns user
            every { userRepository.findByEmail(user.email) } returns user
        }
        every { userRepository.findByEmail(Email("nobody@example.com")) } returns null
    }

    private fun invite() = InviteLibraryMember(libraryRepository, userRepository, guard, policy)

    private fun remove() = RemoveLibraryMember(libraryRepository, guard, policy)

    @Test
    fun `owner invites an existing user by e-mail as a member`() {
        val dto = invite().execute(owner.id, library.id, "  ola@example.com ")

        assertEquals(outsider.id.toString(), dto.userId)
        assertEquals(LibraryRole.MEMBER, dto.role)
        verify { libraryRepository.addMember(LibraryMember(library.id, outsider.id, LibraryRole.MEMBER)) }
    }

    @Test
    fun `invite fails for members, unknown or blocked accounts and existing members`() {
        assertThrows<LibraryAccessDeniedException> { invite().execute(member.id, library.id, "ola@example.com") }
        assertThrows<LibraryAccessDeniedException> { invite().execute(outsider.id, library.id, "ola@example.com") }
        assertThrows<LibraryMemberNotFoundException> { invite().execute(owner.id, library.id, "nobody@example.com") }
        assertThrows<LibraryMemberNotFoundException> { invite().execute(owner.id, library.id, "blocked@example.com") }
        assertThrows<AlreadyLibraryMemberException> { invite().execute(owner.id, library.id, "jan@example.com") }
        verify(exactly = 0) { libraryRepository.addMember(any()) }
    }

    @Test
    fun `owner removes a member and a member can leave`() {
        remove().execute(owner.id, library.id, member.id)
        remove().execute(member.id, library.id, member.id)

        verify(exactly = 2) { libraryRepository.removeMember(library.id, member.id) }
    }

    @Test
    fun `the owner cannot be removed and members cannot remove others`() {
        val second = UserId.generate()
        every { libraryRepository.findMemberRole(library.id, second) } returns LibraryRole.MEMBER

        assertThrows<CannotRemoveLibraryMemberException> { remove().execute(owner.id, library.id, owner.id) }
        assertThrows<CannotRemoveLibraryMemberException> { remove().execute(member.id, library.id, owner.id) }
        assertThrows<CannotRemoveLibraryMemberException> { remove().execute(member.id, library.id, second) }
        assertThrows<LibraryAccessDeniedException> { remove().execute(outsider.id, library.id, member.id) }
        assertThrows<LibraryMemberNotFoundException> { remove().execute(owner.id, library.id, outsider.id) }
        verify(exactly = 0) { libraryRepository.removeMember(any(), any()) }
    }

    @Test
    fun `members are listed for any member, owner first, but not for outsiders`() {
        val useCase = ListLibraryMembers(libraryRepository, userRepository, guard)

        assertEquals(listOf("Anna", "Jan"), useCase.execute(member.id, library.id).map { it.name })
        assertThrows<LibraryAccessDeniedException> { useCase.execute(outsider.id, library.id) }
    }

    @Test
    fun `only the owner renames the library`() {
        every { libraryRepository.save(any()) } answers { firstArg() }
        val useCase = RenameLibrary(libraryRepository, guard, policy)

        assertEquals("Biblioteczka rodzinna", useCase.execute(owner.id, library.id, " Biblioteczka rodzinna ").name)
        assertThrows<LibraryAccessDeniedException> { useCase.execute(member.id, library.id, "Moja") }
    }

    @Test
    fun `my libraries come with my role and member count, my own library first`() {
        val family = Library.create("Biblioteczka rodzinna", outsider.id)
        every { libraryRepository.findMembers(family.id) } returns
            listOf(LibraryMember(family.id, outsider.id, LibraryRole.OWNER), LibraryMember(family.id, member.id, LibraryRole.MEMBER))
        val own = Library.create("Biblioteczka Jana", member.id)
        every { libraryRepository.findByMember(member.id) } returns listOf(family, library, own)
        every { libraryRepository.findMembers(own.id) } returns listOf(LibraryMember(own.id, member.id, LibraryRole.OWNER))

        val summaries = ListMyLibraries(libraryRepository).summaries(member.id)

        assertEquals("Biblioteczka Jana", summaries.first().name)
        assertEquals(
            mapOf(
                "Biblioteczka Jana" to (LibraryRole.OWNER to 1),
                "Biblioteczka rodzinna" to (LibraryRole.MEMBER to 2),
                "Biblioteczka Anny" to (LibraryRole.MEMBER to 2),
            ),
            summaries.associate { it.name to (it.myRole to it.memberCount) },
        )
    }
}
