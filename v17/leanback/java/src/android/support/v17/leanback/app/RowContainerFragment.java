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

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.ItemBridgeAdapter;
import android.support.v17.leanback.widget.ListView;
import android.support.v17.leanback.widget.OnChildSelectedListener;
import android.support.v17.leanback.widget.OnItemSelectedListener;
import android.support.v17.leanback.widget.OnItemClickedListener;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

/**
 * An ordered set of rows of leanback widgets.
 */
class RowContainerFragment extends BaseRowFragment {
    private static final String TAG = "RowContainerFragment";
    private static final boolean DEBUG = false;

    private BackgroundParams mBackgroundParams;
    private ItemBridgeAdapter.ViewHolder mSelectedViewHolder;
    private boolean mExpand = true;

    private OnItemSelectedListener mOnItemSelectedListener;
    private OnItemClickedListener mOnItemClickedListener;

    /**
     * Set background parameters.
     */
    public void setBackgroundParams(BackgroundParams params) {
        mBackgroundParams = params;
    }

    /**
     * Returns the background parameters.
     */
    public BackgroundParams getBackgroundParams() {
        return mBackgroundParams;
    }

    /**
     * Sets an item clicked listener.
     */
    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mOnItemClickedListener = listener;
        notifyDataSetChanged();
    }

    private void notifyDataSetChanged() {
        ItemBridgeAdapter adapter = getBridgeAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Returns the item clicked listener.
     */
    public OnItemClickedListener getOnItemClickedListener() {
        return mOnItemClickedListener;
    }

    /**
     * Set the visibility of titles/hovercard of browse rows.
     */
    public void setExpand(boolean expand) {
        final int count = getListView().getChildCount();
        if (DEBUG) Log.v(TAG, "setExpand " + expand + " count " + count);
        mExpand = expand;
        for (int i = 0; i < count; i++) {
            View view = getListView().getChildAt(i);
            ItemBridgeAdapter.ViewHolder vh = (ItemBridgeAdapter.ViewHolder) getListView().getChildViewHolder(view);
            setRowViewExpanded(vh, mExpand);
        }
    }

    /**
     * Sets an item selection listener.
     */
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
        ListView listView = getListView();
        if (listView != null) {
            final int count = listView.getChildCount();
            for (int i = 0; i < count; i++) {
                View view = listView.getChildAt(i);
                ItemBridgeAdapter.ViewHolder vh = (ItemBridgeAdapter.ViewHolder)
                        listView.getChildViewHolder(view);
                setOnItemSelectedListener(vh, mOnItemSelectedListener);
            }
        }
    }

    @Override
    protected void onRowSelected(ViewGroup parent, View view, int position, long id) {
        ItemBridgeAdapter.ViewHolder vh = (view == null) ? null :
            (ItemBridgeAdapter.ViewHolder) getListView().getChildViewHolder(view);

        if (mSelectedViewHolder != vh) {
            if (DEBUG) Log.v(TAG, "new row selected position " + position + " view " + view);

            if (mSelectedViewHolder != null) {
                setRowViewSelected(mSelectedViewHolder, false);
            }
            mSelectedViewHolder = vh;
            if (mSelectedViewHolder != null) {
                setRowViewSelected(mSelectedViewHolder, true);
            }
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.lb_container_fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        getListView().setItemAlignmentViewId(R.id.row_list);
    }

    private static void setRowViewExpanded(ItemBridgeAdapter.ViewHolder vh, boolean expanded) {
        ((RowPresenter) vh.getPresenter()).setRowViewExpanded(
                (RowPresenter.ViewHolder) vh.getViewHolder(), expanded);
    }

    private static void setRowViewSelected(ItemBridgeAdapter.ViewHolder vh, boolean selected) {
        ((RowPresenter) vh.getPresenter()).setRowViewSelected(
                (RowPresenter.ViewHolder) vh.getViewHolder(), selected);
    }

    private static void setOnItemSelectedListener(ItemBridgeAdapter.ViewHolder vh,
            OnItemSelectedListener listener) {
        ((RowPresenter) vh.getPresenter()).setOnItemSelectedListener(listener);
    }

    private static void setOnItemClickedListener(ItemBridgeAdapter.ViewHolder vh,
            OnItemClickedListener listener) {
        ((RowPresenter) vh.getPresenter()).setOnItemClickedListener(listener);
    }

    private final ItemBridgeAdapter.AdapterListener mBridgeAdapterListener = new ItemBridgeAdapter.AdapterListener() {
        @Override
        public void onCreate(ItemBridgeAdapter.ViewHolder vh) {
        }

        @Override
        public void onAttachedToWindow(ItemBridgeAdapter.ViewHolder vh) {
            if (DEBUG) Log.v(TAG, "onAttachToWindow");
            setRowViewExpanded(vh, mExpand);
            setOnItemSelectedListener(vh, mOnItemSelectedListener);
            setOnItemClickedListener(vh, mOnItemClickedListener);
        }
        @Override
        public void onBind(ItemBridgeAdapter.ViewHolder vh) {
        }
        @Override
        public void onUnbind(ItemBridgeAdapter.ViewHolder vh) {
        }
        @Override
        public void onDetachedFromWindow(ItemBridgeAdapter.ViewHolder vh) {
        }
    };

    @Override
    protected void updateAdapter() {
        super.updateAdapter();
        mSelectedViewHolder = null;
        ItemBridgeAdapter adapter = getBridgeAdapter();
        if (adapter != null) {
            adapter.setAdapterListener(mBridgeAdapterListener);
        }
    }
}
