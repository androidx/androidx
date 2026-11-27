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

import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.document.RemoteContextIds
import com.google.common.truth.Truth.assertThat
import kotlin.math.PI
import kotlin.math.nextDown
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class NumericIntervalTest {

    @Test
    fun triState_booleanAlgebraAndConversions() {
        assertThat(TriState.of(true)).isEqualTo(TriState.TRUE)
        assertThat(TriState.of(false)).isEqualTo(TriState.FALSE)
        assertThat(TriState.TRUE.booleanValueOrNull).isTrue()
        assertThat(TriState.FALSE.booleanValueOrNull).isFalse()
        assertThat(TriState.UNKNOWN.booleanValueOrNull).isNull()

        assertThat(!TriState.TRUE).isEqualTo(TriState.FALSE)
        assertThat(!TriState.FALSE).isEqualTo(TriState.TRUE)
        assertThat(!TriState.UNKNOWN).isEqualTo(TriState.UNKNOWN)

        assertThat(TriState.FALSE and TriState.UNKNOWN).isEqualTo(TriState.FALSE)
        assertThat(TriState.TRUE and TriState.UNKNOWN).isEqualTo(TriState.UNKNOWN)
        assertThat(TriState.TRUE and TriState.TRUE).isEqualTo(TriState.TRUE)

        assertThat(TriState.TRUE or TriState.UNKNOWN).isEqualTo(TriState.TRUE)
        assertThat(TriState.FALSE or TriState.UNKNOWN).isEqualTo(TriState.UNKNOWN)
        assertThat(TriState.FALSE or TriState.FALSE).isEqualTo(TriState.FALSE)

        assertThat(TriState.TRUE.toIntInterval()).isEqualTo(IntInterval.point(1))
        assertThat(TriState.FALSE.toIntInterval()).isEqualTo(IntInterval.ZERO)
        assertThat(TriState.UNKNOWN.toIntInterval()).isEqualTo(IntInterval(0, 1))

        assertThat(TriState.TRUE.toFloatInterval()).isEqualTo(FloatInterval.point(1f))
        assertThat(TriState.FALSE.toFloatInterval()).isEqualTo(FloatInterval.ZERO)
        assertThat(TriState.UNKNOWN.toFloatInterval()).isEqualTo(FloatInterval.UNIT)

        assertThat(FloatInterval.MAX_EXACT_INT).isEqualTo(16_777_216)
    }

    @Test
    fun intervalComparison_wireCodeParityNegateFlipAndNarrow() {
        assertThat(IntervalComparison.fromWireCode(ConditionalOperations.TYPE_EQ.toInt()))
            .isEqualTo(IntervalComparison.EQ)
        assertThat(IntervalComparison.fromWireCode(ConditionalOperations.TYPE_NEQ.toInt()))
            .isEqualTo(IntervalComparison.NEQ)
        assertThat(IntervalComparison.fromWireCode(ConditionalOperations.TYPE_LT.toInt()))
            .isEqualTo(IntervalComparison.LT)
        assertThat(IntervalComparison.fromWireCode(ConditionalOperations.TYPE_LTE.toInt()))
            .isEqualTo(IntervalComparison.LTE)
        assertThat(IntervalComparison.fromWireCode(ConditionalOperations.TYPE_GT.toInt()))
            .isEqualTo(IntervalComparison.GT)
        assertThat(IntervalComparison.fromWireCode(ConditionalOperations.TYPE_GTE.toInt()))
            .isEqualTo(IntervalComparison.GTE)
        assertThat(IntervalComparison.fromWireCode(ConditionalOperations.TYPE_CHANGED.toInt()))
            .isNull()

        for (cmp in IntervalComparison.entries) {
            assertThat(cmp.negate().negate()).isEqualTo(cmp)
            assertThat(cmp.flip().flip()).isEqualTo(cmp)
        }

        assertThat(IntervalComparison.EQ.selfComparisonResult()).isEqualTo(TriState.TRUE)
        assertThat(IntervalComparison.LTE.selfComparisonResult()).isEqualTo(TriState.TRUE)
        assertThat(IntervalComparison.GTE.selfComparisonResult()).isEqualTo(TriState.TRUE)
        assertThat(IntervalComparison.NEQ.selfComparisonResult()).isEqualTo(TriState.FALSE)
        assertThat(IntervalComparison.LT.selfComparisonResult()).isEqualTo(TriState.FALSE)
        assertThat(IntervalComparison.GT.selfComparisonResult()).isEqualTo(TriState.FALSE)

        val (fA, fB) =
            IntervalComparison.LT.narrowFloat(FloatInterval(0f, 20f), FloatInterval(5f, 10f))
        assertThat(fA).isEqualTo(FloatInterval(0f, 10f, minInclusive = true, maxInclusive = false))
        assertThat(fB).isEqualTo(FloatInterval(5f, 10f))

        val (iA, iB) = IntervalComparison.LT.narrowInt(IntInterval(0, 20), IntInterval(5, 10))
        assertThat(iA).isEqualTo(IntInterval(0, 9))
        assertThat(iB).isEqualTo(IntInterval(5, 10))

        assertThat(IntervalComparison.LT.compareLongRangeAgainstZero(-5L, -1L))
            .isEqualTo(TriState.TRUE)
        assertThat(IntervalComparison.LT.compareLongRangeAgainstZero(0L, 5L))
            .isEqualTo(TriState.FALSE)
        assertThat(IntervalComparison.LT.compareLongRangeAgainstZero(-1L, 1L))
            .isEqualTo(TriState.UNKNOWN)
        // Empty Long range (lo > hi) returns UNKNOWN across all comparison ops
        for (cmp in IntervalComparison.entries) {
            assertThat(cmp.compareLongRangeAgainstZero(5L, -5L)).isEqualTo(TriState.UNKNOWN)
        }

        // Empty narrowing short-circuits BOTH operands to EMPTY
        for (cmp in IntervalComparison.entries) {
            assertThat(cmp.narrowFloat(FloatInterval.EMPTY, FloatInterval(0f, 10f)))
                .isEqualTo(Pair(FloatInterval.EMPTY, FloatInterval.EMPTY))
            assertThat(cmp.narrowFloat(FloatInterval(0f, 10f), FloatInterval.EMPTY))
                .isEqualTo(Pair(FloatInterval.EMPTY, FloatInterval.EMPTY))
            assertThat(cmp.narrowInt(IntInterval.EMPTY, IntInterval(0, 10)))
                .isEqualTo(Pair(IntInterval.EMPTY, IntInterval.EMPTY))
            assertThat(cmp.narrowInt(IntInterval(0, 10), IntInterval.EMPTY))
                .isEqualTo(Pair(IntInterval.EMPTY, IntInterval.EMPTY))
        }

        // All comparison ops on narrowFloat and narrowInt (including upper sentinel range)
        assertThat(
                IntervalComparison.EQ.narrowFloat(FloatInterval(0f, 10f), FloatInterval(5f, 20f))
            )
            .isEqualTo(Pair(FloatInterval(5f, 10f), FloatInterval(5f, 10f)))
        assertThat(
                IntervalComparison.NEQ.narrowFloat(FloatInterval(0f, 20f), FloatInterval(5f, 10f))
            )
            .isEqualTo(Pair(FloatInterval(0f, 20f), FloatInterval(5f, 10f)))
        assertThat(
                IntervalComparison.LTE.narrowFloat(FloatInterval(0f, 20f), FloatInterval(5f, 10f))
            )
            .isEqualTo(Pair(FloatInterval(0f, 10f), FloatInterval(5f, 10f)))
        assertThat(
                IntervalComparison.GT.narrowFloat(FloatInterval(0f, 20f), FloatInterval(5f, 10f))
            )
            .isEqualTo(
                Pair(
                    FloatInterval(5f, 20f, minInclusive = false, maxInclusive = true),
                    FloatInterval(5f, 10f),
                )
            )
        assertThat(
                IntervalComparison.GTE.narrowFloat(FloatInterval(0f, 20f), FloatInterval(5f, 10f))
            )
            .isEqualTo(Pair(FloatInterval(5f, 20f), FloatInterval(5f, 10f)))
        assertThat(IntervalComparison.NEQ.narrowInt(IntInterval(0, 20), IntInterval(5, 10)))
            .isEqualTo(Pair(IntInterval(0, 20), IntInterval(5, 10)))
        assertThat(IntervalComparison.GT.narrowInt(IntInterval(0, 20), IntInterval(5, 10)))
            .isEqualTo(Pair(IntInterval(6, 20), IntInterval(5, 10)))
        assertThat(IntervalComparison.GTE.narrowInt(IntInterval(0, 20), IntInterval(5, 10)))
            .isEqualTo(Pair(IntInterval(5, 20), IntInterval(5, 10)))
        assertThat(IntervalComparison.LTE.narrowInt(IntInterval(0, 20), IntInterval(5, 10)))
            .isEqualTo(Pair(IntInterval(0, 10), IntInterval(5, 10)))
        val highSentinel = IntInterval(IntInterval.MAX_BOUND, IntInterval.MAX_BOUND)
        val (narrowHiA, narrowHiB) = IntervalComparison.LT.narrowInt(highSentinel, highSentinel)
        assertThat(narrowHiA.isEmpty).isFalse()
        assertThat(narrowHiB.isEmpty).isFalse()
    }

    @Test
    fun floatInterval_arithmeticOpenClosedTrigRemAndComparisons() {
        val halfOpen = FloatInterval(0f, 10f, minInclusive = true, maxInclusive = false)
        assertThat(0f in halfOpen).isTrue()
        assertThat(10f in halfOpen).isFalse()
        assertThat(halfOpen.isAlwaysLessThan(FloatInterval.point(10f))).isTrue()
        assertThat(halfOpen.compareEqualTo(FloatInterval.point(10f))).isEqualTo(TriState.FALSE)

        // Empty interval normalization in equals/hashCode, NaN, and -0.0f handling
        val reversedEmpty = FloatInterval(5f, 2f)
        val openSingletonEmpty = FloatInterval(5f, 5f, minInclusive = false, maxInclusive = true)
        val nanEmpty = FloatInterval(Float.NaN, 1f)
        assertThat(reversedEmpty.isEmpty).isTrue()
        assertThat(openSingletonEmpty.isEmpty).isTrue()
        assertThat(nanEmpty.isEmpty).isTrue()
        assertThat(reversedEmpty).isEqualTo(FloatInterval.EMPTY)
        assertThat(openSingletonEmpty).isEqualTo(FloatInterval.EMPTY)
        assertThat(nanEmpty).isEqualTo(FloatInterval.EMPTY)
        assertThat(reversedEmpty.hashCode()).isEqualTo(FloatInterval.EMPTY.hashCode())
        assertThat(openSingletonEmpty.hashCode()).isEqualTo(FloatInterval.EMPTY.hashCode())
        assertThat(nanEmpty.hashCode()).isEqualTo(FloatInterval.EMPTY.hashCode())

        assertThat(FloatInterval.point(Float.NaN)).isEqualTo(FloatInterval.UNBOUNDED)
        assertThat(Float.NaN in FloatInterval.UNBOUNDED).isFalse()
        val negZero = FloatInterval(-0.0f, -0.0f)
        assertThat(negZero).isEqualTo(FloatInterval.ZERO)
        assertThat(negZero.hashCode()).isEqualTo(FloatInterval.ZERO.hashCode())
        assertThat(-0.0f in FloatInterval.ZERO).isTrue()

        // Arithmetic & ZERO absorption
        assertThat(FloatInterval(1f, 3f) + FloatInterval(2f, 4f)).isEqualTo(FloatInterval(3f, 7f))
        assertThat(FloatInterval(1f, 3f) - FloatInterval(2f, 4f)).isEqualTo(FloatInterval(-3f, 1f))
        assertThat(-FloatInterval(1f, 3f)).isEqualTo(FloatInterval(-3f, -1f))
        assertThat(FloatInterval.UNBOUNDED * FloatInterval.ZERO).isEqualTo(FloatInterval.ZERO)
        assertThat(FloatInterval(1f, 2f) / FloatInterval(-1f, 1f))
            .isEqualTo(FloatInterval.UNBOUNDED)
        assertThat(FloatInterval(-5f, 3f).abs()).isEqualTo(FloatInterval(0f, 5f))
        assertThat(FloatInterval(1f, 5f).min(FloatInterval(2f, 3f)))
            .isEqualTo(FloatInterval(1f, 3f))
        assertThat(FloatInterval(1f, 5f).max(FloatInterval(2f, 3f)))
            .isEqualTo(FloatInterval(2f, 5f))
        assertThat(FloatInterval(1f, 4f).union(FloatInterval(6f, 10f)))
            .isEqualTo(FloatInterval(1f, 10f))
        assertThat(FloatInterval(1f, 6f).intersect(FloatInterval(4f, 10f)))
            .isEqualTo(FloatInterval(4f, 6f))
        assertThat(FloatInterval(-1f, 10f).clamp(FloatInterval.point(2f), FloatInterval.point(4f)))
            .isEqualTo(FloatInterval(2f, 4f))

        // IEEE-754 rounding soundness: computed finite bounds on +, -, *, /, sqrt are closed
        val openUnit = FloatInterval(0f, 1f, minInclusive = true, maxInclusive = false)
        val bigShift = FloatInterval.point(8388608f) // 2^23
        val shifted = openUnit + bigShift
        assertThat(0.75f + 8388608f in shifted).isTrue() // 8388609f must be contained!
        assertThat(shifted.maxInclusive).isTrue()
        val sqrtOpen = FloatInterval(0f, 2f, minInclusive = true, maxInclusive = false).sqrt()
        assertThat(sqrt(2f.nextDown()) in sqrtOpen).isTrue()
        assertThat(sqrtOpen.maxInclusive).isTrue()
        assertThat(FloatInterval(0f, 4f).sqrt()).isEqualTo(FloatInterval(0f, 2f))
        assertThat(FloatInterval(-0.0f, 4f).sqrt()).isEqualTo(FloatInterval(0f, 2f))
        assertThat(FloatInterval(-4f, 9f).sqrt()).isEqualTo(FloatInterval.UNBOUNDED)
        assertThat(FloatInterval(-4f, -1f).sqrt()).isEqualTo(FloatInterval.UNBOUNDED)
        assertThat(FloatInterval.EMPTY.sqrt()).isEqualTo(FloatInterval.EMPTY)

        // Rounding: floor/ceil return closed intervals; round matches (float) Math.round(x)
        assertThat(halfOpen.floor()).isEqualTo(FloatInterval(0f, 9f))
        assertThat(halfOpen.ceil()).isEqualTo(FloatInterval(0f, 10f))
        assertThat(FloatInterval.point(0.5f).round()).isEqualTo(FloatInterval.point(1f))
        assertThat(FloatInterval.point(-0.5f).round()).isEqualTo(FloatInterval.point(0f))
        assertThat(FloatInterval.point(-1.5f).round()).isEqualTo(FloatInterval.point(-1f))
        assertThat(FloatInterval(1e12f, 2e12f).round())
            .isEqualTo(FloatInterval.point(Int.MAX_VALUE.toFloat()))
        assertThat(FloatInterval.UNBOUNDED.round())
            .isEqualTo(FloatInterval(Int.MIN_VALUE.toFloat(), Int.MAX_VALUE.toFloat()))

        // Periodic trig & rem (including infinite divisor keeping infinite endpoints closed)
        assertThat(FloatInterval(0f, (2.0 * PI).toFloat()).sin()).isEqualTo(FloatInterval(-1f, 1f))
        assertThat(FloatInterval(0f, PI.toFloat()).cos()).isEqualTo(FloatInterval(-1f, 1f))
        val cosHalfPi = FloatInterval(0f, (PI / 2.0).toFloat()).cos()
        assertThat(cosHalfPi.max).isEqualTo(1f)
        assertThat(cosHalfPi.min in -1e-6f..0f).isTrue()
        val remHalfOpen =
            FloatInterval(0f, 3600f, minInclusive = true, maxInclusive = false) %
                FloatInterval.point(60f)
        assertThat(remHalfOpen)
            .isEqualTo(FloatInterval(0f, 60f, minInclusive = true, maxInclusive = false))

        val remSameBucket = FloatInterval(65f, 70f) % FloatInterval.point(60f)
        assertThat(remSameBucket).isEqualTo(FloatInterval(5f, 10f))

        val remInfDiv = FloatInterval(0f, 10f) % FloatInterval(1f, Float.POSITIVE_INFINITY)
        assertThat(remInfDiv).isEqualTo(FloatInterval(0f, Float.POSITIVE_INFINITY))
        assertThat(remInfDiv.maxInclusive).isTrue()

        // All FloatInterval comparison operators
        val lo = FloatInterval(0f, 5f)
        val hi = FloatInterval(6f, 10f)
        assertThat(lo.compare(IntervalComparison.LT, hi)).isEqualTo(TriState.TRUE)
        assertThat(lo.compare(IntervalComparison.LTE, hi)).isEqualTo(TriState.TRUE)
        assertThat(lo.compare(IntervalComparison.GT, hi)).isEqualTo(TriState.FALSE)
        assertThat(lo.compare(IntervalComparison.GTE, hi)).isEqualTo(TriState.FALSE)
        assertThat(lo.compare(IntervalComparison.EQ, hi)).isEqualTo(TriState.FALSE)
        assertThat(lo.compare(IntervalComparison.NEQ, hi)).isEqualTo(TriState.TRUE)
    }

    @Test
    fun intInterval_arithmeticOverflowSentinelsRemAndComparisons() {
        assertThat(IntInterval(1, 3) + IntInterval(2, 4)).isEqualTo(IntInterval(3, 7))
        assertThat(IntInterval(1, 3) - IntInterval(2, 4)).isEqualTo(IntInterval(-3, 1))
        assertThat(-IntInterval(1, 3)).isEqualTo(IntInterval(-3, -1))
        assertThat(IntInterval.UNBOUNDED * IntInterval.ZERO).isEqualTo(IntInterval.ZERO)
        assertThat(IntInterval(1, 10) / IntInterval(-1, 1)).isEqualTo(IntInterval.UNBOUNDED)
        assertThat(IntInterval(-5, 3).abs()).isEqualTo(IntInterval(0, 5))
        assertThat(IntInterval(1, 5).min(IntInterval(2, 3))).isEqualTo(IntInterval(1, 3))
        assertThat(IntInterval(1, 5).max(IntInterval(2, 3))).isEqualTo(IntInterval(2, 5))
        assertThat(IntInterval(1, 4).union(IntInterval(6, 10))).isEqualTo(IntInterval(1, 10))
        assertThat(IntInterval(1, 6).intersect(IntInterval(4, 10))).isEqualTo(IntInterval(4, 6))
        assertThat(IntInterval(-1, 10).clamp(IntInterval.point(2), IntInterval.point(4)))
            .isEqualTo(IntInterval(2, 4))
        assertThat(IntInterval(-5, 20).clampToRange(0, 10)).isEqualTo(IntInterval(0, 10))
        assertThat(IntInterval.UNBOUNDED.clampToRange(0, 255)).isEqualTo(IntInterval(0, 255))
        assertThat(IntInterval.EMPTY.clampToRange(0, 10)).isEqualTo(IntInterval.EMPTY)
        assertFailsWith<IllegalArgumentException> { IntInterval(0, 10).clampToRange(10, 0) }
        assertThat(IntInterval(0, 3599) % IntInterval.point(60)).isEqualTo(IntInterval(0, 59))
        assertThat(IntInterval(65, 70) % IntInterval.point(60)).isEqualTo(IntInterval(5, 10))

        // Constructor normalizes out-of-sentinel and empty bounds
        assertThat(IntInterval(Int.MIN_VALUE, Int.MAX_VALUE)).isEqualTo(IntInterval.UNBOUNDED)
        assertThat(IntInterval(5, 2)).isEqualTo(IntInterval.EMPTY)

        // Non-wrapping sentinel saturation
        val huge = IntInterval.point(IntInterval.MAX_BOUND - 1) + IntInterval.point(10)
        assertThat(huge.isUnbounded).isTrue()
        assertThat(huge.isEmpty).isFalse()
        assertThat((IntInterval.MAX_BOUND + 9) in huge).isTrue()

        // 32-bit wrap-around soundness regressions
        assertThat(IntInterval.point(Int.MAX_VALUE) + IntInterval.point(1))
            .isEqualTo(IntInterval.UNBOUNDED)
        assertThat(IntInterval.NON_NEGATIVE + IntInterval.point(1)).isEqualTo(IntInterval.UNBOUNDED)
        assertThat(IntInterval.point(Int.MIN_VALUE) - IntInterval.point(1))
            .isEqualTo(IntInterval.UNBOUNDED)
        assertThat(IntInterval.point(50_000) * IntInterval.point(50_000))
            .isEqualTo(IntInterval.UNBOUNDED)
        assertThat(IntInterval.UNBOUNDED.abs()).isEqualTo(IntInterval.UNBOUNDED)
        assertThat(Int.MIN_VALUE in IntInterval.UNBOUNDED.abs()).isTrue()
        assertThat(-IntInterval.UNBOUNDED).isEqualTo(IntInterval.UNBOUNDED)
        assertThat(-IntInterval.point(Int.MIN_VALUE)).isEqualTo(IntInterval.UNBOUNDED)
        assertThat(IntInterval.point(Int.MIN_VALUE) / IntInterval.point(-1))
            .isEqualTo(IntInterval.UNBOUNDED)
        // Non-wrapping unbounded ops retain tight bounds
        assertThat(IntInterval.NON_NEGATIVE + IntInterval(-5, -1))
            .isEqualTo(IntInterval(-5, IntInterval.MAX_BOUND))
        assertThat(-IntInterval.NON_NEGATIVE).isEqualTo(IntInterval(IntInterval.MIN_BOUND, 0))

        assertThat(IntInterval(0, 5).compare(IntervalComparison.LT, IntInterval(6, 10)))
            .isEqualTo(TriState.TRUE)
        assertThat(IntInterval(0, 5).compare(IntervalComparison.LTE, IntInterval(5, 10)))
            .isEqualTo(TriState.TRUE)
        assertThat(IntInterval(6, 10).compare(IntervalComparison.LT, IntInterval(0, 5)))
            .isEqualTo(TriState.FALSE)
        assertThat(IntInterval(6, 10).compare(IntervalComparison.GT, IntInterval(0, 5)))
            .isEqualTo(TriState.TRUE)
        assertThat(IntInterval(5, 10).compare(IntervalComparison.GTE, IntInterval(0, 5)))
            .isEqualTo(TriState.TRUE)
        assertThat(IntInterval.point(7).compare(IntervalComparison.EQ, IntInterval.point(7)))
            .isEqualTo(TriState.TRUE)
        assertThat(IntInterval(0, 5).compare(IntervalComparison.NEQ, IntInterval(6, 10)))
            .isEqualTo(TriState.TRUE)
        assertThat(IntInterval(0, 6).compareLessThan(IntInterval(5, 10)))
            .isEqualTo(TriState.UNKNOWN)
    }

    @Test
    fun slotInterval_coupledFloatIntNormalizationUnionAndSystemGlobals() {
        // Normalization, equals/hashCode, and union idempotence
        val unnormalizedEmpty = SlotInterval(0f, 1f, 5, 2)
        val canonicalEmpty = SlotInterval(0f, 1f, 1, 0)
        assertThat(unnormalizedEmpty).isEqualTo(canonicalEmpty)
        assertThat(unnormalizedEmpty.hashCode()).isEqualTo(canonicalEmpty.hashCode())
        assertThat(unnormalizedEmpty.union(unnormalizedEmpty)).isEqualTo(unnormalizedEmpty)

        val unnormalizedSentinel = SlotInterval(0f, 1f, Int.MIN_VALUE, Int.MAX_VALUE)
        val canonicalSentinel = SlotInterval(0f, 1f, IntInterval.MIN_BOUND, IntInterval.MAX_BOUND)
        assertThat(unnormalizedSentinel).isEqualTo(canonicalSentinel)
        assertThat(unnormalizedSentinel.hashCode()).isEqualTo(canonicalSentinel.hashCode())
        assertThat(unnormalizedSentinel.union(unnormalizedSentinel)).isEqualTo(unnormalizedSentinel)

        val joined = SlotInterval.fromFloatPoint(1.5f).union(SlotInterval.fromIntPoint(5))
        assertThat(joined.toFloatInterval()).isEqualTo(FloatInterval(1.5f, 5f))
        assertThat(joined.toIntInterval()).isEqualTo(IntInterval(1, 5))

        val secSlot = SlotInterval.systemGlobalOrNull(RemoteContextIds.ID_CONTINUOUS_SEC)
        assertNotNull(secSlot)
        assertThat(secSlot.toFloatInterval())
            .isEqualTo(FloatInterval(0f, 3600f, minInclusive = true, maxInclusive = false))
        assertThat(secSlot.toIntInterval()).isEqualTo(IntInterval(0, 3599))

        assertThat(
                SlotInterval.systemGlobalOrNull(RemoteContextIds.ID_TIME_IN_SEC)?.toIntInterval()
            )
            .isEqualTo(IntInterval(0, 3599))
        assertThat(
                SlotInterval.systemGlobalOrNull(RemoteContextIds.ID_TIME_IN_MIN)?.toIntInterval()
            )
            .isEqualTo(IntInterval(0, 1439))
        assertThat(SlotInterval.systemGlobalOrNull(RemoteContextIds.ID_TIME_IN_HR)?.toIntInterval())
            .isEqualTo(IntInterval(0, 23))
        assertThat(
                SlotInterval.systemGlobalOrNull(RemoteContextIds.ID_CALENDAR_MONTH)?.toIntInterval()
            )
            .isEqualTo(IntInterval(1, 12))
        assertThat(SlotInterval.systemGlobalOrNull(RemoteContextIds.ID_WEEK_DAY)?.toIntInterval())
            .isEqualTo(IntInterval(1, 7))
        assertThat(
                SlotInterval.systemGlobalOrNull(RemoteContextIds.ID_DAY_OF_MONTH)?.toIntInterval()
            )
            .isEqualTo(IntInterval(1, 31))
        assertThat(
                SlotInterval.systemGlobalOrNull(RemoteContextIds.ID_DAY_OF_YEAR)?.toIntInterval()
            )
            .isEqualTo(IntInterval(1, 366))

        val winW =
            SlotInterval.systemGlobalOrNull(RemoteContextIds.ID_WINDOW_WIDTH, canvasWidth = 450f)
        assertNotNull(winW)
        assertThat(winW.toFloatInterval()).isEqualTo(FloatInterval.point(450f))
        assertThat(winW.toIntInterval()).isEqualTo(IntInterval.point(450))
        assertThat(SlotInterval.systemGlobalOrNull(RemoteContextIds.ID_WINDOW_WIDTH)).isNull()

        val winH =
            SlotInterval.systemGlobalOrNull(RemoteContextIds.ID_WINDOW_HEIGHT, canvasHeight = 800f)
        assertThat(winH?.toFloatInterval()).isEqualTo(FloatInterval.point(800f))
        assertThat(SlotInterval.systemGlobalOrNull(RemoteContextIds.ID_WINDOW_HEIGHT)).isNull()

        for (id in
            intArrayOf(
                RemoteContextIds.ID_COMPONENT_WIDTH,
                RemoteContextIds.ID_COMPONENT_HEIGHT,
                RemoteContextIds.ID_ANIMATION_TIME,
            )) {
            assertThat(SlotInterval.systemGlobalOrNull(id)?.toFloatInterval())
                .isEqualTo(FloatInterval(0f, Float.POSITIVE_INFINITY))
        }
        assertThat(SlotInterval.systemGlobalOrNull(RemoteContextIds.ID_ANIMATION_DELTA_TIME))
            .isEqualTo(SlotInterval.UNBOUNDED)
        assertThat(SlotInterval.systemGlobalOrNull(9999)).isNull()
        assertThat(SlotInterval.fromFloat(FloatInterval.EMPTY)).isEqualTo(SlotInterval.UNBOUNDED)
        assertThat(SlotInterval.fromInt(IntInterval.EMPTY)).isEqualTo(SlotInterval.UNBOUNDED)
    }
}
