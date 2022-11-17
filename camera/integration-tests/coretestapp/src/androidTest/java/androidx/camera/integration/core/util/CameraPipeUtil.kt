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

package androidx.camera.integration.core.util

import android.hardware.camera2.CameraCaptureSession
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.ExtendableBuilder

object CameraPipeUtil {

    @kotlin.OptIn(
        androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop::class
    )
    @OptIn(
        markerClass = [ExperimentalCamera2Interop::class]
    )
    @JvmStatic
    fun <T> setCameraCaptureSessionCallback(
        implName: String,
        builder: ExtendableBuilder<T>,
        captureCallback: CameraCaptureSession.CaptureCallback
    ) {
        if (implName == CameraPipeConfig::class.simpleName) {
            androidx.camera.camera2.pipe.integration.interop.Camera2Interop.Extender(builder)
                .setSessionCaptureCallback(captureCallback)
        } else {
            Camera2Interop.Extender(builder).setSessionCaptureCallback(captureCallback)
        }
    }
}