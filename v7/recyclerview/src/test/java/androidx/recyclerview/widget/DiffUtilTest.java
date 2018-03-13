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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.support.test.filters.SmallTest;

import androidx.annotation.Nullable;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@RunWith(JUnit4.class)
@SmallTest
public class DiffUtilTest {
    private static Random sRand = new Random(System.nanoTime());
    private List<Item> mBefore = new ArrayList<>();
    private List<Item> mAfter = new ArrayList<>();
    private StringBuilder mLog = new StringBuilder();

    private DiffUtil.Callback mCallback = new DiffUtil.Callback() {
        @Override
        public int getOldListSize() {
            return mBefore.size();
        }

        @Override
        public int getNewListSize() {
            return mAfter.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemIndex, int newItemIndex) {
            return mBefore.get(oldItemIndex).id == mAfter.get(newItemIndex).id;
        }

        @Override
        public boolean areContentsTheSame(int oldItemIndex, int newItemIndex) {
            assertThat(mBefore.get(oldItemIndex).id,
                    CoreMatchers.equalTo(mAfter.get(newItemIndex).id));
            return mBefore.get(oldItemIndex).data.equals(mAfter.get(newItemIndex).data);
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemIndex, int newItemIndex) {
            assertThat(mBefore.get(oldItemIndex).id,
                    CoreMatchers.equalTo(mAfter.get(newItemIndex).id));
            assertThat(mBefore.get(oldItemIndex).data,
                    not(CoreMatchers.equalTo(mAfter.get(newItemIndex).data)));
            return mAfter.get(newItemIndex).payload;
        }
    };

