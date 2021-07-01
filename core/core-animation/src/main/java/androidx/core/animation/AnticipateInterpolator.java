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
 * An interpolator where the change starts backward then flings forward.
 */
public class AnticipateInterpolator implements Interpolator {
    private final float mTension;

    /**
     * Creates a new instance of {@link AnticipateInterpolator}.
     */
    public AnticipateInterpolator() {
        mTension = 2.0f;
    }

    /**
     * Creates a new instance of {@link AnticipateInterpolator}.
     *
     * @param tension Amount of anticipation. When tension equals 0.0f, there is
     *                no anticipation and the interpolator becomes a simple
     *                acceleration interpolator.
     */
    public AnticipateInterpolator(float tension) {
        mTension = tension;
    }

    /**
     * Creates a new instance of {@link AnticipateInterpolator} from XML.
     *
     * @param context The context.
     * @param attrs The {@link AttributeSet} from the XML.
     */
    public AnticipateInterpolator(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context.getResources(), context.getTheme(), attrs);
    }

    AnticipateInterpolator(Resources res, Theme theme, AttributeSet attrs) {
        TypedArray a;
        if (theme != null) {
            a = theme.obtainStyledAttributes(attrs,
                    AndroidResources.STYLEABLE_ANTICIPATEOVERSHOOT_INTERPOLATOR, 0, 0);
        } else {
            a = res.obtainAttributes(attrs,
                    AndroidResources.STYLEABLE_ANTICIPATEOVERSHOOT_INTERPOLATOR);
        }

        mTension = a.getFloat(AndroidResources.STYLEABLE_ANTICIPATEOVERSHOOT_INTERPOLATOR_TENSION,
                2.0f);
        a.recycle();
    }

    @Override
    @FloatRange(to = 1)
    public float getInterpolation(@FloatRange(from = 0, to = 1) float input) {
        // a(t) = t * t * ((tension + 1) * t - tension)
        return input * input * ((mTension + 1) * input - mTension);
    }
}
