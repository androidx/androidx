/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.actions

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/*
 * Preferences DataStore files are read and written here rather than through DataStore, so that the
 * test APK adds no DataStore version that could conflict with the app's. A file holds this message
 * of `androidx.datastore.preferences.PreferencesProto`:
 *
 *     message PreferenceMap { map<string, Value> preferences = 1; }
 *     message Value {
 *         oneof value {
 *             bool boolean = 1; float float = 2; int32 integer = 3; int64 long = 4;
 *             string string = 5; StringSet string_set = 6; double double = 7; bytes bytes = 8;
 *         }
 *     }
 *     message StringSet { repeated string strings = 1; }
 */

private const val VARINT = 0
private const val FIXED64 = 1
private const val LENGTH_DELIMITED = 2
private const val FIXED32 = 5

/**
 * Returns the preferences in the Preferences DataStore [file], or none if it doesn't exist.
 *
 * @throws IOException if [file] can't be read as a Preferences DataStore file
 */
internal fun readPreferencesDataStore(file: File): Map<String, Any> {
    if (!file.exists()) return emptyMap()
    val preferences = LinkedHashMap<String, Any>()
    try {
        forEachField(file.readBytes()) { number, entry ->
            if (number != 1) return@forEachField
            var key = ""
            var value: Any? = null
            forEachField(entry as ByteArray) { entryNumber, field ->
                when (entryNumber) {
                    1 -> key = (field as ByteArray).decodeToString()
                    2 -> value = decodeValue(field as ByteArray)
                }
            }
            preferences[key] = value ?: throw IOException("Preference '$key' has no value")
        }
    } catch (e: Exception) {
        throw IOException("Can't read the Preferences DataStore file $file", e)
    }
    return preferences
}

/** Writes [preferences] to the Preferences DataStore [file], replacing its content. */
internal fun writePreferencesDataStore(file: File, preferences: Map<String, Any>) {
    val map = ByteArrayOutputStream()
    for ((key, value) in preferences) {
        val entry = ByteArrayOutputStream()
        entry.writeLengthDelimited(1, key.encodeToByteArray())
        entry.writeLengthDelimited(2, encodeValue(value))
        map.writeLengthDelimited(1, entry.toByteArray())
    }
    file.parentFile?.mkdirs()
    file.writeBytes(map.toByteArray())
}

private fun decodeValue(message: ByteArray): Any {
    var value: Any? = null
    forEachField(message) { number, field ->
        when (number) {
            1 -> value = field as Long != 0L
            2 -> value = Float.fromBits(field as Int)
            3 -> value = (field as Long).toInt()
            4 -> value = field as Long
            5 -> value = (field as ByteArray).decodeToString()
            6 -> value = buildSet {
                    forEachField(field as ByteArray) { setNumber, string ->
                        if (setNumber == 1) add((string as ByteArray).decodeToString())
                    }
                }
            7 -> value = Double.fromBits(field as Long)
            8 -> value = field as ByteArray
        }
    }
    return value ?: throw IOException("Preference value of no supported type")
}

private fun encodeValue(value: Any): ByteArray {
    val out = ByteArrayOutputStream()
    when (value) {
        is Boolean -> out.writeVarintField(1, if (value) 1L else 0L)
        is Float -> {
            out.writeTag(2, FIXED32)
            out.write(littleEndian(4).putFloat(value).array())
        }
        is Int -> out.writeVarintField(3, value.toLong())
        is Long -> out.writeVarintField(4, value)
        is String -> out.writeLengthDelimited(5, value.encodeToByteArray())
        is Set<*> -> {
            val strings = ByteArrayOutputStream()
            value.forEach { strings.writeLengthDelimited(1, (it as String).encodeToByteArray()) }
            out.writeLengthDelimited(6, strings.toByteArray())
        }
        is Double -> {
            out.writeTag(7, FIXED64)
            out.write(littleEndian(8).putDouble(value).array())
        }
        is ByteArray -> out.writeLengthDelimited(8, value)
        else -> throw IllegalArgumentException("Unsupported preference value: ${value.javaClass}")
    }
    return out.toByteArray()
}

/**
 * Calls [onField] with the number and the value of each field of [message]: a `Long` for a varint
 * or a 64-bit field, an `Int` for a 32-bit field, and a `ByteArray` for a length-delimited one.
 */
private fun forEachField(message: ByteArray, onField: (number: Int, value: Any) -> Unit) {
    val buffer = ByteBuffer.wrap(message).order(ByteOrder.LITTLE_ENDIAN)
    while (buffer.hasRemaining()) {
        val tag = buffer.readVarint()
        val value: Any =
            when ((tag and 0x7).toInt()) {
                VARINT -> buffer.readVarint()
                FIXED64 -> buffer.long
                LENGTH_DELIMITED -> {
                    val length = buffer.readVarint()
                    if (length !in 0L..buffer.remaining()) {
                        throw IOException("Invalid protobuf field length $length")
                    }
                    ByteArray(length.toInt()).also { buffer.get(it) }
                }
                FIXED32 -> buffer.int
                else -> throw IOException("Unsupported protobuf field tag $tag")
            }
        onField((tag ushr 3).toInt(), value)
    }
}

private fun ByteBuffer.readVarint(): Long {
    var result = 0L
    for (shift in 0 until 64 step 7) {
        val byte = get().toInt()
        result = result or ((byte and 0x7F).toLong() shl shift)
        if ((byte and 0x80) == 0) return result
    }
    throw IOException("Malformed protobuf varint")
}

private fun littleEndian(size: Int): ByteBuffer =
    ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)

private fun ByteArrayOutputStream.writeTag(number: Int, wireType: Int) {
    writeVarint(((number shl 3) or wireType).toLong())
}

private fun ByteArrayOutputStream.writeVarintField(number: Int, value: Long) {
    writeTag(number, VARINT)
    writeVarint(value)
}

private fun ByteArrayOutputStream.writeLengthDelimited(number: Int, bytes: ByteArray) {
    writeTag(number, LENGTH_DELIMITED)
    writeVarint(bytes.size.toLong())
    write(bytes)
}

private fun ByteArrayOutputStream.writeVarint(value: Long) {
    var rest = value
    while ((rest and 0x7FL.inv()) != 0L) {
        write((rest.toInt() and 0x7F) or 0x80)
        rest = rest ushr 7
    }
    write(rest.toInt())
}
