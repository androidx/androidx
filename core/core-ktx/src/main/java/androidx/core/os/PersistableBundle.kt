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

import android.os.Build
import android.os.PersistableBundle
import androidx.annotation.RequiresApi

/**
 * Returns a new [PersistableBundle] with the given key/value pairs as elements.
 *
 * Supported value types are [Int], [Long], [Double], and [String] and arrays of these types. On API
 * 22 and later [Boolean] and [BooleanArray] are also supported.
 *
 * @throws IllegalArgumentException When a value is not a supported type of [PersistableBundle].
 */
@RequiresApi(21)
fun persistableBundleOf(vararg pairs: Pair<String, Any?>): PersistableBundle {
    val persistableBundle = PersistableBundleApi21ImplKt.createPersistableBundle(pairs.size)
    pairs.forEach { (key, value) ->
        PersistableBundleApi21ImplKt.putValue(persistableBundle, key, value)
    }
    return persistableBundle
}

/** Returns a new empty [PersistableBundle]. */
@RequiresApi(21)
fun persistableBundleOf(): PersistableBundle {
    return PersistableBundleApi21ImplKt.createPersistableBundle(0)
}

/**
 * Covert this map to a [PersistableBundle] with the key/value pairs as elements.
 *
 * Supported value types are [Int], [Long], [Double], and [String] and arrays of these types. On API
 * 22 and later [Boolean] and [BooleanArray] are also supported.
 *
 * @throws IllegalArgumentException When a value is not a supported type of [PersistableBundle].
 */
@RequiresApi(21)
fun Map<String, Any?>.toPersistableBundle(): PersistableBundle {
    val persistableBundle = PersistableBundleApi21ImplKt.createPersistableBundle(this.size)

    for ((key, value) in this) {
        PersistableBundleApi21ImplKt.putValue(persistableBundle, key, value)
    }

    return persistableBundle
}

// These classes ends up being top-level even though they're private. The PersistableBundle prefix
// helps prevent clashes with other ApiImpls in androidx.core.os. And the Kt suffix is used by
// Jetifier to keep them grouped with other members of the core-ktx module.
@RequiresApi(21)
private object PersistableBundleApi21ImplKt {
    @JvmStatic
    fun createPersistableBundle(capacity: Int): PersistableBundle = PersistableBundle(capacity)

    @JvmStatic
    fun putValue(persistableBundle: PersistableBundle, key: String?, value: Any?) {
        persistableBundle.apply {
            when (value) {
                null -> putString(key, null) // Any nullable type will suffice.

                // Scalars
                is Boolean -> {
                    if (Build.VERSION.SDK_INT >= 22) {
                        PersistableBundleApi22ImplKt.putBoolean(this, key, value)
                    } else {
                        throw IllegalArgumentException(
                            "Unsupported value type boolean for key \"$key\" (requires API level " +
                                "22+)"
                        )
                    }
                }
                is Double -> putDouble(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)

                // References
                is String -> putString(key, value)
                is PersistableBundle -> putPersistableBundle(key, value)

                // Scalar arrays
                is BooleanArray -> {
                    if (Build.VERSION.SDK_INT >= 22) {
                        PersistableBundleApi22ImplKt.putBooleanArray(this, key, value)
                    } else {
                        throw IllegalArgumentException(
                            "Unsupported value type boolean[] for key \"$key\" (requires API " +
                                "level 22+)"
                        )
                    }
                }
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
                        "Unsupported value type $valueType for key " + "\"$key\""
                    )
                }
            }
        }
    }
}

@RequiresApi(22)
private object PersistableBundleApi22ImplKt {
    @JvmStatic
    fun putBoolean(persistableBundle: PersistableBundle, key: String?, value: Boolean) {
        persistableBundle.putBoolean(key, value)
    }

    @JvmStatic
    fun putBooleanArray(persistableBundle: PersistableBundle, key: String?, value: BooleanArray) {
        persistableBundle.putBooleanArray(key, value)
    }
}
