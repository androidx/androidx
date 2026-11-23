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

package androidx.xr.runtime

import androidx.annotation.RestrictTo

/** The form factor of an [XrDevice]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FormFactor private constructor(private val value: Int) {

    public companion object {
        /**
         * The device is worn on the user's head (for example, an XR headset or a projected device).
         */
        @JvmField public val HEAD_MOUNTED: FormFactor = FormFactor(1)

        /**
         * The device is held in the user's hand, independent of the user's head (for example, an
         * AR-capable smartphone or tablet).
         */
        @JvmField public val HANDHELD: FormFactor = FormFactor(2)
    }

    /** Returns a string representation of [FormFactor] for debugging. */
    override fun toString(): String =
        when (value) {
            1 -> "HEAD_MOUNTED"
            2 -> "HANDHELD"
            else -> "UNKNOWN($value)"
        }
}
