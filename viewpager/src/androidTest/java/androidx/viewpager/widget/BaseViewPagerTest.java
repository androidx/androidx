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

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.PositionAssertions.isBelow;
import static android.support.test.espresso.assertion.PositionAssertions.isBottomAlignedWith;
import static android.support.test.espresso.assertion.PositionAssertions.isLeftAlignedWith;
import static android.support.test.espresso.assertion.PositionAssertions.isRightAlignedWith;
import static android.support.test.espresso.assertion.PositionAssertions.isTopAlignedWith;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.v4.testutils.TestUtilsAssertions.hasDisplayedChildren;
import static android.support.v4.testutils.TestUtilsMatchers.backgroundColor;
import static android.support.v4.testutils.TestUtilsMatchers.centerAlignedInParent;
import static android.support.v4.testutils.TestUtilsMatchers.endAlignedToParent;
import static android.support.v4.testutils.TestUtilsMatchers.isOfClass;
import static android.support.v4.testutils.TestUtilsMatchers.startAlignedToParent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.graphics.Color;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.EspressoKey;
import android.support.test.filters.FlakyTest;
import android.support.test.filters.LargeTest;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.testutils.TestUtilsMatchers;
import android.text.TextUtils;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.viewpager.test.R;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for testing <code>ViewPager</code>. Most of the testing logic should be in this
 * class as it is independent on the specific pager title implementation (interactive or non
 * interactive).
 *
 * Testing logic that does depend on the specific pager title implementation is pushed into the
 * extending classes in <code>assertStripInteraction()</code> method.
 */
public abstract class BaseViewPagerTest<T extends Activity> {
    @Rule
    public final ActivityTestRule<T> mActivityTestRule;

    private static final int DIRECTION_LEFT = -1;
    private static final int DIRECTION_RIGHT = 1;
    protected ViewPager mViewPager;

    protected static class BasePagerAdapter<Q> extends PagerAdapter {
        protected ArrayList<Pair<String, Q>> mEntries = new ArrayList<>();

        public void add(String title, Q content) {
            mEntries.add(new Pair<>(title, content));
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

    protected static class ColorPagerAdapter extends BasePagerAdapter<Integer> {
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

    protected static class TextPagerAdapter extends BasePagerAdapter<String> {
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

    protected static class ButtonPagerAdapter extends BasePagerAdapter<Integer> {
        private ArrayList<Button[]> mButtons = new ArrayList<>();

        @Override
        public void add(String title, Integer content) {
            super.add(title, content);
            mButtons.add(new Button[3]);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final LinearLayout view = new LinearLayout(container.getContext());
            view.setBackgroundColor(mEntries.get(position).second);
            view.setOrientation(LinearLayout.HORIZONTAL);
            configureInstantiatedItem(view, position);

            for (int i = 0; i < 3; ++i) {
                Button but = new Button(container.getContext());
                but.setText("" + i);
                but.setFocusableInTouchMode(true);
                view.addView(but, ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                mButtons.get(position)[i] = but;
            }

            // Unlike ListView adapters, the ViewPager adapter is responsible
            // for adding the view to the container.
            container.addView(view);

            return new ViewHolder(view, position);
        }

        public View getButton(int page, int idx) {
            return mButtons.get(page)[idx];
        }
    }

    public BaseViewPagerTest(Class<T> activityClass) {
        mActivityTestRule = new ActivityTestRule<T>(activityClass);
    }

    @Before
    public void setUp() throws Exception {
        final T activity = mActivityTestRule.getActivity();
        mViewPager = (ViewPager) activity.findViewById(R.id.pager);

        ColorPagerAdapter adapter = new ColorPagerAdapter();
        adapter.add("Red", Color.RED);
        adapter.add("Green", Color.GREEN);
        adapter.add("Blue", Color.BLUE);
        onView(withId(R.id.pager)).perform(
                ViewPagerActions.setAdapter(adapter), ViewPagerActions.scrollToPage(0, false));
    }

    @After
    public void tearDown() throws Exception {
        onView(withId(R.id.pager)).perform(ViewPagerActions.setAdapter(null));
    }

    private void verifyPageSelections(boolean smoothScroll) {
        assertEquals("Initial state", 0, mViewPager.getCurrentItem());

        ViewPager.OnPageChangeListener mockPageChangeListener =
                mock(ViewPager.OnPageChangeListener.class);
        mViewPager.addOnPageChangeListener(mockPageChangeListener);

        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight(smoothScroll));
        assertEquals("Scroll right", 1, mViewPager.getCurrentItem());
        verify(mockPageChangeListener, times(1)).onPageSelected(1);

        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight(smoothScroll));
        assertEquals("Scroll right", 2, mViewPager.getCurrentItem());
        verify(mockPageChangeListener, times(1)).onPageSelected(2);

        // Try "scrolling" beyond the last page and test that we're still on the last page.
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight(smoothScroll));
        assertEquals("Scroll right beyond last page", 2, mViewPager.getCurrentItem());
        // We're still on this page, so we shouldn't have been called again with index 2
        verify(mockPageChangeListener, times(1)).onPageSelected(2);

        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollLeft(smoothScroll));
        assertEquals("Scroll left", 1, mViewPager.getCurrentItem());
        // Verify that this is the second time we're called on index 1
        verify(mockPageChangeListener, times(2)).onPageSelected(1);

        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollLeft(smoothScroll));
        assertEquals("Scroll left", 0, mViewPager.getCurrentItem());
        // Verify that this is the first time we're called on index 0
        verify(mockPageChangeListener, times(1)).onPageSelected(0);

