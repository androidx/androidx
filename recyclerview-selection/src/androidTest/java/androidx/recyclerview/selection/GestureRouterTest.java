/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.recyclerview.selection;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import androidx.recyclerview.selection.testing.TestEvents.Mouse;
import androidx.recyclerview.selection.testing.TestEvents.Touch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
@SmallTest
public final class GestureRouterTest {

    private TestHandler mHandler;
    private TestHandler mAlt;
    private GestureRouter<TestHandler> mRouter;

    @Before
    public void setUp() {
        mAlt = new TestHandler();
        mHandler = new TestHandler();
    }

    @Test
    public void testDelegates() {
        mRouter = new GestureRouter<>();
        mRouter.register(MotionEvent.TOOL_TYPE_MOUSE, mHandler);
        mRouter.register(MotionEvent.TOOL_TYPE_FINGER, mAlt);

        mRouter.onDown(Mouse.CLICK);
        mHandler.assertCalled_onDown(Mouse.CLICK);
        mAlt.assertNotCalled_onDown();

        mRouter.onShowPress(Mouse.CLICK);
        mHandler.assertCalled_onShowPress(Mouse.CLICK);
        mAlt.assertNotCalled_onShowPress();

        mRouter.onSingleTapUp(Mouse.CLICK);
        mHandler.assertCalled_onSingleTapUp(Mouse.CLICK);
        mAlt.assertNotCalled_onSingleTapUp();

        mRouter.onScroll(null, Mouse.CLICK, -1, -1);
        mHandler.assertCalled_onScroll(null, Mouse.CLICK, -1, -1);
        mAlt.assertNotCalled_onScroll();

        mRouter.onLongPress(Mouse.CLICK);
        mHandler.assertCalled_onLongPress(Mouse.CLICK);
        mAlt.assertNotCalled_onLongPress();

        mRouter.onFling(null, Mouse.CLICK, -1, -1);
        mHandler.assertCalled_onFling(null, Mouse.CLICK, -1, -1);
        mAlt.assertNotCalled_onFling();

        mRouter.onSingleTapConfirmed(Mouse.CLICK);
        mHandler.assertCalled_onSingleTapConfirmed(Mouse.CLICK);
        mAlt.assertNotCalled_onSingleTapConfirmed();

        mRouter.onDoubleTap(Mouse.CLICK);
        mHandler.assertCalled_onDoubleTap(Mouse.CLICK);
        mAlt.assertNotCalled_onDoubleTap();

        mRouter.onDoubleTapEvent(Mouse.CLICK);
        mHandler.assertCalled_onDoubleTapEvent(Mouse.CLICK);
        mAlt.assertNotCalled_onDoubleTapEvent();
    }

    @Test
    public void testFallsback() {
        mRouter = new GestureRouter<>(mAlt);
        mRouter.register(MotionEvent.TOOL_TYPE_MOUSE, mHandler);

        mRouter.onDown(Touch.TAP);
        mAlt.assertCalled_onDown(Touch.TAP);

        mRouter.onShowPress(Touch.TAP);
        mAlt.assertCalled_onShowPress(Touch.TAP);

        mRouter.onSingleTapUp(Touch.TAP);
        mAlt.assertCalled_onSingleTapUp(Touch.TAP);

        mRouter.onScroll(null, Touch.TAP, -1, -1);
        mAlt.assertCalled_onScroll(null, Touch.TAP, -1, -1);

        mRouter.onLongPress(Touch.TAP);
        mAlt.assertCalled_onLongPress(Touch.TAP);

        mRouter.onFling(null, Touch.TAP, -1, -1);
        mAlt.assertCalled_onFling(null, Touch.TAP, -1, -1);

        mRouter.onSingleTapConfirmed(Touch.TAP);
        mAlt.assertCalled_onSingleTapConfirmed(Touch.TAP);

        mRouter.onDoubleTap(Touch.TAP);
        mAlt.assertCalled_onDoubleTap(Touch.TAP);

        mRouter.onDoubleTapEvent(Touch.TAP);
        mAlt.assertCalled_onDoubleTapEvent(Touch.TAP);
    }

