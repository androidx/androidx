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
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteBlendMode
import androidx.compose.remote.creation.compose.state.RemoteCompositingStrategy
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.util.HashMap
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Behavioral tests for [GraphicsLayerModifierOperation] in the embedded [RcPlayer] and wire
 * serialization defaults in `remote-creation-compose`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerGraphicsLayerTest {

    @get:Rule val rule = RcPlayerTestRule()

    private fun loadDocument(bytes: ByteArray): CoreDocument =
        CoreDocument(RemoteClock.SYSTEM).apply {
            ByteArrayInputStream(bytes).use {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }

    private fun findGraphicsLayerOp(operations: List<Operation>): GraphicsLayerModifierOperation {
        return findGraphicsLayerOpOrNull(operations)
            ?: throw NoSuchElementException("GraphicsLayerModifierOperation not found in document")
    }

    private fun findGraphicsLayerOpOrNull(
        operations: List<Operation>
    ): GraphicsLayerModifierOperation? {
        for (op in operations) {
            if (op is GraphicsLayerModifierOperation) return op
            if (op is ComponentModifiers) {
                val found = op.list.filterIsInstance<GraphicsLayerModifierOperation>().firstOrNull()
                if (found != null) return found
            }
            if (op is Container) {
                val found = findGraphicsLayerOpOrNull(op.list)
                if (found != null) return found
            }
        }
        return null
    }

    @Test
    fun creationCompose_defaultOriginSerializesCenter_explicitZeroOriginUsesWireDefault() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            // Default origin in Compose DSL is (0.5f, 0.5f), which must be written to the wire
            // because GraphicsLayerModifierOperation wire default is 0.0f.
            val defaultOriginBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(50.rdp).graphicsLayer {
                                        rotationZ = 45f.rf
                                    }
                            )
                        },
                    )
                    .bytes
            val defaultDoc = loadDocument(defaultOriginBytes)
            val defaultOp = findGraphicsLayerOp(defaultDoc.operations)
            val defaultAttrs = HashMap<Int, Any>()
            defaultOp.fillInAttributes(defaultAttrs)
            assertThat(defaultAttrs[GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_X])
                .isEqualTo(0.5f)
            assertThat(defaultAttrs[GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_Y])
                .isEqualTo(0.5f)

            // Explicit (0f, 0f) matches the wire default (0.0f) and is omitted from attributes map.
            val zeroOriginBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(50.rdp)
                                        .graphicsLayer(
                                            rotationZ = 45f.rf,
                                            transformOriginX = 0f.rf,
                                            transformOriginY = 0f.rf,
                                        )
                            )
                        },
                    )
                    .bytes
            val zeroDoc = loadDocument(zeroOriginBytes)
            val zeroOp = findGraphicsLayerOp(zeroDoc.operations)
            val zeroAttrs = HashMap<Int, Any>()
            zeroOp.fillInAttributes(zeroAttrs)
            assertThat(zeroAttrs.containsKey(GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_X))
                .isFalse()
            assertThat(zeroAttrs.containsKey(GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_Y))
                .isFalse()
            val values = zeroOp.values
            assertThat(values[GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_X].source)
                .isEqualTo(0f)
            assertThat(values[GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_Y].source)
                .isEqualTo(0f)
        }
    }

    @Test
    fun graphicsLayer_defaultOrigin_rotatesNonSquareComponentAroundCenter() {
        // Non-square 80x40 box rotated 180 degrees around default center (0.5f, 0.5f)
        // remains within (0..80, 0..40). If it rotated around (0f, 0f), it would flip into
        // negative coordinates (-80..0, -40..0) outside the root viewport.
        rule.mainClock.autoAdvance = false
        rule.setRemoteContent(
            playComposableWrapper = {
                Box(modifier = Modifier.size(120.dp).background(Color.White)) { it() }
            }
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.width(80.rdp)
                        .height(40.rdp)
                        .graphicsLayer { rotationZ = 180f.rf }
                        .background(Color.Red.rc)
            )
        }

        val d = rule.density.density
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val insideLeft = bitmap.getPixel((20 * d).toInt(), (20 * d).toInt())
        val insideRight = bitmap.getPixel((60 * d).toInt(), (20 * d).toInt())
        val outsideBottomRight = bitmap.getPixel((100 * d).toInt(), (60 * d).toInt())

        assertThat(AndroidColor.red(insideLeft)).isGreaterThan(200)
        assertThat(AndroidColor.green(insideLeft)).isLessThan(50)
        assertThat(AndroidColor.red(insideRight)).isGreaterThan(200)
        assertThat(AndroidColor.green(insideRight)).isLessThan(50)
        assertThat(AndroidColor.green(outsideBottomRight)).isGreaterThan(200)
    }

    @Test
    fun graphicsLayer_explicitAndVariableTransformOrigin_movesPivotDynamically() {
        rule.mainClock.autoAdvance = false
        rule.setRemoteContent(
            playComposableWrapper = {
                Box(modifier = Modifier.size(150.dp).background(Color.White)) { it() }
            }
        ) {
            val origin = remember { MutableRemoteFloat(0.5f) }
            RemoteColumn {
                // 60x30 non-square red box rotated 180 degrees.
                // At origin (0.5, 0.5), pivot is (30, 15) -> occupies (0..60,
                // 0..30).
                // At origin (1.0, 1.0), pivot is (60, 30) -> 180° rotation flips it
                // to occupy (60..120, 30..60).
                RemoteBox(
                    modifier =
                        RemoteModifier.width(60.rdp)
                            .height(30.rdp)
                            .graphicsLayer(
                                rotationZ = 180f.rf,
                                transformOriginX = origin,
                                transformOriginY = origin,
                            )
                            .background(Color.Red.rc)
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(20.rdp)
                            .semantics { contentDescription = "movePivot".rs }
                            .clickable(onClick = valueChangeAction(origin, 1f.rf))
                )
            }
        }

        val d = rule.density.density
        rule.mainClock.advanceTimeBy(0)

        // Frame 0: pivot is (0.5f, 0.5f) -> box occupies (0..60, 0..30)
        val initialBitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val initialOriginalRegion = initialBitmap.getPixel((30 * d).toInt(), (15 * d).toInt())
        val initialFlippedRegion = initialBitmap.getPixel((90 * d).toInt(), (45 * d).toInt())
        assertThat(AndroidColor.red(initialOriginalRegion)).isGreaterThan(200)
        assertThat(AndroidColor.green(initialOriginalRegion)).isLessThan(50)
        assertThat(AndroidColor.green(initialFlippedRegion)).isGreaterThan(200)

        // Update origin to (1f, 1f) via click action
        rule.onNodeWithContentDescription("movePivot").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(350)

        // After transition completes: pivot is (1f, 1f) -> box occupies (60..120, 30..60)
        val updatedBitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val updatedOriginalRegion = updatedBitmap.getPixel((30 * d).toInt(), (15 * d).toInt())
        val updatedFlippedRegion = updatedBitmap.getPixel((90 * d).toInt(), (45 * d).toInt())
        assertThat(AndroidColor.green(updatedOriginalRegion)).isGreaterThan(200)
        assertThat(AndroidColor.red(updatedFlippedRegion)).isGreaterThan(200)
        assertThat(AndroidColor.green(updatedFlippedRegion)).isLessThan(50)
    }

    @Test
    fun graphicsLayer_variableBackedScale_animatesOver300msWithoutAppearanceAnimation() {
        rule.mainClock.autoAdvance = false
        rule.setRemoteContent(
            playComposableWrapper = {
                Box(modifier = Modifier.size(150.dp).background(Color.White)) { it() }
            }
        ) {
            val scaleVar = remember { MutableRemoteFloat(1f) }
            RemoteColumn {
                // 40x40 red box with origin (0f, 0f).
                // At scaleX = 1f -> width is 40dp (spans x = 0..40dp).
                // At scaleX = 3f -> width is 120dp (spans x = 0..120dp).
                RemoteBox(
                    modifier =
                        RemoteModifier.size(40.rdp)
                            .graphicsLayer(
                                scaleX = scaleVar,
                                transformOriginX = 0f.rf,
                                transformOriginY = 0f.rf,
                            )
                            .background(Color.Red.rc)
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(20.rdp)
                            .semantics { contentDescription = "scaleButton".rs }
                            .clickable(onClick = valueChangeAction(scaleVar, 3f.rf))
                )
            }
        }

        val d = rule.density.density
        fun pixelIsRed(xDp: Int, yDp: Int): Boolean {
            val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
            val pixel = bitmap.getPixel((xDp * d).toInt(), (yDp * d).toInt())
            return AndroidColor.red(pixel) > 200 && AndroidColor.green(pixel) < 50
        }

        // Initial frame: no appearance animation; scaleX is immediately 1f (width = 40dp)
        assertThat(pixelIsRed(30, 20)).isTrue()
        assertThat(pixelIsRed(65, 20)).isFalse()
        assertThat(pixelIsRed(105, 20)).isFalse()

        // Trigger value change: scaleVar -> 3f
        rule.onNodeWithContentDescription("scaleButton").performClick()
        rule.mainClock.advanceTimeByFrame()

        // Sample at start (t = 0ms of transition): still at start scale (~40dp)
        assertThat(pixelIsRed(30, 20)).isTrue()
        assertThat(pixelIsRed(65, 20)).isFalse()
        assertThat(pixelIsRed(105, 20)).isFalse()

        // Sample at intermediate timestamp (t = 100ms into 300ms CUBIC_STANDARD transition):
        // scaleX is roughly ~2f (width ~80dp), so x=65dp is now Red, but x=105dp is still White
        rule.mainClock.advanceTimeBy(100)
        assertThat(pixelIsRed(65, 20)).isTrue()
        assertThat(pixelIsRed(105, 20)).isFalse()

        // Sample at completion (t = 350ms >= 300ms): scaleX reaches 3f (width = 120dp),
        // so x=105dp is now Red
        rule.mainClock.advanceTimeBy(250)
        assertThat(pixelIsRed(105, 20)).isTrue()
    }

    @Test
    fun creationCompose_remoteCompositingStrategy_serializesExpectedWireValues() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            suspend fun captureOp(
                strategy: RemoteCompositingStrategy
            ): GraphicsLayerModifierOperation {
                val bytes =
                    captureSingleRemoteDocument(
                            context = context,
                            content = {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(50.rdp)
                                            .graphicsLayer(
                                                shape = RemoteRectangleShape,
                                                compositingStrategy = strategy,
                                            )
                                )
                            },
                        )
                        .bytes
                return findGraphicsLayerOp(loadDocument(bytes).operations)
            }

            // Auto (0) matches wire default (0) and is omitted from serialized attribute map.
            val autoOp = captureOp(RemoteCompositingStrategy.Auto)
            val autoAttrs = HashMap<Int, Any>()
            autoOp.fillInAttributes(autoAttrs)
            assertThat(autoAttrs.containsKey(GraphicsLayerModifierOperation.COMPOSITING_STRATEGY))
                .isFalse()
            assertThat(autoOp.values[GraphicsLayerModifierOperation.COMPOSITING_STRATEGY].intValue)
                .isEqualTo(0)

            // Offscreen (1) is serialized explicitly as int attribute 1.
            val offscreenOp = captureOp(RemoteCompositingStrategy.Offscreen)
            val offscreenAttrs = HashMap<Int, Any>()
            offscreenOp.fillInAttributes(offscreenAttrs)
            assertThat(offscreenAttrs[GraphicsLayerModifierOperation.COMPOSITING_STRATEGY])
                .isEqualTo(1)
            assertThat(
                    offscreenOp.values[GraphicsLayerModifierOperation.COMPOSITING_STRATEGY].intValue
                )
                .isEqualTo(1)

            // ModulateAlpha (2) is serialized explicitly as int attribute 2.
            val modulateOp = captureOp(RemoteCompositingStrategy.ModulateAlpha)
            val modulateAttrs = HashMap<Int, Any>()
            modulateOp.fillInAttributes(modulateAttrs)
            assertThat(modulateAttrs[GraphicsLayerModifierOperation.COMPOSITING_STRATEGY])
                .isEqualTo(2)
            assertThat(
                    modulateOp.values[GraphicsLayerModifierOperation.COMPOSITING_STRATEGY].intValue
                )
                .isEqualTo(2)
        }
    }

    @Test
    fun graphicsLayer_compositingStrategyWithTranslucentOverlap_modulateAlphaDiffersFromAutoAndOffscreen() {
        rule.mainClock.autoAdvance = false
        val strategies =
            listOf(
                RemoteCompositingStrategy.Auto,
                RemoteCompositingStrategy.Offscreen,
                RemoteCompositingStrategy.ModulateAlpha,
            )
        rule.setRemoteContent(
            playComposableWrapper = {
                Box(
                    modifier = Modifier.size(width = 240.dp, height = 80.dp).background(Color.White)
                ) {
                    it()
                }
            }
        ) {
            RemoteRow {
                for (strategy in strategies) {
                    RemoteCanvas(
                        modifier =
                            RemoteModifier.size(80.rdp)
                                .graphicsLayer(
                                    alpha = 0.5f.rf,
                                    shape = RemoteRectangleShape,
                                    compositingStrategy = strategy,
                                )
                    ) {
                        val subSize = RemoteSize(width * 0.7f.rf, height * 0.7f.rf)
                        val redOffset = RemoteOffset(width * 0.3f.rf, height * 0.3f.rf)
                        drawRect(paint = RemotePaint { color = Color.Blue.rc }, size = subSize)
                        drawRect(
                            paint = RemotePaint { color = Color.Red.rc },
                            topLeft = redOffset,
                            size = subSize,
                        )
                    }
                }
            }
        }

        val d = rule.density.density
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        fun sampleDp(xDp: Int, yDp: Int): Int =
            bitmap.getPixel((xDp * d).toInt(), (yDp * d).toInt())

        val autoOverlap = sampleDp(40, 40)
        val autoRedOnly = sampleDp(68, 68)
        val offscreenOverlap = sampleDp(80 + 40, 40)
        val offscreenRedOnly = sampleDp(80 + 68, 68)
        val modulateOverlap = sampleDp(160 + 40, 40)

        // Auto & Offscreen composite into an offscreen buffer first: opaque Red completely
        // occludes Blue in the overlap region before 0.5f layer alpha over White -> ~(255,128,128).
        assertThat(AndroidColor.red(autoOverlap)).isGreaterThan(220)
        assertThat(abs(AndroidColor.blue(autoOverlap) - AndroidColor.green(autoOverlap)))
            .isAtMost(5)
        assertThat(abs(AndroidColor.blue(autoOverlap) - AndroidColor.blue(autoRedOnly))).isAtMost(5)

        assertThat(AndroidColor.red(offscreenOverlap)).isGreaterThan(220)
        assertThat(abs(AndroidColor.blue(offscreenOverlap) - AndroidColor.green(offscreenOverlap)))
            .isAtMost(5)
        assertThat(abs(AndroidColor.blue(offscreenOverlap) - AndroidColor.blue(offscreenRedOnly)))
            .isAtMost(5)

        // ModulateAlpha modulates each draw call individually without an offscreen buffer:
        // Blue (0.5) over White -> (128,128,255), then Red (0.5) over that -> ~(191,64,128),
        // so Blue bleeds strongly through Red in the overlap region.
        assertThat(AndroidColor.blue(modulateOverlap) - AndroidColor.green(modulateOverlap))
            .isGreaterThan(40)
        assertThat(AndroidColor.red(autoOverlap) - AndroidColor.red(modulateOverlap))
            .isGreaterThan(30)
    }

    @Test
    fun graphicsLayer_compositingStrategyOffscreenVsAuto_isolatesBlendModeClearAtFullAlpha() {
        rule.mainClock.autoAdvance = false
        val strategies =
            listOf(
                RemoteCompositingStrategy.Auto,
                RemoteCompositingStrategy.Offscreen,
                RemoteCompositingStrategy.ModulateAlpha,
            )
        rule.setRemoteContent(
            playComposableWrapper = {
                Box(
                    modifier = Modifier.size(width = 240.dp, height = 80.dp).background(Color.White)
                ) {
                    it()
                }
            }
        ) {
            RemoteRow {
                for (strategy in strategies) {
                    RemoteBox(
                        modifier = RemoteModifier.size(80.rdp).background(Color.Yellow.rc),
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteCanvas(
                            modifier =
                                RemoteModifier.size(60.rdp)
                                    .graphicsLayer(
                                        alpha = 1f.rf,
                                        shape = RemoteRectangleShape,
                                        compositingStrategy = strategy,
                                    )
                        ) {
                            drawRect(paint = RemotePaint { color = Color.Blue.rc })
                            drawCircle(
                                paint =
                                    RemotePaint {
                                        color = Color.Black.rc
                                        blendMode = RemoteBlendMode.Clear
                                    },
                                radius = width * 0.3f.rf,
                            )
                        }
                    }
                }
            }
        }

        val d = rule.density.density
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        fun sampleDp(xDp: Int, yDp: Int): Int =
            bitmap.getPixel((xDp * d).toInt(), (yDp * d).toInt())

        val autoCenter = sampleDp(40, 40)
        val offscreenCenter = sampleDp(80 + 40, 40)
        val offscreenBlueRing = sampleDp(80 + 14, 40)
        val modulateCenter = sampleDp(160 + 40, 40)

        // Offscreen allocates an intermediate buffer even at alpha = 1f, so BlendMode.Clear
        // only clears the Blue square in that layer buffer, revealing the intact Yellow backdrop.
        assertThat(AndroidColor.red(offscreenCenter)).isGreaterThan(220)
        assertThat(AndroidColor.green(offscreenCenter)).isGreaterThan(220)
        assertThat(AndroidColor.blue(offscreenCenter)).isLessThan(30)
        assertThat(AndroidColor.blue(offscreenBlueRing)).isGreaterThan(220)
        assertThat(AndroidColor.red(offscreenBlueRing)).isLessThan(30)

        // Auto and ModulateAlpha do not allocate an offscreen buffer at alpha = 1f, so
        // BlendMode.Clear punches straight through the Yellow parent backdrop.
        assertThat(AndroidColor.red(autoCenter)).isLessThan(50)
        assertThat(AndroidColor.green(autoCenter)).isLessThan(50)
        assertThat(AndroidColor.red(modulateCenter)).isLessThan(50)
        assertThat(AndroidColor.green(modulateCenter)).isLessThan(50)
    }
}
