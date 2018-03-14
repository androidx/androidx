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

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.InputDeviceCompat;
import androidx.core.view.ViewConfigurationCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class RecyclerViewOnGenericMotionEventTest {

    TestRecyclerView mRecyclerView;

    @Before
    public void setUp() throws Exception {
        mRecyclerView = new TestRecyclerView(getContext());
    }

    private Context getContext() {
        return InstrumentationRegistry.getContext();
    }

    private void layout() {
        mRecyclerView.layout(0, 0, 320, 320);
    }

    @Test
    public void rotaryEncoderVerticalScroll() {
        MockLayoutManager layoutManager = new MockLayoutManager(true, true);
        mRecyclerView.setLayoutManager(layoutManager);
        layout();
        TouchUtils.scrollView(
                MotionEvent.AXIS_SCROLL, 2, InputDeviceCompat.SOURCE_ROTARY_ENCODER, mRecyclerView);
        assertTotalScroll(0, (int) (-2f * getScaledVerticalScrollFactor()));
    }

    @Test
    public void rotaryEncoderHorizontalScroll() {
        // The encoder is one-dimensional, and can only scroll horizontally if vertical scrolling
        // is not enabled.
        MockLayoutManager layoutManager = new MockLayoutManager(true, false);
        mRecyclerView.setLayoutManager(layoutManager);
        layout();
        TouchUtils.scrollView(
                MotionEvent.AXIS_SCROLL, 2, InputDeviceCompat.SOURCE_ROTARY_ENCODER, mRecyclerView);
        assertTotalScroll((int) (2f * getScaledHorizontalScrollFactor()), 0);
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
        assertEquals("x total scroll", x, mRecyclerView.mTotalX);
        assertEquals("y total scroll", y, mRecyclerView.mTotalY);
    }

    private static MotionEvent obtainScrollMotionEvent(int axis, int axisValue, int inputDevice) {
        MotionEvent.PointerProperties[] pointerProperties = { new MotionEvent.PointerProperties() };
        MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
        coords.setAxisValue(axis, axisValue);
        MotionEvent.PointerCoords[] pointerCoords = { coords };
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

        TestRecyclerView(Context context) {
            super(context);
        }

        boolean scrollByInternal(int x, int y, MotionEvent ev) {
            mTotalX += x;
            mTotalY += y;
            return super.scrollByInternal(x, y, ev);
        }
    }
}
