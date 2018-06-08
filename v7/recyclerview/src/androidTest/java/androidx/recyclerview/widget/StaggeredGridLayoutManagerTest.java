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

import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;
import static androidx.recyclerview.widget.StaggeredGridLayoutManager
        .GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS;
import static androidx.recyclerview.widget.StaggeredGridLayoutManager.GAP_HANDLING_NONE;
import static androidx.recyclerview.widget.StaggeredGridLayoutManager.HORIZONTAL;
import static androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.test.filters.LargeTest;
import android.text.TextUtils;
import android.util.Log;
import android.util.StateSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.AccessibilityDelegateCompat;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LargeTest
public class StaggeredGridLayoutManagerTest extends BaseStaggeredGridLayoutManagerTest {

    @Test
    public void layout_rvHasPaddingChildIsMatchParentVertical_childrenAreInsideParent()
            throws Throwable {
        layout_rvHasPaddingChildIsMatchParent_childrenAreInsideParent(VERTICAL, false);
    }

    @Test
    public void layout_rvHasPaddingChildIsMatchParentHorizontal_childrenAreInsideParent()
            throws Throwable {
        layout_rvHasPaddingChildIsMatchParent_childrenAreInsideParent(HORIZONTAL, false);
    }

    @Test
    public void layout_rvHasPaddingChildIsMatchParentVerticalFullSpan_childrenAreInsideParent()
            throws Throwable {
        layout_rvHasPaddingChildIsMatchParent_childrenAreInsideParent(VERTICAL, true);
    }

    @Test
    public void layout_rvHasPaddingChildIsMatchParentHorizontalFullSpan_childrenAreInsideParent()
            throws Throwable {
        layout_rvHasPaddingChildIsMatchParent_childrenAreInsideParent(HORIZONTAL, true);
    }

