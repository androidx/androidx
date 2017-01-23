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
package com.android.sample.moviebrowser.model;

import android.support.annotation.MainThread;
import android.text.TextUtils;

import com.android.sample.moviebrowser.ContributorData;
import com.android.sample.moviebrowser.network.GithubNetworkManager;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * View model for contributor list data.
 */
public class ContributorListModel implements ViewModel {
    private AuthTokenModel mAuthTokenModel;
    private String mOwner;
    private String mProject;

    private LiveData<List<ContributorData>> mRepositoryListLiveData = new LiveData<>();
    private AtomicBoolean mHasNoMoreDataToLoad = new AtomicBoolean(false);
    private AtomicInteger mLastRequestedPage = new AtomicInteger(0);
    private AtomicBoolean mHasRequestPending = new AtomicBoolean(false);
    private GithubNetworkManager.Cancelable mCurrentCall;

    /**
     * Returns true if the current search term is not empty.
     */
    public boolean hasSearchTerms() {
        return !TextUtils.isEmpty(mOwner) && !TextUtils.isEmpty(mProject);
    }

    /**
     * Sets new search terms.
     */
    @MainThread
    public void setSearchTerms(String owner, String project, AuthTokenModel authTokenModel) {
        mOwner = owner;
        mProject = project;
        mAuthTokenModel = authTokenModel;

        if (mCurrentCall != null) {
            mCurrentCall.cancel();
        }
        mRepositoryListLiveData.setValue(new ArrayList<ContributorData>());
        mHasRequestPending.set(false);
        mLastRequestedPage.set(0);

        fetchNextPage();
    }

    private void fetchNextPage() {
        mHasRequestPending.set(true);
        mCurrentCall = GithubNetworkManager.getInstance(mAuthTokenModel).getContributors(
                mOwner, mProject, mLastRequestedPage.get() + 1,
                new GithubNetworkManager.NetworkCallListener<List<ContributorData>>() {
                    @Override
                    public void onLoadEmpty(int httpCode) {
                    }

                    @Override
                    public void onLoadSuccess(List<ContributorData> data) {
                        int newDataCount = data.size();
                        if (newDataCount == 0) {
                            mHasNoMoreDataToLoad.set(true);
                            return;
                        }

                        // Create a new list that will contain the previous pages and the new one
                        int prevDataCount = mRepositoryListLiveData.getValue().size();
                        ArrayList<ContributorData> newList =
                                new ArrayList<>(prevDataCount + newDataCount);
                        newList.addAll(mRepositoryListLiveData.getValue());
                        newList.addAll(data);
                        // Set it on our LiveData object - our observer will update the adapter
                        mRepositoryListLiveData.setValue(newList);

                        mLastRequestedPage.incrementAndGet();
                        mHasRequestPending.set(false);
                    }

                    @Override
                    public void onLoadFailure() {
                    }
                });
    }

    /**
     * Fetches the next page of search data if necessary.
     */
    @MainThread
    public void fetchMoreIfNecessary() {
        if (mHasRequestPending.get() || mHasNoMoreDataToLoad.get()) {
            // Previous request still processing or no more results
            return;
        }

        fetchNextPage();
    }

    /**
     * Returns the {@LiveData} object that wraps the current list of contributors that matches the
     * last set search terms.
     */
    public LiveData<List<ContributorData>> getContributorListLiveData() {
        return mRepositoryListLiveData;
    }
}
