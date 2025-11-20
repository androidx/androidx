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

package androidx.xr.runtime

/** Contextual label describing the type of detected object. */
public class AugmentedObjectCategory private constructor(private val value: Int) {
    public companion object {
        /** Category value indicating the tracked object is of unknown type. */
        @JvmField public val UNKNOWN: AugmentedObjectCategory = AugmentedObjectCategory(0)
        /** Category value indicating the tracked object is believed to be a keyboard. */
        @JvmField public val KEYBOARD: AugmentedObjectCategory = AugmentedObjectCategory(1)
        /** Category value indicating the tracked object is believed to be a mouse. */
        @JvmField public val MOUSE: AugmentedObjectCategory = AugmentedObjectCategory(2)
        /** Category value indicating the tracked object is believed to be a laptop. */
        @JvmField public val LAPTOP: AugmentedObjectCategory = AugmentedObjectCategory(3)

        @JvmStatic
        /** Returns an array of all available [AugmentedObjectCategory] values. */
        public fun all(): List<AugmentedObjectCategory> = listOf(KEYBOARD, MOUSE, LAPTOP)
    }

    /** Returns a human readable string representation of the label. */
    public override fun toString(): String {
        return when (value) {
            1 -> "Keyboard"
            2 -> "Mouse"
            3 -> "Laptop"
            else -> "Unknown"
        }
    }
}
