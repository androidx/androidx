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
package com.android.sample.githubbrowser.model;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.android.sample.githubbrowser.BuildConfig;
import com.android.support.lifecycle.LiveData;

import javax.inject.Singleton;

/**
 * Model for the auth token.
 */
@Singleton
public class AuthTokenModel {
    private static final String AUTH_TOKEN_KEY = "auth_token";

    private LiveData<String> mAuthToken = new LiveData<>();
    private final SharedPreferences mSharedPreferences;

    public AuthTokenModel(Application application) {
         mSharedPreferences = application
                .getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        loadAsync();
    }

    public void saveToken(String token) {
        saveAsync(token);
        mAuthToken.postValue(token);
    }

    private void loadAsync() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                String token = mSharedPreferences.getString(AUTH_TOKEN_KEY, null);
                mAuthToken.postValue(token);
                return null;
            }
        }.execute();
    }

    private void saveAsync(String token) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... tokens) {
                mSharedPreferences.edit().putString(AUTH_TOKEN_KEY, tokens[0]).apply();
                return null;
            }
        }.execute(token);
    }

    /**
     * Returns the {@link LiveData} object that wraps the auth token.
     */
    public LiveData<String> getAuthTokenData() {
        return mAuthToken;
    }

    public void clearToken() {
        saveToken(null);
    }
}
