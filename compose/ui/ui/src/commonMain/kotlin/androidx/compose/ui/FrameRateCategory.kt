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

package androidx.compose.ui

import kotlin.jvm.JvmInline

/**
 * A type-safe representation of a frame rate category for a display or application.
 * - Default: Default value. This value can also be set to return to default behavior, indicating
 *   that this component has no data for the frame rate.
 * - Normal: Indicates a middle frame rate suitable for animations that do not require higher frame
 *   rates. This is normally 60 Hz or close to it.
 * - High: Indicates a frame rate suitable for animations that require a high frame rate.
 */
@JvmInline
public value class FrameRateCategory private constructor(internal val value: Float) {
    public companion object {
        public val Default: FrameRateCategory
            get() = FrameRateCategory(Float.NaN)

        public val Normal: FrameRateCategory
            get() = FrameRateCategory(-3f)

        public val High: FrameRateCategory
            get() = FrameRateCategory(-4f)
    }

    public override fun toString(): String {
        val text =
            when (value) {
                -3f -> "Normal"
                -4f -> "High"
                else -> "Default"
            }
        return text
    }
}
