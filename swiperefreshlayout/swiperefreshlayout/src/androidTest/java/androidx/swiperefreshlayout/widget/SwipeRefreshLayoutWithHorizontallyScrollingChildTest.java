/*
 * Copyright 2020 The Android Open Source Project
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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;
import static androidx.test.espresso.action.GeneralLocation.CENTER;
import static androidx.testutils.ActivityTestRuleKt.waitForExecution;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.filters.FlakyTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.testutils.SwipeInjector;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SwipeRefreshLayoutWithHorizontallyScrollingChildTest {

    @Rule
    @SuppressWarnings("deprecation")
    public final androidx.test.rule.ActivityTestRule<ComponentActivity>
            mActivityTestRule = new androidx.test.rule.ActivityTestRule<>(ComponentActivity.class);

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;

    private int mTouchSlop;

    private int mRecordedRvPosition;
    private int mRecordedRvOffset;

    @Before
    public void setUp() throws Throwable {
        Activity activity = mActivityTestRule.getActivity();

        // Given a SwipeRefreshLayout as root element,
        mSwipeRefreshLayout = new SwipeRefreshLayout(activity);
        mSwipeRefreshLayout.setLayoutParams(matchParent());

        // With a FrameLayout as child (because it has nested scrolling disabled by default)
        ViewGroup child = new FrameLayout(activity);
        child.setLayoutParams(matchParent());
        mSwipeRefreshLayout.addView(child);

        // Which contains a horizontally scrolling RecyclerView
        mRecyclerView = new RecyclerView(activity);
        mRecyclerView.setLayoutParams(matchParent());
        mLayoutManager = new LinearLayoutManager(activity, HORIZONTAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(new RvAdapter());

        child.addView(mRecyclerView);
        mActivityTestRule.runOnUiThread(() -> activity.setContentView(mSwipeRefreshLayout));

        mTouchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();

        waitForExecution(mActivityTestRule, 1);
        assertThat(isIndicatorVisible(mSwipeRefreshLayout), equalTo(false));
    }

    @Test
    @FlakyTest(bugId = 190192628)
    public void refreshWhileScrollingRecyclerView() {
        // When we drag the RecyclerView horizontally, and then move the pointer down,
        SwipeInjector swiper = new SwipeInjector(InstrumentationRegistry.getInstrumentation());
        swiper.startDrag(CENTER, mRecyclerView);
        swiper.dragBy(-mTouchSlop - 1, 0, 50);
        swiper.dragBy(0, mTouchSlop + 10, 50);

        // The SwipeRefreshLayout shouldn't steal the gesture
        waitForExecution(mActivityTestRule, 1);
        assertThat(isIndicatorVisible(mSwipeRefreshLayout), equalTo(false));

        // And when we continue our drag in horizontal direction
        recordRvPosition();
        swiper.dragBy(-10, 0, 100);

        // Then RecyclerView continued to move
        waitForExecution(mActivityTestRule, 1);
        assertRvOffsetChanged();
    }

    @Test
    @SuppressWarnings("deprecation")
    @FlakyTest(bugId = 190192628)
    public void refreshWhileScrollingRecyclerView_legacy() {
        // If the legacy behavior is enabled
        mSwipeRefreshLayout.setLegacyRequestDisallowInterceptTouchEventEnabled(true);

        // When we drag the RecyclerView horizontally, and then move the pointer down,
        SwipeInjector swiper = new SwipeInjector(InstrumentationRegistry.getInstrumentation());
        swiper.startDrag(CENTER, mRecyclerView);
        swiper.dragBy(-mTouchSlop - 1, 0, 50);
        swiper.dragBy(0, mTouchSlop + 10, 50);

        // The SwipeRefreshLayout should steal the gesture
        waitForExecution(mActivityTestRule, 1);
        assertThat(isIndicatorVisible(mSwipeRefreshLayout), equalTo(true));

        // And when we continue our drag in horizontal direction
        recordRvPosition();
        swiper.dragBy(-10, 0, 100);

        // Then RecyclerView didn't move anymore
        waitForExecution(mActivityTestRule, 1);
        assertRvOffsetUnchanged();
    }

    private void recordRvPosition() {
        mRecordedRvPosition = mLayoutManager.findFirstVisibleItemPosition();
        mRecordedRvOffset = getOffset(mRecordedRvPosition);
    }

    private void assertRvOffsetChanged() {
        int position = mLayoutManager.findFirstVisibleItemPosition();
        int offset = getOffset(position);
        if (position == mRecordedRvPosition) {
            assertThat(offset, not(equalTo(mRecordedRvOffset)));
        }
    }

    private void assertRvOffsetUnchanged() {
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

    private static boolean isIndicatorVisible(SwipeRefreshLayout srl) {
        CircleImageView indicator = srl.mCircleView;
        if (indicator.getVisibility() == View.GONE) {
            return false;
        }
        // If scaled to less than 1/100th pixel, consider it not visible
        return !(Math.abs(indicator.getScaleX() * indicator.getWidth()) < 0.01
                || Math.abs(indicator.getScaleY() * indicator.getHeight()) < 0.01);
    }

    private static ViewGroup.LayoutParams matchParent() {
        return new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
    }

    private static class RvAdapter extends RecyclerView.Adapter<ViewHolder> {

        private static final int[] COLORS = {Color.YELLOW, Color.RED, Color.GREEN};

        @Override
        public int getItemCount() {
            return 3;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setLayoutParams(matchParent());
            textView.setTextSize(40);
            textView.setGravity(Gravity.CENTER);
            return new ViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.itemView.setBackgroundColor(COLORS[position % COLORS.length]);
            ((TextView) holder.itemView).setText("Page " + position);
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
