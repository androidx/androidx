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
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@SdkSuppress(minSdkVersion = 29)
@RunWith(TestParameterInjector::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteFloatOperationTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val creationState = RemoteComposeCreationState(AndroidxRcPlatformServices(), Size(1f, 1f))

    enum class Operation(
        val opName: String,
        val fn: (RemoteFloat, RemoteFloat) -> RemoteFloat,
        val a: Float,
        val b: Float,
        val expected: Float,
        val opCode: Enum<*>,
        val isUnary: Boolean = false,
        val customArgs: ((RemoteFloat, RemoteFloat) -> Array<Any>)? = null,
    ) {
        ADD("+", { a, b -> a + b }, 10f, 5f, 15f, RemoteFloat.OperationKey.Plus),
        SUB("-", { a, b -> a - b }, 10f, 5f, 5f, RemoteFloat.OperationKey.Minus),
        MUL("*", { a, b -> a * b }, 10f, 5f, 50f, RemoteFloat.OperationKey.Times),
        DIV("/", { a, b -> a / b }, 10f, 5f, 2f, RemoteFloat.OperationKey.Div),
        REM("%", { a, b -> a % b }, 10f, 3f, 1f, RemoteFloat.OperationKey.Rem),
        MAX("max", { a, b -> max(a, b) }, 10f, 20f, 20f, RemoteFloat.OperationKey.Max),
        MIN("min", { a, b -> min(a, b) }, 10f, 20f, 10f, RemoteFloat.OperationKey.Min),
        POW("pow", { a, b -> pow(a, b) }, 2f, 3f, 8f, RemoteFloat.OperationKey.Pow),
        SQRT(
            "sqrt",
            { a, _ -> sqrt(a) },
            9f,
            0f,
            3f,
            RemoteFloat.OperationKey.Sqrt,
            isUnary = true,
        ),
        ABS("abs", { a, _ -> abs(a) }, -10f, 0f, 10f, RemoteFloat.OperationKey.Abs, isUnary = true),
        SIGN(
            "sign",
            { a, _ -> sign(a) },
            -10f,
            0f,
            -1f,
            RemoteFloat.OperationKey.Sign,
            isUnary = true,
        ),
        COPYSIGN(
            "copySign",
            { a, b -> copySign(a, b) },
            10f,
            -1f,
            -10f,
            RemoteFloat.OperationKey.CopySign,
        ),
        EXP(
            "exp",
            { a, _ -> exp(a) },
            1f,
            0f,
            Math.exp(1.0).toFloat(),
            RemoteFloat.OperationKey.Exp,
            isUnary = true,
        ),
        CEIL(
            "ceil",
            { a, _ -> ceil(a) },
            10.1f,
            0f,
            11f,
            RemoteFloat.OperationKey.Ceil,
            isUnary = true,
        ),
        FLOOR(
            "floor",
            { a, _ -> floor(a) },
            10.9f,
            0f,
            10f,
            RemoteFloat.OperationKey.Floor,
            isUnary = true,
        ),
        LOG("log", { a, _ -> log(a) }, 100f, 0f, 2f, RemoteFloat.OperationKey.Log, isUnary = true),
        LN(
            "ln",
            { a, _ -> ln(a) },
            Math.E.toFloat(),
            0f,
            0.99999994f,
            RemoteFloat.OperationKey.Ln,
            isUnary = true,
        ),
        ROUND(
            "round",
            { a, _ -> round(a) },
            10.5f,
            0f,
            11f,
            RemoteFloat.OperationKey.Round,
            isUnary = true,
        ),
        SIN("sin", { a, _ -> sin(a) }, 0f, 0f, 0f, RemoteFloat.OperationKey.Sin, isUnary = true),
        COS("cos", { a, _ -> cos(a) }, 0f, 0f, 1f, RemoteFloat.OperationKey.Cos, isUnary = true),
        TAN("tan", { a, _ -> tan(a) }, 0f, 0f, 0f, RemoteFloat.OperationKey.Tan, isUnary = true),
        ASIN(
            "asin",
            { a, _ -> asin(a) },
            0f,
            0f,
            0f,
            RemoteFloat.OperationKey.Asin,
            isUnary = true,
        ),
        ACOS(
            "acos",
            { a, _ -> acos(a) },
            1f,
            0f,
            0f,
            RemoteFloat.OperationKey.Acos,
            isUnary = true,
        ),
        ATAN(
            "atan",
            { a, _ -> atan(a) },
            0f,
            0f,
            0f,
            RemoteFloat.OperationKey.Atan,
            isUnary = true,
        ),
        ATAN2("atan2", { a, b -> atan2(a, b) }, 0f, 1f, 0f, RemoteFloat.OperationKey.Atan2),
        CBRT(
            "cbrt",
            { a, _ -> cbrt(a) },
            27f,
            0f,
            3f,
            RemoteFloat.OperationKey.Cbrt,
            isUnary = true,
        ),
        DEG(
            "toDeg",
            { a, _ -> toDeg(a) },
            Math.PI.toFloat(),
            0f,
            180f,
            RemoteFloat.OperationKey.ToDeg,
            isUnary = true,
        ),
        RAD(
            "toRad",
            { a, _ -> toRad(a) },
            180f,
            0f,
            Math.PI.toFloat(),
            RemoteFloat.OperationKey.ToRad,
            isUnary = true,
        ),
        LERP(
            "lerp",
            { a, b -> lerp(a, b, 0.5f.rf) },
            100f,
            200f,
            150f,
            RemoteFloat.OperationKey.Lerp,
            customArgs = { a, b -> arrayOf(a, b, 0.5f.rf) },
        ),
        MAD(
            "mad",
            { a, b -> mad(a, b, 5f.rf) },
            10f,
            2f,
            25f,
            RemoteFloat.OperationKey.Mad,
            customArgs = { a, b -> arrayOf(a, b, 5f.rf) },
        ),
        CLAMP(
            "clamp",
            { a, _ -> clamp(value = a, min = 10f.rf, max = 20f.rf) },
            5f,
            0f,
            10f,
            RemoteFloat.OperationKey.Clamp,
            isUnary = true,
            customArgs = { a, _ -> arrayOf(10f.rf, 20f.rf, a) },
        );

        internal fun getExpectedKey(a: RemoteFloat, b: RemoteFloat): RemoteStateCacheKey {
            val args = customArgs?.invoke(a, b) ?: if (isUnary) arrayOf(a) else arrayOf(a, b)
            return RemoteOperationCacheKey.create(this.opCode, *args)
        }
    }

    enum class ExpressionMode {
        Constant,
        Reference,
        Forced,
    }

    @Test
    fun testEval(@TestParameter op: Operation, @TestParameter mode: ExpressionMode) {
        val result =
            when (mode) {
                ExpressionMode.Constant -> op.fn(op.a.rf, op.b.rf)
                ExpressionMode.Reference ->
                    op.fn(op.a.rf.createReference(false), op.b.rf.createReference(false))
                ExpressionMode.Forced ->
                    op.fn(op.a.rf.createReference(true), op.b.rf.createReference(true))
            }
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()
        assertWithMessage("Operation ${op.opName} failed with constants")
            .that(context.getFloat(resultId))
            .isWithin(0.001f)
            .of(op.expected)
        val floatId = result.getFloatIdForCreationState(creationState)
        if (mode != ExpressionMode.Forced) {
            assertThat(floatId).isWithin(0.001f).of(op.expected)
        } else {
            assertThat(result.getFloatIdForCreationState(creationState)).isNaN()
        }

        val cacheKey = result.cacheKey
        assertThat(cacheKey).isNotNull()
        if (result.hasConstantValue) {
            assertThat(cacheKey).isEqualTo(RemoteConstantCacheKey(op.expected))
        } else {
            val a =
                when (mode) {
                    ExpressionMode.Constant -> op.a.rf
                    ExpressionMode.Reference -> op.a.rf.createReference(false)
                    ExpressionMode.Forced -> op.a.rf.createReference(true)
                }
            val b =
                when (mode) {
                    ExpressionMode.Constant -> op.b.rf
                    ExpressionMode.Reference -> op.b.rf.createReference(false)
                    ExpressionMode.Forced -> op.b.rf.createReference(true)
                }

            val expectedKey = op.getExpectedKey(a, b)
            assertWithMessage("Cache key mismatch for operation ${op.opName} in mode $mode")
                .that(cacheKey)
                .isEqualTo(expectedKey)
        }
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}
