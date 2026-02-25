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

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class RemoteIntTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val creationState = RemoteComposeCreationState(AndroidxRcPlatformServices(), Size(1f, 1f))
    val time = RemoteInt.createNamedRemoteInt("time", 100).createReference()

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
    fun toRemoteString() {
        val sum = RemoteInt(100) + 20
        val sumString = sum.toRemoteString(3)
        val sumStringId = sumString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(sumStringId)).isEqualTo("120")
    }

    @Test
    fun selectIfLt_less() {
        val result = selectIfLt(RemoteInt(1), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfLt_equal() {
        val result = selectIfLt(RemoteInt(2), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfLt_greater() {
        val result = selectIfLt(RemoteInt(3), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfLe_less() {
        val result = selectIfLe(RemoteInt(1), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfLe_equal() {
        val result = selectIfLe(RemoteInt(2), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfLe_greater() {
        val result = selectIfLe(RemoteInt(2), RemoteInt(1), RemoteInt(100), RemoteInt(200))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGt_less() {
        val result = selectIfGt(RemoteInt(1), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGt_equal() {
        val result = selectIfGt(RemoteInt(2), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGt_greater() {
        val result = selectIfGt(RemoteInt(3), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfGe_less() {
        val result = selectIfGe(RemoteInt(1), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(200)
    }

    @Test
    fun selectIfGe_equal() {
        val result = selectIfGe(RemoteInt(2), RemoteInt(2), RemoteInt(100), RemoteInt(200))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(100)
    }

    @Test
    fun selectIfGe_greater() {
        val result = selectIfGe(RemoteInt(2), RemoteInt(1), RemoteInt(100), RemoteInt(200))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(100)
    }

    @Test
    fun shl() {
        val result = RemoteInt(2) shl RemoteInt(4)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(2 shl 4)
    }

    @Test
    fun shr() {
        val result = RemoteInt(1024) shr RemoteInt(3)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(1024 shr 3)
    }

    @Test
    fun or() {
        val result = RemoteInt(1024) or RemoteInt(512)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(1024 or 512)
    }

    @Test
    fun xor() {
        val result = RemoteInt(1234) xor RemoteInt(5432)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(1234 xor 5432)
    }

    @Test
    fun copySign() {
        val result = copySign(RemoteInt(12345), RemoteInt(-10))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(-12345)
    }

    @Test
    fun unaryMinus() {
        val result = RemoteInt(100) + -RemoteInt(10)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(90)
    }

    @Test
    fun inv() {
        val result = RemoteInt(3).inv()
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(3.inv())
    }

    @Test
    fun absoluteValue() {
        val result1 = RemoteInt(100).absoluteValue
        val result2 = RemoteInt(-100).absoluteValue
        val resultId1 = result1.getIdForCreationState(creationState)
        val resultId2 = result2.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId1)).isEqualTo(100)
        assertThat(context.getInteger(resultId2)).isEqualTo(100)
    }

    @Test
    fun min() {
        val result = min(RemoteInt(20), RemoteInt(10))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(10)
    }

    @Test
    fun max() {
        val result = max(RemoteInt(20), RemoteInt(10))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(20)
    }

    @Test
    fun clamp_lessThan() {
        val lowerBound = RemoteInt(10)
        val upperBound = RemoteInt(100)
        val value = RemoteInt(5)
        val result = clamp(lowerBound, upperBound, value)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(10)
    }

    @Test
    fun clamp_inRange() {
        val lowerBound = RemoteInt(10)
        val upperBound = RemoteInt(100)
        val value = RemoteInt(25)
        val result = clamp(lowerBound, upperBound, value)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(25)
    }

    @Test
    fun clamp_greaterThan() {
        val lowerBound = RemoteInt(10)
        val upperBound = RemoteInt(100)
        val value = RemoteInt(500)
        val result = clamp(lowerBound, upperBound, value)
        val resultId = result.getIdForCreationState(creationState)
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
                (RemoteInt(10) - RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC).toRemoteInt())
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
        assertThat(RemoteInt(10).toRemoteString(2).hasConstantValue).isTrue()
        assertThat(RemoteFloat(10f).toRemoteInt().hasConstantValue).isTrue()
    }

    @Test
    fun hasConstantValue_false() {
        assertThat(RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC).toRemoteInt().hasConstantValue)
            .isFalse()
        assertThat(
                RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC)
                    .toRemoteInt()
                    .toRemoteString(2)
                    .hasConstantValue
            )
            .isFalse()
    }

    @Test
    fun namedRemoteInt_initialValue() {
        val namedRemoteInt = RemoteInt.createNamedRemoteInt("testInt", 100)
        val result = namedRemoteInt * RemoteInt(10)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(1000)
    }

    @Test
    fun namedRemoteInt_overriddenValue() {
        val namedRemoteInt = RemoteInt.createNamedRemoteInt("testInt", 100)
        val result = namedRemoteInt * RemoteInt(10)
        val resultId = result.getIdForCreationState(creationState)

        makeAndUpdateCoreDocument { context.setNamedIntegerOverride("USER:testInt", 20) }

        assertThat(context.getInteger(resultId)).isEqualTo(200)
    }

    @Test
    fun namedRemoteInt_overriddenValue2() {
        val namedRemoteInt = RemoteInt.createNamedRemoteInt("testInt", 100)
        val plusOne = namedRemoteInt + RemoteInt(1)
        val result = plusOne * plusOne
        val resultId = result.getIdForCreationState(creationState)

        makeAndUpdateCoreDocument { context.setNamedIntegerOverride("USER:testInt", 19) }

        assertThat(context.getInteger(resultId)).isEqualTo(400)
    }

    @Test
    fun combineToLongArray() {
        val namedRemoteInt = RemoteInt.createNamedRemoteInt("testInt", 100)
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
    fun peepholeOptimization_plus() {
        val expr = (time + 10) + 1
        expr.getIdForCreationState(creationState)

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
        expr.getIdForCreationState(creationState)

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
        expr.getIdForCreationState(creationState)

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
        expr.getIdForCreationState(creationState)

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
        expr.getIdForCreationState(creationState)

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
        expr.getIdForCreationState(creationState)

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
        expr.getIdForCreationState(creationState)

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
        expr.getIdForCreationState(creationState)

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
    fun peepholeOptimization_zeroDiv() {
        val expr = RemoteInt(0) / time
        expr.getIdForCreationState(creationState)

        val ops = getOperationsStrings()
        assertThat(ops).containsExactly("IntegerExpression[43] = (0)").inOrder()
    }

    @Test
    fun peepholeOptimization_trimToIdentity_plusMinus() {
        val expr = (time + 10) - 10
        expr.getIdForCreationState(creationState)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly("VariableName[43] = \"USER:time\" type=4", "IntegerConstant[43] = 100")
            .inOrder()
        assertThat(expr.getIdForCreationState(creationState))
            .isEqualTo(time.getIdForCreationState(creationState))
    }

    @Test
    fun peepholeOptimization_trimToIdentity_minusPlus() {
        val expr = (time - 10) + 10
        expr.getIdForCreationState(creationState)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly("VariableName[43] = \"USER:time\" type=4", "IntegerConstant[43] = 100")
            .inOrder()
        assertThat(expr.getIdForCreationState(creationState))
            .isEqualTo(time.getIdForCreationState(creationState))
    }

    @Test
    fun peepholeOptimization_trimToIdentity_timesDiv() {
        val expr = (time * 2) / 2
        expr.getIdForCreationState(creationState)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly("VariableName[43] = \"USER:time\" type=4", "IntegerConstant[43] = 100")
            .inOrder()
        assertThat(expr.getIdForCreationState(creationState))
            .isEqualTo(time.getIdForCreationState(creationState))
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

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }

    private fun makeAndUpdateCoreDocument(runAfterInit: () -> Unit) =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            initializeContext(context)

            runAfterInit()

            for (op in operations) {
                if (op is VariableSupport) {
                    op.updateVariables(context)
                }
                op.apply(context)
            }
        }
}
