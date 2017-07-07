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
import android.content.Context;

import com.android.sample.githubbrowser.databinding.DataBindingAdapters;
import com.android.support.lifecycle.LifecycleObserver;
import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.OnLifecycleEvent;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.LifecycleListener;
import com.bumptech.glide.manager.RequestManagerRetriever;
import com.bumptech.glide.manager.RequestManagerTreeNode;

import java.util.Collections;
import java.util.Set;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class LifecycleProviderModule {
    private final LifecycleProvider mLifecycleProvider;

    public LifecycleProviderModule(LifecycleProvider lifecycleProvider) {
        mLifecycleProvider = lifecycleProvider;
    }

    @Provides
    @Singleton
    public LifecycleProvider provideLifecycleProvider() {
        return mLifecycleProvider;
    }

    @Provides
    @LifecycleProviderScope
    public DataBindingAdapters provideAdapters(RequestManager requestManager) {
        return new DataBindingAdapters(requestManager);
    }

    @Provides
    @LifecycleProviderScope
    public RequestManager provideGlideRequestManager(Application application) {
        return new RequestManager(application, new Lifecycle() {
            @Override
            public void addListener(final LifecycleListener listener) {
                mLifecycleProvider.getLifecycle().addObserver(new LifecycleObserver() {
                    @OnLifecycleEvent(com.android.support.lifecycle.Lifecycle.ON_START)
                    public void onStart() {
                        listener.onStart();
                    }

                    @OnLifecycleEvent(com.android.support.lifecycle.Lifecycle.ON_STOP)
                    public void onStop() {
                        listener.onStop();
                    }

                    @OnLifecycleEvent(com.android.support.lifecycle.Lifecycle.ON_DESTROY)
                    public void onDestroy() {
                        listener.onDestroy();
                    }
                });
            }
        }, new RequestManagerTreeNode() {
            @Override
            public Set<RequestManager> getDescendants() {
                return Collections.emptySet();
            }
        });
    }
}
