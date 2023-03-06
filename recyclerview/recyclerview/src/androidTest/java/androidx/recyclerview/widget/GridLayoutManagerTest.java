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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_ACCESSIBILITY_FOCUS;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_IN_DIRECTION;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_TO_POSITION;
import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.UiAutomation;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.util.StateSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GridLayoutManagerTest extends BaseGridLayoutManagerTest {

    private static final int[] SPAN_SIZES = new int[]{1, 1, 1, 2, 2, 2, 2, 3, 3, 2, 2, 2};

    private static final int DEFAULT_ACCESSIBILITY_EVENT_TIMEOUT_MILLIS = 5000;

    private final GridLayoutManager.SpanSizeLookup mSpanSizeLookupForSpanIndexTest =
            new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            return SPAN_SIZES[position];
        }
    };

    @Test
    public void focusSearchFailureUp() throws Throwable {
        focusSearchFailure(false);
    }

    @Test
    public void focusSearchFailureDown() throws Throwable {
        focusSearchFailure(true);
    }

    @Test
    public void scrollToBadOffset() throws Throwable {
        scrollToBadOffset(false);
    }

    @Test
    public void scrollToBadOffsetReverse() throws Throwable {
        scrollToBadOffset(true);
    }

    private void scrollToBadOffset(boolean reverseLayout) throws Throwable {
        final int w = 500;
        final int h = 1000;
        RecyclerView recyclerView = setupBasic(new Config(2, 100).reverseLayout(reverseLayout),
                new GridTestAdapter(100) {
                    @Override
                    public void onBindViewHolder(@NonNull TestViewHolder holder,
                            int position) {
                        super.onBindViewHolder(holder, position);
                        ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
                        if (lp == null) {
                            lp = new ViewGroup.LayoutParams(w / 2, h / 2);
                            holder.itemView.setLayoutParams(lp);
                        } else {
                            lp.width = w / 2;
                            lp.height = h / 2;
                            holder.itemView.setLayoutParams(lp);
                        }
                    }
                });
        TestedFrameLayout.FullControlLayoutParams lp
                = new TestedFrameLayout.FullControlLayoutParams(w, h);
        recyclerView.setLayoutParams(lp);
        waitForFirstLayout(recyclerView);
        mGlm.expectLayout(1);
        scrollToPosition(11);
        mGlm.waitForLayout(2);
        // assert spans and position etc
        for (int i = 0; i < mGlm.getChildCount(); i++) {
            View child = mGlm.getChildAt(i);
            GridLayoutManager.LayoutParams params = (GridLayoutManager.LayoutParams) child
                    .getLayoutParams();
            assertThat("span index for child at " + i + " with position " + params
                            .getViewAdapterPosition(),
                    params.getSpanIndex(), CoreMatchers.is(params.getViewAdapterPosition() % 2));
        }
        // assert spans and positions etc.
        int lastVisible = mGlm.findLastVisibleItemPosition();
        // this should be the scrolled child
        assertThat(lastVisible, CoreMatchers.is(11));
    }

    private void focusSearchFailure(boolean scrollDown) throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 31).reverseLayout(!scrollDown)
                , new GridTestAdapter(31, 1) {
                    RecyclerView mAttachedRv;

                    @Override
                    @SuppressWarnings("deprecation") // used for kitkat tests
                    public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                            int viewType) {
                        TestViewHolder testViewHolder = super.onCreateViewHolder(parent, viewType);
                        testViewHolder.itemView.setFocusable(true);
                        testViewHolder.itemView.setFocusableInTouchMode(true);
                        // Good to have colors for debugging
                        StateListDrawable stl = new StateListDrawable();
                        stl.addState(new int[]{android.R.attr.state_focused},
                                new ColorDrawable(Color.RED));
                        stl.addState(StateSet.WILD_CARD, new ColorDrawable(Color.BLUE));
                        testViewHolder.itemView.setBackgroundDrawable(stl);
                        return testViewHolder;
                    }

                    @Override
                    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
                        mAttachedRv = recyclerView;
                    }

                    @Override
                    public void onBindViewHolder(@NonNull TestViewHolder holder,
                            int position) {
                        super.onBindViewHolder(holder, position);
                        holder.itemView.setMinimumHeight(mAttachedRv.getHeight() / 3);
                    }
                });
        waitForFirstLayout(recyclerView);

        View viewToFocus = recyclerView.findViewHolderForAdapterPosition(1).itemView;
        assertTrue(requestFocus(viewToFocus, true));
        assertSame(viewToFocus, recyclerView.getFocusedChild());
        int pos = 1;
        View focusedView = viewToFocus;
        while (pos < 31) {
            focusSearch(focusedView, scrollDown ? View.FOCUS_DOWN : View.FOCUS_UP);
            waitForIdleScroll(recyclerView);
            focusedView = recyclerView.getFocusedChild();
            assertEquals(Math.min(pos + 3, mAdapter.getItemCount() - 1),
                    recyclerView
                            .getChildViewHolder(focusedView).getAbsoluteAdapterPosition());
            pos += 3;
        }
    }

    /**
     * Tests that the GridLayoutManager retains the focused element after multiple measure
     * calls to the RecyclerView.  There was a bug where the focused view was lost when the soft
     * keyboard opened.  This test simulates the measure/layout events triggered by the opening
     * of the soft keyboard by making two calls to measure.  A simulation was done because using
     * the soft keyboard in the test caused many issues on API levels 15, 17 and 19.
     */
    @Test
    public void focusedChildStaysInViewWhenRecyclerViewShrinks() throws Throwable {

        // Arrange.

        final int spanCount = 3;
        // For simplicity and test stability, item count should be a multiple of spanCount.
        final int itemCount = 33 * spanCount;

        final RecyclerView recyclerView = inflateWrappedRV();
        ViewGroup.LayoutParams lp = recyclerView.getLayoutParams();
        lp.height = WRAP_CONTENT;
        lp.width = MATCH_PARENT;

        Config config = new Config(spanCount, itemCount);
        mGlm = new WrappedGridLayoutManager(getActivity(), config.mSpanCount, config.mOrientation,
                config.mReverseLayout);
        recyclerView.setLayoutManager(mGlm);

        GridFocusableAdapter gridFocusableAdapter = new GridFocusableAdapter(itemCount);
        gridFocusableAdapter.assignSpanSizeLookup(mGlm);
        recyclerView.setAdapter(gridFocusableAdapter);

        mGlm.expectLayout(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().getContainer().addView(recyclerView);
            }
        });
        mGlm.waitForLayout(3);

        int width = recyclerView.getWidth();
        int height = recyclerView.getHeight();
        final int widthMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        final int fullHeightMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST);
        // "MinusOne" so that a measure call will appropriately trigger onMeasure after RecyclerView
        // was previously laid out with the full height version.
        final int fullHeightMinusOneMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(height - 1, View.MeasureSpec.AT_MOST);
        final int halfHeightMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(height / 2, View.MeasureSpec.AT_MOST);

        // Act 1.

        // First focus on the last fully visible child located at span index #1.
        View toFocus = findLastFullyVisibleChild(recyclerView);
        int focusIndex = recyclerView.getChildAdapterPosition(toFocus);
        focusIndex = (focusIndex / spanCount) * spanCount + 1;
        toFocus = recyclerView.findViewHolderForAdapterPosition(focusIndex).itemView;
        assertTrue(focusIndex >= 1 && focusIndex < itemCount);

        requestFocus(toFocus, false);

        mGlm.expectLayout(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.measure(widthMeasureSpec, fullHeightMinusOneMeasureSpec);
                recyclerView.measure(widthMeasureSpec, halfHeightMeasureSpec);
                recyclerView.layout(
                        0,
                        0,
                        recyclerView.getMeasuredWidth(),
                        recyclerView.getMeasuredHeight());
            }
        });
        mGlm.waitForLayout(3);

        // Assert 1.

        assertThat("Child at position " + focusIndex + " should be focused",
                toFocus.hasFocus(), is(true));
        assertTrue("Child view at adapter pos " + focusIndex + " should be fully visible.",
                isViewPartiallyInBound(recyclerView, toFocus));

        // Act 2.

        mGlm.expectLayout(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.measure(widthMeasureSpec, fullHeightMeasureSpec);
                recyclerView.layout(
                        0,
                        0,
                        recyclerView.getMeasuredWidth(),
                        recyclerView.getMeasuredHeight());
            }
        });
        mGlm.waitForLayout(3);

        // Assert 2.

        assertTrue("Child view at adapter pos " + focusIndex + " should be fully visible.",
                isViewPartiallyInBound(recyclerView, toFocus));

        // Act 3.

        // Now focus on the first fully visible EditText located at the last span index.
        toFocus = findFirstFullyVisibleChild(recyclerView);
        focusIndex = recyclerView.getChildAdapterPosition(toFocus);
        focusIndex = (focusIndex / spanCount) * spanCount + (spanCount - 1);
        toFocus = recyclerView.findViewHolderForAdapterPosition(focusIndex).itemView;

        requestFocus(toFocus, false);

        mGlm.expectLayout(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.measure(widthMeasureSpec, fullHeightMinusOneMeasureSpec);
                recyclerView.measure(widthMeasureSpec, halfHeightMeasureSpec);
                recyclerView.layout(
                        0,
                        0,
                        recyclerView.getMeasuredWidth(),
                        recyclerView.getMeasuredHeight());
            }
        });
        mGlm.waitForLayout(3);

        // Assert 3.

        assertTrue("Child view at adapter pos " + focusIndex + " should be fully visible.",
                isViewPartiallyInBound(recyclerView, toFocus));
    }

    @Test
    public void topUnfocusableViewsVisibility() throws Throwable {
        // The maximum number of rows that can be fully in-bounds of RV.
        final int visibleRowCount = 5;
        final int spanCount = 3;
        final int consecutiveFocusableRowsCount = 4;
        final int consecutiveUnFocusableRowsCount = 8;
        final int itemCount = (consecutiveFocusableRowsCount + consecutiveUnFocusableRowsCount)
                * spanCount;

        final RecyclerView recyclerView = setupBasic(new Config(spanCount, itemCount)
                        .reverseLayout(true),
                new GridTestAdapter(itemCount, 1) {
                    RecyclerView mAttachedRv;

                    @Override
                    @SuppressWarnings("deprecated") // using this for kitkat tests
                    public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                            int viewType) {
                        TestViewHolder testViewHolder = super.onCreateViewHolder(parent, viewType);
                        // Good to have colors for debugging
                        StateListDrawable stl = new StateListDrawable();
                        stl.addState(new int[]{android.R.attr.state_focused},
                                new ColorDrawable(Color.RED));
                        stl.addState(StateSet.WILD_CARD, new ColorDrawable(Color.BLUE));
                        testViewHolder.itemView.setBackgroundDrawable(stl);
                        return testViewHolder;
                    }

                    @Override
                    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
                        mAttachedRv = recyclerView;
                    }

                    @Override
                    public void onBindViewHolder(@NonNull TestViewHolder holder,
                            int position) {
                        super.onBindViewHolder(holder, position);
                        if (position < spanCount * consecutiveFocusableRowsCount) {
                            holder.itemView.setFocusable(true);
                            holder.itemView.setFocusableInTouchMode(true);
                        } else {
                            holder.itemView.setFocusable(false);
                            holder.itemView.setFocusableInTouchMode(false);
                        }
                        holder.itemView.setMinimumHeight(mAttachedRv.getHeight() / visibleRowCount);
                    }
                });
        waitForFirstLayout(recyclerView);

        // adapter position of the currently focused item.
        int focusIndex = 1;
        RecyclerView.ViewHolder toFocus = recyclerView.findViewHolderForAdapterPosition(focusIndex);
        View viewToFocus = toFocus.itemView;
        assertTrue(requestFocus(viewToFocus, true));
        assertSame(viewToFocus, recyclerView.getFocusedChild());

        // adapter position of the item (whether focusable or not) that just becomes fully
        // visible after focusSearch.
        int visibleIndex = focusIndex;
        // The VH of the above adapter position
        RecyclerView.ViewHolder toVisible = null;

        int maxFocusIndex = (consecutiveFocusableRowsCount - 1) * spanCount + focusIndex;
        int maxVisibleIndex = (consecutiveFocusableRowsCount + visibleRowCount - 2)
                * spanCount + visibleIndex;

        // Navigate up through the focusable and unfocusable rows. The focusable rows should
        // become focused one by one until hitting the last focusable row, at which point,
        // unfocusable rows should become visible on the screen until the currently focused row
        // stays on the screen.
        int pos = focusIndex + spanCount;
        while (pos < itemCount) {
            focusSearch(recyclerView.getFocusedChild(), View.FOCUS_UP, true);
            waitForIdleScroll(recyclerView);
            focusIndex = Math.min(maxFocusIndex, (focusIndex + spanCount));
            toFocus = recyclerView.findViewHolderForAdapterPosition(focusIndex);
            visibleIndex = Math.min(maxVisibleIndex, (visibleIndex + spanCount));
            toVisible = recyclerView.findViewHolderForAdapterPosition(visibleIndex);

            assertThat("Child at position " + focusIndex + " should be focused",
                    toFocus.itemView.hasFocus(), is(true));
            assertTrue("Focused child should be at least partially visible.",
                    isViewPartiallyInBound(recyclerView, toFocus.itemView));
            assertTrue("Child view at adapter pos " + visibleIndex + " should be fully visible.",
                    isViewFullyInBound(recyclerView, toVisible.itemView));
            pos += spanCount;
        }
    }

    @Test
    public void bottomUnfocusableViewsVisibility() throws Throwable {
        // The maximum number of rows that can be fully in-bounds of RV.
        final int visibleRowCount = 5;
        final int spanCount = 3;
        final int consecutiveFocusableRowsCount = 4;
        final int consecutiveUnFocusableRowsCount = 8;
        final int itemCount = (consecutiveFocusableRowsCount + consecutiveUnFocusableRowsCount)
                * spanCount;

        final RecyclerView recyclerView = setupBasic(new Config(spanCount, itemCount)
                        .reverseLayout(false),
                new GridTestAdapter(itemCount, 1) {
                    RecyclerView mAttachedRv;

                    @Override
                    @SuppressWarnings("deprecated") // using this for kitkat tests
                    public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                            int viewType) {
                        TestViewHolder testViewHolder = super.onCreateViewHolder(parent, viewType);
                        // Good to have colors for debugging
                        StateListDrawable stl = new StateListDrawable();
                        stl.addState(new int[]{android.R.attr.state_focused},
                                new ColorDrawable(Color.RED));
                        stl.addState(StateSet.WILD_CARD, new ColorDrawable(Color.BLUE));
                        testViewHolder.itemView.setBackgroundDrawable(stl);
                        return testViewHolder;
                    }

                    @Override
                    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
                        mAttachedRv = recyclerView;
                    }

                    @Override
                    public void onBindViewHolder(@NonNull TestViewHolder holder,
                            int position) {
                        super.onBindViewHolder(holder, position);
                        if (position < spanCount * consecutiveFocusableRowsCount) {
                            holder.itemView.setFocusable(true);
                            holder.itemView.setFocusableInTouchMode(true);
                        } else {
                            holder.itemView.setFocusable(false);
                            holder.itemView.setFocusableInTouchMode(false);
                        }
                        holder.itemView.setMinimumHeight(mAttachedRv.getHeight() / visibleRowCount);
                    }
                });
        waitForFirstLayout(recyclerView);

        // adapter position of the currently focused item.
        int focusIndex = 1;
        RecyclerView.ViewHolder toFocus = recyclerView.findViewHolderForAdapterPosition(focusIndex);
        View viewToFocus = toFocus.itemView;
        assertTrue(requestFocus(viewToFocus, true));
        assertSame(viewToFocus, recyclerView.getFocusedChild());

        // adapter position of the item (whether focusable or not) that just becomes fully
        // visible after focusSearch.
        int visibleIndex = focusIndex;
        // The VH of the above adapter position
        RecyclerView.ViewHolder toVisible = null;

        int maxFocusIndex = (consecutiveFocusableRowsCount - 1) * spanCount + focusIndex;
        int maxVisibleIndex = (consecutiveFocusableRowsCount + visibleRowCount - 2)
                * spanCount + visibleIndex;

        // Navigate down through the focusable and unfocusable rows. The focusable rows should
        // become focused one by one until hitting the last focusable row, at which point,
        // unfocusable rows should become visible on the screen until the currently focused row
        // stays on the screen.
        int pos = focusIndex + spanCount;
        while (pos < itemCount) {
            focusSearch(recyclerView.getFocusedChild(), View.FOCUS_DOWN, true);
            waitForIdleScroll(recyclerView);
            focusIndex = Math.min(maxFocusIndex, (focusIndex + spanCount));
            toFocus = recyclerView.findViewHolderForAdapterPosition(focusIndex);
            visibleIndex = Math.min(maxVisibleIndex, (visibleIndex + spanCount));
            toVisible = recyclerView.findViewHolderForAdapterPosition(visibleIndex);

            assertThat("Child at position " + focusIndex + " should be focused",
                    toFocus.itemView.hasFocus(), is(true));
            assertTrue("Focused child should be at least partially visible.",
                    isViewPartiallyInBound(recyclerView, toFocus.itemView));
            assertTrue("Child view at adapter pos " + visibleIndex + " should be fully visible.",
                    isViewFullyInBound(recyclerView, toVisible.itemView));
            pos += spanCount;
        }
    }

    @Test
    public void leftUnfocusableViewsVisibility() throws Throwable {
        // The maximum number of columns that can be fully in-bounds of RV.
        final int visibleColCount = 5;
        final int spanCount = 3;
        final int consecutiveFocusableColsCount = 4;
        final int consecutiveUnFocusableColsCount = 8;
        final int itemCount = (consecutiveFocusableColsCount + consecutiveUnFocusableColsCount)
                * spanCount;
        final int childWidth = 200;
        final int childHeight = WRAP_CONTENT;
        // Parent width is 1 more than 4 times child width, so when focusable child is 1 pixel on
        // screen 4 non-focusable children can fit on screen.
        final int parentWidth = childWidth * 4 + 1;
        final int parentHeight = 1000;

        final RecyclerView recyclerView = setupBasic(new Config(spanCount, itemCount)
                        .orientation(HORIZONTAL).reverseLayout(true),
                new GridTestAdapter(itemCount, 1) {

                    @Override
                    @SuppressWarnings("deprecated") // using this for kitkat tests
                    public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                            int viewType) {
                        TestViewHolder testViewHolder = super.onCreateViewHolder(parent, viewType);
                        // Good to have colors for debugging
                        StateListDrawable stl = new StateListDrawable();
                        stl.addState(new int[]{android.R.attr.state_focused},
                                new ColorDrawable(Color.RED));
                        stl.addState(StateSet.WILD_CARD, new ColorDrawable(Color.BLUE));
                        testViewHolder.itemView.setBackgroundDrawable(stl);
                        return testViewHolder;
                    }

                    @Override
                    public void onBindViewHolder(@NonNull TestViewHolder holder,
                            int position) {
                        super.onBindViewHolder(holder, position);
                        if (position < spanCount * consecutiveFocusableColsCount) {
                            holder.itemView.setFocusable(true);
                            holder.itemView.setFocusableInTouchMode(true);
                        } else {
                            holder.itemView.setFocusable(false);
                            holder.itemView.setFocusableInTouchMode(false);
                        }
                        holder.itemView.setLayoutParams(
                                new RecyclerView.LayoutParams(childWidth, childHeight));
                    }
                });
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(parentWidth, parentHeight));
        waitForFirstLayout(recyclerView);

        // adapter position of the currently focused item.
        int focusIndex = 1;
        RecyclerView.ViewHolder toFocus = recyclerView.findViewHolderForAdapterPosition(focusIndex);
        View viewToFocus = toFocus.itemView;
        assertTrue(requestFocus(viewToFocus, true));
        assertSame(viewToFocus, recyclerView.getFocusedChild());

        // adapter position of the item (whether focusable or not) that just becomes fully
        // visible after focusSearch.
        int visibleIndex = focusIndex;
        // The VH of the above adapter position
        RecyclerView.ViewHolder toVisible = null;

        int maxFocusIndex = (consecutiveFocusableColsCount - 1) * spanCount + focusIndex;
        int maxVisibleIndex = (consecutiveFocusableColsCount + visibleColCount - 2)
                * spanCount + visibleIndex;

        // Navigate left through the focusable and unfocusable columns. The focusable columns should
        // become focused one by one until hitting the last focusable column, at which point,
        // unfocusable columns should become visible on the screen until the currently focused
        // column stays on the screen.
        int pos = focusIndex + spanCount;
        while (pos < itemCount) {
            focusSearch(recyclerView.getFocusedChild(), View.FOCUS_LEFT, true);
            waitForIdleScroll(recyclerView);
            focusIndex = Math.min(maxFocusIndex, (focusIndex + spanCount));
            toFocus = recyclerView.findViewHolderForAdapterPosition(focusIndex);
            visibleIndex = Math.min(maxVisibleIndex, (visibleIndex + spanCount));
            toVisible = recyclerView.findViewHolderForAdapterPosition(visibleIndex);

            assertThat("Child at position " + focusIndex + " should be focused",
                    toFocus.itemView.hasFocus(), is(true));
            assertTrue("Focused child should be at least partially visible.",
                    isViewPartiallyInBound(recyclerView, toFocus.itemView));
            assertTrue("Child view at adapter pos " + visibleIndex + " should be fully visible.",
                    isViewFullyInBound(recyclerView, toVisible.itemView));
            pos += spanCount;
        }
    }

    @Test
    public void rightUnfocusableViewsVisibility() throws Throwable {
        // The maximum number of columns that can be fully in-bounds of RV.
        final int visibleColCount = 5;
        final int spanCount = 3;
        final int consecutiveFocusableColsCount = 4;
        final int consecutiveUnFocusableColsCount = 8;
        final int itemCount = (consecutiveFocusableColsCount + consecutiveUnFocusableColsCount)
                * spanCount;
        final int childWidth = 200;
        final int childHeight = WRAP_CONTENT;
        // Parent width is 1 more than 4 times child width, so when focusable child is 1 pixel on
        // screen 4 non-focusable children can fit on screen.
        final int parentWidth = childWidth * 4 + 1;
        final int parentHeight = 1000;

        final RecyclerView recyclerView = setupBasic(new Config(spanCount, itemCount)
                        .orientation(HORIZONTAL).reverseLayout(false),
                new GridTestAdapter(itemCount, 1) {
                    @Override
                    @SuppressWarnings("deprecated") // using this for kitkat tests
                    public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                            int viewType) {
                        TestViewHolder testViewHolder = super.onCreateViewHolder(parent, viewType);
                        // Good to have colors for debugging
                        StateListDrawable stl = new StateListDrawable();
                        stl.addState(new int[]{android.R.attr.state_focused},
                                new ColorDrawable(Color.RED));
                        stl.addState(StateSet.WILD_CARD, new ColorDrawable(Color.BLUE));
                        testViewHolder.itemView.setBackgroundDrawable(stl);
                        return testViewHolder;
                    }

                    @Override
                    public void onBindViewHolder(@NonNull TestViewHolder holder,
                            int position) {
                        super.onBindViewHolder(holder, position);
                        if (position < spanCount * consecutiveFocusableColsCount) {
                            holder.itemView.setFocusable(true);
                            holder.itemView.setFocusableInTouchMode(true);
                        } else {
                            holder.itemView.setFocusable(false);
                            holder.itemView.setFocusableInTouchMode(false);
                        }
                        holder.itemView.setLayoutParams(
                                new RecyclerView.LayoutParams(childWidth, childHeight));
                    }
                });
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(parentWidth, parentHeight));
        waitForFirstLayout(recyclerView);

        // adapter position of the currently focused item.
        int focusIndex = 1;
        RecyclerView.ViewHolder toFocus = recyclerView.findViewHolderForAdapterPosition(focusIndex);
        View viewToFocus = toFocus.itemView;
        assertTrue(requestFocus(viewToFocus, true));
        assertSame(viewToFocus, recyclerView.getFocusedChild());

        // adapter position of the item (whether focusable or not) that just becomes fully
        // visible after focusSearch.
        int visibleIndex = focusIndex;
        // The VH of the above adapter position
        RecyclerView.ViewHolder toVisible = null;

        int maxFocusIndex = (consecutiveFocusableColsCount - 1) * spanCount + focusIndex;
        int maxVisibleIndex = (consecutiveFocusableColsCount + visibleColCount - 2)
                * spanCount + visibleIndex;

        // Navigate right through the focusable and unfocusable columns. The focusable columns
        // should become focused one by one until hitting the last focusable column, at which point,
        // unfocusable columns should become visible on the screen until the currently focused
        // column stays on the screen.
        int pos = focusIndex + spanCount;
        while (pos < itemCount) {
            focusSearch(recyclerView.getFocusedChild(), View.FOCUS_RIGHT, true);
            waitForIdleScroll(recyclerView);
            focusIndex = Math.min(maxFocusIndex, (focusIndex + spanCount));
            toFocus = recyclerView.findViewHolderForAdapterPosition(focusIndex);
            visibleIndex = Math.min(maxVisibleIndex, (visibleIndex + spanCount));
            toVisible = recyclerView.findViewHolderForAdapterPosition(visibleIndex);

            assertThat("Child at position " + focusIndex + " should be focused",
                    toFocus.itemView.hasFocus(), is(true));
            assertTrue("Focused child should be at least partially visible.",
                    isViewPartiallyInBound(recyclerView, toFocus.itemView));
            assertTrue("Child view at adapter pos " + visibleIndex + " should be fully visible.",
                    isViewFullyInBound(recyclerView, toVisible.itemView));
            pos += spanCount;
        }
    }

    @UiThreadTest
    @Test
    public void scrollWithoutLayout() throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 100));
        mGlm.expectLayout(1);
        setRecyclerView(recyclerView);
        mGlm.setSpanCount(5);
        recyclerView.scrollBy(0, 10);
    }

    @Test
    public void scrollWithoutLayoutAfterInvalidate() throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 100));
        waitForFirstLayout(recyclerView);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGlm.setSpanCount(5);
                recyclerView.scrollBy(0, 10);
            }
        });
    }

    @Test
    public void predictiveSpanLookup1() throws Throwable {
        predictiveSpanLookupTest(0, false);
    }

    @Test
    public void predictiveSpanLookup2() throws Throwable {
        predictiveSpanLookupTest(0, true);
    }

    @Test
    public void predictiveSpanLookup3() throws Throwable {
        predictiveSpanLookupTest(1, false);
    }

    @Test
    public void predictiveSpanLookup4() throws Throwable {
        predictiveSpanLookupTest(1, true);
    }

    public void predictiveSpanLookupTest(int remaining, boolean removeFromStart) throws Throwable {
        RecyclerView recyclerView = setupBasic(new Config(3, 10));
        mGlm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position < 0 || position >= mAdapter.getItemCount()) {
                    postExceptionToInstrumentation(new AssertionError("position is not within " +
                            "adapter range. pos:" + position + ", adapter size:" +
                            mAdapter.getItemCount()));
                }
                return 1;
            }

            @Override
            public int getSpanIndex(int position, int spanCount) {
                if (position < 0 || position >= mAdapter.getItemCount()) {
                    postExceptionToInstrumentation(new AssertionError("position is not within " +
                            "adapter range. pos:" + position + ", adapter size:" +
                            mAdapter.getItemCount()));
                }
                return super.getSpanIndex(position, spanCount);
            }
        });
        waitForFirstLayout(recyclerView);
        checkForMainThreadException();
        assertTrue("Assumption check", mGlm.supportsPredictiveItemAnimations());
        mGlm.expectLayout(2);
        int deleteCnt = 10 - remaining;
        int deleteStart = removeFromStart ? 0 : remaining;
        mAdapter.deleteAndNotify(deleteStart, deleteCnt);
        mGlm.waitForLayout(2);
        checkForMainThreadException();
    }

    @Test
    public void movingAGroupOffScreenForAddedItems() throws Throwable {
        final RecyclerView rv = setupBasic(new Config(3, 100));
        final int[] maxId = new int[1];
        maxId[0] = -1;
        final SparseIntArray spanLookups = new SparseIntArray();
        final AtomicBoolean enableSpanLookupLogging = new AtomicBoolean(false);
        mGlm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (maxId[0] > 0 && mAdapter.getItemAt(position).mId > maxId[0]) {
                    return 1;
                } else if (enableSpanLookupLogging.get() && !rv.mState.isPreLayout()) {
                    spanLookups.put(position, spanLookups.get(position, 0) + 1);
                }
                return 3;
            }
        });
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(true);
        waitForFirstLayout(rv);
        View lastView = rv.getChildAt(rv.getChildCount() - 1);
        final int lastPos = rv.getChildAdapterPosition(lastView);
        maxId[0] = mAdapter.getItemAt(mAdapter.getItemCount() - 1).mId;
        // now add a lot of items below this and those new views should have span size 3
        enableSpanLookupLogging.set(true);
        mGlm.expectLayout(2);
        mAdapter.addAndNotify(lastPos - 2, 30);
        mGlm.waitForLayout(2);
        checkForMainThreadException();

        assertEquals("last items span count should be queried twice", 2,
                spanLookups.get(lastPos + 30));

    }

    @Test
    public void layoutParams() throws Throwable {
        layoutParamsTest(GridLayoutManager.HORIZONTAL);
        removeRecyclerView();
        layoutParamsTest(GridLayoutManager.VERTICAL);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void horizontalAccessibilitySpanIndices() throws Throwable {
        accessibilitySpanIndicesTest(HORIZONTAL);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public void verticalAccessibilitySpanIndices() throws Throwable {
        accessibilitySpanIndicesTest(VERTICAL);
    }

    public void accessibilitySpanIndicesTest(int orientation) throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, orientation, false));
        waitForFirstLayout(recyclerView);
        final AccessibilityDelegateCompat delegateCompat = mRecyclerView
                .getCompatAccessibilityDelegate().getItemDelegate();
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        final View chosen = recyclerView.getChildAt(recyclerView.getChildCount() - 2);
        final int position = recyclerView.getChildLayoutPosition(chosen);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(chosen, info);
            }
        });
        GridLayoutManager.SpanSizeLookup ssl = mGlm.mSpanSizeLookup;
        AccessibilityNodeInfoCompat.CollectionItemInfoCompat itemInfo = info
                .getCollectionItemInfo();
        assertNotNull(itemInfo);
        assertEquals("result should have span group position",
                ssl.getSpanGroupIndex(position, mGlm.getSpanCount()),
                orientation == HORIZONTAL ? itemInfo.getColumnIndex() : itemInfo.getRowIndex());
        assertEquals("result should have span index",
                ssl.getSpanIndex(position, mGlm.getSpanCount()),
                orientation == HORIZONTAL ? itemInfo.getRowIndex() : itemInfo.getColumnIndex());
        assertEquals("result should have span size",
                ssl.getSpanSize(position),
                orientation == HORIZONTAL ? itemInfo.getRowSpan() : itemInfo.getColumnSpan());
    }

    @Test
    public void rowCountForAccessibility_verticalOrientation() throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 100));
        waitForFirstLayout(recyclerView);

        int count = mGlm.getRowCountForAccessibility(recyclerView.mRecycler,
                recyclerView.mState);

        assertEquals(34, count);
    }

    @Test
    public void rowCountForAccessibility_horizontalOrientation() throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 100));
        mGlm.setOrientation(RecyclerView.HORIZONTAL);
        waitForFirstLayout(recyclerView);

        int count = mGlm.getRowCountForAccessibility(recyclerView.mRecycler,
                recyclerView.mState);

        assertEquals(3, count);
    }

    @Test
    public void rowCountForAccessibility_verticalOrientation_fewerItemsThanSpanCount()
            throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 2));
        waitForFirstLayout(recyclerView);

        int count = mGlm.getRowCountForAccessibility(recyclerView.mRecycler,
                recyclerView.mState);

        assertEquals(1, count);
    }

    @Test
    public void rowCountForAccessibility_horizontalOrientation_fewerItemsThanSpanCount()
            throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 2));
        mGlm.setOrientation(RecyclerView.HORIZONTAL);
        waitForFirstLayout(recyclerView);

        int count = mGlm.getRowCountForAccessibility(recyclerView.mRecycler,
                recyclerView.mState);

        assertEquals(2, count);
    }

    @Test
    public void columnCountForAccessibility_verticalOrientation() throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 100));
        waitForFirstLayout(recyclerView);

        int count = mGlm.getColumnCountForAccessibility(recyclerView.mRecycler,
                recyclerView.mState);

        assertEquals(3, count);
    }

    @Test
    public void columnCountForAccessibility_horizontalOrientation() throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 100));
        mGlm.setOrientation(RecyclerView.HORIZONTAL);
        waitForFirstLayout(recyclerView);

        int count = mGlm.getColumnCountForAccessibility(recyclerView.mRecycler,
                recyclerView.mState);

        assertEquals(34, count);
    }

    @Test
    public void columnCountForAccessibility_verticalOrientation_fewerItemsThanSpanCount()
            throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 2));
        waitForFirstLayout(recyclerView);

        int count = mGlm.getColumnCountForAccessibility(recyclerView.mRecycler,
                recyclerView.mState);

        assertEquals(2, count);
    }

    @Test
    public void columnCountForAccessibility_horizontalOrientation_fewerItemsThanSpanCount()
            throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 2));
        mGlm.setOrientation(RecyclerView.HORIZONTAL);
        waitForFirstLayout(recyclerView);

        int count = mGlm.getColumnCountForAccessibility(recyclerView.mRecycler,
                recyclerView.mState);

        assertEquals(1, count);
    }

    @Test
    public void accessibilityClassName() throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 100));
        waitForFirstLayout(recyclerView);
        final AccessibilityDelegateCompat delegateCompat = mRecyclerView
                .getCompatAccessibilityDelegate();
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mActivityRule.runOnUiThread(
                () -> delegateCompat.onInitializeAccessibilityNodeInfo(recyclerView, info));
        assertEquals(GridView.class.getName(), info.getClassName());
    }

    @Test
    public void onInitializeAccessibilityNodeInfo_addActionScrollToPosition_notAddedWithEmptyList()
            throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 0));
        waitForFirstLayout(recyclerView);

        final AccessibilityNodeInfoCompat nodeInfo = AccessibilityNodeInfoCompat.obtain();
        assertFalse(nodeInfo.getActionList().contains(ACTION_SCROLL_TO_POSITION));
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGlm.onInitializeAccessibilityNodeInfo(nodeInfo);
            }
        });

        assertFalse(nodeInfo.getActionList().contains(ACTION_SCROLL_TO_POSITION));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void onInitializeAccessibilityNodeInfo_addActionScrollToPosition_addedWithNonEmptyList()
            throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 1));
        waitForFirstLayout(recyclerView);

        final AccessibilityNodeInfoCompat nodeInfo = AccessibilityNodeInfoCompat.obtain();
        assertFalse(nodeInfo.getActionList().contains(ACTION_SCROLL_TO_POSITION));
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGlm.onInitializeAccessibilityNodeInfo(nodeInfo);
            }
        });

        assertTrue(nodeInfo.getActionList().contains(ACTION_SCROLL_TO_POSITION));
    }

    @Test
    public void performAccessibilityAction_actionScrollToPosition_withNullArgs_returnsFalse()
            throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 100));
        waitForFirstLayout(recyclerView);

        final boolean[] returnValue = {false};
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                returnValue[0] = mGlm.performAccessibilityAction(
                        android.R.id.accessibilityActionScrollToPosition, null);
            }
        });

        assertFalse(returnValue[0]);
    }

    @Test
    public void performAccessibilityAction_actionScrollToPosition_noRow_returnsFalse()
            throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 100));
        waitForFirstLayout(recyclerView);

        Bundle bundle = new Bundle();
        bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_COLUMN_INT, 10);

        final boolean[] returnValue = {false};
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                returnValue[0] = mGlm.performAccessibilityAction(
                        android.R.id.accessibilityActionScrollToPosition, bundle);
            }
        });

        assertFalse(returnValue[0]);
    }

    @Test
    public void performAccessibilityAction_actionScrollToPosition_noColumn_returnsFalse()
            throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 100));
        waitForFirstLayout(recyclerView);

        Bundle bundle = new Bundle();
        bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_ROW_INT, 10);

        final boolean[] returnValue = {false};
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                returnValue[0] = mGlm.performAccessibilityAction(
                        android.R.id.accessibilityActionScrollToPosition, bundle);
            }
        });

        assertFalse(returnValue[0]);
    }

    @Test
    public void performAccessibilityAction_withValidRowAndColumn_performsScroll() throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 100));
        final GridLayoutManager.SpanSizeLookup spanSizeLookup =
                new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        if (position % 5 == 0) {
                            return 2;
                        }
                        return 1;
                    }
                };

        mGlm.setOrientation(RecyclerView.HORIZONTAL);
        mGlm.setSpanSizeLookup(spanSizeLookup);
        /*
        This generates the following grid, with items 1, 6, 11, etc. (at indices 0, 5, 10, etc.)
        spanning two rows.
        1   3   6   8   11  13  16  etc.
            4       9       14      etc.
        2   5   7   10  12  15  17  etc.
        */
        waitForFirstLayout(recyclerView);
        mGlm.expectLayout(1);

        Bundle bundle = new Bundle();
        bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_ROW_INT, 0);
        bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_COLUMN_INT, 2);

        final boolean[] returnValue = {false};
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                returnValue[0] = recyclerView.getLayoutManager().performAccessibilityAction(
                        android.R.id.accessibilityActionScrollToPosition, bundle);
            }
        });
        mGlm.waitForLayout(2);

        assertTrue(returnValue[0]);
        assertEquals(((TextView) mGlm.getChildAt(0)).getText(), "Item (6)");
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_withoutSpecifyingDirection()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(6, HORIZONTAL);
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(0));
        final boolean[] returnValue = {false};
        mActivityRule.runOnUiThread(
                () -> {
                    returnValue[0] = mRecyclerView.getLayoutManager().performAccessibilityAction(
                            ACTION_SCROLL_IN_DIRECTION.getId(), null);
                });
        assertThat(returnValue[0]).isFalse();
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_withInvalidDirection()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(6, HORIZONTAL);
        setAccessibilityFocus(uiAutomation, mGlm.getChildAt(0));
        runScrollInDirectionAndFail(-1);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_withoutSettingAccessibilityFocus()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        // Return value of this call is not used.
        setUpGridLayoutManagerAccessibilityTest(6, HORIZONTAL);
        runScrollInDirectionAndFail(View.FOCUS_RIGHT);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusRight_vertical_withAvailableTarget()
            throws Throwable {

        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(4, VERTICAL);
        /*
        This generates the following grid:
        1   2   3
        4
        */
        runScrollInDirectionOnMultipleItemsAndSucceed(uiAutomation, View.FOCUS_RIGHT,
                new HashMap<Integer, String>() {{
                    put(0, "Item (2)");
                    put(1, "Item (3)");
                    put(2, "Item (4)");
                }});
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusRight_vertical_withoutAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, VERTICAL);
        /*
        This generates the following grid:
        1   2   3
        4   5
        */
        runScrollInDirectionOnMultipleItemsAndFail(uiAutomation, View.FOCUS_RIGHT,
                Collections.singletonList(4));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusRight_horizontal_withAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, HORIZONTAL);
        /*
        This generates the following grid:
        1   4
        2   5
        3
        */
        runScrollInDirectionOnMultipleItemsAndSucceed(uiAutomation, View.FOCUS_RIGHT,
                new HashMap<Integer, String>() {{
                    put(0, "Item (4)");
                    put(1, "Item (5)");
                    put(3, "Item (2)");
                    put(4, "Item (3)");
                }});
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusRight_horizontal_withoutAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, HORIZONTAL);
        /*
        This generates the following grid:
        1   4
        2   5
        3
        */
        runScrollInDirectionOnMultipleItemsAndFail(uiAutomation, View.FOCUS_RIGHT,
                Collections.singletonList(2));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusLeft_vertical_withAvailableTarget()
            throws Throwable {

        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(4, VERTICAL);
       /*
        This generates the following grid:
        1   2   3
        4
        */
        runScrollInDirectionOnMultipleItemsAndSucceed(uiAutomation, View.FOCUS_LEFT,
                new HashMap<Integer, String>() {{
                    put(1, "Item (1)");
                    put(2, "Item (2)");
                    put(3, "Item (3)");
                }});
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusLeft_vertical_withoutAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(4, VERTICAL);
        /*
        This generates the following grid:
        1   2   3
        4
        */
        runScrollInDirectionOnMultipleItemsAndFail(uiAutomation, View.FOCUS_LEFT,
                Collections.singletonList(0));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusLeft_horizontal_withAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(4, HORIZONTAL);
        /*
        This generates the following grid:
        1   4
        2
        3
        */
        runScrollInDirectionOnMultipleItemsAndSucceed(uiAutomation, View.FOCUS_LEFT,
                new HashMap<Integer, String>() {{
                    put(1, "Item (4)");
                    put(2, "Item (2)");
                    put(3, "Item (1)");
                }});
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusLeft_horizontal_withoutAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(4, HORIZONTAL);
        /*
        This generates the following grid:
        1   4
        2
        3
        */
        runScrollInDirectionOnMultipleItemsAndFail(uiAutomation, View.FOCUS_LEFT,
                Collections.singletonList(0));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusUp_vertical_withAvailableTarget()
            throws Throwable {

        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, VERTICAL);
       /*
        This generates the following grid:
        1   2   3
        4   5
        */
        runScrollInDirectionOnMultipleItemsAndSucceed(uiAutomation, View.FOCUS_UP,
                new HashMap<Integer, String>() {{
                    put(3, "Item (1)");
                    put(4, "Item (2)");
                }});
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusUp_vertical_withoutAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android version.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(5, VERTICAL);
        /*
        This generates the following grid:
        1   2   3
        4   5
        */
        runScrollInDirectionOnMultipleItemsAndFail(uiAutomation, View.FOCUS_UP,
                Arrays.asList(0, 1, 2));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusUp_horizontal_withAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(4, HORIZONTAL);
        /*
        This generates the following grid:
        1   4
        2
        3
        */
        runScrollInDirectionOnMultipleItemsAndSucceed(uiAutomation, View.FOCUS_UP,
                new HashMap<Integer, String>() {{
                    put(1, "Item (1)");
                    put(2, "Item (2)");
                }});
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void performActionScrollInDirection_focusUp_horizontal_withoutAvailableTarget()
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpGridLayoutManagerAccessibilityTest(4, HORIZONTAL);
        /*
        This generates the following grid:
        1   4
        2
        3
        */
        runScrollInDirectionOnMultipleItemsAndFail(uiAutomation, View.FOCUS_UP,
                Arrays.asList(0, 3));
    }

    /**
     * Batch version of {@code runScrollInDirectionAndSucceed}. Sets accessibility focus on each
     * grid child whose index is a key in {@code startingIndexToScrollTargetTextMap} and then runs
     * {@code runScrollInDirectionAndSucceed} in the specified {@code direction}.
     *
     * @param uiAutomation  UiAutomation instance.
     * @param direction The direction of the scroll.
     * @param startingIndexToScrollTargetTextMap Map where each key is the index of a grid
     *                                              child and the corresponding value is the text
     *                                              of the view targeted by the scroll.
     * @throws TimeoutException Exception thrown when an action times out.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void runScrollInDirectionOnMultipleItemsAndSucceed(UiAutomation uiAutomation,
            int direction, Map<Integer, String> startingIndexToScrollTargetTextMap)
            throws TimeoutException {
        for (Map.Entry<Integer, String> entry : startingIndexToScrollTargetTextMap.entrySet()) {
            setAccessibilityFocus(uiAutomation, mGlm.getChildAt(entry.getKey()));
            runScrollInDirectionAndSucceed(uiAutomation, direction, entry.getValue());
        }
    }

    /**
     * Verifies that a scroll successfully occurs in the specified {@code direction}.
     *
     * @param uiAutomation  UiAutomation instance.
     * @param direction The direction of the scroll.
     * @param scrollTargetText The text of the view targeted by the scroll.
     * @throws TimeoutException Exception thrown when an action times out.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void runScrollInDirectionAndSucceed(UiAutomation uiAutomation, int direction,
            String scrollTargetText)
            throws TimeoutException {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final boolean[] returnValue = {false};
        AccessibilityEvent awaitedEvent = uiAutomation.executeAndWaitForEvent(
                () -> mActivityRule.runOnUiThread(() -> {
                    returnValue[0] =
                            mRecyclerView.getLayoutManager().performAccessibilityAction(
                                    ACTION_SCROLL_IN_DIRECTION.getId(),
                                    bundleWithDirectionArg(direction));
                }),
                event -> event.getEventType() == AccessibilityEvent.TYPE_VIEW_TARGETED_BY_SCROLL,
                DEFAULT_ACCESSIBILITY_EVENT_TIMEOUT_MILLIS);

        assertThat(scrollTargetText).isEqualTo(awaitedEvent.getSource().getText());
        assertThat(returnValue[0]).isTrue();
    }

    /**
     * Batch version of {@code runScrollInDirectionAndFail}. Sets accessibility focus on each
     * grid child whose index is a key in {@code startingIndexToScrollTargetTextMap} and then runs
     * {@code runScrollInDirectionAndFail}.
     *
     * @param uiAutomation  UiAutomation instance.
     * @param direction The direction of the scroll.
     * @param startingIndices List where each item is the index of a grid child.
     * @throws TimeoutException Exception thrown when an action times out.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void runScrollInDirectionOnMultipleItemsAndFail(UiAutomation uiAutomation,
            int direction, List<Integer> startingIndices) throws TimeoutException {
        for (Integer index: startingIndices) {
            setAccessibilityFocus(uiAutomation, mGlm.getChildAt(index));
            runScrollInDirectionAndFail(direction);
        }
    }

    /**
     * Verifies that a scroll does not occur in the specified {@code direction}.
     *
     * @param direction The direction of the scroll.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void runScrollInDirectionAndFail(int direction) {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final boolean[] returnValue = {false};

        mActivityRule.runOnUiThread(
                () -> {
                    returnValue[0] = mRecyclerView.getLayoutManager().performAccessibilityAction(
                            ACTION_SCROLL_IN_DIRECTION.getId(), bundleWithDirectionArg(direction));
                });

        assertThat(returnValue[0]).isFalse();
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @NonNull
    private UiAutomation setUpGridLayoutManagerAccessibilityTest(int itemCount, int orientation)
            throws Throwable {
        // TODO(b/267511848): suppress to LOLLIPOP once U constants are finalized and available in
        //  earlier android versions.

        final UiAutomation uiAutomation = setUpAndReturnUiAutomation();
        setUpRecyclerViewAndGridLayoutManager(itemCount, orientation);
        waitForFirstLayout(mRecyclerView);
        return uiAutomation;
    }

    private Bundle bundleWithDirectionArg(int direction) {
        Bundle bundle = new Bundle();
        bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_DIRECTION_INT, direction);
        return bundle;
    }

    @NonNull
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private UiAutomation setUpAndReturnUiAutomation() {
        UiAutomation uiAutomation = getInstrumentation().getUiAutomation();
        final AccessibilityServiceInfo info = uiAutomation.getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        uiAutomation.setServiceInfo(info);
        return uiAutomation;
    }

    private void setAccessibilityFocus(UiAutomation uiAutomation, View source)
            throws TimeoutException {
        AccessibilityEvent awaitedEvent = null;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            awaitedEvent = uiAutomation.executeAndWaitForEvent(
                    () -> {
                        try {
                            mActivityRule.runOnUiThread(() -> source.performAccessibilityAction(
                                    ACTION_ACCESSIBILITY_FOCUS.getId(), null));
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    },
                    event -> event.getEventType()
                            == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,
                    DEFAULT_ACCESSIBILITY_EVENT_TIMEOUT_MILLIS);
            assertThat(awaitedEvent.getSource().isAccessibilityFocused()).isTrue();
        }
    }

    private void setUpRecyclerViewAndGridLayoutManager(int itemCount, int orientation)
            throws Throwable {
        mRecyclerView = setupBasic(new Config(3, itemCount));
        mGlm.setOrientation(orientation);
    }

    public GridLayoutManager.LayoutParams ensureGridLp(View view) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        GridLayoutManager.LayoutParams glp;
        if (lp instanceof GridLayoutManager.LayoutParams) {
            glp = (GridLayoutManager.LayoutParams) lp;
        } else if (lp == null) {
            glp = (GridLayoutManager.LayoutParams) mGlm
                    .generateDefaultLayoutParams();
            view.setLayoutParams(glp);
        } else {
            glp = (GridLayoutManager.LayoutParams) mGlm.generateLayoutParams(lp);
            view.setLayoutParams(glp);
        }
        return glp;
    }

    public void layoutParamsTest(final int orientation) throws Throwable {
        final RecyclerView rv = setupBasic(new Config(3, 100).orientation(orientation),
                new GridTestAdapter(100) {
                    @Override
                    public void onBindViewHolder(@NonNull TestViewHolder holder,
                            int position) {
                        super.onBindViewHolder(holder, position);
                        GridLayoutManager.LayoutParams glp = ensureGridLp(holder.itemView);
                        int val = 0;
                        switch (position % 5) {
                            case 0:
                                val = 10;
                                break;
                            case 1:
                                val = 30;
                                break;
                            case 2:
                                val = GridLayoutManager.LayoutParams.WRAP_CONTENT;
                                break;
                            case 3:
                                val = GridLayoutManager.LayoutParams.MATCH_PARENT;
                                break;
                            case 4:
                                val = 200;
                                break;
                        }
                        if (orientation == GridLayoutManager.VERTICAL) {
                            glp.height = val;
                        } else {
                            glp.width = val;
                        }
                        holder.itemView.setLayoutParams(glp);
                    }
                });
        waitForFirstLayout(rv);
        final OrientationHelper helper = mGlm.mOrientationHelper;
        final int firstRowSize = Math.max(30, getSize(mGlm.findViewByPosition(2)));
        assertEquals(firstRowSize,
                helper.getDecoratedMeasurement(mGlm.findViewByPosition(0)));
        assertEquals(firstRowSize,
                helper.getDecoratedMeasurement(mGlm.findViewByPosition(1)));
        assertEquals(firstRowSize,
                helper.getDecoratedMeasurement(mGlm.findViewByPosition(2)));
        assertEquals(firstRowSize, getSize(mGlm.findViewByPosition(0)));
        assertEquals(firstRowSize, getSize(mGlm.findViewByPosition(1)));
        assertEquals(firstRowSize, getSize(mGlm.findViewByPosition(2)));

        final int secondRowSize = Math.max(200, getSize(mGlm.findViewByPosition(3)));
        assertEquals(secondRowSize,
                helper.getDecoratedMeasurement(mGlm.findViewByPosition(3)));
        assertEquals(secondRowSize,
                helper.getDecoratedMeasurement(mGlm.findViewByPosition(4)));
        assertEquals(secondRowSize,
                helper.getDecoratedMeasurement(mGlm.findViewByPosition(5)));
        assertEquals(secondRowSize, getSize(mGlm.findViewByPosition(3)));
        assertEquals(secondRowSize, getSize(mGlm.findViewByPosition(4)));
        assertEquals(secondRowSize, getSize(mGlm.findViewByPosition(5)));
    }

    @Test
    public void anchorUpdate() throws InterruptedException {
        GridLayoutManager glm = new GridLayoutManager(getActivity(), 11);
        final GridLayoutManager.SpanSizeLookup spanSizeLookup
                = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position > 200) {
                    return 100;
                }
                if (position > 20) {
                    return 2;
                }
                return 1;
            }
        };
        glm.setSpanSizeLookup(spanSizeLookup);
        glm.mAnchorInfo.mPosition = 11;
        RecyclerView.State state = new RecyclerView.State();
        mRecyclerView = new RecyclerView(getActivity());
        state.mItemCount = 1000;
        glm.onAnchorReady(mRecyclerView.mRecycler, state, glm.mAnchorInfo,
                LinearLayoutManager.LayoutState.ITEM_DIRECTION_TAIL);
        assertEquals("gm should keep anchor in first span", 11, glm.mAnchorInfo.mPosition);

        glm.onAnchorReady(mRecyclerView.mRecycler, state, glm.mAnchorInfo,
                LinearLayoutManager.LayoutState.ITEM_DIRECTION_HEAD);
        assertEquals("gm should keep anchor in last span in the row", 20,
                glm.mAnchorInfo.mPosition);

        glm.mAnchorInfo.mPosition = 5;
        glm.onAnchorReady(mRecyclerView.mRecycler, state, glm.mAnchorInfo,
                LinearLayoutManager.LayoutState.ITEM_DIRECTION_HEAD);
        assertEquals("gm should keep anchor in last span in the row", 10,
                glm.mAnchorInfo.mPosition);

        glm.mAnchorInfo.mPosition = 13;
        glm.onAnchorReady(mRecyclerView.mRecycler, state, glm.mAnchorInfo,
                LinearLayoutManager.LayoutState.ITEM_DIRECTION_TAIL);
        assertEquals("gm should move anchor to first span", 11, glm.mAnchorInfo.mPosition);

        glm.onAnchorReady(mRecyclerView.mRecycler, state, glm.mAnchorInfo,
                LinearLayoutManager.LayoutState.ITEM_DIRECTION_HEAD);
        assertEquals("gm should keep anchor in last span in the row", 20,
                glm.mAnchorInfo.mPosition);

        glm.mAnchorInfo.mPosition = 23;
        glm.onAnchorReady(mRecyclerView.mRecycler, state, glm.mAnchorInfo,
                LinearLayoutManager.LayoutState.ITEM_DIRECTION_TAIL);
        assertEquals("gm should move anchor to first span", 21, glm.mAnchorInfo.mPosition);

        glm.onAnchorReady(mRecyclerView.mRecycler, state, glm.mAnchorInfo,
                LinearLayoutManager.LayoutState.ITEM_DIRECTION_HEAD);
        assertEquals("gm should keep anchor in last span in the row", 25,
                glm.mAnchorInfo.mPosition);

        glm.mAnchorInfo.mPosition = 35;
        glm.onAnchorReady(mRecyclerView.mRecycler, state, glm.mAnchorInfo,
                LinearLayoutManager.LayoutState.ITEM_DIRECTION_TAIL);
        assertEquals("gm should move anchor to first span", 31, glm.mAnchorInfo.mPosition);
        glm.onAnchorReady(mRecyclerView.mRecycler, state, glm.mAnchorInfo,
                LinearLayoutManager.LayoutState.ITEM_DIRECTION_HEAD);
        assertEquals("gm should keep anchor in last span in the row", 35,
                glm.mAnchorInfo.mPosition);
    }

    @Test
    public void spanLookup() {
        spanLookupTest(false);
    }

    @Test
    public void spanLookupWithCache() {
        spanLookupTest(true);
    }

    @Test
    public void spanLookupCache() {
        final GridLayoutManager.SpanSizeLookup ssl
                = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position > 6) {
                    return 2;
                }
                return 1;
            }
        };
        ssl.setSpanIndexCacheEnabled(true);
        assertEquals("reference child non existent", -1,
                GridLayoutManager.SpanSizeLookup.findFirstKeyLessThan(ssl.mSpanIndexCache,
                        2));
        ssl.getCachedSpanIndex(4, 5);
        assertEquals("reference child non existent", -1,
                GridLayoutManager.SpanSizeLookup.findFirstKeyLessThan(ssl.mSpanIndexCache,
                        3));
        // this should not happen and if happens, it is better to return -1
        assertEquals("reference child itself", -1,
                GridLayoutManager.SpanSizeLookup.findFirstKeyLessThan(ssl.mSpanIndexCache,
                        4));
        assertEquals("reference child before", 4,
                GridLayoutManager.SpanSizeLookup.findFirstKeyLessThan(ssl.mSpanIndexCache,
                        5));
        assertEquals("reference child before", 4,
                GridLayoutManager.SpanSizeLookup.findFirstKeyLessThan(ssl.mSpanIndexCache,
                        100));
        ssl.getCachedSpanIndex(6, 5);
        assertEquals("reference child before", 6,
                GridLayoutManager.SpanSizeLookup.findFirstKeyLessThan(ssl.mSpanIndexCache,
                        7));
        assertEquals("reference child before", 4,
                GridLayoutManager.SpanSizeLookup.findFirstKeyLessThan(ssl.mSpanIndexCache,
                        6));
        assertEquals("reference child itself", -1,
                GridLayoutManager.SpanSizeLookup.findFirstKeyLessThan(ssl.mSpanIndexCache,
                        4));
        ssl.getCachedSpanIndex(12, 5);
        assertEquals("reference child before", 12,
                GridLayoutManager.SpanSizeLookup.findFirstKeyLessThan(ssl.mSpanIndexCache,
                        13));
        assertEquals("reference child before", 6,
                GridLayoutManager.SpanSizeLookup.findFirstKeyLessThan(ssl.mSpanIndexCache,
                        12));
        assertEquals("reference child before", 6,
                GridLayoutManager.SpanSizeLookup.findFirstKeyLessThan(ssl.mSpanIndexCache,
                        7));
        for (int i = 0; i < 6; i++) {
            ssl.getCachedSpanIndex(i, 5);
        }

        for (int i = 1; i < 7; i++) {
            assertEquals("reference child right before " + i, i - 1,
                    GridLayoutManager.SpanSizeLookup.findFirstKeyLessThan(ssl.mSpanIndexCache,
                            i));
        }
        assertEquals("reference child before 0 ", -1,
                GridLayoutManager.SpanSizeLookup.findFirstKeyLessThan(ssl.mSpanIndexCache,
                        0));
    }

    public void spanLookupTest(boolean enableCache) {
        final GridLayoutManager.SpanSizeLookup ssl
                = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position > 200) {
                    return 100;
                }
                if (position > 6) {
                    return 2;
                }
                return 1;
            }
        };
        ssl.setSpanIndexCacheEnabled(enableCache);
        assertEquals(0, ssl.getCachedSpanIndex(0, 5));
        assertEquals(4, ssl.getCachedSpanIndex(4, 5));
        assertEquals(0, ssl.getCachedSpanIndex(5, 5));
        assertEquals(1, ssl.getCachedSpanIndex(6, 5));
        assertEquals(2, ssl.getCachedSpanIndex(7, 5));
        assertEquals(2, ssl.getCachedSpanIndex(9, 5));
        assertEquals(0, ssl.getCachedSpanIndex(8, 5));
    }

    @Test
    public void removeAnchorItem() throws Throwable {
        removeAnchorItemTest(
                new Config(3, 0).orientation(VERTICAL).reverseLayout(false), 100, 0);
    }

    @Test
    public void removeAnchorItemReverse() throws Throwable {
        removeAnchorItemTest(
                new Config(3, 0).orientation(VERTICAL).reverseLayout(true), 100,
                0);
    }

    @Test
    public void removeAnchorItemHorizontal() throws Throwable {
        removeAnchorItemTest(
                new Config(3, 0).orientation(HORIZONTAL).reverseLayout(
                        false), 100, 0);
    }

    @Test
    public void removeAnchorItemReverseHorizontal() throws Throwable {
        removeAnchorItemTest(
                new Config(3, 0).orientation(HORIZONTAL).reverseLayout(true),
                100, 0);
    }

    /**
     * This tests a regression where predictive animations were not working as expected when the
     * first item is removed and there aren't any more items to add from that direction.
     * First item refers to the default anchor item.
     */
    public void removeAnchorItemTest(final Config config, int adapterSize,
            final int removePos) throws Throwable {
        GridTestAdapter adapter = new GridTestAdapter(adapterSize) {
            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
                if (!(lp instanceof ViewGroup.MarginLayoutParams)) {
                    lp = new ViewGroup.MarginLayoutParams(0, 0);
                    holder.itemView.setLayoutParams(lp);
                }
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                final int maxSize;
                if (config.mOrientation == HORIZONTAL) {
                    maxSize = mRecyclerView.getWidth();
                    mlp.height = ViewGroup.MarginLayoutParams.MATCH_PARENT;
                } else {
                    maxSize = mRecyclerView.getHeight();
                    mlp.width = ViewGroup.MarginLayoutParams.MATCH_PARENT;
                }

                final int desiredSize;
                if (position == removePos) {
                    // make it large
                    desiredSize = maxSize / 4;
                } else {
                    // make it small
                    desiredSize = maxSize / 8;
                }
                if (config.mOrientation == HORIZONTAL) {
                    mlp.width = desiredSize;
                } else {
                    mlp.height = desiredSize;
                }
            }
        };
        RecyclerView recyclerView = setupBasic(config, adapter);
        waitForFirstLayout(recyclerView);
        final int childCount = mGlm.getChildCount();
        RecyclerView.ViewHolder toBeRemoved = null;
        List<RecyclerView.ViewHolder> toBeMoved = new ArrayList<RecyclerView.ViewHolder>();
        for (int i = 0; i < childCount; i++) {
            View child = mGlm.getChildAt(i);
            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(child);
            if (holder.getAbsoluteAdapterPosition() == removePos) {
                toBeRemoved = holder;
            } else {
                toBeMoved.add(holder);
            }
        }
        assertNotNull("Assumption check", toBeRemoved);
        assertEquals("Assumption check", childCount - 1, toBeMoved.size());
        LoggingItemAnimator loggingItemAnimator = new LoggingItemAnimator();
        mRecyclerView.setItemAnimator(loggingItemAnimator);
        loggingItemAnimator.reset();
        loggingItemAnimator.expectRunPendingAnimationsCall(1);
        mGlm.expectLayout(2);
        adapter.deleteAndNotify(removePos, 1);
        mGlm.waitForLayout(1);
        loggingItemAnimator.waitForPendingAnimationsCall(2);
        assertTrue("removed child should receive remove animation",
                loggingItemAnimator.mRemoveVHs.contains(toBeRemoved));
        for (RecyclerView.ViewHolder vh : toBeMoved) {
            assertTrue("view holder should be in moved list",
                    loggingItemAnimator.mMoveVHs.contains(vh));
        }
        List<RecyclerView.ViewHolder> newHolders = new ArrayList<RecyclerView.ViewHolder>();
        for (int i = 0; i < mGlm.getChildCount(); i++) {
            View child = mGlm.getChildAt(i);
            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(child);
            if (toBeRemoved != holder && !toBeMoved.contains(holder)) {
                newHolders.add(holder);
            }
        }
        assertTrue("some new children should show up for the new space", newHolders.size() > 0);
        assertEquals("no items should receive animate add since they are not new", 0,
                loggingItemAnimator.mAddVHs.size());
        for (RecyclerView.ViewHolder holder : newHolders) {
            assertTrue("new holder should receive a move animation",
                    loggingItemAnimator.mMoveVHs.contains(holder));
        }
        // for removed view, 3 for new row
        assertTrue("control against adding too many children due to bad layout state preparation."
                        + " initial:" + childCount + ", current:" + mRecyclerView.getChildCount(),
                mRecyclerView.getChildCount() <= childCount + 1 + 3);
    }

    @Test
    public void spanGroupIndex() {
        final GridLayoutManager.SpanSizeLookup ssl
                = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position > 200) {
                    return 100;
                }
                if (position > 6) {
                    return 2;
                }
                return 1;
            }
        };
        assertEquals(0, ssl.getSpanGroupIndex(0, 5));
        assertEquals(0, ssl.getSpanGroupIndex(4, 5));
        assertEquals(1, ssl.getSpanGroupIndex(5, 5));
        assertEquals(1, ssl.getSpanGroupIndex(6, 5));
        assertEquals(1, ssl.getSpanGroupIndex(7, 5));
        assertEquals(2, ssl.getSpanGroupIndex(9, 5));
        assertEquals(2, ssl.getSpanGroupIndex(8, 5));
    }

    @Test
    public void notifyDataSetChange() throws Throwable {
        final RecyclerView recyclerView = setupBasic(new Config(3, 100));
        final GridLayoutManager.SpanSizeLookup ssl = mGlm.getSpanSizeLookup();
        ssl.setSpanIndexCacheEnabled(true);
        waitForFirstLayout(recyclerView);
        assertTrue("some positions should be cached", ssl.mSpanIndexCache.size() > 0);
        final Callback callback = new Callback() {
            @Override
            public void onBeforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {
                if (!state.isPreLayout()) {
                    assertEquals("cache should be empty", 0, ssl.mSpanIndexCache.size());
                }
            }

            @Override
            public void onAfterLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {
                if (!state.isPreLayout()) {
                    assertTrue("some items should be cached", ssl.mSpanIndexCache.size() > 0);
                }
            }
        };
        mGlm.mCallbacks.add(callback);
        mGlm.expectLayout(2);
        mAdapter.deleteAndNotify(2, 3);
        mGlm.waitForLayout(2);
        checkForMainThreadException();
    }

    @Test
    public void unevenHeights() throws Throwable {
        final Map<Integer, RecyclerView.ViewHolder> viewHolderMap =
                new HashMap<Integer, RecyclerView.ViewHolder>();
        RecyclerView recyclerView = setupBasic(new Config(3, 3), new GridTestAdapter(3) {
            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                final GridLayoutManager.LayoutParams glp = ensureGridLp(holder.itemView);
                glp.height = 50 + position * 50;
                viewHolderMap.put(position, holder);
            }
        });
        waitForFirstLayout(recyclerView);
        for (RecyclerView.ViewHolder vh : viewHolderMap.values()) {
            assertEquals("all items should get max height", 150,
                    vh.itemView.getHeight());
        }

        for (RecyclerView.ViewHolder vh : viewHolderMap.values()) {
            assertEquals("all items should have measured the max height", 150,
                    vh.itemView.getMeasuredHeight());
        }
    }

    @Test
    public void unevenWidths() throws Throwable {
        final Map<Integer, RecyclerView.ViewHolder> viewHolderMap =
                new HashMap<Integer, RecyclerView.ViewHolder>();
        RecyclerView recyclerView = setupBasic(new Config(3, HORIZONTAL, false),
                new GridTestAdapter(3) {
                    @Override
                    public void onBindViewHolder(@NonNull TestViewHolder holder,
                            int position) {
                        super.onBindViewHolder(holder, position);
                        final GridLayoutManager.LayoutParams glp = ensureGridLp(holder.itemView);
                        glp.width = 50 + position * 50;
                        viewHolderMap.put(position, holder);
                    }
                });
        waitForFirstLayout(recyclerView);
        for (RecyclerView.ViewHolder vh : viewHolderMap.values()) {
            assertEquals("all items should get max width", 150,
                    vh.itemView.getWidth());
        }

        for (RecyclerView.ViewHolder vh : viewHolderMap.values()) {
            assertEquals("all items should have measured the max width", 150,
                    vh.itemView.getMeasuredWidth());
        }
    }

    @Test
    public void spanSizeChange() throws Throwable {
        final RecyclerView rv = setupBasic(new Config(3, 100));
        waitForFirstLayout(rv);
        assertTrue(mGlm.supportsPredictiveItemAnimations());
        mGlm.expectLayout(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGlm.setSpanCount(5);
                assertFalse(mGlm.supportsPredictiveItemAnimations());
            }
        });
        mGlm.waitForLayout(2);
        mGlm.expectLayout(2);
        mAdapter.deleteAndNotify(3, 2);
        mGlm.waitForLayout(2);
        assertTrue(mGlm.supportsPredictiveItemAnimations());
    }

    @Test
    public void cacheSpanIndices() throws Throwable {
        final RecyclerView rv = setupBasic(new Config(3, 100));
        mGlm.mSpanSizeLookup.setSpanIndexCacheEnabled(true);
        waitForFirstLayout(rv);
        GridLayoutManager.SpanSizeLookup ssl = mGlm.mSpanSizeLookup;
        assertTrue("cache should be filled", mGlm.mSpanSizeLookup.mSpanIndexCache.size() > 0);
        assertEquals("item index 5 should be in span 2", 2,
                getLp(mGlm.findViewByPosition(5)).getSpanIndex());
        mGlm.expectLayout(2);
        mAdapter.mFullSpanItems.add(4);
        mAdapter.changeAndNotify(4, 1);
        mGlm.waitForLayout(2);
        assertEquals("item index 5 should be in span 2", 0,
                getLp(mGlm.findViewByPosition(5)).getSpanIndex());
    }

    @Test
    public void computeVerticalScrollRange_spansUsedAndGroupIndexesCached_rangeIsConstant()
            throws Throwable {
        int nItems = 100;
        final RecyclerView rv = setupBasic(new Config(2, nItems));
        mGlm.setUsingSpansToEstimateScrollbarDimensions(true);
        mGlm.mSpanSizeLookup.setSpanGroupIndexCacheEnabled(true);
        int[] fullSpanItems = new int[nItems / 2];
        for (int i = 0; i < fullSpanItems.length; i++) {
            fullSpanItems[i] = i;
        }
        mAdapter.setFullSpan(fullSpanItems);
        waitForFirstLayout(rv);

        int constantRange = mGlm.computeVerticalScrollRange(rv.mState);
        assertEquals(0, mGlm.computeVerticalScrollOffset(rv.mState));

        scrollToPosition(nItems - 1);
        mGlm.waitForLayout(2);
        int maxOffset = mGlm.computeVerticalScrollOffset(rv.mState);
        assertEquals(mGlm.computeVerticalScrollRange(rv.mState), constantRange);
        assertEquals(maxOffset + mGlm.computeVerticalScrollExtent(rv.mState), constantRange);
    }

    @Test // reproduces b/179181037
    public void spanCacheWithAnimations() throws Throwable {
        GridTestAdapter adapter = new GridTestAdapter(8, 1);
        adapter.setItemLayoutParams(
                new RecyclerView.LayoutParams(250, 200)
        );
        final RecyclerView rv =  setupBasic(new Config(2, 8), adapter);
        rv.setLayoutParams(new ViewGroup.LayoutParams(500, 500));
        mAdapter.setFullSpan(0);
        waitForFirstLayout(rv);
        assertThat(getPositionToSpanIndexMapping()).containsExactly(
                0, 0,
                1, 0,
                2, 1,
                3, 0,
                4, 1
        );
        // trigger laying out other items and scroll back to 0 to move them to the recycler cache
        smoothScrollToPosition(7);
        smoothScrollToPosition(0);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyItemRemoved(0);
                mAdapter.notifyItemInserted(0);
            }
        });
        waitForAnimations(10);
        assertThat(getPositionToSpanIndexMapping()).containsExactly(
                0, 0,
                1, 0,
                2, 1,
                3, 0,
                4, 1
        );
        smoothScrollToPosition(7);
        // expected layout
        // ---- visible below
        // 3 4
        // 5 6
        // 7
        assertThat(getPositionToSpanIndexMapping()).containsExactly(
                3, 0,
                4, 1,
                5, 0,
                6, 1,
                7, 0
        );
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    /**
     * Returns a map of adapter position -> span index from GLM children
     */
    private Map<Integer, Integer> getPositionToSpanIndexMapping() {
        Map<Integer, Integer> result = new HashMap<>();
        for (int i = 0; i < mGlm.getChildCount(); i++) {
            TestViewHolder viewHolder = (TestViewHolder) mRecyclerView.getChildViewHolder(
                    mGlm.getChildAt(i)
            );
            int adapterPos = viewHolder.getAbsoluteAdapterPosition();
            GridLayoutManager.LayoutParams layoutParams =
                    (GridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams();
            result.put(adapterPos, layoutParams.getSpanIndex());
        }
        return result;
    }

    @Test
    public void getSpanGroupIndex_noCaching() {
        assertGetSpanGroupIndex();
    }


    @Test
    public void getSpanGroupIndex_cacheSpanIndex() {
        mSpanSizeLookupForSpanIndexTest.setSpanIndexCacheEnabled(true);
        assertGetSpanGroupIndex();
    }

    @Test
    public void getSpanGroupIndex_cacheSpanGroupIndex() {
        mSpanSizeLookupForSpanIndexTest.setSpanGroupIndexCacheEnabled(true);
        assertGetSpanGroupIndex();
    }

    @Test
    public void getSpanGroupIndex_cacheAll() {
        mSpanSizeLookupForSpanIndexTest.setSpanGroupIndexCacheEnabled(true);
        mSpanSizeLookupForSpanIndexTest.setSpanIndexCacheEnabled(true);
        assertGetSpanGroupIndex();
    }

    private void assertGetSpanGroupIndex() {
        assertEquals(0, mSpanSizeLookupForSpanIndexTest.getSpanGroupIndex(0, 3));
        assertEquals(0, mSpanSizeLookupForSpanIndexTest.getSpanGroupIndex(1, 3));
        assertEquals(0, mSpanSizeLookupForSpanIndexTest.getSpanGroupIndex(2, 3));
        assertEquals(1, mSpanSizeLookupForSpanIndexTest.getSpanGroupIndex(3, 3));
        assertEquals(2, mSpanSizeLookupForSpanIndexTest.getSpanGroupIndex(4, 3));
        assertEquals(3, mSpanSizeLookupForSpanIndexTest.getSpanGroupIndex(5, 3));
        assertEquals(4, mSpanSizeLookupForSpanIndexTest.getSpanGroupIndex(6, 3));
        assertEquals(5, mSpanSizeLookupForSpanIndexTest.getSpanGroupIndex(7, 3));
        assertEquals(6, mSpanSizeLookupForSpanIndexTest.getSpanGroupIndex(8, 3));
        assertEquals(7, mSpanSizeLookupForSpanIndexTest.getSpanGroupIndex(9, 3));
        assertEquals(8, mSpanSizeLookupForSpanIndexTest.getSpanGroupIndex(10, 3));
        assertEquals(9, mSpanSizeLookupForSpanIndexTest.getSpanGroupIndex(11, 3));
    }

    @Test
    public void getSpanGroupIndex_calledTwiceForSameItemAndCachingOn_internalCalledOnce() {
        final int[] callCount = new int[] {0};
        GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return SPAN_SIZES[position];
            }

            @Override
            public int getSpanGroupIndex(int adapterPosition, int spanCount) {
                callCount[0]++;
                return super.getSpanGroupIndex(adapterPosition, spanCount);
            }
        };
        spanSizeLookup.setSpanGroupIndexCacheEnabled(true);
        spanSizeLookup.getCachedSpanGroupIndex(0, 3);
        spanSizeLookup.getCachedSpanGroupIndex(0, 3);
        assertEquals(1, callCount[0]);
    }

    @Test
    public void computeVerticalScrollValues_isCorrect() throws Throwable {
        assertThatComputeScrollValuesIsCorrect(VERTICAL);
    }

    @Test
    public void computeHorizontalScrollValues_isCorrect() throws Throwable {
        assertThatComputeScrollValuesIsCorrect(HORIZONTAL);
    }

    private void assertThatComputeScrollValuesIsCorrect(@RecyclerView.Orientation int orientation)
            throws Throwable {
        final int spanCount = 2;
        final int itemCount = 100;
        final int childWidth = orientation == VERTICAL ? MATCH_PARENT : 100;
        final int childHeight = orientation == VERTICAL ? 100 : MATCH_PARENT;
        final int rvHeight = orientation == VERTICAL ? childHeight * 4 : MATCH_PARENT;
        final int rvWidth = orientation == VERTICAL ? MATCH_PARENT : childWidth * 4;

        final RecyclerView recyclerView = setupBasic(new Config(spanCount, itemCount)
                        .orientation(orientation),
                new GridTestAdapter(itemCount, 1) {

                    @Override
                    public void onBindViewHolder(@NonNull TestViewHolder holder,
                            int position) {
                        super.onBindViewHolder(holder, position);
                        holder.itemView.setLayoutParams(
                                new RecyclerView.LayoutParams(childWidth, childHeight));
                    }
                });
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(rvWidth, rvHeight));
        mGlm.setUsingSpansToEstimateScrollbarDimensions(true);
        int[] fullSpanItems = new int[itemCount / 2];
        for (int i = 0; i < fullSpanItems.length; i++) {
            fullSpanItems[i] = i;
        }
        int expectedNumberOfRows = itemCount / 2 /* half the rows contain one item */
                + itemCount / 2 / 2; /* the other half of the rows contain two items */
        mAdapter.setFullSpan(fullSpanItems);

        waitForFirstLayout(recyclerView);

        int expectedExtent = orientation == VERTICAL ? rvHeight : rvWidth;
        int childSize = orientation == VERTICAL ? childHeight : childWidth;

        assertEquals(0, getScrollOffset(recyclerView, orientation));
        assertEquals(expectedExtent, getScrollExtent(recyclerView, orientation));
        assertEquals(childSize * expectedNumberOfRows, getScrollRange(recyclerView, orientation));

        scrollToPosition(10);
        mGlm.waitForLayout(2);

        // We scroll to position 10 that means that the first item on the screen is item 7, because
        // there are four items on the screen, so 7,8,9,10.
        assertEquals(childSize * 7, getScrollOffset(recyclerView, orientation));
        assertEquals(expectedExtent, getScrollExtent(recyclerView, orientation));
        assertEquals(childSize * expectedNumberOfRows, getScrollRange(recyclerView, orientation));

        scrollToPosition(itemCount - 1);
        mGlm.waitForLayout(2);

        assertEquals(childSize * (expectedNumberOfRows - 4),
                getScrollOffset(recyclerView, orientation));
        assertEquals(expectedExtent, getScrollExtent(recyclerView, orientation));
        assertEquals(childSize * expectedNumberOfRows,
                mGlm.computeVerticalScrollRange(recyclerView.mState));
    }

    private int getScrollOffset(
            RecyclerView recyclerView,
            @RecyclerView.Orientation int orientation) {
        return orientation == VERTICAL
                ? mGlm.computeVerticalScrollOffset(recyclerView.mState)
                : mGlm.computeHorizontalScrollOffset(recyclerView.mState);

    }

    private int getScrollExtent(
            RecyclerView recyclerView,
            @RecyclerView.Orientation int orientation) {
        return orientation == VERTICAL
                ? mGlm.computeVerticalScrollExtent(recyclerView.mState)
                : mGlm.computeHorizontalScrollExtent(recyclerView.mState);

    }

    private int getScrollRange(
            RecyclerView recyclerView,
            @RecyclerView.Orientation int orientation) {
        return orientation == VERTICAL
                ? mGlm.computeVerticalScrollRange(recyclerView.mState)
                : mGlm.computeHorizontalScrollRange(recyclerView.mState);

    }
}
