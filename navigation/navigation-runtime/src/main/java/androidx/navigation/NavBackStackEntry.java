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
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import java.util.UUID;

/**
 * Representation of an entry in the back stack of a {@link NavController}.
 */
final class NavBackStackEntry implements ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    private final Context mContext;
    private final NavDestination mDestination;
    private final Bundle mArgs;

    // Internal unique name for this navBackStackEntry;
    @NonNull
    final UUID mId;
    private NavControllerViewModel mNavControllerViewModel;

    NavBackStackEntry(@NonNull Context context,
            @NonNull NavDestination destination, @Nullable Bundle args,
            @Nullable NavControllerViewModel navControllerViewModel) {
        this(context, destination, args, navControllerViewModel, UUID.randomUUID());
    }

    NavBackStackEntry(@NonNull Context context,
            @NonNull NavDestination destination, @Nullable Bundle args,
            @Nullable NavControllerViewModel navControllerViewModel,
            @NonNull UUID uuid) {
        mContext = context;
        mId = uuid;
        mDestination = destination;
        mArgs = args;
        mNavControllerViewModel = navControllerViewModel;
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
    public ViewModelStore getViewModelStore() {
        return mNavControllerViewModel.getViewModelStore(mId);
    }

    @NonNull
    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        return ViewModelProvider.AndroidViewModelFactory.getInstance(
                (Application) mContext.getApplicationContext());
    }
}
