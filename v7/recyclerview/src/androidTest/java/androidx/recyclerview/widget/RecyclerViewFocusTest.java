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

package androidx.recyclerview.widget;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RecyclerViewFocusTest {

    private static final int RV_HEIGHT_WIDTH = 200;
    private static final int ITEM_HEIGHT_WIDTH = 100;

    private RecyclerView mRecyclerView;
    private TestLinearLayoutManager mTestLinearLayoutManager;
    private TestContentView mTestContentView;

    @Rule
    public ActivityTestRule<TestContentViewActivity> mActivityRule =
            new ActivityTestRule<>(TestContentViewActivity.class);

    @Test
    public void focusSearch_layoutInterceptsAndReturnsNotNull_valueReturned() throws Throwable {
        setupRecyclerView(true, RecyclerView.VERTICAL, true);
        View expectedView = new View(mActivityRule.getActivity());
        View currentlyFocusedView = mRecyclerView.getChildAt(0);
        mTestLinearLayoutManager.mOnInterceptFocusSearchReturnValue = expectedView;

        View actualView = mRecyclerView.focusSearch(currentlyFocusedView, View.FOCUS_FORWARD);

        assertThat(actualView, is(equalTo(expectedView)));
    }

    @Test
    public void focusSearch_noAdapter_onFocusSearchFailedNotCalled() throws Throwable {
        setupRecyclerView(false, RecyclerView.VERTICAL, true);
        View currentlyFocusedView = mRecyclerView.getChildAt(1);

        mRecyclerView.focusSearch(currentlyFocusedView, View.FOCUS_FORWARD);

        assertThat(mTestLinearLayoutManager.mOnFocusSearchFailedCalled, is(false));
    }

    @Test
    public void focusSearch_layoutFrozen_onFocusSearchFailedNotCalled() throws Throwable {
        setupRecyclerView(true, RecyclerView.VERTICAL, true);
        mRecyclerView.setLayoutFrozen(true);
        View currentlyFocusedView = mRecyclerView.getChildAt(1);

        mRecyclerView.focusSearch(currentlyFocusedView, View.FOCUS_FORWARD);

        assertThat(mTestLinearLayoutManager.mOnFocusSearchFailedCalled, is(false));
    }

    @Test
    public void focusSearch_focusedViewNull_onFocusSearchFailedNotCalled() throws Throwable {
        setupRecyclerView(true, RecyclerView.VERTICAL, true);
        mRecyclerView.focusSearch(null, View.FOCUS_FORWARD);
        assertThat(mTestLinearLayoutManager.mOnFocusSearchFailedCalled, is(false));
    }

    /*
        Failures, null is returned
        Tests to verify when onFocusSearchFailed is called.
    */

    @Test
    public void focusSearch_verticalAndHasChildInDirection_findsCorrectChild() throws Throwable {
        setupRecyclerView(true, RecyclerView.VERTICAL, true);
        focusSearch_simpleFindFocusSucceeds_returnsCorrectValue(View.FOCUS_FORWARD, 0, 1);
        focusSearch_simpleFindFocusSucceeds_returnsCorrectValue(View.FOCUS_BACKWARD, 1, 0);
        focusSearch_simpleFindFocusSucceeds_returnsCorrectValue(View.FOCUS_DOWN, 0, 1);
        focusSearch_simpleFindFocusSucceeds_returnsCorrectValue(View.FOCUS_UP, 1, 0);
    }

    @Test
    public void focusSearch_horizontalAndHasChildInDirection_findsCorrectChild() throws Throwable {
        setupRecyclerView(true, RecyclerView.HORIZONTAL, true);
        focusSearch_simpleFindFocusSucceeds_returnsCorrectValue(View.FOCUS_FORWARD, 0, 1);
        focusSearch_simpleFindFocusSucceeds_returnsCorrectValue(View.FOCUS_BACKWARD, 1, 0);
        focusSearch_simpleFindFocusSucceeds_returnsCorrectValue(View.FOCUS_RIGHT, 0, 1);
        focusSearch_simpleFindFocusSucceeds_returnsCorrectValue(View.FOCUS_LEFT, 1, 0);
    }

    @Test
    @SdkSuppress(minSdkVersion = 17)
    public void focusSearch_horizontalRtlAndHasChildInDirection_findsCorrectChild()
            throws Throwable {
        setupRecyclerView(true, RecyclerView.HORIZONTAL, false);
        focusSearch_simpleFindFocusSucceeds_returnsCorrectValue(View.FOCUS_FORWARD, 0, 1);
        focusSearch_simpleFindFocusSucceeds_returnsCorrectValue(View.FOCUS_BACKWARD, 1, 0);
        focusSearch_simpleFindFocusSucceeds_returnsCorrectValue(View.FOCUS_RIGHT, 1, 0);
        focusSearch_simpleFindFocusSucceeds_returnsCorrectValue(View.FOCUS_LEFT, 0, 1);
    }

    @Test
    public void focusSearch_verticalAndHasChildInDirection_doesNotCallOnFocusSearchFailed()
            throws Throwable {
        setupRecyclerView(true, RecyclerView.VERTICAL, true);
        focusSearch_simpleFindFocusSucceeds_doesNotCallOnFocusSearchFailedCalled(
                View.FOCUS_FORWARD, 0);
        focusSearch_simpleFindFocusSucceeds_doesNotCallOnFocusSearchFailedCalled(
                View.FOCUS_BACKWARD, 1);
        focusSearch_simpleFindFocusSucceeds_doesNotCallOnFocusSearchFailedCalled(
                View.FOCUS_DOWN, 0);
        focusSearch_simpleFindFocusSucceeds_doesNotCallOnFocusSearchFailedCalled(
                View.FOCUS_UP, 1);
    }

    @Test
    public void focusSearch_horizontalAndHasChildInDirection_doesNotCallOnFocusSearchFailed()
            throws Throwable {
        setupRecyclerView(true, RecyclerView.HORIZONTAL, true);
        focusSearch_simpleFindFocusSucceeds_doesNotCallOnFocusSearchFailedCalled(
                View.FOCUS_FORWARD, 0);
        focusSearch_simpleFindFocusSucceeds_doesNotCallOnFocusSearchFailedCalled(
                View.FOCUS_BACKWARD, 1);
        focusSearch_simpleFindFocusSucceeds_doesNotCallOnFocusSearchFailedCalled(
                View.FOCUS_RIGHT, 0);
        focusSearch_simpleFindFocusSucceeds_doesNotCallOnFocusSearchFailedCalled(
                View.FOCUS_LEFT, 1);
    }

    @Test
    @SdkSuppress(minSdkVersion = 17)
    public void focusSearch_horizontalRtlAndHasChildInDirection_doesNotCallOnFocusSearchFailed()
            throws Throwable {
        setupRecyclerView(true, RecyclerView.HORIZONTAL, false);
        focusSearch_simpleFindFocusSucceeds_doesNotCallOnFocusSearchFailedCalled(
                View.FOCUS_FORWARD, 0);
        focusSearch_simpleFindFocusSucceeds_doesNotCallOnFocusSearchFailedCalled(
                View.FOCUS_BACKWARD, 1);
        focusSearch_simpleFindFocusSucceeds_doesNotCallOnFocusSearchFailedCalled(
                View.FOCUS_RIGHT, 1);
        focusSearch_simpleFindFocusSucceeds_doesNotCallOnFocusSearchFailedCalled(
                View.FOCUS_LEFT, 0);
    }

    @Test
    public void focusSearch_verticalAndDoesNotHaveChildInDirection_callsOnFocusSearchFailed()
            throws Throwable {
        setupRecyclerView(true, RecyclerView.VERTICAL, true);
        focusSearch_simpleFindFocusFails_callsOnFocusSearchFailed(View.FOCUS_FORWARD, 1);
        focusSearch_simpleFindFocusFails_callsOnFocusSearchFailed(View.FOCUS_BACKWARD, 0);
        focusSearch_simpleFindFocusFails_callsOnFocusSearchFailed(View.FOCUS_DOWN, 1);
        focusSearch_simpleFindFocusFails_callsOnFocusSearchFailed(View.FOCUS_UP, 0);
    }

    @Test
    public void focusSearch_horizontalAndDoesNotHaveChildInDirection_callsOnFocusSearchFailed()
            throws Throwable {
        setupRecyclerView(true, RecyclerView.HORIZONTAL, true);
        focusSearch_simpleFindFocusFails_callsOnFocusSearchFailed(View.FOCUS_FORWARD, 1);
        focusSearch_simpleFindFocusFails_callsOnFocusSearchFailed(View.FOCUS_BACKWARD, 0);
        focusSearch_simpleFindFocusFails_callsOnFocusSearchFailed(View.FOCUS_RIGHT, 1);
        focusSearch_simpleFindFocusFails_callsOnFocusSearchFailed(View.FOCUS_LEFT, 0);
    }

    @Test
    @SdkSuppress(minSdkVersion = 17)
    public void focusSearch_horizontalRtlAndDoesNotHaveChildInDirection_callsOnFocusSearchFailed()
            throws Throwable {
        setupRecyclerView(true, RecyclerView.HORIZONTAL, false);
        focusSearch_simpleFindFocusFails_callsOnFocusSearchFailed(View.FOCUS_FORWARD, 1);
        focusSearch_simpleFindFocusFails_callsOnFocusSearchFailed(View.FOCUS_BACKWARD, 0);
        focusSearch_simpleFindFocusFails_callsOnFocusSearchFailed(View.FOCUS_RIGHT, 0);
        focusSearch_simpleFindFocusFails_callsOnFocusSearchFailed(View.FOCUS_LEFT, 1);
    }

    private void focusSearch_simpleFindFocusSucceeds_returnsCorrectValue(int direction,
            int startingChild, int expectedChild) {
        View currentlyFocusedView = mRecyclerView.getChildAt(startingChild);
        View expectedResult = mRecyclerView.getChildAt(expectedChild);

        View actualResult = mRecyclerView.focusSearch(currentlyFocusedView, direction);

        assertThat(actualResult, is(equalTo(expectedResult)));
    }

    private void focusSearch_simpleFindFocusSucceeds_doesNotCallOnFocusSearchFailedCalled(
            int direction, int startingChild) {
        mTestLinearLayoutManager.mOnFocusSearchFailedCalled = false;
        View currentlyFocusedView = mRecyclerView.getChildAt(startingChild);

        mRecyclerView.focusSearch(currentlyFocusedView, direction);

        assertThat(mTestLinearLayoutManager.mOnFocusSearchFailedCalled, is(false));
    }

    private void focusSearch_simpleFindFocusFails_callsOnFocusSearchFailed(int direction,
            int startingChild) {
        mTestLinearLayoutManager.mOnFocusSearchFailedCalled = false;
        View currentlyFocusedView = mRecyclerView.getChildAt(startingChild);
        mRecyclerView.focusSearch(currentlyFocusedView, direction);
        assertThat(mTestLinearLayoutManager.mOnFocusSearchFailedCalled, is(true));
    }

    private void setupRecyclerView(final boolean hasAdapter, int orientation, boolean ltr)
            throws Throwable {
        final TestContentViewActivity testContentViewActivity = mActivityRule.getActivity();
        mTestContentView = testContentViewActivity.getContentView();

        mTestLinearLayoutManager = new TestLinearLayoutManager(testContentViewActivity,
                orientation, false);

        mRecyclerView = new RecyclerView(InstrumentationRegistry.getContext());
        if (!ltr) {
            mRecyclerView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
        mRecyclerView.setBackgroundColor(0xFFFF0000);
        mRecyclerView.setLayoutParams(
                new TestContentView.LayoutParams(RV_HEIGHT_WIDTH, RV_HEIGHT_WIDTH));
        mRecyclerView.setLayoutManager(mTestLinearLayoutManager);

        if (hasAdapter) {
            mRecyclerView.setAdapter(
                    new TestAdapter(100, ITEM_HEIGHT_WIDTH, ITEM_HEIGHT_WIDTH));
        }

        mTestContentView.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTestContentView.addView(mRecyclerView);
            }
        });
        mTestContentView.awaitLayouts(2);
    }

    private class TestLinearLayoutManager extends LinearLayoutManager {

        boolean mOnFocusSearchFailedCalled = false;
        View mOnInterceptFocusSearchReturnValue;
        View mViewToReturnFromonFocusSearchFailed;

        TestLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        @Override
        public View onInterceptFocusSearch(View focused, int direction) {
            return mOnInterceptFocusSearchReturnValue;
        }

        @Override
        public View onFocusSearchFailed(View focused, int focusDirection,
                RecyclerView.Recycler recycler, RecyclerView.State state) {
            mOnFocusSearchFailedCalled = true;
            return mViewToReturnFromonFocusSearchFailed;
        }
    }

    private class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {

        private int mItemCount;
        private int mItemLayoutWidth;
        private int mItemLayoutHeight;

        TestAdapter(int itemCount, int itemLayoutWidth, int itemLayoutHeight) {
            mItemCount = itemCount;
            mItemLayoutWidth = itemLayoutWidth;
            mItemLayoutHeight = itemLayoutHeight;
        }

        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setLayoutParams(
                    new ViewGroup.LayoutParams(mItemLayoutWidth, mItemLayoutHeight));
            textView.setFocusableInTouchMode(true);
            return new TestViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(TestViewHolder holder, int position) {
            ((TextView) holder.itemView).setText("Position: " + position);
        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }
    }

    private class TestViewHolder extends RecyclerView.ViewHolder {

        TestViewHolder(View itemView) {
            super(itemView);
        }
    }
}
