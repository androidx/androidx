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
 * - Bug Id: b/361477717
 * - Description: Quirk indicates Preview is black screen when binding with VideoCapture.
 * - Device(s): Motorola Edge 20 Fusion.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class PreviewBlackScreenQuirk : SurfaceProcessingQuirk {

    public companion object {

        @JvmStatic
        public fun load(): Boolean {
            return isMotorolaEdge20Fusion
        }

        private val isMotorolaEdge20Fusion: Boolean =
            Build.BRAND.equals("motorola", ignoreCase = true) &&
                Build.MODEL.equals("motorola edge 20 fusion", ignoreCase = true)
    }
}
