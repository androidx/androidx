/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.core.view;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.Instrumentation;
import android.graphics.Point;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.core.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;

@RunWith(AndroidJUnit4.class)
public class DragStartHelperTest {

    @Rule
    public ActivityTestRule<DragStartHelperTestActivity> mActivityRule =
            new ActivityTestRule<>(DragStartHelperTestActivity.class);

    private Instrumentation mInstrumentation;
    private View mDragSource;

    interface DragStartListener {
        boolean onDragStart(View view, DragStartHelper helper, Point touchPosition);
    }

    @NonNull
    private DragStartListener createListener(boolean returnValue) {
        final DragStartListener listener = mock(DragStartListener.class);
        when(listener.onDragStart(any(View.class), any(DragStartHelper.class), any(Point.class)))
                .thenReturn(returnValue);
        return listener;
    }

    @NonNull
    private DragStartHelper createDragStartHelper(final DragStartListener listener) {
        return new DragStartHelper(mDragSource, new DragStartHelper.OnDragStartListener() {
            @Override
            public boolean onDragStart(View v, DragStartHelper helper) {
                Point touchPosition = new Point();
                helper.getTouchPosition(touchPosition);
                return listener.onDragStart(v, helper, touchPosition);
            }
        });
    }

    private static int[] getViewCenter(View view) {
        final int[] xy = new int[2];
        view.getLocationOnScreen(xy);
        xy[0] += view.getWidth() / 2;
        xy[1] += view.getHeight() / 2;
        return xy;
    }

    private static MotionEvent obtainTouchEvent(
            int action, View anchor, int offsetX, int offsetY) {
        final long eventTime = SystemClock.uptimeMillis();
        final int[] xy = getViewCenter(anchor);
        return MotionEvent.obtain(
                eventTime, eventTime, action, xy[0] + offsetX, xy[1] + offsetY, 0);
    }

    private void sendTouchEvent(int action, View anchor, int offsetX, int offsetY) {
        mInstrumentation.sendPointerSync(obtainTouchEvent(action, anchor, offsetX, offsetY));
    }

    private static MotionEvent obtainMouseEvent(
            int action, int buttonState, View anchor, int offsetX, int offsetY) {
        final long eventTime = SystemClock.uptimeMillis();

        final int[] xy = getViewCenter(anchor);

        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[] {
                new MotionEvent.PointerProperties()
        };
        props[0].id = 0;
        props[0].toolType = MotionEvent.TOOL_TYPE_MOUSE;

        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[] {
                new MotionEvent.PointerCoords()
        };
        coords[0].x = xy[0] + offsetX;
        coords[0].y = xy[1] + offsetY;

        return MotionEvent.obtain(eventTime, eventTime, action, 1, props, coords, 0,
                buttonState, 0, 0, -1, 0, InputDevice.SOURCE_MOUSE, 0);
    }

    private void sendMouseEvent(
            int action, int buttonState, View anchor, int offsetX, int offsetY) {
        mInstrumentation.sendPointerSync(obtainMouseEvent(
                action, buttonState, anchor, offsetX, offsetY));
    }

    static class TouchPositionMatcher implements ArgumentMatcher<Point> {

        private final Point mExpectedPosition;

        TouchPositionMatcher(int x, int y) {
            mExpectedPosition = new Point(x, y);
        }

        TouchPositionMatcher(View anchor, int x, int y) {
            this(anchor.getWidth() / 2 + x, anchor.getHeight() / 2 + y);
        }

        @Override
        public boolean matches(Point actual) {
            return mExpectedPosition.equals(actual);
        }

        @Override
        public String toString() {
            return "TouchPositionMatcher: " + mExpectedPosition;
        }
    }

    private void waitForLongPress() {
        SystemClock.sleep(ViewConfiguration.getLongPressTimeout() * 2);
    }

