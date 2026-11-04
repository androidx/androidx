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
package androidx.xr.glimmer.remote

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.RemoteCapturedDocument
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.MutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.util.DocumentStats
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import androidx.xr.glimmer.remote.SurfaceOperationCountTest.RemoteSurfaceStats.Companion.ColorExpressions
import androidx.xr.glimmer.remote.SurfaceOperationCountTest.RemoteSurfaceStats.Companion.DocumentSizeBytes
import androidx.xr.glimmer.remote.SurfaceOperationCountTest.RemoteSurfaceStats.Companion.DrawOperations
import androidx.xr.glimmer.remote.SurfaceOperationCountTest.RemoteSurfaceStats.Companion.DynamicStateFloatExpressionsSize
import androidx.xr.glimmer.remote.SurfaceOperationCountTest.RemoteSurfaceStats.Companion.FloatExpressions
import androidx.xr.glimmer.remote.SurfaceOperationCountTest.RemoteSurfaceStats.Companion.FloatExpressionsSize
import androidx.xr.glimmer.remote.SurfaceOperationCountTest.RemoteSurfaceStats.Companion.PaintObjects
import com.google.common.truth.Expect
import java.io.ByteArrayInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Counts the operations that [surface] adds to a `.rc` document in different states. This test
 * ensures that any regressions in [surface] are spotted, as the amount of operations running on the
 * hardware directly impacts performance.
 */
