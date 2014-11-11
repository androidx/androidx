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
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.widget.Toast;

public class DetailsFragment extends android.support.v17.leanback.app.DetailsFragment {
    private static final String TAG = "leanback.DetailsFragment";
    private static final String ITEM = "item";

    private static final int NUM_ROWS = 3;
    private ArrayObjectAdapter mRowsAdapter;
    private PhotoItem mPhotoItem;

    private static final int ACTION_BUY = 1;
    private static final int ACTION_RENT = 2;
    private static final int ACTION_PLAY = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        ClassPresenterSelector ps = new ClassPresenterSelector();
        DetailsOverviewRowPresenter dorPresenter =
                new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter());
        dorPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                Toast.makeText(getActivity(), action.toString(), Toast.LENGTH_SHORT).show();
                if (action.getId() == ACTION_BUY) {
                    DetailsOverviewRow dor = new DetailsOverviewRow(mPhotoItem.getTitle() + "(Owned)");
                    dor.setImageDrawable(getResources().getDrawable(mPhotoItem.getImageResourceId()));
                    dor.addAction(new Action(ACTION_PLAY, "Play"));
                    mRowsAdapter.replace(0, dor);
                } else if (action.getId() == ACTION_RENT) {
                    DetailsOverviewRow dor = new DetailsOverviewRow(mPhotoItem.getTitle() + "(Rented)");
                    dor.setImageDrawable(getResources().getDrawable(mPhotoItem.getImageResourceId()));
                    dor.addAction(new Action(ACTION_PLAY, "Play"));
                    dor.addAction(new Action(ACTION_BUY, "Buy $9.99"));
                    mRowsAdapter.replace(0, dor);
                } else if (action.getId() == ACTION_PLAY) {
                    Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);
                    getActivity().startActivity(intent);
                }
            }
        });

        ps.addClassPresenter(DetailsOverviewRow.class, dorPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());

        mRowsAdapter = new ArrayObjectAdapter(ps);

        PhotoItem item = (PhotoItem) (savedInstanceState != null ?
                savedInstanceState.getParcelable(ITEM) : null);
        if (item != null) {
            setItem(item);
        }
        dorPresenter.setSharedElementEnterTransition(getActivity(),
                DetailsActivity.SHARED_ELEMENT_NAME);

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                    RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemClicked: " + item + " row " + row);
                if (item instanceof PhotoItem){
                    Intent intent = new Intent(getActivity(), DetailsActivity.class);
                    intent.putExtra(DetailsActivity.EXTRA_ITEM, (PhotoItem) item);

                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            getActivity(),
                            ((ImageCardView)itemViewHolder.view).getMainImageView(),
                            DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
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
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ITEM, mPhotoItem);
    }

    public void setItem(PhotoItem photoItem) {
        mPhotoItem = photoItem;

        mRowsAdapter.clear();
        Resources res = getActivity().getResources();
        DetailsOverviewRow dor = new DetailsOverviewRow(photoItem.getTitle());
        dor.setImageDrawable(res.getDrawable(photoItem.getImageResourceId()));
        dor.addAction(new Action(ACTION_BUY, "Buy $9.99"));
        dor.addAction(new Action(ACTION_RENT, "Rent", "$3.99", res.getDrawable(R.drawable.ic_action_a)));
        mRowsAdapter.add(dor);

        final CardPresenter cardPresenter = new CardPresenter();
        for (int i = 0; i < NUM_ROWS; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
            listRowAdapter.add(new PhotoItem("Hello world", R.drawable.gallery_photo_1));
            listRowAdapter.add(new PhotoItem("This is a test", R.drawable.gallery_photo_2));
            listRowAdapter.add(new PhotoItem("Android TV", R.drawable.gallery_photo_3));
            listRowAdapter.add(new PhotoItem("Leanback", R.drawable.gallery_photo_4));
            HeaderItem header = new HeaderItem(i, "Row " + i);
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }

        setAdapter(mRowsAdapter);
    }

}
