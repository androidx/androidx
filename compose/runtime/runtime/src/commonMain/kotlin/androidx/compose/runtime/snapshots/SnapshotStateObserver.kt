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

package androidx.compose.runtime.snapshots

import androidx.collection.MutableObjectIntMap
import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.compose.runtime.AtomicReference
import androidx.compose.runtime.DerivedState
import androidx.compose.runtime.DerivedStateObserver
import androidx.compose.runtime.TestOnly
import androidx.compose.runtime.collection.ScopeMap
import androidx.compose.runtime.collection.fastForEach
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.composeRuntimeError
import androidx.compose.runtime.currentThreadId
import androidx.compose.runtime.currentThreadName
import androidx.compose.runtime.observeDerivedStateRecalculations
import androidx.compose.runtime.requirePrecondition
import androidx.compose.runtime.structuralEqualityPolicy

/**
 * Helper class to efficiently observe snapshot state reads. See [observeReads] for more details.
 *
 * NOTE: This class is not thread-safe, so implementations should not reuse observer between
 * different threads to avoid race conditions.
 */
@Suppress("NotCloseable") // we can't implement AutoCloseable from commonMain
class SnapshotStateObserver(private val onChangedExecutor: (callback: () -> Unit) -> Unit) {
    private val pendingChanges = AtomicReference<Any?>(null)
    private var sendingNotifications = false

    private val applyObserver: (Set<Any>, Snapshot) -> Unit = { applied, _ ->
        addChanges(applied)
        if (drainChanges()) sendNotifications()
    }

    /**
     * Drain the pending changes from the pending changes queue invalidating any scope maps that
     * contain objects in any of the sets. Return true if changes were found.
     *
     * This immediately returns with false if notifications are already being sent. It is the
     * responsibility of any user of this function to ensure that the queue is re-checked after
     * dispatching notifications.
     */
    private fun drainChanges(): Boolean {
        // Don't modify the scope maps while notifications are being sent either by the caller or
        // on another thread
        if (synchronized(observedScopeMaps) { sendingNotifications }) return false

        // Remove all pending changes and return true if any of the objects are observed
        var hasValues = false
        while (true) {
            val notifications = removeChanges() ?: return hasValues
            forEachScopeMap { scopeMap ->
                hasValues = scopeMap.recordInvalidation(notifications) || hasValues
            }
        }
    }

    /**
     * Send any pending notifications. Uses [onChangedExecutor] to schedule this work.
     *
     * This method should only be called if, and only if, a call to `drainChanges()` returns
     * `true`.
     */
    private fun sendNotifications() {
        onChangedExecutor {
            while (true) {
                synchronized(observedScopeMaps) {
                    if (!sendingNotifications) {
                        sendingNotifications = true
                        try {
                            observedScopeMaps.forEach { scopeMap ->
                                scopeMap.notifyInvalidatedScopes()
                            }
                        } finally {
                            sendingNotifications = false
                        }
                    }
                }

                // If any changes arrived while we were notifying, send the new changes.
                if (!drainChanges()) break
            }
        }
    }

    /**
     * Add changes to the changes queue. This uses an atomic reference as a queue to minimize the
     * number of allocations required in the normal case. If, for example, only one set is added to
     * the queue, the set itself is the atomic reference. If the queue is empty the reference is
     * null. Only if there are more than one set added to the queue is an allocation required, then
     * the atomic reference is a list containing all the sets in the queue. Given the size of the
     * queue, the type of object referenced is,
     *   0 -> null
     *   1 -> Set<Any?>
     *   2 or more -> List<Set<Any?>>
     */
    private fun addChanges(set: Set<Any>) {
        while (true) {
            val old = pendingChanges.get()
            val new = when (old) {
                null -> set
                is Set<*> -> listOf(old, set)
                is List<*> -> old + listOf(set)
                else -> report()
            }
            if (pendingChanges.compareAndSet(old, new)) break
        }
    }

    /**
     * Remove a set of changes from the change queue. See [addChanges] for a description of how
     * this queue works.
     */
    @Suppress("UNCHECKED_CAST")
    private fun removeChanges(): Set<Any>? {
        while (true) {
            val old = pendingChanges.get()
            var result: Set<Any>?
            var new: Any?
            when (old) {
                null -> return null // The queue is empty
                is Set<*> -> {
                    result = old as Set<Any>?
                    new = null
                }
                is List<*> -> {
                    result = old[0] as Set<Any>?
                    new = when {
                        old.size == 2 -> old[1]
                        old.size > 2 -> old.subList(1, old.size)
                        else -> null
                    }
                }
                else -> report()
            }
            if (pendingChanges.compareAndSet(old, new)) {
                return result
            }
        }
    }

    private fun report(): Nothing = composeRuntimeError("Unexpected notification")

