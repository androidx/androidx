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

    private boolean mDebug;

    protected RecyclerView mRecyclerView;

    public BaseRecyclerViewInstrumentationTest() {
        this(false);
    }

    public BaseRecyclerViewInstrumentationTest(boolean debug) {
        super("android.support.v7.widget", TestActivity.class);
        mDebug = debug;
    }

    public void removeRecyclerView() throws Throwable {
        mRecyclerView = null;
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().mContainer.removeAllViews();
            }
        });
    }

    public void setRecyclerView(final RecyclerView recyclerView) throws Throwable {
        mRecyclerView = recyclerView;
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().mContainer.addView(recyclerView);
            }
        });
    }

    public void requestLayoutOnUIThread(final View view) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.requestLayout();
            }
        });
    }

    public void scrollBy(final int dt) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRecyclerView.getLayoutManager().canScrollHorizontally()) {
                    mRecyclerView.scrollBy(dt, 0);
                } else {
                    mRecyclerView.scrollBy(0, dt);
                }

            }
        });
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

        public void waitForLayout(long timeout, TimeUnit timeUnit) throws Throwable {
            layoutLatch.await(timeout * (mDebug ? 100 : 1), timeUnit);
            assertEquals("all expected layouts should be executed at the expected time",
                    0, layoutLatch.getCount());
        }

        public void assertLayoutCount(int count, String msg, long timeout) throws Throwable {
            layoutLatch.await(timeout, TimeUnit.SECONDS);
            assertEquals(msg, count, layoutLatch.getCount());
        }

        public void assertNoLayout(String msg, long timeout) throws Throwable {
            layoutLatch.await(timeout, TimeUnit.SECONDS);
            assertFalse(msg, layoutLatch.getCount() == 0);
        }

        public void waitForLayout(long timeout) throws Throwable {
            waitForLayout(timeout, TimeUnit.SECONDS);
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
                if (mDebug) {
                    Log.d(TAG, "testing item " + i);
                }
                assertSame("item position in LP should match adapter value",
                        testAdapter.mItems.get(lp.getViewPosition()), item);
            }
        }

        RecyclerView.LayoutParams getLp(View v) {
            return (RecyclerView.LayoutParams) v.getLayoutParams();
        }

        void layoutRange(RecyclerView.Recycler recycler, int start,
                int end) {
            if (mDebug) {
                Log.d(TAG, "will layout items from " + start + " to " + end);
            }
            for (int i = start; i < end; i++) {
                if (mDebug) {
                    Log.d(TAG, "laying out item " + i);
                }
                View view = recycler.getViewForPosition(i);
                assertNotNull("view should not be null for valid position. "
                        + "got null view at position " + i, view);
                if (!getLp(view).isItemRemoved()) {
                    addView(view);
                }

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

        public void deleteAndNotify(final int start, final int count) throws Throwable {
            deleteAndNotify(new int[]{start, count});
        }

        /**
         * Deletes items in the given ranges.
         * <p>
         * Note that each operation affects the one after so you should offset them properly.
         * <p>
         * For example, if adapter has 5 items (A,B,C,D,E), and then you call this method with
         * <code>[1, 2],[2, 1]</code>, it will first delete items B,C and the new adapter will be
         * A D E. Then it will delete 2,1 which means it will delete E.
         */
        public void deleteAndNotify(final int[]... startCountTuples) throws Throwable {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int t = 0; t < startCountTuples.length; t++) {
                        int[] tuple = startCountTuples[t];
                        for (int i = 0; i < tuple[1]; i++) {
                            mItems.remove(tuple[0]);
                        }
                        notifyItemRangeRemoved(tuple[0], tuple[1]);
                    }

                }
            });
        }

        public void addAndNotify(final int start, final int count) throws Throwable {
            addAndNotify(new int[]{start, count});
        }

        public void addAndNotify(final int[]... startCountTuples) throws Throwable {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int t = 0; t < startCountTuples.length; t++) {
                        int[] tuple = startCountTuples[t];
                        for (int i = 0; i < tuple[1]; i++) {
                            mItems.add(tuple[0], new Item(i, "new item " + i));
                        }
                        // offset others
                        for (int i = tuple[0] + tuple[1]; i < mItems.size(); i++) {
                            mItems.get(i).originalIndex += tuple[1];
                        }
                        notifyItemRangeInserted(tuple[0], tuple[1]);
                    }

                }
            });
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }
}