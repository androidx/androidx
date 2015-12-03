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

import android.graphics.Color;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.UiController;
import android.support.v4.test.R;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.TestActivity;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import org.hamcrest.Matcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNot.not;

public class ViewPagerTest extends ActivityInstrumentationTestCase2<ViewPagerActivity> {
    private ViewPager mViewPager;

    private static class BasePagerAdapter<T> extends PagerAdapter {
        protected ArrayList<Pair<String, T>> mEntries = new ArrayList<>();

        public void add(String title, T content) {
            mEntries.add(new Pair(title, content));
        }

        @Override
        public int getCount() {
            return mEntries.size();
        }

        protected void configureInstantiatedItem(View view, int position) {
            switch (position) {
                case 0:
                    view.setId(R.id.page_0);
                    break;
                case 1:
                    view.setId(R.id.page_1);
                    break;
                case 2:
                    view.setId(R.id.page_2);
                    break;
                case 3:
                    view.setId(R.id.page_3);
                    break;
                case 4:
                    view.setId(R.id.page_4);
                    break;
                case 5:
                    view.setId(R.id.page_5);
                    break;
                case 6:
                    view.setId(R.id.page_6);
                    break;
                case 7:
                    view.setId(R.id.page_7);
                    break;
                case 8:
                    view.setId(R.id.page_8);
                    break;
                case 9:
                    view.setId(R.id.page_9);
                    break;
            }
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            // The adapter is also responsible for removing the view.
            container.removeView(((ViewHolder) object).view);
        }

        @Override
        public int getItemPosition(Object object) {
            return ((ViewHolder) object).position;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((ViewHolder) object).view == view;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mEntries.get(position).first;
        }

        protected static class ViewHolder {
            final View view;
            final int position;

            public ViewHolder(View view, int position) {
                this.view = view;
                this.position = position;
            }
        }
    }

    private static class ColorPagerAdapter extends BasePagerAdapter<Integer> {
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final View view = new View(container.getContext());
            view.setBackgroundColor(mEntries.get(position).second);
            configureInstantiatedItem(view, position);

            // Unlike ListView adapters, the ViewPager adapter is responsible
            // for adding the view to the container.
            container.addView(view);

