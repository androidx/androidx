/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

fun RemoteInt.computeValue(creationState: RemoteComposeCreationState): Int? {
    val array = arrayForCreationState(creationState)
    if (array.size != 1) {
        return null
    }
    val v = array[0]
    if (v >= 0x100000000) {
        return null
    }

    return v.toInt()
}

@RunWith(JUnit4::class)
class RemoteIntTest {
    val context = TestRemoteContext()
    val time = createNamedRemoteInt("time", 100).createReference()
    lateinit var creationState: RemoteComposeCreationState

    @Before
    fun setUp() {
        creationState = testCreationState()
    }

    @Test
    fun addition() {
        val sum = RemoteInt(100) + 20 + RemoteInt(3)

        assertThat(sum.computeValue(creationState)).isEqualTo(123)
    }

    @Test
    fun overflow() {
        val sum = RemoteInt(0x7f000000) + RemoteInt(0x7f000000)

        assertThat(sum.computeValue(creationState)).isEqualTo(-33554432)
    }

    @Test
    fun subtraction() {
        val sum = RemoteInt(100) - 20 - RemoteInt(3)

        assertThat(sum.computeValue(creationState)).isEqualTo(77)
    }

    @Test
    fun times() {
        val sum = RemoteInt(2) * 3 * RemoteInt(4)

        assertThat(sum.computeValue(creationState)).isEqualTo(24)
    }

    @Test
    fun divide() {
        val sum = RemoteInt(100) / 10 / RemoteInt(2)

        assertThat(sum.computeValue(creationState)).isEqualTo(5)
    }

    @Test
    fun remainder() {
        val sum = RemoteInt(101) % 2

        assertThat(sum.computeValue(creationState)).isEqualTo(1)
    }

    @Test
    fun remainder2() {
        val sum = RemoteInt(101) % RemoteInt(2)

        assertThat(sum.computeValue(creationState)).isEqualTo(1)
    }

