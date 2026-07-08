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
package androidx.compose.runtime.tooling

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.mutableObjectListOf
import androidx.collection.mutableScatterMapOf
import androidx.compose.runtime.CancellationHandle
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.RecomposeScopeImpl
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.RecomposerInfo
import androidx.compose.runtime.collection.ScopeMap
import androidx.compose.runtime.collection.removeLast
import androidx.compose.runtime.internal.trace
import androidx.compose.runtime.platform.makeSynchronizedObject
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.tooling.SnapshotInstanceObservers
import androidx.compose.runtime.snapshots.tooling.SnapshotObserver
import androidx.compose.runtime.snapshots.tooling.observeSnapshots
import java.util.concurrent.atomic.AtomicLong
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val _nextFlowId = AtomicLong(0)

private fun getNextFlowId() = _nextFlowId.incrementAndGet()

private fun getLastFlowId() = _nextFlowId.get()

/**
 * Collects recomposition flow events.
 *
 * Tracks the causal flow between state reads/writes and recomposition of [RecomposeScope]s by
 * connecting to recomposition lifecycle and [Snapshot] state events.
 *
 * The traces are recorded in the Perfetto trace format through a provided [TraceCollector].
 *
 * NOTE: Recomposition tracing captures a stack trace and instance string on all state reads and
 * writes, which results a significant overhead. Avoid using it in production.
 */
