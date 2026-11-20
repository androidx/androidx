/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appstate.dataflow

import androidx.appstate.dataflow.concurrent.ReentrantLock
import androidx.appstate.dataflow.concurrent.withLock
import androidx.appstate.transform.DisposableState
import androidx.appstate.transform.RunnableTransformScope
import androidx.compose.runtime.State
import kotlin.concurrent.Volatile
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A registry of [Reducer]s that manages their execution and composition.
 *
 * The registry contains reducers that produce [State] of data that can be obtained via [getState].
 *
 * A registry might contain a parent, used to find data establishing a parent to child relationship
 * between registries.
 *
 * When a registry and reducers are no longer needed invoke [dispose] to stop the composition of
 * active reducers. Once a registry is disposed it can no longer be used.
 */
public class DataflowRegistry
public constructor(
    coroutineContext: CoroutineContext,
    private val parent: DataflowRegistry? = null,
    private val reducerFactories: Map<KClass<out DataKey<*>>, Reducer.Factory<*, *>> = emptyMap(),
) {
    private val coroutineScope = CoroutineScope(coroutineContext.minusKey(Job))
    private val transformScope = RunnableTransformScope()

    private val lock = ReentrantLock()
    private val actives = mutableMapOf<DataKey<*>, ActiveReducer>()

    @Volatile
    internal var isDisposed = false
        private set

    @Volatile private var isTransformStarted = false

    init {
        require(coroutineContext[ContinuationInterceptor] != null) {
            "The provided CoroutineContext must contain a dispatcher."
        }
    }

    /**
     * Gets a state containing data of type [S] from this registry for the given [key].
     *
     * Getting a state from this registry might activate the necessary chain of reducers to produce
     * the data. If this registry has a parent, then the data might be retrieved from the parent, if
     * necessary.
     *
     * The returned [DisposableState] might be shared. Calling [DisposableState.dispose] decrements
     * its reference count in this registry. When all references are disposed, the active reducer is
     * deactivated.
     *
     * @param key The [DataKey] identifying the data type and any runtime arguments.
     * @throws IllegalStateException if a reducer that produces the requested state is not
     *   registered in the chain of registries, or if the chain of registries is already disposed.
     */
    public fun <S : Any> getState(key: DataKey<S>): DisposableState<S> {
        checkIfDisposed()
        lock.withLock {
            runTransformScope()
            val activeReducer = actives[key]
            if (activeReducer != null) {
                activeReducer.references++
                @Suppress("UNCHECKED_CAST")
                return RegistryDisposableState(
                    key = key,
                    registry = this,
                    delegate = activeReducer.disposableState as DisposableState<S>,
                )
            }
            @Suppress("UNCHECKED_CAST")
            val factory = reducerFactories[key::class] as? Reducer.Factory<DataKey<S>, S>
            if (factory != null) {
                val newReducer = createActiveReducer(key, factory)
                actives[key] = newReducer
                newReducer.references++
                @Suppress("UNCHECKED_CAST")
                return RegistryDisposableState(
                    key = key,
                    registry = this,
                    delegate = newReducer.disposableState as DisposableState<S>,
                )
            }
            if (parent != null) {
                return parent.getState(key)
            }
            error("No reducer for '$key' was found in the registry.")
        }
    }

    private fun <S : Any> createActiveReducer(
        key: DataKey<S>,
        factory: Reducer.Factory<DataKey<S>, S>,
    ): ActiveReducer {
        val reducer = factory.create()
        val state =
            transformScope.launchTransform(reducer.initialValue) {
                // TODO: Support SavedStateContainer
                reducer.reduce(key)
            }
        return ActiveReducer(reducer, state)
    }

    private fun releaseActive(key: DataKey<*>) {
        lock.withLock {
            val active = actives[key] ?: return@withLock
            active.references--
            if (active.references <= 0) {
                actives.remove(key)
                active.disposableState.dispose()
            }
        }
    }

    private fun runTransformScope() {
        if (isTransformStarted) {
            return
        }
        isTransformStarted = true
        coroutineScope.launch { transformScope.run() }
    }

    /**
     * Disposes this registry, cancelling the execution of any active reducers.
     *
     * Once a registry is disposed of it should no longer be used.
     */
    public fun dispose() {
        if (isDisposed) {
            return
        }
        isDisposed = true
        lock.withLock {
            coroutineScope.cancel()
            actives.values.forEach { it.disposableState.dispose() }
            actives.clear()
        }
    }

    private fun checkIfDisposed() {
        if (isDisposed) {
            error("Registry is closed")
        }
        if (parent?.isDisposed == true) {
            error("Parent registry is closed")
        }
    }

    // Represents an active reducer in this registry.
    private class ActiveReducer(
        val reducer: Reducer<*, Any>,
        val disposableState: DisposableState<out Any>,
    ) {
        var references: Int = 0
    }

    // Represents a reference counted disposable state from this registry.
    private class RegistryDisposableState<T : Any>(
        private val key: DataKey<*>,
        private val registry: DataflowRegistry,
        private val delegate: DisposableState<T>,
    ) : DisposableState<T>, State<T> by delegate {
        @Volatile private var isDisposed = false

        override fun dispose() {
            if (!isDisposed) {
                isDisposed = true
                registry.releaseActive(key)
            }
        }
    }
}
