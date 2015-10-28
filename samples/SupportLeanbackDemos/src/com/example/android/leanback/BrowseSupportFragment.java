/* This file is auto-generated from BrowseFragment.java.  DO NOT MODIFY. */

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
import android.os.Handler;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v17.leanback.app.GuidedStepSupportFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class BrowseSupportFragment extends android.support.v17.leanback.app.BrowseSupportFragment {
    private static final String TAG = "leanback.BrowseSupportFragment";

    private static final boolean TEST_ENTRANCE_TRANSITION = true;
    private static final int NUM_ROWS = 10;
    // Row heights default to wrap content
    private static final boolean USE_FIXED_ROW_HEIGHT = false;

    private ArrayObjectAdapter mRowsAdapter;
    private BackgroundHelper mBackgroundHelper = new BackgroundHelper();

    // For good performance, it's important to use a single instance of
    // a card presenter for all rows using that presenter.
    final static CardPresenter sCardPresenter = new CardPresenter();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setBadgeDrawable(getActivity().getResources().getDrawable(R.drawable.ic_title));
        setTitle("Leanback Sample App");
        setHeadersState(HEADERS_ENABLED);

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchSupportActivity.class);
                startActivity(intent);
            }
        });

        setupRows();
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                    RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemSelected: " + item + " row " + row);

                if (isShowingHeaders()) {
                    mBackgroundHelper.setBackground(getActivity(), null);
                }
                else if (item instanceof PhotoItem) {
                    mBackgroundHelper.setBackground(
                            getActivity(), ((PhotoItem) item).getImageResourceId());
                }
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
            public void run() {
                loadData();
                startEntranceTransition();
            }
        }, 2000);
    }

    private void setupRows() {
        ListRowPresenter lrp = new ListRowPresenter();

        if (USE_FIXED_ROW_HEIGHT) {
            lrp.setRowHeight(CardPresenter.getRowHeight(getActivity()));
            lrp.setExpandedRowHeight(CardPresenter.getExpandedRowHeight(getActivity()));
        }

        mRowsAdapter = new ArrayObjectAdapter(lrp);

        setAdapter(mRowsAdapter);
    }

    private void loadData() {
        for (int i = 0; i < NUM_ROWS; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(sCardPresenter);
            listRowAdapter.add(new PhotoItem("Hello world", R.drawable.gallery_photo_1));
            listRowAdapter.add(new PhotoItem("This is a test", "Only a test", R.drawable.gallery_photo_2));
            listRowAdapter.add(new PhotoItem("Android TV", "by Google", R.drawable.gallery_photo_3));
            listRowAdapter.add(new PhotoItem("Leanback", R.drawable.gallery_photo_4));
            listRowAdapter.add(new PhotoItem("Hello world", R.drawable.gallery_photo_5));
            listRowAdapter.add(new PhotoItem("This is a test", "Open GuidedStepSupportFragment", R.drawable.gallery_photo_6));
            listRowAdapter.add(new PhotoItem("Android TV", "open RowsSupportActivity", R.drawable.gallery_photo_7));
            listRowAdapter.add(new PhotoItem("Leanback", "open BrowseSupportActivity", R.drawable.gallery_photo_8));
            HeaderItem header = new HeaderItem(i, "Row " + i);
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }

    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

            Intent intent;
            Bundle bundle;
            if (((PhotoItem) item).getImageResourceId() == R.drawable.gallery_photo_6) {
                GuidedStepSupportFragment.add(getFragmentManager(),
                        new GuidedStepSupportActivity.FirstStepFragment(),
                        R.id.lb_guidedstep_host);
                return;
            } else if ( ((PhotoItem) item).getImageResourceId() == R.drawable.gallery_photo_8) {
                intent = new Intent(getActivity(), BrowseSupportActivity.class);
                bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                        .toBundle();
            } else if ( ((PhotoItem) item).getImageResourceId() == R.drawable.gallery_photo_7) {
                intent = new Intent(getActivity(), RowsSupportActivity.class);
                bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                        .toBundle();
            } else {
                intent = new Intent(getActivity(), DetailsSupportActivity.class);
                intent.putExtra(DetailsSupportActivity.EXTRA_ITEM, (PhotoItem) item);
                bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView)itemViewHolder.view).getMainImageView(),
                        DetailsSupportActivity.SHARED_ELEMENT_NAME).toBundle();
            }
            getActivity().startActivity(intent, bundle);
        }
    }
}
