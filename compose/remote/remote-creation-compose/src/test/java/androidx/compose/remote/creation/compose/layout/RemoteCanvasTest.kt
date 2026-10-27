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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.Custom.CustomProperty
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.CanvasOp
import androidx.compose.remote.creation.compose.capture.CanvasOperationBuffer
import androidx.compose.remote.creation.compose.capture.PaintTrackerTest.TestPaintChanges
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.MutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.MutableRemoteString
import androidx.compose.remote.creation.compose.state.RemoteBlendMode
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteBoolean.Companion.createNamedRemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatArray
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap.Companion.createOffscreenRemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteIntArray
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteString.Companion.createNamedRemoteString
import androidx.compose.remote.creation.compose.state.RemoteStringArray
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.selectIfGt
import androidx.compose.remote.creation.compose.state.selectIfLt
import androidx.compose.remote.creation.compose.util.MyRemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.TestPlayer
import androidx.compose.remote.player.core.platform.AndroidCustomContext
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontVariation
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteCanvasTest {
    private enum class DummyEnum {
        VALUE
    }

    private val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    private val fakeBuffer = TestRemoteComposeBuffer()
    private lateinit var creationState: RemoteComposeCreationState
    private lateinit var remoteCanvas: RemoteCanvas

    @Before
    fun setUp() {
        val platform = AndroidxRcPlatformServices()
        val testProfileMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, testProfileMask, platform) {
                creationDisplayInfo,
                profile,
                callbacks ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, testProfileMask),
                )
            }

        creationState =
            RemoteComposeCreationState(RemoteCreationDisplayInfo(500, 500, 160, 1f), null, profile)
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

        remoteCanvas.drawToOffscreenBitmap(offscreenBitmap, AndroidColor.TRANSPARENT) {
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

    @Test
    fun testDrawPath_defaultWinding_deduplicatesRepeatedDraws() {
        setupRemoteCanvas()

        val path =
            RemotePath().apply {
                moveTo(0f, 0f)
                lineTo(10f, 10f)
                close()
            }

        remoteCanvas.drawPath(path)
        remoteCanvas.drawPath(path)

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addPathData(42)",
                "addDrawPath(42)",
                "addDrawPath(42)",
            )
            .inOrder()
    }

    @Test
    fun testDrawScaledBitmap_nullContentDescription_doesNotEmitEmptyTextData() {
        setupRemoteCanvas()

        val bitmap = RemoteImageBitmap.createForId(42)
        remoteCanvas.drawScaledBitmap(
            bitmap = bitmap,
            srcLeft = 0f.rf,
            srcTop = 0f.rf,
            srcRight = 10f.rf,
            srcBottom = 10f.rf,
            dstLeft = 0f.rf,
            dstTop = 0f.rf,
            dstRight = 20f.rf,
            dstBottom = 20f.rf,
            scaleType = RemoteComposeWriter.IMAGE_SCALE_FIT,
            scaleFactor = 1f.rf,
            contentDescription = null,
        )

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls).containsExactly("drawScaledBitmap(42)")
    }

    @Test
    fun testDrawBitmap_withRemoteFloatBounds_recordsOperationAndTracksDependencies() {
        setupRemoteCanvas()

        val bitmap = RemoteImageBitmap.createForId(99)
        val left = createNamedRemoteFloat("left", 10f)
        val top = createNamedRemoteFloat("top", 20f)
        val right = createNamedRemoteFloat("right", 110f)
        val bottom = createNamedRemoteFloat("bottom", 120f)

        remoteCanvas.drawBitmap(
            bitmap = bitmap,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            paint = RemotePaint(),
        )

        assertThat(fakeBuffer.calls).isEmpty()

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addPaint",
                "setNamedVariable(42, \"USER:left\", 1)",
                "setNamedVariable(43, \"USER:top\", 1)",
                "setNamedVariable(44, \"USER:right\", 1)",
                "setNamedVariable(45, \"USER:bottom\", 1)",
                "textData(46, \"\")",
                "addDrawBitmap(99)",
            )
            .inOrder()
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

    @Test
    fun testOperationsAreBuffered() {
        val paint = RemotePaint()
        remoteCanvas.drawRect(1f.rf, 2f.rf, 3f.rf, 4f.rf, paint)

        assertThat(fakeBuffer.calls).isEmpty()

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls).containsExactly("addPaint", "addDrawRect(1.0, 2.0, 3.0, 4.0)")
    }

    @Test
    fun testExecuteOperations() {
        val paint = RemotePaint()
        remoteCanvas.drawRect(5f.rf, 6f.rf, 7f.rf, 8f.rf, paint)

        assertThat(fakeBuffer.calls).isEmpty()

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls).containsExactly("addPaint", "addDrawRect(5.0, 6.0, 7.0, 8.0)")

        fakeBuffer.calls.clear()
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls).isEmpty()
    }

    @Test
    fun testDrawConditionallyBuffered() {
        val condition = createNamedRemoteBoolean("cond", true)

        remoteCanvas.drawConditionally(condition) {
            remoteCanvas.drawRect(9f.rf, 10f.rf, 11f.rf, 12f.rf, RemotePaint())
        }

        assertThat(fakeBuffer.calls).isEmpty()

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:cond\", 4)",
                "addConditionalOperations(1, ID(42), 0.0)",
                "addPaint",
                "addDrawRect(9.0, 10.0, 11.0, 12.0)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testDependencyHoisted() {
        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)
        val sub = x + y // Common subexpression

        remoteCanvas.drawConditionally(RemoteBoolean(true)) {
            remoteCanvas.drawRect(sub, 13f.rf, 14f.rf, 15f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(RemoteBoolean(false)) {
            remoteCanvas.drawRect(sub, 20f.rf, 16f.rf, 17f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "setNamedVariable(43, \"USER:y\", 1)",
                "addAnimatedFloat(44) = ([42] [43] + )",
                "addPaint",
                "addDrawRect(ID(44), 13.0, 14.0, 15.0)",
            )
    }

    @Test
    fun testCommonSubexpressionElimination_Float_InTree() {
        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)
        val sub = x + y // Common subexpression

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", false)

        remoteCanvas.drawConditionally(condition1) {
            remoteCanvas.drawConditionally(condition2) {
                remoteCanvas.drawRect(sub, 18f.rf, 19f.rf, 20f.rf, RemotePaint())
            }
        }

        remoteCanvas.drawConditionally(condition2) {
            remoteCanvas.drawRect(sub, 21f.rf, 22f.rf, 23f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        val subId = creationState.remoteVariableToId.getOrDefault(sub.cacheKey, -1)
        assertThat(subId).isNotEqualTo(-1)

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "setNamedVariable(43, \"USER:y\", 1)",
                "addAnimatedFloat(44) = ([42] [43] + )",
                "setNamedVariable(45, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(45), 0.0)",
                "setNamedVariable(46, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(46), 0.0)",
                "addPaint",
                "addDrawRect(ID(44), 18.0, 19.0, 20.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "endConditionalOperations",
                "addContainerEnd",
                "addConditionalOperations(1, ID(46), 0.0)",
                "addPaint",
                "addDrawRect(ID(44), 21.0, 22.0, 23.0)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testCustomComponent_hoistsExpression() {
        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)
        val sub = x + y // Common subexpression

        // Used both in custom component and in another canvas operation
        remoteCanvas.custom("testConfig") {
            property(0, sub)
        }
        remoteCanvas.drawRect(sub, 1f.rf, 2f.rf, 3f.rf, RemotePaint())

        remoteCanvas.flush()

        val subId = creationState.remoteVariableToId.getOrDefault(sub.cacheKey, -1)
        assertThat(subId).isNotEqualTo(-1)

        // Hoisted animated float must be emitted before custom component content start
        val animatedFloatIndex =
            fakeBuffer.calls.indexOfFirst { it.startsWith("addAnimatedFloat($subId)") }
        val contentStartIndex = fakeBuffer.calls.indexOf("addContentStart")
        val containerEndIndex = fakeBuffer.calls.indexOf("addContainerEnd")

        assertThat(animatedFloatIndex).isNotEqualTo(-1)
        assertThat(contentStartIndex).isNotEqualTo(-1)
        assertThat(containerEndIndex).isNotEqualTo(-1)
        assertThat(animatedFloatIndex).isLessThan(contentStartIndex)
        assertThat(contentStartIndex).isLessThan(containerEndIndex)
    }

    @Test
    fun testCustomComponent_withMultipleStateTypes() {
        val floatState = 5f.rf
        val intState = 42.ri
        val stringState = "hello".rs
        val dpState = 16.rdp
        val colorState = Color.Red.rc
        val boolState = RemoteBoolean(true)

        remoteCanvas.custom("multiTypeConfig") {
            property(0, floatState)
            property(1, intState)
            property(2, stringState)
            property(3, dpState)
            property(4, colorState)
            property(5, boolState)
        }

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls).contains("addContentStart")
        assertThat(fakeBuffer.calls).contains("addContainerEnd")
    }

    @Test
    fun testCustomComponent_withMutableStateTypes_asReturns() {
        val floatState = MutableRemoteFloat(5f)
        val intState = MutableRemoteInt(42)
        val stringState = MutableRemoteString("hello")
        val boolState = MutableRemoteBoolean(true)

        remoteCanvas.custom("mutableReturnConfig") {
            bindReturn(0, floatState)
            bindReturn(1, intState)
            bindReturn(2, stringState)
            bindReturn(3, boolState)
        }

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls).contains("addContentStart")
        assertThat(fakeBuffer.calls).contains("addContainerEnd")
    }

    @Test
    fun testCustomComponent_booleanReturn_playerExecution() {
        val boolState = MutableRemoteBoolean(false)
        val expectedBoolId = boolState.getIdForCreationState(creationState)

        remoteCanvas.custom("testBooleanReturn") {
            bindReturn(1, boolState)
        }
        remoteCanvas.flush()

        val coreDoc =
            CoreDocument().apply {
                fakeBuffer.buffer.index = 0
                initFromBuffer(fakeBuffer)
            }

        var configuredVarId = -1
        val remoteContext =
            AndroidRemoteContext().apply {
                useCanvas(Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)))
            }
        val customSupport =
            object : AndroidCustomContext {
                override fun setRemoteContext(remoteContext: RemoteContext?) {}

                override fun setContext(context: Context) {}

                override fun setCanvas(canvas: Canvas?) {}

                override fun createCustom(id: Int, config: String) {}

                override fun configureCustom(id: Int, type: Int, value: String) {}

                override fun configureCustom(id: Int, type: Int, value: Int) {
                    if (type == 1) {
                        configuredVarId = value
                        remoteContext.overrideInteger(value, 12345)
                    }
                }

                override fun configureCustom(id: Int, type: Int, value: Float) {}

                override fun measureCustom(id: Int, bounds: FloatArray) {}

                override fun layoutCustom(id: Int, bounds: FloatArray) {}

                override fun touchCustom(id: Int, type: Int, x: Float, y: Float): Boolean = false

                override fun drawCustom(id: Int) {}
            }
        remoteContext.paintContext?.setCustomSupport(customSupport)

        // Before paint, the boolean variable has its initial value 0 (false)
        coreDoc.initializeContext(remoteContext)
        assertThat(remoteContext.getInteger(expectedBoolId)).isEqualTo(0)

        // Painting the document executes the custom component which updates the variable to 12345
        coreDoc.paint(remoteContext, 0)

        assertThat(configuredVarId).isEqualTo(expectedBoolId)
        assertThat(remoteContext.getInteger(expectedBoolId)).isEqualTo(12345)
    }

    @Test
    fun testCustomComponent_withMutableStateTypes_asInputs() {
        val floatState = MutableRemoteFloat(5f)
        val intState = MutableRemoteInt(42)
        val stringState = MutableRemoteString("hello")
        val boolState = MutableRemoteBoolean(true)

        remoteCanvas.custom("mutableInputConfig") {
            property(0, floatState)
            property(1, intState)
            property(2, stringState)
            property(3, boolState)
        }

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls).contains("addContentStart")
        assertThat(fakeBuffer.calls).contains("addContainerEnd")
    }

    @Test
    fun testRemoteCustomPropertiesScope_generatesExpectedCustomProperties() {
        val scope =
            RemoteCustomPropertiesScope().apply {
                property(0, 42)
                property(1, Color.Red)
                property(2, 3.14f)
                property(3, "test")
                property(4, true)
                property(5, 10f.rf)
                property(6, 16.rdp)
                property(7, "remoteString".rs)
                property(8, 99.ri)
                property(9, Color.Blue.rc)
                property(10, RemoteBoolean(false))
                bindReturn(11, MutableRemoteFloat(1f))
                bindReturn(12, MutableRemoteString("ret"))
                bindReturn(13, MutableRemoteInt(7))
                bindReturn(14, MutableRemoteBoolean(true))
                bindReturn(15, Color.Green.rc)
            }

        val properties = scope.entries.map { it.toCustomProperty(creationState) }
        assertThat(properties[0].mDataType).isEqualTo(CustomProperty.INT_PROP)
        assertThat(properties[1].mDataType).isEqualTo(CustomProperty.COLOR_PROP)
        assertThat(properties[2].mDataType).isEqualTo(CustomProperty.FLOAT_PROP)
        assertThat(properties[3].mDataType).isEqualTo(CustomProperty.STRING_PROP)
        assertThat(properties[4].mDataType).isEqualTo(CustomProperty.INT_PROP)
        assertThat(properties[5].mDataType).isEqualTo(CustomProperty.FLOAT_PROP)
        assertThat(properties[6].mDataType).isEqualTo(CustomProperty.FLOAT_PROP)
        assertThat(properties[7].mDataType).isEqualTo(CustomProperty.STRING_PROP)
        assertThat(properties[8].mDataType).isEqualTo(CustomProperty.INT_PROP)
        assertThat(properties[9].mDataType).isEqualTo(CustomProperty.COLOR_PROP)
        assertThat(properties[10].mDataType).isEqualTo(CustomProperty.INT_PROP)
        assertThat(properties[11].mDataType).isEqualTo(CustomProperty.FLOAT_RETURN)
        assertThat(properties[12].mDataType).isEqualTo(CustomProperty.TEXT_RETURN)
        assertThat(properties[13].mDataType).isEqualTo(CustomProperty.INT_RETURN)
        assertThat(properties[14].mDataType).isEqualTo(CustomProperty.INT_RETURN)
        assertThat(properties[15].mDataType).isEqualTo(CustomProperty.COLOR_RETURN)
    }

    @Test
    fun testCommonSubexpressionElimination_Int_InTree() {
        val x = createNamedRemoteInt("x", 10)
        val y = createNamedRemoteInt("y", 20)
        val sub = x + y // Common subexpression

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", false)

        remoteCanvas.drawConditionally(condition1) {
            remoteCanvas.drawRect(sub.toRemoteFloat(), 24f.rf, 25f.rf, 26f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            remoteCanvas.drawRect(sub.toRemoteFloat(), 27f.rf, 28f.rf, 29f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        val subId = creationState.remoteVariableToId.getOrDefault(sub.cacheKey, -1)
        assertThat(subId).isNotEqualTo(-1)

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 4)",
                "setNamedVariable(43, \"USER:y\", 4)",
                "addIntegerExpression(44, 7, [42, 43, 65537])",
                "setNamedVariable(45, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(45), 0.0)",
                "addPaint",
                "addDrawRect(ID(44), 24.0, 25.0, 26.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(46, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(46), 0.0)",
                "addPaint",
                "addDrawRect(ID(44), 27.0, 28.0, 29.0)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testCommonSubexpressionElimination_Select_InTree() {
        val x = createNamedRemoteFloat("x", 10f)
        val cond = x.isLessThan(RemoteFloat(5f))
        val sub = cond.select(RemoteFloat(100f), RemoteFloat(200f))

        val condition1 = createNamedRemoteBoolean("cond1", true)

        remoteCanvas.drawConditionally(condition1) {
            remoteCanvas.drawRect(sub, 30f.rf, 31f.rf, 32f.rf, RemotePaint())
        }

        remoteCanvas.drawRect(sub, 33f.rf, 34f.rf, 35f.rf, RemotePaint()) // Top level usage

        remoteCanvas.flush()

        val subId = creationState.remoteVariableToId.getOrDefault(sub.cacheKey, -1)
        assertThat(subId).isNotEqualTo(-1)

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "addAnimatedFloat(43) = (200.0 100.0 5.0 [42] - ifElse )",
                "setNamedVariable(44, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(44), 0.0)",
                "addPaint",
                "addDrawRect(ID(43), 30.0, 31.0, 32.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "addPaint",
                "addDrawRect(ID(43), 33.0, 34.0, 35.0)",
            )
    }

    @Test
    fun testCommonSubexpressionElimination_LongExpression_InTree() {
        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)
        val z = createNamedRemoteFloat("z", 30f)

        val sub = (x * y) + (y * z) - (x * z)

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", false)

        remoteCanvas.drawConditionally(condition1) {
            remoteCanvas.drawRect(sub, 36f.rf, 37f.rf, 38f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            remoteCanvas.drawRect(sub, 39f.rf, 40f.rf, 41f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        val subId = creationState.remoteVariableToId.getOrDefault(sub.cacheKey, -1)
        assertThat(subId).isNotEqualTo(-1)

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "setNamedVariable(43, \"USER:y\", 1)",
                "setNamedVariable(44, \"USER:z\", 1)",
                "addAnimatedFloat(45) = ([42] [43] * [43] [44] * + [42] [44] * - )",
                "setNamedVariable(46, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(46), 0.0)",
                "addPaint",
                "addDrawRect(ID(45), 36.0, 37.0, 38.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(47, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(47), 0.0)",
                "addPaint",
                "addDrawRect(ID(45), 39.0, 40.0, 41.0)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testCommonSubexpressionElimination_MultiLevel_InTree() {
        val a = createNamedRemoteFloat("a", 1f)
        val b = createNamedRemoteFloat("b", 2f)
        val c = createNamedRemoteFloat("c", 3f)

        val sub1 = a + b
        val sub2 = a + c
        val sub3 = sub1 * sub2

        val root1 = sub1
        val root2 = sub2
        val root3 = sub3 + RemoteFloat(1f)
        val root4 = sub3 + RemoteFloat(2f)
        val root5 = sub1 * RemoteFloat(3f)

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", false)

        remoteCanvas.drawConditionally(condition1) {
            remoteCanvas.drawRect(root3, 42f.rf, 43f.rf, 44f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            remoteCanvas.drawRect(root4, 45f.rf, 46f.rf, 47f.rf, RemotePaint())
        }

        remoteCanvas.drawRect(root1, 48f.rf, 49f.rf, 50f.rf, RemotePaint())
        remoteCanvas.drawRect(root2, 51f.rf, 52f.rf, 53f.rf, RemotePaint())
        remoteCanvas.drawRect(root5, 54f.rf, 55f.rf, 56f.rf, RemotePaint())

        remoteCanvas.flush()

        val id1 = sub1.getIdForCreationState(creationState)
        val id2 = sub2.getIdForCreationState(creationState)

        assertThat(id1).isNotEqualTo(-1)
        assertThat(id2).isNotEqualTo(-1)

        val sub1Id = Utils.asNan(id1)

        val array5 = root5.arrayForCreationState(creationState)

        // root5 should contain sub1Id
        assertThat(array5.toList()).contains(sub1Id)

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:a\", 1)",
                "setNamedVariable(43, \"USER:b\", 1)",
                "addAnimatedFloat(44) = ([42] [43] + )",
                "setNamedVariable(45, \"USER:c\", 1)",
                "addAnimatedFloat(46) = ([42] [45] + )",
                "addAnimatedFloat(47) = ([44] [46] * )",
                "setNamedVariable(48, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(48), 0.0)",
                "addPaint",
                "addAnimatedFloat(49) = ([47] 1.0 + )",
                "addDrawRect(ID(49), 42.0, 43.0, 44.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(50, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(50), 0.0)",
                "addPaint",
                "addAnimatedFloat(51) = ([47] 2.0 + )",
                "addDrawRect(ID(51), 45.0, 46.0, 47.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "addPaint",
                "addDrawRect(ID(44), 48.0, 49.0, 50.0)",
                "addDrawRect(ID(46), 51.0, 52.0, 53.0)",
                "addAnimatedFloat(52) = ([44] 3.0 * )",
                "addDrawRect(ID(52), 54.0, 55.0, 56.0)",
            )
    }

    @Test
    fun testSimpleCSEHoisting() {
        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)
        val sub = x + y // Common subexpression
        val sub2 = x + y // Common subexpression

        remoteCanvas.drawRect(sub, 57f.rf, 58f.rf, 59f.rf, RemotePaint())
        remoteCanvas.drawRect(sub2, 60f.rf, 61f.rf, 62f.rf, RemotePaint())

        remoteCanvas.flush()

        val subId = creationState.remoteVariableToId.getOrDefault(sub.cacheKey, -1)
        assertThat(subId).isNotEqualTo(-1)

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "setNamedVariable(43, \"USER:y\", 1)",
                "addAnimatedFloat(44) = ([42] [43] + )",
                "addPaint",
                "addDrawRect(ID(44), 57.0, 58.0, 59.0)",
                "addDrawRect(ID(44), 60.0, 61.0, 62.0)",
            )
    }

    @Test
    fun testLoopBuffered() {
        val from = 0f.rf
        val until = 10f.rf
        val step = 1f.rf

        remoteCanvas.loop(from, until, step) { index ->
            remoteCanvas.drawRect(index, 63f.rf, 64f.rf, 65f.rf, RemotePaint())
        }

        assertThat(fakeBuffer.calls).isEmpty()

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addLoopStart(42, 0.0, 1.0, 10.0)",
                "addPaint",
                "addDrawRect(ID(42), 63.0, 64.0, 65.0)",
                "addLoopEnd",
            )
    }

    @Test
    fun testLoopOptimized() {
        runWithOptimizingCanvas { canvas, optimizingBuffer ->
            val from = 0f.rf
            val until = 10f.rf
            val step = 1f.rf
            canvas.loop(from, until, step) { index ->
                canvas.drawRect(index, 63f.rf, 64f.rf, 65f.rf, RemotePaint())
            }

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(optimizingBuffer.calls)
                .containsExactly(
                    "addLoopStart(42, 0.0, 1.0, 10.0)",
                    "addPaint",
                    "addDrawRect(ID(42), 63.0, 64.0, 65.0)",
                    "addLoopEnd",
                )
        }
    }

    @Test
    fun testDrawToOffscreenBitmapBuffered() {
        val dummyBitmap =
            object :
                RemoteImageBitmap(
                    null,
                    RemoteOperationCacheKey(DummyEnum.VALUE, emptyList<RemoteStateCacheKey>()),
                ) {

                override fun writeToDocument(creationState: RemoteComposeCreationState): Int = 456

                override fun getIdForCreationState(creationState: RemoteComposeCreationState): Int =
                    456
            }

        remoteCanvas.drawToOffscreenBitmap(dummyBitmap) {
            remoteCanvas.drawRect(66f.rf, 67f.rf, 68f.rf, 69f.rf, RemotePaint())
        }

        assertThat(fakeBuffer.calls).isEmpty()

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "drawOnBitmap(456, 1, 0)",
                "addPaint",
                "addDrawRect(66.0, 67.0, 68.0, 69.0)",
                "drawOnBitmap(0, 1, 0)",
            )
    }

    @Test
    fun testDrawToOffscreenBitmap_Nested_StateImbalance() {
        val outerBitmap =
            object :
                RemoteImageBitmap(
                    null,
                    RemoteOperationCacheKey(DummyEnum.VALUE, emptyList<RemoteStateCacheKey>()),
                ) {

                override fun writeToDocument(creationState: RemoteComposeCreationState): Int = 100

                override fun getIdForCreationState(creationState: RemoteComposeCreationState): Int =
                    100
            }

        val innerBitmap =
            object :
                RemoteImageBitmap(
                    null,
                    RemoteOperationCacheKey(DummyEnum.VALUE, emptyList<RemoteStateCacheKey>()),
                ) {

                override fun writeToDocument(creationState: RemoteComposeCreationState): Int = 200

                override fun getIdForCreationState(creationState: RemoteComposeCreationState): Int =
                    200
            }

        remoteCanvas.drawToOffscreenBitmap(outerBitmap, android.graphics.Color.TRANSPARENT) {
            remoteCanvas.save()
            remoteCanvas.translate(1f.rf, 1f.rf)

            remoteCanvas.drawToOffscreenBitmap(innerBitmap, android.graphics.Color.TRANSPARENT) {
                remoteCanvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            }

            remoteCanvas.restore()
            remoteCanvas.drawRect(0f.rf, 0f.rf, 1f.rf, 1f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "drawOnBitmap(100, 0, 0)",
                "addMatrixSave",
                "addMatrixTranslate(1.0, 1.0)",
                "drawOnBitmap(200, 0, 0)",
                "addPaint",
                "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                "drawOnBitmap(100, 1, 0)",
                "addMatrixRestore",
                "addPaint",
                "addDrawRect(0.0, 0.0, 1.0, 1.0)",
                "drawOnBitmap(0, 1, 0)",
            )
    }

    @Test
    fun testDrawToOffscreenBitmap_OuterSaveRestorePreserved() {
        runWithOptimizingCanvas { canvas, buffer ->
            val offscreenBitmap = createOffscreenRemoteBitmap(100, 100)

            // 1. Outer canvas pushes 2 matrix transforms/saves
            canvas.save()
            canvas.translate(10f.rf, 10f.rf)
            canvas.save()
            canvas.scale(2f.rf, 2f.rf)
            assertThat(canvas.saveCount).isEqualTo(2)

            // 2. Draw to offscreen bitmap (creates a childSpan)
            canvas.drawToOffscreenBitmap(offscreenBitmap, android.graphics.Color.TRANSPARENT) {
                // Simulate offscreen drawing that pushes/pops canvas transforms
                canvas.save()
                canvas.translate(5f.rf, 5f.rf)
                canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
                canvas.restore()
            }

            // 3. Back in outer canvas, pop both outer saves
            canvas.restore()
            canvas.restore()
            assertThat(canvas.saveCount).isEqualTo(0)

            // 4. Flush operations to document
            canvas.flush()
            canvas.document.encodeToByteArray()

            // 5. Regression verification: verify that both outer matrixRestore operations are
            // preserved after returning from drawToOffscreenBitmap and are not inlined or absorbed
            // across canvas spans or shared saveCounter mismatches.
            val calls = buffer.calls
            val returnToMainCanvasIndex = calls.indexOf("drawOnBitmap(0, 1, 0)")
            val outerRestoresAfterOffscreen =
                calls.subList(returnToMainCanvasIndex + 1, calls.size).count {
                    it == "addMatrixRestore"
                }

            assertThat(outerRestoresAfterOffscreen).isEqualTo(2)
        }
    }

    @Test
    fun testEliminateRedundantSavesAndRestoresAcrossOffscreenBitmaps() {
        runWithOptimizingCanvas { canvas, buffer ->
            val paint = RemotePaint()

            // 1. Push multiple saves where only one actually translates
            canvas.save() // Identity save 1 (should be pruned/collapsed)
            canvas.save() // Identity save 2 (should be pruned/collapsed)
            canvas.save()
            canvas.translate(100f.rf, 100f.rf)
            canvas.save() // Identity save 3 (should be pruned/collapsed)

            val offscreenBitmap = createOffscreenRemoteBitmap(200, 200)

            // 2. Draw into offscreen bitmap (creates childSpan)
            canvas.drawToOffscreenBitmap(offscreenBitmap, android.graphics.Color.TRANSPARENT) {
                // Child span re-applies parent state + new local transform
                canvas.save()
                canvas.save()
                canvas.save()
                canvas.translate(100f.rf, 100f.rf)
                canvas.save()
                canvas.save()
                canvas.translate(4f.rf, 4f.rf)
                canvas.drawLine(10f.rf, 10f.rf, 20f.rf, 20f.rf, paint)
                canvas.restore()
                canvas.restore()
                canvas.restore()
                canvas.restore()
                canvas.restore()
            }

            // 3. Temporarily pop all transforms on main canvas to draw offscreenBitmap in clean
            // screen space
            canvas.restore()
            canvas.restore()
            canvas.restore()
            canvas.restore()
            canvas.drawBitmap(offscreenBitmap, 0f.rf, 0f.rf, paint)

            // 4. Reinstate parent transforms and draw
            canvas.save()
            canvas.save()
            canvas.save()
            canvas.translate(100f.rf, 100f.rf)
            canvas.save()
            canvas.drawLine(10f.rf, 10f.rf, 100f.rf, 100f.rf, paint)
            canvas.restore()
            canvas.restore()
            canvas.restore()
            canvas.restore()

            canvas.flush()

            val bitmapId = offscreenBitmap.getIdForCreationState(canvas.creationState)
            val expectedOptimizedOps =
                listOf(
                    "addMatrixSave",
                    "addMatrixTranslate(100.0, 100.0)",
                    // Inside offscreen bitmap span:
                    "drawOnBitmap($bitmapId, 0, 0)",
                    "addMatrixSave",
                    "addMatrixTranslate(100.0, 100.0)",
                    "addMatrixSave",
                    "addMatrixTranslate(4.0, 4.0)",
                    "addPaint",
                    "addDrawLine(10.0, 10.0, 20.0, 20.0)",
                    "addMatrixRestore",
                    "addMatrixRestore",
                    // Back on main canvas:
                    "drawOnBitmap(0, 1, 0)",
                    "addMatrixRestore", // Pops the initial (100, 100) translation cleanly
                    "addPaint",
                    "textData(43, \"\")",
                    "addDrawBitmap($bitmapId)",
                    "addMatrixTranslate(100.0, 100.0)",
                    "addDrawLine(10.0, 10.0, 100.0, 100.0)",
                )

            assertThat(buffer.calls).isEqualTo(expectedOptimizedOps)
        }
    }

    @Test
    fun testNestedDrawToOffscreenBitmap_preservesOrderAndScopeDependencies() {
        runWithOptimizingCanvas { canvas, buffer ->
            val remoteBitmap42 = createOffscreenRemoteBitmap(450, 450)
            val remoteBitmap43 = createOffscreenRemoteBitmap(450, 450)
            val sourceBitmap = RemoteImageBitmap.createForId(22)

            canvas.save()
            canvas.translate(21f.rf, 175f.rf)

            canvas.drawToOffscreenBitmap(remoteBitmap42, android.graphics.Color.TRANSPARENT) {
                canvas.save()
                canvas.translate((-12f).rf, (-11f).rf)
                canvas.drawBitmap(sourceBitmap, 0f.rf, 0f.rf, RemotePaint())
                canvas.restore()

                canvas.drawToOffscreenBitmap(remoteBitmap43, android.graphics.Color.TRANSPARENT) {
                    canvas.save()
                    canvas.translate((-17f).rf, 0f.rf)
                    canvas.drawRect(0f.rf, 0f.rf, 190f.rf, 190f.rf, RemotePaint())
                    canvas.restore()
                }

                val maskPaint = RemotePaint { remoteBlendMode = RemoteBlendMode.DstIn }
                canvas.drawBitmap(remoteBitmap43, 0f.rf, 0f.rf, 450f.rf, 450f.rf, maskPaint)
            }

            canvas.drawBitmap(remoteBitmap42, 0f.rf, 0f.rf, 450f.rf, 450f.rf, RemotePaint())
            canvas.restore()

            canvas.flush()
            canvas.document.encodeToByteArray()

            val bitmap42Id = remoteBitmap42.getIdForCreationState(canvas.creationState)
            val bitmap43Id = remoteBitmap43.getIdForCreationState(canvas.creationState)
            val sourceId = sourceBitmap.getIdForCreationState(canvas.creationState)

            assertThat(buffer.calls)
                .containsExactly(
                    "addMatrixSave",
                    "addMatrixTranslate(21.0, 175.0)",
                    "drawOnBitmap($bitmap42Id, 0, 0)",
                    "addMatrixSave",
                    "addMatrixTranslate(-12.0, -11.0)",
                    "addPaint",
                    "textData(44, \"\")",
                    "addDrawBitmap($sourceId)",
                    "addMatrixRestore",
                    "drawOnBitmap($bitmap43Id, 0, 0)",
                    "addMatrixSave",
                    "addMatrixTranslate(-17.0, 0.0)",
                    "addPaint",
                    "addDrawRect(0.0, 0.0, 190.0, 190.0)",
                    "addMatrixRestore",
                    "drawOnBitmap($bitmap42Id, 1, 0)",
                    "addPaint",
                    "addDrawBitmap($bitmap43Id)",
                    "drawOnBitmap(0, 1, 0)",
                    "addPaint",
                    "addDrawBitmap($bitmap42Id)",
                    "addMatrixRestore",
                )
                .inOrder()
        }
    }

    @Test
    fun testRemoteStringLengthHoisted_InTree() {
        val str = createNamedRemoteString("str", "hello")
        val length = str.length

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", false)

        remoteCanvas.drawConditionally(condition1) {
            remoteCanvas.drawRect(length.toRemoteFloat(), 70f.rf, 71f.rf, 72f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            remoteCanvas.drawRect(length.toRemoteFloat(), 73f.rf, 74f.rf, 75f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        val lengthId = creationState.remoteVariableToId.getOrDefault(length.cacheKey, -1)
        assertThat(lengthId).isNotEqualTo(-1)

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:str\", 0)",
                "textLength(43)",
                "setNamedVariable(44, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(44), 0.0)",
                "addPaint",
                "addDrawRect(ID(43), 70.0, 71.0, 72.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(45, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(45), 0.0)",
                "addPaint",
                "addDrawRect(ID(43), 73.0, 74.0, 75.0)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testLengthDynamicString_InTree() {
        val str = createNamedRemoteString("a", "12345") + RemoteString("678")
        val length = str.length

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", false)

        remoteCanvas.drawConditionally(condition1) {
            remoteCanvas.drawRect(length.toRemoteFloat(), 76f.rf, 77f.rf, 78f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            remoteCanvas.drawRect(length.toRemoteFloat(), 79f.rf, 80f.rf, 81f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        val lengthId = creationState.remoteVariableToId.getOrDefault(length.cacheKey, -1)
        assertThat(lengthId).isNotEqualTo(-1)

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:a\", 0)",
                "textData(43, \"678\")",
                "textMerge(44, 42, 43)",
                "textLength(45)",
                "setNamedVariable(46, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(46), 0.0)",
                "addPaint",
                "addDrawRect(ID(45), 76.0, 77.0, 78.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(47, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(47), 0.0)",
                "addPaint",
                "addDrawRect(ID(45), 79.0, 80.0, 81.0)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testRemoteStringExpressionHoisted_InTree() {
        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", false)

        remoteCanvas.drawConditionally(condition1) {
            val str = createNamedRemoteString("a", "123") + RemoteString("456")
            remoteCanvas.drawText(str, 0f.rf, 0f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            val str = createNamedRemoteString("a", "123") + RemoteString("456")
            remoteCanvas.drawText(str, 20f.rf, 20f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        // Verify that str (textMerge) is hoisted to Root!
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:a\", 0)",
                "textData(43, \"456\")",
                "textMerge(44, 42, 43)",
                "setNamedVariable(45, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(45), 0.0)",
                "addPaint",
                "addDrawTextRun(44)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(46, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(46), 0.0)",
                "addPaint",
                "addDrawTextRun(44)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testSpanTreeStructure() {
        remoteCanvas.drawConditionally(RemoteBoolean(true)) {
            remoteCanvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
        }
        remoteCanvas.drawConditionally(RemoteBoolean(false)) {
            remoteCanvas.drawRect(82f.rf, 83f.rf, 84f.rf, 85f.rf, RemotePaint())
        }

        val mSpanTreeRoot = remoteCanvas.buffer.spanTreeRoot

        val child1 = mSpanTreeRoot.child
        assertThat(child1).isNotNull()

        val child2 = child1!!.next
        assertThat(child2).isNotNull()
    }

    @Test
    fun testIterationSafetyInDiscoverIdealSpans() {
        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)
        val depD = x + y // Dependency D
        val cseB = depD * 2f // Expression B (depends on D)

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", true)

        // Child Span C (via condition1)
        remoteCanvas.drawConditionally(condition1) {
            // Record cseB in child span C
            remoteCanvas.drawRect(cseB, 86f.rf, 87f.rf, 88f.rf, RemotePaint())
        }

        // Parent Span (Root)
        // Record operation using cseB at root (or in another branch that hoists it to root)
        remoteCanvas.drawConditionally(condition2) {
            remoteCanvas.drawRect(cseB, 89f.rf, 90f.rf, 91f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        // Verify that both cseB and depD are hoisted to Root!
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "setNamedVariable(43, \"USER:y\", 1)",
                "addAnimatedFloat(44) = ([42] [43] + 2.0 * )",
                "setNamedVariable(45, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(45), 0.0)",
                "addPaint",
                "addDrawRect(ID(44), 86.0, 87.0, 88.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(46, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(46), 0.0)",
                "addPaint",
                "addDrawRect(ID(44), 89.0, 90.0, 91.0)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testExecutionOrderWithNestedCSE() {
        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)
        val cseB = x + y // CSE_B
        val cseA = cseB * 2f // CSE_A (depends on B)

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", true)

        // Use cseA in two branches to make it common
        remoteCanvas.drawConditionally(condition1) {
            remoteCanvas.drawRect(cseA, 92f.rf, 93f.rf, 94f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            remoteCanvas.drawRect(cseA, 95f.rf, 96f.rf, 97f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        // Verify that both CSE_A and CSE_B are hoisted to Root, and B is before A!
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "setNamedVariable(43, \"USER:y\", 1)",
                "addAnimatedFloat(44) = ([42] [43] + 2.0 * )",
                "setNamedVariable(45, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(45), 0.0)",
                "addPaint",
                "addDrawRect(ID(44), 92.0, 93.0, 94.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(46, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(46), 0.0)",
                "addPaint",
                "addDrawRect(ID(44), 95.0, 96.0, 97.0)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testExecutionOrderWithHoistedDependency() {
        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)
        val cseB = x + y // CSE_B
        val cseA = cseB * 2f // CSE_A (depends on B)

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", true)

        remoteCanvas.drawConditionally(condition1) {
            // Use cseB directly in child 1.
            remoteCanvas.drawRect(cseB, 0f.rf, 100f.rf, 100f.rf, RemotePaint())
            // And use cseA.
            remoteCanvas.drawRect(cseA, 10f.rf, 110f.rf, 120f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            // Use cseA in another branch to make it common.
            remoteCanvas.drawRect(cseA, 20f.rf, 130f.rf, 140f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        // Verify that both CSE_A and CSE_B are hoisted to Root and B is ordered before A.
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "setNamedVariable(43, \"USER:y\", 1)",
                "addAnimatedFloat(44) = ([42] [43] + )",
                "addAnimatedFloat(45) = ([44] 2.0 * )",
                "setNamedVariable(46, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(46), 0.0)",
                "addPaint",
                "addDrawRect(ID(44), 0.0, 100.0, 100.0)",
                "addDrawRect(ID(45), 10.0, 110.0, 120.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(47, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(47), 0.0)",
                "addPaint",
                "addDrawRect(ID(45), 20.0, 130.0, 140.0)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testCSE_HoistingWithNonCommonParent() {
        val x = createNamedRemoteFloat("x", 10f)
        val cseA = x + 1f // CSE_A (Common)
        val nonCommonB = cseA * 2f // Non-common parent (Used only once!)

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", true)

        remoteCanvas.drawConditionally(condition1) {
            // Use nonCommonB in branch 1.
            remoteCanvas.drawRect(nonCommonB, 0f.rf, 100f.rf, 100f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            // Use cseA directly in branch 2 to make it common!
            remoteCanvas.drawRect(cseA, 20f.rf, 130f.rf, 140f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        // If fixed, cseA should be hoisted to root!
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "addAnimatedFloat(43) = ([42] 1.0 + )",
                "setNamedVariable(44, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(44), 0.0)",
                "addPaint",
                "addAnimatedFloat(45) = ([43] 2.0 * )",
                "addDrawRect(ID(45), 0.0, 100.0, 100.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(46, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(46), 0.0)",
                "addPaint",
                "addDrawRect(ID(43), 20.0, 130.0, 140.0)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testTraverseCacheKey_InspectCounts() {
        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)
        val expr1 = x + y
        val expr2 = expr1 * 2f

        val counts = androidx.collection.MutableObjectIntMap<RemoteStateCacheKey>()
        val commonOps = mutableSetOf<RemoteOperationCacheKey>()
        val visited = mutableSetOf<RemoteStateCacheKey>()

        remoteCanvas.buffer.traverseCacheKey(expr2.cacheKey, counts, commonOps, visited)

        // Guard against double counting in CSE Pass 0.
        assertThat(counts.getOrDefault(expr1.cacheKey, 0)).isEqualTo(1)
        assertThat(counts.getOrDefault(x.cacheKey, 0)).isEqualTo(1)
        assertThat(counts.getOrDefault(y.cacheKey, 0)).isEqualTo(1)
        assertThat(counts.getOrDefault(expr2.cacheKey, 0)).isEqualTo(0)
    }

    @Test
    fun testRemoteFloatToRemoteStringHoisted() {
        val x = createNamedRemoteFloat("x", 10f)

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", true)

        remoteCanvas.drawConditionally(condition1) {
            val str = x.toRemoteString(java.text.DecimalFormat("#0"))
            remoteCanvas.drawText(str, 10f.rf, 10f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            val str = x.toRemoteString(java.text.DecimalFormat("#0"))
            remoteCanvas.drawText(str, 20f.rf, 20f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "createTextFromFloat(43, ID(42), 255, 0, 517)",
                "setNamedVariable(44, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(44), 0.0)",
                "addPaint",
                "addDrawTextRun(43)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(45, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(45), 0.0)",
                "addPaint",
                "addDrawTextRun(43)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testRemoteIntToRemoteStringHoisted() {
        val x = createNamedRemoteInt("x", 10)

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", true)

        remoteCanvas.drawConditionally(condition1) {
            val str = x.toRemoteString(java.text.DecimalFormat("#0"))
            remoteCanvas.drawText(str, 10f.rf, 10f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            val str = x.toRemoteString(java.text.DecimalFormat("#0"))
            remoteCanvas.drawText(str, 20f.rf, 20f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 4)",
                "createTextFromFloat(43, ID(42), 255, 0, 517)",
                "setNamedVariable(44, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(44), 0.0)",
                "addPaint",
                "addDrawTextRun(43)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(45, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(45), 0.0)",
                "addPaint",
                "addDrawTextRun(43)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testRemoteBooleanSelectWithRemoteStringsHoisted() {
        val cond = createNamedRemoteBoolean("cond", true)
        val str1 = createNamedRemoteString("s1", "hello")
        val str2 = createNamedRemoteString("s2", "world")

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", true)

        remoteCanvas.drawConditionally(condition1) {
            val selected = cond.select(str1, str2)
            remoteCanvas.drawText(selected, 10f.rf, 10f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            val selected = cond.select(str1, str2)
            remoteCanvas.drawText(selected, 20f.rf, 20f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:s2\", 0)",
                "setNamedVariable(43, \"USER:s1\", 0)",
                "addList(2097194, [42, 43])",
                "setNamedVariable(44, \"USER:cond\", 4)",
                "textLookup(45, ID(2097194), 44)",
                "setNamedVariable(46, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(46), 0.0)",
                "addPaint",
                "addDrawTextRun(45)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(47, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(47), 0.0)",
                "addPaint",
                "addDrawTextRun(45)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testRemoteStringSubstringHoisted() {
        val str = createNamedRemoteString("str", "hello world")

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", true)

        remoteCanvas.drawConditionally(condition1) {
            val sub = str.substring(6)
            remoteCanvas.drawText(sub, 10f.rf, 10f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            val sub = str.substring(6)
            remoteCanvas.drawText(sub, 20f.rf, 20f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:str\", 0)",
                "textSubtext(43, 42, 6.0, -1.0)",
                "setNamedVariable(44, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(44), 0.0)",
                "addPaint",
                "addDrawTextRun(43)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(45, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(45), 0.0)",
                "addPaint",
                "addDrawTextRun(43)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testRemoteStringUppercaseHoisted() {
        val str = createNamedRemoteString("str", "hello")

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", true)

        remoteCanvas.drawConditionally(condition1) {
            val upper = str.uppercase()
            remoteCanvas.drawText(upper, 10f.rf, 10f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            val upper = str.uppercase()
            remoteCanvas.drawText(upper, 20f.rf, 20f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:str\", 0)",
                "textTransform(43, 42, 0.0, -1.0, 2)",
                "setNamedVariable(44, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(44), 0.0)",
                "addPaint",
                "addDrawTextRun(43)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(45, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(45), 0.0)",
                "addPaint",
                "addDrawTextRun(43)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testRemoteStringTrimHoisted() {
        val str = createNamedRemoteString("str", " hello ")

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", true)

        remoteCanvas.drawConditionally(condition1) {
            val trimmed = str.trim()
            remoteCanvas.drawText(trimmed, 10f.rf, 10f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            val trimmed = str.trim()
            remoteCanvas.drawText(trimmed, 20f.rf, 20f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:str\", 0)",
                "textTransform(43, 42, 0.0, -1.0, 3)",
                "setNamedVariable(44, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(44), 0.0)",
                "addPaint",
                "addDrawTextRun(43)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(45, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(45), 0.0)",
                "addPaint",
                "addDrawTextRun(43)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testSelectIfLtHoisted() {
        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)
        val str1 = createNamedRemoteString("s1", "hello")
        val str2 = createNamedRemoteString("s2", "world")

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", true)

        remoteCanvas.drawConditionally(condition1) {
            val selected = selectIfLt(x, y, str1, str2)
            remoteCanvas.drawText(selected, 10f.rf, 10f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            val selected = selectIfLt(x, y, str1, str2)
            remoteCanvas.drawText(selected, 20f.rf, 20f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:s1\", 0)",
                "setNamedVariable(43, \"USER:s2\", 0)",
                "addList(2097194, [42, 43])",
                "setNamedVariable(44, \"USER:y\", 1)",
                "setNamedVariable(45, \"USER:x\", 1)",
                "addAnimatedFloat(46) = (1.0 0.0 [44] [45] - ifElse )",
                "textLookup(47, ID(2097194), 46)",
                "setNamedVariable(48, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(48), 0.0)",
                "addPaint",
                "addDrawTextRun(47)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(49, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(49), 0.0)",
                "addPaint",
                "addDrawTextRun(47)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testSelectIfGtHoisted() {
        val x = createNamedRemoteFloat("x", 10f)
        val y = createNamedRemoteFloat("y", 20f)
        val str1 = createNamedRemoteString("s1", "hello")
        val str2 = createNamedRemoteString("s2", "world")

        val condition1 = createNamedRemoteBoolean("cond1", true)
        val condition2 = createNamedRemoteBoolean("cond2", true)

        remoteCanvas.drawConditionally(condition1) {
            val selected = selectIfGt(x, y, str1, str2)
            remoteCanvas.drawText(selected, 10f.rf, 10f.rf, RemotePaint())
        }

        remoteCanvas.drawConditionally(condition2) {
            val selected = selectIfGt(x, y, str1, str2)
            remoteCanvas.drawText(selected, 20f.rf, 20f.rf, RemotePaint())
        }

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:s1\", 0)",
                "setNamedVariable(43, \"USER:s2\", 0)",
                "addList(2097194, [42, 43])",
                "setNamedVariable(44, \"USER:x\", 1)",
                "setNamedVariable(45, \"USER:y\", 1)",
                "addAnimatedFloat(46) = (1.0 0.0 [44] [45] - ifElse )",
                "textLookup(47, ID(2097194), 46)",
                "setNamedVariable(48, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(48), 0.0)",
                "addPaint",
                "addDrawTextRun(47)",
                "endConditionalOperations",
                "addContainerEnd",
                "setNamedVariable(49, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(49), 0.0)",
                "addPaint",
                "addDrawTextRun(47)",
                "endConditionalOperations",
                "addContainerEnd",
            )
    }

    @Test
    fun testOrderingChain_ClipRect() {
        remoteCanvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
        remoteCanvas.clipRect(0f.rf, 0f.rf, 5f.rf, 5f.rf)
        remoteCanvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addPaint",
                "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                "addClipRect(0.0, 0.0, 5.0, 5.0)",
                "addDrawRect(0.0, 0.0, 10.0, 10.0)",
            )
    }

    @Test
    fun testFindCommonAncestor() {
        val root = CanvasOperationBuffer.Span(null, 0)
        val child1 = CanvasOperationBuffer.Span(root, 1)
        val child2 = CanvasOperationBuffer.Span(root, 1)
        val grandChild1 = CanvasOperationBuffer.Span(child1, 2)

        assertThat(CanvasOperationBuffer.findCommonAncestor(child1, child2)).isEqualTo(root)
        assertThat(CanvasOperationBuffer.findCommonAncestor(grandChild1, child2)).isEqualTo(root)
        assertThat(CanvasOperationBuffer.findCommonAncestor(grandChild1, child1)).isEqualTo(child1)
        assertThat(CanvasOperationBuffer.findCommonAncestor(child1, child1)).isEqualTo(child1)
    }

    private class TestRemoteComposeWriter(
        profile: Profile,
        buffer: RemoteComposeBuffer,
        vararg tags: RemoteComposeWriter.HTag,
    ) : RemoteComposeWriterAndroid(profile, buffer, *tags)

    private fun runWithOptimizingCanvas(
        action: (RemoteCanvas, OptimizingTestRemoteComposeBuffer) -> Unit
    ) {
        val platform = AndroidxRcPlatformServices()
        val optimizingBuffer = OptimizingTestRemoteComposeBuffer()
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                _ ->
                TestRemoteComposeWriter(
                    profile,
                    optimizingBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, profile.operationsProfiles),
                    RemoteComposeWriter.hTag(
                        Header.DOC_DENSITY_BEHAVIOR,
                        creationDisplayInfo.densityBehavior,
                    ),
                )
            }
        val localCreationState =
            RemoteComposeCreationState(RemoteCreationDisplayInfo(500, 500, 160, 1f), null, profile)
        val localCanvas =
            RemoteCanvas(
                localCreationState,
                enableOptimizations = true,
            )

        action(localCanvas, optimizingBuffer)
    }

    @Test
    fun testRedundantSaveRestoreElimination() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save()
            canvas.translate(10f.rf, 20f.rf)
            canvas.restore()

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls).isEmpty()
        }
    }

    @Test
    fun testTransformsOptimizedAndRecordedInChildSpans() {
        runWithOptimizingCanvas { canvas, buffer ->
            val condition = createNamedRemoteBoolean("cond", true)
            canvas.drawConditionally(condition) {
                canvas.save()
                canvas.translate(20f.rf, 20f.rf)
                canvas.drawRect(0f.rf, 0f.rf, 50f.rf, 50f.rf, RemotePaint())
                canvas.restore()
            }

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls).contains("addMatrixTranslate(20.0, 20.0)")
        }
    }

    @Test
    fun testTransformFusingInsidePreservedSaveScope() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save()
            canvas.translate(10f.rf, 10f.rf)
            canvas.translate(20f.rf, 20f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            canvas.restore()

            // Subsequent drawing forces the above save/restore scope to PRESERVE its boundaries.
            canvas.drawRect(10f.rf, 10f.rf, 20f.rf, 20f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            // Consecutive translations inside the preserved save block must fuse.
            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "addMatrixSave",
                    "addMatrixTranslate(30.0, 30.0)",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                    "addMatrixRestore",
                    "addDrawRect(10.0, 10.0, 20.0, 20.0)",
                )
        }
    }

    @Test
    fun testTransformCancellation() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.translate(10f.rf, 20f.rf)
            canvas.translate((-10f).rf, (-20f).rf)
            canvas.drawRect(0f.rf, 0f.rf, 50f.rf, 50f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly("addPaint", "addDrawRect(0.0, 0.0, 50.0, 50.0)")
        }
    }

    @Test
    fun testScaleCancellation() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.scale(2f.rf, 4f.rf)
            canvas.scale(0.5f.rf, 0.25f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 50f.rf, 50f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly("addPaint", "addDrawRect(0.0, 0.0, 50.0, 50.0)")
        }
    }

    @Test
    fun testRotateCancellation() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.rotate(45f.rf)
            canvas.rotate((-45f).rf)
            canvas.drawRect(0f.rf, 0f.rf, 50f.rf, 50f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly("addPaint", "addDrawRect(0.0, 0.0, 50.0, 50.0)")
        }
    }

    @Test
    fun testSkewCancellation() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.skew(0.5f.rf, 0f.rf)
            canvas.skew((-0.5f).rf, 0f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 50f.rf, 50f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly("addPaint", "addDrawRect(0.0, 0.0, 50.0, 50.0)")
        }
    }

    @Test
    fun testIntermediateTransformDependenciesAreReplaced() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.translate(10f.rf, 20f.rf)
            val t1SpanOp = canvas.buffer.lastRenderingOp!!
            canvas.translate(30f.rf, 40f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 50f.rf, 50f.rf, RemotePaint())

            // Simulate another operation in the span that directly depended on intermediate T1
            val extraOp = CanvasOperationBuffer.SpanOp(canvas.buffer.spanTreeRoot, CanvasOp.Clip {})
            extraOp.deps.add(t1SpanOp)
            canvas.buffer.spanTreeRoot.operations.add(extraOp)

            canvas.flush()
            canvas.document.encodeToByteArray()

            // If only pendingSpanOps.last() is replaced during fusing, T1 (10.0, 20.0) is not
            // replaced
            // in extraOp.deps and gets recreated alongside the fused (40.0, 60.0).
            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "addMatrixTranslate(40.0, 60.0)",
                    "addDrawRect(0.0, 0.0, 50.0, 50.0)",
                )
        }
    }

    @Test
    fun testTransformFusing() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.translate(10f.rf, 20f.rf)
            canvas.translate(30f.rf, 40f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "addMatrixTranslate(40.0, 60.0)",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                )
        }
    }

    @Test
    fun testMixedTransformCommutingAndFusing() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.translate(10f.rf, 20f.rf)
            canvas.scale(2f.rf, 3f.rf)
            canvas.translate(5f.rf, 10f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "addMatrixTranslate(20.0, 50.0)",
                    "addMatrixScale(2.0, 3.0, NaN, NaN)",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                )
        }
    }

    @Test
    fun testCommuteRotateAndTranslate() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.rotate(90f.rf)
            canvas.translate(10f.rf, 20f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "addMatrixTranslate(-20.0, 10.0)",
                    "addMatrixRotate(90.0, NaN, NaN)",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                )
        }
    }

    @Test
    fun testSaveRestorePointlessElided() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save()
            canvas.translate(10f.rf, 20f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            canvas.restore()

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "addMatrixTranslate(10.0, 20.0)",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                )
        }
    }

    @Test
    fun testSaveRestoreUsefulPreserved() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save()
            canvas.translate(10f.rf, 20f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            canvas.restore()
            canvas.drawRect(0f.rf, 0f.rf, 5f.rf, 5f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "addMatrixSave",
                    "addMatrixTranslate(10.0, 20.0)",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                    "addMatrixRestore",
                    "addDrawRect(0.0, 0.0, 5.0, 5.0)",
                )
        }
    }

    @Test
    fun testNestedSaveRestoreElision() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save()
            canvas.translate(10f.rf, 20f.rf)

            canvas.save()
            canvas.translate(30f.rf, 40f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            canvas.restore()

            canvas.restore()
            canvas.drawRect(0f.rf, 0f.rf, 5f.rf, 5f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "addMatrixSave",
                    "addMatrixTranslate(40.0, 60.0)",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                    "addMatrixRestore",
                    "addDrawRect(0.0, 0.0, 5.0, 5.0)",
                )
        }
    }

    @Test
    fun testNestedSaveWithInnerTransformAndOuterSubsequentDrawing() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save() // Save A (No direct transforms, but contains B)
            canvas.save() // Save B (Has transforms, Has draws)
            canvas.translate(10f.rf, 20f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            canvas.restore() // Restore B
            canvas.restore() // Restore A

            canvas.drawRect(0f.rf, 0f.rf, 5f.rf, 5f.rf, RemotePaint()) // Draw C

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "addMatrixSave",
                    "addMatrixTranslate(10.0, 20.0)",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                    "addMatrixRestore",
                    "addDrawRect(0.0, 0.0, 5.0, 5.0)",
                )
        }
    }

    @Test
    fun testRotateFusing() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.rotate(45f.rf)
            canvas.rotate(90f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "addMatrixRotate(135.0, NaN, NaN)",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                )
        }
    }

    @Test
    fun testClipElision() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save()
            canvas.clipRect(0f.rf, 0f.rf, 10f.rf, 10f.rf)
            canvas.restore()

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls).isEmpty()
        }
    }

    @Test
    fun testClipBarrier() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.translate(10f.rf, 20f.rf)
            canvas.clipRect(0f.rf, 0f.rf, 50f.rf, 50f.rf)
            canvas.translate(30f.rf, 40f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "addMatrixTranslate(10.0, 20.0)",
                    "addClipRect(0.0, 0.0, 50.0, 50.0)",
                    "addMatrixTranslate(30.0, 40.0)",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                )
        }
    }

    @Test
    fun testTranslateWithVariables() {
        runWithOptimizingCanvas { canvas, buffer ->
            val varA = createNamedRemoteFloat("varA", 0f)
            val varB = createNamedRemoteFloat("varB", 0f)

            canvas.translate(10f.rf, 20f.rf)
            canvas.translate(varA, varB)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "setNamedVariable(42, \"USER:varA\", 1)",
                    "addAnimatedFloat(43) = ([42] 10.0 + )",
                    "setNamedVariable(44, \"USER:varB\", 1)",
                    "addAnimatedFloat(45) = ([44] 20.0 + )",
                    "addMatrixTranslate(ID(43), ID(45))",
                    "addPaint",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                )
        }
    }

    @Test
    fun testRotateWithVariables() {
        runWithOptimizingCanvas { canvas, buffer ->
            val varAngle = createNamedRemoteFloat("varAngle", 0f)

            canvas.rotate(10f.rf)
            canvas.rotate(varAngle)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            // They should be fused into a single rotate.
            assertThat(buffer.calls)
                .containsExactly(
                    "setNamedVariable(42, \"USER:varAngle\", 1)",
                    "addAnimatedFloat(43) = ([42] 10.0 + )",
                    "addMatrixRotate(ID(43), NaN, NaN)",
                    "addPaint",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                )
        }
    }

    @Test
    fun testScaleWithVariables() {
        runWithOptimizingCanvas { canvas, buffer ->
            val varSx = createNamedRemoteFloat("varSx", 1f)
            val varSy = createNamedRemoteFloat("varSy", 1f)

            canvas.scale(2f.rf, 3f.rf)
            canvas.scale(varSx, varSy)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            // They should be fused into a single scale.
            assertThat(buffer.calls)
                .containsExactly(
                    "setNamedVariable(42, \"USER:varSx\", 1)",
                    "addAnimatedFloat(43) = ([42] 2.0 * )",
                    "setNamedVariable(44, \"USER:varSy\", 1)",
                    "addAnimatedFloat(45) = ([44] 3.0 * )",
                    "addMatrixScale(ID(43), ID(45), NaN, NaN)",
                    "addPaint",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                )
        }
    }

    @Test
    fun testDrawConditionally_elidedWhenNoChildCommands() {
        runWithOptimizingCanvas { canvas, buffer ->
            val condition = createNamedRemoteBoolean("cond", true)
            canvas.drawConditionally(condition) {
                // Empty block, no child commands
            }

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls).isEmpty()
        }
    }

    @Test
    fun testDrawConditionally_preservedWhenHasChildCommands() {
        runWithOptimizingCanvas { canvas, buffer ->
            val condition = createNamedRemoteBoolean("cond", true)
            canvas.drawConditionally(condition) {
                canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            }

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "setNamedVariable(42, \"USER:cond\", 4)",
                    "addConditionalOperations(1, ID(42), 0.0)",
                    "addPaint",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                    "endConditionalOperations",
                )
        }
    }

    @Test
    fun testDrawConditionally_withExpression_elidedWhenNoChildCommands() {
        runWithOptimizingCanvas { canvas, buffer ->
            val cond1 = createNamedRemoteBoolean("cond1", true)
            val cond2 = createNamedRemoteBoolean("cond2", true)
            val expr = cond1 and cond2

            canvas.drawConditionally(expr) {
                // Empty block, elided
            }
            canvas.drawConditionally(expr) {
                // Empty block, elided
            }

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls).isEmpty()
        }
    }

    @Test
    fun testDrawConditionally_withExpression_preservedWhenHasChildCommands() {
        runWithOptimizingCanvas { canvas, buffer ->
            val cond1 = createNamedRemoteBoolean("cond1", true)
            val cond2 = createNamedRemoteBoolean("cond2", true)
            val expr = cond1 and cond2

            canvas.drawConditionally(expr) {
                canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            }
            canvas.drawConditionally(expr) {
                canvas.drawRect(10f.rf, 10f.rf, 20f.rf, 20f.rf, RemotePaint())
            }

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "setNamedVariable(42, \"USER:cond1\", 4)",
                    "setNamedVariable(43, \"USER:cond2\", 4)",
                    "addIntegerExpression(44, 7, [42, 43, 65546])",
                    "addConditionalOperations(1, ID(44), 0.0)",
                    "addPaint",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                    "endConditionalOperations",
                    "addConditionalOperations(1, ID(44), 0.0)",
                    "addPaint",
                    "addDrawRect(10.0, 10.0, 20.0, 20.0)",
                    "endConditionalOperations",
                )
        }
    }

    @Test
    fun testDrawConditionally_withExpression_usedBeforeElidedConditional_preserved() {
        runWithOptimizingCanvas { canvas, buffer ->
            val cond1 = createNamedRemoteBoolean("cond1", true)
            val cond2 = createNamedRemoteBoolean("cond2", true)
            val expr = cond1 and cond2

            // First: convert expr to RemoteString and drawText (preserves expr)
            val str = expr.select(RemoteString("true"), RemoteString("false"))
            canvas.drawText(str, 10f.rf, 10f.rf, RemotePaint())

            // Second: empty conditional block using expr (elided)
            canvas.drawConditionally(expr) {
                // Empty block, elided
            }

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "textData(42, \"false\")",
                    "textData(43, \"true\")",
                    "addList(2097194, [42, 43])",
                    "setNamedVariable(44, \"USER:cond1\", 4)",
                    "setNamedVariable(45, \"USER:cond2\", 4)",
                    "addIntegerExpression(46, 7, [44, 45, 65546])",
                    "textLookup(47, ID(2097194), 46)",
                    "addDrawTextRun(47)",
                )
        }
    }

    @Test
    fun testDrawConditionally_withExpression_usedAfterElidedConditional_preserved() {
        runWithOptimizingCanvas { canvas, buffer ->
            val cond1 = createNamedRemoteBoolean("cond1", true)
            val cond2 = createNamedRemoteBoolean("cond2", true)
            val expr = cond1 and cond2

            // First: empty conditional block using expr (elided)
            canvas.drawConditionally(expr) {
                // Empty block, elided
            }

            // Second: convert expr to RemoteString and drawText (preserves expr)
            val str = expr.select(RemoteString("true"), RemoteString("false"))
            canvas.drawText(str, 20f.rf, 20f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "textData(42, \"false\")",
                    "textData(43, \"true\")",
                    "addList(2097194, [42, 43])",
                    "setNamedVariable(44, \"USER:cond1\", 4)",
                    "setNamedVariable(45, \"USER:cond2\", 4)",
                    "addIntegerExpression(46, 7, [44, 45, 65546])",
                    "textLookup(47, ID(2097194), 46)",
                    "addDrawTextRun(47)",
                )
        }
    }

    @Test
    fun testDrawConditionally_elidedWhenChildCommandsAreElided() {
        runWithOptimizingCanvas { canvas, buffer ->
            val condition = createNamedRemoteBoolean("cond", true)
            canvas.drawConditionally(condition) {
                canvas.save()
                canvas.translate(10f.rf, 20f.rf)
                // No draw calls, so save/restore is elided.
                // Then drawConditionally has no child commands, so it is elided too.
                canvas.restore()
            }

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls).isEmpty()
        }
    }

    @Test
    fun testDrawConditionally_insideSave_elidesSaveIfOnlyChild() {
        runWithOptimizingCanvas { canvas, buffer ->
            val condition = createNamedRemoteBoolean("cond", true)
            canvas.save()
            canvas.translate(10f.rf, 20f.rf)
            canvas.drawConditionally(condition) {
                // Empty block
            }
            canvas.restore()

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls).isEmpty()
        }
    }

    @Test
    fun testDrawConditionally_multipleChildSpans_preservesOnlyActiveBranches() {
        runWithOptimizingCanvas { canvas, buffer ->
            val cond1 = createNamedRemoteBoolean("cond1", true)
            val cond2 = createNamedRemoteBoolean("cond2", true)
            val cond3 = createNamedRemoteBoolean("cond3", true)

            // Child span 1: elided
            canvas.drawConditionally(cond1) {
                canvas.save()
                canvas.restore()
            }
            // Child span 2: preserved
            canvas.drawConditionally(cond2) {
                canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            }
            // Child span 3: elided
            canvas.drawConditionally(cond3) {
                // Empty block
            }

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "setNamedVariable(42, \"USER:cond2\", 4)",
                    "addConditionalOperations(1, ID(42), 0.0)",
                    "addPaint",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                    "endConditionalOperations",
                )
        }
    }

    @Test
    fun testDrawConditionally_toString() {
        val condition = createNamedRemoteBoolean("myCond", true)
        val span = CanvasOperationBuffer.Span(null, 0)
        val op = CanvasOp.DrawConditionally(condition, span) { _, _ -> }
        assertThat(op.toString()).isEqualTo("DrawConditionally(${condition.toDebugString()})")
    }

    @Test
    fun testSaveTransformBeforeDrawConditionally_preserved() {
        runWithOptimizingCanvas { canvas, buffer ->
            val condition = createNamedRemoteBoolean("cond", true)
            // Save block with transforms right before a conditional draw
            canvas.save()
            canvas.translate(10f.rf, 20f.rf)
            canvas.drawRect(1f.rf, 1f.rf, 2f.rf, 2f.rf, RemotePaint())
            canvas.restore()

            canvas.drawConditionally(condition) {
                canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            }

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly(
                    "addMatrixSave",
                    "addMatrixTranslate(10.0, 20.0)",
                    "addPaint",
                    "addDrawRect(1.0, 1.0, 2.0, 2.0)",
                    "addMatrixRestore",
                    "setNamedVariable(42, \"USER:cond\", 4)",
                    "addConditionalOperations(1, ID(42), 0.0)",
                    "addPaint",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                    "endConditionalOperations",
                )
        }
    }

    @Test
    fun testScaleWithNonZeroPivot() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.scale(2f.rf, 3f.rf, 5f.rf, 5f.rf)
            canvas.translate(10f.rf, 20f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            // Scale(2, 3, 5, 5) -> Translate(10, 20) => Translate(20, 60) -> Scale(2, 3, 5, 5)
            assertThat(buffer.calls)
                .containsExactly(
                    "addMatrixTranslate(20.0, 60.0)",
                    "addMatrixScale(2.0, 3.0, 5.0, 5.0)",
                    "addPaint",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                )
        }
    }

    @Test
    fun testRotateWithDifferentPivots() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.rotate(90f.rf, 10f.rf, 10f.rf)
            canvas.rotate(90f.rf, 20f.rf, 20f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            // Currently we do not fuse rotations with different pivots.
            assertThat(buffer.calls)
                .containsExactly(
                    "addMatrixRotate(90.0, 10.0, 10.0)",
                    "addMatrixRotate(90.0, 20.0, 20.0)",
                    "addPaint",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                )
        }
    }

    @Test
    fun testScaleWithVariablePivot() {
        runWithOptimizingCanvas { canvas, buffer ->
            val varPx = createNamedRemoteFloat("varPx", 5f)
            val varPy = createNamedRemoteFloat("varPy", 5f)

            canvas.scale(2f.rf, 3f.rf, varPx, varPy)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            val pxIdVal = Utils.idFromNan(varPx.getFloatIdForCreationState(canvas.creationState))
            val pyIdVal = Utils.idFromNan(varPy.getFloatIdForCreationState(canvas.creationState))

            // It should NOT discard the pivot.
            assertThat(buffer.calls)
                .containsExactly(
                    "setNamedVariable(42, \"USER:varPx\", 1)",
                    "setNamedVariable(43, \"USER:varPy\", 1)",
                    "addMatrixScale(2.0, 3.0, ID($pxIdVal), ID($pyIdVal))",
                    "addPaint",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                )
        }
    }

    @Test
    fun testDoubleEncodeDoesNotDuplicate() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.translate(10f.rf, 20f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())

            canvas.flush()

            // First encode
            val bytes1 = canvas.document.encodeToByteArray()
            val calls1 = ArrayList(buffer.calls)

            // Second encode (without recording anything new)
            val bytes2 = canvas.document.encodeToByteArray()
            val calls2 = ArrayList(buffer.calls)

            // The calls recorded in the buffer should be identical (no new calls appended)
            assertThat(calls2).isEqualTo(calls1)
            // And the bytes should be identical
            assertThat(bytes2).isEqualTo(bytes1)
        }
    }

    @Test
    fun testSaveRestoreWithOnlyThemeIsPreserved() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save()
            buffer.setTheme(42)
            canvas.restore()

            canvas.flush()
            canvas.document.encodeToByteArray()

            // The save/restore should be inlined, but setTheme(42) must be preserved.
            assertThat(buffer.calls).containsExactly("setTheme(42)")
        }
    }

    @Test
    fun testDiscardedSaveDependency() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save()
            canvas.translate(10f.rf, 10f.rf)
            canvas.restore()
            canvas.drawRect(0f.rf, 0f.rf, 5f.rf, 5f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            // The save/restore and translate should be completely discarded.
            // Only the drawRect (and its paint) should be present.
            assertThat(buffer.calls).containsExactly("addPaint", "addDrawRect(0.0, 0.0, 5.0, 5.0)")
        }
    }

    @Test
    fun testInlinedSaveDependencyAndOrdering() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save()
            canvas.translate(10f.rf, 10f.rf)
            canvas.clipRect(0f.rf, 0f.rf, 10f.rf, 10f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 5f.rf, 5f.rf, RemotePaint())
            canvas.restore()
            // No drawing after it, so it will be inlined.

            canvas.flush()
            canvas.document.encodeToByteArray()

            // The save/restore should be inlined.
            // The translate and clip should be in the correct order.
            assertThat(buffer.calls)
                .containsExactly(
                    "addMatrixTranslate(10.0, 10.0)",
                    "addClipRect(0.0, 0.0, 10.0, 10.0)",
                    "addPaint",
                    "addDrawRect(0.0, 0.0, 5.0, 5.0)",
                )
        }
    }

    @Test
    fun testConsecutiveTopLevelSavesOnlyInlinesLast() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save()
            canvas.translate(10f.rf, 10f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            canvas.restore()

            canvas.save()
            canvas.translate(20f.rf, 20f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 20f.rf, 20f.rf, RemotePaint())
            canvas.restore()

            canvas.flush()
            canvas.document.encodeToByteArray()

            // First save scope must be preserved to contain translate(10, 10).
            // Second save scope is at the tail, so its save/restore is inlined.
            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "addMatrixSave",
                    "addMatrixTranslate(10.0, 10.0)",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                    "addMatrixRestore",
                    "addMatrixTranslate(20.0, 20.0)",
                    "addDrawRect(0.0, 0.0, 20.0, 20.0)",
                )
        }
    }

    @Test
    fun testTrailingTopLevelSaveWithConditionIsNotInlined() {
        runWithOptimizingCanvas { canvas, buffer ->
            val condition = createNamedRemoteBoolean("cond", true)
            canvas.save()
            canvas.translate(10f.rf, 20f.rf)
            canvas.drawConditionally(condition) {
                canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            }
            canvas.restore()

            canvas.flush()
            canvas.document.encodeToByteArray()

            // Even at the absolute tail of the root span, a save block containing a conditional
            // switch must not be inlined.
            assertThat(buffer.calls)
                .containsExactly(
                    "setNamedVariable(42, \"USER:cond\", 4)",
                    "addMatrixSave",
                    "addMatrixTranslate(10.0, 20.0)",
                    "addConditionalOperations(1, ID(42), 0.0)",
                    "addPaint",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                    "endConditionalOperations",
                    "addMatrixRestore",
                )
        }
    }

    @Test
    fun testTailInliningUnlocksMatrixTranslationFusing() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.translate(5f.rf, 5f.rf)
            canvas.save()
            canvas.translate(10f.rf, 10f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            canvas.restore()

            canvas.flush()
            canvas.document.encodeToByteArray()

            // Inlined save scope flattens translate(10, 10), which fuses with root translate(5, 5).
            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint",
                    "addMatrixTranslate(15.0, 15.0)",
                    "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                )
        }
    }

    @Test
    fun testConsecutiveSkewsAreNotFused() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.skew(1f.rf, 0f.rf)
            canvas.skew(0f.rf, 1f.rf)
            canvas.drawRect(0f.rf, 0f.rf, 5f.rf, 5f.rf, RemotePaint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            // Skews should not be fused.
            assertThat(buffer.calls)
                .containsExactly(
                    "addMatrixSkew(1.0, 0.0)",
                    "addMatrixSkew(0.0, 1.0)",
                    "addPaint",
                    "addDrawRect(0.0, 0.0, 5.0, 5.0)",
                )
        }
    }

    @Test
    fun testSaveRestoreWithoutTransformsInlinedEvenWithDrawingAfter() {
        runWithOptimizingCanvas { canvas, buffer ->
            val paint1 = RemotePaint { color = Color.Red.rc }
            val paint2 = RemotePaint { color = Color.Blue.rc }
            canvas.save()
            canvas.drawRect(0f.rf, 0f.rf, 5f.rf, 5f.rf, paint1)
            canvas.restore()
            canvas.drawRect(10f.rf, 10f.rf, 15f.rf, 15f.rf, paint2) // Drawing after!

            canvas.flush()
            canvas.document.encodeToByteArray()

            // The save/restore should be inlined because it contains no transforms or clips.
            assertThat(buffer.calls).doesNotContain("addMatrixSave")
            assertThat(buffer.calls).doesNotContain("addMatrixRestore")

            // We should see both draw calls and their paints in correct order.
            assertThat(buffer.calls)
                .containsExactly(
                    "addPaint", // red
                    "addDrawRect(0.0, 0.0, 5.0, 5.0)",
                    "addPaint", // blue
                    "addDrawRect(10.0, 10.0, 15.0, 15.0)",
                )
        }
    }

    @Test
    fun testOptimizationsAreOffByDefault() {
        // By default, RecordingCanvas should NOT optimize save/restore.
        val localCreationState =
            RemoteComposeCreationState(
                RemoteCreationDisplayInfo(500, 500, 160, 1f),
                null,
                RcPlatformProfiles.ANDROIDX,
            )
        val localCanvas = RemoteCanvas(localCreationState)

        val calls = ArrayList<String>()
        val testBuffer = OptimizingTestRemoteComposeBuffer(calls)
        val optimizingWriter = TestRemoteComposeWriter(RcPlatformProfiles.ANDROIDX, testBuffer)
        localCreationState.document = optimizingWriter

        localCanvas.save()
        localCanvas.translate(10f.rf, 20f.rf)
        localCanvas.restore()

        localCanvas.flush()
        localCanvas.document.encodeToByteArray()

        // Since optimizations are off by default, we expect the save/restore and translate to be
        // preserved.
        assertThat(calls)
            .containsExactly("addMatrixSave", "addMatrixTranslate(10.0, 20.0)", "addMatrixRestore")
    }

    @Test
    fun testTransformPoppingAndReinstatementAroundConditionalDraw() {
        // 1. Push an initial transform on RecordingCanvas: call save() and translate(-87f, -87f).
        remoteCanvas.save()
        remoteCanvas.translate((-87f).rf, (-87f).rf)
        assertThat(remoteCanvas.saveCount).isEqualTo(1)

        // 2. Temporarily pop transforms before a conditional draw: call restore().
        remoteCanvas.restore()
        assertThat(remoteCanvas.saveCount).isEqualTo(0)

        // 3. Call drawConditionally(condition) { drawBitmap(...) } where condition is dynamic.
        val condition = createNamedRemoteBoolean("cond", true)
        val bitmap = RemoteImageBitmap.createForId(42)
        remoteCanvas.drawConditionally(condition) {
            remoteCanvas.drawBitmap(bitmap, 0f.rf, 0f.rf, RemotePaint())
        }
        assertThat(remoteCanvas.saveCount).isEqualTo(0)

        // 4. Reinstate transforms after the conditional draw: call save() and translate(-87f,
        // -87f).
        remoteCanvas.save()
        remoteCanvas.translate((-87f).rf, (-87f).rf)
        assertThat(remoteCanvas.saveCount).isEqualTo(1)

        // 5. Call flush() and verify the recorded operations on the underlying buffer.
        remoteCanvas.flush()
        assertThat(remoteCanvas.saveCount).isEqualTo(1)

        // Verify operation ordering and no reordering or dropping
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addMatrixSave",
                "addMatrixTranslate(-87.0, -87.0)",
                "addMatrixRestore",
                "setNamedVariable(42, \"USER:cond\", 4)",
                "addConditionalOperations(1, ID(42), 0.0)",
                "addPaint",
                "textData(43, \"\")",
                "addDrawBitmap(42)",
                "endConditionalOperations",
                "addContainerEnd",
                "addMatrixSave",
                "addMatrixTranslate(-87.0, -87.0)",
                "addMatrixRestore",
            )
            .inOrder()
    }

    @Test
    fun testTransformPoppingAndReinstatementAroundConditionalDraw_optimized() {
        runWithOptimizingCanvas { canvas, buffer ->
            // 1. Push an initial transform on RecordingCanvas: call save() and translate(-87f,
            // -87f).
            canvas.save()
            canvas.translate((-87f).rf, (-87f).rf)
            assertThat(canvas.saveCount).isEqualTo(1)

            // 2. Temporarily pop transforms before a conditional draw: call restore().
            canvas.restore()
            assertThat(canvas.saveCount).isEqualTo(0)

            // 3. Call drawConditionally(condition) { drawBitmap(...) } where condition is dynamic.
            val condition = createNamedRemoteBoolean("cond", true)
            val bitmap = RemoteImageBitmap.createForId(42)
            canvas.drawConditionally(condition) {
                canvas.drawBitmap(bitmap, 0f.rf, 0f.rf, RemotePaint())
            }
            assertThat(canvas.saveCount).isEqualTo(0)

            // 4. Reinstate transforms after the conditional draw: call save() and translate(-87f,
            // -87f).
            canvas.save()
            canvas.translate((-87f).rf, (-87f).rf)
            assertThat(canvas.saveCount).isEqualTo(1)

            // 5. Call flush() and verify the recorded operations on the underlying buffer.
            canvas.flush()
            assertThat(canvas.saveCount).isEqualTo(1)
            canvas.document.encodeToByteArray()
        }
    }

    @Test
    fun testDrawConditionally_doesNotLeakLastRenderingOp() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save()
            val prevLastOp = canvas.buffer.lastRenderingOp
            assertThat(prevLastOp).isNotNull()

            val condition = createNamedRemoteBoolean("cond", true)
            canvas.drawConditionally(condition) { canvas.translate(10f.rf, 10f.rf) }

            val opAfterCond = canvas.buffer.lastRenderingOp
            assertThat(opAfterCond).isEqualTo(prevLastOp)
        }
    }

    @Test
    fun testDrawToOffscreenBitmap_doesNotLeakLastRenderingOp() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save()
            val prevLastOp = canvas.buffer.lastRenderingOp
            assertThat(prevLastOp).isNotNull()

            val offscreenBitmap = RemoteImageBitmap.createForId(42)
            canvas.drawToOffscreenBitmap(offscreenBitmap) { canvas.translate(5f.rf, 5f.rf) }

            val opAfterOffscreen = canvas.buffer.lastRenderingOp
            assertThat(opAfterOffscreen).isEqualTo(prevLastOp)
        }
    }

    @Test
    fun testLoop_doesNotLeakLastRenderingOp() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save()
            val prevLastOp = canvas.buffer.lastRenderingOp
            assertThat(prevLastOp).isNotNull()

            val from = 0f.rf
            val until = 10f.rf
            val step = 1f.rf
            canvas.loop(from, until, step) { canvas.translate(5f.rf, 5f.rf) }

            val opAfterLoop = canvas.buffer.lastRenderingOp
            assertThat(opAfterLoop).isEqualTo(prevLastOp)
        }
    }

    @Test
    fun testTransformPoppingAndReinstatement_preservesWireOrderAndSaveStack() {
        runWithOptimizingCanvas { canvas, buffer ->
            // Step 1: Push initial group transform (simulating ancestor group translation)
            canvas.save()
            canvas.translate((-87f).rf, (-87f).rf)
            // Add a draw call so optimizing canvas preserves this transform block
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            assertThat(canvas.saveCount).isEqualTo(1)

            // Step 2: Temporarily pop transforms before drawing span
            canvas.restore()
            assertThat(canvas.saveCount).isEqualTo(0)

            // Step 3: Draw span inside a conditional (using a dynamic boolean condition)
            val dynamicCondition = createNamedRemoteBoolean("test", true)
            canvas.drawConditionally(dynamicCondition) {
                val offscreenBitmap = createOffscreenRemoteBitmap(450, 450)
                canvas.drawBitmap(offscreenBitmap, 0f.rf, 0f.rf, null)
            }

            // Step 4: Reinstate group transform after conditional span
            canvas.save()
            canvas.translate((-87f).rf, (-87f).rf)
            // Add a draw call so optimizing canvas preserves this reinstated transform block
            canvas.drawRect(20f.rf, 20f.rf, 30f.rf, 30f.rf, RemotePaint())
            canvas.restore()
            assertThat(canvas.saveCount).isEqualTo(0)

            // Step 4b: Draw something after the reinstated transform so the save block is preserved
            // by elision pass
            canvas.drawRect(40f.rf, 40f.rf, 50f.rf, 50f.rf, RemotePaint())

            // Step 5: Flush buffer and inspect wire command ordering after optimization and
            // topological sort
            canvas.flush()

            // Verify exact wire order in emitted buffer commands
            val restoreIdx = buffer.calls.indexOfFirst { it.contains("Restore", ignoreCase = true) }
            val condIdx =
                buffer.calls.indexOfFirst { it.contains("Conditional", ignoreCase = true) }
            val saveIdx = buffer.calls.indexOfLast { it.contains("Save", ignoreCase = true) }
            val translateIdx =
                buffer.calls.indexOfLast { it.contains("Translate", ignoreCase = true) }

            assertThat(restoreIdx).isNotEqualTo(-1)
            assertThat(condIdx).isNotEqualTo(-1)
            assertThat(saveIdx).isNotEqualTo(-1)
            assertThat(translateIdx).isNotEqualTo(-1)

            assertThat(restoreIdx).isLessThan(condIdx)
            assertThat(condIdx).isLessThan(saveIdx)
            assertThat(saveIdx).isLessThan(translateIdx)
        }
    }

    @Test
    fun testUnbalancedSaveInChildSpanThrowsIllegalStateException() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = RemoteCanvas(creationState, enableOptimizations = false)

        canvas.save()
        assertEquals(1, canvas.saveCount)

        val condition = RemoteBoolean(true)
        val ex1 =
            assertThrows(IllegalStateException::class.java) {
                canvas.drawConditionally(condition) {
                    canvas.save() // Net +1 save
                }
            }
        assertThat(ex1.message).contains("Unbalanced save/restore in child span")

        val ex2 =
            assertThrows(IllegalStateException::class.java) {
                canvas.drawConditionally(condition) {
                    canvas.restore() // Net -1 save
                }
            }
        assertThat(ex2.message).contains("Unbalanced save/restore in child span")
    }

    @Test
    fun testChildSpanUnbalancedSaveRestoresCanvasState() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = RemoteCanvas(creationState, enableOptimizations = false)

        val outerInsertPoint = canvas.buffer.insertPoint
        canvas.save()
        val outerSaveNode = canvas.buffer.currentSaveRestoreNode
        val outerInitialSpanSaveCount = canvas.buffer.initialSpanSaveCount
        val outerSaveCount = canvas.saveCount

        val condition = RemoteBoolean(true)
        assertThrows(IllegalStateException::class.java) {
            canvas.drawConditionally(condition) {
                canvas.save() // Unbalanced save
            }
        }

        // Verify canvas internal state was restored despite exception
        assertThat(canvas.buffer.insertPoint).isSameInstanceAs(outerInsertPoint)
        assertThat(canvas.buffer.currentSaveRestoreNode).isSameInstanceAs(outerSaveNode)
        assertEquals(outerInitialSpanSaveCount, canvas.buffer.initialSpanSaveCount)
        assertEquals(outerSaveCount, canvas.saveCount)

        // Calling restore() once should restore the 1 outer save and bring saveCount to 0
        canvas.restore()
        assertEquals(0, canvas.saveCount)
        // A second restore should throw underflow
        assertThrows(IllegalStateException::class.java) { canvas.restore() }
    }

    @Test
    fun testChildSpanExceptionNotMaskedByUnbalancedSave() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = RemoteCanvas(creationState, enableOptimizations = false)

        class CustomUserException(msg: String) : RuntimeException(msg)

        val condition = RemoteBoolean(true)
        val thrown =
            assertThrows(CustomUserException::class.java) {
                canvas.drawConditionally(condition) {
                    canvas.save() // Leaves save/restore unbalanced
                    throw CustomUserException("original user failure")
                }
            }
        assertThat(thrown.message).isEqualTo("original user failure")
    }

    @Test
    fun testChildSpanCanPopAndReinstateOuterSaveFrames() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = RemoteCanvas(creationState, enableOptimizations = false)

        // 1. Push 2 saves on outer span
        canvas.save()
        canvas.save()
        assertEquals(2, canvas.saveCount)

        // 2. Enter child span via drawConditionally
        val dummyCondition = RemoteBoolean(true)
        canvas.drawConditionally(dummyCondition) {
            // Inside child span, we can push local saves
            canvas.save()
            assertEquals(3, canvas.saveCount)
            canvas.restore()
            assertEquals(2, canvas.saveCount)

            // Temporarily pop outer saves
            canvas.restore()
            assertEquals(1, canvas.saveCount)
            canvas.restore()
            assertEquals(0, canvas.saveCount)

            // Underflow throws IllegalStateException
            assertThrows(IllegalStateException::class.java) { canvas.restore() }

            // Reinstate the popped outer saves
            canvas.save()
            assertEquals(1, canvas.saveCount)
            canvas.save()
            assertEquals(2, canvas.saveCount)
        }

        // 3. Pop outer saves on main canvas
        canvas.restore()
        canvas.restore()
        assertEquals(0, canvas.saveCount)

        // 4. Global underflow on main canvas must throw IllegalStateException
        assertThrows(IllegalStateException::class.java) { canvas.restore() }
    }

    @Test
    fun testChildSpanPopAndReinstateExecutesOnCoreDocument() {
        val calls = ArrayList<String>()
        val buffer = RecordingTestRemoteComposeBuffer(calls)
        val platform = AndroidxRcPlatformServices()
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                callbacks ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    buffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }

        val creationState =
            RemoteComposeCreationState(RemoteCreationDisplayInfo(500, 500, 160, 1f), null, profile)
        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        val canvas = RemoteCanvas(creationState)

        canvas.save()
        canvas.translate(20f.rf, 30f.rf)

        canvas.drawConditionally(RemoteBoolean(true)) {
            canvas.restore()
            canvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, RemotePaint())
            canvas.save()
            canvas.translate(20f.rf, 30f.rf)
            canvas.drawRect(5f.rf, 5f.rf, 15f.rf, 15f.rf, RemotePaint())
        }

        canvas.restore()
        canvas.flush()

        val coreDoc =
            CoreDocument().apply {
                buffer.buffer.index = 0
                initFromBuffer(buffer)
            }
        assertThat(coreDoc.docInfo.mNumberOfOps).isGreaterThan(0)
    }

    @Test
    fun testDefinePattern() {
        val macro = remoteCanvas.definePattern { x: RemoteFloat, y: RemoteFloat, c: RemoteColor ->
            remoteCanvas.drawRect(
                x,
                y,
                x + 50f.rf,
                y + 50f.rf,
                RemotePaint { color = c },
            )
        }

        macro(10f.rf, 10f.rf, Color.Red.rc, null)
        macro(10f.rf, 100f.rf, Color.Green.rc, null)
        macro(100f.rf, 10f.rf, Color.Blue.rc, null)
        macro(100f.rf, 100f.rf, Color.Yellow.rc, null)

        remoteCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter { it.startsWith("drawRect") || it.contains("paint.setColor") }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFFFF0000)",
                "drawRect(10.000000, 10.000000, 60.000000, 60.000000)",
                "  paint.setColor(0xFF00FF00)",
                "drawRect(10.000000, 100.000000, 60.000000, 150.000000)",
                "  paint.setColor(0xFF0000FF)",
                "drawRect(100.000000, 10.000000, 150.000000, 60.000000)",
                "  paint.setColor(0xFFFFFF00)",
                "drawRect(100.000000, 100.000000, 150.000000, 150.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_sixArguments() {
        val macro =
            remoteCanvas.definePattern {
                x: RemoteFloat,
                y: RemoteFloat,
                w: RemoteFloat,
                h: RemoteFloat,
                c: RemoteColor,
                condition: RemoteBoolean ->
                remoteCanvas.drawConditionally(condition) {
                    remoteCanvas.drawRect(
                        x,
                        y,
                        x + w,
                        y + h,
                        RemotePaint { color = c },
                    )
                }
            }

        macro(10f.rf, 10f.rf, 50f.rf, 50f.rf, Color.Red.rc, RemoteBoolean(true), null)
        // Note this will get elided due to the condition being false.
        macro(20f.rf, 20f.rf, 60f.rf, 60f.rf, Color.Green.rc, RemoteBoolean(false), null)

        remoteCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter { it.startsWith("drawRect") || it.contains("paint.setColor") }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFFFF0000)",
                "drawRect(10.000000, 10.000000, 60.000000, 60.000000)",
            )
            .inOrder()
    }

    @Test
    fun testRemoteCanvas_forEach_singleArg() {
        remoteCanvas.forEach(listOf(10f.rf, 30f.rf, 50f.rf)) { cx ->
            remoteCanvas.drawCircle(
                cx,
                50f.rf,
                10f.rf,
                RemotePaint { color = Color.Blue.rc },
            )
        }

        remoteCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawCircle") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(10.000000, 50.000000, 10.000000)",
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(30.000000, 50.000000, 10.000000)",
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(50.000000, 50.000000, 10.000000)",
            )
            .inOrder()
    }

    @Ignore("Requires remote-core tuple alias support for multi-argument forEach")
    @Test
    fun testRemoteCanvas_forEach_multiArgs() {
        remoteCanvas.forEach(
            listOf(10f.rf, 40f.rf),
            listOf(15f.rf, 45f.rf),
            listOf(Color.Red.rc, Color.Green.rc),
        ) { x, y, color ->
            remoteCanvas.drawRect(
                x,
                y,
                x + 20f.rf,
                y + 20f.rf,
                RemotePaint { this.color = color },
            )
        }

        remoteCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter { it.startsWith("drawRect") || it.contains("paint.setColor") }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFFFF0000)",
                "drawRect(10.000000, 15.000000, 30.000000, 35.000000)",
                "  paint.setColor(0xFF00FF00)",
                "drawRect(40.000000, 45.000000, 60.000000, 65.000000)",
            )
            .inOrder()
    }

    @Test
    fun testRemoteCanvas_forEach_floatArray() {
        val paint = RemotePaint { color = Color.Red.rc }
        val array = RemoteFloatArray(listOf(100f.rf, 200f.rf))

        remoteCanvas.forEach(array) { x -> remoteCanvas.drawCircle(x, 50f.rf, 10f.rf, paint) }

        remoteCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawCircle") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFFFF0000)",
                "drawCircle(100.000000, 50.000000, 10.000000)",
                "  paint.setColor(0xFFFF0000)",
                "drawCircle(200.000000, 50.000000, 10.000000)",
            )
            .inOrder()
    }

    @Test
    fun testRemoteCanvas_forEach_intArray() {
        val paint = RemotePaint { color = Color.Blue.rc }
        val intArray = RemoteIntArray(10, 20, 30)

        remoteCanvas.forEach(intArray) { intVal ->
            remoteCanvas.drawCircle(intVal.toRemoteFloat(), 50f.rf, 5f.rf, paint)
        }

        remoteCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawCircle") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(10.000000, 50.000000, 5.000000)",
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(20.000000, 50.000000, 5.000000)",
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(30.000000, 50.000000, 5.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_withRemoteIntArray() {
        val paint = RemotePaint { color = Color.Green.rc }
        val listPattern = remoteCanvas.definePattern { offsets: RemoteIntArray ->
            remoteCanvas.forEach(offsets) { offset ->
                remoteCanvas.drawCircle(offset.toRemoteFloat(), 25f.rf, 5f.rf, paint)
            }
        }

        listPattern(RemoteIntArray(50, 100))

        remoteCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawCircle") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFF00FF00)",
                "drawCircle(50.000000, 25.000000, 5.000000)",
                "  paint.setColor(0xFF00FF00)",
                "drawCircle(100.000000, 25.000000, 5.000000)",
            )
            .inOrder()
    }

    @Test
    fun testRemoteCanvas_forEach_stringArray() {
        val paint = RemotePaint { color = Color.Black.rc }
        val stringArray = RemoteStringArray(listOf("One".rs, "Two".rs))

        remoteCanvas.forEach(stringArray) { str ->
            remoteCanvas.drawText(str, 0f.rf, 10f.rf, paint)
        }

        remoteCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawTextRun") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFF000000)",
                "drawTextRun(text=\"One\", start=0, end=-1, x=0.000000, y=10.000000)",
                "  paint.setColor(0xFF000000)",
                "drawTextRun(text=\"Two\", start=0, end=-1, x=0.000000, y=10.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_inForEach_remoteFloatArray() {
        remoteCanvas.forEach(RemoteFloatArray(listOf(10f.rf, 30f.rf))) { cx ->
            remoteCanvas.drawCircle(
                cx,
                50f.rf,
                10f.rf,
                RemotePaint { color = Color.Blue.rc },
            )
        }

        remoteCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawCircle") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(10.000000, 50.000000, 10.000000)",
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(30.000000, 50.000000, 10.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_inForEach_remoteIntArray() {
        remoteCanvas.forEach(RemoteIntArray(10, 30)) { cx ->
            remoteCanvas.drawCircle(
                cx.toRemoteFloat(),
                50f.rf,
                10f.rf,
                RemotePaint { color = Color.Green.rc },
            )
        }

        remoteCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawCircle") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFF00FF00)",
                "drawCircle(10.000000, 50.000000, 10.000000)",
                "  paint.setColor(0xFF00FF00)",
                "drawCircle(30.000000, 50.000000, 10.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_inForEach_remoteStringArray() {
        remoteCanvas.forEach(RemoteStringArray(listOf("Hello".rs, "World".rs))) { text ->
            remoteCanvas.drawText(
                text,
                0f.rf,
                10f.rf,
                RemotePaint { color = Color.Black.rc },
            )
        }

        remoteCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawTextRun") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFF000000)",
                "drawTextRun(text=\"Hello\", start=0, end=-1, x=0.000000, y=10.000000)",
                "  paint.setColor(0xFF000000)",
                "drawTextRun(text=\"World\", start=0, end=-1, x=0.000000, y=10.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_withRemoteStringArray() {
        val paint = RemotePaint { color = Color.Magenta.rc }
        val listPattern = remoteCanvas.definePattern { texts: RemoteStringArray ->
            remoteCanvas.forEach(texts) { text ->
                remoteCanvas.drawText(text, 0f.rf, 20f.rf, paint)
            }
        }

        listPattern(RemoteStringArray(listOf("Alpha".rs, "Omega".rs)))

        remoteCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawTextRun") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFFFF00FF)",
                "drawTextRun(text=\"Alpha\", start=0, end=-1, x=0.000000, y=20.000000)",
                "  paint.setColor(0xFFFF00FF)",
                "drawTextRun(text=\"Omega\", start=0, end=-1, x=0.000000, y=20.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_withRemoteFloatArray() {
        val paint = RemotePaint { color = Color.Red.rc }
        val listPattern = remoteCanvas.definePattern { offsets: RemoteFloatArray ->
            remoteCanvas.forEach(offsets) { offset ->
                remoteCanvas.drawCircle(offset, 25f.rf, 5f.rf, paint)
            }
        }

        listPattern(RemoteFloatArray(listOf(50f.rf, 100f.rf)))

        remoteCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawCircle") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFFFF0000)",
                "drawCircle(50.000000, 25.000000, 5.000000)",
                "  paint.setColor(0xFFFF0000)",
                "drawCircle(100.000000, 25.000000, 5.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_unsupportedType_throwsIllegalArgumentException() {
        val pattern = remoteCanvas.definePattern { dp: RemoteDp ->
            remoteCanvas.drawCircle(dp.value, 25f.rf, 5f.rf, RemotePaint())
        }

        assertThrows(IllegalArgumentException::class.java) { pattern(10.rdp) }
    }

    @Test
    fun testDrawTweenPath_RemotePathOverload_DoesNotDoubleBuffer() {
        val path1 = RemotePath()
        val path2 = RemotePath()
        val tween = createNamedRemoteFloat("tween", 0.5f)
        val start = RemoteFloat(0f)
        val stop = RemoteFloat(1f)
        remoteCanvas.drawTweenPath(
            path1,
            path2,
            tween,
            start,
            stop,
            RemotePaint(),
        )

        assertThat(fakeBuffer.calls).isEmpty()

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls).contains("addPaint")
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawTweenPath") }).isTrue()
    }
}

private class OptimizingTestRemoteComposeBuffer(calls: ArrayList<String> = ArrayList()) :
    RecordingTestRemoteComposeBuffer(calls)

private fun formatFloat(f: Float): String {
    return if (f.isNaN()) {
        val mathName = AnimatedFloatExpression.toMathName(f)
        if (mathName != null) {
            mathName
        } else {
            val id = Utils.idFromNan(f)
            if (id == 0) {
                "NaN"
            } else {
                "ID($id)"
            }
        }
    } else {
        f.toString()
    }
}

private fun FloatArray.formatToString(): String {
    return this.joinToString(prefix = "[", postfix = "]") { formatFloat(it) }
}

private open class RecordingTestRemoteComposeBuffer(val calls: ArrayList<String>) :
    RemoteComposeBuffer(CoreDocument.DOCUMENT_API_LEVEL) {

    override fun drawOnBitmap(bitmapId: Int, mode: Int, color: Int) {
        calls.add("drawOnBitmap($bitmapId, $mode, $color)")
        super.drawOnBitmap(bitmapId, mode, color)
    }

    override fun addMatrixSave() {
        calls.add("addMatrixSave")
        super.addMatrixSave()
    }

    override fun addMatrixRestore() {
        calls.add("addMatrixRestore")
        super.addMatrixRestore()
    }

    override fun addMatrixTranslate(dx: Float, dy: Float) {
        calls.add("addMatrixTranslate(${formatFloat(dx)}, ${formatFloat(dy)})")
        super.addMatrixTranslate(dx, dy)
    }

    override fun addMatrixScale(scaleX: Float, scaleY: Float) {
        calls.add("addMatrixScale(${formatFloat(scaleX)}, ${formatFloat(scaleY)})")
        super.addMatrixScale(scaleX, scaleY)
    }

    override fun addMatrixScale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {
        calls.add(
            "addMatrixScale(${formatFloat(scaleX)}, ${formatFloat(scaleY)}, ${formatFloat(centerX)}, ${formatFloat(centerY)})"
        )
        super.addMatrixScale(scaleX, scaleY, centerX, centerY)
    }

    override fun addMatrixRotate(angle: Float, centerX: Float, centerY: Float) {
        calls.add(
            "addMatrixRotate(${formatFloat(angle)}, ${formatFloat(centerX)}, ${formatFloat(centerY)})"
        )
        super.addMatrixRotate(angle, centerX, centerY)
    }

    override fun addMatrixSkew(skewX: Float, skewY: Float) {
        calls.add("addMatrixSkew(${formatFloat(skewX)}, ${formatFloat(skewY)})")
        super.addMatrixSkew(skewX, skewY)
    }

    override fun addClipRect(left: Float, top: Float, right: Float, bottom: Float) {
        calls.add(
            "addClipRect(${formatFloat(left)}, ${formatFloat(top)}, ${formatFloat(right)}, ${formatFloat(bottom)})"
        )
        super.addClipRect(left, top, right, bottom)
    }

    override fun addDrawRect(left: Float, top: Float, right: Float, bottom: Float) {
        calls.add(
            "addDrawRect(${formatFloat(left)}, ${formatFloat(top)}, ${formatFloat(right)}, ${formatFloat(bottom)})"
        )
        super.addDrawRect(left, top, right, bottom)
    }

    override fun addDrawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        calls.add(
            "addDrawLine(${formatFloat(x1)}, ${formatFloat(y1)}, ${formatFloat(x2)}, ${formatFloat(y2)})"
        )
        super.addDrawLine(x1, y1, x2, y2)
    }

    override fun addDrawBitmap(
        bitmapId: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        descriptionId: Int,
    ) {
        calls.add("addDrawBitmap($bitmapId)")
        super.addDrawBitmap(bitmapId, left, top, right, bottom, descriptionId)
    }

    override fun addPaint(paint: PaintBundle) {
        calls.add("addPaint")
        super.addPaint(paint)
    }

    override fun setTheme(theme: Int) {
        calls.add("setTheme($theme)")
        super.setTheme(theme)
    }

    override fun addLoopStart(indexId: Int, from: Float, step: Float, until: Float) {
        calls.add(
            "addLoopStart($indexId, ${formatFloat(from)}, ${formatFloat(step)}, ${formatFloat(until)})"
        )
        super.addLoopStart(indexId, from, step, until)
    }

    override fun addLoopEnd() {
        calls.add("addLoopEnd")
        super.addLoopEnd()
    }

    override fun addConditionalOperations(type: Byte, a: Float, b: Float) {
        calls.add("addConditionalOperations($type, ${formatFloat(a)}, ${formatFloat(b)})")
        super.addConditionalOperations(type, a, b)
    }

    override fun endConditionalOperations() {
        calls.add("endConditionalOperations")
        super.endConditionalOperations()
    }

    override fun setNamedVariable(id: Int, name: String, type: Int) {
        calls.add("setNamedVariable($id, \"$name\", $type)")
        super.setNamedVariable(id, name, type)
    }

    override fun addIntegerExpression(id: Int, mask: Int, value: IntArray) {
        calls.add("addIntegerExpression($id, $mask, ${value.toList()})")
        super.addIntegerExpression(id, mask, value)
    }

    override fun addColorExpression(id: Int, alpha: Float, red: Float, green: Float, blue: Float) {
        calls.add(
            "addColorExpression($id, ${formatFloat(alpha)}, ${formatFloat(red)}, ${formatFloat(green)}, ${formatFloat(blue)})"
        )
        super.addColorExpression(id, alpha, red, green, blue)
    }

    override fun addText(id: Int, text: String) {
        calls.add("textData($id, \"$text\")")
        super.addText(id, text)
    }

    override fun addList(id: Int, value: IntArray) {
        calls.add("addList($id, ${value.toList()})")
        super.addList(id, value)
    }

    override fun createTextFromFloat(
        textId: Int,
        value: Float,
        before: Short,
        after: Short,
        flags: Int,
    ): Int {
        calls.add("createTextFromFloat($textId, ${formatFloat(value)}, $before, $after, $flags)")
        return super.createTextFromFloat(textId, value, before, after, flags)
    }

    override fun textLookup(textId: Int, stringListId: Float, indexId: Float) {
        calls.add("textLookup($textId, ${formatFloat(stringListId)}, ${formatFloat(indexId)})")
        super.textLookup(textId, stringListId, indexId)
    }

    override fun textLookup(textId: Int, stringListId: Float, indexId: Int) {
        calls.add("textLookup($textId, ${formatFloat(stringListId)}, $indexId)")
        super.textLookup(textId, stringListId, indexId)
    }

    override fun addDrawTextRun(
        textId: Int,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        rtl: Boolean,
    ) {
        calls.add("addDrawTextRun($textId)")
        super.addDrawTextRun(textId, start, end, contextStart, contextEnd, x, y, rtl)
    }

    override fun addAnimatedFloat(id: Int, value: FloatArray) {
        val labels = arrayOfNulls<String>(value.size)
        for (i in 0 until value.size) {
            if (value[i].isNaN()) {
                labels[i] = "[" + Utils.idFromNan(value[i]) + "]"
            }
        }
        val exprStr = AnimatedFloatExpression.toString(value, labels)
        calls.add("addAnimatedFloat($id) = ($exprStr)")
        super.addAnimatedFloat(id, *value)
    }

    override fun addAnimatedFloat(id: Int, value: FloatArray, animation: FloatArray?) {
        val labels = arrayOfNulls<String>(value.size)
        for (i in 0 until value.size) {
            if (value[i].isNaN()) {
                labels[i] = "[" + Utils.idFromNan(value[i]) + "]"
            }
        }
        val exprStr = AnimatedFloatExpression.toString(value, labels)
        calls.add("addAnimatedFloat($id) = ($exprStr)")
        super.addAnimatedFloat(id, value, animation)
    }
}
