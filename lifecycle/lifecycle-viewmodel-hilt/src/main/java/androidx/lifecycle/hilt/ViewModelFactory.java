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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AbstractSavedStateViewModelFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.savedstate.SavedStateRegistryOwner;

import java.util.Map;

import javax.inject.Provider;

/**
 * View Model Provider Factory for the Hilt Extension.
 */
public final class ViewModelFactory extends AbstractSavedStateViewModelFactory {

    private final Map<
            Class<? extends ViewModel>,
            Provider<ViewModelAssistedFactory<? extends ViewModel>>> mViewModelFactories;

    ViewModelFactory(
            @NonNull SavedStateRegistryOwner owner,
            @Nullable Bundle defaultArgs,
            @NonNull Map<Class<? extends ViewModel>,
                    Provider<ViewModelAssistedFactory<? extends ViewModel>>> viewModelFactories) {
        super(owner, defaultArgs);
        this.mViewModelFactories = viewModelFactories;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    protected <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> modelClass,
            @NonNull SavedStateHandle handle) {
        // TODO(danysantiago): What to do with 'key' ???
        // TODO(danysantiago): Better exception for missing class
        return (T) mViewModelFactories.get(modelClass).get().create(handle);
    }
}
