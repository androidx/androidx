/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v4.view;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.UiController;
import android.support.v4.test.R;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.TestActivity;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import org.hamcrest.Matcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class ViewPagerTest extends ActivityInstrumentationTestCase2<ViewPagerActivity> {
    private ViewPager mViewPager;

    public ViewPagerTest() {
        super("android.support.v4.view", ViewPagerActivity.class);
    }

    public void setUp() throws Exception {
        super.setUp();

        final ViewPagerActivity activity = getActivity();
        mViewPager = (ViewPager) activity.findViewById(R.id.pager);
        mViewPager.setCurrentItem(0);
    }

    @SmallTest
    public void testPageSelections() {
        assertEquals("Initial state", 0, mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight());
        assertEquals("Scroll right", 1, mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight());
        assertEquals("Scroll right", 2, mViewPager.getCurrentItem());

        // Try "scrolling" beyond the last page and test that we're still on the last page.
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight());
        assertEquals("Scroll right beyond last page", 2, mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollLeft());
        assertEquals("Scroll left", 1, mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollLeft());
        assertEquals("Scroll left", 0, mViewPager.getCurrentItem());

        // Try "scrolling" beyond the first page and test that we're still on the first page.
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollLeft());
        assertEquals("Scroll left beyond first page", 0, mViewPager.getCurrentItem());

    }

    @SmallTest
    public void testPageSwipes() {
        assertEquals("Initial state", 0, mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(swipeLeft());
        assertEquals("Swipe left", 1, mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(swipeLeft());
        assertEquals("Swipe left", 2, mViewPager.getCurrentItem());

        // Try swiping beyond the last page and test that we're still on the last page.
        onView(withId(R.id.pager)).perform(swipeLeft());
        assertEquals("Swipe left beyond last page", 2, mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(swipeRight());
        assertEquals("Swipe right", 1, mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(swipeRight());
        assertEquals("Swipe right", 0, mViewPager.getCurrentItem());

        // Try swiping beyond the first page and test that we're still on the first page.
        onView(withId(R.id.pager)).perform(swipeRight());
        assertEquals("Swipe right beyond first page", 0, mViewPager.getCurrentItem());
    }

    @SmallTest
    public void testPageSwipesComposite() {
        assertEquals("Initial state", 0, mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(swipeLeft(), swipeLeft());
        assertEquals("Swipe twice left", 2, mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(swipeLeft(), swipeRight());
        assertEquals("Swipe left beyond last page and then right", 1, mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(swipeRight(), swipeRight());
        assertEquals("Swipe right and then right beyond first page", 0,
                mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(swipeRight(), swipeLeft());
        assertEquals("Swipe right beyond first page and then left", 1, mViewPager.getCurrentItem());
    }
}
