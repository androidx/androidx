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

import static androidx.recyclerview.widget.AdapterHelper.UpdateOp.ADD;
import static androidx.recyclerview.widget.AdapterHelper.UpdateOp.MOVE;
import static androidx.recyclerview.widget.AdapterHelper.UpdateOp.REMOVE;
import static androidx.recyclerview.widget.AdapterHelper.UpdateOp.UPDATE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.support.test.filters.SmallTest;
import android.util.Log;

import androidx.recyclerview.widget.AdapterHelper.UpdateOp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@RunWith(JUnit4.class)
@SmallTest
public class OpReorderTest {

    private static final String TAG = "OpReorderTest";

    List<UpdateOp> mUpdateOps = new ArrayList<UpdateOp>();
    List<Item> mAddedItems = new ArrayList<Item>();
    List<Item> mRemovedItems = new ArrayList<Item>();
    Set<UpdateOp> mRecycledOps = new HashSet<UpdateOp>();
    static Random random = new Random(System.nanoTime());

    OpReorderer mOpReorderer = new OpReorderer(new OpReorderer.Callback() {
        @Override
        public UpdateOp obtainUpdateOp(int cmd, int startPosition, int itemCount, Object payload) {
            return new UpdateOp(cmd, startPosition, itemCount, payload);
        }

        @Override
        public void recycleUpdateOp(UpdateOp op) {
            mRecycledOps.add(op);
        }
    });

    int itemCount = 10;
    int updatedItemCount = 0;

    public void setup(int count) {
        itemCount = count;
        updatedItemCount = itemCount;
    }

    @Before
    public void setUp() throws Exception {
        cleanState();
    }

    void cleanState() {
        mUpdateOps = new ArrayList<UpdateOp>();
        mAddedItems = new ArrayList<Item>();
        mRemovedItems = new ArrayList<Item>();
        mRecycledOps = new HashSet<UpdateOp>();
        Item.idCounter = 0;
    }

    @Test
    public void testMoveRemoved() throws Exception {
        setup(10);
        mv(3, 8);
        rm(7, 3);
        process();
    }

    @Test
    public void testMoveRemove() throws Exception {
        setup(10);
        mv(3, 8);
        rm(3, 5);
        process();
    }

    @Test
    public void test1() {
        setup(10);
        mv(3, 5);
        rm(3, 4);
        process();
    }

    @Test
    public void test2() {
        setup(5);
        mv(1, 3);
        rm(1, 1);
        process();
    }

    @Test
    public void test3() {
        setup(5);
        mv(0, 4);
        rm(2, 1);
        process();
    }

    @Test
    public void test4() {
        setup(5);
        mv(3, 0);
        rm(3, 1);
        process();
    }

    @Test
    public void test5() {
        setup(10);
        mv(8, 1);
        rm(6, 3);
        process();
    }

    @Test
    public void test6() {
        setup(5);
        mv(1, 3);
        rm(0, 3);
        process();
    }

    @Test
    public void test7() {
        setup(5);
        mv(3, 4);
        rm(3, 1);
        process();
    }

    @Test
    public void test8() {
        setup(5);
        mv(4, 3);
        rm(3, 1);
        process();
    }

    @Test
    public void test9() {
        setup(5);
        mv(2, 0);
        rm(2, 2);
        process();
    }

    @Test
    public void testRandom() throws Exception {
        for (int i = 0; i < 150; i++) {
            try {
                cleanState();
                setup(50);
                for (int j = 0; j < 50; j++) {
                    randOp(nextInt(random, nextInt(random, 4)));
                }
                Log.d(TAG, "running random test " + i);
                process();
            } catch (Throwable t) {
                throw new Exception(t.getMessage() + "\n" + opsToString(mUpdateOps));
            }
        }
    }

    @Test
    public void testRandomMoveRemove() throws Exception {
        for (int i = 0; i < 1000; i++) {
            try {
                cleanState();
                setup(5);
                orderedRandom(MOVE, REMOVE);
                process();
            } catch (Throwable t) {
                throw new Exception(t.getMessage() + "\n" + opsToString(mUpdateOps));
            }
        }
    }

    @Test
    public void testRandomMoveAdd() throws Exception {
        for (int i = 0; i < 1000; i++) {
            try {
                cleanState();
                setup(5);
                orderedRandom(MOVE, ADD);
                process();
            } catch (Throwable t) {
                throw new Exception(t.getMessage() + "\n" + opsToString(mUpdateOps));
            }
        }
    }

