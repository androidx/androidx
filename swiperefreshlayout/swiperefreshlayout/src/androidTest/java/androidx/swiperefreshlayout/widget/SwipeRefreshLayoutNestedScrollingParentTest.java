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

package androidx.swiperefreshlayout.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Small integration tests that verifies correctness of {@link SwipeRefreshLayout}'s
 * NestedScrollingChild implementation.
 *
 * This test is not complete, and has only been added to as changes have been made that may have
 * affected NestedScrollingChild implementation behavior.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SwipeRefreshLayoutNestedScrollingParentTest {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private View mChild;

    @Before
    public void instantiateMembers() {
        mSwipeRefreshLayout = new SwipeRefreshLayout(ApplicationProvider.getApplicationContext());
        mChild = new View(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void onStartNestedScroll_scrollAxisIncludesVerticalAndTypeTouch_returnsTrue() {
        int vertical = ViewCompat.SCROLL_AXIS_VERTICAL;
        int both = ViewCompat.SCROLL_AXIS_VERTICAL | ViewCompat.SCROLL_AXIS_HORIZONTAL;
        onStartNestedScroll(vertical, true);
        onStartNestedScroll(both, true);
    }

    @Test
    public void onStartNestedScroll_typeIsNotTouch_returnsFalse() {
        int vertical = ViewCompat.SCROLL_AXIS_VERTICAL;
        int both = ViewCompat.SCROLL_AXIS_VERTICAL | ViewCompat.SCROLL_AXIS_HORIZONTAL;
        onStartNestedScroll(vertical, true);
        onStartNestedScroll(both, true);
    }

    @Test
    public void onStartNestedScroll_scrollAxisExcludesVertical_returnsFalse() {
        int horizontal = ViewCompat.SCROLL_AXIS_HORIZONTAL;
        int neither = ViewCompat.SCROLL_AXIS_NONE;
        onStartNestedScroll(horizontal, false);
        onStartNestedScroll(neither, false);
    }

    private void onStartNestedScroll(int iScrollAxis, boolean oRetValue) {
        boolean retVal = mSwipeRefreshLayout.onStartNestedScroll(mChild, mChild, iScrollAxis);
        assertThat(retVal, is(oRetValue));
    }
}
