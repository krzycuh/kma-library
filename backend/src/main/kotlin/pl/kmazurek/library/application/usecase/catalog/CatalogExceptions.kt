package pl.kmazurek.library.application.usecase.catalog

import pl.kmazurek.library.application.usecase.common.ForbiddenException
import pl.kmazurek.library.application.usecase.common.NotFoundException

class BookNotFoundException(message: String) : NotFoundException(message)

class BookEditForbiddenException(message: String) : ForbiddenException(message)
