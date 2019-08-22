/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.testutils;

import static android.os.SystemClock.uptimeMillis;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.espresso.action.CoordinatesProvider;

/**
 * Injects motion events for custom gestures using Instrumentation. Use this to "draw" the gestures,
 * in a similar way as a Path is defined. Create an instance of this class, start a drag, move it
 * around and then finish it. For example, this injects a "Swipe right then left" gesture from
 * TalkBack:
 *
 * <pre>
 *     View view = findViewById(R.id.view_to_swipe_on);
 *
 *     SwipeInjector swiper = new SwipeInjector(InstrumentationRegistry.getInstrumentation());
 *     swiper.startDrag(GeneralLocation.LEFT, view);      // Start at the left side of the view
 *     swiper.dragTo(GeneralLocation.RIGHT, view, 200);   // Swipe to the right in 200 ms
 *     swiper.dragTo(GeneralLocation.LEFT, view, 200);    // Swipe to the left in 200 ms
 *     swiper.finishDrag();                               // Finish the gesture
 * </pre>
 *
 * @see androidx.test.espresso.action.GeneralLocation GeneralLocation
 * @see TranslatedCoordinatesProvider
 */
public class SwipeInjector {

    private static final int X = 0;
    private static final int Y = 1;

    private Instrumentation mInstrumentation;

    private long mStartTime = 0;
    private long mCurrentTime = 0;
    private float[] mFloats = new float[2];

    /**
     * Creates an injector.
     */
    public SwipeInjector(@NonNull Instrumentation instrumentation) {
        mInstrumentation = instrumentation;
    }

    /**
     * Start a drag on the coordinate provided for the given view. For example,
     * {@code startDrag(GeneralLocation.CENTER, mView)}.
     *
     * @param provider provider of the coordinate of the pointer down event, like
     *        {@link androidx.test.espresso.action.GeneralLocation#CENTER GeneralLocation.CENTER}
     * @param view the view passed to the
     *        {@link CoordinatesProvider#calculateCoordinates(View) coordinates provider}
     */
    @SuppressLint("LambdaLast")
    public void startDrag(@NonNull CoordinatesProvider provider, @NonNull View view) {
        float[] floats = provider.calculateCoordinates(view);
        startDrag(floats[X], floats[Y]);
    }

    /**
     * Start a drag on the given coordinate. For example, {@code startDrag(100, 200)}. Note that the
     * coordinates are absolute coordinates for the screen.
     *
     * @param x the x coordinate of the pointer down event
     * @param y the y coordinate of the pointer down event
     */
    public void startDrag(float x, float y) {
        mStartTime = uptimeMillis();
        mFloats[X] = x;
        mFloats[Y] = y;
        injectMotionEvent(obtainDownEvent(mStartTime, mFloats));
    }

    /**
     * Extend the drag with a single motion event to the coordinates provided for the given view.
     * For example, {@code dragTo(GeneralLocation.LEFT, mView)}. Call one of the {@code startDrag}
     * methods before calling any of the {@code dragTo} methods.
     *
     * @param provider provider of the coordinate of the pointer move event, like
     *        {@link androidx.test.espresso.action.GeneralLocation#CENTER GeneralLocation.CENTER}
     * @param view the view passed to the
     *        {@link CoordinatesProvider#calculateCoordinates(View) coordinates provider}
     */
    @SuppressLint("LambdaLast")
    public void dragTo(@NonNull CoordinatesProvider provider, @NonNull View view) {
        float[] floats = provider.calculateCoordinates(view);
        dragTo(floats[X], floats[Y]);
    }

    /**
     * Extend the drag with a single motion event to the given coordinate. For example,
     * {@code dragTo(0, 10)}. Call one of the {@code startDrag} methods before calling any of the
     * {@code dragTo} methods. Note that the coordinates are absolute coordinates for the screen.
     *
     * @param x the x coordinate of the pointer move event
     * @param y the y coordinate of the pointer move event
     */
    public void dragTo(float x, float y) {
        mFloats[X] = x;
        mFloats[Y] = y;
        injectMotionEvent(obtainMoveEvent(mStartTime, uptimeMillis(), mFloats));
    }

