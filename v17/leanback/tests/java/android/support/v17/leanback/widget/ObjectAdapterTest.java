/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.v17.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;

import android.support.test.filters.SmallTest;
import android.support.v7.widget.RecyclerView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;

@SmallTest
@RunWith(JUnit4.class)
public class ObjectAdapterTest {

    protected ItemBridgeAdapter mBridgeAdapter;
    protected ObjectAdapter mAdapter;

    static void assertAdapterContent(ObjectAdapter adapter, Object[] data) {
        assertEquals(adapter.size(), data.length);
        for (int i = 0; i < adapter.size(); i++) {
            assertEquals(adapter.get(i), data[i]);
        }
    }

    static class AdapterItem {
        private int mId;
        private String mName;

        AdapterItem(int id, String name) {
            this.mId = id;
            this.mName = name;
        }

        public int getId() {
            return mId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AdapterItem test = (AdapterItem) o;

            if (mId != test.mId) return false;
            return mName != null ? mName.equals(test.mName) : test.mName == null;
        }

        @Override
        public int hashCode() {
            int result = mId;
            result = 31 * result + (mName != null ? mName.hashCode() : 0);
            return result;
        }
    }

    @Test
    public void arrayObjectAdapter() {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter();
        mAdapter = adapter;
        mBridgeAdapter = new ItemBridgeAdapter(mAdapter);
        ArrayList items = new ArrayList();
        items.add("a");
        items.add("b");
        items.add("c");
        adapter.addAll(0, items);

        RecyclerView.AdapterDataObserver observer = Mockito.mock(
                RecyclerView.AdapterDataObserver.class);
        mBridgeAdapter.registerAdapterDataObserver(observer);

        // size
        assertEquals(adapter.size(), 3);

        // get
        assertEquals(adapter.get(0), "a");
        assertEquals(adapter.get(1), "b");
        assertEquals(adapter.get(2), "c");

        // indexOf
        assertEquals(adapter.indexOf("a"), 0);
        assertEquals(adapter.indexOf("b"), 1);
        assertEquals(adapter.indexOf("c"), 2);

        // insert
        adapter.add(1, "a1");
        Mockito.verify(observer).onItemRangeInserted(1, 1);
        assertAdapterContent(adapter, new Object[]{"a", "a1", "b", "c"});
        Mockito.reset(observer);

        // insert multiple
        ArrayList newItems1 = new ArrayList();
        newItems1.add("a2");
        newItems1.add("a3");
        adapter.addAll(1, newItems1);
        Mockito.verify(observer).onItemRangeInserted(1, 2);
        assertAdapterContent(adapter, new Object[]{"a", "a2", "a3", "a1", "b", "c"});
        Mockito.reset(observer);

        // update
        adapter.notifyArrayItemRangeChanged(2, 3);
        Mockito.verify(observer).onItemRangeChanged(2, 3, null);
        assertAdapterContent(adapter, new Object[]{"a", "a2", "a3", "a1", "b", "c"});
        Mockito.reset(observer);

        // remove
        adapter.removeItems(1, 4);
        Mockito.verify(observer).onItemRangeRemoved(1, 4);
        assertAdapterContent(adapter, new Object[]{"a", "c"});
        Mockito.reset(observer);

        // move
        adapter.move(0, 1);
        Mockito.verify(observer).onItemRangeMoved(0, 1, 1);
        assertAdapterContent(adapter, new Object[]{"c", "a"});
        Mockito.reset(observer);

        // replace
        adapter.replace(0, "a");
        Mockito.verify(observer).onItemRangeChanged(0, 1, null);
        assertAdapterContent(adapter, new Object[]{"a", "a"});
        Mockito.reset(observer);
        adapter.replace(1, "b");
        Mockito.verify(observer).onItemRangeChanged(1, 1, null);
        assertAdapterContent(adapter, new Object[]{"a", "b"});
        Mockito.reset(observer);

        // remove multiple
        items.clear();
        items.add("a");
        items.add("b");
        adapter.addAll(0, items);
        adapter.removeItems(0, 2);
        Mockito.verify(observer).onItemRangeRemoved(0, 2);
        assertAdapterContent(adapter, new Object[]{"a", "b"});
        Mockito.reset(observer);

        // clear
        adapter.clear();
        Mockito.verify(observer).onItemRangeRemoved(0, 2);
        assertAdapterContent(adapter, new Object[]{});
        Mockito.reset(observer);

        // isImmediateNotifySupported
        assertTrue(adapter.isImmediateNotifySupported());

        // setItems (test basic functionality with specialized comparator and verify adapter's item
        // lists)
        items.clear();
        items.add("a");
        items.add("b");
        items.add("c");

        DiffCallback callback = new DiffCallback<String>() {

            // Always treat two items are the same.
            @Override
            public boolean areItemsTheSame(String oldItem, String newItem) {
                return true;
            }

            // Always treat two items have the same content.
            @Override
            public boolean areContentsTheSame(String oldItem, String newItem) {
                return true;
            }
        };

        adapter.setItems(items, callback);
        Mockito.verify(observer).onItemRangeInserted(0, 3);
        assertAdapterContent(adapter, new Object[]{"a", "b", "c"});
        Mockito.reset(observer);

        // setItems (test basic functionality with specialized comparator and verify adapter's item
        // lists)
        items.clear();
        items.add("a");
        items.add("b");
        items.add("c");

        callback = new DiffCallback<String>() {

            // Always treat two items are the different.
            @Override
            public boolean areItemsTheSame(String oldItem, String newItem) {
                return false;
            }

            // Always treat two items have the different content.
            @Override
            public boolean areContentsTheSame(String oldItem, String newItem) {
                return false;
            }
        };

        adapter.setItems(items, callback);
        Mockito.verify(observer).onItemRangeRemoved(0, 3);
        Mockito.verify(observer).onItemRangeInserted(0, 3);

        // No change or move event should be fired under current callback.
        Mockito.verify(observer, never()).onItemRangeChanged(anyInt(), anyInt(), any());
        Mockito.verify(observer, never()).onItemRangeMoved(anyInt(), anyInt(), anyInt());
        assertAdapterContent(adapter, new Object[]{"a", "b", "c"});
        Mockito.reset(observer);

        // setItems (Using specialized java class to simulate actual scenario)
        callback = new DiffCallback<AdapterItem>() {

            // Using item's mId as the standard to judge if two items is the same
            @Override
            public boolean areItemsTheSame(AdapterItem oldItem, AdapterItem newItem) {
                return oldItem.getId() == newItem.getId();
            }

            // Using equals method to judge if two items have the same content.
            @Override
            public boolean areContentsTheSame(AdapterItem oldItem, AdapterItem newItem) {
                return oldItem.equals(newItem);
            }
        };


        // Trigger notifyItemMoved event
        adapter.clear();
        items.clear();
        items.add(new AdapterItem(1, "a"));
        items.add(new AdapterItem(2, "b"));
        items.add(new AdapterItem(3, "c"));
        adapter.setItems(items, callback);
        Mockito.reset(observer);
        items.clear();
        items.add(new AdapterItem(1, "a"));
        items.add(new AdapterItem(2, "c"));
        items.add(new AdapterItem(3, "b"));
        adapter.setItems(items, callback);
        Mockito.verify(observer).onItemRangeChanged(1, 2, null);
        Mockito.reset(observer);

        // Trigger notifyItemRangeChanged event
        adapter.clear();
        items.clear();
        items.add(new AdapterItem(1, "a"));
        items.add(new AdapterItem(2, "b"));
        items.add(new AdapterItem(3, "c"));
        adapter.clear();
        adapter.setItems(items, callback);
        Mockito.reset(observer);
        items.clear();
        items.add(new AdapterItem(2, "b"));
        items.add(new AdapterItem(1, "a"));
        items.add(new AdapterItem(3, "c"));
        adapter.setItems(items, callback);
        Mockito.verify(observer).onItemRangeMoved(1, 0, 1);
        Mockito.reset(observer);

        // Trigger notifyItemRangeRemoved event
        adapter.clear();
        items.clear();
        items.add(new AdapterItem(1, "a"));
        items.add(new AdapterItem(2, "b"));
        items.add(new AdapterItem(3, "c"));
        adapter.clear();
        adapter.setItems(items, callback);
        Mockito.reset(observer);
        items.clear();
        items.add(new AdapterItem(2, "b"));
        items.add(new AdapterItem(3, "c"));
        adapter.setItems(items, callback);
        Mockito.verify(observer).onItemRangeRemoved(0, 1);
        Mockito.reset(observer);

        // Trigger notifyItemRangeInserted event
        adapter.clear();
        items.clear();
        items.add(new AdapterItem(1, "a"));
        items.add(new AdapterItem(2, "b"));
        items.add(new AdapterItem(3, "c"));
        adapter.clear();
        adapter.setItems(items, callback);
        Mockito.reset(observer);
        items.clear();
        items.add(new AdapterItem(1, "a"));
        items.add(new AdapterItem(2, "b"));
        items.add(new AdapterItem(3, "c"));
        items.add(new AdapterItem(4, "d"));
        adapter.setItems(items, callback);
        Mockito.verify(observer).onItemRangeInserted(3, 1);
        Mockito.reset(observer);

        // Trigger notifyItemRangeInserted event and notifyItemRangeRemoved event simultaneously
        adapter.clear();
        items.clear();
        items.add(new AdapterItem(1, "a"));
        items.add(new AdapterItem(2, "b"));
        items.add(new AdapterItem(3, "c"));
        adapter.clear();
        adapter.setItems(items, callback);
        Mockito.reset(observer);
        items.clear();
        items.add(new AdapterItem(2, "a"));
        items.add(new AdapterItem(2, "b"));
        items.add(new AdapterItem(3, "c"));
        adapter.setItems(items, callback);
        Mockito.verify(observer).onItemRangeRemoved(0, 1);
        Mockito.verify(observer).onItemRangeInserted(0, 1);
        Mockito.reset(observer);


        // Trigger notifyItemRangeMoved and notifyItemRangeChanged event simultaneously
        adapter.clear();
        items.clear();
        items.add(new AdapterItem(1, "a"));
        items.add(new AdapterItem(2, "b"));
        items.add(new AdapterItem(3, "c"));
        adapter.clear();
        adapter.setItems(items, callback);
        Mockito.reset(observer);
        items.clear();
        items.add(new AdapterItem(1, "aa"));
        items.add(new AdapterItem(3, "c"));
        items.add(new AdapterItem(2, "b"));
        adapter.setItems(items, callback);
        Mockito.verify(observer).onItemRangeChanged(0, 1, null);
        Mockito.verify(observer).onItemRangeMoved(2, 1, 1);
        Mockito.reset(observer);

        // Trigger multiple items insertion event
        adapter.clear();
        items.clear();
        items.add(new AdapterItem(0, "a"));
        items.add(new AdapterItem(1, "b"));
        adapter.clear();
        adapter.setItems(items, callback);
        Mockito.reset(observer);
        items.clear();
        items.add(new AdapterItem(0, "a"));
        items.add(new AdapterItem(1, "b"));
        items.add(new AdapterItem(2, "c"));
        items.add(new AdapterItem(3, "d"));
        adapter.setItems(items, callback);
        Mockito.verify(observer).onItemRangeInserted(2, 2);
        Mockito.reset(observer);
    }

}
