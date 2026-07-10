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
package androidx.camera.camera2.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.internal.compat.quirk.BackportedFixQuirk
import androidx.core.backported.fixes.KnownIssue
import androidx.core.backported.fixes.KnownIssues

/**
 * QuirkSummary
 * - Bug Id: b/436119518
 * - Description: Quirk to check JPEG_R support on Pixel devices. Abnormal color tone when capturing
 *   JPEG-R images on some Pixel devices.
 * - Device(s): Pixel devices
 */
@SuppressLint("CameraXQuirksClassDetector")
public class PixelJpegRSupportedQuirk : BackportedFixQuirk() {

    override fun getKnownIssue(): KnownIssue {
        return KnownIssues.KI_398591036
    }

    public companion object {
        @JvmStatic
        public fun isEnabled(): Boolean {
            return Build.VERSION.SDK_INT >= 34 && PixelJpegRSupportedQuirk().hasIssue()
        }
    }
}
