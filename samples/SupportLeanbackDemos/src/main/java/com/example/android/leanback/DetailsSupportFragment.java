// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from DetailsFragment.java.  DO NOT MODIFY. */

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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.DetailsOverviewRowPresenter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnActionClickedListener;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SparseArrayObjectAdapter;

public class DetailsSupportFragment extends androidx.leanback.app.DetailsSupportFragment {
    private static final String TAG = "leanback.DetailsSupportFragment";
    private static final String ITEM = "item";

    private static final int NUM_ROWS = 3;
    private ArrayObjectAdapter mRowsAdapter;
    private PhotoItem mPhotoItem;
    final CardPresenter cardPresenter = new CardPresenter();
    private BackgroundHelper mBackgroundHelper;

    private static final int ACTION_PLAY = 1;
    private static final int ACTION_RENT = 2;
    private static final int ACTION_BUY = 3;

    private static final boolean TEST_SHARED_ELEMENT_TRANSITION = true;
    private static final boolean TEST_ENTRANCE_TRANSITION = true;

    private static final long TIME_TO_LOAD_OVERVIEW_ROW_MS = 1000;
    private static final long TIME_TO_LOAD_RELATED_ROWS_MS = 2000;

    private Action mActionPlay;
    private Action mActionRent;
    private Action mActionBuy;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mBackgroundHelper = new BackgroundHelper(getActivity());
        mBackgroundHelper.attachToWindow();

        Context context = getActivity();
        setBadgeDrawable(ResourcesCompat.getDrawable(context.getResources(),
                R.drawable.ic_title, context.getTheme()));
        setTitle("Leanback Sample App");
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchSupportActivity.class);
                startActivity(intent);
            }
        });

        mActionPlay = new Action(ACTION_PLAY, "Play");
        mActionRent = new Action(ACTION_RENT, "Rent", "$3.99", ResourcesCompat.getDrawable(
                context.getResources(), R.drawable.ic_action_a, context.getTheme()));
        mActionBuy = new Action(ACTION_BUY, "Buy $9.99");

        ClassPresenterSelector ps = new ClassPresenterSelector();
        @SuppressWarnings("deprecation")
        DetailsOverviewRowPresenter dorPresenter =
                new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter());
        dorPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                final Context context = getActivity();
                Toast.makeText(context, action.toString(), Toast.LENGTH_SHORT).show();
                DetailsOverviewRow dor = (DetailsOverviewRow) mRowsAdapter.get(0);
                if (action.getId() == ACTION_BUY) {
                    // on the UI thread, we can modify actions adapter directly
                    SparseArrayObjectAdapter actions = (SparseArrayObjectAdapter)
                            dor.getActionsAdapter();
                    actions.set(ACTION_PLAY, mActionPlay);
                    actions.clear(ACTION_RENT);
                    actions.clear(ACTION_BUY);
                    dor.setItem(mPhotoItem.getTitle() + "(Owned)");
                    dor.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(),
                            R.drawable.details_img_16x9, context.getTheme()));
                } else if (action.getId() == ACTION_RENT) {
                    // on the UI thread, we can modify actions adapter directly
                    SparseArrayObjectAdapter actions = (SparseArrayObjectAdapter)
                            dor.getActionsAdapter();
                    actions.set(ACTION_PLAY, mActionPlay);
                    actions.clear(ACTION_RENT);
                    dor.setItem(mPhotoItem.getTitle() + "(Rented)");
                } else if (action.getId() == ACTION_PLAY) {
                    Intent intent = new Intent(context, PlaybackActivity.class);
                    getActivity().startActivity(intent);
                }
            }
        });

        ps.addClassPresenter(DetailsOverviewRow.class, dorPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());

        mRowsAdapter = new ArrayObjectAdapter(ps);
        updateAdapter();

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                    RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemClicked: " + item + " row " + row);
                if (item instanceof PhotoItem){
                    Intent intent = new Intent(getActivity(), DetailsSupportActivity.class);
                    intent.putExtra(DetailsSupportActivity.EXTRA_ITEM, (PhotoItem) item);

                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            getActivity(),
                            ((ImageCardView)itemViewHolder.view).getMainImageView(),
                            DetailsSupportActivity.SHARED_ELEMENT_NAME).toBundle();
                    getActivity().startActivity(intent, bundle);
                }
            }
        });
        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                    RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemSelected: " + item + " row " + row);
            }
        });

        if (TEST_SHARED_ELEMENT_TRANSITION) {
            dorPresenter.setSharedElementEnterTransition(getActivity(),
                    DetailsSupportActivity.SHARED_ELEMENT_NAME);
        }
        if (TEST_ENTRANCE_TRANSITION) {
            // don't run entrance transition if Activity is restored.
            if (savedInstanceState == null) {
                prepareEntranceTransition();
            }
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ITEM, mPhotoItem);
    }

    public void setItem(PhotoItem photoItem) {
        mPhotoItem = photoItem;
        updateAdapter();
    }

    void updateAdapter() {
        if (mRowsAdapter == null) {
            return;
        }
        mRowsAdapter.clear();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final Context context = getActivity();
                DetailsOverviewRow dor = new DetailsOverviewRow(mPhotoItem.getTitle());
                dor.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(),
                        mPhotoItem.getImageResourceId(), context.getTheme()));
                SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter();
                adapter.set(ACTION_RENT, mActionRent);
                adapter.set(ACTION_BUY, mActionBuy);
                dor.setActionsAdapter(adapter);
                mRowsAdapter.add(0, dor);
                setSelectedPosition(0, false);
            }
        }, TIME_TO_LOAD_OVERVIEW_ROW_MS);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < NUM_ROWS; ++i) {
                    ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                    listRowAdapter.add(new PhotoItem("Hello world", R.drawable.gallery_photo_1));
                    listRowAdapter.add(new PhotoItem("This is a test", R.drawable.gallery_photo_2));
                    listRowAdapter.add(new PhotoItem("Android TV", R.drawable.gallery_photo_3));
                    listRowAdapter.add(new PhotoItem("Leanback", R.drawable.gallery_photo_4));
                    HeaderItem header = new HeaderItem(i, "Row " + i);
                    mRowsAdapter.add(new ListRow(header, listRowAdapter));
                }
                if (TEST_ENTRANCE_TRANSITION) {
                    startEntranceTransition();
                }
            }
        }, TIME_TO_LOAD_RELATED_ROWS_MS);
        setAdapter(mRowsAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPhotoItem != null) {
            mBackgroundHelper.setBackground(mPhotoItem.getImageResourceId());
        }
    }
}
