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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.sample.githubbrowser.adapter.LoadMoreCallback;
import com.android.sample.githubbrowser.adapter.RepositoryListAdapter;
import com.android.sample.githubbrowser.data.PersonData;
import com.android.sample.githubbrowser.data.RepositoryData;
import com.android.sample.githubbrowser.databinding.FragmentUserDetailsBinding;
import com.android.sample.githubbrowser.di.InjectableLifecycleProvider;
import com.android.sample.githubbrowser.di.LifecycleProviderComponent;
import com.android.sample.githubbrowser.view.PersonClickCallback;
import com.android.sample.githubbrowser.view.RepoClickCallback;
import com.android.sample.githubbrowser.viewmodel.PersonDataModel;
import com.android.sample.githubbrowser.viewmodel.RepositoryListModel;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

import java.util.List;

/**
 * Fragment that shows details of a single user, including the list of their repositories.
 */
public class UserDetailsFragment extends BaseFragment implements InjectableLifecycleProvider {
    private static final String USER_LOGIN = "userDetails.login";

    private String mLogin;
    private PersonDataModel mPersonDataModel;
    private LifecycleProviderComponent mComponent;

    public UserDetailsFragment() {
    }

    @Override
    public void inject(LifecycleProviderComponent component) {
        mComponent = component;
    }

    public static UserDetailsFragment createFor(PersonData person) {
        UserDetailsFragment fragment = new UserDetailsFragment();
        Bundle detailsFragmentArgs = new Bundle();
        detailsFragmentArgs.putString(UserDetailsFragment.USER_LOGIN, person.login);
        fragment.setArguments(detailsFragmentArgs);
        return fragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final FragmentUserDetailsBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_user_details, container, false, mComponent);

        mLogin = getArguments().getString(USER_LOGIN);

        // Get our view model instance and register ourselves to observe change to the
        // full user data. When a change is reported, update all UI elements based on the new
        // data.
        mPersonDataModel = ViewModelStore.get(this, mLogin, PersonDataModel.class);
        // Ask the model to load the data for this user. When the data becomes available (either
        // immediately from the previous load or later on when it's fetched from remote API call),
        // we will be notified since this fragment registered itself as an observer on the matching
        // live data object.
        // Note that the last parameter specifies that we're fine with getting partial data as
        // quickly as possible.
        mPersonDataModel.loadData(mLogin, false);
        binding.setEditCallback(new PersonClickCallback() {
            @Override
            public void onClick(PersonData user) {
                if (user == null) {
                    return;
                }
                getNavigationController().openEditUserDetailsFragment(UserDetailsFragment.this,
                        user);
            }
        });
        mPersonDataModel.getPersonData().observe(this, new Observer<PersonData>() {
            @Override
            public void onChanged(@Nullable final PersonData personData) {
                if (personData == null) {
                    return;
                }

                // Populate as much info on this user as we can
                binding.setUser(personData);
                binding.executePendingBindings();

                if (!personData.isFullData()) {
                    // If we only have partial data, initiate a full load.
                    mPersonDataModel.loadData(mLogin, true);
                }
            }
        });

        // Load the list of repositories for this user based on the passed login.
        final RepositoryListModel repositoriesListModel = ViewModelStore.get(this,
                RepositoryListModel.class);
        repositoriesListModel.setSearchTerm(mLogin);

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
                        repositoriesListModel.fetchAtIndexIfNecessary(currentSize);
                    }
                });
        binding.repositories.setAdapter(adapter);
        repositoriesListModel.getRepositoryListLiveData().observe(this,
                new Observer<List<RepositoryData>>() {
                    @Override
                    public void onChanged(@Nullable List<RepositoryData> data) {
                        adapter.setData(data);
                    }
                });
        final int columnCount = getContext().getResources().getInteger(
                R.integer.column_count);
        binding.repositories.setLayoutManager(new GridLayoutManager(getContext(), columnCount));

        return binding.getRoot();
    }
}
