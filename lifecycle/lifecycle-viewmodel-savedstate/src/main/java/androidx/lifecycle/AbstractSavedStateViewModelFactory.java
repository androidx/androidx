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

import static androidx.lifecycle.LegacySavedStateHandleController.attachHandleIfNeeded;
import static androidx.lifecycle.ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryOwner;

/**
 * Skeleton of androidx.lifecycle.ViewModelProvider.KeyedFactory
 * that creates {@link SavedStateHandle} for every requested {@link androidx.lifecycle.ViewModel}.
 * The subclasses implement {@link #create(String, Class, SavedStateHandle)} to actually instantiate
 * {@code androidx.lifecycle.ViewModel}s.
 */
public abstract class AbstractSavedStateViewModelFactory extends ViewModelProvider.OnRequeryFactory
        implements ViewModelProvider.Factory {
    static final String TAG_SAVED_STATE_HANDLE_CONTROLLER = "androidx.lifecycle.savedstate.vm.tag";

    private SavedStateRegistry mSavedStateRegistry;
    private Lifecycle mLifecycle;
    private Bundle mDefaultArgs;

    /**
     * Constructs this factory.
     * <p>
     * When a factory is constructed this way, a component for which {@link SavedStateHandle} is
     * scoped must have called
     * {@link SavedStateHandleSupport#installSavedStateHandleSupport(SavedStateRegistryOwner)}.
     * See {@link SavedStateHandleSupport#createSavedStateHandle(CreationExtras)} docs for more
     * details.
     */
    public AbstractSavedStateViewModelFactory() {
    }

    /**
     * Constructs this factory.
     *
     * @param owner {@link SavedStateRegistryOwner} that will provide restored state for created
     * {@link androidx.lifecycle.ViewModel ViewModels}
     * @param defaultArgs values from this {@code Bundle} will be used as defaults by
     *                    {@link SavedStateHandle} passed in {@link ViewModel ViewModels}
     *                    if there is no previously saved state
     *                    or previously saved state misses a value by such key
     */
    @SuppressLint("LambdaLast")
    public AbstractSavedStateViewModelFactory(@NonNull SavedStateRegistryOwner owner,
            @Nullable Bundle defaultArgs) {
        mSavedStateRegistry = owner.getSavedStateRegistry();
        mLifecycle = owner.getLifecycle();
        mDefaultArgs = defaultArgs;
    }

    @NonNull
    @Override
    public final <T extends ViewModel> T create(@NonNull Class<T> modelClass,
            @NonNull CreationExtras extras) {
        String key = extras.get(VIEW_MODEL_KEY);
        if (key == null) {
            throw new IllegalStateException(
                    "VIEW_MODEL_KEY must always be provided by ViewModelProvider");
        }
        // if a factory constructed in the old way use the old infra to create SavedStateHandle
        if (mSavedStateRegistry != null) {
            return create(key, modelClass);
        } else {
            return create(key, modelClass, SavedStateHandleSupport.createSavedStateHandle(extras));
        }
    }

    @NonNull
    private <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> modelClass) {
        SavedStateHandleController controller = LegacySavedStateHandleController
                .create(mSavedStateRegistry, mLifecycle, key, mDefaultArgs);
        T viewmodel = create(key, modelClass, controller.getHandle());
        viewmodel.setTagIfAbsent(TAG_SAVED_STATE_HANDLE_CONTROLLER, controller);
        return viewmodel;
    }

    @NonNull
    @Override
    public final <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        // ViewModelProvider calls correct create that support same modelClass with different keys
        // If a developer manually calls this method, there is no "key" in picture, so factory
        // simply uses classname internally as as key.
        String canonicalName = modelClass.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
        }
        if (mLifecycle == null) {
            throw new UnsupportedOperationException(
                    "AbstractSavedStateViewModelFactory constructed "
                            + "with empty constructor supports only calls to "
                            +   "create(modelClass: Class<T>, extras: CreationExtras)."
            );
        }
        return create(canonicalName, modelClass);
    }

    /**
     * Creates a new instance of the given {@code Class}.
     * <p>
     *
     * @param key a key associated with the requested ViewModel
     * @param modelClass a {@code Class} whose instance is requested
     * @param handle a handle to saved state associated with the requested ViewModel
     * @param <T> The type parameter for the ViewModel.
     * @return a newly created ViewModels
     */
    @NonNull
    protected abstract <T extends ViewModel> T create(@NonNull String key,
            @NonNull Class<T> modelClass, @NonNull SavedStateHandle handle);

    /**
     * @hide
     */
    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onRequery(@NonNull ViewModel viewModel) {
        // is need only for legacy path
        if (mSavedStateRegistry != null) {
            attachHandleIfNeeded(viewModel, mSavedStateRegistry, mLifecycle);
        }
    }
}

