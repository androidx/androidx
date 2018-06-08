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

package com.android.support.lifecycle.testapp;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.android.support.lifecycle.LifecycleActivity;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;
import com.android.support.lifecycle.ViewModelStore;

/**
 * activity for LiveDataTransactionTest
 */
public class LiveDataTestActivity extends LifecycleActivity {

    public static final String LIVE_DATA_VALUE = "saveInstanceState";
    private static final int MAX_DEPTH = 5;
    private static final String VM_TAG = "test";

    /** view model*/
    public LiveDataViewModel viewModel;
    /** counter of created  */
    public int fragmentsNumber;

    /** ViewModel class */
    public static class LiveDataViewModel extends ViewModel {
        public LiveData<String> liveData = new LiveData<>();
    }

    /** Counting Fragment */
    public static class CountingFragment extends LifecycleFragment {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ((LiveDataTestActivity) getActivity()).fragmentsNumber++;
        }
    }

    /** a fragment which injects new fragment on new value of livedata */
    public static class InternalFragment extends CountingFragment {

        int mDepth = MAX_DEPTH;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            LiveDataViewModel liveDataViewModel = ViewModelStore.get(
                    (LifecycleProvider) getActivity(), VM_TAG, LiveDataViewModel.class);
            liveDataViewModel.liveData.observe(this, s ->
                    getChildFragmentManager().beginTransaction().add(new CountingFragment(),
                            s).commitNow());

            if (mDepth == MAX_DEPTH) {
                return;
            }
            InternalFragment aFragment = new InternalFragment();
            aFragment.mDepth = mDepth + 1;
            InternalFragment bFragment = new InternalFragment();
            bFragment.mDepth = mDepth + 1;
            getChildFragmentManager().beginTransaction()
                    .add(aFragment, getTag() + "_" + mDepth + "_a")
                    .add(bFragment, getTag() + "_" + mDepth + "_b")
                    .commitNow();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelStore.get(this, VM_TAG, LiveDataViewModel.class);
        viewModel.liveData.observe(this, s ->
                getSupportFragmentManager().beginTransaction().add(new CountingFragment(),
                        s).commit());
        String tag = "0_a";
        if (getSupportFragmentManager().findFragmentByTag(tag) == null) {
            InternalFragment internalFragment = new InternalFragment();
            internalFragment.mDepth = 1;
            getSupportFragmentManager().beginTransaction().add(internalFragment, tag).commitNow();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.liveData.setValue(LIVE_DATA_VALUE);
    }
}
