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

package androidx.compose.remote.creation.compose.intervals

import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteConstantCacheKey
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatOperationKey
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteOperation
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateIdKey
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteValueKind
import androidx.compose.remote.creation.compose.state.StringOperationKey
import androidx.compose.remote.creation.compose.state.cacheKey
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.selectIfGe
import androidx.compose.remote.creation.compose.state.selectIfLt
import androidx.compose.remote.document.intervals.IntervalComparison
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteOperationMetadataTest {

    private val x = RemoteFloat.createNamedRemoteFloat("x", 1f)
    private val n = RemoteInt.createNamedRemoteInt("n", 1)
    private val s = RemoteString.createNamedRemoteString("s", "abc")
    private val b = RemoteBoolean.createNamedRemoteBoolean("b", true)

    @Test
    fun boundedFloatOperandComparisonAndSelectMetadata() {
        val mulOp = ((x * 2f.rf).cacheKey as RemoteOperationCacheKey).op
        assertThat(mulOp).isNotInstanceOf(RemoteIntervalOperation::class.java)
        val floatSem = intervalSemanticsFor(mulOp)!!
        for (boundedOp in
            listOf(
                RemoteFloatOperationKey.Times,
                RemoteFloatOperationKey.Div,
                RemoteFloatOperationKey.Rem,
                RemoteFloatOperationKey.Mad,
                RemoteFloatOperationKey.Clamp,
                RemoteFloatOperationKey.Lerp,
                RemoteFloatOperationKey.Min,
                RemoteFloatOperationKey.Max,
                RemoteFloatOperationKey.Abs,
                RemoteFloatOperationKey.Round,
                RemoteFloatOperationKey.Sin,
                RemoteFloatOperationKey.Cos,
                RemoteFloatOperationKey.Atan,
                RemoteFloatOperationKey.Atan2,
                RemoteFloatOperationKey.Sign,
                RemoteFloatOperationKey.CopySign,
                RemoteFloatOperationKey.Exp,
                RemoteFloatOperationKey.Cubic,
                RemoteFloatOperationKey.RandInRange,
            )) {
            assertThat(floatSem.requiresBoundedFloatOperands(boundedOp)).isTrue()
        }
        val addOp = ((x + 2f.rf).cacheKey as RemoteOperationCacheKey).op
        assertThat(floatSem.requiresBoundedFloatOperands(addOp)).isFalse()

        val floatCmpLtOp = (x.isLessThan(5f.rf).cacheKey as RemoteOperationCacheKey).op
        assertThat(floatSem.comparisonCondition(floatCmpLtOp)).isEqualTo(IntervalComparison.LT)
        assertThat((floatCmpLtOp as RemoteOperation).resultKind).isEqualTo(RemoteValueKind.BOOLEAN)
        assertThat(floatSem.isBooleanSelect(floatCmpLtOp)).isFalse()
        assertThat(floatSem.selectComparisonKind(addOp, emptyList()))
            .isEqualTo(RemoteValueKind.OTHER)

        val intCmpGeOp = (n.isGreaterThanOrEqualTo(0.ri).cacheKey as RemoteOperationCacheKey).op
        val intSem = intervalSemanticsFor(intCmpGeOp)!!
        assertThat(intSem.comparisonCondition(intCmpGeOp)).isEqualTo(IntervalComparison.GTE)
        assertThat((intCmpGeOp as RemoteOperation).resultKind).isEqualTo(RemoteValueKind.BOOLEAN)
        assertThat(intSem.isBooleanSelect(intCmpGeOp)).isFalse()

        val selectIfIntKey = selectIfLt(n, 0.ri, 1.ri, 2.ri).cacheKey as RemoteOperationCacheKey
        assertThat(intSem.selectCondition(selectIfIntKey.op)).isEqualTo(IntervalComparison.LT)
        assertThat(intSem.selectComparisonKind(selectIfIntKey.op, selectIfIntKey.args))
            .isEqualTo(RemoteValueKind.INT)
        assertThat(intSem.selectComparisonKind(intCmpGeOp, emptyList()))
            .isEqualTo(RemoteValueKind.OTHER)

        val isEmptyOp = (s.isEmpty.cacheKey as RemoteOperationCacheKey).op
        val strSem = intervalSemanticsFor(isEmptyOp)!!
        assertThat((isEmptyOp as RemoteOperation).resultKind).isEqualTo(RemoteValueKind.BOOLEAN)
        assertThat(strSem.comparisonCondition(isEmptyOp)).isNull()
        assertThat(strSem.isBooleanSelect(isEmptyOp)).isFalse()
        assertThat(strSem.selectComparisonKind(isEmptyOp, emptyList()))
            .isEqualTo(RemoteValueKind.OTHER)

        val selectIfFloatKey =
            selectIfGe(x, 0f.rf, 1f.rf, 2f.rf).cacheKey as RemoteOperationCacheKey
        val selectIfFloatSem = intervalSemanticsFor(selectIfFloatKey.op)!!
        assertThat(selectIfFloatSem.selectCondition(selectIfFloatKey.op))
            .isEqualTo(IntervalComparison.GTE)
        assertThat(
                selectIfFloatSem.selectComparisonKind(selectIfFloatKey.op, selectIfFloatKey.args)
            )
            .isEqualTo(RemoteValueKind.FLOAT)
        assertThat(selectIfFloatSem.isBooleanSelect(selectIfFloatKey.op)).isFalse()

        val selectIfStrIntKey =
            selectIfLt(n, 0.ri, "a".rs, "b".rs).cacheKey as RemoteOperationCacheKey
        val selectIfStrIntSem = intervalSemanticsFor(selectIfStrIntKey.op)!!
        assertThat(selectIfStrIntSem.selectCondition(selectIfStrIntKey.op))
            .isEqualTo(IntervalComparison.LT)
        assertThat(
                selectIfStrIntSem.selectComparisonKind(selectIfStrIntKey.op, selectIfStrIntKey.args)
            )
            .isEqualTo(RemoteValueKind.INT)
        assertThat(selectIfStrIntSem.isBooleanSelect(selectIfStrIntKey.op)).isFalse()

        // Detached state IDs infer domain from the sibling operand, or OTHER when both are unknown.
        assertThat(
                strSem.selectComparisonKind(
                    StringOperationKey.SelectIfLT,
                    listOf(
                        RemoteStateIdKey(42),
                        RemoteConstantCacheKey(0),
                        RemoteConstantCacheKey("a"),
                        RemoteConstantCacheKey("b"),
                    ),
                )
            )
            .isEqualTo(RemoteValueKind.INT)
        assertThat(
                strSem.selectComparisonKind(
                    StringOperationKey.SelectIfLT,
                    listOf(
                        RemoteStateIdKey(42),
                        RemoteConstantCacheKey(0f),
                        RemoteConstantCacheKey("a"),
                        RemoteConstantCacheKey("b"),
                    ),
                )
            )
            .isEqualTo(RemoteValueKind.FLOAT)
        assertThat(
                strSem.selectComparisonKind(
                    StringOperationKey.SelectIfLT,
                    listOf(
                        RemoteStateIdKey(42),
                        RemoteStateIdKey(43),
                        RemoteConstantCacheKey("a"),
                        RemoteConstantCacheKey("b"),
                    ),
                )
            )
            .isEqualTo(RemoteValueKind.OTHER)

        val selectIfStrFloatKey =
            selectIfLt(x, 0f.rf, "a".rs, "b".rs).cacheKey as RemoteOperationCacheKey
        val selectIfStrFloatSem = intervalSemanticsFor(selectIfStrFloatKey.op)!!
        assertThat(
                selectIfStrFloatSem.selectComparisonKind(
                    selectIfStrFloatKey.op,
                    selectIfStrFloatKey.args,
                )
            )
            .isEqualTo(RemoteValueKind.FLOAT)

        val boolSelectFloatOp = (b.select(1f.rf, 2f.rf).cacheKey as RemoteOperationCacheKey).op
        val boolSem = intervalSemanticsFor(boolSelectFloatOp)!!
        assertThat(boolSem.isBooleanSelect(boolSelectFloatOp)).isTrue()
        assertThat(boolSem.selectCondition(boolSelectFloatOp)).isNull()

        val b2 = RemoteBoolean.createNamedRemoteBoolean("b2", false)
        val boolSelectBoolOp = (b.select(b2, false.rb).cacheKey as RemoteOperationCacheKey).op
        assertThat(boolSem.isBooleanSelect(boolSelectBoolOp)).isTrue()
        assertThat((boolSelectBoolOp as RemoteOperation).resultKind)
            .isEqualTo(RemoteValueKind.BOOLEAN)
    }

    @Test
    fun customRemoteIntervalOperation_dispatchesThroughIntervalSemanticsAdapter() {
        val customOp =
            object : RemoteIntervalOperation {
                override val requiresBoundedFloatOperands: Boolean = true
                override val comparisonCondition: IntervalComparison = IntervalComparison.EQ
                override val resultKind: RemoteValueKind = RemoteValueKind.BOOLEAN

                override fun toDebugString(args: List<RemoteStateCacheKey>): String = "custom"
            }
        val sem = intervalSemanticsFor(customOp)!!
        assertThat(sem.requiresBoundedFloatOperands(customOp)).isTrue()
        assertThat(sem.comparisonCondition(customOp)).isEqualTo(IntervalComparison.EQ)
        assertThat(sem.selectComparisonKind(customOp, emptyList())).isEqualTo(RemoteValueKind.OTHER)
        assertThat(sem.isBooleanSelect(customOp)).isFalse()
    }
}
