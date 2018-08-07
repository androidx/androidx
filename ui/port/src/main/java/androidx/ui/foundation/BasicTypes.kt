package androidx.ui.foundation

// Signature for callbacks that filter an iterable.
typealias IterableFilter<T> = (Iterable<T>) -> Iterable<T>

// Signature for callbacks that report that an underlying value has changed.
// See also [ValueSetter].
typealias ValueChanged<T> = (T) -> Unit