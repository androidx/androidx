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

import androidx.xr.arcore.testing.internal.FakeLifecycleManager
import androidx.xr.arcore.testing.internal.FakeRuntimeArDevice
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PreviewSpatialApi
import androidx.xr.runtime.math.Pose

/**
 * An object which represents the user's device in the simulated environment of an ARCore unit test.
 *
 * @property pose the current pose of the device
 * @property isCameraTracking whether the AR Device is currently tracking the environment
 */
public class TestArDevice internal constructor(private val arCoreTestRule: ArCoreTestRule) {

    private val fakeRuntimeArDevice: FakeRuntimeArDevice by lazy {
        arCoreTestRule.runtime.perceptionManager.arDevice as FakeRuntimeArDevice
    }

    private val isConfigured6Dof: Boolean
        get() =
            arCoreTestRule.runtime.config.deviceTracking == DeviceTrackingMode.SPATIAL_LAST_KNOWN

    @OptIn(PreviewSpatialApi::class)
    private val isConfigured3Dof: Boolean
        get() =
            arCoreTestRule.runtime.config.deviceTracking == DeviceTrackingMode.INERTIAL_LAST_KNOWN

    public var pose: Pose = Pose()
        set(value) {
            field = value
            fakeRuntimeArDevice.devicePose =
                if (isConfigured6Dof) {
                    value
                } else if (isConfigured3Dof) {
                    Pose(fakeRuntimeArDevice.devicePose.translation, value.rotation)
                } else {
                    fakeRuntimeArDevice.devicePose
                }
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }

    public var isCameraTracking: Boolean = true
        set(value) {
            field = value
            arCoreTestRule.runtime.perceptionManager.isCameraTracking = value
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }
}
