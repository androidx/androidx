package app.cash.paparazzi

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.security.PrivilegedAction
import sun.misc.Unsafe

/**
 * Inspired by and ported from:
 * https://github.com/powermock/powermock/commit/fc092c5d7e339d01e079184a2a0e88b5c46fc0e8
 * https://github.com/powermock/powermock/commit/bd92bcc5329c4981cf09dece5c3eafcf92fe49ff
 */
internal fun Class<*>.getFieldReflectively(fieldName: String): Field =
  try {
    this.getDeclaredField(fieldName).also { it.isAccessible = true }
  } catch (e: NoSuchFieldException) {
    throw RuntimeException("Field '$fieldName' was not found in class $name.")
  }

internal fun Field.setStaticValue(value: Any) {
  try {
    this.isAccessible = true
    val isFinalModifierPresent = this.modifiers and Modifier.FINAL == Modifier.FINAL
    if (isFinalModifierPresent) {
      @Suppress("DEPRECATION")
      java.security.AccessController.doPrivileged<Any?>(
        PrivilegedAction {
          try {
            val unsafe = Unsafe::class.java.getFieldReflectively("theUnsafe").get(null) as Unsafe
            val offset = unsafe.staticFieldOffset(this)
            val base = unsafe.staticFieldBase(this)
            unsafe.setFieldValue(this, base, offset, value)
            null
          } catch (t: Throwable) {
            throw RuntimeException(t)
          }
        }
      )
    } else {
      this.set(null, value)
    }
  } catch (ex: SecurityException) {
    throw RuntimeException(ex)
  } catch (ex: IllegalAccessException) {
    throw RuntimeException(ex)
  } catch (ex: IllegalArgumentException) {
    throw RuntimeException(ex)
  }
}

internal fun Unsafe.setFieldValue(field: Field, base: Any, offset: Long, value: Any) =
  when (field.type) {
    Integer.TYPE -> this.putInt(base, offset, (value as Int))
    java.lang.Short.TYPE -> this.putShort(base, offset, (value as Short))
    java.lang.Long.TYPE -> this.putLong(base, offset, (value as Long))
    java.lang.Byte.TYPE -> this.putByte(base, offset, (value as Byte))
    java.lang.Boolean.TYPE -> this.putBoolean(base, offset, (value as Boolean))
    java.lang.Float.TYPE -> this.putFloat(base, offset, (value as Float))
    java.lang.Double.TYPE -> this.putDouble(base, offset, (value as Double))
    Character.TYPE -> this.putChar(base, offset, (value as Char))
    else -> this.putObject(base, offset, value)
  }
