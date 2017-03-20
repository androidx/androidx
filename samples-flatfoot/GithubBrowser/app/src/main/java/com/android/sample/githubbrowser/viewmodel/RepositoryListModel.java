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
package com.android.sample.githubbrowser.viewmodel;

import android.os.AsyncTask;
import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.android.sample.githubbrowser.data.GeneralRepoSearchData;
import com.android.sample.githubbrowser.data.RepositoryData;
import com.android.sample.githubbrowser.data.SearchQueryData;
import com.android.sample.githubbrowser.db.GithubDao;
import com.android.sample.githubbrowser.db.GithubDatabase;
import com.android.sample.githubbrowser.di.AppComponent;
import com.android.sample.githubbrowser.model.AuthTokenModel;
import com.android.sample.githubbrowser.network.GithubNetworkManager;
import com.android.sample.githubbrowser.util.ChainedLiveData;
import com.android.sample.githubbrowser.viewmodel.InjectableViewModel;
import com.android.support.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

/**
 * View model for repository list data.
 */
public class RepositoryListModel extends InjectableViewModel {
    /** Constant for the initial loading state. */
    public static final int STATE_INITIAL_LOADING = 0;
    /** Constant for the empty / no data state. */
    public static final int STATE_EMPTY = 1;
    /** Constant for the data state. */
    public static final int STATE_DATA = 2;
    /** Constant for the error state. */
    public static final int STATE_ERROR = 3;

    private String mSearchTerm;

    private final ChainedLiveData<List<RepositoryData>> mRepositoryListLiveData
            = new ChainedLiveData<>();
    private final LiveData<Integer> mStateLiveData = new LiveData<>();
    private AtomicBoolean mHasNetworkRequestPending = new AtomicBoolean(false);
    private GithubNetworkManager.Cancelable mCurrentNetworkCall;

    private SearchQueryData mSearchQueryData;
    private AtomicInteger mLastRequestedIndex = new AtomicInteger(0);

    @Inject
    GithubNetworkManager mGithubNetworkManager;
    @Inject
    GithubDatabase mDatabase;
    @Inject
    AuthTokenModel mAuthTokenModel;

