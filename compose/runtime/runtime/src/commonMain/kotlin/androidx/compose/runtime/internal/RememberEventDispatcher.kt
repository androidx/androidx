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

import androidx.collection.MutableIntList
import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.RecomposeScopeImpl
import androidx.compose.runtime.RememberManager
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stack
import androidx.compose.runtime.snapshots.fastForEach

/**
 * Used as a placeholder for paused compositions to ensure the remembers are dispatch in the correct
 * order. While the paused composition is resuming all remembered objects are placed into the this
 * classes list instead of the main list. As remembers are dispatched, this will dispatch remembers
 * to the object remembered in the paused composition's content in the order that they would have
 * been dispatched had the composition not been paused.
 */
internal class PausedCompositionRemembers(private val abandoning: MutableSet<RememberObserver>) :
    RememberObserver {
    val pausedRemembers = mutableListOf<RememberObserver>()

    override fun onRemembered() {
        pausedRemembers.fastForEach {
            abandoning.remove(it)
            it.onRemembered()
        }
    }

    // These are never called
    override fun onForgotten() {}

    override fun onAbandoned() {}
}

/** Helper for collecting remember observers for later strictly ordered dispatch. */
internal class RememberEventDispatcher(private val abandoning: MutableSet<RememberObserver>) :
    RememberManager {
    private val remembering = mutableListOf<RememberObserver>()
    private var currentRememberingList = remembering
    private val leaving = mutableListOf<Any>()
    private val sideEffects = mutableListOf<() -> Unit>()
    private var releasing: MutableScatterSet<ComposeNodeLifecycleCallback>? = null
    private var pausedPlaceholders:
        MutableScatterMap<RecomposeScopeImpl, PausedCompositionRemembers>? =
        null
    private val pending = mutableListOf<Any>()
    private val priorities = MutableIntList()
    private val afters = MutableIntList()
    private var nestedRemembersLists: Stack<MutableList<RememberObserver>>? = null

    override fun remembering(instance: RememberObserver) {
        currentRememberingList.add(instance)
    }

    override fun forgetting(
        instance: RememberObserver,
        endRelativeOrder: Int,
        priority: Int,
        endRelativeAfter: Int
    ) {
        recordLeaving(instance, endRelativeOrder, priority, endRelativeAfter)
    }

    override fun sideEffect(effect: () -> Unit) {
        sideEffects += effect
    }

    override fun deactivating(
        instance: ComposeNodeLifecycleCallback,
        endRelativeOrder: Int,
        priority: Int,
        endRelativeAfter: Int
    ) {
        recordLeaving(instance, endRelativeOrder, priority, endRelativeAfter)
    }

    override fun releasing(
        instance: ComposeNodeLifecycleCallback,
        endRelativeOrder: Int,
        priority: Int,
        endRelativeAfter: Int
    ) {
        val releasing =
            releasing ?: mutableScatterSetOf<ComposeNodeLifecycleCallback>().also { releasing = it }

        releasing += instance
        recordLeaving(instance, endRelativeOrder, priority, endRelativeAfter)
    }

    override fun rememberPausingScope(scope: RecomposeScopeImpl) {
        val pausedPlaceholder = PausedCompositionRemembers(abandoning)
        (pausedPlaceholders
            ?: mutableScatterMapOf<RecomposeScopeImpl, PausedCompositionRemembers>().also {
                pausedPlaceholders = it
            })[scope] = pausedPlaceholder
        this.currentRememberingList.add(pausedPlaceholder)
    }

    override fun startResumingScope(scope: RecomposeScopeImpl) {
        val placeholder = pausedPlaceholders?.get(scope)
        if (placeholder != null) {
            (nestedRemembersLists
                    ?: Stack<MutableList<RememberObserver>>().also { nestedRemembersLists = it })
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
        // Add any pending out-of-order forgotten objects
        processPendingLeaving(Int.MIN_VALUE)

        // Send forgets and node callbacks
        if (leaving.isNotEmpty()) {
            trace("Compose:onForgotten") {
                val releasing = releasing
                for (i in leaving.size - 1 downTo 0) {
                    val instance = leaving[i]
                    if (instance is RememberObserver) {
                        abandoning.remove(instance)
                        instance.onForgotten()
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

        // Send remembers
        if (remembering.isNotEmpty()) {
            trace("Compose:onRemembered") { dispatchRememberList(remembering) }
        }
    }

    private fun dispatchRememberList(list: List<RememberObserver>) {
        list.fastForEach { instance ->
            abandoning.remove(instance)
            instance.onRemembered()
        }
    }

    fun dispatchSideEffects() {
        if (sideEffects.isNotEmpty()) {
            trace("Compose:sideeffects") {
                sideEffects.fastForEach { sideEffect -> sideEffect() }
                sideEffects.clear()
            }
        }
    }

    fun dispatchAbandons() {
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

    private fun recordLeaving(
        instance: Any,
        endRelativeOrder: Int,
        priority: Int,
        endRelativeAfter: Int
    ) {
        processPendingLeaving(endRelativeOrder)
        if (endRelativeAfter in 0 until endRelativeOrder) {
            pending.add(instance)
            priorities.add(priority)
            afters.add(endRelativeAfter)
        } else {
            leaving.add(instance)
        }
    }

    private fun processPendingLeaving(endRelativeOrder: Int) {
        if (pending.isNotEmpty()) {
            var index = 0
            var toAdd: MutableList<Any>? = null
            var toAddAfter: MutableIntList? = null
            var toAddPriority: MutableIntList? = null
            while (index < afters.size) {
                if (endRelativeOrder <= afters[index]) {
                    val instance = pending.removeAt(index)
                    val endRelativeAfter = afters.removeAt(index)
                    val priority = priorities.removeAt(index)

                    if (toAdd == null) {
                        toAdd = mutableListOf(instance)
                        toAddAfter = MutableIntList().also { it.add(endRelativeAfter) }
                        toAddPriority = MutableIntList().also { it.add(priority) }
                    } else {
                        toAddPriority as MutableIntList
                        toAddAfter as MutableIntList
                        toAdd.add(instance)
                        toAddAfter.add(endRelativeAfter)
                        toAddPriority.add(priority)
                    }
                } else {
                    index++
                }
            }
            if (toAdd != null) {
                toAddPriority as MutableIntList
                toAddAfter as MutableIntList

                // Sort the list into [after, -priority] order where it is ordered by after
                // in ascending order as the primary key and priority in descending order as
                // secondary key.

                // For example if remember occurs after a child group it must be added after
                // all the remembers of the child. This is reported with an after which is the
                // slot index of the child's last slot. As this slot might be at the same
                // location as where its parents ends this would be ambiguous which should
                // first if both the two groups request a slot to be after the same slot.
                // Priority is used to break the tie here which is the group index of the group
                // which is leaving. Groups that are lower must be added before the parent's
                // remember when they have the same after.

                // The sort must be stable as as consecutive remembers in the same group after
                // the same child will have the same after and priority.

                // A selection sort is used here because it is stable and the groups are
                // typically very short so this quickly exit list of one and not loop for
                // for sizes of 2. As the information is split between three lists, to
                // reduce allocations, [MutableList.sort] cannot be used as it doesn't have
                // an option to supply a custom swap.
                for (i in 0 until toAdd.size - 1) {
                    for (j in i + 1 until toAdd.size) {
                        val iAfter = toAddAfter[i]
                        val jAfter = toAddAfter[j]
                        if (
                            iAfter < jAfter ||
                                (jAfter == iAfter && toAddPriority[i] < toAddPriority[j])
                        ) {
                            toAdd.swap(i, j)
                            toAddPriority.swap(i, j)
                            toAddAfter.swap(i, j)
                        }
                    }
                }
                leaving.addAll(toAdd)
            }
        }
    }
}

private fun <T> MutableList<T>.swap(a: Int, b: Int) {
    val item = this[a]
    this[a] = this[b]
    this[b] = item
}

private fun MutableIntList.swap(a: Int, b: Int) {
    val item = this[a]
    this[a] = this[b]
    this[b] = item
}
