package androidx.ui

import kotlin.math.truncate

// These are purely Crane helpers for flutter migration. Feel free to add more.

// Copied from Dart
fun lerpDouble(a: Double, b: Double, t: Double): Double {
    return a + (b - a) * t
}

// Copied from Dart
fun Double.toStringAsFixed(digits: Int) = java.lang.String.format("%.${digits}f", this)!!

// Copied from Dart
fun Double.truncDiv(other: Double) = truncate(this / other).toInt()

fun Double.clamp(min: Double, max: Double) = Math.max(min, Math.min(max, this))

// This is our wrapper for Dart's type to reduce the amount of refactoring
data class Type(val clazz: Class<out Any>) {
    companion object {
        fun fromObject(obj: Any): Type {
            return Type(obj::class.java)
        }
    }

    override fun toString(): String {
        return clazz.simpleName
    }
}

fun Any.runtimeType() = Type.fromObject(this)

// / Signature of callbacks that have no arguments and return no data.
typealias VoidCallback = () -> Unit

fun assert(conditionFunction: () -> Boolean) {
    // TODO(Filip): This should run only in debug mode
    val result = conditionFunction.invoke()
    if (!result) {
        throw AssertionError()
    }
}