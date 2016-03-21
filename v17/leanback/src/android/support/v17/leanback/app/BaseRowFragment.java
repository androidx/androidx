/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.app;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v17.leanback.widget.ItemBridgeAdapter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnChildViewHolderSelectedListener;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * An internal base class for a fragment containing a list of rows.
 */
abstract class BaseRowFragment extends Fragment {
    private static final String CURRENT_SELECTED_POSITION = "currentSelectedPosition";
    private ObjectAdapter mAdapter;
    private VerticalGridView mVerticalGridView;
    private PresenterSelector mPresenterSelector;
    private ItemBridgeAdapter mBridgeAdapter;
    private int mSelectedPosition = -1;
    private boolean mPendingTransitionPrepare;
    private LateSelectionObserver mLateSelectionObserver = new LateSelectionObserver();

    abstract int getLayoutResourceId();

    private final OnChildViewHolderSelectedListener mRowSelectedListener =
            new OnChildViewHolderSelectedListener() {
                @Override
                public void onChildViewHolderSelected(RecyclerView parent,
                        RecyclerView.ViewHolder view, int position, int subposition) {
                    mSelectedPosition = position;
                    onRowSelected(parent, view, position, subposition);
                }
            };

    void onRowSelected(RecyclerView parent, RecyclerView.ViewHolder view,
            int position, int subposition) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutResourceId(), container, false);
        mVerticalGridView = findGridViewFromRoot(view);
        if (mPendingTransitionPrepare) {
            mPendingTransitionPrepare = false;
            onTransitionPrepare();
        }
        return view;
    }

    VerticalGridView findGridViewFromRoot(View view) {
        return (VerticalGridView) view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mSelectedPosition = savedInstanceState.getInt(CURRENT_SELECTED_POSITION, -1);
        }
        if (mBridgeAdapter != null) {
            setAdapterAndSelection();
        }
        mVerticalGridView.setOnChildViewHolderSelectedListener(mRowSelectedListener);
    }

    /**
     * This class waits for the adapter to be updated before setting the selected
     * row.
     */
    private class LateSelectionObserver extends RecyclerView.AdapterDataObserver {
        boolean mIsLateSelection = false;

        public void onChanged() {
            performLateSelection();
        }

        public void onItemRangeInserted(int positionStart, int itemCount) {
            performLateSelection();
        }

        void startLateSelection() {
            mIsLateSelection = true;
            mBridgeAdapter.registerAdapterDataObserver(this);
        }

        void performLateSelection() {
            clear();
            if (mVerticalGridView != null) {
                mVerticalGridView.setSelectedPosition(mSelectedPosition);
            }
        }

        void clear() {
            if (mIsLateSelection) {
                mIsLateSelection = false;
                mBridgeAdapter.unregisterAdapterDataObserver(this);
            }
        }
    }

    void setAdapterAndSelection() {
        mVerticalGridView.setAdapter(mBridgeAdapter);
        // We don't set the selected position unless we've data in the adapter.
        boolean lateSelection = mBridgeAdapter.getItemCount() == 0 && mSelectedPosition >= 0;
        if (lateSelection) {
            mLateSelectionObserver.startLateSelection();
        } else if (mSelectedPosition >= 0) {
            mVerticalGridView.setSelectedPosition(mSelectedPosition);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mLateSelectionObserver.clear();
        mVerticalGridView = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_SELECTED_POSITION, mSelectedPosition);
    }

    /**
     * Set the presenter selector used to create and bind views.
     */
    public final void setPresenterSelector(PresenterSelector presenterSelector) {
        mPresenterSelector = presenterSelector;
        updateAdapter();
    }

    /**
     * Get the presenter selector used to create and bind views.
     */
    public final PresenterSelector getPresenterSelector() {
        return mPresenterSelector;
    }

    /**
     * Sets the adapter for the fragment.
     */
    public final void setAdapter(ObjectAdapter rowsAdapter) {
        mAdapter = rowsAdapter;
        updateAdapter();
    }

    /**
     * Returns the list of rows.
     */
    public final ObjectAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Returns the bridge adapter.
     */
    final ItemBridgeAdapter getBridgeAdapter() {
        return mBridgeAdapter;
    }

    /**
     * Sets the selected row position with smooth animation.
     */
    public void setSelectedPosition(int position) {
        setSelectedPosition(position, true);
    }

    /**
     * Gets position of currently selected row.
     * @return Position of currently selected row.
     */
    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    /**
     * Sets the selected row position.
     */
    public void setSelectedPosition(int position, boolean smooth) {
        if (mSelectedPosition == position) {
            return;
        }
        mSelectedPosition = position;
        if(mVerticalGridView != null && mVerticalGridView.getAdapter() != null) {
            if (mLateSelectionObserver.mIsLateSelection) {
                return;
            }
            if (smooth) {
                mVerticalGridView.setSelectedPositionSmooth(position);
            } else {
                mVerticalGridView.setSelectedPosition(position);
            }
        }
    }

    final VerticalGridView getVerticalGridView() {
        return mVerticalGridView;
    }

    void updateAdapter() {
        if (mBridgeAdapter != null) {
            // detach observer from ObjectAdapter
            mLateSelectionObserver.clear();
            mBridgeAdapter.clear();
            mBridgeAdapter = null;
        }

        if (mAdapter != null) {
            // If presenter selector is null, adapter ps will be used
            mBridgeAdapter = new ItemBridgeAdapter(mAdapter, mPresenterSelector);
        }
        if (mVerticalGridView != null) {
            setAdapterAndSelection();
        }
    }

    Object getItem(Row row, int position) {
        if (row instanceof ListRow) {
            return ((ListRow) row).getAdapter().get(position);
        } else {
            return null;
        }
    }

    public boolean onTransitionPrepare() {
        if (mVerticalGridView != null) {
            mVerticalGridView.setAnimateChildLayout(false);
            mVerticalGridView.setScrollEnabled(false);
            return true;
        }
        mPendingTransitionPrepare = true;
        return false;
    }

    public void onTransitionStart() {
        if (mVerticalGridView != null) {
            mVerticalGridView.setPruneChild(false);
            mVerticalGridView.setLayoutFrozen(true);
            mVerticalGridView.setFocusSearchDisabled(true);
        }
    }

    public void onTransitionEnd() {
        // be careful that fragment might be destroyed before header transition ends.
        if (mVerticalGridView != null) {
            mVerticalGridView.setLayoutFrozen(false);
            mVerticalGridView.setAnimateChildLayout(true);
            mVerticalGridView.setPruneChild(true);
            mVerticalGridView.setFocusSearchDisabled(false);
            mVerticalGridView.setScrollEnabled(true);
        }
    }

    public void setAlignment(int windowAlignOffsetTop) {
        if (mVerticalGridView != null) {
            // align the top edge of item
            mVerticalGridView.setItemAlignmentOffset(0);
            mVerticalGridView.setItemAlignmentOffsetPercent(
                    VerticalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED);

            // align to a fixed position from top
            mVerticalGridView.setWindowAlignmentOffset(windowAlignOffsetTop);
            mVerticalGridView.setWindowAlignmentOffsetPercent(
                    VerticalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
            mVerticalGridView.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
        }
    }
}
