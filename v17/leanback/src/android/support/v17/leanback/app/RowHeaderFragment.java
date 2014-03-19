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

import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.FocusHighlightHelper;
import android.support.v17.leanback.widget.ItemBridgeAdapter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.OnItemSelectedListener;
import android.support.v17.leanback.widget.Row;
import android.view.View;
import android.view.ViewGroup;

/**
 * An internal fragment containing a list of row headers.
 */
class RowHeaderFragment extends BaseRowFragment {
    private HeaderPresenter mHeaderPresenter;
    private OnItemSelectedListener mOnItemSelectedListener;

    public RowHeaderFragment() {
        mHeaderPresenter = new HeaderPresenter();
        setPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object item) {
                return mHeaderPresenter;
            }
        });
    }

    public void setOnHeaderClickListener(HeaderPresenter.OnHeaderClickListener listener) {
        mHeaderPresenter.setOnHeaderClickListener(listener);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    @Override
    protected void onRowSelected(ViewGroup parent, View view, int position, long id) {
        if (mOnItemSelectedListener != null) {
            Row row = (Row) getAdapter().get(position);
            mOnItemSelectedListener.onItemSelected(null, row);
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.lb_browse_header_fragment;
    }

    @Override
    protected void updateAdapter() {
        super.updateAdapter();
        ItemBridgeAdapter adapter = getBridgeAdapter();
        if (adapter != null) {
            FocusHighlightHelper.setupHeaderItemFocusHighlight(adapter);
        }
    }
}
