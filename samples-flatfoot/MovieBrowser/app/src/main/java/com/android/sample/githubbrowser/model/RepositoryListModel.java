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
package com.android.sample.githubbrowser.model;

import android.support.annotation.MainThread;
import android.text.TextUtils;

import com.android.sample.githubbrowser.AuthTokenLifecycle;
import com.android.sample.githubbrowser.RepositoryData;
import com.android.sample.githubbrowser.network.GithubNetworkManager;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * View model for repository list data.
 */
public class RepositoryListModel implements ViewModel {
    private String mSearchTerm;
    private AuthTokenLifecycle mAuthTokenLifecycle;

    private LiveData<List<RepositoryData>> mRepositoryListLiveData = new LiveData<>();
    private AtomicBoolean mHasNoMoreDataToLoad = new AtomicBoolean(false);
    private AtomicInteger mLastRequestedPage = new AtomicInteger(0);
    private AtomicBoolean mHasRequestPending = new AtomicBoolean(false);
    private GithubNetworkManager.Cancelable mCurrentCall;

    /**
     * Returns true if the current search term is not empty.
     */
    public boolean hasSearchTerm() {
        return !TextUtils.isEmpty(mSearchTerm);
    }

    /**
     * Sets new search term.
     */
    @MainThread
    public void setSearchTerm(String searchTerm, AuthTokenLifecycle authTokenLifecycle) {
        mSearchTerm = searchTerm;
        mAuthTokenLifecycle = authTokenLifecycle;

        if (mCurrentCall != null) {
            mCurrentCall.cancel();
        }
        mRepositoryListLiveData.setValue(new ArrayList<RepositoryData>());
        mHasRequestPending.set(false);
        mLastRequestedPage.set(0);

        if ((mAuthTokenLifecycle != null) && mAuthTokenLifecycle.doWeNeedAuthToken()) {
            mAuthTokenLifecycle.getAuthToken();
        } else {
            fetchNextPage();
        }
    }

    private void fetchNextPage() {
        mHasRequestPending.set(true);
        mCurrentCall = GithubNetworkManager.getInstance().listRepositories(
                mSearchTerm, mLastRequestedPage.get() + 1,
                new GithubNetworkManager.NetworkCallListener<List<RepositoryData>>() {
                    @Override
                    public void onLoadEmpty(int httpCode) {
                        if ((httpCode == 401) || (httpCode == 403)) {
                            if (mAuthTokenLifecycle != null) {
                                mAuthTokenLifecycle.invalidateAuthToken();
                                mAuthTokenLifecycle.getAuthToken();
                            }
                        }
                    }

                    @Override
                    public void onLoadSuccess(List<RepositoryData> data) {
                        int newDataCount = data.size();
                        if (newDataCount == 0) {
                            mHasNoMoreDataToLoad.set(true);
                            return;
                        }

                        // Create a new list that will contain the previous pages and the new one
                        int prevDataCount = mRepositoryListLiveData.getValue().size();
                        ArrayList<RepositoryData> newList =
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
     * Resumes loading of data in this model.
     */
    public void resumeLoading() {
        fetchNextPage();
    }

    /**
     * Returns the {@LiveData} object that wraps the current list of repos that matches the last
     * set search term.
     */
    public LiveData<List<RepositoryData>> getRepositoryListLiveData() {
        return mRepositoryListLiveData;
    }
}
