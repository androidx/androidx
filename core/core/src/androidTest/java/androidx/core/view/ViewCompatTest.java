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
package androidx.core.view;

import static androidx.core.view.HapticFeedbackConstantsCompat.CLOCK_TICK;
import static androidx.core.view.HapticFeedbackConstantsCompat.CONFIRM;
import static androidx.core.view.HapticFeedbackConstantsCompat.CONTEXT_CLICK;
import static androidx.core.view.HapticFeedbackConstantsCompat.DRAG_START;
import static androidx.core.view.HapticFeedbackConstantsCompat.GESTURE_END;
import static androidx.core.view.HapticFeedbackConstantsCompat.GESTURE_START;
import static androidx.core.view.HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_ACTIVATE;
import static androidx.core.view.HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_DEACTIVATE;
import static androidx.core.view.HapticFeedbackConstantsCompat.KEYBOARD_RELEASE;
import static androidx.core.view.HapticFeedbackConstantsCompat.LONG_PRESS;
import static androidx.core.view.HapticFeedbackConstantsCompat.NO_HAPTICS;
import static androidx.core.view.HapticFeedbackConstantsCompat.REJECT;
import static androidx.core.view.HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK;
import static androidx.core.view.HapticFeedbackConstantsCompat.SEGMENT_TICK;
import static androidx.core.view.HapticFeedbackConstantsCompat.TEXT_HANDLE_MOVE;
import static androidx.core.view.HapticFeedbackConstantsCompat.TOGGLE_OFF;
import static androidx.core.view.HapticFeedbackConstantsCompat.TOGGLE_ON;
import static androidx.core.view.HapticFeedbackConstantsCompat.VIRTUAL_KEY;
import static androidx.core.view.HapticFeedbackConstantsCompat.VIRTUAL_KEY_RELEASE;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_ARGUMENT_PRESS_AND_HOLD_DURATION_MILLIS_INT;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.Display;
import android.view.View;
import android.view.contentcapture.ContentCaptureSession;
import android.widget.FrameLayout;

