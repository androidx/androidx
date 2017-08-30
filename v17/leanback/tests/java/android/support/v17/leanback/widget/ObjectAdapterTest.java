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
    }

}
