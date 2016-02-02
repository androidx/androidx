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
package android.support.v7.app;

import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.appcompat.test.R;
import android.support.v7.custom.CustomDrawerLayout;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import org.junit.Before;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.v7.testutils.DrawerLayoutActions.closeDrawer;
import static android.support.v7.testutils.DrawerLayoutActions.openDrawer;
import static android.support.v7.testutils.DrawerLayoutActions.setDrawerLockMode;
import static android.support.v7.testutils.TestUtilsActions.setLayoutDirection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DrawerLayoutDoubleTest
        extends BaseInstrumentationTestCase<DrawerLayoutDoubleActivity> {
    private CustomDrawerLayout mDrawerLayout;

    private View mStartDrawer;

    private View mEndDrawer;

    private View mContentView;

    public DrawerLayoutDoubleTest() {
        super(DrawerLayoutDoubleActivity.class);
    }

    @Before
    public void setUp() {
        final DrawerLayoutDoubleActivity activity = mActivityTestRule.getActivity();
        mDrawerLayout = (CustomDrawerLayout) activity.findViewById(R.id.drawer_layout);
        mStartDrawer = mDrawerLayout.findViewById(R.id.start_drawer);
        mEndDrawer = mDrawerLayout.findViewById(R.id.end_drawer);
        mContentView = mDrawerLayout.findViewById(R.id.content);

        // Close the drawers to reset the state for the next test
        onView(withId(R.id.drawer_layout)).perform(closeDrawer(mStartDrawer));
        onView(withId(R.id.drawer_layout)).perform(closeDrawer(mEndDrawer));
    }

    @Test(expected=IllegalArgumentException.class)
    @SmallTest
    public void testQueryOpenStateOfNonExistentDrawer() {
        // Note that we're expecting the isDrawerOpen API call to result in an exception being
        // thrown since mContentView is not a drawer.
        assertFalse("Querying open state of a view that is not a drawer",
                mDrawerLayout.isDrawerOpen(mContentView));
    }

    @Test(expected=IllegalArgumentException.class)
    @SmallTest
    public void testQueryVisibleStateOfNonExistentDrawer() {
        // Note that we're expecting the isDrawerVisible API call to result in an exception being
        // thrown since mContentView is not a drawer.
        assertFalse("Querying visible state of a view that is not a drawer",
                mDrawerLayout.isDrawerVisible(mContentView));
    }

    @Test(expected=IllegalArgumentException.class)
    @SmallTest
    public void testOpenNonExistentDrawer() {
        // Note that we're expecting the openDrawer action to result in an exception being
        // thrown since mContentView is not a drawer.
        onView(withId(R.id.drawer_layout)).perform(openDrawer(mContentView));
    }

    @Test(expected=IllegalArgumentException.class)
    @SmallTest
    public void testCloseNonExistentDrawer() {
        // Note that we're expecting the closeDrawer action to result in an exception being
        // thrown since mContentView is not a drawer.
        onView(withId(R.id.drawer_layout)).perform(closeDrawer(mContentView));
    }

    @Test(expected=IllegalArgumentException.class)
    @SmallTest
    public void testLockNonExistentDrawer() {
        // Note that we're expecting the setDrawerLockMode action to result in an exception being
        // thrown since mContentView is not a drawer.
        onView(withId(R.id.drawer_layout)).perform(
                setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, mContentView));
    }

    private void verifyDrawerOpenClose() {
        assertFalse("Start drawer is closed in initial state",
                mDrawerLayout.isDrawerOpen(mStartDrawer));
        assertFalse("Start drawer is not visible in initial state",
                mDrawerLayout.isDrawerVisible(mStartDrawer));
        assertFalse("End drawer is closed in initial state",
                mDrawerLayout.isDrawerOpen(mEndDrawer));
        assertFalse("End drawer is not visible in initial state",
                mDrawerLayout.isDrawerVisible(mEndDrawer));

        // Open the start drawer
        onView(withId(R.id.drawer_layout)).perform(openDrawer(mStartDrawer));
        // And check that it's open (with the end drawer closed)
        assertTrue("Start drawer is now open", mDrawerLayout.isDrawerOpen(mStartDrawer));
        assertTrue("Start drawer is now visible", mDrawerLayout.isDrawerVisible(mStartDrawer));
        assertFalse("End drawer is still closed", mDrawerLayout.isDrawerOpen(mEndDrawer));
        assertFalse("End drawer is still not visible", mDrawerLayout.isDrawerVisible(mEndDrawer));

        // Close the start drawer
        onView(withId(R.id.drawer_layout)).perform(closeDrawer(mStartDrawer));
        // And check that both drawers are closed
        assertFalse("Start drawer is now closed", mDrawerLayout.isDrawerOpen(mStartDrawer));
        assertFalse("Start drawer is now not visible", mDrawerLayout.isDrawerVisible(mStartDrawer));
        assertFalse("End drawer is still closed", mDrawerLayout.isDrawerOpen(mEndDrawer));
        assertFalse("End drawer is still not visible", mDrawerLayout.isDrawerVisible(mEndDrawer));

        // Open the end drawer
        onView(withId(R.id.drawer_layout)).perform(openDrawer(mEndDrawer));
        // And check that it's open (with the start drawer closed)
        assertFalse("Start drawer is still closed", mDrawerLayout.isDrawerOpen(mStartDrawer));
        assertFalse("Start drawer is still not visible",
                mDrawerLayout.isDrawerVisible(mStartDrawer));
        assertTrue("End drawer is now open", mDrawerLayout.isDrawerOpen(mEndDrawer));
        assertTrue("End drawer is now visible", mDrawerLayout.isDrawerVisible(mEndDrawer));

        // Close the end drawer
        onView(withId(R.id.drawer_layout)).perform(closeDrawer(mEndDrawer));
        // And check that both drawers are closed
        assertFalse("Start drawer is still closed", mDrawerLayout.isDrawerOpen(mStartDrawer));
        assertFalse("Start drawer is still not visible",
                mDrawerLayout.isDrawerVisible(mStartDrawer));
        assertFalse("End drawer is still closed", mDrawerLayout.isDrawerOpen(mEndDrawer));
        assertFalse("End drawer is still not visible", mDrawerLayout.isDrawerVisible(mEndDrawer));
    }

    @Test
    @SmallTest
    public void testDrawerOpenCloseLtr() {
        onView(withId(R.id.drawer_layout)).perform(
                setLayoutDirection(ViewCompat.LAYOUT_DIRECTION_LTR));

        verifyDrawerOpenClose();
    }

    @Test
    @SmallTest
    public void testDrawerOpenCloseRtl() {
        onView(withId(R.id.drawer_layout)).perform(
                setLayoutDirection(ViewCompat.LAYOUT_DIRECTION_RTL));

        verifyDrawerOpenClose();
    }

    private void verifyDrawerLockUnlock() {
        assertEquals("Start drawer is unlocked in initial state",
                DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLayout.getDrawerLockMode(mStartDrawer));
        assertEquals("End drawer is unlocked in initial state",
                DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLayout.getDrawerLockMode(mEndDrawer));

        // Lock the start drawer open
        onView(withId(R.id.drawer_layout)).perform(
                setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, mStartDrawer));
        // And check that it's locked open (with the end drawer unlocked)
        assertEquals("Start drawer is now locked open",
                DrawerLayout.LOCK_MODE_LOCKED_OPEN, mDrawerLayout.getDrawerLockMode(mStartDrawer));
        assertEquals("End drawer is still unlocked",
                DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLayout.getDrawerLockMode(mEndDrawer));

        // Unlock the start drawer and close it
        onView(withId(R.id.drawer_layout)).perform(
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mStartDrawer));
        onView(withId(R.id.drawer_layout)).perform(closeDrawer(mStartDrawer));
        // And check that both drawers are unlocked
        assertEquals("Start drawer is now unlocked",
                DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLayout.getDrawerLockMode(mStartDrawer));
        assertEquals("End drawer is now unlocked",
                DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLayout.getDrawerLockMode(mEndDrawer));

        // Lock the end drawer open
        onView(withId(R.id.drawer_layout)).perform(
                setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, mEndDrawer));
        // And check that it's locked open (with the start drawer unlocked)
        assertEquals("Start drawer is still unlocked",
                DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLayout.getDrawerLockMode(mStartDrawer));
        assertEquals("End drawer is now locked open",
                DrawerLayout.LOCK_MODE_LOCKED_OPEN, mDrawerLayout.getDrawerLockMode(mEndDrawer));

        // Unlock the end drawer and close it
        onView(withId(R.id.drawer_layout)).perform(
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mEndDrawer));
        onView(withId(R.id.drawer_layout)).perform(closeDrawer(mEndDrawer));
        // And check that both drawers are unlocked
        assertEquals("Start drawer is now unlocked",
                DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLayout.getDrawerLockMode(mStartDrawer));
        assertEquals("End drawer is now unlocked",
                DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLayout.getDrawerLockMode(mEndDrawer));
    }

    @Test
    @SmallTest
    public void testDrawerLockUnlockLtr() {
        onView(withId(R.id.drawer_layout)).perform(
                setLayoutDirection(ViewCompat.LAYOUT_DIRECTION_LTR));

        verifyDrawerLockUnlock();
    }

    @Test
    @SmallTest
    public void testDrawerLockUnlockRtl() {
        onView(withId(R.id.drawer_layout)).perform(
                setLayoutDirection(ViewCompat.LAYOUT_DIRECTION_RTL));

        verifyDrawerLockUnlock();
    }
}