    /**
     * Extend the drag with a range of motion events to the coordinate provided for the given view.
     * An event will be injected every 10ms, interpolating linearly to the destination. For example,
     * {@code dragTo(GeneralLocation.LEFT, mView, 300)}. Call one of the {@code startDrag} methods
     * before calling any of the {@code dragTo} methods.
     *
     * @param provider provider of the final coordinate of this part of the drag, like
     *        {@link androidx.test.espresso.action.GeneralLocation#CENTER GeneralLocation.CENTER}
     * @param view the view passed to the
     *        {@link CoordinatesProvider#calculateCoordinates(View) coordinates provider}
     * @param duration the time in milliseconds this part of the drag should take. Actual time will
     *        be different if not a multiple of 10.
     */
    @SuppressLint("LambdaLast")
    public void dragTo(@NonNull CoordinatesProvider provider, @NonNull View view, long duration) {
        float[] floats = provider.calculateCoordinates(view);
        dragTo(floats[X], floats[Y], duration);
    }

    /**
     * Extend the drag with a range of motion events to the given coordinate. An event will be
     * injected every 10ms, interpolating linearly to the destination. For example,
     * {@code dragTo(10, 0, 350)}. Call one of the {@code startDrag} methods before calling any of
     * the {@code dragTo} methods. Note that the coordinates are absolute coordinates for the
     * screen.
     *
     * @param x the final x coordinate of this part of the drag
     * @param y the final y coordinate of this part of the drag
     * @param duration the time in milliseconds this part of the drag should take. Actual time will
     *        be different if not a multiple of 10.
     */
    public void dragTo(float x, float y, long duration) {
        float x0 = mFloats[X];
        float y0 = mFloats[Y];
        float dx = x - x0;
        float dy = y - y0;
        int steps = Math.max(1, Math.round(duration / 10f));
        for (int i = 1; i <= steps; i++) {
            float progress = (float) i / steps;
            mFloats[X] = x0 + dx * progress;
            mFloats[Y] = y0 + dy * progress;
            injectMotionEvent(obtainMoveEvent(mStartTime, mCurrentTime + 10L, mFloats));
        }
    }

    /**
     * Extend the drag with a single motion event covering the given delta. For example,
     * {@code dragBy(10, -20)}. Call one of the {@code startDrag} methods before calling any of
     * the {@code dragBy} methods.
     *
     * @param dx the distance in x direction
     * @param dy the distance in y direction
     */
    public void dragBy(float dx, float dy) {
        dragTo(mFloats[X] + dx, mFloats[Y] + dy);
    }

    /**
     * Extend the drag with a range of motion events covering the given delta. An event will be
     * injected every 10ms, interpolating linearly to the destination. For example,
     * {@code dragBy(-50, 0, 100)}. Call one of the {@code startDrag} methods before calling any of
     * the {@code dragBy} methods.
     *
     * @param dx the distance in x direction
     * @param dy the distance in y direction
     * @param duration the time in milliseconds this part of the drag should take. Actual time will
     *        be different if not a multiple of 10.
     */
    public void dragBy(float dx, float dy, long duration) {
        dragTo(mFloats[X] + dx, mFloats[Y] + dy, duration);
    }

    /**
     * Finish the drag with a pointer up event. A new drag can be started after this with one of the
     * {@code startDrag} methods.
     */
    public void finishDrag() {
        injectMotionEvent(obtainUpEvent(mStartTime, uptimeMillis(), mFloats));
    }

    private static MotionEvent obtainDownEvent(long time, float[] coord) {
        return MotionEvent.obtain(time, time,
                MotionEvent.ACTION_DOWN, coord[X], coord[Y], 0);
    }

    private static MotionEvent obtainMoveEvent(long startTime, long time, float[] coord) {
        return MotionEvent.obtain(startTime, time,
                MotionEvent.ACTION_MOVE, coord[X], coord[Y], 0);
    }

    private static MotionEvent obtainUpEvent(long startTime, long time, float[] coord) {
        return MotionEvent.obtain(startTime, time,
                MotionEvent.ACTION_UP, coord[X], coord[Y], 0);
    }

    private void injectMotionEvent(MotionEvent event) {
        try {
            long eventTime = event.getEventTime();
            long now = uptimeMillis();
            if (eventTime - now > 0) {
                SystemClock.sleep(eventTime - now);
            }
            mInstrumentation.sendPointerSync(event);
            mCurrentTime = eventTime;
        } finally {
            event.recycle();
        }
    }
}
