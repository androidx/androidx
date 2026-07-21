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

package androidx.compose.remote.creation.compose.capture

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.state.RemoteBitmapFont
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.selectIfGt
import androidx.compose.remote.creation.compose.state.selectIfLt
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.graphics.shapes.RoundedPolygon
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecordingCanvasTest {
    private lateinit var creationState: RemoteComposeCreationState
    private lateinit var recordingCanvas: RecordingCanvas
    private lateinit var fakeBuffer: TestRemoteComposeBuffer

    private enum class DummyEnum {
        VALUE
    }

    private class MyRemoteComposeWriterAndroid(
        profile: Profile,
        buffer: RemoteComposeBuffer,
        vararg tags: RemoteComposeWriter.HTag,
    ) : RemoteComposeWriterAndroid(profile, buffer, *tags)

    @Before
    fun setUp() {
        fakeBuffer = TestRemoteComposeBuffer()

        val platform = AndroidxRcPlatformServices()
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
    }

    @Test
    fun testOperationsAreBuffered() {
        val paint = Paint()
        recordingCanvas.drawRect(1f, 2f, 3f, 4f, paint)

        assertThat(fakeBuffer.calls).isEmpty()

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls).containsExactly("addPaint", "addDrawRect(1.0, 2.0, 3.0, 4.0)")
    }

    @Test
    fun testExecuteOperations() {
        val paint = Paint()
        recordingCanvas.drawRect(5f, 6f, 7f, 8f, paint)

        assertThat(fakeBuffer.calls).isEmpty()

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls).containsExactly("addPaint", "addDrawRect(5.0, 6.0, 7.0, 8.0)")

        fakeBuffer.calls.clear()
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls).isEmpty()
    }

    @Test
    fun testDrawConditionallyBuffered() {
        val condition = RemoteBoolean.createNamedRemoteBoolean("cond", true)

        recordingCanvas.drawConditionally(condition) {
            recordingCanvas.drawRect(9f, 10f, 11f, 12f, Paint())
        }

        assertThat(fakeBuffer.calls).isEmpty()

        recordingCanvas.flush()

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
        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)
        val sub = x + y // Common subexpression

        recordingCanvas.drawConditionally(RemoteBoolean(true)) {
            recordingCanvas.drawRect(sub, 13f.rf, 14f.rf, 15f.rf, Paint())
        }

        recordingCanvas.drawConditionally(RemoteBoolean(false)) {
            recordingCanvas.drawRect(sub, 20f.rf, 16f.rf, 17f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)
        val sub = x + y // Common subexpression

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", false)

        recordingCanvas.drawConditionally(condition1) {
            recordingCanvas.drawConditionally(condition2) {
                recordingCanvas.drawRect(sub, 18f.rf, 19f.rf, 20f.rf, Paint())
            }
        }

        recordingCanvas.drawConditionally(condition2) {
            recordingCanvas.drawRect(sub, 21f.rf, 22f.rf, 23f.rf, Paint())
        }

        recordingCanvas.flush()

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
    fun testCommonSubexpressionElimination_Int_InTree() {
        val x = RemoteInt.createNamedRemoteInt("x", 10)
        val y = RemoteInt.createNamedRemoteInt("y", 20)
        val sub = x + y // Common subexpression

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", false)

        recordingCanvas.drawConditionally(condition1) {
            recordingCanvas.drawRect(sub.toRemoteFloat(), 24f.rf, 25f.rf, 26f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            recordingCanvas.drawRect(sub.toRemoteFloat(), 27f.rf, 28f.rf, 29f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val cond = x.isLessThan(RemoteFloat(5f))
        val sub = cond.select(RemoteFloat(100f), RemoteFloat(200f))

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)

        recordingCanvas.drawConditionally(condition1) {
            recordingCanvas.drawRect(sub, 30f.rf, 31f.rf, 32f.rf, Paint())
        }

        recordingCanvas.drawRect(sub, 33f.rf, 34f.rf, 35f.rf, Paint()) // Top level usage

        recordingCanvas.flush()

        val subId = creationState.remoteVariableToId.getOrDefault(sub.cacheKey, -1)
        assertThat(subId).isNotEqualTo(-1)

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "addAnimatedFloat(43) = (0.0 1.0 5.0 [42] - ifElse )",
                "addAnimatedFloat(44) = (200.0 100.0 [43] ifElse )",
                "setNamedVariable(45, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(45), 0.0)",
                "addPaint",
                "addDrawRect(ID(44), 30.0, 31.0, 32.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "addPaint",
                "addDrawRect(ID(44), 33.0, 34.0, 35.0)",
            )
    }

    @Test
    fun testCommonSubexpressionElimination_LongExpression_InTree() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)
        val z = RemoteFloat.createNamedRemoteFloat("z", 30f)

        val sub = (x * y) + (y * z) - (x * z)

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", false)

        recordingCanvas.drawConditionally(condition1) {
            recordingCanvas.drawRect(sub, 36f.rf, 37f.rf, 38f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            recordingCanvas.drawRect(sub, 39f.rf, 40f.rf, 41f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val a = RemoteFloat.createNamedRemoteFloat("a", 1f)
        val b = RemoteFloat.createNamedRemoteFloat("b", 2f)
        val c = RemoteFloat.createNamedRemoteFloat("c", 3f)

        val sub1 = a + b
        val sub2 = a + c
        val sub3 = sub1 * sub2

        val root1 = sub1
        val root2 = sub2
        val root3 = sub3 + RemoteFloat(1f)
        val root4 = sub3 + RemoteFloat(2f)
        val root5 = sub1 * RemoteFloat(3f)

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", false)

        recordingCanvas.drawConditionally(condition1) {
            recordingCanvas.drawRect(root3, 42f.rf, 43f.rf, 44f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            recordingCanvas.drawRect(root4, 45f.rf, 46f.rf, 47f.rf, Paint())
        }

        recordingCanvas.drawRect(root1, 48f.rf, 49f.rf, 50f.rf, Paint())
        recordingCanvas.drawRect(root2, 51f.rf, 52f.rf, 53f.rf, Paint())
        recordingCanvas.drawRect(root5, 54f.rf, 55f.rf, 56f.rf, Paint())

        recordingCanvas.flush()

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
        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)
        val sub = x + y // Common subexpression
        val sub2 = x + y // Common subexpression

        recordingCanvas.drawRect(sub, 57f.rf, 58f.rf, 59f.rf, Paint())
        recordingCanvas.drawRect(sub2, 60f.rf, 61f.rf, 62f.rf, Paint())

        recordingCanvas.flush()

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

        recordingCanvas.loop(from, until, step) { index ->
            recordingCanvas.drawRect(index, 63f.rf, 64f.rf, 65f.rf, Paint())
        }

        assertThat(fakeBuffer.calls).isEmpty()

        recordingCanvas.flush()

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
                canvas.drawRect(index, 63f.rf, 64f.rf, 65f.rf, Paint())
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

        recordingCanvas.drawToOffscreenBitmap(dummyBitmap) {
            recordingCanvas.drawRect(66f, 67f, 68f, 69f, Paint())
        }

        assertThat(fakeBuffer.calls).isEmpty()

        recordingCanvas.flush()

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

        recordingCanvas.drawToOffscreenBitmap(outerBitmap, android.graphics.Color.TRANSPARENT) {
            recordingCanvas.save()
            recordingCanvas.translate(1f, 1f)

            recordingCanvas.drawToOffscreenBitmap(innerBitmap, android.graphics.Color.TRANSPARENT) {
                recordingCanvas.drawRect(0f, 0f, 10f, 10f, Paint())
            }

            recordingCanvas.restore()
            recordingCanvas.drawRect(0f, 0f, 1f, 1f, Paint())
        }

        recordingCanvas.flush()

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
            val offscreenBitmap = RemoteImageBitmap.createOffscreenRemoteBitmap(100, 100)

            // 1. Outer canvas pushes 2 matrix transforms/saves
            canvas.save()
            canvas.translate(10f, 10f)
            canvas.save()
            canvas.scale(2f, 2f)
            assertThat(canvas.globalSaveCounter).isEqualTo(2)

            // 2. Draw to offscreen bitmap (creates a childSpan)
            canvas.drawToOffscreenBitmap(offscreenBitmap, android.graphics.Color.TRANSPARENT) {
                // Simulate offscreen drawing that pushes/pops canvas transforms
                canvas.save()
                canvas.translate(5f, 5f)
                canvas.drawRect(0f, 0f, 10f, 10f, Paint())
                canvas.restore()
            }

            // 3. Back in outer canvas, pop both outer saves
            canvas.restore()
            canvas.restore()
            assertThat(canvas.globalSaveCounter).isEqualTo(0)

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
            val paint = Paint()

            // 1. Push multiple saves where only one actually translates
            canvas.save() // Identity save 1 (should be pruned/collapsed)
            canvas.save() // Identity save 2 (should be pruned/collapsed)
            canvas.save()
            canvas.translate(100f, 100f)
            canvas.save() // Identity save 3 (should be pruned/collapsed)

            val offscreenBitmap = RemoteImageBitmap.createOffscreenRemoteBitmap(200, 200)

            // 2. Draw into offscreen bitmap (creates childSpan)
            canvas.drawToOffscreenBitmap(offscreenBitmap, android.graphics.Color.TRANSPARENT) {
                // Child span re-applies parent state + new local transform
                canvas.save()
                canvas.save()
                canvas.save()
                canvas.translate(100f, 100f)
                canvas.save()
                canvas.save()
                canvas.translate(4f, 4f)
                canvas.drawLine(10f, 10f, 20f, 20f, paint)
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
            canvas.translate(100f, 100f)
            canvas.save()
            canvas.drawLine(10f, 10f, 100f, 100f, paint)
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
            val remoteBitmap42 = RemoteImageBitmap.createOffscreenRemoteBitmap(450, 450)
            val remoteBitmap43 = RemoteImageBitmap.createOffscreenRemoteBitmap(450, 450)
            val sourceBitmap = RemoteImageBitmap.createForId(22)

            canvas.save()
            canvas.translate(21f, 175f)

            canvas.drawToOffscreenBitmap(remoteBitmap42, android.graphics.Color.TRANSPARENT) {
                canvas.save()
                canvas.translate(-12f, -11f)
                canvas.drawBitmap(sourceBitmap, 0f.rf, 0f.rf, Paint())
                canvas.restore()

                canvas.drawToOffscreenBitmap(remoteBitmap43, android.graphics.Color.TRANSPARENT) {
                    canvas.save()
                    canvas.translate(-17f, 0f)
                    canvas.drawRect(0f, 0f, 190f, 190f, Paint())
                    canvas.restore()
                }

                val maskPaint = Paint().apply { blendMode = android.graphics.BlendMode.DST_IN }
                val rect = android.graphics.Rect(0, 0, 450, 450)
                canvas.drawBitmap(remoteBitmap43, rect, rect, maskPaint)
            }

            val rect = android.graphics.Rect(0, 0, 450, 450)
            canvas.drawBitmap(remoteBitmap42, rect, rect, Paint())
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
        val str = RemoteString.createNamedRemoteString("str", "hello")
        val length = str.length

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", false)

        recordingCanvas.drawConditionally(condition1) {
            recordingCanvas.drawRect(length.toRemoteFloat(), 70f.rf, 71f.rf, 72f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            recordingCanvas.drawRect(length.toRemoteFloat(), 73f.rf, 74f.rf, 75f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val str = RemoteString.createNamedRemoteString("a", "12345") + RemoteString("678")
        val length = str.length

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", false)

        recordingCanvas.drawConditionally(condition1) {
            recordingCanvas.drawRect(length.toRemoteFloat(), 76f.rf, 77f.rf, 78f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            recordingCanvas.drawRect(length.toRemoteFloat(), 79f.rf, 80f.rf, 81f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", false)

        recordingCanvas.drawConditionally(condition1) {
            val str = RemoteString.createNamedRemoteString("a", "123") + RemoteString("456")
            recordingCanvas.drawText(str, 3, 0f.rf, 0f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            val str = RemoteString.createNamedRemoteString("a", "123") + RemoteString("456")
            recordingCanvas.drawText(str, 3, 20f.rf, 20f.rf, Paint())
        }

        recordingCanvas.flush()

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
        recordingCanvas.drawConditionally(RemoteBoolean(true)) {
            recordingCanvas.drawRect(0f, 0f, 10f, 10f, Paint())
        }
        recordingCanvas.drawConditionally(RemoteBoolean(false)) {
            recordingCanvas.drawRect(82f, 83f, 84f, 85f, Paint())
        }

        val mSpanTreeRoot = recordingCanvas.buffer.spanTreeRoot

        val child1 = mSpanTreeRoot.child
        assertThat(child1).isNotNull()

        val child2 = child1!!.next
        assertThat(child2).isNotNull()
    }

    @Test
    fun testDrawText() {
        val str = RemoteString.createNamedRemoteString("a", "hello")

        recordingCanvas.drawText(str, 5, 0f.rf, 0f.rf, Paint())

        assertThat(fakeBuffer.calls).isEmpty()

        recordingCanvas.flush()

        // Verify that it calls addDrawTextRun once!
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addPaint",
                "setNamedVariable(42, \"USER:a\", 0)",
                "addDrawTextRun(42)",
            )
    }

    @Test
    fun testDrawText_StringOverload_DoesNotDoubleBuffer() {
        recordingCanvas.drawText("hello", 0f.rf, 0f.rf, Paint())

        assertThat(fakeBuffer.calls).isEmpty()

        // This test guards against double buffering. If drawText calls another buffered method
        // (like drawTextRun) inside its action, that inner method will record a new operation
        // during flush instead of executing drawing calls immediately. This causes the operation
        // to be deferred to the next flush or lost.
        recordingCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly("addPaint", "textData(42, \"hello\")", "addDrawTextRun(42)")
    }

    @Test
    fun testDrawBitmap_DoesNotDoubleBuffer() {
        val bitmap = RemoteImageBitmap.createForId(42)
        recordingCanvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, 10, 10), Paint())

        assertThat(fakeBuffer.calls).isEmpty()

        // This test guards against double buffering of paint operations. If a buffered draw method
        // calls another buffered method (like usePaint) inside its action, the paint operation
        // will be deferred, causing the draw operation to be executed with the wrong paint state.
        recordingCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly("addPaint", "textData(42, \"\")", "addDrawBitmap(42)")
    }

    @Test
    fun testDrawBitmap_RemoteFloat_DoesNotDoubleBuffer() {
        val bitmap = RemoteImageBitmap.createForId(42)
        val left = RemoteFloat.createNamedRemoteFloat("left", 10f)
        val top = RemoteFloat.createNamedRemoteFloat("top", 20f)
        recordingCanvas.drawBitmap(bitmap, left, top, Paint())

        assertThat(fakeBuffer.calls).isEmpty()

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls).contains("addPaint")
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawBitmap") }).isTrue()
    }

    @Test
    fun testDrawTweenPath_PathOverload_DoesNotDoubleBuffer() {
        val path1 = RemotePath().asComposePath()
        val path2 = RemotePath().asComposePath()
        val tween = RemoteFloat.createNamedRemoteFloat("tween", 0.5f)
        val start = RemoteFloat(0f)
        val stop = RemoteFloat(1f)
        recordingCanvas.drawTweenPath(
            path1,
            path2,
            tween,
            start,
            stop,
            androidx.compose.ui.graphics.Paint(),
        )

        assertThat(fakeBuffer.calls).isEmpty()

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls).contains("addPaint")
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawTweenPath") }).isTrue()
    }

    @Test
    fun testDrawTweenPath_RemotePathOverload_DoesNotDoubleBuffer() {
        val path1 = RemotePath()
        val path2 = RemotePath()
        val tween = RemoteFloat.createNamedRemoteFloat("tween", 0.5f)
        val start = RemoteFloat(0f)
        val stop = RemoteFloat(1f)
        recordingCanvas.drawTweenPath(
            path1,
            path2,
            tween,
            start,
            stop,
            androidx.compose.ui.graphics.Paint(),
        )

        assertThat(fakeBuffer.calls).isEmpty()

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls).contains("addPaint")
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawTweenPath") }).isTrue()
    }

    @Test
    fun testIterationSafetyInDiscoverIdealSpans() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)
        val depD = x + y // Dependency D
        val cseB = depD * 2f // Expression B (depends on D)

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)

        // Child Span C (via condition1)
        recordingCanvas.drawConditionally(condition1) {
            // Record cseB in child span C
            recordingCanvas.drawRect(cseB, 86f.rf, 87f.rf, 88f.rf, Paint())
        }

        // Parent Span (Root)
        // Record operation using cseB at root (or in another branch that hoists it to root)
        recordingCanvas.drawConditionally(condition2) {
            recordingCanvas.drawRect(cseB, 89f.rf, 90f.rf, 91f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)
        val cseB = x + y // CSE_B
        val cseA = cseB * 2f // CSE_A (depends on B)

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)

        // Use cseA in two branches to make it common
        recordingCanvas.drawConditionally(condition1) {
            recordingCanvas.drawRect(cseA, 92f.rf, 93f.rf, 94f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            recordingCanvas.drawRect(cseA, 95f.rf, 96f.rf, 97f.rf, Paint())
        }

        recordingCanvas.flush()

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
    fun testDrawBitmapFontTextRun() {
        val str = RemoteString.createNamedRemoteString("str", "hello")
        val bitmapFont = RemoteBitmapFont(emptyList())

        recordingCanvas.drawBitmapFontTextRun(str, bitmapFont, 0, 5, 0f.rf, 0f.rf, 0f.rf, Paint())

        recordingCanvas.flush()

        // Verify that it calls addDrawBitmapFontTextRun!
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addPaint",
                "setNamedVariable(42, \"USER:str\", 0)",
                "addBitmapFont(43)",
                "addDrawBitmapFontTextRun(42, 43)",
            )
    }

    @Test
    fun testExecutionOrderWithHoistedDependency() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)
        val cseB = x + y // CSE_B
        val cseA = cseB * 2f // CSE_A (depends on B)

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)

        recordingCanvas.drawConditionally(condition1) {
            // Use cseB directly in child 1.
            recordingCanvas.drawRect(cseB, 0f.rf, 100f.rf, 100f.rf, Paint())
            // And use cseA.
            recordingCanvas.drawRect(cseA, 10f.rf, 110f.rf, 120f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            // Use cseA in another branch to make it common.
            recordingCanvas.drawRect(cseA, 20f.rf, 130f.rf, 140f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val cseA = x + 1f // CSE_A (Common)
        val nonCommonB = cseA * 2f // Non-common parent (Used only once!)

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)

        recordingCanvas.drawConditionally(condition1) {
            // Use nonCommonB in branch 1.
            recordingCanvas.drawRect(nonCommonB, 0f.rf, 100f.rf, 100f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            // Use cseA directly in branch 2 to make it common!
            recordingCanvas.drawRect(cseA, 20f.rf, 130f.rf, 140f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)
        val expr1 = x + y
        val expr2 = expr1 * 2f

        val counts = androidx.collection.MutableObjectIntMap<RemoteStateCacheKey>()
        val commonOps = mutableSetOf<RemoteOperationCacheKey>()
        val visited = mutableSetOf<RemoteStateCacheKey>()

        recordingCanvas.buffer.traverseCacheKey(expr2.cacheKey, counts, commonOps, visited)

        // Guard against double counting in CSE Pass 0.
        assertThat(counts.getOrDefault(expr1.cacheKey, 0)).isEqualTo(1)
        assertThat(counts.getOrDefault(x.cacheKey, 0)).isEqualTo(1)
        assertThat(counts.getOrDefault(y.cacheKey, 0)).isEqualTo(1)
        assertThat(counts.getOrDefault(expr2.cacheKey, 0)).isEqualTo(0)
    }

    @Test
    fun testRemoteFloatToRemoteStringHoisted() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)

        recordingCanvas.drawConditionally(condition1) {
            val str = x.toRemoteString(java.text.DecimalFormat("#0"))
            recordingCanvas.drawText(str, 5, 10f.rf, 10f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            val str = x.toRemoteString(java.text.DecimalFormat("#0"))
            recordingCanvas.drawText(str, 5, 20f.rf, 20f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val x = RemoteInt.createNamedRemoteInt("x", 10)

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)

        recordingCanvas.drawConditionally(condition1) {
            val str = x.toRemoteString(java.text.DecimalFormat("#0"))
            recordingCanvas.drawText(str, 5, 10f.rf, 10f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            val str = x.toRemoteString(java.text.DecimalFormat("#0"))
            recordingCanvas.drawText(str, 5, 20f.rf, 20f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val cond = RemoteBoolean.createNamedRemoteBoolean("cond", true)
        val str1 = RemoteString.createNamedRemoteString("s1", "hello")
        val str2 = RemoteString.createNamedRemoteString("s2", "world")

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)

        recordingCanvas.drawConditionally(condition1) {
            val selected = cond.select(str1, str2)
            recordingCanvas.drawText(selected, 5, 10f.rf, 10f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            val selected = cond.select(str1, str2)
            recordingCanvas.drawText(selected, 5, 20f.rf, 20f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val str = RemoteString.createNamedRemoteString("str", "hello world")

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)

        recordingCanvas.drawConditionally(condition1) {
            val sub = str.substring(6)
            recordingCanvas.drawText(sub, 5, 10f.rf, 10f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            val sub = str.substring(6)
            recordingCanvas.drawText(sub, 5, 20f.rf, 20f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val str = RemoteString.createNamedRemoteString("str", "hello")

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)

        recordingCanvas.drawConditionally(condition1) {
            val upper = str.uppercase()
            recordingCanvas.drawText(upper, 5, 10f.rf, 10f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            val upper = str.uppercase()
            recordingCanvas.drawText(upper, 5, 20f.rf, 20f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val str = RemoteString.createNamedRemoteString("str", " hello ")

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)

        recordingCanvas.drawConditionally(condition1) {
            val trimmed = str.trim()
            recordingCanvas.drawText(trimmed, 5, 10f.rf, 10f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            val trimmed = str.trim()
            recordingCanvas.drawText(trimmed, 5, 20f.rf, 20f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)
        val str1 = RemoteString.createNamedRemoteString("s1", "hello")
        val str2 = RemoteString.createNamedRemoteString("s2", "world")

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)

        recordingCanvas.drawConditionally(condition1) {
            val selected = selectIfLt(x, y, str1, str2)
            recordingCanvas.drawText(selected, 5, 10f.rf, 10f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            val selected = selectIfLt(x, y, str1, str2)
            recordingCanvas.drawText(selected, 5, 20f.rf, 20f.rf, Paint())
        }

        recordingCanvas.flush()

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
        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)
        val str1 = RemoteString.createNamedRemoteString("s1", "hello")
        val str2 = RemoteString.createNamedRemoteString("s2", "world")

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)

        recordingCanvas.drawConditionally(condition1) {
            val selected = selectIfGt(x, y, str1, str2)
            recordingCanvas.drawText(selected, 5, 10f.rf, 10f.rf, Paint())
        }

        recordingCanvas.drawConditionally(condition2) {
            val selected = selectIfGt(x, y, str1, str2)
            recordingCanvas.drawText(selected, 5, 20f.rf, 20f.rf, Paint())
        }

        recordingCanvas.flush()

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
    fun testHoisting_3LevelsDeep() {
        val x = RemoteFloat.createNamedRemoteFloat("x", 10f)
        val y = RemoteFloat.createNamedRemoteFloat("y", 20f)
        val z = RemoteFloat.createNamedRemoteFloat("z", 30f)

        val condition1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
        val condition2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)

        recordingCanvas.drawConditionally(condition1) {
            // Level 1
            val sub2 = x + z

            recordingCanvas.drawConditionally(condition2) {
                // Level 2 (Leaf)
                val sub1 = x + y
                val sub2_dup = x + z
                val sub3 = y + z // Leaf only!

                recordingCanvas.drawRect(sub1, 10f.rf, 10f.rf, 10f.rf, Paint())
                recordingCanvas.drawRect(sub2_dup, 20f.rf, 20f.rf, 20f.rf, Paint())
                recordingCanvas.drawRect(sub3, 30f.rf, 30f.rf, 30f.rf, Paint())
            }

            recordingCanvas.drawRect(sub2, 40f.rf, 40f.rf, 40f.rf, Paint())
        }

        // Level 0
        val sub1_dup = x + y
        recordingCanvas.drawRect(sub1_dup, 50f.rf, 50f.rf, 50f.rf, Paint())

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "setNamedVariable(42, \"USER:x\", 1)",
                "setNamedVariable(43, \"USER:y\", 1)",
                "addAnimatedFloat(44) = ([42] [43] + )",
                "setNamedVariable(45, \"USER:cond1\", 4)",
                "addConditionalOperations(1, ID(45), 0.0)",
                "setNamedVariable(46, \"USER:z\", 1)",
                "addAnimatedFloat(47) = ([42] [46] + )",
                "setNamedVariable(48, \"USER:cond2\", 4)",
                "addConditionalOperations(1, ID(48), 0.0)",
                "addPaint",
                "addDrawRect(ID(44), 10.0, 10.0, 10.0)",
                "addDrawRect(ID(47), 20.0, 20.0, 20.0)",
                "addAnimatedFloat(49) = ([43] [46] + )",
                "addDrawRect(ID(49), 30.0, 30.0, 30.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "addPaint",
                "addDrawRect(ID(47), 40.0, 40.0, 40.0)",
                "endConditionalOperations",
                "addContainerEnd",
                "addPaint",
                "addDrawRect(ID(44), 50.0, 50.0, 50.0)",
            )
    }

    @Test
    fun testOrderingChain_ClipRect() {
        recordingCanvas.drawRect(0f, 0f, 10f, 10f, Paint())
        recordingCanvas.clipRect(0f, 0f, 5f, 5f)
        recordingCanvas.drawRect(0f, 0f, 10f, 10f, Paint())

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addPaint",
                "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                "addClipRect(0.0, 0.0, 5.0, 5.0)",
                "addDrawRect(0.0, 0.0, 10.0, 10.0)",
            )
    }

    @Test
    fun testUnbufferedUsePaint_Bug() {
        val paint1 = Paint().apply { color = 0xFFFF0000.toInt() }
        val paint2 = Paint().apply { color = 0xFF0000FF.toInt() }

        recordingCanvas.drawRect(0f, 0f, 10f, 10f, paint1)
        recordingCanvas.usePaint(paint2)
        recordingCanvas.drawRect(0f, 0f, 10f, 10f, paint2)

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addPaint",
                "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                "addPaint",
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
        action: (RecordingCanvas, OptimizingTestRemoteComposeBuffer) -> Unit
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
            RecordingCanvas(
                Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888),
                enableOptimizations = true,
            )
        localCanvas.setRemoteComposeCreationState(localCreationState)

        action(localCanvas, optimizingBuffer)
    }

    @Test
    fun testRedundantSaveRestoreElimination() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.save()
            canvas.translate(10f, 20f)
            canvas.restore()

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls).isEmpty()
        }
    }

    @Test
    fun testTransformsOptimizedAndRecordedInChildSpans() {
        runWithOptimizingCanvas { canvas, buffer ->
            val condition = RemoteBoolean.createNamedRemoteBoolean("cond", true)
            canvas.drawConditionally(condition) {
                canvas.save()
                canvas.translate(20f, 20f)
                canvas.drawRect(0f, 0f, 50f, 50f, Paint())
                canvas.restore()
            }

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls).contains("addMatrixTranslate(20.0, 20.0)")
        }
    }

    @Test
    fun testTransformCancellation() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.translate(10f, 20f)
            canvas.translate(-10f, -20f)
            canvas.drawRect(0f, 0f, 50f, 50f, Paint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly("addPaint", "addDrawRect(0.0, 0.0, 50.0, 50.0)")
        }
    }

    @Test
    fun testScaleCancellation() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.scale(2f, 4f)
            canvas.scale(0.5f, 0.25f)
            canvas.drawRect(0f, 0f, 50f, 50f, Paint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly("addPaint", "addDrawRect(0.0, 0.0, 50.0, 50.0)")
        }
    }

    @Test
    fun testRotateCancellation() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.rotate(45f)
            canvas.rotate(-45f)
            canvas.drawRect(0f, 0f, 50f, 50f, Paint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly("addPaint", "addDrawRect(0.0, 0.0, 50.0, 50.0)")
        }
    }

    @Test
    fun testSkewCancellation() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.skew(0.5f, 0f)
            canvas.skew(-0.5f, 0f)
            canvas.drawRect(0f, 0f, 50f, 50f, Paint())

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls)
                .containsExactly("addPaint", "addDrawRect(0.0, 0.0, 50.0, 50.0)")
        }
    }

    @Test
    fun testIntermediateTransformDependenciesAreReplaced() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.translate(10f, 20f)
            val t1SpanOp = canvas.buffer.lastRenderingOp!!
            canvas.translate(30f, 40f)
            canvas.drawRect(0f, 0f, 50f, 50f, Paint())

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
            canvas.translate(10f, 20f)
            canvas.translate(30f, 40f)
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())

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
            canvas.translate(10f, 20f)
            canvas.scale(2f, 3f)
            canvas.translate(5f, 10f)
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())

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
            canvas.rotate(90f)
            canvas.translate(10f, 20f)
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())

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
            canvas.translate(10f, 20f)
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())
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
            canvas.translate(10f, 20f)
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())
            canvas.restore()
            canvas.drawRect(0f, 0f, 5f, 5f, Paint())

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
            canvas.translate(10f, 20f)

            canvas.save()
            canvas.translate(30f, 40f)
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())
            canvas.restore()

            canvas.restore()
            canvas.drawRect(0f, 0f, 5f, 5f, Paint())

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
            canvas.translate(10f, 20f)
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())
            canvas.restore() // Restore B
            canvas.restore() // Restore A

            canvas.drawRect(0f, 0f, 5f, 5f, Paint()) // Draw C

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
            canvas.rotate(45f)
            canvas.rotate(90f)
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())

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
            canvas.clipRect(0f, 0f, 10f, 10f)
            canvas.restore()

            canvas.flush()
            canvas.document.encodeToByteArray()

            assertThat(buffer.calls).isEmpty()
        }
    }

    @Test
    fun testClipBarrier() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.translate(10f, 20f)
            canvas.clipRect(0f, 0f, 50f, 50f)
            canvas.translate(30f, 40f)
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())

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
            val varA = RemoteFloat.createNamedRemoteFloat("varA", 0f)
            val varB = RemoteFloat.createNamedRemoteFloat("varB", 0f)

            canvas.translate(10f, 20f)
            canvas.translate(varA, varB)
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())

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
            val varAngle = RemoteFloat.createNamedRemoteFloat("varAngle", 0f)

            canvas.rotate(10f)
            canvas.rotate(varAngle)
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())

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
            val varSx = RemoteFloat.createNamedRemoteFloat("varSx", 1f)
            val varSy = RemoteFloat.createNamedRemoteFloat("varSy", 1f)

            canvas.scale(2f, 3f)
            canvas.scale(varSx, varSy)
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())

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
            val condition = RemoteBoolean.createNamedRemoteBoolean("cond", true)
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
            val condition = RemoteBoolean.createNamedRemoteBoolean("cond", true)
            canvas.drawConditionally(condition) { canvas.drawRect(0f, 0f, 10f, 10f, Paint()) }

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
            val cond1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
            val cond2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)
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
            val cond1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
            val cond2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)
            val expr = cond1 and cond2

            canvas.drawConditionally(expr) { canvas.drawRect(0f, 0f, 10f, 10f, Paint()) }
            canvas.drawConditionally(expr) { canvas.drawRect(10f, 10f, 20f, 20f, Paint()) }

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
            val cond1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
            val cond2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)
            val expr = cond1 and cond2

            // First: convert expr to RemoteString and drawText (preserves expr)
            val str = expr.select(RemoteString("true"), RemoteString("false"))
            canvas.drawText(str, 5, 10f.rf, 10f.rf, Paint())

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
            val cond1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
            val cond2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)
            val expr = cond1 and cond2

            // First: empty conditional block using expr (elided)
            canvas.drawConditionally(expr) {
                // Empty block, elided
            }

            // Second: convert expr to RemoteString and drawText (preserves expr)
            val str = expr.select(RemoteString("true"), RemoteString("false"))
            canvas.drawText(str, 5, 20f.rf, 20f.rf, Paint())

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
            val condition = RemoteBoolean.createNamedRemoteBoolean("cond", true)
            canvas.drawConditionally(condition) {
                canvas.save()
                canvas.translate(10f, 20f)
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
            val condition = RemoteBoolean.createNamedRemoteBoolean("cond", true)
            canvas.save()
            canvas.translate(10f, 20f)
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
            val cond1 = RemoteBoolean.createNamedRemoteBoolean("cond1", true)
            val cond2 = RemoteBoolean.createNamedRemoteBoolean("cond2", true)
            val cond3 = RemoteBoolean.createNamedRemoteBoolean("cond3", true)

            // Child span 1: elided
            canvas.drawConditionally(cond1) {
                canvas.save()
                canvas.restore()
            }
            // Child span 2: preserved
            canvas.drawConditionally(cond2) { canvas.drawRect(0f, 0f, 10f, 10f, Paint()) }
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
        val condition = RemoteBoolean.createNamedRemoteBoolean("myCond", true)
        val span = CanvasOperationBuffer.Span(null, 0)
        val op = CanvasOp.DrawConditionally(condition, span) { _, _ -> }
        assertThat(op.toString()).isEqualTo("DrawConditionally(${condition.toDebugString()})")
    }

    @Test
    fun testSaveTransformBeforeDrawConditionally_preserved() {
        runWithOptimizingCanvas { canvas, buffer ->
            val condition = RemoteBoolean.createNamedRemoteBoolean("cond", true)
            // Save block with transforms right before a conditional draw
            canvas.save()
            canvas.translate(10f, 20f)
            canvas.drawRect(1f, 1f, 2f, 2f, Paint())
            canvas.restore()

            canvas.drawConditionally(condition) { canvas.drawRect(0f, 0f, 10f, 10f, Paint()) }

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
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())

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
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())

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
            val varPx = RemoteFloat.createNamedRemoteFloat("varPx", 5f)
            val varPy = RemoteFloat.createNamedRemoteFloat("varPy", 5f)

            canvas.scale(2f.rf, 3f.rf, varPx, varPy)
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())

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
            canvas.translate(10f, 20f)
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())

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
            canvas.translate(10f, 10f)
            canvas.restore()
            canvas.drawRect(0f, 0f, 5f, 5f, Paint())

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
            canvas.translate(10f, 10f)
            canvas.clipRect(0f, 0f, 10f, 10f)
            canvas.drawRect(0f, 0f, 5f, 5f, Paint())
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
    fun testConsecutiveSkewsAreNotFused() {
        runWithOptimizingCanvas { canvas, buffer ->
            canvas.skew(1f, 0f)
            canvas.skew(0f, 1f)
            canvas.drawRect(0f, 0f, 5f, 5f, Paint())

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
            val paint1 = Paint().apply { color = android.graphics.Color.RED }
            val paint2 = Paint().apply { color = android.graphics.Color.BLUE }
            canvas.save()
            canvas.drawRect(0f, 0f, 5f, 5f, paint1)
            canvas.restore()
            canvas.drawRect(10f, 10f, 15f, 15f, paint2) // Drawing after!

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
        val localCanvas = RecordingCanvas(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
        localCanvas.setRemoteComposeCreationState(localCreationState)

        val calls = ArrayList<String>()
        val testBuffer = OptimizingTestRemoteComposeBuffer(calls)
        val optimizingWriter = TestRemoteComposeWriter(RcPlatformProfiles.ANDROIDX, testBuffer)
        localCreationState.document = optimizingWriter

        localCanvas.save()
        localCanvas.translate(10f, 20f)
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
        recordingCanvas.save()
        recordingCanvas.translate(-87f, -87f)
        assertThat(recordingCanvas.saveCount).isEqualTo(1)

        // 2. Temporarily pop transforms before a conditional draw: call restore().
        recordingCanvas.restore()
        assertThat(recordingCanvas.saveCount).isEqualTo(0)

        // 3. Call drawConditionally(condition) { drawBitmap(...) } where condition is dynamic.
        val condition = RemoteBoolean.createNamedRemoteBoolean("cond", true)
        val bitmap = RemoteImageBitmap.createForId(42)
        recordingCanvas.drawConditionally(condition) {
            recordingCanvas.drawBitmap(bitmap, 0f.rf, 0f.rf, Paint())
        }
        assertThat(recordingCanvas.saveCount).isEqualTo(0)

        // 4. Reinstate transforms after the conditional draw: call save() and translate(-87f,
        // -87f).
        recordingCanvas.save()
        recordingCanvas.translate(-87f, -87f)
        assertThat(recordingCanvas.saveCount).isEqualTo(1)

        // 5. Call flush() and verify the recorded operations on the underlying buffer.
        recordingCanvas.flush()
        assertThat(recordingCanvas.saveCount).isEqualTo(1)

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
            canvas.translate(-87f, -87f)
            assertThat(canvas.saveCount).isEqualTo(1)

            // 2. Temporarily pop transforms before a conditional draw: call restore().
            canvas.restore()
            assertThat(canvas.saveCount).isEqualTo(0)

            // 3. Call drawConditionally(condition) { drawBitmap(...) } where condition is dynamic.
            val condition = RemoteBoolean.createNamedRemoteBoolean("cond", true)
            val bitmap = RemoteImageBitmap.createForId(42)
            canvas.drawConditionally(condition) { canvas.drawBitmap(bitmap, 0f.rf, 0f.rf, Paint()) }
            assertThat(canvas.saveCount).isEqualTo(0)

            // 4. Reinstate transforms after the conditional draw: call save() and translate(-87f,
            // -87f).
            canvas.save()
            canvas.translate(-87f, -87f)
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

            val condition = RemoteBoolean.createNamedRemoteBoolean("cond", true)
            canvas.drawConditionally(condition) { canvas.translate(10f, 10f) }

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
            canvas.drawToOffscreenBitmap(offscreenBitmap) { canvas.translate(5f, 5f) }

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
            canvas.loop(from, until, step) { canvas.translate(5f, 5f) }

            val opAfterLoop = canvas.buffer.lastRenderingOp
            assertThat(opAfterLoop).isEqualTo(prevLastOp)
        }
    }

    @Test
    fun testTransformPoppingAndReinstatement_preservesWireOrderAndSaveStack() {
        runWithOptimizingCanvas { canvas, buffer ->
            // Step 1: Push initial group transform (simulating ancestor group translation)
            canvas.save()
            canvas.translate(-87f, -87f)
            // Add a draw call so optimizing canvas preserves this transform block
            canvas.drawRect(0f, 0f, 10f, 10f, Paint())
            assertThat(canvas.globalSaveCounter).isEqualTo(1)

            // Step 2: Temporarily pop transforms before drawing span
            canvas.restore()
            assertThat(canvas.globalSaveCounter).isEqualTo(0)

            // Step 3: Draw span inside a conditional (using a dynamic boolean condition)
            val dynamicCondition = RemoteBoolean.createNamedRemoteBoolean("test", true)
            canvas.drawConditionally(dynamicCondition) {
                val offscreenBitmap = RemoteImageBitmap.createOffscreenRemoteBitmap(450, 450)
                canvas.drawBitmap(offscreenBitmap, 0f.rf, 0f.rf, null)
            }

            // Step 4: Reinstate group transform after conditional span
            canvas.save()
            canvas.translate(-87f, -87f)
            // Add a draw call so optimizing canvas preserves this reinstated transform block
            canvas.drawRect(20f, 20f, 30f, 30f, Paint())
            canvas.restore()
            assertThat(canvas.globalSaveCounter).isEqualTo(0)

            // Step 4b: Draw something after the reinstated transform so the save block is preserved
            // by elision pass
            canvas.drawRect(40f, 40f, 50f, 50f, Paint())

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
    fun testDrawColor() {
        recordingCanvas.drawColor(android.graphics.Color.RED)
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawRect") }).isTrue()
    }

    @Test
    fun testDrawOvalOverloads() {
        recordingCanvas.drawOval(0f, 0f, 100f, 100f, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawOval") }).isTrue()

        fakeBuffer.calls.clear()
        recordingCanvas.drawOval(0f.rf, 0f.rf, 100f.rf, 100f.rf, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawOval") }).isTrue()
    }

    @Test
    fun testDrawRoundRectOverloads() {
        recordingCanvas.drawRoundRect(0f, 0f, 100f, 100f, 10f, 10f, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawRoundRect") }).isTrue()

        fakeBuffer.calls.clear()
        recordingCanvas.drawRoundRect(0f.rf, 0f.rf, 100f.rf, 100f.rf, 10f.rf, 10f.rf, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawRoundRect") }).isTrue()
    }

    @Test
    fun testDrawCircleOverloads() {
        recordingCanvas.drawCircle(50f, 50f, 25f, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawCircle") }).isTrue()

        fakeBuffer.calls.clear()
        recordingCanvas.drawCircle(50f.rf, 50f.rf, 25f.rf, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawCircle") }).isTrue()
    }

    @Test
    fun testDrawArcOverloads() {
        recordingCanvas.drawArc(0f, 0f, 100f, 100f, 0f, 90f, false, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawArc") }).isTrue()

        fakeBuffer.calls.clear()
        recordingCanvas.drawArc(0f.rf, 0f.rf, 100f.rf, 100f.rf, 0f.rf, 90f.rf, true, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawSector") }).isTrue()
    }

    @Test
    fun testDrawPathAndDrawRPath() {
        val path =
            android.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(10f, 10f)
            }
        recordingCanvas.drawPath(path, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawPath") }).isTrue()

        fakeBuffer.calls.clear()
        val rPath =
            RemotePath().apply {
                moveTo(0f, 0f)
                lineTo(10f, 10f)
            }
        recordingCanvas.drawRPath(rPath, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawPath") }).isTrue()
    }

    @Test
    fun testDrawRoundedPolygonAndMorph() {
        val poly1 = RoundedPolygon(numVertices = 4)
        recordingCanvas.drawRoundedPolygon(poly1, null)
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawPath") }).isTrue()

        fakeBuffer.calls.clear()
        val poly2 = RoundedPolygon(numVertices = 4)
        recordingCanvas.drawRoundedPolygonMorph(poly1, poly2, 0.5f.rf, null)
        recordingCanvas.flush()
        assertThat(
                fakeBuffer.calls.any { it.startsWith("pathTween") || it.startsWith("addDrawPath") }
            )
            .isTrue()
    }

    @Test
    fun testDrawScaledBitmap() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        recordingCanvas.drawScaledBitmap(
            bitmap,
            0f.rf,
            0f.rf,
            10f.rf,
            10f.rf,
            0f.rf,
            0f.rf,
            100f.rf,
            100f.rf,
            0,
            1f.rf,
            null,
        )
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("drawScaledBitmap") }).isTrue()
    }

    @Test
    fun testDrawTextRunAndSpecializedText() {
        val str = "hello"
        recordingCanvas.drawTextRun(str, 0, 5, 0, 5, 10f, 10f, false, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawTextRun") }).isTrue()

        fakeBuffer.calls.clear()
        val path =
            android.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(100f, 100f)
            }
        recordingCanvas.drawTextOnPath(str, path, 0f, 0f, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawTextOnPath") }).isTrue()
    }

    @Test
    fun testClipRectOverloadsAndBounds() {
        recordingCanvas.clipRect(0f.rf, 0f.rf, 10f.rf, 10f.rf)
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addClipRect") }).isTrue()

        fakeBuffer.calls.clear()
        recordingCanvas.clipRect(android.graphics.Rect(0, 0, 5, 5))
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addClipRect") }).isTrue()

        val bounds = android.graphics.Rect()
        assertThat(recordingCanvas.getClipBounds(bounds)).isTrue()
        assertThat(bounds).isEqualTo(android.graphics.Rect(0, 0, 2048, 2048))
    }

    @Test
    fun testUnclosedSaveInChildSpanDoesNotCorruptOuterRestoreToCount() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = RecordingCanvas(bitmap, enableOptimizations = false)

        val outerSaveCount = canvas.save()
        assertEquals(1, canvas.globalSaveCounter)

        // Inside child span, perform an unbalanced save (no restore)
        val condition = RemoteBoolean(true)
        canvas.drawConditionally(condition) {
            canvas.save() // This increments globalSaveCounter temporarily from 1 to 2
            // No restore called here inside child span
        }

        // After recordInChildSpan completes, globalSaveCounter is reverted back to 1.
        // Calling restoreToCount(outerSaveCount) leaves the outer save intact.
        canvas.restoreToCount(outerSaveCount)

        assertEquals(1, canvas.globalSaveCounter)
        canvas.restore()
        assertEquals(0, canvas.globalSaveCounter)
    }

    @Test
    fun testCrossSpanRestoreThrowsIllegalStateException() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = RecordingCanvas(bitmap, enableOptimizations = false)

        // 1. Push 2 saves on outer span
        canvas.save()
        canvas.save()
        assertEquals(2, canvas.globalSaveCounter)

        // 2. Enter child span via drawConditionally
        val dummyCondition = RemoteBoolean(true)
        canvas.drawConditionally(dummyCondition) {
            // Inside childSpan, localSpanSaveCounter is 0, but globalSaveCounter is 2.
            // Push 1 local save inside childSpan
            canvas.save()
            assertEquals(1, canvas.localSpanSaveCounter)
            assertEquals(3, canvas.globalSaveCounter)

            // Pop local save
            canvas.restore()
            assertEquals(0, canvas.localSpanSaveCounter)
            assertEquals(2, canvas.globalSaveCounter)

            // Attempting to pop outer parent save across span boundary must throw
            // IllegalStateException
            // to prevent unbalanced wire commands on the remote player stack.
            assertThrows(IllegalStateException::class.java) { canvas.restore() }
        }

        // 3. Pop outer saves on main canvas
        canvas.restore()
        canvas.restore()
        assertEquals(0, canvas.globalSaveCounter)

        // 4. Global underflow on main canvas must throw IllegalStateException
        assertThrows(IllegalStateException::class.java) { canvas.restore() }
    }
}

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

private class OptimizingTestRemoteComposeBuffer(calls: ArrayList<String> = ArrayList()) :
    RecordingTestRemoteComposeBuffer(calls)
