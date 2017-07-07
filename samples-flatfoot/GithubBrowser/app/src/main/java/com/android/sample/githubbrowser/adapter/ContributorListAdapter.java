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

import android.databinding.DataBindingUtil;
import android.support.annotation.MainThread;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.DiffUtil.DiffResult;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.android.sample.githubbrowser.R;
import com.android.sample.githubbrowser.adapter.ContributorListAdapter.ContributorBindingHolder;
import com.android.sample.githubbrowser.data.ContributorData;
import com.android.sample.githubbrowser.databinding.UserRowBinding;
import com.android.sample.githubbrowser.di.LifecycleProviderComponent;
import com.android.sample.githubbrowser.view.PersonClickCallback;

import java.util.Collections;
import java.util.List;

/**
 * Adapter for the list of contributors.
 */
public class ContributorListAdapter extends RecyclerView.Adapter<ContributorBindingHolder> {
    /**
     * Holder for the data cell.
     */
    static class ContributorBindingHolder extends RecyclerView.ViewHolder {
        private UserRowBinding mViewDataBinding;

        ContributorBindingHolder(UserRowBinding viewDataBinding) {
            super(viewDataBinding.getRoot());
            mViewDataBinding = viewDataBinding;
        }

        public UserRowBinding getBinding() {
            return mViewDataBinding;
        }
    }

    private List<ContributorData> mCurrList;
    private PersonClickCallback mPersonClickCallback;
    private LoadMoreCallback mLoadMoreCallback;
    private LifecycleProviderComponent mComponent;

    public ContributorListAdapter(LifecycleProviderComponent component,
            PersonClickCallback personClickCallback, LoadMoreCallback loadMoreCallback) {
        mComponent = component;
        mPersonClickCallback = personClickCallback;
        mLoadMoreCallback = loadMoreCallback;
    }

    @MainThread
    public void setData(final List<ContributorData> newList) {
        if (newList == null) {
            setData(Collections.<ContributorData>emptyList());
            return;
        }
        if (mCurrList == null) {
            mCurrList = newList;
            notifyItemRangeInserted(0, newList.size());
        } else {
            DiffResult result = DiffUtil.calculateDiff(
                new DiffUtilListCallback<ContributorData, String>(mCurrList, newList) {
                    @Override
                    String getId(ContributorData item) {
                        return item.id;
                    }
                });
            result.dispatchUpdatesTo(ContributorListAdapter.this);
            mCurrList = newList;
        }
    }

    @Override
    public ContributorBindingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        UserRowBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.user_row, parent, false, mComponent);
        binding.setCallback(mPersonClickCallback);
        return new ContributorBindingHolder(binding);
    }

    @Override
    public void onBindViewHolder(ContributorBindingHolder holder, final int position) {
        final ContributorData data = mCurrList.get(position);

        // Use data binding for wiring the data and the click handler
        UserRowBinding binding = holder.getBinding();
        binding.setContributor(data);
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
