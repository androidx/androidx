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

import android.support.v7.util.SortedList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class SortedListTest extends TestCase {

    SortedList<Item> mList;
    List<Pair> mAdditions = new ArrayList<Pair>();
    List<Pair> mRemovals = new ArrayList<Pair>();
    List<Pair> mMoves = new ArrayList<Pair>();
    List<Pair> mUpdates = new ArrayList<Pair>();
    private SortedList.Callback<Item> mCallback;

    private Comparator<? super Item> sItemComparator = new Comparator<Item>() {
        @Override
        public int compare(Item o1, Item o2) {
            return mCallback.compare(o1, o2);
        }
    };

    @Override
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
        mList = new SortedList<Item>(Item.class, mCallback);
    }

    public void testEmpty() {
        assertEquals("empty", mList.size(), 0);
    }

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

    public void testRemove2() {
        Item item = new Item();
        Item item2 = new Item(item.cmpField);
        insert(item);
        assertFalse(remove(item2));
        assertEquals(0, mRemovals.size());
    }

    public void testBatch() {
        mList.beginBatchedUpdates();
        for (int i = 0; i < 5; i ++) {
            mList.add(new Item(i));
        }
        assertEquals(0, mAdditions.size());
        mList.endBatchedUpdates();
        assertTrue(mAdditions.contains(new Pair(0, 5)));
    }

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
                        log.append("add " + item).append("\n");
                        break;
                    case 1://REMOVE
                        if (copy.size() > 0) {
                            int index = random.nextInt(mList.size());
                            item = mList.get(index);
                            log.append("remove " + item).append("\n");
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
                            log.append("update " + item + " to " + newItem).append("\n");
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
                for (int index = 0; index < copy.size(); index ++) {
                    assertFalse(mList.indexOf(copy.get(index)) == SortedList.INVALID_POSITION);
                    assertTrue(mList.get(index).cmpField >= lastCmp);
                    lastCmp = mList.get(index).cmpField;
                    assertTrue(copy.contains(mList.get(index)));
                }

                for (int index = 0; index < mList.size(); index ++) {
                    assertNotNull(mList.mData[index]);
                }
                for (int index = mList.size(); index < mList.mData.length; index ++) {
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
            for (int i = 0; i < mList.size(); i ++) {
                log.append(mList.get(i)).append("\n");
            }

            throw new Throwable(" \nlog:\n" + log.toString(), t);
        }
    }

    private int size() {
        return mList.size();
    }

    private int insert(Item item) {
        return mList.add(item);
    }

    private boolean remove(Item item ) {
        return mList.remove(item);
    }

    static class Item {
        static int idCounter = 0;
        final int id;

        int cmpField;

        int data = (int) (Math.random() * 1000);//used for comparison

        public Item() {
            id = idCounter ++;;
            cmpField = (int) (Math.random() * 1000);
        }

        public Item(int cmpField) {
            id = idCounter ++;;
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