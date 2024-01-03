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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraExtensionCharacteristics
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.Metadata

object CameraPipeKeys {

    /**
     * Keys for sessionParameters when creating Extension sessions.
     */
    val camera2ExtensionMode = Metadata.Key.create<Int>(
        "androidx.camera.camera2.pipe.ExtensionMode"
    )

    /**
     * [CAMERA2_EXTENSION_MODE_AUTOMATIC]: Automatic selection of particular extensions such
     * as HDR or NIGHT depending on the current lighting and environment conditions. See
     * [CameraExtensionCharacteristics.EXTENSION_AUTOMATIC]
     */
    const val CAMERA2_EXTENSION_MODE_AUTOMATIC = 0

    /**
     * [CAMERA2_EXTENSION_MODE_FACE_RETOUCH]: Smooth skin and apply other cosmetic effects to
     * faces. See [CameraExtensionCharacteristics.EXTENSION_FACE_RETOUCH]
     */
    const val CAMERA2_EXTENSION_MODE_FACE_RETOUCH = 1

    /**
     * [CAMERA2_EXTENSION_MODE_BOKEH]: Blur certain regions of the final image thereby
     * "enhancing" focus for all remaining non-blurred parts. See
     * [CameraExtensionCharacteristics.EXTENSION_BOKEH]
     */
    const val CAMERA2_EXTENSION_MODE_BOKEH = 2

    /**
     * [CAMERA2_EXTENSION_MODE_HDR]: Enhance the dynamic range of the final image.
     * See [CameraExtensionCharacteristics.EXTENSION_HDR]
     */
    const val CAMERA2_EXTENSION_MODE_HDR = 3

    /**
     * [CAMERA2_EXTENSION_MODE_NIGHT]: Suppress noise and improve the overall image
     * quality under low light conditions. See [CameraExtensionCharacteristics.EXTENSION_NIGHT]
     */
    const val CAMERA2_EXTENSION_MODE_NIGHT = 4
}
