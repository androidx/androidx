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

package androidx.xr.arcore.testing

import androidx.xr.arcore.runtime.Anchor as RuntimeAnchor
import androidx.xr.arcore.runtime.AugmentedObject as RuntimeObject
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose

/**
 * Fake implementation of [AugmentedObject][RuntimeObject] for testing purposes.
 *
 * @property anchors a [MutableCollection] of [Anchors][RuntimeAnchor] attached to this object
 */
@SuppressWarnings("HiddenSuperclass")
public class FakeRuntimeAugmentedObject(
    override var centerPose: Pose = Pose(),
    override var extents: FloatSize3d = FloatSize3d(),
    override var category: AugmentedObjectCategory = AugmentedObjectCategory.KEYBOARD,
    override var trackingState: TrackingState = TrackingState.TRACKING,
    public val anchors: MutableCollection<RuntimeAnchor> = mutableListOf(),
) : RuntimeObject {}
