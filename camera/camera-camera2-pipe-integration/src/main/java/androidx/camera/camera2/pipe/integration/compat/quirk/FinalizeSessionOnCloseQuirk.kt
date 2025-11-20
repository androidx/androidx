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
import androidx.camera.camera2.pipe.CameraGraph.Flags.FinalizeSessionOnCloseBehavior
import androidx.camera.core.impl.Quirk
import java.util.Locale

/**
 * A quirk that finalizes [androidx.camera.camera2.pipe.compat.CaptureSessionState] when the
 * CameraGraph is stopped or closed.
 *
 * QuirkSummary
 * - Bug Id: 277310425
 * - Description: When CameraX sets up its video recorder, it waits for the previous Surfaces to be
 *   released before setting them in the new CameraGraph. However, CameraPipe would also wait for
 *   the Surfaces to be set before it creates a new capture session and finalize the previous
 *   session, and therefore not releasing the Surfaces. This essentially creates a deadlock, and
 *   this quirk would enable a behavior in CameraPipe such that the current session gets finalized
 *   either immediately or on a timeout after the CameraGraph is stopped or closed.
 * - Device(s): All devices.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class FinalizeSessionOnCloseQuirk : Quirk {
    public companion object {
        public fun isEnabled(): Boolean = true

        public fun getBehavior(): FinalizeSessionOnCloseBehavior =
            if (CameraQuirks.isImmediateSurfaceReleaseAllowed()) {
                // Finalize immediately for devices that allow immediate Surface reuse.
                FinalizeSessionOnCloseBehavior.IMMEDIATE
            } else if (Build.MODEL.lowercase(Locale.getDefault()).startsWith("cph")) {
                // During shutdown, the test app often experiences ANR which prevents us from
                // eventually closing the camera device and releasing the Surfaces. As a workaround,
                // we leverage CloseCaptureSessionOnDisconnectQuirk to close the capture session,
                // before we use this workaround to finalize the capture session, and thereby
                // releasing the Surfaces.
                FinalizeSessionOnCloseBehavior.IMMEDIATE
            } else {
                FinalizeSessionOnCloseBehavior.OFF
            }
    }
}
