/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.HasDefaultViewModelProviderFactory;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryController;
import androidx.savedstate.SavedStateRegistryOwner;

import java.util.UUID;

/**
 * Representation of an entry in the back stack of a {@link NavController}.
 */
final class NavBackStackEntry implements
        LifecycleOwner,
        ViewModelStoreOwner, HasDefaultViewModelProviderFactory,
        SavedStateRegistryOwner {
    private final Context mContext;
    private final NavDestination mDestination;
    private final Bundle mArgs;
    private final LifecycleRegistry mLifecycle = new LifecycleRegistry(this);
    private final SavedStateRegistryController mSavedStateRegistryController =
            SavedStateRegistryController.create(this);

    // Internal unique name for this navBackStackEntry;
    @NonNull
    final UUID mId;
    private NavControllerViewModel mNavControllerViewModel;
    private ViewModelProvider.Factory mDefaultFactory;

    NavBackStackEntry(@NonNull Context context,
            @NonNull NavDestination destination, @Nullable Bundle args,
            @Nullable LifecycleOwner navControllerLifecyleOwner,
            @Nullable NavControllerViewModel navControllerViewModel) {
        this(context, destination, args,
                navControllerLifecyleOwner, navControllerViewModel,
                UUID.randomUUID(), null);
    }

    NavBackStackEntry(@NonNull Context context,
            @NonNull NavDestination destination, @Nullable Bundle args,
            @Nullable LifecycleOwner navControllerLifecyleOwner,
            @Nullable NavControllerViewModel navControllerViewModel,
            @NonNull UUID uuid, @Nullable Bundle savedState) {
        mContext = context;
        mId = uuid;
        mDestination = destination;
        mArgs = args;
        mNavControllerViewModel = navControllerViewModel;
        mSavedStateRegistryController.performRestore(savedState);
        if (navControllerLifecyleOwner != null) {
            mLifecycle.setCurrentState(navControllerLifecyleOwner.getLifecycle()
                    .getCurrentState());
        } else {
            mLifecycle.setCurrentState(Lifecycle.State.CREATED);
        }
    }

    /**
     * Gets the destination associated with this entry
     * @return The destination that is currently visible to users
     */
    @NonNull
    public NavDestination getDestination() {
        return mDestination;
    }

    /**
     * Gets the arguments used for this entry
     * @return The arguments used when this entry was created
     */
    @Nullable
    public Bundle getArguments() {
        return mArgs;
    }

    void setNavControllerViewModel(@NonNull NavControllerViewModel navControllerViewModel) {
        mNavControllerViewModel = navControllerViewModel;
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycle;
    }

    void handleLifecycleEvent(Lifecycle.Event event) {
        mLifecycle.handleLifecycleEvent(event);
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return mNavControllerViewModel.getViewModelStore(mId);
    }

    @NonNull
    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        if (mDefaultFactory == null) {
            mDefaultFactory = new SavedStateViewModelFactory(
                    (Application) mContext.getApplicationContext(),
                    this,
                    mArgs);
        }
        return mDefaultFactory;
    }

    @NonNull
    @Override
    public SavedStateRegistry getSavedStateRegistry() {
        return mSavedStateRegistryController.getSavedStateRegistry();
    }

    void saveState(@NonNull Bundle outBundle) {
        mSavedStateRegistryController.performSave(outBundle);
    }
}
