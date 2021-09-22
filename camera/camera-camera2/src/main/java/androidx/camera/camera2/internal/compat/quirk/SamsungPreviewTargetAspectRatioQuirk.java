/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.List;

/**
 * Quirk that produces stretched preview on certain Samsung devices.
 *
 * <p> On certain Samsung devices, the HAL provides 16:9 preview even when the Surface size is
 * set to 4:3, which causes the preview to be stretched in PreviewView.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class SamsungPreviewTargetAspectRatioQuirk implements Quirk {

    // List of devices with the issue.
    private static final List<String> DEVICE_MODELS = Arrays.asList(
            "SM-J710MN", // b/170762209
            "SM-T580" // b/169471824
    );

    static boolean load() {
        return "SAMSUNG".equals(Build.BRAND.toUpperCase())
                && DEVICE_MODELS.contains(android.os.Build.MODEL.toUpperCase());
    }

    /**
     * Whether to overwrite the aspect ratio in the config to be 16:9.
     */
    public boolean require16_9(@NonNull Config config) {
        return config instanceof PreviewConfig;
    }
}
