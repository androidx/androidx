/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.transition;

import android.annotation.SuppressLint;

import androidx.annotation.StyleableRes;

/**
 * Copies of styleable ID values generated in the platform R.java.
 */
@SuppressLint("InlinedApi")
class Styleable {

    @StyleableRes
    static final int[] TRANSITION_TARGET = {
            android.R.attr.targetClass,
            android.R.attr.targetId,
            android.R.attr.excludeId,
            android.R.attr.excludeClass,
            android.R.attr.targetName,
            android.R.attr.excludeName,
    };

    interface TransitionTarget {
        @StyleableRes
        int TARGET_CLASS = 0;
        @StyleableRes
        int TARGET_ID = 1;
        @StyleableRes
        int EXCLUDE_ID = 2;
        @StyleableRes
        int EXCLUDE_CLASS = 3;
        @StyleableRes
        int TARGET_NAME = 4;
        @StyleableRes
        int EXCLUDE_NAME = 5;
    }

    @StyleableRes
    static final int[] TRANSITION_MANAGER = {
            android.R.attr.fromScene,
            android.R.attr.toScene,
            android.R.attr.transition,
    };

    interface TransitionManager {
        @StyleableRes
        int FROM_SCENE = 0;
        @StyleableRes
        int TO_SCENE = 1;
        @StyleableRes
        int TRANSITION = 2;
    }

    @StyleableRes
    static final int[] TRANSITION = {
            android.R.attr.interpolator,
            android.R.attr.duration,
            android.R.attr.startDelay,
            android.R.attr.matchOrder,
    };

    interface Transition {
        @StyleableRes
        int INTERPOLATOR = 0;
        @StyleableRes
        int DURATION = 1;
        @StyleableRes
        int START_DELAY = 2;
        @StyleableRes
        int MATCH_ORDER = 3;
    }

    @StyleableRes
    static final int[] CHANGE_BOUNDS = {
            android.R.attr.resizeClip,
    };

    interface ChangeBounds {
        @StyleableRes
        int RESIZE_CLIP = 0;
    }

    @StyleableRes
    static final int[] VISIBILITY_TRANSITION = {
            android.R.attr.transitionVisibilityMode,
    };

    interface VisibilityTransition {
        @StyleableRes
        int TRANSITION_VISIBILITY_MODE = 0;
    }

    @StyleableRes
    static final int[] FADE = {
            android.R.attr.fadingMode,
    };

    interface Fade {
        @StyleableRes
        int FADING_MODE = 0;
    }

    @StyleableRes
    static final int[] CHANGE_TRANSFORM = {
            android.R.attr.reparent,
            android.R.attr.reparentWithOverlay,
    };

    interface ChangeTransform {
        @StyleableRes
        int REPARENT = 0;
        @StyleableRes
        int REPARENT_WITH_OVERLAY = 1;
    }

    @StyleableRes
    static final int[] SLIDE = {
            android.R.attr.slideEdge,
    };

    interface Slide {
        @StyleableRes
        int SLIDE_EDGE = 0;
    }

    @StyleableRes
    static final int[] TRANSITION_SET = {
            android.R.attr.transitionOrdering,
    };

    interface TransitionSet {
        @StyleableRes
        int TRANSITION_ORDERING = 0;
    }

    @StyleableRes
    static final int[] ARC_MOTION = {
            android.R.attr.minimumHorizontalAngle,
            android.R.attr.minimumVerticalAngle,
            android.R.attr.maximumAngle,
    };

    interface ArcMotion {
        @StyleableRes
        int MINIMUM_HORIZONTAL_ANGLE = 0;
        @StyleableRes
        int MINIMUM_VERTICAL_ANGLE = 1;
        @StyleableRes
        int MAXIMUM_ANGLE = 2;
    }

    @StyleableRes
    static final int[] PATTERN_PATH_MOTION = {
            android.R.attr.patternPathData,
    };

    interface PatternPathMotion {
        @StyleableRes
        int PATTERN_PATH_DATA = 0;
    }

    private Styleable() {
    }
}
