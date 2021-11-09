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

import androidx.annotation.StyleableRes;

class AndroidResources {
    private AndroidResources() {}
    static final int[] STYLEABLE_ANTICIPATEOVERSHOOT_INTERPOLATOR = {
        android.R.attr.tension,
        android.R.attr.extraTension
    };
    @StyleableRes
    static final int STYLEABLE_ANTICIPATEOVERSHOOT_INTERPOLATOR_TENSION = 0;
    @StyleableRes
    static final int STYLEABLE_ANTICIPATEOVERSHOOT_INTERPOLATOR_EXTRA_TENSION = 1;
    static final int[] STYLEABLE_ACCELERATE_INTERPOLATOR = {
            android.R.attr.factor
    };
    @StyleableRes
    static final int STYLEABLE_ACCELERATE_INTERPOLATOR_FACTOR = 0;
    static final int[] STYLEABLE_DECELERATE_INTERPOLATOR = {
            android.R.attr.factor
    };
    @StyleableRes
    static final int STYLEABLE_DECELERATE_INTERPOLATOR_FACTOR = 0;
    static final int[] STYLEABLE_CYCLE_INTERPOLATOR = {
            android.R.attr.cycles,
    };
    @StyleableRes
    static final int STYLEABLE_CYCLE_INTERPOLATOR_CYCLES = 0;
    static final int[] STYLEABLE_OVERSHOOT_INTERPOLATOR = {
            android.R.attr.tension
    };
    @StyleableRes
    static final int STYLEABLE_OVERSHOOT_INTERPOLATOR_TENSION = 0;

    public static final int[] STYLEABLE_ANIMATOR = {
            android.R.attr.interpolator, android.R.attr.duration,
            android.R.attr.startOffset, android.R.attr.repeatCount,
            android.R.attr.repeatMode, android.R.attr.valueFrom,
            android.R.attr.valueTo, android.R.attr.valueType,
    };
    public static final int STYLEABLE_ANIMATOR_INTERPOLATOR = 0;
    public static final int STYLEABLE_ANIMATOR_DURATION = 1;
    public static final int STYLEABLE_ANIMATOR_START_OFFSET = 2;
    public static final int STYLEABLE_ANIMATOR_REPEAT_COUNT = 3;
    public static final int STYLEABLE_ANIMATOR_REPEAT_MODE = 4;
    public static final int STYLEABLE_ANIMATOR_VALUE_FROM = 5;
    public static final int STYLEABLE_ANIMATOR_VALUE_TO = 6;
    public static final int STYLEABLE_ANIMATOR_VALUE_TYPE = 7;
    public static final int[] STYLEABLE_ANIMATOR_SET = {
            android.R.attr.ordering
    };
    public static final int STYLEABLE_ANIMATOR_SET_ORDERING = 0;
    public static final int[] STYLEABLE_PROPERTY_VALUES_HOLDER = {
            android.R.attr.valueFrom, android.R.attr.valueTo,
            android.R.attr.valueType, android.R.attr.propertyName,
    };
    public static final int STYLEABLE_PROPERTY_VALUES_HOLDER_VALUE_FROM = 0;
    public static final int STYLEABLE_PROPERTY_VALUES_HOLDER_VALUE_TO = 1;
    public static final int STYLEABLE_PROPERTY_VALUES_HOLDER_VALUE_TYPE = 2;
    public static final int STYLEABLE_PROPERTY_VALUES_HOLDER_PROPERTY_NAME = 3;
    public static final int[] STYLEABLE_KEYFRAME = {
            android.R.attr.value, android.R.attr.interpolator,
            android.R.attr.valueType, android.R.attr.fraction
    };
    public static final int STYLEABLE_KEYFRAME_VALUE = 0;
    public static final int STYLEABLE_KEYFRAME_INTERPOLATOR = 1;
    public static final int STYLEABLE_KEYFRAME_VALUE_TYPE = 2;
    public static final int STYLEABLE_KEYFRAME_FRACTION = 3;
    public static final int[] STYLEABLE_PROPERTY_ANIMATOR = {
            android.R.attr.propertyName, android.R.attr.pathData,
            android.R.attr.propertyXName, android.R.attr.propertyYName,
    };
    public static final int STYLEABLE_PROPERTY_ANIMATOR_PROPERTY_NAME = 0;
    public static final int STYLEABLE_PROPERTY_ANIMATOR_PATH_DATA = 1;
    public static final int STYLEABLE_PROPERTY_ANIMATOR_PROPERTY_X_NAME = 2;
    public static final int STYLEABLE_PROPERTY_ANIMATOR_PROPERTY_Y_NAME = 3;
    public static final int[] STYLEABLE_PATH_INTERPOLATOR = {
            android.R.attr.controlX1, android.R.attr.controlY1,
            android.R.attr.controlX2, android.R.attr.controlY2,
            android.R.attr.pathData
    };
    public static final int STYLEABLE_PATH_INTERPOLATOR_CONTROL_X_1 = 0;
    public static final int STYLEABLE_PATH_INTERPOLATOR_CONTROL_Y_1 = 1;
    public static final int STYLEABLE_PATH_INTERPOLATOR_CONTROL_X_2 = 2;
    public static final int STYLEABLE_PATH_INTERPOLATOR_CONTROL_Y_2 = 3;
    public static final int STYLEABLE_PATH_INTERPOLATOR_PATH_DATA = 4;
    public static final int FAST_OUT_LINEAR_IN = 0x010c000f;
    public static final int FAST_OUT_SLOW_IN = 0x010c000d;
    public static final int LINEAR_OUT_SLOW_IN = 0x010c000e;
}
