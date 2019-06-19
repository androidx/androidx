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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.testutils.PollingCheck;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SwipeRefreshLayoutInHorizontallyScrollingParentTest {

    @Rule
    public final ActivityTestRule<SwipeRefreshLayoutInRecyclerViewActivity> mActivityTestRule =
            new ActivityTestRule<>(SwipeRefreshLayoutInRecyclerViewActivity.class);

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private int mTouchSlop;
    private int mGestureDistance;

    private int mRecordedRvPosition;
    private int mRecordedRvOffset;

    @Nullable
    private SwipeRefreshLayout getSwipeRefreshLayout() {
        return mActivityTestRule.getActivity().mSwipeRefreshLayout;
    }

    @Before
    public void setUp() throws Throwable {
        mRecyclerView = mActivityTestRule.getActivity().mRecyclerView;
        mLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        mTouchSlop = ViewConfiguration.get(mRecyclerView.getContext()).getScaledTouchSlop();

        mActivityTestRule.runOnUiThread(() -> mRecyclerView.scrollToPosition(1));
        waitForExecution(mActivityTestRule, 2);

        mSwipeRefreshLayout = getSwipeRefreshLayout();
        assertThat(mSwipeRefreshLayout, notNullValue());
        assertThat(isIndicatorVisible(mSwipeRefreshLayout), equalTo(false));

        mGestureDistance = mSwipeRefreshLayout.getProgressViewEndOffset()
                - mSwipeRefreshLayout.getProgressViewStartOffset();
        assertThat(mGestureDistance, greaterThanOrEqualTo(2));
    }

    @Test
    public void swipeHorizontallyDuringRefreshGesture() {
        recordRvPosition();
        swipeVerticallyThenHorizontally(mGestureDistance, mTouchSlop * 3);

        // Gesture wasn't completed, so SRL should not be refreshing
        assertThat(mSwipeRefreshLayout.isRefreshing(), equalTo(false));
        // And indicator should vanish shortly
        PollingCheck.waitFor(2000, () -> !isIndicatorVisible(mSwipeRefreshLayout));
        // And parent view should not have scrolled horizontally
        assertRvPositionUnchanged();
    }

    @Test
    public void swipeHorizontallyAfterRefreshGesture() {
        recordRvPosition();
        swipeVerticallyThenHorizontally(mGestureDistance * 2, mTouchSlop * 3);

        // Gesture was completed, so SRL should be refreshing
        assertThat(mSwipeRefreshLayout.isRefreshing(), equalTo(true));
        // And indicator should be visible
        assertThat(isIndicatorVisible(mSwipeRefreshLayout), equalTo(true));
        // And parent view should not have scrolled horizontally
        assertRvPositionUnchanged();
    }

    private void swipeVerticallyThenHorizontally(int dy, int dx) {
        SwipeInjector swiper = new SwipeInjector(InstrumentationRegistry.getInstrumentation());
        swiper.startDrag(CENTER, mRecyclerView);
        swiper.dragBy(0, dy, 300);
        swiper.dragBy(dx, 0, 100);
        swiper.finishDrag();
    }

    private void recordRvPosition() {
        mRecordedRvPosition = mLayoutManager.findFirstVisibleItemPosition();
        mRecordedRvOffset = getOffset(mRecordedRvPosition);
    }

    private void assertRvPositionUnchanged() {
        int position = mLayoutManager.findFirstVisibleItemPosition();
        int offset = getOffset(position);
        assertThat(position, equalTo(mRecordedRvPosition));
        assertThat(offset, equalTo(mRecordedRvOffset));
    }

    private int getOffset(int position) {
        View view = mLayoutManager.findViewByPosition(position);
        assertThat(view, notNullValue());
        return view.getLeft();
    }

    private boolean isIndicatorVisible(SwipeRefreshLayout srl) {
        CircleImageView indicator = srl.mCircleView;
        if (indicator.getVisibility() == View.GONE) {
            return false;
        }
        // If scaled to less then 1/100th pixel, consider it not visible
        return !(Math.abs(indicator.getScaleX() * indicator.getWidth()) < 0.01
                || Math.abs(indicator.getScaleY() * indicator.getHeight()) < 0.01);
    }
}
