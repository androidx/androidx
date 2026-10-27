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

package androidx.compose.remote.player.compose.embedded.doc

import androidx.compose.remote.player.compose.embedded.doc.TimeReadTracker.Companion.WAKE_NONE
import androidx.compose.remote.player.compose.embedded.doc.TimeReadTracker.Companion.WAKE_ON_ANY
import androidx.compose.remote.player.compose.embedded.doc.TimeReadTracker.Companion.WAKE_ON_CONTINUOUS
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Decides how often the embedded player's clock loop advances time, from what the active
 * composition, layout and draw passes actually read.
 *
 * The document scan in [preprocessDocument] (and core's listeners) gives an upper bound: which
 * clock loop the document *could* need. That scan also counts expressions nobody references and
 * operations in state or fit-box branches that aren't shown. This scheduler narrows the bound at
 * runtime using [TimeReadTracker]: a time field that something reads is read again in the frame
 * after it changes, so a field that changes and isn't read has no readers.
 * - [Mode.CONTINUOUS] advances time every frame. After [IDLE_FRAMES] written frames with no read of
 *   a continuous field it drops to [Mode.DISCRETE].
 * - [Mode.DISCRETE] advances time on whole-second boundaries. It parks once every discrete field
 *   the document refers to ([discreteTimeMask]) has changed [IDLE_CHANGES] times without being
 *   read, and nothing read a continuous field. Fields that change slowly or never (the hour, the
 *   day, the year, the UTC offset) are probed instead ([probeMask]): their readers are invalidated
 *   as if the value had changed, so they count as idle after [IDLE_CHANGES] unread probes. A field
 *   whose probe is read isn't probed again for [PROBE_BACKOFF_TICKS] ticks, so a visible hour
 *   readout costs one extra read about once a minute and keeps the loop ticking once per second.
 * - [Mode.PARKED] requests no frames until any time field is read, for example when a state switch
 *   composes a branch that shows a clock.
 *
 * Until a frame draw of the host window has been seen ([TimeReadTracker.drawObserved]), missing
 * reads say nothing, so the loop never idles. The same holds when [canIdle] is false (particles and
 * wake-in operations drive their own frames).
 */
internal class ClockScheduler(
    private val reads: TimeReadTracker,
    private val needsContinuous: Boolean,
    private val discreteTimeMask: Int,
    private val canIdle: Boolean,
) {
    internal enum class Mode {
        CONTINUOUS,
        DISCRETE,
        PARKED,
    }

    internal var mode: Mode = initialMode()
        private set

    private var idleFrames = 0
    private val unreadChanges = IntArray(DISCRETE_TIME_VARIABLES.size)
    private val probeBackoff = IntArray(DISCRETE_TIME_VARIABLES.size)
    private var continuousReadsAtWrite = reads.continuousReads
    private val discreteReadsAtWrite = reads.discreteReads.copyOf()
    private var changedAtWrite = 0
    private var wroteSinceEvaluate = false

    private fun initialMode(): Mode = if (needsContinuous) Mode.CONTINUOUS else Mode.DISCRETE

    /** Records the read counters right after time was written, with the discrete fields changed. */
    internal fun afterWrite(changedDiscreteMask: Int) {
        continuousReadsAtWrite = reads.continuousReads
        reads.discreteReads.copyInto(discreteReadsAtWrite)
        changedAtWrite = changedDiscreteMask
        wroteSinceEvaluate = true
    }

    /**
     * Updates [mode] from the reads made since the last [afterWrite]. Call it at the start of the
     * next tick, after the passes that follow the write have run.
     */
    internal fun evaluate(): Mode {
        if (!wroteSinceEvaluate) return mode
        wroteSinceEvaluate = false
        if (!canIdle || !reads.drawObserved) return mode

        val continuousRead = reads.continuousReads != continuousReadsAtWrite
        for (i in unreadChanges.indices) {
            if (reads.discreteReads[i] != discreteReadsAtWrite[i]) {
                unreadChanges[i] = 0
                probeBackoff[i] = PROBE_BACKOFF_TICKS
            } else if (changedAtWrite and (1 shl i) != 0) {
                unreadChanges[i]++
            }
        }

        when (mode) {
            Mode.CONTINUOUS ->
                if (continuousRead) {
                    idleFrames = 0
                } else if (++idleFrames >= IDLE_FRAMES) {
                    idleFrames = 0
                    mode = if (discreteIdle()) Mode.PARKED else Mode.DISCRETE
                }
            Mode.DISCRETE ->
                if (continuousRead && needsContinuous) {
                    mode = Mode.CONTINUOUS
                } else if (!continuousRead && discreteIdle()) {
                    mode = Mode.PARKED
                }
            Mode.PARKED -> {}
        }
        return mode
    }

    /**
     * The discrete fields to probe after the time write that changed [changedDiscreteMask]: fields
     * the document refers to that didn't change in this write, haven't yet been seen unread
     * [IDLE_CHANGES] times, and weren't read recently. Only probes while ticking per second and
     * while missing reads can be trusted, since that's the only time a probe can let the loop park.
     */
    internal fun probeMask(changedDiscreteMask: Int): Int {
        if (mode != Mode.DISCRETE || !canIdle || !reads.drawObserved) return 0
        var mask = 0
        for (i in unreadChanges.indices) {
            val bit = 1 shl i
            if (discreteTimeMask and bit == 0 || changedDiscreteMask and bit != 0) continue
            if (unreadChanges[i] >= IDLE_CHANGES) continue
            if (probeBackoff[i] > 0) {
                probeBackoff[i]--
                continue
            }
            mask = mask or bit
        }
        return mask
    }

    private fun discreteIdle(): Boolean {
        for (i in unreadChanges.indices) {
            if (discreteTimeMask and (1 shl i) != 0 && unreadChanges[i] < IDLE_CHANGES) {
                return false
            }
        }
        return true
    }

    /** Suspends until a time field is read, then resumes ticking at the document's full rate. */
    internal suspend fun awaitRead() {
        reads.wakeups.tryReceive()
        reads.wakeOn = WAKE_ON_ANY
        try {
            // A read between the last evaluate and arming the wakeup would otherwise be missed.
            if (!readSinceWrite()) reads.wakeups.receive()
        } finally {
            reads.wakeOn = WAKE_NONE
        }
        resume()
    }

    /**
     * Waits [millis] before the next discrete tick. Returns early if a continuous field is read and
     * the document can tick continuously, so a newly shown animation starts without waiting for the
     * next second.
     */
    internal suspend fun awaitDiscreteTick(millis: Long) {
        if (!needsContinuous) {
            delay(millis)
            return
        }
        reads.wakeups.tryReceive()
        reads.wakeOn = WAKE_ON_CONTINUOUS
        try {
            withTimeoutOrNull(millis) { reads.wakeups.receive() }
        } finally {
            reads.wakeOn = WAKE_NONE
        }
    }

    private fun readSinceWrite(): Boolean =
        reads.continuousReads != continuousReadsAtWrite ||
            !reads.discreteReads.contentEquals(discreteReadsAtWrite)

    private fun resume() {
        mode = initialMode()
        idleFrames = 0
        unreadChanges.fill(0)
        probeBackoff.fill(0)
        wroteSinceEvaluate = false
    }

    internal companion object {
        /** Written frames without a continuous read before continuous ticking stops. */
        const val IDLE_FRAMES = 3

        /** Unread changes of each discrete field before discrete ticking stops. */
        const val IDLE_CHANGES = 2

        /** Discrete ticks after a read of a probed field before that field is probed again. */
        const val PROBE_BACKOFF_TICKS = 60
    }
}
