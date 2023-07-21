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
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph.Flags.FinalizeSessionOnCloseBehavior
import androidx.camera.core.impl.Quirk

/**
 * A quirk that finalizes [androidx.camera.camera2.pipe.compat.CaptureSessionState] when the
 * CameraGraph is stopped or closed.
 *
 * QuirkSummary
 * - Bug Id:      277310425
 * - Description: When CameraX sets up its video recorder, it waits for the previous Surfaces to be
 *                released before setting them in the new CameraGraph. However, CameraPipe would
 *                also wait for the Surfaces to be set before it creates a new capture session and
 *                finalize the previous session, and therefore not releasing the Surfaces. This
 *                essentially creates a deadlock, and this quirk would enable a behavior in
 *                CameraPipe such that the current session gets finalized either immediately or on a
 *                timeout after the CameraGraph is stopped or closed.
 * - Device(s):   All devices.
 */
@SuppressLint("CameraXQuirksClassDetector")
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class FinalizeSessionOnCloseQuirk : Quirk {
    companion object {
        fun isEnabled() = true

        fun getBehavior() =
            if (CameraQuirks.isImmediateSurfaceReleaseAllowed()) {
                // Finalize immediately for devices that allow immediate Surface reuse.
                FinalizeSessionOnCloseBehavior.IMMEDIATE
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                // When CloseCaptureSessionOnVideoQuirk is enabled, we close the capture session
                // in anticipation that the onClosed() callback would finalize the session. However,
                // on API levels < M, it could be possible that onClosed() isn't invoked if a new
                // capture session (or CameraGraph) is created too soon (read b/144817309 or
                // CaptureSessionOnClosedNotCalledQuirk for more context). Therefore, we're enabling
                // this quirk (on a timeout) for API levels < M, too.
                FinalizeSessionOnCloseBehavior.TIMEOUT
            } else {
                FinalizeSessionOnCloseBehavior.OFF
            }
    }
}