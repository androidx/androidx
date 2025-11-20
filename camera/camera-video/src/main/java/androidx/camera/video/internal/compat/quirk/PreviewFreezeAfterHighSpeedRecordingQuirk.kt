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

package androidx.camera.video.internal.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: b/405846107
 * - Description: Quirk indicates camera preview freezes after a high-speed/slow-motion recording is
 *   stopped. This issue happens on Pixel 7, 9, 9 Pro Fold and potentially other Pixel devices. More
 *   specifically, it happens after MediaCodec#flush() is called. The workaround is instead of
 *   calling MediaCodec#flush(), use MediaCodec#stop(). The flow is still the same as calling
 *   MediaCodec#flush(), but there is no need to call redundant MediaCodec#stop() after the camera
 *   source is signaled stopped. Although primarily observed on non-A series Pixel phones, this
 *   workaround is applied to all Pixel devices for consistent behavior.
 * - Device(s): Pixel 7, Pixel 9, Pixel 9 Pro Fold.
 */
@SuppressLint("CameraXQuirksClassDetector")
public object PreviewFreezeAfterHighSpeedRecordingQuirk : Quirk {

    @JvmStatic
    public fun load(): Boolean {
        return isPixelPhone
    }

    private val isPixelPhone: Boolean =
        Build.BRAND.equals("google", ignoreCase = true) &&
            Build.MODEL.startsWith("Pixel", ignoreCase = true)
}
