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

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.concurrent.TimeUnit;

public class RecyclerViewAnimationsTest extends BaseRecyclerViewInstrumentationTest {

    private static final boolean DEBUG = false;

    private static final String TAG = "RecyclerViewAnimationsTest";

    Throwable mainThreadException;

    AnimationLayoutManager mLayoutManager;

    TestAdapter mTestAdapter;

    public RecyclerViewAnimationsTest() {
        super(DEBUG);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    void checkForMainThreadException() throws Throwable {
        if (mainThreadException != null) {
            throw mainThreadException;
        }
    }

    RecyclerView setupBasic(int itemCount) throws Throwable {
        return setupBasic(itemCount, 0, itemCount);
    }

    RecyclerView setupBasic(int itemCount, int firstLayoutStartIndex, int firstLayoutItemCount)
            throws Throwable {
        final RecyclerView recyclerView = new TestRecyclerView(getActivity());
        recyclerView.setHasFixedSize(true);
        mTestAdapter = new TestAdapter(itemCount);
        recyclerView.setAdapter(mTestAdapter);
        mLayoutManager = new AnimationLayoutManager();
        recyclerView.setLayoutManager(mLayoutManager);
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = firstLayoutStartIndex;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = firstLayoutItemCount;

        mLayoutManager.expectLayouts(1);
        setRecyclerView(recyclerView);
        mLayoutManager.waitForLayout(2);
        return recyclerView;
    }

    public void testAddInvisibleAndVisible() throws Throwable {
        setupBasic(10, 1, 7);
        mLayoutManager.expectLayouts(2);
        mLayoutManager.mOnLayoutCallbacks.setExpectedItemCounts(10, 12);
        mTestAdapter.addAndNotify(0, 1);// add a new item 0 // invisible
        mTestAdapter.addAndNotify(7, 1);// add a new item after 5th (old 5, new 6)
        mLayoutManager.waitForLayout(2);
    }

    public void testAddInvisible() throws Throwable {
        setupBasic(10, 1, 7);
        mLayoutManager.expectLayouts(1);
        mLayoutManager.mOnLayoutCallbacks.setExpectedItemCounts(10, 12);
        mTestAdapter.addAndNotify(0, 1);// add a new item 0
        mTestAdapter.addAndNotify(8, 1);// add a new item after 6th (old 6, new 7)
        mLayoutManager.waitForLayout(2);
    }

    public void testBasicAdd() throws Throwable {
        setupBasic(10);
        mLayoutManager.expectLayouts(2);
        setExpectedItemCounts(10, 13);
        mTestAdapter.addAndNotify(2, 3);
        mLayoutManager.waitForLayout(2);
    }

    public void testDeleteVisibleAndInvisible() throws Throwable {
        setupBasic(11, 3, 5); //layout items  3 4 5 6 7
        mLayoutManager.expectLayouts(2);
        setLayoutRange(3, 6); //layout previously invisible child 10 from end of the list
        setExpectedItemCounts(9, 8);
        mTestAdapter.deleteAndNotify(new int[]{4, 1}, new int[]{7, 2});// delete items 4, 8, 9
        mLayoutManager.waitForLayout(2);
    }

    private void setLayoutRange(int start, int count) {
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = start;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = count;
    }

    private void setExpectedItemCounts(int preLayout, int postLayout) {
        mLayoutManager.mOnLayoutCallbacks.setExpectedItemCounts(preLayout, postLayout);
    }

    public void testDeleteInvisible() throws Throwable {
        setupBasic(10, 1, 7);
        mLayoutManager.expectLayouts(1);
        mLayoutManager.mOnLayoutCallbacks.setExpectedItemCounts(8, 8);
        mTestAdapter.deleteAndNotify(0, 1);// delete item id 0
        mTestAdapter.deleteAndNotify(7, 1);// delete item id 8
        mLayoutManager.waitForLayout(2);
    }

    public void testBasicDelete() throws Throwable {
        setupBasic(10);
        final OnLayoutCallbacks callbacks = new OnLayoutCallbacks() {
            @Override
            public void postDispatchLayout() {
                // verify this only in first layout
                assertEquals("deleted views should still be children of RV",
                        mLayoutManager.getChildCount() + mDeletedViewCount
                        , mRecyclerView.getChildCount());
            }

            @Override
            void afterPreLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager layoutManager,
                    RecyclerView.State state) {
                super.afterPreLayout(recycler, layoutManager, state);
                mLayoutItemCount = 3;
                mLayoutMin = 0;
            }
        };
        callbacks.mLayoutItemCount = 10;
        callbacks.setExpectedItemCounts(10, 3);
        mLayoutManager.setOnLayoutCallbacks(callbacks);

        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(0, 7);
        mLayoutManager.waitForLayout(2);
        callbacks.reset();// when animations end another layout will happen
    }


    class AnimationLayoutManager extends TestLayoutManager {

        OnLayoutCallbacks mOnLayoutCallbacks = new OnLayoutCallbacks() {
        };

        @Override
        public boolean supportsItemAnimations() {
            return true;
        }

        @Override
        public void expectLayouts(int count) {
            super.expectLayouts(count);
            mOnLayoutCallbacks.mLayoutCount = 0;
        }

