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

package androidx.camera.camera2.pipe.testing

import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CaptureSequence.CaptureSequenceListener
import androidx.camera.camera2.pipe.CaptureSequenceProcessor
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import kotlinx.atomicfu.atomic

/**
 * Fake implementation of a [CaptureSequenceProcessor] that records events and simulates some low
 * level behavior.
 *
 * This allows kotlin tests to check sequences of interactions that dispatch in the background
 * without blocking between events.
 */
public class FakeCaptureSequenceProcessor(
    private val cameraId: CameraId = FakeCameraIds.default,
    private val defaultTemplate: RequestTemplate = RequestTemplate(1)
) : CaptureSequenceProcessor<Request, FakeCaptureSequence> {
    private val debugId = debugIds.incrementAndGet()
    private val lock = Any()
    private val sequenceIds = atomic(0)

    @GuardedBy("lock") private val captureQueue = mutableListOf<FakeCaptureSequence>()

    @GuardedBy("lock") private var repeatingCapture: FakeCaptureSequence? = null

    @GuardedBy("lock") private var shutdown = false

    @GuardedBy("lock") private val _events = mutableListOf<Event>()

    @GuardedBy("lock") private var nextEventIndex = 0
    public val events: List<Event>
        get() = synchronized(lock) { _events }

    /** Get the next event from queue with an option to specify a timeout for tests. */
    public fun nextEvent(): Event {
        synchronized(lock) {
            val eventIdx = nextEventIndex++
            check(_events.size > 0) {
                "Failed to get next event for $this, there have been no interactions."
            }
            check(eventIdx < _events.size) {
                "Failed to get next event. Last event was ${events[eventIdx - 1]}"
            }
            return events[eventIdx]
        }
    }

    public fun clearEvents() {
        synchronized(lock) {
            _events.clear()
            nextEventIndex = 0
        }
    }

    public var rejectBuild: Boolean = false
        get() = synchronized(lock) { field }
        set(value) = synchronized(lock) { field = value }

    public var rejectSubmit: Boolean = false
        get() = synchronized(lock) { field }
        set(value) = synchronized(lock) { field = value }

    public var surfaceMap: Map<StreamId, Surface> = emptyMap()
        get() = synchronized(lock) { field }
        set(value) =
            synchronized(lock) {
                field = value
                println("Configured surfaceMap for $this")
            }

    @Volatile public var throwOnBuild: Boolean = false

    @Volatile public var throwOnSubmit: Boolean = false

    @Volatile public var throwOnStop: Boolean = false

    @Volatile public var throwOnAbort: Boolean = false

    @Volatile public var throwOnShutdown: Boolean = false

    override fun build(
        isRepeating: Boolean,
        requests: List<Request>,
        defaultParameters: Map<*, Any?>,
        requiredParameters: Map<*, Any?>,
        listeners: List<Request.Listener>,
        sequenceListener: CaptureSequenceListener
    ): FakeCaptureSequence? {
        throwTestExceptionIf(throwOnBuild)

        val captureSequence =
            FakeCaptureSequence.create(
                cameraId,
                isRepeating,
                requests,
                surfaceMap,
                defaultTemplate,
                defaultParameters,
                requiredParameters,
                listeners,
                sequenceListener
            )
        synchronized(lock) {
            if (rejectBuild || shutdown || captureSequence == null) {
                println("$this: BuildRejected $captureSequence")
                _events.add(BuildRejected(captureSequence))
                return null
            }
        }
        println("$this: Build $captureSequence")
        return captureSequence
    }

    override fun submit(captureSequence: FakeCaptureSequence): Int {
        throwTestExceptionIf(throwOnSubmit)
        synchronized(lock) {
            if (rejectSubmit || shutdown) {
                println("$this: SubmitRejected $captureSequence")
                _events.add(SubmitRejected(captureSequence))
                return -1
            }
            captureQueue.add(captureSequence)
            if (captureSequence.repeating) {
                repeatingCapture = captureSequence
            }
            println("$this: Submit $captureSequence")
            _events.add(Submit(captureSequence))
            return sequenceIds.incrementAndGet()
        }
    }

    override fun abortCaptures() {
        throwTestExceptionIf(throwOnAbort)

        val requestSequencesToAbort: List<FakeCaptureSequence>
        synchronized(lock) {
            println("$this: AbortCaptures")
            _events.add(AbortCaptures)
            requestSequencesToAbort = captureQueue.toList()
            captureQueue.clear()
        }

        for (sequence in requestSequencesToAbort) {
            sequence.invokeOnSequenceAborted()
        }
    }

    override fun stopRepeating() {
        throwTestExceptionIf(throwOnStop)
        synchronized(lock) {
            println("$this: StopRepeating")
            _events.add(StopRepeating)
            repeatingCapture = null
        }
    }

    override suspend fun shutdown() {
        throwTestExceptionIf(throwOnShutdown)
        synchronized(lock) {
            println("$this: Shutdown")
            shutdown = true
            _events.add(Shutdown)
        }
    }

    override fun toString(): String {
        return "FakeCaptureSequenceProcessor-$debugId($cameraId)"
    }

    /**
     * Get the next CaptureSequence from this CaptureSequenceProcessor. If there are non-repeating
     * capture requests in the queue, remove the first item from the queue. Otherwise, return the
     * current repeating CaptureSequence, or null if there are no active CaptureSequences.
     */
    internal fun nextCaptureSequence(): FakeCaptureSequence? =
        synchronized(lock) { captureQueue.removeFirstOrNull() ?: repeatingCapture }

    private fun throwTestExceptionIf(condition: Boolean) {
        if (condition) {
            throw RuntimeException("Test Exception")
        }
    }

    public open class Event

    public object Shutdown : Event()

    public object StopRepeating : Event()

    public object AbortCaptures : Event()

    public data class BuildRejected(val captureSequence: FakeCaptureSequence?) : Event()

    public data class SubmitRejected(val captureSequence: FakeCaptureSequence) : Event()

    public data class Submit(val captureSequence: FakeCaptureSequence) : Event()

    public companion object {
        private val debugIds = atomic(0)
        public val Event.requests: List<Request>
            get() = checkNotNull(captureSequence).captureRequestList

        public val Event.requiredParameters: Map<*, Any?>
            get() = checkNotNull(captureSequence).requiredParameters

        public val Event.defaultParameters: Map<*, Any?>
            get() = checkNotNull(captureSequence).defaultParameters

        // TODO: Decide if these should only work on successful submit or not.
        public val Event.isRepeating: Boolean
            get() = (this as? Submit)?.captureSequence?.repeating ?: false

        public val Event.isCapture: Boolean
            get() = (this as? Submit)?.captureSequence?.repeating == false

        public val Event.isRejected: Boolean
            get() =
                when (this) {
                    is BuildRejected,
                    is SubmitRejected -> true
                    else -> false
                }

        public val Event.isAbort: Boolean
            get() = this is AbortCaptures

        public val Event.isStopRepeating: Boolean
            get() = this is StopRepeating

        public val Event.isClose: Boolean
            get() = this is Shutdown

        public val Event.captureSequence: FakeCaptureSequence?
            get() =
                when (this) {
                    is Submit -> captureSequence
                    is BuildRejected -> captureSequence
                    is SubmitRejected -> captureSequence
                    else -> null
                }
    }
}
