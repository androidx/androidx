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


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

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
     * @param workTaskExecutor The {@link TaskExecutor} that will run this operation on a background
     *                         thread
     * @param <In> The type of data for {@code inputLiveData}
     * @param <Out> The type of data to output
     * @return A new {@link LiveData} of type {@code Out}
     */
    public static <In, Out> LiveData<Out> dedupedMappedLiveDataFor(
            @NonNull LiveData<In> inputLiveData,
            @NonNull final Function<In, Out> mappingMethod,
            @NonNull final TaskExecutor workTaskExecutor) {

        final Object lock = new Object();
        final MediatorLiveData<Out> outputLiveData = new MediatorLiveData<>();

        outputLiveData.addSource(inputLiveData, new Observer<In>() {

            Out mCurrentOutput = null;

            @Override
            public void onChanged(@Nullable final In input) {
                workTaskExecutor.executeOnBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (lock) {
                            Out newOutput = mappingMethod.apply(input);
                            if (mCurrentOutput == null && newOutput != null) {
                                mCurrentOutput = newOutput;
                                outputLiveData.postValue(newOutput);
                            } else if (mCurrentOutput != null
                                    && !mCurrentOutput.equals(newOutput)) {
                                mCurrentOutput = newOutput;
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
