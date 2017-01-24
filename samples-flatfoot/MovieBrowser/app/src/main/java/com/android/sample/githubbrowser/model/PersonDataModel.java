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

import com.android.sample.githubbrowser.PersonData;
import com.android.sample.githubbrowser.db.PersonDataDatabase;
import com.android.sample.githubbrowser.db.PersonDataDatabaseHelper;
import com.android.sample.githubbrowser.network.GithubNetworkManager;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

/**
 * View model for the full person data.
 */
public class PersonDataModel implements ViewModel {
    private LiveData<Boolean> mFetching = new LiveData<>();
    private LiveData<PersonData> mPersonData = new LiveData<>();

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
    public synchronized void loadData(final Context context, final String login) {
        // Note that the usage of this view model class guarantees that we're always calling
        // with the same login. So checking the value of fetching field is enough to prevent
        // multiple concurrent remote / local DB fetches.
        boolean isFetching = (mFetching.getValue() != null)
                && mFetching.getValue().booleanValue();
        if (isFetching || mPersonData.getValue() != null) {
            // We are either fetching the data or have the data already
            return;
        }

        mFetching.setValue(true);

        final PersonDataDatabase db = PersonDataDatabaseHelper.getDatabase(context);
        // Wrap a DB query call in an AsyncTask. Otherwise we'd be doing a disk IO operation on
        // the UI thread.
        new AsyncTask<String, Void, PersonData>() {
            @Override
            protected PersonData doInBackground(String... params) {
                return db.getPersonDataDao().load(params[0]);
            }

            @Override
            protected void onPostExecute(PersonData personData) {
                if (personData != null) {
                    android.util.Log.e("GithubBrowser", "Got data from local DB");
                    mPersonData.setValue(personData);
                    mFetching.setValue(false);
                } else {
                    GithubNetworkManager.getInstance().getUser(login,
                            new GithubNetworkManager.NetworkCallListener<PersonData>() {
                                @Override
                                public void onLoadEmpty(int httpCode) {
                                    mFetching.setValue(false);
                                }

                                @Override
                                public void onLoadSuccess(PersonData data) {
                                    onDataLoadedFromNetwork(data, db);
                                }

                                @Override
                                public void onLoadFailure() {
                                    mFetching.setValue(false);
                                }
                            });
                }
            }
        }.execute(login);
    }

    @MainThread
    private void onDataLoadedFromNetwork(PersonData data, final PersonDataDatabase db) {
        mPersonData.setValue(data);
        mFetching.setValue(false);

        // Wrap a DB insert call with another AsyncTask. Otherwise we'd
        // be doing a disk IO operation on the UI thread.
        new AsyncTask<PersonData, Void, Void>() {
            @Override
            protected Void doInBackground(PersonData... params) {
                db.getPersonDataDao().insertOrReplace(params[0]);
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
        // And set the new data as our wrapper's value. At this point the observer(s) registered
        // on this live data will get notified of the underlying changes, updating their state
        mPersonData.setValue(newData);

        // And finally update the entry for this person in our database so that it's reflected
        // in the UI the next time it's fetched and displayed
        final PersonDataDatabase db = PersonDataDatabaseHelper.getDatabase(context);
        // Wrap a DB update call with an AsyncTask. Otherwise we'd be doing a disk IO operation on
        // the UI thread.
        new AsyncTask<PersonData, Void, Void>() {
            @Override
            protected Void doInBackground(PersonData... params) {
                db.getPersonDataDao().insertOrReplace(params[0]);
                return null;
            }
        }.execute(newData);
    }
}
