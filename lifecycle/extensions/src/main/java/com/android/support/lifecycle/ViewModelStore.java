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

import android.annotation.SuppressLint;
import android.app.Application;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.android.support.lifecycle.state.StateProviders;

/**
 * Utility class to create managed ViewModels.
 *
 * @see ViewModel ViewModel - for samples and usage patterns.
 */
public class ViewModelStore {
    private static final String LOG_TAG = "ViewModelStore";

    private static final String DEFAULT_KEY =
            "com.android.support.lifecycle.extensions.ViewModelStore.DefaultKey";

    @SuppressLint("StaticFieldLeak")
    private static Application sApplication;

    /**
     * Returns an existing ViewModel or creates a new one for the given scope.
     * <p>
     * The created ViewModel is associated with the given LifecycleProvider and will be retained
     * as long as the scope (LifecycleProvider) is alive (e.g. if it is an activity, until it is
     * finished or process is killed).
     *
     * @param scope      The scope of the ViewModel which will retain it.
     * @param modelClass The class of the ViewModel to create an instance of it if it is not
     *                   present.
     * @param <T>        The type parameter for the ViewModel.
     * @return A ViewModel that is an instance of the given type {@code T}.
     */
    @NonNull
    @MainThread
    public static <T extends ViewModel> T get(LifecycleProvider scope,
            Class<T> modelClass) {
        String canonicalName = modelClass.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
        }
        return get(scope, DEFAULT_KEY + ":" + canonicalName, modelClass);
    }

    /**
     * Returns an existing ViewModel or creates a new one for the given scope with the given key.
     * <p>
     * The created ViewModel is associated with the given LifecycleProvider and will be retained
     * as long as the scope (LifecycleProvider) is alive (e.g. if it is an activity, until it is
     * finished or process is killed).
     *
     * @param scope      The scope of the ViewModel which will retain it.
     * @param key        The key to use to identify the ViewModel.
     * @param modelClass The class of the ViewModel to create an instance of it if it is not
     *                   present.
     * @param <T>        The type parameter for the ViewModel.
     * @return A ViewModel that is an instance of the given type {@code T}.
     */
    @NonNull
    @MainThread
    public static <T extends ViewModel> T get(LifecycleProvider scope, String key,
            Class<T> modelClass) {
        HolderFragment holderFragment = StateProviders.holderFragmentFor(scope);
        if (sApplication == null) {
            FragmentActivity activity = holderFragment.getActivity();
            if (activity == null) {
                throw new IllegalStateException("Can't create VM for detached fragment");
            }
            sApplication = activity.getApplication();
        }

        ViewModel viewModel = holderFragment.getViewModel(key);
        if (modelClass.isInstance(viewModel)) {
            //noinspection unchecked
            return (T) viewModel;
        } else if (viewModel != null) {
            Log.w(LOG_TAG, "Using same key = " + key + " with different ViewModel classes.");
        }
        //noinspection TryWithIdenticalCatches
        try {
            ViewModel oldViewModel = viewModel;
            viewModel = createViewModel(modelClass);
            if (oldViewModel != null) {
                oldViewModel.onCleared();
            }
            holderFragment.putViewModel(key, viewModel);
            //noinspection unchecked
            return (T) viewModel;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot create an instance of " + modelClass, e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Cannot create an instance of " + modelClass, e);
        }
    }

    private static <T extends ViewModel> T createViewModel(Class<T> modelClass)
            throws IllegalAccessException, InstantiationException {
        return modelClass.newInstance();
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    static Application getApplication() {
        return sApplication;
    }
}
