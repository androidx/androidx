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

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.view.View;
import android.support.v17.leanback.R;

/**
 * Helper class for applying dim level to view(s).  The ColorFilterDimmer
 * holds a Paint object and ColorFilter corresponding to current "active" level.
 */
public final class ColorFilterDimmer {

    private final ColorFilterCache mColorDimmer;

    private final float mActiveLevel;
    private final float mDimmedLevel;

    private final Paint mPaint;
    private ColorFilter mFilter;

    /**
     * Create a default ColorFilterDimmer.
     */
    public static ColorFilterDimmer createDefault(Context context) {
        return new ColorFilterDimmer(ColorFilterCache.getColorFilterCache(
                context.getResources().getColor(R.color.lb_view_dim_mask_color)),
                0, context.getResources().getFraction(R.dimen.lb_view_dimmed_level, 1, 1));
    }

    /**
     * Create a ColorFilterDimmer.
     *
     * @param dimmer      The ColorFilterCache for dim color.
     * @param activeLevel The level of dimming for when the view is in its active state. Must be a
     *                    float value between 0.0 and 1.0.
     * @param dimmedLevel The level of dimming for when the view is in its dimmed state. Must be a
     *                    float value between 0.0 and 1.0.
     */
    public static ColorFilterDimmer create(ColorFilterCache dimmer,
            float activeLevel, float dimmedLevel) {
        return new ColorFilterDimmer(dimmer, activeLevel, dimmedLevel);
    }

    private ColorFilterDimmer(ColorFilterCache dimmer, float activeLevel, float dimmedLevel) {
        mColorDimmer = dimmer;
        mActiveLevel = activeLevel;
        mDimmedLevel = dimmedLevel;
        mPaint = new Paint();
    }

    /**
     * Apply current ColorFilter to a view, will assign and remove hardware layer of the view.
     */
    public void applyFilterToView(View view) {
        if (mFilter != null) {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, mPaint);
        } else {
            view.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        // FIXME: Current framework has bug that not triggering invalidate when change layer
        // paint.  Will add conditional sdk version check once bug is fixed in released
        // framework.
        view.invalidate();
    }

    /**
     * Sets the active level and change internal filter and paint.
     * @param level Between 0 for dim and 1 for fully active.
     */
    public void setActiveLevel(float level) {
        mFilter = mColorDimmer.getFilterForLevel(
                mDimmedLevel + level * (mActiveLevel - mDimmedLevel));
        mPaint.setColorFilter(mFilter);
    }

    /**
     * Gets the color filter set to current dim level.
     */
    public ColorFilter getColorFilter() {
        return mFilter;
    }

    /**
     * Gets the paint object set to current dim level.
     */
    public Paint getPaint() {
        return mPaint;
    }

}
