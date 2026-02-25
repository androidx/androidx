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

package androidx.xr.arcore

import androidx.xr.runtime.math.Pose

/** An object that ARCore for Jetpack XR can track and that an [Anchor] can be attached to. */
public interface Anchorable<out T> : Trackable<T> {
    /**
     * Creates an [Anchor] that is attached to this trackable, using the given initial [pose] in the
     * world coordinate space.
     */
    public fun createAnchor(pose: Pose): AnchorCreateResult
}
