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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.paging.PagingDataAdapter;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.TitleHelper;
import androidx.recyclerview.widget.DiffUtil;

public class RowsFragment extends androidx.leanback.app.RowsFragment {

    private static final String TAG = "leanback.RowsFragment";

    private static final int NUM_ROWS = 10;
    // Row heights default to wrap content
    private static final boolean USE_FIXED_ROW_HEIGHT = false;

    private TitleHelper mTitleHelper;

    public void setTitleHelper(TitleHelper titleHelper) {
        mTitleHelper = titleHelper;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setupRows();
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                    RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemSelected: " + item + " row " + row);
                if (mTitleHelper != null) {
                    mTitleHelper.showTitle(getAdapter() == null || getAdapter().size() == 0 ||
                            getAdapter().get(0) == row);
                }
            }
        });
    }

    private void setupRows() {
        ClassPresenterSelector cs = new ClassPresenterSelector();
        ListRowPresenter lrp = new ListRowPresenter();
        PagedRowPresenter prp = new PagedRowPresenter();

        // For good performance, it's important to use a single instance of
        // a card presenter for all rows using that presenter.
        final CardPresenter cardPresenter = new CardPresenter();

        if (USE_FIXED_ROW_HEIGHT) {
            lrp.setRowHeight(cardPresenter.getRowHeight(getActivity()));
            lrp.setExpandedRowHeight(cardPresenter.getExpandedRowHeight(getActivity()));
            prp.setRowHeight(cardPresenter.getRowHeight(getActivity()));
            prp.setExpandedRowHeight(cardPresenter.getExpandedRowHeight(getActivity()));
        }

        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(cs);

        for (int i = 0; i < NUM_ROWS; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
            listRowAdapter.add(new PhotoItem("Hello world", R.drawable.gallery_photo_1));
            listRowAdapter.add(new PhotoItem("This is a test", R.drawable.gallery_photo_2));
            listRowAdapter.add(new PhotoItem("Android TV", R.drawable.gallery_photo_3));
            listRowAdapter.add(new PhotoItem("Leanback", R.drawable.gallery_photo_4));
            listRowAdapter.add(new PhotoItem("Hello world", R.drawable.gallery_photo_5));
            listRowAdapter.add(new PhotoItem("This is a test", R.drawable.gallery_photo_6));
            listRowAdapter.add(new PhotoItem("Android TV", R.drawable.gallery_photo_7));
            listRowAdapter.add(new PhotoItem("Leanback", R.drawable.gallery_photo_8));
            HeaderItem header = new HeaderItem(i, "Row " + i);
            rowsAdapter.add(new ListRow(header, listRowAdapter));
        }

        rowsAdapter.add(getLiveDataRow(NUM_ROWS, cardPresenter));
        cs.addClassPresenter(ListRow.class, lrp);
        cs.addClassPresenter(LiveDataListRow.class, prp);
        setAdapter(rowsAdapter);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            Intent intent = new Intent(getActivity(), DetailsSupportActivity.class);
            intent.putExtra(DetailsSupportActivity.EXTRA_ITEM, (PhotoItem) item);

            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    getActivity(),
                    ((ImageCardView) itemViewHolder.view).getMainImageView(),
                    DetailsSupportActivity.SHARED_ELEMENT_NAME).toBundle();
            getActivity().startActivity(intent, bundle);
        }
    }

    private ListRow getLiveDataRow(int index, CardPresenter cardPresenter) {
        PagingDataAdapter<PhotoItem> pagedListAdapter =
                new PagingDataAdapter<PhotoItem>(cardPresenter,
                        new DiffUtil.ItemCallback<PhotoItem>() {
                            @Override
                            public boolean areItemsTheSame(@NonNull PhotoItem oldItem,
                                    @NonNull PhotoItem newItem) {
                                return oldItem.getId() == newItem.getId();
                            }
                            @Override
                            public boolean areContentsTheSame(@NonNull PhotoItem oldItem,
                                    @NonNull PhotoItem newItem) {
                                return oldItem.equals(newItem);
                            }
                        });

        HeaderItem header = new HeaderItem(index, "Row with paging data adapter");
        return new LiveDataListRow(header, pagedListAdapter);
    }
}
