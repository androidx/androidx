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

package androidx.camera.core.impl.utils

import androidx.camera.core.Logger
import androidx.camera.core.UseCase
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory

public object UseCaseUtil {
    private const val TAG = "UseCaseUtil"

    /** Checks if the receiver [UseCase] list contains a video capture use case instance. */
    @JvmStatic
    public fun Collection<UseCase?>.containsVideoCapture(): Boolean {
        forEach { useCase ->
            if (useCase?.isVideoCapture() == true) {
                return true
            }
        }
        return false
    }

    /** Checks if the receiver [UseCase] is a video capture use case instance. */
    @JvmStatic
    public fun UseCase.isVideoCapture(): Boolean {
        if (currentConfig.containsOption(UseCaseConfig.OPTION_CAPTURE_TYPE)) {
            return currentConfig.captureType == UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE
        } else {
            Logger.e(TAG, "$this UseCase does not have capture type.")
        }
        return false
    }
}
