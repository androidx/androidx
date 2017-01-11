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
package com.android.sample.moviebrowser;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.sample.moviebrowser.adapter.MovieListAdapter;
import com.android.sample.moviebrowser.model.SearchModel;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

import java.util.List;

/**
 * Main fragment.
 */
public class MainActivityFragment extends LifecycleFragment {
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_main,
                container, false);
        final int columnCount = getContext().getResources().getInteger(R.integer.column_count);

        // Get the search model from the activity's scope so that we observe LiveData changes
        // on the same list of movies that matches the search query set from the search box
        // defined in the activity's layout.
        SearchModel searchModel = ViewModelStore.get((LifecycleProvider) getActivity(),
                "searchModel", SearchModel.class);

        recyclerView.setAdapter(new MovieListAdapter(this, searchModel));
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));

        // Register an observer on the LiveData that wraps the list of movies to update the
        // adapter on every change
        searchModel.getMovieListLiveData().observe(this, new Observer<List<MovieData>>() {
            @Override
            public void onChanged(@Nullable List<MovieData> movieDatas) {
                recyclerView.getAdapter().notifyDataSetChanged();
            }
        });

        return recyclerView;
    }
}
