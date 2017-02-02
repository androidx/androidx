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

import com.android.sample.githubbrowser.Utils;
import com.android.sample.githubbrowser.data.PersonData;
import com.android.sample.githubbrowser.db.GithubDatabase;
import com.android.sample.githubbrowser.db.GithubDatabaseHelper;
import com.android.sample.githubbrowser.network.GithubNetworkManager;
import com.android.sample.githubbrowser.network.GithubNetworkManager.NetworkCallListener;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * View model for the full person data.
 */
public class PersonDataModel implements ViewModel {
    private AtomicBoolean mHasNetworkRequestPending = new AtomicBoolean(false);
    private LiveData<PersonData> mPersonData;

    /**
     * Returns the {@LiveData} object that wraps the full person data.
     */
    public LiveData<PersonData> getPersonData() {
        return mPersonData;
    }

    /**
     * Sets the login for fetching the full person data.
     */
    @MainThread
    public synchronized void loadData(final Context context, final String login,
            final boolean forceFullLoad) {
        // Note that the usage of this view model class guarantees that we're always calling
        // with the same login. So checking the value of fetching field is enough to prevent
        // multiple concurrent remote / local DB fetches.
        boolean isFetching = mHasNetworkRequestPending.get();
        boolean havePersonDataAlready = (mPersonData != null) && (mPersonData.getValue() != null)
                && (!forceFullLoad || Utils.isFullData(mPersonData.getValue()));
        if (isFetching || havePersonDataAlready) {
            // We are either fetching the data or have the data already
            return;
        }

        final GithubDatabase db = GithubDatabaseHelper.getDatabase(context);
        mPersonData = db.getGithubDao().loadPerson(login);
        if (mPersonData == null || forceFullLoad) {
            // Issue the network request to bring in the data
            mHasNetworkRequestPending.set(true);

            GithubNetworkManager.getInstance().getUser(login,
                    new NetworkCallListener<PersonData>() {
                        @Override
                        public void onLoadEmpty(int httpCode) {
                            mHasNetworkRequestPending.set(false);
                        }

                        @Override
                        public void onLoadSuccess(PersonData data) {
                            onDataLoadedFromNetwork(data, db);
                        }

                        @Override
                        public void onLoadFailure() {
                            mHasNetworkRequestPending.set(false);
                        }
                    });
        }
    }

    @MainThread
    private void onDataLoadedFromNetwork(PersonData data, final GithubDatabase db) {
        mHasNetworkRequestPending.set(false);

        // Wrap a DB insert call with another AsyncTask. Otherwise we'd
        // be doing a disk IO operation on the UI thread.
        new AsyncTask<PersonData, Void, Void>() {
            @Override
            protected Void doInBackground(PersonData... params) {
                db.getGithubDao().insertOrReplacePerson(params[0]);
                return null;
            }
        }.execute(data);
    }

    /**
     * Updates the data wrapped by this model.
     */
    @MainThread
    public void update(Context context, String email, String location) {
        // Create a copy of the currently wrapped data
        PersonData newData = new PersonData(mPersonData.getValue());
        // Update the relevant fields
        newData.email = email;
        newData.location = location;

        // And update the entry for this person in our database so that it's reflected
        // in the UI the next time it's fetched and displayed
        final GithubDatabase db = GithubDatabaseHelper.getDatabase(context);
        // Wrap a DB update call with an AsyncTask. Otherwise we'd be doing a disk IO operation on
        // the UI thread.
        new AsyncTask<PersonData, Void, Void>() {
            @Override
            protected Void doInBackground(PersonData... params) {
                db.getGithubDao().insertOrReplacePerson(params[0]);
                return null;
            }
        }.execute(newData);

        // Note - this is where you would also issue a network request to update user data
        // on the remote backend.
    }
}
