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

@file:JvmName("SnapshotStateKt")
@file:JvmMultifileClass

package androidx.compose.runtime

import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import androidx.compose.runtime.collection.ScopeMap
import androidx.compose.runtime.platform.makeSynchronizedObject
import androidx.compose.runtime.platform.synchronized
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.fastForEach
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Collects values from this [StateFlow] and represents its latest value via [State]. The
 * [StateFlow.value] is used as an initial value. Every time there would be new value posted into
 * the [StateFlow] the returned [State] will be updated causing recomposition of every [State.value]
 * usage.
 *
 * @sample androidx.compose.runtime.samples.StateFlowSample
 * @param context [CoroutineContext] to use for collecting.
 */
@Suppress("StateFlowValueCalledInComposition")
@Composable
public fun <T> StateFlow<T>.collectAsState(
    context: CoroutineContext = EmptyCoroutineContext
): State<T> = collectAsState(value, context)

/**
 * Collects values from this [Flow] and represents its latest value via [State]. Every time there
 * would be new value posted into the [Flow] the returned [State] will be updated causing
 * recomposition of every [State.value] usage.
 *
 * @sample androidx.compose.runtime.samples.FlowWithInitialSample
 * @param initial the value of the state will have until the first flow value is emitted.
 * @param context [CoroutineContext] to use for collecting.
 */
@Composable
public fun <T : R, R> Flow<T>.collectAsState(
    initial: R,
    context: CoroutineContext = EmptyCoroutineContext,
): State<R> =
    produceState(initial, this, context) {
        if (context == EmptyCoroutineContext) {
            collect { value = it }
        } else withContext(context) { collect { value = it } }
    }

/**
 * Orchestrates the observation of [Snapshot] state for [snapshotFlow]s that are collected on the
 * same thread.
 *
 * Once a [SnapshotFlowManager] is no longer needed, its [dispose] method should be called.
 *
 * It is not safe to share a [SnapshotFlowManager] instance across two [snapshotFlow]s that collect
 * in parallel on two different threads, but it is not a problem for a thread to run apply observers
 * in parallel with another thread collecting a [snapshotFlow].
 *
 * @see snapshotFlow
 */
@ExperimentalComposeRuntimeApi
public class SnapshotFlowManager {
    private var managerImpl: SnapshotFlowManagerImpl? = SingleSubscriptionSnapshotFlowManager()

    /**
     * Returns the result of running [block] and also subscribes [channel] to be notified whenever a
     * change is made to any of the state objects accessed in [block].
     *
     * If there is already a subscription associated with [channel] from a previous invocation of
     * [runAndWatch], it will be cancelled and replaced.
     *
     * If this method is called after this manager has been disposed of, an [IllegalStateException]
     * will be thrown.
     */
    internal fun <T> runAndWatch(channel: SendChannel<Unit>, block: () -> T): T {
        checkPrecondition(managerImpl != null) {
            "Called runAndWatch on a manager that has been disposed of"
        }

        val managerImplRef = managerImpl
        if (managerImplRef is SingleSubscriptionSnapshotFlowManager) {
            val subscribedChannel = managerImplRef.subscribedChannel
            if (subscribedChannel != null && subscribedChannel != channel) {
                // This manager now needs to manage notifications for more than one channel, so we
                // promote the backing object from a [SingleSubscriptionSnapshotFlowManager] to a
                // [MultiSubscriptionSnapshotFlowManager].
                managerImpl = managerImplRef.promote()
            }
        }

        return managerImpl!!.runAndWatch(channel, block)
    }

    /**
     * This must be called by a [snapshotFlow] when it is in the process of exiting to dispose of
     * all subscriptions associated with it.
     *
     * If this method is called after this manager has been disposed of, it will cause no effect.
     */
    internal fun reportSnapshotFlowCancellation(channel: SendChannel<Unit>) {
        managerImpl?.reportSnapshotFlowCancellation(channel)
    }

    /**
     * Disposes of this manager. Disposing of a manager disconnects it from the [Snapshot] system,
     * rendering it incapable of handling any subscriptions.
     *
     * If this method is called after this manager has been disposed of, an [IllegalStateException]
     * will be thrown.
     */
    public fun dispose() {
        managerImpl.let {
            checkPrecondition(it != null) {
                "Called dispose on a manager that has been disposed of"
            }
            it.dispose()
        }
        managerImpl = null
    }
}

