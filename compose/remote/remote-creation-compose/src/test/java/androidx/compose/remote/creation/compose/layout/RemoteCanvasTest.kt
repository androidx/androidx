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

package androidx.compose.remote.creation.compose.layout

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.creation.compose.capture.PaintTrackerTest.TestPaintChanges
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontVariation
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteCanvasTest {
    private val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    private lateinit var creationState: RemoteComposeCreationState
    private lateinit var recordingCanvas: RecordingCanvas
    private lateinit var remoteCanvas: RemoteCanvas

    @Before
    fun setUp() {
        val size = Size(500f, 500f)
        creationState =
            RemoteComposeCreationState(
                androidx.compose.remote.creation.platform.AndroidxRcPlatformServices(),
                size,
            )
        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        recordingCanvas = RecordingCanvas(bitmap)
        recordingCanvas.creationState = creationState
        remoteCanvas = RemoteCanvas(recordingCanvas)
    }

    @Test
    fun testFontVariationSettingsSync() {
        val settings = FontVariation.Settings(FontVariation.weight(500), FontVariation.width(100f))
        val paint = RemotePaint { fontVariationSettings = settings }

        val wghtId = creationState.document.addText("wght")
        val wdthId = creationState.document.addText("wdth")
        context.loadText(wghtId, "wght")
        context.loadText(wdthId, "wdth")

        // Draw with font variations
        remoteCanvas.drawText("Hello".rs, 0f.rf, 0f.rf, paint)

        // Draw with cleared font variations
        val paint2 = RemotePaint { fontVariationSettings = null }
        remoteCanvas.drawText("World".rs, 10f.rf, 10f.rf, paint2)

        val documentOps = getOperations()
        val paintDataOps = documentOps.filterIsInstance<PaintData>()
        assertThat(paintDataOps).hasSize(2)

        val changes1 = TestPaintChanges()
        paintDataOps[0].mPaintData.applyPaintChange(context.paintContext!!, changes1)

        assertThat(changes1.fontVariationAxesSet).isTrue()
        assertThat(changes1.mFontAxisTags).isEqualTo(arrayOf("wght", "wdth"))
        assertThat(changes1.mFontAxisValues).isEqualTo(floatArrayOf(500f, 100f))

        val changes2 = TestPaintChanges()
        paintDataOps[1].mPaintData.applyPaintChange(context.paintContext!!, changes2)

        assertThat(changes2.fontVariationAxesSet).isTrue()
        assertThat(changes2.mFontAxisTags).isEmpty()
    }

    private fun getOperations(): List<Operation> =
        CoreDocument().run {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            operations.onEach {
                if (it is VariableSupport) {
                    it.updateVariables(context)
                }
            }
        }
}