    @Test
    fun isEqualTo() {
        val b0 = RemoteInt(10).isEqualTo(RemoteInt(10))
        val b1 = RemoteInt(10).isEqualTo(RemoteInt(20))
        val result0 = b0.select(RemoteInt(1), RemoteInt(0))
        val result1 = b1.select(RemoteInt(1), RemoteInt(0))
        val result0Id = creationState.getId(result0)
        val result1Id = creationState.getId(result1)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(result0Id)).isEqualTo(1)
        assertThat(context.getInteger(result1Id)).isEqualTo(0)
    }

    @Test
    fun isNotEqualTo() {
        val b0 = RemoteInt(10).isNotEqualTo(RemoteInt(10))
        val b1 = RemoteInt(10).isNotEqualTo(RemoteInt(20))
        val result0 = b0.select(RemoteInt(1), RemoteInt(0))
        val result1 = b1.select(RemoteInt(1), RemoteInt(0))
        val result0Id = creationState.getId(result0)
        val result1Id = creationState.getId(result1)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(result0Id)).isEqualTo(0)
        assertThat(context.getInteger(result1Id)).isEqualTo(1)
    }

    @Test
    fun isLessThan() {
        val b0 = RemoteInt(10).isLessThan(RemoteInt(20))
        val b1 = RemoteInt(10).isLessThan(RemoteInt(10))
        val b2 = RemoteInt(20).isLessThan(RemoteInt(10))
        val result0 = b0.select(RemoteInt(1), RemoteInt(0))
        val result1 = b1.select(RemoteInt(1), RemoteInt(0))
        val result2 = b2.select(RemoteInt(1), RemoteInt(0))
        val result0Id = creationState.getId(result0)
        val result1Id = creationState.getId(result1)
        val result2Id = creationState.getId(result2)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(result0Id)).isEqualTo(1)
        assertThat(context.getInteger(result1Id)).isEqualTo(0)
        assertThat(context.getInteger(result2Id)).isEqualTo(0)
    }

    @Test
    fun isLessThanOrEqualTo() {
        val b0 = RemoteInt(10).isLessThanOrEqualTo(RemoteInt(20))
        val b1 = RemoteInt(10).isLessThanOrEqualTo(RemoteInt(10))
        val b2 = RemoteInt(20).isLessThanOrEqualTo(RemoteInt(10))
        val result0 = b0.select(RemoteInt(1), RemoteInt(0))
        val result1 = b1.select(RemoteInt(1), RemoteInt(0))
        val result2 = b2.select(RemoteInt(1), RemoteInt(0))
        val result0Id = creationState.getId(result0)
        val result1Id = creationState.getId(result1)
        val result2Id = creationState.getId(result2)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(result0Id)).isEqualTo(1)
        assertThat(context.getInteger(result1Id)).isEqualTo(1)
        assertThat(context.getInteger(result2Id)).isEqualTo(0)
    }

    @Test
    fun isGreaterThan() {
        val b0 = RemoteInt(20).isGreaterThan(RemoteInt(10))
        val b1 = RemoteInt(10).isGreaterThan(RemoteInt(10))
        val b2 = RemoteInt(10).isGreaterThan(RemoteInt(20))
        val result0 = b0.select(RemoteInt(1), RemoteInt(0))
        val result1 = b1.select(RemoteInt(1), RemoteInt(0))
        val result2 = b2.select(RemoteInt(1), RemoteInt(0))
        val result0Id = creationState.getId(result0)
        val result1Id = creationState.getId(result1)
        val result2Id = creationState.getId(result2)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(result0Id)).isEqualTo(1)
        assertThat(context.getInteger(result1Id)).isEqualTo(0)
        assertThat(context.getInteger(result2Id)).isEqualTo(0)
    }

    @Test
    fun isGreaterThanOrEqualTo() {
        val b0 = RemoteInt(20).isGreaterThanOrEqualTo(RemoteInt(10))
        val b1 = RemoteInt(10).isGreaterThanOrEqualTo(RemoteInt(10))
        val b2 = RemoteInt(10).isGreaterThanOrEqualTo(RemoteInt(20))
        val result0 = b0.select(RemoteInt(1), RemoteInt(0))
        val result1 = b1.select(RemoteInt(1), RemoteInt(0))
        val result2 = b2.select(RemoteInt(1), RemoteInt(0))
        val result0Id = creationState.getId(result0)
        val result1Id = creationState.getId(result1)
        val result2Id = creationState.getId(result2)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(result0Id)).isEqualTo(1)
        assertThat(context.getInteger(result1Id)).isEqualTo(1)
        assertThat(context.getInteger(result2Id)).isEqualTo(0)
    }

    @Test
    fun toRemoteString() {
        val sum = RemoteInt(100) + 20
        val sumString = sum.toRemoteString()
        val sumStringId = creationState.getId(sumString)
        makeAndPaintCoreDocument()

        assertThat(context.getText(sumStringId)).isEqualTo("120")
    }

    @Test
    fun remoteFloat_toRemoteInt_toRemoteString() {
        val n3_99 = createNamedRemoteFloat("n3_99", 3.99f)
        val str = n3_99.toRemoteInt().toRemoteString()
        val strId = creationState.getId(str)

        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("3")
    }

    @Test
    fun selectIfLt_less() {
        val result = selectIfLt(RemoteInt(1), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfLt_equal() {
        val result = selectIfLt(RemoteInt(2), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfLt_greater() {
        val result = selectIfLt(RemoteInt(3), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfLe_less() {
        val result = selectIfLe(RemoteInt(1), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfLe_equal() {
        val result = selectIfLe(RemoteInt(2), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfLe_greater() {
        val result = selectIfLe(RemoteInt(2), RemoteInt(1), RemoteInt(100), RemoteInt(200))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGt_less() {
        val result = selectIfGt(RemoteInt(1), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGt_equal() {
        val result = selectIfGt(RemoteInt(2), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGt_greater() {
        val result = selectIfGt(RemoteInt(3), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfGe_less() {
        val result = selectIfGe(RemoteInt(1), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGe_equal() {
        val result = selectIfGe(RemoteInt(2), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfGe_greater() {
        val result = selectIfGe(RemoteInt(2), RemoteInt(1), RemoteInt(100), RemoteInt(200))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(100)
    }

    @Test
    fun shl() {
        val result = RemoteInt(2) shl RemoteInt(4)
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(2 shl 4)
    }

    @Test
    fun shr() {
        val result = RemoteInt(1024) shr RemoteInt(3)
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(1024 shr 3)
    }

    @Test
    fun or() {
        val result = RemoteInt(1024) or RemoteInt(512)
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(1024 or 512)
    }

    @Test
    fun xor() {
        val result = RemoteInt(1234) xor RemoteInt(5432)
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(1234 xor 5432)
    }

    @Test
    fun copySign() {
        val result = copySign(RemoteInt(12345), RemoteInt(-10))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(-12345)
    }

    @Test
    fun unaryMinus() {
        val result = RemoteInt(100) + -RemoteInt(10)
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(90)
    }

    @Test
    fun inv() {
        val result = RemoteInt(3).inv()
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(3.inv())
    }

    @Test
    fun absoluteValue() {
        val result1 = RemoteInt(100).absoluteValue
        val result2 = RemoteInt(-100).absoluteValue
        val resultId1 = creationState.getId(result1)
        val resultId2 = creationState.getId(result2)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId1)).isEqualTo(100)
        assertThat(context.getInteger(resultId2)).isEqualTo(100)
    }

    @Test
    fun min() {
        val result = min(RemoteInt(20), RemoteInt(10))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(10)
    }

    @Test
    fun max() {
        val result = max(RemoteInt(20), RemoteInt(10))
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(20)
    }

    @Test
    fun clamp_lessThan() {
        val lowerBound = RemoteInt(10)
        val upperBound = RemoteInt(100)
        val value = RemoteInt(5)
        val result = clamp(lowerBound, upperBound, value)
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(10)
    }

    @Test
    fun clamp_inRange() {
        val lowerBound = RemoteInt(10)
        val upperBound = RemoteInt(100)
        val value = RemoteInt(25)
        val result = clamp(lowerBound, upperBound, value)
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(25)
    }

    @Test
    fun clamp_greaterThan() {
        val lowerBound = RemoteInt(10)
        val upperBound = RemoteInt(100)
        val value = RemoteInt(500)
        val result = clamp(lowerBound, upperBound, value)
        val resultId = creationState.getId(result)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(100)
    }

    @Test
    fun constantValue_constant() {
        assertThat(((RemoteInt(10) - RemoteInt(5)) * RemoteInt(3)).constantValue).isEqualTo(15)
    }

    @Test
    fun constantValue_notConstant() {
        assertThat(
                (RemoteInt(10) - RemoteTimeVariables.continuousSeconds.toRemoteInt())
                    .constantValueOrNull
            )
            .isNull()
    }

    @Test
    fun hasConstantValue_true() {
        assertThat(RemoteInt(10).hasConstantValue).isTrue()
        assertThat(RemoteInt(10).plus(RemoteInt(2)).hasConstantValue).isTrue()
        assertThat(RemoteInt(10).times(RemoteInt(2)).hasConstantValue).isTrue()
        assertThat(RemoteInt(10).minus(RemoteInt(2)).hasConstantValue).isTrue()
        assertThat(RemoteInt(10).div(RemoteInt(2)).hasConstantValue).isTrue()
        assertThat(
                selectIfGt(RemoteInt(3), RemoteInt(2), RemoteInt(100), RemoteInt(200))
                    .hasConstantValue
            )
            .isTrue()
        assertThat(RemoteInt(10).toRemoteString().hasConstantValue).isTrue()
        assertThat(RemoteFloat(10f).toRemoteInt().hasConstantValue).isTrue()
    }

    @Test
    fun hasConstantValue_false() {
        assertThat(RemoteTimeVariables.continuousSeconds.toRemoteInt().hasConstantValue).isFalse()
        assertThat(
                RemoteTimeVariables.continuousSeconds
                    .toRemoteInt()
                    .toRemoteString()
                    .hasConstantValue
            )
            .isFalse()
    }

    @Test
    fun namedRemoteInt_initialValue() {
        val namedRemoteInt = createNamedRemoteInt("testInt", 100)
        val result = namedRemoteInt * RemoteInt(10)
        val resultId = creationState.getId(result)

        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(1000)
    }

    @Test
    fun namedRemoteInt_overriddenValue() {
        val namedRemoteInt = createNamedRemoteInt("testInt", 100)
        val result = namedRemoteInt * RemoteInt(10)
        val resultId = creationState.getId(result)

        makeAndUpdateCoreDocument { context.setNamedIntegerOverride("USER:testInt", 20) }

        assertThat(context.getInteger(resultId)).isEqualTo(200)
    }

    @Test
    fun namedRemoteInt_overriddenValue2() {
        val namedRemoteInt = createNamedRemoteInt("testInt", 100)
        val plusOne = namedRemoteInt + RemoteInt(1)
        val result = plusOne * plusOne
        val resultId = creationState.getId(result)

        makeAndUpdateCoreDocument { context.setNamedIntegerOverride("USER:testInt", 19) }

        assertThat(context.getInteger(resultId)).isEqualTo(400)
    }

    @Test
    fun combineToLongArray() {
        val namedRemoteInt = createNamedRemoteInt("testInt", 100)
        val e2 = namedRemoteInt * namedRemoteInt
        val e4 = e2 * e2
        val e8 = e4 * e4

        val result =
            combineToLongArray(
                creationState,
                arrayOf(e8, e8), // e8's array is long so it shouldn't be inlined.
                0x100000000L + IntegerExpressionEvaluator.I_ADD,
            )

        assertThat(result.size).isEqualTo(3)
    }

    @Test
    fun extensionFunctionMatches() {
        assertThat(10.ri.constantValue).isEqualTo(10)
        assertThat((-10).ri.constantValue).isEqualTo(-10)
    }

    @Test
    fun cacheKeys() {
        val constant = RemoteInt(10)
        assertThat(constant.cacheKey).isEqualTo(RemoteConstantCacheKey(10))

        val named = createNamedRemoteInt("test", 5)
        assertThat(named.cacheKey).isEqualTo(RemoteNamedCacheKey(RemoteValue.Domain.User, "test"))

        val op = constant + named
        // flipped because of peephole optimisation
        val key = op.cacheKey as RemoteOperationCacheKey
        assertThat(key.op.name).isEqualTo("Add")
        assertThat(key.args).isEqualTo(listOf(named.cacheKey, constant.cacheKey))
    }

    @Test
    fun peepholeOptimization_plus() {
        val expr = (time + 10) + 1
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=4",
                "IntegerConstant[43] = 100",
                "IntegerExpression[44] = ([43] 11 +)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_minus() {
        val expr = (time - 10) - 1
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=4",
                "IntegerConstant[43] = 100",
                "IntegerExpression[44] = ([43] 11 -)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_minus2() {
        val expr = (time + 10) - 1
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=4",
                "IntegerConstant[43] = 100",
                "IntegerExpression[44] = ([43] 9 +)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_times() {
        val expr = (time * 10) * 2
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=4",
                "IntegerConstant[43] = 100",
                "IntegerExpression[44] = ([43] 20 *)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_div() {
        val expr = (time / 10) / 2
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=4",
                "IntegerConstant[43] = 100",
                "IntegerExpression[44] = ([43] 20 /)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_div2() {
        val expr = (time * 10) / 2
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=4",
                "IntegerConstant[43] = 100",
                "IntegerExpression[44] = ([43] 5 *)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_complex() {
        val expr = (time + 10) - 5 + 2
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=4",
                "IntegerConstant[43] = 100",
                "IntegerExpression[44] = ([43] 7 +)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_negative() {
        val expr = (time * 10) + 2
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:time\" type=4",
                "IntegerConstant[43] = 100",
                "IntegerExpression[44] = ([43] 10 * 2 +)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_div() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = myBool.select(10.ri, 20.ri) / 2
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "IntegerExpression[44] = (10 5 [43] ifElse)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_rem() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = myBool.select(10.ri, 20.ri) % 3
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "IntegerExpression[44] = (2 1 [43] ifElse)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_times() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = myBool.select(10.ri, 20.ri) * 3
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "IntegerExpression[44] = (60 30 [43] ifElse)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_plus() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = myBool.select(10.ri, 20.ri) + 5
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "IntegerExpression[44] = (25 15 [43] ifElse)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_minus() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = myBool.select(10.ri, 20.ri) - 5
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "IntegerExpression[44] = (15 5 [43] ifElse)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_chained() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = (myBool.select(10.ri, 20.ri) + 10) * 2 / 4
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "IntegerExpression[44] = (15 10 [43] ifElse)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_selectIfLt_div() {
        val x = RemoteInt.createNamedRemoteInt("x", 5)
        val y = RemoteInt.createNamedRemoteInt("y", 10)
        val expr = selectIfLt(x, y, 100.ri, 200.ri) / 10
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:y\" type=4",
                "IntegerConstant[43] = 10",
                "VariableName[44] = \"USER:x\" type=4",
                "IntegerConstant[44] = 5",
                "IntegerExpression[45] = (20 10 [43] [44] - ifElse)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_selectIfGt_times() {
        val x = RemoteInt.createNamedRemoteInt("x", 5)
        val expr = selectIfGt(x, 10.ri, 10.ri, 20.ri) * 2
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=4",
                "IntegerConstant[43] = 5",
                "IntegerExpression[44] = (40 20 [43] 10 - ifElse)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_selectIfLe_plus() {
        val x = RemoteInt.createNamedRemoteInt("x", 5)
        val expr = selectIfLe(x, 10.ri, 10.ri, 20.ri) + 5
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=4",
                "IntegerConstant[43] = 5",
                "IntegerExpression[44] = (15 25 [43] 10 - ifElse)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_selectIfGe_minus() {
        val x = RemoteInt.createNamedRemoteInt("x", 5)
        val expr = selectIfGe(x, 10.ri, 10.ri, 20.ri) - 5
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=4",
                "IntegerConstant[43] = 5",
                "IntegerExpression[44] = (5 15 10 [43] - ifElse)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_booleanComparisonSelect_times() {
        val x = RemoteInt.createNamedRemoteInt("x", 5)
        val y = RemoteInt.createNamedRemoteInt("y", 10)
        val expr = (x.isLessThan(y)).select(10.ri, 20.ri) * 3
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:y\" type=4",
                "IntegerConstant[43] = 10",
                "VariableName[44] = \"USER:x\" type=4",
                "IntegerConstant[44] = 5",
                "IntegerExpression[45] = (60 30 [43] [44] - ifElse)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_selectWithConsumingCondition_doesNotFoldBranches() {
        val x = RemoteInt.createNamedRemoteInt("x", 5)
        val y = RemoteInt.createNamedRemoteInt("y", 10)

        // Represents: [10, 20, x, SUB, y, IFELSE]
        val rawSelect =
            RemoteInt(constantValueOrNull = null, cacheKey = RemoteStateInstanceKey()) {
                creationState ->
                longArrayOf(
                    10L,
                    20L,
                    0x100000000L + creationState.getId(x),
                    0x100000000L + IntegerExpressionEvaluator.I_SUB,
                    0x100000000L + creationState.getId(y),
                    0x100000000L + IntegerExpressionEvaluator.I_IFELSE,
                )
            }

        val expr = rawSelect * 2
        creationState.getId(expr)
        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=4",
                "IntegerConstant[43] = 5",
                "VariableName[44] = \"USER:y\" type=4",
                "IntegerConstant[44] = 10",
                "IntegerExpression[45] = (10 20 [43] - [44] ifElse 2 *)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_divByZeroOnMultiplication_doesNotCrashDuringPeepHole() {
        val x = RemoteInt.createNamedRemoteInt("x", 5)
        val y = RemoteInt.createNamedRemoteInt("y", 10)
        val mult = x * y
        val divExpr = mult / 0
        creationState.getId(divExpr)
        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=4",
                "IntegerConstant[43] = 5",
                "VariableName[44] = \"USER:y\" type=4",
                "IntegerConstant[44] = 10",
                "IntegerExpression[45] = ([43] [44] * 0 /)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_divByZero_doesNotCrashDuringPeepHole() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = myBool.select(10.ri, 20.ri) / 0
        creationState.getId(expr)
        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "IntegerExpression[44] = (20 10 [43] ifElse 0 /)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_select_remByZero_doesNotCrashDuringPeepHole() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val expr = myBool.select(10.ri, 20.ri) % 0
        creationState.getId(expr)
        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "IntegerExpression[44] = (20 10 [43] ifElse 0 %)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_divVariableMultiplication_doesNotPerformModuloOnVariableId() {
        val x = RemoteInt.createNamedRemoteInt("x", 5)
        val y = RemoteInt.createNamedRemoteInt("y", 10)
        val mult = x * y
        val divExpr = mult / 2
        creationState.getId(divExpr)
        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=4",
                "IntegerConstant[43] = 5",
                "VariableName[44] = \"USER:y\" type=4",
                "IntegerConstant[44] = 10",
                "IntegerExpression[45] = ([43] [44] * 2 /)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_divTimes_notAssociative_doesNotFold() {
        val x = RemoteInt.createNamedRemoteInt("x", 19)
        // (19 / 10) * 2 = 1 * 2 = 2.
        // If incorrectly folded to x / (10 / 2) = 19 / 5 = 3.
        val expr = (x / 10) * 2
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=4",
                "IntegerConstant[43] = 19",
                "IntegerExpression[44] = ([43] 10 / 2 *)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_foldTrailingConstant_overflow_doesNotLeakIntoIdSpace() {
        val x = RemoteInt.createNamedRemoteInt("x", 5)
        // 2_000_000_000L * 3L = 6_000_000_000L >= 0x100000000L.
        // Truncated to 32-bit int: 1_705_032_704.
        val expr = (x * 2_000_000_000) * 3
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=4",
                "IntegerConstant[43] = 5",
                "IntegerExpression[44] = ([43] 1705032704 *)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_foldSelectBranches_overflow_doesNotLeakIntoIdSpace() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        // 2_000_000_000L * 3L = 6_000_000_000L >= 0x100000000L -> 1_705_032_704
        // 1_000_000_000L * 3L = 3_000_000_000L -> -1_294_967_296
        val expr = myBool.select(2_000_000_000.ri, 1_000_000_000.ri) * 3
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "IntegerExpression[44] = (-1294967296 1705032704 [43] ifElse)",
            )
            .inOrder()
    }

    @Test
    fun peepholeOptimization_zeroDiv() {
        val expr = RemoteInt(0) / time
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops).containsExactly("IntegerExpression[43] = (0)").inOrder()
    }

    @Test
    fun peepholeOptimization_trimToIdentity_plusMinus() {
        val expr = (time + 10) - 10
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly("VariableName[43] = \"USER:time\" type=4", "IntegerConstant[43] = 100")
            .inOrder()
        assertThat(creationState.getId(expr)).isEqualTo(creationState.getId(time))
    }

    @Test
    fun peepholeOptimization_trimToIdentity_minusPlus() {
        val expr = (time - 10) + 10
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly("VariableName[43] = \"USER:time\" type=4", "IntegerConstant[43] = 100")
            .inOrder()
        assertThat(creationState.getId(expr)).isEqualTo(creationState.getId(time))
    }

    @Test
    fun peepholeOptimization_trimToIdentity_timesDiv() {
        val expr = (time * 2) / 2
        creationState.getId(expr)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly("VariableName[43] = \"USER:time\" type=4", "IntegerConstant[43] = 100")
            .inOrder()
        assertThat(creationState.getId(expr)).isEqualTo(creationState.getId(time))
    }

    @Test
    fun peepholeOptimization_additiveCancellation_constantMinusVar() {
        val yTop = createNamedRemoteInt("yTop", 12)

        val expr1 = yTop + (44.ri - yTop)
        assertThat(expr1.constantValueOrNull).isEqualTo(44)
        creationState.getId(expr1)
        assertThat(getOperationsStrings()).containsExactly("IntegerExpression[43] = (44)").inOrder()

        assertThat(((44.ri - yTop) + yTop).constantValueOrNull).isEqualTo(44)
        assertThat(((yTop + 8.ri) + (44.ri - yTop)).constantValueOrNull).isEqualTo(52)
        assertThat((yTop + ((44.ri - yTop) - 8.ri)).constantValueOrNull).isEqualTo(36)
    }

    @Test
    fun peepholeOptimization_additiveCancellation_varMinusVar() {
        val yTop = createNamedRemoteInt("yTop", 12)
        val yBottom = createNamedRemoteInt("yBottom", 44)
        val padding = createNamedRemoteInt("padding", 6)

        val cancelled = yTop + (yBottom - yTop)
        assertThat(cancelled).isEqualTo(yBottom)
        creationState.getId(cancelled)
        assertThat(getOperationsStrings())
            .containsExactly(
                "VariableName[43] = \"USER:yBottom\" type=4",
                "IntegerConstant[43] = 44",
            )
            .inOrder()

        assertThat((yBottom - yTop) + yTop).isEqualTo(yBottom)
        assertThat((yTop + 4.ri) + (yBottom - 4.ri)).isEqualTo(yTop + yBottom)
        assertThat((yTop + padding) + (yBottom - padding)).isEqualTo(yTop + yBottom)
        assertThat((yTop - yBottom) + (yBottom - padding)).isEqualTo(yTop - padding)
        assertThat((yTop + yBottom) - yTop).isEqualTo(yBottom)
        assertThat((yTop + yBottom) - yBottom).isEqualTo(yTop)
        assertThat(yTop - (yTop - yBottom)).isEqualTo(yBottom)
        assertThat(yTop - (yTop + yBottom)).isEqualTo(-yBottom)
        assertThat((yTop - padding) + (yBottom - yTop)).isEqualTo(yBottom - padding)
        assertThat((yTop - yTop).constantValueOrNull).isEqualTo(0)
        assertThat((yTop + (-yTop)).constantValueOrNull).isEqualTo(0)
        assertThat(((-yTop) + yTop).constantValueOrNull).isEqualTo(0)
        assertThat((yTop + yTop) - yTop).isEqualTo(yTop)

        val combined = (yTop - padding) + (yBottom - yTop)
        val combinedId = creationState.getId(combined)
        makeAndUpdateCoreDocument {
            context.setNamedIntegerOverride("USER:yTop", 5)
            context.setNamedIntegerOverride("USER:yBottom", 50)
            context.setNamedIntegerOverride("USER:padding", 8)
        }
        assertThat(context.getInteger(combinedId)).isEqualTo(42)
    }

    @Test
    fun peepholeOptimization_doubleNegation() {
        val x = createNamedRemoteInt("x", 10)
        assertThat(-(-x)).isEqualTo(x)
        assertThat(44.ri - (-x)).isEqualTo(x + 44)
    }

    @Test
    fun peepholeOptimization_additiveCancellation_overflowWraparound() {
        val x = createNamedRemoteInt("x", 5)

        assertThat((x + (Int.MIN_VALUE.ri - x)).constantValueOrNull).isEqualTo(Int.MIN_VALUE)
        assertThat((x - Int.MIN_VALUE) - Int.MIN_VALUE).isEqualTo(x)

        // 2_000_000_000 + 2_000_000_000 wraps in 32-bit int to -294_967_296 -> x - 294_967_296
        val wrapped = (x + 2_000_000_000) + 2_000_000_000
        val wrappedId = creationState.getId(wrapped)
        assertThat(getOperationsStrings())
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=4",
                "IntegerConstant[43] = 5",
                "IntegerExpression[44] = ([43] 294967296 -)",
            )
            .inOrder()
        makeAndPaintCoreDocument()
        assertThat(context.getInteger(wrappedId)).isEqualTo(5 + 2_000_000_000 + 2_000_000_000)
    }

    @Test
    fun peepholeOptimization_additiveCancellation_mutableRemoteInt() {
        val m1 = MutableRemoteInt(10)
        val m2 = MutableRemoteInt(10)

        assertThat((m1 + (44.ri - m1)).constantValueOrNull).isEqualTo(44)
        assertThat((m1 - m1).constantValueOrNull).isEqualTo(0)

        val diff = m1 - m2
        assertThat(diff.constantValueOrNull).isNull()
        val diffId = creationState.getId(diff)
        makeAndPaintCoreDocument()
        assertThat(context.getInteger(diffId)).isEqualTo(0)
    }

    @Test
    fun peepholeOptimization_additiveCancellation_nonDeterministicRandToInt_doesNotCancel() {
        val r1 = (rand() * 100f).toRemoteInt()
        val r2 = (rand() * 100f).toRemoteInt()

        assertThat((r1 - r2).constantValueOrNull).isNull()
        assertThat((r1 + (10.ri - r2)).constantValueOrNull).isNull()
        assertThat((r1 xor r2).constantValueOrNull).isNull()
        assertThat(min(r1, r2)).isNotEqualTo(r1)
    }

    @Test
    fun peepholeOptimization_select_unaryAndBinary() {
        val myBool = RemoteBoolean.createNamedRemoteBoolean("myBool", true)
        val sel = myBool.select(10.ri, 20.ri)

        val negSel = -sel
        creationState.getId(negSel)
        assertThat(getOperationsStrings())
            .containsExactly(
                "VariableName[43] = \"USER:myBool\" type=4",
                "IntegerConstant[43] = 1",
                "IntegerExpression[44] = (-20 -10 [43] ifElse)",
            )
            .inOrder()

        assertThat(myBool.select((-10).ri, 10.ri).absoluteValue.constantValueOrNull).isEqualTo(10)
        assertThat((sel + (44.ri - sel)).constantValueOrNull).isEqualTo(44)
        assertThat((sel + 5) - 5).isEqualTo(sel)
        assertThat(-(-sel)).isEqualTo(sel)

        val combinedSelect = myBool.select(4.ri, 8.ri) + myBool.select(2.ri, 6.ri)
        assertThat(combinedSelect).isInstanceOf(RemoteIntSelect::class.java)
        assertThat((combinedSelect as RemoteIntSelect).ifTrue.constantValueOrNull).isEqualTo(6)
        assertThat(combinedSelect.ifFalse.constantValueOrNull).isEqualTo(14)

        val constantSelect = myBool.select(4.ri, 8.ri) + myBool.select(6.ri, 2.ri)
        assertThat(constantSelect.constantValueOrNull).isEqualTo(10)

        val x = createNamedRemoteInt("x", 1)
        val y = createNamedRemoteInt("y", 3)
        val collapsedWithExtraVar =
            (x + myBool.select(4.ri, 8.ri)) + (myBool.select(6.ri, 2.ri) - x) + y
        assertThat(collapsedWithExtraVar).isEqualTo(y + 10)
    }

    @Test
    fun peepholeOptimization_idempotentAndSelfCancellingBitwiseAndMinMax() {
        val x = createNamedRemoteInt("x", 10)
        assertThat(min(x, x)).isEqualTo(x)
        assertThat(max(x, x)).isEqualTo(x)
        assertThat(x and x).isEqualTo(x)
        assertThat(x or x).isEqualTo(x)
        assertThat((x xor x).constantValueOrNull).isEqualTo(0)
    }

    @Test
    fun peepholeOptimization_zeroDivByZero_doesNotThrow() {
        assertThat((0.ri / 0).constantValueOrNull).isEqualTo(0)
        assertThat((0.ri / 0.ri).constantValueOrNull).isEqualTo(0)

        val divZero = 10.ri / 0
        val modZero = 10.ri % 0
        val divZeroId = creationState.getId(divZero)
        val modZeroId = creationState.getId(modZero)
        makeAndPaintCoreDocument()
        assertThat(context.getInteger(divZeroId)).isEqualTo(0)
        assertThat(context.getInteger(modZeroId)).isEqualTo(0)
    }

    @Test
    fun peepholeOptimization_divDiv_overflowAndNegOne_doesNotMisFold() {
        val x99 = createNamedRemoteInt("x99", 99)
        // 3 * -1_431_655_765 == 1 in wrapping 32-bit Int; must NOT fold to Identity (x99).
        val modInvExpr = (x99 / 3) / -1_431_655_765
        val modInvId = creationState.getId(modInvExpr)

        // 65536 * 65536 == 0 in wrapping 32-bit Int; must NOT rewrite to x99 / 0.
        val wrapZeroExpr = (x99 / 65536) / 65536
        assertThat(wrapZeroExpr.toString()).isEqualTo("user:x99 / 65536 / 65536")
        val wrapZeroId = creationState.getId(wrapZeroExpr)
        val substituted = wrapZeroExpr.transform { state -> if (state == x99) 100.ri else null }
        assertThat(substituted.constantValueOrNull).isEqualTo(0)

        val xBig = createNamedRemoteInt("xBig", 2_000_000_000)
        val overflowProdExpr = (xBig / 2_000_000_000) / 3
        val overflowProdId = creationState.getId(overflowProdExpr)

        val xMin = createNamedRemoteInt("xMin", Int.MIN_VALUE)
        val wrapMinExpr = (xMin / 65536) / 32768
        val wrapMinId = creationState.getId(wrapMinExpr)

        val negOneDivTwoExpr = (xMin / -1) / 2
        val negOneDivTwoId = creationState.getId(negOneDivTwoExpr)

        assertThat((xMin / -1) / -1).isEqualTo(xMin)

        makeAndPaintCoreDocument()
        assertThat(context.getInteger(modInvId)).isEqualTo(0)
        assertThat(context.getInteger(wrapZeroId)).isEqualTo(0)
        assertThat(context.getInteger(overflowProdId)).isEqualTo(0)
        assertThat(context.getInteger(wrapMinId)).isEqualTo(-1)
        assertThat(context.getInteger(negOneDivTwoId)).isEqualTo(-1_073_741_824)
    }

    @Test
    fun mutableRemoteInt_applyExpression() {
        val timeId = creationState.getId(time)
        val mutableTime = MutableRemoteInt.createMutableForId(timeId.toLong())

        val expr = (mutableTime + 2) / 2
        val exprId = creationState.getId(expr)

        makeAndPaintCoreDocument()

        assertThat(context.getInteger(exprId)).isEqualTo(51)
    }

    @Test
    fun sharedExpressionReferenced() {
        val a = (time + 123) * 456
        val b = a + 789

        // Write 'a' to document first to ensure it's in the document and hasBeenWrittenToDoc
        // returns true
        creationState.getId(a)
        assertThat(a.hasBeenWrittenToDoc(creationState)).isTrue()

        // Now 'b' should reference 'a' instead of inlining it
        val bArray = b.arrayForCreationState(creationState)

        // Expected bArray: [ID_A, 789, ADD] -> size 3
        // If inlined: [TIME, 123, ADD, 456, MUL, 789, ADD] -> size 7
        assertThat(bArray.size).isEqualTo(3)
        assertThat(bArray[0]).isEqualTo(creationState.getLongId(a))
    }

    @Test
    fun unsharedExpressionInlined() {
        val a = (time + 123) * 456
        val b = a + 789

        // 'b' should inline 'a' instead
        val bArray = b.arrayForCreationState(creationState)

        // Expected bArray: [TIME, 123, ADD, 456, MUL, 789, ADD] -> size 7
        assertThat(bArray.size).isEqualTo(7)
    }

    @Test
    fun mutableRemoteInt_smokeTest() {
        val mutableInt = MutableRemoteInt(10)
        val resultId = creationState.getId(mutableInt)
        assertThat(resultId).isGreaterThan(0)
    }

    private fun getOperationsStrings(): List<String> =
        CoreDocument().run {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            getOperations()
                .onEach {
                    if (it is VariableSupport) {
                        it.updateVariables(context)
                    }
                }
                .map { it.toString() }
                .filter {
                    !it.contains("HEADER") &&
                        !it.contains("TextData") &&
                        !it.contains("RootContentDescription")
                }
        }

    private fun makeAndPaintCoreDocument() = context.loadDocument(creationState)

    private fun makeAndUpdateCoreDocument(runAfterInit: () -> Unit) =
        context.loadDocument(creationState, runAfterInit)

    @Test
    fun toString_arithmetic() {
        val x = createNamedRemoteInt("x", 0)
        val expr = -x * 3 + 2
        assertThat(expr.toString()).isEqualTo("-user:x * 3 + 2")
    }

    @Test
    fun toString_conversions() {
        val x = createNamedRemoteInt("x", 0)
        assertThat(x.toRemoteString().toString())
            .isEqualTo("user:x.toRemoteFloat().toRemoteString()")
        assertThat(x.toRemoteFloat().toString()).isEqualTo("user:x.toRemoteFloat()")
    }

    @Test
    fun toString_nestedConditional() {
        val x = createNamedRemoteInt("x", 0)
        val y = createNamedRemoteInt("y", 10)
        val nestedSelect =
            x.isEqualTo(0.ri).select(100.ri, y.isEqualTo(20.ri).select(200.ri, 300.ri))
        assertThat(nestedSelect.toString())
            .isEqualTo("user:x == 0 ? 100 : (user:y == 20 ? 200 : 300)")
    }

    @Test
    fun toString_bitwiseAnd() {
        val i = createNamedRemoteInt("i", 10)
        val expr = i and RemoteInt(0)
        assertThat(expr.toString()).isEqualTo("user:i and 0")
    }

    @Test
    fun toString_contextVariable() {
        val continuousSecFloat = RemoteTimeVariables.continuousSeconds
        val intExpr = continuousSecFloat.toRemoteInt()
        assertThat(intExpr.toString()).isEqualTo("context:continuous_sec.toRemoteInt()")

        assertThat(RemoteTimeVariables.secondOfHour.toString()).isEqualTo("context:time_in_sec")
        assertThat(RemoteTimeVariables.minuteOfDay.toString()).isEqualTo("context:time_in_min")
        assertThat(RemoteTimeVariables.hourOfDay.toString()).isEqualTo("context:time_in_hr")
        assertThat(RemoteTimeVariables.month.toString()).isEqualTo("context:calendar_month")
        assertThat(RemoteTimeVariables.dayOfMonth.toString()).isEqualTo("context:day_of_month")
        assertThat(RemoteTimeVariables.dayOfWeek.toString()).isEqualTo("context:week_day")
        assertThat(RemoteTimeVariables.dayOfYear.toString()).isEqualTo("context:day_of_year")
        assertThat(RemoteTimeVariables.year.toString()).isEqualTo("context:year")
        assertThat(RemoteTimeVariables.utcOffsetSeconds.toString())
            .isEqualTo("context:offset_to_utc")
        assertThat(RemoteTimeVariables.epochSecond.toString()).isEqualTo("context:epoch_second")
    }

    @Test
    fun companionContextVariables() {
        val secondOfHourId = creationState.getId((RemoteTimeVariables.secondOfHour + 1))
        val minuteOfDayId = creationState.getId((RemoteTimeVariables.minuteOfDay + 1))
        val hourOfDayId = creationState.getId((RemoteTimeVariables.hourOfDay + 1))
        val monthId = creationState.getId((RemoteTimeVariables.month + 1))
        val dayOfMonthId = creationState.getId((RemoteTimeVariables.dayOfMonth + 1))
        val dayOfWeekId = creationState.getId((RemoteTimeVariables.dayOfWeek + 1))
        val dayOfYearId = creationState.getId((RemoteTimeVariables.dayOfYear + 1))
        val yearId = creationState.getId((RemoteTimeVariables.year + 1))
        val utcOffsetSecondsId = creationState.getId((RemoteTimeVariables.utcOffsetSeconds + 1))
        val epochSecondId = creationState.getId((RemoteTimeVariables.epochSecond + 1))

        makeAndUpdateCoreDocument {
            // Mirror TimeVariables.updateTime: calendar fields are loaded as floats (and mirrored
            // into the integer map by the player), only the epoch second is loaded as an integer.
            context.loadFloat(RemoteContext.ID_TIME_IN_SEC, 10f)
            context.loadFloat(RemoteContext.ID_TIME_IN_MIN, 20f)
            context.loadFloat(RemoteContext.ID_TIME_IN_HR, 30f)
            context.loadFloat(RemoteContext.ID_CALENDAR_MONTH, 4f)
            context.loadFloat(RemoteContext.ID_DAY_OF_MONTH, 15f)
            context.loadFloat(RemoteContext.ID_WEEK_DAY, 2f)
            context.loadFloat(RemoteContext.ID_DAY_OF_YEAR, 105f)
            context.loadFloat(RemoteContext.ID_YEAR, 2026f)
            context.loadFloat(RemoteContext.ID_OFFSET_TO_UTC, 3600f)
            context.loadInteger(RemoteContext.ID_EPOCH_SECOND, 1700000000)
        }

        assertThat(context.getInteger(secondOfHourId)).isEqualTo(11)
        assertThat(context.getInteger(minuteOfDayId)).isEqualTo(21)
        assertThat(context.getInteger(hourOfDayId)).isEqualTo(31)
        assertThat(context.getInteger(monthId)).isEqualTo(5)
        assertThat(context.getInteger(dayOfMonthId)).isEqualTo(16)
        assertThat(context.getInteger(dayOfWeekId)).isEqualTo(3)
        assertThat(context.getInteger(dayOfYearId)).isEqualTo(106)
        assertThat(context.getInteger(yearId)).isEqualTo(2027)
        assertThat(context.getInteger(utcOffsetSecondsId)).isEqualTo(3601)
        assertThat(context.getInteger(epochSecondId)).isEqualTo(1700000001)
    }

    @Test
    fun toString_constant() {
        val ri = RemoteInt(42)
        assertThat(ri.toString()).isEqualTo("42")
    }
}
