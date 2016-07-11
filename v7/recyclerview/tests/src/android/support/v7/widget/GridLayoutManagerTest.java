/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v7.widget;

import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;
import static android.support.v7.widget.LinearLayoutManager.VERTICAL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.SparseIntArray;
import android.util.StateSet;
import android.view.View;
import android.view.ViewGroup;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class GridLayoutManagerTest extends BaseGridLayoutManagerTest {

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
                    public void onBindViewHolder(TestViewHolder holder,
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
                    public TestViewHolder onCreateViewHolder(ViewGroup parent,
                            int viewType) {
                        TestViewHolder testViewHolder = super.onCreateViewHolder(parent, viewType);
                        testViewHolder.itemView.setFocusable(true);
                        testViewHolder.itemView.setFocusableInTouchMode(true);
                        // Good to have colors for debugging
                        StateListDrawable stl = new StateListDrawable();
                        stl.addState(new int[]{android.R.attr.state_focused},
                                new ColorDrawable(Color.RED));
                        stl.addState(StateSet.WILD_CARD, new ColorDrawable(Color.BLUE));
                        testViewHolder.itemView.setBackground(stl);
                        return testViewHolder;
                    }

                    @Override
                    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
                        mAttachedRv = recyclerView;
                    }

                    @Override
                    public void onBindViewHolder(TestViewHolder holder,
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
                    recyclerView.getChildViewHolder(focusedView).getAdapterPosition());
            pos += 3;
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
        runTestOnUiThread(new Runnable() {
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
        assertTrue("test sanity", mGlm.supportsPredictiveItemAnimations());
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
    public void horizontalAccessibilitySpanIndices() throws Throwable {
        accessibilitySpanIndicesTest(HORIZONTAL);
    }

    @Test
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
        runTestOnUiThread(new Runnable() {
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
                    public void onBindViewHolder(TestViewHolder holder,
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
        assertEquals("reference child non existent", -1, ssl.findReferenceIndexFromCache(2));
        ssl.getCachedSpanIndex(4, 5);
        assertEquals("reference child non existent", -1, ssl.findReferenceIndexFromCache(3));
        // this should not happen and if happens, it is better to return -1
        assertEquals("reference child itself", -1, ssl.findReferenceIndexFromCache(4));
        assertEquals("reference child before", 4, ssl.findReferenceIndexFromCache(5));
        assertEquals("reference child before", 4, ssl.findReferenceIndexFromCache(100));
        ssl.getCachedSpanIndex(6, 5);
        assertEquals("reference child before", 6, ssl.findReferenceIndexFromCache(7));
        assertEquals("reference child before", 4, ssl.findReferenceIndexFromCache(6));
        assertEquals("reference child itself", -1, ssl.findReferenceIndexFromCache(4));
        ssl.getCachedSpanIndex(12, 5);
        assertEquals("reference child before", 12, ssl.findReferenceIndexFromCache(13));
        assertEquals("reference child before", 6, ssl.findReferenceIndexFromCache(12));
        assertEquals("reference child before", 6, ssl.findReferenceIndexFromCache(7));
        for (int i = 0; i < 6; i++) {
            ssl.getCachedSpanIndex(i, 5);
        }

        for (int i = 1; i < 7; i++) {
            assertEquals("reference child right before " + i, i - 1,
                    ssl.findReferenceIndexFromCache(i));
        }
        assertEquals("reference child before 0 ", -1, ssl.findReferenceIndexFromCache(0));
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
            public void onBindViewHolder(TestViewHolder holder,
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
            public void onBindViewHolder(TestViewHolder holder,
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
                    public void onBindViewHolder(TestViewHolder holder,
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
        runTestOnUiThread(new Runnable() {
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
}
