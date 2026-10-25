package pl.kmazurek.library.application.usecase.shelf

import pl.kmazurek.library.application.usecase.common.ConflictException
import pl.kmazurek.library.application.usecase.common.ForbiddenException
import pl.kmazurek.library.application.usecase.common.NotFoundException

class LibraryNotFoundException(message: String) : NotFoundException(message)

class ShelfItemNotFoundException(message: String) : NotFoundException(message)

class LibraryAccessDeniedException(message: String) : ForbiddenException(message)

class ShelfItemAlreadyLentException(message: String) : ConflictException(message)

class NoActiveLoanException(message: String) : ConflictException(message)

class LibraryMemberNotFoundException(message: String) : NotFoundException(message)

class AlreadyLibraryMemberException(message: String) : ConflictException(message)

class CannotRemoveLibraryMemberException(message: String) : ForbiddenException(message)
