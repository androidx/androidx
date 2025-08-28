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

package androidx.compose.ui.test

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.ExperimentalVelocityTrackerApi
import androidx.compose.ui.input.pointer.util.VelocityTrackerStrategyUseImpulse

@OptIn(markerClass = [ExperimentalVelocityTrackerApi::class])
internal actual fun VelocityPathFinder(
    startPosition: Offset,
    endPosition: Offset,
    endVelocity: Float,
    durationMillis: Long,
): VelocityPathFinder {
    return if (VelocityTrackerStrategyUseImpulse) {
        ImpulseVelocityPathFinder(startPosition, endPosition, endVelocity, durationMillis)
    } else {
        LsqVelocityPathFinder(startPosition, endPosition, endVelocity, durationMillis)
    }
}
