/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.camera.core.internal.compat.quirk.SurfaceProcessingQuirk

/**
 * QuirkSummary
 * - Bug Id: b/361477717, b/391480268
 * - Description: Quirk indicates Preview is black screen when binding with VideoCapture. On Samsung
 *   SM-T580, ExtraCroppingQuirk in camera artifact forces VideoCapture to use a 1920x1080
 *   resolution. While this works for the back camera, the front camera does not support 1920x1080
 *   with a MediaCodec Surface, leading to a black screen. Using a SurfaceTexture Surface can
 *   workaround this issue.
 * - Device(s): Motorola Edge 20 Fusion, Samsung Galaxy Tab A (2016) SM-T580
 */
@SuppressLint("CameraXQuirksClassDetector")
public class PreviewBlackScreenQuirk : SurfaceProcessingQuirk {

    public companion object {

        @JvmStatic
        public fun load(): Boolean {
            return isMotorolaEdge20Fusion || isSamsungSmT580
        }

        private val isMotorolaEdge20Fusion: Boolean =
            Build.BRAND.equals("motorola", ignoreCase = true) &&
                Build.MODEL.equals("motorola edge 20 fusion", ignoreCase = true)

        private val isSamsungSmT580: Boolean =
            Build.BRAND.equals("samsung", ignoreCase = true) &&
                Build.MODEL.equals("sm-t580", ignoreCase = true)
    }
}
