/*
 * Copyright 2020 The Android Open Source Project
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
package com.example.android.leanback;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.paging.PagingDataAdapter;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowPresenter;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagingData;

/**
 * PagedRowPresenter
 */
public class PagedRowPresenter extends ListRowPresenter {
    private LifecycleOwner mLifecycleOwner;
    private ListRow mRow;
    @Override
    protected LiveDataRowPresenterViewHolder createRowViewHolder(ViewGroup parent) {
        ListRowPresenter.ViewHolder listRowPresenterViewHolder =
                (ListRowPresenter.ViewHolder) super.createRowViewHolder(parent);
        return new LiveDataRowPresenterViewHolder(
                listRowPresenterViewHolder.view,
                listRowPresenterViewHolder.getGridView(),
                listRowPresenterViewHolder.getListRowPresenter());
    }
    @Override
    protected void onBindRowViewHolder(
            @NonNull RowPresenter.ViewHolder holder,
            @NonNull Object item
    ) {
        super.onBindRowViewHolder(holder, item);
        mRow = (ListRow) item;

        final PagingDataAdapter<PhotoItem> adapter =
                (PagingDataAdapter<PhotoItem>) mRow.getAdapter();
        FragmentActivity attachedFragmentActivity = (FragmentActivity) holder.view.getContext();
        mLifecycleOwner = attachedFragmentActivity;
        PhotoViewModel viewModel =
                new ViewModelProvider(attachedFragmentActivity).get(PhotoViewModel.class);
        viewModel.getPagingDataLiveData().observe(mLifecycleOwner,
                pagingData -> adapter.submitData(mLifecycleOwner.getLifecycle(), pagingData));
    }
    private static class LiveDataRowPresenterViewHolder extends ListRowPresenter.ViewHolder {
        private LiveData<PagingData<PhotoItem>> mLiveData;
        LiveDataRowPresenterViewHolder(View rootView, HorizontalGridView gridView,
                ListRowPresenter p) {
            super(rootView, gridView, p);
        }
        public void setLiveData(LiveData<PagingData<PhotoItem>> liveData) {
            mLiveData = liveData;
        }
        public final LiveData<PagingData<PhotoItem>> getLiveData() {
            return mLiveData;
        }
    }
}
