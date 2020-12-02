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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.AbstractSavedStateViewModelFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModel;
import androidx.savedstate.SavedStateRegistryOwner;

import java.util.Map;

import javax.inject.Provider;

import dagger.hilt.EntryPoint;
import dagger.hilt.EntryPoints;
import dagger.hilt.InstallIn;

/**
 * View Model Provider Factory for the Hilt Extension.
 * <p>
 * A provider for this factory will be installed in the
 * {@link dagger.hilt.android.components.ActivityComponent} and
 * {@link dagger.hilt.android.components.FragmentComponent}. An instance of this factory will also
 * be the default factory by activities and fragments annotated with
 * {@link dagger.hilt.android.AndroidEntryPoint}.
 */
public final class HiltViewModelFactory extends AbstractSavedStateViewModelFactory {

    /** @hide */
    @EntryPoint
    @InstallIn(ViewModelComponent.class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    interface ViewModelFactoriesEntryPoint {
        @InternalViewModelInjectMap
        Map<String, Provider<ViewModel>> getProviderMap();
    }

    private static final String KEY_PREFIX = "androidx.hilt.lifecycle.HiltViewModelFactory";

    private final SavedStateViewModelFactory mDelegateFactory;
    private final ViewModelComponent.Builder mViewModelComponentBuilder;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    HiltViewModelFactory(
            @NonNull SavedStateRegistryOwner owner,
            @Nullable Bundle defaultArgs,
            @NonNull SavedStateViewModelFactory delegateFactory,
            @NonNull ViewModelComponent.Builder viewModelComponentBuilder) {
        super(owner, defaultArgs);
        this.mDelegateFactory = delegateFactory;
        this.mViewModelComponentBuilder = viewModelComponentBuilder;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    protected <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> modelClass,
            @NonNull SavedStateHandle handle) {
        ViewModelComponent component = mViewModelComponentBuilder.bindHandle(handle).build();
        Provider<? extends ViewModel> provider =
                EntryPoints.get(component, ViewModelFactoriesEntryPoint.class)
                        .getProviderMap().get(modelClass.getName());
        if (provider == null) {
            // Delegate to factory that will attempt to reflectively construct the ViewModel.
            // A prefixed key is used to avoid collisions since the
            // AbstractSavedStateViewModelFactory already registered a key for us before invoking
            // this create() method. This is a workaround until b/160796112 is resolved.
            // TODO(danysantiago): Warn about missing @ViewModelInject if this fails.
            return mDelegateFactory.create(KEY_PREFIX + ":" + key, modelClass);
        }
        return (T) provider.get();
    }

}
