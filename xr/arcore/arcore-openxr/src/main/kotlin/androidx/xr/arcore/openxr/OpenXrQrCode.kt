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

import androidx.xr.arcore.runtime.QrCode
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose

/**
 * Wraps a native XrTrackableQrCodeANDROID with the [QrCode] interface.
 *
 * @property qrCodeId the ID of the QR
 * @property centerPose the [Pose] of the center of the QR
 * @property extents the extents of the QR
 * @property trackingState the [TrackingState] of the QR
 */
internal class OpenXrQrCode internal constructor(internal val qrCodeId: Long) : QrCode, Updatable {
    override var centerPose: Pose = Pose()
        private set

    override var extents: FloatSize2d = FloatSize2d()
        private set

    override var data: String = ""
        private set

    override var trackingState: TrackingState = TrackingState.PAUSED
        private set

    override fun update(xrTime: Long) {
        val qrCodeState = nativeGetQrCodeState(qrCodeId, xrTime)
        if (qrCodeState == null) {
            trackingState = TrackingState.PAUSED
            return
        }

        trackingState = qrCodeState.trackingState
        centerPose = qrCodeState.centerPose
        extents = qrCodeState.extents
        data = qrCodeState.data
    }

    private external fun nativeGetQrCodeState(qrCodeId: Long, timestampNs: Long): QrCodeState?
}