@OptIn(ExperimentalComposeRuntimeApi::class)
@InternalComposeTracingApi
public class RecompositionTracer
// events should always be reported immediately for accurate timing
@Suppress("ExecutorRegistration")
internal constructor(private val traceEventListener: TraceEventListener) {

    /** @param traceCollector receives recomposition trace events */
    public constructor(traceCollector: TraceCollector) : this(TraceCollectorAdapter(traceCollector))

    /** Records recomposition flow events. */
    public interface TraceCollector {
        /**
         * Begins a named trace section.
         *
         * @param sectionName name of the trace section
         * @param flowIds ids for the invalidation chain connecting events.
         */
        public fun beginSection(
            sectionName: String,
            // flowIds are immutable and passed to androidx.tracing as List<Long>
            @Suppress("PrimitiveInCollection") flowIds: List<Long>,
        )

        /** Ends the current trace section. */
        public fun endSection()

        /**
         * Records an instant trace event.
         *
         * @param sectionName name of the trace event
         * @param stackTrace call stack associated with the event
         * @param id unique identifier of the instance associated with events
         * @param flowIds ids for the invalidation chain connecting events.s
         */
        public fun instantEvent(
            sectionName: String,
            stackTrace: List<StackTraceElement>,
            id: Int,
            // flowIds are immutable and passed to androidx.tracing as List<Long>
            @Suppress("PrimitiveInCollection") flowIds: List<Long>,
        )

        /** Returns true if trace collector is currently active and accepting events. */
        public fun isEnabled(): Boolean
    }

    /**
     * Installs recomposition tracing.
     *
     * Registers observers and starts recording events using the caller's coroutine context. The
     * method returns after the recomposer observer is installed.
     *
     * @param coroutineContext context to run the observer in
     * @return a [CancellationHandle] to stop tracing and dispose of registered observers
     */
    public fun installTracing(coroutineContext: CoroutineContext): CancellationHandle {
        val observer = RecompositionFlowObserver(traceEventListener)
        val observerJob = runRecomposerObserver(coroutineContext, observer)
        val snapshotObserverHandle =
            Snapshot.observeSnapshots(
                object : SnapshotObserver {
                    val instanceObservers =
                        SnapshotInstanceObservers(writeObserver = observer::onStateWrite)

                    override fun onPreCreate(
                        parent: Snapshot?,
                        readonly: Boolean,
                    ): SnapshotInstanceObservers = instanceObservers
                }
            )
        val writeObserverHandle = Snapshot.registerGlobalWriteObserver(observer::onStateWrite)

        return CancellationHandle {
            observerJob.cancel()
            snapshotObserverHandle.dispose()
            writeObserverHandle.dispose()
            observer.close()
        }
    }

    private fun runRecomposerObserver(
        coroutineContext: CoroutineContext,
        observer: RecompositionFlowObserver,
    ): Job {
        // This job should not be attached to the current context through structured concurrency, as
        // it is a background collection job that is not attached to anything else.
        val observerJob = Job()

        // Starting UNDISPATCHED to process the first value immediately
        CoroutineScope(coroutineContext + observerJob).launch(start = CoroutineStart.UNDISPATCHED) {
            val recomposerObservers = mutableMapOf<RecomposerInfo, CompositionObserverHandle?>()
            try {
                Recomposer.runningRecomposers.collect { running ->
                    running.forEach { recomposer ->
                        if (recomposer !in recomposerObservers) {
                            recomposerObservers[recomposer] = recomposer.observe(observer)
                        }
                    }
                    val currentIterator = recomposerObservers.entries.iterator()
                    while (currentIterator.hasNext()) {
                        val (obs, handle) = currentIterator.next()
                        if (obs !in running) {
                            handle?.dispose()
                            currentIterator.remove()
                        }
                    }
                }
            } finally {
                recomposerObservers.values.forEach { it?.dispose() }
            }
        }

        return observerJob
    }

    /** Receives recomposition flow events. */
    @Suppress("PrimitiveInCollection") // matches [TraceCollector] signature
    internal interface TraceEventListener {
        /**
         * Records a state read inside a [RecomposeScope].
         *
         * @param scope [RecomposeScope] where the read occurred
         * @param value state instance being read
         * @param flowIds correlation ID representing this read
         * @param stackTrace call stack at the read location
         */
        fun onStateRead(
            scope: RecomposeScope,
            value: Any,
            flowIds: List<Long>,
            stackTrace: List<StackTraceElement>,
        )

        /**
         * Records a state write.
         *
         * @param value state instance being written
         * @param flowIds correlation IDs affected by this write
         * @param stackTrace stack trace at the write location
         */
        fun onStateWrite(value: Any, flowIds: List<Long>, stackTrace: List<StackTraceElement>)

        /**
         * Records direct invalidation of a [RecomposeScope].
         *
         * @param scope [RecomposeScope] being invalidated
         * @param flowId correlation ID representing this invalidation
         * @param stackTrace stack trace at the invalidation location
         */
        fun onDirectInvalidation(
            scope: RecomposeScope,
            flowId: Long,
            stackTrace: List<StackTraceElement>,
        )

        /**
         * Begins a recomposition trace section of a group.
         *
         * @param scope [RecomposeScope] being composed
         * @param flowIds correlation IDs causing this recomposition
         */
        fun onBeginRecomposeGroup(scope: RecomposeScope, flowIds: List<Long>)

        /**
         * Ends a recomposition trace section of a group.
         *
         * @param scope [RecomposeScope] ending composition
         */
        fun onEndRecomposeGroup(scope: RecomposeScope)

        /** Returns true if event listening is active. */
        fun isEnabled(): Boolean
    }

    private class TraceCollectorAdapter(private val traceCollector: TraceCollector) :
        TraceEventListener {
        @Suppress("PrimitiveInCollection")
        override fun onStateRead(
            scope: RecomposeScope,
            value: Any,
            flowIds: List<Long>,
            stackTrace: List<StackTraceElement>,
        ) {
            val valueString = value.asString()
            traceCollector.instantEvent(
                sectionName = "State read of $valueString",
                stackTrace = stackTrace,
                id = System.identityHashCode(value),
                flowIds = flowIds,
            )
        }

        override fun onStateWrite(
            value: Any,
            flowIds: List<Long>,
            stackTrace: List<StackTraceElement>,
        ) {
            val valueString = value.asString()
            traceCollector.instantEvent(
                sectionName = "State write of $valueString",
                stackTrace = stackTrace,
                id = System.identityHashCode(value),
                flowIds = flowIds,
            )
        }

        override fun onDirectInvalidation(
            scope: RecomposeScope,
            flowId: Long,
            stackTrace: List<StackTraceElement>,
        ) {
            traceCollector.instantEvent(
                sectionName = "Direct invalidation",
                stackTrace = stackTrace,
                id = 0,
                flowIds = listOf(flowId),
            )
        }

        override fun onBeginRecomposeGroup(scope: RecomposeScope, flowIds: List<Long>) {
            traceCollector.beginSection("Recompose group", flowIds)
        }

        override fun onEndRecomposeGroup(scope: RecomposeScope) {
            traceCollector.endSection()
        }

        override fun isEnabled(): Boolean = traceCollector.isEnabled()
    }
}