    @Before
    public void setUp() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mDragSource = mActivityRule.getActivity().findViewById(R.id.drag_source);
    }

    @SmallTest
    @Test
    public void mouseClick() throws Throwable {
        final DragStartListener listener = createListener(true);
        final DragStartHelper helper = createDragStartHelper(listener);
        helper.attach();

        sendMouseEvent(MotionEvent.ACTION_DOWN, MotionEvent.BUTTON_PRIMARY, mDragSource, 0, 0);
        sendMouseEvent(MotionEvent.ACTION_UP, MotionEvent.BUTTON_PRIMARY, mDragSource, 0, 0);

        // A simple mouse click does not trigger OnDragStart.
        verifyNoMoreInteractions(listener);
    }

    @SmallTest
    @Test
    public void mousePressWithSecondaryButton() throws Throwable {
        final DragStartListener listener = createListener(true);
        final DragStartHelper helper = createDragStartHelper(listener);
        helper.attach();

        sendMouseEvent(MotionEvent.ACTION_DOWN, MotionEvent.BUTTON_PRIMARY, mDragSource, 0, 0);
        sendMouseEvent(MotionEvent.ACTION_MOVE,
                MotionEvent.BUTTON_PRIMARY | MotionEvent.BUTTON_SECONDARY, mDragSource, 0, 0);
        sendMouseEvent(MotionEvent.ACTION_MOVE, MotionEvent.BUTTON_PRIMARY, mDragSource, 0, 0);

        // ACTION_MOVE with the same position does not trigger OnDragStart.
        verifyNoMoreInteractions(listener);
    }

    @SmallTest
    @Test
    public void mouseDrag() throws Throwable {
        final DragStartListener listener = createListener(true);
        final DragStartHelper helper = createDragStartHelper(listener);
        helper.attach();

        sendMouseEvent(MotionEvent.ACTION_DOWN, MotionEvent.BUTTON_PRIMARY, mDragSource, 0, 0);
        sendMouseEvent(MotionEvent.ACTION_MOVE, MotionEvent.BUTTON_PRIMARY, mDragSource, 1, 2);
        sendMouseEvent(MotionEvent.ACTION_MOVE, MotionEvent.BUTTON_PRIMARY, mDragSource, 3, 4);
        sendMouseEvent(MotionEvent.ACTION_MOVE, MotionEvent.BUTTON_PRIMARY, mDragSource, 5, 6);

        // Returning true from the callback prevents further callbacks.
        verify(listener, times(1)).onDragStart(
                eq(mDragSource), eq(helper), argThat(new TouchPositionMatcher(mDragSource, 1, 2)));
        verifyNoMoreInteractions(listener);
    }

    @SmallTest
    @Test
    public void mouseDragWithNonprimaryButton() throws Throwable {
        final DragStartListener listener = createListener(true);
        final DragStartHelper helper = createDragStartHelper(listener);
        helper.attach();

        sendMouseEvent(MotionEvent.ACTION_DOWN, MotionEvent.BUTTON_SECONDARY, mDragSource, 0, 0);
        sendMouseEvent(MotionEvent.ACTION_MOVE, MotionEvent.BUTTON_SECONDARY, mDragSource, 1, 2);
        sendMouseEvent(MotionEvent.ACTION_UP, MotionEvent.BUTTON_SECONDARY, mDragSource, 3, 4);

        sendMouseEvent(MotionEvent.ACTION_DOWN, MotionEvent.BUTTON_TERTIARY, mDragSource, 0, 0);
        sendMouseEvent(MotionEvent.ACTION_MOVE, MotionEvent.BUTTON_TERTIARY, mDragSource, 1, 2);
        sendMouseEvent(MotionEvent.ACTION_UP, MotionEvent.BUTTON_TERTIARY, mDragSource, 3, 4);

        // Dragging mouse with a non-primary button down does not trigger OnDragStart.
        verifyNoMoreInteractions(listener);
    }

    @SmallTest
    @Test
    public void mouseDragUsingTouchListener() throws Throwable {
        final DragStartListener listener = createListener(true);
        final DragStartHelper helper = createDragStartHelper(listener);

        mDragSource.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                helper.onTouch(view, motionEvent);
                return true;
            }
        });

        sendMouseEvent(MotionEvent.ACTION_DOWN, MotionEvent.BUTTON_PRIMARY, mDragSource, 0, 0);
        sendMouseEvent(MotionEvent.ACTION_MOVE, MotionEvent.BUTTON_PRIMARY, mDragSource, 1, 2);
        sendMouseEvent(MotionEvent.ACTION_MOVE, MotionEvent.BUTTON_PRIMARY, mDragSource, 3, 4);
        sendMouseEvent(MotionEvent.ACTION_MOVE, MotionEvent.BUTTON_PRIMARY, mDragSource, 5, 6);

        // Returning true from the callback prevents further callbacks.
        verify(listener, times(1)).onDragStart(
                eq(mDragSource), eq(helper), argThat(new TouchPositionMatcher(mDragSource, 1, 2)));
        verifyNoMoreInteractions(listener);
    }

    @SmallTest
    @Test
    public void mouseDragWhenListenerReturnsFalse() throws Throwable {
        final DragStartListener listener = createListener(false);
        final DragStartHelper helper = createDragStartHelper(listener);
        helper.attach();

        sendMouseEvent(MotionEvent.ACTION_DOWN, MotionEvent.BUTTON_PRIMARY, mDragSource, 0, 0);
        sendMouseEvent(MotionEvent.ACTION_MOVE, MotionEvent.BUTTON_PRIMARY, mDragSource, 1, 2);
        sendMouseEvent(MotionEvent.ACTION_MOVE, MotionEvent.BUTTON_PRIMARY, mDragSource, 3, 4);
        sendMouseEvent(MotionEvent.ACTION_MOVE, MotionEvent.BUTTON_PRIMARY, mDragSource, 5, 6);

        // When the listener returns false every ACTION_MOVE triggers OnDragStart.
        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener, times(1)).onDragStart(
                eq(mDragSource), eq(helper), argThat(new TouchPositionMatcher(mDragSource, 1, 2)));
        inOrder.verify(listener, times(1)).onDragStart(
                eq(mDragSource), eq(helper), argThat(new TouchPositionMatcher(mDragSource, 3, 4)));
        inOrder.verify(listener, times(1)).onDragStart(
                eq(mDragSource), eq(helper), argThat(new TouchPositionMatcher(mDragSource, 5, 6)));
        inOrder.verifyNoMoreInteractions();
    }

    @LargeTest
    @Test
    public void mouseLongPress() throws Throwable {
        final DragStartListener listener = createListener(true);
        final DragStartHelper helper = createDragStartHelper(listener);
        helper.attach();

        sendMouseEvent(MotionEvent.ACTION_DOWN, MotionEvent.BUTTON_PRIMARY, mDragSource, 1, 2);
        waitForLongPress();

        // Long press triggers OnDragStart.
        verify(listener, times(1)).onDragStart(
                eq(mDragSource), eq(helper), argThat(new TouchPositionMatcher(mDragSource, 1, 2)));
        verifyNoMoreInteractions(listener);
    }

    @SmallTest
    @Test
    public void touchDrag() throws Throwable {
        final DragStartListener listener = createListener(false);
        final DragStartHelper helper = createDragStartHelper(listener);
        helper.attach();

        sendTouchEvent(MotionEvent.ACTION_DOWN, mDragSource, 0, 0);
        sendTouchEvent(MotionEvent.ACTION_MOVE, mDragSource, 1, 2);
        sendTouchEvent(MotionEvent.ACTION_MOVE, mDragSource, 3, 4);
        sendTouchEvent(MotionEvent.ACTION_MOVE, mDragSource, 5, 6);

        // Touch and drag (without delay) does not trigger OnDragStart.
        verifyNoMoreInteractions(listener);
    }

    @SmallTest
    @Test
    public void touchTap() throws Throwable {
        final DragStartListener listener = createListener(false);
        final DragStartHelper helper = createDragStartHelper(listener);
        helper.attach();

        sendTouchEvent(MotionEvent.ACTION_DOWN, mDragSource, 0, 0);
        sendTouchEvent(MotionEvent.ACTION_UP, mDragSource, 0, 0);

        // A simple tap does not trigger OnDragStart.
        verifyNoMoreInteractions(listener);
    }

    @LargeTest
    @Test
    public void touchLongPress() throws Throwable {
        final DragStartListener listener = createListener(true);
        final DragStartHelper helper = createDragStartHelper(listener);
        helper.attach();

        sendTouchEvent(MotionEvent.ACTION_DOWN, mDragSource, 1, 2);
        waitForLongPress();

        // Long press triggers OnDragStart.
        verify(listener, times(1)).onDragStart(
                eq(mDragSource), eq(helper), argThat(new TouchPositionMatcher(mDragSource, 1, 2)));
        verifyNoMoreInteractions(listener);
    }

    @LargeTest
    @Test
    public void touchLongPressUsingLongClickListener() throws Throwable {
        final DragStartListener listener = createListener(true);

        final DragStartHelper helper = createDragStartHelper(listener);
        mDragSource.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return helper.onLongClick(view);
            }
        });

        sendTouchEvent(MotionEvent.ACTION_DOWN, mDragSource, 1, 2);
        waitForLongPress();

        // Long press triggers OnDragStart.
        // Since ACTION_DOWN is not handled, the touch offset is not available.
        verify(listener, times(1)).onDragStart(
                eq(mDragSource), eq(helper), argThat(new TouchPositionMatcher(0, 0)));
        verifyNoMoreInteractions(listener);
    }
}
