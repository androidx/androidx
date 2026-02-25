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
import androidx.camera.core.impl.Quirk
import androidx.core.backported.fixes.BackportedFixManager
import androidx.core.backported.fixes.KnownIssue

/**
 * QuirkSummary
 * - Bug Id: b/436119518
 * - Description: BackportedFixManager base quirk. For example, the PixelJpegRSupportedQuirk extends
 *   this to check JPEG_R support on Pixel devices. There is abnormal color tone issue when
 *   capturing JPEG-R images on some Pixel devices.
 * - Device(s): Pixel devices
 */
@SuppressLint("CameraXQuirksClassDetector")
public abstract class BackportedFixQuirk : Quirk {

    public companion object {
        public val backportedFixManager: BackportedFixManager by lazy { BackportedFixManager() }
    }

    public abstract fun getKnownIssue(): KnownIssue

    public fun hasIssue(): Boolean = !backportedFixManager.isFixed(getKnownIssue())
}
