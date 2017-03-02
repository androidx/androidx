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

import com.android.sample.githubbrowser.AuthTokenLifecycle;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

/**
 * Model for the auth token.
 */
public class AuthTokenModel extends ViewModel {
    private LiveData<String> mAuthToken = new LiveData<>();
    private AuthTokenLifecycle mAuthTokenLifecycle;

    /** Sets the auth token lifecycle callback. */
    public void setAuthTokenLifecycle(AuthTokenLifecycle authTokenLifecycle) {
        mAuthTokenLifecycle = authTokenLifecycle;
    }

    /** Returns the current auth token lifecycle callback. */
    public AuthTokenLifecycle getAuthTokenLifecycle() {
        return mAuthTokenLifecycle;
    }

    /**
     * Returns the {@LiveData} object that wraps the auth token.
     */
    public LiveData<String> getAuthTokenData() {
        return mAuthToken;
    }
}
