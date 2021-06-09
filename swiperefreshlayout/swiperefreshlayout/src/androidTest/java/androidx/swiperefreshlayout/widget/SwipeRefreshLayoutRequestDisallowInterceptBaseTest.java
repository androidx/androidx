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

package androidx.swiperefreshlayout.widget;

import static androidx.test.espresso.action.GeneralLocation.CENTER;
import static androidx.testutils.ActivityTestRuleKt.waitForExecution;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import android.view.ViewConfiguration;

import androidx.test.filters.FlakyTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.testutils.SwipeInjector;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public abstract class SwipeRefreshLayoutRequestDisallowInterceptBaseTest {

    @SuppressWarnings("deprecation")
    @Rule
    public final androidx.test.rule
            .ActivityTestRule<? extends SwipeRefreshLayoutInRecyclerViewBaseActivity>
            mActivityTestRule = new androidx.test.rule.ActivityTestRule<>(getActivityClass());

    private RequestDisallowInterceptRecordingRecyclerView mRecyclerView;
    private int mTouchSlop;

    protected abstract
            Class<? extends SwipeRefreshLayoutInRecyclerViewBaseActivity> getActivityClass();

    @Before
    public void setUp() throws Throwable {
        mRecyclerView = mActivityTestRule.getActivity().mRecyclerView;
        mTouchSlop = ViewConfiguration.get(mRecyclerView.getContext()).getScaledTouchSlop();

        mActivityTestRule.runOnUiThread(() -> mRecyclerView.scrollToPosition(1));
        waitForExecution(mActivityTestRule, 2);
    }

    @Test
    public void swipeLessThanTouchSlop_requestDisallowNotCalled() {
        assertThat(mRecyclerView.mRequestDisallowInterceptTrueCalled, equalTo(false));
        assertThat(mRecyclerView.mRequestDisallowInterceptFalseCalled, equalTo(false));

        swipeDown(mTouchSlop / 2);

        assertThat(mRecyclerView.mRequestDisallowInterceptTrueCalled, equalTo(false));
        assertThat(mRecyclerView.mRequestDisallowInterceptFalseCalled, equalTo(false));
    }

    @FlakyTest(bugId = 190613223)
    @Test
    public void swipeMoreThanTouchSlop_requestDisallowIsCalled() {
        assertThat(mRecyclerView.mRequestDisallowInterceptTrueCalled, equalTo(false));
        assertThat(mRecyclerView.mRequestDisallowInterceptFalseCalled, equalTo(false));

        swipeDown(mTouchSlop + 1);

        assertThat(mRecyclerView.mRequestDisallowInterceptTrueCalled, equalTo(true));
        assertThat(mRecyclerView.mRequestDisallowInterceptFalseCalled, equalTo(false));
    }

    private void swipeDown(int dy) {
        SwipeInjector swiper = new SwipeInjector(InstrumentationRegistry.getInstrumentation());
        swiper.startDrag(CENTER, mRecyclerView);
        swiper.dragBy(0, dy);
        swiper.finishDrag();
    }
}
