/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.workaround;

import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.StillCaptureFlashStopRepeatingQuirk;

import java.util.List;

/**
 * Workaround to fix device issues such as calling stopRepeating ahead of still
 * capture on some devices when flash is on or auto. See b/172036589.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class StillCaptureFlow {
    private final boolean mShouldStopRepeatingBeforeStillCapture;
    public StillCaptureFlow() {
        final StillCaptureFlashStopRepeatingQuirk quirk = DeviceQuirks.get(
                StillCaptureFlashStopRepeatingQuirk.class);

        mShouldStopRepeatingBeforeStillCapture = (quirk != null);
    }

    /**
     * Returns whether or not it should call stopRepeating ahead of capture request.
     *
     * @param captureRequests captureRequests to be executed
     * @param isStillCapture true if captureRequests contain a still capture request.
     * @return
     */
    public boolean shouldStopRepeatingBeforeCapture(
            @NonNull List<CaptureRequest> captureRequests, boolean isStillCapture) {
        if (!mShouldStopRepeatingBeforeStillCapture || !isStillCapture) {
            return false;
        }

        for (CaptureRequest request : captureRequests) {
            int aeMode = request.get(CaptureRequest.CONTROL_AE_MODE);
            if (aeMode == CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                    || aeMode == CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH) {
                return true;
            }
        }

        return false;
    }
}
