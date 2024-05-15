/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.widget;


import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import androidx.pdf.util.GestureTracker.Gesture;
import androidx.pdf.widget.ZoomView.ContentResizedMode;
import androidx.pdf.widget.ZoomView.FitMode;
import androidx.pdf.widget.ZoomView.RotateMode;
import androidx.pdf.widget.ZoomView.ZoomScroll;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayDeque;
import java.util.Queue;

/** Unit tests for {@link ZoomView}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
public class ZoomViewTest {

    @Mock
    private ScaleGestureDetector mMockGestureDetector;

    private SpecCapturingView mContentView;
    private ZoomView mZoomview;

    private AutoCloseable mOpenMocks;

    @Before
    public void setUp() {
        mOpenMocks = MockitoAnnotations.openMocks(this);

        FrameLayout parentView = new FrameLayout(ApplicationProvider.getApplicationContext());
        mZoomview = new ZoomView(ApplicationProvider.getApplicationContext(), null);
        parentView.addView(mZoomview);

        mContentView = new SpecCapturingView(ApplicationProvider.getApplicationContext());
        mContentView.setScaleX(1f);
        mContentView.setVisibility(View.VISIBLE);
        setContentViewSize(100, 200);

        mZoomview.addView(mContentView);
    }

    @After
    public void tearDown() throws Exception {
        mOpenMocks.close();
    }

    private void setContentViewSize(int width, int height) {
        mContentView.setLayoutParams(new LayoutParams(width, height));
        mContentView.measure(
                View.MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        mContentView.layout(0, 0, width, height);
        mContentView.clearCapturedSpecs();
    }

    @Test
    public void testCenterAt() {
        mZoomview.onLayout(true, 0, 0, 100, 100);
        mZoomview.centerAt(50, 100);
        ZoomScroll zoomScroll = mZoomview.zoomScroll().get();

        assertThat(zoomScroll.scrollX).isEqualTo(0);
        assertThat(zoomScroll.scrollY).isEqualTo(50);
    }

    @Test
    public void testScrollTo() {
        mZoomview.onLayout(true, 0, 0, 100, 100);
        mZoomview.scrollTo(0, 100, true);

        ZoomScroll zoomScroll = mZoomview.zoomScroll().get();
        assertThat(zoomScroll.scrollX).isEqualTo(0);
        assertThat(zoomScroll.scrollY).isEqualTo(100);
    }

    @Test
    public void testSetZoomCallsContentView() {
        mZoomview.setZoom(2f);

        assertThat(mContentView.getScaleX()).isEqualTo(2f);
        assertThat(mContentView.getScaleY()).isEqualTo(2f);
    }

    @Test
    public void testGetZoom_whenNullContentView_doesNotCrash() {
        mZoomview.removeAllViews();
        assertThat(mZoomview.getZoom()).isEqualTo(1.f);
    }

    @Test
    public void testRemoveAllViews() {
        assertThat(mZoomview.getChildCount()).isEqualTo(1);
        mZoomview.removeAllViews();
        assertThat(mZoomview.getChildCount()).isEqualTo(0);
    }

    @Test
    public void testZoomScrollBundle() {
        ZoomScroll zoomScroll = mZoomview.zoomScroll().get();
        Bundle bundle = zoomScroll.asBundle();
        assertThat(zoomScroll.equals(ZoomScroll.fromBundle(bundle))).isTrue();
    }

    @Test
    public void testMeasureChild() {
        mZoomview.measureChild(mContentView, 123, 456);
        mZoomview.measureChildWithMargins(mContentView, 12, 34, 56, 78);

        Pair<Integer, Integer> firstMeasurements = mContentView.mMeasurementsQueue.poll();
        Pair<Integer, Integer> secondMeasurements = mContentView.mMeasurementsQueue.poll();

        int unbounded = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        assertThat(firstMeasurements).isNotNull();
        assertThat(firstMeasurements.first).isEqualTo(unbounded);
        assertThat(firstMeasurements.second).isEqualTo(unbounded);
        assertThat(secondMeasurements).isNotNull();
        assertThat(secondMeasurements.first).isEqualTo(unbounded);
        assertThat(secondMeasurements.second).isEqualTo(unbounded);
    }

    @Test
    public void testOnLayout() {
        mZoomview.onLayout(true, 0, 0, 100, 200);
        assertThat(100).isEqualTo(mZoomview.getViewportWidth());
        assertThat(200).isEqualTo(mZoomview.getViewportHeight());
        assertThat(mZoomview.getUsableAreaInContentCoords()).isEqualTo(new Rect(0, 0, 100, 200));
        assertThat(mZoomview.getVisibleAreaInContentCoords()).isEqualTo(new Rect(0, 0, 100, 200));

        Rect layoutSpec = mContentView.mLayoutSpecQueue.poll();
        assertThat(
                layoutSpec).isNotNull(); // We only care to assert the child was laid out, not how
    }

    @Test
    public void testOnLayoutRotate_keepSameZoom_fitToBoth() {
        mZoomview.onLayout(true, 0, 0, 100, 200);
        mZoomview.setContentResizedMode(ContentResizedMode.KEEP_SAME_RELATIVE);
        mZoomview.setKeepFitZoomOnRotate(true);
        mZoomview.setRotateMode(RotateMode.KEEP_SAME_ZOOM);
        setContentViewSize(mContentView.getWidth(), 300);
        mZoomview.onLayout(true, 0, 0, 200, 100);

        assertThat(mZoomview.getViewportWidth()).isEqualTo(200);
        assertThat(mZoomview.getViewportHeight()).isEqualTo(100);

        assertThat(mZoomview.zoomScroll().get()).isEqualTo(new ZoomScroll(0.75f, -62, 63, true));
    }

    @Test
    public void testOnLayoutRotate_keepSameViewportWidth_fitToWidth() {
        mZoomview.onLayout(true, 0, 0, 100, 200);
        mZoomview.setContentResizedMode(ContentResizedMode.KEEP_SAME_RELATIVE);
        mZoomview.setKeepFitZoomOnRotate(false);
        mZoomview.setRotateMode(RotateMode.KEEP_SAME_VIEWPORT_WIDTH);
        mZoomview.setFitMode(FitMode.FIT_TO_WIDTH);
        setContentViewSize(mContentView.getWidth(), 300);
        mZoomview.onLayout(true, 0, 0, 200, 100);

        assertThat(mZoomview.getViewportWidth()).isEqualTo(200);
        assertThat(mZoomview.getViewportHeight()).isEqualTo(100);

        // Zoom in slightly to fit constant content width inside increased ZoomView width
        assertThat(mZoomview.zoomScroll().get()).isEqualTo(new ZoomScroll(1.50f, -25, 176, true));
    }

    @Test
    public void testOnLayoutRotate_keepSameViewportHeight_fitToHeight() {
        mZoomview.onLayout(true, 0, 0, 100, 200);
        mZoomview.setContentResizedMode(ContentResizedMode.KEEP_SAME_RELATIVE);
        mZoomview.setKeepFitZoomOnRotate(false);
        mZoomview.setRotateMode(RotateMode.KEEP_SAME_VIEWPORT_HEIGHT);
        mZoomview.setFitMode(FitMode.FIT_TO_HEIGHT);
        setContentViewSize(mContentView.getWidth(), 300);
        mZoomview.onLayout(true, 0, 0, 200, 100);

        assertThat(mZoomview.getViewportWidth()).isEqualTo(200);
        assertThat(mZoomview.getViewportHeight()).isEqualTo(100);

        // Zoom out to fit increased content height inside decreased ZoomView height
        assertThat(mZoomview.zoomScroll().get()).isEqualTo(new ZoomScroll(0.5f, -75, 25, true));
    }

    @Test
    public void testRestoreSavedPosition() throws InterruptedException {
        mZoomview.onLayout(true, 0, 0, 50, 100);
        mZoomview.scrollTo(50, 100, true);
        Parcelable bundle = mZoomview.onSaveInstanceState();
        mZoomview.scrollTo(0, 0, true);

        mZoomview.onRestoreInstanceState(bundle);
        mZoomview.onLayout(false, 0, 0, 50, 100);

        ZoomScroll zoomScroll = mZoomview.zoomScroll().get();
        assertThat(zoomScroll.scrollX).isEqualTo(50);
        assertThat(zoomScroll.scrollY).isEqualTo(100);
    }

    @Test
    public void testGestureHandlerOnScroll() {
        mZoomview.onLayout(true, 0, 0, 100, 100);
        boolean result = mZoomview.mGestureHandler.onScroll(null, null, 0, 100);

        assertThat(result).isTrue();
        ZoomScroll zoomScroll = mZoomview.zoomScroll().get();
        assertThat(zoomScroll.scrollX).isEqualTo(0);
        assertThat(zoomScroll.scrollY).isEqualTo(100);
    }

    @Test
    public void testGestureHandlerOnScrollStraightenVertical() {
        mZoomview.onLayout(true, 0, 0, 100, 100);
        mZoomview.setStraightenVerticalScroll(true);
        mZoomview.mGestureHandler.onGestureEnd(Gesture.ZOOM);
        mZoomview.mGestureHandler.onGestureStart();
        mZoomview.mGestureHandler.onScroll(null, null, 0, 0);
        mZoomview.mGestureHandler.onScroll(null, null, 0, 25);
        mZoomview.mGestureHandler.onScroll(null, null, 0, 50);
        mZoomview.mGestureHandler.onScroll(null, null, 0, 75);
        boolean result = mZoomview.mGestureHandler.onScroll(null, null, 0, 100);
        mZoomview.mGestureHandler.onGestureEnd(Gesture.DRAG_Y);
        assertThat(result).isTrue();
        ZoomScroll zoomScroll = mZoomview.zoomScroll().get();
        assertThat(zoomScroll.scrollX).isEqualTo(0);
        assertThat(zoomScroll.scrollY).isEqualTo(100);
    }

    @Test
    public void testGestureHandlerOnScale_whenMinMaxZoomEqual_returnsFalse() {
        mZoomview.setOverrideMinZoomToFit(false).setOverrideMaxZoomToFit(false).setMinZoom(1f)
                .setMaxZoom(1f);
        assertThat(mZoomview.mGestureHandler.onScale(mMockGestureDetector)).isFalse();
    }

    @Test
    public void testGestureHandlerOnScale_whenMinMaxZoomNotEqual_returnsTrueAndSetsZoom() {
        mZoomview.setOverrideMaxZoomToFit(true);
        mZoomview.setMinZoom(.01f).setMaxZoom(5f);
        float currentSpan = 200F;
        float prevSpan = 100F;
        float newScaleFactor =
                1.F + (currentSpan - prevSpan)
                        * mZoomview.mGestureHandler.mLinearScaleSpanMultiplier;
        when(mMockGestureDetector.getScaleFactor()).thenReturn(2f);
        when(mMockGestureDetector.getCurrentSpan()).thenReturn(currentSpan);
        when(mMockGestureDetector.getPreviousSpan()).thenReturn(prevSpan);
        assertThat(mZoomview.mGestureHandler.onScale(mMockGestureDetector)).isTrue();
        assertThat(mContentView.getScaleX()).isEqualTo(newScaleFactor);
        assertThat(mContentView.getScaleY()).isEqualTo(newScaleFactor);
    }

    @Test
    public void testGestureHandlerOnFling_always_returnsTrue() {
        assertThat(mZoomview.mGestureHandler.onFling(null, null, 23f, 23f)).isTrue();
    }

    @Test
    public void testGestureHandlerOnDoubleTap_whenMinMaxZoomAreEqual_returnsFalse() {
        mZoomview.setMinZoom(1f).setMaxZoom(1f);
        assertThat(mZoomview.mGestureHandler.onDoubleTap(null)).isFalse();
    }

    @Test
    public void testGestureHandlerOnDoubleTap_whenViewportNotInitialized_returnsFalse() {
        mZoomview.setMinZoom(1f).setMaxZoom(5f);
        assertThat(mZoomview.mGestureHandler.onDoubleTap(null)).isFalse();
    }

    @Test
    public void testGestureHandlerOnDoubleTap_whenViewportInitialized_returnsTrue() {
        mZoomview.setMinZoom(1f).setMaxZoom(5f);
        mZoomview.onLayout(true, 0, 0, 100, 100);
        MotionEvent event = MotionEvent.obtain(1, 0, MotionEvent.ACTION_DOWN, 3, 3, 0);
        assertThat(mZoomview.mGestureHandler.onDoubleTap(event)).isTrue();
    }

    @Test
    public void testGestureHandlerOnDoubleTap_whenDoubleTapIsDisabled_returnsFalse() {
        mZoomview.onLayout(true, 0, 0, 100, 100);
        mZoomview.setEnableDoubleTap(false);
        MotionEvent event = MotionEvent.obtain(1, 0, MotionEvent.ACTION_DOWN, 3, 3, 0);
        assertThat(mZoomview.mGestureHandler.onDoubleTap(event)).isFalse();
    }

    @Test
    public void testSetPadding() {
        mZoomview.setPadding(0, 1, 2, 3);

        assertThat(mZoomview.getPaddingLeft()).isEqualTo(0);
        assertThat(mZoomview.getPaddingTop()).isEqualTo(1);
        assertThat(mZoomview.getPaddingRight()).isEqualTo(2);
        assertThat(mZoomview.getPaddingBottom()).isEqualTo(3);
    }

    @Test
    public void testSetPadding_viewportUpdatedOnNextLayoutPass() {
        // Manually trigger an initial layout pass, changed == true for first layout.
        mZoomview.measure(0, 0);
        mZoomview.layout(0, 0, 100, 100);
        mZoomview.onLayout(true, 0, 0, 100, 100);

        // No padding, viewport is full width/height.
        assertThat(mZoomview.getViewportWidth()).isEqualTo(100);
        assertThat(mZoomview.getViewportHeight()).isEqualTo(100);

        mZoomview.setPadding(10, 10, 10, 10);

        // Manually trigger a layout pass that occurs post setPadding, changed == false as size and
        // position didn't change.
        mZoomview.measure(0, 0);
        mZoomview.layout(0, 0, 100, 100);
        mZoomview.onLayout(false, 0, 0, 100, 100);

        // Viewport reflects new padding: width - side padding and height - vertical padding.
        assertThat(mZoomview.getViewportWidth()).isEqualTo(80);
        assertThat(mZoomview.getViewportHeight()).isEqualTo(80);
    }

    /** Bare-bones fake {@link View} class that captures its measurements in a queue. */
    private static class SpecCapturingView extends View {

        /**
         * FIFO {@link Queue} of integer {@link Pair}s that correspond to height and width
         * measurements,
         * respectively, encoded using {@link View.MeasureSpec}
         */
        final Queue<Pair<Integer, Integer>> mMeasurementsQueue = new ArrayDeque<>();
        /**
         * FIFO {@link Queue} of {@link PointF} {@link Pair}s that correspond to the top-left and
         * bottom-right corners, respectively, of layout specs for this {@link View}.
         */
        final Queue<Rect> mLayoutSpecQueue = new ArrayDeque<>();

        SpecCapturingView(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            mMeasurementsQueue.add(Pair.create(widthMeasureSpec, heightMeasureSpec));
        }

        @Override
        public void layout(int l, int t, int r, int b) {
            super.layout(l, t, r, b);
            mLayoutSpecQueue.add(new Rect(l, t, r, b));
        }

        public void clearCapturedSpecs() {
            mMeasurementsQueue.clear();
            mLayoutSpecQueue.clear();
        }
    }
}
