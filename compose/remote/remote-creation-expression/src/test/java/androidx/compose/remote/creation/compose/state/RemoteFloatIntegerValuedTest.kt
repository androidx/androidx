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

package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Utils
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteFloatIntegerValuedTest {

    private val dynamicFloat = RemoteFloat.createNamedRemoteFloat("f", 1.5f)
    private val otherDynamicFloat = RemoteFloat.createNamedRemoteFloat("g", 2.5f)
    private val intFloatA = floor(dynamicFloat)
    private val intFloatB = ceil(otherDynamicFloat)
    private val intFloatC = round(dynamicFloat)
    private val dynamicCond = RemoteBoolean.createNamedRemoteBoolean("cond", true)

    @Test
    fun constants() {
        assertThat(0f.rf.cacheKey.isResultWholeNumber).isTrue()
        assertThat((-0f).rf.cacheKey.isResultWholeNumber).isTrue()
        assertThat(5f.rf.cacheKey.isResultWholeNumber).isTrue()
        assertThat((-42f).rf.cacheKey.isResultWholeNumber).isTrue()
        assertThat(16777216f.rf.cacheKey.isResultWholeNumber).isTrue()

        assertThat(0.5f.rf.cacheKey.isResultWholeNumber).isFalse()
        assertThat((-0.001f).rf.cacheKey.isResultWholeNumber).isFalse()
        assertThat(Float.POSITIVE_INFINITY.rf.cacheKey.isResultWholeNumber).isFalse()
        assertThat(Float.NEGATIVE_INFINITY.rf.cacheKey.isResultWholeNumber).isFalse()
    }

    @Test
    fun constantCacheKeys() {
        assertThat(RemoteConstantCacheKey(42).isResultWholeNumber).isTrue()
        assertThat(RemoteConstantCacheKey(42L).isResultWholeNumber).isTrue()
        assertThat(RemoteConstantCacheKey(42.toShort()).isResultWholeNumber).isTrue()
        assertThat(RemoteConstantCacheKey(42.toByte()).isResultWholeNumber).isTrue()
        assertThat(RemoteConstantCacheKey(true).isResultWholeNumber).isTrue()
        assertThat(RemoteConstantCacheKey(3.0f).isResultWholeNumber).isTrue()
        assertThat(RemoteConstantCacheKey(3.0).isResultWholeNumber).isTrue()

        assertThat(RemoteConstantCacheKey(3.25f).isResultWholeNumber).isFalse()
        assertThat(RemoteConstantCacheKey(3.25).isResultWholeNumber).isFalse()
        assertThat(RemoteConstantCacheKey(Float.POSITIVE_INFINITY).isResultWholeNumber).isFalse()
        assertThat(RemoteConstantCacheKey(Double.NEGATIVE_INFINITY).isResultWholeNumber).isFalse()
        assertThat(RemoteConstantCacheKey("42").isResultWholeNumber).isFalse()
        assertThat(RemoteConstantCacheKey(null).isResultWholeNumber).isFalse()
    }

    @Test
    fun namedAndMutableVariables() {
        assertThat(
                RemoteFloat.createNamedRemoteFloat("named_int_default", 5f)
                    .cacheKey
                    .isResultWholeNumber
            )
            .isFalse()
        assertThat(
                RemoteFloat.createNamedRemoteFloat("named_expr", 5f.rf).cacheKey.isResultWholeNumber
            )
            .isFalse()
        assertThat(MutableRemoteFloat(5f).cacheKey.isResultWholeNumber).isFalse()
        assertThat(MutableRemoteFloat(5f.rf).cacheKey.isResultWholeNumber).isFalse()
        assertThat(MutableRemoteFloat.createReserved().cacheKey.isResultWholeNumber).isFalse()
        assertThat(
                MutableRemoteFloat.createMutableForId(RemoteContext.FLOAT_TIME_IN_SEC)
                    .cacheKey
                    .isResultWholeNumber
            )
            .isFalse()
        assertThat(RemoteFloat { 5f.rf }.cacheKey.isResultWholeNumber).isFalse()

        val namedInt = RemoteInt.createNamedRemoteInt("count", 5)
        assertThat(namedInt.cacheKey.isResultWholeNumber).isTrue()
        assertThat(namedInt.toRemoteFloat().cacheKey.isResultWholeNumber).isTrue()

        val mutableInt = MutableRemoteInt(5)
        assertThat(mutableInt.cacheKey.isResultWholeNumber).isTrue()
        assertThat(mutableInt.toRemoteFloat().cacheKey.isResultWholeNumber).isTrue()

        val namedBool = RemoteBoolean.createNamedRemoteBoolean("flag", true)
        assertThat(namedBool.cacheKey.isResultWholeNumber).isTrue()
        assertThat(namedBool.toRemoteInt().toRemoteFloat().cacheKey.isResultWholeNumber).isTrue()

        val namedLong = RemoteLong.createNamedRemoteLong("ts", 100L)
        assertThat(namedLong.cacheKey.isResultWholeNumber).isTrue()
        assertThat((namedLong + RemoteLong(1L)).cacheKey.isResultWholeNumber).isTrue()

        val namedString = RemoteString.createNamedRemoteString("label", "abc")
        assertThat(namedString.length.cacheKey.isResultWholeNumber).isTrue()
        assertThat(namedString.length.toRemoteFloat().cacheKey.isResultWholeNumber).isTrue()
        assertThat(namedString.isEmpty.cacheKey.isResultWholeNumber).isTrue()
        assertThat(namedString.isNotEmpty.cacheKey.isResultWholeNumber).isTrue()

        val intArray = RemoteIntArray(1, 2, 3)
        assertThat(intArray.cacheKey.isResultWholeNumber).isFalse()
        assertThat(intArray[namedInt].cacheKey.isResultWholeNumber).isTrue()
        assertThat(intArray[namedInt].toRemoteFloat().cacheKey.isResultWholeNumber).isTrue()

        val floatArray = RemoteFloatArray(1f, 2f, 3f)
        assertThat(floatArray.cacheKey.isResultWholeNumber).isFalse()
        assertThat(floatArray[namedInt].cacheKey.isResultWholeNumber).isFalse()

        assertThat(dynamicFloat.toRemoteInt().toRemoteFloat().cacheKey.isResultWholeNumber).isTrue()
    }

    @Test
    fun timeAndContextVariables() {
        assertThat(RemoteTimeVariables.secondOfHour.toRemoteFloat().cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(RemoteTimeVariables.minuteOfDay.toRemoteFloat().cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(RemoteTimeVariables.hourOfDay.toRemoteFloat().cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(RemoteTimeVariables.month.toRemoteFloat().cacheKey.isResultWholeNumber).isTrue()
        assertThat(RemoteTimeVariables.dayOfMonth.toRemoteFloat().cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(RemoteTimeVariables.dayOfWeek.toRemoteFloat().cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(RemoteTimeVariables.dayOfYear.toRemoteFloat().cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(RemoteTimeVariables.year.toRemoteFloat().cacheKey.isResultWholeNumber).isTrue()
        assertThat(
                RemoteTimeVariables.utcOffsetSeconds.toRemoteFloat().cacheKey.isResultWholeNumber
            )
            .isTrue()
        assertThat(RemoteTimeVariables.epochSecond.toRemoteFloat().cacheKey.isResultWholeNumber)
            .isTrue()

        assertThat(RemoteFloat(RemoteContext.FLOAT_TIME_IN_SEC).cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(RemoteFloat(RemoteContext.FLOAT_TIME_IN_MIN).cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(RemoteFloat(RemoteContext.FLOAT_TIME_IN_HR).cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(RemoteFloat(RemoteContext.FLOAT_CALENDAR_MONTH).cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(RemoteFloat(RemoteContext.FLOAT_DAY_OF_MONTH).cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(RemoteFloat(RemoteContext.FLOAT_WEEK_DAY).cacheKey.isResultWholeNumber).isTrue()
        assertThat(RemoteFloat(RemoteContext.FLOAT_DAY_OF_YEAR).cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(RemoteFloat(RemoteContext.FLOAT_YEAR).cacheKey.isResultWholeNumber).isTrue()
        assertThat(RemoteFloat(RemoteContext.FLOAT_OFFSET_TO_UTC).cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(RemoteStateIdKey(RemoteContext.ID_EPOCH_SECOND).isResultWholeNumber).isTrue()

        assertThat(RemoteTimeVariables.continuousSeconds.cacheKey.isResultWholeNumber).isFalse()
        assertThat(RemoteTimeVariables.animationTime.cacheKey.isResultWholeNumber).isFalse()
        assertThat(RemoteTimeVariables.animationDeltaTime.cacheKey.isResultWholeNumber).isFalse()
        assertThat(RemoteFloat(RemoteContext.FLOAT_API_LEVEL).cacheKey.isResultWholeNumber)
            .isFalse()
        assertThat(RemoteFloat(RemoteContext.FLOAT_DENSITY).cacheKey.isResultWholeNumber).isFalse()
        assertThat(RemoteFloat(RemoteContext.FLOAT_WINDOW_WIDTH).cacheKey.isResultWholeNumber)
            .isFalse()
        assertThat(RemoteStateIdKey(1000).isResultWholeNumber).isFalse()
    }

    @Test
    fun integerProducingFloatOperations() {
        assertThat(floor(dynamicFloat).cacheKey.isResultWholeNumber).isTrue()
        assertThat(ceil(dynamicFloat).cacheKey.isResultWholeNumber).isTrue()
        assertThat(round(dynamicFloat).cacheKey.isResultWholeNumber).isTrue()
        assertThat(sign(dynamicFloat).cacheKey.isResultWholeNumber).isTrue()

        val refTime = RemoteLong.createNamedRemoteLong("ref_time", 1_700_000_000_000L)
        assertThat(timeOfReferenceInSeconds(refTime).cacheKey.isResultWholeNumber).isTrue()
        assertThat(timeOfReferenceInMinutes(refTime).cacheKey.isResultWholeNumber).isTrue()
        assertThat(timeOfReferenceInHours(refTime).cacheKey.isResultWholeNumber).isTrue()
        assertThat(dayOfMonthForReference(refTime).cacheKey.isResultWholeNumber).isTrue()
        assertThat(monthOfYearForReference(refTime).cacheKey.isResultWholeNumber).isTrue()
        assertThat(dayOfWeekForReference(refTime).cacheKey.isResultWholeNumber).isTrue()
        assertThat(yearForReference(refTime).cacheKey.isResultWholeNumber).isTrue()

        assertThat(deltaFromReferenceInSeconds(refTime).cacheKey.isResultWholeNumber).isFalse()
        assertThat(deltaFromReferenceInMinutes(refTime).cacheKey.isResultWholeNumber).isFalse()
        assertThat(deltaFromReferenceInHours(refTime).cacheKey.isResultWholeNumber).isFalse()

        assertThat(dynamicFloat.isEqualTo(otherDynamicFloat).cacheKey.isResultWholeNumber).isTrue()
        assertThat(dynamicFloat.isNotEqualTo(otherDynamicFloat).cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(dynamicFloat.isLessThan(otherDynamicFloat).cacheKey.isResultWholeNumber).isTrue()
        assertThat(dynamicFloat.isLessThanOrEqualTo(otherDynamicFloat).cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(dynamicFloat.isGreaterThan(otherDynamicFloat).cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(
                dynamicFloat.isGreaterThanOrEqualTo(otherDynamicFloat).cacheKey.isResultWholeNumber
            )
            .isTrue()
    }

    @Test
    fun integerPreservingUnaryAndCopySignOperations() {
        assertThat(intFloatA.createReference(forceRemote = true).cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(5f.rf.createReference(forceRemote = true).cacheKey.isResultWholeNumber).isTrue()
        assertThat(dynamicFloat.createReference(forceRemote = true).cacheKey.isResultWholeNumber)
            .isFalse()
        assertThat(5.5f.rf.createReference(forceRemote = true).cacheKey.isResultWholeNumber)
            .isFalse()

        assertThat((-intFloatA).cacheKey.isResultWholeNumber).isTrue()
        assertThat((-dynamicFloat).cacheKey.isResultWholeNumber).isFalse()

        assertThat(abs(intFloatA).cacheKey.isResultWholeNumber).isTrue()
        assertThat(abs(dynamicFloat).cacheKey.isResultWholeNumber).isFalse()

        assertThat(copySign(intFloatA, dynamicFloat).cacheKey.isResultWholeNumber).isTrue()
        assertThat(copySign(5f, dynamicFloat).cacheKey.isResultWholeNumber).isTrue()
        assertThat(copySign(dynamicFloat, intFloatA).cacheKey.isResultWholeNumber).isFalse()
    }

    @Test
    fun integerPreservingBinaryOperations() {
        assertThat((intFloatA + intFloatB).cacheKey.isResultWholeNumber).isTrue()
        assertThat((intFloatA + 3f).cacheKey.isResultWholeNumber).isTrue()
        assertThat((intFloatA + dynamicFloat).cacheKey.isResultWholeNumber).isFalse()
        assertThat((intFloatA + 0.5f).cacheKey.isResultWholeNumber).isFalse()

        assertThat((intFloatA - intFloatB).cacheKey.isResultWholeNumber).isTrue()
        assertThat((intFloatA - dynamicFloat).cacheKey.isResultWholeNumber).isFalse()

        assertThat((intFloatA * intFloatB).cacheKey.isResultWholeNumber).isTrue()
        assertThat((intFloatA * dynamicFloat).cacheKey.isResultWholeNumber).isFalse()

        assertThat((intFloatA % intFloatB).cacheKey.isResultWholeNumber).isTrue()
        assertThat((intFloatA % 60f).cacheKey.isResultWholeNumber).isTrue()
        assertThat((intFloatA % dynamicFloat).cacheKey.isResultWholeNumber).isFalse()
        assertThat((intFloatA % 0.5f).cacheKey.isResultWholeNumber).isFalse()

        assertThat(min(intFloatA, intFloatB).cacheKey.isResultWholeNumber).isTrue()
        assertThat(min(intFloatA, dynamicFloat).cacheKey.isResultWholeNumber).isFalse()

        assertThat(max(intFloatA, intFloatB).cacheKey.isResultWholeNumber).isTrue()
        assertThat(max(dynamicFloat, intFloatB).cacheKey.isResultWholeNumber).isFalse()
    }

    @Test
    fun integerPreservingTernaryAndSelectOperations() {
        assertThat(
                clamp(value = intFloatA, min = intFloatB, max = intFloatC)
                    .cacheKey
                    .isResultWholeNumber
            )
            .isTrue()
        assertThat(
                clamp(value = dynamicFloat, min = intFloatB, max = intFloatC)
                    .cacheKey
                    .isResultWholeNumber
            )
            .isFalse()
        assertThat(
                clamp(value = intFloatA, min = dynamicFloat, max = intFloatC)
                    .cacheKey
                    .isResultWholeNumber
            )
            .isFalse()
        assertThat(
                clamp(value = intFloatA, min = intFloatB, max = dynamicFloat)
                    .cacheKey
                    .isResultWholeNumber
            )
            .isFalse()

        assertThat(mad(intFloatA, intFloatB, intFloatC).cacheKey.isResultWholeNumber).isTrue()
        assertThat(mad(dynamicFloat, intFloatB, intFloatC).cacheKey.isResultWholeNumber).isFalse()
        assertThat(mad(intFloatA, dynamicFloat, intFloatC).cacheKey.isResultWholeNumber).isFalse()
        assertThat(mad(intFloatA, intFloatB, dynamicFloat).cacheKey.isResultWholeNumber).isFalse()

        assertThat(lerp(intFloatA, intFloatB, intFloatC).cacheKey.isResultWholeNumber).isTrue()
        assertThat(lerp(intFloatA, intFloatB, 0.5f.rf).cacheKey.isResultWholeNumber).isFalse()
        assertThat(lerp(intFloatA, intFloatB, dynamicFloat).cacheKey.isResultWholeNumber).isFalse()

        assertThat(
                selectIfLt(dynamicFloat, otherDynamicFloat, intFloatA, intFloatB)
                    .cacheKey
                    .isResultWholeNumber
            )
            .isTrue()
        assertThat(
                selectIfLe(dynamicFloat, otherDynamicFloat, intFloatA, intFloatB)
                    .cacheKey
                    .isResultWholeNumber
            )
            .isTrue()
        assertThat(
                selectIfGt(dynamicFloat, otherDynamicFloat, intFloatA, intFloatB)
                    .cacheKey
                    .isResultWholeNumber
            )
            .isTrue()
        assertThat(
                selectIfGe(dynamicFloat, otherDynamicFloat, intFloatA, intFloatB)
                    .cacheKey
                    .isResultWholeNumber
            )
            .isTrue()
        assertThat(
                selectIfLt(dynamicFloat, otherDynamicFloat, intFloatA, dynamicFloat)
                    .cacheKey
                    .isResultWholeNumber
            )
            .isFalse()
        assertThat(
                selectIfGe(dynamicFloat, otherDynamicFloat, dynamicFloat, intFloatB)
                    .cacheKey
                    .isResultWholeNumber
            )
            .isFalse()

        assertThat(
                RemoteOperationCacheKey.create(
                        RemoteFloatOperationKey.ComparisonOp,
                        dynamicFloat,
                        otherDynamicFloat,
                        intFloatA,
                        intFloatB,
                    )
                    .isResultWholeNumber
            )
            .isTrue()
        assertThat(
                RemoteOperationCacheKey.create(
                        RemoteFloatOperationKey.ComparisonOp,
                        dynamicFloat,
                        otherDynamicFloat,
                        intFloatA,
                        dynamicFloat,
                    )
                    .isResultWholeNumber
            )
            .isFalse()

        assertThat(dynamicCond.select(intFloatA, intFloatB).cacheKey.isResultWholeNumber).isTrue()
        assertThat(dynamicCond.select(1f.rf, 2f.rf).cacheKey.isResultWholeNumber).isTrue()
        assertThat(dynamicCond.select(intFloatA, dynamicFloat).cacheKey.isResultWholeNumber)
            .isFalse()
        assertThat(dynamicCond.select(dynamicFloat, intFloatB).cacheKey.isResultWholeNumber)
            .isFalse()
    }

    @Test
    fun nonIntegerOperations() {
        assertThat((intFloatA / intFloatB).cacheKey.isResultWholeNumber).isFalse()
        assertThat(log(intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(ln(intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(rand().cacheKey.isResultWholeNumber).isFalse()
        assertThat(randRange(intFloatA, intFloatB).cacheKey.isResultWholeNumber).isFalse()
        assertThat(sin(intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(cos(intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(tan(intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(asin(intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(acos(intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(atan(intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(atan2(intFloatA, intFloatB).cacheKey.isResultWholeNumber).isFalse()
        assertThat(cbrt(intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(toDeg(intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(toRad(intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(sqrt(intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(exp(intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(pow(intFloatA, intFloatB).cacheKey.isResultWholeNumber).isFalse()
        assertThat(animateRemoteFloatAsState(intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(cubicEasing(0f.rf, 0f.rf, 1f.rf, 1f.rf, intFloatA).cacheKey.isResultWholeNumber)
            .isFalse()
        assertThat(
                evalSpline(RemoteFloatArray(0f, 1f, 2f), loop = false, progress = intFloatA)
                    .cacheKey
                    .isResultWholeNumber
            )
            .isFalse()
        assertThat(
                evalSpline(RemoteFloatArray(0f, 1f, 2f), loop = true, progress = intFloatA)
                    .cacheKey
                    .isResultWholeNumber
            )
            .isFalse()
        assertThat(lerp(dynamicFloat, intFloatB, intFloatA).cacheKey.isResultWholeNumber).isFalse()
        assertThat(lerp(intFloatA, dynamicFloat, intFloatB).cacheKey.isResultWholeNumber).isFalse()

        val namedInt = RemoteInt.createNamedRemoteInt("n", 1)
        assertThat((namedInt + 1.ri).toRemoteFloat().cacheKey.isResultWholeNumber).isTrue()
        assertThat((namedInt / 2.ri).toRemoteFloat().cacheKey.isResultWholeNumber).isTrue()
        assertThat(dynamicCond.select(namedInt, 2.ri).toRemoteFloat().cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(intFloatA.createReference(forceRemote = false).cacheKey.isResultWholeNumber)
            .isTrue()
        assertThat(dynamicFloat.createReference(forceRemote = false).cacheKey.isResultWholeNumber)
            .isFalse()
        assertThat(intFloatA.toRemoteString().cacheKey.isResultWholeNumber).isFalse()
        assertThat(namedInt.toRemoteString().cacheKey.isResultWholeNumber).isFalse()
        assertThat(dynamicCond.select("1".rs, "2".rs).cacheKey.isResultWholeNumber).isFalse()
    }

    @Test
    fun structurallyEqualKeysWithDifferentStateTypes_doNotCollideInMemo() {
        // Same (domain, name) shared by a named RemoteInt and a named RemoteFloat
        val namedInt = RemoteInt.createNamedRemoteInt("shared", 5)
        val namedFloat = RemoteFloat.createNamedRemoteFloat("shared", 1.5f)
        assertThat((namedInt.toRemoteFloat() + namedFloat).cacheKey.isResultWholeNumber).isFalse()

        // Same system context ID shared by an immutable RemoteFloat and a MutableRemoteFloat
        val immutableSec = RemoteFloat(RemoteContext.FLOAT_TIME_IN_SEC)
        val mutableSec = MutableRemoteFloat.createMutableForId(RemoteContext.FLOAT_TIME_IN_SEC)
        assertThat((immutableSec + mutableSec).cacheKey.isResultWholeNumber).isFalse()

        // Same raw ID shared by an integer ID state and an arbitrary float ID state
        val idInt = RemoteInt.createForId(0x100000000L + 100L)
        val idFloat = RemoteFloat(Utils.asNan(100))
        assertThat((idInt.toRemoteFloat() + idFloat).cacheKey.isResultWholeNumber).isFalse()
    }

    @Test
    fun sharedDiamondDag_isMemoizedAndLinear() {
        var integerDag: RemoteFloat = intFloatA
        var fractionalDag: RemoteFloat = dynamicFloat
        repeat(60) {
            integerDag = integerDag + integerDag
            fractionalDag = fractionalDag + fractionalDag
        }

        assertThat(integerDag.cacheKey.isResultWholeNumber).isTrue()
        assertThat(fractionalDag.cacheKey.isResultWholeNumber).isFalse()
    }

    @Test
    fun operationCacheKey_memoizesIsIntegerValuedAndInvalidatesOnSetState() {
        val opKey = RemoteOperationCacheKey.create(RemoteFloatOperationKey.Floor, dynamicFloat)
        assertThat(opKey.isResultWholeNumber).isTrue()
        assertThat(opKey.isResultWholeNumber).isTrue()

        opKey.setState(MutableRemoteFloat(1f))
        assertThat(opKey.isResultWholeNumber).isFalse()

        opKey.setState(null)
        assertThat(opKey.isResultWholeNumber).isTrue()
    }

    @Test
    fun computeExpressionFlags_synthesizesIntegerValuedFlagInConstantTime() {
        val intConst = RemoteConstantCacheKey(4f)
        val fracConst = RemoteConstantCacheKey(4.5f)
        val varIdKey = RemoteStateIdKey(42)

        assertThat(
                RemoteOperationCacheKey.computeExpressionFlags(
                    RemoteFloatOperationKey.Floor,
                    listOf(varIdKey),
                )
            )
            .isEqualTo(
                FLAG_DETERMINISTIC or
                    FLAG_RESULT_WHOLE_NUMBER or
                    FLAG_CAN_CONSTANT_FOLD or
                    FLAG_HAS_STATE_ID
            )

        assertThat(
                RemoteOperationCacheKey.computeExpressionFlags(
                    RemoteFloatOperationKey.Plus,
                    listOf(intConst, intConst),
                )
            )
            .isEqualTo(FLAG_DETERMINISTIC or FLAG_RESULT_WHOLE_NUMBER or FLAG_CAN_CONSTANT_FOLD)

        assertThat(
                RemoteOperationCacheKey.computeExpressionFlags(
                    RemoteFloatOperationKey.Plus,
                    listOf(intConst, fracConst),
                )
            )
            .isEqualTo(FLAG_DETERMINISTIC or FLAG_CAN_CONSTANT_FOLD)

        // Deep chain (2000 nodes) synthesizes FLAG_RESULT_WHOLE_NUMBER bottom-up without recursion.
        var deep: RemoteStateCacheKey = intFloatA.cacheKey
        repeat(2000) {
            deep = RemoteOperationCacheKey(RemoteFloatOperationKey.Plus, listOf(deep, intConst))
        }
        assertThat(deep.isResultWholeNumber).isTrue()
    }

    @Test
    fun imageBitmapDimensions_areWholeNumbersAndReconstructable() {
        val bmp1 = RemoteImageBitmap.createForId(107)
        val bmp2 = RemoteImageBitmap.createForId(108)

        assertThat(bmp1.width.cacheKey.isResultWholeNumber).isTrue()
        assertThat(bmp1.height.cacheKey.isResultWholeNumber).isTrue()
        assertThat(bmp1.width.cacheKey.toDebugString()).isEqualTo("context:#107.width")
        assertThat(bmp1.height.cacheKey.toDebugString()).isEqualTo("context:#107.height")

        val transformedWidth = bmp1.width.transform { if (it === bmp1) bmp2 else null }
        val transformedHeight = bmp1.height.transform { if (it === bmp1) bmp2 else null }
        assertThat(transformedWidth.cacheKey).isEqualTo(bmp2.width.cacheKey)
        assertThat(transformedHeight.cacheKey).isEqualTo(bmp2.height.cacheKey)
    }
}
