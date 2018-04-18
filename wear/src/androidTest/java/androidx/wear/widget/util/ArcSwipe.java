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

package androidx.wear.widget.util;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.os.SystemClock;
import android.support.test.espresso.UiController;
import android.support.test.espresso.action.MotionEvents;
import android.support.test.espresso.action.Swiper;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

/**
 * Swiper for gestures meant to be performed on an arc - part of a circle - not a straight line.
 * This class assumes a square bounding box with the radius of the circle being half the height of
 * the box.
 */
public class ArcSwipe implements Swiper {

    /** Enum describing the exact gesture which will perform the curved swipe. */
    public enum Gesture {
        /** Swipes quickly between the co-ordinates, clockwise. */
        FAST_CLOCKWISE(SWIPE_FAST_DURATION_MS, true),
        /** Swipes deliberately slowly between the co-ordinates, clockwise. */
        SLOW_CLOCKWISE(SWIPE_SLOW_DURATION_MS, true),
        /** Swipes quickly between the co-ordinates, anticlockwise. */
        FAST_ANTICLOCKWISE(SWIPE_FAST_DURATION_MS, false),
        /** Swipes deliberately slowly between the co-ordinates, anticlockwise. */
        SLOW_ANTICLOCKWISE(SWIPE_SLOW_DURATION_MS, false);

        private final int mDuration;
        private final boolean mClockwise;

        Gesture(int duration, boolean clockwise) {
            mDuration = duration;
            mClockwise = clockwise;
        }
    }

    /** The number of motion events to send for each swipe. */
    private static final int SWIPE_EVENT_COUNT = 10;

    /** Length of time a "fast" swipe should last for, in milliseconds. */
    private static final int SWIPE_FAST_DURATION_MS = 100;

    /** Length of time a "slow" swipe should last for, in milliseconds. */
    private static final int SWIPE_SLOW_DURATION_MS = 1500;

    private static final String TAG = ArcSwipe.class.getSimpleName();
    private final RectF mBounds;
    private final Gesture mGesture;

    public ArcSwipe(Gesture gesture, RectF bounds) {
        Preconditions.checkArgument(bounds.height() == bounds.width());
        mGesture = gesture;
        mBounds = bounds;
    }

    @Override
    public Swiper.Status sendSwipe(
            UiController uiController,
            float[] startCoordinates,
            float[] endCoordinates,
            float[] precision) {
        return sendArcSwipe(
                uiController,
                startCoordinates,
                endCoordinates,
                precision,
                mGesture.mDuration,
                mGesture.mClockwise);
    }

    private float[][] interpolate(float[] start, float[] end, int steps, boolean isClockwise) {
        float startAngle = getAngle(start[0], start[1]);
        float endAngle = getAngle(end[0], end[1]);

        Path path = new Path();
        PathMeasure pathMeasure = new PathMeasure();
        path.moveTo(start[0], start[1]);
        path.arcTo(mBounds, startAngle, getSweepAngle(startAngle, endAngle, isClockwise));
        pathMeasure.setPath(path, false);
        float pathLength = pathMeasure.getLength();

        float[][] res = new float[steps][2];
        float[] mPathTangent = new float[2];

        for (int i = 1; i < steps + 1; i++) {
            pathMeasure.getPosTan((pathLength * i) / (steps + 2f), res[i - 1], mPathTangent);
        }

        return res;
    }

    private Swiper.Status sendArcSwipe(
            UiController uiController,
            float[] startCoordinates,
            float[] endCoordinates,
            float[] precision,
            int duration,
            boolean isClockwise) {

        float[][] steps = interpolate(startCoordinates, endCoordinates, SWIPE_EVENT_COUNT,
                isClockwise);
        final int delayBetweenMovements = duration / steps.length;

        MotionEvent downEvent = MotionEvents.sendDown(uiController, startCoordinates,
                precision).down;
        try {
            for (int i = 0; i < steps.length; i++) {
                if (!MotionEvents.sendMovement(uiController, downEvent, steps[i])) {
                    Log.e(TAG,
                            "Injection of move event as part of the swipe failed. Sending cancel "
                                    + "event.");
                    MotionEvents.sendCancel(uiController, downEvent);
                    return Swiper.Status.FAILURE;
                }

                long desiredTime = downEvent.getDownTime() + delayBetweenMovements * i;
                long timeUntilDesired = desiredTime - SystemClock.uptimeMillis();
                if (timeUntilDesired > 10) {
                    uiController.loopMainThreadForAtLeast(timeUntilDesired);
                }
            }

            if (!MotionEvents.sendUp(uiController, downEvent, endCoordinates)) {
                Log.e(TAG,
                        "Injection of up event as part of the swipe failed. Sending cancel event.");
                MotionEvents.sendCancel(uiController, downEvent);
                return Swiper.Status.FAILURE;
            }
        } finally {
            downEvent.recycle();
        }
        return Swiper.Status.SUCCESS;
    }

    @VisibleForTesting
    float getAngle(double x, double y) {
        double relativeX = x - (mBounds.width() / 2);
        double relativeY = y - (mBounds.height() / 2);
        double rowAngle = Math.atan2(relativeX, relativeY);
        double angle = -Math.toDegrees(rowAngle) - 180;
        if (angle < 0) {
            angle += 360;
        }
        return (float) angle;
    }

    @VisibleForTesting
    float getSweepAngle(float startAngle, float endAngle, boolean isClockwise) {
        float sweepAngle = endAngle - startAngle;
        if (sweepAngle < 0) {
            sweepAngle += 360;
        }
        return isClockwise ? sweepAngle : (360 - sweepAngle);
    }
}
