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
import androidx.lifecycle.AbstractSavedStateViewModelFactory;
import androidx.lifecycle.HasDefaultViewModelProviderFactory;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryController;
import androidx.savedstate.SavedStateRegistryOwner;

import java.util.UUID;

/**
 * Representation of an entry in the back stack of a {@link NavController}. The
 * {@link Lifecycle}, {@link ViewModelStore}, and {@link SavedStateRegistry} provided via
 * this object are valid for the lifetime of this destination on the back stack: when this
 * destination is popped off the back stack, the lifecycle will be destroyed, state
 * will no longer be saved, and ViewModels will be cleared.
 */
public final class NavBackStackEntry implements
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
    private Lifecycle.State mHostLifecycle = Lifecycle.State.CREATED;
    private Lifecycle.State mMaxLifecycle = Lifecycle.State.RESUMED;
    private NavControllerViewModel mNavControllerViewModel;
    private ViewModelProvider.Factory mDefaultFactory;
    private SavedStateHandle mSavedStateHandle;

    NavBackStackEntry(@NonNull Context context,
            @NonNull NavDestination destination, @Nullable Bundle args,
            @Nullable LifecycleOwner navControllerLifecycleOwner,
            @Nullable NavControllerViewModel navControllerViewModel) {
        this(context, destination, args,
                navControllerLifecycleOwner, navControllerViewModel,
                UUID.randomUUID(), null);
    }

    NavBackStackEntry(@NonNull Context context,
            @NonNull NavDestination destination, @Nullable Bundle args,
            @Nullable LifecycleOwner navControllerLifecycleOwner,
            @Nullable NavControllerViewModel navControllerViewModel,
            @NonNull UUID uuid, @Nullable Bundle savedState) {
        mContext = context;
        mId = uuid;
        mDestination = destination;
        mArgs = args;
        mNavControllerViewModel = navControllerViewModel;
        mSavedStateRegistryController.performRestore(savedState);
        if (navControllerLifecycleOwner != null) {
            mHostLifecycle = navControllerLifecycleOwner.getLifecycle().getCurrentState();
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

    /**
     * {@inheritDoc}
     * <p>
     * If the {@link NavHost} has not called {@link NavHostController#setLifecycleOwner}, the
     * Lifecycle will be capped at {@link Lifecycle.State#CREATED}.
     */
    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycle;
    }

    void setMaxLifecycle(@NonNull Lifecycle.State maxState) {
        mMaxLifecycle = maxState;
        updateState();
    }

    @NonNull
    Lifecycle.State getMaxLifecycle() {
        return mMaxLifecycle;
    }

    void handleLifecycleEvent(@NonNull Lifecycle.Event event) {
        mHostLifecycle = getStateAfter(event);
        updateState();
    }

    /**
     * Update the state to be the lower of the two constraints:
     */
    void updateState() {
        if (mHostLifecycle.ordinal() < mMaxLifecycle.ordinal()) {
            mLifecycle.setCurrentState(mHostLifecycle);
        } else {
            mLifecycle.setCurrentState(mMaxLifecycle);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if called before the {@link NavHost} has called
     * {@link NavHostController#setViewModelStore}.
     */
    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        if (mNavControllerViewModel == null) {
            throw new IllegalStateException("You must call setViewModelStore() on your "
                    + "NavHostController before accessing "
                    + "the ViewModelStore of a navigation graph.");
        }
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

    /**
     * Gets the {@link SavedStateHandle} for this entry.
     *
     * @return the SavedStateHandle for this entry
     */
    @NonNull
    public SavedStateHandle getSavedStateHandle() {
        if (mSavedStateHandle == null) {
            mSavedStateHandle = new ViewModelProvider(
                    this, new NavResultSavedStateFactory(this, null)
            ).get(SavedStateViewModel.class).getHandle();
        }
        return mSavedStateHandle;
    }

    // Copied from LifecycleRegistry.getStateAfter()
    @NonNull
    private static Lifecycle.State getStateAfter(@NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
            case ON_STOP:
                return Lifecycle.State.CREATED;
            case ON_START:
            case ON_PAUSE:
                return Lifecycle.State.STARTED;
            case ON_RESUME:
                return Lifecycle.State.RESUMED;
            case ON_DESTROY:
                return Lifecycle.State.DESTROYED;
            case ON_ANY:
                break;
        }
        throw new IllegalArgumentException("Unexpected event value " + event);
    }

    /**
     * Used to create the {SavedStateViewModel}
     */
    private static class NavResultSavedStateFactory extends AbstractSavedStateViewModelFactory {

        NavResultSavedStateFactory(
                @NonNull SavedStateRegistryOwner owner, @Nullable Bundle defaultArgs) {
            super(owner, defaultArgs);
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        protected <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> modelClass,
                @NonNull SavedStateHandle handle) {
            SavedStateViewModel savedStateViewModel = new SavedStateViewModel(handle);
            return (T) savedStateViewModel;
        }
    }

    private static class SavedStateViewModel extends ViewModel {
        private SavedStateHandle mHandle;

        SavedStateViewModel(SavedStateHandle handle) {
            mHandle = handle;
        }

        public SavedStateHandle getHandle() {
            return mHandle;
        }
    }
}
