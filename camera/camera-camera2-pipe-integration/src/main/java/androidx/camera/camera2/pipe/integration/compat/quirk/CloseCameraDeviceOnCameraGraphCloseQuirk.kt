/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.impl.Quirk

/**
 * Quirk needed on devices where not closing the camera device before creating a new capture session
 * can lead to undesirable behaviors, such as native camera HAL crashes. On Exynos7870 platforms for
 * example, once their 3A pipeline times out, recreating a capture session has a high chance of
 * triggering use-after-free crashes.
 *
 * QuirkSummary
 * - Bug Id: 282871038
 * - Description: Instructs CameraPipe to close the camera device before creating a new capture
 *   session to avoid undesirable behaviors
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class CloseCameraDeviceOnCameraGraphCloseQuirk : Quirk {
    public companion object {
        @JvmStatic
        public fun isEnabled(): Boolean {
            return Build.HARDWARE == "samsungexynos7870"
        }
    }
}
