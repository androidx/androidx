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

import android.hardware.camera2.CameraExtensionCharacteristics
import androidx.camera.camera2.pipe.Metadata

public object CameraPipeKeys {
    /** Keys for sessionParameters when creating Extension sessions. */
    public val camera2ExtensionMode: Metadata.Key<Int> =
        Metadata.Key.create<Int>("androidx.camera.camera2.pipe.extensionMode")

    /** Key for configuring the tag for a Camera2 CaptureRequest. */
    public val camera2CaptureRequestTag: Metadata.Key<Any> =
        Metadata.Key.create<Any>("androidx.camera.camera2.pipe.captureRequestTag")

    /**
     * Key for defaultParameters and requiredParameters that allows the users to ignore the required
     * 3A parameters stipulated by the 3A controller in CameraPipe.
     */
    public val ignore3ARequiredParameters: Metadata.Key<Boolean> =
        Metadata.Key.create<Boolean>("androidx.camera.camera2.pipe.ignore3ARequiredParameters")

    /**
     * [CAMERA2_EXTENSION_MODE_AUTOMATIC]: Automatic selection of particular extensions such as HDR
     * or NIGHT depending on the current lighting and environment conditions. See
     * [CameraExtensionCharacteristics.EXTENSION_AUTOMATIC]
     */
    public const val CAMERA2_EXTENSION_MODE_AUTOMATIC: Int = 0

    /**
     * [CAMERA2_EXTENSION_MODE_FACE_RETOUCH]: Smooth skin and apply other cosmetic effects to faces.
     * See [CameraExtensionCharacteristics.EXTENSION_FACE_RETOUCH]
     */
    public const val CAMERA2_EXTENSION_MODE_FACE_RETOUCH: Int = 1

    /**
     * [CAMERA2_EXTENSION_MODE_BOKEH]: Blur certain regions of the final image thereby "enhancing"
     * focus for all remaining non-blurred parts. See
     * [CameraExtensionCharacteristics.EXTENSION_BOKEH]
     */
    public const val CAMERA2_EXTENSION_MODE_BOKEH: Int = 2

    /**
     * [CAMERA2_EXTENSION_MODE_HDR]: Enhance the dynamic range of the final image. See
     * [CameraExtensionCharacteristics.EXTENSION_HDR]
     */
    public const val CAMERA2_EXTENSION_MODE_HDR: Int = 3

    /**
     * [CAMERA2_EXTENSION_MODE_NIGHT]: Suppress noise and improve the overall image quality under
     * low light conditions. See [CameraExtensionCharacteristics.EXTENSION_NIGHT]
     */
    public const val CAMERA2_EXTENSION_MODE_NIGHT: Int = 4
}
