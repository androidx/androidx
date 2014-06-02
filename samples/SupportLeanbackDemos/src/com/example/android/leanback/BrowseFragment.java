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
package com.example.android.leanback;

import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemClickedListener;
import android.support.v17.leanback.widget.Row;
import android.util.Log;
import android.view.View;

public class BrowseFragment extends android.support.v17.leanback.app.BrowseFragment {
    private static final String TAG = "leanback.BrowseFragment";

    private static final int NUM_ROWS = 10;
    private ArrayObjectAdapter mRowsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        Params p = new Params();
        p.setBadgeImage(getActivity().getResources().getDrawable(R.drawable.ic_title));
        p.setTitle("Leanback Sample App");
        p.setHeadersState(HEADERS_ENABLED);
        setBrowseParams(p);

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setupRows();
        setOnItemClickedListener(new ItemClickedListener());
    }

    private void setupRows() {
        ListRowPresenter lrp = new ListRowPresenter();
        float density = getActivity().getResources().getDisplayMetrics().density;
        float height = 160 * density + 0.5f;
        float expandedHeight = height + 52 * density + 0.5f;
        lrp.setRowHeight((int)height);
        lrp.setExpandedRowHeight((int)expandedHeight);

        mRowsAdapter = new ArrayObjectAdapter(lrp);

        // For good performance, it's important to use a single instance of
        // a card presenter for all rows using that presenter.
        final CardPresenter cardPresenter = new CardPresenter();

        for (int i = 0; i < NUM_ROWS; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
            listRowAdapter.add("Hello world");
            listRowAdapter.add("This is a test");
            HeaderItem header = new HeaderItem(i, "Row " + i, null);
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }

        setAdapter(mRowsAdapter);
    }

    private final class ItemClickedListener implements OnItemClickedListener {
        public void onItemClicked(Object item, Row row) {
            // TODO: use a fragment transaction instead of launching a new
            // activity
            Intent intent = new Intent(getActivity(), DetailsActivity.class);
            startActivity(intent);
        }
    }
}
