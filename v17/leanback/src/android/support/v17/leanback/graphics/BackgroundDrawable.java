/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v17.leanback.graphics;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic drawable class that can be composed of multiple regions. Whenever the bounds changes
 * for this class, it updates those of it's children by calling {@link RegionDrawable#updateBounds}.
 */
public class BackgroundDrawable extends Drawable {
    protected List<RegionDrawable> regions = new ArrayList();

    /**
     * Adds the supplied region.
     */
    public void addRegion(RegionDrawable region) {
        this.regions.add(region);
    }

    /**
     * Returns the {@link RegionDrawable} for the given index.
     */
    public RegionDrawable getRegion(int index) {
        if (index < regions.size()) {
            return regions.get(index);
        }

        throw new IllegalArgumentException("Invalid index: "+index);
    }

    /**
     * Removes the region corresponding to the given index.
     */
    public void removeRegion(int index) {
        if (index < regions.size()) {
            regions.remove(index);
            return;
        }

        throw new IllegalArgumentException("Invalid index "+index);
    }

    /**
     * Removes the given region.
     */
    public void removeRegion(RegionDrawable drawable) {
        this.regions.remove(drawable);
    }

    /**
     * Returns the total number of regions.
     */
    public int getNumberOfRegions() {
        return regions.size();
    }

    @Override
    public void draw(Canvas canvas) {
        for (int i = 0; i < regions.size(); i++) {
            regions.get(i).draw(canvas);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        for (int i = 0; i < regions.size(); i++) {
            regions.get(i).updateBounds(bounds);
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        for (int i = 0; i < regions.size(); i++) {
            regions.get(i).setColorFilter(colorFilter);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    @Override
    public void setAlpha(int alpha) {
        for (int i = 0; i < regions.size(); i++) {
            regions.get(i).setAlpha(alpha);
        }
    }
}
