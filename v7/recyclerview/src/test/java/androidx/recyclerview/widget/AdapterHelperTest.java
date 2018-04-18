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

import static androidx.recyclerview.widget.RecyclerView.ViewHolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import android.support.test.filters.SmallTest;
import android.view.View;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(JUnit4.class)
@SmallTest
public class AdapterHelperTest {

    private static final boolean DEBUG = false;

    private boolean mCollectLogs = false;

    private static final String TAG = "AHT";

    private List<MockViewHolder> mViewHolders;

    private AdapterHelper mAdapterHelper;

    private List<AdapterHelper.UpdateOp> mFirstPassUpdates, mSecondPassUpdates;

    TestAdapter mTestAdapter;

    private TestAdapter mPreProcessClone;
    // we clone adapter pre-process to run operations to see result

    private List<TestAdapter.Item> mPreLayoutItems;

    private StringBuilder mLog = new StringBuilder();

    @Rule
    public TestWatcher reportErrorLog = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            System.out.println(mLog.toString());
        }

        @Override
        protected void succeeded(Description description) {
        }
    };

    @Before
    public void cleanState() {
        mLog.setLength(0);
        mPreLayoutItems = new ArrayList<>();
        mViewHolders = new ArrayList<>();
        mFirstPassUpdates = new ArrayList<>();
        mSecondPassUpdates = new ArrayList<>();
        mPreProcessClone = null;
        mAdapterHelper = new AdapterHelper(new AdapterHelper.Callback() {
            @Override
            public RecyclerView.ViewHolder findViewHolder(int position) {
                for (ViewHolder vh : mViewHolders) {
                    if (vh.mPosition == position && !vh.isRemoved()) {
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
                        holder.flagRemovedAndOffsetPosition(positionStart - 1, -itemCount, true);
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
                        holder.flagRemovedAndOffsetPosition(positionStart - 1, -itemCount, false);
                    }
                }
            }

            @Override
            public void markViewHoldersUpdated(int positionStart, int itemCount, Object payload) {
                final int positionEnd = positionStart + itemCount;
                for (ViewHolder holder : mViewHolders) {
                    if (holder.mPosition >= positionStart && holder.mPosition < positionEnd) {
                        holder.addFlags(ViewHolder.FLAG_UPDATE);
                        holder.addChangePayload(payload);
                    }
                }
            }

            @Override
            public void onDispatchFirstPass(AdapterHelper.UpdateOp updateOp) {
                if (DEBUG) {
                    log("first pass:" + updateOp.toString());
                }
                for (ViewHolder viewHolder : mViewHolders) {
                    for (int i = 0; i < updateOp.itemCount; i++) {
                        // events are dispatched before view holders are updated for consistency
                        assertFalse("update op should not match any existing view holders",
                                viewHolder.getLayoutPosition() == updateOp.positionStart + i);
                    }
                }

                mFirstPassUpdates.add(updateOp);
            }

            @Override
            public void onDispatchSecondPass(AdapterHelper.UpdateOp updateOp) {
                if (DEBUG) {
                    log("second pass:" + updateOp.toString());
                }
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

            @Override
            public void offsetPositionsForMove(int from, int to) {
                final int start, end, inBetweenOffset;
                if (from < to) {
                    start = from;
                    end = to;
                    inBetweenOffset = -1;
                } else {
                    start = to;
                    end = from;
                    inBetweenOffset = 1;
                }
                for (ViewHolder holder : mViewHolders) {
                    if (holder == null || holder.mPosition < start || holder.mPosition > end) {
                        continue;
                    }
                    if (holder.mPosition == from) {
                        holder.offsetPosition(to - from, false);
                    } else {
                        holder.offsetPosition(inBetweenOffset, false);
                    }
                }
            }
        }, true);
    }

    void log(String msg) {
        if (mCollectLogs) {
            mLog.append(msg).append("\n");
        } else {
            System.out.println(TAG + ":" + msg);
        }
    }

    void setupBasic(int count, int visibleStart, int visibleCount) {
        if (DEBUG) {
            log("setupBasic(" + count + "," + visibleStart + "," + visibleCount + ");");
        }
        mTestAdapter = new TestAdapter(count, mAdapterHelper);
        for (int i = 0; i < visibleCount; i++) {
            addViewHolder(visibleStart + i);
        }
        mPreProcessClone = mTestAdapter.createCopy();
    }

    private void addViewHolder(int position) {
        MockViewHolder viewHolder = new MockViewHolder();
        viewHolder.mPosition = position;
        viewHolder.mItem = mTestAdapter.mItems.get(position);
        mViewHolders.add(viewHolder);
    }

    @Test
    public void testChangeAll() throws Exception {
        try {
            setupBasic(5, 0, 3);
            up(0, 5);
            mAdapterHelper.preProcess();
        } catch (Throwable t) {
            throw new Exception(mLog.toString());
        }
    }

    @Test
    public void testFindPositionOffsetInPreLayout() {
        setupBasic(50, 25, 10);
        rm(24, 5);
        mAdapterHelper.preProcess();
        // since 25 is invisible, we offset by one while checking
        assertEquals("find position for view 23",
                23, mAdapterHelper.findPositionOffset(23));
        assertEquals("find position for view 24",
                -1, mAdapterHelper.findPositionOffset(24));
        assertEquals("find position for view 25",
                -1, mAdapterHelper.findPositionOffset(25));
        assertEquals("find position for view 26",
                -1, mAdapterHelper.findPositionOffset(26));
        assertEquals("find position for view 27",
                -1, mAdapterHelper.findPositionOffset(27));
        assertEquals("find position for view 28",
                24, mAdapterHelper.findPositionOffset(28));
        assertEquals("find position for view 29",
                25, mAdapterHelper.findPositionOffset(29));
    }

    @Test
    public void testNotifyAfterPre() {
        setupBasic(10, 2, 3);
        add(2, 1);
        mAdapterHelper.preProcess();
        add(3, 1);
        mAdapterHelper.consumeUpdatesInOnePass();
        mPreProcessClone.applyOps(mFirstPassUpdates, mTestAdapter);
        mPreProcessClone.applyOps(mSecondPassUpdates, mTestAdapter);
        assertAdaptersEqual(mTestAdapter, mPreProcessClone);
    }

    @Test
    public void testSinglePass() {
        setupBasic(10, 2, 3);
        add(2, 1);
        rm(1, 2);
        add(1, 5);
        mAdapterHelper.consumeUpdatesInOnePass();
        assertDispatch(0, 3);
    }

    @Test
    public void testDeleteVisible() {
        setupBasic(10, 2, 3);
        rm(2, 1);
        preProcess();
        assertDispatch(0, 1);
    }

    @Test
    public void testDeleteInvisible() {
        setupBasic(10, 3, 4);
        rm(2, 1);
        preProcess();
        assertDispatch(1, 0);
    }

    @Test
    public void testAddCount() {
        setupBasic(0, 0, 0);
        add(0, 1);
        assertEquals(1, mAdapterHelper.mPendingUpdates.size());
    }

    @Test
    public void testDeleteCount() {
        setupBasic(1, 0, 0);
        rm(0, 1);
        assertEquals(1, mAdapterHelper.mPendingUpdates.size());
    }

    @Test
    public void testAddProcess() {
        setupBasic(0, 0, 0);
        add(0, 1);
        preProcess();
        assertEquals(0, mAdapterHelper.mPendingUpdates.size());
    }

    @Test
    public void testAddRemoveSeparate() {
        setupBasic(10, 2, 2);
        add(6, 1);
        rm(5, 1);
        preProcess();
        assertDispatch(1, 1);
    }

    @Test
    public void testScenario1() {
        setupBasic(10, 3, 2);
        rm(4, 1);
        rm(3, 1);
        rm(3, 1);
        preProcess();
        assertDispatch(1, 2);
    }

    @Test
    public void testDivideDelete() {
        setupBasic(10, 3, 4);
        rm(2, 2);
        preProcess();
        assertDispatch(1, 1);
    }

    @Test
    public void testScenario2() {
        setupBasic(10, 3, 3); // 3-4-5
        add(4, 2); // 3 a b 4 5
        rm(0, 1); // (0) 3(2) a(3) b(4) 4(3) 5(4)
        rm(1, 3); // (1,2) (x) a(1) b(2) 4(3)
        preProcess();
        assertDispatch(2, 2);
    }

    @Test
    public void testScenario3() {
        setupBasic(10, 2, 2);
        rm(0, 5);
        preProcess();
        assertDispatch(2, 1);
        assertOps(mFirstPassUpdates, rmOp(0, 2), rmOp(2, 1));
        assertOps(mSecondPassUpdates, rmOp(0, 2));
    }
    // TODO test MOVE then remove items in between.
    // TODO test MOVE then remove it, make sure it is not dispatched

    @Test
    public void testScenario4() {
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

    @Test
    public void testScenario5() {
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

    @Test
    public void testScenario6() {
//        setupBasic(47, 19, 24);
//        mv(11, 12);
//        add(24, 16);
//        rm(9, 3);
        setupBasic(10, 5, 3);
        mv(2, 3);
        add(6, 4);
        rm(4, 1);
        preProcess();
    }

    @Test
    public void testScenario8() {
        setupBasic(68, 51, 13);
        mv(22, 11);
        mv(22, 52);
        rm(37, 19);
        add(12, 38);
        preProcess();
    }

    @Test
    public void testScenario9() {
        setupBasic(44, 3, 7);
        add(7, 21);
        rm(31, 3);
        rm(32, 11);
        mv(29, 5);
        mv(30, 32);
        add(25, 32);
        rm(15, 66);
        preProcess();
    }

    @Test
    public void testScenario10() {
        setupBasic(14, 10, 3);
        rm(4, 4);
        add(5, 11);
        mv(5, 18);
        rm(2, 9);
        preProcess();
    }

    @Test
    public void testScenario11() {
        setupBasic(78, 3, 64);
        mv(34, 28);
        add(1, 11);
        rm(9, 74);
        preProcess();
    }

    @Test
    public void testScenario12() {
        setupBasic(38, 9, 7);
        rm(26, 3);
        mv(29, 15);
        rm(30, 1);
        preProcess();
    }

    @Test
    public void testScenario13() {
        setupBasic(49, 41, 3);
        rm(30, 13);
        add(4, 10);
        mv(3, 38);
        mv(20, 17);
        rm(18, 23);
        preProcess();
    }

    @Test
    public void testScenario14() {
        setupBasic(24, 3, 11);
        rm(2, 15);
        mv(2, 1);
        add(2, 34);
        add(11, 3);
        rm(10, 25);
        rm(13, 6);
        rm(4, 4);
        rm(6, 4);
        preProcess();
    }

    @Test
    public void testScenario15() {
        setupBasic(10, 8, 1);
        mv(6, 1);
        mv(1, 4);
        rm(3, 1);
        preProcess();
    }

    @Test
    public void testScenario16() {
        setupBasic(10, 3, 3);
        rm(2, 1);
        rm(1, 7);
        rm(0, 1);
        preProcess();
    }

    @Test
    public void testScenario17() {
        setupBasic(10, 8, 1);
        mv(1, 0);
        mv(5, 1);
        rm(1, 7);
        preProcess();
    }

    @Test
    public void testScenario18() {
        setupBasic(10, 1, 4);
        add(2, 11);
        rm(16, 1);
        add(3, 1);
        rm(9, 10);
        preProcess();
    }

    @Test
    public void testScenario19() {
        setupBasic(10, 8, 1);
        mv(9, 7);
        mv(9, 3);
        rm(5, 4);
        preProcess();
    }

    @Test
    public void testScenario20() {
        setupBasic(10, 7, 1);
        mv(9, 1);
        mv(3, 9);
        rm(7, 2);
        preProcess();
    }

    @Test
    public void testScenario21() {
        setupBasic(10, 5, 2);
        mv(1, 0);
        mv(9, 1);
        rm(2, 3);
        preProcess();
    }

    @Test
    public void testScenario22() {
        setupBasic(10, 7, 2);
        add(2, 16);
        mv(20, 9);
        rm(17, 6);
        preProcess();
    }

    @Test
    public void testScenario23() {
        setupBasic(10, 5, 3);
        mv(9, 6);
        add(4, 15);
        rm(21, 3);
        preProcess();
    }

    @Test
    public void testScenario24() {
        setupBasic(10, 1, 6);
        add(6, 5);
        mv(14, 6);
        rm(7, 6);
        preProcess();
    }

    @Test
    public void testScenario25() {
        setupBasic(10, 3, 4);
        mv(3, 9);
        rm(5, 4);
        preProcess();
    }

    @Test
    public void testScenario25a() {
        setupBasic(10, 3, 4);
        rm(6, 4);
        mv(3, 5);
        preProcess();
    }

    @Test
    public void testScenario26() {
        setupBasic(10, 4, 4);
        rm(3, 5);
        mv(2, 0);
        mv(1, 0);
        rm(1, 1);
        mv(0, 2);
        preProcess();
    }

    @Test
    public void testScenario27() {
        setupBasic(10, 0, 3);
        mv(9, 4);
        mv(8, 4);
        add(7, 6);
        rm(5, 5);
        preProcess();
    }

    @Test
    public void testScenerio28() {
        setupBasic(10, 4, 1);
        mv(8, 6);
        rm(8, 1);
        mv(7, 5);
        rm(3, 3);
        rm(1, 4);
        preProcess();
    }

    @Test
    public void testScenerio29() {
        setupBasic(10, 6, 3);
        mv(3, 6);
        up(6, 2);
        add(5, 5);
    }

    @Test
    public void testScenerio30() {
        mCollectLogs = true;
        setupBasic(10, 3, 1);
        rm(3, 2);
        rm(2, 5);
        preProcess();
    }

    @Test
    public void testScenerio31() {
        mCollectLogs = true;
        setupBasic(10, 3, 1);
        rm(3, 1);
        rm(2, 3);
        preProcess();
    }

    @Test
    public void testScenerio32() {
        setupBasic(10, 8, 1);
        add(9, 2);
        add(7, 39);
        up(0, 39);
        mv(36, 20);
        add(1, 48);
        mv(22, 98);
        mv(96, 29);
        up(36, 29);
        add(60, 36);
        add(127, 34);
        rm(142, 22);
        up(12, 69);
        up(116, 13);
        up(118, 19);
        mv(94, 69);
        up(98, 21);
        add(89, 18);
        rm(94, 70);
        up(71, 8);
        rm(54, 26);
        add(2, 20);
        mv(78, 84);
        mv(56, 2);
        mv(1, 79);
        rm(76, 7);
        rm(57, 12);
        rm(30, 27);
        add(24, 13);
        add(21, 5);
        rm(11, 27);
        rm(32, 1);
        up(0, 5);
        mv(14, 9);
        rm(15, 12);
        up(19, 1);
        rm(7, 1);
        mv(10, 4);
        up(4, 3);
        rm(16, 1);
        up(13, 5);
        up(2, 8);
        add(10, 19);
        add(15, 42);
        preProcess();
    }

    @Test
    public void testScenerio33() throws Throwable {
        try {
            mCollectLogs = true;
            setupBasic(10, 7, 1);
            mv(0, 6);
            up(0, 7);
            preProcess();
        } catch (Throwable t) {
            throw new Throwable(t.getMessage() + "\n" + mLog.toString());
        }
    }

    @Test
    public void testScenerio34() {
        setupBasic(10, 6, 1);
        mv(9, 7);
        rm(5, 2);
        up(4, 3);
        preProcess();
    }

    @Test
    public void testScenerio35() {
        setupBasic(10, 4, 4);
        mv(1, 4);
        up(2, 7);
        up(0, 1);
        preProcess();
    }

    @Test
    public void testScenerio36() {
        setupBasic(10, 7, 2);
        rm(4, 1);
        mv(1, 6);
        up(4, 4);
        preProcess();
    }

    @Test
    public void testScenerio37() throws Throwable {
        try {
            mCollectLogs = true;
            setupBasic(10, 5, 2);
            mv(3, 6);
            rm(4, 4);
            rm(3, 2);
            preProcess();
        } catch (Throwable t) {
            throw new Throwable(t.getMessage() + "\n" + mLog.toString());
        }
    }

    @Test
    public void testScenerio38() {
        setupBasic(10, 2, 2);
        add(0, 24);
        rm(26, 4);
        rm(1, 24);
        preProcess();
    }

    @Test
    public void testScenerio39() {
        setupBasic(10, 7, 1);
        mv(0, 2);
        rm(8, 1);
        rm(2, 6);
        preProcess();
    }

    @Test
    public void testScenerio40() {
        setupBasic(10, 5, 3);
        rm(5, 4);
        mv(0, 5);
        rm(2, 3);
        preProcess();
    }

    @Test
    public void testScenerio41() {
        setupBasic(10, 7, 2);
        mv(4, 9);
        rm(0, 6);
        rm(0, 1);
        preProcess();
    }

    @Test
    public void testScenerio42() {
        setupBasic(10, 6, 2);
        mv(5, 9);
        rm(5, 1);
        rm(2, 6);
        preProcess();
    }

    @Test
    public void testScenerio43() {
        setupBasic(10, 1, 6);
        mv(6, 8);
        rm(3, 5);
        up(3, 1);
        preProcess();
    }

    @Test
    public void testScenerio44() {
        setupBasic(10, 5, 2);
        mv(6, 4);
        mv(4, 1);
        rm(5, 3);
        preProcess();
    }

    @Test
    public void testScenerio45() {
        setupBasic(10, 4, 2);
        rm(1, 4);
        preProcess();
    }

    @Test
    public void testScenerio46() {
        setupBasic(10, 4, 3);
        up(6, 1);
        mv(8, 0);
        rm(2, 7);
        preProcess();
    }

    @Test
    public void testMoveAdded() {
        setupBasic(10, 2, 2);
        add(3, 5);
        mv(4, 2);
        preProcess();
    }

    @Test
    public void testPayloads() {
        setupBasic(10, 2, 2);
        up(3, 3, "payload");
        preProcess();
        assertOps(mFirstPassUpdates, upOp(4, 2, "payload"));
        assertOps(mSecondPassUpdates, upOp(3, 1, "payload"));
    }

    @Test
    public void testRandom() throws Throwable {
        mCollectLogs = true;
        Random random = new Random(System.nanoTime());
        for (int i = 0; i < 100; i++) {
            try {
                log("running random test " + i);
                randomTest(random, Math.max(40, 10 + nextInt(random, i)));
            } catch (Throwable t) {
                throw new Throwable("failure at random test " + i + "\n" + t.getMessage()
                        + "\n" + mLog.toString(), t);
            }
        }
    }

    private void randomTest(Random random, int opCount) {
        cleanState();
        if (DEBUG) {
            log("randomTest");
        }
        final int count = 10;// + nextInt(random,100);
        final int start = nextInt(random, count - 1);
        final int layoutCount = Math.max(1, nextInt(random, count - start));
        setupBasic(count, start, layoutCount);

        while (opCount-- > 0) {
            final int op = nextInt(random, 5);
            switch (op) {
                case 0:
                    if (mTestAdapter.mItems.size() > 1) {
                        int s = nextInt(random, mTestAdapter.mItems.size() - 1);
                        int len = Math.max(1, nextInt(random, mTestAdapter.mItems.size() - s));
                        rm(s, len);
                    }
                    break;
                case 1:
                    int s = mTestAdapter.mItems.size() == 0 ? 0 :
                            nextInt(random, mTestAdapter.mItems.size());
                    add(s, nextInt(random, 50));
                    break;
                case 2:
                    if (mTestAdapter.mItems.size() >= 2) {
                        int from = nextInt(random, mTestAdapter.mItems.size());
                        int to;
                        do {
                            to = nextInt(random, mTestAdapter.mItems.size());
                        } while (to == from);
                        mv(from, to);
                    }
                    break;
                case 3:
                    if (mTestAdapter.mItems.size() > 1) {
                        s = nextInt(random, mTestAdapter.mItems.size() - 1);
                        int len = Math.max(1, nextInt(random, mTestAdapter.mItems.size() - s));
                        up(s, len);
                    }
                    break;
                case 4:
                    if (mTestAdapter.mItems.size() > 1) {
                        s = nextInt(random, mTestAdapter.mItems.size() - 1);
                        int len = Math.max(1, nextInt(random, mTestAdapter.mItems.size() - s));
                        up(s, len, Integer.toString(s));
                    }
                    break;
            }
        }
        preProcess();
    }

    private int nextInt(Random random, int n) {
        if (n == 0) {
            return 0;
        }
        return random.nextInt(n);
    }

    private void assertOps(List<AdapterHelper.UpdateOp> actual,
            AdapterHelper.UpdateOp... expected) {
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual.get(i));
        }
    }

    private void assertDispatch(int firstPass, int secondPass) {
        assertEquals(firstPass, mFirstPassUpdates.size());
        assertEquals(secondPass, mSecondPassUpdates.size());
    }

    private void preProcess() {
        for (MockViewHolder vh : mViewHolders) {
            final int ind = mTestAdapter.mItems.indexOf(vh.mItem);
            assertEquals("actual adapter position should match", ind,
                    mAdapterHelper.applyPendingUpdatesToPosition(vh.mPosition));
        }
        mAdapterHelper.preProcess();
        for (int i = 0; i < mPreProcessClone.mItems.size(); i++) {
            TestAdapter.Item item = mPreProcessClone.mItems.get(i);
            final int preLayoutIndex = mPreLayoutItems.indexOf(item);
            final int endIndex = mTestAdapter.mItems.indexOf(item);
            if (preLayoutIndex != -1) {
                assertEquals("find position offset should work properly for existing elements" + i
                        + " at pre layout position " + preLayoutIndex + " and post layout position "
                        + endIndex, endIndex, mAdapterHelper.findPositionOffset(preLayoutIndex));
            }
        }
        // make sure visible view holders still have continuous positions
        final StringBuilder vhLogBuilder = new StringBuilder();
        for (ViewHolder vh : mViewHolders) {
            vhLogBuilder.append("\n").append(vh.toString());
        }
        if (mViewHolders.size() > 0) {
            final String vhLog = vhLogBuilder.toString();
            final int start = mViewHolders.get(0).getLayoutPosition();
            for (int i = 1; i < mViewHolders.size(); i++) {
                assertEquals("view holder positions should be continious in pre-layout" + vhLog,
                        start + i, mViewHolders.get(i).getLayoutPosition());
            }
        }
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

    private AdapterHelper.UpdateOp op(int cmd, int start, int count) {
        return new AdapterHelper.UpdateOp(cmd, start, count, null);
    }

    private AdapterHelper.UpdateOp op(int cmd, int start, int count, Object payload) {
        return new AdapterHelper.UpdateOp(cmd, start, count, payload);
    }

    private AdapterHelper.UpdateOp rmOp(int start, int count) {
        return op(AdapterHelper.UpdateOp.REMOVE, start, count);
    }

    private AdapterHelper.UpdateOp upOp(int start, int count, @SuppressWarnings
            ("SameParameterValue") Object payload) {
        return op(AdapterHelper.UpdateOp.UPDATE, start, count, payload);
    }

    void add(int start, int count) {
        if (DEBUG) {
            log("add(" + start + "," + count + ");");
        }
        mTestAdapter.add(start, count);
    }

    private boolean isItemLaidOut(int pos) {
        for (ViewHolder viewHolder : mViewHolders) {
            if (viewHolder.mOldPosition == pos) {
                return true;
            }
        }
        return false;
    }

    private void mv(int from, int to) {
        if (DEBUG) {
            log("mv(" + from + "," + to + ");");
        }
        mTestAdapter.move(from, to);
    }

    private void rm(int start, int count) {
        if (DEBUG) {
            log("rm(" + start + "," + count + ");");
        }
        for (int i = start; i < start + count; i++) {
            if (!isItemLaidOut(i)) {
                TestAdapter.Item item = mTestAdapter.mItems.get(i);
                mPreLayoutItems.remove(item);
            }
        }
        mTestAdapter.remove(start, count);
    }

    void up(int start, int count) {
        if (DEBUG) {
            log("up(" + start + "," + count + ");");
        }
        mTestAdapter.update(start, count);
    }

    void up(int start, int count, Object payload) {
        if (DEBUG) {
            log("up(" + start + "," + count + "," + payload + ");");
        }
        mTestAdapter.update(start, count, payload);
    }

    static class TestAdapter {

        List<Item> mItems;

        final AdapterHelper mAdapterHelper;

        Queue<Item> mPendingAdded;

        public TestAdapter(int initialCount, AdapterHelper container) {
            mItems = new ArrayList<>();
            mAdapterHelper = container;
            mPendingAdded = new LinkedList<>();
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
                    AdapterHelper.UpdateOp.ADD, index, count, null
            ));
        }

        public void move(int from, int to) {
            mItems.add(to, mItems.remove(from));
            mAdapterHelper.addUpdateOp(new AdapterHelper.UpdateOp(
                    AdapterHelper.UpdateOp.MOVE, from, to, null
            ));
        }

        public void remove(int index, int count) {
            for (int i = 0; i < count; i++) {
                mItems.remove(index);
            }
            mAdapterHelper.addUpdateOp(new AdapterHelper.UpdateOp(
                    AdapterHelper.UpdateOp.REMOVE, index, count, null
            ));
        }

        public void update(int index, int count) {
            update(index, count, null);
        }

        public void update(int index, int count, Object payload) {
            for (int i = 0; i < count; i++) {
                mItems.get(index + i).update(payload);
            }
            mAdapterHelper.addUpdateOp(new AdapterHelper.UpdateOp(
                    AdapterHelper.UpdateOp.UPDATE, index, count, payload
            ));
        }

        TestAdapter createCopy() {
            TestAdapter adapter = new TestAdapter(0, mAdapterHelper);
            adapter.mItems.addAll(mItems);
            return adapter;
        }

        void applyOps(List<AdapterHelper.UpdateOp> updates,
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
                            mItems.get(op.positionStart + i).handleUpdate(op.payload);
                        }
                        break;
                    case AdapterHelper.UpdateOp.MOVE:
                        mItems.add(op.itemCount, mItems.remove(op.positionStart));
                        break;
                }
            }
        }

        private Item consumeNextAdded() {
            return mPendingAdded.remove();
        }

        public static class Item {

            private static AtomicInteger itemCounter = new AtomicInteger();

            @SuppressWarnings("unused")
            private final int id;

            private int mVersionCount = 0;

            private ArrayList<Object> mPayloads = new ArrayList<>();

            public Item() {
                id = itemCounter.incrementAndGet();
            }

            public void update(Object payload) {
                mPayloads.add(payload);
                mVersionCount++;
            }

            void handleUpdate(Object payload) {
                assertSame(payload, mPayloads.get(0));
                mPayloads.remove(0);
                mVersionCount--;
            }

            int getUpdateCount() {
                return mVersionCount;
            }
        }
    }

    static class MockViewHolder extends RecyclerView.ViewHolder {
        TestAdapter.Item mItem;

        MockViewHolder() {
            super(Mockito.mock(View.class));
        }

        @Override
        public String toString() {
            return mItem == null ? "null" : mItem.toString();
        }
    }
}
