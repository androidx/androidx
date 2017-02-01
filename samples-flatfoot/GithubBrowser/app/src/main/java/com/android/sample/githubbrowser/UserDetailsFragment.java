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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.sample.githubbrowser.adapter.RepositoryListAdapter;
import com.android.sample.githubbrowser.data.PersonData;
import com.android.sample.githubbrowser.databinding.FragmentUserDetailsBinding;
import com.android.sample.githubbrowser.model.PersonDataModel;
import com.android.sample.githubbrowser.model.RepositoryListModel;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

/**
 * Fragment that shows details of a single user, including the list of their repositories.
 */
public class UserDetailsFragment extends LifecycleFragment {
    public static final String USER_LOGIN = "userDetails.login";
    public static final int CODE_EDIT = 1;

    private String mLogin;
    private PersonDataModel mPersonDataModel;

    public UserDetailsFragment() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final FragmentUserDetailsBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_user_details, container, false);
        final View result = binding.getRoot();

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
        mPersonDataModel.loadData(getContext(), mLogin, false);
        mPersonDataModel.getPersonData().observe(this, new Observer<PersonData>() {
            @Override
            public void onChanged(@Nullable final PersonData personData) {
                if (personData == null) {
                    return;
                }

                // Populate as much info on this user as we can
                binding.setUser(personData);
                binding.setFragment(UserDetailsFragment.this);
                binding.executePendingBindings();

                final RecyclerView repositoriesRecycler = (RecyclerView) result.findViewById(
                        R.id.repositories);
                if (repositoriesRecycler.getAdapter() == null) {
                    // Load the list of repositories for this user based on the passed login.
                    final RepositoryListModel repositoriesListModel = ViewModelStore.get(
                            UserDetailsFragment.this, "repositoriesListModel",
                            RepositoryListModel.class);
                    if (!repositoriesListModel.hasSearchTerm()) {
                        repositoriesListModel.setSearchTerm(getContext(), mLogin, null);
                    }

                    repositoriesRecycler.setAdapter(
                            new RepositoryListAdapter(UserDetailsFragment.this,
                                    repositoriesListModel));
                    final int columnCount = getContext().getResources().getInteger(
                            R.integer.column_count);
                    repositoriesRecycler.setLayoutManager(
                            new GridLayoutManager(getContext(), columnCount));
                }

                if (!Utils.isFullData(personData)) {
                    // If we only have partial data, initiate a full load.
                    mPersonDataModel.loadData(getContext(), mLogin, true);
                }
            }
        });

        return result;
    }
}
