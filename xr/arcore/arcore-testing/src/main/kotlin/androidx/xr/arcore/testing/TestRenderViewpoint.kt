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

import androidx.xr.arcore.RenderViewpoint
import androidx.xr.arcore.testing.internal.FakeLifecycleManager
import androidx.xr.arcore.testing.internal.FakeRuntimeRenderViewpoint
import androidx.xr.runtime.math.FieldOfView
import androidx.xr.runtime.math.Pose

/**
 * Represents a [RenderViewpoint] on the device.
 *
 * @property pose the [Pose]
 * @property fieldOfView the [FieldOfView]
 */
public class TestRenderViewpoint
internal constructor(
    private val arCoreTestRule: ArCoreTestRule,
    private val fakeRuntimeRenderViewpoint: FakeRuntimeRenderViewpoint,
) {
    public var pose: Pose = Pose()
        set(value) {
            field = value
            fakeRuntimeRenderViewpoint.pose = value
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }

    @Suppress("DEPRECATION")
    public var fieldOfView: FieldOfView = FieldOfView(0f, 0f, 0f, 0f)
        set(value) {
            field = value
            fakeRuntimeRenderViewpoint.fieldOfView =
                androidx.xr.runtime.FieldOfView(
                    value.angleLeft,
                    value.angleRight,
                    value.angleUp,
                    value.angleDown,
                )
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }
}