    /**
     * The observer used by this [SnapshotStateObserver] during [observeReads].
     */
    private val readObserver: (Any) -> Unit = { state ->
        if (!isPaused) {
            synchronized(observedScopeMaps) {
                currentMap!!.recordRead(state)
            }
        }
    }

    /**
     * List of all [ObservedScopeMap]s. When [observeReads] is called, there will be a
     * [ObservedScopeMap] associated with its [ObservedScopeMap.onChanged] callback in this list.
     * The list only grows.
     */
    private val observedScopeMaps = mutableVectorOf<ObservedScopeMap>()

    /**
     * Helper for synchronized iteration over [observedScopeMaps]. All observed reads should
     * happen on the same thread, but snapshots can be applied on a different thread, requiring
     * synchronization.
     */
    private inline fun forEachScopeMap(block: (ObservedScopeMap) -> Unit) {
        synchronized(observedScopeMaps) {
            observedScopeMaps.forEach(block)
        }
    }

    private inline fun removeScopeMapIf(block: (ObservedScopeMap) -> Boolean) {
        synchronized(observedScopeMaps) {
            observedScopeMaps.removeIf(block)
        }
    }

    /**
     * Method to call when unsubscribing from the apply observer.
     */
    private var applyUnsubscribe: ObserverHandle? = null

    /**
     * `true` when [withNoObservations] is called and read observations should not
     * be considered invalidations for the current scope.
     */
    private var isPaused = false

    /**
     * The [ObservedScopeMap] that should be added to when a model is read during [observeReads].
     */
    private var currentMap: ObservedScopeMap? = null

    /**
     * Thread id that has set the [currentMap]
     */
    private var currentMapThreadId = -1L

    /**
     * Executes [block], observing state object reads during its execution.
     *
     * The [scope] and [onValueChangedForScope] are associated with any values that are read so
     * that when those values change, [onValueChangedForScope] will be called with the [scope]
     * parameter.
     *
     * Observation can be paused with [Snapshot.withoutReadObservation].
     *
     * @param scope value associated with the observed scope.
     * @param onValueChangedForScope is called with the [scope] when value read within [block]
     * has been changed. For repeated observations, it is more performant to pass the same instance
     * of the callback, as [observedScopeMaps] grows with each new callback instance.
     * @param block to observe reads within.
     */
    fun <T : Any> observeReads(scope: T, onValueChangedForScope: (T) -> Unit, block: () -> Unit) {
        val scopeMap = synchronized(observedScopeMaps) {
            ensureMap(onValueChangedForScope)
        }

        val oldPaused = isPaused
        val oldMap = currentMap
        val oldThreadId = currentMapThreadId

        if (oldThreadId != -1L) {
            requirePrecondition(oldThreadId == currentThreadId()) {
                "Detected multithreaded access to SnapshotStateObserver: " +
                    "previousThreadId=$oldThreadId), " +
                    "currentThread={id=${currentThreadId()}, name=${currentThreadName()}}. " +
                    "Note that observation on multiple threads in layout/draw is not supported. " +
                    "Make sure your measure/layout/draw for each Owner (AndroidComposeView) " +
                    "is executed on the same thread."
            }
        }

        try {
            isPaused = false
            currentMap = scopeMap
            @Suppress("deprecation") // b/317114874
            currentMapThreadId = Thread.currentThread().id

            scopeMap.observe(scope, readObserver, block)
        } finally {
            currentMap = oldMap
            isPaused = oldPaused
            currentMapThreadId = oldThreadId
        }
    }

    /**
     * Stops observing state object reads while executing [block]. State object reads may be
     * restarted by calling [observeReads] inside [block].
     */
    @Deprecated(
        "Replace with Snapshot.withoutReadObservation()",
        ReplaceWith(
            "Snapshot.withoutReadObservation(block)",
            "androidx.compose.runtime.snapshots.Snapshot"
        )
    )
    fun withNoObservations(block: () -> Unit) {
        val oldPaused = isPaused
        isPaused = true
        try {
            block()
        } finally {
            isPaused = oldPaused
        }
    }

    /**
     * Clears all state read observations for a given [scope]. This clears values for all
     * `onValueChangedForScope` callbacks passed in [observeReads].
     */
    fun clear(scope: Any) {
        removeScopeMapIf {
            it.clearScopeObservations(scope)
            !it.hasScopeObservations()
        }
    }

    /**
     * Remove observations using [predicate] to identify scopes to be removed. This is
     * used when a scope is no longer in the hierarchy and should not receive any callbacks.
     */
    fun clearIf(predicate: (scope: Any) -> Boolean) {
        removeScopeMapIf { scopeMap ->
            scopeMap.removeScopeIf(predicate)
            !scopeMap.hasScopeObservations()
        }
    }

