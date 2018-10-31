package androidx.ui

import android.os.Looper
import kotlin.math.truncate

// These are purely Crane helpers for flutter migration. Feel free to add more.

// Copied from Dart
fun lerpDouble(a: Double, b: Double, t: Double): Double {
    return a + (b - a) * t
}

fun lerpInt(a: Int, b: Int, t: Double): Double {
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

fun Int.toRadixString(size: Int): String {
    val asLong = this.toLong() and 0xFFFFFFFF
    return java.lang.Long.toString(asLong, size)
}

// Convenience port of Dart's sublist(int) method.  Maybe remove later.
fun <E> List<E>.subList(fromIndex: Int) = this.subList(fromIndex, this.size)

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

@Deprecated("Use property instead", ReplaceWith("this.runtimeType"))
fun Any.runtimeType() = Type.fromObject(this)

// Duplicates functionality of Any.runtimeType() method but more canonically accurate as a property.  That function will be removed in a future cl
val Any.runtimeType: Type
    get() = Type.fromObject(this)

/** Signature of callbacks that have no arguments and return no data. */
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
fun describeEnum(enumEntry: Enum<*>): String {
//    val description = enumEntry.toString()
//    val indexOfDot = description.indexOf('.')
//    assert(indexOfDot != -1 && indexOfDot < description.length - 1)
//    return description.substring(indexOfDot + 1)
    // TODO(Andrey) it's not the case for Kotlin. Day.monday.toString() == "monday"
    // We shouldn't rely on toString for this - changed to .name - Migration/ryanmentley
    // TODO(Migration/ryanmentley): We should probably inline this once we're done porting
    return enumEntry.name
}

fun requireMainThread() {
    require(Looper.myLooper() == Looper.getMainLooper())
}

// TODO(Migration/ryanmentley): Make these better (e.g., actual classes)?  Or eliminate them?
typealias Int32List = IntArray
typealias Float64List = DoubleArray
