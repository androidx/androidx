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

package com.android.sample.githubbrowser.util;

import android.support.annotation.Nullable;

import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.Observer;

/**
 * A live data that can be backed by another live data and this backing live data can be swapped.
 * It automatically starts / stops observing on the backing live data as this LiveData's observers
 * and active state changes.
 * <p>
 * This is useful when we want to use a LiveData in a model that arrives from another provider. We
 * don't want the UI to care about this nor we want to leak previous LiveData instances.
 * @param <T>
 */
public class ChainedLiveData<T> extends LiveData<T> {
    private final Observer<T> mObserver = new Observer<T>() {
        @Override
        public void onChanged(@Nullable T t) {
            setValue(t);
        }
    };

    @Nullable
    private LiveData<T> mBackingLiveData;

    public void setBackingLiveData(@Nullable LiveData<T> backingLiveData) {
        if (mBackingLiveData != null) {
            mBackingLiveData.removeObserver(mObserver);
        }
        mBackingLiveData = backingLiveData;
        if (backingLiveData == null) {
            setValue(null);
        } else {
            if (getActiveObserverCount() > 0) {
                backingLiveData.observeForever(mObserver);
            } else {
                setValue(backingLiveData.getValue());
            }
        }
    }

    @Override
    protected void onActive() {
        if (mBackingLiveData != null) {
            mBackingLiveData.observeForever(mObserver);
        }
    }

    @Override
    protected void onInactive() {
        if (mBackingLiveData != null) {
            mBackingLiveData.removeObserver(mObserver);
        }
    }
}