@Suppress("RestrictedApiAndroidX")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SurfaceOperationCountTest {

    @get:Rule(order = 0) val expect: Expect = Expect.create()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val displayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun enabled() = runTest {
        val document =
            captureSingleRemoteDocument(context, displayInfo) {
                val modifier =
                    RemoteModifier.surface(
                        enabled = true.rb,
                        focused = false.rb,
                        pressed = false.rb,
                    )
                RemoteBox(RemoteModifier.size(100.rdp).then(modifier))
            }

        assertSurfaceStats(document) {
            numFloatExpressions = FloatExpressions
            totalFloatExpressionSize = FloatExpressionsSize
            numColorExpressions = ColorExpressions
            numPaintObjects = PaintObjects
            numDrawOperations = DrawOperations
            numIntegerExpressions = 0
            numConditionalOperations = 0
            documentSizeBytes = DocumentSizeBytes
        }
    }

    @Test
    fun focused() = runTest {
        val document =
            captureSingleRemoteDocument(context, displayInfo) {
                val modifier =
                    RemoteModifier.surface(enabled = true.rb, focused = true.rb, pressed = false.rb)
                RemoteBox(RemoteModifier.size(100.rdp).then(modifier))
            }

        // Focus is animated by expressions, so a constant focused state doesn't change the stats.
        assertSurfaceStats(document) {
            numFloatExpressions = FloatExpressions
            totalFloatExpressionSize = FloatExpressionsSize
            numColorExpressions = ColorExpressions
            numPaintObjects = PaintObjects
            numDrawOperations = DrawOperations
            numIntegerExpressions = 0
            numConditionalOperations = 0
            documentSizeBytes = DocumentSizeBytes
        }
    }

    @Test
    fun focusedAndPressed() = runTest {
        val document =
            captureSingleRemoteDocument(context, displayInfo) {
                val modifier =
                    RemoteModifier.surface(enabled = true.rb, focused = true.rb, pressed = true.rb)
                RemoteBox(RemoteModifier.size(100.rdp).then(modifier))
            }

        // Press is animated by expressions, so a constant pressed state doesn't change the stats.
        assertSurfaceStats(document) {
            numFloatExpressions = FloatExpressions
            totalFloatExpressionSize = FloatExpressionsSize
            numColorExpressions = ColorExpressions
            numPaintObjects = PaintObjects
            numDrawOperations = DrawOperations
            numIntegerExpressions = 0
            numConditionalOperations = 0
            documentSizeBytes = DocumentSizeBytes
        }
    }

    @Test
    fun disabled() = runTest {
        val document =
            captureSingleRemoteDocument(context, displayInfo) {
                val modifier = RemoteModifier.surface(enabled = false.rb)
                RemoteBox(RemoteModifier.size(100.rdp).then(modifier))
            }

        // Disabled state includes an extra layer that is drawn last, on top of others.
        assertSurfaceStats(document) {
            numFloatExpressions = FloatExpressions
            totalFloatExpressionSize = FloatExpressionsSize
            numColorExpressions = ColorExpressions
            // Unconditional disabled overlay.
            numPaintObjects = PaintObjects + 1
            numDrawOperations = DrawOperations + 1
            numIntegerExpressions = 0
            numConditionalOperations = 0
            documentSizeBytes = DocumentSizeBytes + 58
        }
    }

    @Test
    fun dynamicState() = runTest {
        val document =
            captureSingleRemoteDocument(context, displayInfo) {
                val modifier =
                    RemoteModifier.surface(
                        enabled = remember { MutableRemoteBoolean(true) },
                        pressed = remember { MutableRemoteBoolean(false) },
                        focused = remember { MutableRemoteBoolean(false) },
                    )
                RemoteBox(RemoteModifier.size(100.rdp).then(modifier))
            }

        assertSurfaceStats(document) {
            numFloatExpressions = FloatExpressions
            totalFloatExpressionSize = DynamicStateFloatExpressionsSize
            numColorExpressions = ColorExpressions
            // Disabled state includes an extra layer that is drawn last, on top of others.
            numPaintObjects = PaintObjects + 1
            numDrawOperations = DrawOperations + 1
            // drawConditionally
            numIntegerExpressions = 1
            numConditionalOperations = 1
            documentSizeBytes = DocumentSizeBytes + 185
        }
    }

    @Test
    fun circleShape() = runTest {
        val document =
            captureSingleRemoteDocument(context, displayInfo) {
                val modifier = RemoteModifier.surface(shape = RemoteCircleShape)
                RemoteBox(RemoteModifier.size(100.rdp).then(modifier))
            }

        // Unlike default shape with constant radius, a circle shape depends on the size of the
        // canvas that makes it a bit more complex for calculations.
        assertSurfaceStats(document) {
            numFloatExpressions = FloatExpressions + 1
            totalFloatExpressionSize = FloatExpressionsSize + 3
            numColorExpressions = ColorExpressions
            numPaintObjects = PaintObjects
            numDrawOperations = DrawOperations
            numIntegerExpressions = 0
            numConditionalOperations = 0
            documentSizeBytes = DocumentSizeBytes + 21
        }
    }

    @Test
    fun unevenShape() = runTest {
        val document =
            captureSingleRemoteDocument(context, displayInfo) {
                val unevenShape =
                    RemoteRoundedCornerShape(
                        topStart = 0.rdp,
                        topEnd = 1.rdp,
                        bottomEnd = 2.rdp,
                        bottomStart = 3.rdp,
                    )
                val modifier = RemoteModifier.surface(shape = unevenShape)
                RemoteBox(RemoteModifier.size(100.rdp).then(modifier))
            }

        // A shape with different corners ends up with more expressions, since each corner has its
        // own value instead of the one value shared across all corners. Uneven shape is not CUJ,
        // but it's good to test these numbers in case of any changes.
        assertSurfaceStats(document) {
            numFloatExpressions = FloatExpressions + 13
            totalFloatExpressionSize = FloatExpressionsSize + 154
            numColorExpressions = ColorExpressions
            numPaintObjects = PaintObjects
            numDrawOperations = DrawOperations
            numIntegerExpressions = 0
            numConditionalOperations = 0
            documentSizeBytes = 4_113
        }
    }

    /** Asserts that properties of [document] match the expected stats set by [assertion]. */
    private fun assertSurfaceStats(
        document: RemoteCapturedDocument,
        assertion: RemoteSurfaceStats.() -> Unit,
    ) {
        val expectedStats = RemoteSurfaceStats().apply(assertion)
        val coreDocument =
            CoreDocument().apply {
                initFromBuffer(
                    RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document.bytes))
                )
            }
        val actualStats = DocumentStats.examineFullDocument(coreDocument)

        // TODO: b/581299105 - Find a better alternative to validate the doc.
        expect
            .withMessage("Number of float expressions")
            .that(actualStats.numFloatExpressions)
            .isAtMost(expectedStats.numFloatExpressions)

        expect
            .withMessage("Total size of float expressions")
            .that(actualStats.totalFloatExpressionSize)
            .isAtMost(expectedStats.totalFloatExpressionSize)

        expect
            .withMessage("Number of color expressions")
            .that(actualStats.numColorExpressions)
            .isAtMost(expectedStats.numColorExpressions)

        expect
            .withMessage("Number of paint objects")
            .that(actualStats.numPaintObjects)
            .isAtMost(expectedStats.numPaintObjects)

        expect
            .withMessage("Number of draw operations")
            .that(actualStats.numDrawOperations)
            .isAtMost(expectedStats.numDrawOperations)

        expect
            .withMessage("Number of integer expressions")
            .that(actualStats.numIntegerExpressions)
            .isAtMost(expectedStats.numIntegerExpressions)

        expect
            .withMessage("Number of conditional operations")
            .that(actualStats.numConditionalOperations)
            .isAtMost(expectedStats.numConditionalOperations)

        expect
            .withMessage("Document size in bytes")
            .that(document.bytes.size)
            .isAtMost(expectedStats.documentSizeBytes)
    }

    /** The subset of [DocumentStats] that is affected by [surface]. */
    private class RemoteSurfaceStats(
        var numFloatExpressions: Int = 0,
        var totalFloatExpressionSize: Int = 0,
        var numColorExpressions: Int = 0,
        var numPaintObjects: Int = 0,
        var numDrawOperations: Int = 0,
        var numIntegerExpressions: Int = 0,
        var numConditionalOperations: Int = 0,
        var documentSizeBytes: Int = 0,
    ) {
        /**
         * Measured and pinned operation counts of [surface]. These values directly affect the
         * performance of the surface on the player side.
         *
         * If the tests start failing, investigate in which direction the values have changed and
         * why before updating them. A change can be caused both by changes in the [surface]
         * implementation and by updates of the Remote Compose libraries.
         */
        companion object {
            /**
             * Float expressions are document operations that compute a float value on the player
             * side, for example from the animation progress or the component size. They are stored
             * as formulas instead of values, so the player can recompute them when their inputs
             * change. The surface uses them for its animated colors, border widths and gradient
             * transformation.
             */
            const val FloatExpressions = 43

            /**
             * Total length of all [FloatExpressions] formulas: each formula is a list of operands
             * and operators, so the size reflects how much work the player does to recompute them.
             * Measured when interaction states are constants.
             */
            const val FloatExpressionsSize = 303

            /**
             * Same as [FloatExpressionsSize], but with dynamic interaction states: the formulas get
             * longer because they read the state variables instead of constants.
             */
            const val DynamicStateFloatExpressionsSize = 309

            /**
             * Colors of the 1 background + 6 border layers x 4 gradient stops (the second half of
             * the symmetric gradient reuses the same colors).
             */
            const val ColorExpressions = 25

            /** Paints of 1 background + 6 border layers. */
            const val PaintObjects = 7

            /** Draw operations of 1 background, 6 border layers and 1 content. */
            const val DrawOperations = 8

            /**
             * Size of the whole serialized `.rc` document in bytes, including the content that
             * isn't counted by the other stats (headers, layout, data, etc.).
             */
            const val DocumentSizeBytes = 3_271
        }
    }
}
