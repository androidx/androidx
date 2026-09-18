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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteContext
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf

/**
 * Encapsulates reactive Compose snapshot states for all wall-clock, calendar, and continuous time
 * fields in a [GraphContext], along with a reactive [RemoteClock] and [RemoteClock.TimeSnapshot]
 * view over those states.
 */
internal class GraphTimeState(
    private val baseClock: RemoteClock = RemoteClock.SYSTEM,
    initialTimeMillis: Float = 0f,
) {
    internal val startClockMillis: Long = baseClock.millis()
    private val startClockNanoTime: Long = baseClock.nanoTime()

    private var lastEpochSecond: Long = Long.MIN_VALUE
    private var lastTimeInSec: Float = 0f

    private val currentMillisState = mutableLongStateOf(startClockMillis)
    private val animationTimeState = mutableFloatStateOf(0f)
    private val continuousSecState = mutableFloatStateOf(0f)
    internal val epochSecondState = mutableIntStateOf(0)
    private val epochSecondFloatState = mutableFloatStateOf(0f)
    private val timeInSecState = mutableFloatStateOf(0f)
    private val timeInMinState = mutableFloatStateOf(0f)
    private val timeInHrState = mutableFloatStateOf(0f)
    private val calendarMonthState = mutableFloatStateOf(0f)
    private val offsetToUtcState = mutableFloatStateOf(0f)
    private val weekDayState = mutableFloatStateOf(0f)
    private val dayOfMonthState = mutableFloatStateOf(0f)
    private val dayOfYearState = mutableFloatStateOf(0f)
    private val yearState = mutableFloatStateOf(0f)
    private val zeroFloatState = mutableFloatStateOf(0f)

    init {
        updateTime(initialTimeMillis)
    }

    /**
     * Updates the time states for the given elapsed [frameMillis] since player start.
     *
     * Continuous fields ([RemoteContext.ID_ANIMATION_TIME], [RemoteContext.ID_CONTINUOUS_SEC])
     * update every frame when [updateContinuous] is true, whereas quantized wall-clock and calendar
     * fields only update when the epoch second advances (and Compose's [MutableFloatState] only
     * emits a snapshot change when the specific field's value actually changes).
     */
    internal fun updateTime(frameMillis: Float, updateContinuous: Boolean = true): Boolean {
        val currentMillis = startClockMillis + frameMillis.toLong()
        val epochSec = Math.floorDiv(currentMillis, 1000L)
        val secondChanged = epochSec != lastEpochSecond
        if (secondChanged) {
            lastEpochSecond = epochSec
            val boundaryMillis = epochSec * 1000L
            val snapshot = baseClock.snapshot(boundaryMillis)
            val timeInSec = snapshot.timeInSec
            lastTimeInSec = timeInSec
            epochSecondState.intValue = snapshot.epochSeconds
            epochSecondFloatState.floatValue = snapshot.epochSeconds.toFloat()
            timeInSecState.floatValue = timeInSec
            timeInMinState.floatValue = snapshot.timeInMin
            timeInHrState.floatValue = snapshot.hour.toFloat()
            calendarMonthState.floatValue = snapshot.month.toFloat()
            offsetToUtcState.floatValue = snapshot.offsetSeconds.toFloat()
            weekDayState.floatValue = snapshot.dayOfWeek.toFloat()
            dayOfMonthState.floatValue = snapshot.dayOfMonth.toFloat()
            dayOfYearState.floatValue = snapshot.dayOfYear.toFloat()
            yearState.floatValue = snapshot.year.toFloat()
        }
        if (updateContinuous || secondChanged) {
            currentMillisState.longValue = currentMillis
            animationTimeState.floatValue = frameMillis / 1000f
            continuousSecState.floatValue =
                lastTimeInSec + Math.floorMod(currentMillis, 1000L) * 1e-3f
            return true
        }
        return false
    }

    internal fun timeFloatState(id: Int): State<Float> =
        when (id) {
            RemoteContext.ID_ANIMATION_TIME -> animationTimeState
            RemoteContext.ID_CONTINUOUS_SEC -> continuousSecState
            RemoteContext.ID_TIME_IN_SEC -> timeInSecState
            RemoteContext.ID_TIME_IN_MIN -> timeInMinState
            RemoteContext.ID_TIME_IN_HR -> timeInHrState
            RemoteContext.ID_CALENDAR_MONTH -> calendarMonthState
            RemoteContext.ID_OFFSET_TO_UTC -> offsetToUtcState
            RemoteContext.ID_WEEK_DAY -> weekDayState
            RemoteContext.ID_DAY_OF_MONTH -> dayOfMonthState
            RemoteContext.ID_DAY_OF_YEAR -> dayOfYearState
            RemoteContext.ID_YEAR -> yearState
            RemoteContext.ID_EPOCH_SECOND -> epochSecondFloatState
            else -> zeroFloatState
        }

    internal val reactiveNowSnapshot: RemoteClock.TimeSnapshot =
        object : RemoteClock.TimeSnapshot {
            override fun getMillis(): Long = currentMillisState.longValue

            override fun getYear(): Int = yearState.floatValue.toInt()

            override fun getMonth(): Int = calendarMonthState.floatValue.toInt()

            override fun getDayOfMonth(): Int = dayOfMonthState.floatValue.toInt()

            override fun getDayOfYear(): Int = dayOfYearState.floatValue.toInt()

            override fun getHour(): Int = timeInHrState.floatValue.toInt()

            override fun getMinute(): Int = timeInMinState.floatValue.toInt() % 60

            override fun getSecond(): Int = timeInSecState.floatValue.toInt() % 60

            override fun getMillisOfSecond(): Int =
                Math.floorMod(currentMillisState.longValue, 1000L).toInt()

            override fun getDayOfWeek(): Int = weekDayState.floatValue.toInt()

            override fun getOffsetSeconds(): Int = offsetToUtcState.floatValue.toInt()

            override fun getContinuousSeconds(): Float = continuousSecState.floatValue

            override fun getTimeInSec(): Float = timeInSecState.floatValue

            override fun getTimeInMin(): Float = timeInMinState.floatValue

            override fun getEpochSeconds(): Int = epochSecondState.intValue
        }

    internal val reactiveClock: RemoteClock =
        object : RemoteClock {
            override fun millis(): Long = currentMillisState.longValue

            override fun nanoTime(): Long =
                startClockNanoTime + (animationTimeState.floatValue * 1_000_000_000f).toLong()

            override fun getZoneId(): String = baseClock.zoneId

            override fun snapshot(millis: Long?): RemoteClock.TimeSnapshot =
                if (millis == null) reactiveNowSnapshot else baseClock.snapshot(millis)
        }
}

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
