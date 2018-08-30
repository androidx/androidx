/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.events

import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.ui.pointer.PointerDeviceKind

// /// The pointer has moved with respect to the device while the pointer is not
// /// in contact with the device.
// ///
// /// See also:
// ///
// ///  * [PointerMoveEvent], which reports movement while the pointer is in
// ///    contact with the device.
class PointerHoverEvent(
    timeStamp: Duration = Duration.zero,
    kind: PointerDeviceKind = PointerDeviceKind.touch,
    device: Int = 0,
    position: Offset = Offset.zero,
    delta: Offset = Offset.zero,
    buttons: Int = 0,
    obscured: Boolean = false,
    pressureMin: Double = 1.0,
    pressureMax: Double = 1.0,
    distance: Double = 0.0,
    distanceMax: Double = 0.0,
    radiusMajor: Double = 0.0,
    radiusMinor: Double = 0.0,
    radiusMin: Double = 0.0,
    radiusMax: Double = 0.0,
    orientation: Double = 0.0,
    tilt: Double = 0.0,
    synthesized: Boolean = false
) : PointerEvent(
    timeStamp = timeStamp,
    kind = kind,
    device = device,
    position = position,
    delta = delta,
    buttons = buttons,
    obscured = obscured,
    pressureMin = pressureMin,
    pressureMax = pressureMax,
    distance = distance,
    distanceMax = distanceMax,
    radiusMajor = radiusMajor,
    radiusMinor = radiusMinor,
    radiusMin = radiusMin,
    radiusMax = radiusMax,
    orientation = orientation,
    tilt = tilt,
    synthesized = synthesized
)
