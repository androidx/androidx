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

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.ViewModel;

import java.util.Map;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.multibindings.Multibinds;

/**
 * Hilt Module for the View Model Provider Factory
 *
 * @hide
 */
@Module
@InstallIn(ActivityComponent.class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
abstract class ViewModelModule {

    @Multibinds
    abstract Map<Class<? extends ViewModel>, ViewModelAssistedFactory<?>> viewModelFactoriesMap();

    @Provides
    @NonNull
    static ViewModelFactory provideFactory(
            @NonNull Activity activity,
            @NonNull Map<Class<? extends ViewModel>,
                    ViewModelAssistedFactory<?>> viewModelFactories) {
        // TODO(danysantiago): Validate activity instanceof ComponentActivity
        return new ViewModelFactory(
                (ComponentActivity) activity,
                activity.getIntent().getExtras(),
                viewModelFactories);
    }
}
