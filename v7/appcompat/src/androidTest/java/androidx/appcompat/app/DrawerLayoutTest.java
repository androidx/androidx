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
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static androidx.appcompat.testutils.DrawerLayoutActions.closeDrawer;
import static androidx.appcompat.testutils.DrawerLayoutActions.openDrawer;
import static androidx.appcompat.testutils.DrawerLayoutActions.setDrawerLockMode;
import static androidx.appcompat.testutils.DrawerLayoutActions.wrap;
import static androidx.appcompat.testutils.TestUtilsMatchers.inAscendingOrder;
import static androidx.appcompat.testutils.TestUtilsMatchers.inDescendingOrder;
import static androidx.appcompat.testutils.TestUtilsMatchers.inRange;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.os.Build;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.GeneralSwipeAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Swipe;
import android.support.test.filters.FlakyTest;
import android.support.test.filters.LargeTest;
import android.support.test.filters.MediumTest;
import android.support.test.filters.Suppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import androidx.appcompat.custom.CustomDrawerLayout;
import androidx.appcompat.test.R;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

@RunWith(AndroidJUnit4.class)
public class DrawerLayoutTest {
    @Rule
    public final ActivityTestRule<DrawerLayoutActivity> mActivityTestRule =
            new ActivityTestRule<DrawerLayoutActivity>(DrawerLayoutActivity.class);

    private CustomDrawerLayout mDrawerLayout;

    private View mStartDrawer;

    private View mContentView;

    @Before
    public void setUp() {
        final DrawerLayoutActivity activity = mActivityTestRule.getActivity();
        mDrawerLayout = (CustomDrawerLayout) activity.findViewById(R.id.drawer_layout);
        mStartDrawer = mDrawerLayout.findViewById(R.id.start_drawer);
        mContentView = mDrawerLayout.findViewById(R.id.content);

        // Close the drawer to reset the state for the next test
        onView(withId(R.id.drawer_layout)).perform(closeDrawer(GravityCompat.START));
    }

    // Tests for opening and closing the drawer and checking the open state

