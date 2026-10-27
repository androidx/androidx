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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.doc

import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteContext
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.IntState
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.tracing.trace
import kotlinx.coroutines.channels.Channel

/**
 * Encapsulates reactive Compose snapshot states for all wall-clock, calendar, and continuous time
 * fields in a [GraphContext], along with a reactive [RemoteClock] and [RemoteClock.TimeSnapshot]
 * view over those states.
 *
 * Every read of a time field goes through [reads], so the clock loop can tell which fields the
 * active composition, layout and draw passes actually read (see [ClockScheduler]).
 */
internal class GraphTimeState(
    internal val baseClock: RemoteClock,
    internal val reads: TimeReadTracker,
) {
    internal val startClockMillis: Long = baseClock.millis()
    private val startClockNanoTime: Long = baseClock.nanoTime()

    private var lastEpochSecond: Long = Long.MIN_VALUE
    private var lastTimeInSec: Float = 0f

    /**
     * Bitmask of the discrete fields (indexed as in [DISCRETE_TIME_VARIABLES]) whose value changed
     * in the last [updateTime] call.
     */
    internal var lastChangedDiscreteMask: Int = 0
        private set

    private val currentMillisState = mutableLongStateOf(startClockMillis)
    private val animationTimeState = mutableFloatStateOf(0f)
    private val continuousSecState = mutableFloatStateOf(0f)
    private val epochSecondIntState = mutableIntStateOf(0)
    private val discreteStates = Array(DISCRETE_TIME_VARIABLES.size) { mutableFloatStateOf(0f) }

    /**
     * One probe counter per discrete field. Every read of a discrete field also reads its probe
     * counter, so [probe] invalidates the field's current readers as if its value had changed.
     */
    private val probeStates = Array(DISCRETE_TIME_VARIABLES.size) { mutableIntStateOf(0) }
    private val zeroFloatState = mutableFloatStateOf(0f)

    private val trackedAnimationTime: FloatState = ContinuousReadState(animationTimeState, reads)
    private val trackedContinuousSec: FloatState = ContinuousReadState(continuousSecState, reads)
    private val trackedDiscrete: Array<FloatState> =
        Array(DISCRETE_TIME_VARIABLES.size) {
            DiscreteReadState(discreteStates[it], probeStates[it], it, reads)
        }

    /** The epoch second as an integer, tracked as a read of [RemoteContext.ID_EPOCH_SECOND]. */
    internal val epochSecondState: IntState =
        object : IntState {
            override val intValue: Int
                get() {
                    reads.onDiscreteRead(EPOCH_SECOND_INDEX)
                    probeStates[EPOCH_SECOND_INDEX].intValue
                    return epochSecondIntState.intValue
                }
        }

    init {
        // Seed every field for frame time 0 (startClockMillis), so the calendar and wall-clock
        // fields hold real values before the first frame instead of 0. lastEpochSecond starts at
        // Long.MIN_VALUE, so this always counts as a new second and fills the discrete fields.
        // Must stay after the state declarations above, which updateTime writes.
        updateTime(0f)
    }

    private fun currentMillis(): Long {
        reads.onContinuousRead()
        return currentMillisState.longValue
    }

    private fun discrete(index: Int): Float = trackedDiscrete[index].floatValue

    private fun setDiscrete(index: Int, value: Float, changed: Int): Int {
        val state = discreteStates[index]
        if (state.floatValue == value) return changed
        state.floatValue = value
        return changed or (1 shl index)
    }

    /**
     * Updates the time states for the given elapsed [frameMillis] since player start.
     *
     * Continuous fields ([RemoteContext.ID_ANIMATION_TIME], [RemoteContext.ID_CONTINUOUS_SEC])
     * update every frame when [updateContinuous] is true, whereas quantized wall-clock and calendar
     * fields only update when the epoch second advances (and Compose's [MutableFloatState] only
     * emits a snapshot change when the specific field's value actually changes).
     */
    internal fun updateTime(frameMillis: Float, updateContinuous: Boolean = true): Boolean =
        trace("RcPlayer:updateTime") {
            val currentMillis = startClockMillis + frameMillis.toLong()
            val epochSec = Math.floorDiv(currentMillis, 1000L)
            val secondChanged = epochSec != lastEpochSecond
            var changed = 0
            if (secondChanged) {
                lastEpochSecond = epochSec
                val boundaryMillis = epochSec * 1000L
                val snapshot = baseClock.snapshot(boundaryMillis)
                val timeInSec = snapshot.timeInSec
                lastTimeInSec = timeInSec
                epochSecondIntState.intValue = snapshot.epochSeconds
                changed = setDiscrete(EPOCH_SECOND_INDEX, snapshot.epochSeconds.toFloat(), changed)
                changed = setDiscrete(TIME_IN_SEC_INDEX, timeInSec, changed)
                changed = setDiscrete(TIME_IN_MIN_INDEX, snapshot.timeInMin, changed)
                changed = setDiscrete(TIME_IN_HR_INDEX, snapshot.hour.toFloat(), changed)
                changed = setDiscrete(CALENDAR_MONTH_INDEX, snapshot.month.toFloat(), changed)
                changed =
                    setDiscrete(OFFSET_TO_UTC_INDEX, snapshot.offsetSeconds.toFloat(), changed)
                changed = setDiscrete(WEEK_DAY_INDEX, snapshot.dayOfWeek.toFloat(), changed)
                changed = setDiscrete(DAY_OF_MONTH_INDEX, snapshot.dayOfMonth.toFloat(), changed)
                changed = setDiscrete(DAY_OF_YEAR_INDEX, snapshot.dayOfYear.toFloat(), changed)
                changed = setDiscrete(YEAR_INDEX, snapshot.year.toFloat(), changed)
            }
            lastChangedDiscreteMask = changed
            if (updateContinuous || secondChanged) {
                currentMillisState.longValue = currentMillis
                animationTimeState.floatValue = frameMillis / 1000f
                continuousSecState.floatValue =
                    lastTimeInSec + Math.floorMod(currentMillis, 1000L) * 1e-3f
                true
            } else {
                false
            }
        }

    /**
     * Invalidates the current readers of the discrete fields in [mask] (indexed as in
     * [DISCRETE_TIME_VARIABLES]) without changing their values, and reports them in
     * [lastChangedDiscreteMask] as changed. Call it right after [updateTime].
     *
     * Slow fields such as the hour or the year rarely or never change, so the clock loop can't
     * learn from a natural change whether anything still reads them. A probe makes every composed,
     * laid out or drawn reader (and every observed derived state) read the field again, so a probe
     * that isn't followed by a read shows the field has no readers. See [ClockScheduler].
     */
    internal fun probe(mask: Int) {
        if (mask == 0) return
        for (i in probeStates.indices) {
            if (mask and (1 shl i) != 0) probeStates[i].intValue++
        }
        lastChangedDiscreteMask = lastChangedDiscreteMask or mask
    }

    /**
     * The animation time in seconds, read without recording a snapshot read or a time read. Lets a
     * draw check whether it needs time at all before making a tracked read of it.
     */
    internal fun peekAnimationTime(): Float = Snapshot.withoutReadObservation {
        animationTimeState.floatValue
    }

    internal fun timeFloatState(id: Int): State<Float> =
        when (id) {
            RemoteContext.ID_ANIMATION_TIME -> trackedAnimationTime
            RemoteContext.ID_CONTINUOUS_SEC -> trackedContinuousSec
            else -> {
                val index = discreteTimeIndex(id)
                if (index >= 0) trackedDiscrete[index] else zeroFloatState
            }
        }

    internal val reactiveNowSnapshot: RemoteClock.TimeSnapshot =
        object : RemoteClock.TimeSnapshot {
            override fun getMillis(): Long = currentMillis()

            override fun getYear(): Int = discrete(YEAR_INDEX).toInt()

            override fun getMonth(): Int = discrete(CALENDAR_MONTH_INDEX).toInt()

            override fun getDayOfMonth(): Int = discrete(DAY_OF_MONTH_INDEX).toInt()

            override fun getDayOfYear(): Int = discrete(DAY_OF_YEAR_INDEX).toInt()

            override fun getHour(): Int = discrete(TIME_IN_HR_INDEX).toInt()

            override fun getMinute(): Int = discrete(TIME_IN_MIN_INDEX).toInt() % 60

            override fun getSecond(): Int = discrete(TIME_IN_SEC_INDEX).toInt() % 60

            override fun getMillisOfSecond(): Int = Math.floorMod(currentMillis(), 1000L).toInt()

            override fun getDayOfWeek(): Int = discrete(WEEK_DAY_INDEX).toInt()

            override fun getOffsetSeconds(): Int = discrete(OFFSET_TO_UTC_INDEX).toInt()

            override fun getContinuousSeconds(): Float = trackedContinuousSec.floatValue

            override fun getTimeInSec(): Float = discrete(TIME_IN_SEC_INDEX)

            override fun getTimeInMin(): Float = discrete(TIME_IN_MIN_INDEX)

            override fun getEpochSeconds(): Int = epochSecondState.intValue
        }

    internal val reactiveClock: RemoteClock =
        object : RemoteClock {
            override fun millis(): Long = currentMillis()

            override fun nanoTime(): Long =
                startClockNanoTime + (trackedAnimationTime.floatValue * 1_000_000_000f).toLong()

            override fun getZoneId(): String = baseClock.zoneId

            override fun snapshot(millis: Long?): RemoteClock.TimeSnapshot =
                if (millis == null) reactiveNowSnapshot else baseClock.snapshot(millis)
        }
}

