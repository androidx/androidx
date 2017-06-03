/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.lifecycle;

import java.util.HashMap;

/**
 * Class to store {@code ViewModels}.
 * <p>
 * An instance of {@code ViewModelStore} must be retained through configuration changes:
 * if an owner of this {@code ViewModelStore} is destroyed and recreated due to configuration
 * changes, new instance of an owner should still have the same old instance of
 * {@code ViewModelStore}.
 * <p>
 * If an owner of this {@code ViewModelStore} is destroyed and is not going to be recreated,
 * then it should call {@link #clear()} on this {@code ViewModelStore}, so {@code ViewModels} would
 * be notified that they are no longer used.
 * <p>
 * {@link android.arch.lifecycle.ViewModelStores} provides a {@code ViewModelStore} for
 * activities and fragments.
 */
public class ViewModelStore {

    private final HashMap<String, ViewModel> mMap = new HashMap<>();

    final void put(String key, ViewModel viewModel) {
        ViewModel oldViewModel = mMap.get(key);
        if (oldViewModel != null) {
            oldViewModel.onCleared();
        }
        mMap.put(key, viewModel);
    }

    final ViewModel get(String key) {
        return mMap.get(key);
    }

    /**
     *  Clears internal storage and notifies ViewModels that they are no longer used.
     */
    public final void clear() {
        for (ViewModel vm : mMap.values()) {
            vm.onCleared();
        }
        mMap.clear();
    }
}
