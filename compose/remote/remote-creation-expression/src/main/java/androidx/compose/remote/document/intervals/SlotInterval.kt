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

package androidx.compose.remote.document.intervals

import androidx.annotation.RestrictTo
import androidx.compose.remote.document.RemoteContextIds
import kotlin.math.max
import kotlin.math.min
import kotlin.math.nextDown
import kotlin.math.nextUp

/**
 * Coupled 4-bound slot matching `RemoteComposeState`'s coupled `mFloatMap[id]` and
 * `mIntegerMap[id]` storage.
 *
 * At runtime:
 * - Updating a float slot writes `mFloatMap[id] = v` and `mIntegerMap[id] = (int) v`.
 * - Updating an integer slot writes `mIntegerMap[id] = v` and `mFloatMap[id] = (float) v`.
 *
 * Keeping all four bounds in a single slot makes float and integer updates atomic and preserves
 * tight bounds across mixed-type reads.
 *
 * Integer bounds with `intMin > intMax` normalize to [IntInterval.EMPTY] (`[1, 0]`), and finite
 * bounds outside `[IntInterval.MIN_BOUND, IntInterval.MAX_BOUND]` normalize to the corresponding
 * infinity sentinel.
 *
 * @property floatMin Lower float bound (or [Float.NEGATIVE_INFINITY] if unbounded below).
 * @property floatMax Upper float bound (or [Float.POSITIVE_INFINITY] if unbounded above).
 * @property intMin Lower integer bound ([IntInterval.MIN_BOUND] if unbounded below).
 * @property intMax Upper integer bound ([IntInterval.MAX_BOUND] if unbounded above).
 * @property isFloatMinInclusive Whether [floatMin] is included in the float view.
 * @property isFloatMaxInclusive Whether [floatMax] is included in the float view.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SlotInterval
@JvmOverloads
constructor(
    public val floatMin: Float,
    public val floatMax: Float,
    intMin: Int,
    intMax: Int,
    public val isFloatMinInclusive: Boolean = true,
    public val isFloatMaxInclusive: Boolean = true,
) {
    /** Lower integer bound ([IntInterval.MIN_BOUND] if unbounded below). */
    public val intMin: Int =
        if (intMin > intMax) 1 else intMin.coerceIn(IntInterval.MIN_BOUND, IntInterval.MAX_BOUND)

    /** Upper integer bound ([IntInterval.MAX_BOUND] if unbounded above). */
    public val intMax: Int =
        if (intMin > intMax) 0 else intMax.coerceIn(IntInterval.MIN_BOUND, IntInterval.MAX_BOUND)

    /** Returns the float view of this coupled slot. */
    public fun toFloatInterval(): FloatInterval =
        FloatInterval(floatMin, floatMax, isFloatMinInclusive, isFloatMaxInclusive)

    /** Returns the integer view of this coupled slot. */
    public fun toIntInterval(): IntInterval = IntInterval(intMin, intMax)

    /** Joins this slot with [other] component-wise across both float and integer views. */
    public fun union(other: SlotInterval): SlotInterval {
        val f = toFloatInterval().union(other.toFloatInterval())
        val i = toIntInterval().union(other.toIntInterval())
        return SlotInterval(f.min, f.max, i.min, i.max, f.minInclusive, f.maxInclusive)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SlotInterval) return false
        return toFloatInterval() == other.toFloatInterval() &&
            toIntInterval() == other.toIntInterval()
    }

    override fun hashCode(): Int = 31 * toFloatInterval().hashCode() + toIntInterval().hashCode()

    override fun toString(): String =
        "SlotInterval(float=${toFloatInterval()}, int=${toIntInterval()})"

    public companion object {
        /** Unbounded coupled slot across both float and integer views. */
        public val UNBOUNDED: SlotInterval =
            SlotInterval(
                floatMin = Float.NEGATIVE_INFINITY,
                floatMax = Float.POSITIVE_INFINITY,
                intMin = IntInterval.MIN_BOUND,
                intMax = IntInterval.MAX_BOUND,
            )

        /**
         * Constructs a coupled [SlotInterval] from a float write, truncating toward zero for the
         * integer bounds identically to Java `(int) float`. Widens [FloatInterval.EMPTY]
         * conservatively to [UNBOUNDED].
         */
        @JvmStatic
        public fun fromFloat(interval: FloatInterval): SlotInterval {
            if (interval.isEmpty) return UNBOUNDED
            val effectiveMin =
                if (interval.minInclusive || interval.min.isInfinite()) {
                    interval.min
                } else {
                    interval.min.nextUp()
                }
            val effectiveMax =
                if (interval.maxInclusive || interval.max.isInfinite()) {
                    interval.max
                } else {
                    interval.max.nextDown()
                }
            val iMin = floatToIntBound(effectiveMin)
            val iMax = floatToIntBound(effectiveMax)
            return SlotInterval(
                floatMin = interval.min,
                floatMax = interval.max,
                intMin = min(iMin, iMax),
                intMax = max(iMin, iMax),
                isFloatMinInclusive = interval.minInclusive,
                isFloatMaxInclusive = interval.maxInclusive,
            )
        }

        /**
         * Constructs a coupled [SlotInterval] from an integer write, promoting bounds to `Float`.
         * Widens [IntInterval.EMPTY] conservatively to [UNBOUNDED].
         */
        @JvmStatic
        public fun fromInt(interval: IntInterval): SlotInterval {
            if (interval.isEmpty) return UNBOUNDED
            val fi = interval.toFloatInterval()
            return SlotInterval(
                floatMin = fi.min,
                floatMax = fi.max,
                intMin = interval.min,
                intMax = interval.max,
                isFloatMinInclusive = fi.minInclusive,
                isFloatMaxInclusive = fi.maxInclusive,
            )
        }

        /** Constructs a coupled point [SlotInterval] from a float constant. */
        @JvmStatic
        public fun fromFloatPoint(value: Float): SlotInterval =
            fromFloat(FloatInterval.point(value))

        /** Constructs a coupled point [SlotInterval] from an integer constant. */
        @JvmStatic
        public fun fromIntPoint(value: Int): SlotInterval = fromInt(IntInterval.point(value))

        /**
         * Returns the canonical runtime range for built-in [RemoteContextIds] system globals, or
         * `null` if [id] has no built-in static bound.
         *
         * Matches `RemoteClock.TimeSnapshot` and `RemoteContext` runtime semantics.
         *
         * @param id The [RemoteContextIds] global identifier.
         * @param canvasWidth Optional fixed viewport width for [RemoteContextIds.ID_WINDOW_WIDTH].
         *   Callers must only provide a non-null value when the document runs with
         *   `RootContentBehavior.SIZING_SCALE` (or otherwise guarantees fixed, integer-truncated
         *   runtime window dimensions), because `CoreDocument.paint()` overwrites `ID_WINDOW_WIDTH`
         *   with `(int) context.mWidth` whenever `mContentSizing != SIZING_SCALE` (the default is
         *   `NONE`).
         * @param canvasHeight Optional fixed viewport height for
         *   [RemoteContextIds.ID_WINDOW_HEIGHT], subject to the same `SIZING_SCALE` and
         *   integer-dimension precondition as [canvasWidth].
         */
        @JvmStatic
        @JvmOverloads
        public fun systemGlobalOrNull(
            id: Int,
            canvasWidth: Float? = null,
            canvasHeight: Float? = null,
        ): SlotInterval? =
            when (id) {
                RemoteContextIds.ID_CONTINUOUS_SEC ->
                    fromFloat(FloatInterval(0f, 3600f, minInclusive = true, maxInclusive = false))
                RemoteContextIds.ID_TIME_IN_SEC -> fromInt(IntInterval(0, 3599))
                RemoteContextIds.ID_TIME_IN_MIN -> fromInt(IntInterval(0, 1439))
                RemoteContextIds.ID_TIME_IN_HR -> fromInt(IntInterval(0, 23))
                RemoteContextIds.ID_CALENDAR_MONTH -> fromInt(IntInterval(1, 12))
                RemoteContextIds.ID_WEEK_DAY -> fromInt(IntInterval(1, 7))
                RemoteContextIds.ID_DAY_OF_MONTH -> fromInt(IntInterval(1, 31))
                RemoteContextIds.ID_DAY_OF_YEAR -> fromInt(IntInterval(1, 366))
                RemoteContextIds.ID_WINDOW_WIDTH -> canvasWidth?.let { fromFloatPoint(it) }
                RemoteContextIds.ID_WINDOW_HEIGHT -> canvasHeight?.let { fromFloatPoint(it) }
                RemoteContextIds.ID_COMPONENT_WIDTH,
                RemoteContextIds.ID_COMPONENT_HEIGHT,
                RemoteContextIds.ID_ANIMATION_TIME ->
                    fromFloat(FloatInterval(0f, Float.POSITIVE_INFINITY))
                // Players initialize lastAnimationTime = 0.1f and compute animationTime -
                // lastAnimationTime, which can be negative on frame 1 or after setDocument resets.
                RemoteContextIds.ID_ANIMATION_DELTA_TIME -> UNBOUNDED
                else -> null
            }

        private fun floatToIntBound(value: Float): Int =
            when {
                value.isNaN() -> IntInterval.MIN_BOUND
                value <= IntInterval.MIN_BOUND.toFloat() -> IntInterval.MIN_BOUND
                value >= IntInterval.MAX_BOUND.toFloat() -> IntInterval.MAX_BOUND
                else -> value.toInt()
            }
    }
}
