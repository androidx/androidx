/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.privacysandbox.databridge.integration.testutils

import android.os.Bundle
import androidx.privacysandbox.databridge.core.Key
import java.io.IOException
import java.util.ArrayList

private const val EXCEPTION_NAME = "exceptionName"
private const val EXCEPTION_MESSAGE = "exceptionMessage"
private const val KEY_NAME = "keyName"
private const val KEY_TYPE = "keyType"
private const val IS_SUCCESS = "isSuccess"
private const val VALUE = "value"

fun Bundle.toKeyResultPair(): Pair<Key, Result<Any?>> {
    return when (getString(KEY_TYPE)) {
        "INT" -> {
            val res =
                getResultFromBundle(this) { bundle, key ->
                    if (bundle.getBoolean("isValueNull")) {
                        null
                    } else {
                        bundle.getInt(key)
                    }
                }
            Key.createIntKey(getString(KEY_NAME)!!) to res
        }
        "LONG" -> {
            val res =
                getResultFromBundle(this) { bundle, key ->
                    if (bundle.getBoolean("isValueNull")) {
                        null
                    } else {
                        bundle.getLong(key)
                    }
                }
            Key.createLongKey(getString(KEY_NAME)!!) to res
        }
        "FLOAT" -> {
            val res =
                getResultFromBundle(this) { bundle, key ->
                    if (bundle.getBoolean("isValueNull")) {
                        null
                    } else {
                        bundle.getFloat(key)
                    }
                }
            Key.createFloatKey(getString(KEY_NAME)!!) to res
        }
        "DOUBLE" -> {
            val res =
                getResultFromBundle(this) { bundle, key ->
                    if (bundle.getBoolean("isValueNull")) {
                        null
                    } else {
                        bundle.getDouble(key)
                    }
                }
            Key.createDoubleKey(getString(KEY_NAME)!!) to res
        }
        "BOOLEAN" -> {
            val res =
                getResultFromBundle(this) { bundle, key ->
                    if (bundle.getBoolean("isValueNull")) {
                        null
                    } else {
                        bundle.getBoolean(key)
                    }
                }
            Key.createBooleanKey(getString(KEY_NAME)!!) to res
        }
        "STRING" -> {
            val res =
                getResultFromBundle(this) { bundle, key ->
                    if (bundle.getBoolean("isValueNull")) {
                        null
                    } else {
                        bundle.getString(key)
                    }
                }
            Key.createStringKey(getString(KEY_NAME)!!) to res
        }
        "STRING_SET" -> {
            val res =
                getResultFromBundle(this) { bundle, key ->
                    if (bundle.getBoolean("isValueNull")) {
                        null
                    } else {
                        bundle.getStringArrayList(key)?.toSet()
                    }
                }
            Key.createStringSetKey(getString(KEY_NAME)!!) to res
        }
        "BYTE_ARRAY" -> {
            val res =
                getResultFromBundle(this) { bundle, key ->
                    if (bundle.getBoolean("isValueNull")) {
                        null
                    } else {
                        bundle.getByteArray(key)
                    }
                }
            Key.createByteArrayKey(getString(KEY_NAME)!!) to res
        }
        else -> throw IllegalArgumentException("Unsupported type: " + getString(KEY_TYPE))
    }
}

fun Bundle.toKeyValuePair(keyName: String, keyType: String): Pair<Key, Any?> {
    return when (keyType) {
        "INT" -> Key.createIntKey(keyName) to getInt(VALUE)
        "LONG" -> Key.createLongKey(keyName) to getLong(VALUE)
        "FLOAT" -> Key.createFloatKey(keyName) to getFloat(VALUE)
        "DOUBLE" -> Key.createDoubleKey(keyName) to getDouble(VALUE)
        "BOOLEAN" -> Key.createBooleanKey(keyName) to getBoolean(VALUE)
        "STRING" -> Key.createStringKey(keyName) to getString(VALUE)
        "STRING_SET" -> {
            Key.createStringSetKey(keyName) to getStringArrayList(VALUE)?.toSet()
        }
        "BYTE_ARRAY" -> Key.createByteArrayKey(keyName) to getByteArray(VALUE)
        else -> throw IllegalArgumentException("Unsupported type: " + getString(KEY_TYPE))
    }
}

@Suppress("UNCHECKED_CAST")
fun Bundle.fromKeyValue(key: Key, value: Any?): Bundle {
    when (key.type.toString()) {
        "INT" -> putInt(VALUE, value as Int)
        "LONG" -> putLong(VALUE, value as Long)
        "FLOAT" -> putFloat(VALUE, value as Float)
        "DOUBLE" -> putDouble(VALUE, value as Double)
        "BOOLEAN" -> putBoolean(VALUE, value as Boolean)
        "STRING" -> putString(VALUE, value as String)
        "STRING_SET" -> {
            val data = value as Set<*>
            putStringArrayList(VALUE, data.toList() as ArrayList<String>)
        }
        "BYTE_ARRAY" -> putByteArray(VALUE, value as ByteArray)
    }
    return this
}

@Suppress("UNCHECKED_CAST")
fun Bundle.fromKeyResult(key: Key, result: Result<Any?>): Bundle {
    putString(KEY_NAME, key.name)
    putString(KEY_TYPE, key.type.toString())

    if (result.isSuccess) {
        putBoolean("isSuccess", true)
        val value = result.getOrNull()
        if (value == null) {
            putBoolean("isValueNull", true)
            return this
        }
        putBoolean("isValueNull", false)
        when (key.type.toString()) {
            "INT" -> putInt(VALUE, value as Int)
            "LONG" -> putLong(VALUE, value as Long)
            "FLOAT" -> putFloat(VALUE, value as Float)
            "DOUBLE" -> putDouble(VALUE, value as Double)
            "BOOLEAN" -> putBoolean(VALUE, value as Boolean)
            "STRING" -> putString(VALUE, value as String)
            "STRING_SET" -> {
                val data = value as Set<*>
                putStringArrayList(VALUE, data.toList() as ArrayList<String?>?)
            }
            "BYTE_ARRAY" -> putByteArray(VALUE, value as ByteArray)
        }
    } else {
        putBoolean(IS_SUCCESS, false)
        val exception: Throwable? = result.exceptionOrNull()
        putString(EXCEPTION_NAME, exception!!::class.java.canonicalName)
        putString(EXCEPTION_MESSAGE, exception.message)
    }
    return this
}

private fun <T> getResultFromBundle(
    bundle: Bundle,
    valueGetter: (Bundle, String) -> T,
): Result<Any?> {
    return if (bundle.getBoolean(IS_SUCCESS)) {
        Result.success(valueGetter(bundle, VALUE))
    } else {
        getResultFailureFromThrowable(
            bundle.getString(EXCEPTION_NAME)!!,
            bundle.getString(EXCEPTION_MESSAGE)!!,
        )
    }
}

private fun getResultFailureFromThrowable(
    exceptionName: String,
    exceptionMessage: String,
): Result<Any?> {
    val throwable =
        when (exceptionName) {
            java.lang.ClassCastException::class.java.canonicalName -> {
                ClassCastException(exceptionMessage)
            }
            IOException::class.java.canonicalName -> {
                IOException(exceptionMessage)
            }
            else -> {
                IllegalStateException(exceptionMessage)
            }
        }
    return Result.failure(throwable)
}
