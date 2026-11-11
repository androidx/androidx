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
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.createTestCreationState
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import com.google.common.truth.Truth.assertThat
import java.text.DecimalFormat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteIntTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    lateinit var creationState: RemoteComposeCreationState

    @Before
    fun setUp() {
        // Necessary for test isolation.
        creationState = createTestCreationState()
    }

    @Test
    fun toRemoteString_decimalFormat() {
        val sum = RemoteInt(100) + 20
        val sumString = sum.toRemoteString(DecimalFormat("##0"))
        val sumStringId = sumString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(sumStringId)).isEqualTo("120")
    }

    @Test
    fun toRemoteString_operations() {
        val formatOptional = DecimalFormat("0.######") // min 0, max 6
        val x = createNamedRemoteInt("x", 5)
        val str = x.toRemoteString(formatOptional)
        str.getIdForCreationState(creationState)

        val ops = getOperationsStrings()
        assertThat(ops)
            .containsExactly(
                "VariableName[43] = \"USER:x\" type=4",
                "IntegerConstant[43] = 5",
                "TextFromFloat[44] = [43] 255.0 517",
            )
            .inOrder()
    }

    @Test
    fun remoteFloat_toRemoteInt_toRemoteString_customFormat() {
        val formatOptional = DecimalFormat("0.######")
        val n3_99 = createNamedRemoteFloat("n3_99", 3.99f)
        val str = n3_99.toRemoteInt().toRemoteString(formatOptional)
        val strId = str.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("3")
    }

    @Test
    fun hasConstantValue_decimalFormat() {
        assertThat(RemoteInt(10).toRemoteString(DecimalFormat("#0")).hasConstantValue).isTrue()
        assertThat(
                RemoteTimeVariables.continuousSeconds
                    .toRemoteInt()
                    .toRemoteString(DecimalFormat("#0"))
                    .hasConstantValue
            )
            .isFalse()
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
}
