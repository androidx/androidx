/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.background.workmanager.utils;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

/**
 * Utility methods for {@link LiveData}.
 */

public class LiveDataUtils {

    /**
     * Creates a new {@link LiveData} object that mirrors the input, but only triggers its observers
     * when the values actually change.
     *
     * @param inputLiveData An input {@link LiveData}
     * @param <T> The type of payload associated with the {@link LiveData}
     * @return A new {@link LiveData} that triggers its observers only when the input LiveData's
     *         value changes (as determined by an {@code equals} call)
     */
    public static <T> LiveData<T> dedupedLiveDataFor(LiveData<T> inputLiveData) {
        final MediatorLiveData<T> dedupedLiveData = new MediatorLiveData<>();
        dedupedLiveData.addSource(inputLiveData, new Observer<T>() {
            @Override
            public void onChanged(@Nullable T updatedValue) {
                T dedupedValue = dedupedLiveData.getValue();
                boolean valueNotSet = (dedupedValue == null && updatedValue != null);
                boolean valueChanged = (dedupedValue != null && !dedupedValue.equals(updatedValue));
                if (valueNotSet || valueChanged) {
                    dedupedLiveData.setValue(updatedValue);
                }
            }
        });
        return dedupedLiveData;
    }

    private LiveDataUtils() {
    }
}
