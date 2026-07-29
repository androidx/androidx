/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.interop

import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.camera.core.InteropConfigurator
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase

/**
 * Adaptor classes for configuring Camera2 interop options on CameraX builders and components.
 *
 * **Note:** Using Camera2 interop options can override internal CameraX configurations. If an
 * option configured via interop conflicts with options required by CameraX internally, the option
 * from Camera2Interop will override, which may result in unexpected behavior or interfere with
 * CameraX functionality.
 */

// =========================================================================================
// Public Configurator Classes (SAM for Java / Lambda for Kotlin)
// =========================================================================================

/** Configures [UseCaseCamera2Interop] options on [androidx.camera.core.UseCase] builders. */
public class UseCaseCamera2Configurator
internal constructor(private val camera2Configurator: (UseCaseCamera2Interop) -> Unit) :
    InteropConfigurator<UseCase.InteropConfigurable<*>> {

    /**
     * Applies options to the specified [useCaseConfigurable].
     *
     * @param useCaseConfigurable configurable target to apply options to
     */
    override fun configure(useCaseConfigurable: UseCase.InteropConfigurable<*>) {
        camera2Configurator.invoke(
            UseCaseCamera2InteropImpl(useCaseConfigurable.interopMutableConfig)
        )
    }
}

/**
 * Configures [ImageCaptureCamera2Interop] options on an
 * [androidx.camera.core.ImageCapture.Builder].
 */
public class ImageCaptureCamera2Configurator
internal constructor(private val camera2Configurator: (ImageCaptureCamera2Interop) -> Unit) :
    InteropConfigurator<ImageCapture.Builder> {

    /**
     * Applies options to the specified [imageCaptureBuilder].
     *
     * @param imageCaptureBuilder builder to apply options to
     */
    override fun configure(imageCaptureBuilder: ImageCapture.Builder) {
        camera2Configurator.invoke(
            ImageCaptureCamera2InteropImpl(imageCaptureBuilder.interopMutableConfig)
        )
    }
}

/**
 * Configures [SessionConfigCamera2Interop] options on a
 * [androidx.camera.core.SessionConfig.Builder].
 */
public class SessionConfigCamera2Configurator
internal constructor(private val camera2Configurator: (SessionConfigCamera2Interop) -> Unit) :
    InteropConfigurator<SessionConfig.Builder> {

    /**
     * Applies options to the specified [sessionConfigBuilder].
     *
     * @param sessionConfigBuilder builder to apply options to
     */
    override fun configure(sessionConfigBuilder: SessionConfig.Builder) {
        camera2Configurator.invoke(
            SessionConfigCamera2InteropImpl(sessionConfigBuilder.interopMutableConfig)
        )
    }
}

/** Configures [CameraControlCamera2Interop] options on a [androidx.camera.core.CameraControl]. */
public class CameraControlCamera2Configurator
internal constructor(private val camera2Configurator: (CameraControlCamera2Interop) -> Unit) :
    InteropConfigurator<CameraControl> {

    /**
     * Applies options to the specified [cameraControl].
     *
     * @param cameraControl camera control to apply options to
     */
    override fun configure(cameraControl: CameraControl) {
        camera2Configurator.invoke(
            CameraControlCamera2InteropImpl(cameraControl.interopMutableConfig)
        )
    }
}
