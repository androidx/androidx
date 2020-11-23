/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.provider.Settings;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Helper for getting the system standard animations for a full-screen window or activity.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ActivityAnimationUtil{

    /**
     * Activity animation types
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({OPEN_ENTER, OPEN_EXIT, CLOSE_ENTER, CLOSE_EXIT})
    public @interface ActivityAnimationType {
    }

    /**
     * The animation that is run on the next activity (which is entering the screen) when closing
     * the current activity.
     */
    public static final int CLOSE_ENTER = 0;

    /**
     * The animation that is run on the current activity (which is existing the screen) when
     * closing the current activity.
     */
    public static final int CLOSE_EXIT = 1;

    /**
     * The animation that is run on the next activity (which is entering the screen) when opening
     * a new activity.
     */
    public static final int OPEN_ENTER = 2;

    /**
     * The animation that is run on the previous activity (which is exiting the screen) when
     * opening a new activity.
     */
    public static final int OPEN_EXIT = 3;

    private static final int [] ACTIVITY_ANIMATION_ATTRS = new int [] {
            android.R.attr.activityCloseEnterAnimation,
            android.R.attr.activityCloseExitAnimation,
            android.R.attr.activityOpenEnterAnimation,
            android.R.attr.activityOpenExitAnimation
    };

    private ActivityAnimationUtil() {}

    /**
     * Get the specified type of activity animation.
     * @param context the current context
     * @param animationType the animation type
     * @param scaled whether or not the scaling factor for activity transition animations set in
     *               the global settings are applied on the returned animation.
     * @return the specified type of activity animation
     */
    @Nullable
    public static Animation getStandardActivityAnimation(
            @NonNull Context context, @ActivityAnimationType int animationType, boolean scaled) {
        TypedArray animations = context.obtainStyledAttributes(
                android.R.style.Animation_Activity,
                new int [] {ACTIVITY_ANIMATION_ATTRS[animationType]});
        Animation animation = null;
        if (animations.getIndexCount() > 0) {
            animation = AnimationUtils.loadAnimation(context, animations.getResourceId(0, 0));
            if (scaled) {
                float scale = Settings.Global.getInt(
                        context.getContentResolver(),
                        Settings.Global.TRANSITION_ANIMATION_SCALE, 1);
                animation.scaleCurrentDuration(scale);
            }
        }
        animations.recycle();
        return animation;
    }
}
