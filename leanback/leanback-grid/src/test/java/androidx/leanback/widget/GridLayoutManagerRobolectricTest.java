/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowSystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class GridLayoutManagerRobolectricTest {
    private Context mContext;
    private final List<Integer> mSelectedPositions = new ArrayList<>();

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    static class TestAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        int mItemCount;
        final int[] mFirstItemHeight;

        TestAdapter(int itemCount, int[] firstItemHeight) {
            mItemCount = itemCount;
            mFirstItemHeight = firstItemHeight;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            View view = new View(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
            return new RecyclerView.ViewHolder(view) {};
        }

        @Override
        public void onBindViewHolder(
                RecyclerView.@NonNull ViewHolder holder, int position) {
            if (mFirstItemHeight != null) {
                holder.itemView.getLayoutParams().height = (position == 0)
                        ? mFirstItemHeight[0] : 100;
                holder.itemView.requestLayout();
            }
        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }

        public void insertItem(int position) {
            mItemCount++;
            notifyItemInserted(position);
        }
    }

    private VerticalGridView setupGridView(int itemCount, final int[] firstItemHeight) {
        return setupGridView(itemCount, firstItemHeight, true);
    }

    private VerticalGridView setupGridView(
            int itemCount, final int[] firstItemHeight, boolean touchMode) {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(touchMode);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        VerticalGridView gridView = new VerticalGridView(activity);
        gridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);

        mSelectedPositions.clear();
        gridView.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(RecyclerView parent,
                    RecyclerView.ViewHolder viewHolder, int position, int subposition) {
                mSelectedPositions.add(position);
            }
        });

        TestAdapter adapter = new TestAdapter(itemCount, firstItemHeight);
        gridView.setAdapter(adapter);

        FrameLayout frameLayout = new FrameLayout(activity);
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(1000, 1000));
        gridView.setLayoutParams(new FrameLayout.LayoutParams(1000, 1000));
        gridView.setHasFixedSize(false); // force it handle changes in layout().
        frameLayout.addView(gridView);

        activity.setContentView(frameLayout);

        measureAndLayout(frameLayout);

        return gridView;
    }

    private void measureAndLayout(View view) {
        view.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, 1000, 1000);
    }

    @Test
    public void testInitialLayoutInTouchMode_noEdge() {
        VerticalGridView gridView = setupGridView(10, null);

        assertTrue(gridView.isInTouchMode());

        View child = gridView.getChildAt(0);
        int top = child.getTop();
        int bottom = child.getBottom();

        assertEquals(450, top);
    }

    @Test
    public void testFastRelayout_InTouchMode_DoesNotInvalidateAllItems_KeepsPosition() {
        VerticalGridView gridView = setupGridView(10, null);

        assertTrue(gridView.isInTouchMode());

        View child = gridView.getChildAt(0);

        // Scroll the view to a new position
        gridView.scrollBy(0, 11);
        View childAfterScroll = gridView.getChildAt(0);
        int top1 = childAfterScroll.getTop();
        assertEquals(439, top1); // 450 - 11 (scrolled up)

        // Notify a change of a single item that does not invalidate all items.
        // It triggers fastRelayout but keeps the scroll offset unchanged.
        gridView.getAdapter().notifyItemChanged(3);
        assertTrue(gridView.isLayoutRequested());

        measureAndLayout((View) gridView.getParent());

        child = gridView.getChildAt(0);
        int top2 = child.getTop();

        // It shouldn't align to center after scroll, instead it should keep the scrolled position.
        assertEquals(439, top2);
    }

    @Test
    public void testFastRelayout_InTouchMode_InvalidatesAllItems_DoesNotAlignToFocus() {
        final int[] firstItemHeight = {100};
        VerticalGridView gridView = setupGridView(10, firstItemHeight);

        View child = gridView.getChildAt(0);

        // Scroll the view to a new position
        gridView.scrollBy(0, 11);
        int top1 = child.getTop();
        assertEquals(439, top1); // 450 - 11 (scrolled up)

        // Notify a change of the first item with size change to invalidate all items.
        // It triggers fastRelayout which should not require alignment.
        firstItemHeight[0] = 200;
        gridView.getAdapter().notifyItemChanged(0);
        assertTrue(gridView.isLayoutRequested());

        measureAndLayout((View) gridView.getParent());

        int top2 = child.getTop();

        // Because structure didn't change, it should not realign.
        // The offset of 11 is kept.
        assertEquals(439, top2);
    }

    @Test
    public void testFastRelayout_InTouchMode_StructureChange_AlignsToFocus() {
        VerticalGridView gridView = setupGridView(10, null);

        View child = gridView.getChildAt(0);

        // Scroll the view to a new position
        gridView.scrollBy(0, 11);
        int top1 = child.getTop();
        assertEquals(439, top1); // 450 - 11 (scrolled up)

        // Trigger structure change (insert 1 item at position 0)
        // This should shift focus to position 1, and force realignment of focus (position 1) to keyline (450)
        TestAdapter adapter = (TestAdapter) gridView.getAdapter();
        adapter.insertItem(0);
        assertTrue(gridView.isLayoutRequested());

        measureAndLayout((View) gridView.getParent());

        // Because structure changed, it should realign the focused item (now at position 1) to keyline (450).
        RecyclerView.ViewHolder holder = gridView.findViewHolderForAdapterPosition(1);
        assertNotNull(holder);
        assertEquals(450, holder.itemView.getTop());
    }

    @Test
    public void testFlingToAlreadyAlignedPosition_shouldBeIdle() {
        VerticalGridView gridView = setupGridView(50, null);

        // Focus on 2nd item (index 1)
        gridView.setSelectedPosition(1);

        measureAndLayout((View) gridView.getParent());

        // Now we simulate a drag to the exact aligned position of item 0
        // Item 1 top is at 450. Item 0 top is 350. We want item 0 top to be 450.
        // So we scroll the view by -100.
        gridView.scrollBy(0, -100);

        // Verify item 0 is at 450
        View child = gridView.getChildAt(0);
        assertEquals(450, child.getTop());

        gridView.setFocusScrollStrategy(BaseGridView.FOCUS_SCROLL_ALIGNED_AND_SNAP);

        // Simulate a fling using touch events dragging UP (to reveal items BELOW).
        // This generates a positive fling velocity (moving content up).
        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(
                downTime, downTime, MotionEvent.ACTION_DOWN, 500, 500, 0);
        gridView.dispatchTouchEvent(downEvent);

        ShadowSystemClock.advanceBy(50, TimeUnit.MILLISECONDS);
        long moveTime = SystemClock.uptimeMillis();
        MotionEvent moveEvent = MotionEvent.obtain(
                downTime, moveTime, MotionEvent.ACTION_MOVE, 500, 300, 0);
        gridView.dispatchTouchEvent(moveEvent);

        ShadowSystemClock.advanceBy(50, TimeUnit.MILLISECONDS);
        long moveTime2 = SystemClock.uptimeMillis();
        MotionEvent moveEvent2 = MotionEvent.obtain(
                downTime, moveTime2, MotionEvent.ACTION_MOVE, 500, 100, 0);
        gridView.dispatchTouchEvent(moveEvent2);

        // Verify that GridView indeed entered DRAGGING state
        assertEquals("GridView should be in DRAGGING state after ACTION_MOVE",
                RecyclerView.SCROLL_STATE_DRAGGING, gridView.getScrollState());

        // At this point, GridView is dragging, and SnapHelper should be attached.
        // We wrap the OnFlingListener so we can verify that onFling() is indeed invoked.
        final boolean[] onFlingInvoked = {false};
        final RecyclerView.OnFlingListener originalListener = gridView.getOnFlingListener();

        // Assert that the SnapHelper is actually attached!
        assertTrue("SnapHelper should be attached after DRAGGING", originalListener != null);

        gridView.setOnFlingListener(null); // Detach temporarily to allow setting a new one
        gridView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                onFlingInvoked[0] = true;
                return originalListener.onFling(velocityX, velocityY);
            }
        });

        long upTime = moveTime2 + 10;
        MotionEvent upEvent = MotionEvent.obtain(
                downTime, upTime, MotionEvent.ACTION_UP, 500, 0, 0);
        gridView.dispatchTouchEvent(upEvent);

        downEvent.recycle();
        moveEvent.recycle();
        moveEvent2.recycle();
        upEvent.recycle();

        assertTrue("onFling() should be called during the ACTION_UP phase", onFlingInvoked[0]);
        ShadowLooper.idleMainLooper();
        assertEquals(RecyclerView.SCROLL_STATE_IDLE, gridView.getScrollState());
    }

    @Test
    public void testDraggingOverThreshold_snap_toNext() {
        VerticalGridView gridView = setupGridView(100, null);

        GridLayoutManager layoutManager = (GridLayoutManager) gridView.getLayoutManager();
        gridView.setFocusScrollStrategy(BaseGridView.FOCUS_SCROLL_ALIGNED_AND_SNAP);

        ShadowLooper.idleMainLooper();
        assertEquals(0, gridView.getSelectedPosition());

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent =
                MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 500, 500, 0);
        gridView.dispatchTouchEvent(downEvent);

        ShadowSystemClock.advanceBy(1000, TimeUnit.MILLISECONDS);
        long moveTime = SystemClock.uptimeMillis();
        assertEquals(16, ViewConfiguration.get(gridView.getContext()).getScaledTouchSlop());
        float threshold = 48f;
        // Drag 1 pixel over threshold, will trigger select next item.
        float y = 500 - 1f - threshold;
        MotionEvent moveEvent =
                MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, 500, y, 0);
        gridView.dispatchTouchEvent(moveEvent);
        ShadowLooper.idleMainLooper();

        // Verify that GridView indeed entered DRAGGING state
        assertEquals(RecyclerView.SCROLL_STATE_DRAGGING, gridView.getScrollState());

        ShadowSystemClock.advanceBy(1000, TimeUnit.MILLISECONDS);
        long upTime = SystemClock.uptimeMillis();
        MotionEvent upEvent =
                MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, 500, y, 0);
        gridView.dispatchTouchEvent(upEvent);

        downEvent.recycle();
        moveEvent.recycle();
        upEvent.recycle();

        ShadowLooper.idleMainLooper();
        assertEquals(RecyclerView.SCROLL_STATE_IDLE, gridView.getScrollState());

        assertEquals(1, gridView.getSelectedPosition());
    }

    @Test
    public void testDraggingLessThanThreshold_snap_toCurrent() {
        VerticalGridView gridView = setupGridView(100, null);

        GridLayoutManager layoutManager = (GridLayoutManager) gridView.getLayoutManager();
        gridView.setFocusScrollStrategy(BaseGridView.FOCUS_SCROLL_ALIGNED_AND_SNAP);

        ShadowLooper.idleMainLooper();
        assertEquals(0, gridView.getSelectedPosition());

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent =
                MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 500, 500, 0);
        gridView.dispatchTouchEvent(downEvent);

        ShadowSystemClock.advanceBy(1000, TimeUnit.MILLISECONDS);
        long moveTime = SystemClock.uptimeMillis();
        assertEquals(16, ViewConfiguration.get(gridView.getContext()).getScaledTouchSlop());
        float threshold = 48f;
        // Drag 1 pixel less than threshold, will stay in current selection.
        float y = 500 - threshold + 1f;
        MotionEvent moveEvent =
                MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, 500, y, 0);
        gridView.dispatchTouchEvent(moveEvent);
        ShadowLooper.idleMainLooper();

        // Verify that GridView indeed entered DRAGGING state
        assertEquals(RecyclerView.SCROLL_STATE_DRAGGING, gridView.getScrollState());

        ShadowSystemClock.advanceBy(1000, TimeUnit.MILLISECONDS);
        long upTime = SystemClock.uptimeMillis();
        MotionEvent upEvent =
                MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, 500, y, 0);
        gridView.dispatchTouchEvent(upEvent);

        downEvent.recycle();
        moveEvent.recycle();
        upEvent.recycle();

        ShadowLooper.idleMainLooper();
        assertEquals(RecyclerView.SCROLL_STATE_IDLE, gridView.getScrollState());

        assertEquals(0, gridView.getSelectedPosition());
    }

    @Test
    public void testTouchMode_setSelectedPositionToUnalignedChild_doesNotAlign() {
        VerticalGridView gridView = setupGridView(10, null);
        assertTrue(gridView.isInTouchMode());

        // Scroll to unalign
        gridView.scrollBy(0, 11);
        View child1 = gridView.getChildAt(1);
        int topBefore = child1.getTop();
        assertEquals(539, topBefore); // 450 (keyline) + 100 (item 0) - 11 (scroll)

        // Select unaligned
        mSelectedPositions.clear();
        gridView.setSelectedPositionToUnalignedChild(child1);
        assertEquals(1, mSelectedPositions.size());
        assertEquals(1, (int) mSelectedPositions.get(0));

        // Trigger view layout change (requestLayout)
        child1.requestLayout();
        measureAndLayout((View) gridView.getParent());

        // Verify it did not align (remains at 539)
        assertEquals(539, child1.getTop());
        assertEquals(1, mSelectedPositions.size());
    }

    @Test
    public void testTouchMode_setSelectedPosition_aligns() {
        VerticalGridView gridView = setupGridView(10, null);
        assertTrue(gridView.isInTouchMode());

        // Scroll to unalign
        gridView.scrollBy(0, 11);
        View child1 = gridView.getChildAt(1);
        assertEquals(539, child1.getTop());

        // Select aligned (programmatic)
        mSelectedPositions.clear();
        gridView.setSelectedPosition(1);

        measureAndLayout((View) gridView.getParent());

        // Verify it aligned (child 1 moves to keyline 450)
        assertEquals(450, child1.getTop());
        assertEquals(1, mSelectedPositions.size());
        assertEquals(1, (int) mSelectedPositions.get(0));
    }

    @Test
    public void testTouchMode_setSelectedPosition_offscreen_aligns() {
        VerticalGridView gridView = setupGridView(15, null);
        assertTrue(gridView.isInTouchMode());

        // Select offscreen item (index 12 is offscreen since height is 1000 and items are 100)
        mSelectedPositions.clear();
        gridView.setSelectedPosition(12);

        measureAndLayout((View) gridView.getParent());

        // Verify it is aligned to keyline (450)
        RecyclerView.ViewHolder holder = gridView.findViewHolderForAdapterPosition(12);
        assertNotNull(holder);
        assertEquals(450, holder.itemView.getTop());
        assertEquals(1, mSelectedPositions.size());
        assertEquals(12, (int) mSelectedPositions.get(0));
    }

    @Test
    public void testTouchMode_setSelectedPosition_sameFrameAsLayoutChange_aligns() {
        VerticalGridView gridView = setupGridView(10, null);
        assertTrue(gridView.isInTouchMode());

        // Scroll to unalign
        gridView.scrollBy(0, 11);
        View child1 = gridView.getChildAt(1);
        assertEquals(539, child1.getTop());

        // In the same frame: change layout of child 1 and call setSelectedPosition
        child1.getLayoutParams().height = 150;
        child1.requestLayout();
        mSelectedPositions.clear();
        gridView.setSelectedPosition(1);

        measureAndLayout((View) gridView.getParent());

        // Verify it aligned to keyline (425 because height is 150)
        assertEquals(425, child1.getTop());
        assertEquals(150, child1.getHeight());
        assertEquals(1, mSelectedPositions.size());
        assertEquals(1, (int) mSelectedPositions.get(0));
    }

    @Test
    public void testTouchMode_setSelectedPositionToUnaligned_layoutChange_doesNotAlign() {
        VerticalGridView gridView = setupGridView(10, null);
        assertTrue(gridView.isInTouchMode());

        // Scroll to unalign
        gridView.scrollBy(0, 11);
        View child1 = gridView.getChildAt(1);
        assertEquals(539, child1.getTop());

        // In the same frame: change layout of child 1 and call setSelectedPositionToUnalignedChild
        child1.getLayoutParams().height = 150;
        child1.requestLayout();
        mSelectedPositions.clear();
        gridView.setSelectedPositionToUnalignedChild(child1);

        measureAndLayout((View) gridView.getParent());

        // Verify it did not align (remains at 539, but height is updated)
        assertEquals(539, child1.getTop());
        assertEquals(150, child1.getHeight());
        assertEquals(1, mSelectedPositions.size());
        assertEquals(1, (int) mSelectedPositions.get(0));
    }

    @Test
    public void testDPADMode_setSelectedPositionToUnalignedChild_doesNotAlign() {
        // Setup in DPAD mode (touchMode = false)
        VerticalGridView gridView = setupGridView(10, null, false);
        assertTrue(!gridView.isInTouchMode());

        // Scroll to unalign
        gridView.scrollBy(0, 11);
        View child1 = gridView.getChildAt(1);
        assertEquals(539, child1.getTop());

        // Select unaligned
        mSelectedPositions.clear();
        gridView.setSelectedPositionToUnalignedChild(child1);

        child1.requestLayout();
        measureAndLayout((View) gridView.getParent());

        // Verify it did not align (keeps hover offset)
        assertEquals(539, child1.getTop());
        assertEquals(1, mSelectedPositions.size());
        assertEquals(1, (int) mSelectedPositions.get(0));
    }

    @Test
    public void testDPADMode_resumeDPADNavigation_aligns() {
        VerticalGridView gridView = setupGridView(10, null, false);
        assertTrue(!gridView.isInTouchMode());

        // Focus on item 0
        gridView.setSelectedPosition(0);
        measureAndLayout((View) gridView.getParent());
        View child0 = gridView.getChildAt(0);
        assertEquals(450, child0.getTop());

        // Scroll to unalign
        gridView.scrollBy(0, 11);
        assertEquals(439, child0.getTop());

        mSelectedPositions.clear();

        // Hover select item 0 (stays unaligned)
        gridView.setSelectedPositionToUnalignedChild(child0);
        child0.requestLayout();
        measureAndLayout((View) gridView.getParent());
        assertEquals(439, child0.getTop());
        assertTrue(mSelectedPositions.isEmpty());

        // Simulate DPAD navigation to item 1.
        // We call focusSearch to simulate key press.
        View nextFocus = gridView.focusSearch(child0, View.FOCUS_DOWN);
        assertNotNull(nextFocus);
        nextFocus.requestFocus();
        ShadowLooper.idleMainLooper();

        measureAndLayout((View) gridView.getParent());

        // Verify next focus (item 1) is aligned to keyline (450)
        RecyclerView.ViewHolder holder1 = gridView.findViewHolderForAdapterPosition(1);
        assertNotNull(holder1);
        assertEquals(450, holder1.itemView.getTop());
        assertEquals(1, mSelectedPositions.size());
        assertEquals(1, (int) mSelectedPositions.get(0));
    }

    @Test
    public void testTouchMode_setSelectedPositionToUnaligned_structureChange_aligns() {
        VerticalGridView gridView = setupGridView(10, null);
        assertTrue(gridView.isInTouchMode());

        // Scroll to unalign
        gridView.scrollBy(0, 11);
        View child1 = gridView.getChildAt(1);
        int topBefore = child1.getTop();
        assertEquals(539, topBefore); // 450 (keyline) + 100 (item 0) - 11 (scroll)

        // Select unaligned (sets PF_KEEP_UNALIGNED)
        mSelectedPositions.clear();

        gridView.setSelectedPositionToUnalignedChild(child1);
        assertEquals(1, mSelectedPositions.size());
        assertEquals(1, (int) mSelectedPositions.get(0));

        child1.requestLayout();
        measureAndLayout((View) gridView.getParent());

        // Verify it did not align (remains at 539)
        assertEquals(539, child1.getTop());
        assertEquals(1, mSelectedPositions.size()); // No new events

        // Trigger structure change (insert 1 item at position 0)
        // This should shift focus to position 2, and force realignment of focus
        // (position 2) to keyline (450)
        TestAdapter adapter = (TestAdapter) gridView.getAdapter();
        adapter.insertItem(0);
        assertTrue(gridView.isLayoutRequested());

        measureAndLayout((View) gridView.getParent());

        // Verify it aligned the focused item (now at position 2) to keyline (450)
        RecyclerView.ViewHolder holder = gridView.findViewHolderForAdapterPosition(2);
        assertNotNull(holder);
        assertEquals(450, holder.itemView.getTop());

        // Verify it dispatched position 2 selection event.
        assertEquals(2, mSelectedPositions.size());
        assertEquals(2, (int) mSelectedPositions.get(1));
    }

    @Test
    public void testDPADMode_setSelectedPosition_aligns() {
        VerticalGridView gridView = setupGridView(10, null, false);
        assertTrue(!gridView.isInTouchMode());

        // Scroll to unalign
        gridView.scrollBy(0, 11);
        View child1 = gridView.getChildAt(1);
        assertEquals(539, child1.getTop());

        // Select unaligned (hover)
        mSelectedPositions.clear();

        gridView.setSelectedPositionToUnalignedChild(child1);
        child1.requestLayout();
        measureAndLayout((View) gridView.getParent());
        assertEquals(539, child1.getTop());
        assertEquals(1, mSelectedPositions.size());
        assertEquals(1, (int) mSelectedPositions.get(0));

        // Select aligned programmatically
        gridView.setSelectedPosition(1);
        measureAndLayout((View) gridView.getParent());

        // Verify it aligned (child 1 moves to keyline 450)
        assertEquals(450, child1.getTop());
        // Position didn't change, so no new event.
        assertEquals(1, mSelectedPositions.size());
    }

    @Test
    public void testDPADMode_setSelectedPositionToUnaligned_structureChange_aligns() {
        VerticalGridView gridView = setupGridView(10, null, false);
        assertTrue(!gridView.isInTouchMode());

        // Scroll to unalign
        gridView.scrollBy(0, 11);
        View child1 = gridView.getChildAt(1);
        assertEquals(539, child1.getTop());

        // Select unaligned (hover)
        mSelectedPositions.clear();

        gridView.setSelectedPositionToUnalignedChild(child1);
        child1.requestLayout();
        measureAndLayout((View) gridView.getParent());
        assertEquals(539, child1.getTop());
        assertEquals(1, mSelectedPositions.size());
        assertEquals(1, (int) mSelectedPositions.get(0));

        // Trigger structure change (insert 1 item at position 0)
        // Focus shifts to position 2, and should align to keyline (450)
        TestAdapter adapter = (TestAdapter) gridView.getAdapter();
        adapter.insertItem(0);
        assertTrue(gridView.isLayoutRequested());

        measureAndLayout((View) gridView.getParent());

        // Verify it aligned the focused item (now at position 2) to keyline (450)
        RecyclerView.ViewHolder holder = gridView.findViewHolderForAdapterPosition(2);
        assertNotNull(holder);
        assertEquals(450, holder.itemView.getTop());

        // Verify it dispatched position 2 selection event.
        assertEquals(2, mSelectedPositions.size());
        assertEquals(2, (int) mSelectedPositions.get(1));
    }

    @Test
    public void testTouchMode_setSelectedPosition_alreadyAligned_doesNotLeakPendingAlign() {
        VerticalGridView gridView = setupGridView(10, null);
        assertTrue(gridView.isInTouchMode());

        // Focus on 0, it is aligned by default (450)
        View child0 = gridView.getChildAt(0);
        assertEquals(450, child0.getTop());

        // Call setSelectedPosition(0) programmatically.
        // It is already aligned, so it shouldn't scroll or layout.
        mSelectedPositions.clear();

        gridView.setSelectedPosition(0);
        assertTrue(!gridView.isLayoutRequested());
        assertTrue(mSelectedPositions.isEmpty());

        // Scroll the view to make it unaligned.
        gridView.scrollBy(0, 11);
        assertEquals(439, child0.getTop());

        // Trigger layout pass.
        child0.requestLayout();
        assertTrue(gridView.isLayoutRequested());
        measureAndLayout((View) gridView.getParent());

        // If PF_PENDING_ALIGN leaked, it will force alignment back to 450.
        // It should remain at 439 (since we scrolled it and didn't request realignment).
        assertEquals(439, child0.getTop());
        assertTrue(mSelectedPositions.isEmpty());
    }

    @Test
    public void testTouchMode_setSelectedPosition_visibleView_clearsPendingAlign() {
        VerticalGridView gridView = setupGridView(10, null);
        assertTrue(gridView.isInTouchMode());

        // Child 1 is at 550, visible (grid height 1000)
        View child1 = gridView.getChildAt(1);
        assertEquals(550, child1.getTop());

        mSelectedPositions.clear();

        // Select child 1. This should trigger immediate scroll because the view is
        // available and layout is not requested.
        gridView.setSelectedPosition(1);

        // Verify that the view scrolled immediately.
        assertEquals(450, child1.getTop());

        // Verify that PF_PENDING_ALIGN is not remaining.
        GridLayoutManager layoutManager = (GridLayoutManager) gridView.getLayoutManager();
        assertTrue((layoutManager.mFlag & GridLayoutManager.PF_PENDING_ALIGN) == 0);

        // Verify that the selection event was dispatched once.
        assertEquals(1, mSelectedPositions.size());
        assertEquals(1, (int) mSelectedPositions.get(0));
    }
}
