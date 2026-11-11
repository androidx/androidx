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
import android.graphics.Color as AndroidColor
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.ColorAttribute
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createTestCreationState
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.player.core.state.RemoteDomains
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@SdkSuppress(minSdkVersion = 29)
class RemoteColorTest {
    @get:Rule val remoteCaptureRule = RemoteCaptureTestRule()

    private val androidContext: Context = ApplicationProvider.getApplicationContext()
    val applicationContext = ApplicationProvider.getApplicationContext<Context>()
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val creationState = createTestCreationState()

    @Test
    fun writesOutColorOnce() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1, 1.0f)

        val document =
            captureSingleRemoteDocument(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                val color =
                    RemoteColor.createNamedRemoteColor("A", Color.Red.rc).copy(alpha = 0.4f.rf)
                RemoteBox(modifier = RemoteModifier.background(color))
            }

        val operations =
            ArrayList<Operation>().apply {
                RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document.bytes))
                    .inflateFromBuffer(this)
            }

        assertThat(operations.filterIsInstance<NamedVariable>().size).isEqualTo(1)
        assertThat(operations.filterIsInstance<ColorAttribute>().size).isEqualTo(3)
    }

    @Test
    fun copy_rememberNamedRemoteColor_resultsSingleNamedColor() = runTest {
        val colorName = "TEST"
        val coreDoc =
            remoteCaptureRule.captureDocument(context = androidContext) {
                val namedColor = remember {
                    RemoteColor.createNamedRemoteColor(colorName, Color.Red.rc)
                }

                val copy = namedColor.copy(alpha = 0f.rf)

                RemoteCanvas {
                    drawRect(paint = RemotePaint { color = copy })
                    drawCircle(
                        paint = RemotePaint { color = copy },
                        radius = size.minDimension / 2f,
                    )
                }
            }
        assertThat(coreDoc.namedColors).hasLength(1)
        assertThat(coreDoc.namedColors[0]).isEqualTo("${RemoteDomains.USER}:$colorName")
    }

    @Test
    fun extensionFunctionMatches() {
        assertThat(Color.Black.rc.asConstantColor()).isEqualTo(Color.Black)
        assertThat(Color.Transparent.rc.asConstantColor()).isEqualTo(Color.Transparent)
        assertThat(RemoteColor(Color.Red).asConstantColorOrNull()).isEqualTo(Color.Red)
    }

    @Test
    fun playbackEvaluation_namedColorOverrides_constantAndDynamic() {
        val constNamed = RemoteColor.createNamedRemoteColor("constColor", Color.Red.rc)
        val dynamicRed = createNamedRemoteFloat("dynRed", 0.2f)
        val dynamicBase = RemoteColor.rgb(dynamicRed, 0.4f.rf, 0.6f.rf, 1f.rf)
        assertThat(dynamicBase.hasConstantValue).isFalse()

        val dynamicNamed1 = RemoteColor.createNamedRemoteColor("dynColor1", dynamicBase)
        val dynamicNamed2 = RemoteColor.createNamedRemoteColor("dynColor2", dynamicBase)

        val constId = constNamed.getIdForCreationState(creationState)
        val dyn1Id = dynamicNamed1.getIdForCreationState(creationState)
        val dyn2Id = dynamicNamed2.getIdForCreationState(creationState)

        val doc = makeAndPaintCoreDocument()

        val initialDynColorInt = Utils.toARGB(1f, 0.2f, 0.4f, 0.6f)
        assertThat(context.getColor(constId)).isEqualTo(Color.Red.toArgb())
        assertThat(context.getColor(dyn1Id)).isEqualTo(initialDynColorInt)
        assertThat(context.getColor(dyn2Id)).isEqualTo(initialDynColorInt)

        val overrideConst = AndroidColor.BLUE
        context.setNamedColorOverride("USER:constColor", overrideConst)
        doc.paint(context, 0)
        assertThat(context.getColor(constId)).isEqualTo(overrideConst)
        assertThat(context.getColor(dyn1Id)).isEqualTo(initialDynColorInt)
        assertThat(context.getColor(dyn2Id)).isEqualTo(initialDynColorInt)

        val overrideDyn1 = AndroidColor.YELLOW
        context.setNamedColorOverride("USER:dynColor1", overrideDyn1)
        context.setNamedFloatOverride("USER:dynRed", 0.8f)
        doc.paint(context, 0)

        val updatedDynColorInt = Utils.toARGB(1f, 0.8f, 0.4f, 0.6f)
        assertThat(context.getColor(constId)).isEqualTo(overrideConst)
        assertThat(context.getColor(dyn1Id)).isEqualTo(overrideDyn1)
        assertThat(context.getColor(dyn2Id)).isEqualTo(updatedDynColorInt)
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            initializeContext(context)
            paint(context, 0)
        }
}
