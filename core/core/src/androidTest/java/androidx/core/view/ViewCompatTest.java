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

import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_ARGUMENT_PRESS_AND_HOLD_DURATION_MILLIS_INT;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.graphics.Insets;
import androidx.core.test.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Before;
import org.junit.Ignore;
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
        assertEquals("LTR constants", View.LAYOUT_DIRECTION_LTR, ViewCompat.LAYOUT_DIRECTION_LTR);
        assertEquals("RTL constants", View.LAYOUT_DIRECTION_RTL, ViewCompat.LAYOUT_DIRECTION_RTL);
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
    @SdkSuppress(minSdkVersion = 21)
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
            int generatedId = ViewCompat.generateViewId();
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
    @SdkSuppress(minSdkVersion = 20) // dispatchApplyWindowInsets only works on API 20+
    public void dispatchApplyWindowInsets_correctReturnValue() {
        final View view = mActivityTestRule.getActivity().findViewById(R.id.container);

        // Set an OnApplyWindowInsetsListener which returns consumed insets
        ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                return insets.consumeSystemWindowInsets();
            }
        });

        // Now create an inset instance and dispatch it to the view
        final WindowInsetsCompat insets = new WindowInsetsCompat.Builder()
                .setSystemWindowInsets(Insets.of(10, 20, 30, 40))
                .build();
        final WindowInsetsCompat result = ViewCompat.dispatchApplyWindowInsets(view, insets);

        // Assert that the return insets doesn't equal what we passed in, and it is consumed
        assertNotEquals(insets, result);
        assertTrue(result.isConsumed());
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    public void testPerformAction_ExpectedActionAndArguments() {
        AccessibilityActionCompat actionCompat = AccessibilityActionCompat.ACTION_PRESS_AND_HOLD;
        View view = mock(View.class);
        Bundle bundle = new Bundle();
        bundle.putInt(ACTION_ARGUMENT_PRESS_AND_HOLD_DURATION_MILLIS_INT, 100);

        ViewCompat.performAccessibilityAction(view, actionCompat.getId(), bundle);

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(view).performAccessibilityAction(eq(actionCompat.getId()), bundleCaptor.capture());
        assertEquals(100,
                bundleCaptor.getValue().getInt(ACTION_ARGUMENT_PRESS_AND_HOLD_DURATION_MILLIS_INT));
    }

    @Ignore
    @Test
    public void testGetWindowInsetsController_UnwrapsContextWrappers()
            throws Throwable {
        final ContextThemeWrapper wrapper = new ContextThemeWrapper(mActivityTestRule.getActivity(),
                0);


        final LayoutInflater inflater = LayoutInflater.from(wrapper);
        final View view = inflater.createView(View.class.getName(), null, null);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityTestRule.getActivity().setContentView(view);
            }
        });

        final WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(view);

        assertNotNull(controller);
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
                int dyUnconsumed, @Nullable int[] offsetInWindow) {
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
                int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
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
                int dyUnconsumed, @Nullable int[] offsetInWindow, int type,
                @NonNull int[] consumed) {
        }
    }
}
