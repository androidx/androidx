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

package androidx.camera.extensions.internal

import android.hardware.camera2.CameraExtensionCharacteristics
import android.util.Size
import androidx.annotation.RequiresApi

/** An interface to provide Camera2Extensions related info. */
public interface Camera2ExtensionsInfoProvider {
    /** Retrieves [CameraExtensionCharacteristics] for the specified camera. */
    @RequiresApi(31)
    public fun getExtensionCharacteristics(cameraId: String): CameraExtensionCharacteristics

    /**
     * Returns true if the specified camera supports the specific extension mode. Otherwise, returns
     * false.
     */
    public fun isExtensionAvailable(cameraId: String, mode: Int): Boolean

    /** Retrieves supported output sizes for the specified camera, extension mode and format. */
    public fun getSupportedOutputSizes(cameraId: String, mode: Int, format: Int): List<Size>
}