/**
 * The methods of [SnapshotFlowManager] are implemented by delegating to an instance of this class.
 */
internal abstract class SnapshotFlowManagerImpl internal constructor() {
    // Used to lock access to the fields read in the apply observers registered by the subclasses,
    // because apply observers can run concurrently with other parts of `snapshotFlow` logic.
    protected val lock = makeSynchronizedObject()

    /**
     * Adds [obj] to the set of objects watched by [channel], meaning that [channel] will be
     * notified of future changes to [obj].
     *
     * The subscription changes resulting from an invocation of this method will not take effect
     * until committed by an invocation of [commitSubscriptionChanges].
     */
    internal abstract fun watch(channel: SendChannel<Unit>, obj: Any)

    /**
     * Returns [watch] with its first argument fixed to [channel] by partial application. That is,
     * [readObserverFor(channel)(obj)] is equivalent to [watch(channel, obj)].
     */
    internal abstract fun readObserverFor(channel: SendChannel<Unit>): (Any) -> Unit

    /**
     * Unsubscribes [channel] from being notified of changes to all objects.
     *
     * The subscription changes resulting from an invocation of this method will not take effect
     * until committed by an invocation of [commitSubscriptionChanges].
     */
    internal abstract fun clearWatchSet(channel: SendChannel<Unit>)

    /** Commits subscription changes made since the last invocation of this method. */
    internal abstract fun commitSubscriptionChanges()

    /**
     * Returns the result of running [block] and also subscribes [channel] to be notified whenever a
     * change is made to any of the state objects accessed in [block].
     *
     * If there is already a subscription associated with [channel] from a previous invocation of
     * [runAndWatch], it will be cancelled and replaced.
     */
    internal fun <T> runAndWatch(channel: SendChannel<Unit>, block: () -> T): T {
        val result =
            Snapshot.takeSnapshot(readObserverFor(channel)).run {
                clearWatchSet(channel)
                try {
                    enter(block)
                } finally {
                    dispose()
                }
            }

        commitSubscriptionChanges()
        return result
    }

    /**
     * This must be called by a [snapshotFlow] when it is in the process of exiting to dispose of
     * all subscriptions associated with it.
     */
    internal fun reportSnapshotFlowCancellation(channel: SendChannel<Unit>) {
        clearWatchSet(channel)
        commitSubscriptionChanges()
    }

    /**
     * Disposes of this manager. Disposing of a manager disconnects it from the [Snapshot] system,
     * rendering it incapable of handling any subscriptions.
     */
    internal abstract fun dispose()
}

/**
 * A [SnapshotFlowManagerImpl] that can manage at most one active subscription.
 *
 * If an instance of [SingleSubscriptionSnapshotFlowManager] is managing an active subscription
 * (i.e. has been passed to an invocation of [snapshotFlow]) and is passed to another invocation of
 * [snapshotFlow], an [IllegalStateException] will be thrown.
 */
private class SingleSubscriptionSnapshotFlowManager : SnapshotFlowManagerImpl() {
    // This variable allows us to avoid allocating [watchSet] until we need to watch more than one
    // object.
    private var soleWatchedObject: Any? = null

    /**
     * A working copy of [soleWatchedObject].
     *
     * Invocations of [watch] and [clearWatchSet] only affect this working copy. Invoking
     * [commitSubscriptionChanges] clones this working copy into [soleWatchedObject].
     */
    private var workingSoleWatchedObject: Any? = null

    /** The set of objects that are being watched for changes. */
    private var watchSet: MutableScatterSet<Any>? = null

    /**
     * A working copy of [watchSet].
     *
     * Invocations of [watch] and [clearWatchSet] only affect this working copy. Invoking
     * [commitSubscriptionChanges] clones this working copy into [watchSet].
     */
    private var workingWatchSet: MutableScatterSet<Any>? = null

    /** The channel that has an active subscription being managed by this manager. */
    var subscribedChannel: SendChannel<Unit>? = null

