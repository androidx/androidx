/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.app.UiAutomation;
import android.app.UiAutomation.AccessibilityEventFilter;
import android.graphics.Point;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * The InteractionProvider is responsible for injecting user events such as touch events
 * (includes swipes) and text key events into the system. To do so, all it needs to know about
 * are coordinates of the touch events and text for the text input events.
 * The InteractionController performs no synchronization. It will fire touch and text input events
 * as fast as it receives them. All idle synchronization is performed prior to querying the
 * hierarchy. See {@link QueryController}
 */
class InteractionController {

    private static final String TAG = InteractionController.class.getSimpleName();

    // Duration of a long press (with multiplier to ensure detection).
    private static final long LONG_PRESS_DURATION_MS =
            (long) (ViewConfiguration.getLongPressTimeout() * 1.5f);

    private final UiDevice mDevice;

    private static final long REGULAR_CLICK_LENGTH = 100;

    private long mDownTime;

    // Inserted after each motion event injection.
    private static final int MOTION_EVENT_INJECTION_DELAY_MILLIS = 5;

    private static final Map<Integer, Integer> KEY_MODIFIER = new HashMap<>();

    static {
        KEY_MODIFIER.put(KeyEvent.KEYCODE_SHIFT_LEFT,
                KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON);
        KEY_MODIFIER.put(KeyEvent.KEYCODE_SHIFT_RIGHT,
                KeyEvent.META_SHIFT_RIGHT_ON | KeyEvent.META_SHIFT_ON);
        KEY_MODIFIER.put(KeyEvent.KEYCODE_ALT_LEFT,
                KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_ON);
        KEY_MODIFIER.put(KeyEvent.KEYCODE_ALT_RIGHT,
                KeyEvent.META_ALT_RIGHT_ON | KeyEvent.META_ALT_ON);
        KEY_MODIFIER.put(KeyEvent.KEYCODE_SYM, KeyEvent.META_SYM_ON);
        KEY_MODIFIER.put(KeyEvent.KEYCODE_FUNCTION, KeyEvent.META_FUNCTION_ON);
        KEY_MODIFIER.put(KeyEvent.KEYCODE_CTRL_LEFT,
                KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_CTRL_ON);
        KEY_MODIFIER.put(KeyEvent.KEYCODE_CTRL_RIGHT,
                KeyEvent.META_CTRL_RIGHT_ON | KeyEvent.META_CTRL_ON);
        KEY_MODIFIER.put(KeyEvent.KEYCODE_META_LEFT, KeyEvent.META_META_LEFT_ON);
        KEY_MODIFIER.put(KeyEvent.KEYCODE_META_RIGHT, KeyEvent.META_META_RIGHT_ON);
        KEY_MODIFIER.put(KeyEvent.KEYCODE_CAPS_LOCK, KeyEvent.META_CAPS_LOCK_ON);
        KEY_MODIFIER.put(KeyEvent.KEYCODE_NUM_LOCK, KeyEvent.META_NUM_LOCK_ON);
        KEY_MODIFIER.put(KeyEvent.KEYCODE_SCROLL_LOCK, KeyEvent.META_SCROLL_LOCK_ON);
    }

    InteractionController(UiDevice device) {
        mDevice = device;
    }

    /**
     * Predicate for waiting for any of the events specified in the mask
     */
    static class WaitForAnyEventPredicate implements AccessibilityEventFilter {
        final int mMask;
        WaitForAnyEventPredicate(int mask) {
            mMask = mask;
        }
        @Override
        public boolean accept(AccessibilityEvent t) {
            // check current event in the list
            return (t.getEventType() & mMask) != 0;
        }
    }

    /**
     * Predicate for waiting for every event specified in the mask to be matched at least once
     */
    static class WaitForAllEventPredicate implements AccessibilityEventFilter {
        int mMask;
        WaitForAllEventPredicate(int mask) {
            mMask = mask;
        }

        @Override
        public boolean accept(AccessibilityEvent t) {
            // check current event in the list
            if ((t.getEventType() & mMask) != 0) {
                // remove from mask since this condition is satisfied
                mMask &= ~t.getEventType();

                // Since we're waiting for all events to be matched at least once
                return mMask == 0;
            }

            // no match yet
            return false;
        }
    }

