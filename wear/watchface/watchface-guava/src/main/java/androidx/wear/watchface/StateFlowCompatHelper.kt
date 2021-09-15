/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface

import androidx.annotation.UiThread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext

/**
 * A helper which provides Java style callbacks whenever [StateFlow.value] changes in the supplied
 * [StateFlow].
 *
 * @param stateFlow The [StateFlow] to observe.
 */
// TODO(alexclarke): Consider if there's a better location for this helper
public class StateFlowCompatHelper<T>(private val stateFlow: StateFlow<T>) {
    /** A listener for observing StateFlow changes from java. */
    public fun interface ValueChangeListener<T> {
        /** Called whenever the value changes. */
        @UiThread
        public fun onValueChanged(userStyle: T)
    }

    private val listeners = HashMap<ValueChangeListener<T>, Executor>()
    private val lock = Any()

    init {
        val immediateCoroutineScope = CoroutineScope(
            object : CoroutineDispatcher() {
                override fun dispatch(context: CoroutineContext, block: Runnable) {
                    block.run()
                }
            }
        )
        immediateCoroutineScope.launch {
            stateFlow.collect {
                // We iterate over a copy of the listeners set because callbacks could mutate it.
                for ((listener, executor) in synchronized(lock) { HashMap(listeners) }) {
                    executor.execute {
                        listener.onValueChanged(it)
                    }
                }
            }
        }
    }

    /**
     * Adds a [ValueChangeListener] which is called immediately with the [StateFlow]'s current
     * value and subsequently whenever the value changes.
     *
     * NB the order in which ambient vs style changes are reported is not guaranteed.
     */
    public fun addValueChangeListener(
        userStyleChangeListener: ValueChangeListener<T>,
        executor: Executor
    ) {
        synchronized(lock) {
            listeners.put(userStyleChangeListener, executor)
        }
        userStyleChangeListener.onValueChanged(stateFlow.value)
    }

    /** Removes a [ValueChangeListener] previously added by [addValueChangeListener]. */
    public fun removeValueChangeListener(userStyleChangeListener: ValueChangeListener<T>) {
        synchronized(lock) {
            listeners.remove(userStyleChangeListener)
        }
    }
}