    private void layout_rvHasPaddingChildIsMatchParent_childrenAreInsideParent(
            final int orientation, final boolean fullSpan)
            throws Throwable {

        setupByConfig(new Config(orientation, false, 1, GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS),
                new GridTestAdapter(10, orientation) {

                    @NonNull
                    @Override
                    public TestViewHolder onCreateViewHolder(
                            @NonNull ViewGroup parent, int viewType) {
                        View view = new View(parent.getContext());
                        StaggeredGridLayoutManager.LayoutParams layoutParams =
                                new StaggeredGridLayoutManager.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT);
                        layoutParams.setFullSpan(fullSpan);
                        view.setLayoutParams(layoutParams);
                        return new TestViewHolder(view);
                    }

                    @Override
                    public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
                        // No actual binding needed, but we need to override this to prevent default
                        // behavior of GridTestAdapter.
                    }
                });
        mRecyclerView.setPadding(1, 2, 3, 4);

        waitFirstLayout();

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int childDimension;
                int recyclerViewDimensionMinusPadding;
                if (orientation == VERTICAL) {
                    childDimension = mRecyclerView.getChildAt(0).getHeight();
                    recyclerViewDimensionMinusPadding = mRecyclerView.getHeight()
                            - mRecyclerView.getPaddingTop()
                            - mRecyclerView.getPaddingBottom();
                } else {
                    childDimension = mRecyclerView.getChildAt(0).getWidth();
                    recyclerViewDimensionMinusPadding = mRecyclerView.getWidth()
                            - mRecyclerView.getPaddingLeft()
                            - mRecyclerView.getPaddingRight();
                }
                assertThat(childDimension, equalTo(recyclerViewDimensionMinusPadding));
            }
        });
    }

    @Test
    public void forceLayoutOnDetach() throws Throwable {
        setupByConfig(new Config(VERTICAL, false, 3, GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS));
        waitFirstLayout();
        assertFalse("test sanity", mRecyclerView.isLayoutRequested());
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLayoutManager.onDetachedFromWindow(mRecyclerView, mRecyclerView.mRecycler);
            }
        });
        assertTrue(mRecyclerView.isLayoutRequested());
    }

    @Test
    public void areAllStartsTheSame() throws Throwable {
        setupByConfig(new Config(VERTICAL, false, 3, GAP_HANDLING_NONE).itemCount(300));
        waitFirstLayout();
        smoothScrollToPosition(100);
        mLayoutManager.expectLayouts(1);
        mAdapter.deleteAndNotify(0, 2);
        mLayoutManager.waitForLayout(2000);
        smoothScrollToPosition(0);
        assertFalse("all starts should not be the same", mLayoutManager.areAllStartsEqual());
    }

    @Test
    public void areAllEndsTheSame() throws Throwable {
        setupByConfig(new Config(VERTICAL, true, 3, GAP_HANDLING_NONE).itemCount(300));
        waitFirstLayout();
        smoothScrollToPosition(100);
        mLayoutManager.expectLayouts(1);
        mAdapter.deleteAndNotify(0, 2);
        mLayoutManager.waitForLayout(2);
        smoothScrollToPosition(0);
        assertFalse("all ends should not be the same", mLayoutManager.areAllEndsEqual());
    }

    @Test
    public void getPositionsBeforeInitialization() throws Throwable {
        setupByConfig(new Config(VERTICAL, false, 3, GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS));
        int[] positions = mLayoutManager.findFirstCompletelyVisibleItemPositions(null);
        MatcherAssert.assertThat(positions,
                CoreMatchers.is(new int[]{RecyclerView.NO_POSITION, RecyclerView.NO_POSITION,
                        RecyclerView.NO_POSITION}));
    }

    @Test
    public void findLastInUnevenDistribution() throws Throwable {
        setupByConfig(new Config(VERTICAL, false, 2, GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS)
                .itemCount(5));
        mAdapter.mOnBindCallback = new OnBindCallback() {
            @Override
            void onBoundItem(TestViewHolder vh, int position) {
                LayoutParams lp = (LayoutParams) vh.itemView.getLayoutParams();
                if (position == 1) {
                    lp.height = mRecyclerView.getHeight() - 10;
                } else {
                    lp.height = 5;
                }
                vh.itemView.setMinimumHeight(0);
            }
        };
        waitFirstLayout();
        int[] into = new int[2];
        mLayoutManager.findFirstCompletelyVisibleItemPositions(into);
        assertEquals("first completely visible item from span 0 should be 0", 0, into[0]);
        assertEquals("first completely visible item from span 1 should be 1", 1, into[1]);
        mLayoutManager.findLastCompletelyVisibleItemPositions(into);
        assertEquals("last completely visible item from span 0 should be 4", 4, into[0]);
        assertEquals("last completely visible item from span 1 should be 1", 1, into[1]);
        assertEquals("first fully visible child should be at position",
                0, mRecyclerView.getChildViewHolder(mLayoutManager.
                        findFirstVisibleItemClosestToStart(true)).getPosition());
        assertEquals("last fully visible child should be at position",
                4, mRecyclerView.getChildViewHolder(mLayoutManager.
                        findFirstVisibleItemClosestToEnd(true)).getPosition());

        assertEquals("first visible child should be at position",
                0, mRecyclerView.getChildViewHolder(mLayoutManager.
                        findFirstVisibleItemClosestToStart(false)).getPosition());
        assertEquals("last visible child should be at position",
                4, mRecyclerView.getChildViewHolder(mLayoutManager.
                        findFirstVisibleItemClosestToEnd(false)).getPosition());

    }

    @Test
    public void customWidthInHorizontal() throws Throwable {
        customSizeInScrollDirectionTest(
                new Config(HORIZONTAL, false, 3, GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS));
    }

    @Test
    public void customHeightInVertical() throws Throwable {
        customSizeInScrollDirectionTest(
                new Config(VERTICAL, false, 3, GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS));
    }

    public void customSizeInScrollDirectionTest(final Config config) throws Throwable {
        setupByConfig(config);
        final Map<View, Integer> sizeMap = new HashMap<View, Integer>();
        mAdapter.mOnBindCallback = new OnBindCallback() {
            @Override
            void onBoundItem(TestViewHolder vh, int position) {
                final ViewGroup.LayoutParams layoutParams = vh.itemView.getLayoutParams();
                final int size = 1 + position * 5;
                if (config.mOrientation == HORIZONTAL) {
                    layoutParams.width = size;
                } else {
                    layoutParams.height = size;
                }
                sizeMap.put(vh.itemView, size);
                if (position == 3) {
                    getLp(vh.itemView).setFullSpan(true);
                }
            }

            @Override
            boolean assignRandomSize() {
                return false;
            }
        };
        waitFirstLayout();
        assertTrue("[test sanity] some views should be laid out", sizeMap.size() > 0);
        for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
            View child = mRecyclerView.getChildAt(i);
            final int size = config.mOrientation == HORIZONTAL ? child.getWidth()
                    : child.getHeight();
            assertEquals("child " + i + " should have the size specified in its layout params",
                    sizeMap.get(child).intValue(), size);
        }
        checkForMainThreadException();
    }

    @Test
    public void gapHandlingWhenItemMovesToTop() throws Throwable {
        gapHandlingWhenItemMovesToTopTest();
    }

    @Test
    public void gapHandlingWhenItemMovesToTopWithFullSpan() throws Throwable {
        gapHandlingWhenItemMovesToTopTest(0);
    }

    @Test
    public void gapHandlingWhenItemMovesToTopWithFullSpan2() throws Throwable {
        gapHandlingWhenItemMovesToTopTest(1);
    }

    public void gapHandlingWhenItemMovesToTopTest(int... fullSpanIndices) throws Throwable {
        Config config = new Config(VERTICAL, false, 2, GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        config.itemCount(3);
        setupByConfig(config);
        mAdapter.mOnBindCallback = new OnBindCallback() {
            @Override
            void onBoundItem(TestViewHolder vh, int position) {
            }

            @Override
            boolean assignRandomSize() {
                return false;
            }
        };
        for (int i : fullSpanIndices) {
            mAdapter.mFullSpanItems.add(i);
        }
        waitFirstLayout();
        mLayoutManager.expectLayouts(1);
        mAdapter.moveItem(1, 0, true);
        mLayoutManager.waitForLayout(2);
        final Map<Item, Rect> desiredPositions = mLayoutManager.collectChildCoordinates();
        // move back.
        mLayoutManager.expectLayouts(1);
        mAdapter.moveItem(0, 1, true);
        mLayoutManager.waitForLayout(2);
        mLayoutManager.expectLayouts(2);
        mAdapter.moveAndNotify(1, 0);
        mLayoutManager.waitForLayout(2);
        Thread.sleep(1000);
        getInstrumentation().waitForIdleSync();
        checkForMainThreadException();
        // item should be positioned properly
        assertRectSetsEqual("final position after a move", desiredPositions,
                mLayoutManager.collectChildCoordinates());

    }

    @Test
    public void focusSearchFailureUp() throws Throwable {
        focusSearchFailure(false);
    }

    @Test
    public void focusSearchFailureDown() throws Throwable {
        focusSearchFailure(true);
    }

    @Test
    public void focusSearchFailureFromSubChild() throws Throwable {
        setupByConfig(new Config(VERTICAL, false, 3, GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS),
                new GridTestAdapter(1000, VERTICAL) {

                    @NonNull
                    @Override
                    public TestViewHolder onCreateViewHolder(
                            @NonNull ViewGroup parent, int viewType) {
                        FrameLayout fl = new FrameLayout(parent.getContext());
                        EditText editText = new EditText(parent.getContext());
                        fl.addView(editText);
                        editText.setEllipsize(TextUtils.TruncateAt.END);
                        return new TestViewHolder(fl);
                    }

                    @Override
                    public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
                        Item item = mItems.get(position);
                        holder.mBoundItem = item;
                        ((EditText) ((FrameLayout) holder.itemView).getChildAt(0)).setText(
                                item.mText + " (" + item.mId + ")");
                        // Good to have colors for debugging
                        StateListDrawable stl = new StateListDrawable();
                        stl.addState(new int[]{android.R.attr.state_focused},
                                new ColorDrawable(Color.RED));
                        stl.addState(StateSet.WILD_CARD, new ColorDrawable(Color.BLUE));
                        //noinspection deprecation using this for kitkat tests
                        holder.itemView.setBackgroundDrawable(stl);
                        if (mOnBindCallback != null) {
                            mOnBindCallback.onBoundItem(holder, position);
                        }
                    }
                });
        mLayoutManager.expectLayouts(1);
        setRecyclerView(mRecyclerView);
        mLayoutManager.waitForLayout(10);
        getInstrumentation().waitForIdleSync();
        ViewGroup lastChild = (ViewGroup) mRecyclerView.getChildAt(
                mRecyclerView.getChildCount() - 1);
        RecyclerView.ViewHolder lastViewHolder = mRecyclerView.getChildViewHolder(lastChild);
        View subChildToFocus = lastChild.getChildAt(0);
        requestFocus(subChildToFocus, true);
        assertThat("test sanity", subChildToFocus.isFocused(), CoreMatchers.is(true));
        focusSearch(subChildToFocus, View.FOCUS_FORWARD);
        waitForIdleScroll(mRecyclerView);
        checkForMainThreadException();
        View focusedChild = mRecyclerView.getFocusedChild();
        if (focusedChild == subChildToFocus.getParent()) {
            focusSearch(focusedChild, View.FOCUS_FORWARD);
            waitForIdleScroll(mRecyclerView);
            focusedChild = mRecyclerView.getFocusedChild();
        }
        RecyclerView.ViewHolder containingViewHolder = mRecyclerView.findContainingViewHolder(
                focusedChild);
        assertTrue("new focused view should have a larger position "
                        + lastViewHolder.getAdapterPosition() + " vs "
                        + containingViewHolder.getAdapterPosition(),
                lastViewHolder.getAdapterPosition() < containingViewHolder.getAdapterPosition());
    }

    public void focusSearchFailure(boolean scrollDown) throws Throwable {
        int focusDir = scrollDown ? View.FOCUS_DOWN : View.FOCUS_UP;
        setupByConfig(new Config(VERTICAL, !scrollDown, 3, GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS)
                , new GridTestAdapter(31, 1) {
                    RecyclerView mAttachedRv;

                    @Override
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
                        holder.itemView.setMinimumHeight(mAttachedRv.getHeight() / 3);
                    }
                });
        /**
         * 0  1  2
         * 3  4  5
         * 6  7  8
         * 9  10 11
         * 12 13 14
         * 15 16 17
         * 18 18 18
         * 19
         * 20 20 20
         * 21 22
         * 23 23 23
         * 24 25 26
         * 27 28 29
         * 30
         */
        mAdapter.mFullSpanItems.add(18);
        mAdapter.mFullSpanItems.add(20);
        mAdapter.mFullSpanItems.add(23);
        waitFirstLayout();
        View viewToFocus = mRecyclerView.findViewHolderForAdapterPosition(1).itemView;
        assertTrue(requestFocus(viewToFocus, true));
        assertSame(viewToFocus, mRecyclerView.getFocusedChild());
        int pos = 1;
        View focusedView = viewToFocus;
        while (pos < 16) {
            focusSearchAndWaitForScroll(focusedView, focusDir);
            focusedView = mRecyclerView.getFocusedChild();
            assertEquals(pos + 3,
                    mRecyclerView.getChildViewHolder(focusedView).getAdapterPosition());
            pos += 3;
        }
        for (int i : new int[]{18, 19, 20, 21, 23, 24}) {
            focusSearchAndWaitForScroll(focusedView, focusDir);
            focusedView = mRecyclerView.getFocusedChild();
            assertEquals(i, mRecyclerView.getChildViewHolder(focusedView).getAdapterPosition());
        }
        // now move right
        focusSearch(focusedView, View.FOCUS_RIGHT);
        waitForIdleScroll(mRecyclerView);
        focusedView = mRecyclerView.getFocusedChild();
        assertEquals(25, mRecyclerView.getChildViewHolder(focusedView).getAdapterPosition());
        for (int i : new int[]{28, 30}) {
            focusSearchAndWaitForScroll(focusedView, focusDir);
            focusedView = mRecyclerView.getFocusedChild();
            assertEquals(i, mRecyclerView.getChildViewHolder(focusedView).getAdapterPosition());
        }
    }

    private void focusSearchAndWaitForScroll(View focused, int dir) throws Throwable {
        focusSearch(focused, dir);
        waitForIdleScroll(mRecyclerView);
    }

    @Test
    public void topUnfocusableViewsVisibility() throws Throwable {
        // The maximum number of rows that can be fully in-bounds of RV.
        final int visibleRowCount = 5;
        final int spanCount = 3;
        final int lastFocusableIndex = 6;

        setupByConfig(new Config(VERTICAL, true, spanCount, GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS),
                new GridTestAdapter(18, 1) {
                    RecyclerView mAttachedRv;

                    @Override
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
                        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.itemView
                                .getLayoutParams();
                        if (position <= lastFocusableIndex) {
                            holder.itemView.setFocusable(true);
                            holder.itemView.setFocusableInTouchMode(true);
                        } else {
                            holder.itemView.setFocusable(false);
                            holder.itemView.setFocusableInTouchMode(false);
                        }
                        holder.itemView.setMinimumHeight(mAttachedRv.getHeight() / visibleRowCount);
                        lp.topMargin = 0;
                        lp.leftMargin = 0;
                        lp.rightMargin = 0;
                        lp.bottomMargin = 0;
                        if (position == 11) {
                            lp.bottomMargin = 9;
                        }
                    }
                });

        /**
         *
         * 15 16 17
         * 12 13 14
         * 11 11 11
         * 9 10
         * 8 8 8
         * 7
         * 6 6 6
         * 3 4 5
         * 0 1 2
         */
        mAdapter.mFullSpanItems.add(6);
        mAdapter.mFullSpanItems.add(8);
        mAdapter.mFullSpanItems.add(11);
        waitFirstLayout();


        // adapter position of the currently focused item.
        int focusIndex = 1;
        RecyclerView.ViewHolder toFocus = mRecyclerView.findViewHolderForAdapterPosition(
                focusIndex);
        View viewToFocus = toFocus.itemView;
        assertTrue(requestFocus(viewToFocus, true));
        assertSame(viewToFocus, mRecyclerView.getFocusedChild());

        // The VH of the unfocusable item that just became fully visible after focusSearch.
        RecyclerView.ViewHolder toVisible = null;

        View focusedView = viewToFocus;
        int actualFocusIndex = -1;
        // First, scroll until the last focusable row.
        for (int i : new int[]{4, 6}) {
            focusSearchAndWaitForScroll(focusedView, View.FOCUS_UP);
            focusedView = mRecyclerView.getFocusedChild();
            actualFocusIndex = mRecyclerView.getChildViewHolder(focusedView).getAdapterPosition();
            assertEquals("Focused view should be at adapter position " + i + " whereas it's at "
                    + actualFocusIndex, i, actualFocusIndex);
        }

        // Further scroll up in order to make the unfocusable rows visible. This process should
        // continue until the currently focused item is still visible. The focused item should not
        // change in this loop.
        for (int i : new int[]{9, 11, 11, 11}) {
            focusSearchAndWaitForScroll(focusedView, View.FOCUS_UP);
            focusedView = mRecyclerView.getFocusedChild();
            actualFocusIndex = mRecyclerView.getChildViewHolder(focusedView).getAdapterPosition();
            toVisible = mRecyclerView.findViewHolderForAdapterPosition(i);

            assertEquals("Focused view should not be changed, whereas it's now at "
                    + actualFocusIndex, 6, actualFocusIndex);
            assertTrue("Focused child should be at least partially visible.",
                    isViewPartiallyInBound(mRecyclerView, focusedView));
            assertTrue("Child view at adapter pos " + i + " should be fully visible.",
                    isViewFullyInBound(mRecyclerView, toVisible.itemView));
        }
    }

    @Test
    public void bottomUnfocusableViewsVisibility() throws Throwable {
        // The maximum number of rows that can be fully in-bounds of RV.
        final int visibleRowCount = 5;
        final int spanCount = 3;
        final int lastFocusableIndex = 6;

        setupByConfig(new Config(VERTICAL, false, spanCount, GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS),
                new GridTestAdapter(18, 1) {
                    RecyclerView mAttachedRv;

                    @Override
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
                        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.itemView
                                .getLayoutParams();
                        if (position <= lastFocusableIndex) {
                            holder.itemView.setFocusable(true);
                            holder.itemView.setFocusableInTouchMode(true);
                        } else {
                            holder.itemView.setFocusable(false);
                            holder.itemView.setFocusableInTouchMode(false);
                        }
                        holder.itemView.setMinimumHeight(mAttachedRv.getHeight() / visibleRowCount);
                        lp.topMargin = 0;
                        lp.leftMargin = 0;
                        lp.rightMargin = 0;
                        lp.bottomMargin = 0;
                        if (position == 11) {
                            lp.topMargin = 9;
                        }
                    }
                });

        /**
         * 0 1 2
         * 3 4 5
         * 6 6 6
         * 7
         * 8 8 8
         * 9 10
         * 11 11 11
         * 12 13 14
         * 15 16 17
         */
        mAdapter.mFullSpanItems.add(6);
        mAdapter.mFullSpanItems.add(8);
        mAdapter.mFullSpanItems.add(11);
        waitFirstLayout();


        // adapter position of the currently focused item.
        int focusIndex = 1;
        RecyclerView.ViewHolder toFocus = mRecyclerView.findViewHolderForAdapterPosition(
                focusIndex);
        View viewToFocus = toFocus.itemView;
        assertTrue(requestFocus(viewToFocus, true));
        assertSame(viewToFocus, mRecyclerView.getFocusedChild());

        // The VH of the unfocusable item that just became fully visible after focusSearch.
        RecyclerView.ViewHolder toVisible = null;

        View focusedView = viewToFocus;
        int actualFocusIndex = -1;
        // First, scroll until the last focusable row.
        for (int i : new int[]{4, 6}) {
            focusSearchAndWaitForScroll(focusedView, View.FOCUS_DOWN);
            focusedView = mRecyclerView.getFocusedChild();
            actualFocusIndex = mRecyclerView.getChildViewHolder(focusedView).getAdapterPosition();
            assertEquals("Focused view should be at adapter position " + i + " whereas it's at "
                    + actualFocusIndex, i, actualFocusIndex);
        }

        // Further scroll down in order to make the unfocusable rows visible. This process should
        // continue until the currently focused item is still visible. The focused item should not
        // change in this loop.
        for (int i : new int[]{9, 11, 11, 11}) {
            focusSearchAndWaitForScroll(focusedView, View.FOCUS_DOWN);
            focusedView = mRecyclerView.getFocusedChild();
            actualFocusIndex = mRecyclerView.getChildViewHolder(focusedView).getAdapterPosition();
            toVisible = mRecyclerView.findViewHolderForAdapterPosition(i);

            assertEquals("Focused view should not be changed, whereas it's now at "
                    + actualFocusIndex, 6, actualFocusIndex);
            assertTrue("Focused child should be at least partially visible.",
                    isViewPartiallyInBound(mRecyclerView, focusedView));
            assertTrue("Child view at adapter pos " + i + " should be fully visible.",
                    isViewFullyInBound(mRecyclerView, toVisible.itemView));
        }
    }

    @Test
    public void leftUnfocusableViewsVisibility() throws Throwable {
        // The maximum number of columns that can be fully in-bounds of RV.
        final int visibleColCount = 5;
        final int spanCount = 3;
        final int lastFocusableIndex = 6;

        // Reverse layout so that views are placed from right to left.
        setupByConfig(new Config(HORIZONTAL, true, spanCount,
                        GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS),
                new GridTestAdapter(18, 1) {
                    RecyclerView mAttachedRv;

                    @Override
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
                        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.itemView
                                .getLayoutParams();
                        if (position <= lastFocusableIndex) {
                            holder.itemView.setFocusable(true);
                            holder.itemView.setFocusableInTouchMode(true);
                        } else {
                            holder.itemView.setFocusable(false);
                            holder.itemView.setFocusableInTouchMode(false);
                        }
                        holder.itemView.setMinimumWidth(mAttachedRv.getWidth() / visibleColCount);
                        lp.topMargin = 0;
                        lp.leftMargin = 0;
                        lp.rightMargin = 0;
                        lp.bottomMargin = 0;
                        if (position == 11) {
                            lp.rightMargin = 9;
                        }
                    }
                });

        /**
         * 15 12 11 9  8 7 6 3 0
         * 16 13 11 10 8   6 4 1
         * 17 14 11    8   6 5 2
         */
        mAdapter.mFullSpanItems.add(6);
        mAdapter.mFullSpanItems.add(8);
        mAdapter.mFullSpanItems.add(11);
        waitFirstLayout();


        // adapter position of the currently focused item.
        int focusIndex = 1;
        RecyclerView.ViewHolder toFocus = mRecyclerView.findViewHolderForAdapterPosition(
                focusIndex);
        View viewToFocus = toFocus.itemView;
        assertTrue(requestFocus(viewToFocus, true));
        assertSame(viewToFocus, mRecyclerView.getFocusedChild());

        // The VH of the unfocusable item that just became fully visible after focusSearch.
        RecyclerView.ViewHolder toVisible = null;

        View focusedView = viewToFocus;
        int actualFocusIndex = -1;
        // First, scroll until the last focusable column.
        for (int i : new int[]{4, 6}) {
            focusSearchAndWaitForScroll(focusedView, View.FOCUS_LEFT);
            focusedView = mRecyclerView.getFocusedChild();
            actualFocusIndex = mRecyclerView.getChildViewHolder(focusedView).getAdapterPosition();
            assertEquals("Focused view should be at adapter position " + i + " whereas it's at "
                    + actualFocusIndex, i, actualFocusIndex);
        }

        // Further scroll left in order to make the unfocusable columns visible. This process should
        // continue until the currently focused item is still visible. The focused item should not
        // change in this loop.
        for (int i : new int[]{9, 11, 11, 11}) {
            focusSearchAndWaitForScroll(focusedView, View.FOCUS_LEFT);
            focusedView = mRecyclerView.getFocusedChild();
            actualFocusIndex = mRecyclerView.getChildViewHolder(focusedView).getAdapterPosition();
            toVisible = mRecyclerView.findViewHolderForAdapterPosition(i);

            assertEquals("Focused view should not be changed, whereas it's now at "
                    + actualFocusIndex, 6, actualFocusIndex);
            assertTrue("Focused child should be at least partially visible.",
                    isViewPartiallyInBound(mRecyclerView, focusedView));
            assertTrue("Child view at adapter pos " + i + " should be fully visible.",
                    isViewFullyInBound(mRecyclerView, toVisible.itemView));
        }
    }

    @Test
    public void rightUnfocusableViewsVisibility() throws Throwable {
        // The maximum number of columns that can be fully in-bounds of RV.
        final int visibleColCount = 5;
        final int spanCount = 3;
        final int lastFocusableIndex = 6;

        setupByConfig(new Config(HORIZONTAL, false, spanCount,
                        GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS),
                new GridTestAdapter(18, 1) {
                    RecyclerView mAttachedRv;

                    @Override
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
                        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.itemView
                                .getLayoutParams();
                        if (position <= lastFocusableIndex) {
                            holder.itemView.setFocusable(true);
                            holder.itemView.setFocusableInTouchMode(true);
                        } else {
                            holder.itemView.setFocusable(false);
                            holder.itemView.setFocusableInTouchMode(false);
                        }
                        holder.itemView.setMinimumWidth(mAttachedRv.getWidth() / visibleColCount);
                        lp.topMargin = 0;
                        lp.leftMargin = 0;
                        lp.rightMargin = 0;
                        lp.bottomMargin = 0;
                        if (position == 11) {
                            lp.leftMargin = 9;
                        }
                    }
                });

        /**
         * 0 3 6 7 8 9  11 12 15
         * 1 4 6   8 10 11 13 16
         * 2 5 6   8    11 14 17
         */
        mAdapter.mFullSpanItems.add(6);
        mAdapter.mFullSpanItems.add(8);
        mAdapter.mFullSpanItems.add(11);
        waitFirstLayout();


        // adapter position of the currently focused item.
        int focusIndex = 1;
        RecyclerView.ViewHolder toFocus = mRecyclerView.findViewHolderForAdapterPosition(
                focusIndex);
        View viewToFocus = toFocus.itemView;
        assertTrue(requestFocus(viewToFocus, true));
        assertSame(viewToFocus, mRecyclerView.getFocusedChild());

        // The VH of the unfocusable item that just became fully visible after focusSearch.
        RecyclerView.ViewHolder toVisible = null;

        View focusedView = viewToFocus;
        int actualFocusIndex = -1;
        // First, scroll until the last focusable column.
        for (int i : new int[]{4, 6}) {
            focusSearchAndWaitForScroll(focusedView, View.FOCUS_RIGHT);
            focusedView = mRecyclerView.getFocusedChild();
            actualFocusIndex = mRecyclerView.getChildViewHolder(focusedView).getAdapterPosition();
            assertEquals("Focused view should be at adapter position " + i + " whereas it's at "
                    + actualFocusIndex, i, actualFocusIndex);
        }

        // Further scroll right in order to make the unfocusable rows visible. This process should
        // continue until the currently focused item is still visible. The focused item should not
        // change in this loop.
        for (int i : new int[]{9, 11, 11, 11}) {
            focusSearchAndWaitForScroll(focusedView, View.FOCUS_RIGHT);
            focusedView = mRecyclerView.getFocusedChild();
            actualFocusIndex = mRecyclerView.getChildViewHolder(focusedView).getAdapterPosition();
            toVisible = mRecyclerView.findViewHolderForAdapterPosition(i);

            assertEquals("Focused view should not be changed, whereas it's now at "
                    + actualFocusIndex, 6, actualFocusIndex);
            assertTrue("Focused child should be at least partially visible.",
                    isViewPartiallyInBound(mRecyclerView, focusedView));
            assertTrue("Child view at adapter pos " + i + " should be fully visible.",
                    isViewFullyInBound(mRecyclerView, toVisible.itemView));
        }
    }

    @Test
    public void scrollToPositionWithPredictive() throws Throwable {
        scrollToPositionWithPredictive(0, LinearLayoutManager.INVALID_OFFSET);
        removeRecyclerView();
        scrollToPositionWithPredictive(Config.DEFAULT_ITEM_COUNT / 2,
                LinearLayoutManager.INVALID_OFFSET);
        removeRecyclerView();
        scrollToPositionWithPredictive(9, 20);
        removeRecyclerView();
        scrollToPositionWithPredictive(Config.DEFAULT_ITEM_COUNT / 2, 10);

    }

    public void scrollToPositionWithPredictive(final int scrollPosition, final int scrollOffset)
            throws Throwable {
        setupByConfig(new Config(StaggeredGridLayoutManager.VERTICAL,
                false, 3, StaggeredGridLayoutManager.GAP_HANDLING_NONE));
        waitFirstLayout();
        mLayoutManager.mOnLayoutListener = new OnLayoutListener() {
            @Override
            void after(RecyclerView.Recycler recycler, RecyclerView.State state) {
                RecyclerView rv = mLayoutManager.mRecyclerView;
                if (state.isPreLayout()) {
                    assertEquals("pending scroll position should still be pending",
                            scrollPosition, mLayoutManager.mPendingScrollPosition);
                    if (scrollOffset != LinearLayoutManager.INVALID_OFFSET) {
                        assertEquals("pending scroll position offset should still be pending",
                                scrollOffset, mLayoutManager.mPendingScrollPositionOffset);
                    }
                } else {
                    RecyclerView.ViewHolder vh = rv.findViewHolderForLayoutPosition(scrollPosition);
                    assertNotNull("scroll to position should work", vh);
                    if (scrollOffset != LinearLayoutManager.INVALID_OFFSET) {
                        assertEquals("scroll offset should be applied properly",
                                mLayoutManager.getPaddingTop() + scrollOffset
                                        + ((RecyclerView.LayoutParams) vh.itemView
                                        .getLayoutParams()).topMargin,
                                mLayoutManager.getDecoratedTop(vh.itemView));
                    }
                }
            }
        };
        mLayoutManager.expectLayouts(2);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mAdapter.addAndNotify(0, 1);
                    if (scrollOffset == LinearLayoutManager.INVALID_OFFSET) {
                        mLayoutManager.scrollToPosition(scrollPosition);
                    } else {
                        mLayoutManager.scrollToPositionWithOffset(scrollPosition,
                                scrollOffset);
                    }

                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }

            }
        });
        mLayoutManager.waitForLayout(2);
        checkForMainThreadException();
    }

    @Test
    public void moveGapHandling() throws Throwable {
        Config config = new Config().spanCount(2).itemCount(40);
        setupByConfig(config);
        waitFirstLayout();
        mLayoutManager.expectLayouts(2);
        mAdapter.moveAndNotify(4, 1);
        mLayoutManager.waitForLayout(2);
        assertNull("moving item to upper should not cause gaps", mLayoutManager.hasGapsToFix());
    }

    @Test
    public void updateAfterFullSpan() throws Throwable {
        updateAfterFullSpanGapHandlingTest(0);
    }

    @Test
    public void updateAfterFullSpan2() throws Throwable {
        updateAfterFullSpanGapHandlingTest(20);
    }

    @Test
    public void temporaryGapHandling() throws Throwable {
        int fullSpanIndex = 200;
        setupByConfig(new Config().spanCount(2).itemCount(500));
        mAdapter.mFullSpanItems.add(fullSpanIndex);
        waitFirstLayout();
        smoothScrollToPosition(fullSpanIndex + 200);// go far away
        assertNull("test sanity. full span item should not be visible",
                mRecyclerView.findViewHolderForAdapterPosition(fullSpanIndex));
        mLayoutManager.expectLayouts(1);
        mAdapter.deleteAndNotify(fullSpanIndex + 1, 3);
        mLayoutManager.waitForLayout(1);
        smoothScrollToPosition(0);
        mLayoutManager.expectLayouts(1);
        smoothScrollToPosition(fullSpanIndex + 2 * (AVG_ITEM_PER_VIEW - 1));
        String log = mLayoutManager.layoutToString("post gap");
        mLayoutManager.assertNoLayout("if an interim gap is fixed, it should not cause a "
                + "relayout " + log, 2);
        View fullSpan = mLayoutManager.findViewByPosition(fullSpanIndex);
        assertNotNull("full span item should be there:\n" + log, fullSpan);
        View view1 = mLayoutManager.findViewByPosition(fullSpanIndex + 1);
        assertNotNull("next view should be there\n" + log, view1);
        View view2 = mLayoutManager.findViewByPosition(fullSpanIndex + 2);
        assertNotNull("+2 view should be there\n" + log, view2);

        LayoutParams lp1 = (LayoutParams) view1.getLayoutParams();
        LayoutParams lp2 = (LayoutParams) view2.getLayoutParams();
        assertEquals("view 1 span index", 0, lp1.getSpanIndex());
        assertEquals("view 2 span index", 1, lp2.getSpanIndex());
        assertEquals("no gap between span and view 1",
                mLayoutManager.mPrimaryOrientation.getDecoratedEnd(fullSpan),
                mLayoutManager.mPrimaryOrientation.getDecoratedStart(view1));
        assertEquals("no gap between span and view 2",
                mLayoutManager.mPrimaryOrientation.getDecoratedEnd(fullSpan),
                mLayoutManager.mPrimaryOrientation.getDecoratedStart(view2));
    }

    public void updateAfterFullSpanGapHandlingTest(int fullSpanIndex) throws Throwable {
        setupByConfig(new Config().spanCount(2).itemCount(100));
        mAdapter.mFullSpanItems.add(fullSpanIndex);
        waitFirstLayout();
        smoothScrollToPosition(fullSpanIndex + 30);
        mLayoutManager.expectLayouts(1);
        mAdapter.deleteAndNotify(fullSpanIndex + 1, 3);
        mLayoutManager.waitForLayout(1);
        smoothScrollToPosition(fullSpanIndex);
        // give it some time to fix the gap
        Thread.sleep(500);
        View fullSpan = mLayoutManager.findViewByPosition(fullSpanIndex);

        View view1 = mLayoutManager.findViewByPosition(fullSpanIndex + 1);
        View view2 = mLayoutManager.findViewByPosition(fullSpanIndex + 2);

        LayoutParams lp1 = (LayoutParams) view1.getLayoutParams();
        LayoutParams lp2 = (LayoutParams) view2.getLayoutParams();
        assertEquals("view 1 span index", 0, lp1.getSpanIndex());
        assertEquals("view 2 span index", 1, lp2.getSpanIndex());
        assertEquals("no gap between span and view 1",
                mLayoutManager.mPrimaryOrientation.getDecoratedEnd(fullSpan),
                mLayoutManager.mPrimaryOrientation.getDecoratedStart(view1));
        assertEquals("no gap between span and view 2",
                mLayoutManager.mPrimaryOrientation.getDecoratedEnd(fullSpan),
                mLayoutManager.mPrimaryOrientation.getDecoratedStart(view2));
    }

    @Test
    public void innerGapHandling() throws Throwable {
        innerGapHandlingTest(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        innerGapHandlingTest(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
    }

    public void innerGapHandlingTest(int strategy) throws Throwable {
        Config config = new Config().spanCount(3).itemCount(500);
        setupByConfig(config);
        mLayoutManager.setGapStrategy(strategy);
        mAdapter.mFullSpanItems.add(100);
        mAdapter.mFullSpanItems.add(104);
        mAdapter.mViewsHaveEqualSize = true;
        mAdapter.mOnBindCallback = new OnBindCallback() {
            @Override
            void onBoundItem(TestViewHolder vh, int position) {

            }

            @Override
            void onCreatedViewHolder(TestViewHolder vh) {
                super.onCreatedViewHolder(vh);
                //make sure we have enough views
                mAdapter.mSizeReference = mRecyclerView.getHeight() / 5;
            }
        };
        waitFirstLayout();
        mLayoutManager.expectLayouts(1);
        scrollToPosition(400);
        mLayoutManager.waitForLayout(2);
        View view400 = mLayoutManager.findViewByPosition(400);
        assertNotNull("test sanity, scrollToPos should succeed", view400);
        assertTrue("test sanity, view should be visible top",
                mLayoutManager.mPrimaryOrientation.getDecoratedStart(view400) >=
                        mLayoutManager.mPrimaryOrientation.getStartAfterPadding());
        assertTrue("test sanity, view should be visible bottom",
                mLayoutManager.mPrimaryOrientation.getDecoratedEnd(view400) <=
                        mLayoutManager.mPrimaryOrientation.getEndAfterPadding());
        mLayoutManager.expectLayouts(2);
        mAdapter.addAndNotify(101, 1);
        mLayoutManager.waitForLayout(2);
        checkForMainThreadException();
        if (strategy == GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS) {
            mLayoutManager.expectLayouts(1);
        }
        // state
        // now smooth scroll to 99 to trigger a layout around 100
        mLayoutManager.validateChildren();
        smoothScrollToPosition(99);
        switch (strategy) {
            case GAP_HANDLING_NONE:
                assertSpans("gap handling:" + Config.gapStrategyName(strategy), new int[]{100, 0},
                        new int[]{101, 2}, new int[]{102, 0}, new int[]{103, 1}, new int[]{104, 2},
                        new int[]{105, 0});
                break;
            case GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS:
                mLayoutManager.waitForLayout(2);
                assertSpans("swap items between spans", new int[]{100, 0}, new int[]{101, 0},
                        new int[]{102, 1}, new int[]{103, 2}, new int[]{104, 0}, new int[]{105, 0});
                break;
        }

    }

    @Test
    public void fullSizeSpans() throws Throwable {
        Config config = new Config().spanCount(5).itemCount(30);
        setupByConfig(config);
        mAdapter.mFullSpanItems.add(3);
        waitFirstLayout();
        assertSpans("Testing full size span", new int[]{0, 0}, new int[]{1, 1}, new int[]{2, 2},
                new int[]{3, 0}, new int[]{4, 0}, new int[]{5, 1}, new int[]{6, 2},
                new int[]{7, 3}, new int[]{8, 4});
    }

    void assertSpans(String msg, int[]... childSpanTuples) {
        msg = msg + mLayoutManager.layoutToString("\n\n");
        for (int i = 0; i < childSpanTuples.length; i++) {
            assertSpan(msg, childSpanTuples[i][0], childSpanTuples[i][1]);
        }
    }

    void assertSpan(String msg, int childPosition, int expectedSpan) {
        View view = mLayoutManager.findViewByPosition(childPosition);
        assertNotNull(msg + " view at position " + childPosition + " should exists", view);
        assertEquals(msg + "[child:" + childPosition + "]", expectedSpan,
                getLp(view).mSpan.mIndex);
    }

    @Test
    public void partialSpanInvalidation() throws Throwable {
        Config config = new Config().spanCount(5).itemCount(100);
        setupByConfig(config);
        for (int i = 20; i < mAdapter.getItemCount(); i += 20) {
            mAdapter.mFullSpanItems.add(i);
        }
        waitFirstLayout();
        smoothScrollToPosition(50);
        int prevSpanId = mLayoutManager.mLazySpanLookup.mData[30];
        mAdapter.changeAndNotify(15, 2);
        Thread.sleep(200);
        assertEquals("Invalidation should happen within full span item boundaries", prevSpanId,
                mLayoutManager.mLazySpanLookup.mData[30]);
        assertEquals("item in invalidated range should have clear span id",
                LayoutParams.INVALID_SPAN_ID, mLayoutManager.mLazySpanLookup.mData[16]);
        smoothScrollToPosition(85);
        int[] prevSpans = copyOfRange(mLayoutManager.mLazySpanLookup.mData, 62, 85);
        mAdapter.deleteAndNotify(55, 2);
        Thread.sleep(200);
        assertEquals("item in invalidated range should have clear span id",
                LayoutParams.INVALID_SPAN_ID, mLayoutManager.mLazySpanLookup.mData[16]);
        int[] newSpans = copyOfRange(mLayoutManager.mLazySpanLookup.mData, 60, 83);
        assertSpanAssignmentEquality("valid spans should be shifted for deleted item", prevSpans,
                newSpans, 0, 0, newSpans.length);
    }

    // Same as Arrays.copyOfRange but for API 7
    private int[] copyOfRange(int[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        int[] copy = new int[newLength];
        System.arraycopy(original, from, copy, 0,
                Math.min(original.length - from, newLength));
        return copy;
    }

    @Test
    public void spanReassignmentsOnItemChange() throws Throwable {
        Config config = new Config().spanCount(5);
        setupByConfig(config);
        waitFirstLayout();
        smoothScrollToPosition(mAdapter.getItemCount() / 2);
        final int changePosition = mAdapter.getItemCount() / 4;
        mLayoutManager.expectLayouts(1);
        if (RecyclerView.POST_UPDATES_ON_ANIMATION) {
            mAdapter.changeAndNotify(changePosition, 1);
            mLayoutManager.assertNoLayout("no layout should happen when an invisible child is "
                    + "updated", 1);
        } else {
            mAdapter.changeAndNotify(changePosition, 1);
            mLayoutManager.waitForLayout(1);
        }

        // delete an item before visible area
        int deletedPosition = mLayoutManager.getPosition(mLayoutManager.getChildAt(0)) - 2;
        assertTrue("test sanity", deletedPosition >= 0);
        Map<Item, Rect> before = mLayoutManager.collectChildCoordinates();
        if (DEBUG) {
            Log.d(TAG, "before:");
            for (Map.Entry<Item, Rect> entry : before.entrySet()) {
                Log.d(TAG, entry.getKey().mAdapterIndex + ":" + entry.getValue());
            }
        }
        mLayoutManager.expectLayouts(1);
        mAdapter.deleteAndNotify(deletedPosition, 1);
        mLayoutManager.waitForLayout(2);
        assertRectSetsEqual(config + " when an item towards the head of the list is deleted, it "
                        + "should not affect the layout if it is not visible", before,
                mLayoutManager.collectChildCoordinates()
        );
        deletedPosition = mLayoutManager.getPosition(mLayoutManager.getChildAt(2));
        mLayoutManager.expectLayouts(1);
        mAdapter.deleteAndNotify(deletedPosition, 1);
        mLayoutManager.waitForLayout(2);
        assertRectSetsNotEqual(config + " when a visible item is deleted, it should affect the "
                + "layout", before, mLayoutManager.collectChildCoordinates());
    }

    void assertSpanAssignmentEquality(String msg, int[] set1, int[] set2, int start1, int start2,
            int length) {
        for (int i = 0; i < length; i++) {
            assertEquals(msg + " ind1:" + (start1 + i) + ", ind2:" + (start2 + i), set1[start1 + i],
                    set2[start2 + i]);
        }
    }

    @Test
    public void spanCountChangeOnRestoreSavedState() throws Throwable {
        Config config = new Config(HORIZONTAL, true, 5, GAP_HANDLING_NONE).itemCount(50);
        setupByConfig(config);
        waitFirstLayout();

        int beforeChildCount = mLayoutManager.getChildCount();
        Parcelable savedState = mRecyclerView.onSaveInstanceState();
        // we append a suffix to the parcelable to test out of bounds
        String parcelSuffix = UUID.randomUUID().toString();
        Parcel parcel = Parcel.obtain();
        savedState.writeToParcel(parcel, 0);
        parcel.writeString(parcelSuffix);
        removeRecyclerView();
        // reset for reading
        parcel.setDataPosition(0);
        // re-create
        savedState = RecyclerView.SavedState.CREATOR.createFromParcel(parcel);
        removeRecyclerView();

        RecyclerView restored = new RecyclerView(getActivity());
        mLayoutManager = new WrappedLayoutManager(config.mSpanCount, config.mOrientation);
        mLayoutManager.setReverseLayout(config.mReverseLayout);
        mLayoutManager.setGapStrategy(config.mGapStrategy);
        restored.setLayoutManager(mLayoutManager);
        // use the same adapter for Rect matching
        restored.setAdapter(mAdapter);
        restored.onRestoreInstanceState(savedState);
        mLayoutManager.setSpanCount(1);
        mLayoutManager.expectLayouts(1);
        setRecyclerView(restored);
        mLayoutManager.waitForLayout(2);
        assertEquals("on saved state, reverse layout should be preserved",
                config.mReverseLayout, mLayoutManager.getReverseLayout());
        assertEquals("on saved state, orientation should be preserved",
                config.mOrientation, mLayoutManager.getOrientation());
        assertEquals("after setting new span count, layout manager should keep new value",
                1, mLayoutManager.getSpanCount());
        assertEquals("on saved state, gap strategy should be preserved",
                config.mGapStrategy, mLayoutManager.getGapStrategy());
        assertTrue("when span count is dramatically changed after restore, # of child views "
                + "should change", beforeChildCount > mLayoutManager.getChildCount());
        // make sure SGLM can layout all children. is some span info is leaked, this would crash
        smoothScrollToPosition(mAdapter.getItemCount() - 1);
    }

    @Test
    public void scrollAndClear() throws Throwable {
        setupByConfig(new Config());
        waitFirstLayout();

        assertTrue("Children not laid out", mLayoutManager.collectChildCoordinates().size() > 0);

        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLayoutManager.scrollToPositionWithOffset(1, 0);
                mAdapter.clearOnUIThread();
            }
        });
        mLayoutManager.waitForLayout(2);

        assertEquals("Remaining children", 0, mLayoutManager.collectChildCoordinates().size());
    }

    @Test
    public void accessibilityPositions() throws Throwable {
        setupByConfig(new Config(VERTICAL, false, 3, GAP_HANDLING_NONE));
        waitFirstLayout();
        final AccessibilityDelegateCompat delegateCompat = mRecyclerView
                .getCompatAccessibilityDelegate();
        final AccessibilityEvent event = AccessibilityEvent.obtain();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityEvent(mRecyclerView, event);
            }
        });
        final int start = mRecyclerView
                .getChildLayoutPosition(
                        mLayoutManager.findFirstVisibleItemClosestToStart(false));
        final int end = mRecyclerView
                .getChildLayoutPosition(
                        mLayoutManager.findFirstVisibleItemClosestToEnd(false));
        assertEquals("first item position should match",
                Math.min(start, end), event.getFromIndex());
        assertEquals("last item position should match",
                Math.max(start, end), event.getToIndex());

    }
}