import androidx.core.graphics.Insets;
import androidx.core.test.R;
import androidx.core.view.autofill.AutofillIdCompat;
import androidx.core.view.contentcapture.ContentCaptureSessionCompat;
import androidx.core.viewtree.ViewTree;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ViewCompatTest extends BaseInstrumentationTestCase<ViewCompatActivity> {

    private View mView;

    public ViewCompatTest() {
        super(ViewCompatActivity.class);
    }

    @Before
    public void setUp() {
        final Activity activity = mActivityTestRule.getActivity();
        mView = activity.findViewById(R.id.view);
    }

    @Test
    public void testConstants() {
        // Compat constants must match core constants since they can be used interchangeably
        // in various support lib calls.
        assertEquals("LTR constants", View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_LTR);
        assertEquals("RTL constants", View.LAYOUT_DIRECTION_RTL, View.LAYOUT_DIRECTION_RTL);
    }

    @Test
    public void testGetDisplay() {
        final Display display = ViewCompat.getDisplay(mView);
        assertNotNull(display);
    }

    @Test
    public void testGetDisplay_returnsNullForUnattachedView() {
        final View view = new View(mActivityTestRule.getActivity());
        final Display display = ViewCompat.getDisplay(view);
        assertNull(display);
    }

    @Test
    public void testTransitionName() {
        final View view = new View(mActivityTestRule.getActivity());
        ViewCompat.setTransitionName(view, "abc");
        assertEquals("abc", ViewCompat.getTransitionName(view));
    }

    @Test
    public void testGetDisjointParentOfOverlay() throws Throwable {
        final Activity activity = mActivityTestRule.getActivity();
        FrameLayout overlayHost = new FrameLayout(activity);
        View overlaidView = new View(activity);

        mActivityTestRule.runOnUiThread(() -> {
            activity.setContentView(overlayHost);
            ViewCompat.addOverlayView(overlayHost, overlaidView);
        });

        assertEquals(
                overlayHost,
                ViewTree.getParentOrViewTreeDisjointParent((View) overlaidView.getParent())
        );
    }

    @Test
    public void  dispatchNestedScroll_viewIsNestedScrollingChild3_callsCorrectMethod() {
        final NestedScrollingChild3Impl nestedScrollingChild3Impl =
                mock(NestedScrollingChild3Impl.class);

        ViewCompat.dispatchNestedScroll(
                nestedScrollingChild3Impl,
                1,
                2,
                3,
                4,
                new int[]{5, 6},
                ViewCompat.TYPE_TOUCH,
                new int[]{9, 10});

        verify(nestedScrollingChild3Impl).dispatchNestedScroll(
                1,
                2,
                3,
                4,
                new int[]{5, 6},
                ViewCompat.TYPE_TOUCH,
                new int[]{9, 10});
        verify(nestedScrollingChild3Impl, never()).dispatchNestedScroll(
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(int[].class),
                anyInt());
        verify(nestedScrollingChild3Impl, never()).dispatchNestedScroll(
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(int[].class));
    }

    @Test
    public void  dispatchNestedScroll_viewIsNestedScrollingChild2_callsCorrectMethod() {
        final NestedScrollingChild2Impl nestedScrollingChild2Impl =
                mock(NestedScrollingChild2Impl.class);

        ViewCompat.dispatchNestedScroll(
                nestedScrollingChild2Impl,
                1,
                2,
                3,
                4,
                new int[]{5, 6},
                ViewCompat.TYPE_TOUCH,
                new int[]{9, 10});

        verify(nestedScrollingChild2Impl).dispatchNestedScroll(
                1,
                2,
                3,
                4,
                new int[]{5, 6},
                ViewCompat.TYPE_TOUCH);
        verify(nestedScrollingChild2Impl, never()).dispatchNestedScroll(
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(int[].class));
    }

    @Test
    public void  dispatchNestedScroll_viewIsNscTouchTypeNotTouch_callsNothing() {
        final NestedScrollingChildImpl nestedScrollingChildImpl =
                mock(NestedScrollingChildImpl.class);

        ViewCompat.dispatchNestedScroll(
                nestedScrollingChildImpl,
                11,
                2,
                3,
                4,
                new int[]{5, 6},
                ViewCompat.TYPE_NON_TOUCH,
                new int[]{9, 10});

        verify(nestedScrollingChildImpl, never()).dispatchNestedScroll(
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(int[].class));
    }

    @Test
    public void  dispatchNestedScroll_viewIsNotAndroidXNestedScrollingChild_callsCorrectMethod() {
        final ViewSubclass viewSubclass = mock(ViewSubclass.class);

        ViewCompat.dispatchNestedScroll(
                viewSubclass,
                1,
                2,
                3,
                4,
                new int[]{5, 6},
                ViewCompat.TYPE_TOUCH,
                new int[]{9, 10});

        verify(viewSubclass).dispatchNestedScroll(
                1,
                2,
                3,
                4,
                new int[]{5, 6});
    }

    @Test
    public void testGenerateViewId() {
        final int requestCount = 100;

        Set<Integer> generatedIds = new HashSet<>();
        for (int i = 0; i < requestCount; i++) {
            int generatedId = View.generateViewId();
            assertTrue(isViewIdGenerated(generatedId));
            generatedIds.add(generatedId);
        }

        assertThat(generatedIds.size(), equalTo(requestCount));
    }

    @Test
    public void testRequireViewByIdFound() {
        View container = mActivityTestRule.getActivity().findViewById(R.id.container);
        assertSame(mView, ViewCompat.requireViewById(container, R.id.view));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequireViewByIdMissing() {
        View container = mActivityTestRule.getActivity().findViewById(R.id.container);

        // action_bar isn't present inside container
        ViewCompat.requireViewById(container, R.id.action_bar);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequireViewByIdInvalid() {
        View container = mActivityTestRule.getActivity().findViewById(R.id.container);

        // NO_ID is always invalid
        ViewCompat.requireViewById(container, View.NO_ID);
    }

    @Test
    public void testSystemGestureExclusionRects() {
        final View container = mActivityTestRule.getActivity().findViewById(R.id.container);

        final List<Rect> expected = new ArrayList<>();
        expected.add(new Rect(0, 0, 25, 25));
        final List<Rect> rects = new ArrayList<>(expected);

        ViewCompat.setSystemGestureExclusionRects(container, rects);
        final List<Rect> returnedRects = ViewCompat.getSystemGestureExclusionRects(container);

        if (Build.VERSION.SDK_INT >= 29) {
            assertEquals("round trip for expected rects", expected, returnedRects);
        } else {
            assertTrue("empty list for old device", returnedRects.isEmpty());
        }
    }

    @Test
    @UiThreadTest
    public void dispatchApplyWindowInsets_correctReturnValue() {
        final View view = mActivityTestRule.getActivity().findViewById(R.id.container);

        // Set an OnApplyWindowInsetsListener which returns consumed insets
        ViewCompat.setOnApplyWindowInsetsListener(view,
                (v, insets) -> insets.consumeSystemWindowInsets());

        // Now create an inset instance and dispatch it to the view
        final WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                .setSystemWindowInsets(Insets.of(10, 20, 30, 40))
                .build();
        final WindowInsetsCompat result = ViewCompat.dispatchApplyWindowInsets(view, insets);

        // Assert that the return insets doesn't equal what we passed in, and it is consumed
        assertNotEquals(insets, result);
        assertTrue(result.isConsumed());
    }

    @Test
    public void testPerformAction_ExpectedActionAndArguments() {
        AccessibilityActionCompat actionCompat = AccessibilityActionCompat.ACTION_PRESS_AND_HOLD;
        View view = mock(View.class);
        Bundle bundle = new Bundle();
        bundle.putInt(ACTION_ARGUMENT_PRESS_AND_HOLD_DURATION_MILLIS_INT, 100);

        view.performAccessibilityAction(actionCompat.getId(), bundle);

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(view).performAccessibilityAction(eq(actionCompat.getId()), bundleCaptor.capture());
        assertEquals(100,
                bundleCaptor.getValue().getInt(ACTION_ARGUMENT_PRESS_AND_HOLD_DURATION_MILLIS_INT));
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    public void testGetAutofillId_returnsNullBelowSDK26() {
        Object result = ViewCompat.getAutofillId(mView);
        assertNull(result);
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testGetAutofillId_returnsAutofillIdAboveSDK26() {
        AutofillIdCompat result = ViewCompat.getAutofillId(mView);

        assertEquals(mView.getAutofillId(), result.toAutofillId());
    }

    @SdkSuppress(maxSdkVersion = 29)
    @Test
    public void testGetImportantForContentCapture_returnsZeroBelowSDK30() {
        int result = ViewCompat.getImportantForContentCapture(mView);
        assertEquals(0, result);
    }

    @SdkSuppress(minSdkVersion = 30)
    @Test
    public void testSetImportantForContentCapture_successAboveSDK30() {
        int result = ViewCompat.getImportantForContentCapture(mView);
        assertEquals(0, result);

        ViewCompat.setImportantForContentCapture(mView,
                ViewCompat.IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS);
        result = ViewCompat.getImportantForContentCapture(mView);
        assertEquals(ViewCompat.IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS, result);
    }

    @SdkSuppress(maxSdkVersion = 29)
    @Test
    public void testIsImportantForContentCapture_returnsFalseBelowSDK30() {
        boolean result = ViewCompat.isImportantForContentCapture(mView);
        assertFalse(result);
    }

    @SdkSuppress(minSdkVersion = 30)
    @Test
    public void testSetIsImportantForContentCapture_successAboveSDK30() {
        ViewCompat.setImportantForContentCapture(mView,
                ViewCompat.IMPORTANT_FOR_CONTENT_CAPTURE_YES);
        boolean result = ViewCompat.isImportantForContentCapture(mView);
        assertTrue(result);
    }

    @SdkSuppress(maxSdkVersion = 28)
    @Test
    public void testGetContentCaptureSession_returnsNullBelowSDK29() {
        Object result = ViewCompat.getContentCaptureSession(mView);
        assertNull(result);
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testGetContentCaptureSession_returnsNullWhenSessionNotSetAboveSDK29() {
        Object result = ViewCompat.getContentCaptureSession(mView);
        assertNull(result);
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testSetContentCaptureSession_successAboveSDK29() {
        ContentCaptureSession contentCaptureSession = mock(ContentCaptureSession.class);
        ViewCompat.setContentCaptureSession(mView,
                ContentCaptureSessionCompat.toContentCaptureSessionCompat(
                        contentCaptureSession, mView));
        ContentCaptureSessionCompat result = ViewCompat.getContentCaptureSession(mView);
        assertEquals(contentCaptureSession, result.toContentCaptureSession());
    }

    @Test
    public void testPerformHapticFeedback_skipsHapticFeedbackForNoHapticsConstant() {
        View spyView = spy(mView);
        ViewCompat.performHapticFeedback(spyView, NO_HAPTICS);
        verify(spyView, never()).performHapticFeedback(anyInt(), anyInt());
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    public void testPerformHapticFeedback_useSameInputFeedbackConstantOnSdk34() {
        for (int constant = HapticFeedbackConstantsCompat.FIRST_CONSTANT_INT;
                constant < HapticFeedbackConstantsCompat.LAST_CONSTANT_INT; constant++) {
            assertHapticFeedbackPerformed(constant);
        }
    }

    @SdkSuppress(minSdkVersion = 30, maxSdkVersion = 33)
    @Test
    public void testPerformHapticFeedback_useFallbackForConstantsFromSdk34() {
        // Maintain constants supported in SDK >= 30
        assertHapticFeedbackPerformed(CONFIRM);
        assertHapticFeedbackPerformed(REJECT);
        assertHapticFeedbackPerformed(GESTURE_START);
        assertHapticFeedbackPerformed(GESTURE_END);

        // Fallbacks for constants from SDK >= 34
        assertFallbackHapticFeedbackPerformed(LONG_PRESS, DRAG_START);
        assertFallbackHapticFeedbackPerformed(CONTEXT_CLICK, TOGGLE_ON);
        assertFallbackHapticFeedbackPerformed(CONTEXT_CLICK, SEGMENT_TICK);
        assertFallbackHapticFeedbackPerformed(CONTEXT_CLICK, GESTURE_THRESHOLD_ACTIVATE);
        assertFallbackHapticFeedbackPerformed(CLOCK_TICK, TOGGLE_OFF);
        assertFallbackHapticFeedbackPerformed(CLOCK_TICK, SEGMENT_FREQUENT_TICK);
        assertFallbackHapticFeedbackPerformed(CLOCK_TICK, GESTURE_THRESHOLD_DEACTIVATE);
    }

    @SdkSuppress(maxSdkVersion = 29)
    @Test
    public void testPerformHapticFeedback_useFallbackForConstantsFromSdk30() {
        // Maintain constants supported in SDK >= 23
        assertHapticFeedbackPerformed(CONTEXT_CLICK);

        if (Build.VERSION.SDK_INT >= 27) {
            // Maintain constants supported in SDK >= 27
            assertHapticFeedbackPerformed(TEXT_HANDLE_MOVE);
            assertHapticFeedbackPerformed(KEYBOARD_RELEASE);
            assertHapticFeedbackPerformed(VIRTUAL_KEY_RELEASE);
        } else {
            assertNoHapticFeedbackPerformed(TEXT_HANDLE_MOVE);
            assertNoHapticFeedbackPerformed(KEYBOARD_RELEASE);
            assertNoHapticFeedbackPerformed(VIRTUAL_KEY_RELEASE);
        }

        // Fallbacks for constants from SDK >= 30
        assertFallbackHapticFeedbackPerformed(LONG_PRESS, DRAG_START);
        assertFallbackHapticFeedbackPerformed(LONG_PRESS, REJECT);
        assertFallbackHapticFeedbackPerformed(VIRTUAL_KEY, CONFIRM);
        assertFallbackHapticFeedbackPerformed(VIRTUAL_KEY, GESTURE_START);
        assertFallbackHapticFeedbackPerformed(CONTEXT_CLICK, GESTURE_END);
        assertFallbackHapticFeedbackPerformed(CONTEXT_CLICK, TOGGLE_ON);
        assertFallbackHapticFeedbackPerformed(CONTEXT_CLICK, SEGMENT_TICK);
        assertFallbackHapticFeedbackPerformed(CONTEXT_CLICK, GESTURE_THRESHOLD_ACTIVATE);
        assertFallbackHapticFeedbackPerformed(CLOCK_TICK, TOGGLE_OFF);
        assertFallbackHapticFeedbackPerformed(CLOCK_TICK, SEGMENT_FREQUENT_TICK);
        assertFallbackHapticFeedbackPerformed(CLOCK_TICK, GESTURE_THRESHOLD_DEACTIVATE);
    }

    private void assertHapticFeedbackPerformed(int feedbackConstant) {
        View spyView = spy(mView);
        int flags = HapticFeedbackConstantsCompat.FLAG_IGNORE_VIEW_SETTING;

        ViewCompat.performHapticFeedback(spyView, feedbackConstant);
        verify(spyView).performHapticFeedback(eq(feedbackConstant));

        ViewCompat.performHapticFeedback(spyView, feedbackConstant, flags);
        verify(spyView).performHapticFeedback(eq(feedbackConstant), eq(flags));
    }

    private void assertNoHapticFeedbackPerformed(int feedbackConstant) {
        View spyView = spy(mView);
        int flags = HapticFeedbackConstantsCompat.FLAG_IGNORE_VIEW_SETTING;

        ViewCompat.performHapticFeedback(spyView, feedbackConstant);
        verify(spyView, never()).performHapticFeedback(anyInt());

        ViewCompat.performHapticFeedback(spyView, feedbackConstant, flags);
        verify(spyView, never()).performHapticFeedback(anyInt(), anyInt());
    }

    private void assertFallbackHapticFeedbackPerformed(int expectedFallback, int feedbackConstant) {
        View spyView = spy(mView);
        int flags = HapticFeedbackConstantsCompat.FLAG_IGNORE_VIEW_SETTING;

        ViewCompat.performHapticFeedback(spyView, feedbackConstant);
        verify(spyView).performHapticFeedback(eq(expectedFallback));

        ViewCompat.performHapticFeedback(spyView, feedbackConstant, flags);
        verify(spyView).performHapticFeedback(eq(expectedFallback), eq(flags));
    }

    private static boolean isViewIdGenerated(int id) {
        return (id & 0xFF000000) == 0 && (id & 0x00FFFFFF) != 0;
    }

    public abstract static class ViewSubclass extends View {
        public ViewSubclass(Context context) {
            super(context);
        }
    }

    public abstract static class NestedScrollingChildImpl extends ViewSubclass
            implements NestedScrollingChild{
        public NestedScrollingChildImpl(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, int @Nullable [] offsetInWindow) {
            return true;
        }
    }

    public abstract static class NestedScrollingChild2Impl extends NestedScrollingChildImpl
            implements NestedScrollingChild2 {

        public NestedScrollingChild2Impl(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, int @Nullable [] offsetInWindow, int type) {
            return true;
        }
    }

    public abstract static class NestedScrollingChild3Impl extends NestedScrollingChild2Impl
            implements NestedScrollingChild3 {

        public NestedScrollingChild3Impl(Context context) {
            super(context);
        }

        @Override
        public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, int @Nullable [] offsetInWindow, int type,
                int @NonNull [] consumed) {
        }
    }

    @Test
    public void testTransformMatrixToGlobal() throws Throwable {
        View container = mActivityTestRule.getActivity().findViewById(R.id.container);
        View view = mActivityTestRule.getActivity().findViewById(R.id.view);
        mActivityTestRule.runOnUiThread(() -> {
            container.setScrollX(1);
            container.setScrollY(2);
            container.setScaleX(3);
            container.setScaleY(4);
            container.setLeft(6);
            container.setRight(7);
            view.setScrollX(10);
            view.setScrollY(20);
            view.setScaleX(30);
            view.setScaleY(40);
            view.setLeft(60);
            view.setRight(70);
        });
        Matrix resultMatrix = new Matrix();
        ViewCompat.transformMatrixToGlobal(view, resultMatrix);
        float[] resultVals = new float[9];
        resultMatrix.getValues(resultVals);

        Matrix fallbackResultMatrix = new Matrix();
        ViewCompat.fallbackTransformMatrixToGlobal(view, fallbackResultMatrix);
        float[] fallbackResultVals = new float[9];
        fallbackResultMatrix.getValues(fallbackResultVals);

        int[] viewTopLeftScreenPositionOldInt = new int[2];
        view.getLocationOnScreen(viewTopLeftScreenPositionOldInt);
        float[] viewTopLeftScreenPositionOld = new float[]{
                viewTopLeftScreenPositionOldInt[0], viewTopLeftScreenPositionOldInt[1]};
        float[] viewTopLeftScreenPositionNew = new float[]{0.0f, 0.0f};
        resultMatrix.mapPoints(viewTopLeftScreenPositionNew);
        assertArrayEquals(viewTopLeftScreenPositionOld, viewTopLeftScreenPositionNew, 1.0f);

        Matrix expectedMatrix = new Matrix();
        expectedMatrix.setScale(30.0f * 3.0f, 40.0f * 4.0f);
        expectedMatrix.postTranslate(
                viewTopLeftScreenPositionOld[0], viewTopLeftScreenPositionOld[1]);
        float[] expectedVals = new float[9];
        expectedMatrix.getValues(expectedVals);
        assertArrayEquals(expectedVals, resultVals, 1.0f);
        assertArrayEquals(expectedVals, fallbackResultVals, 1.0f);
    }
}
