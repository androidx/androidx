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

import android.os.Bundle;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.FocusHighlightHelper;
import android.support.v17.leanback.widget.ItemBridgeAdapter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.OnItemSelectedListener;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowHeaderPresenter;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * An internal fragment containing a list of row headers.
 */
public class HeadersFragment extends BaseRowFragment {

    interface OnHeaderClickedListener {
        void onHeaderClicked();
    }

    private OnItemSelectedListener mOnItemSelectedListener;
    private OnHeaderClickedListener mOnHeaderClickedListener;
    private boolean mShow = true;

    private static final Presenter sHeaderPresenter = new RowHeaderPresenter();

    public HeadersFragment() {
        setPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object item) {
                return sHeaderPresenter;
            }
        });
    }

    public void setOnHeaderClickedListener(OnHeaderClickedListener listener) {
        mOnHeaderClickedListener = listener;
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    @Override
    protected void onRowSelected(ViewGroup parent, View view, int position, long id) {
        if (mOnItemSelectedListener != null) {
            if (position >= 0) {
                Row row = (Row) getAdapter().get(position);
                mOnItemSelectedListener.onItemSelected(null, row);
            }
        }
    }

    private final ItemBridgeAdapter.AdapterListener mAdapterListener =
            new ItemBridgeAdapter.AdapterListener() {
        @Override
        public void onCreate(ItemBridgeAdapter.ViewHolder viewHolder) {
            View headerView = viewHolder.getViewHolder().view;
            headerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnHeaderClickedListener != null) {
                        mOnHeaderClickedListener.onHeaderClicked();
                    }
                }
            });
            headerView.setFocusable(true);
            headerView.setFocusableInTouchMode(true);
        }

        @Override
        public void onAttachedToWindow(ItemBridgeAdapter.ViewHolder viewHolder) {
            View headerView = viewHolder.getViewHolder().view;
            headerView.setVisibility(mShow ? View.VISIBLE : View.INVISIBLE);
        }
    };

    @Override
    protected int getLayoutResourceId() {
        return R.layout.lb_headers_fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getBridgeAdapter() != null && getVerticalGridView() != null) {
            FocusHighlightHelper.setupHeaderItemFocusHighlight(getVerticalGridView());
        }
    }

    void getHeaderViews(List<View> headers, List<Integer> positions) {
        final VerticalGridView listView = getVerticalGridView();
        if (listView == null) {
            return;
        }
        final int count = listView.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = listView.getChildAt(i);
            headers.add(child);
            positions.add(listView.getChildViewHolder(child).getPosition());
        }
    }

    void setHeadersVisiblity(boolean show) {
        mShow = show;
        final VerticalGridView listView = getVerticalGridView();
        if (listView == null) {
            return;
        }
        final int count = listView.getChildCount();
        final int visibility = mShow ? View.VISIBLE : View.INVISIBLE;

        // we should set visibility of selected view first so that it can
        // regain the focus from parent (which is FOCUS_AFTER_DESCENDANT)
        final int selectedPosition = listView.getSelectedPosition();
        if (selectedPosition >= 0) {
            RecyclerView.ViewHolder vh = listView.findViewHolderForPosition(selectedPosition);
            if (vh != null) {
                vh.itemView.setVisibility(visibility);
            }
        }
        for (int i = 0; i < count; i++) {
            View child = listView.getChildAt(i);
            if (listView.getChildPosition(child) != selectedPosition) {
                child.setVisibility(visibility);
            }
        }
    }

    @Override
    protected void updateAdapter() {
        super.updateAdapter();
        ItemBridgeAdapter adapter = getBridgeAdapter();
        if (adapter != null) {
            adapter.setAdapterListener(mAdapterListener);
        }
        if (adapter != null && getVerticalGridView() != null) {
            FocusHighlightHelper.setupHeaderItemFocusHighlight(getVerticalGridView());
        }
    }
}
