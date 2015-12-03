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

import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.GeneralSwipeAction;
import android.support.test.espresso.action.Swipe;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.appcompat.test.R;

import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class DrawerLayoutTest extends BaseInstrumentationTestCase<DrawerLayoutActivity> {
    private DrawerLayout mDrawerLayout;

    public DrawerLayoutTest() {
        super(DrawerLayoutActivity.class);
    }

    public void setUp() throws Exception {
        super.setUp();

        final DrawerLayoutActivity activity = getActivity();
        mDrawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);

        // Close the drawer to reset the state for the next test
        onView(withId(R.id.drawer_layout)).perform(
                DrawerLayoutActions.closeDrawer(GravityCompat.START));
    }

    @Test
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
}