        // Try "scrolling" beyond the first page and test that we're still on the first page.
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollLeft(smoothScroll));
        assertEquals("Scroll left beyond first page", 0, mViewPager.getCurrentItem());
        // We're still on this page, so we shouldn't have been called again with index 0
        verify(mockPageChangeListener, times(1)).onPageSelected(0);

        // Unregister our listener
        mViewPager.removeOnPageChangeListener(mockPageChangeListener);

        // Go from index 0 to index 2
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollToPage(2, smoothScroll));
        assertEquals("Scroll to last page", 2, mViewPager.getCurrentItem());
        // Our listener is not registered anymore, so we shouldn't have been called with index 2
        verify(mockPageChangeListener, times(1)).onPageSelected(2);

        // And back to 0
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollToPage(0, smoothScroll));
        assertEquals("Scroll to first page", 0, mViewPager.getCurrentItem());
        // Our listener is not registered anymore, so we shouldn't have been called with index 0
        verify(mockPageChangeListener, times(1)).onPageSelected(0);

        // Verify the overall sequence of calls to onPageSelected of our listener
        ArgumentCaptor<Integer> pageSelectedCaptor = ArgumentCaptor.forClass(int.class);
        verify(mockPageChangeListener, times(4)).onPageSelected(pageSelectedCaptor.capture());
        assertThat(pageSelectedCaptor.getAllValues(), TestUtilsMatchers.matches(1, 2, 1, 0));
    }

    @Test
    @MediumTest
    public void testPageSelectionsImmediate() {
        verifyPageSelections(false);
    }

    @Test
    @LargeTest
    public void testPageSelectionsSmooth() {
        verifyPageSelections(true);
    }

    private void verifyPageChangeViewActions(ViewAction next, ViewAction previous) {
        assertEquals("Initial state", 0, mViewPager.getCurrentItem());
        assertFalse(mViewPager.canScrollHorizontally(DIRECTION_LEFT));
        assertTrue(mViewPager.canScrollHorizontally(DIRECTION_RIGHT));

        ViewPager.OnPageChangeListener mockPageChangeListener =
                mock(ViewPager.OnPageChangeListener.class);
        mViewPager.addOnPageChangeListener(mockPageChangeListener);

        onView(withId(R.id.pager)).perform(next);
        assertEquals("Move to next page", 1, mViewPager.getCurrentItem());
        verify(mockPageChangeListener, times(1)).onPageSelected(1);
        assertTrue(mViewPager.canScrollHorizontally(DIRECTION_LEFT));
        assertTrue(mViewPager.canScrollHorizontally(DIRECTION_RIGHT));

        onView(withId(R.id.pager)).perform(next);
        assertEquals("Move to next page", 2, mViewPager.getCurrentItem());
        verify(mockPageChangeListener, times(1)).onPageSelected(2);
        assertTrue(mViewPager.canScrollHorizontally(DIRECTION_LEFT));
        assertFalse(mViewPager.canScrollHorizontally(DIRECTION_RIGHT));

        // Try swiping beyond the last page and test that we're still on the last page.
        onView(withId(R.id.pager)).perform(next);
        assertEquals("Attempt to move to next page beyond last page", 2,
                mViewPager.getCurrentItem());
        // We're still on this page, so we shouldn't have been called again with index 2
        verify(mockPageChangeListener, times(1)).onPageSelected(2);
        assertTrue(mViewPager.canScrollHorizontally(DIRECTION_LEFT));
        assertFalse(mViewPager.canScrollHorizontally(DIRECTION_RIGHT));

        onView(withId(R.id.pager)).perform(previous);
        assertEquals("Move to previous page", 1, mViewPager.getCurrentItem());
        // Verify that this is the second time we're called on index 1
        verify(mockPageChangeListener, times(2)).onPageSelected(1);
        assertTrue(mViewPager.canScrollHorizontally(DIRECTION_LEFT));
        assertTrue(mViewPager.canScrollHorizontally(DIRECTION_RIGHT));

        onView(withId(R.id.pager)).perform(previous);
        assertEquals("Move to previous page", 0, mViewPager.getCurrentItem());
        // Verify that this is the first time we're called on index 0
        verify(mockPageChangeListener, times(1)).onPageSelected(0);
        assertFalse(mViewPager.canScrollHorizontally(DIRECTION_LEFT));
        assertTrue(mViewPager.canScrollHorizontally(DIRECTION_RIGHT));

        // Try swiping beyond the first page and test that we're still on the first page.
        onView(withId(R.id.pager)).perform(previous);
        assertEquals("Attempt to move to previous page beyond first page", 0,
                mViewPager.getCurrentItem());
        // We're still on this page, so we shouldn't have been called again with index 0
        verify(mockPageChangeListener, times(1)).onPageSelected(0);
        assertFalse(mViewPager.canScrollHorizontally(DIRECTION_LEFT));
        assertTrue(mViewPager.canScrollHorizontally(DIRECTION_RIGHT));

        mViewPager.removeOnPageChangeListener(mockPageChangeListener);

        // Verify the overall sequence of calls to onPageSelected of our listener
        ArgumentCaptor<Integer> pageSelectedCaptor = ArgumentCaptor.forClass(int.class);
        verify(mockPageChangeListener, times(4)).onPageSelected(pageSelectedCaptor.capture());
        assertThat(pageSelectedCaptor.getAllValues(), TestUtilsMatchers.matches(1, 2, 1, 0));
    }

    @Test
    @LargeTest
    public void testPageSwipes() {
        verifyPageChangeViewActions(ViewPagerActions.wrap(swipeLeft()), ViewPagerActions.wrap(swipeRight()));
    }

    @Test
    @LargeTest
    public void testArrowPageChanges() {
        verifyPageChangeViewActions(
                ViewPagerActions.arrowScroll(View.FOCUS_RIGHT), ViewPagerActions.arrowScroll(View.FOCUS_LEFT));
    }

    @Test
    @LargeTest
    public void testPageSwipesComposite() {
        assertEquals("Initial state", 0, mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(ViewPagerActions.wrap(swipeLeft()), ViewPagerActions.wrap(swipeLeft()));
        assertEquals("Swipe twice left", 2, mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(ViewPagerActions.wrap(swipeLeft()), ViewPagerActions.wrap(swipeRight()));
        assertEquals("Swipe left beyond last page and then right", 1, mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(
                ViewPagerActions.wrap(swipeRight()), ViewPagerActions.wrap(swipeRight()));
        assertEquals("Swipe right and then right beyond first page", 0,
                mViewPager.getCurrentItem());

        onView(withId(R.id.pager)).perform(
                ViewPagerActions.wrap(swipeRight()), ViewPagerActions.wrap(swipeLeft()));
        assertEquals("Swipe right beyond first page and then left", 1, mViewPager.getCurrentItem());
    }

    private void verifyPageContent(boolean smoothScroll) {
        assertEquals("Initial state", 0, mViewPager.getCurrentItem());

        // Verify the displayed content to match the initial adapter - with 3 pages and each
        // one rendered as a View.

        // Page #0 should be displayed, page #1 should not be displayed and page #2 should not exist
        // yet as it's outside of the offscreen window limit.
        onView(withId(R.id.page_0)).check(matches(allOf(
                isOfClass(View.class),
                isDisplayed(),
                backgroundColor(Color.RED))));
        onView(withId(R.id.page_1)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_2)).check(doesNotExist());

        // Scroll one page to select page #1
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight(smoothScroll));
        assertEquals("Scroll right", 1, mViewPager.getCurrentItem());
        // Pages #0 / #2 should not be displayed, page #1 should be displayed.
        onView(withId(R.id.page_0)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_1)).check(matches(allOf(
                isOfClass(View.class),
                isDisplayed(),
                backgroundColor(Color.GREEN))));
        onView(withId(R.id.page_2)).check(matches(not(isDisplayed())));

        // Scroll one more page to select page #2
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight(smoothScroll));
        assertEquals("Scroll right again", 2, mViewPager.getCurrentItem());
        // Page #0 should not exist as it's bumped to the outside of the offscreen window limit,
        // page #1 should not be displayed, page #2 should be displayed.
        onView(withId(R.id.page_0)).check(doesNotExist());
        onView(withId(R.id.page_1)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_2)).check(matches(allOf(
                isOfClass(View.class),
                isDisplayed(),
                backgroundColor(Color.BLUE))));
    }

    @Test
    @MediumTest
    public void testPageContentImmediate() {
        verifyPageContent(false);
    }

    @Test
    @LargeTest
    public void testPageContentSmooth() {
        verifyPageContent(true);
    }

    private void verifyAdapterChange(boolean smoothScroll) {
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
        onView(withId(R.id.pager)).perform(
                ViewPagerActions.setAdapter(newAdapter), ViewPagerActions.scrollToPage(0, smoothScroll));

        // Verify the displayed content to match the newly set adapter - with 4 pages and each
        // one rendered as a TextView.

        // Page #0 should be displayed, page #1 should not be displayed and pages #2 / #3 should not
        // exist yet as they're outside of the offscreen window limit.
        onView(withId(R.id.page_0)).check(matches(allOf(
                isOfClass(TextView.class),
                isDisplayed(),
                withText("Body 0"))));
        onView(withId(R.id.page_1)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_2)).check(doesNotExist());
        onView(withId(R.id.page_3)).check(doesNotExist());

        // Scroll one page to select page #1
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight(smoothScroll));
        assertEquals("Scroll right", 1, mViewPager.getCurrentItem());
        // Pages #0 / #2 should not be displayed, page #1 should be displayed, page #3 is still
        // outside the offscreen limit.
        onView(withId(R.id.page_0)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_1)).check(matches(allOf(
                isOfClass(TextView.class),
                isDisplayed(),
                withText("Body 1"))));
        onView(withId(R.id.page_2)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_3)).check(doesNotExist());

        // Scroll one more page to select page #2
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight(smoothScroll));
        assertEquals("Scroll right again", 2, mViewPager.getCurrentItem());
        // Page #0 should not exist as it's bumped to the outside of the offscreen window limit,
        // pages #1 / #3 should not be displayed, page #2 should be displayed.
        onView(withId(R.id.page_0)).check(doesNotExist());
        onView(withId(R.id.page_1)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_2)).check(matches(allOf(
                isOfClass(TextView.class),
                isDisplayed(),
                withText("Body 2"))));
        onView(withId(R.id.page_3)).check(matches(not(isDisplayed())));

        // Scroll one more page to select page #2
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight(smoothScroll));
        assertEquals("Scroll right one more time", 3, mViewPager.getCurrentItem());
        // Pages #0 / #1 should not exist as they're bumped to the outside of the offscreen window
        // limit, page #2 should not be displayed, page #3 should be displayed.
        onView(withId(R.id.page_0)).check(doesNotExist());
        onView(withId(R.id.page_1)).check(doesNotExist());
        onView(withId(R.id.page_2)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_3)).check(matches(allOf(
                isOfClass(TextView.class),
                isDisplayed(),
                withText("Body 3"))));
    }

    @Test
    @MediumTest
    public void testAdapterChangeImmediate() {
        verifyAdapterChange(false);
    }

    @Test
    @LargeTest
    public void testAdapterChangeSmooth() {
        verifyAdapterChange(true);
    }

    private void verifyTitleStripLayout(String expectedStartTitle, String expectedSelectedTitle,
            String expectedEndTitle, int selectedPageId) {
        // Check that the title strip spans the whole width of the pager and is aligned to
        // its top
        onView(withId(R.id.titles)).check(isLeftAlignedWith(withId(R.id.pager)));
        onView(withId(R.id.titles)).check(isRightAlignedWith(withId(R.id.pager)));
        onView(withId(R.id.titles)).check(isTopAlignedWith(withId(R.id.pager)));

        // Check that the currently selected page spans the whole width of the pager and is below
        // the title strip
        onView(withId(selectedPageId)).check(isLeftAlignedWith(withId(R.id.pager)));
        onView(withId(selectedPageId)).check(isRightAlignedWith(withId(R.id.pager)));
        onView(withId(selectedPageId)).check(isBelow(withId(R.id.titles)));
        onView(withId(selectedPageId)).check(isBottomAlignedWith(withId(R.id.pager)));

        boolean hasStartTitle = !TextUtils.isEmpty(expectedStartTitle);
        boolean hasEndTitle = !TextUtils.isEmpty(expectedEndTitle);

        // Check that the title strip shows the expected number of children (tab titles)
        int nonNullTitles = (hasStartTitle ? 1 : 0) + 1 + (hasEndTitle ? 1 : 0);
        onView(withId(R.id.titles)).check(hasDisplayedChildren(nonNullTitles));

        if (hasStartTitle) {
            // Check that the title for the start page is displayed at the start edge of its parent
            // (title strip)
            onView(withId(R.id.titles)).check(matches(hasDescendant(
                    allOf(withText(expectedStartTitle), isDisplayed(), startAlignedToParent()))));
        }
        // Check that the title for the selected page is displayed centered in its parent
        // (title strip)
        onView(withId(R.id.titles)).check(matches(hasDescendant(
                allOf(withText(expectedSelectedTitle), isDisplayed(), centerAlignedInParent()))));
        if (hasEndTitle) {
            // Check that the title for the end page is displayed at the end edge of its parent
            // (title strip)
            onView(withId(R.id.titles)).check(matches(hasDescendant(
                    allOf(withText(expectedEndTitle), isDisplayed(), endAlignedToParent()))));
        }
    }

    private void verifyPagerStrip(boolean smoothScroll) {
        // Set an adapter with 5 pages
        final ColorPagerAdapter adapter = new ColorPagerAdapter();
        adapter.add("Red", Color.RED);
        adapter.add("Green", Color.GREEN);
        adapter.add("Blue", Color.BLUE);
        adapter.add("Yellow", Color.YELLOW);
        adapter.add("Magenta", Color.MAGENTA);
        onView(withId(R.id.pager)).perform(ViewPagerActions.setAdapter(adapter),
                ViewPagerActions.scrollToPage(0, smoothScroll));

        // Check that the pager has a title strip
        onView(withId(R.id.pager)).check(matches(hasDescendant(withId(R.id.titles))));
        // Check that the title strip is displayed and is of the expected class
        onView(withId(R.id.titles)).check(matches(allOf(
                isDisplayed(), isOfClass(getStripClass()))));

        // The following block tests the overall layout of tab strip and main pager content
        // (vertical stacking), the content of the tab strip (showing texts for the selected
        // tab and the ones on its left / right) as well as the alignment of the content in the
        // tab strip (selected in center, others on left and right).

        // Check the content and alignment of title strip for selected page #0
        verifyTitleStripLayout(null, "Red", "Green", R.id.page_0);

        // Scroll one page to select page #1 and check layout / content of title strip
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight(smoothScroll));
        verifyTitleStripLayout("Red", "Green", "Blue", R.id.page_1);

        // Scroll one page to select page #2 and check layout / content of title strip
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight(smoothScroll));
        verifyTitleStripLayout("Green", "Blue", "Yellow", R.id.page_2);

        // Scroll one page to select page #3 and check layout / content of title strip
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight(smoothScroll));
        verifyTitleStripLayout("Blue", "Yellow", "Magenta", R.id.page_3);

        // Scroll one page to select page #4 and check layout / content of title strip
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollRight(smoothScroll));
        verifyTitleStripLayout("Yellow", "Magenta", null, R.id.page_4);

        // Scroll back to page #0
        onView(withId(R.id.pager)).perform(ViewPagerActions.scrollToPage(0, smoothScroll));

        assertStripInteraction(smoothScroll);
    }

    @Test
    @LargeTest
    public void testPagerStripImmediate() {
        verifyPagerStrip(false);
    }

    @Test
    @LargeTest
    public void testPagerStripSmooth() {
        verifyPagerStrip(true);
    }

    /**
     * Returns the class of the pager strip.
     */
    protected abstract Class getStripClass();

    /**
     * Checks assertions that are specific to the pager strip implementation (interactive or
     * non interactive).
     */
    protected abstract void assertStripInteraction(boolean smoothScroll);

    /**
     * Helper method that performs the specified action on the <code>ViewPager</code> and then
     * checks the sequence of calls to the page change listener based on the specified expected
     * scroll state changes.
     *
     * If that expected list is empty, this method verifies that there were no calls to
     * onPageScrollStateChanged when the action was performed. Otherwise it verifies that the actual
     * sequence of calls to onPageScrollStateChanged matches the expected (specified) one.
     */
    private void verifyScrollStateChange(ViewAction viewAction, int... expectedScrollStateChanges) {
        ViewPager.OnPageChangeListener mockPageChangeListener =
                mock(ViewPager.OnPageChangeListener.class);
        mViewPager.addOnPageChangeListener(mockPageChangeListener);

        // Perform our action
        onView(withId(R.id.pager)).perform(viewAction);

        int expectedScrollStateChangeCount = (expectedScrollStateChanges != null) ?
                expectedScrollStateChanges.length : 0;

        if (expectedScrollStateChangeCount == 0) {
            verify(mockPageChangeListener, never()).onPageScrollStateChanged(anyInt());
        } else {
            ArgumentCaptor<Integer> pageScrollStateCaptor = ArgumentCaptor.forClass(int.class);
            verify(mockPageChangeListener, times(expectedScrollStateChangeCount)).
                    onPageScrollStateChanged(pageScrollStateCaptor.capture());
            assertThat(pageScrollStateCaptor.getAllValues(),
                    TestUtilsMatchers.matches(expectedScrollStateChanges));
        }

        // Remove our mock listener to get back to clean state for the next test
        mViewPager.removeOnPageChangeListener(mockPageChangeListener);
    }

    @Test
    @MediumTest
    public void testPageScrollStateChangedImmediate() {
        // Note that all the actions tested in this method are immediate (no scrolling) and
        // as such we test that we do not get any calls to onPageScrollStateChanged in any of them

        // Select one page to the right
        verifyScrollStateChange(ViewPagerActions.scrollRight(false));
        // Select one more page to the right
        verifyScrollStateChange(ViewPagerActions.scrollRight(false));
        // Select one page to the left
        verifyScrollStateChange(ViewPagerActions.scrollLeft(false));
        // Select one more page to the left
        verifyScrollStateChange(ViewPagerActions.scrollLeft(false));
        // Select last page
        verifyScrollStateChange(ViewPagerActions.scrollToLast(false));
        // Select first page
        verifyScrollStateChange(ViewPagerActions.scrollToFirst(false));
    }

    @Test
    @LargeTest
    public void testPageScrollStateChangedSmooth() {
        // Note that all the actions tested in this method use smooth scrolling and as such we test
        // that we get the matching calls to onPageScrollStateChanged
        final int[] expectedScrollStateChanges = new int[] {
                ViewPager.SCROLL_STATE_SETTLING, ViewPager.SCROLL_STATE_IDLE
        };

        // Select one page to the right
        verifyScrollStateChange(ViewPagerActions.scrollRight(true), expectedScrollStateChanges);
        // Select one more page to the right
        verifyScrollStateChange(ViewPagerActions.scrollRight(true), expectedScrollStateChanges);
        // Select one page to the left
        verifyScrollStateChange(ViewPagerActions.scrollLeft(true), expectedScrollStateChanges);
        // Select one more page to the left
        verifyScrollStateChange(ViewPagerActions.scrollLeft(true), expectedScrollStateChanges);
        // Select last page
        verifyScrollStateChange(ViewPagerActions.scrollToLast(true), expectedScrollStateChanges);
        // Select first page
        verifyScrollStateChange(ViewPagerActions.scrollToFirst(true), expectedScrollStateChanges);
    }

    @Test
    @LargeTest
    public void testPageScrollStateChangedSwipe() {
        // Note that all the actions tested in this method use swiping and as such we test
        // that we get the matching calls to onPageScrollStateChanged
        final int[] expectedScrollStateChanges = new int[] { ViewPager.SCROLL_STATE_DRAGGING,
                ViewPager.SCROLL_STATE_SETTLING, ViewPager.SCROLL_STATE_IDLE };

        // Swipe one page to the left
        verifyScrollStateChange(ViewPagerActions.wrap(swipeLeft()), expectedScrollStateChanges);
        assertEquals("Swipe left", 1, mViewPager.getCurrentItem());

        // Swipe one more page to the left
        verifyScrollStateChange(ViewPagerActions.wrap(swipeLeft()), expectedScrollStateChanges);
        assertEquals("Swipe left", 2, mViewPager.getCurrentItem());

        // Swipe one page to the right
        verifyScrollStateChange(ViewPagerActions.wrap(swipeRight()), expectedScrollStateChanges);
        assertEquals("Swipe right", 1, mViewPager.getCurrentItem());

        // Swipe one more page to the right
        verifyScrollStateChange(ViewPagerActions.wrap(swipeRight()), expectedScrollStateChanges);
        assertEquals("Swipe right", 0, mViewPager.getCurrentItem());
    }

    /**
     * Helper method to verify the internal consistency of values passed to
     * {@link ViewPager.OnPageChangeListener#onPageScrolled} callback when we go from a page with
     * lower index to a page with higher index.
     *
     * @param startPageIndex Index of the starting page.
     * @param endPageIndex Index of the ending page.
     * @param pageWidth Page width in pixels.
     * @param positions List of "position" values passed to all
     *      {@link ViewPager.OnPageChangeListener#onPageScrolled} calls.
     * @param positionOffsets List of "positionOffset" values passed to all
     *      {@link ViewPager.OnPageChangeListener#onPageScrolled} calls.
     * @param positionOffsetPixels List of "positionOffsetPixel" values passed to all
     *      {@link ViewPager.OnPageChangeListener#onPageScrolled} calls.
     */
    private void verifyScrollCallbacksToHigherPage(int startPageIndex, int endPageIndex,
            int pageWidth, List<Integer> positions, List<Float> positionOffsets,
            List<Integer> positionOffsetPixels) {
        int callbackCount = positions.size();

        // The last entry in all three lists must match the index of the end page
        Assert.assertEquals("Position at last index",
                endPageIndex, (int) positions.get(callbackCount - 1));
        Assert.assertEquals("Position offset at last index",
                0.0f, positionOffsets.get(callbackCount - 1), 0.0f);
        Assert.assertEquals("Position offset pixel at last index",
                0, (int) positionOffsetPixels.get(callbackCount - 1));

        // If this was our only callback, return. This can happen on immediate page change
        // or on very slow devices.
        if (callbackCount == 1) {
            return;
        }

        // If we have additional callbacks, verify that the values provided to our callback reflect
        // a valid sequence of events going from startPageIndex to endPageIndex.
        for (int i = 0; i < callbackCount - 1; i++) {
            // Page position must be between start page and end page
            int pagePositionCurr = positions.get(i);
            if ((pagePositionCurr < startPageIndex) || (pagePositionCurr > endPageIndex)) {
                Assert.fail("Position at #" + i + " is " + pagePositionCurr +
                        ", but should be between " + startPageIndex + " and " + endPageIndex);
            }

            // Page position sequence cannot be decreasing
            int pagePositionNext = positions.get(i + 1);
            if (pagePositionCurr > pagePositionNext) {
                Assert.fail("Position at #" + i + " is " + pagePositionCurr +
                        " and then decreases to " + pagePositionNext + " at #" + (i + 1));
            }

            // Position offset must be in [0..1) range (inclusive / exclusive)
            float positionOffsetCurr = positionOffsets.get(i);
            if ((positionOffsetCurr < 0.0f) || (positionOffsetCurr >= 1.0f)) {
                Assert.fail("Position offset at #" + i + " is " + positionOffsetCurr +
                        ", but should be in [0..1) range");
            }

            // Position pixel offset must be in [0..pageWidth) range (inclusive / exclusive)
            int positionOffsetPixelCurr = positionOffsetPixels.get(i);
            if ((positionOffsetPixelCurr < 0.0f) || (positionOffsetPixelCurr >= pageWidth)) {
                Assert.fail("Position pixel offset at #" + i + " is " + positionOffsetCurr +
                        ", but should be in [0.." + pageWidth + ") range");
            }

            // Position pixel offset must match the position offset and page width within
            // a one-pixel tolerance range
            Assert.assertEquals("Position pixel offset at #" + i + " is " +
                    positionOffsetPixelCurr + ", but doesn't match position offset which is" +
                    positionOffsetCurr + " and page width which is " + pageWidth,
                    positionOffsetPixelCurr, positionOffsetCurr * pageWidth, 1.0f);

            // If we stay on the same page between this index and the next one, both position
            // offset and position pixel offset must increase
            if (pagePositionNext == pagePositionCurr) {
                float positionOffsetNext = positionOffsets.get(i + 1);
                // Note that since position offset sequence is float, we are checking for strict
                // increasing
                if (positionOffsetNext <= positionOffsetCurr) {
                    Assert.fail("Position offset at #" + i + " is " + positionOffsetCurr +
                            " and at #" + (i + 1) + " is " + positionOffsetNext +
                            ". Since both are for page " + pagePositionCurr +
                            ", they cannot decrease");
                }

                int positionOffsetPixelNext = positionOffsetPixels.get(i + 1);
                // Note that since position offset pixel sequence is the mapping of position offset
                // into screen pixels, we can get two (or more) callbacks with strictly increasing
                // position offsets that are converted into the same pixel value. This is why here
                // we are checking for non-strict increasing
                if (positionOffsetPixelNext < positionOffsetPixelCurr) {
                    Assert.fail("Position offset pixel at #" + i + " is " +
                            positionOffsetPixelCurr + " and at #" + (i + 1) + " is " +
                            positionOffsetPixelNext + ". Since both are for page " +
                            pagePositionCurr + ", they cannot decrease");
                }
            }
        }
    }

    /**
     * Helper method to verify the internal consistency of values passed to
     * {@link ViewPager.OnPageChangeListener#onPageScrolled} callback when we go from a page with
     * higher index to a page with lower index.
     *
     * @param startPageIndex Index of the starting page.
     * @param endPageIndex Index of the ending page.
     * @param pageWidth Page width in pixels.
     * @param positions List of "position" values passed to all
     *      {@link ViewPager.OnPageChangeListener#onPageScrolled} calls.
     * @param positionOffsets List of "positionOffset" values passed to all
     *      {@link ViewPager.OnPageChangeListener#onPageScrolled} calls.
     * @param positionOffsetPixels List of "positionOffsetPixel" values passed to all
     *      {@link ViewPager.OnPageChangeListener#onPageScrolled} calls.
     */
    private void verifyScrollCallbacksToLowerPage(int startPageIndex, int endPageIndex,
            int pageWidth, List<Integer> positions, List<Float> positionOffsets,
            List<Integer> positionOffsetPixels) {
        int callbackCount = positions.size();

        // The last entry in all three lists must match the index of the end page
        Assert.assertEquals("Position at last index",
                endPageIndex, (int) positions.get(callbackCount - 1));
        Assert.assertEquals("Position offset at last index",
                0.0f, positionOffsets.get(callbackCount - 1), 0.0f);
        Assert.assertEquals("Position offset pixel at last index",
                0, (int) positionOffsetPixels.get(callbackCount - 1));

        // If this was our only callback, return. This can happen on immediate page change
        // or on very slow devices.
        if (callbackCount == 1) {
            return;
        }

        // If we have additional callbacks, verify that the values provided to our callback reflect
        // a valid sequence of events going from startPageIndex to endPageIndex.
        for (int i = 0; i < callbackCount - 1; i++) {
            // Page position must be between start page and end page
            int pagePositionCurr = positions.get(i);
            if ((pagePositionCurr > startPageIndex) || (pagePositionCurr < endPageIndex)) {
                Assert.fail("Position at #" + i + " is " + pagePositionCurr +
                        ", but should be between " + endPageIndex + " and " + startPageIndex);
            }

            // Page position sequence cannot be increasing
            int pagePositionNext = positions.get(i + 1);
            if (pagePositionCurr < pagePositionNext) {
                Assert.fail("Position at #" + i + " is " + pagePositionCurr +
                        " and then increases to " + pagePositionNext + " at #" + (i + 1));
            }

            // Position offset must be in [0..1) range (inclusive / exclusive)
            float positionOffsetCurr = positionOffsets.get(i);
            if ((positionOffsetCurr < 0.0f) || (positionOffsetCurr >= 1.0f)) {
                Assert.fail("Position offset at #" + i + " is " + positionOffsetCurr +
                        ", but should be in [0..1) range");
            }

            // Position pixel offset must be in [0..pageWidth) range (inclusive / exclusive)
            int positionOffsetPixelCurr = positionOffsetPixels.get(i);
            if ((positionOffsetPixelCurr < 0.0f) || (positionOffsetPixelCurr >= pageWidth)) {
                Assert.fail("Position pixel offset at #" + i + " is " + positionOffsetCurr +
                        ", but should be in [0.." + pageWidth + ") range");
            }

            // Position pixel offset must match the position offset and page width within
            // a one-pixel tolerance range
            Assert.assertEquals("Position pixel offset at #" + i + " is " +
                            positionOffsetPixelCurr + ", but doesn't match position offset which is" +
                            positionOffsetCurr + " and page width which is " + pageWidth,
                    positionOffsetPixelCurr, positionOffsetCurr * pageWidth, 1.0f);

            // If we stay on the same page between this index and the next one, both position
            // offset and position pixel offset must decrease
            if (pagePositionNext == pagePositionCurr) {
                float positionOffsetNext = positionOffsets.get(i + 1);
                // Note that since position offset sequence is float, we are checking for strict
                // decreasing
                if (positionOffsetNext >= positionOffsetCurr) {
                    Assert.fail("Position offset at #" + i + " is " + positionOffsetCurr +
                            " and at #" + (i + 1) + " is " + positionOffsetNext +
                            ". Since both are for page " + pagePositionCurr +
                            ", they cannot increase");
                }

                int positionOffsetPixelNext = positionOffsetPixels.get(i + 1);
                // Note that since position offset pixel sequence is the mapping of position offset
                // into screen pixels, we can get two (or more) callbacks with strictly decreasing
                // position offsets that are converted into the same pixel value. This is why here
                // we are checking for non-strict decreasing
                if (positionOffsetPixelNext > positionOffsetPixelCurr) {
                    Assert.fail("Position offset pixel at #" + i + " is " +
                            positionOffsetPixelCurr + " and at #" + (i + 1) + " is " +
                            positionOffsetPixelNext + ". Since both are for page " +
                            pagePositionCurr + ", they cannot increase");
                }
            }
        }
    }

    private void verifyScrollCallbacksToHigherPage(ViewAction viewAction,
            int expectedEndPageIndex) {
        final int startPageIndex = mViewPager.getCurrentItem();

        ViewPager.OnPageChangeListener mockPageChangeListener =
                mock(ViewPager.OnPageChangeListener.class);
        mViewPager.addOnPageChangeListener(mockPageChangeListener);

        // Perform our action
        onView(withId(R.id.pager)).perform(viewAction);

        final int endPageIndex = mViewPager.getCurrentItem();
        Assert.assertEquals("Current item after action", expectedEndPageIndex, endPageIndex);

        ArgumentCaptor<Integer> positionCaptor = ArgumentCaptor.forClass(int.class);
        ArgumentCaptor<Float> positionOffsetCaptor = ArgumentCaptor.forClass(float.class);
        ArgumentCaptor<Integer> positionOffsetPixelsCaptor = ArgumentCaptor.forClass(int.class);
        verify(mockPageChangeListener, atLeastOnce()).onPageScrolled(positionCaptor.capture(),
                positionOffsetCaptor.capture(), positionOffsetPixelsCaptor.capture());

        verifyScrollCallbacksToHigherPage(startPageIndex, endPageIndex, mViewPager.getWidth(),
                positionCaptor.getAllValues(), positionOffsetCaptor.getAllValues(),
                positionOffsetPixelsCaptor.getAllValues());

        // Remove our mock listener to get back to clean state for the next test
        mViewPager.removeOnPageChangeListener(mockPageChangeListener);
    }

    private void verifyScrollCallbacksToLowerPage(ViewAction viewAction,
            int expectedEndPageIndex) {
        final int startPageIndex = mViewPager.getCurrentItem();

        ViewPager.OnPageChangeListener mockPageChangeListener =
                mock(ViewPager.OnPageChangeListener.class);
        mViewPager.addOnPageChangeListener(mockPageChangeListener);

        // Perform our action
        onView(withId(R.id.pager)).perform(viewAction);

        final int endPageIndex = mViewPager.getCurrentItem();
        Assert.assertEquals("Current item after action", expectedEndPageIndex, endPageIndex);

        ArgumentCaptor<Integer> positionCaptor = ArgumentCaptor.forClass(int.class);
        ArgumentCaptor<Float> positionOffsetCaptor = ArgumentCaptor.forClass(float.class);
        ArgumentCaptor<Integer> positionOffsetPixelsCaptor = ArgumentCaptor.forClass(int.class);
        verify(mockPageChangeListener, atLeastOnce()).onPageScrolled(positionCaptor.capture(),
                positionOffsetCaptor.capture(), positionOffsetPixelsCaptor.capture());

        verifyScrollCallbacksToLowerPage(startPageIndex, endPageIndex, mViewPager.getWidth(),
                positionCaptor.getAllValues(), positionOffsetCaptor.getAllValues(),
                positionOffsetPixelsCaptor.getAllValues());

        // Remove our mock listener to get back to clean state for the next test
        mViewPager.removeOnPageChangeListener(mockPageChangeListener);
    }

    @Test
    @MediumTest
    public void testPageScrollPositionChangesImmediate() {
        // Scroll one page to the right
        verifyScrollCallbacksToHigherPage(ViewPagerActions.scrollRight(false), 1);
        // Scroll one more page to the right
        verifyScrollCallbacksToHigherPage(ViewPagerActions.scrollRight(false), 2);
        // Scroll one page to the left
        verifyScrollCallbacksToLowerPage(ViewPagerActions.scrollLeft(false), 1);
        // Scroll one more page to the left
        verifyScrollCallbacksToLowerPage(ViewPagerActions.scrollLeft(false), 0);

        // Scroll to the last page
        verifyScrollCallbacksToHigherPage(ViewPagerActions.scrollToLast(false), 2);
        // Scroll to the first page
        verifyScrollCallbacksToLowerPage(ViewPagerActions.scrollToFirst(false), 0);
    }

    @Test
    @LargeTest
    public void testPageScrollPositionChangesSmooth() {
        // Scroll one page to the right
        verifyScrollCallbacksToHigherPage(ViewPagerActions.scrollRight(true), 1);
        // Scroll one more page to the right
        verifyScrollCallbacksToHigherPage(ViewPagerActions.scrollRight(true), 2);
        // Scroll one page to the left
        verifyScrollCallbacksToLowerPage(ViewPagerActions.scrollLeft(true), 1);
        // Scroll one more page to the left
        verifyScrollCallbacksToLowerPage(ViewPagerActions.scrollLeft(true), 0);

        // Scroll to the last page
        verifyScrollCallbacksToHigherPage(ViewPagerActions.scrollToLast(true), 2);
        // Scroll to the first page
        verifyScrollCallbacksToLowerPage(ViewPagerActions.scrollToFirst(true), 0);
    }

    @Test
    @LargeTest
    public void testPageScrollPositionChangesSwipe() {
        // Swipe one page to the left
        verifyScrollCallbacksToHigherPage(ViewPagerActions.wrap(swipeLeft()), 1);
        // Swipe one more page to the left
        verifyScrollCallbacksToHigherPage(ViewPagerActions.wrap(swipeLeft()), 2);
        // Swipe one page to the right
        verifyScrollCallbacksToLowerPage(ViewPagerActions.wrap(swipeRight()), 1);
        // Swipe one more page to the right
        verifyScrollCallbacksToLowerPage(ViewPagerActions.wrap(swipeRight()), 0);
    }

    @FlakyTest(bugId = 38260187)
    @Test
    @LargeTest
    public void testKeyboardNavigation() {
        ButtonPagerAdapter adapter = new ButtonPagerAdapter();
        adapter.add("Red", Color.RED);
        adapter.add("Green", Color.GREEN);
        adapter.add("Blue", Color.BLUE);
        onView(withId(R.id.pager)).perform(
                ViewPagerActions.setAdapter(adapter), ViewPagerActions.scrollToPage(0, false));
        View firstButton = adapter.getButton(0, 0);
        firstButton.requestFocus();
        assertTrue(firstButton.isFocused());
        assertEquals(0, mViewPager.getCurrentItem());

        // Normal arrows should traverse contents first
        onView(is(firstButton)).perform(pressKey(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertEquals(0, mViewPager.getCurrentItem());
        assertTrue(adapter.getButton(0, 1).isFocused());

        // Alt arrows should change page even if there are more focusables in that direction
        onView(is(adapter.getButton(0, 1))).perform(pressKey(new EspressoKey.Builder()
                .withAltPressed(true).withKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT).build()));
        assertEquals(1, mViewPager.getCurrentItem());
        assertTrue(adapter.getButton(1, 0).isFocused());

        // Normal arrows should change page if there are no more focusables in that direction
        onView(is(adapter.getButton(1, 0))).perform(pressKey(KeyEvent.KEYCODE_DPAD_LEFT));
        assertEquals(0, mViewPager.getCurrentItem());
    }
}
