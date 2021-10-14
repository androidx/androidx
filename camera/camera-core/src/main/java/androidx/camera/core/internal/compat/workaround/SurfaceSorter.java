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

package androidx.camera.core.internal.compat.workaround;

import android.media.MediaCodec;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.internal.compat.quirk.DeviceQuirks;
import androidx.camera.core.internal.compat.quirk.SurfaceOrderQuirk;

import java.util.Collections;
import java.util.List;

/**
 * Workaround that put {@link Preview} surface in front of the {@link MediaCodec} surface
 *
 * @see SurfaceOrderQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class SurfaceSorter {
    // The larger priority value will be placed at the back of the list.
    private static final int PRIORITY_MEDIA_CODEC_SURFACE = 1;
    private static final int PRIORITY_OTHERS = 0;

    private final boolean mHasQuirk = DeviceQuirks.get(SurfaceOrderQuirk.class) != null;

    /**
     * Sorts the list to prevent from the device specific issue.
     *
     * @param surfaces the input surface list to sort, must be a mutable list.
     */
    public void sort(@NonNull List<DeferrableSurface> surfaces) {
        if (!mHasQuirk) {
            return;
        }
        Collections.sort(surfaces, (surface1, surface2) -> {
            int p1 = getSurfacePriority(surface1);
            int p2 = getSurfacePriority(surface2);
            return p1 - p2;
        });
    }

    private int getSurfacePriority(@NonNull DeferrableSurface surface) {
        if (surface.getContainerClass() == MediaCodec.class
                || surface.getContainerClass() == VideoCapture.class) {
            return PRIORITY_MEDIA_CODEC_SURFACE;
        }
        return PRIORITY_OTHERS;
    }
}
