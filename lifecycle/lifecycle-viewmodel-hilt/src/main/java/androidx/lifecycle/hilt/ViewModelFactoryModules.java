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

package androidx.lifecycle.hilt;

import android.app.Activity;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.savedstate.SavedStateRegistryOwner;

import java.util.Map;

import javax.inject.Provider;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.components.FragmentComponent;
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
        abstract Map<Class<? extends ViewModel>,
                ViewModelAssistedFactory<? extends ViewModel>> viewModelFactoriesMap();

        @Provides
        @NonNull
        @ActivityViewModelFactory
        static ViewModelFactory provideFactory(
                @NonNull Activity activity,
                @NonNull Map<Class<? extends ViewModel>,
                        Provider<ViewModelAssistedFactory<? extends ViewModel>>>
                        viewModelFactories) {
            // Hilt guarantees concrete activity is a subclass of ComponentActivity.
            SavedStateRegistryOwner owner = (ComponentActivity) activity;
            Bundle defaultArgs = activity.getIntent().getExtras();
            return new ViewModelFactory(owner, defaultArgs, viewModelFactories);
        }
    }

    /**
     * Hilt Modules for providing the fragment level ViewModelFactory
     */
    @Module
    @InstallIn(FragmentComponent.class)
    public static final class FragmentModule {

        @Provides
        @NonNull
        @FragmentViewModelFactory
        static ViewModelFactory provideFactory(
                @NonNull Fragment fragment,
                @NonNull Map<Class<? extends ViewModel>,
                        Provider<ViewModelAssistedFactory<? extends ViewModel>>>
                        viewModelFactories) {
            Bundle defaultArgs = fragment.getArguments();
            return new ViewModelFactory(fragment, defaultArgs, viewModelFactories);
        }

        private FragmentModule() {
        }
    }

    private ViewModelFactoryModules() {
    }
}
