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

import android.test.AndroidTestCase;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static android.support.v7.widget.RecyclerView.*;

public class AdapterHelperTest extends AndroidTestCase {

    static final int NO_POSITION = -1;

    List<ViewHolder> mViewHolders;

    AdapterHelper mAdapterHelper;

    List<AdapterHelper.UpdateOp> mFirstPassUpdates, mSecondPassUpdates;

    TestAdapter mTestAdapter;

    TestAdapter mPreProcessClone; // we clone adapter pre-process to run operations to see result

    @Override
    protected void setUp() throws Exception {
        cleanState();
    }

    private void cleanState() {
        mViewHolders = new ArrayList<ViewHolder>();
        mFirstPassUpdates = new ArrayList<AdapterHelper.UpdateOp>();
        mSecondPassUpdates = new ArrayList<AdapterHelper.UpdateOp>();
        mAdapterHelper = new AdapterHelper(new AdapterHelper.Callback() {
            @Override
            public RecyclerView.ViewHolder findViewHolder(int position) {
                for (ViewHolder vh : mViewHolders) {
                    if (vh.mPosition == position) {
                        return vh;
                    }
                }
                return null;
            }

            @Override
            public void offsetPositionsForRemovingInvisible(int positionStart, int itemCount) {
                final int positionEnd = positionStart + itemCount;
                for (ViewHolder holder : mViewHolders) {
                    if (holder.mPosition >= positionEnd) {
                        holder.offsetPosition(-itemCount, true);
                    } else if (holder.mPosition >= positionStart) {
                        holder.addFlags(ViewHolder.FLAG_REMOVED);
                        holder.offsetPosition(-itemCount, true);
                    }
                }
            }

            @Override
            public void offsetPositionsForRemovingLaidOutOrNewView(int positionStart,
                    int itemCount) {
                final int positionEnd = positionStart + itemCount;
                for (ViewHolder holder : mViewHolders) {
                    if (holder.mPosition >= positionEnd) {
                        holder.offsetPosition(-itemCount, false);
                    } else if (holder.mPosition >= positionStart) {
                        holder.addFlags(ViewHolder.FLAG_REMOVED);
                        holder.offsetPosition(-itemCount, false);
                    }
                }
            }

            @Override
            public void markViewHoldersUpdated(int positionStart, int itemCount) {
                final int positionEnd = positionStart + itemCount;
                for (ViewHolder holder : mViewHolders) {
                    if (holder.mPosition >= positionStart && holder.mPosition < positionEnd) {
                        holder.addFlags(ViewHolder.FLAG_UPDATE);
                    }
                }
            }

            @Override
            public void onDispatchFirstPass(AdapterHelper.UpdateOp updateOp) {
                mFirstPassUpdates.add(updateOp);
            }

            @Override
            public void onDispatchSecondPass(AdapterHelper.UpdateOp updateOp) {
                mSecondPassUpdates.add(updateOp);
            }

            @Override
            public void offsetPositionsForAdd(int positionStart, int itemCount) {
                for (ViewHolder holder : mViewHolders) {
                    if (holder != null && holder.mPosition >= positionStart) {
                        holder.offsetPosition(itemCount, false);
                    }
                }
            }
        }, true);
    }

    void setupBasic(int count, int visibleStart, int visibleCount) {
        mTestAdapter = new TestAdapter(count, mAdapterHelper);
        for (int i = 0; i < visibleCount; i++) {
            addViewHolder(visibleStart + i);
        }
        mPreProcessClone = mTestAdapter.createCopy();
    }

    private void addViewHolder(int posiiton) {
        ViewHolder viewHolder = new RecyclerViewBasicTest.MockViewHolder(
                new TextView(getContext()));
        viewHolder.mPosition = posiiton;
        mViewHolders.add(viewHolder);
    }

    public void testSinglePass() {
        setupBasic(10, 2, 3);
        add(2, 1);
        rm(1, 2);
        add(1, 5);
        mAdapterHelper.consumeUpdatesInOnePass();
        assertDispatch(0, 3);
    }

    public void testDeleteVisible() {
        setupBasic(10, 2, 3);
        rm(2, 1);
        preProcess();
        assertDispatch(0, 1);
    }

    public void testDeleteInvisible() {
        setupBasic(10, 3, 4);
        rm(2, 1);
        preProcess();
        assertDispatch(1, 0);
    }

