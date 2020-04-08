/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.livedata

import androidx.compose.Composable
import androidx.compose.CompositionLifecycleObserver
import androidx.compose.State
import androidx.compose.mutableStateOf
import androidx.compose.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.ui.core.LifecycleOwnerAmbient

/**
 * Starts observing this [LiveData] and represents its values via [State]. Every time there would
 * be new value posted into the [LiveData] the returned [State] will be updated causing
 * recomposition of every [State.value] usage.
 *
 * The inner observer will automatically be removed when this composable disposes or the current
 * [LifecycleOwner] moves to the [Lifecycle.State.DESTROYED] state.
 *
 * @sample androidx.ui.livedata.samples.LiveDataSample
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun <T> LiveData<T>.observeAsState(): State<T?> = observeAsState(value)

/**
 * Starts observing this [LiveData] and represents its values via [State]. Every time there would
 * be new value posted into the [LiveData] the returned [State] will be updated causing
 * recomposition of every [State.value] usage.
 *
 * The inner observer will automatically be removed when this composable disposes or the current
 * [LifecycleOwner] moves to the [Lifecycle.State.DESTROYED] state.
 *
 * @sample androidx.ui.livedata.samples.LiveDataWithInitialSample
 */
@Composable
fun <R, T : R> LiveData<T>.observeAsState(initial: R): State<R> {
    val lifecycleOwner = LifecycleOwnerAmbient.current
    val observer = remember { DisposableObserver<R, T>(initial, lifecycleOwner) }
    observer.source = this
    return observer.state
}

private class DisposableObserver<R, T : R>(
    initial: R,
    private val lifecycleOwner: LifecycleOwner
) : Observer<T>, CompositionLifecycleObserver {

    val state = mutableStateOf(initial)

    var source: LiveData<T>? = null
        set(source) {
            if (source !== field) {
                field?.removeObserver(this)
                field = source
                source?.observe(lifecycleOwner, this)
            }
        }

    override fun onChanged(t: T) {
        state.value = t
    }

    override fun onLeave() {
        // the same as onDispose()
        source = null
    }

    override fun onEnter() {
        // do nothing
    }
}
