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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryOwner;

/**
 * Skeleton of androidx.lifecycle.ViewModelProvider.KeyedFactory
 * that creates {@link SavedStateHandle} for every requested {@link androidx.lifecycle.ViewModel}.
 * The subclasses implement {@link #create(String, Class, SavedStateHandle)} to actually instantiate
 * {@code androidx.lifecycle.ViewModel}s.
 */
public abstract class AbstractSavedStateViewModelFactory extends ViewModelProvider.KeyedFactory {
    static final String TAG_SAVED_STATE_HANDLE_CONTROLLER = "androidx.lifecycle.savedstate.vm.tag";

    private final SavedStateRegistry mSavedStateRegistry;
    private final Lifecycle mLifecycle;
    private final Bundle mDefaultArgs;

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
    public AbstractSavedStateViewModelFactory(@NonNull SavedStateRegistryOwner owner,
            @Nullable Bundle defaultArgs) {
        mSavedStateRegistry = owner.getSavedStateRegistry();
        mLifecycle = owner.getLifecycle();
        mDefaultArgs = defaultArgs;
    }

    // TODO: make KeyedFactory#create(String, Class) package private
    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    public final <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> modelClass) {
        Bundle restoredState = mSavedStateRegistry.consumeRestoredStateForKey(key);
        SavedStateHandle handle = SavedStateHandle.createHandle(restoredState, mDefaultArgs);
        SavedStateHandleController controller = new SavedStateHandleController(key, handle);
        controller.attachToLifecycle(mSavedStateRegistry, mLifecycle);
        T viewmodel = create(key, modelClass, handle);
        viewmodel.setTagIfAbsent(TAG_SAVED_STATE_HANDLE_CONTROLLER, controller);
        mSavedStateRegistry.runOnNextRecreation(OnRecreation.class);
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

    static final class OnRecreation implements SavedStateRegistry.AutoRecreated {

        @Override
        public void onRecreated(@NonNull SavedStateRegistryOwner owner) {
            if (!(owner instanceof ViewModelStoreOwner)) {
                throw new IllegalStateException(
                        "Internal error: OnRecreation should be registered only on components"
                                + "that implement ViewModelStoreOwner");
            }
            ViewModelStore viewModelStore = ((ViewModelStoreOwner) owner).getViewModelStore();
            SavedStateRegistry savedStateRegistry = owner.getSavedStateRegistry();
            for (String key : viewModelStore.keys()) {
                ViewModel viewModel = viewModelStore.get(key);
                SavedStateHandleController controller = viewModel.getTag(
                        TAG_SAVED_STATE_HANDLE_CONTROLLER);
                if (controller != null && !controller.isAttached()) {
                    controller.attachToLifecycle(owner.getSavedStateRegistry(),
                            owner.getLifecycle());
                }
            }
            if (!viewModelStore.keys().isEmpty()) {
                savedStateRegistry.runOnNextRecreation(OnRecreation.class);
            }
        }
    }

    static final class SavedStateHandleController implements LifecycleEventObserver {
        private final String mKey;
        boolean mIsAttached = false;
        private final SavedStateHandle mHandle;

        SavedStateHandleController(String key, SavedStateHandle handle) {
            mKey = key;
            mHandle = handle;
        }

        boolean isAttached() {
            return mIsAttached;
        }

        void attachToLifecycle(SavedStateRegistry registry, Lifecycle lifecycle) {
            if (mIsAttached) {
                throw new IllegalStateException("Already attached to lifecycleOwner");
            }
            mIsAttached = true;
            lifecycle.addObserver(this);
            registry.registerSavedStateProvider(mKey, mHandle.savedStateProvider());
        }

        @Override
        public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                mIsAttached = false;
                source.getLifecycle().removeObserver(this);
            }
        }
    }
}

