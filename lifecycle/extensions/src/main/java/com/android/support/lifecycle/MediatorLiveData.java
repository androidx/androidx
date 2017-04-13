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

package com.android.support.lifecycle;

import android.support.annotation.CallSuper;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * {@link LiveData} subclass which may observer other {@code LiveData} objects and react on
 * {@code OnChanged} events from them.
 * <p>
 * This class correctly propagates its active/inactive states down to source {@code LiveData}
 * objects.
 *
 * @param <T> The type of data hold by this instance
 */
@SuppressWarnings("WeakerAccess")
public class MediatorLiveData<T> extends MutableLiveData<T> {
    private Set<Source<?>> mSources = new HashSet<>();

    /**
     * Starts to listen the given {@code source} LiveData, {@code onChanged} observer will be called
     * when {@code source} value was changed.
     * <p>
     * {@code onChanged} callback will be called only when this {@code MediatorLiveData} is active.
     *
     * @param source    the {@code LiveData} to listen to
     * @param onChanged The observer that will receive the events
     * @param <S>       The type of data hold by {@code source} LiveData
     */
    @MainThread
    public <S> void addSource(LiveData<S> source, Observer<S> onChanged) {
        Source<S> e = new Source<>(source, onChanged);
        mSources.add(e);
        if (getActiveObserverCount() > 0) {
            e.plug();
        }
    }

    /**
     * Stops to listen the given {@code LiveData}.
     *
     * @param toRemote {@code LiveData} to stop to listen
     * @param <S>      the type of data hold by {@code source} LiveData
     */
    @MainThread
    public <S> void removeSource(LiveData<S> toRemote) {
        for (Iterator<Source<?>> iterator = mSources.iterator(); iterator.hasNext(); ) {
            Source<?> source = iterator.next();
            if (source.mLiveData == toRemote) {
                source.unplug();
                iterator.remove();
                break;
            }
        }
    }

    @CallSuper
    @Override
    protected void onActive() {
        for (Source<?> source : mSources) {
            source.plug();
        }
    }

    @CallSuper
    @Override
    protected void onInactive() {
        for (Source<?> source : mSources) {
            source.unplug();
        }
    }

    private static class Source<V> {
        final LiveData<V> mLiveData;
        final Observer<V> mObserver;
        int mVersion = START_VERSION;

        Source(LiveData<V> liveData, final Observer<V> observer) {
            mLiveData = liveData;
            mObserver = new Observer<V>() {
                @Override
                public void onChanged(@Nullable V v) {
                    if (mVersion != mLiveData.getVersion()) {
                        mVersion = mLiveData.getVersion();
                        observer.onChanged(v);
                    }
                }
            };
        }

        void plug() {
            mLiveData.observeForever(mObserver);
        }

        void unplug() {
            mLiveData.removeObserver(mObserver);
        }
    }
}
