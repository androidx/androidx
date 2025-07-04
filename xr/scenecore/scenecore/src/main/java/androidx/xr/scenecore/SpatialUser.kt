/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.xr.runtime.Config.HeadTrackingMode
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.LifecycleManager

/** SpatialUser represents the user in a spatialized Activity. */
public class SpatialUser(
    private val lifecycleManager: LifecycleManager,
    private val runtime: JxrPlatformAdapter,
) {

    private var leftCamera: CameraView? = null
        get() {
            if (field == null) {
                field = CameraView.createLeft(runtime)
            }
            return field
        }

    private var rightCamera: CameraView? = null
        get() {
            if (field == null) {
                field = CameraView.createRight(runtime)
            }
            return field
        }

    internal companion object {
        /** Factory function for creating [SpatialUser] instance. */
        internal fun create(
            lifecycleManager: LifecycleManager,
            runtime: JxrPlatformAdapter,
        ): SpatialUser {
            return SpatialUser(lifecycleManager, runtime)
        }
    }

    /**
     * Returns the Head for the SpatialUser, or null if it is not yet available.
     *
     * @throws [IllegalStateException] if [session.config.headTracking] is set to
     *   [HeadTrackingMode.DISABLED].
     */
    public var head: Head? = null
        get() {
            check(lifecycleManager.config.headTracking != HeadTrackingMode.DISABLED) {
                "Config.HeadTrackingMode is set to Disabled."
            }
            if (field == null) {
                field = Head.create(runtime)
            }
            return field
        }
        private set

    /**
     * A Map of the [CameraView] objects attached to the SpatialUser. The length and elements of the
     * list are dependent on the device type. The list will be empty if the cameras are not yet
     * available.
     *
     * @throws [IllegalStateException] if [session.config.headTracking] is set to
     *   [HeadTrackingMode.DISABLED].
     */
    public var cameraViews: Map<CameraView.CameraType, CameraView> = emptyMap()
        get() {
            check(lifecycleManager.config.headTracking != HeadTrackingMode.DISABLED) {
                "Config.HeadTrackingMode is set to Disabled."
            }
            if (field.isEmpty()) {
                if (leftCamera != null && rightCamera != null) {
                    field =
                        mapOf(
                            CameraView.CameraType.LEFT_EYE to leftCamera!!,
                            CameraView.CameraType.RIGHT_EYE to rightCamera!!,
                        )
                }
            }
            return field
        }
        private set
}
