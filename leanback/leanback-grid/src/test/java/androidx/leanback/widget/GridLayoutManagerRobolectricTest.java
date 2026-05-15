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

@RunWith(AndroidJUnit4.class)
public class GridLayoutManagerRobolectricTest {
    private Context mContext;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testInitialLayoutInTouchMode_noEdge() {
        // Force touch mode
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
                    }

                    @Override
                    public int getItemCount() {
                        return 10;
                    }
                };
        gridView.setAdapter(adapter);

        FrameLayout frameLayout = new FrameLayout(activity);
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(1000, 1000));
        gridView.setLayoutParams(new FrameLayout.LayoutParams(1000, 1000));
        gridView.setHasFixedSize(false); // force it handle changes in layout().
        frameLayout.addView(gridView);

        activity.setContentView(frameLayout);

        assertTrue(gridView.isInTouchMode());

        // measure and layout
        frameLayout.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        frameLayout.layout(0, 0, 1000, 1000);

        View child = gridView.getChildAt(0);
        int top = child.getTop();
        int bottom = child.getBottom();

        assertEquals(450, top);
    }

    @Test
    public void testFastRelayout_InTouchMode_DoesNotInvalidateAllItems_KeepsPosition() {
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
                    }

                    @Override
                    public int getItemCount() {
                        return 10;
                    }
                };
        gridView.setAdapter(adapter);

        FrameLayout frameLayout = new FrameLayout(activity);
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(1000, 1000));
        gridView.setLayoutParams(new FrameLayout.LayoutParams(1000, 1000));
        gridView.setHasFixedSize(false); // force it handle changes in layout().
        frameLayout.addView(gridView);

        activity.setContentView(frameLayout);

        assertTrue(gridView.isInTouchMode());

        // First layout
        frameLayout.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        frameLayout.layout(0, 0, 1000, 1000);

        View child = gridView.getChildAt(0);

        // Scroll the view to a new position
        gridView.scrollBy(0, 11);
        int top1 = child.getTop();
        assertEquals(439, top1); // 450 - 11 (scrolled up)

        // Notify a change of a single item that does not invalidate all items.
        // It triggers fastRelayout but keeps the scroll offset unchanged.
        adapter.notifyItemChanged(3);
        assertTrue(gridView.isLayoutRequested());

        frameLayout.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        frameLayout.layout(0, 0, 1000, 1000);

        child = gridView.getChildAt(0);
        int top2 = child.getTop();

        // It shouldn't align to center after scroll, instead it should keep the scrolled position.
        assertEquals(439, top2);
    }

    @Test
    public void testFastRelayout_InTouchMode_InvalidatesAllItems_AlignsToFocus() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        VerticalGridView gridView = new VerticalGridView(activity);
        gridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_NO_EDGE);

        final int[] firstItemHeight = {100};

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
                        holder.itemView.getLayoutParams().height = (position == 0)
                                ? firstItemHeight[0] : 100;
                        holder.itemView.requestLayout();
                    }

                    @Override
                    public int getItemCount() {
                        return 10;
                    }
                };
        gridView.setAdapter(adapter);

        FrameLayout frameLayout = new FrameLayout(activity);
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(1000, 1000));
        gridView.setLayoutParams(new FrameLayout.LayoutParams(1000, 1000));
        gridView.setHasFixedSize(false); // force it handle changes in layout().
        frameLayout.addView(gridView);

        activity.setContentView(frameLayout);

        // First layout
        frameLayout.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        frameLayout.layout(0, 0, 1000, 1000);
        View child = gridView.getChildAt(0);

        // Scroll the view to a new position
        gridView.scrollBy(0, 11);
        int top1 = child.getTop();
        assertEquals(439, top1); // 450 - 11 (scrolled up)

        // Notify a change of the first item with size change to invalidate all items.
        // It triggers fastRelayout which requires alignment.
        firstItemHeight[0] = 200;
        adapter.notifyItemChanged(0);
        assertTrue(gridView.isLayoutRequested());

        frameLayout.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        frameLayout.layout(0, 0, 1000, 1000);

        int top2 = child.getTop();

        // Because fastRelayout invalidates all items (index 0), it should realign.
        // Item is 200px high, container is 1000px, so it should be centered at 400.
        // The offset of 11 is lost.
        assertEquals(400, top2);
    }
}
