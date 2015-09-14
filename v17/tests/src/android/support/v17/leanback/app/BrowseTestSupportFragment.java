/* This file is auto-generated from BrowseTestFragment.java.  DO NOT MODIFY. */

/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.view.View;

/**
 * @hide from javadoc
 */
public class BrowseTestSupportFragment extends BrowseSupportFragment {
    private static final String TAG = "BrowseTestSupportFragment";

    final static int DEFAULT_NUM_ROWS = 100;
    final static int DEFAULT_REPEAT_PER_ROW = 20;
    final static long DEFAULT_LOAD_DATA_DELAY = 2000;
    final static boolean DEFAULT_TEST_ENTRANCE_TRANSITION = true;

    static int NUM_ROWS = DEFAULT_NUM_ROWS;
    static int REPEAT_PER_ROW = DEFAULT_REPEAT_PER_ROW;
    static long LOAD_DATA_DELAY = DEFAULT_LOAD_DATA_DELAY;
    static boolean TEST_ENTRANCE_TRANSITION = DEFAULT_TEST_ENTRANCE_TRANSITION;

    private ArrayObjectAdapter mRowsAdapter;

    // For good performance, it's important to use a single instance of
    // a card presenter for all rows using that presenter.
    final static StringPresenter sCardPresenter = new StringPresenter();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setTitle("BrowseTestSupportFragment");
        setHeadersState(HEADERS_ENABLED);

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onSearchClicked");
            }
        });

        setupRows();
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                    RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemSelected: " + item + " row " + row);
            }
        });
        if (TEST_ENTRANCE_TRANSITION) {
            // don't run entrance transition if fragment is restored.
            if (savedInstanceState == null) {
                prepareEntranceTransition();
            }
        }
        // simulates in a real world use case  data being loaded two seconds later
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadData();
                startEntranceTransition();
            }
        }, LOAD_DATA_DELAY);
    }

    private void setupRows() {
        ListRowPresenter lrp = new ListRowPresenter();

        mRowsAdapter = new ArrayObjectAdapter(lrp);

        setAdapter(mRowsAdapter);
    }

    private void loadData() {
        for (int i = 0; i < NUM_ROWS; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(sCardPresenter);
            for (int j = 0; j < REPEAT_PER_ROW; ++j) {
                listRowAdapter.add("Hello world");
                listRowAdapter.add("This is a test");
                listRowAdapter.add("Android TV");
                listRowAdapter.add("Leanback");
                listRowAdapter.add("Hello world");
                listRowAdapter.add("Android TV");
                listRowAdapter.add("Leanback");
                listRowAdapter.add("GuidedStepFragment");
            }
            HeaderItem header = new HeaderItem(i, "Row " + i);
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }

    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            Log.i(TAG, "onItemClicked: " + item + " row " + row);
        }
    }
}
