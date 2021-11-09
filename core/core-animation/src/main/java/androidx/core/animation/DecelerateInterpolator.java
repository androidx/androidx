/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.core.animation;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An interpolator where the rate of change starts out quickly and
 * and then decelerates.
 *
 */
public class DecelerateInterpolator implements Interpolator {
    public DecelerateInterpolator() {
    }

    /**
     * Constructor
     *
     * @param factor Degree to which the animation should be eased. Setting factor to 1.0f produces
     *        an upside-down y=x^2 parabola. Increasing factor above 1.0f makes exaggerates the
     *        ease-out effect (i.e., it starts even faster and ends evens slower)
     */
    public DecelerateInterpolator(float factor) {
        mFactor = factor;
    }

    public DecelerateInterpolator(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context.getResources(), context.getTheme(), attrs);
    }

    DecelerateInterpolator(Resources res, Theme theme, AttributeSet attrs) {
        TypedArray a;
        if (theme != null) {
            a = theme.obtainStyledAttributes(attrs,
                    AndroidResources.STYLEABLE_DECELERATE_INTERPOLATOR, 0, 0);
        } else {
            a = res.obtainAttributes(attrs, AndroidResources.STYLEABLE_DECELERATE_INTERPOLATOR);
        }

        mFactor = a.getFloat(AndroidResources.STYLEABLE_DECELERATE_INTERPOLATOR_FACTOR,
                1.0f);
        a.recycle();
    }

    @Override
    @FloatRange(from = 0, to = 1)
    public float getInterpolation(@FloatRange(from = 0, to = 1) float input) {
        float result;
        if (mFactor == 1.0f) {
            result = 1.0f - (1.0f - input) * (1.0f - input);
        } else {
            result = (float) (1.0f - Math.pow((1.0f - input), 2 * mFactor));
        }
        return result;
    }

    private float mFactor = 1.0f;
}
