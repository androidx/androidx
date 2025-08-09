/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.os

import android.os.PersistableBundle

/**
 * Returns a new [PersistableBundle] with the given key/value pairs as elements.
 *
 * Supported value types are [Int], [Long], [Double], [Boolean], and [String] and arrays of these
 * types.
 *
 * @throws IllegalArgumentException When a value is not a supported type of [PersistableBundle].
 */
fun persistableBundleOf(vararg pairs: Pair<String, Any?>): PersistableBundle {
    val persistableBundle = PersistableBundle(pairs.size)
    pairs.forEach { (key, value) ->
        PersistableBundleApi21ImplKt.putValue(persistableBundle, key, value)
    }
    return persistableBundle
}

/** Returns a new empty [PersistableBundle]. */
fun persistableBundleOf(): PersistableBundle {
    return PersistableBundle(0)
}

/**
 * Covert this map to a [PersistableBundle] with the key/value pairs as elements.
 *
 * Supported value types are [Int], [Long], [Double], [Boolean], and [String] and arrays of these
 * types.
 *
 * @throws IllegalArgumentException When a value is not a supported type of [PersistableBundle].
 */
fun Map<String, Any?>.toPersistableBundle(): PersistableBundle {
    val persistableBundle = PersistableBundle(this.size)

    for ((key, value) in this) {
        PersistableBundleApi21ImplKt.putValue(persistableBundle, key, value)
    }

    return persistableBundle
}

// These classes ends up being top-level even though they're private. The PersistableBundle prefix
// helps prevent clashes with other ApiImpls in androidx.core.os. And the Kt suffix is used by
// Jetifier to keep them grouped with other members of the core-ktx module.
private object PersistableBundleApi21ImplKt {
    @JvmStatic
    fun putValue(persistableBundle: PersistableBundle, key: String?, value: Any?) {
        persistableBundle.apply {
            when (value) {
                null -> putString(key, null) // Any nullable type will suffice.

                // Scalars
                is Boolean -> putBoolean(key, value)
                is Double -> putDouble(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)

                // References
                is String -> putString(key, value)
                is PersistableBundle -> putPersistableBundle(key, value)

                // Scalar arrays
                is BooleanArray -> putBooleanArray(key, value)
                is DoubleArray -> putDoubleArray(key, value)
                is IntArray -> putIntArray(key, value)
                is LongArray -> putLongArray(key, value)

                // Reference arrays
                is Array<*> -> {
                    val componentType = value::class.java.componentType!!
                    @Suppress("UNCHECKED_CAST") // Checked by reflection.
                    when {
                        String::class.java.isAssignableFrom(componentType) -> {
                            putStringArray(key, value as Array<String>)
                        }
                        else -> {
                            val valueType = componentType.canonicalName
                            throw IllegalArgumentException(
                                "Unsupported value array type $valueType for key \"$key\""
                            )
                        }
                    }
                }
                else -> {
                    val valueType = value.javaClass.canonicalName
                    throw IllegalArgumentException(
                        "Unsupported value type $valueType for key \"$key\""
                    )
                }
            }
        }
    }
}
