/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.recyclerview.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.EdgeEffect;

import androidx.annotation.NonNull;
import androidx.core.os.BuildCompat;
import androidx.core.view.InputDeviceCompat;
import androidx.core.widget.EdgeEffectCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class StretchEdgeEffectTest extends BaseRecyclerViewInstrumentationTest {
    private static final int NUM_ITEMS = 10;

    private TestRecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private TestEdgeEffectFactory mFactory;

    @Before
    public void setup() throws Throwable {
        mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.ensureLayoutState();

        mRecyclerView = new TestRecyclerView(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(new TestAdapter(NUM_ITEMS) {

            @Override
            public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                    int viewType) {
                TestViewHolder holder = super.onCreateViewHolder(parent, viewType);
                holder.itemView.setMinimumHeight(mRecyclerView.getMeasuredHeight() * 2 / NUM_ITEMS);
                holder.itemView.setMinimumWidth(mRecyclerView.getMeasuredWidth() * 2 / NUM_ITEMS);
                return holder;
            }
        });
        mFactory = new TestEdgeEffectFactory();
        mRecyclerView.setEdgeEffectFactory(mFactory);
        setRecyclerView(mRecyclerView);
        getInstrumentation().waitForIdleSync();
        assertThat("Assumption check", mRecyclerView.getChildCount() > 0, is(true));
    }

    /**
     * After pulling the edge effect, releasing should return the edge effect to 0.
     */
    @Test
    public void testLeftEdgeEffectRetract() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);
        scrollHorizontalBy(-3);
        if (BuildCompat.isAtLeastS()) {
            assertTrue(EdgeEffectCompat.getDistance(mFactory.mLeft) > 0);
        }
        scrollHorizontalBy(4);
        assertEquals(0f, EdgeEffectCompat.getDistance(mFactory.mLeft), 0f);
        if (BuildCompat.isAtLeastS()) {
            assertTrue(mFactory.mLeft.isFinished());
        }
    }

    /**
     * After pulling the edge effect, releasing should return the edge effect to 0.
     */
    @Test
    public void testTopEdgeEffectRetract() throws Throwable {
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);
        scrollVerticalBy(3);
        if (BuildCompat.isAtLeastS()) {
            assertTrue(EdgeEffectCompat.getDistance(mFactory.mTop) > 0);
        }
        scrollVerticalBy(-4);
        assertEquals(0f, EdgeEffectCompat.getDistance(mFactory.mTop), 0f);
        if (BuildCompat.isAtLeastS()) {
            assertTrue(mFactory.mTop.isFinished());
        }
    }

    /**
     * After pulling the edge effect, releasing should return the edge effect to 0.
     */
    @Test
    public void testRightEdgeEffectRetract() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);
        scrollHorizontalBy(3);
        if (BuildCompat.isAtLeastS()) {
            assertTrue(EdgeEffectCompat.getDistance(mFactory.mRight) > 0);
        }
        scrollHorizontalBy(-4);
        assertEquals(0f, EdgeEffectCompat.getDistance(mFactory.mRight), 0f);
        if (BuildCompat.isAtLeastS()) {
            assertTrue(mFactory.mRight.isFinished());
        }
    }

    /**
     * After pulling the edge effect, releasing should return the edge effect to 0.
     */
    @Test
    public void testBottomEdgeEffectRetract() throws Throwable {
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);
        scrollVerticalBy(-3);
        if (BuildCompat.isAtLeastS()) {
            assertTrue(EdgeEffectCompat.getDistance(mFactory.mBottom) > 0);
        }

        scrollVerticalBy(4);
        if (BuildCompat.isAtLeastS()) {
            assertEquals(0f, EdgeEffectCompat.getDistance(mFactory.mBottom), 0f);
            assertTrue(mFactory.mBottom.isFinished());
        }
    }

    /**
     * A fling should be allowed during pull, but only for S and later.
     */
    @Test
    public void testFlingAfterStretchLeft() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);

        // test flinging right
        dragHorizontally(1000);

        if (BuildCompat.isAtLeastS()) {
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mLeft);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(-1000, 0));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mLeft), 0.01f);
                assertEquals(1000, mFactory.mLeft.mAbsorbVelocity);
                // reset the edge effect
                mFactory.mLeft.finish();
            });

            dragHorizontally(1000);

            // test flinging left
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mLeft);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(1000, 0));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mLeft), 0.01f);
                assertEquals(-1000, mFactory.mLeft.mAbsorbVelocity);
            });
        } else {
            mActivityRule.runOnUiThread(() -> {
                assertEquals(0, mFactory.mLeft.mAbsorbVelocity);
            });

            dragHorizontally(1000);

            // fling left and it should just scroll
            mActivityRule.runOnUiThread(() -> {
                assertEquals(0, mLayoutManager.findFirstVisibleItemPosition());
                assertTrue(mRecyclerView.fling(5000, 0));
                assertEquals(0, mFactory.mLeft.mAbsorbVelocity);
            });
            waitForIdleScroll(mRecyclerView);
            mActivityRule.runOnUiThread(() -> {
                assertTrue(mLayoutManager.findFirstVisibleItemPosition() > 0);
            });
        }
    }

    /**
     * A fling should be allowed during pull.
     */
    @Test
    public void testFlingAfterStretchTop() throws Throwable {
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);

        Thread.sleep(1000);
        // test flinging down
        dragVertically(1000);
        Thread.sleep(1000);

        if (BuildCompat.isAtLeastS()) {
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mTop);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(0, -1000));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mTop), 0.01f);
                assertEquals(1000, mFactory.mTop.mAbsorbVelocity);
                // reset the edge effect
                mFactory.mTop.finish();
            });

            dragVertically(1000);

            // test flinging up
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mTop);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(0, 1000));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mTop), 0.01f);
                assertEquals(-1000, mFactory.mTop.mAbsorbVelocity);
            });
        } else {
            mActivityRule.runOnUiThread(() -> {
                assertEquals(0, mFactory.mTop.mAbsorbVelocity);
            });

            dragVertically(1000);

            // fling up and it should just scroll
            mActivityRule.runOnUiThread(() -> {
                assertEquals(0, mLayoutManager.findFirstVisibleItemPosition());
                assertTrue(mRecyclerView.fling(0, 5000));
                assertEquals(0, mFactory.mTop.mAbsorbVelocity);
            });
            waitForIdleScroll(mRecyclerView);
            mActivityRule.runOnUiThread(() -> {
                assertTrue(mLayoutManager.findFirstVisibleItemPosition() > 0);
            });
        }
    }

    /**
     * A fling should be allowed during pull.
     */
    @Test
    public void testFlingAfterStretchRight() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);

        // test flinging left
        dragHorizontally(-1000);

        if (BuildCompat.isAtLeastS()) {
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mRight);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(1000, 0));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mRight), 0.01f);
                assertEquals(1000, mFactory.mRight.mAbsorbVelocity);
                // reset the edge effect
                mFactory.mRight.finish();
            });

            dragHorizontally(-1000);

            // test flinging right
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mRight);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(-1000, 0));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mRight), 0.01f);
                assertEquals(-1000, mFactory.mRight.mAbsorbVelocity);
            });
        } else {
            mActivityRule.runOnUiThread(() -> {
                assertEquals(0, mFactory.mRight.mAbsorbVelocity);
            });

            dragHorizontally(-1000);

            // fling right and it should just scroll
            mActivityRule.runOnUiThread(() -> {
                assertEquals(mRecyclerView.getAdapter().getItemCount() - 1,
                        mLayoutManager.findLastVisibleItemPosition());
                assertTrue(mRecyclerView.fling(-5000, 0));
                assertEquals(0, mFactory.mRight.mAbsorbVelocity);
            });
            waitForIdleScroll(mRecyclerView);
            mActivityRule.runOnUiThread(() -> {
                assertTrue(mLayoutManager.findLastVisibleItemPosition()
                        < mRecyclerView.getAdapter().getItemCount() - 1);
            });

        }
    }

    /**
     * A fling should be allowed during pull.
     */
    @Test
    public void testFlingAfterStretchBottom() throws Throwable {
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);

        // test flinging up
        dragVertically(-1000);

        if (BuildCompat.isAtLeastS()) {
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mBottom);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(0, 1000));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mBottom), 0.01f);
                assertEquals(1000, mFactory.mBottom.mAbsorbVelocity);
                // reset the edge effect
                mFactory.mBottom.finish();
            });

            dragVertically(-1000);

            // test flinging down
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mBottom);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(0, -1000));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mBottom), 0.01f);
                assertEquals(-1000, mFactory.mBottom.mAbsorbVelocity);
            });
        } else {
            mActivityRule.runOnUiThread(() -> {
                assertEquals(0, mFactory.mBottom.mAbsorbVelocity);
            });

            dragVertically(-1000);

            // fling down and it should just scroll
            mActivityRule.runOnUiThread(() -> {
                assertEquals(mRecyclerView.getAdapter().getItemCount() - 1,
                        mLayoutManager.findLastVisibleItemPosition());
                assertTrue(mRecyclerView.fling(0, -5000));
                assertEquals(0, mFactory.mBottom.mAbsorbVelocity);
            });
            waitForIdleScroll(mRecyclerView);
            mActivityRule.runOnUiThread(() -> {
                assertTrue(mLayoutManager.findLastVisibleItemPosition()
                        < mRecyclerView.getAdapter().getItemCount() - 1);
            });
        }
    }

    @Test
    public void testScrollState() throws Throwable {
        // Drag down and it should only activate over scroll
        dragVertically(1000);
        waitForIdleScroll(mRecyclerView);

        mActivityRule.runOnUiThread(() -> {
            List<Integer> scrollStates = mRecyclerView.scrollStates;
            assertTrue(scrollStates.size() >= 2);
            assertEquals(RecyclerView.SCROLL_STATE_DRAGGING, (int) scrollStates.get(0));
            assertEquals(
                    RecyclerView.SCROLL_STATE_IDLE,
                    (int) scrollStates.get(scrollStates.size() - 1)
            );
        });
    }

    private void scrollVerticalBy(final int value) throws Throwable {
        mActivityRule.runOnUiThread(() -> TouchUtils.scrollView(MotionEvent.AXIS_VSCROLL, value,
                InputDeviceCompat.SOURCE_CLASS_POINTER, mRecyclerView));
    }

    private void scrollHorizontalBy(final int value) throws Throwable {
        mActivityRule.runOnUiThread(() -> TouchUtils.scrollView(MotionEvent.AXIS_HSCROLL, value,
                InputDeviceCompat.SOURCE_CLASS_POINTER, mRecyclerView));
    }

    private void dragVertically(int amount) {
        drag(0, amount);
    }

    private void dragHorizontally(int amount) {
        drag(amount, 0);
    }

    private void drag(int deltaX, int deltaY) {
        float centerX = mRecyclerView.getWidth() / 2f;
        float centerY = mRecyclerView.getHeight() / 2f;
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN,
                centerX, centerY, 0);
        mActivityRule.runOnUiThread(() -> mRecyclerView.dispatchTouchEvent(down));
        for (int i = 0; i < 10; i++) {
            float x = centerX + (deltaX * (i + 1) / 10f);
            float y = centerY + (deltaY * (i + 1) / 10f);
            MotionEvent move = MotionEvent.obtain(0, (16 * i) + 16, MotionEvent.ACTION_MOVE,
                    x, y, 0);
            mActivityRule.runOnUiThread(() -> mRecyclerView.dispatchTouchEvent(move));
        }
        MotionEvent up = MotionEvent.obtain(0, 160, MotionEvent.ACTION_UP,
                centerX + deltaX, centerY + deltaY, 0);
        mActivityRule.runOnUiThread(() -> mRecyclerView.dispatchTouchEvent(up));
    }

    private static class TestEdgeEffectFactory extends RecyclerView.EdgeEffectFactory {
        TestEdgeEffect mTop, mBottom, mLeft, mRight;

        @NonNull
        @Override
        protected EdgeEffect createEdgeEffect(RecyclerView view, int direction) {
            TestEdgeEffect effect = new TestEdgeEffect(view.getContext());
            switch (direction) {
                case DIRECTION_LEFT:
                    mLeft = effect;
                    break;
                case DIRECTION_TOP:
                    mTop = effect;
                    break;
                case DIRECTION_RIGHT:
                    mRight = effect;
                    break;
                case DIRECTION_BOTTOM:
                    mBottom = effect;
                    break;
            }
            return effect;
        }
    }

    private static class TestEdgeEffect extends EdgeEffect {

        private float mDistance;
        public int mAbsorbVelocity;

        TestEdgeEffect(Context context) {
            super(context);
        }

        @Override
        public void onPull(float deltaDistance, float displacement) {
            onPull(deltaDistance);
        }

        @Override
        public void onPull(float deltaDistance) {
            mDistance += deltaDistance;
        }

        @Override
        public float onPullDistance(float deltaDistance, float displacement) {
            float maxDelta = Math.max(-mDistance, deltaDistance);
            onPull(maxDelta);
            return maxDelta;
        }

        @Override
        public float getDistance() {
            return mDistance;
        }

        @Override
        public void finish() {
            super.finish();
            mDistance = 0;
        }

        @Override
        public boolean isFinished() {
            return mDistance == 0;
        }

        @Override
        public void onAbsorb(int velocity) {
            mAbsorbVelocity = velocity;
        }
    }

    private static class TestRecyclerView extends RecyclerView {
        public List<Integer> scrollStates = new ArrayList<Integer>();

        TestRecyclerView(@NonNull Context context) {
            super(context);
        }

        @Override
        public void onScrollStateChanged(int state) {
            super.onScrollStateChanged(state);
            scrollStates.add(state);
        }
    }
}