/**
 * Counts reads of the player's time fields, so the clock loop can tell whether anything that is
 * currently composed, laid out or drawn still depends on time.
 *
 * Reads are counted from the snapshot read path (composition, layout, draw and `derivedStateOf`
 * recomputation), so a field that only an inactive state or fit-box branch, or an unreferenced
 * expression, depends on is never read again after it changes.
 */
internal class TimeReadTracker {
    /** Number of reads of continuous fields (animation time, continuous seconds, millis). */
    internal var continuousReads: Int = 0
        private set

    /** Number of reads of each discrete field, indexed as in [DISCRETE_TIME_VARIABLES]. */
    internal val discreteReads: IntArray = IntArray(DISCRETE_TIME_VARIABLES.size)

    /** Number of reads of any time field. */
    internal var totalReads: Int = 0
        private set

    /**
     * Whether a frame draw of the host window has been seen. Until then the absence of reads says
     * nothing (for example, test environments that never run draw passes), so the clock loop never
     * idles.
     */
    @Volatile internal var drawObserved: Boolean = false

    /** Which reads should wake a waiting clock loop (a combination of the `WAKE_*` flags). */
    @Volatile internal var wakeOn: Int = WAKE_NONE

    /** Signalled when a read matching [wakeOn] happens. */
    internal val wakeups: Channel<Unit> = Channel(Channel.CONFLATED)

