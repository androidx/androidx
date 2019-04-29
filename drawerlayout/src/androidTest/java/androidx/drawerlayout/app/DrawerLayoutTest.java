/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.drawerlayout.app;


import static androidx.drawerlayout.testutils.DrawerLayoutActions.closeDrawer;
import static androidx.drawerlayout.testutils.DrawerLayoutActions.openDrawer;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Rect;
import android.os.Build;
import android.view.View;

import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.test.R;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class DrawerLayoutTest {
    @Rule
    public final ActivityTestRule<DrawerLayoutTestActivity> mActivityTestRule =
            new ActivityTestRule<>(DrawerLayoutTestActivity.class);

    private DrawerLayout mDrawerLayout;

    private View mDrawer;

    private View mContentView;

    @Before
    public void setUp() {
        final DrawerLayoutTestActivity activity = mActivityTestRule.getActivity();
        mDrawerLayout = activity.findViewById(R.id.drawer_layout);
        mDrawer = mDrawerLayout.findViewById(R.id.drawer);
        mContentView = mDrawerLayout.findViewById(R.id.content);

        // Close the drawer to reset the state for the next test
        onView(withId(R.id.drawer_layout)).perform(closeDrawer(GravityCompat.START));
    }

    @Test
    @SmallTest
    public void testSystemGestureExclusionRects() {
        if (Build.VERSION.SDK_INT < 29) {
            // Not expected on old device
            return;
        }

        onView(withId(R.id.drawer_layout)).check(new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewFoundException) {
                final List<Rect> exclusionRects = ViewCompat.getSystemGestureExclusionRects(view);
                assertTrue("view is laid out", view.isLaidOut());
                assertFalse("exclusion rects empty", exclusionRects.isEmpty());
                for (Rect rect : exclusionRects) {
                    assertFalse("rect " + rect + " is empty", rect.isEmpty());
                    assertEquals("rect height (" + rect.height() + ") == view height ("
                            + view.getHeight() + ")", view.getHeight(), rect.height());
                }
            }
        });

        onView(withId(R.id.drawer_layout)).perform(openDrawer(mDrawer));

        assertTrue("open drawer exclusion rects empty",
                ViewCompat.getSystemGestureExclusionRects(mDrawerLayout).isEmpty());

        onView(withId(R.id.drawer_layout)).perform(closeDrawer(mDrawer));

        final List<Rect> reclosed = ViewCompat.getSystemGestureExclusionRects(mDrawerLayout);
        assertFalse("re-closed drawer has exclusion rects", reclosed.isEmpty());
        for (Rect rect : reclosed) {
            assertFalse("re-closed drawer exclusion rect should not be empty", rect.isEmpty());
            assertEquals("re-closed drawer rect correct height",
                    mDrawerLayout.getHeight(), rect.height());
        }
    }
}
