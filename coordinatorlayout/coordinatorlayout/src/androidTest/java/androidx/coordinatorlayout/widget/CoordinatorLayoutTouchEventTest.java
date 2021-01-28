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

package androidx.coordinatorlayout.widget;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;

@SuppressWarnings({"unchecked", "rawtypes"})
@LargeTest
@RunWith(AndroidJUnit4.class)
public class CoordinatorLayoutTouchEventTest {
    private static final Touch[] NO_TOUCHES = new Touch[] {};

    @Rule
    public final ActivityTestRule<CoordinatorLayoutActivity> mActivityTestRule;

    private CoordinatorLayout mCoordinatorLayout;
    private View mView1;
    private View mView2;
    private View mView3;
    private CoordinatorLayout.Behavior mBehavior1;
    private CoordinatorLayout.Behavior mBehavior2;
    private CoordinatorLayout.Behavior mBehavior3;

    public CoordinatorLayoutTouchEventTest() {
        mActivityTestRule = new ActivityTestRule<>(CoordinatorLayoutActivity.class);
    }

    @Before
    public void setup() throws Throwable {
        mCoordinatorLayout = mActivityTestRule.getActivity().mCoordinatorLayout;
        mView1 = new View(mCoordinatorLayout.getContext());
        mView2 = new View(mCoordinatorLayout.getContext());
        mView3 = new View(mCoordinatorLayout.getContext());
        mBehavior1 = mock(CoordinatorLayout.Behavior.class);
        mBehavior2 = mock(CoordinatorLayout.Behavior.class);
        mBehavior3 = mock(CoordinatorLayout.Behavior.class);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addViewWithBehavior(mCoordinatorLayout, mView1, mBehavior1);
                addViewWithBehavior(mCoordinatorLayout, mView2, mBehavior2);
                addViewWithBehavior(mCoordinatorLayout, mView3, mBehavior3);
            }
        });
        getInstrumentation().waitForIdleSync();
        reset(mBehavior1, mBehavior2, mBehavior3);
    }

    @Test
    public void onInterceptTouchEvent_noneHandles() throws Throwable {
        mView1.setOnTouchListener(new ReturnTrueOnTouchListener());
        final MotionEvent downEvent = obtainEvent(MotionEvent.ACTION_DOWN);
        final MotionEvent moveEvent = obtainEvent(MotionEvent.ACTION_MOVE);
        final MotionEvent upEvent = obtainEvent(MotionEvent.ACTION_UP);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCoordinatorLayout.dispatchTouchEvent(downEvent);
                mCoordinatorLayout.dispatchTouchEvent(moveEvent);
                mCoordinatorLayout.dispatchTouchEvent(upEvent);
            }
        });
        getInstrumentation().waitForIdleSync();

        Touch[] touches = new Touch[]{
                onInterceptTouch(downEvent),
                onInterceptTouch(moveEvent),
                onInterceptTouch(upEvent)};
        verifyTouches(mView1, touches);
        verifyTouches(mView2, touches);
        verifyTouches(mView3, touches);
    }

    @Test
    @Ignore // This test is broken on all SDK levels.
    public void onInterceptTouchEvent_interceptOnDown() throws Throwable {
        mView1.setOnTouchListener(new ReturnTrueOnTouchListener());
        final MotionEvent downEvent = obtainEvent(MotionEvent.ACTION_DOWN);
        final MotionEvent moveEvent = obtainEvent(MotionEvent.ACTION_MOVE);
        final MotionEvent upEvent = obtainEvent(MotionEvent.ACTION_UP);

        when(mBehavior2.onInterceptTouchEvent(mCoordinatorLayout, mView2, downEvent))
                .thenReturn(true);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCoordinatorLayout.dispatchTouchEvent(downEvent);
                mCoordinatorLayout.dispatchTouchEvent(moveEvent);
                mCoordinatorLayout.dispatchTouchEvent(upEvent);
            }
        });
        getInstrumentation().waitForIdleSync();

        verifyTouches(mView1, NO_TOUCHES);
        verifyTouches(mView2,
                onInterceptTouch(downEvent),
                onTouch(moveEvent),
                onTouch(upEvent));
        verifyTouches(mView3,
                onInterceptTouch(downEvent),
                onInterceptTouch(MotionEvent.ACTION_CANCEL));
    }

    @Test
    public void onInterceptTouchEvent_interceptOnMove() throws Throwable {
        mView1.setOnTouchListener(new ReturnTrueOnTouchListener());
        final MotionEvent downEvent = obtainEvent(MotionEvent.ACTION_DOWN);
        final MotionEvent moveEvent = obtainEvent(MotionEvent.ACTION_MOVE);
        final MotionEvent upEvent = obtainEvent(MotionEvent.ACTION_UP);

        when(mBehavior2.onInterceptTouchEvent(mCoordinatorLayout, mView2, moveEvent))
                .thenReturn(true);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCoordinatorLayout.dispatchTouchEvent(downEvent);
                mCoordinatorLayout.dispatchTouchEvent(moveEvent);
                mCoordinatorLayout.dispatchTouchEvent(upEvent);
            }
        });
        getInstrumentation().waitForIdleSync();

        verifyTouches(mView1,
                onInterceptTouch(downEvent),
                onInterceptTouch(MotionEvent.ACTION_CANCEL));
        verifyTouches(mView2,
                onInterceptTouch(downEvent),
                onInterceptTouch(moveEvent),
                onTouch(upEvent));
        verifyTouches(mView3,
                onInterceptTouch(downEvent),
                onInterceptTouch(moveEvent),
                onInterceptTouch(MotionEvent.ACTION_CANCEL));
    }

    @Test
    public void onInterceptTouchEvent_interceptOnUp() throws Throwable {
        mView1.setOnTouchListener(new ReturnTrueOnTouchListener());
        final MotionEvent downEvent = obtainEvent(MotionEvent.ACTION_DOWN);
        final MotionEvent moveEvent = obtainEvent(MotionEvent.ACTION_MOVE);
        final MotionEvent upEvent = obtainEvent(MotionEvent.ACTION_UP);

        when(mBehavior2.onInterceptTouchEvent(mCoordinatorLayout, mView2, upEvent))
                .thenReturn(true);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCoordinatorLayout.dispatchTouchEvent(downEvent);
                mCoordinatorLayout.dispatchTouchEvent(moveEvent);
                mCoordinatorLayout.dispatchTouchEvent(upEvent);
            }
        });
        getInstrumentation().waitForIdleSync();

        verifyTouches(mView1,
                onInterceptTouch(downEvent),
                onInterceptTouch(moveEvent),
                onInterceptTouch(MotionEvent.ACTION_CANCEL));
        verifyTouches(mView2,
                onInterceptTouch(downEvent),
                onInterceptTouch(moveEvent),
                onInterceptTouch(upEvent));
        verifyTouches(mView3,
                onInterceptTouch(downEvent),
                onInterceptTouch(moveEvent),
                onInterceptTouch(upEvent));
    }

    @Test
    @Ignore // This test is broken on all SDK levels.
    public void onInterceptTouchEvent_multipleDowns_intercepted() throws Throwable {
        mView1.setOnTouchListener(new ReturnTrueOnTouchListener());
        final MotionEvent downEvent = obtainEvent(MotionEvent.ACTION_DOWN);
        final MotionEvent moveEvent = obtainEvent(MotionEvent.ACTION_MOVE);
        final MotionEvent downEvent2 = obtainEvent(MotionEvent.ACTION_DOWN);

        when(mBehavior2.onInterceptTouchEvent(mCoordinatorLayout, mView2, moveEvent))
                .thenReturn(true);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCoordinatorLayout.dispatchTouchEvent(downEvent);
                mCoordinatorLayout.dispatchTouchEvent(moveEvent);
                mCoordinatorLayout.dispatchTouchEvent(downEvent2);
            }
        });
        getInstrumentation().waitForIdleSync();

        verifyTouches(mView1,
                onInterceptTouch(downEvent),
                onInterceptTouch(MotionEvent.ACTION_CANCEL),
                onInterceptTouch(downEvent2));
        verifyTouches(mView2,
                onInterceptTouch(downEvent),
                onInterceptTouch(moveEvent),
                onTouch(MotionEvent.ACTION_CANCEL),
                onInterceptTouch(downEvent2));
        verifyTouches(mView3,
                onInterceptTouch(downEvent),
                onInterceptTouch(moveEvent),
                onInterceptTouch(MotionEvent.ACTION_CANCEL),
                onInterceptTouch(downEvent2));
    }

    @Test
    public void onInterceptTouchEvent_multipleDowns() throws Throwable {
        mView1.setOnTouchListener(new ReturnTrueOnTouchListener());
        final MotionEvent downEvent = obtainEvent(MotionEvent.ACTION_DOWN);
        final MotionEvent moveEvent = obtainEvent(MotionEvent.ACTION_MOVE);
        final MotionEvent downEvent2 = obtainEvent(MotionEvent.ACTION_DOWN);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCoordinatorLayout.dispatchTouchEvent(downEvent);
                mCoordinatorLayout.dispatchTouchEvent(moveEvent);
                mCoordinatorLayout.dispatchTouchEvent(downEvent2);
            }
        });
        getInstrumentation().waitForIdleSync();

        Touch[] touches = new Touch[] {
                onInterceptTouch(downEvent),
                onInterceptTouch(moveEvent),
                onInterceptTouch(downEvent2)};
        verifyTouches(mView1, touches);
        verifyTouches(mView2, touches);
        verifyTouches(mView3, touches);
    }

    @Test
    public void requestDisallowInterceptTouchEvent() throws Throwable {
        mView1.setOnTouchListener(new ReturnTrueOnTouchListener());
        final MotionEvent downEvent = obtainEvent(MotionEvent.ACTION_DOWN);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCoordinatorLayout.dispatchTouchEvent(downEvent);
                mCoordinatorLayout.requestDisallowInterceptTouchEvent(true);
            }
        });
        getInstrumentation().waitForIdleSync();

        Touch[] touches = new Touch[] {
                onInterceptTouch(downEvent),
                onInterceptTouch(MotionEvent.ACTION_CANCEL)};
        verifyTouches(mView1, touches);
        verifyTouches(mView2, touches);
        verifyTouches(mView3, touches);
    }

    @Test
    public void requestDisallowInterceptTouchEvent_behaviorHandling() throws Throwable {
        mView1.setOnTouchListener(new ReturnTrueOnTouchListener());
        final MotionEvent downEvent = obtainEvent(MotionEvent.ACTION_DOWN);

        when(mBehavior2.onInterceptTouchEvent(mCoordinatorLayout, mView2, downEvent))
                .thenReturn(true);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCoordinatorLayout.dispatchTouchEvent(downEvent);
                mCoordinatorLayout.requestDisallowInterceptTouchEvent(true);
            }
        });
        getInstrumentation().waitForIdleSync();

        Touch[] touches = new Touch[] {
                };
        verifyTouches(mView1, NO_TOUCHES);
        verifyTouches(mView2,
                onInterceptTouch(downEvent),
                onTouch(downEvent),
                onTouch(MotionEvent.ACTION_CANCEL));
        verifyTouches(mView3,
                onInterceptTouch(downEvent),
                onInterceptTouch(MotionEvent.ACTION_CANCEL));
    }

    @Test
    public void blocksInteractionBelow_down() throws Throwable {
        mView1.setOnTouchListener(new ReturnTrueOnTouchListener());
        final MotionEvent downEvent = obtainEvent(MotionEvent.ACTION_DOWN);
        final MotionEvent moveEvent = obtainEvent(MotionEvent.ACTION_MOVE);
        final MotionEvent upEvent = obtainEvent(MotionEvent.ACTION_UP);

        when(mBehavior2.blocksInteractionBelow(mCoordinatorLayout, mView2))
                .thenReturn(true);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCoordinatorLayout.dispatchTouchEvent(downEvent);
                mCoordinatorLayout.dispatchTouchEvent(moveEvent);
                mCoordinatorLayout.dispatchTouchEvent(upEvent);
            }
        });
        getInstrumentation().waitForIdleSync();

        verifyTouches(mView1, NO_TOUCHES);
        verifyTouches(mView2,
                onInterceptTouch(downEvent),
                onInterceptTouch(moveEvent),
                onInterceptTouch(upEvent));
        verifyTouches(mView3,
                onInterceptTouch(downEvent),
                onInterceptTouch(moveEvent),
                onInterceptTouch(upEvent));
    }

    @Test
    @Ignore // This test is broken on all SDK levels.
    public void blocksInteractionBelow_move() throws Throwable {
        mView1.setOnTouchListener(new ReturnTrueOnTouchListener());
        final MotionEvent downEvent = obtainEvent(MotionEvent.ACTION_DOWN);
        final MotionEvent moveEvent = obtainEvent(MotionEvent.ACTION_MOVE);
        final MotionEvent upEvent = obtainEvent(MotionEvent.ACTION_UP);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCoordinatorLayout.dispatchTouchEvent(downEvent);
                when(mBehavior2.blocksInteractionBelow(mCoordinatorLayout, mView2))
                        .thenReturn(true);
                mCoordinatorLayout.dispatchTouchEvent(moveEvent);
                mCoordinatorLayout.dispatchTouchEvent(upEvent);
            }
        });
        getInstrumentation().waitForIdleSync();

        verifyTouches(mView1,
                onInterceptTouch(downEvent),
                onInterceptTouch(MotionEvent.ACTION_CANCEL));
        verifyTouches(mView2,
                onInterceptTouch(downEvent),
                onInterceptTouch(moveEvent),
                onInterceptTouch(upEvent));
        verifyTouches(mView3,
                onInterceptTouch(downEvent),
                onInterceptTouch(moveEvent),
                onInterceptTouch(upEvent));
    }

    @Test
    public void onTouchEvent() throws Throwable {
        TouchDelegate touchDelegate = mock(TouchDelegate.class);
        mCoordinatorLayout.setTouchDelegate(touchDelegate);
        final MotionEvent downEvent = obtainEvent(MotionEvent.ACTION_DOWN);
        final MotionEvent moveEvent = obtainEvent(MotionEvent.ACTION_MOVE);
        final MotionEvent upEvent = obtainEvent(MotionEvent.ACTION_UP);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCoordinatorLayout.dispatchTouchEvent(downEvent);
                mCoordinatorLayout.dispatchTouchEvent(moveEvent);
                mCoordinatorLayout.dispatchTouchEvent(upEvent);
            }
        });
        getInstrumentation().waitForIdleSync();

        verify(touchDelegate).onTouchEvent(downEvent);
        verify(touchDelegate).onTouchEvent(moveEvent);
        verify(touchDelegate).onTouchEvent(upEvent);
        verifyNoMoreInteractions(touchDelegate);
        Touch[] touches = new Touch[] {
                onInterceptTouch(downEvent),
                onTouch(downEvent),
                onTouch(moveEvent),
                onTouch(upEvent)};
        verifyTouches(mView1, touches);
        verifyTouches(mView2, touches);
        verifyTouches(mView3, touches);
    }

    @Test
    @Ignore // This test is broken on all SDK levels.
    public void onTouchEvent_intercept() throws Throwable {
        TouchDelegate touchDelegate = mock(TouchDelegate.class);
        mCoordinatorLayout.setTouchDelegate(touchDelegate);
        final MotionEvent downEvent = obtainEvent(MotionEvent.ACTION_DOWN);
        final MotionEvent moveEvent = obtainEvent(MotionEvent.ACTION_MOVE);
        final MotionEvent upEvent = obtainEvent(MotionEvent.ACTION_UP);

        when(mBehavior2.onTouchEvent(mCoordinatorLayout, mView2, moveEvent))
                .thenReturn(true);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCoordinatorLayout.dispatchTouchEvent(downEvent);
                mCoordinatorLayout.dispatchTouchEvent(moveEvent);
                mCoordinatorLayout.dispatchTouchEvent(upEvent);
            }
        });
        getInstrumentation().waitForIdleSync();

        verify(touchDelegate).onTouchEvent(downEvent);
        verify(touchDelegate).onTouchEvent(action(MotionEvent.ACTION_CANCEL));
        verifyNoMoreInteractions(touchDelegate);
        verifyTouches(mView1,
                onInterceptTouch(downEvent),
                onTouch(MotionEvent.ACTION_CANCEL));
        verifyTouches(mView2,
                onInterceptTouch(downEvent),
                onTouch(moveEvent),
                onTouch(upEvent));
        verifyTouches(mView3,
                onInterceptTouch(downEvent),
                onTouch(moveEvent),
                onTouch(MotionEvent.ACTION_CANCEL));
    }

    private static void verifyTouches(View view, Touch... touches) {
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
        CoordinatorLayout.Behavior behavior = lp.getBehavior();
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) view.getParent();
        InOrder inOrder = inOrder(behavior);
        for (Touch touch : touches) {
            switch (touch.mType) {
                case INTERCEPT:
                    inOrder.verify(behavior)
                            .onInterceptTouchEvent(
                                    eq(coordinatorLayout), eq(view), touch.getMatcher());
                    break;
                case TOUCH:
                    inOrder.verify(behavior)
                            .onTouchEvent(eq(coordinatorLayout), eq(view), touch.getMatcher());
                    break;
            }
        }
        verifyNoMoreTouches(inOrder, behavior);
    }

    private static void verifyNoMoreTouches(InOrder order, CoordinatorLayout.Behavior behavior) {
        order.verify(behavior, times(0))
                .onInterceptTouchEvent(
                        any(CoordinatorLayout.class), any(View.class), any(MotionEvent.class));
        order.verify(behavior, times(0))
                .onTouchEvent(
                        any(CoordinatorLayout.class), any(View.class), any(MotionEvent.class));
    }

    private static MotionEvent obtainEvent(int action) {
        final long now = SystemClock.uptimeMillis();
        return MotionEvent.obtain(now, now, action, 0, 0, 0);
    }

    private static void addViewWithBehavior(CoordinatorLayout coordinatorLayout, View view,
            CoordinatorLayout.Behavior behavior) {
        final CoordinatorLayout.LayoutParams lp = coordinatorLayout.generateDefaultLayoutParams();
        lp.setBehavior(behavior);
        coordinatorLayout.addView(view, lp);
    }

    private static MotionEvent action(int action) {
        return argThat(new MotionEventMatcher(action));
    }

    private static Touch onInterceptTouch(MotionEvent motionEvent) {
        return new Touch(Touch.Type.INTERCEPT, motionEvent);
    }

    private static Touch onInterceptTouch(int action) {
        return new Touch(Touch.Type.INTERCEPT, action);
    }

    private static Touch onTouch(MotionEvent motionEvent) {
        return new Touch(Touch.Type.TOUCH, motionEvent);
    }

    private static Touch onTouch(int action) {
        return new Touch(Touch.Type.TOUCH, action);
    }

    private static final class Touch {
        enum Type {
            INTERCEPT,
            TOUCH,
        }
        private final Type mType;
        private final MotionEvent mMotionEvent;
        private final int mAction;

        Touch(Type type, MotionEvent motionEvent) {
            mType = type;
            mMotionEvent = motionEvent;
            mAction = 0;
        }

        Touch(Type type, int action) {
            mType = type;
            mMotionEvent = null;
            mAction = action;
        }

        MotionEvent getMatcher() {
            return mMotionEvent != null ? eq(mMotionEvent) : action(mAction);
        }
    }

    private static final class MotionEventMatcher implements ArgumentMatcher<MotionEvent> {

        private final int mAction;

        MotionEventMatcher(int action) {
            this.mAction = action;
        }

        @Override
        public boolean matches(MotionEvent event) {
            return event.getAction() == mAction;
        }

        @NonNull
        @Override
        public String toString() {
            return "MotionEvent#getAction() == " + MotionEvent.actionToString(mAction);
        }
    }

    private static final class ReturnTrueOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    }
}
