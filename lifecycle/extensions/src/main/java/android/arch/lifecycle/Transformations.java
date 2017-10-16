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

import android.arch.core.util.Function;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;

/**
 * Transformations for a {@link LiveData} class.
 */
@SuppressWarnings("WeakerAccess")
public class Transformations {

    private Transformations() {
    }

    /**
     * Applies the given function on the main thread to each value emitted by {@code source}
     * LiveData and returns LiveData, which emits resulting values.
     * <p>
     * The given function {@code func} will be executed on the main thread.
     *
     * @param source a {@code LiveData} to listen to
     * @param func   a function to apply
     * @param <X>    a type of {@code source} LiveData
     * @param <Y>    a type of resulting LiveData.
     * @return a LiveData which emits resulting values
     */
    @MainThread
    public static <X, Y> LiveData<Y> map(LiveData<X> source, final Function<X, Y> func) {
        final MediatorLiveData<Y> result = new MediatorLiveData<>();
        result.addSource(source, new Observer<X>() {
            @Override
            public void onChanged(@Nullable X x) {
                result.setValue(func.apply(x));
            }
        });
        return result;
    }

    /**
     * Creates a LiveData, let's name it {@code swLiveData}, which follows next flow:
     * it reacts on changes of {@code trigger} LiveData, applies the given function to new value of
     * {@code trigger} LiveData and sets resulting LiveData as a "backing" LiveData
     * to {@code swLiveData}.
     * "Backing" LiveData means, that all events emitted by it will retransmitted
     * by {@code swLiveData}.
     * <p>
     * If the given function returns null, then {@code swLiveData} is not "backed" by any other
     * LiveData.
     * <p>
     * The given function {@code func} will be executed on the main thread.
     *
     * @param trigger a {@code LiveData} to listen to
     * @param func    a function which creates "backing" LiveData
     * @param <X>     a type of {@code source} LiveData
     * @param <Y>     a type of resulting LiveData
     */
    @MainThread
    public static <X, Y> LiveData<Y> switchMap(LiveData<X> trigger,
            final Function<X, LiveData<Y>> func) {
        final MediatorLiveData<Y> result = new MediatorLiveData<>();
        result.addSource(trigger, new Observer<X>() {
            LiveData<Y> mSource;

            @Override
            public void onChanged(@Nullable X x) {
                LiveData<Y> newLiveData = func.apply(x);
                if (mSource == newLiveData) {
                    return;
                }
                if (mSource != null) {
                    result.removeSource(mSource);
                }
                mSource = newLiveData;
                if (mSource != null) {
                    result.addSource(mSource, new Observer<Y>() {
                        @Override
                        public void onChanged(@Nullable Y y) {
                            result.setValue(y);
                        }
                    });
                }
            }
        });
        return result;
    }
}
