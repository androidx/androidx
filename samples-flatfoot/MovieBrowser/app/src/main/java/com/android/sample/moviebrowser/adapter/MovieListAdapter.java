/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.sample.moviebrowser.adapter;

import android.databinding.DataBindingUtil;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.android.sample.moviebrowser.MovieData;
import com.android.sample.moviebrowser.R;
import com.android.sample.moviebrowser.databinding.MovieCardBinding;
import com.android.sample.moviebrowser.model.SearchModel;

import java.util.List;

/**
 * Adapter for our main fragment.
 */
public class MovieListAdapter extends RecyclerView.Adapter<MovieListAdapter.MovieBindingHolder> {
    /**
     * Holder for the data cell.
     */
    public static class MovieBindingHolder extends RecyclerView.ViewHolder {
        private MovieCardBinding mViewDataBinding;

        public MovieBindingHolder(MovieCardBinding viewDataBinding) {
            super(viewDataBinding.getRoot());
            mViewDataBinding = viewDataBinding;
        }

        public MovieCardBinding getBinding() {
            return mViewDataBinding;
        }
    }

    private Fragment mFragment;
    private SearchModel mSearchModel;

    /**
     * Creates an adapter.
     */
    public MovieListAdapter(Fragment fragment, SearchModel searchModel) {
        mFragment = fragment;
        mSearchModel = searchModel;
    }

    @Override
    public MovieBindingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MovieCardBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                R.layout.movie_card, parent, false);
        return new MovieBindingHolder(binding);
    }

    @Override
    public void onBindViewHolder(MovieBindingHolder holder, final int position) {
        List<MovieData> movieDataList = mSearchModel.getMovieListLiveData().getValue();
        final MovieData data = movieDataList.get(position);

        // Use data binding for wiring the data and the click handler
        MovieCardBinding binding = holder.getBinding();
        binding.setMovie(data);
        binding.setFragment(mFragment);
        binding.executePendingBindings();

        // Do we need to request another page?
        if (position <= (movieDataList.size() - 2)) {
            // We are not close to the end of our data
            return;
        }

        mSearchModel.fetchMoreIfNecessary();
    }

    @Override
    public int getItemCount() {
        return mSearchModel.getMovieListLiveData().getValue().size();
    }
}
