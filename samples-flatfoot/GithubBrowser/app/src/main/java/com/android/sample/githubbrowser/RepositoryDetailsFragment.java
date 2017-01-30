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

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.sample.githubbrowser.adapter.ContibutorListAdapter;
import com.android.sample.githubbrowser.data.RepositoryData;
import com.android.sample.githubbrowser.databinding.FragmentRepoDetailsBinding;
import com.android.sample.githubbrowser.model.ContributorListModel;
import com.android.sample.githubbrowser.model.RepositoryDataModel;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

/**
 * Fragment that shows details of a single repository, including the list of its contributors.
 */
public class RepositoryDetailsFragment extends LifecycleFragment {
    public static final String REPO_ID = "repoDetails.id";
    public static final String REPO_FULL_NAME = "repoDetails.fullName";

    public RepositoryDetailsFragment() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final FragmentRepoDetailsBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_repo_details, container, false);
        final View result = binding.getRoot();

        final String repoId = getArguments().getString(REPO_ID);
        final String repoFullName = getArguments().getString(REPO_FULL_NAME);

        // Get our view model instance and register ourselves to observe change to the
        // full user data. When a change is reported, update all UI elements based on the new
        // data.
        RepositoryDataModel repositoryDataModel = ViewModelStore.get(this, repoId,
                RepositoryDataModel.class);
        repositoryDataModel.getRepositoryData().observe(this, new Observer<RepositoryData>() {
            @Override
            public void onChanged(@Nullable final RepositoryData repositoryData) {
                if (repositoryData != null) {
                    // Bind the data on this fragment
                    bindRepositoryData(binding, repositoryData);
                }
            }
        });

        // Ask the model to load the data for this repository. When the data becomes available
        // (either immediately from the previous load or later on when it's fetched from
        // remote API call), we will be notified since this fragment registered itself as an
        // observer on the matching live data object.
        repositoryDataModel.loadData(getContext(), repoId, repoFullName);

        return result;
    }

    private void bindRepositoryData(FragmentRepoDetailsBinding binding,
            RepositoryData repositoryData) {
        binding.setRepo(repositoryData);
        binding.setFragment(RepositoryDetailsFragment.this);
        binding.executePendingBindings();

        final ContributorListModel contributorListModel = ViewModelStore.get(this,
                "contributorListModel", ContributorListModel.class);
        if (!contributorListModel.hasSearchTerms()) {
            // TODO - this is temporary until Room persists non-primitive fields. Until
            // then we split full name into user and name manually
            String[] split = repositoryData.full_name.split("/");
            contributorListModel.setSearchTerms(split[0], repositoryData.name);
        }

        final RecyclerView contributorsRecycler = (RecyclerView) binding.getRoot().findViewById(
                R.id.contributors);
        contributorsRecycler.setAdapter(new ContibutorListAdapter(this, contributorListModel));
        contributorsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
    }
}
