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

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ADD
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.time.Duration
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Loops in the embedded player ([RcPlayer]).
 *
 * Setup doesn't iterate loop bodies, so the static resources a body defines (text, fonts, bitmap
 * fonts, sounds) must be registered once without running it; the draw pass reads them from the
 * store but never applies them. Draw loops may also count down with a negative step, which
 * `LoopOperation.read` accepts.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "mdpi")
class RcPlayerLoopTest {

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

    @Test
    fun setup_registersTextDataInDocumentLevelLoop() {
        val document = buildDocument {
            writer.rcPaint.setColor(AndroidColor.RED).setTextSize(30f).commit()
            writer.startLoopVar(0f, 1f, 3f)
            writer.drawTextAnchored(LOOP_TEXT, 50f, 50f, 0f, 0f, 0)
            writer.endLoop()
        }
        val textId = textDataId(document, LOOP_TEXT)

        val state = RcPlayerState(document)

        assertThat(state.inspectableRemoteContext.getText(textId)).isEqualTo(LOOP_TEXT)
        rule.setContent { Box(Modifier.size(100.dp)) { RcPlayer(state = state) } }
        assertWithMessage("red text pixels drawn by the loop")
            .that(countRed(rule.captureRootBitmap()))
            .isGreaterThan(MIN_TEXT_INK)
    }

    @Test
    fun setup_registersTextDataInCanvasStreamLoop() {
        val document = canvasDocument {
            rcPaint.setColor(AndroidColor.RED).setTextSize(30f).commit()
            startLoopVar(0f, 1f, 3f)
            drawTextAnchored(LOOP_TEXT, 50f, 50f, 0f, 0f, 0)
            endLoop()
        }
        val textId = textDataId(document, LOOP_TEXT)

        val state = RcPlayerState(document)

        assertThat(state.inspectableRemoteContext.getText(textId)).isEqualTo(LOOP_TEXT)
        rule.setContent { Box(Modifier.size(100.dp)) { RcPlayer(state = state) } }
        assertWithMessage("red text pixels drawn by the loop")
            .that(countRed(rule.captureRootBitmap()))
            .isGreaterThan(MIN_TEXT_INK)
    }

    @Test
    fun setup_registersFontAndBitmapFontInLoops() {
        var fontId = 0
        var bitmapFontId = 0
        val document = buildDocument {
            writer.startLoopVar(0f, 1f, 3f)
            fontId = writer.addFont(byteArrayOf(1, 2, 3, 4))
            bitmapFontId =
                writer.addBitmapFont(arrayOf(BitmapFontData.Glyph("a", 0, 0, 0, 0, 0, 10, 10)))
            writer.endLoop()
            writer.drawRect(0f, 0f, 1f, 1f)
        }

        val state = RcPlayerState(document)

        assertWithMessage("font registered from a loop body")
            .that(state.inspectableRemoteContext.getObject(fontId))
            .isNotNull()
        assertWithMessage("bitmap font registered from a loop body")
            .that(state.inspectableRemoteContext.getObject(bitmapFontId))
            .isInstanceOf(BitmapFontData::class.java)
    }

    @Test
    fun setup_loadsSoundInLoopOnce_notPerIterationOrFrame() {
        val loaded = mutableListOf<Int>()
        var soundId = 0
        val document = canvasDocument {
            rcPaint.setColor(AndroidColor.RED).commit()
            startLoopVar(0f, 1f, 5f)
            soundId = addSound(byteArrayOf(1, 2, 3, 4))
            drawRect(0f, 0f, 10f, 10f)
            endLoop()
        }
        document.setSoundEngine(
            object : CoreDocument.SoundEngine {
                override fun loadSound(soundId: Int, data: ByteArray) {
                    loaded += soundId
                }

                override fun playSound(soundId: Int) {}
            }
        )

        val state = RcPlayerState(document)
        assertThat(loaded).containsExactly(soundId)

        rule.mainClock.autoAdvance = false
        rule.setContent { Box(Modifier.size(100.dp)) { RcPlayer(state = state) } }
        repeat(5) { frame() }
        assertRed(rule.captureRootBitmap(), 5, 5)
        assertThat(loaded).containsExactly(soundId)
    }

    @Test
    fun setup_registersLoopResourcesWithoutIterating() {
        val document =
            canvasDocument(
                beforeRoot = {
                    startLoopVar(0f, 1f, 100_000f)
                    addText("document loop text")
                    endLoop()
                }
            ) {
                startLoopVar(0f, 1f, 100_000f)
                addText("canvas loop text")
                endLoop()
            }
        val documentTextId = textDataId(document, "document loop text")
        val canvasTextId = textDataId(document, "canvas loop text")

        // Setup would exceed this cap if it iterated either loop.
        Limits.MAX_OP_COUNT = 50
        val state = RcPlayerState(document)

        assertThat(state.inspectableRemoteContext.getLastOpCount()).isEqualTo(0)
        assertThat(state.inspectableRemoteContext.getText(documentTextId))
            .isEqualTo("document loop text")
        assertThat(state.inspectableRemoteContext.getText(canvasTextId))
            .isEqualTo("canvas loop text")
    }

    @Test
    fun staticLoopDocument_noPerFrameStateWrites() {
        val document = canvasDocument {
            rcPaint.setColor(AndroidColor.RED).setTextSize(20f).commit()
            startLoopVar(0f, 1f, 20f)
            drawTextAnchored(LOOP_TEXT, 50f, 50f, 0f, 0f, 0)
            drawRect(0f, 0f, 10f, 10f)
            endLoop()
        }
        val state = RcPlayerState(document)
        rule.mainClock.autoAdvance = false
        rule.setContent { Box(Modifier.size(100.dp)) { RcPlayer(state = state) } }
        frame()

        assertThat(rule.framesWithStateChanges(10, ::frame)).isEqualTo(0)
        assertRed(rule.captureRootBitmap(), 5, 5)
    }

