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

package androidx.viewpager2.widget.swipe;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.espresso.PerformException;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs a swipe on a view from the center of that view to on of its edges.
 *
 * Obtain a new instance of this class for each swipe you want to perform, with one of the {@link
 * #swipeLeft() swipe methods}. Inject the motion events by calling {@link #perform(Instrumentation,
 * View)}.
 */
public class ManualSwipeInjector {

    private static final int X = 0;
    private static final int Y = 1;

    private static final int SWIPE_TIME = 100;
    private static final int STEPS = 10;

    private final CoordinatesProvider mStartCoordinatesProvider;
    private final CoordinatesProvider mEndCoordinatesProviders;
    private final int mDuration;
    private final int mSteps;

    private ManualSwipeInjector(CoordinatesProvider startCoordinatesProvider,
            CoordinatesProvider endCoordinatesProvider, int duration, int steps) {
        mStartCoordinatesProvider = startCoordinatesProvider;
        mEndCoordinatesProviders = endCoordinatesProvider;
        mDuration = duration;
        mSteps = steps;
    }

    /**
     * Swipe from the center of a view to its left side.
     */
    public static ManualSwipeInjector swipeLeft() {
        return new ManualSwipeInjector(GeneralLocation.CENTER,
                GeneralLocation.CENTER_LEFT, SWIPE_TIME, STEPS);
    }

    /**
     * Swipe from the center of a view to its right side.
     */
    public static ManualSwipeInjector swipeRight() {
        return new ManualSwipeInjector(GeneralLocation.CENTER,
                GeneralLocation.CENTER_RIGHT, SWIPE_TIME, STEPS);
    }

    /**
     * Swipe from the center of a view to its top side.
     */
    public static ManualSwipeInjector swipeUp() {
        return new ManualSwipeInjector(GeneralLocation.CENTER,
                GeneralLocation.TOP_CENTER, SWIPE_TIME, STEPS);
    }

    /**
     * Swipe from the center of a view to its bottom side.
     */
    public static ManualSwipeInjector swipeDown() {
        return new ManualSwipeInjector(GeneralLocation.CENTER,
                GeneralLocation.BOTTOM_CENTER, SWIPE_TIME, STEPS);
    }

    /**
     * Perform the swipe on the given view by generating and injecting the appropriate motion events
     * into the Instrumentation instance.
     */
    public void perform(Instrumentation instr, View view) {
        float[] swipeStart = mStartCoordinatesProvider.calculateCoordinates(view);
        float[] swipeEnd = mEndCoordinatesProviders.calculateCoordinates(view);
        sendSwipe(instr, swipeStart, swipeEnd, mDuration, mSteps);
    }

    /**
     * Inject motion events to emulate a swipe to the target location.
     *
     * @param instr The controller to inject the motion events with
     * @param from The pointer location where we start the swipe
     * @param to The pointer location where we end the swipe
     * @param duration The duration in milliseconds of the swipe gesture
     * @param steps The number of move motion events that will be sent for the gesture
     */
    private void sendSwipe(Instrumentation instr, float[] from, float[] to, int duration,
            int steps) {
        float[][] coords = interpolate(from, to, steps);
        long startTime = SystemClock.uptimeMillis();

        List<MotionEvent> events = new ArrayList<>();
        try {
            injectMotionEvent(instr, obtainDownEvent(startTime, coords[0]), events);
            for (int i = 1; i <= steps; i++) {
                injectMotionEvent(instr, obtainMoveEvent(startTime, duration * i / steps,
                        coords[i]), events);
            }
            injectMotionEvent(instr, obtainUpEvent(startTime, duration, coords[coords.length - 1]),
                    events);
        } catch (Exception e) {
            throw new PerformException.Builder().withCause(e).build();
        } finally {
            for (MotionEvent event : events) {
                event.recycle();
            }
        }
    }

    private static MotionEvent obtainDownEvent(long time, float[] coord) {
        return MotionEvent.obtain(time, time,
                MotionEvent.ACTION_DOWN, coord[X], coord[Y], 0);
    }

    private static MotionEvent obtainMoveEvent(long startTime, long elapsedTime, float[] coord) {
        return MotionEvent.obtain(startTime, startTime + elapsedTime,
                MotionEvent.ACTION_MOVE, coord[X], coord[Y], 0);
    }

    private static MotionEvent obtainUpEvent(long startTime, long elapsedTime, float[] coord) {
        return MotionEvent.obtain(startTime, startTime + elapsedTime,
                MotionEvent.ACTION_UP, coord[X], coord[Y], 0);
    }

    private static void injectMotionEvent(Instrumentation instrumentation, MotionEvent event,
            List<MotionEvent> events) {
        events.add(event);
        long eventTime = event.getEventTime();
        long now = SystemClock.uptimeMillis();
        if (eventTime - now > 10) {
            SystemClock.sleep(eventTime - now - 10);
        }
        instrumentation.sendPointerSync(event);
    }

    private static float[][] interpolate(float[] from, float[] to, int steps) {
        float[][] coords = new float[steps + 1][2];
        coords[0][X] = from[X];
        coords[0][Y] = from[Y];
        for (int i = 1; i <= steps; i++) {
            lerp(from, to, (float) i / steps, coords[i]);
        }
        return coords;
    }

    private static void lerp(float[] from, float[] to, float f, float[] out) {
        out[X] = (int) (from[X] + (to[X] - from[X]) * f);
        out[Y] = (int) (from[Y] + (to[Y] - from[Y]) * f);
    }
}
