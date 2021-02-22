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
package androidx.leanback.app;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;

import androidx.leanback.test.R;
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.SparseArrayObjectAdapter;

/**
 * Base class provides overview row and some related rows.
 */
public class DetailsTestSupportFragment extends androidx.leanback.app.DetailsSupportFragment {
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

    private static final int ACTION_RENT = 2;
    private static final int ACTION_BUY = 3;

    protected long mTimeToLoadOverviewRow = 1000;
    protected long mTimeToLoadRelatedRow = 2000;

    private Action mActionRent;
    private Action mActionBuy;

    protected int mMinVerticalOffset = -100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Leanback Sample App");

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

    public void setItem(PhotoItem photoItem) {
        mPhotoItem = photoItem;
        mRowsAdapter.clear();
        new Handler().postDelayed(new Runnable() {
            @Override
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
        }, mTimeToLoadOverviewRow);


        new Handler().postDelayed(new Runnable() {
            @Override
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
        }, mTimeToLoadRelatedRow);

        setAdapter(mRowsAdapter);
    }

}
