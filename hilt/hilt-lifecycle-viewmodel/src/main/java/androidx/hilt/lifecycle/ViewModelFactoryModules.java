/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.hilt.lifecycle;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.savedstate.SavedStateRegistryOwner;

import java.util.Map;

import javax.inject.Provider;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.components.FragmentComponent;
import dagger.hilt.android.internal.lifecycle.DefaultActivityViewModelFactory;
import dagger.hilt.android.internal.lifecycle.DefaultFragmentViewModelFactory;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;

/**
 * Hilt Modules for providing ViewModel factories.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class ViewModelFactoryModules {

    /**
     * Hilt Modules for providing the activity level ViewModelFactory
     */
    @Module
    @InstallIn(ActivityComponent.class)
    public abstract static class ActivityModule {

        @NonNull
        @Multibinds
        abstract Map<String, ViewModelAssistedFactory<? extends ViewModel>> viewModelFactoriesMap();

        @Provides
        @IntoSet
        @NonNull
        @DefaultActivityViewModelFactory
        static ViewModelProvider.Factory provideFactory(
                @NonNull Activity activity,
                @NonNull Application application,
                @NonNull Map<String, Provider<ViewModelAssistedFactory<? extends ViewModel>>>
                        viewModelFactories) {
            // Hilt guarantees concrete activity is a subclass of ComponentActivity.
            SavedStateRegistryOwner owner = (ComponentActivity) activity;
            Bundle defaultArgs = activity.getIntent() != null
                    ? activity.getIntent().getExtras() : null;
            SavedStateViewModelFactory delegate =
                    new SavedStateViewModelFactory(application, owner, defaultArgs);
            return new HiltViewModelFactory(owner, defaultArgs, delegate, viewModelFactories);
        }
    }

    /**
     * Hilt Modules for providing the fragment level ViewModelFactory
     */
    @Module
    @InstallIn(FragmentComponent.class)
    public static final class FragmentModule {

        @Provides
        @IntoSet
        @NonNull
        @DefaultFragmentViewModelFactory
        static ViewModelProvider.Factory provideFactory(
                @NonNull Fragment fragment,
                @NonNull Application application,
                @NonNull Map<String, Provider<ViewModelAssistedFactory<? extends ViewModel>>>
                        viewModelFactories) {
            Bundle defaultArgs = fragment.getArguments();
            SavedStateViewModelFactory delegate =
                    new SavedStateViewModelFactory(application, fragment, defaultArgs);
            return new HiltViewModelFactory(fragment, defaultArgs, delegate, viewModelFactories);
        }

        private FragmentModule() {
        }
    }

    private ViewModelFactoryModules() {
    }
}