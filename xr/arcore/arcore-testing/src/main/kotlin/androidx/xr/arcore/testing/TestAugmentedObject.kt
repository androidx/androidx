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
import androidx.xr.arcore.testing.internal.FakeRuntimeAugmentedObject
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose

/**
 * A representation of a real-world object of an [AugmentedObjectCategory] in the test environment.
 *
 * @property category the [AugmentedObjectCategory] that describes the object
 * @property centerPose [Pose] at the center of the augmented object
 * @property extents the [FloatSize3d] extents used to determine the size of the object
 */
public class TestAugmentedObject(category: AugmentedObjectCategory) : TestTrackable() {

    internal val fakeRuntimeTrackable = FakeRuntimeAugmentedObject(category = category)

    override var isVisible: Boolean = true
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.trackingState =
                    if (value) {
                        TrackingState.TRACKING
                    } else {
                        TrackingState.PAUSED
                    }
            }
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }

    public var category: AugmentedObjectCategory = category
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.category = value
            }
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }

    public var centerPose: Pose = Pose()
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.centerPose = value
            }
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }

    public var extents: FloatSize3d = FloatSize3d()
        set(value) {
            field = value
            if (isConfigured()) {
                fakeRuntimeTrackable.extents = value
            }
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }

    internal fun isConfigured(): Boolean =
        if (isAddedToTestRule)
            arCoreTestRule.runtime.config.augmentedObjectCategories.contains(category)
        else false
}
