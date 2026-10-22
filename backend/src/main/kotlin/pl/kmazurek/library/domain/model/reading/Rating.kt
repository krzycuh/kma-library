package pl.kmazurek.library.domain.model.reading

/**
 * Value Object: rating on a 1..5 star scale
 */
@JvmInline
value class Rating(val value: Int) {
    init {
        require(value in MIN..MAX) { "Rating must be between $MIN and $MAX" }
    }

    companion object {
        const val MIN = 1
        const val MAX = 5
    }
}