    // Caches the only valid return value of [readObserverFor].
    private val readObserverCache = { obj: Any -> watch(subscribedChannel!!, obj) }

    private val unregisterApplyObserver =
        Snapshot.registerApplyObserver { changed, _ ->
            var toNotify: SendChannel<Unit>? = null
            synchronized(lock) {
                val watchSet = watchSet
                if (watchSet == null) {
                    if (changed.contains(soleWatchedObject)) {
                        toNotify = subscribedChannel
                    }
                } else {
                    // Assumption: [watchSet] will typically be smaller than [changed].
                    if (watchSet.any { changed.contains(it) }) {
                        toNotify = subscribedChannel
                    }
                }
            }
            toNotify?.trySend(Unit)
        }

    override fun watch(channel: SendChannel<Unit>, obj: Any) {
        checkPrecondition(subscribedChannel == channel) {
            "Requested a SingleSubscriptionSnapshotFlowManager to manage multiple subscriptions"
        }

        val workingWatchSetRef = workingWatchSet
        val workingSoleWatchedObjectRef = workingSoleWatchedObject
        if (workingWatchSetRef == null) {
            if (workingSoleWatchedObjectRef == null) {
                workingSoleWatchedObject = obj
            } else {
                workingWatchSet =
                    mutableScatterSetOf<Any>().also {
                        it.add(workingSoleWatchedObjectRef)
                        it.add(obj)
                    }
                workingSoleWatchedObject = null
            }
        } else {
            checkPrecondition(
                workingSoleWatchedObject == null,
                { "workingSoleWatchedObject must be null when workingWatchSet is non-null" },
            )
            workingWatchSetRef.add(obj)
        }
    }

    override fun readObserverFor(channel: SendChannel<Unit>): (Any) -> Unit {
        checkPrecondition(subscribedChannel == null || subscribedChannel == channel) {
            "Requested a SingleSubscriptionSnapshotFlowManager to manage multiple subscriptions"
        }

        subscribedChannel = channel

        return readObserverCache
    }

    private fun clearWatchSetImpl() {
        workingSoleWatchedObject = null
        workingWatchSet = null
    }

    override fun clearWatchSet(channel: SendChannel<Unit>) = clearWatchSetImpl()

    override fun commitSubscriptionChanges() {
        synchronized(lock) {
            soleWatchedObject = workingSoleWatchedObject

            if (workingWatchSet == null) {
                watchSet = null
            } else {
                if (watchSet == null) {
                    watchSet = mutableScatterSetOf()
                }
                val temp = watchSet
                watchSet = workingWatchSet
                workingWatchSet = temp
            }
        }
    }

    override fun dispose() {
        unregisterApplyObserver.dispose()
        clearWatchSetImpl()
        synchronized(lock) {
            subscribedChannel = null
            soleWatchedObject = null
            watchSet = null
        }
    }

    /**
     * Transfers all subscriptions being managed by this manager to a new
     * [MultiSubscriptionSnapshotFlowManager], disposes of this manager, and returns the new
     * manager.
     */
    fun promote(): MultiSubscriptionSnapshotFlowManager {
        val multiSubscriptionManager =
            MultiSubscriptionSnapshotFlowManager().also {
                val subscribedChannel = subscribedChannel
                checkPrecondition(subscribedChannel != null) {
                    "promote must only be called when a manager is managing subscriptions for " +
                        "one channel and needs to start managing them for a second"
                }
                val watchSet = watchSet
                if (watchSet == null) {
                    it.watch(subscribedChannel, soleWatchedObject!!)
                } else {
                    watchSet.forEach { obj -> it.watch(subscribedChannel, obj) }
                }
                it.commitSubscriptionChanges()
            }

        dispose()
        return multiSubscriptionManager
    }
}

/**
 * A [SnapshotFlowManagerImpl] that can manage an unbounded number of active subscriptions.
 *
 * This class is not thread-safe.
 */
private class MultiSubscriptionSnapshotFlowManager : SnapshotFlowManagerImpl() {
    private sealed interface SubscriptionChange

    private class Add(val obj: Any, val channel: SendChannel<Unit>) : SubscriptionChange

    private class RemoveScope(val channel: SendChannel<Unit>) : SubscriptionChange

