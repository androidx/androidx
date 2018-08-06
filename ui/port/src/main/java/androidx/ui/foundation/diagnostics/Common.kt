package androidx.ui.foundation.diagnostics

class _NoDefaultValue

// / Marker object indicating that a [DiagnosticsNode] has no default value.
val kNoDefaultValue: _NoDefaultValue = _NoDefaultValue()

// / Returns a 5 character long hexadecimal string generated from
// / [Object.hashCode]'s 20 least-significant bits.
fun shortHash(obj: Any): String {
    // TODO(Filip): Fix this
    // return obj.hashCode().toUnsigned(20).toRadixString(16).padLeft(5, '0')
    return obj.hashCode().toString()
}

// / Returns a summary of the runtime type and hash code of `object`.
// /
// / See also:
// /
// /  * [Object.hashCode], a value used when placing an object in a [Map] or
// /    other similar data structure, and which is also used in debug output to
// /    distinguish instances of the same class (hash collisions are
// /    possible, but rare enough that its use in debug output is useful).
// /  * [Object.runtimeType], the [Type] of an object.
fun describeIdentity(obj: Any): String = "${obj::class.java.simpleName}#${shortHash(obj)}"