    @Test
    public void testEatsEventsWhenNoFallback() {
        mRouter = new GestureRouter<>();
        // Register the the delegate on mouse so touch events don't get handled.
        mRouter.register(MotionEvent.TOOL_TYPE_MOUSE, mHandler);

        mRouter.onDown(Touch.TAP);
        mAlt.assertNotCalled_onDown();

        mRouter.onShowPress(Touch.TAP);
        mAlt.assertNotCalled_onShowPress();

        mRouter.onSingleTapUp(Touch.TAP);
        mAlt.assertNotCalled_onSingleTapUp();

        mRouter.onScroll(null, Touch.TAP, -1, -1);
        mAlt.assertNotCalled_onScroll();

        mRouter.onLongPress(Touch.TAP);
        mAlt.assertNotCalled_onLongPress();

        mRouter.onFling(null, Touch.TAP, -1, -1);
        mAlt.assertNotCalled_onFling();

        mRouter.onSingleTapConfirmed(Touch.TAP);
        mAlt.assertNotCalled_onSingleTapConfirmed();

        mRouter.onDoubleTap(Touch.TAP);
        mAlt.assertNotCalled_onDoubleTap();

        mRouter.onDoubleTapEvent(Touch.TAP);
        mAlt.assertNotCalled_onDoubleTapEvent();
    }

    private static final class TestHandler implements OnGestureListener, OnDoubleTapListener {

        private final Spy mSpy = Mockito.mock(Spy.class);

        @Override
        public boolean onDown(MotionEvent e) {
            return mSpy.onDown(e);
        }

        @Override
        public void onShowPress(MotionEvent e) {
            mSpy.onShowPress(e);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return mSpy.onSingleTapUp(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return mSpy.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            mSpy.onLongPress(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return mSpy.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return mSpy.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return mSpy.onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return mSpy.onDoubleTapEvent(e);
        }

        void assertCalled_onDown(MotionEvent e) {
            verify(mSpy).onDown(e);
        }

        void assertCalled_onShowPress(MotionEvent e) {
            verify(mSpy).onShowPress(e);
        }

        void assertCalled_onSingleTapUp(MotionEvent e) {
            verify(mSpy).onSingleTapUp(e);
        }

        void assertCalled_onScroll(MotionEvent e1, MotionEvent e2, float x, float y) {
            verify(mSpy).onScroll(e1, e2, x, y);
        }

        void assertCalled_onLongPress(MotionEvent e) {
            verify(mSpy).onLongPress(e);
        }

        void assertCalled_onFling(MotionEvent e1, MotionEvent e2, float x, float y) {
            Mockito.verify(mSpy).onFling(e1, e2, x, y);
        }

        void assertCalled_onSingleTapConfirmed(MotionEvent e) {
            Mockito.verify(mSpy).onSingleTapConfirmed(e);
        }

        void assertCalled_onDoubleTap(MotionEvent e) {
            Mockito.verify(mSpy).onDoubleTap(e);
        }

        void assertCalled_onDoubleTapEvent(MotionEvent e) {
            Mockito.verify(mSpy).onDoubleTapEvent(e);
        }

        void assertNotCalled_onDown() {
            verify(mSpy, never()).onDown((MotionEvent) any());
        }

        void assertNotCalled_onShowPress() {
            verify(mSpy, never()).onShowPress((MotionEvent) any());
        }

        void assertNotCalled_onSingleTapUp() {
            verify(mSpy, never()).onSingleTapUp((MotionEvent) any());
        }

        void assertNotCalled_onScroll() {
            verify(mSpy, never()).onScroll(
                    (MotionEvent) any(), (MotionEvent) any(), anyFloat(), anyFloat());
        }

        void assertNotCalled_onLongPress() {
            verify(mSpy, never()).onLongPress((MotionEvent) any());
        }

        void assertNotCalled_onFling() {
            Mockito.verify(mSpy, never()).onFling(
                    (MotionEvent) any(), (MotionEvent) any(), anyFloat(), anyFloat());
        }

        void assertNotCalled_onSingleTapConfirmed() {
            Mockito.verify(mSpy, never()).onSingleTapConfirmed((MotionEvent) any());
        }

        void assertNotCalled_onDoubleTap() {
            Mockito.verify(mSpy, never()).onDoubleTap((MotionEvent) any());
        }

        void assertNotCalled_onDoubleTapEvent() {
            Mockito.verify(mSpy, never()).onDoubleTapEvent((MotionEvent) any());
        }
    }

    private interface Spy extends OnGestureListener, OnDoubleTapListener {
    }
}
