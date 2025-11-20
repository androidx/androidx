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

package androidx.camera.video.internal.compat.quirk;

import android.os.Build;

import androidx.camera.core.DynamicRange;
import androidx.camera.core.impl.Quirk;

import org.jspecify.annotations.NonNull;

/**
 * <p>QuirkSummary
 *     Bug Id: b/403450605
 *     Description: Quirk denotes that repeating request fails on Samsung Galaxy S25 Ultra when
 *                  Preview uses 4:3 resolutions, VideoCapture uses 16:9 resolutions and HDR is
 *                  enabled. This issue also occurs when only VideoCapture is bound, in which case
 *                  a PRIV stream (for metering repeating) will be bound implicitly. As a result of
 *                  repeating request failure, both preview and video capture will fail.
 *     Device(s): Samsung Galaxy S25 Ultra
 */
public class HdrRepeatingRequestFailureQuirk implements Quirk {

    static boolean load() {
        return isSamsungS25Ultra();
    }

    private static boolean isSamsungS25Ultra() {
        return "samsung".equalsIgnoreCase(Build.BRAND) && "pa3q".equalsIgnoreCase(Build.DEVICE);
    }

    /**
     * Returns {@code true} if the issue can be workaround by surface processing (OpenGL) pipeline.
     *
     * <p>The issue can be workaround by providing a {@link android.graphics.SurfaceTexture} to the
     * camera instead of an encoder surface. However, it would be difficult for the VideoCapture to
     * know what aspect ratio is configured for the Preview. For simplicity, only dynamic range is
     * considered to decide whether to enable this workaround.
     *
     * @param dynamicRange The {@link DynamicRange} for video recording.
     * @return {@code true} if surface processing can be used to avoid the issue.
     */
    @SuppressWarnings("ReferenceEquality") // dynamicRange != DynamicRange.SDR
    public boolean workaroundBySurfaceProcessing(@NonNull DynamicRange dynamicRange) {
        boolean isHdr = dynamicRange != DynamicRange.SDR;
        return isSamsungS25Ultra() && isHdr;
    }
}
