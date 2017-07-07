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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.sample.githubbrowser.adapter.ContributorListAdapter;
import com.android.sample.githubbrowser.adapter.LoadMoreCallback;
import com.android.sample.githubbrowser.data.ContributorData;
import com.android.sample.githubbrowser.data.PersonData;
import com.android.sample.githubbrowser.data.RepositoryData;
import com.android.sample.githubbrowser.databinding.FragmentRepoDetailsBinding;
import com.android.sample.githubbrowser.di.InjectableLifecycleProvider;
import com.android.sample.githubbrowser.di.LifecycleProviderComponent;
import com.android.sample.githubbrowser.view.PersonClickCallback;
import com.android.sample.githubbrowser.viewmodel.ContributorListModel;
import com.android.sample.githubbrowser.viewmodel.RepositoryDataModel;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

import java.util.List;

/**
 * Fragment that shows details of a single repository, including the list of its contributors.
 */
public class RepositoryDetailsFragment extends BaseFragment implements
        InjectableLifecycleProvider {
    private static final String REPO_ID = "repoDetails.id";
    private static final String REPO_FULL_NAME = "repoDetails.fullName";
    private LifecycleProviderComponent mComponent;
    private FragmentRepoDetailsBinding mBinding;

    public RepositoryDetailsFragment() {
    }

    public static RepositoryDetailsFragment createFor(RepositoryData repo) {
        RepositoryDetailsFragment fragment = new RepositoryDetailsFragment();
        Bundle detailsFragmentArgs = new Bundle();
        detailsFragmentArgs.putString(RepositoryDetailsFragment.REPO_ID, repo.id);
        detailsFragmentArgs.putString(RepositoryDetailsFragment.REPO_FULL_NAME, repo.full_name);
        fragment.setArguments(detailsFragmentArgs);
        return fragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_repo_details, container, false, mComponent);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        RepositoryDataModel repositoryDataModel = ViewModelStore.get(this,
                RepositoryDataModel.class);
        final ContributorListModel contributorListModel = ViewModelStore.get(this,
                ContributorListModel.class);

        final String repoId = getArguments().getString(REPO_ID);
        final String repoFullName = getArguments().getString(REPO_FULL_NAME);
        setupRecyclerView(contributorListModel);
        // Ask the model to load the data for this repository. When the data becomes available
        // (either immediately from the previous load or later on when it's fetched from
        // remote API call), we will be notified since this fragment registered itself as an
        // observer on the matching live data object.
        repositoryDataModel.loadData(repoId, repoFullName);

        repositoryDataModel.getRepositoryData().observe(this, new Observer<RepositoryData>() {
            @Override
            public void onChanged(@Nullable final RepositoryData repositoryData) {
                if (repositoryData != null) {
                    // Bind the data on this fragment
                    mBinding.setRepo(repositoryData);
                    // TODO decompose this data
                    String[] split = repositoryData.full_name.split("/");
                    contributorListModel.setSearchTerms(split[0], repositoryData.name);
                } else {
                    contributorListModel.setSearchTerms(null, null);
                }
            }
        });
    }

    private PersonClickCallback mPersonClickCallback = new PersonClickCallback() {
        @Override
        public void onClick(PersonData person) {
            getNavigationController().openUserDetailsFragment(person);
        }
    };

    private void setupRecyclerView(final ContributorListModel contributorListModel) {
        final ContributorListAdapter adapter = new ContributorListAdapter(mComponent,
                mPersonClickCallback,
                new LoadMoreCallback() {
                    @Override
                    public void loadMore(int currentSize) {
                        contributorListModel.fetchAtIndexIfNecessary(currentSize);
                    }
                });
        contributorListModel.getContributorListLiveData().observe(this,
                new Observer<List<ContributorData>>() {
                    @Override
                    public void onChanged(@Nullable List<ContributorData> contributorList) {
                        adapter.setData(contributorList);
                    }
                });
        mBinding.contributors.setAdapter(adapter);
    }

    @Override
    public void inject(LifecycleProviderComponent component) {
        mComponent = component;
    }
}
