package pl.kmazurek.library.domain.model.user

/**
 * Value Object representing an email address
 * Enforces basic email validation rules
 */
@JvmInline
value class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "Email cannot be blank" }
        require(isValidEmail(value)) { "Invalid email format: $value" }
    }

    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()

        private fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email)
    }

    override fun toString() = value
}
