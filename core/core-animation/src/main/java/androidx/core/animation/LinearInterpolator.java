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
import android.util.AttributeSet;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An interpolator where the rate of change is constant
 */

public class LinearInterpolator implements Interpolator{

    public LinearInterpolator() {
    }

    public LinearInterpolator(@NonNull Context context, @Nullable AttributeSet attrs) {
    }

    @Override
    @FloatRange(from = 0, to = 1)
    public float getInterpolation(@FloatRange(from = 0, to = 1) float input) {
        return input;
    }

}
