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
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
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

import java.util.concurrent.TimeUnit;


@RunWith(AndroidJUnit4.class)
public class GridLayoutManagerRobolectricTest {
    private Context mContext;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    private VerticalGridView setupGridView(int itemCount, final int[] firstItemHeight) {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        VerticalGridView gridView = new VerticalGridView(activity);
        gridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);

        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter =
                new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @Override
                    public RecyclerView.@NonNull ViewHolder onCreateViewHolder(
                            @NonNull ViewGroup parent, int viewType) {
                        View view = new View(parent.getContext());
                        view.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
                        return new RecyclerView.ViewHolder(view) {};
                    }

                    @Override
                    public void onBindViewHolder(
                            RecyclerView.@NonNull ViewHolder holder, int position) {
                        if (firstItemHeight != null) {
                            holder.itemView.getLayoutParams().height = (position == 0)
                                    ? firstItemHeight[0] : 100;
                            holder.itemView.requestLayout();
                        }
                    }

                    @Override
                    public int getItemCount() {
                        return itemCount;
                    }
                };
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
    public void testFastRelayout_InTouchMode_InvalidatesAllItems_AlignsToFocus() {
        final int[] firstItemHeight = {100};
        VerticalGridView gridView = setupGridView(10, firstItemHeight);

        View child = gridView.getChildAt(0);

        // Scroll the view to a new position
        gridView.scrollBy(0, 11);
        int top1 = child.getTop();
        assertEquals(439, top1); // 450 - 11 (scrolled up)

        // Notify a change of the first item with size change to invalidate all items.
        // It triggers fastRelayout which requires alignment.
        firstItemHeight[0] = 200;
        gridView.getAdapter().notifyItemChanged(0);
        assertTrue(gridView.isLayoutRequested());

        measureAndLayout((View) gridView.getParent());

        int top2 = child.getTop();

        // Because fastRelayout invalidates all items (index 0), it should realign.
        // Item is 200px high, container is 1000px, so it should be centered at 400.
        // The offset of 11 is lost.
        assertEquals(400, top2);
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
}
