package androidx.ui.foundation

// A [Key] is an identifier for [Widget]s, [Element]s and [SemanticsNode]s.
/**
 *
 * A new widget will only be used to update an existing element if its key is
 * the same as the key of the current widget associated with the element.
 *
 * Keys must be unique amongst the [Element]s with the same parent.
 *
 * Subclasses of [Key] should either subclass [LocalKey] or [GlobalKey].
 *
 * See also the discussion at [Widget.key].
 */
abstract class Key {

    companion object {
        /**
         * Construct a [ValueKey<String>] with the given [String].
         *
         * This is the simplest way to create keys.
         */
        fun createKey(value: String) = ValueKey(value)
    }
}

/**
 * A key that is not a [GlobalKey].
 *
 * Keys must be unique amongst the [Element]s with the same parent. By
 * contrast, [GlobalKey]s must be unique across the entire app.
 *
 * See also the discussion at [Widget.key].
 */
abstract class LocalKey : Key()

/**
 * A key that uses a value of a particular type to identify itself.
 *
 * A [ValueKey<T>] is equal to another [ValueKey<T>] if, and only if, their
 * values are [operator==].
 *
 * This class can be subclassed to create value keys that will not be equal to
 * other value keys that happen to use the same value. If the subclass is
 * private, this results in a value key type that cannot collide with keys from
 * other sources, which could be useful, for example, if the keys are being
 * used as fallbacks in the same scope as keys supplied from another widget.
 *
 * See also the discussion at [Widget.key].
 */
data class ValueKey<T : Any>(
    /** The value to which this key delegates its [operator==] */
    val value: T
) : LocalKey() {

    override fun toString(): String {
        val valueString = if (value is String) { "<'$value'>" } else { "<$value>" }
        return "[${value::class.java.simpleName} $valueString]"
    }
}
