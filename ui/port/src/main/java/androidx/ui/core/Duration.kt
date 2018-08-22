/*
 * Copyright 2018 The Android Open Source Project
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

/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.core

import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

/**
 * A span of time, such as 27 days, 4 hours, 12 minutes, and 3 seconds.
 *
 * A `Duration` represents a difference from one point in time to another. The
 * duration may be "negative" if the difference is from a later time to an
 * earlier.
 *
 * Durations are context independent. For example, a duration of 2 days is
 * always 48 hours, even when it is added to a `DateTime` just when the
 * time zone is about to do a daylight-savings switch. (See [DateTime.add]).
 *
 * Despite the same name, a `Duration` object does not implement "Durations"
 * as specified by ISO 8601. In particular, a duration object does not keep
 * track of the individually provided members (such as "days" or "hours"), but
 * only uses these arguments to compute the length of the corresponding time
 * interval.
 *
 * To create a new Duration object, use this class's single constructor
 * giving the appropriate arguments:
 *
 *     Duration fastestMarathon = new Duration(hours:2, minutes:3, seconds:2);
 *
 * The [Duration] is the sum of all individual parts.
 * This means that individual parts can be larger than the next-bigger unit.
 * For example, [inMinutes] can be greater than 59.
 *
 *     assert(fastestMarathon.inMinutes == 123);
 *
 * All individual parts are allowed to be negative.
 *
 * Use one of the properties, such as [inDays],
 * to retrieve the integer value of the Duration in the specified time unit.
 * Note that the returned value is rounded down.
 * For example,
 *
 *     Duration aLongWeekend = new Duration(hours:88);
 *     assert(aLongWeekend.inDays == 3);
 *
 * This class provides a collection of arithmetic
 * and comparison operators,
 * plus a set of constants useful for converting time units.
 *
 * See [DateTime] to represent a point in time.
 * See [Stopwatch] to measure time-spans.
 *
 */
data class Duration(private val microseconds: Long) : Comparable<Duration> {

    companion object {
        val zero = Duration(0)

        fun create(
            days: Long = 0,
            hours: Long = 0,
            minutes: Long = 0,
            seconds: Long = 0,
            milliseconds: Long = 0,
            microseconds: Long = 0
        ): Duration {
            return Duration(TimeUnit.DAYS.toMicros(days) +
                    TimeUnit.HOURS.toMicros(hours) +
                    TimeUnit.MINUTES.toMicros(minutes) +
                    TimeUnit.SECONDS.toMicros(seconds) +
                    TimeUnit.MILLISECONDS.toMicros(milliseconds) +
                    microseconds
            )
        }
    }

    /**
     * Adds this Duration and [other] and
     * returns the sum as a new Duration object.
     */
    operator fun plus(other: Duration) = Duration(microseconds + other.microseconds)

    /**
     * Subtracts [other] from this Duration and
     * returns the difference as a new Duration object.
     */
    operator fun minus(other: Duration) = Duration(microseconds - other.microseconds)

    /**
     * Multiplies this Duration by the given [factor] and returns the result
     * as a new Duration object.
     */
    operator fun times(factor: Int) = Duration(microseconds * factor)

    /**
     * Divides this Duration by the given [quotient] and returns the truncated
     * result as a new Duration object.
     */
    operator fun div(quotient: Int) = Duration(microseconds / quotient)

    /**
     * Compares this Duration to [other], returning zero if the values are equal.
     *
     * Returns a negative integer if this `Duration` is shorter than
     * [other], or a positive integer if it is longer.
     *
     * A negative `Duration` is always considered shorter than a positive one.
     *
     * It is always the case that `duration1.compareTo(duration2) < 0` if
     * `(someDate + duration1).compareTo(someDate + duration2) < 0`.
     */
    override fun compareTo(other: Duration): Int {
        return if (microseconds < other.microseconds) -1 else
            (if ((microseconds == other.microseconds)) 0 else 1)
    }

    /**
     * Returns the number of whole days spanned by this Duration.
     */
    val inDays: Long get() = TimeUnit.MICROSECONDS.toDays(microseconds)

    /**
     * Returns the number of whole hours spanned by this Duration.
     *
     * The returned value can be greater than 23.
     */
    val inHours: Long get() = TimeUnit.MICROSECONDS.toHours(microseconds)

    /**
     * Returns the number of whole minutes spanned by this Duration.
     *
     * The returned value can be greater than 59.
     */
    val inMinutes: Long get() = TimeUnit.MICROSECONDS.toMinutes(microseconds)

    /**
     * Returns the number of whole seconds spanned by this Duration.
     *
     * The returned value can be greater than 59.
     */
    val inSeconds: Long get() = TimeUnit.MICROSECONDS.toSeconds(microseconds)

    /**
     * Returns number of whole milliseconds spanned by this Duration.
     *
     * The returned value can be greater than 999.
     */
    val inMilliseconds: Long get() = TimeUnit.MICROSECONDS.toMillis(microseconds)

    /**
     * Returns number of whole microseconds spanned by this Duration.
     */
    val inMicroseconds: Long get() = microseconds

    /**
     * Returns whether this `Duration` is negative.
     *
     * A negative `Duration` represents the difference from a later time to an
     * earlier time.
     */
    val isNegative: Boolean get() = microseconds < 0

    /**
     * Returns a new `Duration` representing the absolute value of this
     * `Duration`.
     *
     * The returned `Duration` has the same length as this one, but is always
     * positive.
     */
    fun abs() = Duration(microseconds.absoluteValue)

    /**
     * Returns a string representation of this `Duration`.
     *
     * Returns a string with hours, minutes, seconds, and microseconds, in the
     * following format: `HH:MM:SS.mmmmmm`. For example,
     *
     *     var d = new Duration(days:1, hours:1, minutes:33, microseconds: 500);
     *     d.toString();  // "25:33:00.000500"
     */
    override fun toString(): String {
        val sixDigits: (Long) -> String = { n ->
            when {
                n >= 100000 -> "$n"
                n >= 10000 -> "0$n"
                n >= 1000 -> "00$n"
                n >= 100 -> "000$n"
                n >= 10 -> "0000$n"
                else -> "00000$n"
            }
        }

        val twoDigits: (Long) -> String = { n ->
            if (n >= 10) "$n"
            else "0$n"
        }

        if (inMicroseconds < 0) {
            return "-${Duration(-microseconds)}"
        }
        val twoDigitMinutes = twoDigits(inMinutes.rem(TimeUnit.HOURS.toMinutes(1)))
        val twoDigitSeconds = twoDigits(inSeconds.rem(TimeUnit.MINUTES.toSeconds(1)))
        val sixDigitUs = sixDigits(inMicroseconds.rem(TimeUnit.SECONDS.toMicros(1)))
        return "$inHours:$twoDigitMinutes:$twoDigitSeconds.$sixDigitUs"
    }
}