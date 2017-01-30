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
package com.android.sample.githubbrowser.adapter;

import android.databinding.DataBindingUtil;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.android.sample.githubbrowser.R;
import com.android.sample.githubbrowser.adapter.ContibutorListAdapter.ContributorBindingHolder;
import com.android.sample.githubbrowser.data.ContributorData;
import com.android.sample.githubbrowser.databinding.UserRowBinding;
import com.android.sample.githubbrowser.model.ContributorListModel;

import java.util.List;

/**
 * Adapter for the list of contributors.
 */
public class ContibutorListAdapter extends RecyclerView.Adapter<ContributorBindingHolder> {
    /**
     * Holder for the data cell.
     */
    public static class ContributorBindingHolder extends RecyclerView.ViewHolder {
        private UserRowBinding mViewDataBinding;

        public ContributorBindingHolder(UserRowBinding viewDataBinding) {
            super(viewDataBinding.getRoot());
            mViewDataBinding = viewDataBinding;
        }

        public UserRowBinding getBinding() {
            return mViewDataBinding;
        }
    }

    private Fragment mFragment;
    private ContributorListModel mSearchModel;

    /**
     * Creates an adapter.
     */
    public ContibutorListAdapter(Fragment fragment, ContributorListModel searchModel) {
        mFragment = fragment;
        mSearchModel = searchModel;
    }

    @Override
    public ContributorBindingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        UserRowBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.user_row, parent, false);
        return new ContributorBindingHolder(binding);
    }

    @Override
    public void onBindViewHolder(ContributorBindingHolder holder, final int position) {
        List<ContributorData> contributorDataList =
                mSearchModel.getContributorListLiveData().getValue();
        final ContributorData data = contributorDataList.get(position);

        // Use data binding for wiring the data and the click handler
        UserRowBinding binding = holder.getBinding();
        binding.setContributor(data);
        binding.setFragment(mFragment);
        binding.executePendingBindings();

        // Do we need to request another page?
        if (position <= (contributorDataList.size() - 2)) {
            // We are not close to the end of our data
            return;
        }

        mSearchModel.fetchMoreIfNecessary();
    }

    @Override
    public int getItemCount() {
        return mSearchModel.getContributorListLiveData().getValue().size();
    }
}
