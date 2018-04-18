package androidx.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Parcelable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

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


    class OuterAdapter extends RecyclerView.Adapter<OuterAdapter.ViewHolder> {
        OuterAdapter() {
            for (int i = 0; i < getItemCount(); i++) {
                mAdapters.add(createBoxAdapter());
                mSavedStates.add(null);
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final RecyclerView mRecyclerView;
            ViewHolder(RecyclerView itemView) {
                super(itemView);
                mRecyclerView = itemView;
            }
        }

        ArrayList<RecyclerView.Adapter> mAdapters = new ArrayList<>();
        ArrayList<Parcelable> mSavedStates = new ArrayList<>();
        RecyclerView.RecycledViewPool mSharedPool = new RecyclerView.RecycledViewPool();

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            HorizontalGridView gridView = new HorizontalGridView(getContext());
            gridView.setNumRows(1);
            gridView.setRowHeight(100);
            gridView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            gridView.setLayoutParams(new GridLayoutManager.LayoutParams(350, 100));
            gridView.setRecycledViewPool(mSharedPool);
            return new ViewHolder(gridView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mRecyclerView.swapAdapter(mAdapters.get(position), true);

            Parcelable savedState = mSavedStates.get(position);
            if (savedState != null) {
                holder.mRecyclerView.getLayoutManager().onRestoreInstanceState(savedState);
                mSavedStates.set(position, null);
            }
        }

        @Override
        public int getItemCount() {
            return 100;
        }
    };

    public void validateInitialPrefetch(BaseGridView gridView,
            int... positionData) {
        RecyclerView.LayoutManager.LayoutPrefetchRegistry registry
                = mock(RecyclerView.LayoutManager.LayoutPrefetchRegistry.class);
        gridView.getLayoutManager().collectInitialPrefetchPositions(
                gridView.getAdapter().getItemCount(), registry);

        verify(registry, times(positionData.length)).addPosition(anyInt(), anyInt());
        for (int position : positionData) {
            verify(registry).addPosition(position, 0);
        }
    }

    @Test
    public void prefetchInitialFocusTest() {
        VerticalGridView view = new VerticalGridView(getContext());
        view.setNumColumns(1);
        view.setColumnWidth(350);
        view.setAdapter(createBoxAdapter());

        // check default
        assertEquals(4, view.getInitialPrefetchItemCount());

        // check setter behavior
        view.setInitialPrefetchItemCount(0);
        assertEquals(0, view.getInitialPrefetchItemCount());

        // check positions fetched, relative to focus
        view.scrollToPosition(2);
        view.setInitialPrefetchItemCount(5);
        validateInitialPrefetch(view, 0, 1, 2, 3, 4);

        view.setInitialPrefetchItemCount(3);
        validateInitialPrefetch(view, 1, 2, 3);

        view.scrollToPosition(0);
        view.setInitialPrefetchItemCount(4);
        validateInitialPrefetch(view, 0, 1, 2, 3);

        view.scrollToPosition(98);
        view.setInitialPrefetchItemCount(5);
        validateInitialPrefetch(view, 95, 96, 97, 98, 99);

        view.setInitialPrefetchItemCount(7);
        validateInitialPrefetch(view, 93, 94, 95, 96, 97, 98, 99);

        // implementation detail - rounds up
        view.scrollToPosition(50);
        view.setInitialPrefetchItemCount(4);
        validateInitialPrefetch(view, 49, 50, 51, 52);
    }

    @Test
    public void prefetchNested() {
        VerticalGridView gridView = new VerticalGridView(getContext());
        gridView.setNumColumns(1);
        gridView.setColumnWidth(350);
        OuterAdapter outerAdapter = new OuterAdapter();
        gridView.setAdapter(outerAdapter);
        gridView.setItemViewCacheSize(1); // enough to cache child 0 while offscreen

        layout(gridView, 350, 150);

        // validate 2 top level children in viewport
        assertEquals(2, gridView.getChildCount());
        for (int y = 0; y < 2; y++) {
            View child = gridView.getLayoutManager().findViewByPosition(y);
            assertEquals(y * 100, child.getTop());
            // each has 4 children

            HorizontalGridView inner = (HorizontalGridView) child;
            for (int x = 0; x < 4; x++) {
                assertEquals(x * 100, inner.getLayoutManager().findViewByPosition(x).getLeft());
            }
        }

        // center child 0 at position 10
        HorizontalGridView offsetChild =
                (HorizontalGridView) gridView.getLayoutManager().findViewByPosition(0);
        offsetChild.scrollToPosition(10);

        // scroll to position 2, and layout
        gridView.scrollToPosition(2);
        layout(gridView, 350, 150);

        // now, offset by 175, centered around row 2. Validate 3 top level children in viewport
        assertEquals(3, gridView.getChildCount());
        for (int y = 1; y < 4; y++) {
            assertEquals(y * 100 - 175, gridView.getLayoutManager().findViewByPosition(y).getTop());
        }

        validatePrefetch(gridView, 0, -5, new Integer[] {0, 75});
        validatePrefetch(gridView, 0, 5, new Integer[] {4, 75});

        // assume offsetChild still bound, in cache, just not attached...
        validateInitialPrefetch(offsetChild, 9, 10, 11, 12);
    }
}
