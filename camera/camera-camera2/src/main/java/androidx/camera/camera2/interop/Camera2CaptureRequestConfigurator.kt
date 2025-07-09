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

package androidx.camera.camera2.interop

import android.hardware.camera2.CaptureRequest
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.camera.core.CameraXConfig
import androidx.camera.core.impl.Config

internal val OPTION_CAPTURE_REQUEST_CONFIGURATOR:
    Config.Option<Camera2CaptureRequestConfigurator?> =
    Config.Option.create<Camera2CaptureRequestConfigurator?>(
        "camerax.core.appConfig.captureRequestConfigurator",
        Camera2CaptureRequestConfigurator::class.java,
    )

/** A configurator that handles configurations associated with a [CaptureRequest]. */
@RestrictTo(Scope.LIBRARY_GROUP)
public fun interface Camera2CaptureRequestConfigurator {
    /**
     * Configure with a [CaptureRequest] that contains the parameters that are originally configured
     * to Camera2.
     *
     * @param captureRequest The [CaptureRequest] to configure with.
     */
    public fun configureWith(captureRequest: CaptureRequest)
}

/** Gets the configurator for configuring the camera in customized ways. */
@RestrictTo(Scope.LIBRARY_GROUP)
public fun CameraXConfig.getCamera2CaptureRequestConfigurator():
    Camera2CaptureRequestConfigurator? {
    return config.retrieveOption(OPTION_CAPTURE_REQUEST_CONFIGURATOR, null)
}

/**
 * Sets a `Camera2CaptureRequestConfigurator` to a [CameraXConfig.Builder].
 *
 * @param captureRequestConfigurator The [Camera2CaptureRequestConfigurator] to set.
 * @return The current `Builder`.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public fun CameraXConfig.Builder.setCamera2CaptureRequestConfigurator(
    captureRequestConfigurator: Camera2CaptureRequestConfigurator
): CameraXConfig.Builder {
    mutableConfig.insertOption(OPTION_CAPTURE_REQUEST_CONFIGURATOR, captureRequestConfigurator)
    return this
}
