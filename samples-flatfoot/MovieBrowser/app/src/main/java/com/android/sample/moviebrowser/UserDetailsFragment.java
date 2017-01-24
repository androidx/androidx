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
package com.android.sample.moviebrowser;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.sample.moviebrowser.adapter.RepositoryListAdapter;
import com.android.sample.moviebrowser.databinding.FragmentUserDetailsBinding;
import com.android.sample.moviebrowser.model.PersonDataModel;
import com.android.sample.moviebrowser.model.RepositoryListModel;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

import java.util.List;

/**
 * Fragment that shows details of a single user, including the list of their repositories.
 */
public class UserDetailsFragment extends LifecycleFragment {
    public static final String INITIAL = "userDetails.INITIAL";
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

        final ContributorData initialData = getArguments().getParcelable(INITIAL);
        mLogin = initialData.login;

        // Use the initial / partial data to populate as much info on this user as we can
        binding.setUserPartial(initialData);
        binding.setFragment(this);
        binding.executePendingBindings();

        // Load the list of repositories for this user (based on the login from the partial data)
        final RepositoryListModel repositoriesListModel = ViewModelStore.get(
                UserDetailsFragment.this, "repositoriesListModel", RepositoryListModel.class);
        if (!repositoriesListModel.hasSearchTerm()) {
            repositoriesListModel.setSearchTerm(initialData.login, null);
        }

        final RecyclerView repositoriesRecycler = (RecyclerView) result.findViewById(
                R.id.repositories);
        repositoriesRecycler.setAdapter(
                new RepositoryListAdapter(this, repositoriesListModel));
        final int columnCount = getContext().getResources().getInteger(R.integer.column_count);
        repositoriesRecycler.setLayoutManager(new GridLayoutManager(getContext(), columnCount));

        // Register an observer on the LiveData that wraps the list of repositories
        // to update the adapter on every change
        repositoriesListModel.getRepositoryListLiveData().observe(this,
                new Observer<List<RepositoryData>>() {
                    @Override
                    public void onChanged(
                            @Nullable List<RepositoryData> repositoryDataList) {
                        repositoriesRecycler.getAdapter().notifyDataSetChanged();
                    }
                });

        // Get our view model instance and register ourselves to observe change to the
        // full user data. When a change is reported, update all UI elements based on the new
        // data.
        mPersonDataModel = ViewModelStore.get(this, mLogin, PersonDataModel.class);
        mPersonDataModel.getPersonData().observe(this, new Observer<PersonData>() {
            @Override
            public void onChanged(@Nullable final PersonData personData) {
                if (personData != null) {
                    binding.setUser(personData);
                    binding.executePendingBindings();
                }
            }
        });

        // Ask the model to load the data for this user. When the data becomes available (either
        // immediately from the previous load or later on when it's fetched from remote API call),
        // we will be notified since this fragment registered itself as an observer on the matching
        // live data object.
        mPersonDataModel.loadData(getContext(), mLogin);

        return result;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the result matches the requested edit code, ask the view model to update itself
        // with the new data. As this fragment already registered itself as to observe changes
        // to the underlying data, we will update the UI as the side-result of the .update()
        // call.
        if ((requestCode == CODE_EDIT) && (resultCode == Activity.RESULT_OK)) {
            Snackbar.make(getView(), "Updating after edit", Snackbar.LENGTH_SHORT).show();
            mPersonDataModel.update(getContext(),
                    data.getStringExtra(EditUserDetailsFragment.KEY_EMAIL),
                    data.getStringExtra(EditUserDetailsFragment.KEY_LOCATION));
        }
    }
}
