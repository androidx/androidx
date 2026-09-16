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

package androidx.camera.camera2.pipe.internal

import androidx.camera.camera2.pipe.MemoryEstimator
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.config.CameraPipeJob
import androidx.camera.camera2.pipe.core.Threads
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * A CameraPipe scoped resource trimmer. This trimmer keeps track of incoming and started camera
 * [Request]s and proactively trims evictable resources to make room for newly requested resources.
 * It keeps tracks of the [androidx.camera.camera2.pipe.CameraGraph]s started so far and delegates
 * the trimming task to them based on some policy.
 */
@Singleton
internal class CameraPipeResourceTrimmer
@Inject
constructor(
    private val memoryEstimator: MemoryEstimator,
    threads: Threads,
    cameraPipeLifetime: CameraPipeLifetime,
    @CameraPipeJob cameraPipeJob: Job,
) {
    private val scope =
        CoroutineScope(
            SupervisorJob(cameraPipeJob) +
                threads.lightweightDispatcher.plus(CoroutineName("CXCP-ResourceTrimmer"))
        )

    private val lock = Any()
    private val trimmers = CopyOnWriteArrayList<FrameGraphResourceTrimmer>()
    private val trimSignal = Channel<Unit>(Channel.CONFLATED)

    // Counter for fair, cross-invocation round-robin trimming across camera graphs
    private val roundRobinCounter = atomic(0)

    // Tracks the most recently started graph. This is to ensure that this graph get minimal to no
    // penalty when trimming the active graphs.
    private val recentlyActiveTrimmer = atomic<FrameGraphResourceTrimmer?>(null)

    private val trimJob: Job =
        scope.launch(CoroutineName("CXCP-ResourceTrimmer-TrimJob")) {
            for (signal in trimSignal) {
                for (trimmer in trimmers) {
                    trimmer.trimForStreams()
                }

                val totalMemoryTarget = trimmers.sumOf { it.currentMemoryRequirement }
                if (totalMemoryTarget <= 0L) continue

                if (!memoryEstimator.canAllocateNow(totalMemoryTarget)) {
                    trimMemory(totalMemoryTarget)
                }
            }
        }

    // This method trims one frame from each trimmer in a round-robin order until we have enough
    // capacity, or we run out of frames to trim.
    private fun trimMemory(totalMemoryTarget: Long) {
        var framesTrimmed = true
        while (framesTrimmed && !memoryEstimator.canAllocateNow(totalMemoryTarget)) {
            framesTrimmed = trimNextRoundRobin { it.trim() }
        }
    }

    private inline fun trimNextRoundRobin(
        predicate: (FrameGraphResourceTrimmer) -> Boolean
    ): Boolean {
        val size = trimmers.size
        if (size == 0) return false

        // Save the recently active trimmer. We will try a round-robin loop across other trimmers.
        val mostRecentTrimmer = recentlyActiveTrimmer.value

        for (i in 0 until size) {
            val index = roundRobinCounter.getAndIncrement()
            val trimmer = trimmers.getRoundRobinIndex(index, size) ?: continue

            if (trimmer == mostRecentTrimmer) {
                continue
            }
            if (predicate(trimmer)) {
                return true
            }
        }
        // Attempt trim from the most recent trimmer if trim attempt from other trimmers was
        // unsuccessful.
        return mostRecentTrimmer != null &&
            trimmers.contains(mostRecentTrimmer) &&
            predicate(mostRecentTrimmer)
    }

    init {
        cameraPipeLifetime.addShutdownAction(CameraPipeLifetime.ShutdownType.SCOPE) {
            trimJob.cancel()
            scope.cancel()
        }
    }

    fun register(trimmer: FrameGraphResourceTrimmer) {
        synchronized(lock) {
            if (!trimmers.contains(trimmer)) {
                trimmers.add(trimmer)
            }
        }
    }

    fun markRecentlyActive(trimmer: FrameGraphResourceTrimmer) {
        recentlyActiveTrimmer.value = trimmer
    }

    fun unregister(trimmer: FrameGraphResourceTrimmer) {
        synchronized(lock) { trimmers.remove(trimmer) }
        recentlyActiveTrimmer.compareAndSet(trimmer, null)
    }

    /**
     * Explicitly triggers the trim job to re-evaluate memory bounds and stream capacities. This can
     * then trim FrameBuffer(s) across the registered trimmers if needed.
     */
    fun invalidate() {
        trimSignal.trySend(Unit)
    }

    companion object {
        /**
         * The number of frames worth of memory to proactively keep free as headroom for active
         * repeating requests.
         */
        internal const val REPEATING_FRAME_MARGIN_COUNT = 3
    }
}

/**
 * Safely retrieves an item from a [CopyOnWriteArrayList] using modulo arithmetic in a round-robin
 * fashion. Returns null if the list was concurrently modified and the index is out of bounds.
 */
internal fun <T> CopyOnWriteArrayList<T>.getRoundRobinIndex(index: Int, size: Int): T? {
    if (size == 0) return null
    val listIndex = (index % size).let { if (it < 0) it + size else it }

    return try {
        this[listIndex]
    } catch (e: IndexOutOfBoundsException) {
        // This can happen since the CopyOnWriteArrayList could have shrunk after checking the size.
        null
    }
}
