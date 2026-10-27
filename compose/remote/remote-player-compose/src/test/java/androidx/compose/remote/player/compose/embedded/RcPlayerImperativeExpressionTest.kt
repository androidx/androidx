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

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Float expressions that a draw stream evaluates imperatively, in order, so a later operation must
 * see the value written just before it rather than a value derived on demand.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerImperativeExpressionTest {

    @get:Rule val rule = RcPlayerTestRule()

    /** Writes [values] as a float expression to the existing float variable [id]. */
    private fun RemoteComposeContextAndroid.assign(id: Float, vararg values: Float) {
        FloatExpression.apply(writer.buffer.buffer, Utils.idFromNan(id), values, null)
    }

    /** A red bar from x = 0 to [width] between [top] and [top] + 50. */
    private fun RemoteComposeContextAndroid.drawBar(top: Float, width: Float) {
        painter.setColor(AndroidColor.RED).commit()
        writer.drawRect(0f, top, width, top + 50f)
    }

    @Test
    fun snapshotOfFunctionOutput_keepsTheValueFromItsCall() {
        val bitmap = rule.renderDocument {
            val args = FloatArray(1)
            val out = writer.createFloatId()
            val function = writer.createFloatFunction(args)
            assign(out, args[0])
            writer.endFloatFunction()

            writer.callFloatFunction(function, 20f)
            val first = floatExpression(out, 0f, AnimatedFloatExpression.ADD)
            writer.callFloatFunction(function, 80f)
            val second = floatExpression(out, 1f, AnimatedFloatExpression.MUL)

            drawBar(top = 0f, width = first)
            drawBar(top = 50f, width = second)
        }

        assertNotRed(bitmap, 50, 25)
        assertRed(bitmap, 10, 25)
        assertRed(bitmap, 50, 75)
    }

    @Test
    fun reassignedVariable_readsTheLatestAssignment() {
        val bitmap = rule.renderDocument {
            val width = writer.createFloatId()
            assign(width, 20f)
            drawBar(top = 0f, width = width)
            assign(width, 80f)
            drawBar(top = 50f, width = width)
        }

        assertNotRed(bitmap, 50, 25)
        assertRed(bitmap, 10, 25)
        assertRed(bitmap, 50, 75)
    }

    @Test
    fun loopAccumulator_readsTheValueAfterTheLoop() {
        val bitmap = rule.renderDocument {
            val width = writer.createFloatId()
            assign(width, 0f)
            writer.startLoop(4f)
            assign(width, width, 20f, AnimatedFloatExpression.ADD)
            writer.endLoop()
            drawBar(top = 0f, width = width)
        }

        assertRed(bitmap, 70, 25)
        assertNotRed(bitmap, 90, 25)
    }

    @Test
    fun conditionalAssignment_readsTheBranchValue() {
        val bitmap = rule.renderDocument {
            val width = writer.createFloatId()
            assign(width, 20f)
            writer.conditionalOperations(ConditionalOperations.TYPE_GT, 1f, 0f)
            assign(width, 80f)
            writer.endConditionalOperations()
            drawBar(top = 0f, width = width)
        }

        assertRed(bitmap, 50, 25)
    }

    /**
     * Draws [document] once per frame and returns how many of the last 10 frames applied a snapshot
     * state change. Values the draw stream writes must not go through snapshot state, or the draw
     * invalidates itself on every frame.
     */
    private fun framesWithStateChangesWhileDrawing(document: CoreDocument): Int {
        rule.mainClock.autoAdvance = false
        rule.setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) } }
        val drawFrame = {
            rule.mainClock.advanceTimeByFrame()
            rule.captureRootBitmap()
            Unit
        }
        rule.framesWithStateChanges(3, drawFrame)
        return rule.framesWithStateChanges(10, drawFrame)
    }

    @Test
    fun loopDrawingAtItsIndex_doesNotChangeStateEachFrame() {
        val document = buildDocument {
            painter.setColor(AndroidColor.RED).commit()
            val index = writer.startLoop(4f)
            val left = floatExpression(index, 20f, AnimatedFloatExpression.MUL)
            val right = floatExpression(left, 10f, AnimatedFloatExpression.ADD)
            writer.drawRect(left, 0f, right, 10f)
            writer.endLoop()
        }

        assertThat(framesWithStateChangesWhileDrawing(document)).isEqualTo(0)
    }

    @Test
    fun variableReassignedEachPass_doesNotChangeStateEachFrame() {
        val document = buildDocument {
            val width = writer.createFloatId()
            assign(width, 20f)
            drawBar(top = 0f, width = width)
            assign(width, 80f)
            drawBar(top = 50f, width = width)
        }

        assertThat(framesWithStateChangesWhileDrawing(document)).isEqualTo(0)
    }
}
