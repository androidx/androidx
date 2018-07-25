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

abstract class SavedStateVMFactory implements ViewModelProvider.KeyedFactory {
    static final String TAG_SAVED_STATE_HANDLE = "androidx.lifecycle.savedstate.vm.tag";
    static final String LOG_TAG = "SavedStateVMFactory";

    private final ViewModelWithStateFactory mWrappedFactory;
    private final SavedStateStore mSavedStateStore;

    SavedStateVMFactory(SavedStateStore savedStateStore, ViewModelWithStateFactory factory) {
        mWrappedFactory = factory;
        mSavedStateStore = savedStateStore;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> modelClass) {
        Bundle savedState = mSavedStateStore.consumeRestoredStateForKey(key);
        SavedStateHandle handle = new SavedStateHandle(mSavedStateStore.getArguments(),
                savedState);
        mSavedStateStore.registerSavedStateCallback(key, handle.savedStateComponent());
        T viewmodel = mWrappedFactory.create(key, modelClass, handle);
        viewmodel.setTag(TAG_SAVED_STATE_HANDLE, handle);
        return viewmodel;
    }
}

