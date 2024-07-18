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

package androidx.camera.core.internal.utils;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.utils.CompareSizesByArea;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for size related operations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class SizeUtil {
    public static final Size RESOLUTION_ZERO = new Size(0, 0);
    public static final Size RESOLUTION_QVGA = new Size(320, 240);
    public static final Size RESOLUTION_VGA = new Size(640, 480);
    public static final Size RESOLUTION_480P = new Size(720, 480);
    public static final Size RESOLUTION_720P = new Size(1280, 720);
    public static final Size RESOLUTION_1080P = new Size(1920, 1080);
    public static final Size RESOLUTION_1440P = new Size(1920, 1440);

    private SizeUtil() {
    }

    /**
     * Returns the area of the supplied size.
     */
    public static int getArea(@NonNull Size size) {
        return size.getWidth() * size.getHeight();
    }

    /**
     * Returns {@code true} if the source size area is smaller than the target size area.
     * Otherwise, returns {@code false}.
     */
    public static boolean isSmallerByArea(@NonNull Size sourceSize, @NonNull Size targetSize) {
        return getArea(sourceSize) < getArea(targetSize);
    }

    /**
     * Returns {@code true} if any edge of the source size is longer than the corresponding edge of
     * the target size. Otherwise, returns {@code false}.
     */
    public static boolean isLongerInAnyEdge(@NonNull Size sourceSize, @NonNull Size targetSize) {
        return sourceSize.getWidth() > targetSize.getWidth()
                || sourceSize.getHeight() > targetSize.getHeight();
    }

    /**
     * Returns the size which has the max area in the input size list. Returns null if the input
     * size list is empty.
     */
    @Nullable
    public static Size getMaxSize(@NonNull List<Size> sizeList) {
        if (sizeList.isEmpty()) {
            return null;
        }

        return Collections.max(sizeList, new CompareSizesByArea());
    }

    /** Returns the nearest higher entry value from a area sorted map and an input size. */
    @Nullable
    public static <T> T findNearestHigherFor(@NonNull Size size,
            @NonNull TreeMap<Size, T> areaSortedSizeMap) {
        Map.Entry<Size, T> ceilEntry = areaSortedSizeMap.ceilingEntry(size);

        if (ceilEntry != null) {
            // The ceiling entry will either be equivalent or higher in size, so always return it.
            return ceilEntry.getValue();
        } else {
            // If a ceiling entry doesn't exist and a floor entry exists, it is the closest
            // we have, so return it.
            Map.Entry<Size, T> floorEntry = areaSortedSizeMap.floorEntry(size);
            if (floorEntry != null) {
                return floorEntry.getValue();
            }
        }
        return null;
    }
}
