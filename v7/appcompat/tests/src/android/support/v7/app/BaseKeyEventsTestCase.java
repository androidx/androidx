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

package android.support.v7.app;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.support.v7.appcompat.test.R;
import android.support.v7.testutils.BaseTestActivity;
import android.support.v7.view.ActionMode;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseKeyEventsTestCase<A extends BaseTestActivity>
        extends BaseInstrumentationTestCase<A> {

    protected BaseKeyEventsTestCase(Class<A> activityClass) {
        super(activityClass);
    }

    @Test
    @SmallTest
    public void testBackDismissesActionMode() {
        final AtomicBoolean destroyed = new AtomicBoolean();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().startSupportActionMode(new ActionMode.Callback() {
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

        getInstrumentation().waitForIdleSync();
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        getInstrumentation().waitForIdleSync();

        assertFalse("Activity was not finished", getActivity().isFinishing());
        assertTrue("ActionMode was destroyed", destroyed.get());
    }

    @Test
    @SmallTest
    public void testBackCollapsesSearchView() throws InterruptedException {
        final String itemTitle = getActivity().getString(R.string.search_menu_title);

        // Click on the Search menu item
        onView(withContentDescription(itemTitle)).perform(click());
        // Check that the SearchView is displayed
        onView(withId(R.id.search_bar)).check(matches(isDisplayed()));

        // Wait for the IME to show
        getInstrumentation().waitForIdleSync();
        // Now send a back event to dismiss the IME
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        // ...and another to collapse the SearchView
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);

        // Check that the Activity is still running
        assertFalse(getActivity().isFinishing());
        assertFalse(getActivity().isDestroyed());
        // ...and that the SearchView is not attached
        onView(withId(R.id.search_bar)).check(doesNotExist());
    }

    @Test
    @SmallTest
    public void testMenuPressInvokesPanelCallbacks() throws InterruptedException {
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        getInstrumentation().waitForIdleSync();
        assertTrue("onMenuOpened called", getActivity().wasOnMenuOpenedCalled());

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        getInstrumentation().waitForIdleSync();
        assertTrue("onPanelClosed called", getActivity().wasOnPanelClosedCalled());
    }

    @Test
    @SmallTest
    public void testBackPressWithMenuInvokesOnPanelClosed() throws InterruptedException {
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        getInstrumentation().waitForIdleSync();

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        getInstrumentation().waitForIdleSync();
        assertTrue("onPanelClosed called", getActivity().wasOnPanelClosedCalled());
    }

    @Test
    @SmallTest
    public void testBackPressWithEmptyMenuFinishesActivity() throws InterruptedException {
        repopulateWithEmptyMenu();

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        getInstrumentation().waitForIdleSync();

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        assertTrue(getActivity().isFinishing());
    }

    @Test
    @SmallTest
    public void testDelKeyEventReachesActivity() {
        // First send the event
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DEL);
        getInstrumentation().waitForIdleSync();

        KeyEvent downEvent = getActivity().getInvokedKeyDownEvent();
        assertNotNull("onKeyDown called", downEvent);
        assertEquals("onKeyDown event matches", KeyEvent.KEYCODE_DEL, downEvent.getKeyCode());

        KeyEvent upEvent = getActivity().getInvokedKeyUpEvent();
        assertNotNull("onKeyUp called", upEvent);
        assertEquals("onKeyUp event matches", KeyEvent.KEYCODE_DEL, upEvent.getKeyCode());
    }

    @Test
    @SmallTest
    public void testMenuKeyEventReachesActivity() throws InterruptedException {
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        getInstrumentation().waitForIdleSync();

        KeyEvent downEvent = getActivity().getInvokedKeyDownEvent();
        assertNotNull("onKeyDown called", downEvent);
        assertEquals("onKeyDown event matches", KeyEvent.KEYCODE_MENU, downEvent.getKeyCode());

        KeyEvent upEvent = getActivity().getInvokedKeyUpEvent();
        assertNotNull("onKeyUp called", upEvent);
        assertEquals("onKeyDown event matches", KeyEvent.KEYCODE_MENU, upEvent.getKeyCode());
    }

    private void repopulateWithEmptyMenu() throws InterruptedException {
        int count = 0;
        getActivity().setShouldPopulateOptionsMenu(false);
        while (count++ < 10) {
            Menu menu = getActivity().getMenu();
            if (menu == null || menu.size() != 0) {
                Thread.sleep(100);
            } else {
                return;
            }
        }
    }
}
