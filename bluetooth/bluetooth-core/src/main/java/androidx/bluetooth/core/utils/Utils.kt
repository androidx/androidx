/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.bluetooth.core.utils

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RestrictTo

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class Utils {
    companion object {

        // TODO: Migrate to BundleCompat when available
        @SuppressLint("ClassVerificationFailure") // bundle.getParcelable(key, clazz)
        @Suppress("DEPRECATION") // bundle.getParcelable(key)
        fun <T : Parcelable> getParcelableFromBundle(
            bundle: Bundle,
            key: String,
            clazz: Class<T>
        ): T? {
            val parcelable: T?
            bundle.classLoader = clazz.classLoader
            try {
                if (Build.VERSION.SDK_INT >= 33) {
                    parcelable = bundle.getParcelable(key, clazz)
                } else {
                    parcelable = bundle.getParcelable(key)
                }
            } catch (e: Exception) {
                return null
            }
            return parcelable
        }

        @Suppress("DEPRECATION")
        fun <T : Parcelable> getParcelableArrayListFromBundle(
            bundle: Bundle,
            key: String,
            clazz: Class<T>
        ): List<T> {
            bundle.classLoader = clazz.classLoader
            if (Build.VERSION.SDK_INT >= 33) {
                // TODO: Return framework's getParcelableArrayList when SDK 33 is available
                // return bundle.getParcelableArrayList(key, clazz)
                TODO()
            } else {
                val parcelable: List<T>
                try {
                    parcelable = bundle.getParcelableArrayList(key) ?: emptyList()
                } catch (e: Exception) {
                    return emptyList()
                }
                return parcelable
            }
        }
    }
}