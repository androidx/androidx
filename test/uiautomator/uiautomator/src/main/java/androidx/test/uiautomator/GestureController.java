/*
 * Copyright (C) 2014 The Android Open Source Project
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

package androidx.test.uiautomator;

import android.app.Service;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * The {@link GestureController} provides methods for performing high-level {@link PointerGesture}s.
 */
class GestureController {
    private static final String TAG = "GestureController";

    private static final long MOTION_EVENT_INJECTION_DELAY_MILLIS = 8; // 120Hz touch report rate

    // Singleton instance
    private static GestureController sInstance;

    // @TestApi method to set display id.
    private static Method sMotionEvent_setDisplayId;

    static {
        try {
            sMotionEvent_setDisplayId =
                MotionEvent.class.getMethod("setDisplayId", int.class);
        } catch (Exception e) {
            Log.i(TAG, "can't find MotionEvent#setDisplayId(int)", e);
        }
    }

    private final UiDevice mDevice;

    private final DisplayManager mDisplayManager;

    /** Comparator for sorting PointerGestures by start times. */
    private static final Comparator<PointerGesture> START_TIME_COMPARATOR =
            (o1, o2) -> Long.compare(o1.delay(), o2.delay());

    /** Comparator for sorting PointerGestures by end times. */
    private static final Comparator<PointerGesture> END_TIME_COMPARATOR =
            (o1, o2) -> Long.compare(o1.delay() + o1.duration(), o2.delay() + o2.duration());


    // Private constructor.
    private GestureController(UiDevice device) {
        mDevice = device;
        mDisplayManager = (DisplayManager) mDevice.getInstrumentation().getContext()
                .getSystemService(Service.DISPLAY_SERVICE);
    }

    /** Returns the {@link GestureController} instance for the given {@link UiDevice}. */
    public static GestureController getInstance(UiDevice device) {
        if (sInstance == null) {
            sInstance = new GestureController(device);
        }

        return sInstance;
    }

    /**
     * Performs the given gesture and waits for the {@code condition} to be met.
     *
     * @param condition The {@link EventCondition} to wait for.
     * @param timeout Maximum amount of time to wait in milliseconds.
     * @param gestures One or more {@link PointerGesture}s which define the gesture to be performed.
     * @return The final result returned by the condition.
     */
    public <U> U performGestureAndWait(EventCondition<U> condition, long timeout,
            PointerGesture ... gestures) {

        return getDevice().performActionAndWait(new GestureRunnable(gestures), condition, timeout);
    }

