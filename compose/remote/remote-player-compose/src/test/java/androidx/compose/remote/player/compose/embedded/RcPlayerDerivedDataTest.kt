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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.TextTransform
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ADD
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_DEREF
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_LEN
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SUB
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Derived data operations whose inputs change after document setup. The View player re-applies them
 * whenever an input changes; each test changes a named float after the first frame and checks the
 * width of a bar that depends on the derived value.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerDerivedDataTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    private lateinit var state: RcPlayerState

    @Test
    fun textLookupFollowsItsIndex() {
        var index = 0
        val document = buildDocument {
            val list = writer.addStringList("a", "bb", "ccc")
            val indexVar = writer.addNamedFloat("index", 0f)
            index = Utils.idFromNan(indexVar)
            val text = writer.textLookup(list, indexVar)
            bar(writer.floatExpression(writer.textLength(text), 10f, MUL))
        }

        assertBarWidth(play(document), 10)
        assertBarWidth(update(index, 2f), 30)
    }

    @Test
    fun textSubtextFollowsItsLength() {
        var length = 0
        val document = buildDocument {
            val lengthVar = writer.addNamedFloat("length", 1f)
            length = Utils.idFromNan(lengthVar)
            val text = writer.textSubtext(writer.textCreateId("abcdef"), 0f, lengthVar)
            bar(writer.floatExpression(writer.textLength(text), 10f, MUL))
        }

        assertBarWidth(play(document), 10)
        assertBarWidth(update(length, 4f), 40)
    }

    @Test
    fun textTransformFollowsItsLength() {
        var length = 0
        val document = buildDocument {
            val lengthVar = writer.addNamedFloat("length", 2f)
            length = Utils.idFromNan(lengthVar)
            val text =
                writer.textTransform(
                    writer.textCreateId("abcdef"),
                    0f,
                    lengthVar,
                    TextTransform.TEXT_TO_UPPERCASE,
                )
            bar(writer.floatExpression(writer.textLength(text), 10f, MUL))
        }

        assertBarWidth(play(document), 20)
        assertBarWidth(update(length, 5f), 50)
    }

    @Test
    fun dynamicListWriteInTheDrawStreamFollowsItsValue() {
        var value = 0
        val document = buildDocument {
            val array = writer.addDynamicFloatArray(1f)
            val valueVar = writer.addNamedFloat("value", 2f)
            value = Utils.idFromNan(valueVar)
            writer.setArrayValue(Utils.idFromNan(array), 0f, valueVar)
            bar(writer.floatExpression(array, 0f, A_DEREF, 10f, MUL))
        }

        assertBarWidth(play(document), 20)
        assertBarWidth(update(value, 5f), 50)
    }

    @Test
    fun floatListCollectionSurvivesPlayerContextInitialization() {
        val document = buildDocument {
            root {
                canvas(RecordingModifier().fillMaxSize()) {
                    val list = writer.addFloatArray(floatArrayOf(10f, 20f, 30f, 40f))
                    val len = writer.floatExpression(list, A_LEN)
                    val last = writer.floatExpression(list, len, 1f, SUB, A_DEREF)
                    bar(last)
                }
            }
        }

        assertBarWidth(play(document), 40)
    }

    @Test
    fun sequentialLoopsSharingIndexVariableDoNotEvaluateOutOfBounds() {
        val document = buildDocument {
            val list = writer.addFloatArray(floatArrayOf(10f, 20f, 30f, 40f))
            val len = writer.floatExpression(list, A_LEN)
            val loopIndexNan = writer.reserveFloatVariable()
            val loopIndexId = Utils.idFromNan(loopIndexNan)
            // First loop runs i = 0..3, leaving loopIndexId = 3f at loop completion.
            writer.loop(loopIndexId, 0f, 1f, len) {
                val v = writer.floatExpression(list, loopIndexNan, A_DEREF)
                bar(v)
            }
            // Second loop runs i = 0..2 and indexes list[i + 1]; must not evaluate its body
            // with the stale loopIndexId = 3f before resetting loopIndexId to 0f.
            val lenMinusOne = writer.floatExpression(len, 1f, SUB)
            writer.loop(loopIndexId, 0f, 1f, lenMinusOne) {
                val next = writer.floatExpression(list, loopIndexNan, 1f, ADD, A_DEREF)
                bar(next)
            }
        }

        assertBarWidth(play(document), 40)
    }

    private fun play(document: CoreDocument): Bitmap {
        rule.setContent {
            state = rememberRcPlayerState(document)
            Box(modifier = Modifier.size(100.dp)) { RcPlayer(state = state) }
        }
        return rule.onRoot().captureToImage().asAndroidBitmap()
    }

    /** Sets float variable [id] to [value] after setup, then captures the next frame. */
    private fun update(id: Int, value: Float): Bitmap {
        rule.runOnIdle { state.remoteContext.loadFloat(id, value) }
        return rule.onRoot().captureToImage().asAndroidBitmap()
    }

    /** Draws a red bar from x = 0 to [width], across the middle of the player. */
    private fun RemoteComposeContextAndroid.bar(width: Float) {
        painter.setColor(AndroidColor.RED).commit()
        writer.drawRect(0f, 40f, width, 60f)
    }

    private fun assertBarWidth(bitmap: Bitmap, width: Int) {
        assertRed(bitmap, width - 5, 50)
        assertNotRed(bitmap, width + 5, 50)
    }
}
