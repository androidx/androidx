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
package com.android.sample.githubbrowser;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.sample.githubbrowser.adapter.RepositoryListAdapter;
import com.android.sample.githubbrowser.model.AuthTokenModel;
import com.android.sample.githubbrowser.model.RepositoryListModel;
import com.android.sample.githubbrowser.model.RepositorySearchModel;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

/**
 * Fragment that shows the list of all repositories that match the current search term.
 */
public class RepositoryListFragment extends LifecycleFragment {
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final RecyclerView recyclerView = (RecyclerView) inflater.inflate(
                R.layout.fragment_repo_list, container, false);

        // Get all the models that are needed for this fragment

        // The model for our search query. Note that we are using the activity scope since
        // that is where the search box "lives"
        final RepositorySearchModel mainSearchModel = ViewModelStore.get(
                (LifecycleProvider) getActivity(),
                "mainSearchModel", RepositorySearchModel.class);
        // The model for the list of repositories that are shown in this fragment.
        final RepositoryListModel repositoryListModel = ViewModelStore.get(
                this, "repoListModel", RepositoryListModel.class);
        // The model for auth token.
        final AuthTokenModel authTokenModel = ViewModelStore.get((LifecycleProvider) getActivity(),
                "authTokenModel", AuthTokenModel.class);

        // Wire changes in search query to update the list of repositories
        mainSearchModel.getSearchQueryData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                // When the main search query changes, update the list model with that query
                // so that it starts loading new data.
                MainActivity mainActivity = (MainActivity) getActivity();
                repositoryListModel.setSearchTerm(getContext(), s,
                        authTokenModel.getAuthTokenLifecycle());
                // Also set a new adapter on our main recycler. This is simpler that updating
                // the existing adapter since the adapter registers itself as an observer on
                // the LiveData that wraps the list of repositories, and that field is set to
                // a new LiveData instance on new search query.
                recyclerView.setAdapter(new RepositoryListAdapter(RepositoryListFragment.this,
                        repositoryListModel));
            }
        });

        final int columnCount = getContext().getResources().getInteger(R.integer.column_count);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));

        // Wire changes in auth token to continue loading the list of repositories
        authTokenModel.getAuthTokenData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                if (!TextUtils.isEmpty(s)) {
                    repositoryListModel.resumeLoading();
                }
            }
        });

        return recyclerView;
    }
}
