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

package androidx.camera.camera2.pipe.compat

import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Base class for CameraCaptureSession.StateCallback()
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
interface OnSessionFinalized {
    /**
     * Artificial event indicating the session is no longer in use and may be called several
     * times. onClosed() and [onConfigureFailed() methods should call this method directly.
     * This method should also be called whenever the underlying camera devices is closed, and
     * whenever a subsequent capture session is configured on the same camera device.
     *
     * See b/249258992 for more details.
     */
    fun onSessionFinalized()
}