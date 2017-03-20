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

import com.android.sample.githubbrowser.data.ContributorData;
import com.android.sample.githubbrowser.data.ContributorSearchData;
import com.android.sample.githubbrowser.data.SearchQueryData;
import com.android.sample.githubbrowser.db.GithubDao;
import com.android.sample.githubbrowser.db.GithubDatabase;
import com.android.sample.githubbrowser.di.AppComponent;
import com.android.sample.githubbrowser.network.GithubNetworkManager;
import com.android.sample.githubbrowser.util.ChainedLiveData;
import com.android.support.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

/**
 * View model for contributor list data.
 */
public class ContributorListModel extends InjectableViewModel {
    private String mOwner;
    private String mProject;

    private final ChainedLiveData<List<ContributorData>> mContributorListLiveData
            = new ChainedLiveData<>();
    private AtomicBoolean mHasNetworkRequestPending = new AtomicBoolean(false);
    private GithubNetworkManager.Cancelable mCurrentNetworkCall;

    private SearchQueryData mSearchQueryData;
    private AtomicInteger mLastRequestedIndex = new AtomicInteger(0);

    @Inject
    GithubNetworkManager mGithubNetworkManager;
    @Inject
    GithubDatabase mDatabase;

    @Override
    void inject(AppComponent appComponent) {
        appComponent.inject(this);
    }

    /**
     * Sets new search terms.
     */
    @MainThread
    public void setSearchTerms(String owner, String project) {
        mOwner = owner;
        mProject = project;

        if (mCurrentNetworkCall != null) {
            mCurrentNetworkCall.cancel();
        }
        if (mOwner == null || mProject == null) {
            mContributorListLiveData.setBackingLiveData(null);
            return;
        }

        final GithubDao githubDao = mDatabase.getGithubDao();

        // Get the LiveData wrapper around the list of contributors that match our current
        // search query. The wrapped list will be updated on every successful network request
        // that is performed for data that is not available in our database.
        mContributorListLiveData
                .setBackingLiveData(githubDao.getContributors(mOwner + "/" + mProject));

        mHasNetworkRequestPending.set(false);

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                // Get data about locally persisted results of our current search query. Note that
                // since this is working with a disk-based database, we're running off the main
                // thread.
                mSearchQueryData = githubDao.getSearchQueryData(
                        params[0], SearchQueryData.REPOSITORY_CONTRIBUTORS);
                if (mSearchQueryData == null) {
                    // This query has not been performed before - initialize an entry in the
                    // database. TODO - consult the timestamp of network requests for staleness.
                    mSearchQueryData = new SearchQueryData();
                    mSearchQueryData.searchQuery = params[0];
                    mSearchQueryData.searchKind = SearchQueryData.REPOSITORY_CONTRIBUTORS;
                    mSearchQueryData.numberOfFetchedItems = -1;
                    githubDao.update(mSearchQueryData);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                fetchNextPage();
            }
        }.execute(mOwner + "/" + mProject);
    }

    private void fetchNextPage() {
        if (mSearchQueryData == null) {
            // Not ready to fetch yet.
            return;
        }

        // Do we have data in the database?
        if (mSearchQueryData.numberOfFetchedItems >= mLastRequestedIndex.get()) {
            // We already have the data stored (and retrieved) from database.
            return;
        }

        if (mHasNetworkRequestPending.get() || mSearchQueryData.hasNoMoreData) {
            // Previous request still processing or no more results
            return;
        }

        mHasNetworkRequestPending.set(true);
        mCurrentNetworkCall = mGithubNetworkManager.getContributors(
                mOwner, mProject, mSearchQueryData.indexOfLastFetchedPage + 1,
                new GithubNetworkManager.NetworkCallListener<List<ContributorData>>() {
                    @Override
                    public void onLoadEmpty(int httpCode) {
                    }

                    @Override
                    public void onLoadSuccess(List<ContributorData> data) {
                        new AsyncTask<ContributorData, Void, Void>() {
                            @Override
                            protected Void doInBackground(ContributorData... params) {
                                // Note that since we're going to be inserting data into disk-based
                                // database, we need to be running off the main thread.
                                processNewPageOfData(params);
                                return null;
                            }
                        }.execute(data.toArray(new ContributorData[data.size()]));
                    }

                    @Override
                    public void onLoadFailure() {
                    }
                });
    }

    @WorkerThread
    private void processNewPageOfData(ContributorData... data) {
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
                // Insert entries for the newly loaded contributors in two places:
                // 1. The table that stores contributor IDs that match a specific query.
                // 2. The table that stores full data on each individual contributor.
                // This way we don't store multiple full entries for the same contributor
                // that happens to match two or more search queries.
                ContributorSearchData[] contributorSearchDataArray =
                        new ContributorSearchData[newDataCount];
                for (int i = 0; i < newDataCount; i++) {
                    contributorSearchDataArray[i] = new ContributorSearchData();
                    contributorSearchDataArray[i].searchQuery = mOwner + "/" + mProject;
                    contributorSearchDataArray[i].resultIndex = indexOfFirstData + i;
                    contributorSearchDataArray[i].contributorId = data[i].id;
                    contributorSearchDataArray[i].contributions = data[i].contributions;
                }
                githubDao.insert(contributorSearchDataArray);
                githubDao.insert(data);
            }
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }

        mHasNetworkRequestPending.set(false);
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
     * Returns the {@link LiveData} object that wraps the current list of contributors that matches
     * the last set search terms.
     */
    public LiveData<List<ContributorData>> getContributorListLiveData() {
        return mContributorListLiveData;
    }
}
