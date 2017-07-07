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
package com.android.sample.githubbrowser.adapter;

import android.databinding.DataBindingComponent;
import android.databinding.DataBindingUtil;
import android.support.annotation.MainThread;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.DiffUtil.DiffResult;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.android.sample.githubbrowser.R;
import com.android.sample.githubbrowser.adapter.RepositoryListAdapter.RepositoryBindingHolder;
import com.android.sample.githubbrowser.data.RepositoryData;
import com.android.sample.githubbrowser.databinding.RepositoryCardBinding;
import com.android.sample.githubbrowser.view.RepoClickCallback;

import java.util.Collections;
import java.util.List;

/**
 * Adapter for a list of repositories.
 */
public class RepositoryListAdapter extends RecyclerView.Adapter<RepositoryBindingHolder> {
    /**
     * Holder for the data cell.
     */
    static class RepositoryBindingHolder extends RecyclerView.ViewHolder {
        private RepositoryCardBinding mViewDataBinding;

        RepositoryBindingHolder(RepositoryCardBinding viewDataBinding) {
            super(viewDataBinding.getRoot());
            mViewDataBinding = viewDataBinding;
        }

        public RepositoryCardBinding getBinding() {
            return mViewDataBinding;
        }
    }

    private List<RepositoryData> mCurrList;
    private DataBindingComponent mComponent;
    private RepoClickCallback mRepoClickCallback;
    private LoadMoreCallback mLoadMoreCallback;

    /**
     * Creates an adapter.
     */
    public RepositoryListAdapter(android.databinding.DataBindingComponent component,
            RepoClickCallback callback, LoadMoreCallback loadMoreCallback) {
        mComponent = component;
        mRepoClickCallback = callback;
        mLoadMoreCallback = loadMoreCallback;
    }

    @MainThread
    public void setData(final List<RepositoryData> newList) {
        if (newList == null) {
            setData(Collections.<RepositoryData>emptyList());
            return;
        }
        if (mCurrList == null) {
            mCurrList = newList;
            notifyItemRangeInserted(0, newList.size());
        } else {
            DiffResult result = DiffUtil.calculateDiff(
                    new DiffUtilListCallback<RepositoryData, String>(mCurrList, newList) {
                        @Override
                        String getId(RepositoryData item) {
                            return item.id;
                        }
                    });
            result.dispatchUpdatesTo(RepositoryListAdapter.this);
            mCurrList = newList;
        }
    }

    @Override
    public RepositoryBindingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RepositoryCardBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.repository_card, parent, false, mComponent);
        binding.setRepoClickCallback(mRepoClickCallback);
        return new RepositoryBindingHolder(binding);
    }

    @Override
    public void onBindViewHolder(RepositoryBindingHolder holder, final int position) {
        final RepositoryData data = mCurrList.get(position);

        // Use data binding for wiring the data and the click handler
        RepositoryCardBinding binding = holder.getBinding();
        binding.setRepo(data);
        binding.executePendingBindings();

        // Do we need to request another page?
        if (position > (mCurrList.size() - 2)) {
            mLoadMoreCallback.loadMore(mCurrList.size());
        }
    }

    @Override
    public int getItemCount() {
        return mCurrList == null ? 0 : mCurrList.size();
    }
}