    /**
     * Starts watching for state commits.
     */
    fun start() {
        applyUnsubscribe = Snapshot.registerApplyObserver(applyObserver)
    }

    /**
     * Stops watching for state commits.
     */
    fun stop() {
        applyUnsubscribe?.dispose()
    }

    /**
     * This method is only used for testing. It notifies that [changes] have been made on
     * [snapshot].
     */
    @TestOnly
    fun notifyChanges(changes: Set<Any>, snapshot: Snapshot) {
        applyObserver(changes, snapshot)
    }

    /**
     * Remove all observations.
     */
    fun clear() {
        forEachScopeMap { scopeMap ->
            scopeMap.clear()
        }
    }

    /**
     * Returns the [ObservedScopeMap] within [observedScopeMaps] associated with [onChanged] or a newly-
     * inserted one if it doesn't exist.
     *
     * Must be called inside a synchronized block.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> ensureMap(onChanged: (T) -> Unit): ObservedScopeMap {
        val scopeMap = observedScopeMaps.firstOrNull { it.onChanged === onChanged }
        if (scopeMap == null) {
            val map = ObservedScopeMap(onChanged as ((Any) -> Unit))
            observedScopeMaps += map
            return map
        }
        return scopeMap
    }

    /**
     * Connects observed values to scopes for each [onChanged] callback.
     */
    @Suppress("UNCHECKED_CAST")
    private class ObservedScopeMap(val onChanged: (Any) -> Unit) {
        /**
         * Currently observed scope.
         */
        private var currentScope: Any? = null

        /**
         * key: State reads observed in current scope.
         * value: [currentToken] at the time the read was observed in.
         */
        private var currentScopeReads: MutableObjectIntMap<Any>? = null

        /**
         * Token for current observation cycle; usually corresponds to snapshot ID at the time when
         * observation started.
         */
        private var currentToken: Int = -1

        /**
         * Values that have been read during the scope's [SnapshotStateObserver.observeReads].
         */
        private val valueToScopes = ScopeMap<Any, Any>()

        /**
         * Reverse index (scope -> values) for faster scope invalidation.
         */
        private val scopeToValues: MutableScatterMap<Any, MutableObjectIntMap<Any>> =
            MutableScatterMap()

        /**
         * Scopes that were invalidated during previous apply step.
         */
        private val invalidated = MutableScatterSet<Any>()

        /**
         * Reusable vector for re-recording states inside [recordInvalidation]
         */
        private val statesToReread = mutableVectorOf<DerivedState<*>>()

        // derived state handling

        /**
         * Observer for derived state recalculation
         */
        val derivedStateObserver = object : DerivedStateObserver {
            override fun start(derivedState: DerivedState<*>) {
                deriveStateScopeCount++
            }

            override fun done(derivedState: DerivedState<*>) {
                deriveStateScopeCount--
            }
        }

        /**
         * Counter for skipping reads inside derived states. If count is > 0, read happens inside
         * a derived state.
         * Reads for derived states are captured separately through
         * [DerivedState.Record.dependencies].
         */
        private var deriveStateScopeCount = 0

        /**
         * Invalidation index from state objects to derived states reading them.
         */
        private val dependencyToDerivedStates = ScopeMap<Any, DerivedState<*>>()

        /**
         * Last derived state value recorded during read.
         */
        private val recordedDerivedStateValues = HashMap<DerivedState<*>, Any?>()

        fun recordRead(value: Any) {
            val scope = currentScope!!
            recordRead(
                value = value,
                currentToken = currentToken,
                currentScope = scope,
                recordedValues = currentScopeReads ?: MutableObjectIntMap<Any>().also {
                    currentScopeReads = it
                    scopeToValues[scope] = it
                }
            )
        }

        /**
         * Record that [value] was read in [currentScope].
         */
        private fun recordRead(
            value: Any,
            currentToken: Int,
            currentScope: Any,
            recordedValues: MutableObjectIntMap<Any>
        ) {
            if (deriveStateScopeCount > 0) {
                // Reads coming from derivedStateOf block
                return
            }

            val previousToken = recordedValues.put(value, currentToken, -1)
            if (value is DerivedState<*> && previousToken != currentToken) {
                val record = value.currentRecord
                // re-read the value before removing dependencies, in case the new value wasn't read
                recordedDerivedStateValues[value] = record.currentValue

                val dependencies = record.dependencies
                val dependencyToDerivedStates = dependencyToDerivedStates

                dependencyToDerivedStates.removeScope(value)
                dependencies.forEachKey { dependency ->
                    if (dependency is StateObjectImpl) {
                        dependency.recordReadIn(ReaderKind.SnapshotStateObserver)
                    }
                    dependencyToDerivedStates.add(dependency, value)
                }
            }

            if (previousToken == -1) {
                if (value is StateObjectImpl) {
                    value.recordReadIn(ReaderKind.SnapshotStateObserver)
                }
                valueToScopes.add(value, currentScope)
            }
        }

