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

package com.android.support.lifecycle;

import android.annotation.SuppressLint;
import android.app.Application;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.android.support.lifecycle.ViewModelProvider.Creator;

import java.lang.reflect.InvocationTargetException;

/**
 * Utilities methods for {@link ViewModelStore} class.
 */
public class ViewModelProviders {

    @SuppressLint("StaticFieldLeak")
    private static DefaultCreator sDefaultCreator;

    private static void initializeCreatorIfNeeded(Application application) {
        if (sDefaultCreator == null) {
            sDefaultCreator = new DefaultCreator(application);
        }
    }

    /**
     * Creates a {@link ViewModelProvider}, which retains ViewModels while a scope of given
     * {@code fragment} is alive. More detailed explanation is in {@link ViewModel}.
     * <p>
     * It uses {@link ViewModelProviders.DefaultCreator} to instantiate new ViewModels.
     *
     * @param fragment a fragment, in whose scope ViewModels should be retained
     * @return a ViewModelProvider instance
     */
    @MainThread
    public static ViewModelProvider of(@NonNull Fragment fragment) {
        FragmentActivity activity = fragment.getActivity();
        if (activity == null) {
            throw new IllegalArgumentException(
                    "Can't create ViewModelProvider for detached fragment");
        }
        initializeCreatorIfNeeded(activity.getApplication());
        return new ViewModelProvider(ViewModelStores.of(fragment), sDefaultCreator);
    }

    /**
     * Creates a {@link ViewModelProvider}, which retains ViewModels while a scope of given Activity
     * is alive. More detailed explanation is in {@link ViewModel}.
     * <p>
     * It uses {@link ViewModelProviders.DefaultCreator} to instantiate new ViewModels.
     *
     * @param activity an activity, in whose scope ViewModels should be retained
     * @return a ViewModelProvider instance
     */
    @MainThread
    public static ViewModelProvider of(@NonNull FragmentActivity activity) {
        initializeCreatorIfNeeded(activity.getApplication());
        return new ViewModelProvider(ViewModelStores.of(activity), sDefaultCreator);
    }

    /**
     * Creates a {@link ViewModelProvider}, which retains ViewModels while a scope of given
     * {@code fragment} is alive. More detailed explanation is in {@link ViewModel}.
     * <p>
     * It uses the given {@link Creator} to instantiate new ViewModels.
     *
     * @param fragment a fragment, in whose scope ViewModels should be retained
     * @param creator  a {@code Creator} to instantiate new ViewModels
     * @return a ViewModelProvider instance
     */
    @MainThread
    public static ViewModelProvider of(@NonNull Fragment fragment, @NonNull Creator creator) {
        return new ViewModelProvider(ViewModelStores.of(fragment), creator);
    }

    /**
     * Creates a {@link ViewModelProvider}, which retains ViewModels while a scope of given Activity
     * is alive. More detailed explanation is in {@link ViewModel}.
     * <p>
     * It uses the given {@link Creator} to instantiate new ViewModels.
     *
     * @param activity an activity, in whose scope ViewModels should be retained
     * @param creator  a {@code Creator} to instantiate new ViewModels
     * @return a ViewModelProvider instance
     */
    @MainThread
    public static ViewModelProvider of(@NonNull FragmentActivity activity,
            @NonNull Creator creator) {
        return new ViewModelProvider(ViewModelStores.of(activity), creator);
    }

    /**
     * {@link Creator} which may create {@link AndroidViewModel} and
     * {@link ViewModel}, which have an empty constructor.
     */
    @SuppressWarnings("WeakerAccess")
    public static class DefaultCreator extends ViewModelProvider.NewInstanceCreator {

        private Application mApplication;

        /**
         * Creates a {@code DefaultCreator}
         *
         * @param application an application to pass in {@link AndroidViewModel}
         */
        public DefaultCreator(@NonNull Application application) {
            mApplication = application;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            if (AndroidViewModel.class.isAssignableFrom(modelClass)) {
                //noinspection TryWithIdenticalCatches
                try {
                    return modelClass.getConstructor(Application.class).newInstance(mApplication);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Cannot create an instance of " + modelClass, e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Cannot create an instance of " + modelClass, e);
                } catch (InstantiationException e) {
                    throw new RuntimeException("Cannot create an instance of " + modelClass, e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Cannot create an instance of " + modelClass, e);
                }
            }
            return super.create(modelClass);
        }
    }
}
