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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.AspectRatioLegacyApi21Quirk
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.Nexus4AndroidLTargetAspectRatioQuirk

/**
 * Workaround to get corrected target aspect ratio.
 *
 * @see Nexus4AndroidLTargetAspectRatioQuirk
 * @see AspectRatioLegacyApi21Quirk
 */
public class TargetAspectRatio {
    /** Gets corrected target aspect ratio based on device and camera quirks. */
    @Ratio
    public operator fun get(
        cameraMetadata: CameraMetadata,
        streamConfigurationMapCompat: StreamConfigurationMapCompat
    ): Int {
        val cameraQuirks = CameraQuirks(cameraMetadata, streamConfigurationMapCompat)
        val nexus4AndroidLTargetAspectRatioQuirk =
            DeviceQuirks[Nexus4AndroidLTargetAspectRatioQuirk::class.java]
        if (nexus4AndroidLTargetAspectRatioQuirk != null) {
            return nexus4AndroidLTargetAspectRatioQuirk.getCorrectedAspectRatio()
        }

        val aspectRatioLegacyApi21Quirk =
            cameraQuirks.quirks[AspectRatioLegacyApi21Quirk::class.java]
        return aspectRatioLegacyApi21Quirk?.getCorrectedAspectRatio() ?: RATIO_ORIGINAL
    }

    /**  */
    @IntDef(RATIO_4_3, RATIO_16_9, RATIO_MAX_JPEG, RATIO_ORIGINAL)
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public annotation class Ratio

    public companion object {
        /** 4:3 standard aspect ratio. */
        public const val RATIO_4_3: Int = 0

        /** 16:9 standard aspect ratio. */
        public const val RATIO_16_9: Int = 1

        /** The same aspect ratio as the maximum JPEG resolution. */
        public const val RATIO_MAX_JPEG: Int = 2

        /** No correction is needed. */
        public const val RATIO_ORIGINAL: Int = 3
    }
}