    @Override
    void inject(AppComponent appComponent) {
        appComponent.inject(this);
    }

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
    public void setSearchTerm(String searchTerm) {
        mSearchTerm = searchTerm;

        if (mCurrentNetworkCall != null) {
            mCurrentNetworkCall.cancel();
        }

        final GithubDao githubDao = mDatabase.getGithubDao();

        // Get the LiveData wrapper around the list of repositories that match our current
        // search query. The wrapped list will be updated on every successful network request
        // that is performed for data that is not available in our database.
        mRepositoryListLiveData.setBackingLiveData(githubDao.getRepositories(mSearchTerm));

        mStateLiveData.setValue(STATE_INITIAL_LOADING);
        mHasNetworkRequestPending.set(false);

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                // Get data about locally persisted results of our current search query. Note that
                // since this is working with a disk-based database, we're running off the main
                // thread.
                mSearchQueryData = githubDao.getSearchQueryData(
                        params[0], SearchQueryData.GENERAL_REPOSITORIES);
                if (mSearchQueryData == null) {
                    // This query has not been performed before - initialize an entry in the
                    // database. TODO - consult the timestamp of network requests for staleness.
                    mSearchQueryData = new SearchQueryData();
                    mSearchQueryData.searchQuery = params[0];
                    mSearchQueryData.searchKind = SearchQueryData.GENERAL_REPOSITORIES;
                    mSearchQueryData.numberOfFetchedItems = -1;
                    githubDao.update(mSearchQueryData);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                fetchNextPage();
            }
        }.execute(mSearchTerm);
    }

    private void fetchNextPage() {
        if (mSearchQueryData == null) {
            // Not ready to fetch yet.
            return;
        }

        // Do we have data in the database?
        if (mSearchQueryData.numberOfFetchedItems >= mLastRequestedIndex.get()) {
            // We already have the data stored (and retrieved) from database.
            mStateLiveData.setValue(STATE_DATA);
            return;
        }

        if (mHasNetworkRequestPending.get()) {
            // Previous request still processing
            return;
        }

        if (mSearchQueryData.hasNoMoreData) {
            // We don't have any more results
            if (mSearchQueryData.numberOfFetchedItems <= 0) {
                mStateLiveData.setValue(STATE_EMPTY);
            }
            return;
        }

        mHasNetworkRequestPending.set(true);
        mCurrentNetworkCall = mGithubNetworkManager.listRepositories(
                mSearchTerm, mSearchQueryData.indexOfLastFetchedPage + 1,
                new GithubNetworkManager.NetworkCallListener<List<RepositoryData>>() {
                    @Override
                    public void onLoadEmpty(int httpCode) {
                        switch (httpCode) {
                            case 404:
                                // No such user
                                mStateLiveData.setValue(STATE_EMPTY);
                                break;
                            default:
                                mStateLiveData.setValue(STATE_ERROR);
                        }
                    }

                    @Override
                    public void onLoadSuccess(List<RepositoryData> data) {
                        new AsyncTask<RepositoryData, Void, Void>() {
                            @Override
                            protected Void doInBackground(RepositoryData... params) {
                                // Note that since we're going to be inserting data into disk-based
                                // database, we need to be running off the main thread.
                                processNewPageOfData(params);
                                return null;
                            }
                        }.execute(data.toArray(new RepositoryData[data.size()]));
                    }

                    @Override
                    public void onLoadFailure() {
                        mStateLiveData.setValue(STATE_ERROR);
                    }
                });
    }

    @WorkerThread
    private void processNewPageOfData(RepositoryData... data) {
        try {
            mDatabase.beginTransaction();
            int newDataCount = data.length;

            final GithubDao githubDao = mDatabase.getGithubDao();
            final int indexOfFirstData = mSearchQueryData.numberOfFetchedItems;
            // Update the metadata about our current search query (in the database)
            if (newDataCount == 0) {
                mSearchQueryData.hasNoMoreData = true;
            } else {
                if (mSearchQueryData.indexOfLastFetchedPage == 0) {
                    mSearchQueryData.timestamp = System.currentTimeMillis();
                }
                mSearchQueryData.indexOfLastFetchedPage++;
                mSearchQueryData.numberOfFetchedItems += newDataCount;
            }
            githubDao.update(mSearchQueryData);

            if (newDataCount > 0) {
                // Insert entries for the newly loaded repositories in two places:
                // 1. The table that stores repository IDs that match a specific query.
                // 2. The table that stores full data on each individual repository.
                // This way we don't store multiple full entries for the same repository
                // that happens to match two or more search queries.
                GeneralRepoSearchData[] generalRepoSearchDataArray =
                        new GeneralRepoSearchData[newDataCount];
                for (int i = 0; i < newDataCount; i++) {
                    generalRepoSearchDataArray[i] = new GeneralRepoSearchData();
                    generalRepoSearchDataArray[i].searchQuery = mSearchTerm;
                    generalRepoSearchDataArray[i].resultIndex = indexOfFirstData + i;
                    generalRepoSearchDataArray[i].repoId = data[i].id;
                }
                githubDao.insert(generalRepoSearchDataArray);
                githubDao.insert(data);
            }
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }

        mHasNetworkRequestPending.set(false);
        mStateLiveData.postValue(
                (mSearchQueryData.numberOfFetchedItems <= 0) && mSearchQueryData.hasNoMoreData
                    ? STATE_EMPTY : STATE_DATA);
    }

    /**
     * Fetches data at specified index if data does not exist yet.
     */
    public void fetchAtIndexIfNecessary(int index) {
        if (mSearchQueryData == null) {
            // If we're here, we've been asked to start fetching items before we've retrieved
            // the top-level metadata for our search. Save the requested index and return. Once
            // that metadata is fetched off the main thread in the AsyncTask executed in
            // setSearchTerms, we'll call fetchNextPage().
            mLastRequestedIndex.set(index);
            return;
        }

        if (mHasNetworkRequestPending.get() || mSearchQueryData.hasNoMoreData) {
            // Previous request still processing or no more results
            return;
        }

        mLastRequestedIndex.set(index);

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

    /**
     * Returns the {@LiveData} object that wraps the current data state.
     */
    public LiveData<Integer> getStateLiveData() {
        return mStateLiveData;
    }
}
