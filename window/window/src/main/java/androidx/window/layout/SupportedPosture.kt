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

package androidx.window.layout

/** A class to represent a posture that the device supports. */
class SupportedPosture internal constructor(private val rawValue: Int) {

    override fun toString(): String {
        return when (this) {
            TABLETOP -> "TABLETOP"
            else -> "UNKNOWN"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other::class != SupportedPosture::class) return false

        other as SupportedPosture

        return rawValue == other.rawValue
    }

    override fun hashCode(): Int {
        return rawValue
    }

    companion object {
        /** The posture where there is a single fold in the half-opened state. */
        @JvmField val TABLETOP: SupportedPosture = SupportedPosture(0)
    }
}
