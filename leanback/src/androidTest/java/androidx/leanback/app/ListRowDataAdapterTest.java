/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.leanback.app;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.support.test.filters.MediumTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.DividerRow;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.SectionRow;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for {@link ListRowDataAdapter} class.
 */
@RunWith(AndroidJUnit4.class)
public class ListRowDataAdapterTest {
    @Mock
    private PresenterSelector presenterSelector;
    @Mock
    private ObjectAdapter.DataObserver dataObserver;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @SmallTest
    @Test
    public void itemRangeChangedTest() {
        int itemCount = 4;
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
        adapter.add(new SectionRow("section 1"));
        for (int i = 0; i < itemCount; i++) {
            HeaderItem headerItem = new HeaderItem(i, "header "+i);
            adapter.add(new ListRow(headerItem, createListRowAdapter()));
        }

        ListRowDataAdapter listRowDataAdapter = new ListRowDataAdapter(adapter);
        listRowDataAdapter.registerObserver(dataObserver);
        SectionRow sectionRow = new SectionRow("section 11");
        adapter.replace(0, sectionRow);

        verify(dataObserver, times(1)).onItemRangeChanged(0, 1);
        assertEquals(5, listRowDataAdapter.size());
    }

    @SmallTest
    @Test
    public void adapterSize_nonVisibleRowPresent() {
        int itemCount = 4;
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
        adapter.add(new SectionRow("section 1"));
        for (int i = 0; i < itemCount; i++) {
            HeaderItem headerItem = new HeaderItem(i, "header "+i);
            adapter.add(new ListRow(headerItem, createListRowAdapter()));
        }

        ListRowDataAdapter listRowDataAdapter = new ListRowDataAdapter(adapter);
        assertEquals(5, listRowDataAdapter.size());

        List<DividerRow> invisibleRows = new ArrayList<>();
        invisibleRows.add(new DividerRow());
        invisibleRows.add(new DividerRow());
        adapter.addAll(5, invisibleRows);
        verify(dataObserver, times(0)).onItemRangeInserted(anyInt(), anyInt());
        assertEquals(5, listRowDataAdapter.size());
    }

    @SmallTest
    @Test
    public void adapterSize_visibleRowInserted() {
        int itemCount = 4;
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
        adapter.add(new SectionRow("section 1"));
        for (int i = 0; i < itemCount; i++) {
            HeaderItem headerItem = new HeaderItem(i, "header "+i);
            adapter.add(new ListRow(headerItem, createListRowAdapter()));
        }

        ListRowDataAdapter listRowDataAdapter = new ListRowDataAdapter(adapter);
        assertEquals(5, listRowDataAdapter.size());

        listRowDataAdapter.registerObserver(dataObserver);
        List<ListRow> visibleRows = new ArrayList<>();
        visibleRows.add(new ListRow(new HeaderItem(0, "Header 51"), createListRowAdapter()));
        visibleRows.add(new ListRow(new HeaderItem(0, "Header 52"), createListRowAdapter()));
        visibleRows.add(new ListRow(new HeaderItem(0, "Header 53"), createListRowAdapter()));
        adapter.addAll(2, visibleRows);
        verify(dataObserver, times(1)).onItemRangeInserted(2, 3);
        assertEquals(8, listRowDataAdapter.size());
    }

    @SmallTest
    @Test
    public void adapterSize_nonVisibleRowInserted() {
        int itemCount = 4;
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
        adapter.add(new SectionRow("section 1"));
        for (int i = 0; i < itemCount; i++) {
            HeaderItem headerItem = new HeaderItem(i, "header "+i);
            adapter.add(new ListRow(headerItem, createListRowAdapter()));
        }

        ListRowDataAdapter listRowDataAdapter = new ListRowDataAdapter(adapter);
        assertEquals(5, listRowDataAdapter.size());

        List<DividerRow> invisibleRows = new ArrayList<>();
        invisibleRows.add(new DividerRow());
        invisibleRows.add(new DividerRow());

        listRowDataAdapter.registerObserver(dataObserver);
        adapter.addAll(adapter.size(), invisibleRows);
        verify(dataObserver, times(0)).onItemRangeInserted(anyInt(), anyInt());
        assertEquals(5, listRowDataAdapter.size());

        adapter.add(new DividerRow());
        verify(dataObserver, times(0)).onItemRangeInserted(anyInt(), anyInt());
        assertEquals(5, listRowDataAdapter.size());

        adapter.add(new ListRow(new HeaderItem(0, "Header 5"), createListRowAdapter()));
        verify(dataObserver, times(1)).onItemRangeInserted(5, 4);
        assertEquals(9, listRowDataAdapter.size());
    }

    @SmallTest
    @Test
    public void adapterSize_visibleRowRemoved() {
        int itemCount = 4;
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
        adapter.add(new SectionRow("section 1"));
        for (int i = 0; i < itemCount; i++) {
            HeaderItem headerItem = new HeaderItem(i, "header "+i);
            adapter.add(new ListRow(headerItem, createListRowAdapter()));
        }

        ListRowDataAdapter listRowDataAdapter = new ListRowDataAdapter(adapter);
        assertEquals(5, listRowDataAdapter.size());
        adapter.add(new DividerRow());
        assertEquals(5, listRowDataAdapter.size());

        listRowDataAdapter.registerObserver(dataObserver);
        adapter.removeItems(2, 2);
        verify(dataObserver, times(1)).onItemRangeRemoved(2, 2);
        assertEquals(3, listRowDataAdapter.size());
    }