        /**
         * Setup new scope for state read observation, observe them, and cleanup afterwards
         */
        fun observe(scope: Any, readObserver: (Any) -> Unit, block: () -> Unit) {
            val previousScope = currentScope
            val previousReads = currentScopeReads
            val previousToken = currentToken

            currentScope = scope
            currentScopeReads = scopeToValues[scope]
            if (currentToken == -1) {
                currentToken = currentSnapshot().id
            }

            observeDerivedStateRecalculations(derivedStateObserver) {
                Snapshot.observe(readObserver, null, block)
            }

            clearObsoleteStateReads(currentScope!!)

            currentScope = previousScope
            currentScopeReads = previousReads
            currentToken = previousToken
        }

        private fun clearObsoleteStateReads(scope: Any) {
            val currentToken = currentToken
            currentScopeReads?.removeIf { value, token ->
                (token != currentToken).also { willRemove ->
                    if (willRemove) {
                        removeObservation(scope, value)
                    }
                }
            }
        }

        /**
         * Clear observations for [scope].
         */
        fun clearScopeObservations(scope: Any) {
            val recordedValues = scopeToValues.remove(scope) ?: return
            recordedValues.forEach { value, _ ->
                removeObservation(scope, value)
            }
        }

        /**
         * Remove observations in scopes matching [predicate].
         */
        fun removeScopeIf(predicate: (scope: Any) -> Boolean) {
            scopeToValues.removeIf { scope, valueSet ->
                predicate(scope).also { willRemove ->
                    if (willRemove) {
                        valueSet.forEach { value, _ ->
                            removeObservation(scope, value)
                        }
                    }
                }
            }
        }

        fun hasScopeObservations(): Boolean =
            scopeToValues.isNotEmpty()

        private fun removeObservation(scope: Any, value: Any) {
            valueToScopes.remove(value, scope)
            if (value is DerivedState<*> && value !in valueToScopes) {
                dependencyToDerivedStates.removeScope(value)
                recordedDerivedStateValues.remove(value)
            }
        }

        /**
         * Clear all observations.
         */
        fun clear() {
            valueToScopes.clear()
            scopeToValues.clear()
            dependencyToDerivedStates.clear()
            recordedDerivedStateValues.clear()
        }

        /**
         * Record scope invalidation for given set of values.
         * @return whether any scopes observe changed values
         */
        fun recordInvalidation(changes: Set<Any>): Boolean {
            var hasValues = false

            val dependencyToDerivedStates = dependencyToDerivedStates
            val recordedDerivedStateValues = recordedDerivedStateValues
            val valueToScopes = valueToScopes
            val invalidated = invalidated

            changes.fastForEach { value ->
                if (value is StateObjectImpl &&
                    !value.isReadIn(ReaderKind.SnapshotStateObserver)
                ) {
                    return@fastForEach
                }

                if (value in dependencyToDerivedStates) {
                    // Find derived state that is invalidated by this change
                    dependencyToDerivedStates.forEachScopeOf(value) { derivedState ->
                        derivedState as DerivedState<Any?>
                        val previousValue = recordedDerivedStateValues[derivedState]
                        val policy = derivedState.policy ?: structuralEqualityPolicy()

                        // Invalidate only if currentValue is different than observed on read
                        if (!policy.equivalent(
                                derivedState.currentRecord.currentValue,
                                previousValue
                            )
                        ) {
                            valueToScopes.forEachScopeOf(derivedState) { scope ->
                                invalidated.add(scope)
                                hasValues = true
                            }
                        } else {
                            // Re-read state to ensure its dependencies are up-to-date
                            statesToReread.add(derivedState)
                        }
                    }
                }

                valueToScopes.forEachScopeOf(value) { scope ->
                    invalidated.add(scope)
                    hasValues = true
                }
            }

            if (statesToReread.isNotEmpty()) {
                statesToReread.forEach {
                    rereadDerivedState(it)
                }
                statesToReread.clear()
            }

            return hasValues
        }

        fun rereadDerivedState(derivedState: DerivedState<*>) {
            val scopeToValues = scopeToValues
            val token = currentSnapshot().id

            valueToScopes.forEachScopeOf(derivedState) { scope ->
                recordRead(
                    value = derivedState,
                    currentToken = token,
                    currentScope = scope,
                    recordedValues = scopeToValues[scope] ?: MutableObjectIntMap<Any>().also {
                        scopeToValues[scope] = it
                    }
                )
            }
        }

        /**
         * Call [onChanged] for previously invalidated scopes.
         */
        fun notifyInvalidatedScopes() {
            val invalidated = invalidated
            invalidated.forEach(onChanged)
            invalidated.clear()
        }
    }
}
