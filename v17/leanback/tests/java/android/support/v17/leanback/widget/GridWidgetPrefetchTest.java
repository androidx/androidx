package android.support.v17.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class GridWidgetPrefetchTest {

    private Context getContext() {
        return InstrumentationRegistry.getContext();
    }

    private void layout(View view, int width, int height) {
        view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, width, height);
    }

    public void validatePrefetch(BaseGridView gridView, int scrollX, int scrollY,
            Integer[]... positionData) {
        // duplicates logic in support.v7.widget.CacheUtils#verifyPositionsPrefetched
        RecyclerView.State state = mock(RecyclerView.State.class);
        when(state.getItemCount()).thenReturn(gridView.getAdapter().getItemCount());
        RecyclerView.LayoutManager.LayoutPrefetchRegistry registry
                = mock(RecyclerView.LayoutManager.LayoutPrefetchRegistry.class);

        gridView.getLayoutManager().collectAdjacentPrefetchPositions(scrollX, scrollY,
                state, registry);

        verify(registry, times(positionData.length)).addPosition(anyInt(), anyInt());
        for (Integer[] aPositionData : positionData) {
            verify(registry).addPosition(aPositionData[0], aPositionData[1]);
        }
    }

    private RecyclerView.Adapter createBoxAdapter() {
        return new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = new View(getContext());
                view.setMinimumWidth(100);
                view.setMinimumHeight(100);
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                // noop
            }

            @Override
            public int getItemCount() {
                return 100;
            }
        };
    }

    @Test
    public void prefetch() {
        HorizontalGridView gridView = new HorizontalGridView(getContext());
        gridView.setNumRows(1);
        gridView.setRowHeight(100);
        gridView.setAdapter(createBoxAdapter());

        layout(gridView, 150, 100);

        // validate 2 children in viewport
        assertEquals(2, gridView.getChildCount());
        assertEquals(0, gridView.getLayoutManager().findViewByPosition(0).getLeft());
        assertEquals(100, gridView.getLayoutManager().findViewByPosition(1).getLeft());

        validatePrefetch(gridView, -50, 0); // no view to left
        validatePrefetch(gridView, 50, 0, new Integer[] {2, 50}); // next view 50 pixels to right

        // scroll to position 5, and layout
        gridView.scrollToPosition(5);
        layout(gridView, 150, 100);

        /* Visual representation, each number column represents 25 pixels:
         *              |           |
         * ... 3 3 4 4 4|4 5 5 5 5 6|6 6 6 7 7 ...
         *              |           |
         */

        // validate the 3 children in the viewport, and their positions
        assertEquals(3, gridView.getChildCount());
        assertNotNull(gridView.getLayoutManager().findViewByPosition(4));
        assertNotNull(gridView.getLayoutManager().findViewByPosition(5));
        assertNotNull(gridView.getLayoutManager().findViewByPosition(6));
        assertEquals(-75, gridView.getLayoutManager().findViewByPosition(4).getLeft());
        assertEquals(25, gridView.getLayoutManager().findViewByPosition(5).getLeft());
        assertEquals(125, gridView.getLayoutManager().findViewByPosition(6).getLeft());

        // next views are 75 pixels to right and left:
        validatePrefetch(gridView, -50, 0, new Integer[] {3, 75});
        validatePrefetch(gridView, 50, 0, new Integer[] {7, 75});

        // no views returned for vertical prefetch:
        validatePrefetch(gridView, 0, 10);
        validatePrefetch(gridView, 0, -10);

        // test minor offset
        gridView.scrollBy(5, 0);
        validatePrefetch(gridView, -50, 0, new Integer[] {3, 80});
        validatePrefetch(gridView, 50, 0, new Integer[] {7, 70});
    }

    @Test
    public void prefetchRtl() {
        HorizontalGridView gridView = new HorizontalGridView(getContext());
        gridView.setNumRows(1);
        gridView.setRowHeight(100);
        gridView.setAdapter(createBoxAdapter());
        gridView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        layout(gridView, 150, 100);

        // validate 2 children in viewport
        assertEquals(2, gridView.getChildCount());
        assertEquals(50, gridView.getLayoutManager().findViewByPosition(0).getLeft());
        assertEquals(-50, gridView.getLayoutManager().findViewByPosition(1).getLeft());

        validatePrefetch(gridView, 50, 0); // no view to right
        validatePrefetch(gridView, -10, 0, new Integer[] {2, 50}); // next view 50 pixels to right


        // scroll to position 5, and layout
        gridView.scrollToPosition(5);
        layout(gridView, 150, 100);


        /* Visual representation, each number column represents 25 pixels:
         *              |           |
         * ... 7 7 6 6 6|6 5 5 5 5 4|4 4 4 3 3 ...
         *              |           |
         */
        // validate 3 children in the viewport
        assertEquals(3, gridView.getChildCount());
        assertNotNull(gridView.getLayoutManager().findViewByPosition(6));
        assertNotNull(gridView.getLayoutManager().findViewByPosition(5));
        assertNotNull(gridView.getLayoutManager().findViewByPosition(4));
        assertEquals(-75, gridView.getLayoutManager().findViewByPosition(6).getLeft());
        assertEquals(25, gridView.getLayoutManager().findViewByPosition(5).getLeft());
        assertEquals(125, gridView.getLayoutManager().findViewByPosition(4).getLeft());

        // next views are 75 pixels to right and left:
        validatePrefetch(gridView, 50, 0, new Integer[] {3, 75});
        validatePrefetch(gridView, -50, 0, new Integer[] {7, 75});

        // no views returned for vertical prefetch:
        validatePrefetch(gridView, 0, 10);
        validatePrefetch(gridView, 0, -10);

        // test minor offset
        gridView.scrollBy(-5, 0);
        validatePrefetch(gridView, 50, 0, new Integer[] {3, 80});
        validatePrefetch(gridView, -50, 0, new Integer[] {7, 70});
    }
}