    /**
     * Performs the given gesture as represented by the given {@link PointerGesture}s.
     *
     * Each {@link PointerGesture} represents the actions of a single pointer from the time when it
     * is first touched down until the pointer is released. To perform the gesture, this method
     * tracks the locations of each pointer and injects {@link MotionEvent}s as appropriate.
     *
     * @param gestures One or more {@link PointerGesture}s which define the gesture to be performed.
     */
    public void performGesture(PointerGesture ... gestures) {
        // Initialize pointers
        int count = 0;
        Map<PointerGesture, Pointer> pointers = new HashMap<>();
        for (PointerGesture g : gestures) {
            pointers.put(g, new Pointer(count++, g.start()));
        }

        // Initialize MotionEvent arrays
        List<PointerProperties> properties = new ArrayList<>();
        List<PointerCoords>     coordinates = new ArrayList<>();

        // Track active and pending gestures
        PriorityQueue<PointerGesture> active = new PriorityQueue<>(gestures.length,
                END_TIME_COMPARATOR);
        PriorityQueue<PointerGesture> pending = new PriorityQueue<>(gestures.length,
                START_TIME_COMPARATOR);
        pending.addAll(Arrays.asList(gestures));

        // Record the start time
        final long startTime = SystemClock.uptimeMillis();

        // Update motion event delay to twice of the display refresh rate
        long injectionDelay = MOTION_EVENT_INJECTION_DELAY_MILLIS;
        try {
            int displayId = pending.peek().displayId();
            Display display = mDisplayManager.getDisplay(displayId);
            float displayRefreshRate = display.getRefreshRate();
            injectionDelay = (long) (500 / displayRefreshRate);
        } catch (Exception e) {
            Log.e(TAG, "Fail to update motion event delay", e);
        }

        // Loop
        MotionEvent event;
        long elapsedTime = 0;
        for (; !pending.isEmpty() || !active.isEmpty(); elapsedTime += injectionDelay) {

            // Touch up any completed pointers
            while (!active.isEmpty()
                    && elapsedTime > active.peek().delay() + active.peek().duration()) {

                PointerGesture gesture = active.remove();
                Pointer pointer = pointers.get(gesture);

                // Update pointer positions
                pointer.updatePosition(gesture.end());
                for (PointerGesture current : active) {
                    pointers.get(current).updatePosition(current.pointAt(elapsedTime));
                }

                int action = MotionEvent.ACTION_UP;
                int index = properties.indexOf(pointer.prop);
                if (!active.isEmpty()) {
                    action = MotionEvent.ACTION_POINTER_UP
                            + (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
                }
                event = getMotionEvent(startTime, SystemClock.uptimeMillis(), action, properties,
                        coordinates, gesture.displayId());
                getDevice().getUiAutomation().injectInputEvent(event, false);

                properties.remove(index);
                coordinates.remove(index);
            }

            // Move any active pointers
            for (PointerGesture gesture : active) {
                Pointer pointer = pointers.get(gesture);
                pointer.updatePosition(gesture.pointAt(elapsedTime - gesture.delay()));

            }
            if (!active.isEmpty()) {
                event = getMotionEvent(startTime, SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_MOVE, properties, coordinates,
                        active.peek().displayId());
                getDevice().getUiAutomation().injectInputEvent(event, false);
            }

            // Touchdown any new pointers
            while (!pending.isEmpty() && elapsedTime >= pending.peek().delay()) {
                PointerGesture gesture = pending.remove();
                Pointer pointer = pointers.get(gesture);

                // Add the pointer to the MotionEvent arrays
                properties.add(pointer.prop);
                coordinates.add(pointer.coords);

                // Touch down
                int action = MotionEvent.ACTION_DOWN;
                if (!active.isEmpty()) {
                    // Use ACTION_POINTER_DOWN for secondary pointers. The index is stored at
                    // ACTION_POINTER_INDEX_SHIFT.
                    action = MotionEvent.ACTION_POINTER_DOWN
                            + ((properties.size() - 1) << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
                }
                event = getMotionEvent(startTime, SystemClock.uptimeMillis(), action, properties,
                        coordinates, gesture.displayId());
                getDevice().getUiAutomation().injectInputEvent(event, false);

                // Move the PointerGesture to the active list
                active.add(gesture);
            }

            SystemClock.sleep(injectionDelay);
        }

        long upTime = SystemClock.uptimeMillis() - startTime;
        if (upTime >= 2 * elapsedTime) {
            Log.w(TAG, String.format("Gestures took longer than expected (%dms >> %dms), device "
                    + "might be in a busy state.", upTime, elapsedTime));
        }
    }

    /** Helper function to obtain a MotionEvent. */
    private static MotionEvent getMotionEvent(long downTime, long eventTime, int action,
            List<PointerProperties> properties, List<PointerCoords> coordinates, int displayId) {

        PointerProperties[] props = properties.toArray(new PointerProperties[0]);
        PointerCoords[] coords = coordinates.toArray(new PointerCoords[0]);
        final MotionEvent ev = MotionEvent.obtain(
                downTime, eventTime, action, props.length, props, coords,
                0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        if (displayId != Display.DEFAULT_DISPLAY) {
            if (sMotionEvent_setDisplayId == null) {
                Log.e(TAG, "Action on display " + displayId + " requested, "
                      + "but can't inject MotionEvent to display " + displayId);
            } else {
                try {
                    sMotionEvent_setDisplayId.invoke(ev, displayId);
                } catch (Exception e) {
                    Log.e(TAG, "Action on display " + displayId + " requested, "
                          + "but can't invoke MotionEvent#setDisplayId(int)", e);
                }
            }
        }
        return ev;
    }

    /** Helper class which tracks an individual pointer as part of a MotionEvent. */
    private static class Pointer {
        final PointerProperties prop;
        final PointerCoords coords;

        public Pointer(int id, Point point) {
            prop = new PointerProperties();
            prop.id = id;
            prop.toolType = Configurator.getInstance().getToolType();
            coords = new PointerCoords();
            coords.pressure = 1;
            coords.size = 1;
            coords.x = point.x;
            coords.y = point.y;
        }

        public void updatePosition(Point point) {
            coords.x = point.x;
            coords.y = point.y;
        }

        @Override
        public String toString() {
            return "Pointer " + prop.id + " {" + coords.x + " " + coords.y + "}";
        }
    }

    /** Runnable wrapper around a {@link GestureController#performGesture} call. */
    private class GestureRunnable implements Runnable {
        private final PointerGesture[] mGestures;

        public GestureRunnable(PointerGesture[] gestures) {
            mGestures = gestures;
        }

        @Override
        public void run() {
            performGesture(mGestures);
        }
    }

    UiDevice getDevice() {
        return mDevice;
    }
}
