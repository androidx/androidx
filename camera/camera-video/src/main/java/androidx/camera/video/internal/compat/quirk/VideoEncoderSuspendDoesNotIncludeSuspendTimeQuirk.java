/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video.internal.compat.quirk;

import android.media.MediaCodec;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 *     Bug Id: b/202798609, b/202798572
 *     Description: Quirk indicates that the video encoder outputs incorrect timestamps after
 *                  pause and resume recording. The issue happens on many Samsung devices with
 *                  API level < 29. After the codec is paused and resumed by invoking
 *                  {@link MediaCodec#setParameters} with
 *                  {@link MediaCodec#PARAMETER_KEY_SUSPEND}, the timestamp of the encoded video
 *                  data after resumed will lack the pause duration, resulting in A/V out of sync
 *                  after resumed.
 *     Device(s): Some Samsung devices pre-API 29
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class VideoEncoderSuspendDoesNotIncludeSuspendTimeQuirk implements Quirk {

    static boolean load() {
        return "Samsung".equalsIgnoreCase(Build.BRAND)
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q /* API 29 */;
    }
}