        public void setOnLayoutCallbacks(OnLayoutCallbacks onLayoutCallbacks) {
            mOnLayoutCallbacks = onLayoutCallbacks;
        }

        @Override
        public final void onLayoutChildren(RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            try {
                mOnLayoutCallbacks.onLayoutChildren(recycler, this, state);
            } finally {
                layoutLatch.countDown();
            }
        }


        public void onPostDispatchLayout() {
            mOnLayoutCallbacks.postDispatchLayout();
        }

        @Override
        public void waitForLayout(long timeout, TimeUnit timeUnit) throws Throwable {
            super.waitForLayout(timeout, timeUnit);
            checkForMainThreadException();
        }
    }

    abstract class OnLayoutCallbacks {

        int mLayoutMin = Integer.MIN_VALUE;

        int mLayoutItemCount = Integer.MAX_VALUE;

        int expectedPreLayoutItemCount = -1;

        int expectedPostLayoutItemCount = -1;

        private int mLayoutCount;

        int mDeletedViewCount;

        void setExpectedItemCounts(int preLayout, int postLayout) {
            expectedPreLayoutItemCount = preLayout;
            expectedPostLayoutItemCount = postLayout;
        }

        void reset() {
            mLayoutCount = 0;
            mLayoutMin = Integer.MIN_VALUE;
            mLayoutItemCount = Integer.MAX_VALUE;
            expectedPreLayoutItemCount = -1;
            expectedPostLayoutItemCount = -1;
        }

        void beforePreLayout(RecyclerView.Recycler recycler,
                AnimationLayoutManager lm, RecyclerView.State state) {
            mDeletedViewCount = 0;
            for (int i = 0; i < lm.getChildCount(); i++) {
                View v = lm.getChildAt(i);
                if (lm.getLp(v).isItemRemoved()) {
                    mDeletedViewCount++;
                }
            }
        }

        void doLayout(RecyclerView.Recycler recycler, AnimationLayoutManager lm,
                RecyclerView.State state) {
            if (DEBUG) {
                Log.d(TAG, "item count " + state.getItemCount());
            }
            lm.detachAndScrapAttachedViews(recycler);
            final int start = mLayoutMin == Integer.MIN_VALUE ? 0 : mLayoutMin;
            final int count = mLayoutItemCount
                    == Integer.MAX_VALUE ? state.getItemCount() : mLayoutItemCount;
            lm.layoutRange(recycler, start, start + count);
            assertEquals("correct # of children should be laid out",
                    count - (inPreLayout() ? mDeletedViewCount : 0), lm.getChildCount());
            if (!inPreLayout()) { // may not be the correct check
                lm.assertVisibleItemPositions();
            }
        }

        void onLayoutChildren(RecyclerView.Recycler recycler, AnimationLayoutManager lm,
                RecyclerView.State state) {

            if (mLayoutCount == 0) {
                if (expectedPreLayoutItemCount != -1) {
                    assertEquals("on pre layout, state should return abstracted adapter size",
                            expectedPreLayoutItemCount, state.getItemCount());
                }
                beforePreLayout(recycler, lm, state);
            } else if (mLayoutCount == 1) {
                if (expectedPostLayoutItemCount != -1) {
                    assertEquals("on post layout, state should return real adapter size",
                            expectedPostLayoutItemCount, state.getItemCount());
                }
                beforePostLayout(recycler, lm, state);
            }
            doLayout(recycler, lm, state);
            if (mLayoutCount == 0) {
                afterPreLayout(recycler, lm, state);
            } else if (mLayoutCount == 1) {
                afterPostLayout(recycler, lm, state);
            }
            mLayoutCount++;
        }

        void afterPreLayout(RecyclerView.Recycler recycler, AnimationLayoutManager layoutManager,
                RecyclerView.State state) {
        }

        void beforePostLayout(RecyclerView.Recycler recycler, AnimationLayoutManager layoutManager,
                RecyclerView.State state) {
        }

        void afterPostLayout(RecyclerView.Recycler recycler, AnimationLayoutManager layoutManager,
                RecyclerView.State state) {
        }

        void postDispatchLayout() {
        }

        boolean inPreLayout() {
            return mLayoutCount == 0;
        }
    }

    class TestRecyclerView extends RecyclerView {

        public TestRecyclerView(Context context) {
            super(context);
        }

        public TestRecyclerView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public TestRecyclerView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        void dispatchLayout() {
            try {
                super.dispatchLayout();
                if (getLayoutManager() instanceof AnimationLayoutManager) {
                    ((AnimationLayoutManager) getLayoutManager()).onPostDispatchLayout();
                }
            } catch (Throwable t) {
                postExceptionToInstrumentation(t);
            }

        }

        private void postExceptionToInstrumentation(Throwable t) {
            if (DEBUG) {
                Log.e(TAG, "captured exception on main thread", t);
            }
            mainThreadException = t;
            if (mLayoutManager instanceof TestLayoutManager) {
                TestLayoutManager lm = mLayoutManager;
                // finish all layouts so that we get the correct exception
                while (lm.layoutLatch.getCount() > 0) {
                    lm.layoutLatch.countDown();
                }
            }
        }
    }
}
