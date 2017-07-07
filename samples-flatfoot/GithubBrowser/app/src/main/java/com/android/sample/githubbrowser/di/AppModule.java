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

package com.android.sample.githubbrowser.di;

import android.app.Application;

import com.android.sample.githubbrowser.db.GithubDatabase;
import com.android.sample.githubbrowser.model.AuthTokenModel;
import com.android.sample.githubbrowser.network.GithubNetworkManager;
import com.android.support.room.Room;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {
    private final Application mApplication;

    public AppModule(Application application) {
        mApplication = application;
    }

    @Singleton
    @Provides
    public GithubNetworkManager provideGithubNetworkManager(AuthTokenModel authTokenModel) {
        return new GithubNetworkManager(authTokenModel);
    }

    @Singleton
    @Provides
    public AuthTokenModel provideAuthTokenModel(Application application) {
        return new AuthTokenModel(application);
    }

    @Provides
    public Application provideApplication() {
        return mApplication;
    }

    @Singleton
    @Provides
    public GithubDatabase provideGithubDatabase(Application application) {
        return Room.databaseBuilder(application, GithubDatabase.class, "github.db")
                .build();
    }
}