// TraceCollector uses List<Long> to encode flows in the API to match consumers
// (such as androidx.tracing). Since the adapter will have to box flows regardless, we can avoid
// additional boxing when crossing this boundary.
// It still boxes every flow id once, but that effectively pre-allocates those values when passing
// them along to androidx.tracing and allows using `ScatterMap#compute` to avoid extra lookups.
// The tracing is expected to be a heavy operation because of stack trace capture anyways, so using
// boxed long is not making it significantly worse.
@OptIn(InternalComposeTracingApi::class, ExperimentalComposeRuntimeApi::class)
@Suppress("PrimitiveInCollection")
private class RecompositionFlowObserver(
    private val traceEventListener: RecompositionTracer.TraceEventListener
) : CompositionObserver, CompositionRegistrationObserver, AutoCloseable {

    private val lock = makeSynchronizedObject()
    private val compositionHandles =
        mutableScatterMapOf<ObservableComposition, CompositionObserverHandle>()
    private val scopeMetadata = mutableScatterMapOf<RecomposeScope, ScopeData>()
    private val readsToScopes = ScopeMap<Any, ScopeData>()
    private val invalidatedScopeStack = mutableObjectListOf<RecomposeScope>()

    override fun onCompositionRegistered(composition: ObservableComposition) {
        synchronized(lock) { compositionHandles[composition] = composition.setObserver(this) }
    }

    override fun onCompositionUnregistered(composition: ObservableComposition) {
        synchronized(lock) { compositionHandles -= composition }
    }

    override fun onBeginComposition(composition: ObservableComposition) {
        // Nothing to do here, composition is already traced
    }

    override fun onScopeEnter(scope: RecomposeScope) {
        if (!traceEventListener.isEnabled()) return

        synchronized(lock) { onScopeEnterLocked(scope) }
    }

    private fun onScopeEnterLocked(scope: RecomposeScope) {
        val data = scopeMetadata[scope]
        if (data != null) {
            data.recordFlowIdOnEnter()
            if (data.hasInvalidations()) {
                invalidatedScopeStack += scope
                traceEventListener.onBeginRecomposeGroup(scope, data.invalidationFlowIds())
                data.resetInvalidations()
            }
        }
    }

    override fun onReadInScope(scope: RecomposeScope, value: Any) {
        if (!traceEventListener.isEnabled()) return

        synchronized(lock) { onReadInScopeLocked(scope, value) }
    }

    private fun onReadInScopeLocked(scope: RecomposeScope, value: Any) {
        val data = scopeMetadata.getOrPut(scope) { ScopeData() }

        val flowId = data.trackRead(value)
        traceEventListener.onStateRead(scope, value, listOf(flowId), currentStackTrace())
        readsToScopes.add(value, data)
    }

    fun onStateWrite(instance: Any) {
        if (!traceEventListener.isEnabled()) return

        synchronized(lock) { onStateWriteLocked(instance) }
    }

    private fun onStateWriteLocked(instance: Any) {
        var flows: MutableList<Long>? = null
        readsToScopes.forEachScopeOf(instance) { scopeData ->
            val flows = flows ?: mutableListOf<Long>().also { flows = it }
            flows += scopeData.trackWrite(instance)
        }
        if (!flows.isNullOrEmpty()) {
            traceEventListener.onStateWrite(instance, flows, currentStackTrace())
        }
    }

    override fun onScopeExit(scope: RecomposeScope) {
        if (!traceEventListener.isEnabled()) return

        synchronized(lock) { onScopeExitLocked(scope) }
    }

    private fun onScopeExitLocked(scope: RecomposeScope) {
        val data = scopeMetadata[scope]
        if (!(scope as RecomposeScopeImpl).skipped) {
            data?.cleanupStaleReads { readsToScopes.remove(it, data) }
        }
        if (invalidatedScopeStack.lastOrNull() == scope) {
            invalidatedScopeStack.removeLast()
            traceEventListener.onEndRecomposeGroup(scope)
        }
    }

    override fun onEndComposition(composition: ObservableComposition) {
        // Nothing to do here, composition is already traced
    }

    override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {
        if (!traceEventListener.isEnabled()) return

        synchronized(lock) { onScopeInvalidatedLocked(scope, value) }
    }

    private fun onScopeInvalidatedLocked(scope: RecomposeScope, value: Any?) {
        val data = scopeMetadata[scope]
        if (value == null) {
            // Direct invalidation is special as it does not involve a state write.
            // This is usually a result of ComposableLambda instance changing.
            val data = data ?: ScopeData().also { scopeMetadata[scope] = it }
            val flowId = data.trackRead(null)
            data.invalidateWith(null)
            traceEventListener.onDirectInvalidation(scope, flowId, currentStackTrace())
        } else if (data != null) {
            data.invalidateWith(value)
            readsToScopes.remove(value, data)
        }
    }

    override fun onScopeDisposed(scope: RecomposeScope) {
        synchronized(lock) {
            val data = scopeMetadata.remove(scope)
            data?.readFlowIds?.forEachKey {
                if (it != null) {
                    readsToScopes.remove(it, data)
                }
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            compositionHandles.forEachValue { it.dispose() }
            compositionHandles.clear()
            scopeMetadata.clear()
        }
    }
}

private fun Any.asString() = Snapshot.withoutReadObservation { toString() }

@Suppress("PrimitiveInCollection") // see [RecompositionFlowObserver]
private class ScopeData {
    private val invalidationIds = MutableScatterSet<Long>(0)
    val readFlowIds = MutableScatterMap<Any?, Long>(0)
    val writeFlowIds = MutableScatterMap<Any?, Long>(0)
    private var enterFlowId = -1L

    fun recordFlowIdOnEnter() {
        enterFlowId = getLastFlowId()
    }

    inline fun cleanupStaleReads(crossinline onTrackedInstanceRemoved: (Any) -> Unit) {
        readFlowIds.removeIf { k, v ->
            val valid = v.isValid()
            if (!valid && k != null) {
                onTrackedInstanceRemoved(k)
            }
            !valid
        }
        writeFlowIds.removeIf { _, v -> !v.isValid() }
    }

    fun invalidateWith(instance: Any?) {
        val readFlowId = readFlowIds[instance]
        if (readFlowId.isValid()) {
            invalidationIds += readFlowId
        }
        val writeFlowId = writeFlowIds[instance]
        if (writeFlowId.isValid()) {
            invalidationIds += writeFlowId
        }
    }

    fun trackRead(instance: Any?): Long =
        readFlowIds.compute(instance) { _, v -> if (v.isValid()) v else getNextFlowId() }

    fun trackWrite(instance: Any?): Long =
        writeFlowIds.compute(instance) { _, v -> if (v.isValid()) v else getNextFlowId() }

    fun invalidationFlowIds(): List<Long> =
        ArrayList<Long>(invalidationIds.size).also { ids ->
            invalidationIds.forEach { ids.add(it) }
        }

    fun hasInvalidations() = invalidationIds.isNotEmpty()

    fun resetInvalidations() {
        invalidationIds.clear()
    }

    @OptIn(ExperimentalContracts::class)
    private fun Long?.isValid(): Boolean {
        contract { returns(true) implies (this@isValid != null) }
        return this != null && this > enterFlowId
    }
}

private fun currentStackTrace(): List<StackTraceElement> {
    trace("currentStackTrace") {
        val frames = Exception().stackTrace.toMutableList()

        var isPrefix = true
        // Filter captured frames to remove common prefix / suffix / internal frames.
        // This reduces verbosity and slightly improves perf.
        val filtered = ArrayList<StackTraceElement>(frames.size)
        for (i in frames.indices) {
            val element = frames[i]
            if (isPrefix) {
                if (!element.isPrefixFrame()) {
                    isPrefix = false
                } else {
                    continue
                }
            }

            if (element.isSuffixFrame()) {
                break
            }

            if (filter(element)) {
                continue
            }

            filtered.add(element)
        }

        // Accidentally removed all elements, just return the unfiltered list.
        if (filtered.isEmpty()) {
            return frames
        }
        return filtered
    }
}

private fun StackTraceElement.isPrefixFrame(): Boolean =
    when (val name = className) {
        "androidx.compose.runtime.tooling.RecompositionFlowObserver",
        "androidx.compose.runtime.tooling.RecompositionTracer_jvmAndAndroidKt",
        "androidx.compose.runtime.tooling.RecompositionTracer" -> true
        else -> {
            name.startsWith("androidx.compose.runtime.tooling.RecompositionTracer") ||
                name.startsWith("androidx.compose.runtime.snapshots.GlobalSnapshot")
        }
    }

// Filter intermediate frames from the stack trace to reduce visual clutter and overhead
private fun filter(element: StackTraceElement): Boolean =
    when (element.className) {
        // Filter invoke of the composable lambda to reduce number of frames
        // It might be invoke$lambda$0 etc for restarting scopes.
        "androidx.compose.runtime.internal.ComposableLambdaImpl" -> {
            element.methodName.startsWith("invoke")
        }
        else -> false
    }

// Marks the end of meaningful trace frames
private fun StackTraceElement.isSuffixFrame(): Boolean =
    when (className) {
        // Ui dispatcher is always at the root
        "androidx.compose.ui.platform.AndroidUiDispatcher" -> {
            when (methodName) {
                "performFrameDispatch",
                "performTrampolineDispatch" -> true
                else -> false
            }
        }
        else -> false
    }
