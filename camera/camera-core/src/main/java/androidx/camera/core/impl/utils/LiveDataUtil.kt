/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.impl.utils

import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.camera.core.impl.utils.Threads.runOnMain
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

public object LiveDataUtil {
    /**
     * Returns a [LiveData] mapped from the input [LiveData] by applying {@code mapFunction} to each
     * value set on {@code source}.
     *
     * Similar to [androidx.lifecycle.Transformations.map], but it can ensure [getValue] returns
     * mapped value without the need to have active observers.
     */
    @MainThread
    @JvmStatic
    public fun <I, O> map(source: LiveData<I>, mapFunction: Function<I, O>): LiveData<O> {
        val result = MappingRedirectableLiveData<I, O>(mapFunction.apply(source.value), mapFunction)
        result.redirectTo(source)
        return result
    }
}

/**
 * A [LiveData] which can be redirected to a source [LiveData] in order to use the source data as
 * its own.
 *
 * This `LiveData` is considered as the destination `LiveData` while the source `LiveData` is the
 * one this redirects to. Whenever source `LiveData` gets updated with a value, this `LiveData` is
 * also updated with that value.
 *
 * @param T The type of both the source `LiveData` and this `LiveData`.
 * @property initialValue The initial value that is returned if no redirection is set yet.
 * @see MappingRedirectableLiveData
 */
public class RedirectableLiveData<T>(private val initialValue: T) :
    MappingRedirectableLiveData<T, T>(initialValue, { it })

/**
 * A [LiveData] which can be redirected to a source [LiveData] in order to map the source data as
 * its own data.
 *
 * This `LiveData` is considered as the destination `LiveData` while the source `LiveData` is the
 * one this redirects to. Whenever source `LiveData` gets updated with a value, this `LiveData` is
 * also updated with a mapped equivalent of that value.
 *
 * @param I The type of the source `LiveData`.
 * @param O The type of the destination `LiveData`.
 * @property initialValue The initial value that is returned if no redirection is set yet.
 * @property mapFunction The function that maps source data.
 * @see redirectTo
 */
public open class MappingRedirectableLiveData<I, O>(
    private val initialValue: O,
    private val mapFunction: Function<I, O>,
) : MediatorLiveData<O>() {
    private var liveDataSource: LiveData<I>? = null

    /**
     * Redirects to a source [LiveData] whose value is mapped to this `LiveData`.
     *
     * After this function is invoked, the data of this `LiveData` will depend on the source live
     * data with [mapFunction] invoked.
     *
     * @param liveDataSource The source `LiveData`.
     */
    public fun redirectTo(liveDataSource: LiveData<I>) {
        val oldSource = this.liveDataSource
        this.liveDataSource = liveDataSource
        runOnMain {
            if (oldSource != null) {
                super.removeSource(oldSource)
            }
            super.addSource(liveDataSource) { value: I -> this.value = mapFunction.apply(value) }
        }
    }

    override fun <S> addSource(source: LiveData<S>, onChanged: Observer<in S>) {
        throw UnsupportedOperationException()
    }

    // Overrides getValue() to reflect the correct value from source. This is required to ensure
    // getValue() is correct when observe() or observeForever() is not called.
    /**
     * Gets the value of this [LiveData] based on the provided redirection and mapping.
     *
     * If no redirection has been set yet, the [initialValue] property is returned.
     *
     * @see redirectTo
     * @see MediatorLiveData.getValue
     */
    override fun getValue(): O? {
        // Returns initial value if source is not set.
        val liveDataSource = this.liveDataSource // snapshot for non-null smart casting
        if (liveDataSource == null) {
            return initialValue
        }
        return mapFunction.apply(liveDataSource.value)
    }
}
