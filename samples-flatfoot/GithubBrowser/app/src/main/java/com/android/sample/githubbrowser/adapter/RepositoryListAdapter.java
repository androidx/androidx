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
import com.android.sample.githubbrowser.adapter.RepositoryListAdapter.RepositoryBindingHolder;
import com.android.sample.githubbrowser.data.RepositoryData;
import com.android.sample.githubbrowser.databinding.RepositoryCardBinding;
import com.android.sample.githubbrowser.model.RepositoryListModel;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.Observer;

import java.util.List;

/**
 * Adapter for a list of repositories.
 */
public class RepositoryListAdapter extends RecyclerView.Adapter<RepositoryBindingHolder> {
    /**
     * Holder for the data cell.
     */
    public static class RepositoryBindingHolder extends RecyclerView.ViewHolder {
        private RepositoryCardBinding mViewDataBinding;

        public RepositoryBindingHolder(RepositoryCardBinding viewDataBinding) {
            super(viewDataBinding.getRoot());
            mViewDataBinding = viewDataBinding;
        }

        public RepositoryCardBinding getBinding() {
            return mViewDataBinding;
        }
    }

    private LifecycleFragment mFragment;
    private RepositoryListModel mListModel;
    private List<RepositoryData> mCurrList;

    /**
     * Creates an adapter.
     */
    public RepositoryListAdapter(LifecycleFragment fragment, RepositoryListModel listModel) {
        mFragment = fragment;
        mListModel = listModel;

        // Register an observer on the LiveData that wraps the list of repositories
        // to calculate the content diff and update ourselves.
        mListModel.getRepositoryListLiveData().observe(mFragment,
                new Observer<List<RepositoryData>>() {
                    @Override
                    public void onChanged(@Nullable final List<RepositoryData> repositoryDataList) {
                        DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                            @Override
                            public int getOldListSize() {
                                return (mCurrList == null) ? 0 : mCurrList.size();
                            }

                            @Override
                            public int getNewListSize() {
                                return repositoryDataList.size();
                            }

                            @Override
                            public boolean areItemsTheSame(int oldItemPosition,
                                    int newItemPosition) {
                                return mCurrList.get(oldItemPosition).id.equals(
                                        repositoryDataList.get(newItemPosition).id);
                            }

                            @Override
                            public boolean areContentsTheSame(int oldItemPosition,
                                    int newItemPosition) {
                                return mCurrList.get(oldItemPosition).equals(
                                        repositoryDataList.get(newItemPosition));
                            }
                        });
                        result.dispatchUpdatesTo(RepositoryListAdapter.this);
                        mCurrList = repositoryDataList;
                    }
                });
    }

    @Override
    public RepositoryBindingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RepositoryCardBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.repository_card, parent, false);
        return new RepositoryBindingHolder(binding);
    }

    @Override
    public void onBindViewHolder(RepositoryBindingHolder holder, final int position) {
        List<RepositoryData> repositoryDataList =
                mListModel.getRepositoryListLiveData().getValue();
        final RepositoryData data = repositoryDataList.get(position);

        // Use data binding for wiring the data and the click handler
        RepositoryCardBinding binding = holder.getBinding();
        binding.setRepo(data);
        binding.setFragment(mFragment);
        binding.executePendingBindings();

        // Do we need to request another page?
        if (position <= (repositoryDataList.size() - 2)) {
            // We are not close to the end of our data
            return;
        }

        mListModel.fetchAtIndexIfNecessary(repositoryDataList.size());
    }

    @Override
    public int getItemCount() {
        List<RepositoryData> repositoryDataList =
                mListModel.getRepositoryListLiveData().getValue();
        return (repositoryDataList == null) ? 0 : repositoryDataList.size();
    }
}
