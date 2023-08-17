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

package androidx.recyclerview.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.OverScroller;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.core.view.DifferentialMotionFlingController;
import androidx.core.view.DifferentialMotionFlingTarget;
import androidx.core.view.InputDeviceCompat;
import androidx.core.view.ViewConfigurationCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class RecyclerViewOnGenericMotionEventTest {

    TestRecyclerView mRecyclerView;
    TestDifferentialMotionFlingController mFlingController;

    @Before
    public void setUp() throws Exception {
        mRecyclerView = new TestRecyclerView(getContext());
        mFlingController = createDummyFlingController();
        mRecyclerView.mDifferentialMotionFlingController = mFlingController;
    }

    private Context getContext() {
        return ApplicationProvider.getApplicationContext();
    }

    private void layout() {
        mRecyclerView.layout(0, 0, 320, 320);
    }

    @Test
    public void rotaryEncoderVerticalScroll_nonLowResDevice() {
        mRecyclerView.mLowResRotaryEncoderFeature = false;
        MockLayoutManager layoutManager = new MockLayoutManager(true, true);
        mRecyclerView.setLayoutManager(layoutManager);
        layout();

        TouchUtils.scrollView(
                MotionEvent.AXIS_SCROLL, 2, InputDeviceCompat.SOURCE_ROTARY_ENCODER, mRecyclerView);

        assertTotalScroll(0, (int) (-2f * getScaledVerticalScrollFactor()),
                /* assertSmoothScroll= */ false);
        assertEquals(MotionEvent.AXIS_SCROLL, mFlingController.mLastAxis);
        assertEquals(mRecyclerView.mLastGenericMotionEvent, mFlingController.mLastMotionEvent);
    }

    @Test
    public void rotaryEncoderHorizontalScroll_nonLowResDevice() {
        mRecyclerView.mLowResRotaryEncoderFeature = false;
        // The encoder is one-dimensional, and can only scroll horizontally if vertical scrolling
        // is not enabled.
        MockLayoutManager layoutManager = new MockLayoutManager(true, false);
        mRecyclerView.setLayoutManager(layoutManager);
        layout();

        TouchUtils.scrollView(
                MotionEvent.AXIS_SCROLL, 2, InputDeviceCompat.SOURCE_ROTARY_ENCODER, mRecyclerView);

        assertTotalScroll((int) (2f * getScaledHorizontalScrollFactor()), 0,
                /* assertSmoothScroll= */ false);
        assertEquals(MotionEvent.AXIS_SCROLL, mFlingController.mLastAxis);
        assertEquals(mRecyclerView.mLastGenericMotionEvent, mFlingController.mLastMotionEvent);
    }

    @Test
    public void rotaryEncoderVerticalScroll_lowResDevice() {
        mRecyclerView.mLowResRotaryEncoderFeature = true;
        MockLayoutManager layoutManager = new MockLayoutManager(true, true);
        mRecyclerView.setLayoutManager(layoutManager);
        layout();
        TouchUtils.scrollView(
                MotionEvent.AXIS_SCROLL, 2, InputDeviceCompat.SOURCE_ROTARY_ENCODER, mRecyclerView);
        assertTotalScroll(0, (int) (-2f * getScaledVerticalScrollFactor()),
                /* assertSmoothScroll= */ true);
        assertNull(mFlingController.mLastMotionEvent);
    }

    @Test
    public void rotaryEncoderVerticalScroll_lowResDevice_backToBackScrollEvents() {
        mRecyclerView.mLowResRotaryEncoderFeature = true;
        MockLayoutManager layoutManager = new MockLayoutManager(true, true);
        mRecyclerView.setLayoutManager(layoutManager);
        layout();

        TouchUtils.scrollView(
                MotionEvent.AXIS_SCROLL, 2, InputDeviceCompat.SOURCE_ROTARY_ENCODER, mRecyclerView);
        OverScroller overScroller = mRecyclerView.mViewFlinger.mOverScroller;
        int remainingScroll = overScroller.getFinalY() - overScroller.getCurrY();
        TouchUtils.scrollView(
                MotionEvent.AXIS_SCROLL, 2, InputDeviceCompat.SOURCE_ROTARY_ENCODER, mRecyclerView);

        // The expected total scroll will be the amount corresponding to each of the two scroll
        // events, plus the amount of scroll remaining from the first scroll by the time the
        // second scroll was initiated.
        assertTotalScroll(0, (int) (-4f * getScaledVerticalScrollFactor()) + remainingScroll,
                /* assertSmoothScroll= */ true);
    }

    @Test
    public void rotaryEncoderHorizontalScroll_lowResDevice() {
        mRecyclerView.mLowResRotaryEncoderFeature = true;
        // The encoder is one-dimensional, and can only scroll horizontally if vertical scrolling
        // is not enabled.
        MockLayoutManager layoutManager = new MockLayoutManager(true, false);
        mRecyclerView.setLayoutManager(layoutManager);
        layout();
        TouchUtils.scrollView(
                MotionEvent.AXIS_SCROLL, 2, InputDeviceCompat.SOURCE_ROTARY_ENCODER, mRecyclerView);
        assertTotalScroll((int) (2f * getScaledHorizontalScrollFactor()), 0,
                /* assertSmoothScroll= */ true);
        assertNull(mFlingController.mLastMotionEvent);
    }

    @Test
    public void rotaryEncoderHorizontalScroll_lowResDevice_backToBackScrollEvents() {
        mRecyclerView.mLowResRotaryEncoderFeature = true;
        MockLayoutManager layoutManager = new MockLayoutManager(true, false);
        mRecyclerView.setLayoutManager(layoutManager);
        layout();

        TouchUtils.scrollView(
                MotionEvent.AXIS_SCROLL, 2, InputDeviceCompat.SOURCE_ROTARY_ENCODER, mRecyclerView);
        OverScroller overScroller = mRecyclerView.mViewFlinger.mOverScroller;
        int remainingScroll = overScroller.getFinalX() - overScroller.getCurrX();
        TouchUtils.scrollView(
                MotionEvent.AXIS_SCROLL, 2, InputDeviceCompat.SOURCE_ROTARY_ENCODER, mRecyclerView);

        // The expected total scroll will be the amount corresponding to each of the two scroll
        // events, plus the amount of scroll remaining from the first scroll by the time the
        // second scroll was initiated.
        assertTotalScroll((int) (4f * getScaledVerticalScrollFactor()) + remainingScroll, 0,
                /* assertSmoothScroll= */ true);
    }

    @Test
    public void pointerVerticalScroll() {
        MockLayoutManager layoutManager = new MockLayoutManager(true, true);
        mRecyclerView.setLayoutManager(layoutManager);
        layout();
        TouchUtils.scrollView(
                MotionEvent.AXIS_VSCROLL, 2, InputDeviceCompat.SOURCE_CLASS_POINTER, mRecyclerView);
        assertTotalScroll(0, (int) (-2f * getScaledVerticalScrollFactor()));
    }

    @Test
    public void pointerHorizontalScroll() {
        MockLayoutManager layoutManager = new MockLayoutManager(true, true);
        mRecyclerView.setLayoutManager(layoutManager);
        layout();
        TouchUtils.scrollView(
                MotionEvent.AXIS_HSCROLL, 2, InputDeviceCompat.SOURCE_CLASS_POINTER, mRecyclerView);
        assertTotalScroll((int) (2f * getScaledHorizontalScrollFactor()), 0);
    }

    @Test
    public void nonZeroScaledVerticalScrollFactor() {
        assertNotEquals(0, getScaledVerticalScrollFactor());
    }

    @Test
    public void nonZeroScaledHorizontalScrollFactor() {
        assertNotEquals(0, getScaledHorizontalScrollFactor());
    }

    private void assertTotalScroll(int x, int y) {
        assertTotalScroll(x, y, /* smoothScroll= */ false);
    }

    private void assertTotalScroll(int x, int y, boolean assertSmoothScroll) {
        if (assertSmoothScroll) {
            assertEquals("x total smooth scroll", x, mRecyclerView.mTotalSmoothX);
            assertEquals("y total smooth scroll", y, mRecyclerView.mTotalSmoothY);
        } else {
            assertEquals("x total scroll", x, mRecyclerView.mTotalX);
            assertEquals("y total scroll", y, mRecyclerView.mTotalY);
        }
    }

    private static MotionEvent obtainScrollMotionEvent(int axis, int axisValue, int inputDevice) {
        MotionEvent.PointerProperties[] pointerProperties = {new MotionEvent.PointerProperties()};
        MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
        coords.setAxisValue(axis, axisValue);
        MotionEvent.PointerCoords[] pointerCoords = {coords};
        float xPrecision = 1;
        float yPrecision = 1;
        int deviceId = 0;
        int edgeFlags = 0;
        int flags = 0;
        return MotionEvent.obtain(0, System.currentTimeMillis(), MotionEvent.ACTION_SCROLL,
                1, pointerProperties, pointerCoords, 0, 0, xPrecision, yPrecision, deviceId,
                edgeFlags, inputDevice, flags);
    }

    private float getScaledVerticalScrollFactor() {
        return ViewConfigurationCompat.getScaledVerticalScrollFactor(
                ViewConfiguration.get(getContext()), getContext());
    }

    private float getScaledHorizontalScrollFactor() {
        return ViewConfigurationCompat.getScaledHorizontalScrollFactor(
                ViewConfiguration.get(getContext()), getContext());
    }

    static class MockLayoutManager extends RecyclerView.LayoutManager {

        private final boolean mCanScrollHorizontally;

        private final boolean mCanScrollVertically;

        MockLayoutManager(boolean canScrollHorizontally, boolean canScrollVertically) {
            mCanScrollHorizontally = canScrollHorizontally;
            mCanScrollVertically = canScrollVertically;
        }

        @Override
        public RecyclerView.LayoutParams generateDefaultLayoutParams() {
            return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        @Override
        public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            return dx;
        }

        @Override
        public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            return dy;
        }

        @Override
        public boolean canScrollHorizontally() {
            return mCanScrollHorizontally;
        }

        @Override
        public boolean canScrollVertically() {
            return mCanScrollVertically;
        }
    }

    static class MockAdapter extends RecyclerView.Adapter {

        private int mCount = 0;

        MockAdapter(int count) {
            this.mCount = count;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MockViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return mCount;
        }
    }

    static class MockViewHolder extends RecyclerView.ViewHolder {
        MockViewHolder(View itemView) {
            super(itemView);
        }
    }

    private static class TestRecyclerView extends RecyclerView {
        int mTotalX = 0;
        int mTotalY = 0;

        int mTotalSmoothX = 0;
        int mTotalSmoothY = 0;

        MotionEvent mLastGenericMotionEvent;

        TestRecyclerView(Context context) {
            super(context);
        }

        @Override
        public boolean onGenericMotionEvent(MotionEvent ev) {
            mLastGenericMotionEvent = ev;
            return super.onGenericMotionEvent(ev);
        }

        boolean scrollByInternal(int x, int y, MotionEvent ev, int type) {
            mTotalX += x;
            mTotalY += y;
            return super.scrollByInternal(x, y, ev, type);
        }

        void smoothScrollBy(@Px int dx, @Px int dy, @Nullable Interpolator interpolator,
                int duration, boolean withNestedScrolling) {
            mTotalSmoothX += dx;
            mTotalSmoothY += dy;
            super.smoothScrollBy(dx, dy, interpolator, duration, withNestedScrolling);
        }
    }

    private TestDifferentialMotionFlingController createDummyFlingController() {
        return new TestDifferentialMotionFlingController(
                mRecyclerView.getContext(),
                new DifferentialMotionFlingTarget() {
                    @Override
                    public boolean startDifferentialMotionFling(float velocity) {
                        return false;
                    }

                    @Override
                    public void stopDifferentialMotionFling() {}

                    @Override
                    public float getScaledScrollFactor() {
                        return 0;
                    }
                });
    }

    private static class TestDifferentialMotionFlingController extends
            DifferentialMotionFlingController {
        MotionEvent mLastMotionEvent;
        int mLastAxis;

        TestDifferentialMotionFlingController(Context context,
                DifferentialMotionFlingTarget target) {
            super(context, target);
        }

        @Override
        public void onMotionEvent(MotionEvent event, int axis) {
            mLastMotionEvent = event;
            mLastAxis = axis;
        }
    }

}
