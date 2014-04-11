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
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Internal presenter for header items.
 */
class HeaderPresenter extends Presenter {
    interface OnHeaderClickListener {
        void onHeaderClicked();
    }

    private OnHeaderClickListener mOnHeaderClickListener;

    public void setOnHeaderClickListener(OnHeaderClickListener listener) {
        mOnHeaderClickListener = listener;
    }

    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        TextView view = (TextView) inflater.inflate(R.layout.lb_row_header, parent, false);
        return new Presenter.ViewHolder(view);
    }

    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Row row = (Row) item;
        String headerText;
        // TODO: handle null headers more elegantly. Right now the
        // BrowseFragment maps header positions to row positions, so failing to
        // render a null item will mess up the selection.
        if (row.getHeaderItem() == null) {
            headerText = "(null)";
        } else {
            headerText = row.getHeaderItem().getName();
        }

        ((TextView) viewHolder.view).setText(headerText);
        viewHolder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnHeaderClickListener != null) {
                    mOnHeaderClickListener.onHeaderClicked();
                }
            }
        });
    }

    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {}
}
