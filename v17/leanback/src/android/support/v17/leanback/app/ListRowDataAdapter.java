package android.support.v17.leanback.app;

import android.support.v17.leanback.widget.DividerRow;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.Row;

/**
 * Wrapper class for {@link ObjectAdapter} used by {@link BrowseFragment} to initialize
 * {@link RowsFragment}. We use invisible rows to represent
 * {@link android.support.v17.leanback.widget.DividerRow},
 * {@link android.support.v17.leanback.widget.SectionRow} and
 * {@link android.support.v17.leanback.widget.PageRow} in RowsFragment. In case we have an
 * invisible row at the end of a RowsFragment, it creates a jumping effect as the layout manager
 * thinks there are items even though they're invisible. This class takes care of filtering out
 * the invisible rows at the end. In case the data inside the adapter changes, it adjusts the
 * bounds to reflect the latest data.
 */
class ListRowDataAdapter extends ObjectAdapter {
    private final ObjectAdapter mAdapter;
    private int mLastVisibleRowIndex;

    public ListRowDataAdapter(ObjectAdapter adapter) {
        super(adapter.getPresenterSelector());
        this.mAdapter = adapter;
        initialize();
        mAdapter.registerObserver(new DataObserver() {

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                initialize();
                notifyItemRangeChanged(positionStart, itemCount);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                Row item = (Row) mAdapter.get(mLastVisibleRowIndex);
                boolean isLastRowNonVisible = (item instanceof DividerRow);
                initialize();

                boolean hasVisibleRow = hasVisibleRow(positionStart);
                if (positionStart >= mLastVisibleRowIndex
                        && isLastRowNonVisible
                        && hasVisibleRow) {
                    positionStart--;
                    itemCount++;
                }
                notifyItemRangeInserted(positionStart, itemCount);
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                Row item = (Row) mAdapter.get(mLastVisibleRowIndex);
                boolean isLastRowNonVisible = (item instanceof DividerRow);
                initialize();

                if (positionStart + itemCount >= mLastVisibleRowIndex && isLastRowNonVisible) {
                    itemCount--;
                }
                notifyItemRangeRemoved(positionStart, itemCount);
            }

            @Override
            public void onChanged() {
                initialize();
                notifyChanged();
            }
        });
    }

    private boolean hasVisibleRow(int startIndex) {
        for (int i = startIndex; i < Math.min(mLastVisibleRowIndex, mAdapter.size()); i++) {
            Row item = (Row) mAdapter.get(i);
            if (item instanceof ListRow) {
                return true;
            }
        }
        return false;
    }

    private void initialize() {
        int i = 0;
        while (i < mAdapter.size()) {
            Row item = (Row) mAdapter.get(i);
            if (item.isRenderedAsRowView()) {
                break;
            }
            i++;
        }

        i = mAdapter.size() - 1;
        while (i >= 0) {
            Row item = (Row) mAdapter.get(i);
            if (item.isRenderedAsRowView()) {
                mLastVisibleRowIndex = i;
                break;
            }
            i--;
        }
    }

    @Override
    public int size() {
        return mLastVisibleRowIndex + 1;
    }

    @Override
    public Object get(int index) {
        return mAdapter.get(index);
    }
}