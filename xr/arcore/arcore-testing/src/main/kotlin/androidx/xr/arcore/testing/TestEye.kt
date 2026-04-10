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
import androidx.xr.arcore.testing.internal.FakeLifecycleManager
import androidx.xr.arcore.testing.internal.FakeRuntimeEye
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.math.Pose

/**
 * Represents one of the user's eyes in a testing environment.
 *
 * @property isOpen whether the eye is open
 * @property pose the [Pose] of the eye
 */
public class TestEye
internal constructor(
    private val arCoreTestRule: ArCoreTestRule,
    private val fakeRuntimeEye: FakeRuntimeEye,
) {
    private val isConfigured: Boolean
        get() = arCoreTestRule.runtime.config.eyeTracking != EyeTrackingMode.DISABLED

    /** Flag indicating the eye is open and thus visible to the runtime. */
    public var isOpen: Boolean = true
        set(value) {
            field = value
            if (isConfigured) {
                fakeRuntimeEye.isOpen = value
                fakeRuntimeEye.trackingState =
                    if (value) {
                        TrackingState.TRACKING
                    } else {
                        TrackingState.PAUSED
                    }
            }
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }

    /** Pose of the eye within the testing environment. */
    public var pose: Pose = Pose()
        set(value) {
            field = value
            if (isConfigured && isOpen) {
                if (isOpen) {
                    fakeRuntimeEye.pose = value
                }
            }
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }
}
