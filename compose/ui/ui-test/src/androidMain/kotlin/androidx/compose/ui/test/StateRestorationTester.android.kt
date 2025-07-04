/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.test

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.VisibleForTesting

/**
 * Controls whether platform-specific encoding and decoding is applied during state saving.
 *
 * When `true`, the `platformEncodeDecode` function processes the state returned by
 * `currentRegistry.performSave()`, triggering Parcelization on Android. When `false`, Parcelization
 * is bypassed entirely.
 *
 * The default is `false` to prevent Compose tests from failing due to non-Parcelable instances.
 * Once those issues are resolved, this flag will be removed, and platform encoding will be enabled
 * by default.
 */
@VisibleForTesting internal var IS_PLATFORM_ENCODING_AND_DECODING_ENABLED = false

@OptIn(ExperimentalTestApi::class)
internal actual fun platformEncodeDecode(
    savedState: Map<String, List<Any?>>
): Map<String, List<Any?>> {
    // If platform encoding/decoding is disabled, return the state as-is.
    if (!IS_PLATFORM_ENCODING_AND_DECODING_ENABLED) return savedState

    // Instrumentation tests can involve multiple class loaders, potentially leading to
    // `ClassNotFoundException` during state unmarshalling. This function addresses
    // this by constructing a `CompositeClassLoader` that combines the class loader
    // of `StateRestorationTester` with those found within the `savedState` map.
    //
    // The `savedState.values.flatten()` operation attempts to gather all relevant
    // class loaders. However, due to potential nested lists within the saved state,
    // class loaders of deeply nested instances may be overlooked. This could result in
    // class-loading failures in those specific, less common scenarios.
    val parentLoader = StateRestorationTester::class.java.classLoader
    val childLoaders = savedState.values.flatten().mapNotNull { it?.javaClass?.classLoader }
    val compositeClassLoader = CompositeClassLoader(parentLoader, childLoaders)

    // Convert `Map` to `Bundle`.
    val inBundle = Bundle().apply { classLoader = compositeClassLoader }
    for ((key, value) in savedState) {
        @Suppress("UNCHECKED_CAST") val list = ArrayList(value) as ArrayList<Parcelable?>
        inBundle.putParcelableArrayList(key, list)
    }

    // Serialize a `Bundle` object into a `ByteArray`.
    val inParcel = Parcel.obtain()
    inParcel.writeBundle(inBundle)
    val bytes = inParcel.marshall()
    inParcel.recycle()

    // Deserialize a `ByteArray` back into a `SavedState` object.
    val outParcel = Parcel.obtain()
    outParcel.unmarshall(bytes, 0, bytes.size)
    outParcel.setDataPosition(0)
    val outBundle = outParcel.readBundle(compositeClassLoader)!!
    outParcel.recycle()

    // Check the serialized data size is within 1 MB to prevent excessively large state objects.
    check(bytes.size <= 1024 * 1024) { "Bundle exceeds maximum size (1 MB): ${bytes.size} bytes." }

    // Convert `Bundle` to `Map`.
    return outBundle.keySet().associateWith { key ->
        @Suppress("DEPRECATION") outBundle.getParcelableArrayList<Parcelable?>(key)!!
    }
}

/**
 * A composite [ClassLoader] that delegates class loading to a set of provided class loaders. If a
 * class cannot be found in any of the specified loaders, it falls back to the [parentLoader]
 * loader.
 */
private class CompositeClassLoader(
    parentLoader: ClassLoader?,
    childLoaders: Iterable<ClassLoader>,
) : ClassLoader(parentLoader) {

    private val loaders = childLoaders.toSet() // Remove duplicates.

    override fun loadClass(name: String?): Class<*>? {
        for (loader in loaders) {
            try {
                return loader.loadClass(name)
            } catch (_: ClassNotFoundException) {
                // Ignore and try the next loader.
            }
        }
        return super.loadClass(name)
    }
}
