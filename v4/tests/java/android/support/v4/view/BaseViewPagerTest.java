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

import android.app.Activity;
import android.graphics.Color;
import android.support.test.espresso.ViewAction;
import android.support.v4.BaseInstrumentationTestCase;
import android.support.v4.test.R;
import android.support.v4.testutils.TestUtilsMatchers;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.PositionAssertions.*;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.v4.testutils.TestUtilsAssertions.hasDisplayedChildren;
import static android.support.v4.testutils.TestUtilsMatchers.*;
import static android.support.v4.view.ViewPagerActions.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Base class for testing <code>ViewPager</code>. Most of the testing logic should be in this
 * class as it is independent on the specific pager title implementation (interactive or non
 * interactive).
 *
 * Testing logic that does depend on the specific pager title implementation is pushed into the
 * extending classes in <code>assertStripInteraction()</code> method.
 */
public abstract class BaseViewPagerTest<T extends Activity> extends BaseInstrumentationTestCase<T> {
    protected ViewPager mViewPager;

    protected static class BasePagerAdapter<Q> extends PagerAdapter {
        protected ArrayList<Pair<String, Q>> mEntries = new ArrayList<>();

        public void add(String title, Q content) {
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

    public BaseViewPagerTest(Class<T> activityClass) {
        super(activityClass);
    }

    @Before
    public void setUp() throws Exception {
        final T activity = mActivityTestRule.getActivity();
        mViewPager = (ViewPager) activity.findViewById(R.id.pager);

        ColorPagerAdapter adapter = new ColorPagerAdapter();
        adapter.add("Red", Color.RED);
        adapter.add("Green", Color.GREEN);
        adapter.add("Blue", Color.BLUE);
        onView(withId(R.id.pager)).perform(setAdapter(adapter), scrollToPage(0, false));
    }

    @After
    public void tearDown() throws Exception {
        onView(withId(R.id.pager)).perform(setAdapter(null));
    }

    private void verifyPageSelections(boolean smoothScroll) {
        assertEquals("Initial state", 0, mViewPager.getCurrentItem());

        ViewPager.OnPageChangeListener mockPageChangeListener =
                mock(ViewPager.OnPageChangeListener.class);
        mViewPager.addOnPageChangeListener(mockPageChangeListener);

        onView(withId(R.id.pager)).perform(scrollRight(smoothScroll));
        assertEquals("Scroll right", 1, mViewPager.getCurrentItem());
        verify(mockPageChangeListener, times(1)).onPageSelected(1);

        onView(withId(R.id.pager)).perform(scrollRight(smoothScroll));
        assertEquals("Scroll right", 2, mViewPager.getCurrentItem());
        verify(mockPageChangeListener, times(1)).onPageSelected(2);

        // Try "scrolling" beyond the last page and test that we're still on the last page.
        onView(withId(R.id.pager)).perform(scrollRight(smoothScroll));
        assertEquals("Scroll right beyond last page", 2, mViewPager.getCurrentItem());
        // We're still on this page, so we shouldn't have been called again with index 2
        verify(mockPageChangeListener, times(1)).onPageSelected(2);

        onView(withId(R.id.pager)).perform(scrollLeft(smoothScroll));
        assertEquals("Scroll left", 1, mViewPager.getCurrentItem());
        // Verify that this is the second time we're called on index 1
        verify(mockPageChangeListener, times(2)).onPageSelected(1);

        onView(withId(R.id.pager)).perform(scrollLeft(smoothScroll));
        assertEquals("Scroll left", 0, mViewPager.getCurrentItem());
        // Verify that this is the first time we're called on index 0
        verify(mockPageChangeListener, times(1)).onPageSelected(0);

        // Try "scrolling" beyond the first page and test that we're still on the first page.
        onView(withId(R.id.pager)).perform(scrollLeft(smoothScroll));
        assertEquals("Scroll left beyond first page", 0, mViewPager.getCurrentItem());
        // We're still on this page, so we shouldn't have been called again with index 0
        verify(mockPageChangeListener, times(1)).onPageSelected(0);

        // Unregister our listener
        mViewPager.removeOnPageChangeListener(mockPageChangeListener);

        // Go from index 0 to index 2
        onView(withId(R.id.pager)).perform(scrollToPage(2, smoothScroll));
        assertEquals("Scroll to last page", 2, mViewPager.getCurrentItem());
        // Our listener is not registered anymore, so we shouldn't have been called with index 2
        verify(mockPageChangeListener, times(1)).onPageSelected(2);

        // And back to 0
        onView(withId(R.id.pager)).perform(scrollToPage(0, smoothScroll));
        assertEquals("Scroll to first page", 0, mViewPager.getCurrentItem());
        // Our listener is not registered anymore, so we shouldn't have been called with index 0
        verify(mockPageChangeListener, times(1)).onPageSelected(0);

        // Verify the overall sequence of calls to onPageSelected of our listener
        ArgumentCaptor<Integer> pageSelectedCaptor = ArgumentCaptor.forClass(int.class);
        verify(mockPageChangeListener, times(4)).onPageSelected(pageSelectedCaptor.capture());
        assertThat(pageSelectedCaptor.getAllValues(), TestUtilsMatchers.matches(1, 2, 1, 0));
    }

    @Test
    @SmallTest
    public void testPageSelectionsImmediate() {
        verifyPageSelections(false);
    }

    @Test
    @SmallTest
    public void testPageSelectionsSmooth() {
        verifyPageSelections(true);
    }

    @Test
    @SmallTest
    public void testPageSwipes() {
        assertEquals("Initial state", 0, mViewPager.getCurrentItem());

        ViewPager.OnPageChangeListener mockPageChangeListener =
                mock(ViewPager.OnPageChangeListener.class);
        mViewPager.addOnPageChangeListener(mockPageChangeListener);

        onView(withId(R.id.pager)).perform(swipeLeft());
        assertEquals("Swipe left", 1, mViewPager.getCurrentItem());
        verify(mockPageChangeListener, times(1)).onPageSelected(1);

        onView(withId(R.id.pager)).perform(swipeLeft());
        assertEquals("Swipe left", 2, mViewPager.getCurrentItem());
        verify(mockPageChangeListener, times(1)).onPageSelected(2);

        // Try swiping beyond the last page and test that we're still on the last page.
        onView(withId(R.id.pager)).perform(swipeLeft());
        assertEquals("Swipe left beyond last page", 2, mViewPager.getCurrentItem());
        // We're still on this page, so we shouldn't have been called again with index 2
        verify(mockPageChangeListener, times(1)).onPageSelected(2);

        onView(withId(R.id.pager)).perform(swipeRight());
        assertEquals("Swipe right", 1, mViewPager.getCurrentItem());
        // Verify that this is the second time we're called on index 1
        verify(mockPageChangeListener, times(2)).onPageSelected(1);

        onView(withId(R.id.pager)).perform(swipeRight());
        assertEquals("Swipe right", 0, mViewPager.getCurrentItem());
        // Verify that this is the first time we're called on index 0
        verify(mockPageChangeListener, times(1)).onPageSelected(0);

        // Try swiping beyond the first page and test that we're still on the first page.
        onView(withId(R.id.pager)).perform(swipeRight());
        assertEquals("Swipe right beyond first page", 0, mViewPager.getCurrentItem());
        // We're still on this page, so we shouldn't have been called again with index 0
        verify(mockPageChangeListener, times(1)).onPageSelected(0);

        mViewPager.removeOnPageChangeListener(mockPageChangeListener);

        // Verify the overall sequence of calls to onPageSelected of our listener
        ArgumentCaptor<Integer> pageSelectedCaptor = ArgumentCaptor.forClass(int.class);
        verify(mockPageChangeListener, times(4)).onPageSelected(pageSelectedCaptor.capture());
        assertThat(pageSelectedCaptor.getAllValues(), TestUtilsMatchers.matches(1, 2, 1, 0));
    }

    @Test
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
        onView(withId(R.id.pager)).perform(scrollRight(smoothScroll));
        assertEquals("Scroll right", 1, mViewPager.getCurrentItem());
        // Pages #0 / #2 should not be displayed, page #1 should be displayed.
        onView(withId(R.id.page_0)).check(matches(not(isDisplayed())));
        onView(withId(R.id.page_1)).check(matches(allOf(
                isOfClass(View.class),
                isDisplayed(),
                backgroundColor(Color.GREEN))));
        onView(withId(R.id.page_2)).check(matches(not(isDisplayed())));

        // Scroll one more page to select page #2
        onView(withId(R.id.pager)).perform(scrollRight(smoothScroll));
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
    @SmallTest
    public void testPageContentImmediate() {
        verifyPageContent(false);
    }

    @Test
    @SmallTest
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
        onView(withId(R.id.pager)).perform(setAdapter(newAdapter), scrollToPage(0, smoothScroll));

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
        onView(withId(R.id.pager)).perform(scrollRight(smoothScroll));
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
        onView(withId(R.id.pager)).perform(scrollRight(smoothScroll));
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
        onView(withId(R.id.pager)).perform(scrollRight(smoothScroll));
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
    @SmallTest
    public void testAdapterChangeImmediate() {
        verifyAdapterChange(false);
    }

    @Test
    @SmallTest
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
        onView(withId(R.id.pager)).perform(setAdapter(adapter),
                scrollToPage(0, smoothScroll));

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
        onView(withId(R.id.pager)).perform(scrollRight(smoothScroll));
        verifyTitleStripLayout("Red", "Green", "Blue", R.id.page_1);

        // Scroll one page to select page #2 and check layout / content of title strip
        onView(withId(R.id.pager)).perform(scrollRight(smoothScroll));
        verifyTitleStripLayout("Green", "Blue", "Yellow", R.id.page_2);

        // Scroll one page to select page #3 and check layout / content of title strip
        onView(withId(R.id.pager)).perform(scrollRight(smoothScroll));
        verifyTitleStripLayout("Blue", "Yellow", "Magenta", R.id.page_3);

        // Scroll one page to select page #4 and check layout / content of title strip
        onView(withId(R.id.pager)).perform(scrollRight(smoothScroll));
        verifyTitleStripLayout("Yellow", "Magenta", null, R.id.page_4);

        // Scroll back to page #0
        onView(withId(R.id.pager)).perform(scrollToPage(0, smoothScroll));

        assertStripInteraction(smoothScroll);
    }

