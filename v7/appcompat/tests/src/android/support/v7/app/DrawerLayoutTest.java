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

import org.junit.Before;
import org.junit.Test;

import android.os.Build;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.GeneralSwipeAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Swipe;
import android.support.v4.view.GravityCompat;
import android.support.v7.appcompat.test.R;
import android.support.v7.custom.CustomDrawerLayout;
import android.view.View;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DrawerLayoutTest extends BaseInstrumentationTestCase<DrawerLayoutActivity> {
    private CustomDrawerLayout mDrawerLayout;

    private View mStartDrawer;

    private View mContentView;

    public DrawerLayoutTest() {
        super(DrawerLayoutActivity.class);
    }

    @Before
    public void setUp() {

        final DrawerLayoutActivity activity = mActivityTestRule.getActivity();
        mDrawerLayout = (CustomDrawerLayout) activity.findViewById(R.id.drawer_layout);
        mStartDrawer = mDrawerLayout.findViewById(R.id.start_drawer);
        mContentView = mDrawerLayout.findViewById(R.id.content);

        // Close the drawer to reset the state for the next test
        onView(withId(R.id.drawer_layout)).perform(
                DrawerLayoutActions.closeDrawer(GravityCompat.START));
    }

    @Test
    @MediumTest
    public void testDrawerOpenCloseViaAPI() {
        assertFalse("Initial state", mDrawerLayout.isDrawerOpen(GravityCompat.START));

        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.drawer_layout)).perform(
                    DrawerLayoutActions.openDrawer(GravityCompat.START));
            assertTrue("Opened drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            onView(withId(R.id.drawer_layout)).perform(
                    DrawerLayoutActions.closeDrawer(GravityCompat.START));
            assertFalse("Closed drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));
        }
    }

    @Test
    @MediumTest
    public void testDrawerOpenCloseWithRedundancyViaAPI() {
        assertFalse("Initial state", mDrawerLayout.isDrawerOpen(GravityCompat.START));

        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.drawer_layout)).perform(
                    DrawerLayoutActions.openDrawer(GravityCompat.START));
            assertTrue("Opened drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            // Try opening the drawer when it's already opened
            onView(withId(R.id.drawer_layout)).perform(
                    DrawerLayoutActions.openDrawer(GravityCompat.START));
            assertTrue("Opened drawer is still opened #" + i,
                    mDrawerLayout.isDrawerOpen(GravityCompat.START));

            onView(withId(R.id.drawer_layout)).perform(
                    DrawerLayoutActions.closeDrawer(GravityCompat.START));
            assertFalse("Closed drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            // Try closing the drawer when it's already closed
            onView(withId(R.id.drawer_layout)).perform(
                    DrawerLayoutActions.closeDrawer(GravityCompat.START));
            assertFalse("Closed drawer is still closed #" + i,
                    mDrawerLayout.isDrawerOpen(GravityCompat.START));
        }
    }

    @Test
    @MediumTest
    public void testDrawerOpenCloseViaSwipes() {
        assertFalse("Initial state", mDrawerLayout.isDrawerOpen(GravityCompat.START));

        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.drawer_layout)).perform(
                    new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_LEFT,
                            GeneralLocation.CENTER_RIGHT, Press.FINGER));
            assertTrue("Opened drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            onView(withId(R.id.drawer_layout)).perform(
                    new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_RIGHT,
                            GeneralLocation.CENTER_LEFT, Press.FINGER));
            assertFalse("Closed drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));
        }
    }

    @Test
    @MediumTest
    public void testDrawerOpenCloseWithRedundancyViaSwipes() {
        assertFalse("Initial state", mDrawerLayout.isDrawerOpen(GravityCompat.START));

        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.drawer_layout)).perform(
                    new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_LEFT,
                            GeneralLocation.CENTER_RIGHT, Press.FINGER));
            assertTrue("Opened drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            // Try opening the drawer when it's already opened
            onView(withId(R.id.drawer_layout)).perform(
                    new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_LEFT,
                            GeneralLocation.CENTER_RIGHT, Press.FINGER));
            assertTrue("Opened drawer is still opened #" + i,
                    mDrawerLayout.isDrawerOpen(GravityCompat.START));

            onView(withId(R.id.drawer_layout)).perform(
                    new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_RIGHT,
                            GeneralLocation.CENTER_LEFT, Press.FINGER));
            assertFalse("Closed drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            // Try closing the drawer when it's already closed
            onView(withId(R.id.drawer_layout)).perform(
                    new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_RIGHT,
                            GeneralLocation.CENTER_LEFT, Press.FINGER));
            assertFalse("Closed drawer is still closed #" + i,
                    mDrawerLayout.isDrawerOpen(GravityCompat.START));
        }
    }

    @Test
    @SmallTest
    public void testDrawerHeight() {
        // Open the drawer so it becomes visible
        onView(withId(R.id.drawer_layout)).perform(
                DrawerLayoutActions.closeDrawer(GravityCompat.START));

        final int drawerLayoutHeight = mDrawerLayout.getHeight();
        final int startDrawerHeight = mStartDrawer.getHeight();
        final int contentHeight = mContentView.getHeight();

        // On all devices the height of the drawer layout and the drawer should be identical.
        assertEquals("Drawer layout and drawer heights", drawerLayoutHeight, startDrawerHeight);

        if (Build.VERSION.SDK_INT < 21) {
            // On pre-L devices the content height should be the same as the drawer layout height.
            assertEquals("Drawer layout and content heights on pre-L",
                    drawerLayoutHeight, contentHeight);
        } else {
            // Our drawer layout is configured with android:fitsSystemWindows="true" which should be
            // respected on L+ devices to extend the drawer layout into the system status bar.
            // The start drawer is also configured with the same attribute so it should have the
            // same height as the drawer layout. The main content does not have that attribute
            // specified, so it should have its height reduced by the height of the system status
            // bar.

            // Get the system window top inset that was propagated to the top-level DrawerLayout
            // during its layout.
            int drawerTopInset = mDrawerLayout.getSystemWindowInsetTop();
            assertTrue("Drawer top inset is positive on L+", drawerTopInset > 0);
            assertEquals("Drawer layout and drawer heights on L+",
                    drawerLayoutHeight - drawerTopInset, contentHeight);
        }
    }
}
