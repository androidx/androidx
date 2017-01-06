/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.lifecycle;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * When initialized, it hooks into the Activity callback of the Application and observes
 * FragmentActivity classes' FragmentManagers to dispatch Fragment lifecycle events.
 */
class FragmentLifecycleDispatcher {
    private static AtomicBoolean sInitialized = new AtomicBoolean(false);

    static void init(Context context) {
        if (sInitialized.getAndSet(true)) {
            return;
        }
        ((Application) context.getApplicationContext())
                .registerActivityLifecycleCallbacks(new DispatcherActivityCallback());
    }

    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    static class DispatcherActivityCallback implements Application.ActivityLifecycleCallbacks {
        private final FragmentCallback mFragmentCallback;

        DispatcherActivityCallback() {
            mFragmentCallback = new FragmentCallback();
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (activity instanceof FragmentActivity) {
                ((FragmentActivity) activity).getSupportFragmentManager()
                        .registerFragmentLifecycleCallbacks(mFragmentCallback, true);
            }
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

    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    static class FragmentCallback extends FragmentManager.FragmentLifecycleCallbacks {
        @Override
        public void onFragmentCreated(FragmentManager fm, Fragment f, Bundle savedInstanceState) {
            dispatchIfLifecycleFragment(f, Lifecycle.ON_CREATE);
        }

        @Override
        public void onFragmentStarted(FragmentManager fm, Fragment f) {
            dispatchIfLifecycleFragment(f, Lifecycle.ON_START);
        }

        @Override
        public void onFragmentResumed(FragmentManager fm, Fragment f) {
            dispatchIfLifecycleFragment(f, Lifecycle.ON_RESUME);
        }

        @Override
        public void onFragmentPaused(FragmentManager fm, Fragment f) {
            dispatchIfLifecycleFragment(f, Lifecycle.ON_PAUSE);
        }

        @Override
        public void onFragmentStopped(FragmentManager fm, Fragment f) {
            dispatchIfLifecycleFragment(f, Lifecycle.ON_STOP);
        }

        @Override
        public void onFragmentDestroyed(FragmentManager fm, Fragment f) {
            dispatchIfLifecycleFragment(f, Lifecycle.ON_DESTROY);
        }

        private void dispatchIfLifecycleFragment(Fragment fragment,
                @Lifecycle.Event int event) {
            if (fragment instanceof LifecycleRegistryProvider) {
                ((LifecycleRegistryProvider) fragment).getLifecycle().handleLifecycleEvent(event);
            }
        }
    }
}