    @Test
    @LargeTest
    public void testDrawerOpenCloseViaAPI() {
        assertFalse("Initial state", mDrawerLayout.isDrawerOpen(GravityCompat.START));

        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.drawer_layout)).perform(openDrawer(GravityCompat.START));
            assertTrue("Opened drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            onView(withId(R.id.drawer_layout)).perform(closeDrawer(GravityCompat.START));
            assertFalse("Closed drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));
        }
    }

    @Test
    @MediumTest
    public void testDrawerOpenCloseNoAnimationViaAPI() {
        assertFalse("Initial state", mDrawerLayout.isDrawerOpen(GravityCompat.START));

        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.drawer_layout)).perform(openDrawer(GravityCompat.START, false));
            assertTrue("Opened drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            onView(withId(R.id.drawer_layout)).perform(closeDrawer(GravityCompat.START, false));
            assertFalse("Closed drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));
        }
    }

    @Test
    @LargeTest
    public void testDrawerOpenCloseFocus() throws Throwable {
        assertFalse("Initial state", mDrawerLayout.isDrawerOpen(GravityCompat.START));

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mContentView.setFocusableInTouchMode(true);
                mContentView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        fail("Unnecessary focus change");
                    }
                });
            }
        });

        onView(withId(R.id.drawer_layout)).perform(openDrawer(GravityCompat.START));
        assertTrue("Opened drawer", mDrawerLayout.isDrawerOpen(GravityCompat.START));

        onView(withId(R.id.drawer_layout)).perform(closeDrawer(GravityCompat.START));
        assertFalse("Closed drawer", mDrawerLayout.isDrawerOpen(GravityCompat.START));
    }

    @Test
    @LargeTest
    public void testDrawerOpenCloseWithRedundancyViaAPI() {
        assertFalse("Initial state", mDrawerLayout.isDrawerOpen(GravityCompat.START));

        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.drawer_layout)).perform(openDrawer(GravityCompat.START));
            assertTrue("Opened drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            // Try opening the drawer when it's already opened
            onView(withId(R.id.drawer_layout)).perform(openDrawer(GravityCompat.START));
            assertTrue("Opened drawer is still opened #" + i,
                    mDrawerLayout.isDrawerOpen(GravityCompat.START));

            onView(withId(R.id.drawer_layout)).perform(closeDrawer(GravityCompat.START));
            assertFalse("Closed drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            // Try closing the drawer when it's already closed
            onView(withId(R.id.drawer_layout)).perform(closeDrawer(GravityCompat.START));
            assertFalse("Closed drawer is still closed #" + i,
                    mDrawerLayout.isDrawerOpen(GravityCompat.START));
        }
    }

    @Test
    @MediumTest
    public void testDrawerOpenCloseNoAnimationWithRedundancyViaAPI() {
        assertFalse("Initial state", mDrawerLayout.isDrawerOpen(GravityCompat.START));

        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.drawer_layout)).perform(openDrawer(GravityCompat.START, false));
            assertTrue("Opened drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            // Try opening the drawer when it's already opened
            onView(withId(R.id.drawer_layout)).perform(openDrawer(GravityCompat.START, false));
            assertTrue("Opened drawer is still opened #" + i,
                    mDrawerLayout.isDrawerOpen(GravityCompat.START));

            onView(withId(R.id.drawer_layout)).perform(closeDrawer(GravityCompat.START, false));
            assertFalse("Closed drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            // Try closing the drawer when it's already closed
            onView(withId(R.id.drawer_layout)).perform(closeDrawer(GravityCompat.START, false));
            assertFalse("Closed drawer is still closed #" + i,
                    mDrawerLayout.isDrawerOpen(GravityCompat.START));
        }
    }

    @Test
    @LargeTest
    public void testDrawerOpenCloseViaSwipes() {
        assertFalse("Initial state", mDrawerLayout.isDrawerOpen(GravityCompat.START));

        // Note that we're using GeneralSwipeAction instead of swipeLeft() / swipeRight().
        // Those Espresso actions use edge fuzzying which doesn't work well with edge-based
        // detection of swiping the drawers open in DrawerLayout.
        // It's critically important to wrap the GeneralSwipeAction to "wait" until the
        // DrawerLayout has settled to STATE_IDLE state before continuing to query the drawer
        // open / close state. This is done in DrawerLayoutActions.wrap method.
        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.drawer_layout)).perform(
                    wrap(new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_LEFT,
                            GeneralLocation.CENTER_RIGHT, Press.FINGER)));
            assertTrue("Opened drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            onView(withId(R.id.drawer_layout)).perform(
                    wrap(new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_RIGHT,
                            GeneralLocation.CENTER_LEFT, Press.FINGER)));
            assertFalse("Closed drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));
        }
    }

    @Test
    @LargeTest
    public void testDrawerOpenCloseWithRedundancyViaSwipes() {
        assertFalse("Initial state", mDrawerLayout.isDrawerOpen(GravityCompat.START));

        // Note that we're using GeneralSwipeAction instead of swipeLeft() / swipeRight().
        // Those Espresso actions use edge fuzzying which doesn't work well with edge-based
        // detection of swiping the drawers open in DrawerLayout.
        // It's critically important to wrap the GeneralSwipeAction to "wait" until the
        // DrawerLayout has settled to STATE_IDLE state before continuing to query the drawer
        // open / close state. This is done in DrawerLayoutActions.wrap method.
        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.drawer_layout)).perform(
                    wrap(new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_LEFT,
                            GeneralLocation.CENTER_RIGHT, Press.FINGER)));
            assertTrue("Opened drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            // Try opening the drawer when it's already opened
            onView(withId(R.id.drawer_layout)).perform(
                    wrap(new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_LEFT,
                            GeneralLocation.CENTER_RIGHT, Press.FINGER)));
            assertTrue("Opened drawer is still opened #" + i,
                    mDrawerLayout.isDrawerOpen(GravityCompat.START));

            onView(withId(R.id.drawer_layout)).perform(
                    wrap(new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_RIGHT,
                            GeneralLocation.CENTER_LEFT, Press.FINGER)));
            assertFalse("Closed drawer #" + i, mDrawerLayout.isDrawerOpen(GravityCompat.START));

            // Try closing the drawer when it's already closed
            onView(withId(R.id.drawer_layout)).perform(
                    wrap(new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_RIGHT,
                            GeneralLocation.CENTER_LEFT, Press.FINGER)));
            assertFalse("Closed drawer is still closed #" + i,
                    mDrawerLayout.isDrawerOpen(GravityCompat.START));
        }
    }

    @Test
    @MediumTest
    public void testDrawerHeight() {
        // Open the drawer so it becomes visible
        onView(withId(R.id.drawer_layout)).perform(openDrawer(GravityCompat.START));

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

            final int[] contentViewLocationOnScreen = new int[2];
            mContentView.getLocationOnScreen(contentViewLocationOnScreen);
            final int statusBarHeight = contentViewLocationOnScreen[1];
            // Get the system window top inset that was propagated to the top-level DrawerLayout
            // during its layout.
            int drawerTopInset = mDrawerLayout.getSystemWindowInsetTop();
            if (statusBarHeight > 0) {
                assertEquals("Drawer top inset is positive on L+", statusBarHeight, drawerTopInset);
            } else {
                assertEquals("Drawer top inset 0 due to no status bar", 0, drawerTopInset);
            }
            assertEquals("Drawer layout and drawer heights on L+",
                    drawerLayoutHeight - drawerTopInset, contentHeight);
        }
    }

    // Tests for listener(s) being notified of various events

    @Test
    @MediumTest
    public void testDrawerListenerCallbacksOnOpeningViaAPI() {
        // Register a mock listener
        DrawerLayout.DrawerListener mockedListener = mock(DrawerLayout.DrawerListener.class);
        mDrawerLayout.addDrawerListener(mockedListener);

        // Open the drawer so it becomes visible
        onView(withId(R.id.drawer_layout)).perform(openDrawer(GravityCompat.START));

        // We expect that our listener has been notified that the drawer has been opened
        // with the reference to our drawer
        verify(mockedListener, times(1)).onDrawerOpened(mStartDrawer);
        // We expect that our listener has not been notified that the drawer has been closed
        verify(mockedListener, never()).onDrawerClosed(any(View.class));

        // We expect that our listener has been notified at least once on the drawer slide
        // event. We expect that all such callbacks pass the reference to our drawer as the first
        // parameter, and we capture the float slide values for further analysis
        ArgumentCaptor<Float> floatSlideCaptor = ArgumentCaptor.forClass(float.class);
        verify(mockedListener, atLeastOnce()).onDrawerSlide(eq(mStartDrawer),
                floatSlideCaptor.capture());
        // Now we verify that calls to onDrawerSlide "gave" us an increasing sequence of values
        // in [0..1] range. Note that we don't have any expectation on how many times onDrawerSlide
        // is called since that depends on the hardware capabilities of the device and the current
        // load on the CPU / GPU.
        assertThat(floatSlideCaptor.getAllValues(), inRange(0.0f, 1.0f));
        assertThat(floatSlideCaptor.getAllValues(), inAscendingOrder());

        // We expect that our listener will be called with specific state changes
        InOrder inOrder = inOrder(mockedListener);
        inOrder.verify(mockedListener).onDrawerStateChanged(DrawerLayout.STATE_SETTLING);
        inOrder.verify(mockedListener).onDrawerStateChanged(DrawerLayout.STATE_IDLE);

        mDrawerLayout.removeDrawerListener(mockedListener);
    }

    @Test
    @MediumTest
    public void testDrawerListenerCallbacksOnOpeningNoAnimationViaAPI() {
        // Register a mock listener
        DrawerLayout.DrawerListener mockedListener = mock(DrawerLayout.DrawerListener.class);
        mDrawerLayout.addDrawerListener(mockedListener);

        // Open the drawer so it becomes visible
        onView(withId(R.id.drawer_layout)).perform(openDrawer(GravityCompat.START, false));

        // We expect that our listener has been notified that the drawer has been opened
        // with the reference to our drawer
        verify(mockedListener, times(1)).onDrawerOpened(mStartDrawer);
        // We expect that our listener has not been notified that the drawer has been closed
        verify(mockedListener, never()).onDrawerClosed(any(View.class));

        verify(mockedListener, times(1)).onDrawerSlide(any(View.class), eq(1f));

        // Request to open the drawer again
        onView(withId(R.id.drawer_layout)).perform(openDrawer(GravityCompat.START, false));

        // We expect that our listener has not been notified again that the drawer has been opened
        verify(mockedListener, times(1)).onDrawerOpened(mStartDrawer);
        // We expect that our listener has not been notified that the drawer has been closed
        verify(mockedListener, never()).onDrawerClosed(any(View.class));

        mDrawerLayout.removeDrawerListener(mockedListener);
    }

    @Test
    @LargeTest
    public void testDrawerListenerCallbacksOnClosingViaAPI() {
        // Open the drawer so it becomes visible
        onView(withId(R.id.drawer_layout)).perform(openDrawer(GravityCompat.START));

        // Register a mock listener
        DrawerLayout.DrawerListener mockedListener = mock(DrawerLayout.DrawerListener.class);
        mDrawerLayout.addDrawerListener(mockedListener);

        // Close the drawer
        onView(withId(R.id.drawer_layout)).perform(closeDrawer(GravityCompat.START));

        // We expect that our listener has not been notified that the drawer has been opened
        verify(mockedListener, never()).onDrawerOpened(any(View.class));
        // We expect that our listener has been notified that the drawer has been closed
        // with the reference to our drawer
        verify(mockedListener, times(1)).onDrawerClosed(mStartDrawer);

        // We expect that our listener has been notified at least once on the drawer slide
        // event. We expect that all such callbacks pass the reference to our drawer as the first
        // parameter, and we capture the float slide values for further analysis
        ArgumentCaptor<Float> floatSlideCaptor = ArgumentCaptor.forClass(float.class);
        verify(mockedListener, atLeastOnce()).onDrawerSlide(eq(mStartDrawer),
                floatSlideCaptor.capture());
        // Now we verify that calls to onDrawerSlide "gave" us a decreasing sequence of values
        // in [0..1] range. Note that we don't have any expectation on how many times onDrawerSlide
        // is called since that depends on the hardware capabilities of the device and the current
        // load on the CPU / GPU.
        assertThat(floatSlideCaptor.getAllValues(), inRange(0.0f, 1.0f));
        assertThat(floatSlideCaptor.getAllValues(), inDescendingOrder());

        // We expect that our listener will be called with specific state changes
        InOrder inOrder = inOrder(mockedListener);
        inOrder.verify(mockedListener).onDrawerStateChanged(DrawerLayout.STATE_SETTLING);
        inOrder.verify(mockedListener).onDrawerStateChanged(DrawerLayout.STATE_IDLE);

        mDrawerLayout.removeDrawerListener(mockedListener);
    }

    @Test
    @MediumTest
    public void testDrawerListenerCallbacksOnClosingNoAnimationViaAPI() {
        // Open the drawer so it becomes visible
        onView(withId(R.id.drawer_layout)).perform(openDrawer(GravityCompat.START, false));

        // Register a mock listener
        DrawerLayout.DrawerListener mockedListener = mock(DrawerLayout.DrawerListener.class);
        mDrawerLayout.addDrawerListener(mockedListener);

        // Close the drawer
        onView(withId(R.id.drawer_layout)).perform(closeDrawer(GravityCompat.START, false));

        // We expect that our listener has not been notified that the drawer has been opened
        verify(mockedListener, never()).onDrawerOpened(any(View.class));
        // We expect that our listener has been notified that the drawer has been closed
        // with the reference to our drawer
        verify(mockedListener, times(1)).onDrawerClosed(mStartDrawer);

        verify(mockedListener, times(1)).onDrawerSlide(any(View.class), eq(0f));

        // Attempt to close the drawer again.
        onView(withId(R.id.drawer_layout)).perform(closeDrawer(GravityCompat.START, false));

        // We expect that our listener has not been notified that the drawer has been opened
        verify(mockedListener, never()).onDrawerOpened(any(View.class));
        // We expect that our listener has not been notified again that the drawer has been closed
        verify(mockedListener, times(1)).onDrawerClosed(mStartDrawer);

        mDrawerLayout.removeDrawerListener(mockedListener);
    }

    @Suppress
    @FlakyTest(bugId = 33659300)
    @Test
    @MediumTest
    public void testDrawerListenerCallbacksOnOpeningViaSwipes() {
        // Register a mock listener
        DrawerLayout.DrawerListener mockedListener = mock(DrawerLayout.DrawerListener.class);
        mDrawerLayout.addDrawerListener(mockedListener);

        // Open the drawer so it becomes visible
        // Note that we're using GeneralSwipeAction instead of swipeLeft() / swipeRight().
        // Those Espresso actions use edge fuzzying which doesn't work well with edge-based
        // detection of swiping the drawers open in DrawerLayout.
        // It's critically important to wrap the GeneralSwipeAction to "wait" until the
        // DrawerLayout has settled to STATE_IDLE state before continuing to query the drawer
        // open / close state. This is done in DrawerLayoutActions.wrap method.
        onView(withId(R.id.drawer_layout)).perform(
                wrap(new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_LEFT,
                        GeneralLocation.CENTER_RIGHT, Press.FINGER)));

        // We expect that our listener has been notified that the drawer has been opened
        // with the reference to our drawer
        verify(mockedListener, times(1)).onDrawerOpened(mStartDrawer);
        // We expect that our listener has not been notified that the drawer has been closed
        verify(mockedListener, never()).onDrawerClosed(any(View.class));

        // We expect that our listener has been notified at least once on the drawer slide
        // event. We expect that all such callbacks pass the reference to our drawer as the first
        // parameter, and we capture the float slide values for further analysis
        ArgumentCaptor<Float> floatSlideCaptor = ArgumentCaptor.forClass(float.class);
        verify(mockedListener, atLeastOnce()).onDrawerSlide(eq(mStartDrawer),
                floatSlideCaptor.capture());
        // Now we verify that calls to onDrawerSlide "gave" us an increasing sequence of values
        // in [0..1] range. Note that we don't have any expectation on how many times onDrawerSlide
        // is called since that depends on the hardware capabilities of the device and the current
        // load on the CPU / GPU.
        assertThat(floatSlideCaptor.getAllValues(), inRange(0.0f, 1.0f));
        assertThat(floatSlideCaptor.getAllValues(), inAscendingOrder());

        // We expect that our listener will be called with specific state changes
        InOrder inOrder = inOrder(mockedListener);
        inOrder.verify(mockedListener).onDrawerStateChanged(DrawerLayout.STATE_DRAGGING);
        inOrder.verify(mockedListener).onDrawerStateChanged(DrawerLayout.STATE_IDLE);

        mDrawerLayout.removeDrawerListener(mockedListener);
    }

    @Test
    @LargeTest
    public void testDrawerListenerCallbacksOnClosingViaSwipes() {
        // Open the drawer so it becomes visible
        onView(withId(R.id.drawer_layout)).perform(openDrawer(GravityCompat.START));

        // Register a mock listener
        DrawerLayout.DrawerListener mockedListener = mock(DrawerLayout.DrawerListener.class);
        mDrawerLayout.addDrawerListener(mockedListener);

        // Close the drawer
        // Note that we're using GeneralSwipeAction instead of swipeLeft() / swipeRight().
        // Those Espresso actions use edge fuzzying which doesn't work well with edge-based
        // detection of swiping the drawers open in DrawerLayout.
        // It's critically important to wrap the GeneralSwipeAction to "wait" until the
        // DrawerLayout has settled to STATE_IDLE state before continuing to query the drawer
        // open / close state. This is done in DrawerLayoutActions.wrap method.
        onView(withId(R.id.drawer_layout)).perform(
                wrap(new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_RIGHT,
                        GeneralLocation.CENTER_LEFT, Press.FINGER)));

        // We expect that our listener has not been notified that the drawer has been opened
        verify(mockedListener, never()).onDrawerOpened(any(View.class));
        // We expect that our listener has been notified that the drawer has been closed
        // with the reference to our drawer
        verify(mockedListener, times(1)).onDrawerClosed(mStartDrawer);

        // We expect that our listener has been notified at least once on the drawer slide
        // event. We expect that all such callbacks pass the reference to our drawer as the first
        // parameter, and we capture the float slide values for further analysis
        ArgumentCaptor<Float> floatSlideCaptor = ArgumentCaptor.forClass(float.class);
        verify(mockedListener, atLeastOnce()).onDrawerSlide(eq(mStartDrawer),
                floatSlideCaptor.capture());
        // Now we verify that calls to onDrawerSlide "gave" us a decreasing sequence of values
        // in [0..1] range. Note that we don't have any expectation on how many times onDrawerSlide
        // is called since that depends on the hardware capabilities of the device and the current
        // load on the CPU / GPU.
        assertThat(floatSlideCaptor.getAllValues(), inRange(0.0f, 1.0f));
        assertThat(floatSlideCaptor.getAllValues(), inDescendingOrder());

        // We expect that our listener will be called with specific state changes
        InOrder inOrder = inOrder(mockedListener);
        inOrder.verify(mockedListener).onDrawerStateChanged(DrawerLayout.STATE_DRAGGING);
        inOrder.verify(mockedListener).onDrawerStateChanged(DrawerLayout.STATE_IDLE);

        mDrawerLayout.removeDrawerListener(mockedListener);
    }

    @Test
    @LargeTest
    public void testDrawerLockUnlock() {
        assertEquals("Drawer is unlocked in initial state",
                DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLayout.getDrawerLockMode(mStartDrawer));

        // Lock the drawer open
        onView(withId(R.id.drawer_layout)).perform(
                setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, GravityCompat.START));
        // Check that it's locked open
        assertEquals("Drawer is now locked open",
                DrawerLayout.LOCK_MODE_LOCKED_OPEN, mDrawerLayout.getDrawerLockMode(mStartDrawer));
        // and also opened
        assertTrue("Drawer is also opened", mDrawerLayout.isDrawerOpen(mStartDrawer));

        // Unlock the drawer
        onView(withId(R.id.drawer_layout)).perform(
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mStartDrawer));
        // Check that it's still opened
        assertTrue("Drawer is still opened", mDrawerLayout.isDrawerOpen(mStartDrawer));
        // Close the drawer
        onView(withId(R.id.drawer_layout)).perform(closeDrawer(mStartDrawer));
        // Check that the drawer is unlocked
        assertEquals("Start drawer is now unlocked",
                DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLayout.getDrawerLockMode(mStartDrawer));

        // Open the drawer and then clock it closed
        onView(withId(R.id.drawer_layout)).perform(openDrawer(mStartDrawer));
        onView(withId(R.id.drawer_layout)).perform(
                setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START));
        // Check that the drawer is locked close
        assertEquals("Drawer is now locked close",
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                mDrawerLayout.getDrawerLockMode(mStartDrawer));
        // and also closed
        assertFalse("Drawer is also closed", mDrawerLayout.isDrawerOpen(mStartDrawer));

        // Unlock the drawer
        onView(withId(R.id.drawer_layout)).perform(
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mStartDrawer));
        // Check that it's still closed
        assertFalse("Drawer is still closed", mDrawerLayout.isDrawerOpen(mStartDrawer));
    }
}
