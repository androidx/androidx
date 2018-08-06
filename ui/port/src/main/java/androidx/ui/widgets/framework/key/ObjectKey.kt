package androidx.ui.widgets.framework.key

import androidx.ui.foundation.LocalKey
import androidx.ui.foundation.diagnostics.describeIdentity
import androidx.ui.runtimeType

// / A key that takes its identity from the object used as its value.
// /
// / Used to tie the identity of a widget to the identity of an object used to
// / generate that widget.
// /
// / See also the discussions at [Key] and [Widget.key].
data class ObjectKey(
        // / The object whose identity is used by this key's [operator==].
    val value: Any
) : LocalKey() {

    // TODO(Migration/Filip): Not needed for data class
    //    @override
    //    bool operator ==(dynamic other) {
    //        if (other.runtimeType != runtimeType)
    //            return false;
    //        final ObjectKey typedOther = other;
    //        return identical(value, typedOther.value);
    //    }
    //
    //    @override
    //    int get hashCode => hashValues(runtimeType, identityHashCode(value));

    override fun toString(): String {
        if (runtimeType().clazz == ObjectKey::class.java)
            return "[${describeIdentity(value)}]"
        return "[${runtimeType()} ${describeIdentity(value)}]"
    }
}