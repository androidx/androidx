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

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

/**
 * NavControllerViewModel is the always up to date view of the NavController's
 * non configuration state
 */
class NavControllerViewModel extends ViewModel {

    private static final ViewModelProvider.Factory FACTORY = new ViewModelProvider.Factory() {
        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            NavControllerViewModel viewModel = new NavControllerViewModel();
            return (T) viewModel;
        }
    };

    @NonNull
    static NavControllerViewModel getInstance(ViewModelStore viewModelStore) {
        ViewModelProvider viewModelProvider = new ViewModelProvider(viewModelStore, FACTORY);
        return viewModelProvider.get(NavControllerViewModel.class);
    }

    private final HashMap<UUID, ViewModelStore> mViewModelStores = new HashMap<>();

    void clear(@NonNull UUID backStackEntryUUID) {
        // Clear and remove the NavGraph's ViewModelStore
        ViewModelStore viewModelStore = mViewModelStores.remove(backStackEntryUUID);
        if (viewModelStore != null) {
            viewModelStore.clear();
        }
    }

    @Override
    protected void onCleared() {
        for (UUID key: mViewModelStores.keySet()) {
            clear(key);
        }
    }

    @NonNull
    ViewModelStore getViewModelStore(@NonNull UUID backStackEntryUUID) {
        ViewModelStore viewModelStore = mViewModelStores.get(backStackEntryUUID);
        if (viewModelStore == null) {
            viewModelStore = new ViewModelStore();
            mViewModelStores.put(backStackEntryUUID, viewModelStore);
        }
        return viewModelStore;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("NavControllerViewModel{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append("} ViewModelStores (");
        Iterator<UUID> viewModelStoreIterator = mViewModelStores.keySet().iterator();
        while (viewModelStoreIterator.hasNext()) {
            sb.append(viewModelStoreIterator.next());
            if (viewModelStoreIterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(')');
        return sb.toString();
    }
}
