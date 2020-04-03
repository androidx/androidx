/*
 * Copyright 2018 The Android Open Source Project
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

/**
 * An interpolator where the rate of change starts out slowly and
 * and then accelerates.
 */
public class AccelerateInterpolator implements Interpolator {
    private final float mFactor;
    private final double mDoubleFactor;

    /**
     * Creates a new instance of {@link AccelerateInterpolator} with y=x^2 parabola.
     */
    public AccelerateInterpolator() {
        mFactor = 1.0f;
        mDoubleFactor = 2.0;
    }

    /**
     * Creates a new instance of {@link AccelerateInterpolator}.
     *
     * @param factor Degree to which the animation should be eased. Setting
     *        factor to 1.0f produces a y=x^2 parabola. Increasing factor above
     *        1.0f  exaggerates the ease-in effect (i.e., it starts even
     *        slower and ends evens faster)
     */
    public AccelerateInterpolator(float factor) {
        mFactor = factor;
        mDoubleFactor = 2 * mFactor;
    }

    /**
     * Creates a new instance of {@link AccelerateInterpolator} from XML.
     *
     * @param context The context.
     * @param attrs The AttributeSet from XML.
     */
    public AccelerateInterpolator(@NonNull Context context, @NonNull AttributeSet attrs) {
        this(context.getResources(), context.getTheme(), attrs);
    }

    AccelerateInterpolator(Resources res, Theme theme, AttributeSet attrs) {
        TypedArray a;
        if (theme != null) {
            a = theme.obtainStyledAttributes(attrs,
                    AndroidResources.STYLEABLE_ACCELERATE_INTERPOLATOR, 0, 0);
        } else {
            a = res.obtainAttributes(attrs, AndroidResources.STYLEABLE_ACCELERATE_INTERPOLATOR);
        }

        mFactor = a.getFloat(AndroidResources.STYLEABLE_ACCELERATE_INTERPOLATOR_FACTOR, 1.0f);
        mDoubleFactor = 2 * mFactor;
        a.recycle();
    }

    @Override
    @FloatRange(from = 0, to = 1)
    public float getInterpolation(@FloatRange(from = 0, to = 1) float input) {
        if (mFactor == 1.0f) {
            return input * input;
        } else {
            return (float) Math.pow(input, mDoubleFactor);
        }
    }
}
