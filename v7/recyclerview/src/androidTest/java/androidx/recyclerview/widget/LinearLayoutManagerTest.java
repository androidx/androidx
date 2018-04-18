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

import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.util.Log;
import android.util.StateSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Includes tests for {@link LinearLayoutManager}.
 * <p>
 * Since most UI tests are not practical, these tests are focused on internal data representation
 * and stability of LinearLayoutManager in response to different events (state change, scrolling
 * etc) where it is very hard to do manual testing.
 */
@LargeTest
public class LinearLayoutManagerTest extends BaseLinearLayoutManagerTest {

    /**
     * Tests that the LinearLayoutManager retains the focused element after multiple measure
     * calls to the RecyclerView.  There was a bug where the focused view was lost when the soft
     * keyboard opened.  This test simulates the measure/layout events triggered by the opening
     * of the soft keyboard by making two calls to measure.  A simulation was done because using
     * the soft keyboard in the test caused many issues on API levels 15, 17 and 19.
     */
    @Test
    public void focusedChildStaysInViewWhenRecyclerViewShrinks() throws Throwable {

        // Arrange.

        final RecyclerView recyclerView = inflateWrappedRV();
        ViewGroup.LayoutParams lp = recyclerView.getLayoutParams();
        lp.height = WRAP_CONTENT;
        lp.width = MATCH_PARENT;
        recyclerView.setHasFixedSize(true);

        final FocusableAdapter focusableAdapter =
                new FocusableAdapter(50);
        recyclerView.setAdapter(focusableAdapter);

        mLayoutManager = new WrappedLinearLayoutManager(getActivity(), VERTICAL, false);
        recyclerView.setLayoutManager(mLayoutManager);

        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().getContainer().addView(recyclerView);
            }
        });
        mLayoutManager.waitForLayout(3);

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

        View toFocus = findLastFullyVisibleChild(recyclerView);
        int focusIndex = recyclerView.getChildAdapterPosition(toFocus);

        requestFocus(toFocus, false);

        mLayoutManager.expectLayouts(1);
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
        mLayoutManager.waitForLayout(3);

        // Verify 1.

        assertThat("Child at position " + focusIndex + " should be focused",
                toFocus.hasFocus(), is(true));
        // Testing for partial visibility instead of full visibility since TextView calls
        // requestRectangleOnScreen (inside bringPointIntoView) for the focused view with a rect
        // containing the content area. This rect is guaranteed to be fully visible whereas a
        // portion of TextView could be out of bounds.
        assertThat("Child view at adapter pos " + focusIndex + " should be fully visible.",
                isViewPartiallyInBound(recyclerView, toFocus), is(true));

        // Act 2.

        mLayoutManager.expectLayouts(1);
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
        mLayoutManager.waitForLayout(3);

        // Verify 2.

        assertThat("Child at position " + focusIndex + " should be focused",
                toFocus.hasFocus(), is(true));
        assertTrue("Child view at adapter pos " + focusIndex + " should be fully visible.",
                isViewPartiallyInBound(recyclerView, toFocus));

        // Act 3.

        // Now focus on the first fully visible EditText.
        toFocus = findFirstFullyVisibleChild(recyclerView);
        focusIndex = recyclerView.getChildAdapterPosition(toFocus);

        requestFocus(toFocus, false);

        mLayoutManager.expectLayouts(1);
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
        mLayoutManager.waitForLayout(3);

        // Assert 3.

        assertTrue("Child view at adapter pos " + focusIndex + " should be fully visible.",
                isViewPartiallyInBound(recyclerView, toFocus));
    }

    @Test
    public void topUnfocusableViewsVisibility() throws Throwable {
        // The maximum number of child views that can be visible at any time.
        final int visibleChildCount = 5;
        final int consecutiveFocusablesCount = 2;
        final int consecutiveUnFocusablesCount = 18;
        final TestAdapter adapter = new TestAdapter(
                consecutiveFocusablesCount + consecutiveUnFocusablesCount) {
            RecyclerView mAttachedRv;

            @Override
            public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TestViewHolder testViewHolder = super.onCreateViewHolder(parent, viewType);
                // Good to have colors for debugging
                StateListDrawable stl = new StateListDrawable();
                stl.addState(new int[]{android.R.attr.state_focused},
                        new ColorDrawable(Color.RED));
                stl.addState(StateSet.WILD_CARD, new ColorDrawable(Color.BLUE));
                //noinspection deprecation used to support kitkat tests
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
                if (position < consecutiveFocusablesCount) {
                    holder.itemView.setFocusable(true);
                    holder.itemView.setFocusableInTouchMode(true);
                } else {
                    holder.itemView.setFocusable(false);
                    holder.itemView.setFocusableInTouchMode(false);
                }
                // This height ensures that some portion of #visibleChildCount'th child is
                // off-bounds, creating more interesting test scenario.
                holder.itemView.setMinimumHeight((mAttachedRv.getHeight()
                        + mAttachedRv.getHeight() / (2 * visibleChildCount)) / visibleChildCount);
            }
        };
        setupByConfig(new Config(VERTICAL, false, false).adapter(adapter).reverseLayout(true),
                false);
        waitForFirstLayout();

        // adapter position of the currently focused item.
        int focusIndex = 0;
        View newFocused = mRecyclerView.getChildAt(focusIndex);
        requestFocus(newFocused, true);
        RecyclerView.ViewHolder toFocus = mRecyclerView.findViewHolderForAdapterPosition(
                focusIndex);
        assertThat("Child at position " + focusIndex + " should be focused",
                toFocus.itemView.hasFocus(), is(true));

        // adapter position of the item (whether focusable or not) that just becomes fully
        // visible after focusSearch.
        int visibleIndex = 0;
        // The VH of the above adapter position
        RecyclerView.ViewHolder toVisible = null;

        // Navigate up through the focusable and unfocusable chunks. The focusable items should
        // become focused one by one until hitting the last focusable item, at which point,
        // unfocusable items should become visible on the screen until the currently focused item
        // stays on the screen.
        for (int i = 0; i < adapter.getItemCount(); i++) {
            focusSearch(mRecyclerView.getFocusedChild(), View.FOCUS_UP, true);
            // adapter position of the currently focused item.
            focusIndex = Math.min(consecutiveFocusablesCount - 1, (focusIndex + 1));
            toFocus = mRecyclerView.findViewHolderForAdapterPosition(focusIndex);
            visibleIndex = Math.min(consecutiveFocusablesCount + visibleChildCount - 2,
                    (visibleIndex + 1));
            toVisible = mRecyclerView.findViewHolderForAdapterPosition(visibleIndex);

            assertThat("Child at position " + focusIndex + " should be focused",
                    toFocus.itemView.hasFocus(), is(true));
            assertTrue("Focused child should be at least partially visible.",
                    isViewPartiallyInBound(mRecyclerView, toFocus.itemView));
            assertTrue("Child view at adapter pos " + visibleIndex + " should be fully visible.",
                    isViewFullyInBound(mRecyclerView, toVisible.itemView));
        }
    }

    @Test
    public void bottomUnfocusableViewsVisibility() throws Throwable {
        // The maximum number of child views that can be visible at any time.
        final int visibleChildCount = 5;
        final int consecutiveFocusablesCount = 2;
        final int consecutiveUnFocusablesCount = 18;
        final TestAdapter adapter = new TestAdapter(
                consecutiveFocusablesCount + consecutiveUnFocusablesCount) {
            RecyclerView mAttachedRv;

            @Override
            public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TestViewHolder testViewHolder = super.onCreateViewHolder(parent, viewType);
                // Good to have colors for debugging
                StateListDrawable stl = new StateListDrawable();
                stl.addState(new int[]{android.R.attr.state_focused},
                        new ColorDrawable(Color.RED));
                stl.addState(StateSet.WILD_CARD, new ColorDrawable(Color.BLUE));
                //noinspection deprecation used to support kitkat tests
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
                if (position < consecutiveFocusablesCount) {
                    holder.itemView.setFocusable(true);
                    holder.itemView.setFocusableInTouchMode(true);
                } else {
                    holder.itemView.setFocusable(false);
                    holder.itemView.setFocusableInTouchMode(false);
                }
                // This height ensures that some portion of #visibleChildCount'th child is
                // off-bounds, creating more interesting test scenario.
                holder.itemView.setMinimumHeight((mAttachedRv.getHeight()
                        + mAttachedRv.getHeight() / (2 * visibleChildCount)) / visibleChildCount);
            }
        };
        setupByConfig(new Config(VERTICAL, false, false).adapter(adapter), false);
        waitForFirstLayout();

        // adapter position of the currently focused item.
        int focusIndex = 0;
        View newFocused = mRecyclerView.getChildAt(focusIndex);
        requestFocus(newFocused, true);
        RecyclerView.ViewHolder toFocus = mRecyclerView.findViewHolderForAdapterPosition(
                focusIndex);
        assertThat("Child at position " + focusIndex + " should be focused",
                toFocus.itemView.hasFocus(), is(true));

        // adapter position of the item (whether focusable or not) that just becomes fully
        // visible after focusSearch.
        int visibleIndex = 0;
        // The VH of the above adapter position
        RecyclerView.ViewHolder toVisible = null;

        // Navigate down through the focusable and unfocusable chunks. The focusable items should
        // become focused one by one until hitting the last focusable item, at which point,
        // unfocusable items should become visible on the screen until the currently focused item
        // stays on the screen.
        for (int i = 0; i < adapter.getItemCount(); i++) {
            focusSearch(mRecyclerView.getFocusedChild(), View.FOCUS_DOWN, true);
            // adapter position of the currently focused item.
            focusIndex = Math.min(consecutiveFocusablesCount - 1, (focusIndex + 1));
            toFocus = mRecyclerView.findViewHolderForAdapterPosition(focusIndex);
            visibleIndex = Math.min(consecutiveFocusablesCount + visibleChildCount - 2,
                    (visibleIndex + 1));
            toVisible = mRecyclerView.findViewHolderForAdapterPosition(visibleIndex);

            assertThat("Child at position " + focusIndex + " should be focused",
                    toFocus.itemView.hasFocus(), is(true));
            assertTrue("Focused child should be at least partially visible.",
                    isViewPartiallyInBound(mRecyclerView, toFocus.itemView));
            assertTrue("Child view at adapter pos " + visibleIndex + " should be fully visible.",
                    isViewFullyInBound(mRecyclerView, toVisible.itemView));
        }
    }

    @Test
    public void leftUnfocusableViewsVisibility() throws Throwable {
        // The maximum number of child views that can be visible at any time.
        final int visibleChildCount = 5;
        final int consecutiveFocusablesCount = 2;
        final int consecutiveUnFocusablesCount = 18;
        final TestAdapter adapter = new TestAdapter(
                consecutiveFocusablesCount + consecutiveUnFocusablesCount) {
            RecyclerView mAttachedRv;

            @Override
            public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TestViewHolder testViewHolder = super.onCreateViewHolder(parent, viewType);
                // Good to have colors for debugging
                StateListDrawable stl = new StateListDrawable();
                stl.addState(new int[]{android.R.attr.state_focused},
                        new ColorDrawable(Color.RED));
                stl.addState(StateSet.WILD_CARD, new ColorDrawable(Color.BLUE));
                //noinspection deprecation used to support kitkat tests
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
                if (position < consecutiveFocusablesCount) {
                    holder.itemView.setFocusable(true);
                    holder.itemView.setFocusableInTouchMode(true);
                } else {
                    holder.itemView.setFocusable(false);
                    holder.itemView.setFocusableInTouchMode(false);
                }
                // This width ensures that some portion of #visibleChildCount'th child is
                // off-bounds, creating more interesting test scenario.
                holder.itemView.setMinimumWidth((mAttachedRv.getWidth()
                        + mAttachedRv.getWidth() / (2 * visibleChildCount)) / visibleChildCount);
            }
        };
        setupByConfig(new Config(HORIZONTAL, false, false).adapter(adapter).reverseLayout(true),
                false);
        waitForFirstLayout();

        // adapter position of the currently focused item.
        int focusIndex = 0;
        View newFocused = mRecyclerView.getChildAt(focusIndex);
        requestFocus(newFocused, true);
        RecyclerView.ViewHolder toFocus = mRecyclerView.findViewHolderForAdapterPosition(
                focusIndex);
        assertThat("Child at position " + focusIndex + " should be focused",
                toFocus.itemView.hasFocus(), is(true));

        // adapter position of the item (whether focusable or not) that just becomes fully
        // visible after focusSearch.
        int visibleIndex = 0;
        // The VH of the above adapter position
        RecyclerView.ViewHolder toVisible = null;

        // Navigate left through the focusable and unfocusable chunks. The focusable items should
        // become focused one by one until hitting the last focusable item, at which point,
        // unfocusable items should become visible on the screen until the currently focused item
        // stays on the screen.
        for (int i = 0; i < adapter.getItemCount(); i++) {
            focusSearch(mRecyclerView.getFocusedChild(), View.FOCUS_LEFT, true);
            // adapter position of the currently focused item.
            focusIndex = Math.min(consecutiveFocusablesCount - 1, (focusIndex + 1));
            toFocus = mRecyclerView.findViewHolderForAdapterPosition(focusIndex);
            visibleIndex = Math.min(consecutiveFocusablesCount + visibleChildCount - 2,
                    (visibleIndex + 1));
            toVisible = mRecyclerView.findViewHolderForAdapterPosition(visibleIndex);

            assertThat("Child at position " + focusIndex + " should be focused",
                    toFocus.itemView.hasFocus(), is(true));
            assertTrue("Focused child should be at least partially visible.",
                    isViewPartiallyInBound(mRecyclerView, toFocus.itemView));
            assertTrue("Child view at adapter pos " + visibleIndex + " should be fully visible.",
                    isViewFullyInBound(mRecyclerView, toVisible.itemView));
        }
    }

    @Test
    public void rightUnfocusableViewsVisibility() throws Throwable {
        // The maximum number of child views that can be visible at any time.
        final int visibleChildCount = 5;
        final int consecutiveFocusablesCount = 2;
        final int consecutiveUnFocusablesCount = 18;
        final TestAdapter adapter = new TestAdapter(
                consecutiveFocusablesCount + consecutiveUnFocusablesCount) {
            RecyclerView mAttachedRv;

            @Override
            public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TestViewHolder testViewHolder = super.onCreateViewHolder(parent, viewType);
                // Good to have colors for debugging
                StateListDrawable stl = new StateListDrawable();
                stl.addState(new int[]{android.R.attr.state_focused},
                        new ColorDrawable(Color.RED));
                stl.addState(StateSet.WILD_CARD, new ColorDrawable(Color.BLUE));
                //noinspection deprecation used to support kitkat tests
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
                if (position < consecutiveFocusablesCount) {
                    holder.itemView.setFocusable(true);
                    holder.itemView.setFocusableInTouchMode(true);
                } else {
                    holder.itemView.setFocusable(false);
                    holder.itemView.setFocusableInTouchMode(false);
                }
                // This width ensures that some portion of #visibleChildCount'th child is
                // off-bounds, creating more interesting test scenario.
                holder.itemView.setMinimumWidth((mAttachedRv.getWidth()
                        + mAttachedRv.getWidth() / (2 * visibleChildCount)) / visibleChildCount);
            }
        };
        setupByConfig(new Config(HORIZONTAL, false, false).adapter(adapter), false);
        waitForFirstLayout();

        // adapter position of the currently focused item.
        int focusIndex = 0;
        View newFocused = mRecyclerView.getChildAt(focusIndex);
        requestFocus(newFocused, true);
        RecyclerView.ViewHolder toFocus = mRecyclerView.findViewHolderForAdapterPosition(
                focusIndex);
        assertThat("Child at position " + focusIndex + " should be focused",
                toFocus.itemView.hasFocus(), is(true));

        // adapter position of the item (whether focusable or not) that just becomes fully
        // visible after focusSearch.
        int visibleIndex = 0;
        // The VH of the above adapter position
        RecyclerView.ViewHolder toVisible = null;

        // Navigate right through the focusable and unfocusable chunks. The focusable items should
        // become focused one by one until hitting the last focusable item, at which point,
        // unfocusable items should become visible on the screen until the currently focused item
        // stays on the screen.
        for (int i = 0; i < adapter.getItemCount(); i++) {
            focusSearch(mRecyclerView.getFocusedChild(), View.FOCUS_RIGHT, true);
            // adapter position of the currently focused item.
            focusIndex = Math.min(consecutiveFocusablesCount - 1, (focusIndex + 1));
            toFocus = mRecyclerView.findViewHolderForAdapterPosition(focusIndex);
            visibleIndex = Math.min(consecutiveFocusablesCount + visibleChildCount - 2,
                    (visibleIndex + 1));
            toVisible = mRecyclerView.findViewHolderForAdapterPosition(visibleIndex);

            assertThat("Child at position " + focusIndex + " should be focused",
                    toFocus.itemView.hasFocus(), is(true));
            assertTrue("Focused child should be at least partially visible.",
                    isViewPartiallyInBound(mRecyclerView, toFocus.itemView));
            assertTrue("Child view at adapter pos " + visibleIndex + " should be fully visible.",
                    isViewFullyInBound(mRecyclerView, toVisible.itemView));
        }
    }

    // Run this test on Jelly Bean and newer because clearFocus on API 15 will call
    // requestFocus in ViewRootImpl when clearChildFocus is called. Whereas, in API 16 and above,
    // this call is delayed until after onFocusChange callback is called. Thus on API 16+, there's a
    // transient state of no child having focus during which onFocusChange is executed. This
    // transient state does not exist on API 15-.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
    @Test
    public void unfocusableScrollingWhenFocusCleared() throws Throwable {
        // The maximum number of child views that can be visible at any time.
        final int visibleChildCount = 5;
        final int consecutiveFocusablesCount = 2;
        final int consecutiveUnFocusablesCount = 18;
        final TestAdapter adapter = new TestAdapter(
                consecutiveFocusablesCount + consecutiveUnFocusablesCount) {
            RecyclerView mAttachedRv;

            @Override
            public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TestViewHolder testViewHolder = super.onCreateViewHolder(parent, viewType);
                // Good to have colors for debugging
                StateListDrawable stl = new StateListDrawable();
                stl.addState(new int[]{android.R.attr.state_focused},
                        new ColorDrawable(Color.RED));
                stl.addState(StateSet.WILD_CARD, new ColorDrawable(Color.BLUE));
                //noinspection deprecation used to support kitkat tests
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
                if (position < consecutiveFocusablesCount) {
                    holder.itemView.setFocusable(true);
                    holder.itemView.setFocusableInTouchMode(true);
                } else {
                    holder.itemView.setFocusable(false);
                    holder.itemView.setFocusableInTouchMode(false);
                }
                // This height ensures that some portion of #visibleChildCount'th child is
                // off-bounds, creating more interesting test scenario.
                holder.itemView.setMinimumHeight((mAttachedRv.getHeight()
                        + mAttachedRv.getHeight() / (2 * visibleChildCount)) / visibleChildCount);
            }
        };
        setupByConfig(new Config(VERTICAL, false, false).adapter(adapter), false);
        waitForFirstLayout();

        // adapter position of the currently focused item.
        int focusIndex = 0;
        View newFocused = mRecyclerView.getChildAt(focusIndex);
        requestFocus(newFocused, true);
        RecyclerView.ViewHolder toFocus = mRecyclerView.findViewHolderForAdapterPosition(
                focusIndex);
        assertThat("Child at position " + focusIndex + " should be focused",
                toFocus.itemView.hasFocus(), is(true));

        final View nextView = focusSearch(mRecyclerView.getFocusedChild(), View.FOCUS_DOWN, true);
        focusIndex++;
        assertThat("Child at position " + focusIndex + " should be focused",
                mRecyclerView.findViewHolderForAdapterPosition(focusIndex).itemView.hasFocus(),
                is(true));
        final CountDownLatch focusLatch = new CountDownLatch(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                nextView.setOnFocusChangeListener(new View.OnFocusChangeListener(){
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        assertNull("Focus just got cleared and no children should be holding"
                                + " focus now.", mRecyclerView.getFocusedChild());
                        try {
                            // Calling focusSearch should be a no-op here since even though there
                            // are unfocusable views down to scroll to, none of RV's children hold
                            // focus at this stage.
                            View focusedChild  = focusSearch(v, View.FOCUS_DOWN, true);
                            assertNull("Calling focusSearch should be no-op when no children hold"
                                    + "focus", focusedChild);
                            // No scrolling should have happened, so any unfocusables that were
                            // invisible should still be invisible.
                            RecyclerView.ViewHolder unforcusablePartiallyVisibleChild =
                                    mRecyclerView.findViewHolderForAdapterPosition(
                                            visibleChildCount - 1);
                            assertFalse("Child view at adapter pos " + (visibleChildCount - 1)
                                            + " should not be fully visible.",
                                    isViewFullyInBound(mRecyclerView,
                                            unforcusablePartiallyVisibleChild.itemView));
                        } catch (Throwable t) {
                            postExceptionToInstrumentation(t);
                        }
                    }
                });
                nextView.clearFocus();
                focusLatch.countDown();
            }
        });
        assertTrue(focusLatch.await(2, TimeUnit.SECONDS));
        assertThat("Child at position " + focusIndex + " should no longer be focused",
                mRecyclerView.findViewHolderForAdapterPosition(focusIndex).itemView.hasFocus(),
                is(false));
    }

    @Test
    public void removeAnchorItem() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(VERTICAL).stackFromBottom(false).reverseLayout(
                        false), 100, 0);
    }

    @Test
    public void removeAnchorItemReverse() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(VERTICAL).stackFromBottom(false).reverseLayout(true), 100,
                0);
    }

    @Test
    public void removeAnchorItemStackFromEnd() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(VERTICAL).stackFromBottom(true).reverseLayout(false), 100,
                99);
    }

    @Test
    public void removeAnchorItemStackFromEndAndReverse() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(VERTICAL).stackFromBottom(true).reverseLayout(true), 100,
                99);
    }

    @Test
    public void removeAnchorItemHorizontal() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(HORIZONTAL).stackFromBottom(false).reverseLayout(
                        false), 100, 0);
    }

    @Test
    public void removeAnchorItemReverseHorizontal() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(HORIZONTAL).stackFromBottom(false).reverseLayout(true),
                100, 0);
    }

    @Test
    public void removeAnchorItemStackFromEndHorizontal() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(HORIZONTAL).stackFromBottom(true).reverseLayout(false),
                100, 99);
    }

    @Test
    public void removeAnchorItemStackFromEndAndReverseHorizontal() throws Throwable {
        removeAnchorItemTest(
                new Config().orientation(HORIZONTAL).stackFromBottom(true).reverseLayout(true), 100,
                99);
    }

    /**
     * This tests a regression where predictive animations were not working as expected when the
     * first item is removed and there aren't any more items to add from that direction.
     * First item refers to the default anchor item.
     */
    public void removeAnchorItemTest(final Config config, int adapterSize,
            final int removePos) throws Throwable {
        config.adapter(new TestAdapter(adapterSize) {
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
        });
        setupByConfig(config, true);
        final int childCount = mLayoutManager.getChildCount();
        RecyclerView.ViewHolder toBeRemoved = null;
        List<RecyclerView.ViewHolder> toBeMoved = new ArrayList<RecyclerView.ViewHolder>();
        for (int i = 0; i < childCount; i++) {
            View child = mLayoutManager.getChildAt(i);
            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(child);
            if (holder.getAdapterPosition() == removePos) {
                toBeRemoved = holder;
            } else {
                toBeMoved.add(holder);
            }
        }
        assertNotNull("test sanity", toBeRemoved);
        assertEquals("test sanity", childCount - 1, toBeMoved.size());
        LoggingItemAnimator loggingItemAnimator = new LoggingItemAnimator();
        mRecyclerView.setItemAnimator(loggingItemAnimator);
        loggingItemAnimator.reset();
        loggingItemAnimator.expectRunPendingAnimationsCall(1);
        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(removePos, 1);
        mLayoutManager.waitForLayout(1);
        loggingItemAnimator.waitForPendingAnimationsCall(2);
        assertTrue("removed child should receive remove animation",
                loggingItemAnimator.mRemoveVHs.contains(toBeRemoved));
        for (RecyclerView.ViewHolder vh : toBeMoved) {
            assertTrue("view holder should be in moved list",
                    loggingItemAnimator.mMoveVHs.contains(vh));
        }
        List<RecyclerView.ViewHolder> newHolders = new ArrayList<RecyclerView.ViewHolder>();
        for (int i = 0; i < mLayoutManager.getChildCount(); i++) {
            View child = mLayoutManager.getChildAt(i);
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
        assertTrue("control against adding too many children due to bad layout state preparation."
                        + " initial:" + childCount + ", current:" + mRecyclerView.getChildCount(),
                mRecyclerView.getChildCount() <= childCount + 3 /*1 for removed view, 2 for its size*/);
    }

    void waitOneCycle() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    @Test
    public void hiddenNoneRemoveViewAccessibility() throws Throwable {
        final Config config = new Config();
        int adapterSize = 1000;
        final boolean[] firstItemSpecialSize = new boolean[] {false};
        TestAdapter adapter = new TestAdapter(adapterSize) {
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
                if (position == 0 && firstItemSpecialSize[0]) {
                    desiredSize = maxSize / 3;
                } else {
                    desiredSize = maxSize / 8;
                }
                if (config.mOrientation == HORIZONTAL) {
                    mlp.width = desiredSize;
                } else {
                    mlp.height = desiredSize;
                }
            }

            @Override
            public void onBindViewHolder(TestViewHolder holder,
                    int position, List<Object> payloads) {
                onBindViewHolder(holder, position);
            }
        };
        adapter.setHasStableIds(false);
        config.adapter(adapter);
        setupByConfig(config, true);
        final DummyItemAnimator itemAnimator = new DummyItemAnimator();
        mRecyclerView.setItemAnimator(itemAnimator);

        // push last item out by increasing first item's size
        final int childBeingPushOut = mLayoutManager.getChildCount() - 1;
        RecyclerView.ViewHolder itemViewHolder = mRecyclerView
                .findViewHolderForAdapterPosition(childBeingPushOut);
        final int originalAccessibility = ViewCompat.getImportantForAccessibility(
                itemViewHolder.itemView);
        assertTrue(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO == originalAccessibility
                || ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES == originalAccessibility);

        itemAnimator.expect(DummyItemAnimator.MOVE_START, 1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                firstItemSpecialSize[0] = true;
                mTestAdapter.notifyItemChanged(0, "XXX");
            }
        });
        // wait till itemAnimator starts which will block itemView's accessibility
        itemAnimator.waitFor(DummyItemAnimator.MOVE_START);
        // RV Changes accessiblity after onMoveStart, so wait one more cycle.
        waitOneCycle();
        assertTrue(itemAnimator.getMovesAnimations().contains(itemViewHolder));
        assertEquals(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
                ViewCompat.getImportantForAccessibility(itemViewHolder.itemView));

        // notify Change again to run predictive animation.
        mLayoutManager.expectLayouts(2);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTestAdapter.notifyItemChanged(0, "XXX");
            }
        });
        mLayoutManager.waitForLayout(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                itemAnimator.endAnimations();
            }
        });
        // scroll to the view being pushed out, it should get same view from cache as the item
        // in adapter does not change.
        smoothScrollToPosition(childBeingPushOut);
        RecyclerView.ViewHolder itemViewHolder2 = mRecyclerView
                .findViewHolderForAdapterPosition(childBeingPushOut);
        assertSame(itemViewHolder, itemViewHolder2);
        // the important for accessibility should be reset to YES/AUTO:
        final int newAccessibility = ViewCompat.getImportantForAccessibility(
                itemViewHolder.itemView);
        assertTrue(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO == newAccessibility
                || ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES == newAccessibility);
    }

    @Test
    public void layoutFrozenBug70402422() throws Throwable {
        final Config config = new Config();
        TestAdapter adapter = new TestAdapter(2);
        adapter.setHasStableIds(false);
        config.adapter(adapter);
        setupByConfig(config, true);
        final DummyItemAnimator itemAnimator = new DummyItemAnimator();
        mRecyclerView.setItemAnimator(itemAnimator);

        final View firstItemView = mRecyclerView
                .findViewHolderForAdapterPosition(0).itemView;

        itemAnimator.expect(DummyItemAnimator.REMOVE_START, 1);
        mTestAdapter.deleteAndNotify(1, 1);
        itemAnimator.waitFor(DummyItemAnimator.REMOVE_START);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setLayoutFrozen(true);
            }
        });
        // requestLayout during item animation, which should be eaten by setLayoutFrozen(true)
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                firstItemView.requestLayout();
            }
        });
        assertTrue(firstItemView.isLayoutRequested());
        assertFalse(mRecyclerView.isLayoutRequested());
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                itemAnimator.endAnimations();
            }
        });
        // When setLayoutFrozen(false), the firstItemView should run a layout pass and clear
        // isLayoutRequested() flag.
        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setLayoutFrozen(false);
            }
        });
        mLayoutManager.waitForLayout(1);
        assertFalse(firstItemView.isLayoutRequested());
        assertFalse(mRecyclerView.isLayoutRequested());
    }

    @Test
    public void keepFocusOnRelayout() throws Throwable {
        setupByConfig(new Config(VERTICAL, false, false).itemCount(500), true);
        int center = (mLayoutManager.findLastVisibleItemPosition()
                - mLayoutManager.findFirstVisibleItemPosition()) / 2;
        final RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForLayoutPosition(center);
        final int top = mLayoutManager.mOrientationHelper.getDecoratedStart(vh.itemView);
        requestFocus(vh.itemView, true);
        assertTrue("view should have the focus", vh.itemView.hasFocus());
        // add a bunch of items right before that view, make sure it keeps its position
        mLayoutManager.expectLayouts(2);
        final int childCountToAdd = mRecyclerView.getChildCount() * 2;
        mTestAdapter.addAndNotify(center, childCountToAdd);
        center += childCountToAdd; // offset item
        mLayoutManager.waitForLayout(2);
        mLayoutManager.waitForAnimationsToEnd(20);
        final RecyclerView.ViewHolder postVH = mRecyclerView.findViewHolderForLayoutPosition(center);
        assertNotNull("focused child should stay in layout", postVH);
        assertSame("same view holder should be kept for unchanged child", vh, postVH);
        assertEquals("focused child's screen position should stay unchanged", top,
                mLayoutManager.mOrientationHelper.getDecoratedStart(postVH.itemView));
    }

    @Test
    public void keepFullFocusOnResize() throws Throwable {
        keepFocusOnResizeTest(new Config(VERTICAL, false, false).itemCount(500), true);
    }

    @Test
    public void keepPartialFocusOnResize() throws Throwable {
        keepFocusOnResizeTest(new Config(VERTICAL, false, false).itemCount(500), false);
    }

    @Test
    public void keepReverseFullFocusOnResize() throws Throwable {
        keepFocusOnResizeTest(new Config(VERTICAL, true, false).itemCount(500), true);
    }

    @Test
    public void keepReversePartialFocusOnResize() throws Throwable {
        keepFocusOnResizeTest(new Config(VERTICAL, true, false).itemCount(500), false);
    }

    @Test
    public void keepStackFromEndFullFocusOnResize() throws Throwable {
        keepFocusOnResizeTest(new Config(VERTICAL, false, true).itemCount(500), true);
    }

    @Test
    public void keepStackFromEndPartialFocusOnResize() throws Throwable {
        keepFocusOnResizeTest(new Config(VERTICAL, false, true).itemCount(500), false);
    }

    public void keepFocusOnResizeTest(final Config config, boolean fullyVisible) throws Throwable {
        setupByConfig(config, true);
        final int targetPosition;
        if (config.mStackFromEnd) {
            targetPosition = mLayoutManager.findFirstVisibleItemPosition();
        } else {
            targetPosition = mLayoutManager.findLastVisibleItemPosition();
        }
        final OrientationHelper helper = mLayoutManager.mOrientationHelper;
        final RecyclerView.ViewHolder vh = mRecyclerView
                .findViewHolderForLayoutPosition(targetPosition);

        // scroll enough to offset the child
        int startMargin = helper.getDecoratedStart(vh.itemView) -
                helper.getStartAfterPadding();
        int endMargin = helper.getEndAfterPadding() -
                helper.getDecoratedEnd(vh.itemView);
        Log.d(TAG, "initial start margin " + startMargin + " , end margin:" + endMargin);
        requestFocus(vh.itemView, true);
        assertTrue("view should gain the focus", vh.itemView.hasFocus());
        // scroll enough to offset the child
        startMargin = helper.getDecoratedStart(vh.itemView) -
                helper.getStartAfterPadding();
        endMargin = helper.getEndAfterPadding() -
                helper.getDecoratedEnd(vh.itemView);

        Log.d(TAG, "start margin " + startMargin + " , end margin:" + endMargin);
        assertTrue("View should become fully visible", startMargin >= 0 && endMargin >= 0);

        int expectedOffset = 0;
        boolean offsetAtStart = false;
        if (!fullyVisible) {
            // move it a bit such that it is no more fully visible
            final int childSize = helper
                    .getDecoratedMeasurement(vh.itemView);
            expectedOffset = childSize / 3;
            if (startMargin < endMargin) {
                scrollBy(expectedOffset);
                offsetAtStart = true;
            } else {
                scrollBy(-expectedOffset);
                offsetAtStart = false;
            }
            startMargin = helper.getDecoratedStart(vh.itemView) -
                    helper.getStartAfterPadding();
            endMargin = helper.getEndAfterPadding() -
                    helper.getDecoratedEnd(vh.itemView);
            assertTrue("test sanity, view should not be fully visible", startMargin < 0
                    || endMargin < 0);
        }

        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ViewGroup.LayoutParams layoutParams = mRecyclerView.getLayoutParams();
                if (config.mOrientation == HORIZONTAL) {
                    layoutParams.width = mRecyclerView.getWidth() / 2;
                } else {
                    layoutParams.height = mRecyclerView.getHeight() / 2;
                }
                mRecyclerView.setLayoutParams(layoutParams);
            }
        });
        Thread.sleep(100);
        // add a bunch of items right before that view, make sure it keeps its position
        mLayoutManager.waitForLayout(2);
        mLayoutManager.waitForAnimationsToEnd(20);
        assertTrue("view should preserve the focus", vh.itemView.hasFocus());
        final RecyclerView.ViewHolder postVH = mRecyclerView
                .findViewHolderForLayoutPosition(targetPosition);
        assertNotNull("focused child should stay in layout", postVH);
        assertSame("same view holder should be kept for unchanged child", vh, postVH);
        View focused = postVH.itemView;

        startMargin = helper.getDecoratedStart(focused) - helper.getStartAfterPadding();
        endMargin = helper.getEndAfterPadding() - helper.getDecoratedEnd(focused);

        assertTrue("focused child should be somewhat visible",
                helper.getDecoratedStart(focused) < helper.getEndAfterPadding()
                        && helper.getDecoratedEnd(focused) > helper.getStartAfterPadding());
        if (fullyVisible) {
            assertTrue("focused child end should stay fully visible",
                    endMargin >= 0);
            assertTrue("focused child start should stay fully visible",
                    startMargin >= 0);
        } else {
            if (offsetAtStart) {
                assertTrue("start should preserve its offset", startMargin < 0);
                assertTrue("end should be visible", endMargin >= 0);
            } else {
                assertTrue("end should preserve its offset", endMargin < 0);
                assertTrue("start should be visible", startMargin >= 0);
            }
        }
    }

    @Test
    public void scrollToPositionWithPredictive() throws Throwable {
        scrollToPositionWithPredictive(0, LinearLayoutManager.INVALID_OFFSET);
        removeRecyclerView();
        scrollToPositionWithPredictive(3, 20);
        removeRecyclerView();
        scrollToPositionWithPredictive(Config.DEFAULT_ITEM_COUNT / 2,
                LinearLayoutManager.INVALID_OFFSET);
        removeRecyclerView();
        scrollToPositionWithPredictive(Config.DEFAULT_ITEM_COUNT / 2, 10);
    }

    @Test
    public void recycleDuringAnimations() throws Throwable {
        final AtomicInteger childCount = new AtomicInteger(0);
        final TestAdapter adapter = new TestAdapter(300) {
            @Override
            public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                    int viewType) {
                final int cnt = childCount.incrementAndGet();
                final TestViewHolder testViewHolder = super.onCreateViewHolder(parent, viewType);
                if (DEBUG) {
                    Log.d(TAG, "CHILD_CNT(create):" + cnt + ", " + testViewHolder);
                }
                return testViewHolder;
            }
        };
        setupByConfig(new Config(VERTICAL, false, false).itemCount(300)
                .adapter(adapter), true);

        final RecyclerView.RecycledViewPool pool = new RecyclerView.RecycledViewPool() {
            @Override
            public void putRecycledView(RecyclerView.ViewHolder scrap) {
                super.putRecycledView(scrap);
                int cnt = childCount.decrementAndGet();
                if (DEBUG) {
                    Log.d(TAG, "CHILD_CNT(put):" + cnt + ", " + scrap);
                }
            }

            @Override
            public RecyclerView.ViewHolder getRecycledView(int viewType) {
                final RecyclerView.ViewHolder recycledView = super.getRecycledView(viewType);
                if (recycledView != null) {
                    final int cnt = childCount.incrementAndGet();
                    if (DEBUG) {
                        Log.d(TAG, "CHILD_CNT(get):" + cnt + ", " + recycledView);
                    }
                }
                return recycledView;
            }
        };
        pool.setMaxRecycledViews(mTestAdapter.getItemViewType(0), 500);
        mRecyclerView.setRecycledViewPool(pool);


        // now keep adding children to trigger more children being created etc.
        for (int i = 0; i < 100; i ++) {
            adapter.addAndNotify(15, 1);
            Thread.sleep(15);
        }
        getInstrumentation().waitForIdleSync();
        waitForAnimations(2);
        assertEquals("Children count should add up", childCount.get(),
                mRecyclerView.getChildCount() + mRecyclerView.mRecycler.mCachedViews.size());

        // now trigger lots of add again, followed by a scroll to position
        for (int i = 0; i < 100; i ++) {
            adapter.addAndNotify(5 + (i % 3) * 3, 1);
            Thread.sleep(25);
        }

        final AtomicInteger lastVisiblePosition = new AtomicInteger();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lastVisiblePosition.set(mLayoutManager.findLastVisibleItemPosition());
            }
        });

        smoothScrollToPosition(lastVisiblePosition.get() + 20);
        waitForAnimations(2);
        getInstrumentation().waitForIdleSync();
        assertEquals("Children count should add up", childCount.get(),
                mRecyclerView.getChildCount() + mRecyclerView.mRecycler.mCachedViews.size());
    }


    @Test
    public void dontRecycleChildrenOnDetach() throws Throwable {
        setupByConfig(new Config().recycleChildrenOnDetach(false), true);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int recyclerSize = mRecyclerView.mRecycler.getRecycledViewPool().size();
                ((ViewGroup)mRecyclerView.getParent()).removeView(mRecyclerView);
                assertEquals("No views are recycled", recyclerSize,
                        mRecyclerView.mRecycler.getRecycledViewPool().size());
            }
        });
    }

    @Test
    public void recycleChildrenOnDetach() throws Throwable {
        setupByConfig(new Config().recycleChildrenOnDetach(true), true);
        final int childCount = mLayoutManager.getChildCount();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int recyclerSize = mRecyclerView.mRecycler.getRecycledViewPool().size();
                mRecyclerView.mRecycler.getRecycledViewPool().setMaxRecycledViews(
                        mTestAdapter.getItemViewType(0), recyclerSize + childCount);
                ((ViewGroup)mRecyclerView.getParent()).removeView(mRecyclerView);
                assertEquals("All children should be recycled", childCount + recyclerSize,
                        mRecyclerView.mRecycler.getRecycledViewPool().size());
            }
        });
    }

    @Test
    public void scrollAndClear() throws Throwable {
        setupByConfig(new Config(), true);

        assertTrue("Children not laid out", mLayoutManager.collectChildCoordinates().size() > 0);

        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLayoutManager.scrollToPositionWithOffset(1, 0);
                mTestAdapter.clearOnUIThread();
            }
        });
        mLayoutManager.waitForLayout(2);

        assertEquals("Remaining children", 0, mLayoutManager.collectChildCoordinates().size());
    }


    @Test
    public void accessibilityPositions() throws Throwable {
        setupByConfig(new Config(VERTICAL, false, false), true);
        final AccessibilityDelegateCompat delegateCompat = mRecyclerView
                .getCompatAccessibilityDelegate();
        final AccessibilityEvent event = AccessibilityEvent.obtain();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityEvent(mRecyclerView, event);
            }
        });
        assertEquals("result should have first position",
                event.getFromIndex(),
                mLayoutManager.findFirstVisibleItemPosition());
        assertEquals("result should have last position",
                event.getToIndex(),
                mLayoutManager.findLastVisibleItemPosition());
    }
}
