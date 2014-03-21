/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.graphics;

import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.SparseArray;

/**
 * Helper class of mapping ColorFilter from a color with different alpha levels.
 */
public final class ColorFilterCache {

    private static final SparseArray<ColorFilterCache> sColorToFiltersMap =
            new SparseArray<ColorFilterCache>();

    private final PorterDuffColorFilter[] mFilters = new PorterDuffColorFilter[0x100];

    /**
     * Get ColorDimmer for a given color.  Only r/g/b are used, alpha channel is ignored
     * from parameter dimColor.
     */
    public static ColorFilterCache getColorFilterCache(int dimColor) {
        final int r = Color.red(dimColor);
        final int g = Color.green(dimColor);
        final int b = Color.blue(dimColor);
        dimColor = Color.rgb(r, g, b);
        ColorFilterCache colorDimmer = sColorToFiltersMap.get(dimColor);
        if (colorDimmer == null) {
            colorDimmer = new ColorFilterCache(r, g, b);
            sColorToFiltersMap.put(dimColor, colorDimmer);
        }
        return colorDimmer;
    }

    private ColorFilterCache(int r, int g, int b) {
        // Pre cache all Dim filter levels
        for (int i = 0x00; i <= 0xFF; i++) {
            int color = Color.argb(i, r, g, b);
            mFilters[i] = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
    }

    /**
     * Returns ColorFilter for a given level between 0 and 1.
     */
    public ColorFilter getFilterForLevel(float level) {
        if (level >= 0 && level <= 1.0) {
            int filterIndex = (int) (0xFF * level);
            return mFilters[filterIndex];
        } else {
            return null;
        }
    }
}