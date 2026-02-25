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

import androidx.xr.arcore.runtime.Eye
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose

/** Fake implementation of [Eye] for testing purposes. */
public class FakeRuntimeEye : Eye {
    override var isOpen: Boolean = true

    override var pose: Pose = Pose()

    override var trackingState: TrackingState = TrackingState.TRACKING
}
