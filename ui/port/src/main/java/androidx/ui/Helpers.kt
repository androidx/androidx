package androidx.ui

import android.os.Looper
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

// Dart spec: If both operands are ints then a ~/ b performs the truncating integer division.
fun Int.truncDiv(other: Int) = this / other

fun Double.clamp(min: Double, max: Double) = Math.max(min, Math.min(max, this))

fun Int.clamp(min: Int, max: Int) = Math.max(min, Math.min(max, this))

// TODO(Migration/Filip): Start supporting size if needed
fun Int.toRadixString(size: Int) = java.lang.Integer.toHexString(this)

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

/**
 * Returns a short description of an enum value.
 *
 * Strips off the enum class name from the `enumEntry.toString()`.
 *
 * ## Sample code
 *
 * ```dart
 * enum Day {
 *   monday, tuesday, wednesday, thursday, friday, saturday, sunday
 * }
 *
 * validateDescribeEnum() {
 *   assert(Day.monday.toString() == 'Day.monday');
 *   assert(describeEnum(Day.monday) == 'monday');
 * }
 * ```
 */
fun describeEnum(enumEntry: Any): String {
//    val description = enumEntry.toString()
//    val indexOfDot = description.indexOf('.')
//    assert(indexOfDot != -1 && indexOfDot < description.length - 1)
//    return description.substring(indexOfDot + 1)
    // TODO(Andrey) it's not the case for Kotlin. Day.monday.toString() == "monday"
    return enumEntry.toString()
}

fun requireMainThread() {
    require(Looper.myLooper() == Looper.getMainLooper())
}

// TODO(Migration/ryanmentley): Make these better (e.g., actual classes)?  Or eliminate them?
typealias Int32List = IntArray
typealias Float64List = DoubleArray
