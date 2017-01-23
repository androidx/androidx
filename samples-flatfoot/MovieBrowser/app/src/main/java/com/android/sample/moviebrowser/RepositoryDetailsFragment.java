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

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.sample.moviebrowser.adapter.ContibutorListAdapter;
import com.android.sample.moviebrowser.databinding.FragmentRepoDetailsBinding;
import com.android.sample.moviebrowser.model.AuthTokenModel;
import com.android.sample.moviebrowser.model.ContributorListModel;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

import java.util.List;

/**
 * Fragment that shows details of a single repository, including the list of its contributors.
 */
public class RepositoryDetailsFragment extends LifecycleFragment {
    public static final String KEY_REPO = "repoDetails.full";

    private RepositoryData mRepositoryData;

    public RepositoryDetailsFragment() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final FragmentRepoDetailsBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_repo_details, container, false);
        final View result = binding.getRoot();

        mRepositoryData = (savedInstanceState == null)
                ? (RepositoryData) getArguments().getParcelable(KEY_REPO)
                : (RepositoryData) savedInstanceState.getParcelable(KEY_REPO);

        binding.setRepo(mRepositoryData);
        binding.setFragment(this);
        binding.executePendingBindings();

        AuthTokenModel authTokenModel = ViewModelStore.get(
                (LifecycleProvider) getActivity(), "authTokenModel",
                AuthTokenModel.class);
        final ContributorListModel contributorListModel = ViewModelStore.get(this,
                "contributorListModel", ContributorListModel.class);
        if (!contributorListModel.hasSearchTerms()) {
            contributorListModel.setSearchTerms(mRepositoryData.owner.login, mRepositoryData.name,
                    authTokenModel);
        }

        final RecyclerView contributorsRecycler = (RecyclerView) result.findViewById(
                R.id.contributors);
        contributorsRecycler.setAdapter(new ContibutorListAdapter(this, contributorListModel));
        contributorsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        // Register an observer on the LiveData that wraps the list of contributors to update the
        // adapter on every change
        contributorListModel.getContributorListLiveData().observe(this,
                new Observer<List<ContributorData>>() {
                    @Override
                    public void onChanged(@Nullable List<ContributorData> contributorDataList) {
                        contributorsRecycler.getAdapter().notifyDataSetChanged();
                    }
                });

        return result;
    }
}
