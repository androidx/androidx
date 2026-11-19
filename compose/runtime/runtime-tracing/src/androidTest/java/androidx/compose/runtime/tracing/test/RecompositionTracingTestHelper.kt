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

package androidx.compose.runtime.tracing.test

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.Counter
import androidx.tracing.DelicateTracingApi
import androidx.tracing.EventMetadataCloseable
import androidx.tracing.ExperimentalContextPropagation
import androidx.tracing.PropagationToken
import androidx.tracing.PropagationUnsupportedToken
import androidx.tracing.Tracer
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

@OptIn(DelicateTracingApi::class)
internal fun withRecordingTracer(block: (events: List<String>) -> Unit) {
    val previousTracer = Tracer.global
    val recordingTracer = RecordingTracer()
    try {
        Tracer.resetGlobalTracer()
        Tracer.setGlobalTracer(recordingTracer)
        block(recordingTracer.instantEvents)
    } finally {
        Tracer.resetGlobalTracer()
        if (previousTracer !== Tracer.getStubTracer()) {
            Tracer.setGlobalTracer(previousTracer)
        }
    }
}

internal fun runOnMainWithRecomposer(
    beforeStartRecomposer: () -> Unit,
    block: (MainThreadRecomposer) -> Unit,
) {
    var recomposer: MainThreadRecomposer? = null
    try {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            beforeStartRecomposer()
            val localRecomposer = MainThreadRecomposer().also { recomposer = it }
            try {
                block(localRecomposer)
            } finally {
                localRecomposer.disposeCompositions()
            }
        }
    } finally {
        recomposer?.cancelAndJoin()
    }
}

internal class MainThreadRecomposer {
    private val frameClock = BroadcastFrameClock()
    private val recomposerContext = Dispatchers.Main + frameClock
    private val recomposer = Recomposer(recomposerContext)
    private val scope = CoroutineScope(recomposerContext)
    private val job: Job =
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                recomposer.runRecomposeAndApplyChanges()
            } catch (_: CancellationException) {
                // Expected on cancel
            }
        }
    private val compositions = mutableListOf<Composition>()

    fun setContent(content: @Composable () -> Unit) {
        val composition = Composition(EmptyApplier(), recomposer)
        compositions += composition
        composition.setContent(content)
    }

    fun disposeCompositions() {
        var firstError: Throwable? = null
        for (i in compositions.indices.reversed()) {
            try {
                compositions[i].dispose()
            } catch (t: Throwable) {
                if (firstError == null) {
                    firstError = t
                } else {
                    firstError.addSuppressed(t)
                }
            }
        }
        compositions.clear()
        firstError?.let { throw it }
    }

    fun cancelAndJoin() {
        recomposer.cancel()
        job.cancel()
        scope.cancel()
        runBlocking {
            withTimeout(5.seconds) {
                job.join()
                recomposer.join()
            }
        }
    }
}

private class EmptyApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) {}

    override fun insertBottomUp(index: Int, instance: Unit) {}

    override fun remove(index: Int, count: Int) {}

    override fun move(from: Int, to: Int, count: Int) {}

    override fun onClear() {}
}

@OptIn(DelicateTracingApi::class, ExperimentalContextPropagation::class)
private class RecordingTracer : Tracer() {
    val instantEvents = CopyOnWriteArrayList<String>()

    override fun tokenForManualPropagation(flowIds: List<Long>): PropagationToken =
        PropagationUnsupportedToken

    override fun tokenFromThreadContext(): PropagationToken = PropagationUnsupportedToken

    override suspend fun tokenFromCoroutineContext(): PropagationToken = PropagationUnsupportedToken

    override fun beginSectionWithMetadata(
        category: String,
        name: String,
        token: PropagationToken?,
        isRoot: Boolean,
    ): EventMetadataCloseable = EventMetadataCloseable()

    override suspend fun beginCoroutineSectionWithMetadata(
        category: String,
        name: String,
        token: PropagationToken?,
        isRoot: Boolean,
    ): EventMetadataCloseable = EventMetadataCloseable()

    override fun isCategoryEnabled(category: String): Boolean = category == "androidx.compose"

    override fun counter(category: String, name: String): Counter =
        throw UnsupportedOperationException()

    override fun writeInstant(
        category: String,
        name: String,
        token: PropagationToken?,
    ): EventMetadataCloseable {
        if (category == "androidx.compose") {
            instantEvents.add(name)
        }
        return EventMetadataCloseable()
    }
}
