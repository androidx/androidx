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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CaptureRequest
import android.util.Rational
import android.util.Size
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.PreviewPixelHDRnetQuirk
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.core.impl.SessionConfig

private val ASPECT_RATIO_16_9 = Rational(16, 9)

/**
 * Turns on WYSIWYG viewfinder on Pixel devices
 *
 * @see PreviewPixelHDRnetQuirk
 */
public fun SessionConfig.Builder.setupHDRnet(resolution: Size) {
    DeviceQuirks[PreviewPixelHDRnetQuirk::class.java] ?: return

    if (isAspectRatioMatch(resolution, ASPECT_RATIO_16_9)) return

    val camera2ConfigBuilder =
        Camera2ImplConfig.Builder().apply {
            setCaptureRequestOption<Int>(
                CaptureRequest.TONEMAP_MODE,
                CaptureRequest.TONEMAP_MODE_HIGH_QUALITY
            )
        }

    addImplementationOptions(camera2ConfigBuilder.build())
}

private fun isAspectRatioMatch(resolution: Size, aspectRatio: Rational): Boolean {
    return aspectRatio == Rational(resolution.width, resolution.height)
}
