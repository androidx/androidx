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
package android.support.design.widget;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.core.AllOf.allOf;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.design.testutils.TestUtilsMatchers.*;

import android.content.res.Resources;
import android.support.design.test.R;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.Menu;
import android.view.MenuItem;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class BottomNavigationViewTest
        extends BaseInstrumentationTestCase<BottomNavigationViewActivity> {
    private static final int[] MENU_CONTENT_ITEM_IDS = { R.id.destination_home,
            R.id.destination_profile, R.id.destination_people };
    private Map<Integer, String> mMenuStringContent;

    private BottomNavigationView mBottomNavigation;

    public BottomNavigationViewTest() {
        super(BottomNavigationViewActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        final BottomNavigationViewActivity activity = mActivityTestRule.getActivity();
        mBottomNavigation = (BottomNavigationView) activity.findViewById(R.id.bottom_navigation);

        final Resources res = activity.getResources();
        mMenuStringContent = new HashMap<>(MENU_CONTENT_ITEM_IDS.length);
        mMenuStringContent.put(R.id.destination_home, res.getString(R.string.navigate_home));
        mMenuStringContent.put(R.id.destination_profile, res.getString(R.string.navigate_profile));
        mMenuStringContent.put(R.id.destination_people, res.getString(R.string.navigate_people));
    }

    @Test
    @SmallTest
    public void testBasics() {
        // Check the contents of the Menu object
        final Menu menu = mBottomNavigation.getMenu();
        assertNotNull("Menu should not be null", menu);
        assertEquals("Should have matching number of items", MENU_CONTENT_ITEM_IDS.length,
                menu.size());
        for (int i = 0; i < MENU_CONTENT_ITEM_IDS.length; i++) {
            final MenuItem currItem = menu.getItem(i);
            assertEquals("ID for Item #" + i, MENU_CONTENT_ITEM_IDS[i], currItem.getItemId());
        }

    }

    @Test
    @SmallTest
    public void testNavigationSelectionListener() {
        BottomNavigationView.OnNavigationItemSelectedListener mockedListener =
                mock(BottomNavigationView.OnNavigationItemSelectedListener.class);
        mBottomNavigation.setOnNavigationItemSelectedListener(mockedListener);

        // Click one of our items
        onView(allOf(withText(mMenuStringContent.get(R.id.destination_profile)),
                isDescendantOfA(withId(R.id.bottom_navigation)))).perform(click());
        // And that our listener has been notified of the click
        verify(mockedListener, times(1)).onNavigationItemSelected(
                mBottomNavigation.getMenu().findItem(R.id.destination_profile));

        // Set null listener to test that the next click is not going to notify the
        // previously set listener
        mBottomNavigation.setOnNavigationItemSelectedListener(null);

        // Click one of our items
        onView(allOf(withText(mMenuStringContent.get(R.id.destination_people)),
                isDescendantOfA(withId(R.id.bottom_navigation)))).perform(click());
        // And that our previous listener has not been notified of the click
        verifyNoMoreInteractions(mockedListener);
    }
}
