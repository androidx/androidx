/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.wearable.view;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.wearable.view.util.MoreViewAssertions.approximateBottom;
import static android.support.wearable.view.util.MoreViewAssertions.approximateTop;
import static android.support.wearable.view.util.MoreViewAssertions.bottom;
import static android.support.wearable.view.util.MoreViewAssertions.left;
import static android.support.wearable.view.util.MoreViewAssertions.right;
import static android.support.wearable.view.util.MoreViewAssertions.screenBottom;
import static android.support.wearable.view.util.MoreViewAssertions.screenLeft;
import static android.support.wearable.view.util.MoreViewAssertions.screenRight;
import static android.support.wearable.view.util.MoreViewAssertions.screenTop;
import static android.support.wearable.view.util.MoreViewAssertions.top;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.wearable.test.R;
import android.support.wearable.view.util.WakeLockRule;
import android.util.DisplayMetrics;
import android.view.View;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BoxInsetLayoutTest {
    private static final float FACTOR = 0.146467f; //(1 - sqrt(2)/2)/2

    @Rule
    public final WakeLockRule wakeLock = new WakeLockRule();

    @Rule
    public final ActivityTestRule<LayoutTestActivity> activityRule = new ActivityTestRule<>(
            LayoutTestActivity.class, true, false);

    @Test
    public void testCase1() {
        activityRule.launchActivity(new Intent().putExtra(LayoutTestActivity
                .EXTRA_LAYOUT_RESOURCE_ID, R.layout.box_inset_layout_testcase_1));
        DisplayMetrics dm = InstrumentationRegistry.getTargetContext().getResources()
                .getDisplayMetrics();
        int boxInset = (int) (FACTOR * Math.min(dm.widthPixels, dm.heightPixels));

        int desiredPadding = 0;
        if (activityRule.getActivity().getResources().getConfiguration().isScreenRound()) {
            desiredPadding = boxInset;
        }

        // Child 1 is match_parent width and height
        // layout_box=right|bottom
        // Padding of boxInset should be added to the right and bottom sides only
        onView(withId(R.id.child1)).check(left(equalTo(0))).check(top(equalTo(0))).check(
                right(equalTo(dm.widthPixels - desiredPadding))).check(
                bottom(equalTo(dm.heightPixels - desiredPadding)));

        // Content 1 is is width and height match_parent
        // The bottom and right sides should be inset by boxInset pixels due to padding
        // on the parent view
        onView(withId(R.id.content1)).check(screenLeft(equalTo(0))).check(
                screenTop(equalTo(0))).check(
                screenRight(equalTo(dm.widthPixels - desiredPadding))).check(
                screenBottom(equalTo(dm.heightPixels - desiredPadding)));
    }

    @Test
    public void testCase2() {
        activityRule.launchActivity(
                new Intent().putExtra(LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                        R.layout.box_inset_layout_testcase_2));
        DisplayMetrics dm =
                InstrumentationRegistry.getTargetContext().getResources().getDisplayMetrics();
        int boxInset = (int) (FACTOR * Math.min(dm.widthPixels, dm.heightPixels));

        int desiredPadding = 0;
        if (activityRule.getActivity().getResources().getConfiguration().isScreenRound()) {
            desiredPadding = boxInset;
        }

        View child1 = activityRule.getActivity().findViewById(R.id.child1);
        View child2 = activityRule.getActivity().findViewById(R.id.child2);
        View child3 = activityRule.getActivity().findViewById(R.id.child3);
        View child4 = activityRule.getActivity().findViewById(R.id.child4);
        // Child 1 is width match_parent, height=60dp, gravity top
        // layout_box=all means it should have padding added to left, top and right
        onView(withId(R.id.child1)).check(left(is(equalTo(desiredPadding)))).check(
                top(is(equalTo(desiredPadding)))).check(
                right(is(equalTo(dm.widthPixels - desiredPadding)))).check(
                bottom(is(equalTo(desiredPadding + child1.getHeight()))));

        // Content 1 is width and height match_parent
        // the left top and right edges should be inset by boxInset pixels, due to
        // padding in the parent
        onView(withId(R.id.content1)).check(screenLeft(equalTo(desiredPadding))).check(
                screenTop(equalTo(desiredPadding))).check(
                screenRight(equalTo(dm.widthPixels - desiredPadding)));

        // Child 2 is width match_parent, height=60dp, gravity bottom
        // layout_box=all means it should have padding added to left, bottom and right
        onView(withId(R.id.child2)).check(left(is(equalTo(desiredPadding)))).check(
                top(is(equalTo(dm.heightPixels - desiredPadding - child2.getHeight())))).check(
                right(is(equalTo(dm.widthPixels - desiredPadding)))).check(
                bottom(is(equalTo(dm.heightPixels - desiredPadding))));

        // Content 2 is width and height match_parent
        // the left bottom and right edges should be inset by boxInset pixels, due to
        // padding in the parent
        onView(withId(R.id.content2)).check(screenLeft(equalTo(desiredPadding))).check(
                screenRight(equalTo(dm.widthPixels - desiredPadding))).check(
                screenBottom(equalTo(dm.heightPixels - desiredPadding)));

        // Child 3 is width wrap_content, height=20dp, gravity left|center_vertical.
        // layout_box=all means it should have padding added to left
        // marginLeft be ignored due to gravity and layout_box=all (screenLeft=0)
        onView(withId(R.id.child3)).check(left(is(equalTo(desiredPadding)))).check(approximateTop(
                is(closeTo((dm.heightPixels / 2 - child3.getHeight() / 2), 1)))).check(
                right(is(equalTo(desiredPadding + child3.getWidth())))).check(
                approximateBottom(is(closeTo((dm.heightPixels / 2 + child3.getHeight() / 2), 1))));

        // Content 3 width and height match_parent
        // the left edge should be offset from the screen edge by boxInset pixels, due to left on
        // the parent
        onView(withId(R.id.content3)).check(screenLeft(equalTo(desiredPadding)));

        // Child 4 is width wrap_content, height=20dp, gravity right|center_vertical.
        // layout_box=all means it should have padding added to right
        // it should have marginRight ignored due to gravity and layout_box=all (screenRight=max)
        onView(withId(R.id.child4)).check(
                left(is(dm.widthPixels - desiredPadding - child4.getWidth()))).check(approximateTop(
                is(closeTo((dm.heightPixels / 2 - child3.getHeight() / 2), 1)))).check(
                right(is(equalTo(dm.widthPixels - desiredPadding)))).check(
                approximateBottom(is(closeTo((dm.heightPixels / 2 + child4.getHeight() / 2), 1))));

        // Content 4 width and height wrap_content
        // the right edge should be offset from the screen edge by boxInset pixels, due to
        // right on the parent
        onView(withId(R.id.content4)).check(screenRight(equalTo(dm.widthPixels - desiredPadding)));
    }

    @Test
    public void testCase3() {
        activityRule.launchActivity(
                new Intent().putExtra(LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                        R.layout.box_inset_layout_testcase_3));
        DisplayMetrics dm =
                InstrumentationRegistry.getTargetContext().getResources().getDisplayMetrics();
        int boxInset = (int) (FACTOR * Math.min(dm.widthPixels, dm.heightPixels));

        int desiredPadding = 0;
        if (activityRule.getActivity().getResources().getConfiguration().isScreenRound()) {
            desiredPadding = boxInset;
        }

        View child1 = activityRule.getActivity().findViewById(R.id.child1);
        View child2 = activityRule.getActivity().findViewById(R.id.child2);
        View child3 = activityRule.getActivity().findViewById(R.id.child3);
        View child4 = activityRule.getActivity().findViewById(R.id.child4);

        // Child 1 is width and height wrap_content
        // gravity is top|left, position should be 0,0 on screen
        onView(withId(R.id.child1)).check(left(is(equalTo(desiredPadding)))).check(
                top(is(equalTo(desiredPadding)))).check(
                right(is(equalTo(desiredPadding + child1.getWidth())))).check(
                bottom(is(equalTo(desiredPadding + child1.getHeight()))));

        // Content 1 is width and height wrap_content
        // the left and top edges should be offset from the screen edges by boxInset pixels
        onView(withId(R.id.content1)).check(screenLeft(equalTo(desiredPadding))).check(
                screenTop(equalTo(desiredPadding)));

        // Child 2 is width and height wrap_content
        // gravity is top|right, position should be 0,max on screen
        onView(withId(R.id.child2)).check(
                left(is(equalTo(dm.widthPixels - desiredPadding - child2.getWidth())))).check(
                top(is(equalTo(desiredPadding)))).check(
                right(is(equalTo(dm.widthPixels - desiredPadding)))).check(
                bottom(is(equalTo(desiredPadding + child2.getHeight()))));

        // Content 2 is width and height wrap_content
        // the top and right edges should be offset from the screen edges by boxInset pixels
        onView(withId(R.id.content2)).check(screenTop(equalTo(desiredPadding))).check(
                screenRight(equalTo(dm.widthPixels - desiredPadding)));

        // Child 3 is width and height wrap_content
        // gravity is bottom|right, position should be max,max on screen
        onView(withId(R.id.child3)).check(
                left(is(equalTo(dm.widthPixels - desiredPadding - child3.getWidth())))).check(
                top(is(equalTo(dm.heightPixels - desiredPadding - child3.getHeight())))).check(
                right(is(equalTo(dm.widthPixels - desiredPadding)))).check(
                bottom(is(equalTo(dm.heightPixels - desiredPadding))));

        // Content 3 is width and height wrap_content
        // the right and bottom edges should be offset from the screen edges by boxInset pixels
        onView(withId(R.id.content3)).check(
                screenBottom(equalTo(dm.heightPixels - desiredPadding))).check(
                screenRight(equalTo(dm.widthPixels - desiredPadding)));

        // Child 4 is width and height wrap_content
        // gravity is bottom|left, position should be max,0 on screen
        onView(withId(R.id.child4)).check(left(is(equalTo(desiredPadding)))).check(
                top(is(equalTo(dm.heightPixels - desiredPadding - child4.getHeight())))).check(
                right(is(equalTo(desiredPadding + child4.getWidth())))).check(
                bottom(is(equalTo(dm.heightPixels - desiredPadding))));

        // Content 3 is width and height wrap_content
        // the bottom and left edges should be offset from the screen edges by boxInset pixels
        onView(withId(R.id.content4)).check(
                screenBottom(equalTo(dm.heightPixels - desiredPadding))).check(
                screenLeft(equalTo(desiredPadding)));
    }

    @Test
    public void testCase4() {
        activityRule.launchActivity(new Intent().putExtra(LayoutTestActivity
                .EXTRA_LAYOUT_RESOURCE_ID, R.layout.box_inset_layout_testcase_4));
        DisplayMetrics dm = InstrumentationRegistry.getTargetContext().getResources()
                .getDisplayMetrics();
        int boxInset = (int) (FACTOR * Math.min(dm.widthPixels, dm.heightPixels));

        int desiredPadding = 0;
        if (activityRule.getActivity().getResources().getConfiguration().isScreenRound()) {
            desiredPadding = boxInset;
        }

        View container = activityRule.getActivity().findViewById(R.id.container);
        View child1 = activityRule.getActivity().findViewById(R.id.child1);
        // Child 1 is match_parent width and wrap_content height
        // layout_box=right|left
        // Padding of boxInset should be added to the right and bottom sides only
        onView(withId(R.id.child1)).check(left(equalTo(desiredPadding))).check(
                top(equalTo(container.getTop()))).check(
                right(equalTo(dm.widthPixels - desiredPadding))).check(
                bottom(equalTo(container.getTop() + child1.getHeight())));
    }
}