    @Rule
    public TestWatcher mLogOnExceptionWatcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            System.err.println(mLog.toString());
        }
    };


    @Test
    public void testNoChange() {
        initWithSize(5);
        check();
    }

    @Test
    public void testAddItems() {
        initWithSize(2);
        add(1);
        check();
    }

    //@Test
    //@LargeTest
    // Used for development
    public void testRandom() {
        for (int x = 0; x < 100; x++) {
            for (int i = 0; i < 100; i++) {
                for (int j = 2; j < 40; j++) {
                    testRandom(i, j);
                }
            }
        }
    }

    @Test
    public void testGen2() {
        initWithSize(5);
        add(5);
        delete(3);
        delete(1);
        check();
    }

    @Test
    public void testGen3() {
        initWithSize(5);
        add(0);
        delete(1);
        delete(3);
        check();
    }

    @Test
    public void testGen4() {
        initWithSize(5);
        add(5);
        add(1);
        add(4);
        add(4);
        check();
    }

    @Test
    public void testGen5() {
        initWithSize(5);
        delete(0);
        delete(2);
        add(0);
        add(2);
        check();
    }

    @Test
    public void testGen6() {
        initWithSize(2);
        delete(0);
        delete(0);
        check();
    }

    @Test
    public void testGen7() {
        initWithSize(3);
        move(2, 0);
        delete(2);
        add(2);
        check();
    }

    @Test
    public void testGen8() {
        initWithSize(3);
        delete(1);
        add(0);
        move(2, 0);
        check();
    }

    @Test
    public void testGen9() {
        initWithSize(2);
        add(2);
        move(0, 2);
        check();
    }

    @Test
    public void testGen10() {
        initWithSize(3);
        move(0, 1);
        move(1, 2);
        add(0);
        check();
    }

    @Test
    public void testGen11() {
        initWithSize(4);
        move(2, 0);
        move(2, 3);
        check();
    }

    @Test
    public void testGen12() {
        initWithSize(4);
        move(3, 0);
        move(2, 1);
        check();
    }

    @Test
    public void testGen13() {
        initWithSize(4);
        move(3, 2);
        move(0, 3);
        check();
    }

    @Test
    public void testGen14() {
        initWithSize(4);
        move(3, 2);
        add(4);
        move(0, 4);
        check();
    }

    @Test
    public void testAdd1() {
        initWithSize(1);
        add(1);
        check();
    }

    @Test
    public void testMove1() {
        initWithSize(3);
        move(0, 2);
        check();
    }

    @Test
    public void tmp() {
        initWithSize(4);
        move(0, 2);
        check();
    }

    @Test
    public void testUpdate1() {
        initWithSize(3);
        update(2);
        check();
    }

    @Test
    public void testUpdate2() {
        initWithSize(2);
        add(1);
        update(1);
        update(2);
        check();
    }

    @Test
    public void testDisableMoveDetection() {
        initWithSize(5);
        move(0, 4);
        List<Item> applied = applyUpdates(mBefore, DiffUtil.calculateDiff(mCallback, false));
        assertThat(applied.size(), is(5));
        assertThat(applied.get(4).newItem, is(true));
        assertThat(applied.contains(mBefore.get(0)), is(false));
    }

    private void testRandom(int initialSize, int operationCount) {
        mLog.setLength(0);
        initWithSize(initialSize);
        for (int i = 0; i < operationCount; i++) {
            int op = sRand.nextInt(5);
            switch (op) {
                case 0:
                    add(sRand.nextInt(mAfter.size() + 1));
                    break;
                case 1:
                    if (!mAfter.isEmpty()) {
                        delete(sRand.nextInt(mAfter.size()));
                    }
                    break;
                case 2:
                    // move
                    if (mAfter.size() > 0) {
                        move(sRand.nextInt(mAfter.size()), sRand.nextInt(mAfter.size()));
                    }
                    break;
                case 3:
                    // update
                    if (mAfter.size() > 0) {
                        update(sRand.nextInt(mAfter.size()));
                    }
                    break;
                case 4:
                    // update with payload
                    if (mAfter.size() > 0) {
                        updateWithPayload(sRand.nextInt(mAfter.size()));
                    }
                    break;
            }
        }
        check();
    }

    private void check() {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(mCallback);
        log("before", mBefore);
        log("after", mAfter);
        log("snakes", result.getSnakes());

        List<Item> applied = applyUpdates(mBefore, result);
        assertEquals(applied, mAfter);
    }

    private void initWithSize(int size) {
        mBefore.clear();
        mAfter.clear();
        for (int i = 0; i < size; i++) {
            mBefore.add(new Item(false));
        }
        mAfter.addAll(mBefore);
        mLog.append("initWithSize(" + size + ");\n");
    }

    private void log(String title, List<?> items) {
        mLog.append(title).append(":").append(items.size()).append("\n");
        for (Object item : items) {
            mLog.append("  ").append(item).append("\n");
        }
    }

    private void assertEquals(List<Item> applied, List<Item> after) {
        log("applied", applied);

        String report = mLog.toString();
        assertThat(report, applied.size(), is(after.size()));
        for (int i = 0; i < after.size(); i++) {
            Item item = applied.get(i);
            if (after.get(i).newItem) {
                assertThat(report, item.newItem, is(true));
            } else if (after.get(i).changed) {
                assertThat(report, item.newItem, is(false));
                assertThat(report, item.changed, is(true));
                assertThat(report, item.id, is(after.get(i).id));
                assertThat(report, item.payload, is(after.get(i).payload));
            } else {
                assertThat(report, item, equalTo(after.get(i)));
            }
        }
    }

    private List<Item> applyUpdates(List<Item> before, DiffUtil.DiffResult result) {
        final List<Item> target = new ArrayList<>();
        target.addAll(before);
        result.dispatchUpdatesTo(new ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                for (int i = 0; i < count; i++) {
                    target.add(i + position, new Item(true));
                }
            }

            @Override
            public void onRemoved(int position, int count) {
                for (int i = 0; i < count; i++) {
                    target.remove(position);
                }
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                Item item = target.remove(fromPosition);
                target.add(toPosition, item);
            }

            @Override
            public void onChanged(int position, int count, Object payload) {
                for (int i = 0; i < count; i++) {
                    int positionInList = position + i;
                    Item existing = target.get(positionInList);
                    // make sure we don't update same item twice in callbacks
                    assertThat(existing.changed, is(false));
                    assertThat(existing.newItem, is(false));
                    assertThat(existing.payload, is(nullValue()));
                    Item replica = new Item(existing);
                    replica.payload = (String) payload;
                    replica.changed = true;
                    target.remove(positionInList);
                    target.add(positionInList, replica);
                }
            }
        });
        return target;
    }

    private void add(int index) {
        mAfter.add(index, new Item(true));
        mLog.append("add(").append(index).append(");\n");
    }

    private void delete(int index) {
        mAfter.remove(index);
        mLog.append("delete(").append(index).append(");\n");
    }

    private void update(int index) {
        Item existing = mAfter.get(index);
        if (existing.newItem) {
            return;//new item cannot be changed
        }
        Item replica = new Item(existing);
        replica.changed = true;
        // clean the payload since this might be after an updateWithPayload call
        replica.payload = null;
        replica.data = UUID.randomUUID().toString();
        mAfter.remove(index);
        mAfter.add(index, replica);
        mLog.append("update(").append(index).append(");\n");
    }

    private void updateWithPayload(int index) {
        Item existing = mAfter.get(index);
        if (existing.newItem) {
            return;//new item cannot be changed
        }
        Item replica = new Item(existing);
        replica.changed = true;
        replica.data = UUID.randomUUID().toString();
        replica.payload = UUID.randomUUID().toString();
        mAfter.remove(index);
        mAfter.add(index, replica);
        mLog.append("update(").append(index).append(");\n");
    }

    private void move(int from, int to) {
        Item removed = mAfter.remove(from);
        mAfter.add(to, removed);
        mLog.append("move(").append(from).append(",").append(to).append(");\n");
    }

    static class Item {
        static long idCounter = 0;
        final long id;
        final boolean newItem;
        boolean changed = false;
        String payload;

        String data = UUID.randomUUID().toString();

        public Item(boolean newItem) {
            id = idCounter++;
            this.newItem = newItem;
        }

        public Item(Item other) {
            id = other.id;
            newItem = other.newItem;
            changed = other.changed;
            payload = other.payload;
            data = other.data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Item item = (Item) o;

            if (id != item.id) return false;
            if (newItem != item.newItem) return false;
            if (changed != item.changed) return false;
            if (payload != null ? !payload.equals(item.payload) : item.payload != null) {
                return false;
            }
            return data.equals(item.data);

        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (newItem ? 1 : 0);
            result = 31 * result + (changed ? 1 : 0);
            result = 31 * result + (payload != null ? payload.hashCode() : 0);
            result = 31 * result + data.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Item{" +
                    "id=" + id +
                    ", newItem=" + newItem +
                    ", changed=" + changed +
                    ", payload='" + payload + '\'' +
                    ", data='" + data + '\'' +
                    '}';
        }
    }
}
