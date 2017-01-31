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

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.MainThread;

import com.android.sample.githubbrowser.data.RepositoryData;
import com.android.sample.githubbrowser.db.PersonDataDatabase;
import com.android.sample.githubbrowser.db.PersonDataDatabaseHelper;
import com.android.sample.githubbrowser.network.GithubNetworkManager;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

/**
 * View model for the full repository data.
 */
public class RepositoryDataModel implements ViewModel {
    private LiveData<Boolean> mFetching = new LiveData<>();
    private LiveData<RepositoryData> mRepositoryData = new LiveData<>();

    /**
     * Returns the {@LiveData} object that wraps the full repository data.
     */
    public LiveData<RepositoryData> getRepositoryData() {
        return mRepositoryData;
    }

    /**
     * Sets the information for fetching the full repository data.
     */
    @MainThread
    public synchronized void loadData(final Context context, final String id,
            final String fullName) {
        // Note that the usage of this view model class guarantees that we're always calling
        // with the same info. So checking the value of fetching field is enough to prevent
        // multiple concurrent remote / local DB fetches.
        boolean isFetching = (mFetching.getValue() != null)
                && mFetching.getValue().booleanValue();
        if (isFetching || mRepositoryData.getValue() != null) {
            // We are either fetching the data or have the data already
            return;
        }

        mFetching.setValue(true);

        final PersonDataDatabase db = PersonDataDatabaseHelper.getDatabase(context);
        // Wrap a DB query call in an AsyncTask. Otherwise we'd be doing a disk IO operation on
        // the UI thread.
        new AsyncTask<String, Void, RepositoryData>() {
            @Override
            protected RepositoryData doInBackground(String... params) {
                return db.getPersonDataDao().loadRepository(params[0]);
            }

            @Override
            protected void onPostExecute(RepositoryData repositoryData) {
                if (repositoryData != null) {
                    android.util.Log.e("GithubBrowser", "Got data from local DB");
                    mRepositoryData.setValue(repositoryData);
                    mFetching.setValue(false);
                } else {
                    // TODO - this is temporary until Room persists non-primitive fields. Until
                    // then we split full name into user and name manually
                    String[] split = fullName.split("/");
                    GithubNetworkManager.getInstance().getRepository(split[0], split[1],
                            new GithubNetworkManager.NetworkCallListener<RepositoryData>() {
                                @Override
                                public void onLoadEmpty(int httpCode) {
                                    mFetching.setValue(false);
                                }

                                @Override
                                public void onLoadSuccess(RepositoryData data) {
                                    onDataLoadedFromNetwork(data, db);
                                }

                                @Override
                                public void onLoadFailure() {
                                    mFetching.setValue(false);
                                }
                            });
                }
            }
        }.execute(id);
    }

    @MainThread
    private void onDataLoadedFromNetwork(RepositoryData data, final PersonDataDatabase db) {
        mRepositoryData.setValue(data);
        mFetching.setValue(false);

        // Wrap a DB insert call with another AsyncTask. Otherwise we'd
        // be doing a disk IO operation on the UI thread.
        new AsyncTask<RepositoryData, Void, Void>() {
            @Override
            protected Void doInBackground(RepositoryData... params) {
                db.getPersonDataDao().insertOrReplaceRepository(params[0]);
                return null;
            }
        }.execute(data);
    }
}
