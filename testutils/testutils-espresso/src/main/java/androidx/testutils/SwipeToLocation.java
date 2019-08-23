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

import android.app.Instrumentation;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
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
import java.util.concurrent.CountDownLatch;

/**
 * <p>A {@link ViewAction} that swipes the view on which the action is performed to the given
 * top-left coordinates. It is required that the view moves along with the swipe, as it would list
 * views (e.g., a RecyclerView). Can be instantiated and run independently of Espresso as well, by
 * {@link #initialize(View) initializing} and then {@link #perform(Instrumentation) performing} the
 * action.</p>
 *
 * <p>Provides two different ways to provide the target coordinates: either the center of the view's
 * parent ({@link #swipeToCenter()} and {@link #flingToCenter()}), or fixed coordinates ({@link
 * #swipeTo(float[])} and {@link #flingTo(float[])})</p>
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
            return String.format(Locale.US, "fixed coordinates (%f, %f)",
                    mCoordinates[X], mCoordinates[Y]);
        }
    }

    private static final int X = 0;
    private static final int Y = 1;

    private final CoordinatesProvider mCoordinatesProvider;
    private final int mDuration;
    private final int mSteps;

    // The view to move and to swipe on
    @SuppressWarnings("WeakerAccess") // package-protected to prevent synthetic access
    View mView;
    // The pointer location where we start the swipe, must be on the view
    private float[] mSwipeStart;
    // The view location where we want the view to end
    private float[] mTargetViewLocation;

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

    /**
     * Sets the action up to run on the given view.
     *
     * @param view The View that is moved and on which the swipe is performed
     */
    public void initialize(@NonNull View view) {
        this.mView = view;
        mSwipeStart = getCenterOfView(view);
        mTargetViewLocation = mCoordinatesProvider.calculateCoordinates(view);
    }

    @Override
    public void perform(UiController uiController, View view) {
        initialize(view);
        performWithMotionInjector(new UiFacadeWithUiController(uiController));
    }

    /**
     * Performs this action manually instead of as a ViewAction. Must not be called from the main
     * thread. Useful if performing the swipe as a ViewAction doesn't work because Espresso waits
     * until the main thread is idle while you actually want to execute it now.
     *
     * @param instrumentation The Instrumentation object used to inject MotionEvents
     */
    public void perform(Instrumentation instrumentation) {
        if (mView == null || mSwipeStart == null || mTargetViewLocation == null) {
            throwWith(new IllegalStateException("SwipeToLocation must be initialized with a View "
                    + "first. See SwipeToLocation.initialize(View view)"));
        }
        performWithMotionInjector(new UiFacadeWithInstrumentation(instrumentation));
    }

    private void performWithMotionInjector(UiFacade uiController) {
        sendOnlineSwipe(uiController, mDuration, mSteps);
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
     * @param duration The duration in milliseconds of the swipe gesture
     * @param steps The number of move motion events that will be sent for the gesture
     */
    private void sendOnlineSwipe(UiFacade uiController, int duration, int steps) {
        final long startTime = SystemClock.uptimeMillis();
        long eventTime = startTime;
        final float[] pointerLocation = new float[]{mSwipeStart[X], mSwipeStart[Y]};
        final float[] viewLocation = new float[2];
        final float[] nextViewLocation = new float[2];
        final List<MotionEvent> events = new ArrayList<>();
        final Runnable updateCoordinates = new Runnable() {
            @Override
            public void run() {
                // Update the view coordinates on the UI thread so the view is in a stable state
                getCurrentCoords(mView, viewLocation);
            }
        };
        try {
            // Down event
            MotionEvent downEvent = obtainDownEvent(startTime, pointerLocation);
            events.add(downEvent);
            injectMotionEvent(uiController, downEvent);

            // Move events
            for (int i = 1; i <= steps; i++) {
                eventTime = startTime + duration * i / duration;
                uiController.runOnUiThreadSync(updateCoordinates);
                lerp(viewLocation, mTargetViewLocation, 1f / (steps - i + 1), nextViewLocation);
                updatePointerLocation(pointerLocation, viewLocation, nextViewLocation);

                MotionEvent moveEvent = obtainMoveEvent(startTime, eventTime, pointerLocation);
                events.add(moveEvent);
                injectMotionEvent(uiController, moveEvent);
            }

            // Up event
            MotionEvent upEvent = obtainUpEvent(startTime, eventTime, pointerLocation);
            events.add(upEvent);
            injectMotionEvent(uiController, upEvent);
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

    private static void injectMotionEvent(UiFacade uiController, MotionEvent event) {
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

    @SuppressWarnings("WeakerAccess") // package-protected to prevent synthetic access
    static void getCurrentCoords(View view, float[] out) {
        out[X] = view.getLeft();
        out[Y] = view.getTop();
    }

    private static void lerp(float[] from, float[] to, float f, float[] out) {
        out[X] = (int) (from[X] + (to[X] - from[X]) * f);
        out[Y] = (int) (from[Y] + (to[Y] - from[Y]) * f);
    }

    @SuppressWarnings("WeakerAccess") // package-protected to prevent synthetic access
    static void throwWith(Throwable error) {
        throw new PerformException.Builder().withActionDescription("Perform swipe")
                .withViewDescription("unknown").withCause(error).build();
    }

    /**
     * An interface to inject events and interact with the UI thread. This allows us to use either
     * {@link UiController} when performed as a {@link ViewAction}, or use {@link Instrumentation}
     * when performing the swipe action manually.
     */
    private interface UiFacade {
        void injectMotionEvent(@NonNull MotionEvent event);

        void loopMainThreadForAtLeast(long millisDelay);

        void runOnUiThreadSync(@NonNull Runnable runnable);
    }

    /**
     * A {@link UiFacade} build from a {@link UiController}. Instantiated when {@link
     * SwipeToLocation#perform(UiController, View)} is executed by Espresso. As Espresso runs
     * perform() on the UI thread, all interactions with this implementation happen on the UI
     * thread.
     */
    private static class UiFacadeWithUiController implements UiFacade {
        private final UiController mUiController;

        UiFacadeWithUiController(UiController uiController) {
            mUiController = uiController;
        }

        @Override
        public void injectMotionEvent(@NonNull MotionEvent event) {
            try {
                mUiController.injectMotionEvent(event);
            } catch (InjectEventSecurityException e) {
                throwWith(e);
            }
        }

        @Override
        public void loopMainThreadForAtLeast(long millisDelay) {
            mUiController.loopMainThreadForAtLeast(millisDelay);
        }

        @Override
        public void runOnUiThreadSync(@NonNull Runnable runnable) {
            // We're already on the UI thread
            runnable.run();
        }
    }

    /**
     * A {@link UiFacade} build from a {@link Instrumentation}. Instantiated when {@link
     * SwipeToLocation#perform(Instrumentation)} is called manually. It is assumed that interactions
     * with this implementation happen from another thread than the UI thread.
     */
    private static class UiFacadeWithInstrumentation implements UiFacade {
        private final Instrumentation mInstrumentation;
        private final Handler mHandler;

        UiFacadeWithInstrumentation(Instrumentation instrumentation) {
            mInstrumentation = instrumentation;
            mHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void injectMotionEvent(@NonNull MotionEvent event) {
            mInstrumentation.sendPointerSync(event);
        }

        @Override
        public void loopMainThreadForAtLeast(long millisDelay) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw new IllegalStateException(UiFacadeWithInstrumentation.class.getSimpleName()
                        + " cannot loop the main thread from the main thread itself");
            }
            if (millisDelay > 0) {
                SystemClock.sleep(millisDelay);
            }
        }

        @Override
        public void runOnUiThreadSync(@NonNull final Runnable runnable) {
            final CountDownLatch latch = new CountDownLatch(1);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                throwWith(e);
            }
        }
    }
}