    public void testAddCount() {
        setupBasic(0, 0, 0);
        add(0, 1);
        assertEquals(1, mAdapterHelper.mPendingUpdates.size());
    }

    public void testDeleteCount() {
        setupBasic(1, 0, 0);
        rm(0, 1);
        assertEquals(1, mAdapterHelper.mPendingUpdates.size());
    }

    public void testAddProcess() {
        setupBasic(0, 0, 0);
        add(0, 1);
        preProcess();
        assertEquals(0, mAdapterHelper.mPendingUpdates.size());
    }

    public void testAddRemoveSeparate() {
        setupBasic(10, 2, 2);
        add(6, 1);
        rm(5, 1);
        preProcess();
        assertDispatch(1, 1);
    }

    public void testSchenerio1() {
        setupBasic(10, 3, 2);
        rm(4, 1);
        rm(3, 1);
        rm(3, 1);
        preProcess();
        assertDispatch(1, 2);
    }

    public void testDivideDelete() {
        setupBasic(10, 3, 4);
        rm(2, 2);
        preProcess();
        assertDispatch(1, 1);
    }

    public void testSchenerio2() {
        setupBasic(10, 3, 3); // 3-4-5
        add(4, 2); // 3 a b 4 5
        rm(0, 1); // (0) 3(2) a(3) b(4) 4(3) 5(4)
        rm(1, 3); // (1,2) (x) a(1) b(2) 4(3)
        preProcess();
        assertDispatch(2, 2);
    }

    public void testSchenerio3() {
        setupBasic(10, 2, 2);
        rm(0, 5);
        preProcess();
        assertDispatch(2, 1);
        assertOps(mFirstPassUpdates, rmOp(0, 2), rmOp(2, 1));
        assertOps(mSecondPassUpdates, rmOp(0, 2));
    }

    public void testSchenerio4() {
        setupBasic(5, 0, 5);
        // 0 1 2 3 4
        // 0 1 2 a b 3 4
        // 0 2 a b 3 4
        // 0 c d 2 a b 3 4
        // 0 c d 2 a 4
        // c d 2 a 4
        // pre: 0 1 2 3 4
        add(3, 2);
        rm(1, 1);
        add(1, 2);
        rm(5, 2);
        rm(0, 1);
        preProcess();
    }

    public void testSchenerio5() {
        setupBasic(5, 0, 5);
        // 0 1 2 3 4
        // 0 1 2 a b 3 4
        // 0 1 b 3 4
        // pre: 0 1 2 3 4
        // pre w/ adap: 0 1 2 b 3 4
        add(3, 2);
        rm(2, 2);
        preProcess();
    }

    public void testRandom() {
        Random random = new Random(System.nanoTime());
        for (int i = 0; i < 10; i++) {
            randomTest(random, i);
        }
    }

    public void randomTest(Random random, int opCount) {
        cleanState();
        final int count = 10 + random.nextInt(100);
        final int start = random.nextInt(count - 1);
        final int layoutCount = Math.max(1, random.nextInt(count - start));
        setupBasic(count, start, layoutCount);

        while (opCount-- > 0) {
            final int op = random.nextInt(AdapterHelper.UpdateOp.REMOVE);
            switch (op) {
                case AdapterHelper.UpdateOp.REMOVE:
                    if (mTestAdapter.mItems.size() > 1) {
                        int s = random.nextInt(mTestAdapter.mItems.size() - 1);
                        int len = Math.max(1, random.nextInt(mTestAdapter.mItems.size() - s));
                        rm(s, len);
                    }
                    break;
                case AdapterHelper.UpdateOp.ADD:
                    int s = random.nextInt(mTestAdapter.mItems.size() - 1);
                    add(s, random.nextInt(50));
                    break;
            }
        }
        preProcess();
    }

