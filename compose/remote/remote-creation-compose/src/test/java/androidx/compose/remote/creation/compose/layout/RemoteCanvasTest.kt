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
import android.graphics.Color
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.capture.PaintTrackerTest.TestPaintChanges
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.state.RemoteBoolean.Companion.createNamedRemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap.Companion.createOffscreenRemoteBitmap
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.util.MyRemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.compose.test.utils.TestPlayer
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
    private lateinit var remoteCanvas: RemoteCanvas

    @Before
    fun setUp() {
        val size = Size(500f, 500f)
        creationState =
            RemoteComposeCreationState(
                androidx.compose.remote.creation.platform.AndroidxRcPlatformServices(),
                size,
            )
        remoteCanvas = RemoteCanvas(creationState)
    }

    @Test
    fun testFontVariationSettingsSync() {
        setupRemoteCanvas()

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

        remoteCanvas.flush()
        val documentOps = getOperations(remoteCanvas.document.buffer)
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
        setupRemoteCanvas()

        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)
        val sub = x + y // Common subexpression

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", true)
        val condition3 = createNamedRemoteBoolean("cond3", true)

        remoteCanvas.drawConditionally(condition1) {
            remoteCanvas.drawConditionally(condition2) {
                remoteCanvas.drawConditionally(condition3) {
                    remoteCanvas.save()
                    remoteCanvas.translate(1f.rf, 1f.rf)
                    remoteCanvas.drawRect(sub, 0f.rf, 0f.rf, 0f.rf, null)
                    remoteCanvas.restore()
                }
            }
        }

        remoteCanvas.drawConditionally(condition2) {
            remoteCanvas.save()
            remoteCanvas.translate(1f.rf, 1f.rf)
            remoteCanvas.drawRect(sub, 10f.rf, 10f.rf, 10f.rf, null)
            remoteCanvas.restore()
        }

        remoteCanvas.flush()

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
                "addMatrixTranslate(1.0, 1.0)",
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
                "addMatrixTranslate(1.0, 1.0)",
                "addDrawRect(ID(44), 10.0, 10.0, 10.0)",
                "addMatrixRestore",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testCSE_DependencyOrderingBug() {
        setupRemoteCanvas()

        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)
        val a = x + y
        val b = a * 2f
        val c = b + 5f

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", true)

        // Use c in two places to make it common
        remoteCanvas.drawConditionally(condition1) {
            remoteCanvas.drawRect(c, 0f.rf, 0f.rf, 0f.rf, null)
        }
        remoteCanvas.drawConditionally(condition2) {
            remoteCanvas.drawRect(c, 10f.rf, 10f.rf, 10f.rf, null)
        }

        // Use b in another place to make it common too!
        remoteCanvas.drawRect(b, 20f.rf, 20f.rf, 20f.rf, null)

        remoteCanvas.flush()

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
        setupRemoteCanvas()

        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)
        val a = x + y // Should be common!
        val b = a * 2f // Common
        val c = a + 5f // Not common

        // Use b in two places
        remoteCanvas.drawRect(b, 0f.rf, 0f.rf, 0f.rf, null)
        remoteCanvas.drawRect(b, 10f.rf, 10f.rf, 10f.rf, null)

        // Use c in one place
        remoteCanvas.drawRect(c, 20f.rf, 20f.rf, 20f.rf, null)

        remoteCanvas.flush()

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
        setupRemoteCanvas()

        val condition = createNamedRemoteBoolean("cond", true)

        remoteCanvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, null) // Op 1

        remoteCanvas.drawConditionally(condition) {
            remoteCanvas.drawRect(10f.rf, 10f.rf, 20f.rf, 20f.rf, null) // Op 2
        } // Op 3

        remoteCanvas.drawRect(20f.rf, 20f.rf, 30f.rf, 30f.rf, null) // Op 4

        // This test guards against operations after drawConditionally being reordered.
        // If drawConditionally uses record instead of recordRenderingOp, it fails to add
        // itself to the dependency chain, allowing subsequent operations to be reordered.
        remoteCanvas.flush()

        val calls = fakeBuffer.calls
        val idx3 = calls.indexOfFirst { it.startsWith("addConditionalOperations") }
        val idx4 = calls.indexOfLast { it.startsWith("addDrawRect") && it.contains("20.0") }

        assertThat(idx3).isLessThan(idx4)
    }

    @Test
    fun testLoop_ChainsDependencies() {
        setupRemoteCanvas()

        val from = createNamedRemoteFloat("from", 0f)
        val until = createNamedRemoteFloat("until", 10f)
        val step = createNamedRemoteFloat("step", 1f)

        remoteCanvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, null) // Op 1

        remoteCanvas.loop(from, until, step) { _ ->
            remoteCanvas.drawRect(10f.rf, 10f.rf, 20f.rf, 20f.rf, null) // Op 2
        } // Op 3

        remoteCanvas.drawRect(20f.rf, 20f.rf, 30f.rf, 30f.rf, null) // Op 4

        // This test guards against operations after loop being reordered.
        // If loop uses record instead of recordRenderingOp, it fails to add
        // itself to the dependency chain, allowing subsequent operations to be reordered.
        remoteCanvas.flush()

        val calls = fakeBuffer.calls
        val idx3 = calls.indexOfFirst { it.startsWith("addLoopStart") }
        val idx4 = calls.indexOfLast { it.startsWith("addDrawRect") && it.contains("20.0") }

        assertThat(idx3).isLessThan(idx4)
    }

    @Test
    fun testCSE_PropagationOrder_RootToLeaf() {
        setupRemoteCanvas()

        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)

        val child = x + y
        val parent = child * 2f
        val grandParent = parent + 5f

        val condition = createNamedRemoteBoolean("cond", true)

        remoteCanvas.drawConditionally(condition) {
            remoteCanvas.drawRect(grandParent, 0f.rf, 0f.rf, 0f.rf, null)
        }

        remoteCanvas.drawRect(child, 10f.rf, 10f.rf, 10f.rf, null)
        remoteCanvas.drawRect(parent, 20f.rf, 20f.rf, 20f.rf, null)

        // This test guards against arbitrary iteration order in CSE Pass 1.
        // Propagation of ideal spans must happen in root-to-leaf order. If a child is processed
        // before its parent, it might miss the span propagated from the parent.
        remoteCanvas.flush()

        val animatedFloatCalls = fakeBuffer.calls.filter { it.startsWith("addAnimatedFloat") }
        assertThat(animatedFloatCalls.size).isEqualTo(3)
    }

    @Test
    fun testClipRect_preservesSaveRestoreWhenOptimized() {
        setupRemoteCanvas(enableOptimizations = true)

        remoteCanvas.save()
        remoteCanvas.clipRect(10f.rf, 10f.rf, 50f.rf, 50f.rf)
        remoteCanvas.drawRect(0f.rf, 0f.rf, 100f.rf, 100f.rf, null)
        remoteCanvas.restore()
        remoteCanvas.drawRect(0f.rf, 0f.rf, 100f.rf, 100f.rf, null)

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsAtLeast(
                "addMatrixSave",
                "addClipRect(10.0, 10.0, 50.0, 50.0)",
                "addDrawRect(0.0, 0.0, 100.0, 100.0)",
                "addMatrixRestore",
                "addDrawRect(0.0, 0.0, 100.0, 100.0)",
            )
            .inOrder()
    }

    @Test
    fun testDrawToOffscreenBitmap_preservesOuterSaveRestoreWhenOptimized() {
        setupRemoteCanvas(enableOptimizations = true)

        val offscreenBitmap = createOffscreenRemoteBitmap(100, 100)

        remoteCanvas.save()
        remoteCanvas.translate(10f.rf, 10f.rf)
        remoteCanvas.save()
        remoteCanvas.scale(2f.rf, 2f.rf)

        remoteCanvas.drawToOffscreenBitmap(offscreenBitmap, Color.TRANSPARENT) {
            remoteCanvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, null)
        }

        remoteCanvas.restore()
        remoteCanvas.restore()
        remoteCanvas.drawRect(0f.rf, 0f.rf, 50f.rf, 50f.rf, null)

        remoteCanvas.flush()

        val calls = fakeBuffer.calls
        val lastRestoreIndex = calls.lastIndexOf("addMatrixRestore")
        val finalDrawRectIndex = calls.indexOfLast {
            it.startsWith("addDrawRect") && it.contains("50.0")
        }

        assertThat(lastRestoreIndex).isNotEqualTo(-1)
        assertThat(finalDrawRectIndex).isNotEqualTo(-1)
        assertThat(lastRestoreIndex).isLessThan(finalDrawRectIndex)
        assertThat(calls.count { it == "addMatrixRestore" }).isEqualTo(2)
    }

    @Test
    fun testLoop_evaluatesLoopIndexForEachIterationDuringPlayback() {
        setupRemoteCanvas()

        remoteCanvas.loop(0f.rf, 3f.rf, 1f.rf) { index ->
            remoteCanvas.drawRect(index * 10f.rf, 0f.rf, (index * 10f.rf) + 5f.rf, 5f.rf, null)
        }

        remoteCanvas.flush()

        val drawCalls = captureDrawCalls().filter { it.startsWith("drawRect") }
        assertThat(drawCalls)
            .containsExactly(
                "drawRect(0.000000, 0.000000, 5.000000, 5.000000)",
                "drawRect(10.000000, 0.000000, 15.000000, 5.000000)",
                "drawRect(20.000000, 0.000000, 25.000000, 5.000000)",
            )
            .inOrder()
    }

    @Test
    fun testCustomComponent_withChildDrawingContent() {
        setupRemoteCanvas(
            profileMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
        )

        remoteCanvas.custom(
            "testConfig",
            content = { remoteCanvas.drawRect(1f.rf, 2f.rf, 3f.rf, 4f.rf, null) },
        )

        remoteCanvas.flush()

        val contentStartIndex = fakeBuffer.calls.indexOf("addContentStart")
        val drawRectIndex = fakeBuffer.calls.indexOf("addDrawRect(1.0, 2.0, 3.0, 4.0)")
        val containerEndIndex = fakeBuffer.calls.indexOf("addContainerEnd")

        assertThat(contentStartIndex).isNotEqualTo(-1)
        assertThat(drawRectIndex).isNotEqualTo(-1)
        assertThat(containerEndIndex).isNotEqualTo(-1)
        assertThat(contentStartIndex).isLessThan(drawRectIndex)
        assertThat(drawRectIndex).isLessThan(containerEndIndex)
    }

    private fun captureDrawCalls(): List<String> {
        val wireBuffer = fakeBuffer.buffer
        val bytes = wireBuffer.buffer.copyOfRange(0, wireBuffer.size)
        val player = TestPlayer.fromBytes(bytes, 500f, 500f)
        return player.paint()
    }

    private fun setupRemoteCanvas(
        enableOptimizations: Boolean = false,
        profileMask: Int = RcProfiles.PROFILE_ANDROIDX,
    ) {
        val profile =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                profileMask,
                AndroidxRcPlatformServices(),
            ) { creationDisplayInfo, p, _ ->
                MyRemoteComposeWriterAndroid(
                    p,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, profileMask),
                )
            }
        creationState =
            RemoteComposeCreationState(
                RemoteCreationDisplayInfo(500, 500, 160, 1f),
                null,
                profile,
            )
        remoteCanvas = RemoteCanvas(creationState, enableOptimizations = enableOptimizations)
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
