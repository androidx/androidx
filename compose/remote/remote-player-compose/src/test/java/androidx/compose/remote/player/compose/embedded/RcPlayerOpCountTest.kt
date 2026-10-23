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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.FloatConstant
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloatExpression
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerOpCountTest {

    @get:Rule val rule = RcPlayerTestRule()

    private var savedMaxOpCount: Int = Limits.MAX_OP_COUNT

    @Before
    fun setUp() {
        savedMaxOpCount = Limits.MAX_OP_COUNT
    }

    @After
    fun tearDown() {
        Limits.MAX_OP_COUNT = savedMaxOpCount
    }

    private fun buildDocumentFromBuffer(block: RemoteComposeBuffer.() -> Unit): CoreDocument {
        val remoteComposeBuffer = RemoteComposeBuffer()
        remoteComposeBuffer.addHeader(
            shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT),
            arrayOf<Any>(200, 200),
        )
        remoteComposeBuffer.block()
        val wire = remoteComposeBuffer.buffer
        val bytes = wire.buffer.copyOf(wire.size())
        return CoreDocument(RemoteClock.SYSTEM).apply {
            ByteArrayInputStream(bytes).use {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }
    }

    private fun RcPlayerState.remoteContext(): AndroidRemoteContext =
        inspectableElements.first { it.name == "remoteContext" }.value as AndroidRemoteContext

    @Test
    fun applyOperationsWithoutBitmaps_doesNotRecurseIntoFunctionDefineOrLoopOperation() {
        val fnId = 10
        val argId = 11
        val fnOutId = 12
        val loopIndexId = 20
        val loopOutId = 21
        val topLevelId = 30
        val nestedBitmapId = 40

        val document = buildDocumentFromBuffer {
            FloatConstant.apply(buffer, topLevelId, 42f)

            defineFloatFunction(fnId, intArrayOf(argId))
            addAnimatedFloat(fnOutId, floatArrayOf(999f), null)
            BitmapData.apply(buffer, nestedBitmapId, 1, 1, ByteArray(4))
            addEndFloatFunctionDef()

            addLoopStart(loopIndexId, 0f, 1f, 50f)
            FloatConstant.apply(buffer, loopOutId, 777f)
            addLoopEnd()
        }

        // Only top-level operations (Header/Root, FloatConstant, FloatFunctionDefine,
        // LoopOperation) are counted during setup; if the 50 loop iterations or function body
        // operations were traversed, Limits.MAX_OP_COUNT = 10 would throw.
        Limits.MAX_OP_COUNT = 10
        val playerState = RcPlayerState(document)
        val ctx = playerState.remoteContext()

        // FloatFunctionDefine registered the function definition object on the context.
        assertThat(ctx.getObject(fnId)).isInstanceOf(FloatFunctionDefine::class.java)
        // Nested BitmapData metadata is still registered for lazy decoding.
        assertThat(ctx.getObject(nestedBitmapId)).isInstanceOf(BitmapData::class.java)
        // Top-level constant was applied.
        assertThat(ctx.getFloat(topLevelId)).isEqualTo(42f)
        // Function body and loop body operations were NOT prematurely evaluated during setup.
        assertThat(ctx.getFloat(fnOutId)).isNotEqualTo(999f)
        assertThat(ctx.getFloat(loopOutId)).isNotEqualTo(777f)
    }

    @Test
    fun initializePlayerRemoteContext_resetsOpCountAfterSetup() {
        val fnId = 10
        val argId = 11
        val fnOutId = 12
        val loopIndexId = 20

        val document = buildDocumentFromBuffer {
            defineFloatFunction(fnId, intArrayOf(argId))
            for (i in 0 until 20) {
                addAnimatedFloat(fnOutId + i, floatArrayOf(i.toFloat()), null)
            }
            addEndFloatFunctionDef()

            addRootStart()
            addCanvasStart(1, 0)
            addCanvasContentStart(2)
            addLoopStart(loopIndexId, 0f, 1f, 50f)
            callFloatFunction(fnId, floatArrayOf(1f))
            addDrawCircle(50f, 50f, 10f)
            addLoopEnd()
            addContainerEnd()
            addContainerEnd()
            addContainerEnd()
        }

        val playerState = RcPlayerState(document)
        val ctx = playerState.remoteContext()

        // After player context initialization finishes, mOpCount must be reset to 0.
        assertThat(ctx.getLastOpCount()).isEqualTo(0)
    }

    @Test
    fun multiFrameCanvasExecution_resetsOpCountPerFrame_andDoesNotAccumulateBeyondMaxOpCount() {
        // Set a tight per-frame cap: 400 operations per frame.
        // Each frame executes a LoopOperation of 40 iterations x (AnimatedFloat expressions +
        // DrawCircle) = ~165 operations per frame, which fits within 400 for a single frame,
        // but across 10 frames totals ~1,650 operations (> 4x the limit if not reset per frame).
        Limits.MAX_OP_COUNT = 400

        val context: Context = ApplicationProvider.getApplicationContext()
        val captured = runBlocking {
            captureSingleRemoteDocument(context = context) {
                val trigger = remember { createNamedRemoteFloatExpression("trigger") { 1f.rf } }
                RemoteBox(modifier = RemoteModifier.fillMaxSize()) {
                    RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                        val paint = RemotePaint()
                        loop(0f.rf, 1f.rf, 40f.rf) { index ->
                            val x = (index * 2f) + 1f
                            drawCircle(
                                paint = paint,
                                center = RemoteOffset(x, trigger),
                                radius = 5f.rf,
                            )
                        }
                    }
                }
            }
        }

        val playerState = RcPlayerState(captured)
        val ctx = playerState.remoteContext()
        rule.setContent { RcPlayer(state = playerState, modifier = Modifier.size(200.dp)) }

        rule.onRoot().captureToImage()
        assertThat(ctx.getLastOpCount()).isEqualTo(0)

        // Drive 10 consecutive frame redraws by updating the reactive state variable and capturing.
        for (frame in 1..10) {
            playerState.floatState("trigger").value = frame.toFloat() * 2f
            rule.onRoot().captureToImage()
            assertThat(ctx.getLastOpCount()).isEqualTo(0)
        }
    }

    @Test
    fun singleFrameExceedingMaxOpCount_throwsTooManyOperationsExecuted_andResetsCountInFinally() {
        Limits.MAX_OP_COUNT = 100

        val loopIndexId = 20
        val document = buildDocumentFromBuffer {
            addLoopStart(loopIndexId, 0f, 1f, 60f)
            addDrawCircle(10f, 10f, 5f)
            addDrawCircle(20f, 20f, 5f)
            addLoopEnd()
        }

        val playerState = RcPlayerState(document)
        val ctx = playerState.remoteContext()

        var caughtError: RuntimeException? = null
        rule.setContent {
            Box(
                modifier =
                    Modifier.size(100.dp).drawWithContent {
                        caughtError = assertFailsWith<RuntimeException> { drawContent() }
                    }
            ) {
                RcPlayer(state = playerState, modifier = Modifier.size(100.dp))
            }
        }
        rule.onRoot().captureToImage()

        assertThat(caughtError).isNotNull()
        assertThat(caughtError!!.message).contains("Too many operations executed")
        // Even when a frame aborts due to exceeding MAX_OP_COUNT, the finally block must reset
        // mOpCount to 0 so the context is not left poisoned.
        assertThat(ctx.getLastOpCount()).isEqualTo(0)
    }
}
