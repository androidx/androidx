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

package androidx.xr.arcore.openxr

import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose

/**
 * State of a [OpenXrQrCode] instance's mutable fields.
 *
 * @property trackingState the [androidx.xr.arcore.runtime.TrackingState] value describing if the QR
 *   code is being updated
 * @property centerPose the pose of the center of the detected QR code. The pose's transformed +Y
 *   axis will be point normal out of the QR code, with the +X and +Z axes orienting the extents of
 *   the bounding rectangle
 * @property extents the dimensions of the detected QR code
 * @property data the content of the detected QR code
 */
internal data class QrCodeState(
    @JvmField val trackingState: TrackingState = TrackingState.PAUSED,
    @JvmField val centerPose: Pose = Pose(),
    @JvmField val extents: FloatSize2d = FloatSize2d(),
    @JvmField val data: String = "",
)
