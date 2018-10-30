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

package androidx.testutils;

import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.espresso.InjectEventSecurityException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.CoordinatesProvider;

import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Swipes the view on which the action is performed to the given top-left coordinates. It is
 * required that the view moves along with the swipe, as it would list views (e.g., a RecyclerView).
 *
 * Provides two different ways to provide the target coordinates: either the center of the view's
 * parent ({@link #swipeToCenter()} and {@link #flingToCenter()}), or fixed coordinates ({@link
 * #swipeTo(float[])} and {@link #flingTo(float[])})
 */
public class SwipeToLocation implements ViewAction {

    private static CoordinatesProvider sCenterInParent = new CoordinatesProvider() {
        @Override
        public float[] calculateCoordinates(View view) {
            View parent = (View) view.getParent();

            int horizontalPadding = parent.getPaddingLeft() + parent.getPaddingRight();
            int verticalPadding = parent.getPaddingTop() + parent.getPaddingBottom();
            int widthParent = parent.getWidth() - horizontalPadding;
            int heightParent = parent.getHeight() - verticalPadding;
            int widthView = view.getWidth();
            int heightView = view.getHeight();

            float[] coords = new float[2];
            coords[X] = (widthParent - widthView) / 2;
            coords[Y] = (heightParent - heightView) / 2;
            return coords;
        }

        @NonNull
        @Override
        public String toString() {
            return "center in parent";
        }
    };

    private static class FixedCoordinates implements CoordinatesProvider {
        private final float[] mCoordinates;

        FixedCoordinates(float[] coordinates) {
            mCoordinates = coordinates;
        }

        @Override
        public float[] calculateCoordinates(View view) {
            return mCoordinates;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("fixed coordinates (%f, %f)", mCoordinates[X], mCoordinates[Y]);
        }
    }

    private static final int X = 0;
    private static final int Y = 1;

    private final CoordinatesProvider mCoordinatesProvider;
    private final int mDuration;
    private final int mSteps;

    private SwipeToLocation(CoordinatesProvider coordinatesProvider, int duration, int steps) {
        mCoordinatesProvider = coordinatesProvider;
        mDuration = duration;
        mSteps = steps;
    }

    /**
     * Swipe the view to the given target location. Swiping takes 1 second to complete.
     *
     * @param targetLocation The top-left target coordinates of the view
     * @return The ViewAction to use in {@link
     * androidx.test.espresso.ViewInteraction#perform(ViewAction...)}
     */
    public static SwipeToLocation swipeTo(float[] targetLocation) {
        return new SwipeToLocation(new FixedCoordinates(targetLocation), 1000, 10);
    }

    /**
     * Fling the view to the given target location. Flinging takes 0.1 seconds to complete.
     *
     * @param targetLocation The top-left target coordinates of the view
     * @return The ViewAction to use in {@link
     * androidx.test.espresso.ViewInteraction#perform(ViewAction...)}
     */
    public static SwipeToLocation flingTo(float[] targetLocation) {
        return new SwipeToLocation(new FixedCoordinates(targetLocation), 100, 10);
    }

    /**
     * Swipe the view to the center of its parent. Swiping takes 1 second to complete.
     *
     * @return The ViewAction to use in {@link
     * androidx.test.espresso.ViewInteraction#perform(ViewAction...)}
     */
    public static SwipeToLocation swipeToCenter() {
        return new SwipeToLocation(sCenterInParent, 1000, 10);
    }

    /**
     * Fling the view to the center of its parent. Flinging takes 0.1 seconds to complete.
     *
     * @return The ViewAction to use in {@link
     * androidx.test.espresso.ViewInteraction#perform(ViewAction...)}
     */
    public static SwipeToLocation flingToCenter() {
        return new SwipeToLocation(sCenterInParent, 100, 10);
    }

    @Override
    public Matcher<View> getConstraints() {
        return isDisplayingAtLeast(10);
    }

    @Override
    public String getDescription() {
        return String.format(Locale.US, "Swiping view to location %s", mCoordinatesProvider);
    }

    @Override
    public void perform(UiController uiController, View view) {
        float[] swipeStart = getCenterOfView(view);
        float[] targetCoordinates = mCoordinatesProvider.calculateCoordinates(view);
        sendOnlineSwipe(uiController, view, targetCoordinates, swipeStart, mDuration, mSteps);
    }

    /**
     * Inject motion events to emulate a swipe to the target location. Instead of calculating all
     * events up front and then injecting them one by one, perform the required number of steps and
     * determine the distance to cover in the current step based on the current distance of the view
     * to the target. This makes it robust against movements of the view during the event sequence.
     * This is for example likely to happen between the down event and the first move event if we're
     * interrupting a smooth scroll.
     *
     * @param uiController The controller to inject the motion events with
     * @param view The view to swipe on
     * @param targetViewLocation The view location where we want the view to end
     * @param swipeStart The pointer location where we start the swipe, must be on the view
     * @param duration The duration in milliseconds of the swipe gesture
     * @param steps The number of move motion events that will be sent for the gesture
     */
    private void sendOnlineSwipe(UiController uiController, View view, float[] targetViewLocation,
            float[] swipeStart, int duration, int steps) {
        final long startTime = SystemClock.uptimeMillis();
        long eventTime = startTime;
        float[] pointerLocation = new float[]{swipeStart[X], swipeStart[Y]};
        float[] viewLocation = new float[2];
        float[] nextViewLocation = new float[2];
        List<MotionEvent> events = new ArrayList<>();
        try {
            // Down event
            MotionEvent downEvent = obtainDownEvent(startTime, pointerLocation);
            events.add(downEvent);
            injectMotionEvent(uiController, downEvent);

            // Move events
            for (int i = 1; i <= steps; i++) {
                eventTime = startTime + duration * i / duration;
                getCurrentCoords(view, viewLocation);
                lerp(viewLocation, targetViewLocation, 1f / (steps - i + 1), nextViewLocation);
                updatePointerLocation(pointerLocation, viewLocation, nextViewLocation);

                MotionEvent moveEvent = obtainMoveEvent(startTime, eventTime, pointerLocation);
                events.add(moveEvent);
                injectMotionEvent(uiController, moveEvent);
            }

            // Up event
            MotionEvent upEvent = obtainUpEvent(startTime, eventTime, pointerLocation);
            events.add(upEvent);
            injectMotionEvent(uiController, upEvent);
        } catch (Exception e) {
            throw new PerformException.Builder().withActionDescription("Perform swipe")
                    .withViewDescription("unknown").withCause(e).build();
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
        return MotionEvent.obtain(startTime, elapsedTime,
                MotionEvent.ACTION_MOVE, coord[X], coord[Y], 0);
    }

    private static MotionEvent obtainUpEvent(long startTime, long elapsedTime, float[] coord) {
        return MotionEvent.obtain(startTime, elapsedTime,
                MotionEvent.ACTION_UP, coord[X], coord[Y], 0);
    }

    private static void injectMotionEvent(UiController uiController, MotionEvent event)
            throws InjectEventSecurityException {
        while (event.getEventTime() - SystemClock.uptimeMillis() > 10) {
            // Because the loopMainThreadForAtLeast is overkill for waiting, intentionally only
            // call it with a smaller amount of milliseconds as best effort
            uiController.loopMainThreadForAtLeast(10);
        }
        uiController.injectMotionEvent(event);
    }

    private void updatePointerLocation(float[] pointerLocation, float[] viewLocation,
            float[] nextViewLocation) {
        pointerLocation[X] += nextViewLocation[X] - viewLocation[X];
        pointerLocation[Y] += nextViewLocation[Y] - viewLocation[Y];
    }

    private static float[] getCenterOfView(View view) {
        Rect r = new Rect();
        view.getGlobalVisibleRect(r);
        return new float[]{r.centerX(), r.centerY()};
    }

    private static void getCurrentCoords(View view, float[] out) {
        out[X] = view.getLeft();
        out[Y] = view.getTop();
    }

    private static void lerp(float[] from, float[] to, float f, float[] out) {
        out[X] = (int) (from[X] + (to[X] - from[X]) * f);
        out[Y] = (int) (from[Y] + (to[Y] - from[Y]) * f);
    }
}
