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

package androidx.compose.runtime.internal

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.RecomposeScopeImpl
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.RememberObserverHolder
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.Stack
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.composer.RememberManager
import androidx.compose.runtime.debugRuntimeCheck
import androidx.compose.runtime.tooling.CompositionErrorContext

/**
 * Used as a placeholder for paused compositions to ensure the remembers are dispatch in the correct
 * order. While the paused composition is resuming all remembered objects are placed into the this
 * classes list instead of the main list. As remembers are dispatched, this will dispatch remembers
 * to the object remembered in the paused composition's content in the order that they would have
 * been dispatched had the composition not been paused.
 */
internal class PausedCompositionRemembers(private val abandoning: MutableSet<RememberObserver>) :
    RememberObserver {
    val pausedRemembers = mutableVectorOf<RememberObserverHolder>()

    override fun onRemembered() {
        pausedRemembers.forEach {
            val wrapped = it.wrapped
            abandoning.remove(wrapped)
            wrapped.onRemembered()
        }
    }

    // These are never called
    override fun onForgotten() {}

    override fun onAbandoned() {}
}

/** Helper for collecting remember observers for later strictly ordered dispatch. */
internal class RememberEventDispatcher() : RememberManager {
    private var abandoning: MutableSet<RememberObserver>? = null
    private var traceContext: CompositionErrorContext? = null
    private val remembering = mutableVectorOf<RememberObserverHolder>()
    private var rememberSet = mutableScatterSetOf<RememberObserverHolder>()
    private var currentRememberingList = remembering
    private val leaving = mutableVectorOf<Any>()
    private val sideEffects = mutableVectorOf<() -> Unit>()
    private var releasing: MutableScatterSet<ComposeNodeLifecycleCallback>? = null
    private var pausedPlaceholders:
        MutableScatterMap<RecomposeScopeImpl, PausedCompositionRemembers>? =
        null
    private var nestedRemembersLists: Stack<MutableVector<RememberObserverHolder>>? = null
    private var ignoreLeavingSet: ScatterSet<RememberObserverHolder>? = null

    fun prepare(abandoning: MutableSet<RememberObserver>, traceContext: CompositionErrorContext?) {
        clear()
        this.abandoning = abandoning
        this.traceContext = traceContext
    }

    inline fun use(
        abandoning: MutableSet<RememberObserver>,
        traceContext: CompositionErrorContext?,
        block: RememberEventDispatcher.() -> Unit,
    ) {
        try {
            prepare(abandoning, traceContext)
            this.block()
        } finally {
            clear()
        }
    }

    fun clear() {
        this.abandoning = null
        this.traceContext = null
        this.remembering.clear()
        this.rememberSet.clear()
        this.currentRememberingList = remembering
        this.leaving.clear()
        this.sideEffects.clear()
        this.releasing = null
        this.pausedPlaceholders = null
        this.nestedRemembersLists = null
    }

    override fun remembering(instance: RememberObserverHolder) {
        currentRememberingList.add(instance)
        rememberSet.add(instance)
    }

    override fun forgetting(instance: RememberObserverHolder) {
        if (instance in rememberSet) {
            rememberSet.remove(instance)
            val removed = currentRememberingList.remove(instance) || remembering.remove(instance)
            if (!removed) {
                // The instance must be in a nested paused composition.
                fun removeFrom(vector: MutableVector<RememberObserverHolder>): Boolean {
                    vector.forEach { holder ->
                        val nested = holder.wrapped
                        if (nested is PausedCompositionRemembers) {
                            val remembers = nested.pausedRemembers
                            if (remembers.remove(instance)) return true
                            if (removeFrom(remembers)) return true
                        }
                    }
                    return false
                }
                val result = removeFrom(remembering)
                debugRuntimeCheck(result) {
                    "The instance $instance(${instance.wrapped} is in the current remember set " +
                        " but it could not be found to be removed"
                }
            }
            val abandoning = abandoning ?: return
            abandoning.add(instance.wrapped)
        }
        val ignoreSet = ignoreLeavingSet
        if (ignoreSet == null || instance !in ignoreSet) {
            recordLeaving(instance)
        }
    }

    override fun sideEffect(effect: () -> Unit) {
        sideEffects += effect
    }

    override fun deactivating(instance: ComposeNodeLifecycleCallback) {
        recordLeaving(instance)
    }

