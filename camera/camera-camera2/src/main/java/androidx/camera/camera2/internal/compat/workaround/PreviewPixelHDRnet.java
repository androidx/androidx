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

package androidx.camera.camera2.internal.compat.workaround;

import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.PreviewPixelHDRnetQuirk;
import androidx.camera.core.impl.SessionConfig;

/**
 * Workaround that handles turning on the WYSIWYG preview on Pixel devices.
 *
 * @see PreviewPixelHDRnetQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class PreviewPixelHDRnet {

    private PreviewPixelHDRnet() {
    }

    /**
     * Turns on WYSIWYG viewfinder on Pixel devices
     */
    public static void setHDRnet(@NonNull SessionConfig.Builder sessionBuilder) {
        final PreviewPixelHDRnetQuirk quirk = DeviceQuirks.get(PreviewPixelHDRnetQuirk.class);
        if (quirk == null) {
            return;
        }

        Camera2ImplConfig.Builder camera2ConfigBuilder = new Camera2ImplConfig.Builder();
        camera2ConfigBuilder.setCaptureRequestOption(CaptureRequest.TONEMAP_MODE,
                CaptureRequest.TONEMAP_MODE_HIGH_QUALITY);
        sessionBuilder.addImplementationOptions(camera2ConfigBuilder.build());
    }
}
