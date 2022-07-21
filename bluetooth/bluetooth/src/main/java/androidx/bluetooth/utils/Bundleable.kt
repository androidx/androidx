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

package androidx.bluetooth.utils

import android.os.Bundle

/**
 * @hide
 */
interface Bundleable {
    /** Returns a [Bundle] representing the information stored in this object.  */
    fun toBundle(): Bundle

    /** Interface for the static `CREATOR` field of [Bundleable] classes.  */
    interface Creator<T : Bundleable> {
        /**
         * Restores a [Bundleable] instance from a [Bundle] produced by [ ][Bundleable.toBundle].
         *
         *
         * It guarantees the compatibility of [Bundle] representations produced by different
         * versions of [Bundleable.toBundle] by providing best default values for missing
         * fields. It throws an exception if any essential fields are missing.
         */
        fun fromBundle(bundle: Bundle): T
    }
}