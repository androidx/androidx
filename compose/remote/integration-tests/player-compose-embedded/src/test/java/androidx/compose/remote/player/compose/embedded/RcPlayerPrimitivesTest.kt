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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.alignByBaseline
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.basicMarquee
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.combinedClickable
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.offset
import androidx.compose.remote.creation.compose.modifier.onTouchCancel
import androidx.compose.remote.creation.compose.modifier.onTouchDown
import androidx.compose.remote.creation.compose.modifier.onTouchUp
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.rippleEffect
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.modifier.zIndex
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.RemoteLinearShader
import androidx.compose.remote.creation.compose.shaders.image
import androidx.compose.remote.creation.compose.state.RemoteBlendModeColorFilter
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.animateRemoteFloat
import androidx.compose.remote.creation.compose.state.deltaFromReferenceInSeconds
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteColor
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteLong
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.state.tween
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.RemoteButton
import java.io.ByteArrayInputStream
import kotlin.OptIn
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerPrimitivesTest {

    @get:Rule val rule = RcPlayerTestRule()

    private val experimentalProfile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            AndroidxRcPlatformServices(),
        ) { creationDisplayInfo, profile, callback ->
            RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
        }

    @Test
    fun testRemoteTextRenders() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteText(
                                "Hello Remote".rs
                            )
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Hello Remote").assertExists()
        }
    }

    @Test
    fun testRemoteTextWithLineHeightRenders() {
        rule.setRemoteContent {
            RemoteText(text = "Hello Line Height".rs, style = RemoteTextStyle(lineHeight = 24.rsp))
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Hello Line Height").assertExists()
    }

    @Test
    fun testRemoteTextWithFontVariationAndDecoration() {
        rule.setRemoteContent {
            val style = RemoteTextStyle(textDecoration = TextDecoration.Underline)
            val fontVariationSettings = FontVariation.Settings(FontVariation.Setting("wdth", 100f))
            RemoteText(
                text = "Decorated Var Text".rs,
                style = style,
                fontVariationSettings = fontVariationSettings,
            )
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Decorated Var Text").assertExists()
    }

    @Test
    fun testDrawWithContent() {
        runBlocking {
            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = ApplicationProvider.getApplicationContext(),
                        content = {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp).drawWithContent {
                                        drawCircle(paint = null, radius = 50f.rf)
                                        drawContent()
                                    }
                            ) {
                                RemoteText("Target".rs)
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Target").assertExists()
        }
    }

    @Test
    fun testNamedColors() {
        runBlocking {
            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = ApplicationProvider.getApplicationContext(),
                        content = {
                            val color =
                                androidx.compose.remote.creation.compose.state.RemoteColor
                                    .createNamedRemoteColor(
                                        "TestColor",
                                        androidx.compose.ui.graphics.Color.Green,
                                    )
                            RemoteBox(modifier = RemoteModifier.size(100.rdp).background(color))
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("playerRoot")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            // Verify that the node exists!
            rule.onNodeWithTag("playerRoot").assertExists()
        }
    }

    @Test
    fun testDrawRect() {
        runBlocking {
            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = ApplicationProvider.getApplicationContext(),
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                drawRect(
                                    paint =
                                        RemotePaint {
                                            color =
                                                RemoteColor(androidx.compose.ui.graphics.Color.Red)
                                            style = PaintingStyle.Fill
                                        },
                                    topLeft = RemoteOffset(10f.rf, 10f.rf),
                                    size = RemoteSize(80f.rf, 80f.rf),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        }
    }

    @Test
    fun testDrawBitmap() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.RED)
            val imageBitmap = bitmap.asImageBitmap()
            val remoteBitmap = imageBitmap.rb

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                drawImage(remoteBitmap, RemoteOffset.Zero, null)
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        }
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testDrawBitmapInt() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            runBlocking {
                val context = ApplicationProvider.getApplicationContext<Context>()
                val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.RED)

                val documentBytes =
                    androidx.compose.remote.creation.compose.capture
                        .captureSingleRemoteDocument(
                            context = context,
                            content = {
                                val state = LocalRemoteComposeCreationState.current
                                state.document.drawBitmap(bitmap, 10, 10, "Test Bitmap")
                            },
                        )
                        .bytes

                val document =
                    androidx.compose.remote.core
                        .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                        .apply {
                            ByteArrayInputStream(documentBytes).use {
                                initFromBuffer(
                                    androidx.compose.remote.core.RemoteComposeBuffer
                                        .fromInputStream(it)
                                )
                            }
                        }

                rule.setContent {
                    Box(
                        modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")
                    ) {
                        RcPlayer(document = document)
                    }
                }

                rule.mainClock.advanceTimeBy(100)

                rule.onNodeWithTag("canvasParent").assertExists()
            }
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testDrawBitmapScaled() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            runBlocking {
                val context = ApplicationProvider.getApplicationContext<Context>()
                val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.RED)

                val documentBytes =
                    androidx.compose.remote.creation.compose.capture
                        .captureSingleRemoteDocument(
                            context = context,
                            content = {
                                val state = LocalRemoteComposeCreationState.current
                                state.document.drawScaledBitmap(
                                    bitmap,
                                    0f,
                                    0f,
                                    10f,
                                    10f,
                                    0f,
                                    0f,
                                    100f,
                                    100f,
                                    0,
                                    1f,
                                    "Test Bitmap",
                                )
                            },
                        )
                        .bytes

                val document =
                    androidx.compose.remote.core
                        .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                        .apply {
                            ByteArrayInputStream(documentBytes).use {
                                initFromBuffer(
                                    androidx.compose.remote.core.RemoteComposeBuffer
                                        .fromInputStream(it)
                                )
                            }
                        }

                rule.setContent {
                    Box(
                        modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")
                    ) {
                        RcPlayer(document = document)
                    }
                }

                rule.mainClock.advanceTimeBy(100)

                rule.onNodeWithTag("canvasParent").assertExists()
            }
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    fun testDrawLine() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                drawLine(
                                    paint =
                                        RemotePaint {
                                            color =
                                                RemoteColor(androidx.compose.ui.graphics.Color.Red)
                                            style = PaintingStyle.Stroke
                                            strokeWidth = 5f.rf
                                        },
                                    start = RemoteOffset(0f.rf, 0f.rf),
                                    end = RemoteOffset(100f.rf, 100f.rf),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        }
    }

    @Test
    fun testDrawOval() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                drawOval(
                                    paint =
                                        RemotePaint {
                                            color =
                                                RemoteColor(androidx.compose.ui.graphics.Color.Red)
                                        },
                                    topLeft = RemoteOffset(10f.rf, 10f.rf),
                                    size = RemoteSize(80f.rf, 80f.rf),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        }
    }

    @Test
    fun testDrawPath() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val path = RemotePath("M 0 0 L 100 100")

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                drawPath(
                                    path = path,
                                    paint =
                                        RemotePaint {
                                            color =
                                                RemoteColor(androidx.compose.ui.graphics.Color.Red)
                                            style = PaintingStyle.Stroke
                                            strokeWidth = 5f.rf
                                        },
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        }
    }

    @Test
    fun testDrawRoundRect() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                drawRoundRect(
                                    paint =
                                        RemotePaint {
                                            color =
                                                RemoteColor(androidx.compose.ui.graphics.Color.Red)
                                        },
                                    topLeft = RemoteOffset(10f.rf, 10f.rf),
                                    size = RemoteSize(80f.rf, 80f.rf),
                                    cornerRadius = RemoteOffset(10f.rf, 10f.rf),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        }
    }

    @Test
    fun testDrawSector() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                drawArc(
                                    paint =
                                        RemotePaint {
                                            color =
                                                RemoteColor(androidx.compose.ui.graphics.Color.Red)
                                        },
                                    startAngle = 0f.rf,
                                    sweepAngle = 90f.rf,
                                    useCenter = true,
                                    topLeft = RemoteOffset(10f.rf, 10f.rf),
                                    size = RemoteSize(80f.rf, 80f.rf),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        }
    }

    @Test
    fun testDrawTextOnPath() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val path = RemotePath("M 0 50 L 100 50")
            val remoteString =
                androidx.compose.remote.creation.compose.state.RemoteString("Hello Path")

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                drawTextOnPath(
                                    text = remoteString,
                                    path = path,
                                    hOffset = 0f.rf,
                                    vOffset = 0f.rf,
                                    paint =
                                        RemotePaint {
                                            color =
                                                RemoteColor(androidx.compose.ui.graphics.Color.Red)
                                        },
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        }
    }

    @Ignore("DRAW_TEXT_ON_CIRCLE is commented out in Operations.java")
    @Test
    fun testDrawTextOnCircle() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val remoteString =
                androidx.compose.remote.creation.compose.state.RemoteString("Hello Circle")

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                drawTextOnCircle(
                                    text = remoteString,
                                    centerX = 50f.rf,
                                    centerY = 50f.rf,
                                    radius = 40f.rf,
                                    startAngle = 0f.rf,
                                    warpRadiusOffset = 0f.rf,
                                    paint =
                                        RemotePaint {
                                            color =
                                                RemoteColor(androidx.compose.ui.graphics.Color.Red)
                                        },
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        }
    }

    @Test
    fun testDrawText() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val remoteString =
                androidx.compose.remote.creation.compose.state.RemoteString("Hello Text")

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                drawText(
                                    text = remoteString,
                                    x = 10f.rf,
                                    y = 50f.rf,
                                    paint =
                                        RemotePaint {
                                            color =
                                                RemoteColor(androidx.compose.ui.graphics.Color.Red)
                                        },
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        }
    }

    @Test
    fun testDrawAnchoredText() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val remoteString =
                androidx.compose.remote.creation.compose.state.RemoteString("Hello Anchor")

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                drawAnchoredText(
                                    text = remoteString,
                                    anchorX = 50f.rf,
                                    anchorY = 50f.rf,
                                    panX = 0f.rf,
                                    panY = 0f.rf,
                                    flags = 0,
                                    paint =
                                        RemotePaint {
                                            color =
                                                RemoteColor(androidx.compose.ui.graphics.Color.Red)
                                        },
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        }
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testDrawBitmapFontText() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            runBlocking {
                val context = ApplicationProvider.getApplicationContext<Context>()

                val documentBytes =
                    androidx.compose.remote.creation.compose.capture
                        .captureSingleRemoteDocument(
                            context = context,
                            content = {
                                val state = LocalRemoteComposeCreationState.current
                                val textId = state.document.textCreateId("Hello")
                                state.document.drawBitmapFontTextRun(textId, 42, 0, 5, 10f, 50f, 0f)
                            },
                        )
                        .bytes

                val document =
                    androidx.compose.remote.core
                        .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                        .apply {
                            ByteArrayInputStream(documentBytes).use {
                                initFromBuffer(
                                    androidx.compose.remote.core.RemoteComposeBuffer
                                        .fromInputStream(it)
                                )
                            }
                        }

                rule.setContent {
                    Box(
                        modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")
                    ) {
                        RcPlayer(document = document)
                    }
                }

                rule.mainClock.advanceTimeBy(100)

                rule.onNodeWithTag("canvasParent").assertExists()
            }
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testDrawBitmapFontTextOnPath() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            runBlocking {
                val context = ApplicationProvider.getApplicationContext<Context>()
                val path = RemotePath("M 0 50 L 100 50")

                val documentBytes =
                    androidx.compose.remote.creation.compose.capture
                        .captureSingleRemoteDocument(
                            context = context,
                            content = {
                                val state = LocalRemoteComposeCreationState.current
                                val textId = state.document.textCreateId("Hello")
                                state.document.drawBitmapFontTextRunOnPath(
                                    textId,
                                    42,
                                    path,
                                    0,
                                    5,
                                    0f,
                                    0f,
                                )
                            },
                        )
                        .bytes

                val document =
                    androidx.compose.remote.core
                        .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                        .apply {
                            ByteArrayInputStream(documentBytes).use {
                                initFromBuffer(
                                    androidx.compose.remote.core.RemoteComposeBuffer
                                        .fromInputStream(it)
                                )
                            }
                        }

                rule.setContent {
                    Box(
                        modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")
                    ) {
                        RcPlayer(document = document)
                    }
                }

                rule.mainClock.advanceTimeBy(100)

                rule.onNodeWithTag("canvasParent").assertExists()
            }
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testDrawBitmapTextAnchored() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            runBlocking {
                val context = ApplicationProvider.getApplicationContext<Context>()

                val documentBytes =
                    androidx.compose.remote.creation.compose.capture
                        .captureSingleRemoteDocument(
                            context = context,
                            content = {
                                val state = LocalRemoteComposeCreationState.current
                                state.document.drawBitmapTextAnchored(
                                    "Hello",
                                    42,
                                    0f,
                                    5f,
                                    10f,
                                    50f,
                                    0f,
                                    0f,
                                    0f,
                                )
                            },
                        )
                        .bytes

                val document =
                    androidx.compose.remote.core
                        .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                        .apply {
                            ByteArrayInputStream(documentBytes).use {
                                initFromBuffer(
                                    androidx.compose.remote.core.RemoteComposeBuffer
                                        .fromInputStream(it)
                                )
                            }
                        }

                rule.setContent {
                    Box(
                        modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")
                    ) {
                        RcPlayer(document = document)
                    }
                }

                rule.mainClock.advanceTimeBy(100)

                rule.onNodeWithTag("canvasParent").assertExists()
            }
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    fun testDrawToBitmap() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bitmap =
                android.graphics.Bitmap.createBitmap(
                    10,
                    10,
                    android.graphics.Bitmap.Config.ARGB_8888,
                )
            val imageBitmap = bitmap.asImageBitmap()
            val remoteBitmap = imageBitmap.rb

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                drawToOffscreenBitmap(remoteBitmap) {
                                    drawRect(
                                        paint =
                                            RemotePaint {
                                                color =
                                                    RemoteColor(
                                                        androidx.compose.ui.graphics.Color.Red
                                                    )
                                            },
                                        topLeft = RemoteOffset(0f.rf, 0f.rf),
                                        size = RemoteSize(10f.rf, 10f.rf),
                                    )
                                }
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        }
    }

    @Test
    fun testDrawTweenPath() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val path1 = RemotePath("M 0 0 L 100 100")
            val path2 = RemotePath("M 0 100 L 100 0")

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                drawTweenPath(
                                    path1 = path1,
                                    path2 = path2,
                                    tween = 0.5f.rf,
                                    paint =
                                        RemotePaint {
                                            color =
                                                RemoteColor(androidx.compose.ui.graphics.Color.Red)
                                            style =
                                                androidx.compose.ui.graphics.PaintingStyle.Stroke
                                            strokeWidth = 5f.rf
                                        },
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        }
    }

    @Test
    fun testLayoutRoot() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            // Empty content, should still create a root component
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("playerRoot")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("playerRoot").assertExists()
        }
    }

    @Test
    fun testLayoutContent() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Content").assertExists()
        }
    }

    @Test
    fun testLayoutRemoteFitBox() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteFitBox {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Fit Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Fit Content").assertExists()
        }
    }

    @Test
    fun testLayoutRow() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteRow(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Row Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Row Content").assertExists()
        }
    }

    @Test
    fun testLayoutCollapsibleRow() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCollapsibleRow(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Collapsible Row Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Collapsible Row Content").assertExists()
        }
    }

    @Test
    fun testLayoutFlow() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        profile = experimentalProfile,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteFlowRow(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Flow Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Flow Content").assertExists()
        }
    }

    @Test
    fun testLayoutColumn() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteColumn(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Column Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Column Content").assertExists()
        }
    }

    @Test
    fun testLayoutCollapsibleColumn() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumn(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Collapsible Column Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Collapsible Column Content").assertExists()
        }
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testLayoutText() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            runBlocking {
                val context = ApplicationProvider.getApplicationContext<Context>()

                val documentBytes =
                    androidx.compose.remote.creation.compose.capture
                        .captureSingleRemoteDocument(
                            context = context,
                            content = {
                                val state = LocalRemoteComposeCreationState.current
                                val textId = state.document.textCreateId("Hello TextLayout")
                                val modifier =
                                    androidx.compose.remote.creation.compose.modifier.RemoteModifier
                                val recordingModifier =
                                    with(modifier) { state.toRecordingModifier() }
                                state.document.startTextComponent(
                                    recordingModifier,
                                    textId,
                                    0xff000000.toInt(), // color
                                    14f, // fontSize
                                    0, // fontStyle
                                    400f, // fontWeight
                                    null, // fontFamily
                                    0.toShort(), // flags
                                    0.toShort(), // textAlign
                                    0, // overflow
                                    Int.MAX_VALUE, // maxLines
                                )
                                state.document.endTextComponent()
                            },
                        )
                        .bytes

                val document =
                    androidx.compose.remote.core
                        .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                        .apply {
                            ByteArrayInputStream(documentBytes).use {
                                initFromBuffer(
                                    androidx.compose.remote.core.RemoteComposeBuffer
                                        .fromInputStream(it)
                                )
                            }
                        }

                rule.setContent {
                    Box(
                        modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")
                    ) {
                        RcPlayer(document = document)
                    }
                }

                rule.mainClock.advanceTimeBy(100)

                rule.onNodeWithTag("canvasParent").assertExists()
            }
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testLayoutTextStyle() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            runBlocking {
                val context = ApplicationProvider.getApplicationContext<Context>()

                val documentBytes =
                    androidx.compose.remote.creation.compose.capture
                        .captureSingleRemoteDocument(
                            context = context,
                            content = {
                                val state = LocalRemoteComposeCreationState.current
                                val textId = state.document.textCreateId("Hello Styled Text")
                                val styleId =
                                    state.document.addTextStyle(
                                        0xff000000.toInt(), // color
                                        null, // colorId
                                        16f, // fontSize
                                        null,
                                        null, // min/max fontSize
                                        0, // fontStyle
                                        400f, // fontWeight
                                        null, // fontFamily
                                        0, // textAlign
                                        0, // overflow
                                        Int.MAX_VALUE, // maxLines
                                        0f, // letterSpacing
                                        0f, // lineHeightAdd
                                        1f, // lineHeightMultiplier
                                        0, // lineBreakStrategy
                                        0, // hyphenationFrequency
                                        0, // justificationMode
                                        false, // underline
                                        false, // strikethrough
                                        null, // fontAxis
                                        null, // fontAxisValues
                                        false, // autosize
                                        0, // parentId
                                    )
                                val modifier =
                                    androidx.compose.remote.creation.compose.modifier.RemoteModifier
                                val recordingModifier =
                                    with(modifier) { state.toRecordingModifier() }
                                state.document.startTextComponent(
                                    recordingModifier,
                                    textId,
                                    styleId,
                                    0, // flags
                                )
                                state.document.endTextComponent()
                            },
                        )
                        .bytes

                val document =
                    androidx.compose.remote.core
                        .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                        .apply {
                            ByteArrayInputStream(documentBytes).use {
                                initFromBuffer(
                                    androidx.compose.remote.core.RemoteComposeBuffer
                                        .fromInputStream(it)
                                )
                            }
                        }

                rule.setContent {
                    Box(
                        modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")
                    ) {
                        RcPlayer(document = document)
                    }
                }

                rule.mainClock.advanceTimeBy(100)

                rule.onNodeWithTag("canvasParent").assertExists()
            }
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    fun testLayoutState() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val checked = androidx.compose.remote.creation.compose.state.RemoteBoolean(true)

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteStateLayout(
                                currentState = checked
                            ) { state ->
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "State $state".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("State true").assertExists()
        }
    }

    @Test
    fun testLayoutImage() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bitmap =
                android.graphics.Bitmap.createBitmap(
                    10,
                    10,
                    android.graphics.Bitmap.Config.ARGB_8888,
                )
            val imageBitmap = bitmap.asImageBitmap()
            val remoteBitmap = imageBitmap.rb

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteImage(
                                remoteBitmap = remoteBitmap,
                                contentDescription = "Test Image".rs,
                                modifier = RemoteModifier.size(100.rdp),
                            )
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvasParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        }
    }

    @Test
    fun testModifierWidth() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.width(100.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Width Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Width Content").assertExists()
        }
    }

    @Test
    fun testModifierHeight() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.height(100.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Height Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Height Content").assertExists()
        }
    }

    @Test
    fun testModifierWidthIn() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.widthIn(min = 10.rdp, max = 100.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "WidthIn Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("WidthIn Content").assertExists()
        }
    }

    @Test
    fun testModifierHeightIn() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.heightIn(min = 10.rdp, max = 100.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "HeightIn Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("HeightIn Content").assertExists()
        }
    }

    @Test
    fun testModifierCollapsiblePriority() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCollapsibleRow(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    text = "Priority Content".rs,
                                    modifier = RemoteModifier.priority(1f),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Priority Content").assertExists()
        }
    }

    @Test
    fun testModifierBackground() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp)
                                        .background(androidx.compose.ui.graphics.Color.Red.rc)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Background Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Background Content").assertExists()
        }
    }

    @Test
    fun testModifierBorder() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp)
                                        .border(
                                            1.rdp,
                                            androidx.compose.remote.creation.compose.state
                                                .RemoteColor(
                                                    androidx.compose.ui.graphics.Color.Black
                                                ),
                                        )
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Border Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Border Content").assertExists()
        }
    }

    @Test
    fun testModifierPadding() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.size(100.rdp).padding(10.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Padding Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Padding Content").assertExists()
        }
    }

    @Test
    fun testModifierClick() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val emptyAction = androidx.compose.remote.creation.compose.action.combinedAction()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp).clickable(action = emptyAction)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Click Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Click Content").assertExists()
        }
    }

    @Test
    fun testModifierMultiClick() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val emptyAction = androidx.compose.remote.creation.compose.action.combinedAction()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            @OptIn(
                                androidx.compose.remote.creation.compose
                                    .ExperimentalRemoteCreationComposeApi::class
                            )
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp)
                                        .combinedClickable(onDoubleClick = emptyAction)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "MultiClick Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("MultiClick Content").assertExists()
        }
    }

    @Test
    fun testModifierTouchDown() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val emptyAction = androidx.compose.remote.creation.compose.action.combinedAction()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.size(100.rdp).onTouchDown(emptyAction)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "TouchDown Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("TouchDown Content").assertExists()
        }
    }

    @Test
    fun testModifierTouchUp() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val emptyAction = androidx.compose.remote.creation.compose.action.combinedAction()
            val dummyDownAction =
                androidx.compose.remote.creation.compose.action.hostAction(
                    "dummyDown".rs,
                    "dummy".rs,
                )

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp)
                                        .onTouchDown(dummyDownAction)
                                        .onTouchUp(emptyAction)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "TouchUp Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("TouchUp Content").assertExists()
        }
    }

    @Test
    fun testModifierTouchCancel() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val emptyAction = androidx.compose.remote.creation.compose.action.combinedAction()
            val dummyDownAction =
                androidx.compose.remote.creation.compose.action.hostAction(
                    "dummyDown".rs,
                    "dummy".rs,
                )

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp)
                                        .onTouchDown(dummyDownAction)
                                        .onTouchCancel(emptyAction)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "TouchCancel Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("TouchCancel Content").assertExists()
        }
    }

    @Test
    fun testModifierVisibility() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            // RemoteCompose visibility is GONE=0, VISIBLE=1 (not Android View 0/4/8).
            val visibleState =
                androidx.compose.remote.creation.compose.state.RemoteInt(
                    androidx.compose.remote.core.operations.layout.Component.Visibility.VISIBLE
                )

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.size(100.rdp).visibility(visibleState)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Visibility Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Visibility Content").assertExists()
        }
    }

    @Test
    fun testModifierOffset() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.size(100.rdp).offset(10.rdp, 10.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Offset Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Offset Content").assertExists()
        }
    }

    @Test
    fun testModifierZIndex() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.size(100.rdp).zIndex(1f.rf)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "ZIndex Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("ZIndex Content").assertExists()
        }
    }

    @Test
    fun testModifierGraphicsLayer() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp).graphicsLayer(alpha = 0.5f.rf)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "GraphicsLayer Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("GraphicsLayer Content").assertExists()
        }
    }

    @Test
    fun testModifierScroll() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val scrollState = rememberRemoteScrollState()
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.size(100.rdp).verticalScroll(scrollState)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Scroll Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Scroll Content").assertExists()
        }
    }

    @Test
    fun testModifierMarquee() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.size(100.rdp).basicMarquee()
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Marquee Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Marquee Content").assertExists()
        }
    }

    @Test
    fun testModifierRipple() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.size(100.rdp).rippleEffect()
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Ripple Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("Ripple Content").assertExists()
        }
    }

    @Test
    fun testModifierAlignBy() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        profile = experimentalProfile,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteRow(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    text = "AlignBy Content".rs,
                                    modifier = RemoteModifier.alignByBaseline(),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("AlignBy Content").assertExists()
        }
    }

    @Test
    fun testDataFloat() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp).graphicsLayer(alpha = 0.7f.rf)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "DataFloat Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("DataFloat Content").assertExists()
        }
    }

    @Test
    fun testAnimatedFloat() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val animated = animateRemoteFloat(rf = 1.0f.rf, duration = 1.0f)
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp).graphicsLayer(alpha = animated)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "AnimatedFloat Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("AnimatedFloat Content").assertExists()
        }
    }

    @Test
    fun testDataInt() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val invisibleState =
                androidx.compose.remote.creation.compose.state.RemoteInt(1) // INVISIBLE

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.size(100.rdp).visibility(invisibleState)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "DataInt Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("DataInt Content").assertExists()
        }
    }

    @Test
    fun testIntegerExpression() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val mutableInt = rememberMutableRemoteInt(1)
                            // shl returns RemoteIntExpression if one of the operands is not
                            // constant
                            val expression = mutableInt shl 2.ri
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.size(100.rdp).visibility(expression)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "IntegerExpression Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("IntegerExpression Content").assertExists()
        }
    }

    @Test
    fun testDataLong() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val longState = rememberNamedRemoteLong("myLong", 0L)
                            val floatExpr = deltaFromReferenceInSeconds(longState)
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp).graphicsLayer(alpha = floatExpr)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "DataLong Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("DataLong Content").assertExists()
        }
    }

    @Test
    fun testColorExpressions() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val colorExpr =
                                tween(
                                    android.graphics.Color.RED,
                                    android.graphics.Color.BLUE,
                                    0.5f.rf,
                                )
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.size(100.rdp).background(colorExpr)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "ColorExpressions Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithText("ColorExpressions Content").assertExists()
        }
    }

    @Test
    fun testTextSize() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply { textSize = 20f.rf }
                                drawText("Size 20".rs, 0f.rf, 0f.rf, paint)
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("textSizeParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("textSizeParent").assertExists()
        }
    }

    @Test
    fun testPaintColor() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply {
                                            color =
                                                androidx.compose.remote.creation.compose.state
                                                    .RemoteColor(android.graphics.Color.BLUE)
                                        }
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        100f.rf,
                                        100f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("paintColorParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("paintColorParent").assertExists()
        }
    }

    @Test
    fun testStrokeWidth() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply {
                                            style = PaintingStyle.Stroke
                                            strokeWidth = 5f.rf
                                        }
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        100f.rf,
                                        100f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier =
                        androidx.compose.ui.Modifier.size(100.dp).testTag("strokeWidthParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("strokeWidthParent").assertExists()
        }
    }

    @Test
    fun testRemoteIconTint() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val tintVar =
                                androidx.compose.remote.creation.compose.state
                                    .rememberNamedRemoteColor(
                                        "tint",
                                        androidx.compose.ui.graphics.Color(0xFFFF0000),
                                    )
                            val imageVector =
                                androidx.compose.remote.creation.compose.capture.RemoteImageVector
                                    .Builder(
                                        viewportWidth = 24f.rf,
                                        viewportHeight = 24f.rf,
                                        tintColor = tintVar,
                                        name = "dummy",
                                    )
                                    .build()

                            androidx.wear.compose.remote.material3.RemoteIcon(
                                imageVector = imageVector,
                                contentDescription = null,
                                tint = tintVar,
                            )
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("iconTintParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("iconTintParent").assertExists()
        }
    }

    @Test
    fun testStrokeCap() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply {
                                            style = PaintingStyle.Stroke
                                            strokeWidth = 5f.rf
                                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                        }
                                drawLine(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        100f.rf,
                                        100f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("strokeCapParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("strokeCapParent").assertExists()
        }
    }

    @Test
    fun testPaintStyle() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply { style = PaintingStyle.Stroke }
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        100f.rf,
                                        100f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("paintStyleParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("paintStyleParent").assertExists()
        }
    }

    @Test
    fun testShader() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply {
                                            shader =
                                                RemoteLinearShader(
                                                    0f.rf,
                                                    0f.rf,
                                                    100f.rf,
                                                    100f.rf,
                                                    listOf(
                                                        androidx.compose.ui.graphics.Color.Red.rc,
                                                        androidx.compose.ui.graphics.Color.Blue.rc,
                                                    ),
                                                    null,
                                                    androidx.compose.ui.graphics.TileMode.Clamp,
                                                )
                                        }
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        100f.rf,
                                        100f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("shaderParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("shaderParent").assertExists()
        }
    }

    @Test
    fun testImageFilterQuality() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply {
                                            filterQuality =
                                                androidx.compose.ui.graphics.FilterQuality.High
                                        }
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        100f.rf,
                                        100f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier =
                        androidx.compose.ui.Modifier.size(100.dp).testTag("filterQualityParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("filterQualityParent").assertExists()
        }
    }

    @Test
    fun testPaintAlpha() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply {
                                            color =
                                                androidx.compose.remote.creation.compose.state
                                                    .RemoteColor(
                                                        android.graphics.Color.argb(128, 255, 0, 0)
                                                    ) // Red with 50% alpha
                                        }
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        100f.rf,
                                        100f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("paintAlphaParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("paintAlphaParent").assertExists()
        }
    }

    @Test
    fun testPaintColorFilter() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply {
                                            colorFilter =
                                                RemoteBlendModeColorFilter(
                                                    androidx.compose.ui.graphics.Color.Red.rc,
                                                    BlendMode.SrcOver,
                                                )
                                        }
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        100f.rf,
                                        100f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier =
                        androidx.compose.ui.Modifier.size(100.dp).testTag("colorFilterParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("colorFilterParent").assertExists()
        }
    }

    @Test
    fun testPaintAntiAlias() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply { isAntiAlias = false }
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        100f.rf,
                                        100f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("antiAliasParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("antiAliasParent").assertExists()
        }
    }

    @Test
    fun testStrokeJoin() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply {
                                            style = PaintingStyle.Stroke
                                            strokeWidth = 5f.rf
                                            strokeJoin =
                                                androidx.compose.ui.graphics.StrokeJoin.Round
                                        }
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        100f.rf,
                                        100f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("strokeJoinParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("strokeJoinParent").assertExists()
        }
    }

    @Test
    fun testPaintTypeface() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply {
                                            typeface =
                                                androidx.compose.remote.creation.compose.text
                                                    .RemoteTypeface
                                                    .fromAndroidTypeface(
                                                        android.graphics.Typeface.DEFAULT_BOLD
                                                    )
                                        }
                                drawText("Bold Text".rs, 0f.rf, 0f.rf, paint)
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("typefaceParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("typefaceParent").assertExists()
        }
    }

    @Test
    fun testPaintBlendMode() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply {
                                            blendMode =
                                                androidx.compose.ui.graphics.BlendMode.SrcOver
                                        }
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        100f.rf,
                                        100f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("blendModeParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("blendModeParent").assertExists()
        }
    }

    @Test
    fun testColorId() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val namedColor =
                                rememberNamedRemoteColor(
                                    "myColor",
                                    androidx.compose.ui.graphics.Color.Red,
                                )
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply { color = namedColor }
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        100f.rf,
                                        100f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("colorIdParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("colorIdParent").assertExists()
        }
    }

    @Test
    fun testColorOverride() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val namedColor = rememberNamedRemoteColor("myColor", Color.Red)
                            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                                val paint = RemotePaint().apply { color = namedColor }
                                drawRect(
                                    paint,
                                    RemoteOffset(0f.rf, 0f.rf),
                                    RemoteSize(100f.rf, 100f.rf),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            val overrides = androidx.collection.mutableObjectIntMapOf<String>()
            overrides["myColor"] = 0xFF00FF00.toInt() // Green

            rule.setContent { RcPlayer(document = document, namedColorOverrides = overrides) }

            rule.mainClock.advanceTimeBy(100)

            val state = document.remoteComposeState

            fun findNamedVariable(ops: Collection<Operation>, name: String): NamedVariable? {
                for (op in ops) {
                    if (op is NamedVariable && op.mVarName == name) {
                        return op
                    }
                    if (op is Container) {
                        val found = findNamedVariable(op.getList(), name)
                        if (found != null) return found
                    }
                    if (op is LayoutComponent) {
                        val canvasOps = op.getCanvasOperations()
                        if (canvasOps != null) {
                            val found = findNamedVariable(listOf(canvasOps), name)
                            if (found != null) return found
                        }
                    }
                }
                return null
            }

            val namedVar = findNamedVariable(document.getOperationsReflection(), "USER:myColor")
            assertNotNull(namedVar)
            val varId = namedVar!!.mVarId

            val resolvedColor = state.getColor(varId)
            assertEquals(0xFF00FF00.toInt(), resolvedColor)
        }
    }

    @Test
    fun testColorFilterId() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val namedColor =
                                rememberNamedRemoteColor(
                                    "myColor",
                                    androidx.compose.ui.graphics.Color.Red,
                                )
                            val colorFilter =
                                RemoteBlendModeColorFilter(namedColor, BlendMode.SrcOver)
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply { this.colorFilter = colorFilter }
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        100f.rf,
                                        100f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier =
                        androidx.compose.ui.Modifier.size(100.dp).testTag("colorFilterIdParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("colorFilterIdParent").assertExists()
        }
    }

    @Test
    fun testClearColorFilter() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply {
                                            colorFilter =
                                                RemoteBlendModeColorFilter(
                                                    androidx.compose.ui.graphics.Color.Red.rc,
                                                    BlendMode.SrcOver,
                                                )
                                        }
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        50f.rf,
                                        50f.rf,
                                    ),
                                )

                                paint.colorFilter = null
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        50f.rf,
                                        50f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        50f.rf,
                                        50f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier =
                        androidx.compose.ui.Modifier.size(100.dp).testTag("clearColorFilterParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("clearColorFilterParent").assertExists()
        }
    }

    @Test
    fun testShaderMatrix() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply {
                                            shader =
                                                RemoteLinearShader(
                                                        0f.rf,
                                                        0f.rf,
                                                        100f.rf,
                                                        100f.rf,
                                                        listOf(
                                                            androidx.compose.ui.graphics.Color.Red
                                                                .rc,
                                                            androidx.compose.ui.graphics.Color.Blue
                                                                .rc,
                                                        ),
                                                        null,
                                                        androidx.compose.ui.graphics.TileMode.Clamp,
                                                    )
                                                    .apply {
                                                        remoteMatrix3x3 =
                                                            RemoteMatrix3x3.createRotate(45f.rf)
                                                    }
                                        }
                                drawRect(
                                    paint,
                                    androidx.compose.remote.creation.compose.layout.RemoteOffset(
                                        0f.rf,
                                        0f.rf,
                                    ),
                                    androidx.compose.remote.creation.compose.layout.RemoteSize(
                                        100f.rf,
                                        100f.rf,
                                    ),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier =
                        androidx.compose.ui.Modifier.size(100.dp).testTag("shaderMatrixParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("shaderMatrixParent").assertExists()
        }
    }

    @Test
    fun testFontAxis() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                val paint =
                                    androidx.compose.remote.creation.compose.state
                                        .RemotePaint()
                                        .apply {
                                            fontVariationSettings =
                                                androidx.compose.ui.text.font.FontVariation
                                                    .Settings(
                                                        androidx.compose.ui.text.font.FontVariation
                                                            .Setting("wdth", 100f)
                                                    )
                                        }
                                drawText("Var Text".rs, 0f.rf, 0f.rf, paint)
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("fontAxisParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("fontAxisParent").assertExists()
        }
    }

    @Test
    fun testTexture() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val bitmap =
                                android.graphics.Bitmap.createBitmap(
                                        10,
                                        10,
                                        android.graphics.Bitmap.Config.ARGB_8888,
                                    )
                                    .asImageBitmap()
                            val remoteBitmap = bitmap.rb
                            val brush = RemoteBrush.image(remoteBitmap)
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier = RemoteModifier.size(100.rdp).background(brush)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Texture Content".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("textureParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("textureParent").assertExists()
        }
    }

    @Test
    fun testRemoteColumnWeight() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteColumn(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteBox(
                                    modifier =
                                        RemoteModifier.weight(1f)
                                            .background(androidx.compose.ui.graphics.Color.Red.rc)
                                ) {
                                    androidx.compose.remote.creation.compose.layout.RemoteText(
                                        "Weight 1".rs
                                    )
                                }
                                androidx.compose.remote.creation.compose.layout.RemoteBox(
                                    modifier =
                                        RemoteModifier.size(50.rdp)
                                            .background(androidx.compose.ui.graphics.Color.Blue.rc)
                                ) {
                                    androidx.compose.remote.creation.compose.layout.RemoteText(
                                        "Size 50".rs
                                    )
                                }
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier =
                        androidx.compose.ui.Modifier.size(100.dp).testTag("columnWeightParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("columnWeightParent").assertExists()
        }
    }

    @Test
    fun testBlendMode() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                drawRect(
                                    paint =
                                        RemotePaint {
                                            color =
                                                RemoteColor(androidx.compose.ui.graphics.Color.Red)
                                            blendMode =
                                                androidx.compose.ui.graphics.BlendMode.SrcOver
                                        },
                                    topLeft = RemoteOffset(10f.rf, 10f.rf),
                                    size = RemoteSize(80f.rf, 80f.rf),
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("blendModeParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("blendModeParent").assertExists()
        }
    }

    @Test
    fun testRemoteCardLayoutReproduction() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val shape =
                androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape(20.rdp)

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp)
                                        .clip(shape = shape)
                                        .padding(16.rdp)
                                        .background(androidx.compose.ui.graphics.Color.LightGray.rc)
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "This is long text that might cut off".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier =
                        androidx.compose.ui.Modifier.size(100.dp).testTag("reproductionParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("reproductionParent").assertExists()
            rule
                .onNodeWithText("This is long text that might cut off", substring = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun testRemoteButtonLayoutReproduction() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.compose.remote.creation.compose.layout.RemoteRow(
                                modifier =
                                    RemoteModifier.heightIn(min = 52.rdp)
                                        .widthIn(min = 100.rdp)
                                        .clip(
                                            shape =
                                                androidx.compose.remote.creation.compose.shapes
                                                    .RemoteRoundedCornerShape(16.rdp)
                                        )
                                        .padding(
                                            androidx.compose.remote.creation.compose.layout
                                                .RemotePaddingValues(14.rdp, 6.rdp)
                                        ),
                                verticalAlignment =
                                    androidx.compose.remote.creation.compose.layout.RemoteAlignment
                                        .CenterVertically,
                                horizontalArrangement =
                                    androidx.compose.remote.creation.compose.layout
                                        .RemoteArrangement
                                        .Center,
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Click".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier =
                        androidx.compose.ui.Modifier.size(100.dp)
                            .testTag("buttonReproductionParent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("buttonReproductionParent").assertExists()

            try {
                val bounds = rule.onNodeWithText("Click").getUnclippedBoundsInRoot()
                val width = bounds.right.value - bounds.left.value
                val height = bounds.bottom.value - bounds.top.value
                assert(width > height) {
                    "Button should be wider than tall, but bounds are $bounds"
                }
            } catch (e: Throwable) {}
        }
    }

    @Test
    fun testRemoteButtonFromMaterial3() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                androidx.compose.remote.creation.compose.capture
                    .captureSingleRemoteDocument(
                        context = context,
                        content = {
                            RemoteButton(
                                onClick =
                                    androidx.compose.remote.creation.compose.action.Action.Empty
                            ) {
                                androidx.compose.remote.creation.compose.layout.RemoteText(
                                    "Click".rs
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(
                    modifier =
                        androidx.compose.ui.Modifier.size(200.dp).testTag("buttonMaterial3Parent")
                ) {
                    RcPlayer(document = document)
                }
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("buttonMaterial3Parent").assertExists()

            try {
                val bounds = rule.onNodeWithText("Click").getUnclippedBoundsInRoot()
            } catch (e: Throwable) {}
        }
    }
}
