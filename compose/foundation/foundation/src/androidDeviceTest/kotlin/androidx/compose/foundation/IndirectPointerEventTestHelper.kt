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

package androidx.compose.foundation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

// Horizontal external indirect pointer input device
internal val horizontalExternalInputDeviceSize = IntSize(3082, 616)

// Vertical external indirect pointer input device
internal val verticalExternalInputDeviceSize = IntSize(616, 3082)

// Square external indirect pointer input device
internal val squareExternalInputDeviceSize = IntSize(3082, 3082)

internal const val defaultPeriodBetweenEventsMillis = 16L

internal const val TouchPadEnd = 1000f
internal const val TouchPadStart = 0f

internal const val defaultStepCount: Int = 10
internal const val defaultForwardDeltaMovement: Float =
    (TouchPadEnd - TouchPadStart) / defaultStepCount.toFloat()

// Movement along X-Axis values
internal val startOffsetForXAxisMovement = Offset(TouchPadStart, 0f)
internal val endOffsetForXAxisMovement = Offset(TouchPadEnd, 0f)
internal val defaultForwardMovementAlongXAxis = Offset(defaultForwardDeltaMovement, 0f)

// Movement along Y-Axis values
internal val startOffsetForYAxisMovement = Offset(0f, TouchPadStart)
internal val endOffsetForYAxisMovement = Offset(0f, TouchPadEnd)
internal val defaultForwardMovementAlongYAxis = Offset(0f, defaultForwardDeltaMovement)