    public void assertOps(List<AdapterHelper.UpdateOp> actual,
            AdapterHelper.UpdateOp... expected) {
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual.get(i));
        }
    }

    void assertDispatch(int firstPass, int secondPass) {
        assertEquals(firstPass, mFirstPassUpdates.size());
        assertEquals(secondPass, mSecondPassUpdates.size());
    }

    void preProcess() {
        mAdapterHelper.preProcess();
        mAdapterHelper.consumePostponedUpdates();
        // now assert these two adapters have identical data.
        mPreProcessClone.applyOps(mFirstPassUpdates, mTestAdapter);
        mPreProcessClone.applyOps(mSecondPassUpdates, mTestAdapter);
        assertAdaptersEqual(mTestAdapter, mPreProcessClone);
    }

    private void assertAdaptersEqual(TestAdapter a1, TestAdapter a2) {
        assertEquals(a1.mItems.size(), a2.mItems.size());
        for (int i = 0; i < a1.mItems.size(); i++) {
            TestAdapter.Item item = a1.mItems.get(i);
            assertSame(item, a2.mItems.get(i));
            assertEquals(0, item.getUpdateCount());
        }
        assertEquals(0, a1.mPendingAdded.size());
        assertEquals(0, a2.mPendingAdded.size());
    }

    AdapterHelper.UpdateOp op(int cmd, int start, int count) {
        return new AdapterHelper.UpdateOp(cmd, start, count);
    }

    AdapterHelper.UpdateOp addOp(int start, int count) {
        return op(AdapterHelper.UpdateOp.ADD, start, count);
    }

    AdapterHelper.UpdateOp rmOp(int start, int count) {
        return op(AdapterHelper.UpdateOp.REMOVE, start, count);
    }

    AdapterHelper.UpdateOp upOp(int start, int count) {
        return op(AdapterHelper.UpdateOp.UPDATE, start, count);
    }

    void add(int start, int count) {
        mTestAdapter.add(start, count);
    }

    void rm(int start, int count) {
        mTestAdapter.remove(start, count);
    }

    void up(int start, int count) {
        mTestAdapter.update(start, count);
    }

    static class TestAdapter {

        List<Item> mItems;

        final AdapterHelper mAdapterHelper;

        Queue<Item> mPendingAdded;

        public TestAdapter(int initialCount, AdapterHelper container) {
            mItems = new ArrayList<Item>();
            mAdapterHelper = container;
            mPendingAdded = new LinkedList<Item>();
            for (int i = 0; i < initialCount; i++) {
                mItems.add(new Item());
            }
        }

        public void add(int index, int count) {
            for (int i = 0; i < count; i++) {
                Item item = new Item();
                mPendingAdded.add(item);
                mItems.add(index + i, item);
            }
            mAdapterHelper.addUpdateOp(new AdapterHelper.UpdateOp(
                    AdapterHelper.UpdateOp.ADD, index, count
            ));
        }

        public void remove(int index, int count) {
            for (int i = 0; i < count; i++) {
                mItems.remove(index);
            }
            mAdapterHelper.addUpdateOp(new AdapterHelper.UpdateOp(
                    AdapterHelper.UpdateOp.REMOVE, index, count
            ));
        }

        public void update(int index, int count) {
            for (int i = 0; i < count; i++) {
                mItems.get(index + i).update();
            }
            mAdapterHelper.addUpdateOp(new AdapterHelper.UpdateOp(
                    AdapterHelper.UpdateOp.UPDATE, index, count
            ));
        }

        protected TestAdapter createCopy() {
            TestAdapter adapter = new TestAdapter(0, mAdapterHelper);
            for (Item item : mItems) {
                adapter.mItems.add(item);
            }
            return adapter;
        }

        public void applyOps(List<AdapterHelper.UpdateOp> updates,
                TestAdapter dataSource) {
            for (AdapterHelper.UpdateOp op : updates) {
                switch (op.cmd) {
                    case AdapterHelper.UpdateOp.ADD:
                        for (int i = 0; i < op.itemCount; i++) {
                            mItems.add(op.positionStart + i, dataSource.consumeNextAdded());
                        }
                        break;
                    case AdapterHelper.UpdateOp.REMOVE:
                        for (int i = 0; i < op.itemCount; i++) {
                            mItems.remove(op.positionStart);
                        }
                        break;
                    case AdapterHelper.UpdateOp.UPDATE:
                        for (int i = 0; i < op.itemCount; i++) {
                            mItems.get(i).handleUpdate();
                        }
                        break;
                }
            }
        }

        private Item consumeNextAdded() {
            return mPendingAdded.remove();
        }

        public static class Item {

            private static AtomicInteger itemCounter = new AtomicInteger();

            private final int id;

            private int mVersionCount = 0;

            private int mUpdateCount;

            public Item() {
                id = itemCounter.incrementAndGet();
            }

            public void update() {
                mVersionCount++;
            }

            public void handleUpdate() {
                mVersionCount--;
            }

            public int getUpdateCount() {
                return mUpdateCount;
            }
        }
    }
}
