/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.widget.helper;

import android.app.Instrumentation;
import android.os.Debug;
import android.os.SystemClock;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.BaseRecyclerViewInstrumentationTest;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.WrappedRecyclerView;
import android.test.InstrumentationTestCase;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.helper.ItemTouchHelper.*;

public class ItemTouchHelperTest extends BaseRecyclerViewInstrumentationTest {

    TestAdapter mAdapter;

    TestLayoutManager mLayoutManager;

    private LoggingCalback mCalback;

    private LoggingItemTouchHelper mItemTouchHelper;

    private WrappedRecyclerView mWrappedRecyclerView;

    private Boolean mSetupRTL;

    public ItemTouchHelperTest() {
        super(false);
    }

    private RecyclerView setup(int dragDirs, int swipeDirs) throws Throwable {
        mWrappedRecyclerView = inflateWrappedRV();
        mAdapter = new TestAdapter(10);
        mLayoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, 0, Math.min(5, state.getItemCount()));
                layoutLatch.countDown();
            }

            @Override
            public boolean canScrollHorizontally() {
                return false;
            }

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        mWrappedRecyclerView.setFakeRTL(mSetupRTL);
        mWrappedRecyclerView.setAdapter(mAdapter);
        mWrappedRecyclerView.setLayoutManager(mLayoutManager);
        mCalback = new LoggingCalback(dragDirs, swipeDirs);
        mItemTouchHelper = new LoggingItemTouchHelper(mCalback);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mItemTouchHelper.attachToRecyclerView(mWrappedRecyclerView);
            }
        });

        return mWrappedRecyclerView;
    }

    public void testSwipeLeft() throws Throwable {
        basicSwipeTest(LEFT, LEFT | RIGHT, -getActivity().getWindow().getDecorView().getWidth());
    }

    public void testSwipeRight() throws Throwable {
        basicSwipeTest(RIGHT, LEFT | RIGHT, getActivity().getWindow().getDecorView().getWidth());
    }

    public void testSwipeStart() throws Throwable {
        basicSwipeTest(START, START | END, -getActivity().getWindow().getDecorView().getWidth());
    }

    public void testSwipeEnd() throws Throwable {
        basicSwipeTest(END, START | END, getActivity().getWindow().getDecorView().getWidth());
    }

    public void testSwipeStartInRTL() throws Throwable {
        mSetupRTL = true;
        basicSwipeTest(START, START | END, getActivity().getWindow().getDecorView().getWidth());
    }

    public void testSwipeEndInRTL() throws Throwable {
        mSetupRTL = true;
        basicSwipeTest(END, START | END, -getActivity().getWindow().getDecorView().getWidth());
    }

    private void setLayoutDirection(final View view, final int layoutDir) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewCompat.setLayoutDirection(view, layoutDir);
            }
        });
    }

    public void basicSwipeTest(int dir, int swipeDirs, int targetX) throws Throwable {
        final RecyclerView recyclerView = setup(0, swipeDirs);
        mLayoutManager.expectLayouts(1);
        setRecyclerView(recyclerView);
        mLayoutManager.waitForLayout(1);

        final RecyclerView.ViewHolder target = mRecyclerView
                .findViewHolderForAdapterPosition(1);
        TouchUtils.dragViewToX(this, target.itemView, Gravity.CENTER, targetX);
        Thread.sleep(100); //wait for animation end
        final SwipeRecord swipe = mCalback.getSwipe(target);
        assertNotNull(swipe);
        assertEquals(dir, swipe.dir);
        assertEquals(1, mItemTouchHelper.mRecoverAnimations.size());
        assertEquals(1, mItemTouchHelper.mPendingCleanup.size());
        // get rid of the view
        mLayoutManager.expectLayouts(1);
        mAdapter.deleteAndNotify(1, 1);
        mLayoutManager.waitForLayout(1);
        waitForAnimations();
        assertEquals(0, mItemTouchHelper.mRecoverAnimations.size());
        assertEquals(0, mItemTouchHelper.mPendingCleanup.size());
        assertTrue(mCalback.isCleared(target));
    }

    private void waitForAnimations() throws InterruptedException {
        while (mRecyclerView.getItemAnimator().isRunning()) {
            Thread.sleep(100);
        }
    }

    private static class LoggingCalback extends SimpleCallback {

        private List<MoveRecord> mMoveRecordList = new ArrayList<MoveRecord>();

        private List<SwipeRecord> mSwipeRecords = new ArrayList<SwipeRecord>();

        private List<RecyclerView.ViewHolder> mCleared = new ArrayList<RecyclerView.ViewHolder>();

        public LoggingCalback(int dragDirs, int swipeDirs) {
            super(dragDirs, swipeDirs);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                RecyclerView.ViewHolder target) {
            mMoveRecordList.add(new MoveRecord(viewHolder, target));
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            mSwipeRecords.add(new SwipeRecord(viewHolder, direction));
        }

        public MoveRecord getMove(RecyclerView.ViewHolder vh) {
            for (MoveRecord move : mMoveRecordList) {
                if (move.from == vh) {
                    return move;
                }
            }
            return null;
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            mCleared.add(viewHolder);
        }

        public SwipeRecord getSwipe(RecyclerView.ViewHolder vh) {
            for (SwipeRecord swipe : mSwipeRecords) {
                if (swipe.viewHolder == vh) {
                    return swipe;
                }
            }
            return null;
        }

        public boolean isCleared(RecyclerView.ViewHolder vh) {
            return mCleared.contains(vh);
        }
    }

    private static class LoggingItemTouchHelper extends ItemTouchHelper {

        public LoggingItemTouchHelper(Callback callback) {
            super(callback);
        }
    }

    private static class SwipeRecord {

        RecyclerView.ViewHolder viewHolder;

        int dir;

        public SwipeRecord(RecyclerView.ViewHolder viewHolder, int dir) {
            this.viewHolder = viewHolder;
            this.dir = dir;
        }
    }

    private static class MoveRecord {

        final int fromPos, toPos;

        RecyclerView.ViewHolder from, to;

        public MoveRecord(RecyclerView.ViewHolder from,
                RecyclerView.ViewHolder to) {
            this.from = from;
            this.to = to;
            fromPos = from.getAdapterPosition();
            toPos = to.getAdapterPosition();
        }
    }


    /**
     * RecyclerView specific TouchUtils.
     */
    static class TouchUtils {

        /**
         * Simulate touching the center of a view and releasing quickly (before the tap timeout).
         *
         * @param test The test case that is being run
         * @param v    The view that should be clicked
         */
        public static void tapView(InstrumentationTestCase test, RecyclerView recyclerView,
                View v) {
            int[] xy = new int[2];
            v.getLocationOnScreen(xy);

            final int viewWidth = v.getWidth();
            final int viewHeight = v.getHeight();

            final float x = xy[0] + (viewWidth / 2.0f);
            float y = xy[1] + (viewHeight / 2.0f);

            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis();

            MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                    MotionEvent.ACTION_DOWN, x, y, 0);
            Instrumentation inst = test.getInstrumentation();
            inst.sendPointerSync(event);
            inst.waitForIdleSync();

            eventTime = SystemClock.uptimeMillis();
            final int touchSlop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE,
                    x + (touchSlop / 2.0f), y + (touchSlop / 2.0f), 0);
            inst.sendPointerSync(event);
            inst.waitForIdleSync();

            eventTime = SystemClock.uptimeMillis();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
            inst.sendPointerSync(event);
            inst.waitForIdleSync();
        }

        /**
         * Simulate touching the center of a view and cancelling (so no onClick should
         * fire, etc).
         *
         * @param test The test case that is being run
         * @param v    The view that should be clicked
         */
        public static void touchAndCancelView(InstrumentationTestCase test, View v) {
            int[] xy = new int[2];
            v.getLocationOnScreen(xy);

            final int viewWidth = v.getWidth();
            final int viewHeight = v.getHeight();

            final float x = xy[0] + (viewWidth / 2.0f);
            float y = xy[1] + (viewHeight / 2.0f);

            Instrumentation inst = test.getInstrumentation();

            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis();

            MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                    MotionEvent.ACTION_DOWN, x, y, 0);
            inst.sendPointerSync(event);
            inst.waitForIdleSync();

            eventTime = SystemClock.uptimeMillis();
            final int touchSlop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_CANCEL,
                    x + (touchSlop / 2.0f), y + (touchSlop / 2.0f), 0);
            inst.sendPointerSync(event);
            inst.waitForIdleSync();

        }

        /**
         * Simulate touching the center of a view and releasing.
         *
         * @param test The test case that is being run
         * @param v    The view that should be clicked
         */
        public static void clickView(InstrumentationTestCase test, View v) {
            int[] xy = new int[2];
            v.getLocationOnScreen(xy);

            final int viewWidth = v.getWidth();
            final int viewHeight = v.getHeight();

            final float x = xy[0] + (viewWidth / 2.0f);
            float y = xy[1] + (viewHeight / 2.0f);

            Instrumentation inst = test.getInstrumentation();

            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis();

            MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                    MotionEvent.ACTION_DOWN, x, y, 0);
            inst.sendPointerSync(event);
            inst.waitForIdleSync();

            eventTime = SystemClock.uptimeMillis();
            final int touchSlop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE,
                    x + (touchSlop / 2.0f), y + (touchSlop / 2.0f), 0);
            inst.sendPointerSync(event);
            inst.waitForIdleSync();

            eventTime = SystemClock.uptimeMillis();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
            inst.sendPointerSync(event);
            inst.waitForIdleSync();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /**
         * Simulate touching the center of a view, holding until it is a long press, and then
         * releasing.
         *
         * @param test The test case that is being run
         * @param v    The view that should be clicked
         */
        public static void longClickView(InstrumentationTestCase test, View v) {
            int[] xy = new int[2];
            v.getLocationOnScreen(xy);

            final int viewWidth = v.getWidth();
            final int viewHeight = v.getHeight();

            final float x = xy[0] + (viewWidth / 2.0f);
            float y = xy[1] + (viewHeight / 2.0f);

            Instrumentation inst = test.getInstrumentation();

            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis();

            MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                    MotionEvent.ACTION_DOWN, x, y, 0);
            inst.sendPointerSync(event);
            inst.waitForIdleSync();

            eventTime = SystemClock.uptimeMillis();
            final int touchSlop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE,
                    x + touchSlop / 2, y + touchSlop / 2, 0);
            inst.sendPointerSync(event);
            inst.waitForIdleSync();

            try {
                Thread.sleep((long) (ViewConfiguration.getLongPressTimeout() * 1.5f));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            eventTime = SystemClock.uptimeMillis();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
            inst.sendPointerSync(event);
            inst.waitForIdleSync();
        }

        /**
         * Simulate touching the center of a view and dragging to the top of the screen.
         *
         * @param test The test case that is being run
         * @param v    The view that should be dragged
         */
        public static void dragViewToTop(InstrumentationTestCase test, View v) {
            dragViewToTop(test, v, 4);
        }

        /**
         * Simulate touching the center of a view and dragging to the top of the screen.
         *
         * @param test      The test case that is being run
         * @param v         The view that should be dragged
         * @param stepCount How many move steps to include in the drag
         */
        public static void dragViewToTop(InstrumentationTestCase test, View v, int stepCount) {
            int[] xy = new int[2];
            v.getLocationOnScreen(xy);

            final int viewWidth = v.getWidth();
            final int viewHeight = v.getHeight();

            final float x = xy[0] + (viewWidth / 2.0f);
            float fromY = xy[1] + (viewHeight / 2.0f);
            float toY = 0;

            drag(test, x, x, fromY, toY, stepCount);
        }

        /**
         * Get the location of a view. Use the gravity param to specify which part of the view to
         * return.
         *
         * @param v       View to find
         * @param gravity A combination of (TOP, CENTER_VERTICAL, BOTTOM) and (LEFT,
         *                CENTER_HORIZONTAL,
         *                RIGHT)
         * @param xy      Result
         */
        private static void getStartLocation(View v, int gravity, int[] xy) {
            v.getLocationOnScreen(xy);

            final int viewWidth = v.getWidth();
            final int viewHeight = v.getHeight();

            switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
                case Gravity.TOP:
                    break;
                case Gravity.CENTER_VERTICAL:
                    xy[1] += viewHeight / 2;
                    break;
                case Gravity.BOTTOM:
                    xy[1] += viewHeight - 1;
                    break;
                default:
                    // Same as top -- do nothing
            }

            switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                case Gravity.LEFT:
                    break;
                case Gravity.CENTER_HORIZONTAL:
                    xy[0] += viewWidth / 2;
                    break;
                case Gravity.RIGHT:
                    xy[0] += viewWidth - 1;
                    break;
                default:
                    // Same as left -- do nothing
            }
        }

        /**
         * Simulate touching a view and dragging it to a specified location.
         *
         * @param test    The test case that is being run
         * @param v       The view that should be dragged
         * @param gravity Which part of the view to use for the initial down event. A combination
         *                of
         *                (TOP, CENTER_VERTICAL, BOTTOM) and (LEFT, CENTER_HORIZONTAL, RIGHT)
         * @param toX     Final location of the view after dragging
         * @param toY     Final location of the view after dragging
         * @return distance in pixels covered by the drag
         */
        public static int dragViewTo(InstrumentationTestCase test, View v, int gravity, int toX,
                int toY) {
            int[] xy = new int[2];

            getStartLocation(v, gravity, xy);

            final int fromX = xy[0];
            final int fromY = xy[1];

            int deltaX = fromX - toX;
            int deltaY = fromY - toY;

            int distance = (int) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            drag(test, fromX, toX, fromY, toY, distance);

            return distance;
        }

        /**
         * Simulate touching a view and dragging it to a specified location. Only moves
         * horizontally.
         *
         * @param test    The test case that is being run
         * @param v       The view that should be dragged
         * @param gravity Which part of the view to use for the initial down event. A combination
         *                of
         *                (TOP, CENTER_VERTICAL, BOTTOM) and (LEFT, CENTER_HORIZONTAL, RIGHT)
         * @param toX     Final location of the view after dragging
         * @return distance in pixels covered by the drag
         */
        public static int dragViewToX(InstrumentationTestCase test, View v, int gravity, int toX) {
            int[] xy = new int[2];

            getStartLocation(v, gravity, xy);

            final int fromX = xy[0];
            final int fromY = xy[1];

            int deltaX = fromX - toX;

            drag(test, fromX, toX, fromY, fromY, Math.max(10, Math.abs(deltaX) / 10));

            return deltaX;
        }

        /**
         * Simulate touching a view and dragging it to a specified location. Only moves vertically.
         *
         * @param test    The test case that is being run
         * @param v       The view that should be dragged
         * @param gravity Which part of the view to use for the initial down event. A combination
         *                of
         *                (TOP, CENTER_VERTICAL, BOTTOM) and (LEFT, CENTER_HORIZONTAL, RIGHT)
         * @param toY     Final location of the view after dragging
         * @return distance in pixels covered by the drag
         */
        public static int dragViewToY(InstrumentationTestCase test, View v, int gravity, int toY) {
            int[] xy = new int[2];

            getStartLocation(v, gravity, xy);

            final int fromX = xy[0];
            final int fromY = xy[1];

            int deltaY = fromY - toY;

            drag(test, fromX, fromX, fromY, toY, deltaY);

            return deltaY;
        }


        /**
         * Simulate touching a specific location and dragging to a new location.
         *
         * @param test      The test case that is being run
         * @param fromX     X coordinate of the initial touch, in screen coordinates
         * @param toX       Xcoordinate of the drag destination, in screen coordinates
         * @param fromY     X coordinate of the initial touch, in screen coordinates
         * @param toY       Y coordinate of the drag destination, in screen coordinates
         * @param stepCount How many move steps to include in the drag
         */
        public static void drag(InstrumentationTestCase test, float fromX, float toX, float fromY,
                float toY, int stepCount) {
            Instrumentation inst = test.getInstrumentation();

            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis();

            float y = fromY;
            float x = fromX;

            float yStep = (toY - fromY) / stepCount;
            float xStep = (toX - fromX) / stepCount;

            MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                    MotionEvent.ACTION_DOWN, x, y, 0);
            inst.sendPointerSync(event);
            for (int i = 0; i < stepCount; ++i) {
                y += yStep;
                x += xStep;
                eventTime = SystemClock.uptimeMillis();
                event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y, 0);
                inst.sendPointerSync(event);
            }

            eventTime = SystemClock.uptimeMillis();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
            inst.sendPointerSync(event);
            inst.waitForIdleSync();
        }
    }

}
