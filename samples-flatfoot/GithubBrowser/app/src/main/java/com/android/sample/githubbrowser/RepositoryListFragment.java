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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.sample.githubbrowser.adapter.LoadMoreCallback;
import com.android.sample.githubbrowser.adapter.RepositoryListAdapter;
import com.android.sample.githubbrowser.data.RepositoryData;
import com.android.sample.githubbrowser.databinding.FragmentRepoListBinding;
import com.android.sample.githubbrowser.di.InjectableLifecycleProvider;
import com.android.sample.githubbrowser.di.LifecycleProviderComponent;
import com.android.sample.githubbrowser.model.AuthTokenModel;
import com.android.sample.githubbrowser.view.RepoClickCallback;
import com.android.sample.githubbrowser.viewmodel.RepositoryListModel;
import com.android.sample.githubbrowser.viewmodel.RepositorySearchModel;
import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

import java.util.List;

import javax.inject.Inject;

/**
 * Fragment that shows the list of all repositories that match the current search term.
 */
public class RepositoryListFragment extends BaseFragment implements
        InjectableLifecycleProvider {
    @Inject
    AuthTokenModel mAuthTokenModel;
    FragmentRepoListBinding mBinding;
    LifecycleProviderComponent mComponent;

    @Override
    public void inject(LifecycleProviderComponent component) {
        mComponent = component;
        component.inject(this);
    }
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_repo_list, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final RecyclerView recyclerView = mBinding.repoList;

        // Get all the models that are needed for this fragment

        // The model for our search query. Note that we are using the activity scope since
        // that is where the search box "lives"
        final RepositorySearchModel mainSearchModel = ViewModelStore.get(
                (LifecycleProvider) getActivity(), RepositorySearchModel.class);
        // The model for the list of repositories that are shown in this fragment.
        final RepositoryListModel repositoryListModel = ViewModelStore.get(
                this, RepositoryListModel.class);
        // The model for auth token.
        final RepositoryListAdapter adapter = new RepositoryListAdapter(mComponent,
                new RepoClickCallback() {
                    @Override
                    public void onClick(RepositoryData repositoryData) {
                        getNavigationController().openRepositoryDetailsFragment(repositoryData);
                    }
                },
                new LoadMoreCallback() {
                    @Override
                    public void loadMore(int currentSize) {
                        repositoryListModel.fetchAtIndexIfNecessary(currentSize);
                    }
                });
        recyclerView.setAdapter(adapter);

        // Wire changes in search query to update the list of repositories
        mainSearchModel.getSearchQueryData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                // When the main search query changes, update the list model with that query
                // so that it starts loading new data.
                repositoryListModel.setSearchTerm(s);
                mBinding.setQuery(s);
            }
        });

        repositoryListModel.getRepositoryListLiveData().observe(this,
                new Observer<List<RepositoryData>>() {
                    @Override
                    public void onChanged(@Nullable List<RepositoryData> repoList) {
                        adapter.setData(repoList);
                    }
                });

        repositoryListModel.getStateLiveData().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer state) {
                mBinding.setState(state);
            }
        });

        final int columnCount = getContext().getResources().getInteger(R.integer.column_count);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));

        // Wire changes in auth token to continue loading the list of repositories
        mAuthTokenModel.getAuthTokenData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                if (!TextUtils.isEmpty(s)) {
                    repositoryListModel.resumeLoading();
                }
            }
        });
    }
}
