package android.support.v17.leanback.app;

import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.CursorObjectAdapter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;

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
    public static final int ON_ITEM_RANGE_CHANGED = 2;
    public static final int ON_ITEM_RANGE_INSERTED = 4;
    public static final int ON_ITEM_RANGE_REMOVED = 8;
    public static final int ON_CHANGED = 16;

    private final ObjectAdapter mAdapter;
    private int mLastVisibleRowIndex;
    private Handler mHandler;
    private Update mPendingUpdate;
    private int mPendingUpdateCount;

    private static class Update {
        int eventType;
        int positionStart;
        int itemCount;

        public Update(int type, int positionStart, int itemCount) {
            this.eventType = type;
            this.positionStart = positionStart;
            this.itemCount = itemCount;
        }
    }

    private Runnable notificationTask = new Runnable() {
        @Override
        public void run() {
            if (mPendingUpdateCount == 0) {
                return;
            } else if (mPendingUpdateCount == 1 && mPendingUpdate != null) {
                doNotify(
                        mPendingUpdate.eventType,
                        mPendingUpdate.positionStart,
                        mPendingUpdate.itemCount);
            } else {
                notifyChanged();
            }
            mPendingUpdate = null;
            mPendingUpdateCount = 0;
        }
    };

    public ListRowDataAdapter(ObjectAdapter adapter) {
        super(adapter.getPresenterSelector());
        this.mAdapter = adapter;
        initialize();

        // If an user implements its own ObjectAdapter, notification corresponding to data
        // updates can be batched e.g. remove, add might be followed by notifyRemove, notifyAdd.
        // But underlying data would have changed during the notifyRemove call by the previous add
        // operation. To handle this case, we enqueue the updates in a queue and have a worker
        // service that queue. The common case will be to have a single pending update in the queue.
        // But in case the worker encounters multiple updates in the queue, it will send a
        // notifyChanged() call to RecyclerView forcing it to do a full refresh.
        if ((adapter instanceof ArrayObjectAdapter)
                || (adapter instanceof CursorObjectAdapter)
                || (adapter instanceof SparseArrayObjectAdapter)) {
            mAdapter.registerObserver(new SimpleDataObserver());
        } else {
            mHandler = new Handler();
            mAdapter.registerObserver(new QueueBasedDataObserver());
        }
    }

    private void initialize() {
        int i = mAdapter.size() - 1;
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

    private void doNotify(int eventType, int positionStart, int itemCount) {
        switch (eventType) {
            case ON_ITEM_RANGE_CHANGED:
                notifyItemRangeChanged(positionStart, itemCount);
                break;
            case ON_ITEM_RANGE_INSERTED:
                notifyItemRangeInserted(positionStart, itemCount);
                break;
            case ON_ITEM_RANGE_REMOVED:
                notifyItemRangeRemoved(positionStart, itemCount);
                break;
            case ON_CHANGED:
                notifyChanged();
            default:
                throw new IllegalArgumentException("Invalid event type " + eventType);
        }
    }

    private class SimpleDataObserver extends DataObserver {

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            if (positionStart <= mLastVisibleRowIndex) {
                onEventFired(ON_ITEM_RANGE_CHANGED, positionStart,
                        Math.min(itemCount, mLastVisibleRowIndex - positionStart + 1));
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (positionStart <= mLastVisibleRowIndex) {
                mLastVisibleRowIndex += itemCount;
                onEventFired(ON_ITEM_RANGE_INSERTED, positionStart, itemCount);
                return;
            }

            int lastVisibleRowIndex = mLastVisibleRowIndex;
            initialize();
            if (mLastVisibleRowIndex > lastVisibleRowIndex) {
                int totalItems = mLastVisibleRowIndex - lastVisibleRowIndex;
                onEventFired(ON_ITEM_RANGE_INSERTED, lastVisibleRowIndex + 1, totalItems);
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            if (positionStart + itemCount - 1 < mLastVisibleRowIndex) {
                mLastVisibleRowIndex -= itemCount;
                onEventFired(ON_ITEM_RANGE_REMOVED, positionStart, itemCount);
                return;
            }

            int lastVisibleRowIndex = mLastVisibleRowIndex;
            initialize();
            int totalItems = lastVisibleRowIndex - mLastVisibleRowIndex;
            if (totalItems > 0) {
                onEventFired(ON_ITEM_RANGE_REMOVED,
                        Math.min(lastVisibleRowIndex + 1, positionStart),
                        totalItems);
            }
        }

        @Override
        public void onChanged() {
            initialize();
            onEventFired(ON_CHANGED, -1, -1);
        }

        protected void onEventFired(int eventType, int positionStart, int itemCount) {
            doNotify(eventType, positionStart, itemCount);
        }
    }

    private class QueueBasedDataObserver extends SimpleDataObserver {

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            incrementAndPost();
            super.onItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            incrementAndPost();
            super.onItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            incrementAndPost();
            super.onItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onChanged() {
            incrementAndPost();
            super.onChanged();
        }

        @Override
        protected void onEventFired(
                final int eventType, final int positionStart, final int itemCount) {

            if (mPendingUpdateCount == 1) {
                mPendingUpdate = new Update(eventType, positionStart, itemCount);
            }
        }

        private void incrementAndPost() {
            mPendingUpdateCount++;
            if (mPendingUpdateCount == 1) {
                mHandler.post(notificationTask);
            }
        }
    }
}
