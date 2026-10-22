package pl.kmazurek.library.domain.model.user

/**
 * Value Object representing a hashed password
 * This is the hashed form, never store plain text passwords
 */
@JvmInline
value class HashedPassword(val value: String) {
    init {
        require(value.isNotBlank()) { "Hashed password cannot be blank" }
    }

    override fun toString() = "HashedPassword(***)"
}