    /**
     * A map from snapshot state objects to the channels that are subscribed to be notified of
     * changes to those objects.
     */
    private var subscriptions: ScopeMap<Any, SendChannel<Unit>> = ScopeMap()

    /**
     * A list of changes that will be applied to [subscriptions] the next time
     * [commitSubscriptionChanges] is invoked.
     */
    private val pendingChanges = mutableListOf<SubscriptionChange>()

    /** Used by the apply observer to keep track of the channels that it needs to notify. */
    private val toNotify = mutableScatterSetOf<SendChannel<Unit>>()

    // Used by [readObserverFor] to cache partially applied functions.
    private val readObserverCache = mutableScatterMapOf<SendChannel<Unit>, (Any) -> Unit>()

    private val unregisterApplyObserver =
        Snapshot.registerApplyObserver { changed, _ ->
            synchronized(lock) {
                // Assumption: there will typically be fewer keys in [subscriptions] than elements
                // in [changed].
                subscriptions.forEachKey { key ->
                    if (changed.contains(key)) {
                        subscriptions.forEachScopeOf(key) { toNotify.add(it) }
                    }
                }

                toNotify.forEach { it.trySend(Unit) }
                toNotify.clear()
            }
        }

    override fun watch(channel: SendChannel<Unit>, obj: Any) {
        pendingChanges.add(Add(obj, channel))
    }

    override fun readObserverFor(channel: SendChannel<Unit>): (Any) -> Unit {
        return readObserverCache.get(channel)
            ?: { obj: Any -> watch(channel, obj) }.also { readObserverCache.put(channel, it) }
    }

    override fun clearWatchSet(channel: SendChannel<Unit>) {
        pendingChanges.add(RemoveScope(channel))
    }

    override fun commitSubscriptionChanges() {
        synchronized(lock) {
            pendingChanges.fastForEach {
                when (it) {
                    is Add -> {
                        subscriptions.add(it.obj, it.channel)
                    }
                    is RemoveScope -> {
                        subscriptions.removeScope(it.channel)
                    }
                }
            }
        }
        pendingChanges.clear()
    }

    override fun dispose() {
        unregisterApplyObserver.dispose()
        pendingChanges.clear()
        readObserverCache.clear()
        synchronized(lock) { subscriptions.clear() }
    }
}

@OptIn(ExperimentalComposeRuntimeApi::class)
private fun <T> snapshotFlowImpl(externalManager: SnapshotFlowManager?, block: () -> T): Flow<T> =
    flow {
        val manager = externalManager ?: SnapshotFlowManager()

        val needToRerunBlock = Channel<Unit>(1)

        try {
            var lastValue = manager.runAndWatch(needToRerunBlock, block)
            emit(lastValue)

            while (true) {
                needToRerunBlock.receive()

                val newValue = manager.runAndWatch(needToRerunBlock, block)
                if (newValue != lastValue) {
                    lastValue = newValue
                    emit(newValue)
                }
            }
        } finally {
            manager.reportSnapshotFlowCancellation(needToRerunBlock)
            if (externalManager == null) {
                // If [manager] was not supplied by the user, it must be disposed of here.
                manager.dispose()
            }
        }
    }