    @Test
    fun countdownLoop_drawsEveryIteration() {
        val bitmap = rule.renderDocument {
            writer.rcPaint.setColor(AndroidColor.RED).commit()
            // idx = 5, 4, 3, 2, 1; `until` is exclusive.
            val idx = writer.startLoopVar(5f, -1f, 0f)
            drawColumn(writer, idx)
            writer.endLoop()
        }

        assertCountdownColumns(bitmap)
    }

    @Test
    fun countdownLoop_inCanvasStream_drawsEveryIteration() {
        val document = canvasDocument {
            rcPaint.setColor(AndroidColor.RED).commit()
            val idx = startLoopVar(5f, -1f, 0f)
            drawColumn(this, idx)
            endLoop()
        }

        assertCountdownColumns(rule.renderDocument(document))
    }

    @Test
    fun countdownLoop_fractionalStep() {
        val bitmap = rule.renderDocument {
            writer.rcPaint.setColor(AndroidColor.RED).commit()
            // idx = 1, 0.75, 0.5, 0.25 -> x = 80, 60, 40, 20.
            val idx = writer.startLoopVar(1f, -0.25f, 0f)
            val left = writer.floatExpression(idx, 80f, MUL)
            val right = writer.floatExpression(idx, 80f, MUL, 10f, ADD)
            writer.drawRect(left, 40f, right, 60f)
            writer.endLoop()
        }

        for (x in intArrayOf(85, 65, 45, 25)) assertRed(bitmap, x, 50)
        assertNotRed(bitmap, 5, 50)
        assertNotRed(bitmap, 95, 50)
    }

    @Test
    fun loop_withBoundsRunningAwayFromUntil_drawsNothing() {
        // Bounds read from variables skip LoopOperation.read's validation.
        val cases =
            listOf(
                Triple(0f, -1f, 5f), // counts down, but `until` is above `from`
                Triple(5f, 1f, 0f), // counts up, but `until` is below `from`
                Triple(0f, 0f, 5f), // never advances
            )
        val bitmap = rule.renderDocument {
            writer.rcPaint.setColor(AndroidColor.RED).commit()
            cases.forEachIndexed { case, (from, step, until) ->
                writer.startLoopVar(
                    writer.addNamedFloat("from$case", from),
                    writer.addNamedFloat("step$case", step),
                    writer.addNamedFloat("until$case", until),
                )
                writer.drawRect(case * 30f, 40f, case * 30f + 20f, 60f)
                writer.endLoop()
            }
        }

        for (case in cases.indices) assertNotRed(bitmap, case * 30 + 10, 50)
    }

    @Test
    fun countdownLoop_stuckIndex_stopsAtIterationCap() {
        // 1e9f - 1f == 1e9f in float, so the index never reaches `until`; only the iteration cap
        // ends the loop.
        Limits.MAX_OP_COUNT = Int.MAX_VALUE
        val bitmap = rule.renderDocument {
            writer.rcPaint.setColor(AndroidColor.RED).commit()
            writer.startLoopVar(1e9f, -1f, 0f)
            writer.drawRect(0f, 0f, 10f, 10f)
            writer.endLoop()
        }

        assertRed(bitmap, 5, 5)
    }

    /** Draws a 10px wide column at x = idx * 15. */
    private fun drawColumn(writer: RemoteComposeWriter, idx: Float) {
        val left = writer.floatExpression(idx, 15f, MUL)
        val right = writer.floatExpression(idx, 15f, MUL, 10f, ADD)
        writer.drawRect(left, 40f, right, 60f)
    }

    private fun assertCountdownColumns(bitmap: Bitmap) {
        // idx 5..1 -> columns at 75, 60, 45, 30, 15.
        for (x in intArrayOf(80, 65, 50, 35, 20)) assertRed(bitmap, x, 50)
        // idx 0 is excluded and the gaps between columns stay empty.
        assertNotRed(bitmap, 5, 50)
        assertNotRed(bitmap, 27, 50)
        assertNotRed(bitmap, 90, 50)
    }

    /** Advances one frame and lets the main looper run a draw traversal. */
    private fun frame() {
        rule.mainClock.advanceTimeByFrame()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16))
    }

    /**
     * A 100x100 document with a root box whose canvas draw stream holds [canvas], after the
     * document-level operations in [beforeRoot].
     */
    private fun canvasDocument(
        beforeRoot: RemoteComposeWriter.() -> Unit = {},
        canvas: RemoteComposeWriter.() -> Unit,
    ): CoreDocument = buildDocument {
        writer.beforeRoot()
        root {
            startBox(
                RecordingModifier().width(100f).height(100f).then { w ->
                    w.startCanvasOperations()
                    w.canvas()
                    w.endCanvasOperations()
                },
                BoxLayout.START,
                BoxLayout.TOP,
            )
            endBox()
        }
    }

    /** The id of the [TextData] holding [text], wherever it's nested in [document]. */
    private fun textDataId(document: CoreDocument, text: String): Int =
        document.allOperations().filterIsInstance<TextData>().single { it.mText == text }.mTextId

    private fun countRed(bitmap: Bitmap): Int {
        var count = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (
                    AndroidColor.red(pixel) > 200 &&
                        AndroidColor.green(pixel) < 60 &&
                        AndroidColor.blue(pixel) < 60
                ) {
                    count++
                }
            }
        }
        return count
    }

    private companion object {
        const val LOOP_TEXT = "Loop only"
        const val MIN_TEXT_INK = 20
    }
}
