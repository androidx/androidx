/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.leanback.tab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.LinearLayout;

import androidx.leanback.tab.app.TabLayoutTestActivity;
import androidx.leanback.tab.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.testutils.AnimationActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class LeanbackTabLayoutTest {

    @Rule
    public AnimationActivityTestRule<TabLayoutTestActivity> mActivityTestRule =
            new AnimationActivityTestRule<TabLayoutTestActivity>(TabLayoutTestActivity.class,
                    false, false);

    Activity mActivity;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.launchActivity(new Intent());
    }

    /**
     * Tests the tab changes when it is focused
     * @throws Throwable
     */
    @Test
    public void testChangeOfTabOnFocusChange() throws Throwable {

        LeanbackTabLayout leanbackTabLayout = mActivity.findViewById(R.id.tab_layout);
        LeanbackViewPager leanbackViewPager = mActivity.findViewById(R.id.view_pager);

        LinearLayout tabStrip = (LinearLayout) leanbackTabLayout.getChildAt(0);

        int numberOfTabs = TabLayoutTestActivity.getTabCount();

        mActivityTestRule.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < numberOfTabs; ++i) {
                            tabStrip.getChildAt(i).requestFocus();
                            int index = leanbackViewPager.getCurrentItem();
                            assertEquals(i, index);
                        }
                    }
                }
        );
    }

    /**
     * Tests focus does not move out of the tabs when DPAD_RIGHT is received on the rightmost tab
     * and DPAD_LEFT is received in the leftmost tab
     * @throws Throwable
     */
    @Test
    public void testChangeOfTabOnExtremeEnd() throws Throwable {

        LeanbackTabLayout leanbackTabLayout = mActivity.findViewById(R.id.tab_layout);
        LeanbackViewPager leanbackViewPager = mActivity.findViewById(R.id.view_pager);

        LinearLayout tabStrip = (LinearLayout) leanbackTabLayout.getChildAt(0);

        int numberOfTabs = TabLayoutTestActivity.getTabCount();

        focusOnTab(0, tabStrip);

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT);
        int index = leanbackViewPager.getCurrentItem();
        assertEquals(0, index);

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
        index = leanbackViewPager.getCurrentItem();
        assertEquals(1, index);

        focusOnTab(numberOfTabs - 1, tabStrip);

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
        index = leanbackViewPager.getCurrentItem();
        assertEquals(numberOfTabs - 1, index);
    }

    /**
     * Test focus moves back to the currently selected tab on DPAD_UP
     * @throws Throwable
     */
    @Test
    public void testFocusMovesBackToTab() throws Throwable {

        LeanbackTabLayout leanbackTabLayout = mActivity.findViewById(R.id.tab_layout);
        LeanbackViewPager leanbackViewPager = mActivity.findViewById(R.id.view_pager);

        LinearLayout tabStrip = (LinearLayout) leanbackTabLayout.getChildAt(0);

        int numberOfTabs = TabLayoutTestActivity.getTabCount();

        for (int tabIndex = 0; tabIndex < numberOfTabs; ++tabIndex) {

            focusOnTab(tabIndex, tabStrip);

            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(
                    KeyEvent.KEYCODE_DPAD_DOWN);

            assertFalse(leanbackTabLayout.hasFocus());
            assertTrue(leanbackViewPager.hasFocus());

            sendRepeatedDpadEvent(KeyEvent.KEYCODE_DPAD_RIGHT, 10);

            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(
                    KeyEvent.KEYCODE_DPAD_UP);
            assertTrue(leanbackTabLayout.hasFocus());
            assertEquals(tabIndex, leanbackViewPager.getCurrentItem());
        }
    }

    /**
     * Tests focus does not move from viewpager to tab on DPAD_LEFT, DPAD_RIGHT and the current
     * tab is not changed when DPAD_LEFT,DPAD_RIGHT events are received when focus is on viewpager
     * @throws Throwable
     */
    @Test
    public void testFocusDoesNotMoveToTabOnDpadLeftRight() throws Throwable {

        LeanbackTabLayout leanbackTabLayout = mActivity.findViewById(R.id.tab_layout);
        LeanbackViewPager leanbackViewPager = mActivity.findViewById(R.id.view_pager);

        LinearLayout tabStrip = (LinearLayout) leanbackTabLayout.getChildAt(0);

        int numberOfTabs = TabLayoutTestActivity.getTabCount();

        for (int tabIndex = 0; tabIndex < numberOfTabs; ++tabIndex) {

            focusOnTab(tabIndex, tabStrip);

            Thread.sleep(100);

            assertTrue(leanbackTabLayout.hasFocus());

            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(
                    KeyEvent.KEYCODE_DPAD_DOWN);

            assertEquals(tabIndex, leanbackViewPager.getCurrentItem());
            assertFalse(leanbackTabLayout.hasFocus());
            assertTrue(leanbackViewPager.hasFocus());

            sendRepeatedDpadEvent(KeyEvent.KEYCODE_DPAD_RIGHT, 15);
            assertTrue(leanbackViewPager.hasFocus());

            sendRepeatedDpadEvent(KeyEvent.KEYCODE_DPAD_LEFT, 15);
            assertTrue(leanbackViewPager.hasFocus());

            assertEquals(tabIndex, leanbackViewPager.getCurrentItem());

            leanbackViewPager.setKeyEventsEnabled(true);
            sendRepeatedDpadEvent(KeyEvent.KEYCODE_DPAD_LEFT, 15);
            assertEquals(0, leanbackViewPager.getCurrentItem());
            leanbackViewPager.setKeyEventsEnabled(false);
        }
    }

    private void sendDpadEvent(int keyCode) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(
                    keyCode);
    }

    private void sendRepeatedDpadEvent(int keyCode, int count) {
        for (int i = 0; i < count; ++i) {
            sendDpadEvent(keyCode);
        }
    }

    private void focusOnTab(final int tabIndex, LinearLayout tabStrip) throws Throwable {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        tabStrip.getChildAt(tabIndex).requestFocus();
                    }
                }
        );
    }
}
