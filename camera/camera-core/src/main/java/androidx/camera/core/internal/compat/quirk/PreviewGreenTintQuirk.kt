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

package androidx.camera.core.internal.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.Quirk
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory

/**
 * QuirkSummary
 * - Bug Id: 361488335
 * - Description: Quirk indicates the preview contains green tint.
 * - Device(s): Motorola E20
 */
@SuppressLint("CameraXQuirksClassDetector")
public object PreviewGreenTintQuirk : Quirk {

    private val isMotoE20
        get() =
            "motorola".equals(Build.BRAND, ignoreCase = true) &&
                "moto e20".equals(Build.MODEL, ignoreCase = true)

    @JvmStatic public fun load(): Boolean = isMotoE20

    /** Returns whether stream sharing should be forced enabled. */
    @JvmStatic
    public fun shouldForceEnableStreamSharing(
        cameraId: String,
        appUseCases: Collection<UseCase>
    ): Boolean {
        if (isMotoE20) {
            return shouldForceEnableStreamSharingForMotoE20(cameraId, appUseCases)
        }
        return false
    }

    private fun shouldForceEnableStreamSharingForMotoE20(
        cameraId: String,
        appUseCases: Collection<UseCase>
    ): Boolean {
        if (cameraId != "0" || appUseCases.size != 2) return false

        val hasPreview = appUseCases.any { it is Preview }
        val hasVideoCapture =
            appUseCases.any {
                it.currentConfig.containsOption(UseCaseConfig.OPTION_CAPTURE_TYPE) &&
                    it.currentConfig.captureType == UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE
            }
        return hasPreview && hasVideoCapture
    }
}
