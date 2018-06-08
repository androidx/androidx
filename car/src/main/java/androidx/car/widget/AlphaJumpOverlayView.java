/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.car.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.Collection;

import androidx.annotation.NonNull;
import androidx.car.R;
import androidx.gridlayout.widget.GridLayout;

/**
 * This view shows a grid of alphabetic letters that you can tap on to advance a list to the
 * beginning of that list.
 */
public class AlphaJumpOverlayView extends GridLayout {
    private IAlphaJumpAdapter mAdapter;
    private PagedListView mPagedListView;
    private Collection<IAlphaJumpAdapter.Bucket> mBuckets;

    public AlphaJumpOverlayView(@NonNull Context context) {
        super(context);
        setBackgroundResource(R.color.car_card);
        setColumnCount(context.getResources().getInteger(R.integer.alpha_jump_button_columns));
        setUseDefaultMargins(false);
    }

    void init(PagedListView plv, IAlphaJumpAdapter adapter) {
        mPagedListView = plv;
        mAdapter = adapter;
        mBuckets = adapter.getAlphaJumpBuckets();

        createButtons();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        // TODO: change the hamburger button into a back button...
        if (visibility == VISIBLE && changedView == this) {
            mAdapter.onAlphaJumpEnter();
        }
    }

    private void createButtons() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        removeAllViews();
        for (IAlphaJumpAdapter.Bucket bucket : mBuckets) {
            View container = inflater.inflate(R.layout.car_alpha_jump_button, this, false);
            TextView btn = container.findViewById(R.id.button);
            btn.setText(bucket.getLabel());
            btn.setOnClickListener(this::onButtonClick);
            btn.setTag(bucket);
            if (bucket.isEmpty()) {
                btn.setEnabled(false);
            }
            addView(container);
        }
    }

    private void onButtonClick(View v) {
        setVisibility(View.GONE);
        IAlphaJumpAdapter.Bucket bucket = (IAlphaJumpAdapter.Bucket) v.getTag();
        if (bucket != null) {
            mAdapter.onAlphaJumpLeave(bucket);

            mPagedListView.snapToPosition(bucket.getIndex());
        }
    }
}