            return new ViewHolder(view, position);
        }
    }

    private static class TextPagerAdapter extends BasePagerAdapter<String> {
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final TextView view = new TextView(container.getContext());
            view.setText(mEntries.get(position).second);
            configureInstantiatedItem(view, position);

            // Unlike ListView adapters, the ViewPager adapter is responsible
            // for adding the view to the container.
            container.addView(view);

            return new ViewHolder(view, position);
        }
    }

    public ViewPagerTest() {
        super("android.support.v4.view", ViewPagerActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        final ViewPagerActivity activity = getActivity();
        mViewPager = (ViewPager) activity.findViewById(R.id.pager);

        ColorPagerAdapter adapter = new ColorPagerAdapter();
        adapter.add("Red", Color.RED);
        adapter.add("Green", Color.GREEN);
        adapter.add("Blue", Color.BLUE);
        onView(withId(R.id.pager)).perform(ViewPagerActions.setAdapter(adapter),
                ViewPagerActions.scrollToPage(0));
    }

    @Override
    public void tearDown() throws Exception {
        onView(withId(R.id.pager)).perform(ViewPagerActions.setAdapter(null));

        super.tearDown();
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

    @SmallTest
    public void testPageContent() {
        assertEquals("Initial state", 0, mViewPager.getCurrentItem());

        // Verify the displayed content to match the initial adapter - with 3 pages and each
        // one rendered as a View.

        // Page #0 should be displayed, page #1 should not be displayed and page #2 should not exist
        // yet as it's outside of the offscreen window limit.
        onView(withId(R.id.page_0)).check(matches(allOf(
                ViewPagerMatchers.isOfClass(View.class),
                isDisplayed(),
                ViewPagerMatchers.backgroundColor(Color.RED))));
        onView(withId(R.id.page_1)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_2)).check(doesNotExist());

        // Scroll one page to select page #1
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight());
        assertEquals("Scroll right", 1, mViewPager.getCurrentItem());
        // Pages #0 / #2 should not be displayed, page #1 should be displayed.
        onView(withId(R.id.page_0)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_1)).check(matches(allOf(
                ViewPagerMatchers.isOfClass(View.class),
                isDisplayed(),
                ViewPagerMatchers.backgroundColor(Color.GREEN))));
        onView(withId(R.id.page_2)).check(matches(not(isDisplayed())));

        // Scroll one more page to select page #2
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight());
        assertEquals("Scroll right again", 2, mViewPager.getCurrentItem());
        // Page #0 should not exist as it's bumped to the outside of the offscreen window limit,
        // page #1 should not be displayed, page #2 should be displayed.
        onView(withId(R.id.page_0)).check(doesNotExist());
        onView(withId(R.id.page_1)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_2)).check(matches(allOf(
                ViewPagerMatchers.isOfClass(View.class),
                isDisplayed(),
                ViewPagerMatchers.backgroundColor(Color.BLUE))));
    }

    @SmallTest
    public void testAdapterChange() {
        // Verify that we have the expected initial adapter
        PagerAdapter initialAdapter = mViewPager.getAdapter();
        assertEquals("Initial adapter class", ColorPagerAdapter.class, initialAdapter.getClass());
        assertEquals("Initial adapter page count", 3, initialAdapter.getCount());

        // Create a new adapter
        TextPagerAdapter newAdapter = new TextPagerAdapter();
        newAdapter.add("Title 0", "Body 0");
        newAdapter.add("Title 1", "Body 1");
        newAdapter.add("Title 2", "Body 2");
        newAdapter.add("Title 3", "Body 3");
        onView(withId(R.id.pager)).perform(ViewPagerActions.setAdapter(newAdapter),
                ViewPagerActions.scrollToPage(0));

        // Verify the displayed content to match the newly set adapter - with 4 pages and each
        // one rendered as a TextView.

        // Page #0 should be displayed, page #1 should not be displayed and pages #2 / #3 should not
        // exist yet as they're outside of the offscreen window limit.
        onView(withId(R.id.page_0)).check(matches(allOf(
                ViewPagerMatchers.isOfClass(TextView.class),
                isDisplayed(),
                withText("Body 0"))));
        onView(withId(R.id.page_1)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_2)).check(doesNotExist());
        onView(withId(R.id.page_3)).check(doesNotExist());

        // Scroll one page to select page #1
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight());
        assertEquals("Scroll right", 1, mViewPager.getCurrentItem());
        // Pages #0 / #2 should not be displayed, page #1 should be displayed, page #3 is still
        // outside the offscreen limit.
        onView(withId(R.id.page_0)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_1)).check(matches(allOf(
                ViewPagerMatchers.isOfClass(TextView.class),
                isDisplayed(),
                withText("Body 1"))));
        onView(withId(R.id.page_2)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_3)).check(doesNotExist());

        // Scroll one more page to select page #2
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight());
        assertEquals("Scroll right again", 2, mViewPager.getCurrentItem());
        // Page #0 should not exist as it's bumped to the outside of the offscreen window limit,
        // pages #1 / #3 should not be displayed, page #2 should be displayed.
        onView(withId(R.id.page_0)).check(doesNotExist());
        onView(withId(R.id.page_1)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_2)).check(matches(allOf(
                ViewPagerMatchers.isOfClass(TextView.class),
                isDisplayed(),
                withText("Body 2"))));
        onView(withId(R.id.page_3)).check(matches(not(isDisplayed())));

        // Scroll one more page to select page #2
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight());
        assertEquals("Scroll right one more time", 3, mViewPager.getCurrentItem());
        // Pages #0 / #1 should not exist as they're bumped to the outside of the offscreen window
        // limit, page #2 should not be displayed, page #3 should be displayed.
        onView(withId(R.id.page_0)).check(doesNotExist());
        onView(withId(R.id.page_1)).check(doesNotExist());
        onView(withId(R.id.page_2)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_3)).check(matches(allOf(
                ViewPagerMatchers.isOfClass(TextView.class),
                isDisplayed(),
                withText("Body 3"))));
    }
}
