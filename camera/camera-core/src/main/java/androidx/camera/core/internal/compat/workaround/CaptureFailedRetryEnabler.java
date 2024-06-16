/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.internal.compat.workaround;

import androidx.camera.core.ImageCapture;
import androidx.camera.core.internal.compat.quirk.CaptureFailedRetryQuirk;
import androidx.camera.core.internal.compat.quirk.DeviceQuirks;

/**
 * Workaround that allows the {@link ImageCapture} to retry the same capture request once when
 * encountering capture failures.
 *
 * @see CaptureFailedRetryQuirk
 */
public class CaptureFailedRetryEnabler {

    private final CaptureFailedRetryQuirk mCaptureFailedRetryQuirk = DeviceQuirks.get(
            CaptureFailedRetryQuirk.class);

    /**
     * Returns the count which the image capture request can be retried.
     */
    public int getRetryCount() {
        return mCaptureFailedRetryQuirk == null ? 0 : mCaptureFailedRetryQuirk.getRetryCount();
    }
}
