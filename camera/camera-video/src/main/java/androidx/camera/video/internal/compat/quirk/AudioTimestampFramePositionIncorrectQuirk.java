/*
 * Copyright 2022 The Android Open Source Project
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

import android.media.AudioTimestamp;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 *     Bug Id: 245518008
 *     Description: Quirk which denotes {@link android.media.AudioTimestamp#framePosition} queried
 *                  by {@link android.media.AudioRecord#getTimestamp(AudioTimestamp, int)} returns
 *                  incorrect info. On Redmi 6A, frame position becomes negative after recording
 *                  multiple times.
 *
 *     Device(s): Redmi 6A
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AudioTimestampFramePositionIncorrectQuirk implements Quirk {

    static boolean load() {
        return isRedmi6A();
    }

    private static boolean isRedmi6A() {
        return "Xiaomi".equalsIgnoreCase(Build.BRAND) && "Redmi 6A".equalsIgnoreCase(Build.MODEL);
    }
}
