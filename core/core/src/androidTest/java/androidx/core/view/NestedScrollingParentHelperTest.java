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
package androidx.core.view;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;

import static org.hamcrest.CoreMatchers.is;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NestedScrollingParentHelperTest {

    private NestedScrollingParentHelper mNestedScrollingParentHelper;
    private View mView;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(new FrameLayout(context));
        mView = new View(context);
    }

    @Test
    public void getNestedScrollAxes_didNotAccept_returnsNone() {
        assertThat(mNestedScrollingParentHelper.getNestedScrollAxes(),
                is(ViewCompat.SCROLL_AXIS_NONE));
    }

    @Test
    public void getNestedScrollAxes_acceptedTouch_returnsTouch() {
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        assertThat(mNestedScrollingParentHelper.getNestedScrollAxes(),
                is(ViewCompat.SCROLL_AXIS_VERTICAL));
    }

    @Test
    public void getNestedScrollAxes_acceptedNonTouch_returnsNonTouch() {
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_HORIZONTAL, ViewCompat.TYPE_NON_TOUCH);
        assertThat(mNestedScrollingParentHelper.getNestedScrollAxes(),
                is(ViewCompat.SCROLL_AXIS_HORIZONTAL));
    }

    @Test
    public void getNestedScrollAxes_acceptedTouchThenStopsTouch_returnsNone() {
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);

        mNestedScrollingParentHelper.onStopNestedScroll(mView, ViewCompat.TYPE_TOUCH);

        assertThat(mNestedScrollingParentHelper.getNestedScrollAxes(),
                is(ViewCompat.SCROLL_AXIS_NONE));
    }

    @Test
    public void getNestedScrollAxes_acceptedNonTouchThenStopsNonTouch_returnsNone() {
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_HORIZONTAL, ViewCompat.TYPE_NON_TOUCH);

        mNestedScrollingParentHelper.onStopNestedScroll(mView, ViewCompat.TYPE_NON_TOUCH);

        assertThat(mNestedScrollingParentHelper.getNestedScrollAxes(),
                is(ViewCompat.SCROLL_AXIS_NONE));
    }

    @Test
    public void getNestedScrollAxes_acceptedViaV1_returnsTouch() {
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_VERTICAL);
        assertThat(mNestedScrollingParentHelper.getNestedScrollAxes(),
                is(ViewCompat.SCROLL_AXIS_VERTICAL));
    }

    @Test
    public void getNestedScrollAxes_acceptedBoth_returnsDisjunction() {
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_HORIZONTAL, ViewCompat.TYPE_NON_TOUCH);

        assertThat(mNestedScrollingParentHelper.getNestedScrollAxes(),
                is(ViewCompat.SCROLL_AXIS_VERTICAL | ViewCompat.SCROLL_AXIS_HORIZONTAL));
    }

    @Test
    public void getNestedScrollAxes_acceptBothThenStopTouch_returnsNonTouch() {
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_HORIZONTAL, ViewCompat.TYPE_NON_TOUCH);

        mNestedScrollingParentHelper.onStopNestedScroll(mView, ViewCompat.TYPE_TOUCH);

        assertThat(mNestedScrollingParentHelper.getNestedScrollAxes(),
                is(ViewCompat.SCROLL_AXIS_HORIZONTAL));
    }

    @Test
    public void getNestedScrollAxes_acceptBothThenStopNonTouch_returnsTouch() {
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_HORIZONTAL, ViewCompat.TYPE_NON_TOUCH);

        mNestedScrollingParentHelper.onStopNestedScroll(mView, ViewCompat.TYPE_NON_TOUCH);

        assertThat(mNestedScrollingParentHelper.getNestedScrollAxes(),
                is(ViewCompat.SCROLL_AXIS_VERTICAL));
    }

    @Test
    public void getNestedScrollAxes_acceptBothThenStopV1_returnsNonTouch() {
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_HORIZONTAL, ViewCompat.TYPE_NON_TOUCH);

        mNestedScrollingParentHelper.onStopNestedScroll(mView);

        assertThat(mNestedScrollingParentHelper.getNestedScrollAxes(),
                is(ViewCompat.SCROLL_AXIS_HORIZONTAL));
    }

    @Test
    public void getNestedScrollAxes_acceptBothThenStopBoth_returnsNone() {
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        mNestedScrollingParentHelper.onNestedScrollAccepted(
                mView, mView, ViewCompat.SCROLL_AXIS_HORIZONTAL, ViewCompat.TYPE_NON_TOUCH);

        mNestedScrollingParentHelper.onStopNestedScroll(mView, ViewCompat.TYPE_TOUCH);
        mNestedScrollingParentHelper.onStopNestedScroll(mView, ViewCompat.TYPE_NON_TOUCH);

        assertThat(mNestedScrollingParentHelper.getNestedScrollAxes(),
                is(ViewCompat.SCROLL_AXIS_NONE));
    }
}