    override fun releasing(instance: ComposeNodeLifecycleCallback) {
        val releasing =
            releasing ?: mutableScatterSetOf<ComposeNodeLifecycleCallback>().also { releasing = it }

        releasing += instance
        recordLeaving(instance)
    }

    override fun rememberPausingScope(scope: RecomposeScopeImpl) {
        val abandoning = abandoning ?: return
        val pausedPlaceholder = PausedCompositionRemembers(abandoning)
        (pausedPlaceholders
            ?: mutableScatterMapOf<RecomposeScopeImpl, PausedCompositionRemembers>().also {
                pausedPlaceholders = it
            })[scope] = pausedPlaceholder
        this.currentRememberingList.add(RememberObserverHolder(pausedPlaceholder, after = null))
    }

    override fun startResumingScope(scope: RecomposeScopeImpl) {
        val placeholder = pausedPlaceholders?.get(scope)
        if (placeholder != null) {
            (nestedRemembersLists
                    ?: Stack<MutableVector<RememberObserverHolder>>().also {
                        nestedRemembersLists = it
                    })
                .push(currentRememberingList)
            currentRememberingList = placeholder.pausedRemembers
        }
    }

    override fun endResumingScope(scope: RecomposeScopeImpl) {
        val pausedPlaceholders = pausedPlaceholders
        if (pausedPlaceholders != null) {
            val placeholder = pausedPlaceholders[scope]
            if (placeholder != null) {
                nestedRemembersLists?.pop()?.let { currentRememberingList = it }
                pausedPlaceholders.remove(scope)
            }
        }
    }

    fun dispatchRememberObservers() {
        val abandoning = abandoning ?: return
        ignoreLeavingSet = null

        // Send forgets and node callbacks
        if (leaving.isNotEmpty()) {
            trace("Compose:onForgotten") {
                val releasing = releasing
                for (i in leaving.size - 1 downTo 0) {
                    val instance = leaving[i]
                    withComposeStackTrace(instance) {
                        if (instance is RememberObserverHolder) {
                            val wrapped = instance.wrapped
                            abandoning.remove(wrapped)
                            wrapped.onForgotten()
                        }
                        if (instance is ComposeNodeLifecycleCallback) {
                            // node callbacks are in the same queue as forgets to ensure ordering
                            if (releasing != null && instance in releasing) {
                                instance.onRelease()
                            } else {
                                instance.onDeactivate()
                            }
                        }
                    }
                }
            }
        }

        // Send remembers
        if (remembering.isNotEmpty()) {
            trace("Compose:onRemembered") { dispatchRememberList(remembering) }
        }
    }

    fun dispatchOnDeactivateIfNecessary(instance: ComposeNodeLifecycleCallback) {
        val removed = leaving.remove(instance)
        if (removed) {
            instance.onDeactivate()
        }
    }

    fun ignoreForgotten(ignoreSet: ScatterSet<RememberObserverHolder>) {
        ignoreLeavingSet = ignoreSet
    }

    fun extractRememberSet(): ScatterSet<RememberObserverHolder>? =
        if (rememberSet.isNotEmpty()) {
            rememberSet.also {
                rememberSet = mutableScatterSetOf()
                remembering.clear()
            }
        } else null

    private fun dispatchRememberList(list: MutableVector<RememberObserverHolder>) {
        val abandoning = abandoning ?: return
        list.forEach { instance ->
            val wrapped = instance.wrapped
            abandoning.remove(wrapped)
            withComposeStackTrace(instance) { wrapped.onRemembered() }
        }
    }

    fun dispatchSideEffects() {
        if (sideEffects.isNotEmpty()) {
            trace("Compose:sideeffects") {
                sideEffects.forEach { sideEffect -> sideEffect() }
                sideEffects.clear()
            }
        }
    }

    fun dispatchAbandons() {
        val abandoning = abandoning ?: return
        if (abandoning.isNotEmpty()) {
            trace("Compose:abandons") {
                val iterator = abandoning.iterator()
                // remove elements one by one to ensure that abandons will not be dispatched
                // second time in case [onAbandoned] throws.
                while (iterator.hasNext()) {
                    val instance = iterator.next()
                    iterator.remove()
                    instance.onAbandoned()
                }
            }
        }
    }

    private fun recordLeaving(instance: Any) {
        leaving.add(instance)
    }

    private inline fun <T> withComposeStackTrace(instance: Any, block: () -> T): T =
        try {
            block()
        } catch (e: Throwable) {
            throw e.also { traceContext?.apply { e.attachComposeStackTrace(instance) } }
        }
}
