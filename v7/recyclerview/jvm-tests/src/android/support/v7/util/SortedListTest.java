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

package android.support.v7.util;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import android.test.suitebuilder.annotation.SmallTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@RunWith(JUnit4.class)
@SmallTest
public class SortedListTest extends TestCase {

    SortedList<Item> mList;
    List<Pair> mAdditions = new ArrayList<Pair>();
    List<Pair> mRemovals = new ArrayList<Pair>();
    List<Pair> mMoves = new ArrayList<Pair>();
    List<Pair> mUpdates = new ArrayList<Pair>();
    private SortedList.Callback<Item> mCallback;
    InsertedCallback<Item> mInsertedCallback;
    ChangedCallback<Item> mChangedCallback;

    private Comparator<? super Item> sItemComparator = new Comparator<Item>() {
        @Override
        public int compare(Item o1, Item o2) {
            return mCallback.compare(o1, o2);
        }
    };

    private abstract class InsertedCallback<T> {
        public abstract void onInserted(int position, int count);
    }

    private abstract class ChangedCallback<T> {
        public abstract void onChanged(int position, int count);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        mCallback = new SortedList.Callback<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                return o1.cmpField < o2.cmpField ? -1 : (o1.cmpField == o2.cmpField ? 0 : 1);
            }

            @Override
            public void onInserted(int position, int count) {
                mAdditions.add(new Pair(position, count));
                if (mInsertedCallback != null) {
                    mInsertedCallback.onInserted(position, count);
                }
            }

            @Override
            public void onRemoved(int position, int count) {
                mRemovals.add(new Pair(position, count));
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                mMoves.add(new Pair(fromPosition, toPosition));
            }

            @Override
            public void onChanged(int position, int count) {
                mUpdates.add(new Pair(position, count));
                if (mChangedCallback != null) {
                    mChangedCallback.onChanged(position, count);
                }
            }

            @Override
            public boolean areContentsTheSame(Item oldItem, Item newItem) {
                return oldItem.cmpField == newItem.cmpField && oldItem.data == newItem.data;
            }

