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

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 *     Bug Id: b/202799148
 *     Description: Quirk indicates that the audio encoder outputs incorrect timestamps after
 *                  pause and resume recording. On the problem device, the audio codec only refers
 *                  to the first timestamp of the input audio data, and then accumulates
 *                  timestamps based on the subsequent input audio data, but does not refer to the
 *                  timestamp. So if we stop sending audio data to the codec or pause the codec
 *                  when paused, then codec will miss the timestamp increment during pause
 *                  duration, resulting in a/v out of sync after resume.
 *     Device(s): Sony-G3125
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AudioEncoderIgnoresInputTimestampQuirk implements Quirk {

    static boolean load() {
        return isSonyG3125();
    }

    private static boolean isSonyG3125() {
        return "Sony".equalsIgnoreCase(Build.BRAND) && "G3125".equalsIgnoreCase(Build.MODEL);
    }
}
