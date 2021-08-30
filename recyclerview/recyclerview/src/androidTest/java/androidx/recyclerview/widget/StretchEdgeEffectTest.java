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
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.EdgeEffect;

import androidx.annotation.NonNull;
import androidx.core.view.InputDeviceCompat;
import androidx.core.widget.EdgeEffectCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class StretchEdgeEffectTest extends BaseRecyclerViewInstrumentationTest {
    private static final int NUM_ITEMS = 10;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;

    @Before
    public void setup() throws Throwable {
        mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.ensureLayoutState();

        mRecyclerView = new RecyclerView(getActivity());
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
        TestEdgeEffectFactory
                factory = new TestEdgeEffectFactory();
        mRecyclerView.setEdgeEffectFactory(factory);
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);
        scrollHorizontalBy(-3);
        if (isSOrHigher()) {
            assertTrue(EdgeEffectCompat.getDistance(factory.mLeft) > 0);
        }
        scrollHorizontalBy(4);
        assertEquals(0f, EdgeEffectCompat.getDistance(factory.mLeft), 0f);
        if (isSOrHigher()) {
            assertTrue(factory.mLeft.isFinished());
        }
    }

    /**
     * After pulling the edge effect, releasing should return the edge effect to 0.
     */
    @Test
    public void testTopEdgeEffectRetract() throws Throwable {
        TestEdgeEffectFactory
                factory = new TestEdgeEffectFactory();
        mRecyclerView.setEdgeEffectFactory(factory);
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);
        scrollVerticalBy(3);
        if (isSOrHigher()) {
            assertTrue(EdgeEffectCompat.getDistance(factory.mTop) > 0);
        }
        scrollVerticalBy(-4);
        assertEquals(0f, EdgeEffectCompat.getDistance(factory.mTop), 0f);
        if (isSOrHigher()) {
            assertTrue(factory.mTop.isFinished());
        }
    }

    /**
     * After pulling the edge effect, releasing should return the edge effect to 0.
     */
    @Test
    public void testRightEdgeEffectRetract() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        TestEdgeEffectFactory
                factory = new TestEdgeEffectFactory();
        mRecyclerView.setEdgeEffectFactory(factory);
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);
        scrollHorizontalBy(3);
        if (isSOrHigher()) {
            assertTrue(EdgeEffectCompat.getDistance(factory.mRight) > 0);
        }
        scrollHorizontalBy(-4);
        assertEquals(0f, EdgeEffectCompat.getDistance(factory.mRight), 0f);
        if (isSOrHigher()) {
            assertTrue(factory.mRight.isFinished());
        }
    }

    /**
     * After pulling the edge effect, releasing should return the edge effect to 0.
     */
    @Test
    public void testBottomEdgeEffectRetract() throws Throwable {
        TestEdgeEffectFactory factory = new TestEdgeEffectFactory();
        mRecyclerView.setEdgeEffectFactory(factory);
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);
        scrollVerticalBy(-3);
        if (isSOrHigher()) {
            assertTrue(EdgeEffectCompat.getDistance(factory.mBottom) > 0);
        }

        scrollVerticalBy(4);
        if (isSOrHigher()) {
            assertEquals(0f, EdgeEffectCompat.getDistance(factory.mBottom), 0f);
            assertTrue(factory.mBottom.isFinished());
        }
    }

    /**
     * A fling should be allowed during pull, but only for and earlier
     */
    @Test
    public void testFlingAfterStretchLeft() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        CaptureOnAbsorbFactory factory = new CaptureOnAbsorbFactory();
        mRecyclerView.setEdgeEffectFactory(factory);
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);
        scrollHorizontalBy(-3);

        if (isSOrHigher()) {
            // test flinging right
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(factory.mLeft);
                assertTrue(pullDistance > 0);
                assertTrue(mRecyclerView.fling(-1000, 0));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(factory.mLeft), 0.01f);
                assertEquals(1000, factory.mLeft.mAbsorbVelocity);
                // reset the edge effect
                factory.mLeft.finish();
            });

            scrollHorizontalBy(-3);

            // test flinging left
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(factory.mLeft);
                assertTrue(pullDistance > 0);
                assertTrue(mRecyclerView.fling(1000, 0));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(factory.mLeft), 0.01f);
                assertEquals(-1000, factory.mLeft.mAbsorbVelocity);
            });
        } else {
            // fling left and it should just scroll
            mActivityRule.runOnUiThread(() -> {
                assertEquals(0, mLayoutManager.findFirstVisibleItemPosition());
                assertTrue(mRecyclerView.fling(5000, 0));
                assertEquals(0, factory.mLeft.mAbsorbVelocity);
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
        CaptureOnAbsorbFactory factory = new CaptureOnAbsorbFactory();
        mRecyclerView.setEdgeEffectFactory(factory);
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);
        scrollVerticalBy(3);

        if (isSOrHigher()) {
            // test flinging down
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(factory.mTop);
                assertTrue(pullDistance > 0);
                assertTrue(mRecyclerView.fling(0, -1000));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(factory.mTop), 0.01f);
                assertEquals(1000, factory.mTop.mAbsorbVelocity);
                // reset the edge effect
                factory.mTop.finish();
            });

            scrollVerticalBy(3);

            // test flinging up
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(factory.mTop);
                assertTrue(pullDistance > 0);
                assertTrue(mRecyclerView.fling(0, 1000));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(factory.mTop), 0.01f);
                assertEquals(-1000, factory.mTop.mAbsorbVelocity);
            });
        } else {
            // fling up and it should just scroll
            mActivityRule.runOnUiThread(() -> {
                assertEquals(0, mLayoutManager.findFirstVisibleItemPosition());
                assertTrue(mRecyclerView.fling(0, 5000));
                assertEquals(0, factory.mTop.mAbsorbVelocity);
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
        CaptureOnAbsorbFactory factory = new CaptureOnAbsorbFactory();
        mRecyclerView.setEdgeEffectFactory(factory);
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);
        scrollHorizontalBy(3);

        if (isSOrHigher()) {
            // test flinging left
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(factory.mRight);
                assertTrue(pullDistance > 0);
                assertTrue(mRecyclerView.fling(1000, 0));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(factory.mRight), 0.01f);
                assertEquals(1000, factory.mRight.mAbsorbVelocity);
                // reset the edge effect
                factory.mRight.finish();
            });

            scrollHorizontalBy(3);

            // test flinging right
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(factory.mRight);
                assertTrue(pullDistance > 0);
                assertTrue(mRecyclerView.fling(-1000, 0));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(factory.mRight), 0.01f);
                assertEquals(-1000, factory.mRight.mAbsorbVelocity);
            });
        } else {
            // fling right and it should just scroll
            mActivityRule.runOnUiThread(() -> {
                assertEquals(mRecyclerView.getAdapter().getItemCount() - 1,
                        mLayoutManager.findLastVisibleItemPosition());
                assertTrue(mRecyclerView.fling(-5000, 0));
                assertEquals(0, factory.mRight.mAbsorbVelocity);
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
        CaptureOnAbsorbFactory factory = new CaptureOnAbsorbFactory();
        mRecyclerView.setEdgeEffectFactory(factory);
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);
        scrollVerticalBy(-3);

        if (isSOrHigher()) {
            // test flinging up
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(factory.mBottom);
                assertTrue(pullDistance > 0);
                assertTrue(mRecyclerView.fling(0, 1000));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(factory.mBottom), 0.01f);
                assertEquals(1000, factory.mBottom.mAbsorbVelocity);
                // reset the edge effect
                factory.mBottom.finish();
            });

            scrollVerticalBy(-3);

            // test flinging down
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(factory.mBottom);
                assertTrue(pullDistance > 0);
                assertTrue(mRecyclerView.fling(0, -1000));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(factory.mBottom), 0.01f);
                assertEquals(-1000, factory.mBottom.mAbsorbVelocity);
            });
        } else {
            // fling up and it should just scroll
            mActivityRule.runOnUiThread(() -> {
                assertEquals(mRecyclerView.getAdapter().getItemCount() - 1,
                        mLayoutManager.findLastVisibleItemPosition());
                assertTrue(mRecyclerView.fling(0, -5000));
                assertEquals(0, factory.mBottom.mAbsorbVelocity);
            });
            waitForIdleScroll(mRecyclerView);
            mActivityRule.runOnUiThread(() -> {
                assertTrue(mLayoutManager.findLastVisibleItemPosition()
                        < mRecyclerView.getAdapter().getItemCount() - 1);
            });
        }
    }

    private static boolean isSOrHigher() {
        // TODO(b/181171227): Simplify this
        int sdk = Build.VERSION.SDK_INT;
        return sdk > Build.VERSION_CODES.R
                || (sdk == Build.VERSION_CODES.R && Build.VERSION.PREVIEW_SDK_INT != 0);
    }

    private void scrollVerticalBy(final int value) throws Throwable {
        mActivityRule.runOnUiThread(() -> TouchUtils.scrollView(MotionEvent.AXIS_VSCROLL, value,
                InputDeviceCompat.SOURCE_CLASS_POINTER, mRecyclerView));
    }

    private void scrollHorizontalBy(final int value) throws Throwable {
        mActivityRule.runOnUiThread(() -> TouchUtils.scrollView(MotionEvent.AXIS_HSCROLL, value,
                InputDeviceCompat.SOURCE_CLASS_POINTER, mRecyclerView));
    }

    private class TestEdgeEffectFactory extends RecyclerView.EdgeEffectFactory {
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

    private class TestEdgeEffect extends EdgeEffect {

        private float mDistance;

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
    }

    private class CaptureOnAbsorbFactory extends RecyclerView.EdgeEffectFactory {
        CaptureOnAbsorb mTop, mBottom, mLeft, mRight;

        @NonNull
        @Override
        protected EdgeEffect createEdgeEffect(RecyclerView view, int direction) {
            CaptureOnAbsorb effect = new CaptureOnAbsorb(view.getContext());
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

    private static class CaptureOnAbsorb extends EdgeEffect {
        public int mAbsorbVelocity;

        CaptureOnAbsorb(Context context) {
            super(context);
        }

        @Override
        public void onAbsorb(int velocity) {
            super.onAbsorb(velocity);
            mAbsorbVelocity = velocity;
        }

    }
}
