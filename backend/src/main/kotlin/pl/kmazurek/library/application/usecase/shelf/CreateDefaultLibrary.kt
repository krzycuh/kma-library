package pl.kmazurek.library.application.usecase.shelf

import org.springframework.stereotype.Service
import pl.kmazurek.library.domain.model.shelf.Library
import pl.kmazurek.library.domain.model.shelf.LibraryMember
import pl.kmazurek.library.domain.model.shelf.LibraryRole
import pl.kmazurek.library.domain.model.user.User
import pl.kmazurek.library.domain.repository.LibraryRepository

/**
 * Use Case: create the personal library of a freshly registered user.
 * The only place where Identity & Access touches the Shelf context.
 */
@Service
class CreateDefaultLibrary(
    private val libraryRepository: LibraryRepository,
) {
    fun execute(user: User): Library {
        val library = libraryRepository.save(Library.create(defaultName(user.name), user.id))
        libraryRepository.addMember(LibraryMember(library.id, user.id, LibraryRole.OWNER))
        return library
    }

    private fun defaultName(userName: String): String {
        val firstName = userName.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        return if (firstName.isBlank()) "Moja biblioteczka" else "Biblioteczka $firstName"
    }
}