    /**
     * Helper used by methods to perform actions and wait for any accessibility events and return
     * predicated on predefined filter.
     *
     * @param command
     * @param filter
     * @param timeout
     * @return
     */
    private AccessibilityEvent runAndWaitForEvents(Runnable command,
            AccessibilityEventFilter filter, long timeout) {

        try {
            return getUiAutomation().executeAndWaitForEvent(command, filter, timeout);
        } catch (TimeoutException e) {
            Log.w(TAG, String.format("Timed out waiting %dms for command and events.", timeout));
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception while waiting for command and events.", e);
            return null;
        }
    }

    /**
     * Send keys and blocks until the first specified accessibility event.
     *
     * Most key presses will cause some UI change to occur. If the device is busy, this will
     * block until the device begins to process the key press at which point the call returns
     * and normal wait for idle processing may begin. If no events are detected for the
     * timeout period specified, the call will return anyway with false.
     *
     * @param keyCode
     * @param metaState
     * @param eventType
     * @param timeout
     * @return true if events is received, otherwise false.
     */
    public boolean sendKeyAndWaitForEvent(final int keyCode, final int metaState,
            final int eventType, long timeout) {
        Runnable command = () -> {
            final long eventTime = SystemClock.uptimeMillis();
            KeyEvent downEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN,
                    keyCode, 0, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                    InputDevice.SOURCE_KEYBOARD);
            if (injectEventSync(downEvent)) {
                KeyEvent upEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP,
                        keyCode, 0, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                        InputDevice.SOURCE_KEYBOARD);
                injectEventSync(upEvent);
            }
        };

        return runAndWaitForEvents(command, new WaitForAnyEventPredicate(eventType), timeout)
                != null;
    }

    /**
     * Clicks at coordinates without waiting for device idle. This may be used for operations
     * that require stressing the target.
     * @param x
     * @param y
     * @return true if the click executed successfully
     */
    public boolean clickNoSync(int x, int y) {
        if (touchDown(x, y)) {
            SystemClock.sleep(REGULAR_CLICK_LENGTH);
            return touchUp(x, y);
        }
        return false;
    }

    /**
     * Click at coordinates and blocks until either accessibility event TYPE_WINDOW_CONTENT_CHANGED
     * or TYPE_VIEW_SELECTED are received.
     *
     * @param x
     * @param y
     * @param timeout waiting for event
     * @return true if events are received, else false if timeout.
     */
    public boolean clickAndSync(final int x, final int y, long timeout) {
        return runAndWaitForEvents(() -> clickNoSync(x, y), new WaitForAnyEventPredicate(
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                AccessibilityEvent.TYPE_VIEW_SELECTED), timeout) != null;
    }

    /**
     * Clicks at coordinates and waits for for a TYPE_WINDOW_STATE_CHANGED event followed
     * by TYPE_WINDOW_CONTENT_CHANGED. If timeout occurs waiting for TYPE_WINDOW_STATE_CHANGED,
     * no further waits will be performed and the function returns.
     * @param x
     * @param y
     * @param timeout waiting for event
     * @return true if both events occurred in the expected order
     */
    public boolean clickAndWaitForNewWindow(final int x, final int y, long timeout) {
        return runAndWaitForEvents(() -> clickNoSync(x, y), new WaitForAllEventPredicate(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED), timeout) != null;
    }

    /**
     * Touches down for a long press at the specified coordinates.
     *
     * @param x
     * @param y
     * @return true if successful.
     */
    public boolean longTapNoSync(int x, int y) {
        if (touchDown(x, y)) {
            SystemClock.sleep(LONG_PRESS_DURATION_MS);
            return touchUp(x, y);
        }
        return false;
    }

    /**
     * Long tap at coordinates and blocks until either accessibility event
     * TYPE_WINDOW_CONTENT_CHANGED or TYPE_VIEW_SELECTED are received.
     *
     * @param x
     * @param y
     * @param timeout waiting for event
     * @return true if events are received, else false if timeout.
     */
    public boolean longTapAndSync(final int x, final int y, long timeout) {
        return runAndWaitForEvents(() -> longTapNoSync(x, y), new WaitForAnyEventPredicate(
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                AccessibilityEvent.TYPE_VIEW_SELECTED), timeout) != null;
    }

    boolean touchDown(int x, int y) {
        mDownTime = SystemClock.uptimeMillis();
        MotionEvent event = getMotionEvent(mDownTime, mDownTime, MotionEvent.ACTION_DOWN, x, y);
        return injectEventSync(event);
    }

    boolean touchUp(int x, int y) {
        final long eventTime = SystemClock.uptimeMillis();
        MotionEvent event = getMotionEvent(mDownTime, eventTime, MotionEvent.ACTION_UP, x, y);
        mDownTime = 0;
        return injectEventSync(event);
    }

    private boolean touchMove(int x, int y) {
        final long eventTime = SystemClock.uptimeMillis();
        MotionEvent event = getMotionEvent(mDownTime, eventTime, MotionEvent.ACTION_MOVE, x, y);
        return injectEventSync(event);
    }

    /**
     * Handle swipes in any direction where the result is a scroll event. This call blocks
     * until the UI has fired a scroll event or timeout.
     * @param downX
     * @param downY
     * @param upX
     * @param upY
     * @param steps
     * @return true if we are not at the beginning or end of the scrollable view.
     */
    public boolean scrollSwipe(final int downX, final int downY, final int upX, final int upY,
            final int steps) {
        Runnable command = () -> swipe(downX, downY, upX, upY, steps);

        // Get scroll direction based on position.
        Direction direction;
        if (Math.abs(downX - upX) > Math.abs(downY - upY)) {
            // Horizontal.
            direction = downX > upX ? Direction.RIGHT : Direction.LEFT;
        } else {
            // Vertical.
            direction = downY > upY ? Direction.DOWN : Direction.UP;
        }
        EventCondition<Boolean> condition = Until.scrollFinished(direction);
        runAndWaitForEvents(command,
                condition,
                Configurator.getInstance().getScrollAcknowledgmentTimeout());

        return Boolean.FALSE.equals(condition.getResult());
    }

    /**
     * Handle swipes in any direction.
     * @param downX
     * @param downY
     * @param upX
     * @param upY
     * @param steps
     * @return true if the swipe executed successfully
     */
    public boolean swipe(int downX, int downY, int upX, int upY, int steps) {
        return swipe(downX, downY, upX, upY, steps, false /*drag*/);
    }

    /**
     * Handle swipes/drags in any direction.
     * @param downX
     * @param downY
     * @param upX
     * @param upY
     * @param steps
     * @param drag when true, the swipe becomes a drag swipe
     * @return true if the swipe executed successfully
     */
    public boolean swipe(int downX, int downY, int upX, int upY, int steps, boolean drag) {
        boolean ret;
        int swipeSteps = steps;
        double xStep, yStep;

        // avoid a divide by zero
        if(swipeSteps == 0)
            swipeSteps = 1;

        xStep = ((double)(upX - downX)) / swipeSteps;
        yStep = ((double)(upY - downY)) / swipeSteps;

        // first touch starts exactly at the point requested
        ret = touchDown(downX, downY);
        SystemClock.sleep(MOTION_EVENT_INJECTION_DELAY_MILLIS);
        if (drag)
            SystemClock.sleep(LONG_PRESS_DURATION_MS);
        for(int i = 1; i < swipeSteps; i++) {
            ret &= touchMove(downX + (int)(xStep * i), downY + (int)(yStep * i));
            if (!ret) {
                break;
            }
            // set some known constant delay between steps as without it this
            // become completely dependent on the speed of the system and results
            // may vary on different devices. This guarantees at minimum we have
            // a preset delay.
            SystemClock.sleep(MOTION_EVENT_INJECTION_DELAY_MILLIS);
        }
        if (drag)
            SystemClock.sleep(REGULAR_CLICK_LENGTH);
        ret &= touchUp(upX, upY);
        return ret;
    }

    /**
     * Performs a swipe between points in the Point array.
     * @param segments is Point array containing at least one Point object
     * @param segmentSteps steps to inject between two Points
     * @return true on success
     */
    public boolean swipe(Point[] segments, int segmentSteps) {
        boolean ret;
        int swipeSteps = segmentSteps;
        double xStep, yStep;

        // avoid a divide by zero
        if(segmentSteps == 0)
            segmentSteps = 1;

        // must have some points
        if(segments.length == 0)
            return false;

        // first touch starts exactly at the point requested
        ret = touchDown(segments[0].x, segments[0].y);
        SystemClock.sleep(MOTION_EVENT_INJECTION_DELAY_MILLIS);
        for(int seg = 0; seg < segments.length; seg++) {
            if(seg + 1 < segments.length) {

                xStep = ((double)(segments[seg+1].x - segments[seg].x)) / segmentSteps;
                yStep = ((double)(segments[seg+1].y - segments[seg].y)) / segmentSteps;

                for(int i = 1; i < swipeSteps; i++) {
                    ret &= touchMove(segments[seg].x + (int)(xStep * i),
                            segments[seg].y + (int)(yStep * i));
                    if (!ret) {
                        break;
                    }
                    // set some known constant delay between steps as without it this
                    // become completely dependent on the speed of the system and results
                    // may vary on different devices. This guarantees at minimum we have
                    // a preset delay.
                    SystemClock.sleep(MOTION_EVENT_INJECTION_DELAY_MILLIS);
                }
            }
        }
        ret &= touchUp(segments[segments.length - 1].x, segments[segments.length -1].y);
        return ret;
    }

    public boolean sendKey(int keyCode, int metaState) {
        return sendKeys(new int[]{keyCode}, metaState);
    }

    /**
     * Send multiple keys
     *
     * @param keyCodes array of keycode
     * @param metaState the pressed state of key modifiers
     * @return true if keys are sent.
     */
    public boolean sendKeys(int[] keyCodes, int metaState) {
        final long eventTime = SystemClock.uptimeMillis();
        for (int keyCode : keyCodes) {
            if (KEY_MODIFIER.containsKey(keyCode)) {
                metaState |= KEY_MODIFIER.get(keyCode);
            }
            KeyEvent downEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN,
                    keyCode, 0, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                    InputDevice.SOURCE_KEYBOARD);
            if (!injectEventSync(downEvent)) {
                return false;
            }
        }
        for (int keyCode : keyCodes) {
            KeyEvent upEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP,
                    keyCode, 0, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                    InputDevice.SOURCE_KEYBOARD);
            if (!injectEventSync(upEvent)) {
                return false;
            }
            if (KEY_MODIFIER.containsKey(keyCode)) {
                metaState &= ~KEY_MODIFIER.get(keyCode);
            }
        }
        return true;
    }

    /**
     * This method simply presses the power button if the screen is OFF else
     * it does nothing if the screen is already ON.
     * On API 20 or later devices, this will press the wakeup button instead.
     * @return true if the device was asleep else false
     * @throws RemoteException
     */
    public boolean wakeDevice() throws RemoteException {
        if(!isScreenOn()) {
            sendKey(KeyEvent.KEYCODE_WAKEUP, 0);
            return true;
        }
        return false;
    }

    /**
     * This method simply presses the power button if the screen is ON else
     * it does nothing if the screen is already OFF.
     * On API 20 or later devices, this will press the sleep button instead.
     * @return true if the device was awake else false
     * @throws RemoteException
     */
    public boolean sleepDevice() throws RemoteException {
        if(isScreenOn()) {
            sendKey(KeyEvent.KEYCODE_SLEEP , 0);
            return true;
        }
        return false;
    }

    /**
     * Checks the power manager if the screen is ON
     * @return true if the screen is ON else false
     */
    public boolean isScreenOn() {
        PowerManager pm = (PowerManager) mDevice.getInstrumentation().getContext().getSystemService(
                Service.POWER_SERVICE);
        return pm.isScreenOn();
    }

    boolean injectEventSync(InputEvent event) {
        return getUiAutomation().injectInputEvent(event, true);
    }

    private int getPointerAction(int motionEnvent, int index) {
        return motionEnvent + (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
    }

    /**
     * Performs a multi-touch gesture
     *
     * Takes a series of touch coordinates for at least 2 pointers. Each pointer must have
     * all of its touch steps defined in an array of {@link PointerCoords}. By having the ability
     * to specify the touch points along the path of a pointer, the caller is able to specify
     * complex gestures like circles, irregular shapes etc, where each pointer may take a
     * different path.
     *
     * To create a single point on a pointer's touch path
     * <code>
     *       PointerCoords p = new PointerCoords();
     *       p.x = stepX;
     *       p.y = stepY;
     *       p.pressure = 1;
     *       p.size = 1;
     * </code>
     * @param touches each array of {@link PointerCoords} constitute a single pointer's touch path.
     *        Multiple {@link PointerCoords} arrays constitute multiple pointers, each with its own
     *        path. Each {@link PointerCoords} in an array constitute a point on a pointer's path.
     * @return <code>true</code> if all points on all paths are injected successfully, <code>false
     *        </code>otherwise
     */
    public boolean performMultiPointerGesture(PointerCoords[] ... touches) {
        boolean ret;
        if (touches.length < 2) {
            throw new IllegalArgumentException("Must provide coordinates for at least 2 pointers");
        }

        // Get the pointer with the max steps to inject.
        int maxSteps = 0;
        for (PointerCoords[] touch : touches) maxSteps = Math.max(maxSteps, touch.length);

        // specify the properties for each pointer as finger touch
        PointerProperties[] properties = new PointerProperties[touches.length];
        PointerCoords[] pointerCoords = new PointerCoords[touches.length];
        for (int x = 0; x < touches.length; x++) {
            PointerProperties prop = new PointerProperties();
            prop.id = x;
            prop.toolType = Configurator.getInstance().getToolType();
            properties[x] = prop;

            // for each pointer set the first coordinates for touch down
            pointerCoords[x] = touches[x][0];
        }

        // Touch down all pointers
        long downTime = SystemClock.uptimeMillis();
        MotionEvent event;
        event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 1,
                properties, pointerCoords, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        ret = injectEventSync(event);

        for (int x = 1; x < touches.length; x++) {
            event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                    getPointerAction(MotionEvent.ACTION_POINTER_DOWN, x), x + 1, properties,
                    pointerCoords, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
            ret &= injectEventSync(event);
        }
        SystemClock.sleep(MOTION_EVENT_INJECTION_DELAY_MILLIS);

        // Move all pointers
        for (int i = 1; i < maxSteps - 1; i++) {
            // for each pointer
            for (int x = 0; x < touches.length; x++) {
                // check if it has coordinates to move
                if (touches[x].length > i)
                    pointerCoords[x] = touches[x][i];
                else
                    pointerCoords[x] = touches[x][touches[x].length - 1];
            }

            event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_MOVE, touches.length, properties, pointerCoords, 0, 0, 1, 1,
                    0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);

            ret &= injectEventSync(event);
            SystemClock.sleep(MOTION_EVENT_INJECTION_DELAY_MILLIS);
        }

        // For each pointer get the last coordinates
        for (int x = 0; x < touches.length; x++)
            pointerCoords[x] = touches[x][touches[x].length - 1];

        // touch up
        for (int x = 1; x < touches.length; x++) {
            event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                    getPointerAction(MotionEvent.ACTION_POINTER_UP, x), x + 1, properties,
                    pointerCoords, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
            ret &= injectEventSync(event);
        }

        // first to touch down is last up
        event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 1,
                properties, pointerCoords, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        ret &= injectEventSync(event);
        return ret;
    }

    /** Helper function to obtain a MotionEvent. */
    private static MotionEvent getMotionEvent(long downTime, long eventTime, int action,
            float x, float y) {

        PointerProperties properties = new PointerProperties();
        properties.id = 0;
        properties.toolType = Configurator.getInstance().getToolType();

        PointerCoords coords = new PointerCoords();
        coords.pressure = 1;
        coords.size = 1;
        coords.x = x;
        coords.y = y;

        return MotionEvent.obtain(downTime, eventTime, action, 1,
                new PointerProperties[] { properties }, new PointerCoords[] { coords },
                0, 0, 1.0f, 1.0f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
    }

    UiAutomation getUiAutomation() {
        return mDevice.getUiAutomation();
    }
}
