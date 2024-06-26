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

package androidx.pdf.util;


import static androidx.test.espresso.action.ViewActions.doubleClick;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.RequiresApi;
import androidx.pdf.TestActivity;
import androidx.pdf.util.GestureTracker.Gesture;
import androidx.pdf.util.GestureTracker.GestureHandler;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** On-device test for {@link GestureTrackingView}. */
@SuppressWarnings("deprecation")
@LargeTest
public class GestureTrackingViewTest {
    protected static final String TAG = "GestureTrackingViewTest";

    @Mock
    private PublicGestureHandler mViewGestureHandler;
    @Mock
    private PublicGestureHandler mContainerGestureHandler;

    @Rule
    public ActivityScenarioRule<TestActivity> activityRule =
            new ActivityScenarioRule<>(TestActivity.class);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }


    /**
     * Attaches a {@link GestureTrackingView} and a nested {@link View} to the test activity.
     *
     * @param interceptedGestures The {@link Gesture}s intercepted by the container View.
     */
    private void setUpViews(Gesture... interceptedGestures) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);

        activityRule.getScenario().onActivity(activity -> {
            final ContainerTestView container =
                    new ContainerTestView(activity, interceptedGestures);
            container.setLayoutParams(params);
            container.setId(1);
            container.setGestureHandler(mContainerGestureHandler);

            View plainView = new View(activity);
            plainView.setId(2);
            plainView.setBackgroundColor(Color.BLUE);
            plainView.setLayoutParams(params);

            GestureTracker viewTracker = new GestureTracker(activity);
            viewTracker.setDelegateHandler(mViewGestureHandler);
            plainView.setOnTouchListener(viewTracker);

            container.addView(plainView);

            activity.setContentView(container);

        });

    }

    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    @Test
    public void testInterceptDoubleTap() {
        setUpViews(Gesture.DOUBLE_TAP);

        when(mContainerGestureHandler.onSingleTapUp(any(MotionEvent.class))).thenReturn(true);
        when(mViewGestureHandler.onSingleTapUp(any(MotionEvent.class))).thenReturn(true);
        Espresso.onView(withId(2)).perform(doubleClick());
        verify(mViewGestureHandler).onGestureStart();
        verify(mViewGestureHandler).onGestureEnd(eq(Gesture.DOUBLE_TAP));
        verify(mViewGestureHandler, times(2)).onSingleTapUp(any(MotionEvent.class));
        verify(mViewGestureHandler).onDoubleTap(any(MotionEvent.class));
        verifyNoMoreInteractions(mViewGestureHandler);

        verify(mContainerGestureHandler).onDoubleTap(any(MotionEvent.class));
        verifyNoMoreInteractions(mContainerGestureHandler);
    }

    /**
     * A simple configurable {@link GestureTrackingView}.
     */
    private static final class ContainerTestView extends GestureTrackingView {

        private final Gesture[] mInterceptedGestures;

        private ContainerTestView(Context context, Gesture... gestures) {
            super(context);
            mInterceptedGestures = gestures;
        }

        public void setGestureHandler(GestureHandler handler) {
            mGestureTracker.setDelegateHandler(handler);
        }

        @Override
        protected boolean interceptGesture(GestureTracker gestureTracker) {
            return gestureTracker.matches(mInterceptedGestures);
        }
    }

    /** A {@link GestureHandler} adaptor that makes all methods public, so they can be mocked. */
    public static class PublicGestureHandler extends GestureHandler {

        @Override
        public void onGestureStart() {
            super.onGestureStart();
        }

        @Override
        public void onGestureEnd(Gesture gesture) {
            super.onGestureEnd(gesture);
        }
    }
}