/**
 * Create a [Flow] from observable [Snapshot] state. (e.g. state holders returned by
 * [mutableStateOf][androidx.compose.runtime.mutableStateOf].)
 *
 * [snapshotFlow] creates a [Flow] that runs [block] when collected and emits the result, recording
 * any snapshot state that was accessed. While collection continues, if a new [Snapshot] is applied
 * that changes state accessed by [block], the flow will run [block] again, re-recording the
 * snapshot state that was accessed. If the result of [block] is not [equal to][Any.equals] the
 * previous result, the flow will emit that new result. (This behavior is similar to that of
 * [Flow.distinctUntilChanged][kotlinx.coroutines.flow.distinctUntilChanged].) Collection will
 * continue indefinitely unless it is explicitly cancelled or limited by the use of other [Flow]
 * operators.
 *
 * @sample androidx.compose.runtime.samples.snapshotFlowSample
 *
 * [block] is run in a **read-only** [Snapshot] and may not modify snapshot data. If [block]
 * attempts to modify snapshot data, flow collection will fail with [IllegalStateException].
 *
 * [block] may run more than once for equal sets of inputs or only once after many rapid snapshot
 * changes; it should be idempotent and free of side effects.
 *
 * When working with [Snapshot] state it is useful to keep the distinction between **events** and
 * **state** in mind. [snapshotFlow] models snapshot changes as events, but events **cannot** be
 * effectively modeled as observable state. Observable state is a lossy compression of the events
 * that produced that state.
 *
 * An observable **event** happens at a point in time and is discarded. All registered observers at
 * the time the event occurred are notified. All individual events in a stream are assumed to be
 * relevant and may build on one another; repeated equal events have meaning and therefore a
 * registered observer must observe all events without skipping.
 *
 * Observable **state** raises change events when the state changes from one value to a new, unequal
 * value. State change events are **conflated;** only the most recent state matters. Observers of
 * state changes must therefore be **idempotent;** given the same state value the observer should
 * produce the same result. It is valid for a state observer to both skip intermediate states as
 * well as run multiple times for the same state and the result should be the same.
 */
@OptIn(ExperimentalComposeRuntimeApi::class)
public fun <T> snapshotFlow(block: () -> T): Flow<T> {
    return snapshotFlowImpl(externalManager = null, block)
}

/**
 * Create a [Flow] from observable [Snapshot] state. (e.g. state holders returned by
 * [mutableStateOf][androidx.compose.runtime.mutableStateOf].)
 *
 * [snapshotFlow] creates a [Flow] that runs [block] when collected and emits the result, recording
 * any snapshot state that was accessed. While collection continues, if a new [Snapshot] is applied
 * that changes state accessed by [block], the flow will run [block] again, re-recording the
 * snapshot state that was accessed. If the result of [block] is not [equal to][Any.equals] the
 * previous result, the flow will emit that new result. (This behavior is similar to that of
 * [Flow.distinctUntilChanged][kotlinx.coroutines.flow.distinctUntilChanged].) Collection will
 * continue indefinitely unless it is explicitly cancelled or limited by the use of other [Flow]
 * operators.
 *
 * [manager] controls how snapshot state is observed. When the [manager] argument is omitted, a
 * [SnapshotFlowManager] is instantiated under the hood, so by explicitly managing a
 * [SnapshotFlowManager] and passing it to multiple [snapshotFlow]s that will be collected on the
 * same thread, you can improve performance by sharing resources between those [snapshotFlow]s. It
 * is not safe to share a [SnapshotFlowManager] instance across two [snapshotFlow]s that collect in
 * parallel on two different threads. Sharing a [SnapshotFlowManager] across [snapshotFlow]s that
 * cannot be collected in parallel to each other is always encouraged.
 *
 * @sample androidx.compose.runtime.samples.snapshotFlowSample
 *
 * [block] is run in a **read-only** [Snapshot] and may not modify snapshot data. If [block]
 * attempts to modify snapshot data, flow collection will fail with [IllegalStateException].
 *
 * [block] may run more than once for equal sets of inputs or only once after many rapid snapshot
 * changes; it should be idempotent and free of side effects.
 *
 * When working with [Snapshot] state it is useful to keep the distinction between **events** and
 * **state** in mind. [snapshotFlow] models snapshot changes as events, but events **cannot** be
 * effectively modeled as observable state. Observable state is a lossy compression of the events
 * that produced that state.
 *
 * An observable **event** happens at a point in time and is discarded. All registered observers at
 * the time the event occurred are notified. All individual events in a stream are assumed to be
 * relevant and may build on one another; repeated equal events have meaning and therefore a
 * registered observer must observe all events without skipping.
 *
 * Observable **state** raises change events when the state changes from one value to a new, unequal
 * value. State change events are **conflated;** only the most recent state matters. Observers of
 * state changes must therefore be **idempotent;** given the same state value the observer should
 * produce the same result. It is valid for a state observer to both skip intermediate states as
 * well as run multiple times for the same state and the result should be the same.
 */
@ExperimentalComposeRuntimeApi
public fun <T> snapshotFlow(manager: SnapshotFlowManager, block: () -> T): Flow<T> {
    return snapshotFlowImpl(manager, block)
}