    @MediumTest
    @Test
    public void adapterSize_nonVisibleRowRemoved() {
        int itemCount = 4;
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
        adapter.add(new SectionRow("section 1"));
        for (int i = 0; i < itemCount; i++) {
            HeaderItem headerItem = new HeaderItem(i, "header "+i);
            adapter.add(new ListRow(headerItem, createListRowAdapter()));
        }

        ListRowDataAdapter listRowDataAdapter = new ListRowDataAdapter(adapter);
        assertEquals(5, listRowDataAdapter.size());
        adapter.add(new DividerRow());
        assertEquals(5, listRowDataAdapter.size());

        listRowDataAdapter.registerObserver(dataObserver);
        adapter.removeItems(4, 1);
        verify(dataObserver, times(1)).onItemRangeRemoved(4, 1);
        assertEquals(4, listRowDataAdapter.size());

        adapter.removeItems(4, 1);
        verify(dataObserver, times(0)).onItemRangeInserted(anyInt(), anyInt());
        assertEquals(4, listRowDataAdapter.size());
    }

    @SmallTest
    @Test
    public void adapterSize_rowsRemoveAll() {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
        adapter.add(new SectionRow("section 1"));
        for (int i = 0; i < 4; i++) {
            HeaderItem headerItem = new HeaderItem(i, "header "+i);
            adapter.add(new ListRow(headerItem, createListRowAdapter()));
        }

        ListRowDataAdapter listRowDataAdapter = new ListRowDataAdapter(adapter);
        assertEquals(5, listRowDataAdapter.size());

        adapter.clear();
        assertEquals(0, listRowDataAdapter.size());

        HeaderItem headerItem = new HeaderItem(10, "header "+10);
        adapter.add(new ListRow(headerItem, createListRowAdapter()));
        assertEquals(1, listRowDataAdapter.size());
    }

    @SmallTest
    @Test
    public void changeRemove_revealInvisibleItems() {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
        for (int i = 0; i < 4; i++) {
            HeaderItem headerItem = new HeaderItem(i, "header "+i);
            adapter.add(new ListRow(headerItem, createListRowAdapter()));
        }
        adapter.add(new SectionRow("section"));
        for (int i = 4; i < 8; i++) {
            HeaderItem headerItem = new HeaderItem(i, "header "+i);
            adapter.add(new ListRow(headerItem, createListRowAdapter()));
        }

        ListRowDataAdapter listRowDataAdapter = new ListRowDataAdapter(adapter);
        assertEquals(9, listRowDataAdapter.size());

        listRowDataAdapter.registerObserver(dataObserver);
        adapter.removeItems(5, 4);
        verify(dataObserver, times(1)).onItemRangeRemoved(4, 5);
        assertEquals(4, listRowDataAdapter.size());
    }

    @SmallTest
    @Test
    public void adapterSize_rowsRemoved() {
        int itemCount = 4;
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
        adapter.add(new SectionRow("section 1"));
        for (int i = 0; i < itemCount; i++) {
            HeaderItem headerItem = new HeaderItem(i, "header "+i);
            adapter.add(new ListRow(headerItem, createListRowAdapter()));
        }

        ListRowDataAdapter listRowDataAdapter = new ListRowDataAdapter(adapter);
        assertEquals(5, listRowDataAdapter.size());

        adapter.add(new DividerRow());
        assertEquals(5, listRowDataAdapter.size());

        listRowDataAdapter.registerObserver(dataObserver);
        adapter.removeItems(3, 3);
        verify(dataObserver, times(1)).onItemRangeRemoved(3, 2);
        assertEquals(3, listRowDataAdapter.size());
    }

    @SmallTest
    @Test
    public void customObjectAdapterTest() {
        int itemCount = 4;
        ArrayObjectAdapter adapter = new CustomAdapter(presenterSelector);
        adapter.add(new SectionRow("section 1"));
        for (int i = 0; i < itemCount; i++) {
            HeaderItem headerItem = new HeaderItem(i, "header "+i);
            adapter.add(new ListRow(headerItem, createListRowAdapter()));
        }

        ListRowDataAdapter listRowDataAdapter = new ListRowDataAdapter(adapter);
        assertEquals(5, listRowDataAdapter.size());

        adapter.add(new DividerRow());
        assertEquals(5, listRowDataAdapter.size());

        listRowDataAdapter.registerObserver(dataObserver);
        adapter.removeItems(3, 3);
        verify(dataObserver, times(1)).onChanged();
        assertEquals(3, listRowDataAdapter.size());

        Mockito.reset(dataObserver);
        adapter.add(new DividerRow());
        verify(dataObserver, times(1)).onChanged();
        assertEquals(3, listRowDataAdapter.size());
    }

    private ArrayObjectAdapter createListRowAdapter() {
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(presenterSelector);
        listRowAdapter.add(new Integer(1));
        listRowAdapter.add(new Integer(2));
        return listRowAdapter;
    }

    private class CustomAdapter extends ArrayObjectAdapter {

        public CustomAdapter(PresenterSelector selector) {
            super(selector);
        }

        @Override
        public boolean isImmediateNotifySupported() {
            return false;
        }
    }
}
