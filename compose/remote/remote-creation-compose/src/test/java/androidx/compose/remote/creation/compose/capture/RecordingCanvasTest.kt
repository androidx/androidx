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
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteBitmapFont
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteOperationCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.selectIfGt
import androidx.compose.remote.creation.compose.state.selectIfLt
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
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
                "addDrawRect(ID(43), 63.0, 64.0, 65.0)",
                "addLoopEnd",
            )
    }

    @Test
    fun testDrawToOffscreenBitmapBuffered() {
        val dummyBitmap =
            object :
                RemoteBitmap(
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
                RemoteBitmap(
                    null,
                    RemoteOperationCacheKey(DummyEnum.VALUE, emptyList<RemoteStateCacheKey>()),
                ) {

                override fun writeToDocument(creationState: RemoteComposeCreationState): Int = 100

                override fun getIdForCreationState(creationState: RemoteComposeCreationState): Int =
                    100
            }

        val innerBitmap =
            object :
                RemoteBitmap(
                    null,
                    RemoteOperationCacheKey(DummyEnum.VALUE, emptyList<RemoteStateCacheKey>()),
                ) {

                override fun writeToDocument(creationState: RemoteComposeCreationState): Int = 200

                override fun getIdForCreationState(creationState: RemoteComposeCreationState): Int =
                    200
            }

        recordingCanvas.drawToOffscreenBitmap(outerBitmap, android.graphics.Color.TRANSPARENT) {
            recordingCanvas.save()

            recordingCanvas.drawToOffscreenBitmap(innerBitmap, android.graphics.Color.TRANSPARENT) {
                recordingCanvas.drawRect(0f, 0f, 10f, 10f, Paint())
            }

            recordingCanvas.restore()
        }

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "drawOnBitmap(100, 0, 0)",
                "addMatrixSave",
                "drawOnBitmap(200, 0, 0)",
                "addPaint",
                "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                "drawOnBitmap(100, 1, 0)",
                "addMatrixRestore",
                "drawOnBitmap(0, 1, 0)",
            )
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
        val bitmap = RemoteBitmap.createForId(42)
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
        val bitmap = RemoteBitmap.createForId(42)
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
}
