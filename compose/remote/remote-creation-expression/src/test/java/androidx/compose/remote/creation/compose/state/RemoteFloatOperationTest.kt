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
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.DataMap
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.document.RemoteDocumentWriter
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import java.lang.reflect.Proxy
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class RemoteFloatOperationTest {
    private val context = TestRemoteContext()
    private val rawBuffer = RemoteComposeBuffer().apply { header(100, 100, 1f, 0L) }
    private var nextId = 42
    private val testWriter: RemoteDocumentWriter =
        Proxy.newProxyInstance(
            RemoteDocumentWriter::class.java.classLoader,
            arrayOf(RemoteDocumentWriter::class.java),
        ) { _, method, args ->
            if (method.name.startsWith("addFloatExpression")) {
                val values = args[0] as FloatArray
                val id = nextId++
                rawBuffer.addAnimatedFloat(id, values, null)
                id
            } else {
                throw UnsupportedOperationException(method.name)
            }
        } as RemoteDocumentWriter

    private val creationState =
        RemoteComposeCreationState().apply {
            document = testWriter
            remoteDensity = RemoteDensity(1f.rf, 1f.rf)
        }

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
        ADD("+", { a, b -> a + b }, 10f, 5f, 15f, RemoteFloatOperationKey.Plus),
        SUB("-", { a, b -> a - b }, 10f, 5f, 5f, RemoteFloatOperationKey.Minus),
        MUL("*", { a, b -> a * b }, 10f, 5f, 50f, RemoteFloatOperationKey.Times),
        DIV("/", { a, b -> a / b }, 10f, 5f, 2f, RemoteFloatOperationKey.Div),
        REM("%", { a, b -> a % b }, 10f, 3f, 1f, RemoteFloatOperationKey.Rem),
        MAX("max", { a, b -> max(a, b) }, 10f, 20f, 20f, RemoteFloatOperationKey.Max),
        MIN("min", { a, b -> min(a, b) }, 10f, 20f, 10f, RemoteFloatOperationKey.Min),
        POW("pow", { a, b -> pow(a, b) }, 2f, 3f, 8f, RemoteFloatOperationKey.Pow),
        SQRT(
            "sqrt",
            { a, _ -> sqrt(a) },
            9f,
            0f,
            3f,
            RemoteFloatOperationKey.Sqrt,
            isUnary = true,
        ),
        ABS(
            "abs",
            { a, _ -> abs(a) },
            -10f,
            0f,
            10f,
            RemoteFloatOperationKey.Abs,
            isUnary = true,
        ),
        SIGN(
            "sign",
            { a, _ -> sign(a) },
            -10f,
            0f,
            -1f,
            RemoteFloatOperationKey.Sign,
            isUnary = true,
        ),
        COPYSIGN(
            "copySign",
            { a, b -> copySign(a, b) },
            10f,
            -1f,
            -10f,
            RemoteFloatOperationKey.CopySign,
        ),
        EXP(
            "exp",
            { a, _ -> exp(a) },
            1f,
            0f,
            Math.exp(1.0).toFloat(),
            RemoteFloatOperationKey.Exp,
            isUnary = true,
        ),
        CEIL(
            "ceil",
            { a, _ -> ceil(a) },
            10.1f,
            0f,
            11f,
            RemoteFloatOperationKey.Ceil,
            isUnary = true,
        ),
        FLOOR(
            "floor",
            { a, _ -> floor(a) },
            10.9f,
            0f,
            10f,
            RemoteFloatOperationKey.Floor,
            isUnary = true,
        ),
        LOG(
            "log",
            { a, _ -> log(a) },
            100f,
            0f,
            2f,
            RemoteFloatOperationKey.Log,
            isUnary = true,
        ),
        LN(
            "ln",
            { a, _ -> ln(a) },
            Math.E.toFloat(),
            0f,
            0.99999994f,
            RemoteFloatOperationKey.Ln,
            isUnary = true,
        ),
        ROUND(
            "round",
            { a, _ -> round(a) },
            10.5f,
            0f,
            11f,
            RemoteFloatOperationKey.Round,
            isUnary = true,
        ),
        SIN("sin", { a, _ -> sin(a) }, 0f, 0f, 0f, RemoteFloatOperationKey.Sin, isUnary = true),
        COS("cos", { a, _ -> cos(a) }, 0f, 0f, 1f, RemoteFloatOperationKey.Cos, isUnary = true),
        TAN("tan", { a, _ -> tan(a) }, 0f, 0f, 0f, RemoteFloatOperationKey.Tan, isUnary = true),
        ASIN(
            "asin",
            { a, _ -> asin(a) },
            0f,
            0f,
            0f,
            RemoteFloatOperationKey.Asin,
            isUnary = true,
        ),
        ACOS(
            "acos",
            { a, _ -> acos(a) },
            1f,
            0f,
            0f,
            RemoteFloatOperationKey.Acos,
            isUnary = true,
        ),
        ATAN(
            "atan",
            { a, _ -> atan(a) },
            0f,
            0f,
            0f,
            RemoteFloatOperationKey.Atan,
            isUnary = true,
        ),
        ATAN2("atan2", { a, b -> atan2(a, b) }, 0f, 1f, 0f, RemoteFloatOperationKey.Atan2),
        CBRT(
            "cbrt",
            { a, _ -> cbrt(a) },
            27f,
            0f,
            3f,
            RemoteFloatOperationKey.Cbrt,
            isUnary = true,
        ),
        DEG(
            "toDeg",
            { a, _ -> toDeg(a) },
            Math.PI.toFloat(),
            0f,
            180f,
            RemoteFloatOperationKey.ToDeg,
            isUnary = true,
        ),
        RAD(
            "toRad",
            { a, _ -> toRad(a) },
            180f,
            0f,
            Math.PI.toFloat(),
            RemoteFloatOperationKey.ToRad,
            isUnary = true,
        ),
        LERP(
            "lerp",
            { a, b -> lerp(a, b, 0.5f.rf) },
            100f,
            200f,
            150f,
            RemoteFloatOperationKey.Lerp,
            customArgs = { a, b -> arrayOf(a, b, 0.5f.rf) },
        ),
        MAD(
            "mad",
            { a, b -> mad(a, b, 5f.rf) },
            10f,
            2f,
            25f,
            RemoteFloatOperationKey.Mad,
            customArgs = { a, b -> arrayOf(a, b, 5f.rf) },
        ),
        CLAMP(
            "clamp",
            { a, _ -> clamp(value = a, min = 10f.rf, max = 20f.rf) },
            5f,
            0f,
            10f,
            RemoteFloatOperationKey.Clamp,
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
        val resultId = creationState.getId(result)
        evaluateCoreDocument()
        assertWithMessage("Operation ${op.opName} failed with constants")
            .that(context.getFloat(resultId))
            .isWithin(0.001f)
            .of(op.expected)
        val floatId = creationState.getFloatId(result)
        if (mode != ExpressionMode.Forced) {
            assertThat(floatId).isWithin(0.001f).of(op.expected)
        } else {
            assertThat(creationState.getFloatId(result)).isNaN()
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

    private fun evaluateCoreDocument() =
        CoreDocument().apply {
            rawBuffer.buffer.index = 0
            initFromBuffer(rawBuffer)
            initializeContext(context)
        }

    private class TestRemoteContext : RemoteContext() {
        private val floats = HashMap<Int, Float>()

        override fun clearVariables() {
            super.clearVariables()
            floats.clear()
        }

        override fun loadFloat(id: Int, value: Float) {
            floats[id] = value
        }

        override fun overrideFloat(id: Int, value: Float) {
            floats[id] = value
        }

        override fun getFloat(id: Int): Float = floats[id] ?: Float.NaN

        override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {}

        override fun getPathData(instanceId: Int): FloatArray? = null

        override fun loadVariableName(varName: String, varId: Int, varType: Int) {}

        override fun loadColor(id: Int, color: Int) {}

        override fun setNamedColorOverride(colorName: String, color: Int) {}

        override fun setNamedStringOverride(stringName: String, value: String) {}

        override fun clearNamedStringOverride(stringName: String) {}

        override fun setNamedBooleanOverride(booleanName: String, value: Boolean) {}

        override fun clearNamedBooleanOverride(booleanName: String) {}

        override fun setNamedIntegerOverride(integerName: String, value: Int) {}

        override fun clearNamedIntegerOverride(integerName: String) {}

        override fun setNamedFloatOverride(floatName: String, value: Float) {}

        override fun clearNamedFloatOverride(floatName: String) {}

        override fun setNamedLong(name: String, value: Long) {}

        override fun setNamedDataOverride(dataName: String, value: Any) {}

        override fun clearNamedDataOverride(dataName: String) {}

        override fun addCollection(id: Int, collection: ArrayAccess) {}

        override fun putDataMap(id: Int, map: DataMap) {}

        override fun getDataMap(id: Int): DataMap? = null

        override fun runAction(id: Int, metadata: String) {}

        override fun runNamedAction(id: Int, value: Any?) {}

        override fun putObject(id: Int, value: Any) {}

        override fun getObject(id: Int): Any? = null

        override fun hapticEffect(type: Int) {}

        override fun loadBitmap(
            imageId: Int,
            encoding: Short,
            type: Short,
            width: Int,
            height: Int,
            bitmap: ByteArray,
        ) {}

        override fun loadText(id: Int, text: String) {}

        override fun getText(id: Int): String? = null

        override fun loadInteger(id: Int, value: Int) {}

        override fun overrideInteger(id: Int, value: Int) {}

        override fun overrideText(id: Int, valueId: Int) {}

        override fun loadAnimatedFloat(id: Int, animatedFloat: FloatExpression) {}

        override fun loadShader(id: Int, value: ShaderData) {}

        override fun getInteger(id: Int): Int = 0

        override fun getLong(id: Int): Long = 0L

        override fun getColor(id: Int): Int = 0

        override fun listensTo(id: Int, variableSupport: VariableSupport) {}

        override fun updateOps(): Int = 0

        override fun getShader(id: Int): ShaderData? = null

        override fun addClickArea(
            id: Int,
            contentDescriptionId: Int,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            metadataId: Int,
        ) {}
    }
}
