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

package com.android.sample.githubbrowser;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.android.sample.githubbrowser.di.AppComponent;
import com.android.sample.githubbrowser.di.AppModule;
import com.android.sample.githubbrowser.di.DaggerAppComponent;
import com.android.sample.githubbrowser.di.InjectableLifecycleProvider;
import com.android.sample.githubbrowser.di.LifecycleProviderComponent;
import com.android.sample.githubbrowser.di.LifecycleProviderModule;
import com.android.support.lifecycle.LifecycleProvider;

public class GithubBrowserApp extends Application {
    AppComponent mAppComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        mAppComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this)).build();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksAdapter() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
                tryInject(mAppComponent, activity);

                if (activity instanceof FragmentActivity) {
                    ((FragmentActivity) activity).getSupportFragmentManager()
                            .registerFragmentLifecycleCallbacks(
                                    new FragmentManager.FragmentLifecycleCallbacks() {
                                        @Override
                                        public void onFragmentPreAttached(FragmentManager fm,
                                                Fragment f, Context context) {
                                            tryInject(mAppComponent, f);
                                        }
                                    }, true);
                }
            }
        });
    }

    public AppComponent getAppComponent() {
        return mAppComponent;
    }

    private void tryInject(AppComponent appComponent, Object object) {
        if (object instanceof LifecycleProvider
                && object instanceof InjectableLifecycleProvider) {
            final LifecycleProviderComponent component = appComponent
                    .plus(new LifecycleProviderModule((LifecycleProvider) object));
            ((InjectableLifecycleProvider) object).inject(component);
        }
    }

    /**
     * Empty activity callback impl.
     */
    private static class ActivityLifecycleCallbacksAdapter implements ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    }
}
