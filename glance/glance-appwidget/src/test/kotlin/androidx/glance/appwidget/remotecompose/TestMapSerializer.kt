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

package androidx.glance.appwidget.remotecompose

import androidx.compose.remote.core.serialize.MapSerializer

/**
 * For working with the serialization api on
 * [androidx.compose.remote.core.operations.layout.LayoutComponent]
 *
 * Ai Generated.
 */
class TestMapSerializer : MapSerializer {

    val map = LinkedHashMap<String, Any?>()

    /**
     * Returns the completed, immutable map. This is a terminal operation that finalizes the
     * serialization process.
     */
    fun build(): Map<String, Any?> = map

    override fun addType(type: String): MapSerializer {
        map["type"] = type
        return this
    }

    override fun addFloatExpressionSrc(key: String, value: FloatArray): MapSerializer {
        map[key] = value
        return this
    }

    override fun addIntExpressionSrc(key: String, value: IntArray, mask: Int): MapSerializer {
        map[key] = orderedOf("src", value, "mask", mask)
        return this
    }

    override fun addPath(key: String, path: FloatArray): MapSerializer {
        map[key] = path
        return this
    }

    override fun addTags(
        vararg value: androidx.compose.remote.core.serialize.SerializeTags
    ): MapSerializer {
        // Adds tags to a dedicated "tags" key, serialized by their string name.
        map["tags"] = value.map { it.name }
        return this
    }

    override fun <T> add(key: String, value: List<T>?): MapSerializer {
        map[key] = value
        return this
    }

    override fun <T> add(key: String, value: Map<String, T>?): MapSerializer {
        map[key] = value
        return this
    }

    override fun add(
        key: String,
        value: androidx.compose.remote.core.serialize.Serializable?,
    ): MapSerializer {
        TODO()
    }

    override fun add(key: String, value: String?): MapSerializer {
        map[key] = value
        return this
    }

    override fun add(key: String, a: Float, r: Float, g: Float, b: Float): MapSerializer {
        map[key] = orderedOf("a", a, "r", r, "g", g, "b", b)
        return this
    }

    override fun add(key: String, id: Float, value: Float): MapSerializer {
        map[key] = orderedOf("id", id, "value", value)
        return this
    }

    override fun add(key: String, value: Byte?): MapSerializer {
        map[key] = value
        return this
    }

    override fun add(key: String, value: Short?): MapSerializer {
        map[key] = value
        return this
    }

    override fun add(key: String, value: Int?): MapSerializer {
        map[key] = value
        return this
    }

    override fun add(key: String, value: Long?): MapSerializer {
        map[key] = value
        return this
    }

    override fun add(key: String, value: Float?): MapSerializer {
        map[key] = value
        return this
    }

    override fun add(key: String, value: Double?): MapSerializer {
        map[key] = value
        return this
    }

    override fun add(key: String, value: Boolean?): MapSerializer {
        map[key] = value
        return this
    }

    override fun <T : Enum<T>> add(key: String, value: Enum<T>?): MapSerializer {
        // Enums are serialized by their string name for robustness.
        map[key] = value?.name
        return this
    }

    companion object {
        /**
         * Similar to `Map.of`, but creates a `LinkedHashMap` preserving insertion order for
         * predictable serialization.
         *
         * @param keysAndValues An even number of items, alternating between a `String` key and its
         *   corresponding value.
         * @return A `LinkedHashMap` containing the provided keys and values.
         * @throws IllegalArgumentException if the number of arguments is odd or if a key is not a
         *   `String`.
         */
        @JvmStatic
        fun orderedOf(vararg keysAndValues: Any): LinkedHashMap<String, Any> {
            require(keysAndValues.size % 2 == 0) {
                "Must provide an even number of arguments for keys and values."
            }
            val map = LinkedHashMap<String, Any>()
            for (i in keysAndValues.indices step 2) {
                val key =
                    keysAndValues[i] as? String
                        ?: throw IllegalArgumentException(
                            "Key at index $i must be a String, but found " +
                                "${keysAndValues[i].javaClass.simpleName}."
                        )
                map[key] = keysAndValues[i + 1]
            }
            return map
        }
    }
}
