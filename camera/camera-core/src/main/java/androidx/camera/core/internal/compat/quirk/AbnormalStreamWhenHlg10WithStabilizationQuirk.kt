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

package androidx.camera.core.internal.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.DynamicRange
import androidx.camera.core.UseCase
import androidx.camera.core.impl.Quirk
import androidx.camera.core.impl.stabilization.VideoStabilization
import androidx.camera.core.impl.utils.UseCaseUtil.getVideoStabilization
import androidx.camera.core.impl.utils.UseCaseUtil.isVideoCapture

/**
 * QuirkSummary
 * - Bug Id: b/576802475
 * - Description: Quirk denotes that the video recording contains only solid green frames when an
 *   HLG10 VideoCapture is bound with video stabilization (CONTROL_VIDEO_STABILIZATION_MODE ON or
 *   PREVIEW_STABILIZATION) enabled. With stabilization on, the HAL stabilizes a single buffer and
 *   software-scales it into each additional output stream, but the scaler doesn't support 10-bit
 *   buffers: it logs "scaler_sw.cpp: FunctionProcess: ... Resize is NOT working" for every frame,
 *   and that stream receives all-zero buffers. The session is configured successfully and the
 *   preview looks correct. With StreamSharing, the camera outputs a single stream and no scaling is
 *   needed, so StreamSharing is force enabled for the affected use case combinations and
 *   stabilization stays enabled. VideoCapture bound alone isn't covered, since StreamSharing
 *   requires more than one use case.
 * - Device(s): Samsung devices with the Snapdragon 8 Gen 2 (SM8550) chipset. The failing scaler is
 *   part of Samsung's camera HAL, so devices from other manufacturers with the same chipset are not
 *   included. Verified on Galaxy S23 Ultra (SM-S918U1, SM-S918B), Galaxy Z Flip5 (SM-F731U1) and
 *   Galaxy Z Fold5 (SM-F946U1). Galaxy S24 (SM-S921B, Exynos 2400) and Galaxy S24 Ultra (SM-S928U1,
 *   SM8650) are not affected. Other 10-bit dynamic ranges have not been verified.
 */
@SuppressLint("CameraXQuirksClassDetector")
public object AbnormalStreamWhenHlg10WithStabilizationQuirk : Quirk {

    private const val SOC_MODEL_SNAPDRAGON_8_GEN_2 = "SM8550"

    private val isSamsungSnapdragon8Gen2: Boolean
        get() =
            ("samsung".equals(Build.BRAND, ignoreCase = true) ||
                "samsung".equals(Build.MANUFACTURER, ignoreCase = true)) &&
                Build.VERSION.SDK_INT >= 31 &&
                SOC_MODEL_SNAPDRAGON_8_GEN_2.equals(Build.SOC_MODEL, ignoreCase = true)

    @JvmStatic public fun load(): Boolean = isSamsungSnapdragon8Gen2

    /**
     * Returns whether stream sharing should be force enabled for the given use cases.
     *
     * Returns `true` when a VideoCapture with the HLG10 dynamic range is bound and video
     * stabilization or preview stabilization is enabled.
     */
    public fun shouldForceEnableStreamSharing(appUseCases: Collection<UseCase>): Boolean {
        if (!isSamsungSnapdragon8Gen2) return false

        val hasHlg10VideoCapture = appUseCases.any {
            it.isVideoCapture() && it.currentConfig.dynamicRange.isHlg10()
        }
        return hasHlg10VideoCapture &&
            when (appUseCases.getVideoStabilization()) {
                VideoStabilization.ON,
                VideoStabilization.PREVIEW -> true

                VideoStabilization.OFF,
                VideoStabilization.UNSPECIFIED -> false
            }
    }

    // HDR_UNSPECIFIED_10_BIT is resolved to the camera's recommended 10-bit profile later in the
    // binding flow, which is HLG10 on the Galaxy S23 Ultra.
    private fun DynamicRange.isHlg10(): Boolean =
        this == DynamicRange.HLG_10_BIT || this == DynamicRange.HDR_UNSPECIFIED_10_BIT
}
