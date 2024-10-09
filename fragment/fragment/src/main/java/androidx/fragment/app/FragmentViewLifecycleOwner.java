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

package androidx.fragment.app;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.lifecycle.HasDefaultViewModelProviderFactory;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.SavedStateHandleSupport;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.lifecycle.viewmodel.MutableCreationExtras;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryController;
import androidx.savedstate.SavedStateRegistryOwner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

class FragmentViewLifecycleOwner implements HasDefaultViewModelProviderFactory,
        SavedStateRegistryOwner, ViewModelStoreOwner {
    private final Fragment mFragment;
    private final ViewModelStore mViewModelStore;
    private final Runnable mRestoreViewSavedStateRunnable;

    private ViewModelProvider.Factory mDefaultFactory;

    private LifecycleRegistry mLifecycleRegistry = null;
    private SavedStateRegistryController mSavedStateRegistryController = null;

    FragmentViewLifecycleOwner(
            @NonNull Fragment fragment,
            @NonNull ViewModelStore viewModelStore,
            @NonNull Runnable restoreViewSavedStateRunnable
    ) {
        mFragment = fragment;
        mViewModelStore = viewModelStore;
        mRestoreViewSavedStateRunnable = restoreViewSavedStateRunnable;
    }

    @Override
    public @NonNull ViewModelStore getViewModelStore() {
        initialize();
        return mViewModelStore;
    }

    /**
     * Initializes the underlying Lifecycle if it hasn't already been created.
     */
    void initialize() {
        if (mLifecycleRegistry == null) {
            mLifecycleRegistry = new LifecycleRegistry(this);
            mSavedStateRegistryController = SavedStateRegistryController.create(this);
            mSavedStateRegistryController.performAttach();
            mRestoreViewSavedStateRunnable.run();
        }
    }

    /**
     * @return True if the Lifecycle has been initialized.
     */
    boolean isInitialized() {
        return mLifecycleRegistry != null;
    }

    @Override
    public @NonNull Lifecycle getLifecycle() {
        initialize();
        return mLifecycleRegistry;
    }

    void setCurrentState(Lifecycle.@NonNull State state) {
        mLifecycleRegistry.setCurrentState(state);
    }

    void handleLifecycleEvent(Lifecycle.@NonNull Event event) {
        mLifecycleRegistry.handleLifecycleEvent(event);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@link Fragment#getArguments() Fragment's arguments} when this is first called will
     * be used as the defaults to any {@link androidx.lifecycle.SavedStateHandle} passed to a
     * view model created using this factory.</p>
     */
    @Override
    public ViewModelProvider.@NonNull Factory getDefaultViewModelProviderFactory() {
        ViewModelProvider.Factory currentFactory =
                mFragment.getDefaultViewModelProviderFactory();

        if (!currentFactory.equals(mFragment.mDefaultFactory)) {
            mDefaultFactory = currentFactory;
            return currentFactory;
        }

        if (mDefaultFactory == null) {
            Application application = null;
            Context appContext = mFragment.requireContext().getApplicationContext();
            while (appContext instanceof ContextWrapper) {
                if (appContext instanceof Application) {
                    application = (Application) appContext;
                    break;
                }
                appContext = ((ContextWrapper) appContext).getBaseContext();
            }

            mDefaultFactory = new SavedStateViewModelFactory(
                    application,
                    mFragment,
                    mFragment.getArguments());
        }

        return mDefaultFactory;
    }

    @Override
    @CallSuper
    public @NonNull CreationExtras getDefaultViewModelCreationExtras() {
        Application application = null;
        Context appContext = mFragment.requireContext().getApplicationContext();
        while (appContext instanceof ContextWrapper) {
            if (appContext instanceof Application) {
                application = (Application) appContext;
                break;
            }
            appContext = ((ContextWrapper) appContext).getBaseContext();
        }
        MutableCreationExtras extras = new MutableCreationExtras();
        if (application != null) {
            extras.set(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY, application);
        }
        extras.set(SavedStateHandleSupport.SAVED_STATE_REGISTRY_OWNER_KEY, mFragment);
        extras.set(SavedStateHandleSupport.VIEW_MODEL_STORE_OWNER_KEY, this);
        if (mFragment.getArguments() != null) {
            extras.set(SavedStateHandleSupport.DEFAULT_ARGS_KEY, mFragment.getArguments());
        }
        return extras;
    }

    @Override
    public @NonNull SavedStateRegistry getSavedStateRegistry() {
        initialize();
        return mSavedStateRegistryController.getSavedStateRegistry();
    }

    void performRestore(@Nullable Bundle savedState) {
        mSavedStateRegistryController.performRestore(savedState);
    }

    void performSave(@NonNull Bundle outBundle) {
        mSavedStateRegistryController.performSave(outBundle);
    }
}
