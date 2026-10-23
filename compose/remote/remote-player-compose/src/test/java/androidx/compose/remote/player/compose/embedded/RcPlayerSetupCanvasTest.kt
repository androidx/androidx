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
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Document setup in [RcPlayerState] must not evaluate a layout component's canvas draw stream (e.g.
 * `drawWithContent`) before layout: the stream runs on every draw pass, after measurement, so setup
 * only registers the resources it reads from the store.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "mdpi")
class RcPlayerSetupCanvasTest {

    @get:Rule val rule = RcPlayerTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var savedMaxOpCount: Int = Limits.MAX_OP_COUNT

    @Before
    fun setUp() {
        savedMaxOpCount = Limits.MAX_OP_COUNT
    }

    @After
    fun tearDown() {
        Limits.MAX_OP_COUNT = savedMaxOpCount
    }

    @Test
    fun setup_doesNotEvaluateSizeDependentCanvasExpressions() {
        val document = capture {
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp).drawWithContent {
                        // A 50px wide rect once measured. Evaluated before layout, against the
                        // unmeasured component size, the right edge would be negative.
                        drawRect(
                            paint = RemotePaint { color = RemoteColor(Color.Red) },
                            topLeft = RemoteOffset(0f.rf, 0f.rf),
                            size = RemoteSize(width - 50f, height),
                        )
                        drawContent()
                    }
            ) {}
        }
        val canvasExpressionIds =
            canvasOperations(document).filterIsInstance<FloatExpression>().map { it.mId }
        assertThat(canvasExpressionIds).isNotEmpty()

        val state = RcPlayerState(document)
        val ctx = state.remoteContext()

        for (id in canvasExpressionIds) {
            assertWithMessage("Canvas expression $id after setup")
                .that(ctx.getFloat(id))
                .isAtLeast(0f)
        }

        // The first frame still resolves the stream against the measured size.
        rule.setContent { RcPlayer(state = state, modifier = Modifier.size(100.dp)) }
        val bitmap = rule.captureRootBitmap()
        assertRed(bitmap, 25, 50)
        assertNotRed(bitmap, 75, 50)
    }

    @Test
    fun setup_doesNotCountCanvasOperations() {
        val circlesPerComponent = 30
        val document = capture {
            RemoteColumn {
                repeat(3) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(30.rdp).drawWithContent {
                                val paint = RemotePaint { color = RemoteColor(Color.Red) }
                                repeat(circlesPerComponent) { i ->
                                    drawCircle(paint = paint, radius = (1f + i * 0.1f).rf)
                                }
                                drawContent()
                            }
                    ) {}
                }
            }
        }
        val canvasOpCount = canvasOperations(document).size
        assertThat(canvasOpCount).isAtLeast(3 * circlesPerComponent)

        // Each draw stream fits the per-pass cap on its own, but not all three together: setup
        // would exceed it if it walked every component's draw stream in one pass.
        Limits.MAX_OP_COUNT = circlesPerComponent * 2
        val state = RcPlayerState(document)
        assertThat(state.remoteContext().getLastOpCount()).isEqualTo(0)

        rule.setContent { RcPlayer(state = state, modifier = Modifier.size(100.dp)) }
        assertRed(rule.captureRootBitmap(), 15, 15)
    }

    @Test
    fun setup_registersTextDataFromCanvasStreams() {
        val document = capture {
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp).drawWithContent {
                        drawText(
                            "Canvas setup text".rs,
                            10f.rf,
                            50f.rf,
                            RemotePaint { color = RemoteColor(Color.Black) },
                        )
                        drawContent()
                    }
            ) {}
        }
        val textData =
            canvasOperations(document).filterIsInstance<TextData>().single {
                it.mText == "Canvas setup text"
            }

        val state = RcPlayerState(document)

        assertThat(state.remoteContext().getText(textData.mTextId)).isEqualTo("Canvas setup text")
    }

    private fun capture(content: @Composable @RemoteComposable () -> Unit): CoreDocument {
        val captured = runBlocking {
            captureSingleRemoteDocument(context = context, content = content)
        }
        return CoreDocument(RemoteClock.SYSTEM).apply {
            ByteArrayInputStream(captured.bytes).use {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }
    }

    private fun RcPlayerState.remoteContext(): AndroidRemoteContext =
        inspectableElements.first { it.name == "remoteContext" }.value as AndroidRemoteContext

    /** All operations nested in layout components' canvas draw streams. */
    private fun canvasOperations(document: CoreDocument): List<Operation> {
        val result = ArrayList<Operation>()
        fun collect(ops: List<Operation>, inCanvas: Boolean) {
            for (op in ops) {
                if (inCanvas) result.add(op)
                if (op is LayoutComponent) {
                    op.getCanvasOperations()?.let { collect(it.list, inCanvas = true) }
                }
                if (op is Container) collect(op.list, inCanvas)
            }
        }
        collect(document.operations, inCanvas = false)
        return result
    }
}
