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

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.util.Log;

public class DetailsFragment extends android.support.v17.leanback.app.DetailsFragment {
    private static final String TAG = "leanback.BrowseFragment";

    private static final int NUM_ROWS = 3;
    private ArrayObjectAdapter mRowsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setupRows();
    }

    private void setupRows() {
        ClassPresenterSelector ps = new ClassPresenterSelector();
        ps.addClassPresenter(DetailsOverviewRow.class,
                new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter()));
        ps.addClassPresenter(ListRow.class,
                new ListRowPresenter());
        mRowsAdapter = new ArrayObjectAdapter(ps);

        Resources res = getActivity().getResources();
        DetailsOverviewRow dor = new DetailsOverviewRow("Details Overview");
        dor.setImageDrawable(res.getDrawable(R.drawable.details_img));
        dor.addAction(new Action(1, "Buy $9.99"));
        dor.addAction(new Action(2, "Rent", "$3.99", res.getDrawable(R.drawable.ic_action_a)));
        mRowsAdapter.add(dor);

        for (int i = 0; i < NUM_ROWS; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new StringPresenter());
            listRowAdapter.add("Hello world");
            listRowAdapter.add("This is a test");
            HeaderItem header = new HeaderItem(i, "Row " + i, null);
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }

        setAdapter(mRowsAdapter);
    }

}
