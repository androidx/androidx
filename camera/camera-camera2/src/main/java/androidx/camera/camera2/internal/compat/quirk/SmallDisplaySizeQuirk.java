/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.quirk;

import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * <p>QuirkSummary
 *     Bug Id: b/287341266
 *     Description: Quirk required to return the display size for problematic devices. Some
 *                  devices might return abnormally small display size (16x16). This might cause
 *                  PREVIEW size to be incorrectly determined and all supported output sizes are
 *                  filtered out.
 *     Device(s): Redmi Note8, Redmi Note 7, SM-A207M (see b/287341266 for the devices list)
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class SmallDisplaySizeQuirk implements Quirk {

    private static final String TAG = "SmallDisplaySizeQuirk";

    private static final Map<String, Size> MODEL_TO_DISPLAY_SIZE_MAP =
            new HashMap<>();

    static {
        MODEL_TO_DISPLAY_SIZE_MAP.put("REDMI NOTE 8", new Size(1080, 2340));
        MODEL_TO_DISPLAY_SIZE_MAP.put("REDMI NOTE 7", new Size(1080, 2340));
        MODEL_TO_DISPLAY_SIZE_MAP.put("SM-A207M", new Size(720, 1560));
        MODEL_TO_DISPLAY_SIZE_MAP.put("REDMI NOTE 7S", new Size(1080, 2340));
        MODEL_TO_DISPLAY_SIZE_MAP.put("SM-A127F", new Size(720, 1600));
        MODEL_TO_DISPLAY_SIZE_MAP.put("SM-A536E", new Size(1080, 2400));
        MODEL_TO_DISPLAY_SIZE_MAP.put("220233L2I", new Size(720, 1600));
        MODEL_TO_DISPLAY_SIZE_MAP.put("V2149", new Size(720, 1600));
        MODEL_TO_DISPLAY_SIZE_MAP.put("VIVO 1920", new Size(1080, 2340));
        MODEL_TO_DISPLAY_SIZE_MAP.put("CPH2223", new Size(1080, 2400));
        MODEL_TO_DISPLAY_SIZE_MAP.put("V2029", new Size(720, 1600));
        MODEL_TO_DISPLAY_SIZE_MAP.put("CPH1901", new Size(720, 1520));
        MODEL_TO_DISPLAY_SIZE_MAP.put("REDMI Y3", new Size(720, 1520));
        MODEL_TO_DISPLAY_SIZE_MAP.put("SM-A045M", new Size(720, 1600));
        MODEL_TO_DISPLAY_SIZE_MAP.put("SM-A146U", new Size(1080, 2408));
        MODEL_TO_DISPLAY_SIZE_MAP.put("CPH1909", new Size(720, 1520));
        MODEL_TO_DISPLAY_SIZE_MAP.put("NOKIA 4.2", new Size(720, 1520));
        MODEL_TO_DISPLAY_SIZE_MAP.put("SM-G960U1", new Size(1440, 2960));
        MODEL_TO_DISPLAY_SIZE_MAP.put("SM-A137F", new Size(1080, 2408));
        MODEL_TO_DISPLAY_SIZE_MAP.put("VIVO 1816", new Size(720, 1520));
        MODEL_TO_DISPLAY_SIZE_MAP.put("INFINIX X6817", new Size(720, 1612));
        MODEL_TO_DISPLAY_SIZE_MAP.put("SM-A037F", new Size(720, 1600));
        MODEL_TO_DISPLAY_SIZE_MAP.put("NOKIA 2.4", new Size(720, 1600));
        MODEL_TO_DISPLAY_SIZE_MAP.put("SM-A125M", new Size(720, 1600));
        MODEL_TO_DISPLAY_SIZE_MAP.put("INFINIX X670", new Size(1080, 2400));
    }

    static boolean load() {
        return MODEL_TO_DISPLAY_SIZE_MAP.containsKey(Build.MODEL.toUpperCase(Locale.US));
    }

    @NonNull
    public Size getDisplaySize() {
        return MODEL_TO_DISPLAY_SIZE_MAP.get(Build.MODEL.toUpperCase(Locale.US));
    }
}
