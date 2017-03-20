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

import com.android.sample.githubbrowser.data.PersonData;
import com.android.sample.githubbrowser.db.GithubDatabase;
import com.android.sample.githubbrowser.di.AppComponent;
import com.android.sample.githubbrowser.network.GithubNetworkManager;
import com.android.sample.githubbrowser.network.GithubNetworkManager.NetworkCallListener;
import com.android.sample.githubbrowser.util.ChainedLiveData;
import com.android.support.lifecycle.LiveData;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

/**
 * View model for the full person data.
 */
public class PersonDataModel extends InjectableViewModel {
    private AtomicBoolean mHasNetworkRequestPending = new AtomicBoolean(false);
    private final ChainedLiveData<PersonData> mPersonData = new ChainedLiveData<>();

    @Inject
    GithubNetworkManager mGithubNetworkManager;
    @Inject
    GithubDatabase mDatabase;

    @Override
    public void inject(AppComponent appComponent) {
        appComponent.inject(this);
    }

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
    public synchronized void loadData(final String login, final boolean forceFullLoad) {
        // Note that the usage of this view model class guarantees that we're always calling
        // with the same login. So checking the value of fetching field is enough to prevent
        // multiple concurrent remote / local DB fetches.
        boolean isFetching = mHasNetworkRequestPending.get();
        boolean havePersonDataAlready = mPersonData.getValue() != null
                && (!forceFullLoad || mPersonData.getValue().isFullData());
        if (isFetching || havePersonDataAlready) {
            // We are either fetching the data or have the data already
            return;
        }
        mPersonData.setBackingLiveData(mDatabase.getGithubDao().loadPerson(login));
        if (mPersonData.getValue() == null || forceFullLoad) {
            // Issue the network request to bring in the data
            mHasNetworkRequestPending.set(true);

            mGithubNetworkManager.getUser(login,
                    new NetworkCallListener<PersonData>() {
                        @Override
                        public void onLoadEmpty(int httpCode) {
                            mHasNetworkRequestPending.set(false);
                        }

                        @Override
                        public void onLoadSuccess(PersonData data) {
                            onDataLoadedFromNetwork(data);
                        }

                        @Override
                        public void onLoadFailure() {
                            mHasNetworkRequestPending.set(false);
                        }
                    });
        }
    }

    @MainThread
    private void onDataLoadedFromNetwork(PersonData data) {
        mHasNetworkRequestPending.set(false);

        // Wrap a DB insert call with another AsyncTask. Otherwise we'd
        // be doing a disk IO operation on the UI thread.
        new AsyncTask<PersonData, Void, Void>() {
            @Override
            protected Void doInBackground(PersonData... params) {
                mDatabase.getGithubDao().insertOrReplacePerson(params[0]);
                return null;
            }
        }.execute(data);
    }

    /**
     * Updates the data wrapped by this model.
     */
    @MainThread
    public void update(final String login, final String email, final String location) {
        // Create a copy of the currently wrapped data
        // Update the relevant fields
        // And update the entry for this person in our database so that it's reflected
        // in the UI the next time it's fetched and displayed
        // Wrap a DB update call with an AsyncTask. Otherwise we'd be doing a disk IO operation on
        // the UI thread.
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mDatabase.getGithubDao().updateUser(login, email, location);
                return null;
            }
        }.execute();

        // Note - this is where you would also issue a network request to update user data
        // on the remote backend.
    }
}
