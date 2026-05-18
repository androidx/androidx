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

package androidx.xr.arcore.testing

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor as RuntimeAnchor
import androidx.xr.arcore.runtime.QrCode as RuntimeQrCode
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose

/**
 * Test-only implementation of [RuntimeQrCode]
 *
 * The properties of the [FakeRuntimeQrCode] can be set manually in order to simulate a runtime qr
 * code in the environment.
 *
 * For example, for a [FakeRuntimeQrCode] with [TrackingState.PAUSED]:
 * ```
 * val qrCode = FakeRuntimeQrCode(trackingState = TrackingState.PAUSED)
 * ```
 *
 * And to modify the properties during the test:
 * ```
 * qrCode.apply {
 *     trackingState = TrackingState.TRACKING
 *     centerPose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 0f, 0f, 1f))
 * }
 * ```
 */
@SuppressWarnings("HiddenSuperclass")
@Deprecated(
    "arcore-testing fakes have been moved internal and should no longer be used by unit tests."
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FakeRuntimeQrCode(
    override var trackingState: TrackingState = TrackingState.TRACKING,
    override var centerPose: Pose = Pose(),
    override var extents: FloatSize2d = FloatSize2d(),
    override var data: String = "",
    /** The anchors that are attached to this qr code. */
    public val anchors: MutableCollection<RuntimeAnchor> = mutableListOf(),
) : RuntimeQrCode {}
