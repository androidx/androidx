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

package androidx.camera.video.internal.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: b/573671138
 * - Description: Quirk denotes that releasing a video encoder which was configured with a surface
 *   input but never started leaks a native thread in the media framework (`C2NodeImpl`, named
 *   `C2OMXNode` on older versions). One thread is leaked per released encoder and stays for the
 *   lifetime of the process. The encoder is released in this state whenever a `VideoCapture` is
 *   unbound without an active recording, since the encoder is configured in advance and is
 *   configured again after each recording stops. Starting the codec right before releasing it
 *   avoids the leak. The issue exists since Android 10 (API 29) and affects encoders using the
 *   Codec2 framework; encoders using OMX are not affected.
 * - Device(s): All devices on Android 10 (API 29) and above.
 */
@SuppressLint("CameraXQuirksClassDetector")
public object VideoEncoderLeaksThreadOnReleaseWithoutStartQuirk : Quirk {

    @JvmStatic
    public fun load(): Boolean {
        // The issue is in the Codec2 media framework, introduced in Android 10. Add an upper
        // bound once the framework fix ships.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
}
