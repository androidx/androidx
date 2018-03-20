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

package androidx.work.impl.utils;

import android.arch.core.util.Function;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor;

/**
 * Utility methods for {@link LiveData}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LiveDataUtils {

    /**
     * Creates a new {@link LiveData} object that maps the values of {@code inputLiveData} using
     * {@code mappingMethod} on a background thread, but only triggers its observers when the mapped
     * values actually change.
     *
     * @param inputLiveData An input {@link LiveData}
     * @param mappingMethod A {@link Function} that maps input of type {@code In} to output of type
     *                      {@code Out}
     * @param <In> The type of data for {@code inputLiveData}
     * @param <Out> The type of data to output
     * @return A new {@link LiveData} of type {@code Out}
     */
    public static <In, Out> LiveData<Out> dedupedMappedLiveDataFor(
            @NonNull LiveData<In> inputLiveData,
            @NonNull final Function<In, Out> mappingMethod) {
        final MediatorLiveData<Out> outputLiveData = new MediatorLiveData<>();
        outputLiveData.addSource(inputLiveData, new Observer<In>() {
            @Override
            public void onChanged(@Nullable final In input) {
                WorkManagerTaskExecutor.getInstance().executeOnBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (outputLiveData) {
                            Out newOutput = mappingMethod.apply(input);
                            Out previousOutput = outputLiveData.getValue();
                            if (previousOutput == null && newOutput != null) {
                                outputLiveData.postValue(newOutput);
                            } else if (
                                    previousOutput != null && !previousOutput.equals(newOutput)) {
                                outputLiveData.postValue(newOutput);
                            }
                        }
                    }
                });
            }
        });
        return outputLiveData;
    }

    private LiveDataUtils() {
    }
}
