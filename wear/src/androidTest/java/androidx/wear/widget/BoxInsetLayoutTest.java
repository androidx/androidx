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

package androidx.wear.widget;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static androidx.wear.widget.util.MoreViewAssertions.approximateBottom;
import static androidx.wear.widget.util.MoreViewAssertions.approximateTop;
import static androidx.wear.widget.util.MoreViewAssertions.bottom;
import static androidx.wear.widget.util.MoreViewAssertions.left;
import static androidx.wear.widget.util.MoreViewAssertions.right;
import static androidx.wear.widget.util.MoreViewAssertions.screenBottom;
import static androidx.wear.widget.util.MoreViewAssertions.screenLeft;
import static androidx.wear.widget.util.MoreViewAssertions.screenRight;
import static androidx.wear.widget.util.MoreViewAssertions.screenTop;
import static androidx.wear.widget.util.MoreViewAssertions.top;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.wear.test.R;
import androidx.wear.widget.util.WakeLockRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class BoxInsetLayoutTest {
    private static final float FACTOR = 0.146467f; //(1 - sqrt(2)/2)/2

    @Rule
    public final WakeLockRule mWakeLock = new WakeLockRule();

    @Rule
    public final ActivityTestRule<LayoutTestActivity> mActivityRule = new ActivityTestRule<>(
            LayoutTestActivity.class, true, false);

    @Test
    public void testCase1() throws Throwable {
        mActivityRule.launchActivity(new Intent().putExtra(LayoutTestActivity
                .EXTRA_LAYOUT_RESOURCE_ID, R.layout.box_inset_layout_testcase_1));
        DisplayMetrics dm = InstrumentationRegistry.getTargetContext().getResources()
                .getDisplayMetrics();
        int boxInset = (int) (FACTOR * Math.min(dm.widthPixels, dm.heightPixels));

        int desiredPadding = 0;
        if (mActivityRule.getActivity().getResources().getConfiguration().isScreenRound()) {
            desiredPadding = boxInset;
        }

        ViewFetchingRunnable customRunnable = new ViewFetchingRunnable(){
            @Override
            public void run() {
                View box = mActivityRule.getActivity().findViewById(R.id.box);
                mIdViewMap.put(R.id.box, box);
            }
        };
        mActivityRule.runOnUiThread(customRunnable);

        View box = customRunnable.mIdViewMap.get(R.id.box);
        // proxy for window location
        View boxParent = (View) box.getParent();
        int parentLeft = boxParent.getLeft();
        int parentTop = boxParent.getTop();
        int parentRight = boxParent.getLeft() + boxParent.getWidth();
        int parentBottom = boxParent.getTop() + boxParent.getHeight();

        // Child 1 is match_parent width and height
        // layout_box=right|bottom
        // Padding of boxInset should be added to the right and bottom sides only
        onView(withId(R.id.child1))
                .check(screenLeft(equalTo(parentLeft)))
                .check(screenTop(equalTo(parentTop)))
                .check(screenRight(equalTo(parentRight - desiredPadding)))
                .check(screenBottom(equalTo(parentBottom - desiredPadding)));

        // Content 1 is is width and height match_parent
        // The bottom and right sides should be inset by boxInset pixels due to padding
        // on the parent view
        onView(withId(R.id.content1))
                .check(screenLeft(equalTo(parentLeft)))
                .check(screenTop(equalTo(parentTop)))
                .check(screenRight(equalTo(parentRight - desiredPadding)))
                .check(screenBottom(equalTo(parentBottom - desiredPadding)));
    }

    @Test
    public void testCase2() throws Throwable {
        mActivityRule.launchActivity(
                new Intent().putExtra(LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                        R.layout.box_inset_layout_testcase_2));
        DisplayMetrics dm =
                InstrumentationRegistry.getTargetContext().getResources().getDisplayMetrics();
        int boxInset = (int) (FACTOR * Math.min(dm.widthPixels, dm.heightPixels));

        int desiredPadding = 0;
        if (mActivityRule.getActivity().getResources().getConfiguration().isScreenRound()) {
            desiredPadding = boxInset;
        }

        ViewFetchingRunnable customRunnable = new ViewFetchingRunnable(){
            @Override
            public void run() {
                View box = mActivityRule.getActivity().findViewById(R.id.box);
                View child1 = mActivityRule.getActivity().findViewById(R.id.child1);
                View child2 = mActivityRule.getActivity().findViewById(R.id.child2);
                View child3 = mActivityRule.getActivity().findViewById(R.id.child3);
                View child4 = mActivityRule.getActivity().findViewById(R.id.child4);
                mIdViewMap.put(R.id.box, box);
                mIdViewMap.put(R.id.child1, child1);
                mIdViewMap.put(R.id.child2, child2);
                mIdViewMap.put(R.id.child3, child3);
                mIdViewMap.put(R.id.child4, child4);

            }
        };
        mActivityRule.runOnUiThread(customRunnable);

        View box = customRunnable.mIdViewMap.get(R.id.box);
        View child1 = customRunnable.mIdViewMap.get(R.id.child1);
        View child2 = customRunnable.mIdViewMap.get(R.id.child2);
        View child3 = customRunnable.mIdViewMap.get(R.id.child3);
        View child4 = customRunnable.mIdViewMap.get(R.id.child4);

        // proxy for window location
        View boxParent = (View) box.getParent();
        int parentLeft = boxParent.getLeft();
        int parentTop = boxParent.getTop();
        int parentRight = boxParent.getLeft() + boxParent.getWidth();
        int parentBottom = boxParent.getTop() + boxParent.getHeight();
        int parentWidth = boxParent.getWidth();
        int parentHeight = boxParent.getHeight();

        // Child 1 is width match_parent, height=60dp, gravity top
        // layout_box=all means it should have padding added to left, top and right
        onView(withId(R.id.child1))
                .check(screenLeft(is(equalTo(parentLeft + desiredPadding))))
                .check(screenTop(is(equalTo(parentTop + desiredPadding))))
                .check(screenRight(is(equalTo(parentRight - desiredPadding))))
                .check(screenBottom(is(equalTo(parentTop + desiredPadding + child1.getHeight()))));

        // Content 1 is width and height match_parent
        // the left top and right edges should be inset by boxInset pixels, due to
        // padding in the parent
        onView(withId(R.id.content1))
                .check(screenLeft(equalTo(parentLeft + desiredPadding)))
                .check(screenTop(equalTo(parentTop + desiredPadding)))
                .check(screenRight(equalTo(parentRight - desiredPadding)));

        // Child 2 is width match_parent, height=60dp, gravity bottom
        // layout_box=all means it should have padding added to left, bottom and right
        onView(withId(R.id.child2))
                .check(screenLeft(is(equalTo(parentLeft + desiredPadding))))
                .check(screenTop(is(equalTo(parentBottom - desiredPadding - child2.getHeight()))))
                .check(screenRight(is(equalTo(parentRight - desiredPadding))))
                .check(screenBottom(is(equalTo(parentBottom - desiredPadding))));

        // Content 2 is width and height match_parent
        // the left bottom and right edges should be inset by boxInset pixels, due to
        // padding in the parent
        onView(withId(R.id.content2))
                .check(screenLeft(equalTo(parentLeft + desiredPadding)))
                .check(screenRight(equalTo(parentRight - desiredPadding)))
                .check(screenBottom(equalTo(parentBottom - desiredPadding)));

        // Child 3 is width wrap_content, height=20dp, gravity left|center_vertical.
        // layout_box=all means it should have padding added to left
        // marginLeft be ignored due to gravity and layout_box=all (screenLeft=0)
        onView(withId(R.id.child3))
                .check(screenLeft(is(equalTo(parentLeft + desiredPadding))))
                .check(approximateTop(is(closeTo((parentHeight / 2 - child3.getHeight() / 2), 1))))
                .check(screenRight(is(equalTo(parentLeft + desiredPadding + child3.getWidth()))))
                .check(approximateBottom(is(
                        closeTo((parentHeight / 2 + child3.getHeight() / 2), 1))));

        // Content 3 width and height match_parent
        // the left edge should be offset from the screen edge by boxInset pixels, due to left on
        // the parent
        onView(withId(R.id.content3)).check(screenLeft(equalTo(desiredPadding)));

        // Child 4 is width wrap_content, height=20dp, gravity right|center_vertical.
        // layout_box=all means it should have padding added to right
        // it should have marginRight ignored due to gravity and layout_box=all (screenRight=max)
        onView(withId(R.id.child4))
                .check(screenLeft(is(parentWidth - desiredPadding - child4.getWidth())))
                .check(approximateTop(is(closeTo((parentHeight / 2 - child3.getHeight() / 2), 1))))
                .check(screenRight(is(equalTo(parentWidth - desiredPadding))))
                .check(approximateBottom(is(
                        closeTo((parentHeight / 2 + child4.getHeight() / 2), 1))));

        // Content 4 width and height wrap_content
        // the right edge should be offset from the screen edge by boxInset pixels, due to
        // right on the parent
        onView(withId(R.id.content4)).check(screenRight(equalTo(parentWidth - desiredPadding)));
    }

    @Test
    public void testCase3() throws Throwable {
        mActivityRule.launchActivity(
                new Intent().putExtra(LayoutTestActivity.EXTRA_LAYOUT_RESOURCE_ID,
                        R.layout.box_inset_layout_testcase_3));
        DisplayMetrics dm =
                InstrumentationRegistry.getTargetContext().getResources().getDisplayMetrics();
        int boxInset = (int) (FACTOR * Math.min(dm.widthPixels, dm.heightPixels));

        int desiredPadding = 0;
        if (mActivityRule.getActivity().getResources().getConfiguration().isScreenRound()) {
            desiredPadding = boxInset;
        }

        ViewFetchingRunnable customRunnable = new ViewFetchingRunnable(){
            @Override
            public void run() {
                View box = mActivityRule.getActivity().findViewById(R.id.box);
                View child1 = mActivityRule.getActivity().findViewById(R.id.child1);
                View child2 = mActivityRule.getActivity().findViewById(R.id.child2);
                View child3 = mActivityRule.getActivity().findViewById(R.id.child3);
                View child4 = mActivityRule.getActivity().findViewById(R.id.child4);
                mIdViewMap.put(R.id.box, box);
                mIdViewMap.put(R.id.child1, child1);
                mIdViewMap.put(R.id.child2, child2);
                mIdViewMap.put(R.id.child3, child3);
                mIdViewMap.put(R.id.child4, child4);
            }
        };
        mActivityRule.runOnUiThread(customRunnable);

        View box = customRunnable.mIdViewMap.get(R.id.box);
        View child1 = customRunnable.mIdViewMap.get(R.id.child1);
        View child2 = customRunnable.mIdViewMap.get(R.id.child2);
        View child3 = customRunnable.mIdViewMap.get(R.id.child3);
        View child4 = customRunnable.mIdViewMap.get(R.id.child4);
        // proxy for window location
        View boxParent = (View) box.getParent();
        int parentLeft = boxParent.getLeft();
        int parentTop = boxParent.getTop();
        int parentBottom = boxParent.getTop() + boxParent.getHeight();
        int parentWidth = boxParent.getWidth();

        // Child 1 is width and height wrap_content
        // gravity is top|left, position should be 0,0 on screen
        onView(withId(R.id.child1))
                .check(screenLeft(is(equalTo(parentLeft + desiredPadding))))
                .check(screenTop(is(equalTo(parentTop + desiredPadding))))
                .check(screenRight(is(equalTo(parentLeft + desiredPadding + child1.getWidth()))))
                .check(screenBottom(is(equalTo(parentTop + desiredPadding + child1.getHeight()))));

        // Content 1 is width and height wrap_content
        // the left and top edges should be offset from the screen edges by boxInset pixels
        onView(withId(R.id.content1))
                .check(screenLeft(equalTo(parentLeft + desiredPadding)))
                .check(screenTop(equalTo(parentTop + desiredPadding)));

        // Child 2 is width and height wrap_content
        // gravity is top|right, position should be 0,max on screen
        onView(withId(R.id.child2))
                .check(screenLeft(is(equalTo(parentWidth - desiredPadding - child2.getWidth()))))
                .check(screenTop(is(equalTo(parentTop + desiredPadding))))
                .check(screenRight(is(equalTo(parentWidth - desiredPadding))))
                .check(screenBottom(is(equalTo(parentTop + desiredPadding + child2.getHeight()))));

        // Content 2 is width and height wrap_content
        // the top and right edges should be offset from the screen edges by boxInset pixels
        onView(withId(R.id.content2))
                .check(screenTop(equalTo(parentTop + desiredPadding)))
                .check(screenRight(equalTo(parentWidth - desiredPadding)));

        // Child 3 is width and height wrap_content
        // gravity is bottom|right, position should be max,max on screen
        onView(withId(R.id.child3))
                .check(screenLeft(is(equalTo(parentWidth - desiredPadding - child3.getWidth()))))
                .check(screenTop(is(
                        equalTo(parentBottom - desiredPadding - child3.getHeight()))))
                .check(screenRight(is(equalTo(parentWidth - desiredPadding))))
                .check(screenBottom(is(equalTo(parentBottom - desiredPadding))));

        // Content 3 is width and height wrap_content
        // the right and bottom edges should be offset from the screen edges by boxInset pixels
        onView(withId(R.id.content3))
                .check(screenBottom(equalTo(parentBottom - desiredPadding)))
                .check(screenRight(equalTo(parentWidth - desiredPadding)));

        // Child 4 is width and height wrap_content
        // gravity is bottom|left, position should be max,0 on screen
        onView(withId(R.id.child4))
                .check(screenLeft(is(equalTo(parentLeft + desiredPadding))))
                .check(screenTop(is(equalTo(parentBottom - desiredPadding - child4.getHeight()))))
                .check(screenRight(is(equalTo(parentLeft + desiredPadding + child4.getWidth()))))
                .check(screenBottom(is(equalTo(parentBottom - desiredPadding))));

        // Content 3 is width and height wrap_content
        // the bottom and left edges should be offset from the screen edges by boxInset pixels
        onView(withId(R.id.content4)).check(
                screenBottom(equalTo(parentBottom - desiredPadding)))
                .check(screenLeft(equalTo(parentLeft + desiredPadding)));
    }

    @Test
    public void testCase4() throws Throwable {
        mActivityRule.launchActivity(new Intent().putExtra(LayoutTestActivity
                .EXTRA_LAYOUT_RESOURCE_ID, R.layout.box_inset_layout_testcase_4));
        DisplayMetrics dm = InstrumentationRegistry.getTargetContext().getResources()
                .getDisplayMetrics();
        int boxInset = (int) (FACTOR * Math.min(dm.widthPixels, dm.heightPixels));

        int desiredPadding = 0;
        if (mActivityRule.getActivity().getResources().getConfiguration().isScreenRound()) {
            desiredPadding = boxInset;
        }

        ViewFetchingRunnable customRunnable = new ViewFetchingRunnable(){
            @Override
            public void run() {
                View container = mActivityRule.getActivity().findViewById(R.id.container);
                View child1 = mActivityRule.getActivity().findViewById(R.id.child1);
                mIdViewMap.put(R.id.container, container);
                mIdViewMap.put(R.id.child1, child1);

            }
        };
        mActivityRule.runOnUiThread(customRunnable);

        View container = customRunnable.mIdViewMap.get(R.id.container);
        View child1 = customRunnable.mIdViewMap.get(R.id.child1);
        // Child 1 is match_parent width and wrap_content height
        // layout_box=right|left
        // Padding of boxInset should be added to the right and bottom sides only
        onView(withId(R.id.child1)).check(left(equalTo(desiredPadding))).check(
                top(equalTo(container.getTop()))).check(
                right(equalTo(dm.widthPixels - desiredPadding))).check(
                bottom(equalTo(container.getTop() + child1.getHeight())));
    }

    private abstract class ViewFetchingRunnable implements Runnable {
        Map<Integer, View> mIdViewMap = new HashMap<>();
    }
}
