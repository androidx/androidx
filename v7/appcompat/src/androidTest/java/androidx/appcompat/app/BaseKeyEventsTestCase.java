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

package androidx.appcompat.app;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.test.R;
import androidx.appcompat.testutils.BaseTestActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.view.MenuItemCompat;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
public abstract class BaseKeyEventsTestCase<A extends BaseTestActivity> {
    @Rule
    public final ActivityTestRule<A> mActivityTestRule;

    private Instrumentation mInstrumentation;
    private A mActivity;

    protected BaseKeyEventsTestCase(Class<A> activityClass) {
        mActivityTestRule = new ActivityTestRule<>(activityClass);
    }

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mActivity = mActivityTestRule.getActivity();
    }

    @Test
    @SmallTest
    public void testBackDismissesActionMode() {
        final AtomicBoolean destroyed = new AtomicBoolean();

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.startSupportActionMode(new ActionMode.Callback() {
                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        mode.getMenuInflater().inflate(R.menu.sample_actions, menu);
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        return false;
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        destroyed.set(true);
                    }
                });
            }
        });

        mInstrumentation.waitForIdleSync();
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        mInstrumentation.waitForIdleSync();

        assertFalse("Activity was not finished", mActivity.isFinishing());
        assertTrue("ActionMode was destroyed", destroyed.get());
    }

    @Test
    @LargeTest
    public void testBackCollapsesActionView() throws InterruptedException {
        // Click on the Search menu item
        onView(withId(R.id.action_search)).perform(click());
        // Check that the action view is displayed (expanded)
        onView(withClassName(Matchers.is(CustomCollapsibleView.class.getName())))
                .check(matches(isDisplayed()));

        // Let things settle
        mInstrumentation.waitForIdleSync();
        // Now send a back event to collapse the custom action view
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        mInstrumentation.waitForIdleSync();

        // Check that the Activity is still running
        assertFalse(mActivity.isFinishing());
        assertFalse(mActivity.isDestroyed());
        // ... and that our action view is not attached
        onView(withClassName(Matchers.is(CustomCollapsibleView.class.getName())))
                .check(doesNotExist());
    }

    @Test
    @SmallTest
    public void testMenuPressInvokesPanelCallbacks() throws InterruptedException {
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        mInstrumentation.waitForIdleSync();
        assertTrue("onMenuOpened called", mActivity.wasOnMenuOpenedCalled());

        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        mInstrumentation.waitForIdleSync();
        assertTrue("onPanelClosed called", mActivity.wasOnPanelClosedCalled());
    }

    @Test
    @SmallTest
    public void testBackPressWithMenuInvokesOnPanelClosed() throws InterruptedException {
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        mInstrumentation.waitForIdleSync();

        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        mInstrumentation.waitForIdleSync();
        assertTrue("onPanelClosed called", mActivity.wasOnPanelClosedCalled());
    }

    @Test
    @SmallTest
    public void testBackPressWithEmptyMenuFinishesActivity() throws InterruptedException {
        repopulateWithEmptyMenu();

        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        mInstrumentation.waitForIdleSync();

        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        assertTrue(mActivity.isFinishing());
    }

    @Test
    @SmallTest
    public void testDelKeyEventReachesActivity() {
        // First send the event
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DEL);
        mInstrumentation.waitForIdleSync();

        KeyEvent downEvent = mActivity.getInvokedKeyDownEvent();
        assertNotNull("onKeyDown called", downEvent);
        assertEquals("onKeyDown event matches", KeyEvent.KEYCODE_DEL, downEvent.getKeyCode());

        KeyEvent upEvent = mActivity.getInvokedKeyUpEvent();
        assertNotNull("onKeyUp called", upEvent);
        assertEquals("onKeyUp event matches", KeyEvent.KEYCODE_DEL, upEvent.getKeyCode());
    }

    @Test
    @SmallTest
    public void testMenuKeyEventReachesActivity() throws InterruptedException {
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        mInstrumentation.waitForIdleSync();

        KeyEvent downEvent = mActivity.getInvokedKeyDownEvent();
        assertNotNull("onKeyDown called", downEvent);
        assertEquals("onKeyDown event matches", KeyEvent.KEYCODE_MENU, downEvent.getKeyCode());

        KeyEvent upEvent = mActivity.getInvokedKeyUpEvent();
        assertNotNull("onKeyUp called", upEvent);
        assertEquals("onKeyDown event matches", KeyEvent.KEYCODE_MENU, upEvent.getKeyCode());
    }

    @Test
    @SmallTest
    public void testActionMenuContent() throws Throwable {
        onView(withId(R.id.action_search))
                .check(matches(isDisplayed()))
                .check(matches(withContentDescription(R.string.search_menu_description)));

        onView(withId(R.id.action_alpha_shortcut))
                .check(matches(isDisplayed()))
                .check(matches(withContentDescription((String) null)));

        Menu menu = mActivity.getMenu();
        final MenuItem alphaItem = menu.findItem(R.id.action_alpha_shortcut);
        assertNotNull(alphaItem);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MenuItemCompat.setContentDescription(alphaItem,
                        mActivity.getString(R.string.alpha_menu_description));
                MenuItemCompat.setTooltipText(alphaItem,
                        mActivity.getString(R.string.alpha_menu_tooltip));
            }
        });

        onView(withId(R.id.action_alpha_shortcut))
                .check(matches(isDisplayed()))
                .check(matches(withContentDescription(R.string.alpha_menu_description)));
    }

    private void repopulateWithEmptyMenu() throws InterruptedException {
        int count = 0;
        mActivity.setShouldPopulateOptionsMenu(false);
        while (count++ < 10) {
            Menu menu = mActivity.getMenu();
            if (menu == null || menu.size() != 0) {
                Thread.sleep(100);
            } else {
                return;
            }
        }
    }
}
