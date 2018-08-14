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

package androidx.core.widget;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class NestedScrollViewTestUtils {

    public static int[] getTargetFlingVelocityTimeAndDistance(Context context) {
        ViewConfiguration configuration =
                ViewConfiguration.get(context);
        int touchSlop = configuration.getScaledTouchSlop();
        int mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        int mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        int targetVelocitySeconds = ((mMaximumVelocity - mMinimumVelocity) / 2) + mMinimumVelocity;
        int targetDistanceTraveled = touchSlop * 2;
        int targetTimePassed = (targetDistanceTraveled * 1000) / targetVelocitySeconds;

        return new int[]{targetVelocitySeconds, targetTimePassed, targetDistanceTraveled};
    }

    public static MotionEvent[] generateMotionEvents(int[] targetFlingVelocityTimeAndDistance) {
        int targetTimePassed = targetFlingVelocityTimeAndDistance[1];
        int targetDistanceTraveled = targetFlingVelocityTimeAndDistance[2];
        targetDistanceTraveled *= -1;

        MotionEvent down = MotionEvent.obtain(
                0,
                0,
                MotionEvent.ACTION_DOWN,
                500,
                500,
                0);
        MotionEvent move = MotionEvent.obtain(
                0,
                targetTimePassed,
                MotionEvent.ACTION_MOVE,
                500,
                500 + targetDistanceTraveled,
                0);
        MotionEvent up = MotionEvent.obtain(
                0,
                targetTimePassed,
                MotionEvent.ACTION_UP,
                500,
                500 + targetDistanceTraveled,
                0);

        return new MotionEvent[]{down, move, up};
    }

    public static void dispatchMotionEventsToView(View view, MotionEvent[] motionEvents) {
        for (MotionEvent motionEvent : motionEvents) {
            view.dispatchTouchEvent(motionEvent);
        }
    }

    public static void simulateFlingDown(Context context, View view) {
        int[] targetFlingTimeAndDistance =
                NestedScrollViewTestUtils.getTargetFlingVelocityTimeAndDistance(context);
        MotionEvent[] motionEvents =
                NestedScrollViewTestUtils.generateMotionEvents(targetFlingTimeAndDistance);
        NestedScrollViewTestUtils.dispatchMotionEventsToView(view, motionEvents);
    }
}
