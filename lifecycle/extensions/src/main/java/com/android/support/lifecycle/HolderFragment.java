/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.lifecycle;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;

import com.android.support.lifecycle.state.RetainedStateProvider;
import com.android.support.lifecycle.state.SavedStateProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HolderFragment extends Fragment {

    private SavedStateProvider mSavedStateProvider = new SavedStateProvider();
    private RetainedStateProvider mRetainedStateProvider = new RetainedStateProvider();
    private Map<String, ViewModel> mViewModels = new HashMap<>();

    public HolderFragment() {
        setRetainInstance(true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSavedStateProvider.restoreState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSavedStateProvider.saveState(outState);
    }

    /**
     * get ViewModel by key
     */
    public ViewModel getViewModel(String key) {
        return mViewModels.get(key);
    }

    /**
     * adds new ViewModels
     */
    public void putViewModel(String key, ViewModel viewModel) {
        mViewModels.put(key, viewModel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (ViewModel vm : mViewModels.values()) {
            vm.onCleared();
        }
    }

    public SavedStateProvider getSavedStateProvider() {
        return mSavedStateProvider;
    }

    public RetainedStateProvider getRetainedStateProvider() {
        return mRetainedStateProvider;
    }
}
