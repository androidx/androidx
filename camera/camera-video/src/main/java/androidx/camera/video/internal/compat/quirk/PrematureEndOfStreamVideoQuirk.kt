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
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: 361014312
 * - Description: Quirk for an issue on the Oppo CPH1931 where the video codec incorrectly sends an
 *   EOS (End of Stream) signal when recording for the second time, causing the recording to stop
 *   prematurely.
 * - Device(s): Oppo CPH1931
 */
@SuppressLint("CameraXQuirksClassDetector")
public object PrematureEndOfStreamVideoQuirk : Quirk {

    @JvmStatic public fun load(): Boolean = isCph1931

    private val isCph1931: Boolean =
        "OPPO".equals(Build.BRAND, ignoreCase = true) &&
            "CPH1931".equals(Build.MODEL, ignoreCase = true)
}
