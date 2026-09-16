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
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.util.HashMap
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Behavioral tests for [GraphicsLayerModifierOperation] in the embedded [RcPlayer] and wire
 * serialization defaults in `remote-creation-compose`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RcPlayerGraphicsLayerTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

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
            val values = zeroOp.getValuesReflection()
            assertThat(values[GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_X].source)
                .isEqualTo(0f)
            assertThat(values[GraphicsLayerModifierOperation.TRANSFORM_ORIGIN_Y].source)
                .isEqualTo(0f)
        }
    }

    @Test
    fun graphicsLayer_defaultOrigin_rotatesNonSquareComponentAroundCenter() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            // Non-square 80x40 box rotated 180 degrees around default center (0.5f, 0.5f)
            // remains within (0..80, 0..40). If it rotated around (0f, 0f), it would flip into
            // negative coordinates (-80..0, -40..0) outside the root viewport.
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.width(80.rdp)
                                        .height(40.rdp)
                                        .graphicsLayer { rotationZ = 180f.rf }
                                        .background(Color.Red.rc)
                            )
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(120.dp).background(Color.White)) {
                    RcPlayer(document = document)
                }
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
    }

    @Test
    fun graphicsLayer_explicitAndVariableTransformOrigin_movesPivotDynamically() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val origin = rememberMutableRemoteFloat(0.5f)
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
                                            .clickable(action = valueChange(origin, 1f.rf))
                                )
                            }
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(150.dp).background(Color.White)) {
                    RcPlayer(document = document)
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
    }

    @Test
    fun graphicsLayer_variableBackedScale_animatesOver300msWithoutAppearanceAnimation() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val scaleVar = rememberMutableRemoteFloat(1f)
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
                                            .clickable(action = valueChange(scaleVar, 3f.rf))
                                )
                            }
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(150.dp).background(Color.White)) {
                    RcPlayer(document = document)
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
    }
}
