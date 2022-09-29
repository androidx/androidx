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

import android.media.MediaMuxer;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;
import androidx.camera.video.internal.workaround.CorrectNegativeLatLongForMediaMuxer;

/**
 * <p>QuirkSummary
 *      Bug Id:      b/232327925
 *      Description: Setting negative latitude or longitude value to
 *                   {@link MediaMuxer#setLocation(float, float)} may result in the value in the
 *                   video metadata to be incorrectly increased a little. For example, setting
 *                   (10.0, -20.0) to {@code MediaMuxer#setLocation} results in "+10.0000-19.9999/"
 *                   in the video meta data.
 *      Device(s):   All devices
 *
 * @see CorrectNegativeLatLongForMediaMuxer
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class NegativeLatLongSavesIncorrectlyQuirk implements Quirk {

    static boolean load() {
        return Build.VERSION.SDK_INT < 34;
    }
}
