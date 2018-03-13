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

package androidx.coordinatorlayout.widget;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertTrue;

import android.os.Handler;
import android.os.Looper;
import android.support.test.annotation.UiThreadTest;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.filters.MediumTest;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.coordinatorlayout.custom.CustomBar;
import androidx.coordinatorlayout.custom.TestFloatingBehavior;
import androidx.coordinatorlayout.test.R;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@MediumTest
public class CoordinatorSnackbarWithButtonTest extends BaseDynamicCoordinatorLayoutTest {
    private View mBar;

    @After
    @UiThreadTest
    public void teardown() throws Throwable {
        mCoordinatorLayout.removeView(mBar);
    }

    /**
     * Returns the location of our bar on the screen.
     */
    private static int[] getBarLocationOnScreen() {
        final int[] location = new int[2];
        onView(isAssignableFrom(CustomBar.class)).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isEnabled();
            }

            @Override
            public String getDescription() {
                return "Bar matcher";
            }

            @Override
            public void perform(UiController uiController, View view) {
                view.getLocationOnScreen(location);
            }
        });
        return location;
    }

    /**
     * Helper method that verifies that the passed view is above the bar in the activity
     * window.
     */
    private static void verifyBarViewStacking(View view, int extraBottomMargin) {
        // Get location of bar in window
        final int[] barOnScreenXY = getBarLocationOnScreen();
        // Get location of passed view in window
        final int[] viewOnScreenXY = new int[2];
        view.getLocationOnScreen(viewOnScreenXY);

        // Compute the bottom visible edge of the view
        int viewBottom = viewOnScreenXY[1] + view.getHeight() - extraBottomMargin;
        int barTop = barOnScreenXY[1];
        // and verify that our view is above the bar
        assertTrue(viewBottom <= barTop);
    }

    private void addBar() {
        final CountDownLatch latch = new CountDownLatch(1);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                LayoutInflater inflater = LayoutInflater.from(mActivityTestRule.getActivity());
                mBar = inflater.inflate(R.layout.emulated_snackbar, mCoordinatorLayout, false);
                mCoordinatorLayout.addView(mBar);
                latch.countDown();
            }
        });
        try {
            assertTrue("Could not add emulated snackbar", latch.await(5, TimeUnit.SECONDS));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Test
    public void testBehaviorBasedSlidingFromLayoutAttribute() {
        // Use a layout in which a TextView child has Behavior object configured via
        // layout_behavior XML attribute
        onView(withId(R.id.coordinator_stub)).perform(
                inflateViewStub(R.layout.design_snackbar_behavior_layout_attr));

        // Create and show the bar
        addBar();

        final TextView textView = mCoordinatorLayout.findViewById(R.id.text);
        verifyBarViewStacking(textView, 0);
    }

    @Test
    public void testBehaviorBasedSlidingFromClassAnnotation() {
        // Use a layout in which a custom child view has Behavior object configured via
        // annotation on the class that extends TextView
        onView(withId(R.id.coordinator_stub)).perform(
                inflateViewStub(R.layout.design_snackbar_behavior_annotation));

        // Create and show the bar
        addBar();

        final TextView textView = mCoordinatorLayout.findViewById(R.id.text);
        verifyBarViewStacking(textView, 0);
    }

    @Test
    public void testBehaviorBasedSlidingFromClassInterface() {
        // Use a layout in which a custom child view has Behavior object configured via
        // the interface on the class that extends TextView
        onView(withId(R.id.coordinator_stub)).perform(
                inflateViewStub(R.layout.design_snackbar_behavior_interface));

        // Create and show the bar
        addBar();

        final TextView textView = mCoordinatorLayout.findViewById(R.id.text);
        verifyBarViewStacking(textView, 0);
    }

    @Test
    public void testBehaviorBasedSlidingFromRuntimeApiCall() {
        // Use a layout in which a TextView child doesn't have any configured Behavior
        onView(withId(R.id.coordinator_stub)).perform(
                inflateViewStub(R.layout.design_snackbar_behavior_runtime));

        // and configure that Behavior at runtime by setting it on its LayoutParams
        final TextView textView = mCoordinatorLayout.findViewById(R.id.text);
        final CoordinatorLayout.LayoutParams textViewLp =
                (CoordinatorLayout.LayoutParams) textView.getLayoutParams();
        textViewLp.setBehavior(new TestFloatingBehavior());

        // Create and show the bar
        addBar();

        verifyBarViewStacking(textView, 0);
    }
}