    @Test
    @SmallTest
    public void testPagerStripImmediate() {
        verifyPagerStrip(false);
    }

    @Test
    @SmallTest
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
    @SmallTest
    public void testPageScrollStateChangedImmediate() {
        // Note that all the actions tested in this method are immediate (no scrolling) and
        // as such we test that we do not get any calls to onPageScrollStateChanged in any of them

        // Select one page to the right
        verifyScrollStateChange(scrollRight(false));
        // Select one more page to the right
        verifyScrollStateChange(scrollRight(false));
        // Select one page to the left
        verifyScrollStateChange(scrollLeft(false));
        // Select one more page to the left
        verifyScrollStateChange(scrollLeft(false));
        // Select last page
        verifyScrollStateChange(scrollToLast(false));
        // Select first page
        verifyScrollStateChange(scrollToFirst(false));
    }

    @Test
    @MediumTest
    public void testPageScrollStateChangedSmooth() {
        // Note that all the actions tested in this method use smooth scrolling and as such we test
        // that we get the matching calls to onPageScrollStateChanged
        final int[] expectedScrollStateChanges = new int[] {
                ViewPager.SCROLL_STATE_SETTLING, ViewPager.SCROLL_STATE_IDLE
        };

        // Select one page to the right
        verifyScrollStateChange(scrollRight(true), expectedScrollStateChanges);
        // Select one more page to the right
        verifyScrollStateChange(scrollRight(true), expectedScrollStateChanges);
        // Select one page to the left
        verifyScrollStateChange(scrollLeft(true), expectedScrollStateChanges);
        // Select one more page to the left
        verifyScrollStateChange(scrollLeft(true), expectedScrollStateChanges);
        // Select last page
        verifyScrollStateChange(scrollToLast(true), expectedScrollStateChanges);
        // Select first page
        verifyScrollStateChange(scrollToFirst(true), expectedScrollStateChanges);
    }
}
