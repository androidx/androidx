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

package androidx.preference.tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.view.View;
import android.view.ViewGroup.OnHierarchyChangeListener;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.test.R;
import androidx.preference.tests.helpers.PreferenceTestHelperActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

/**
 * Test for {@link androidx.preference.Preference} visibility logic.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class PreferenceVisibilityTest {

    @SuppressWarnings("deprecation")
    @Rule
    public androidx.test.rule.ActivityTestRule<PreferenceTestHelperActivity> mActivityRule =
            new androidx.test.rule.ActivityTestRule<>(PreferenceTestHelperActivity.class);

    private static final String CATEGORY = "Category";
    private static final String DEFAULT = "Default";
    private static final String VISIBLE = "Visible";
    private static final String INVISIBLE = "Invisible";

    private PreferenceFragmentCompat mFragment;

    @Before
    @UiThreadTest
    public void setUp() {
        mFragment = mActivityRule.getActivity().setupPreferenceHierarchy(
                R.xml.test_visibility);
    }

    @Test
    public void preferencesInflatedFromXml_visibilitySetCorrectly() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Parent category without visibility set should be visible and shown
                assertTrue(mFragment.findPreference("category").isVisible());
                assertTrue(mFragment.findPreference("category").isShown());
                // Preference without visibility set should be visible and shown
                assertTrue(mFragment.findPreference("default").isVisible());
                assertTrue(mFragment.findPreference("default").isShown());
                // Preference with visibility set to true should be visible and shown
                assertTrue(mFragment.findPreference("visible").isVisible());
                assertTrue(mFragment.findPreference("visible").isShown());
                // Preference with visibility set to false should not be visible or shown
                assertFalse(mFragment.findPreference("invisible").isVisible());
                assertFalse(mFragment.findPreference("invisible").isShown());
            }
        });

        // Parent category without visibility set should be displayed
        onView(withText(CATEGORY)).check(matches(isDisplayed()));
        // Preference without visibility set should be displayed
        onView(withText(DEFAULT)).check(matches(isDisplayed()));
        // Preference with visibility set to true should be displayed
        onView(withText(VISIBLE)).check(matches(isDisplayed()));
        // Preference with visibility set to false should not be displayed
        onView(withText(INVISIBLE)).check(doesNotExist());

        // The category and its two visible children should be added to the RecyclerView
        assertEquals(3, mFragment.getListView().getChildCount());
    }

    @Test
    public void hidingPreference_visibilitySetCorrectly() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Hide a preference
                mFragment.findPreference("default").setVisible(false);

                // Preference should not be visible or shown
                assertFalse(mFragment.findPreference("default").isVisible());
                assertFalse(mFragment.findPreference("default").isShown());
            }
        });

        CountDownLatch latch = new CountDownLatch(1);
        // Expect a hierarchy change
        mFragment.getListView().setOnHierarchyChangeListener(new Listener(latch, true));

        // Wait for hierarchy rebuild
        latch.await(1, SECONDS);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Preference should no longer be shown
        onView(withText(DEFAULT)).check(doesNotExist());

        // This shouldn't affect other preferences
        onView(withText(CATEGORY)).check(matches(isDisplayed()));
        onView(withText(VISIBLE)).check(matches(isDisplayed()));
        onView(withText(INVISIBLE)).check(doesNotExist());

        // The category and its only visible child should be added to the RecyclerView
        assertEquals(2, mFragment.getListView().getChildCount());
    }

    @Test
    public void hidingParentGroup_childrenNeverVisible() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Hide the parent category
                mFragment.findPreference("category").setVisible(false);

                // Category should not be visible or shown
                assertFalse(mFragment.findPreference("category").isVisible());
                assertFalse(mFragment.findPreference("category").isShown());

                // Preference visibility should be unchanged
                assertTrue(mFragment.findPreference("default").isVisible());
                assertTrue(mFragment.findPreference("visible").isVisible());
                assertFalse(mFragment.findPreference("invisible").isVisible());

                // Preferences should not be shown since their parent is not visible
                assertFalse(mFragment.findPreference("default").isShown());
                assertFalse(mFragment.findPreference("visible").isShown());
                assertFalse(mFragment.findPreference("invisible").isShown());
            }
        });

        CountDownLatch latch = new CountDownLatch(1);
        // Expect a hierarchy change
        mFragment.getListView().setOnHierarchyChangeListener(new Listener(latch, true));

        // Wait for hierarchy rebuild
        latch.await(1, SECONDS);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Nothing should be displayed
        onView(withText(CATEGORY)).check(doesNotExist());
        onView(withText(DEFAULT)).check(doesNotExist());
        onView(withText(VISIBLE)).check(doesNotExist());
        onView(withText(INVISIBLE)).check(doesNotExist());

        // Nothing should be added to the RecyclerView
        assertEquals(0, mFragment.getListView().getChildCount());

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Attempt to show a previously hidden preference
                mFragment.findPreference("invisible").setVisible(true);
            }
        });

        // Create a new latch to reset state
        latch = new CountDownLatch(1);
        // The hierarchy should not be updated here, but we need to wait to ensure that it isn't.
        mFragment.getListView().setOnHierarchyChangeListener(new Listener(latch, false));

        // Wait for hierarchy rebuild
        latch.await(1, SECONDS);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // No preferences should be visible
        onView(withText(CATEGORY)).check(doesNotExist());
        onView(withText(DEFAULT)).check(doesNotExist());
        onView(withText(VISIBLE)).check(doesNotExist());
        onView(withText(INVISIBLE)).check(doesNotExist());

        // Nothing should be added to the RecyclerView
        assertEquals(0, mFragment.getListView().getChildCount());
    }

    @Test
    @UiThreadTest
    public void preferenceNotAttachedToHierarchy_visibleButNotShown() {
        // Create a new preference not attached to the root preference screen
        Preference preference = new Preference(mFragment.getContext());

        // Preference is visible, but since it is not attached to the hierarchy, it is not shown
        assertTrue(preference.isVisible());
        assertFalse(preference.isShown());
    }

    /**
     * A {@link OnHierarchyChangeListener} that will count down a provided {@link CountDownLatch}
     * to allow tests to wait for the view hierarchy to be updated, and fail the test if the
     * hierarchy is updated when it shouldn't be.
     */
    private static class Listener implements OnHierarchyChangeListener {

        private final boolean mHierarchyChangeExpected;
        private final CountDownLatch mLatch;

        /**
         * This Listener should be used in
         * {@link RecyclerView#setOnHierarchyChangeListener(OnHierarchyChangeListener)}.
         *
         * @param latch                   The {@link CountDownLatch} to count down
         * @param hierarchyChangeExpected Whether the hierarchy is expected to be updated here. If
         *                                {@code false}, and the hierarchy is updated, the test
         *                                will fail.
         */
        Listener(CountDownLatch latch, boolean hierarchyChangeExpected) {
            mHierarchyChangeExpected = hierarchyChangeExpected;
            mLatch = latch;
        }

        @Override
        public void onChildViewAdded(View view, View view1) {
            updateLatch();
        }

        @Override
        public void onChildViewRemoved(View view, View view1) {
            updateLatch();
        }

        private void updateLatch() {
            if (mHierarchyChangeExpected) {
                mLatch.countDown();
            } else {
                fail("The hierarchy should not have been changed here.");
            }
        }
    }
}
