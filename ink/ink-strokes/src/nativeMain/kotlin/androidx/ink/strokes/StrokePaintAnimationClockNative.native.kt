/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.strokes

import androidx.ink.nativeloader.InkInternalOnlyApi
import androidx.ink.nativeloader.cinterop.StrokePaintAnimationClockNative_calculateBasePaintAnimationPhase
import androidx.ink.nativeloader.cinterop.StrokePaintAnimationClockNative_calculateCurrentAnimationPhase
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class, InkInternalOnlyApi::class)
actual internal object StrokePaintAnimationClockNative {
    actual fun calculateBasePaintAnimationPhase(
        clockStateMillis: Long,
        brushFamilyLoopDurationMillis: Long,
    ): Float =
        StrokePaintAnimationClockNative_calculateBasePaintAnimationPhase(
            clockStateMillis,
            brushFamilyLoopDurationMillis,
        )

    actual fun calculateCurrentAnimationPhase(
        clockStateMillis: Long,
        brushFamilyLoopDurationMillis: Long,
        brushPaintLoopDurationMillis: Long,
        strokeBasePaintAnimationPhase: Float,
    ): Float =
        StrokePaintAnimationClockNative_calculateCurrentAnimationPhase(
            clockStateMillis,
            brushFamilyLoopDurationMillis,
            brushPaintLoopDurationMillis,
            strokeBasePaintAnimationPhase,
        )
}
