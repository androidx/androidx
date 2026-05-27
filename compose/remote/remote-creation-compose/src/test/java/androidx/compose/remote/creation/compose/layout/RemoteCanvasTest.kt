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
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.capture.PaintTrackerTest.TestPaintChanges
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.util.MyRemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.profile.Profile
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
    private val fakeBuffer = TestRemoteComposeBuffer()
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
        val platform = androidx.compose.remote.creation.platform.AndroidxRcPlatformServices()
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                callbacks ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }

        creationState =
            RemoteComposeCreationState(RemoteCreationDisplayInfo(500, 500, 160, 1f), null, profile)

        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        recordingCanvas = RecordingCanvas(bitmap)
        recordingCanvas.creationState = creationState
        remoteCanvas = RemoteCanvas(recordingCanvas)

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

        recordingCanvas.flush()
        val documentOps = getOperations(recordingCanvas.document.buffer)
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

    @Test
    fun testHoisting_3LevelsDeep() {
        val platform = androidx.compose.remote.creation.platform.AndroidxRcPlatformServices()
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                callbacks ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }

        creationState =
            RemoteComposeCreationState(RemoteCreationDisplayInfo(500, 500, 160, 1f), null, profile)

        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        recordingCanvas = RecordingCanvas(bitmap)
        recordingCanvas.creationState = creationState
        remoteCanvas = RemoteCanvas(recordingCanvas)

        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)
        val sub = x + y // Common subexpression

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)
        val condition3 = RemoteBoolean.createNamedRemoteBoolean("cond3", true)

        remoteCanvas.drawConditionally(condition1) {
            remoteCanvas.drawConditionally(condition2) {
                remoteCanvas.drawConditionally(condition3) {
                    recordingCanvas.save()
                    remoteCanvas.drawRect(sub, 0f.rf, 0f.rf, 0f.rf, null)
                    recordingCanvas.restore()
                }
            }
        }

        remoteCanvas.drawConditionally(condition2) {
            recordingCanvas.save()
            remoteCanvas.drawRect(sub, 10f.rf, 10f.rf, 10f.rf, null)
            recordingCanvas.restore()
        }

        recordingCanvas.flush()

        // Verify that sub is hoisted to Root level because it is used in two different branches of
        // condition2 if condition2 was top level, or just Root because condition2 is used in two
        // different contexts!
        // Common ancestor of depth 3 and depth 1 is depth 0 (Root).

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "setNamedVariable(43, \"USER:y\", 1)",
                "addAnimatedFloat(44) = ([42] [43] + )",
                "setNamedVariable(45, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(45), 0.0)",
                "setNamedVariable(46, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(46), 0.0)",
                "setNamedVariable(47, \"USER:cond3\", 4)",
                "addConditionalOperations(1, ID(47), 0.0)",
                "addMatrixSave",
                "addDrawRect(ID(44), 0.0, 0.0, 0.0)",
                "addMatrixRestore",
                "endConditionalOperations",
                "addContainerEnd",
                "endConditionalOperations",
                "addContainerEnd",
                "endConditionalOperations",
                "addContainerEnd",
                "addConditionalOperations(1, ID(46), 0.0)",
                "addMatrixSave",
                "addDrawRect(ID(44), 10.0, 10.0, 10.0)",
                "addMatrixRestore",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testCSE_DependencyOrderingBug() {
        val platform = androidx.compose.remote.creation.platform.AndroidxRcPlatformServices()
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                callbacks ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }

        creationState =
            RemoteComposeCreationState(RemoteCreationDisplayInfo(500, 500, 160, 1f), null, profile)

        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        recordingCanvas = RecordingCanvas(bitmap)
        recordingCanvas.creationState = creationState
        remoteCanvas = RemoteCanvas(recordingCanvas)

        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)
        val a = x + y
        val b = a * 2f
        val c = b + 5f

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)

        // Use c in two places to make it common
        remoteCanvas.drawConditionally(condition1) {
            remoteCanvas.drawRect(c, 0f.rf, 0f.rf, 0f.rf, null)
        }
        remoteCanvas.drawConditionally(condition2) {
            remoteCanvas.drawRect(c, 10f.rf, 10f.rf, 10f.rf, null)
        }

        // Use b in another place to make it common too!
        remoteCanvas.drawRect(b, 20f.rf, 20f.rf, 20f.rf, null)

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "setNamedVariable(43, \"USER:y\", 1)",
                "addAnimatedFloat(44) = ([42] [43] + 2.0 * )",
                "addAnimatedFloat(45) = ([44] 5.0 + )",
                "setNamedVariable(46, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(46), 0.0)",
                "addDrawRect(ID(45), 0.0, 0.0, 0.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(47, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(47), 0.0)",
                "addDrawRect(ID(45), 10.0, 10.0, 10.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "addDrawRect(ID(44), 20.0, 20.0, 20.0)",
            )
    }

    @Test
    fun testCSE_NestedDependencyBug() {
        val platform = androidx.compose.remote.creation.platform.AndroidxRcPlatformServices()
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                callbacks ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }

        creationState =
            RemoteComposeCreationState(RemoteCreationDisplayInfo(500, 500, 160, 1f), null, profile)

        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        recordingCanvas = RecordingCanvas(bitmap)
        recordingCanvas.creationState = creationState
        remoteCanvas = RemoteCanvas(recordingCanvas)

        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)
        val a = x + y // Should be common!
        val b = a * 2f // Common
        val c = a + 5f // Not common

        // Use b in two places
        remoteCanvas.drawRect(b, 0f.rf, 0f.rf, 0f.rf, null)
        remoteCanvas.drawRect(b, 10f.rf, 10f.rf, 10f.rf, null)

        // Use c in one place
        remoteCanvas.drawRect(c, 20f.rf, 20f.rf, 20f.rf, null)

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "setNamedVariable(43, \"USER:y\", 1)",
                "addAnimatedFloat(44) = ([42] [43] + )",
                "addAnimatedFloat(45) = ([44] 2.0 * )",
                "addDrawRect(ID(45), 0.0, 0.0, 0.0)",
                "addDrawRect(ID(45), 10.0, 10.0, 10.0)",
                "addAnimatedFloat(46) = ([44] 5.0 + )",
                "addDrawRect(ID(46), 20.0, 20.0, 20.0)",
            )
    }

    @Test
    fun testDrawConditionally_ChainsDependencies() {
        val platform = androidx.compose.remote.creation.platform.AndroidxRcPlatformServices()
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                callbacks ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }

        creationState =
            RemoteComposeCreationState(RemoteCreationDisplayInfo(100, 100, 160, 1f), null, profile)

        val bitmap =
            android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
        recordingCanvas = RecordingCanvas(bitmap)
        recordingCanvas.creationState = creationState
        remoteCanvas = RemoteCanvas(recordingCanvas)

        val condition = RemoteBoolean.createNamedRemoteBoolean("cond", true)

        remoteCanvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, null) // Op 1

        remoteCanvas.drawConditionally(condition) {
            remoteCanvas.drawRect(10f.rf, 10f.rf, 20f.rf, 20f.rf, null) // Op 2
        } // Op 3

        remoteCanvas.drawRect(20f.rf, 20f.rf, 30f.rf, 30f.rf, null) // Op 4

        // This test guards against operations after drawConditionally being reordered.
        // If drawConditionally uses record instead of recordRenderingOp, it fails to add
        // itself to the dependency chain, allowing subsequent operations to be reordered.
        recordingCanvas.flush()

        val calls = fakeBuffer.calls
        val idx3 = calls.indexOfFirst { it.startsWith("addConditionalOperations") }
        val idx4 = calls.indexOfLast { it.startsWith("addDrawRect") && it.contains("20.0") }

        assertThat(idx3).isLessThan(idx4)
    }

    @Test
    fun testLoop_ChainsDependencies() {
        val platform = androidx.compose.remote.creation.platform.AndroidxRcPlatformServices()
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                callbacks ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }

        creationState =
            RemoteComposeCreationState(RemoteCreationDisplayInfo(100, 100, 160, 1f), null, profile)

        val bitmap =
            android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
        recordingCanvas = RecordingCanvas(bitmap)
        recordingCanvas.creationState = creationState
        remoteCanvas = RemoteCanvas(recordingCanvas)

        val from = RemoteFloat.createNamedRemoteFloat("from", 0f)
        val until = RemoteFloat.createNamedRemoteFloat("until", 10f)
        val step = RemoteFloat.createNamedRemoteFloat("step", 1f)

        remoteCanvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, null) // Op 1

        remoteCanvas.loop(from, until, step) { index ->
            remoteCanvas.drawRect(10f.rf, 10f.rf, 20f.rf, 20f.rf, null) // Op 2
        } // Op 3

        remoteCanvas.drawRect(20f.rf, 20f.rf, 30f.rf, 30f.rf, null) // Op 4

        // This test guards against operations after loop being reordered.
        // If loop uses record instead of recordRenderingOp, it fails to add
        // itself to the dependency chain, allowing subsequent operations to be reordered.
        recordingCanvas.flush()

        val calls = fakeBuffer.calls
        val idx3 = calls.indexOfFirst { it.startsWith("addLoopStart") }
        val idx4 = calls.indexOfLast { it.startsWith("addDrawRect") && it.contains("20.0") }

        assertThat(idx3).isLessThan(idx4)
    }

    @Test
    fun testCSE_PropagationOrder_RootToLeaf() {
        val platform = androidx.compose.remote.creation.platform.AndroidxRcPlatformServices()
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                callbacks ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }

        creationState =
            RemoteComposeCreationState(RemoteCreationDisplayInfo(100, 100, 160, 1f), null, profile)

        val bitmap =
            android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
        recordingCanvas = RecordingCanvas(bitmap)
        recordingCanvas.creationState = creationState
        remoteCanvas = RemoteCanvas(recordingCanvas)

        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)

        val child = x + y
        val parent = child * 2f
        val grandParent = parent + 5f

        val condition = RemoteBoolean.createNamedRemoteBoolean("cond", true)

        remoteCanvas.drawConditionally(condition) {
            remoteCanvas.drawRect(grandParent, 0f.rf, 0f.rf, 0f.rf, null)
        }

        remoteCanvas.drawRect(child, 10f.rf, 10f.rf, 10f.rf, null)
        remoteCanvas.drawRect(parent, 20f.rf, 20f.rf, 20f.rf, null)

        // This test guards against arbitrary iteration order in CSE Pass 1.
        // Propagation of ideal spans must happen in root-to-leaf order. If a child is processed
        // before its parent, it might miss the span propagated from the parent.
        recordingCanvas.flush()

        val animatedFloatCalls = fakeBuffer.calls.filter { it.startsWith("addAnimatedFloat") }
        assertThat(animatedFloatCalls.size).isEqualTo(3)
    }

    private fun getOperations(buffer: RemoteComposeBuffer): List<Operation> =
        CoreDocument().run {
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            operations.onEach {
                if (it is VariableSupport) {
                    it.updateVariables(context)
                }
            }
        }
}