            @Override
            public boolean areItemsTheSame(Item item1, Item item2) {
                return item1.id == item2.id;
            }
        };
        mInsertedCallback = null;
        mChangedCallback = null;
        mList = new SortedList<Item>(Item.class, mCallback);
    }

    @Test
    public void testEmpty() {
        assertEquals("empty", mList.size(), 0);
    }

    @Test
    public void testAdd() {
        Item item = new Item();
        assertEquals(insert(item), 0);
        assertEquals(size(), 1);
        assertTrue(mAdditions.contains(new Pair(0, 1)));
        Item item2 = new Item();
        item2.cmpField = item.cmpField + 1;
        assertEquals(insert(item2), 1);
        assertEquals(size(), 2);
        assertTrue(mAdditions.contains(new Pair(1, 1)));
        Item item3 = new Item();
        item3.cmpField = item.cmpField - 1;
        mAdditions.clear();
        assertEquals(insert(item3), 0);
        assertEquals(size(), 3);
        assertTrue(mAdditions.contains(new Pair(0, 1)));
    }

    @Test
    public void testAddDuplicate() {
        Item item = new Item();
        Item item2 = new Item(item.id, item.cmpField);
        item2.data = item.data;
        insert(item);
        assertEquals(0, insert(item2));
        assertEquals(1, size());
        assertEquals(1, mAdditions.size());
        assertEquals(0, mUpdates.size());
    }

    @Test
    public void testRemove() {
        Item item = new Item();
        assertFalse(remove(item));
        assertEquals(0, mRemovals.size());
        insert(item);
        assertTrue(remove(item));
        assertEquals(1, mRemovals.size());
        assertTrue(mRemovals.contains(new Pair(0, 1)));
        assertEquals(0, size());
        assertFalse(remove(item));
        assertEquals(1, mRemovals.size());
    }

    @Test
    public void testRemove2() {
        Item item = new Item();
        Item item2 = new Item(item.cmpField);
        insert(item);
        assertFalse(remove(item2));
        assertEquals(0, mRemovals.size());
    }

    @Test
    public void clearTest() {
        insert(new Item(1));
        insert(new Item(2));
        assertEquals(2, mList.size());
        mList.clear();
        assertEquals(0, mList.size());
        insert(new Item(3));
        assertEquals(1, mList.size());
    }

    @Test
    public void testBatch() {
        mList.beginBatchedUpdates();
        for (int i = 0; i < 5; i++) {
            mList.add(new Item(i));
        }
        assertEquals(0, mAdditions.size());
        mList.endBatchedUpdates();
        assertTrue(mAdditions.contains(new Pair(0, 5)));
    }

    @Test
    public void testRandom() throws Throwable {
        Random random = new Random(System.nanoTime());
        List<Item> copy = new ArrayList<Item>();
        StringBuilder log = new StringBuilder();
        try {
            for (int i = 0; i < 10000; i++) {
                switch (random.nextInt(3)) {
                    case 0://ADD
                        Item item = new Item();
                        copy.add(item);
                        insert(item);
                        log.append("add ").append(item).append("\n");
                        break;
                    case 1://REMOVE
                        if (copy.size() > 0) {
                            int index = random.nextInt(mList.size());
                            item = mList.get(index);
                            log.append("remove ").append(item).append("\n");
                            assertTrue(copy.remove(item));
                            assertTrue(mList.remove(item));
                        }
                        break;
                    case 2://UPDATE
                        if (copy.size() > 0) {
                            int index = random.nextInt(mList.size());
                            item = mList.get(index);
                            // TODO this cannot work
                            Item newItem = new Item(item.id, item.cmpField);
                            log.append("update ").append(item).append(" to ").append(newItem)
                                    .append("\n");
                            while (newItem.data == item.data) {
                                newItem.data = random.nextInt(1000);
                            }
                            int itemIndex = mList.add(newItem);
                            copy.remove(item);
                            copy.add(newItem);
                            assertSame(mList.get(itemIndex), newItem);
                            assertNotSame(mList.get(index), item);
                        }
                        break;
                    case 3:// UPDATE AT
                        if (copy.size() > 0) {
                            int index = random.nextInt(mList.size());
                            item = mList.get(index);
                            Item newItem = new Item(item.id, random.nextInt());
                            mList.updateItemAt(index, newItem);
                            copy.remove(item);
                            copy.add(newItem);
                        }
                }
                int lastCmp = Integer.MIN_VALUE;
                for (int index = 0; index < copy.size(); index++) {
                    assertFalse(mList.indexOf(copy.get(index)) == SortedList.INVALID_POSITION);
                    assertTrue(mList.get(index).cmpField >= lastCmp);
                    lastCmp = mList.get(index).cmpField;
                    assertTrue(copy.contains(mList.get(index)));
                }

                for (int index = 0; index < mList.size(); index++) {
                    assertNotNull(mList.mData[index]);
                }
                for (int index = mList.size(); index < mList.mData.length; index++) {
                    assertNull(mList.mData[index]);
                }

            }
        } catch (Throwable t) {
            Collections.sort(copy, sItemComparator);
            log.append("Items:\n");
            for (Item item : copy) {
                log.append(item).append("\n");
            }
            log.append("SortedList:\n");
            for (int i = 0; i < mList.size(); i++) {
                log.append(mList.get(i)).append("\n");
            }

            throw new Throwable(" \nlog:\n" + log.toString(), t);
        }
    }

    private static Item[] createItems(int idFrom, int idTo, int idStep) {
        final int count = (idTo - idFrom) / idStep + 1;
        Item[] items = new Item[count];
        int id = idFrom;
        for (int i = 0; i < count; i++) {
            Item item = new Item(id, id);
            item.data = id;
            items[i] = item;
            id += idStep;
        }
        return items;
    }

    private static Item[] shuffle(Item[] items) {
        Random random = new Random(System.nanoTime());
        final int count = items.length;
        for (int i = 0; i < count; i++) {
            int pos1 = random.nextInt(count);
            int pos2 = random.nextInt(count);
            if (pos1 != pos2) {
                Item temp = items[pos1];
                items[pos1] = items[pos2];
                items[pos2] = temp;
            }
        }
        return items;
    }

    private void assertIntegrity(int size, String context) {
        assertEquals(context + ": incorrect size", size, size());
        int rangeStart = 0;
        for (int i = 0; i < size(); i++) {
            Item item = mList.get(i);
            assertNotNull(context + ": get returned null @" + i, item);
            assertEquals(context + ": incorrect indexOf result @" + i, i, mList.indexOf(item));
            if (i == 0) {
                continue;
            }

            final int compare = mCallback.compare(mList.get(i - 1), item);
            assertTrue(context + ": incorrect sorting order @" + i, compare <= 0);

            if (compare == 0) {
                for (int j = rangeStart; j < i; j++) {
                    assertFalse(context + ": duplicates found @" + j + " and " + i,
                            mCallback.areItemsTheSame(mList.get(j), item));
                }
            } else {
                rangeStart = i;
            }
        }
    }

    private void assertSequentialOrder() {
        for (int i = 0; i < size(); i++) {
            assertEquals(i, mList.get(i).cmpField);
        }
    }

    @Test
    public void testAddAllMerge() throws Throwable {
        mList.addAll(new Item[0]);
        assertIntegrity(0, "addAll, empty list, empty input");
        assertEquals(0, mAdditions.size());

        // Add first 5 even numbers. Test adding to an empty list.
        mList.addAll(createItems(0, 8, 2));
        assertIntegrity(5, "addAll, empty list, non-empty input");
        assertEquals(1, mAdditions.size());
        assertTrue(mAdditions.contains(new Pair(0, 5)));

        mList.addAll(new Item[0]);
        assertIntegrity(5, "addAll, non-empty list, empty input");
        assertEquals(1, mAdditions.size());

        // Add 5 more even numbers, shuffled (test pre-sorting).
        mList.addAll(shuffle(createItems(10, 18, 2)));
        assertIntegrity(10, "addAll, shuffled input");
        assertEquals(2, mAdditions.size());
        assertTrue(mAdditions.contains(new Pair(5, 5)));

        // Add 5 more even numbers, reversed (test pre-sorting).
        mList.addAll(shuffle(createItems(28, 20, -2)));
        assertIntegrity(15, "addAll, reversed input");
        assertEquals(3, mAdditions.size());
        assertTrue(mAdditions.contains(new Pair(10, 5)));

        // Add first 10 odd numbers.
        // Test the merge when the new items run out first.
        mList.addAll(createItems(1, 19, 2));
        assertIntegrity(25, "addAll, merging in the middle");
        assertEquals(13, mAdditions.size());
        for (int i = 1; i <= 19; i += 2) {
            assertTrue(mAdditions.contains(new Pair(i, 1)));
        }

        // Add 10 more odd numbers.
        // Test the merge when the old items run out first.
        mList.addAll(createItems(21, 39, 2));
        assertIntegrity(35, "addAll, merging at the end");
        assertEquals(18, mAdditions.size());
        for (int i = 21; i <= 27; i += 2) {
            assertTrue(mAdditions.contains(new Pair(i, 1)));
        }
        assertTrue(mAdditions.contains(new Pair(29, 6)));

        // Add 5 more even numbers.
        mList.addAll(createItems(30, 38, 2));
        assertIntegrity(40, "addAll, merging more");
        assertEquals(23, mAdditions.size());
        for (int i = 30; i <= 38; i += 2) {
            assertTrue(mAdditions.contains(new Pair(i, 1)));
        }

        assertEquals(0, mMoves.size());
        assertEquals(0, mUpdates.size());
        assertEquals(0, mRemovals.size());

        assertSequentialOrder();
    }

    @Test
    public void testAddAllUpdates() throws Throwable {
        // Add first 5 even numbers.
        Item[] evenItems = createItems(0, 8, 2);
        for (Item item : evenItems) {
            item.data = 1;
        }
        mList.addAll(evenItems);
        assertEquals(5, size());
        assertEquals(1, mAdditions.size());
        assertTrue(mAdditions.contains(new Pair(0, 5)));
        assertEquals(0, mUpdates.size());

        Item[] sameEvenItems = createItems(0, 8, 2);
        for (Item item : sameEvenItems) {
            item.data = 1;
        }
        mList.addAll(sameEvenItems);
        assertEquals(1, mAdditions.size());
        assertEquals(0, mUpdates.size());

        Item[] newEvenItems = createItems(0, 8, 2);
        for (Item item : newEvenItems) {
            item.data = 2;
        }
        mList.addAll(newEvenItems);
        assertEquals(5, size());
        assertEquals(1, mAdditions.size());
        assertEquals(1, mUpdates.size());
        assertTrue(mUpdates.contains(new Pair(0, 5)));
        for (int i = 0; i < 5; i++) {
            assertEquals(2, mList.get(i).data);
        }

        // Add all numbers from 0 to 9
        Item[] sequentialItems = createItems(0, 9, 1);
        for (Item item : sequentialItems) {
            item.data = 3;
        }
        mList.addAll(sequentialItems);

        // Odd numbers should have been added.
        assertEquals(6, mAdditions.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(mAdditions.contains(new Pair(i * 2 + 1, 1)));
        }

        // All even items should have been updated.
        assertEquals(6, mUpdates.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(mUpdates.contains(new Pair(i * 2, 1)));
        }

        assertEquals(10, size());

        // All items should have the latest data value.
        for (int i = 0; i < 10; i++) {
            assertEquals(3, mList.get(i).data);
        }
        assertEquals(0, mMoves.size());
        assertEquals(0, mRemovals.size());
        assertSequentialOrder();
    }

    @Test
    public void testAddAllWithDuplicates() throws Throwable {
        final int maxCmpField = 5;
        final int idsPerCmpField = 10;
        final int maxUniqueId = maxCmpField * idsPerCmpField;
        final int maxGeneration = 5;

        Item[] items = new Item[maxUniqueId * maxGeneration];

        int index = 0;
        for (int generation = 0; generation < maxGeneration; generation++) {
            int uniqueId = 0;
            for (int cmpField = 0; cmpField < maxCmpField; cmpField++) {
                for (int id = 0; id < idsPerCmpField; id++) {
                    Item item = new Item(uniqueId++, cmpField);
                    item.data = generation;
                    items[index++] = item;
                }
            }
        }

        mList.addAll(items);

        assertIntegrity(maxUniqueId, "addAll with duplicates");

        // Check that the most recent items have made it to the list.
        for (int i = 0; i != size(); i++) {
            Item item = mList.get(i);
            assertEquals(maxGeneration - 1, item.data);
        }
    }

    @Test
    public void testAddAllFast() throws Throwable {
        mList.addAll(new Item[0], true);
        assertIntegrity(0, "addAll(T[],boolean), empty list, with empty input");
        assertEquals(0, mAdditions.size());

        mList.addAll(createItems(0, 9, 1), true);
        assertIntegrity(10, "addAll(T[],boolean), empty list, non-empty input");
        assertEquals(1, mAdditions.size());
        assertTrue(mAdditions.contains(new Pair(0, 10)));

        mList.addAll(new Item[0], true);
        assertEquals(1, mAdditions.size());
        assertIntegrity(10, "addAll(T[],boolean), non-empty list, empty input");

        mList.addAll(createItems(10, 19, 1), true);
        assertEquals(2, mAdditions.size());
        assertTrue(mAdditions.contains(new Pair(10, 10)));
        assertIntegrity(20, "addAll(T[],boolean), non-empty list, non-empty input");
    }

    @Test
    public void testAddAllCollection() throws Throwable {
        Collection<Item> itemList = new ArrayList<Item>();
        for (int i = 0; i < 5; i++) {
            itemList.add(new Item(i));
        }
        mList.addAll(itemList);

        assertEquals(1, mAdditions.size());
        assertTrue(mAdditions.contains(new Pair(0, itemList.size())));
        assertIntegrity(itemList.size(), "addAll on collection");
    }

    @Test
    public void testAddAllStableSort() {
        int id = 0;
        Item item = new Item(id++, 0);
        mList.add(item);

        // Create a few items with the same sort order.
        Item[] items = new Item[3];
        for (int i = 0; i < 3; i++) {
            items[i] = new Item(id++, item.cmpField);
            assertEquals(0, mCallback.compare(item, items[i]));
        }

        mList.addAll(items);
        assertEquals(1 + items.length, size());

        // Check that the order has been preserved.
        for (int i = 0; i < size(); i++) {
            assertEquals(i, mList.get(i).id);
        }
    }


    @Test
    public void testAddAllAccessFromCallbacks() {
        // Add first 5 even numbers.
        Item[] evenItems = createItems(0, 8, 2);
        for (Item item : evenItems) {
            item.data = 1;
        }

        mInsertedCallback = new InsertedCallback<Item>() {
            @Override
            public void onInserted(int position, int count) {
                assertEquals(0, position);
                assertEquals(5, count);
                for (int i = 0; i < count; i++) {
                    assertEquals(i * 2, mList.get(i).id);
                }
                assertIntegrity(5, "onInserted(" + position + ", " + count + ")");
            }
        };

        mList.addAll(evenItems);
        assertEquals(1, mAdditions.size());
        assertEquals(0, mUpdates.size());

        // Add all numbers from 0 to 9. This should trigger 5 change and 5 insert notifications.
        Item[] sequentialItems = createItems(0, 9, 1);
        for (Item item : sequentialItems) {
            item.data = 2;
        }

        mChangedCallback = new ChangedCallback<Item>() {
            int expectedSize = 5;

            @Override
            public void onChanged(int position, int count) {
                assertEquals(1, count);
                assertEquals(position, mList.get(position).id);
                assertIntegrity(++expectedSize, "onChanged(" + position + ")");
            }
        };

        mInsertedCallback = new InsertedCallback<Item>() {
            int expectedSize = 5;

            @Override
            public void onInserted(int position, int count) {
                assertEquals(1, count);
                assertEquals(position, mList.get(position).id);
                assertIntegrity(++expectedSize, "onInserted(" + position + ")");
            }
        };

        mList.addAll(sequentialItems);
        assertEquals(6, mAdditions.size());
        assertEquals(5, mUpdates.size());
    }

    @Test
    public void testModificationFromCallbackThrows() {
        final Item extraItem = new Item(0);

        Item[] items = createItems(1, 5, 2);
        for (Item item : items) {
            item.data = 1;
        }
        mList.addAll(items);

        mInsertedCallback = new InsertedCallback<Item>() {
            @Override
            public void onInserted(int position, int count) {
                try {
                    mList.add(new Item());
                    fail("add must throw from within a callback");
                } catch (IllegalStateException e) {
                }
                try {
                    mList.addAll(createItems(0, 0, 1));
                    fail("addAll must throw from within a callback");
                } catch (IllegalStateException e) {
                }
                try {
                    mList.addAll(createItems(0, 0, 1), true);
                    fail("addAll(T[],boolean) must throw from within a callback");
                } catch (IllegalStateException e) {
                }
                try {
                    mList.remove(extraItem);
                    fail("remove must throw from within a callback");
                } catch (IllegalStateException e) {
                }
                try {
                    mList.removeItemAt(0);
                    fail("removeItemAt must throw from within a callback");
                } catch (IllegalStateException e) {
                }
                try {
                    mList.updateItemAt(0, extraItem);
                    fail("updateItemAt must throw from within a callback");
                } catch (IllegalStateException e) {
                }
                try {
                    mList.recalculatePositionOfItemAt(0);
                    fail("recalculatePositionOfItemAt must throw from within a callback");
                } catch (IllegalStateException e) {
                }
                try {
                    mList.clear();
                    fail("recalculatePositionOfItemAt must throw from within a callback");
                } catch (IllegalStateException e) {
                }
            }
        };

        // Make sure that the last one notification is change, so that the above callback is
        // not called from endBatchUpdates when the nested alls are actually OK.
        items = createItems(1, 5, 1);
        for (Item item : items) {
            item.data = 2;
        }
        mList.addAll(items);
        assertIntegrity(5, "Modification from callback");
    }

    @Test
    public void testAddAllOutsideBatchedUpdates() {
        mList.add(new Item(1));
        assertEquals(1, mAdditions.size());
        mList.add(new Item(2));
        assertEquals(2, mAdditions.size());
        mList.addAll(new Item(3), new Item(4));
        assertEquals(3, mAdditions.size());
        mList.add(new Item(5));
        assertEquals(4, mAdditions.size());
        mList.add(new Item(6));
        assertEquals(5, mAdditions.size());
    }

    @Test
    public void testAddAllInsideBatchedUpdates() {
        mList.beginBatchedUpdates();

        mList.add(new Item(1));
        assertEquals(0, mAdditions.size());
        mList.add(new Item(2));
        assertEquals(0, mAdditions.size());
        mList.addAll(new Item(3), new Item(4));
        assertEquals(0, mAdditions.size());
        mList.add(new Item(5));
        assertEquals(0, mAdditions.size());
        mList.add(new Item(6));
        assertEquals(0, mAdditions.size());

        mList.endBatchedUpdates();

        assertEquals(1, mAdditions.size());
        assertTrue(mAdditions.contains(new Pair(0, 6)));
    }

    private int size() {
        return mList.size();
    }

    private int insert(Item item) {
        return mList.add(item);
    }

    private boolean remove(Item item) {
        return mList.remove(item);
    }

    static class Item {

        static int idCounter = 0;
        final int id;

        int cmpField;

        int data = (int) (Math.random() * 1000);//used for comparison

        public Item() {
            id = idCounter++;
            cmpField = (int) (Math.random() * 1000);
        }

        public Item(int cmpField) {
            id = idCounter++;
            this.cmpField = cmpField;
        }

        public Item(int id, int cmpField) {
            this.id = id;
            this.cmpField = cmpField;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Item item = (Item) o;

            if (cmpField != item.cmpField) {
                return false;
            }
            if (id != item.id) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + cmpField;
            return result;
        }

        @Override
        public String toString() {
            return "Item{" +
                    "id=" + id +
                    ", cmpField=" + cmpField +
                    ", data=" + data +
                    '}';
        }
    }

    private static final class Pair {

        final int first, second;

        public Pair(int first) {
            this.first = first;
            this.second = Integer.MIN_VALUE;
        }

        public Pair(int first, int second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Pair pair = (Pair) o;

            if (first != pair.first) {
                return false;
            }
            if (second != pair.second) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = first;
            result = 31 * result + second;
            return result;
        }
    }
}