    internal fun onContinuousRead() {
        continuousReads++
        totalReads++
        if (wakeOn and WAKE_ON_CONTINUOUS != 0) wakeups.trySend(Unit)
    }

    internal fun onDiscreteRead(index: Int) {
        discreteReads[index]++
        totalReads++
        if (wakeOn and WAKE_ON_DISCRETE != 0) wakeups.trySend(Unit)
    }

    internal companion object {
        const val WAKE_NONE = 0
        const val WAKE_ON_CONTINUOUS = 1
        const val WAKE_ON_DISCRETE = 2
        const val WAKE_ON_ANY = WAKE_ON_CONTINUOUS or WAKE_ON_DISCRETE
    }
}

private class ContinuousReadState(
    private val delegate: FloatState,
    private val reads: TimeReadTracker,
) : FloatState {
    override val floatValue: Float
        get() {
            reads.onContinuousRead()
            return delegate.floatValue
        }
}

private class DiscreteReadState(
    private val delegate: FloatState,
    private val probe: IntState,
    private val index: Int,
    private val reads: TimeReadTracker,
) : FloatState {
    override val floatValue: Float
        get() {
            reads.onDiscreteRead(index)
            probe.intValue
            return delegate.floatValue
        }
}

/** The built-in variables that change continuously, every frame. */
internal val CONTINUOUS_TIME_VARIABLES: IntArray =
    intArrayOf(RemoteContext.ID_ANIMATION_TIME, RemoteContext.ID_CONTINUOUS_SEC)

/** The built-in wall-clock and calendar variables that change at most once per second. */
internal val DISCRETE_TIME_VARIABLES: IntArray =
    intArrayOf(
        RemoteContext.ID_TIME_IN_SEC,
        RemoteContext.ID_TIME_IN_MIN,
        RemoteContext.ID_TIME_IN_HR,
        RemoteContext.ID_CALENDAR_MONTH,
        RemoteContext.ID_OFFSET_TO_UTC,
        RemoteContext.ID_WEEK_DAY,
        RemoteContext.ID_DAY_OF_MONTH,
        RemoteContext.ID_DAY_OF_YEAR,
        RemoteContext.ID_YEAR,
        RemoteContext.ID_EPOCH_SECOND,
    )

private const val TIME_IN_SEC_INDEX = 0
private const val TIME_IN_MIN_INDEX = 1
private const val TIME_IN_HR_INDEX = 2
private const val CALENDAR_MONTH_INDEX = 3
private const val OFFSET_TO_UTC_INDEX = 4
private const val WEEK_DAY_INDEX = 5
private const val DAY_OF_MONTH_INDEX = 6
private const val DAY_OF_YEAR_INDEX = 7
private const val YEAR_INDEX = 8
private const val EPOCH_SECOND_INDEX = 9

/** Bitmask with a bit for every discrete field in [DISCRETE_TIME_VARIABLES]. */
internal val ALL_DISCRETE_TIME_MASK: Int = (1 shl DISCRETE_TIME_VARIABLES.size) - 1

/** The index of [id] in [DISCRETE_TIME_VARIABLES], or -1 if it isn't a discrete time variable. */
internal fun discreteTimeIndex(id: Int): Int = DISCRETE_TIME_VARIABLES.indexOf(id)

internal fun isContinuousTimeVariable(id: Int): Boolean =
    id == RemoteContext.ID_ANIMATION_TIME || id == RemoteContext.ID_CONTINUOUS_SEC

internal fun isDiscreteTimeVariable(id: Int): Boolean =
    when (id) {
        RemoteContext.ID_TIME_IN_SEC,
        RemoteContext.ID_TIME_IN_MIN,
        RemoteContext.ID_TIME_IN_HR,
        RemoteContext.ID_CALENDAR_MONTH,
        RemoteContext.ID_OFFSET_TO_UTC,
        RemoteContext.ID_WEEK_DAY,
        RemoteContext.ID_DAY_OF_MONTH,
        RemoteContext.ID_DAY_OF_YEAR,
        RemoteContext.ID_YEAR,
        RemoteContext.ID_EPOCH_SECOND -> true
        else -> false
    }

internal fun isTimeVariable(id: Int): Boolean =
    isContinuousTimeVariable(id) || isDiscreteTimeVariable(id)
