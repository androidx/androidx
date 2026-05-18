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

import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.arcore.testing.internal.FakePerceptionRuntime
import androidx.xr.arcore.testing.internal.FakeRuntimeQrCode
import androidx.xr.runtime.QrCodeTrackingMode
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose

/**
 * Represents a QR code in the test environment.
 *
 * @property data the data of the QR code
 * @property centerPose [Pose] at the center of the augmented image
 * @property extents the [FloatSize2d] extents used to determine the size of the image
 */
public class TestQrCode(data: String) : TestTrackable() {

    override val fakeRuntimeTrackable = FakeRuntimeQrCode(data = data)

    override var isVisible: Boolean = true
        set(value) {
            field = value
            if (canBeTracked) {
                fakeRuntimeTrackable.trackingState =
                    if (value) {
                        TrackingState.TRACKING
                    } else {
                        TrackingState.PAUSED
                    }
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var data: String = data
        set(value) {
            field = value
            if (canBeTracked) {
                fakeRuntimeTrackable.data = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var centerPose: Pose = Pose()
        set(value) {
            field = value
            if (canBeTracked) {
                fakeRuntimeTrackable.centerPose = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    public var extents: FloatSize2d = FloatSize2d()
        set(value) {
            field = value
            if (canBeTracked) {
                fakeRuntimeTrackable.extents = value
            }
            FakePerceptionRuntime.allowOneMoreCallToUpdate()
        }

    override fun isTrackableConfigured(): Boolean =
        if (isAddedToTestRule)
            arCoreTestRule.runtime.config.qrCodeTracking != QrCodeTrackingMode.DISABLED
        else false
}
