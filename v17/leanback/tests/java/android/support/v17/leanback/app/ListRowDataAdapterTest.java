package android.support.v17.leanback.app;

import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.DividerRow;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.SectionRow;
import android.test.suitebuilder.annotation.SmallTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for {@link ListRowDataAdapter} class.
 */
@RunWith(JUnit4.class)
@SmallTest
public class ListRowDataAdapterTest {
    public static final int ITEM_COUNT = 4;

    @Mock
    private PresenterSelector presenterSelector;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void adapterSize_nonVisibleRowPresent() {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
        adapter.add(new SectionRow("section 1"));
        for (int i = 0; i < ITEM_COUNT; i++) {
            HeaderItem headerItem = new HeaderItem(i, "header "+i);
            adapter.add(new ListRow(headerItem, createListRowAdapter()));
        }

        ListRowDataAdapter listRowDataAdapter = new ListRowDataAdapter(adapter);
        assertEquals(5, listRowDataAdapter.size());

        adapter.add(new DividerRow());
        assertEquals(5, listRowDataAdapter.size());

        adapter.add(new DividerRow());
        assertEquals(5, listRowDataAdapter.size());
    }

    @Test
    public void adapterSize_nonVisibleRowInserted() {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
        adapter.add(new SectionRow("section 1"));
        for (int i = 0; i < ITEM_COUNT; i++) {
            HeaderItem headerItem = new HeaderItem(i, "header "+i);
            adapter.add(new ListRow(headerItem, createListRowAdapter()));
        }

        ListRowDataAdapter listRowDataAdapter = new ListRowDataAdapter(adapter);
        assertEquals(5, listRowDataAdapter.size());

        adapter.add(new DividerRow());
        assertEquals(5, listRowDataAdapter.size());

        adapter.add(new ListRow(new HeaderItem(5, "header 5"), createListRowAdapter()));
        assertEquals(7, listRowDataAdapter.size());

        adapter.add(new DividerRow());
        assertEquals(7, listRowDataAdapter.size());

        adapter.add(5, new ListRow(new HeaderItem(5, "header 5"), createListRowAdapter()));
        assertEquals(8, listRowDataAdapter.size());
    }

    @Test
    public void adapterSize_nonVisibleRowRemoved() {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
        adapter.add(new SectionRow("section 1"));
        for (int i = 0; i < ITEM_COUNT; i++) {
            HeaderItem headerItem = new HeaderItem(i, "header "+i);
            adapter.add(new ListRow(headerItem, createListRowAdapter()));
        }

        ListRowDataAdapter listRowDataAdapter = new ListRowDataAdapter(adapter);
        assertEquals(5, listRowDataAdapter.size());

        adapter.add(new DividerRow());
        assertEquals(5, listRowDataAdapter.size());

        adapter.removeItems(4, 1);
        assertEquals(4, listRowDataAdapter.size());

        adapter.removeItems(5, 1);
        assertEquals(4, listRowDataAdapter.size());
    }

    private ArrayObjectAdapter createListRowAdapter() {
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(presenterSelector);
        listRowAdapter.add(new Integer(1));
        listRowAdapter.add(new Integer(2));
        return listRowAdapter;
    }
}
