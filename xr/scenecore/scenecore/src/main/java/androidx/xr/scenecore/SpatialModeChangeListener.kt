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

package androidx.xr.scenecore

import androidx.xr.runtime.math.Pose

/**
 * The SpatialModeChangeListener is used to handle scenegraph updates when the spatial mode for the
 * scene changes.
 */
public fun interface SpatialModeChangeListener {
    /**
     * Callback that provides a new recommended pose and scale for placing content.
     *
     * This is called whenever the activity encounters a spatial mode change or re-center. The most
     * common app behavior is to set the pose and scale of an entity relative to the ActivitySpace
     * origin. The default behavior is equivalent to:
     * ```
     * Scene.keyEntity.setPose(recommendedPose, relativeTo = Space.ACTIVITY);
     * Scene.keyEntity.setScale(recommendedScale, relativeTo = Space.ACTIVITY);
     * ```
     *
     * @param recommendedPose the recommended pose for the keyEntity. The pose is relative to
     *   [ActivitySpace] origin, not relative to the keyEntity's parent.
     * @param recommendedScale the recommended scale for the keyEntity. The scale value is the
     *   accumulated scale for this entity i.e. accumulated scale in [ActivitySpace], not relative
     *   to the keyEntity's parent.
     */
    public fun onSpatialModeChanged(recommendedPose: Pose, recommendedScale: Float)
}
