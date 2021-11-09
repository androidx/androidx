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
import androidx.annotation.Nullable;

/**
 * An interpolator where the change flings forward and overshoots the last value
 * then comes back.
 */
public class OvershootInterpolator implements Interpolator {
    private final float mTension;

    public OvershootInterpolator() {
        mTension = 2.0f;
    }

    /**
     * @param tension Amount of overshoot. When tension equals 0.0f, there is
     *                no overshoot and the interpolator becomes a simple
     *                deceleration interpolator.
     */
    public OvershootInterpolator(float tension) {
        mTension = tension;
    }

    public OvershootInterpolator(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context.getResources(), context.getTheme(), attrs);
    }

    OvershootInterpolator(Resources res, Theme theme, AttributeSet attrs) {
        TypedArray a;
        if (theme != null) {
            a = theme.obtainStyledAttributes(attrs,
                    AndroidResources.STYLEABLE_OVERSHOOT_INTERPOLATOR, 0, 0);
        } else {
            a = res.obtainAttributes(attrs, AndroidResources.STYLEABLE_OVERSHOOT_INTERPOLATOR);
        }

        mTension = a.getFloat(AndroidResources.STYLEABLE_OVERSHOOT_INTERPOLATOR_TENSION, 2.0f);
        a.recycle();
    }

    @Override
    @FloatRange(from = 0)
    public float getInterpolation(@FloatRange(from = 0, to = 1) float input) {
        // _o(t) = t * t * ((tension + 1) * t + tension)
        // o(t) = _o(t - 1) + 1
        input -= 1.0f;
        return input * input * ((mTension + 1) * input + mTension) + 1.0f;
    }
}
