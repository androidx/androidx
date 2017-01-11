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

import com.android.sample.moviebrowser.MovieData;
import com.android.sample.moviebrowser.SearchData;
import com.android.sample.moviebrowser.network.NetworkManager;
import com.android.sample.moviebrowser.network.NetworkManager.Cancelable;
import com.android.sample.moviebrowser.network.NetworkManager.NetworkCallListener;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * View model for search data.
 */
public class SearchModel implements ViewModel {
    private String mSearchTerm;
    private LiveData<List<MovieData>> mMovieListLiveData = new LiveData<>();
    private int mTotalSearchResults;
    private AtomicInteger mLastRequestedPage = new AtomicInteger(0);
    private AtomicBoolean mHasRequestPending = new AtomicBoolean(false);
    private Cancelable mCurrentCall;

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
        if (mCurrentCall != null) {
            mCurrentCall.cancel();
        }
        mMovieListLiveData.setValue(new ArrayList<MovieData>());
        mHasRequestPending.set(false);
        mLastRequestedPage.set(0);

        fetchNextPage();
    }

    private void fetchNextPage() {
        mHasRequestPending.set(true);
        mCurrentCall = NetworkManager.getInstance().fetchSearchResults(mSearchTerm,
                mLastRequestedPage.get() + 1,
                new NetworkCallListener<SearchData>() {
                    @Override
                    public void onLoadSuccess(SearchData data) {
                        // Get the list of movies in this page
                        List<MovieData> newData = data.Search;
                        int newDataCount = newData.size();

                        // Create a new list that will contain the previous pages and the new one
                        int prevDataCount = mMovieListLiveData.getValue().size();
                        ArrayList<MovieData> newList =
                                new ArrayList<>(prevDataCount + newDataCount);
                        newList.addAll(mMovieListLiveData.getValue());
                        newList.addAll(newData);
                        // Set it on our LiveData object - our observer will update the adapter
                        mMovieListLiveData.setValue(newList);

                        mTotalSearchResults = data.totalResults;
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
        if (mHasRequestPending.get()
                || (mMovieListLiveData.getValue().size() == mTotalSearchResults)) {
            // Previous request still processing or no more results
            return;
        }

        if (mLastRequestedPage.get() > 100) {
            // backend only supports fetching up to 100 pages
            return;
        }

        fetchNextPage();

    }

    /**
     * Returns the {@LiveData} object that wraps the current list of movies that matches the last
     * set search term.
     */
    public LiveData<List<MovieData>> getMovieListLiveData() {
        return mMovieListLiveData;
    }
}