    @Test
    public void testRandomMoveUpdate() throws Exception {
        for (int i = 0; i < 1000; i++) {
            try {
                cleanState();
                setup(5);
                orderedRandom(MOVE, UPDATE);
                process();
            } catch (Throwable t) {
                throw new Exception(t.getMessage() + "\n" + opsToString(mUpdateOps));
            }
        }
    }

    private String opsToString(List<UpdateOp> updateOps) {
        StringBuilder sb = new StringBuilder();
        for (UpdateOp op : updateOps) {
            sb.append("\n").append(op.toString());
        }
        return sb.append("\n").toString();
    }

    public void orderedRandom(int... ops) {
        for (int op : ops) {
            randOp(op);
        }
    }

    void randOp(int cmd) {
        switch (cmd) {
            case REMOVE:
                if (updatedItemCount > 1) {
                    int s = nextInt(random, updatedItemCount - 1);
                    int len = Math.max(1, nextInt(random, updatedItemCount - s));
                    rm(s, len);
                }
                break;
            case ADD:
                int s = updatedItemCount == 0 ? 0 : nextInt(random, updatedItemCount);
                add(s, nextInt(random, 50));
                break;
            case MOVE:
                if (updatedItemCount >= 2) {
                    int from = nextInt(random, updatedItemCount);
                    int to;
                    do {
                        to = nextInt(random, updatedItemCount);
                    } while (to == from);
                    mv(from, to);
                }
                break;
            case UPDATE:
                if (updatedItemCount > 1) {
                    s = nextInt(random, updatedItemCount - 1);
                    int len = Math.max(1, nextInt(random, updatedItemCount - s));
                    up(s, len);
                }
                break;
        }
    }

    int nextInt(Random random, int n) {
        if (n == 0) {
            return 0;
        }
        return random.nextInt(n);
    }

    UpdateOp rm(int start, int count) {
        updatedItemCount -= count;
        return record(new UpdateOp(REMOVE, start, count, null));
    }

    UpdateOp mv(int from, int to) {
        return record(new UpdateOp(MOVE, from, to, null));
    }

    UpdateOp add(int start, int count) {
        updatedItemCount += count;
        return record(new UpdateOp(ADD, start, count, null));
    }

    UpdateOp up(int start, int count) {
        return record(new UpdateOp(UPDATE, start, count, null));
    }

    UpdateOp record(UpdateOp op) {
        mUpdateOps.add(op);
        return op;
    }

