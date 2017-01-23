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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.sample.moviebrowser.adapter.RepositoryListAdapter;
import com.android.sample.moviebrowser.model.AuthTokenModel;
import com.android.sample.moviebrowser.model.RepositoryListModel;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

import java.util.List;

/**
 * Fragment that shows the list of all repositories that match the current search term.
 */
public class RepositoryListFragment extends LifecycleFragment {
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final RecyclerView recyclerView = (RecyclerView) inflater.inflate(
                R.layout.fragment_repo_list, container, false);
        final int columnCount = getContext().getResources().getInteger(R.integer.column_count);

        // Get the search model from the activity's scope so that we observe LiveData changes
        // on the same list of movies that matches the search query set from the search box
        // defined in the activity's layout.
        final RepositoryListModel searchModel = ViewModelStore.get(
                (LifecycleProvider) getActivity(),
                "mainRepoModel", RepositoryListModel.class);

        recyclerView.setAdapter(new RepositoryListAdapter(this, searchModel));
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));

        // Register an observer on the LiveData that wraps the list of repositories to update the
        // adapter on every change
        searchModel.getRepositoryListLiveData().observe(this, new Observer<List<RepositoryData>>() {
            @Override
            public void onChanged(@Nullable List<RepositoryData> repositoryDataList) {
                recyclerView.getAdapter().notifyDataSetChanged();
            }
        });

        AuthTokenModel authTokenModel = ViewModelStore.get((LifecycleProvider) getActivity(),
                "authTokenModel", AuthTokenModel.class);
        authTokenModel.getAuthTokenData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                if (!TextUtils.isEmpty(s)) {
                    searchModel.resumeLoading();
                }
            }
        });

        return recyclerView;
    }
}
