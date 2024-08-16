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

package androidx.camera.camera2.internal.compat.params

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.annotation.OptIn
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.impl.Config.OptionPriority

/** Helper for accessing features in [CaptureRequest] in a backwards compatible fashion. */
internal object CaptureRequestParameterCompat {
    /** Sets the [CaptureRequest.CONTROL_SETTINGS_OVERRIDE_ZOOM] option if supported. */
    @OptIn(ExperimentalCamera2Interop::class)
    @JvmStatic
    fun setSettingsOverrideZoom(options: Camera2ImplConfig.Builder, priority: OptionPriority) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            options.setCaptureRequestOptionWithPriority(
                CaptureRequest.CONTROL_SETTINGS_OVERRIDE,
                CameraMetadata.CONTROL_SETTINGS_OVERRIDE_ZOOM,
                priority
            )
        }
    }
}
