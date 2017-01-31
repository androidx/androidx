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
import com.android.sample.githubbrowser.adapter.RepositoryListAdapter.RepositoryBindingHolder;
import com.android.sample.githubbrowser.data.RepositoryData;
import com.android.sample.githubbrowser.databinding.RepositoryCardBinding;
import com.android.sample.githubbrowser.model.RepositoryListModel;

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

    private Fragment mFragment;
    private RepositoryListModel mSearchModel;

    /**
     * Creates an adapter.
     */
    public RepositoryListAdapter(Fragment fragment, RepositoryListModel searchModel) {
        mFragment = fragment;
        mSearchModel = searchModel;
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
                mSearchModel.getRepositoryListLiveData().getValue();
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

        mSearchModel.fetchAtIndexIfNecessary(repositoryDataList.size());
    }

    @Override
    public int getItemCount() {
        List<RepositoryData> repositoryDataList =
                mSearchModel.getRepositoryListLiveData().getValue();
        return (repositoryDataList == null) ? 0 : repositoryDataList.size();
    }
}
