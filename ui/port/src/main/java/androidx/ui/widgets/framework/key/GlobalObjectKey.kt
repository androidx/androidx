package androidx.ui.widgets.framework.key

import androidx.ui.foundation.diagnostics.describeIdentity
import androidx.ui.runtimeType
import androidx.ui.widgets.framework.State
import androidx.ui.widgets.framework.StatefulWidget

/**
 * A global key that takes its identity from the object used as its value.
 *
 * Used to tie the identity of a widget to the identity of an object used to
 * generate that widget.
 *
 * If the object is not private, then it is possible that collisions will occur
 * where independent widgets will reuse the same object as their
 * [GlobalObjectKey] value in a different part of the tree, leading to a global
 * key conflict. To avoid this problem, create a private [GlobalObjectKey]
 * subclass, as in:
 *
 * ```dart
 * class _MyKey extends GlobalObjectKey {
 *   const _MyKey(Object value) : super(value);
 * }
 * ```
 *
 * Since the [runtimeType] of the key is part of its identity, this will
 * prevent clashes with other [GlobalObjectKey]s even if they have the same
 * value.
 *
 * Any [GlobalObjectKey] created for the same value will match.
 */
data class GlobalObjectKey<T : State<StatefulWidget>>(
    /** The object whose identity is used by this key's [operator==]. */
    val value: Any
) : GlobalKey<T>() {

// TODO (Migration/Filip): Not needed for data class
//    @override
//    bool operator ==(dynamic other) {
//        if (other.runtimeType != runtimeType)
//            return false;
//        final GlobalObjectKey<T> typedOther = other;
//        return identical(value, typedOther.value);
//    }
//
//    @override
//    int get hashCode => identityHashCode(value);

    override fun toString(): String {
        var selfType = runtimeType().toString()
        // const GlobalObjectKey().runtimeType.toString() returns 'GlobalObjectKey<State<StatefulWidget>>'
        // because GlobalObjectKey is instantiated to its bounds. To avoid cluttering the output
        // we remove the suffix.
        val suffix = "<State<StatefulWidget>>"
        if (selfType.endsWith(suffix)) {
            selfType = selfType.substring(0, selfType.length - suffix.length)
        }
        return "[$selfType ${describeIdentity(value)}]"
    }
}