package android.support.v17.leanback.app;

import android.support.test.runner.AndroidJUnitRunner;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.DividerRow;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.SectionRow;
import android.test.suitebuilder.annotation.SmallTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.support.test.runner.AndroidJUnit4;

/**
 * Unit test for {@link ListRowDataAdapter} class.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ListRowDataAdapterTest {
    @Mock
    private PresenterSelector presenterSelector;
    @Mock
    private ObjectAdapter.DataObserver dataObserver;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

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