    void process() {
        List<Item> items = new ArrayList<Item>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            items.add(Item.create());
        }
        List<Item> clones = new ArrayList<Item>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            clones.add(Item.clone(items.get(i)));
        }
        List<UpdateOp> rewritten = rewriteOps(mUpdateOps);

        assertAllMovesAtTheEnd(rewritten);

        apply(items, mUpdateOps);
        List<Item> originalAdded = mAddedItems;
        List<Item> originalRemoved = mRemovedItems;
        if (originalAdded.size() > 0) {
            Item.idCounter = originalAdded.get(0).id;
        }
        mAddedItems = new ArrayList<Item>();
        mRemovedItems = new ArrayList<Item>();
        apply(clones, rewritten);

        // now check equality
        assertListsIdentical(items, clones);
        assertHasTheSameItems(originalAdded, mAddedItems);
        assertHasTheSameItems(originalRemoved, mRemovedItems);

        assertRecycledOpsAreNotReused(items);
        assertRecycledOpsAreNotReused(clones);
    }

    private void assertRecycledOpsAreNotReused(List<Item> items) {
        for (Item item : items) {
            assertFalse(mRecycledOps.contains(item));
        }
    }

    private void assertAllMovesAtTheEnd(List<UpdateOp> ops) {
        boolean foundMove = false;
        for (UpdateOp op : ops) {
            if (op.cmd == MOVE) {
                foundMove = true;
            } else {
                assertFalse(foundMove);
            }
        }
    }

    private void assertHasTheSameItems(List<Item> items,
            List<Item> clones) {
        String log = "has the same items\n" + toString(items) + "--\n" + toString(clones);
        assertEquals(log, items.size(), clones.size());
        for (Item item : items) {
            for (Item clone : clones) {
                if (item.id == clone.id && item.version == clone.version) {
                    clones.remove(clone);
                    break;
                }
            }
        }
        assertEquals(log, 0, clones.size());
    }

    private void assertListsIdentical(List<Item> items, List<Item> clones) {
        String log = "is identical\n" + toString(items) + "--\n" + toString(clones);
        assertEquals(items.size(), clones.size());
        for (int i = 0; i < items.size(); i++) {
            Item.assertIdentical(log, items.get(i), clones.get(i));
        }
    }

    private void apply(List<Item> items, List<UpdateOp> updateOps) {
        for (UpdateOp op : updateOps) {
            switch (op.cmd) {
                case UpdateOp.ADD:
                    for (int i = 0; i < op.itemCount; i++) {
                        final Item newItem = Item.create();
                        mAddedItems.add(newItem);
                        items.add(op.positionStart + i, newItem);
                    }
                    break;
                case UpdateOp.REMOVE:
                    for (int i = 0; i < op.itemCount; i++) {
                        mRemovedItems.add(items.remove(op.positionStart));
                    }
                    break;
                case UpdateOp.MOVE:
                    items.add(op.itemCount, items.remove(op.positionStart));
                    break;
                case UpdateOp.UPDATE:
                    for (int i = 0; i < op.itemCount; i++) {
                        final int index = op.positionStart + i;
                        items.get(index).version = items.get(index).version + 1;
                    }
                    break;
            }
        }
    }

    private List<UpdateOp> rewriteOps(List<UpdateOp> updateOps) {
        List<UpdateOp> copy = new ArrayList<UpdateOp>();
        for (UpdateOp op : updateOps) {
            copy.add(new UpdateOp(op.cmd, op.positionStart, op.itemCount, null));
        }
        mOpReorderer.reorderOps(copy);
        return copy;
    }

    @Test
    public void testSwapMoveRemove_1() {
        mv(10, 15);
        rm(2, 3);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(2, mUpdateOps.size());
        assertEquals(mv(7, 12), mUpdateOps.get(1));
        assertEquals(rm(2, 3), mUpdateOps.get(0));
    }

    @Test
    public void testSwapMoveRemove_2() {
        mv(3, 8);
        rm(4, 2);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(2, mUpdateOps.size());
        assertEquals(rm(5, 2), mUpdateOps.get(0));
        assertEquals(mv(3, 6), mUpdateOps.get(1));
    }

    @Test
    public void testSwapMoveRemove_3() {
        mv(3, 8);
        rm(3, 2);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(2, mUpdateOps.size());
        assertEquals(rm(4, 2), mUpdateOps.get(0));
        assertEquals(mv(3, 6), mUpdateOps.get(1));
    }

    @Test
    public void testSwapMoveRemove_4() {
        mv(3, 8);
        rm(2, 3);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(3, mUpdateOps.size());
        assertEquals(rm(4, 2), mUpdateOps.get(0));
        assertEquals(rm(2, 1), mUpdateOps.get(1));
        assertEquals(mv(2, 5), mUpdateOps.get(2));
    }

    @Test
    public void testSwapMoveRemove_5() {
        mv(3, 0);
        rm(2, 3);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(3, mUpdateOps.size());
        assertEquals(rm(4, 1), mUpdateOps.get(0));
        assertEquals(rm(1, 2), mUpdateOps.get(1));
        assertEquals(mv(1, 0), mUpdateOps.get(2));
    }

    @Test
    public void testSwapMoveRemove_6() {
        mv(3, 10);
        rm(2, 3);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(3, mUpdateOps.size());
        assertEquals(rm(4, 2), mUpdateOps.get(0));
        assertEquals(rm(2, 1), mUpdateOps.get(1));
    }

    @Test
    public void testSwapMoveRemove_7() {
        mv(3, 2);
        rm(6, 2);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(2, mUpdateOps.size());
        assertEquals(rm(6, 2), mUpdateOps.get(0));
        assertEquals(mv(3, 2), mUpdateOps.get(1));
    }

    @Test
    public void testSwapMoveRemove_8() {
        mv(3, 4);
        rm(3, 1);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(1, mUpdateOps.size());
        assertEquals(rm(4, 1), mUpdateOps.get(0));
    }

    @Test
    public void testSwapMoveRemove_9() {
        mv(3, 4);
        rm(4, 1);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(1, mUpdateOps.size());
        assertEquals(rm(3, 1), mUpdateOps.get(0));
    }

    @Test
    public void testSwapMoveRemove_10() {
        mv(1, 3);
        rm(0, 3);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(2, mUpdateOps.size());
        assertEquals(rm(2, 2), mUpdateOps.get(0));
        assertEquals(rm(0, 1), mUpdateOps.get(1));
    }

    @Test
    public void testSwapMoveRemove_11() {
        mv(3, 8);
        rm(7, 3);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(2, mUpdateOps.size());
        assertEquals(rm(3, 1), mUpdateOps.get(0));
        assertEquals(rm(7, 2), mUpdateOps.get(1));
    }

    @Test
    public void testSwapMoveRemove_12() {
        mv(1, 3);
        rm(2, 1);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(2, mUpdateOps.size());
        assertEquals(rm(3, 1), mUpdateOps.get(0));
        assertEquals(mv(1, 2), mUpdateOps.get(1));
    }

    @Test
    public void testSwapMoveRemove_13() {
        mv(1, 3);
        rm(1, 2);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(1, mUpdateOps.size());
        assertEquals(rm(2, 2), mUpdateOps.get(1));
    }

    @Test
    public void testSwapMoveRemove_14() {
        mv(4, 2);
        rm(3, 1);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(2, mUpdateOps.size());
        assertEquals(rm(2, 1), mUpdateOps.get(0));
        assertEquals(mv(2, 3), mUpdateOps.get(1));
    }

    @Test
    public void testSwapMoveRemove_15() {
        mv(4, 2);
        rm(3, 2);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(1, mUpdateOps.size());
        assertEquals(rm(2, 2), mUpdateOps.get(0));
    }

    @Test
    public void testSwapMoveRemove_16() {
        mv(2, 3);
        rm(1, 2);
        swapMoveRemove(mUpdateOps, 0);
        assertEquals(2, mUpdateOps.size());
        assertEquals(rm(3, 1), mUpdateOps.get(0));
        assertEquals(rm(1, 1), mUpdateOps.get(1));
    }

    @Test
    public void testSwapMoveUpdate_0() {
        mv(1, 3);
        up(1, 2);
        swapMoveUpdate(mUpdateOps, 0);
        assertEquals(2, mUpdateOps.size());
        assertEquals(up(2, 2), mUpdateOps.get(0));
        assertEquals(mv(1, 3), mUpdateOps.get(1));
    }

    @Test
    public void testSwapMoveUpdate_1() {
        mv(0, 2);
        up(0, 4);
        swapMoveUpdate(mUpdateOps, 0);
        assertEquals(3, mUpdateOps.size());
        assertEquals(up(0, 1), mUpdateOps.get(0));
        assertEquals(up(1, 3), mUpdateOps.get(1));
        assertEquals(mv(0, 2), mUpdateOps.get(2));
    }

    @Test
    public void testSwapMoveUpdate_2() {
        mv(2, 0);
        up(1, 3);
        swapMoveUpdate(mUpdateOps, 0);
        assertEquals(3, mUpdateOps.size());
        assertEquals(up(3, 1), mUpdateOps.get(0));
        assertEquals(up(0, 2), mUpdateOps.get(1));
        assertEquals(mv(2, 0), mUpdateOps.get(2));
    }

    private void swapMoveUpdate(List<UpdateOp> list, int move) {
        mOpReorderer.swapMoveUpdate(list, move, list.get(move), move + 1, list.get(move + 1));
    }

    private void swapMoveRemove(List<UpdateOp> list, int move) {
        mOpReorderer.swapMoveRemove(list, move, list.get(move), move + 1, list.get(move + 1));
    }

    private String toString(List<Item> items) {
        StringBuilder sb = new StringBuilder();
        for (Item item : items) {
            sb.append(item.toString()).append("\n");
        }
        return sb.toString();
    }

    static class Item {

        static int idCounter = 0;
        int id;
        int version;

        Item(int id, int version) {
            this.id = id;
            this.version = version;
        }

        static Item create() {
            return new Item(idCounter++, 1);
        }

        static Item clone(Item other) {
            return new Item(other.id, other.version);
        }

        public static void assertIdentical(String logPrefix, Item item1, Item item2) {
            assertEquals(logPrefix + "\n" + item1 + " vs " + item2, item1.id, item2.id);
            assertEquals(logPrefix + "\n" + item1 + " vs " + item2, item1.version, item2.version);
        }

        @Override
        public String toString() {
            return "Item{" +
                    "id=" + id +
                    ", version=" + version +
                    '}';
        }
    }
}
