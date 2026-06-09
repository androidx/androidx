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
package androidx.viewpager.widget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

import androidx.viewpager.test.R;

/**
 * Provides assertions that depend on the interactive nature of <code>PagerTabStrip</code>.
 */
public class ViewPagerWithTabStripTest extends BaseViewPagerTest<ViewPagerWithTabStripActivity> {
    public ViewPagerWithTabStripTest() {
        super(ViewPagerWithTabStripActivity.class);
    }

    @Override
    protected Class<PagerTabStrip> getStripClass() {
        return PagerTabStrip.class;
    }

    @Override
    protected void assertStripInteraction(boolean smoothScroll) {
        // The following block tests that ViewPager page selection changes on clicking titles of
        // various tabs as PagerTabStrip is interactive

        // Click the tab title for page #0 and verify that we're still on page #0
        onView(allOf(isDescendantOfA(withId(R.id.titles)), withText("Red"))).perform(click());
        assertEquals("Click tab #0 on tab #0", 0, mViewPager.getCurrentItem());

        // Click the tab title for page #1 and verify that we're on page #1
        onView(allOf(isDescendantOfA(withId(R.id.titles)), withText("Green"))).perform(click());
        assertEquals("Click tab #1 on tab #0", 1, mViewPager.getCurrentItem());

        // Click the tab title for page #0 and verify that we're on page #0
        onView(allOf(isDescendantOfA(withId(R.id.titles)), withText("Red"))).perform(click());
        assertEquals("Click tab #0 on tab #1", 0, mViewPager.getCurrentItem());

        // Go back to page #1
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight(smoothScroll));

        // Click the tab title for page #1 and verify that we're still on page #1
        onView(allOf(isDescendantOfA(withId(R.id.titles)), withText("Green"))).perform(click());
        assertEquals("Click tab #1 on tab #1", 1, mViewPager.getCurrentItem());

        // Click the tab title for page #2 and verify that we're on page #2
        onView(allOf(isDescendantOfA(withId(R.id.titles)), withText("Blue"))).perform(click());
        assertEquals("Click tab #2 on tab #1", 2, mViewPager.getCurrentItem());

        // The following block tests that ViewPager page selection changes on clicking in
        // between titles of tabs as that functionality is exposed by PagerTabStrip

        // Scroll back to page #0
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollToPage(0, smoothScroll));

        // Click between titles of page #0 and page #1 and verify that we're on page #1
        onView(withId(R.id.titles)).perform(ViewPagerActions.clickBetweenTwoTitles("Red", "Green"));
        assertEquals("Click in between tabs #0 and #1 on tab #0", 1, mViewPager.getCurrentItem());

        // Click between titles of page #0 and page #1 and verify that we're on page #0
        onView(withId(R.id.titles)).perform(ViewPagerActions.clickBetweenTwoTitles("Red", "Green"));
        assertEquals("Click in between tabs #0 and #1 on tab #1", 0, mViewPager.getCurrentItem());

        // Go back to page #1
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight(smoothScroll));

        // Click between titles of page #1 and page #2 and verify that we're on page #2
        onView(withId(R.id.titles)).perform(ViewPagerActions.clickBetweenTwoTitles("Green", "Blue"));
        assertEquals("Click in between tabs #1 and #2 on tab #1", 2, mViewPager.getCurrentItem());
    }
}
