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
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.DiffUtil.DiffResult;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.android.sample.githubbrowser.R;
import com.android.sample.githubbrowser.adapter.ContibutorListAdapter.ContributorBindingHolder;
import com.android.sample.githubbrowser.data.ContributorData;
import com.android.sample.githubbrowser.databinding.UserRowBinding;
import com.android.sample.githubbrowser.model.ContributorListModel;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.Observer;

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

    private LifecycleFragment mFragment;
    private ContributorListModel mSearchModel;
    private List<ContributorData> mCurrList;

    /**
     * Creates an adapter.
     */
    public ContibutorListAdapter(LifecycleFragment fragment, ContributorListModel searchModel) {
        mFragment = fragment;
        mSearchModel = searchModel;

        // Register an observer on the LiveData that wraps the list of contributors to update
        // ourselves on every change
        mSearchModel.getContributorListLiveData().observe(fragment,
                new Observer<List<ContributorData>>() {
                    @Override
                    public void onChanged(
                            @Nullable final List<ContributorData> contributorDataList) {
                        DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                            @Override
                            public int getOldListSize() {
                                return (mCurrList == null) ? 0 : mCurrList.size();
                            }

                            @Override
                            public int getNewListSize() {
                                return contributorDataList.size();
                            }

                            @Override
                            public boolean areItemsTheSame(int oldItemPosition,
                                    int newItemPosition) {
                                return mCurrList.get(oldItemPosition).id.equals(
                                        contributorDataList.get(newItemPosition).id);
                            }

                            @Override
                            public boolean areContentsTheSame(int oldItemPosition,
                                    int newItemPosition) {
                                return mCurrList.get(oldItemPosition).equals(
                                        contributorDataList.get(newItemPosition));
                            }
                        });
                        result.dispatchUpdatesTo(ContibutorListAdapter.this);
                        mCurrList = contributorDataList;
                    }
                });
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
