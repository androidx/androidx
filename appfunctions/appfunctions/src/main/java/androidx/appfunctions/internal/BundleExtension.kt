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

package androidx.appfunctions.internal

import android.os.Build
import android.os.Bundle
import android.os.Parcelable

/**
 * Returns the [String] value associated with [key].
 *
 * @throws IllegalArgumentException if [key] is missing or its value is not a [String].
 */
internal fun Bundle.requireString(key: String): String =
    requireNotNull(getString(key)) { "Missing $key" }

/**
 * Returns the [CharSequence] value associated with [key].
 *
 * @throws IllegalArgumentException if [key] is missing or its value is not a [CharSequence].
 */
internal fun Bundle.requireCharSequence(key: String): CharSequence =
    requireNotNull(getCharSequence(key)) { "Missing $key" }

/**
 * Returns the [List] of non-null [String]s associated with [key], or `null` if [key] is missing or
 * its value is not a `String[]`.
 *
 * @throws IllegalArgumentException if the array contains a `null` element.
 */
internal fun Bundle.getNonNullStringList(key: String): List<String>? {
    val array = getStringArray(key) ?: return null
    return array.map { element -> requireNotNull(element) { "Malformed entry in $key" } }
}

/**
 * Returns the [List] of non-null [String]s associated with [key].
 *
 * @throws IllegalArgumentException if [key] is missing, its value is not a `String[]`, or the array
 *   contains a `null` element.
 */
internal fun Bundle.requireNonNullStringList(key: String): List<String> =
    requireNotNull(getNonNullStringList(key)) { "Missing $key" }

/**
 * Returns the nested [Bundle] associated with [key].
 *
 * @throws IllegalArgumentException if [key] is missing or its value is not a [Bundle].
 */
internal fun Bundle.requireBundle(key: String): Bundle =
    requireNotNull(getBundle(key)) { "Missing $key" }

/**
 * Returns the [Parcelable] of type [T] associated with [key].
 *
 * @throws IllegalArgumentException if [key] is missing or its value is not of type [T].
 */
internal inline fun <reified T : Parcelable> Bundle.requireParcelable(key: String): T {
    // Even though the typed overload was introduced in API 33, gate on API 34 to avoid Android 13
    // platform bugs in Parcel/Bundle typed unparceling (b/232589966, b/240585930), matching
    // BundleCompat.getParcelable.
    val value: T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            getParcelable(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelable(key) as? T
        }
    return requireNotNull(value) { "Missing $key" }
}

/**
 * Returns the [List] of [Bundle]s associated with [key], stored as a parcelable array list.
 *
 * @throws IllegalArgumentException if [key] is missing, is not a parcelable list, or contains an
 *   element that is not a [Bundle].
 */
internal fun Bundle.requireBundleList(key: String): List<Bundle> {
    // Even though the typed overload was introduced in API 33, gate on API 34 to avoid Android 13
    // platform bugs in Parcel/Bundle typed unparceling (b/232589966, b/240585930), matching
    // BundleCompat.getParcelableArrayList.
    val rawList: List<*> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requireNotNull(getParcelableArrayList(key, Bundle::class.java)) { "Missing $key" }
        } else {
            @Suppress("DEPRECATION")
            requireNotNull(getParcelableArrayList<Bundle>(key)) { "Missing $key" }
        }
    return rawList.map { element ->
        requireNotNull(element as? Bundle) { "Malformed entry in $key" }
    }
}
