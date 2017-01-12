/*
 * Copyright (C) 2016 The Android Open Source Project
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
package android.support.v17.leanback.app;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.test.R;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.view.ViewGroup;

public class DetailsTestFragment extends android.support.v17.leanback.app.DetailsFragment {
    private static final String ITEM = "item";
    public static final String VERTICAL_OFFSET = "details_fragment";

    private static final int NUM_ROWS = 3;
    private ArrayObjectAdapter mRowsAdapter;
    private PhotoItem mPhotoItem;
    private final Presenter mCardPresenter = new Presenter() {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            ImageCardView cardView = new ImageCardView(getActivity());
            cardView.setFocusable(true);
            cardView.setFocusableInTouchMode(true);
            return new ViewHolder(cardView);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ImageCardView imageCardView = (ImageCardView) viewHolder.view;
            imageCardView.setTitleText("Android Tv");
            imageCardView.setContentText("Android Tv Production Inc.");
            imageCardView.setMainImageDimensions(313, 176);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    };

    private static final int ACTION_PLAY = 1;
    private static final int ACTION_RENT = 2;
    private static final int ACTION_BUY = 3;

    private static final long TIME_TO_LOAD_OVERVIEW_ROW_MS = 1000;
    private static final long TIME_TO_LOAD_RELATED_ROWS_MS = 2000;

    private Action mActionPlay;
    private Action mActionRent;
    private Action mActionBuy;

    private FullWidthDetailsOverviewSharedElementHelper mHelper;
    private DetailsBackgroundParallaxHelper mParallaxHelper;
    private int mMinVerticalOffset;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Leanback Sample App");

        if (getArguments() != null) {
            mMinVerticalOffset = getArguments().getInt(VERTICAL_OFFSET, -100);
        }
        mParallaxHelper = new DetailsBackgroundParallaxHelper.ParallaxBuilder(
                getActivity(),
                getParallaxManager())
                .setCoverImageMinVerticalOffset(mMinVerticalOffset)
                .build();
        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        backgroundManager.setDrawable(mParallaxHelper.getDrawable());

        mActionPlay = new Action(ACTION_PLAY, "Play");
        mActionRent = new Action(ACTION_RENT, "Rent", "$3.99",
                getResources().getDrawable(R.drawable.ic_action_a));
        mActionBuy = new Action(ACTION_BUY, "Buy $9.99");

        ClassPresenterSelector ps = new ClassPresenterSelector();
        FullWidthDetailsOverviewRowPresenter dorPresenter =
                new FullWidthDetailsOverviewRowPresenter(new AbstractDetailsDescriptionPresenter() {
                    @Override
                    protected void onBindDescription(
                            AbstractDetailsDescriptionPresenter.ViewHolder vh, Object item) {
                        vh.getTitle().setText("Funny Movie");
                        vh.getSubtitle().setText("Android TV Production Inc.");
                        vh.getBody().setText("What a great movie!");
                    }
                });

        ps.addClassPresenter(DetailsOverviewRow.class, dorPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());
        mRowsAdapter = new ArrayObjectAdapter(ps);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ITEM, mPhotoItem);
    }

    public void setItem(PhotoItem photoItem) {
        mPhotoItem = photoItem;
        mRowsAdapter.clear();
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (getActivity() == null) {
                    return;
                }
                Resources res = getActivity().getResources();
                DetailsOverviewRow dor = new DetailsOverviewRow(mPhotoItem.getTitle());
                dor.setImageDrawable(res.getDrawable(mPhotoItem.getImageResourceId()));
                SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter();
                adapter.set(ACTION_RENT, mActionRent);
                adapter.set(ACTION_BUY, mActionBuy);
                dor.setActionsAdapter(adapter);
                mRowsAdapter.add(0, dor);
                setSelectedPosition(0, true);
            }
        }, TIME_TO_LOAD_OVERVIEW_ROW_MS);


        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (getActivity() == null) {
                    return;
                }
                for (int i = 0; i < NUM_ROWS; ++i) {
                    ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(mCardPresenter);
                    listRowAdapter.add(new PhotoItem("Hello world", R.drawable.spiderman));
                    listRowAdapter.add(new PhotoItem("This is a test", R.drawable.spiderman));
                    listRowAdapter.add(new PhotoItem("Android TV", R.drawable.spiderman));
                    listRowAdapter.add(new PhotoItem("Leanback", R.drawable.spiderman));
                    HeaderItem header = new HeaderItem(i, "Row " + i);
                    mRowsAdapter.add(new ListRow(header, listRowAdapter));
                }
            }
        }, TIME_TO_LOAD_RELATED_ROWS_MS);

        setAdapter(mRowsAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        Bitmap bitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                R.drawable.spiderman);
        mParallaxHelper.setCoverImageBitmap(bitmap);
    }

    DetailsBackgroundParallaxHelper getParallaxHelper() {
        return mParallaxHelper;
    }
}
