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
import android.media.MediaCodec
import android.os.Build
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: b/391508996
 * - Description: Quirk indicates OpenGL pipeline gets stuck ([android.opengl.GLES20.glDrawArrays])
 *   after [MediaCodec.flush] is called. The workaround is instead of calling [MediaCodec.flush],
 *   use [MediaCodec.stop]. The flow is still the same as calling [MediaCodec.flush], but there is
 *   no need to call redundant [MediaCodec.stop] after the camera source is signaled stopped.
 * - Device(s): Twist 2 Pro
 */
@SuppressLint("CameraXQuirksClassDetector")
public object GLProcessingStuckOnCodecFlushQuirk : Quirk {

    @JvmStatic
    public fun load(): Boolean {
        return isPositivoTwist2Pro
    }

    private val isPositivoTwist2Pro: Boolean
        get() =
            "positivo".equals(Build.BRAND, ignoreCase = true) &&
                "twist 2 pro".equals(Build.MODEL, ignoreCase = true)
}
