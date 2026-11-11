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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext.ID_CONTINUOUS_SEC
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createTestCreationState
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.runtime.remember
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.text.DecimalFormat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteFloatTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val applicationContext = ApplicationProvider.getApplicationContext<Context>()
    lateinit var creationState: RemoteComposeCreationState

    @Before
    fun setUp() {
        // Necessary for test isolation.
        creationState = createTestCreationState()
    }

    @Test
    fun hasConstantValue_decimalFormat() {
        assertThat(RemoteFloat(21.5f).toRemoteString(DecimalFormat("#0.00")).hasConstantValue)
            .isTrue()
        assertThat(
                RemoteTimeVariables.continuousSeconds
                    .toRemoteString(DecimalFormat("#0.00"))
                    .hasConstantValue
            )
            .isFalse()
    }

    @Test
    fun textFromFloat() {
        testTextFromFloat("0.50", 0.5f.rf, DecimalFormat("#.00"))
        testTextFromFloat("-0.50", (-0.5f).rf, DecimalFormat("#.00"))
        testTextFromFloat("00.5000", 0.5f.rf, DecimalFormat("00.0000"))
        testTextFromFloat("5000000", 5000000.rf, DecimalFormat("#######0"))
    }

    fun testTextFromFloat(expected: String, value: RemoteFloat, formatter: DecimalFormat) {
        val constantFloatString = value.toRemoteString(formatter)

        val constantStringId = constantFloatString.getIdForCreationState(creationState)

        // ensure we have an id to look up
        val variableFloat = value.createReference()
        val variableFloatId = variableFloat.getIdForCreationState(creationState)
        val variableFloatString = variableFloat.toRemoteString(formatter)
        val variableStringId = variableFloatString.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getFloat(variableFloatId)).isEqualTo(value.constantValue)
        assertThat(context.getText(constantStringId)).isEqualTo(expected)
        assertThat(context.getText(variableStringId)).isEqualTo(expected)
    }

    @Test
    fun textFromFloatFormatting() {
        testTextFromFloat("0.500", 0.5f.rf, DecimalFormat("#0.000"))
        testTextFromFloat("-0.500", (-0.5f).rf, DecimalFormat("#0.000"))
        testTextFromFloat("0.50", 0.5f.rf, DecimalFormat("#,##0.00;(#,##0.00)"))
        testTextFromFloat("(0.50)", (-0.5f).rf, DecimalFormat("#,##0.00;(#,##0.00)"))
        testTextFromFloat("(50,000.50)", (-50000.50001f).rf, DecimalFormat("#,##0.00;(#,##0.00)"))
        testTextFromFloat("5000000", 5000000.rf, DecimalFormat("#0.##"))
        testTextFromFloat("050", 50f.rf, DecimalFormat("000"))
    }

    @Test
    fun textFromFloat_decimalFormat_zeroHash_constantAndNamed() {
        val formatOptional = DecimalFormat("0.######") // min 0, max 6
        val formatFixed = DecimalFormat("0.00") // min 2, max 2

        // Constants
        val c5_0 = RemoteFloat(5.0f).toRemoteString(formatOptional)
        val c5_1 = RemoteFloat(5.1f).toRemoteString(formatOptional)
        val c5_12 = RemoteFloat(5.12f).toRemoteString(formatOptional)
        val c5_123 = RemoteFloat(5.123f).toRemoteString(formatOptional)
        val c5_1234 = RemoteFloat(5.1234f).toRemoteString(formatOptional)

        // Named (dynamic)
        val n5_0 = createNamedRemoteFloat("n5_0", 5.0f).toRemoteString(formatOptional)
        val n5_1 = createNamedRemoteFloat("n5_1", 5.1f).toRemoteString(formatOptional)
        val n5_12 = createNamedRemoteFloat("n5_12", 5.12f).toRemoteString(formatOptional)
        val n5_123 = createNamedRemoteFloat("n5_123", 5.123f).toRemoteString(formatOptional)
        val n5_1234 = createNamedRemoteFloat("n5_1234", 5.1234f).toRemoteString(formatOptional)

        // Constants
        val c5_0_fixed = RemoteFloat(5.0f).toRemoteString(formatFixed)
        val c5_1_fixed = RemoteFloat(5.1f).toRemoteString(formatFixed)
        val c5_123_fixed = RemoteFloat(5.123f).toRemoteString(formatFixed)

        // Named (dynamic)
        val n5_0_fixed = createNamedRemoteFloat("n5_0_fixed", 5.0f).toRemoteString(formatFixed)
        val n5_1_fixed = createNamedRemoteFloat("n5_1_fixed", 5.1f).toRemoteString(formatFixed)
        val n5_123_fixed =
            createNamedRemoteFloat("n5_123_fixed", 5.123f).toRemoteString(formatFixed)

        // Get IDs
        val c5_0_id = c5_0.getIdForCreationState(creationState)
        val c5_1_id = c5_1.getIdForCreationState(creationState)
        val c5_12_id = c5_12.getIdForCreationState(creationState)
        val c5_123_id = c5_123.getIdForCreationState(creationState)
        val c5_1234_id = c5_1234.getIdForCreationState(creationState)

        val n5_0_id = n5_0.getIdForCreationState(creationState)
        val n5_1_id = n5_1.getIdForCreationState(creationState)
        val n5_12_id = n5_12.getIdForCreationState(creationState)
        val n5_123_id = n5_123.getIdForCreationState(creationState)
        val n5_1234_id = n5_1234.getIdForCreationState(creationState)

        val c5_0_fixed_id = c5_0_fixed.getIdForCreationState(creationState)
        val c5_1_fixed_id = c5_1_fixed.getIdForCreationState(creationState)
        val c5_123_fixed_id = c5_123_fixed.getIdForCreationState(creationState)

        val n5_0_fixed_id = n5_0_fixed.getIdForCreationState(creationState)
        val n5_1_fixed_id = n5_1_fixed.getIdForCreationState(creationState)
        val n5_123_fixed_id = n5_123_fixed.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        // Assert optional fraction digits
        assertThat(context.getText(c5_0_id)).isEqualTo("5")
        assertThat(context.getText(c5_1_id)).isEqualTo("5.1")
        assertThat(context.getText(c5_12_id)).isEqualTo("5.12")
        assertThat(context.getText(c5_123_id)).isEqualTo("5.123")
        assertThat(context.getText(c5_1234_id)).isEqualTo("5.1234")

        assertThat(context.getText(n5_0_id)).isEqualTo("5")
        assertThat(context.getText(n5_1_id)).isEqualTo("5.1")
        assertThat(context.getText(n5_12_id)).isEqualTo("5.12")
        assertThat(context.getText(n5_123_id)).isEqualTo("5.123")
        assertThat(context.getText(n5_1234_id)).isEqualTo("5.1234")

        // Assert fixed fraction digits (should keep/pad zeros and round)
        assertThat(context.getText(c5_0_fixed_id)).isEqualTo("5.00")
        assertThat(context.getText(c5_1_fixed_id)).isEqualTo("5.10")
        assertThat(context.getText(c5_123_fixed_id)).isEqualTo("5.12")

        assertThat(context.getText(n5_0_fixed_id)).isEqualTo("5.00")
        assertThat(context.getText(n5_1_fixed_id)).isEqualTo("5.10")
        assertThat(context.getText(n5_123_fixed_id)).isEqualTo("5.12")
    }

    @Test
    fun textFromFloat_precisionError_formattedAsInteger() {
        val formatOptional = DecimalFormat("0.######") // min 0, max 6

        // Simulate a value that should be 9.0 but has a tiny precision error (9.000001)
        val n9_error = createNamedRemoteFloat("n9_error", 9.000001f).toRemoteString(formatOptional)
        val n9_error_id = n9_error.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        // With tolerance, this should be recognized as an integer and formatted as "9"
        assertThat(context.getText(n9_error_id)).isEqualTo("9")
    }

    @Test
    fun textFromFloat_closeToNextInteger_formattedCorrectly() {
        val formatOptional = DecimalFormat("0.######")
        // Value close to 10.0
        val n9_999999 =
            createNamedRemoteFloat("n9_999999", 9.999999f).toRemoteString(formatOptional)
        val n9_999999_id = n9_999999.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(n9_999999_id)).isEqualTo("10")
    }

    @Test
    fun rememberNamedRemoteFloatConstant() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1, 1.0f)
        val document =
            captureSingleRemoteDocument(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                val myFloatFromConstant = rememberNamedRemoteFloat("C", 5.rf)
                RemoteBox(modifier = RemoteModifier.size(myFloatFromConstant.asRemoteDp()))
            }

        makeAndUpdateCoreDocument(
            RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document.bytes))
        )

        val floatId = context.getVariableId("USER:C")
        assertThat(context.getFloat(floatId)).isEqualTo(5f)
        context.setNamedFloatOverride("USER:C", 20f)
        assertThat(context.getFloat(floatId)).isEqualTo(20f)
    }

    @Test
    fun rememberNamedRemoteFloat() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1, 1.0f)
        val document =
            captureSingleRemoteDocument(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                val myFloatFromConstant =
                    rememberNamedRemoteFloat("E", RemoteTimeVariables.continuousSeconds)
                RemoteBox(modifier = RemoteModifier.size(myFloatFromConstant.asRemoteDp()))
            }

        makeAndUpdateCoreDocument(
            RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document.bytes))
        )

        val floatId = context.getVariableId("USER:E")
        assertThat(context.getFloat(floatId)).isEqualTo(context.getFloat(ID_CONTINUOUS_SEC))

        context.setNamedFloatOverride("USER:E", 20f)
        assertThat(context.getFloat(floatId)).isEqualTo(20f)
    }

    @Test
    fun rememberMutableRemoteFloatConstant() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1, 1.0f)
        val document =
            captureSingleRemoteDocument(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                val myFloatFromConstant = remember { MutableRemoteFloat(5.rf) }
                RemoteBox(modifier = RemoteModifier.size(myFloatFromConstant.asRemoteDp()))
            }

        var floatId = 0
        makeAndUpdateCoreDocument(
            RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document.bytes))
        ) {
            floatId =
                (it.rootLayoutComponent!!.list.first { it is FloatExpression } as FloatExpression)
                    .mId
        }

        assertThat(context.getFloat(floatId)).isEqualTo(5f)

        context.mRemoteComposeState.overrideFloat(floatId, 20f)
        assertThat(context.getFloat(floatId)).isEqualTo(20f)
    }

    @Test
    fun rememberMutableRemoteFloat() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1, 1.0f)
        val document =
            captureSingleRemoteDocument(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                val myFloatFromConstant = remember {
                    MutableRemoteFloat(RemoteTimeVariables.continuousSeconds)
                }
                RemoteBox(modifier = RemoteModifier.size(myFloatFromConstant.asRemoteDp()))
            }

        var floatId = 0
        makeAndUpdateCoreDocument(
            RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document.bytes))
        ) {
            floatId =
                (it.rootLayoutComponent!!.list.first { it is FloatExpression } as FloatExpression)
                    .mId
        }

        assertThat(context.getFloat(floatId)).isEqualTo(context.getFloat(ID_CONTINUOUS_SEC))

        context.mRemoteComposeState.overrideFloat(floatId, 20f)
        assertThat(context.getFloat(floatId)).isEqualTo(20f)
    }

    @Test
    fun rememberCreateMutableRemoteFloatConstant() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1, 1.0f)
        val document =
            captureSingleRemoteDocument(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                val myFloatFromConstant = remember { MutableRemoteFloat(5f) }
                RemoteBox(modifier = RemoteModifier.size(myFloatFromConstant.asRemoteDp()))
            }

        var floatId = 0
        makeAndUpdateCoreDocument(
            RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document.bytes))
        ) {
            floatId =
                (it.rootLayoutComponent!!.list.first { it is FloatExpression } as FloatExpression)
                    .mId
        }

        assertThat(context.getFloat(floatId)).isEqualTo(5f)

        context.mRemoteComposeState.overrideFloat(floatId, 20f)
        assertThat(context.getFloat(floatId)).isEqualTo(20f)
    }

    @Ignore("b/521688885: CoreDocument.evaluateFloatExpression ignores overrides")
    @Test
    fun evaluateFloatExpression_respectsOverride() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1, 1.0f)
        val document =
            captureSingleRemoteDocument(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                val myFloat = remember { MutableRemoteFloat(5.rf) }
                RemoteBox(modifier = RemoteModifier.size(myFloat.asRemoteDp()))
            }

        var floatId = 0
        val coreDoc =
            makeAndUpdateCoreDocument(
                RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document.bytes))
            ) {
                floatId =
                    (it.getRootLayoutComponent()!!.list.first { it is FloatExpression }
                            as FloatExpression)
                        .mId
            }

        assertThat(context.getFloat(floatId)).isEqualTo(5f)

        // Override the mutable variable
        context.mRemoteComposeState.overrideFloat(floatId, 20f)

        // Evaluate the variable as an expression pointing to targetId
        val targetId = 100
        coreDoc.evaluateFloatExpression(floatId, targetId, context)

        // Should be 20f (from override), not 5f (initial value)
        assertThat(context.getFloat(targetId)).isEqualTo(20f)
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }

    private fun makeAndUpdateCoreDocument(
        buffer: RemoteComposeBuffer? = null,
        runAfterInit: (CoreDocument) -> Unit = {},
    ) =
        CoreDocument().apply {
            val buffer = buffer ?: creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            initializeContext(context)

            runAfterInit(this)

            for (op in operations) {
                if (op is VariableSupport) {
                    op.updateVariables(context)
                }
                op.apply(context)
            }
        }
}
