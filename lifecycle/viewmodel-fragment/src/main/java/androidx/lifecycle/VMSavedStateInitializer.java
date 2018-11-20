/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.lifecycle;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

class VMSavedStateInitializer implements Application.ActivityLifecycleCallbacks {

    private static boolean sInitialized = false;

    static void initializeIfNeeded(Application application) {
        if (!sInitialized) {
            application.registerActivityLifecycleCallbacks(new VMSavedStateInitializer());
            sInitialized = true;
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity instanceof FragmentActivity) {
            final FragmentActivity fragmentActivity = (FragmentActivity) activity;
            fragmentActivity.getSupportFragmentManager()
                    .registerFragmentLifecycleCallbacks(new FragmentCallbacks(), true);
            // but it is too early - viewmodels aren't ready yet.
            fragmentActivity.getLifecycle().addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                    // next event is going to be created....
                    if (!fragmentActivity.getViewModelStore().keys().isEmpty()) {
                        attach(fragmentActivity.getBundleSavedStateRegistry(), fragmentActivity);
                    }
                    source.getLifecycle().removeObserver(this);
                }
            });

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

    @SuppressWarnings("WeakerAccess")
    static void attach(SavedStateRegistry<Bundle> savedStateStore, ViewModelStoreOwner store) {
        ViewModelStore viewModelStore = store.getViewModelStore();
        for (String key : viewModelStore.keys()) {
            ViewModel viewModel = viewModelStore.get(key);
            SavedStateHandle handle = viewModel
                    .getTag(SavedStateVMFactory.TAG_SAVED_STATE_HANDLE);
            if (handle != null) {
                savedStateStore.registerSavedStateProvider(key, handle.savedStateComponent());
            }
        }
    }

    static class FragmentCallbacks extends FragmentManager.FragmentLifecycleCallbacks {
        @Override
        public void onFragmentAttached(@NonNull FragmentManager fm,
                @NonNull Fragment fragment, @NonNull Context context) {
            if (!fragment.getViewModelStore().keys().isEmpty()) {
                attach(fragment.getBundleSavedStateRegistry(), fragment);
            }
        }
    }
}
