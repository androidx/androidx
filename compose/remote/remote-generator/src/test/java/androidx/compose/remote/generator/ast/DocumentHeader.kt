/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.generator.ast

import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.WireBuffer
import java.util.Objects

/**
 * Metadata header for a RemoteCompose document. Follows AndroidX API guidelines (standard class,
 * immutable, no data class).
 */
class DocumentHeader(
    val majorVersion: Int,
    val minorVersion: Int,
    val patchVersion: Int,
    val width: Int,
    val height: Int,
    val capabilities: Long = 0L,
    properties: Map<Short, Any> = emptyMap(),
) {
    val properties: Map<Short, Any> =
        if (properties.isEmpty()) {
            emptyMap()
        } else {
            val map = LinkedHashMap<Short, Any>()
            if (!properties.containsKey(DOC_WIDTH) && width != DEFAULT_DIMENSION) {
                map[DOC_WIDTH] = width
            }
            if (!properties.containsKey(DOC_HEIGHT) && height != DEFAULT_DIMENSION) {
                map[DOC_HEIGHT] = height
            }
            if (capabilities != 0L && !properties.containsKey(DOC_PROFILES)) {
                map[DOC_PROFILES] = capabilities
            }
            map.putAll(properties)
            map
        }

    fun write(buffer: WireBuffer) {
        buffer.start(Operations.HEADER)
        if (properties.isEmpty()) {
            buffer.writeInt(majorVersion)
            buffer.writeInt(minorVersion)
            buffer.writeInt(patchVersion)
            buffer.writeInt(width)
            buffer.writeInt(height)
            buffer.writeLong(capabilities)
        } else {
            // Write properties header format
            val magic = MAGIC_NUMBER or (majorVersion and 0xFFFF)
            buffer.writeInt(magic)
            buffer.writeInt(minorVersion)
            buffer.writeInt(patchVersion)

            val streamProperties = LinkedHashMap<Short, Any>()
            if (!properties.containsKey(DOC_WIDTH) && width != DEFAULT_DIMENSION) {
                streamProperties[DOC_WIDTH] = width
            }
            if (!properties.containsKey(DOC_HEIGHT) && height != DEFAULT_DIMENSION) {
                streamProperties[DOC_HEIGHT] = height
            }
            if (capabilities != 0L && !properties.containsKey(DOC_PROFILES)) {
                streamProperties[DOC_PROFILES] = capabilities
            }
            streamProperties.putAll(properties)

            buffer.writeInt(streamProperties.size)
            for ((key, value) in streamProperties) {
                writeProperty(buffer, key, value)
            }
        }
    }

    private fun writeProperty(buffer: WireBuffer, key: Short, value: Any) {
        require(key.toInt() in 0..0x3F) { "Property key out of range (0..63): $key" }
        when (value) {
            is Int -> {
                val tag = (key.toInt() or (DATA_TYPE_INT shl 10)).toShort()
                buffer.writeShort(tag.toInt())
                buffer.writeShort(4)
                buffer.writeInt(value)
            }
            is Float -> {
                val tag = (key.toInt() or (DATA_TYPE_FLOAT shl 10)).toShort()
                buffer.writeShort(tag.toInt())
                buffer.writeShort(4)
                buffer.writeFloat(value)
            }
            is Long -> {
                val tag = (key.toInt() or (DATA_TYPE_LONG shl 10)).toShort()
                buffer.writeShort(tag.toInt())
                buffer.writeShort(8)
                buffer.writeLong(value)
            }
            is String -> {
                val tag = (key.toInt() or (DATA_TYPE_STRING shl 10)).toShort()
                val bytes = value.toByteArray(Charsets.UTF_8)
                buffer.writeShort(tag.toInt())
                buffer.writeShort(bytes.size + 4)
                buffer.writeBuffer(bytes)
            }
            else -> throw IllegalArgumentException("Unsupported property type: ${value.javaClass}")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DocumentHeader) return false
        return majorVersion == other.majorVersion &&
            minorVersion == other.minorVersion &&
            patchVersion == other.patchVersion &&
            width == other.width &&
            height == other.height &&
            capabilities == other.capabilities &&
            properties == other.properties
    }

    override fun hashCode(): Int =
        Objects.hash(
            majorVersion,
            minorVersion,
            patchVersion,
            width,
            height,
            capabilities,
            properties,
        )

    override fun toString(): String =
        "DocumentHeader(v$majorVersion.$minorVersion.$patchVersion, ${width}x$height, " +
            "capabilities=$capabilities, properties=$properties)"

    companion object {
        /** Property tag key for document width in pixels. */
        const val DOC_WIDTH: Short = 5

        /** Property tag key for document height in pixels. */
        const val DOC_HEIGHT: Short = 6

        /** Property tag key for supported profile capabilities. */
        const val DOC_PROFILES: Short = 14

        private const val DEFAULT_DIMENSION = 256
        private const val MAGIC_NUMBER = 0x048C0000
        private const val MAX_HEADER_PROPERTIES = 1000
        private const val DATA_TYPE_INT = 0
        private const val DATA_TYPE_FLOAT = 1
        private const val DATA_TYPE_LONG = 2
        private const val DATA_TYPE_STRING = 3

        fun read(buffer: WireBuffer): DocumentHeader {
            val type = buffer.readByte() and 0xFF
            require(type == Operations.HEADER) { "Invalid header type: $type" }

            val rawMajor = buffer.readInt()
            val minor = buffer.readInt()
            val patch = buffer.readInt()

            if (rawMajor in 0 until 0x10000) {
                val w = buffer.readInt()
                val h = buffer.readInt()
                val cap = buffer.readLong()
                return DocumentHeader(rawMajor, minor, patch, w, h, cap)
            }

            require((rawMajor and 0xFFFF0000.toInt()) == MAGIC_NUMBER) {
                "Invalid header magic number: 0x${rawMajor.toUInt().toString(16)}"
            }
            val major = rawMajor and 0xFFFF
            val numProps = buffer.readInt()
            require(numProps in 0..MAX_HEADER_PROPERTIES) {
                "Invalid header property count: $numProps"
            }
            val props = LinkedHashMap<Short, Any>()

            for (i in 0 until numProps) {
                val tag = buffer.readShort().toShort().toInt()
                buffer.readShort() // itemLen
                val dataType = tag shr 10
                val key = (tag and 0x3F).toShort()
                val value: Any? =
                    when (dataType) {
                        DATA_TYPE_INT -> buffer.readInt()
                        DATA_TYPE_FLOAT -> buffer.readFloat()
                        DATA_TYPE_LONG -> buffer.readLong()
                        DATA_TYPE_STRING -> buffer.readUTF8(4000)
                        else -> null
                    }
                if (value != null) {
                    props[key] = value
                }
            }

            val w = (props[DOC_WIDTH] as? Number)?.toInt() ?: DEFAULT_DIMENSION
            val h = (props[DOC_HEIGHT] as? Number)?.toInt() ?: DEFAULT_DIMENSION
            val cap =
                (props[DOC_PROFILES] as? Number)?.let {
                    if (it is Int) it.toLong() and 0xFFFFFFFFL else it.toLong()
                } ?: 0L

            return DocumentHeader(major, minor, patch, w, h, cap, props)
        }
    }
}
