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

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

abstract public class BaseRecyclerViewInstrumentationTest extends
        ActivityInstrumentationTestCase2<TestActivity> {

    private static final String TAG = "RecyclerViewTest";

    private static final boolean DEBUG = true;

    public BaseRecyclerViewInstrumentationTest() {
        super("android.support.v7.widget", TestActivity.class);
    }

    class TestViewHolder extends RecyclerView.ViewHolder {

        Item mBindedItem;

        public TestViewHolder(View itemView) {
            super(itemView);
        }
    }

    class TestLayoutManager extends RecyclerView.LayoutManager {

        CountDownLatch layoutLatch;

        public void expectLayouts(int count) {
            layoutLatch = new CountDownLatch(count);
        }

        public void waitForLayout(long timeout, TimeUnit timeUnit) throws InterruptedException {
            layoutLatch.await(timeout, timeUnit);
            assertEquals("all expected layouts should be executed at the expected time",
                    0, layoutLatch.getCount());
        }

        @Override
        public RecyclerView.LayoutParams generateDefaultLayoutParams() {
            return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        void assertVisibleItemPositions() {
            int i = getChildCount();
            TestAdapter testAdapter = (TestAdapter) mRecyclerView.getAdapter();
            while (i-- > 0) {
                View view = getChildAt(i);
                RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
                Item item = ((TestViewHolder) lp.mViewHolder).mBindedItem;
                if (DEBUG) {
                    Log.d(TAG, "testing item " + i);
                }
                assertSame("item position in LP should match adapter value",
                        testAdapter.mItems.get(lp.getViewPosition()), item);
            }
        }

        void layoutRange(RecyclerView.Recycler recycler, int start,
                int end) {
            if (DEBUG) {
                Log.d(TAG, "will layout items from " + start + " to " + end);
            }
            for (int i = start; i < end; i++) {
                if (DEBUG) {
                    Log.d(TAG, "laying out item " + i);
                }
                View view = recycler.getViewForPosition(i);
                assertNotNull("view should not be null for valid position", view);
                addView(view);
                measureChildWithMargins(view, 0, 0);
                layoutDecorated(view, 0, (i - start) * 10, getDecoratedMeasuredWidth(view)
                        , getDecoratedMeasuredHeight(view));
            }
        }
    }

    static class Item {

        int originalIndex;

        final String text;

        Item(int originalIndex, String text) {
            this.originalIndex = originalIndex;
            this.text = text;
        }
    }

    class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {

        List<Item> mItems;

        TestAdapter(int count) {
            mItems = new ArrayList<Item>(count);
            for (int i = 0; i < count; i++) {
                mItems.add(new Item(i, "Item " + i));
            }
        }

        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent,
                int viewType) {
            return new TestViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(TestViewHolder holder, int position) {
            final Item item = mItems.get(position);
            ((TextView) (holder.itemView)).setText(item.text);
            holder.mBindedItem = item;
        }

        public void deleteRangeAndNotify(final int start, final int end) throws Throwable {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = start; i < end; i++) {
                        mItems.remove(start);
                    }
                    notifyItemRangeRemoved(start, end - start);
                }
            });
        }

        public void addRangeAndNotify(final int start, final int end) throws Throwable {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final int count = end - start;
                    for (int i = start; i < end; i++) {
                        mItems.add(start, new Item(i, "new item " + i));
                    }
                    // offset others
                    for (int i = end; i < mItems.size(); i++) {
                        mItems.get(i).originalIndex += count;
                    }
                    notifyItemRangeInserted(start, count);
                }
            });

        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }
}

