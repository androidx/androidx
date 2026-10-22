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
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.action.combinedAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumn
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleRow
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteFitBox
import androidx.compose.remote.creation.compose.layout.RemoteFlowRow
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.RemoteScrollState
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
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteBlendModeColorFilter
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteColor.Companion.createNamedRemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloatArray.Companion.createNamedRemoteFloatArray
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteLong.Companion.createNamedRemoteLong
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3.Companion.createRotate
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.deltaFromReferenceInSeconds
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.state.tween
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.creation.compose.text.RemoteTypeface
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
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
import androidx.wear.compose.remote.material3.RemoteIcon
import java.io.ByteArrayInputStream
import kotlin.OptIn
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
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
        rule.setRemoteContent {
            RemoteText("Hello Remote".rs)
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Hello Remote").assertExists()
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
        rule.setRemoteContent {
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp).drawWithContent {
                        drawCircle(paint = null, radius = 50f.rf)
                        drawContent()
                    }
            ) {
                RemoteText("Target".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Target").assertExists()
    }

    @Test
    fun testNamedColors() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("playerRoot")) {
            val color =
                createNamedRemoteColor(
                    "TestColor",
                    Color.Green,
                )
            RemoteBox(modifier = RemoteModifier.size(100.rdp).background(color))
        }

        rule.mainClock.advanceTimeBy(100)

        // Verify that the node exists!
        rule.onNodeWithTag("playerRoot").assertExists()
    }

    @Test
    fun testDrawRect() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawRect(
                    paint =
                        RemotePaint {
                            color = RemoteColor(Color.Red)
                            style = PaintingStyle.Fill
                        },
                    topLeft = RemoteOffset(10f.rf, 10f.rf),
                    size = RemoteSize(80f.rf, 80f.rf),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("canvasParent").assertExists()
    }

    @Test
    fun testDrawBitmap() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(AndroidColor.RED)
        val imageBitmap = bitmap.asImageBitmap()
        val remoteBitmap = imageBitmap.rb

        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawImage(remoteBitmap, RemoteOffset.Zero, null)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("canvasParent").assertExists()
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testDrawBitmapInt() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(AndroidColor.RED)

            rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
                val state = LocalRemoteComposeCreationState.current
                state.document.drawBitmap(bitmap, 10, 10, "Test Bitmap")
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testDrawBitmapScaled() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(AndroidColor.RED)

            rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
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
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    fun testDrawLine() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawLine(
                    paint =
                        RemotePaint {
                            color = RemoteColor(Color.Red)
                            style = PaintingStyle.Stroke
                            strokeWidth = 5f.rf
                        },
                    start = RemoteOffset(0f.rf, 0f.rf),
                    end = RemoteOffset(100f.rf, 100f.rf),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("canvasParent").assertExists()
    }

    @Test
    fun testDrawOval() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawOval(
                    paint =
                        RemotePaint {
                            color = RemoteColor(Color.Red)
                        },
                    topLeft = RemoteOffset(10f.rf, 10f.rf),
                    size = RemoteSize(80f.rf, 80f.rf),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("canvasParent").assertExists()
    }

    @Test
    fun testDrawPath() {
        val path = RemotePath("M 0 0 L 100 100")

        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawPath(
                    path = path,
                    paint =
                        RemotePaint {
                            color = RemoteColor(Color.Red)
                            style = PaintingStyle.Stroke
                            strokeWidth = 5f.rf
                        },
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("canvasParent").assertExists()
    }

    @Test
    fun testDrawRoundRect() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawRoundRect(
                    paint =
                        RemotePaint {
                            color = RemoteColor(Color.Red)
                        },
                    topLeft = RemoteOffset(10f.rf, 10f.rf),
                    size = RemoteSize(80f.rf, 80f.rf),
                    cornerRadius = RemoteOffset(10f.rf, 10f.rf),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("canvasParent").assertExists()
    }

    @Test
    fun testDrawSector() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawArc(
                    paint =
                        RemotePaint {
                            color = RemoteColor(Color.Red)
                        },
                    startAngle = 0f.rf,
                    sweepAngle = 90f.rf,
                    useCenter = true,
                    topLeft = RemoteOffset(10f.rf, 10f.rf),
                    size = RemoteSize(80f.rf, 80f.rf),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("canvasParent").assertExists()
    }

    @Test
    fun testDrawTextOnPath() {
        val path = RemotePath("M 0 50 L 100 50")
        val remoteString = RemoteString("Hello Path")

        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawTextOnPath(
                    text = remoteString,
                    path = path,
                    hOffset = 0f.rf,
                    vOffset = 0f.rf,
                    paint =
                        RemotePaint {
                            color = RemoteColor(Color.Red)
                        },
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("canvasParent").assertExists()
    }

    @Ignore("DRAW_TEXT_ON_CIRCLE is commented out in Operations.java")
    @Test
    fun testDrawTextOnCircle() {
        val remoteString = RemoteString("Hello Circle")

        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawTextOnCircle(
                    text = remoteString,
                    centerX = 50f.rf,
                    centerY = 50f.rf,
                    radius = 40f.rf,
                    startAngle = 0f.rf,
                    warpRadiusOffset = 0f.rf,
                    paint =
                        RemotePaint {
                            color = RemoteColor(Color.Red)
                        },
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("canvasParent").assertExists()
    }

    @Test
    fun testDrawText() {
        val remoteString = RemoteString("Hello Text")

        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawText(
                    text = remoteString,
                    x = 10f.rf,
                    y = 50f.rf,
                    paint =
                        RemotePaint {
                            color = RemoteColor(Color.Red)
                        },
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("canvasParent").assertExists()
    }

    @Test
    fun testDrawAnchoredText() {
        val remoteString = RemoteString("Hello Anchor")

        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawAnchoredText(
                    text = remoteString,
                    anchorX = 50f.rf,
                    anchorY = 50f.rf,
                    panX = 0f.rf,
                    panY = 0f.rf,
                    flags = 0,
                    paint =
                        RemotePaint {
                            color = RemoteColor(Color.Red)
                        },
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("canvasParent").assertExists()
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testDrawBitmapFontText() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
                val state = LocalRemoteComposeCreationState.current
                val textId = state.document.textCreateId("Hello")
                state.document.drawBitmapFontTextRun(textId, 42, 0, 5, 10f, 50f, 0f)
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testDrawBitmapFontTextOnPath() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            val path = RemotePath("M 0 50 L 100 50")

            rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
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
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testDrawBitmapTextAnchored() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
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
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    fun testDrawToBitmap() {
        val bitmap =
            Bitmap.createBitmap(
                10,
                10,
                Bitmap.Config.ARGB_8888,
            )
        val imageBitmap = bitmap.asImageBitmap()
        val remoteBitmap = imageBitmap.rb

        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawToOffscreenBitmap(remoteBitmap) {
                    drawRect(
                        paint =
                            RemotePaint {
                                color = RemoteColor(Color.Red)
                            },
                        topLeft = RemoteOffset(0f.rf, 0f.rf),
                        size = RemoteSize(10f.rf, 10f.rf),
                    )
                }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("canvasParent").assertExists()
    }

    @Test
    fun testDrawTweenPath() {
        val path1 = RemotePath("M 0 0 L 100 100")
        val path2 = RemotePath("M 0 100 L 100 0")

        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawTweenPath(
                    path1 = path1,
                    path2 = path2,
                    tween = 0.5f.rf,
                    paint =
                        RemotePaint {
                            color = RemoteColor(Color.Red)
                            style = PaintingStyle.Stroke
                            strokeWidth = 5f.rf
                        },
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("canvasParent").assertExists()
    }

    @Test
    fun testLayoutRoot() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("playerRoot")) {
            // Empty content, should still create a root component
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("playerRoot").assertExists()
    }

    @Test
    fun testLayoutContent() {
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.size(100.rdp)) {
                RemoteText("Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Content").assertExists()
    }

    @Test
    fun testLayoutRemoteFitBox() {
        rule.setRemoteContent {
            RemoteFitBox {
                RemoteText("Fit Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Fit Content").assertExists()
    }

    @Test
    fun testLayoutRow() {
        rule.setRemoteContent {
            RemoteRow(modifier = RemoteModifier.size(100.rdp)) {
                RemoteText("Row Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Row Content").assertExists()
    }

    @Test
    fun testLayoutCollapsibleRow() {
        rule.setRemoteContent {
            RemoteCollapsibleRow(modifier = RemoteModifier.size(100.rdp)) {
                RemoteText("Collapsible Row Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Collapsible Row Content").assertExists()
    }

    @Test
    fun testLayoutFlow() {
        rule.setRemoteContent(profile = experimentalProfile) {
            RemoteFlowRow(modifier = RemoteModifier.size(100.rdp)) {
                RemoteText("Flow Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Flow Content").assertExists()
    }

    @Test
    fun testLayoutColumn() {
        rule.setRemoteContent {
            RemoteColumn(modifier = RemoteModifier.size(100.rdp)) {
                RemoteText("Column Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Column Content").assertExists()
    }

    @Test
    fun testLayoutCollapsibleColumn() {
        rule.setRemoteContent {
            RemoteCollapsibleColumn(modifier = RemoteModifier.size(100.rdp)) {
                RemoteText("Collapsible Column Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Collapsible Column Content").assertExists()
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testLayoutText() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
                val state = LocalRemoteComposeCreationState.current
                val textId = state.document.textCreateId("Hello TextLayout")
                val modifier = RemoteModifier
                val recordingModifier = with(modifier) { state.toRecordingModifier() }
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
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testLayoutTextStyle() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
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
                val modifier = RemoteModifier
                val recordingModifier = with(modifier) { state.toRecordingModifier() }
                state.document.startTextComponent(
                    recordingModifier,
                    textId,
                    styleId,
                    0, // flags
                )
                state.document.endTextComponent()
            }

            rule.mainClock.advanceTimeBy(100)

            rule.onNodeWithTag("canvasParent").assertExists()
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        }
    }

    @Test
    fun testLayoutState() {
        val checked = RemoteBoolean(true)

        rule.setRemoteContent {
            RemoteStateLayout(currentState = checked) { state ->
                RemoteText("State $state".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("State true").assertExists()
    }

    @Test
    fun testLayoutImage() {
        val bitmap =
            Bitmap.createBitmap(
                10,
                10,
                Bitmap.Config.ARGB_8888,
            )
        val imageBitmap = bitmap.asImageBitmap()
        val remoteBitmap = imageBitmap.rb

        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("canvasParent")) {
            RemoteImage(
                remoteBitmap = remoteBitmap,
                contentDescription = "Test Image".rs,
                modifier = RemoteModifier.size(100.rdp),
            )
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("canvasParent").assertExists()
    }

    @Test
    fun testModifierWidth() {
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.width(100.rdp)) {
                RemoteText("Width Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Width Content").assertExists()
    }

    @Test
    fun testModifierHeight() {
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.height(100.rdp)) {
                RemoteText("Height Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Height Content").assertExists()
    }

    @Test
    fun testModifierWidthIn() {
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.widthIn(min = 10.rdp, max = 100.rdp)) {
                RemoteText("WidthIn Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("WidthIn Content").assertExists()
    }

    @Test
    fun testModifierHeightIn() {
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.heightIn(min = 10.rdp, max = 100.rdp)) {
                RemoteText("HeightIn Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("HeightIn Content").assertExists()
    }

    @Test
    fun testModifierCollapsiblePriority() {
        rule.setRemoteContent {
            RemoteCollapsibleRow(modifier = RemoteModifier.size(100.rdp)) {
                RemoteText(
                    text = "Priority Content".rs,
                    modifier = RemoteModifier.collapsiblePriority(1f),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Priority Content").assertExists()
    }

    @Test
    fun testModifierBackground() {
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).background(Color.Red.rc)) {
                RemoteText("Background Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Background Content").assertExists()
    }

    @Test
    fun testModifierBorder() {
        rule.setRemoteContent {
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp)
                        .border(
                            1.rdp,
                            RemoteColor(Color.Black),
                        )
            ) {
                RemoteText("Border Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Border Content").assertExists()
    }

    @Test
    fun testModifierPadding() {
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).padding(10.rdp)) {
                RemoteText("Padding Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Padding Content").assertExists()
    }

    @Test
    fun testModifierClick() {
        val emptyAction = combinedAction()

        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).clickable(action = emptyAction)) {
                RemoteText("Click Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Click Content").assertExists()
    }

    @Test
    fun testModifierMultiClick() {
        val emptyAction = combinedAction()

        rule.setRemoteContent {
            @OptIn(ExperimentalRemoteCreationComposeApi::class)
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp).combinedClickable(onDoubleClick = emptyAction)
            ) {
                RemoteText("MultiClick Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("MultiClick Content").assertExists()
    }

    @Test
    fun testModifierTouchDown() {
        val emptyAction = combinedAction()

        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).onTouchDown(emptyAction)) {
                RemoteText("TouchDown Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("TouchDown Content").assertExists()
    }

    @Test
    fun testModifierTouchUp() {
        val emptyAction = combinedAction()
        val dummyDownAction =
            hostAction(
                "dummyDown".rs,
                "dummy".rs,
            )

        rule.setRemoteContent {
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp).onTouchDown(dummyDownAction).onTouchUp(emptyAction)
            ) {
                RemoteText("TouchUp Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("TouchUp Content").assertExists()
    }

    @Test
    fun testModifierTouchCancel() {
        val emptyAction = combinedAction()
        val dummyDownAction =
            hostAction(
                "dummyDown".rs,
                "dummy".rs,
            )

        rule.setRemoteContent {
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp)
                        .onTouchDown(dummyDownAction)
                        .onTouchCancel(emptyAction)
            ) {
                RemoteText("TouchCancel Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("TouchCancel Content").assertExists()
    }

    @Test
    fun testModifierVisibility() {
        // RemoteCompose visibility is GONE=0, VISIBLE=1 (not Android View 0/4/8).
        val visibleState = RemoteInt(Component.Visibility.VISIBLE)

        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).visibility(visibleState)) {
                RemoteText("Visibility Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Visibility Content").assertExists()
    }

    @Test
    fun testModifierOffset() {
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).offset(10.rdp, 10.rdp)) {
                RemoteText("Offset Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Offset Content").assertExists()
    }

    @Test
    fun testModifierZIndex() {
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).zIndex(1f.rf)) {
                RemoteText("ZIndex Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("ZIndex Content").assertExists()
    }

    @Test
    fun testModifierGraphicsLayer() {
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).graphicsLayer(alpha = 0.5f.rf)) {
                RemoteText("GraphicsLayer Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("GraphicsLayer Content").assertExists()
    }

    @Test
    fun testModifierScroll() {
        rule.setRemoteContent {
            val scrollState = remember { RemoteScrollState() }
            RemoteBox(modifier = RemoteModifier.size(100.rdp).verticalScroll(scrollState)) {
                RemoteText("Scroll Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Scroll Content").assertExists()
    }

    @Test
    fun testModifierMarquee() {
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).basicMarquee()) {
                RemoteText("Marquee Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Marquee Content").assertExists()
    }

    @Test
    fun testModifierRipple() {
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).rippleEffect()) {
                RemoteText("Ripple Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("Ripple Content").assertExists()
    }

    @Test
    fun testModifierAlignBy() {
        rule.setRemoteContent(profile = experimentalProfile) {
            RemoteRow(modifier = RemoteModifier.size(100.rdp)) {
                RemoteText(
                    text = "AlignBy Content".rs,
                    modifier = RemoteModifier.alignByBaseline(),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("AlignBy Content").assertExists()
    }

    @Test
    fun testDataFloat() {
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).graphicsLayer(alpha = 0.7f.rf)) {
                RemoteText("DataFloat Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("DataFloat Content").assertExists()
    }

    @Test
    fun testAnimatedFloat() {
        rule.setRemoteContent {
            val animated =
                animateRemoteFloatAsState(
                    targetValue = 1.0f.rf,
                    animationSpec = remoteTween(durationMillis = 1000),
                )
            RemoteBox(modifier = RemoteModifier.size(100.rdp).graphicsLayer(alpha = animated)) {
                RemoteText("AnimatedFloat Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("AnimatedFloat Content").assertExists()
    }

    @Test
    fun testDataInt() {
        val invisibleState = RemoteInt(1) // INVISIBLE

        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).visibility(invisibleState)) {
                RemoteText("DataInt Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("DataInt Content").assertExists()
    }

    @Test
    fun testIntegerExpression() {
        rule.setRemoteContent {
            val mutableInt = remember { MutableRemoteInt(1) }
            // shl returns RemoteIntExpression if one of the operands is not
            // constant
            val expression = mutableInt shl 2.ri
            RemoteBox(modifier = RemoteModifier.size(100.rdp).visibility(expression)) {
                RemoteText("IntegerExpression Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("IntegerExpression Content").assertExists()
    }

    @Test
    fun testDataLong() {
        rule.setRemoteContent {
            val longState = remember { createNamedRemoteLong("myLong", 0L) }
            val floatExpr = deltaFromReferenceInSeconds(longState)
            RemoteBox(modifier = RemoteModifier.size(100.rdp).graphicsLayer(alpha = floatExpr)) {
                RemoteText("DataLong Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("DataLong Content").assertExists()
    }

    @Test
    fun testColorExpressions() {
        rule.setRemoteContent {
            val colorExpr =
                tween(
                    AndroidColor.RED,
                    AndroidColor.BLUE,
                    0.5f.rf,
                )
            RemoteBox(modifier = RemoteModifier.size(100.rdp).background(colorExpr)) {
                RemoteText("ColorExpressions Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithText("ColorExpressions Content").assertExists()
    }

    @Test
    fun testTextSize() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("textSizeParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint = RemotePaint().apply { textSize = 20f.rf }
                drawText("Size 20".rs, 0f.rf, 0f.rf, paint)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("textSizeParent").assertExists()
    }

    @Test
    fun testPaintColor() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("paintColorParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        color = RemoteColor(AndroidColor.BLUE)
                    }
                drawRect(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteSize(
                        100f.rf,
                        100f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("paintColorParent").assertExists()
    }

    @Test
    fun testStrokeWidth() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("strokeWidthParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        style = PaintingStyle.Stroke
                        strokeWidth = 5f.rf
                    }
                drawRect(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteSize(
                        100f.rf,
                        100f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("strokeWidthParent").assertExists()
    }

    @Test
    fun testRemoteIconTint() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("iconTintParent")) {
            val tintVar = remember {
                createNamedRemoteColor(
                    "tint",
                    Color(0xFFFF0000),
                )
            }
            val imageVector =
                RemoteImageVector.Builder(
                        viewportWidth = 24f.rf,
                        viewportHeight = 24f.rf,
                        tintColor = tintVar,
                        name = "dummy",
                    )
                    .build()

            RemoteIcon(
                imageVector = imageVector,
                contentDescription = null,
                tint = tintVar,
            )
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("iconTintParent").assertExists()
    }

    @Test
    fun testStrokeCap() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("strokeCapParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        style = PaintingStyle.Stroke
                        strokeWidth = 5f.rf
                        strokeCap = StrokeCap.Round
                    }
                drawLine(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteOffset(
                        100f.rf,
                        100f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("strokeCapParent").assertExists()
    }

    @Test
    fun testPaintStyle() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("paintStyleParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint = RemotePaint().apply { style = PaintingStyle.Stroke }
                drawRect(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteSize(
                        100f.rf,
                        100f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("paintStyleParent").assertExists()
    }

    @Test
    fun testShader() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("shaderParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        shader =
                            RemoteLinearShader(
                                0f.rf,
                                0f.rf,
                                100f.rf,
                                100f.rf,
                                listOf(
                                    Color.Red.rc,
                                    Color.Blue.rc,
                                ),
                                null,
                                TileMode.Clamp,
                            )
                    }
                drawRect(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteSize(
                        100f.rf,
                        100f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("shaderParent").assertExists()
    }

    @Test
    fun testImageFilterQuality() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("filterQualityParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        filterQuality = FilterQuality.High
                    }
                drawRect(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteSize(
                        100f.rf,
                        100f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("filterQualityParent").assertExists()
    }

    @Test
    fun testPaintAlpha() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("paintAlphaParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        color = RemoteColor(AndroidColor.argb(128, 255, 0, 0)) // Red with 50% alpha
                    }
                drawRect(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteSize(
                        100f.rf,
                        100f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("paintAlphaParent").assertExists()
    }

    @Test
    fun testPaintColorFilter() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("colorFilterParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        colorFilter =
                            RemoteBlendModeColorFilter(
                                Color.Red.rc,
                                BlendMode.SrcOver,
                            )
                    }
                drawRect(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteSize(
                        100f.rf,
                        100f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("colorFilterParent").assertExists()
    }

    @Test
    fun testPaintAntiAlias() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("antiAliasParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint = RemotePaint().apply { isAntiAlias = false }
                drawRect(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteSize(
                        100f.rf,
                        100f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("antiAliasParent").assertExists()
    }

    @Test
    fun testStrokeJoin() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("strokeJoinParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        style = PaintingStyle.Stroke
                        strokeWidth = 5f.rf
                        strokeJoin = StrokeJoin.Round
                    }
                drawRect(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteSize(
                        100f.rf,
                        100f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("strokeJoinParent").assertExists()
    }

    @Test
    fun testPaintTypeface() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("typefaceParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        typeface = RemoteTypeface.DefaultBold
                    }
                drawText("Bold Text".rs, 0f.rf, 0f.rf, paint)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("typefaceParent").assertExists()
    }

    @Test
    fun testPaintBlendMode() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("blendModeParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        blendMode = BlendMode.SrcOver
                    }
                drawRect(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteSize(
                        100f.rf,
                        100f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("blendModeParent").assertExists()
    }

    @Test
    fun testColorId() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("colorIdParent")) {
            val namedColor = remember {
                createNamedRemoteColor(
                    "myColor",
                    Color.Red,
                )
            }
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint = RemotePaint().apply { color = namedColor }
                drawRect(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteSize(
                        100f.rf,
                        100f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("colorIdParent").assertExists()
    }

    @Test
    fun testColorOverride() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val namedColor = remember {
                                createNamedRemoteColor("myColor", Color.Red)
                            }
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

            val playerState = RcPlayerState(document)
            playerState.colorState("myColor").value = Color(0xFF00FF00.toInt())

            rule.setContent { RcPlayer(state = playerState) }

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

            val namedVar = findNamedVariable(document.getOperationsList(), "USER:myColor")
            assertNotNull(namedVar)
            val varId = namedVar!!.mVarId

            val resolvedColor = state.getColor(varId)
            assertEquals(0xFF00FF00.toInt(), resolvedColor)
        }
    }

    @Test
    fun testColorFilterId() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("colorFilterIdParent")) {
            val namedColor = remember {
                createNamedRemoteColor(
                    "myColor",
                    Color.Red,
                )
            }
            val colorFilter = RemoteBlendModeColorFilter(namedColor, BlendMode.SrcOver)
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint = RemotePaint().apply { this.colorFilter = colorFilter }
                drawRect(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteSize(
                        100f.rf,
                        100f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("colorFilterIdParent").assertExists()
    }

    @Test
    fun testClearColorFilter() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("clearColorFilterParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        colorFilter =
                            RemoteBlendModeColorFilter(
                                Color.Red.rc,
                                BlendMode.SrcOver,
                            )
                    }
                drawRect(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteSize(
                        50f.rf,
                        50f.rf,
                    ),
                )

                paint.colorFilter = null
                drawRect(
                    paint,
                    RemoteOffset(
                        50f.rf,
                        50f.rf,
                    ),
                    RemoteSize(
                        50f.rf,
                        50f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("clearColorFilterParent").assertExists()
    }

    @Test
    fun testShaderMatrix() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("shaderMatrixParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        shader =
                            RemoteLinearShader(
                                    0f.rf,
                                    0f.rf,
                                    100f.rf,
                                    100f.rf,
                                    listOf(
                                        Color.Red.rc,
                                        Color.Blue.rc,
                                    ),
                                    null,
                                    TileMode.Clamp,
                                )
                                .apply {
                                    remoteMatrix3x3 = createRotate(45f.rf)
                                }
                    }
                drawRect(
                    paint,
                    RemoteOffset(
                        0f.rf,
                        0f.rf,
                    ),
                    RemoteSize(
                        100f.rf,
                        100f.rf,
                    ),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("shaderMatrixParent").assertExists()
    }

    @Test
    fun testFontAxis() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("fontAxisParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        fontVariationSettings =
                            FontVariation.Settings(FontVariation.Setting("wdth", 100f))
                    }
                drawText("Var Text".rs, 0f.rf, 0f.rf, paint)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("fontAxisParent").assertExists()
    }

    @Test
    fun testTexture() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("textureParent")) {
            val bitmap =
                Bitmap.createBitmap(
                        10,
                        10,
                        Bitmap.Config.ARGB_8888,
                    )
                    .asImageBitmap()
            val remoteBitmap = bitmap.rb
            val brush = RemoteBrush.image(remoteBitmap)
            RemoteBox(modifier = RemoteModifier.size(100.rdp).background(brush)) {
                RemoteText("Texture Content".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("textureParent").assertExists()
    }

    @Test
    fun testRemoteColumnWeight() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("columnWeightParent")) {
            RemoteColumn(modifier = RemoteModifier.size(100.rdp)) {
                RemoteBox(modifier = RemoteModifier.weight(1f).background(Color.Red.rc)) {
                    RemoteText("Weight 1".rs)
                }
                RemoteBox(modifier = RemoteModifier.size(50.rdp).background(Color.Blue.rc)) {
                    RemoteText("Size 50".rs)
                }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("columnWeightParent").assertExists()
    }

    @Test
    fun testBlendMode() {
        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("blendModeParent")) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawRect(
                    paint =
                        RemotePaint {
                            color = RemoteColor(Color.Red)
                            blendMode = BlendMode.SrcOver
                        },
                    topLeft = RemoteOffset(10f.rf, 10f.rf),
                    size = RemoteSize(80f.rf, 80f.rf),
                )
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("blendModeParent").assertExists()
    }

    @Test
    fun testRemoteCardLayoutReproduction() {
        val shape = RemoteRoundedCornerShape(20.rdp)

        rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("reproductionParent")) {
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp)
                        .clip(shape = shape)
                        .padding(16.rdp)
                        .background(Color.LightGray.rc)
            ) {
                RemoteText("This is long text that might cut off".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("reproductionParent").assertExists()
        rule
            .onNodeWithText("This is long text that might cut off", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testRemoteButtonLayoutReproduction() {
        rule.setRemoteContent(
            modifier = Modifier.size(100.dp).testTag("buttonReproductionParent")
        ) {
            RemoteRow(
                modifier =
                    RemoteModifier.heightIn(min = 52.rdp)
                        .widthIn(min = 100.rdp)
                        .clip(shape = RemoteRoundedCornerShape(16.rdp))
                        .padding(RemotePaddingValues(14.rdp, 6.rdp)),
                verticalAlignment = RemoteAlignment.CenterVertically,
                horizontalArrangement = RemoteArrangement.Center,
            ) {
                RemoteText("Click".rs)
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

    @Test
    fun testRemoteButtonFromMaterial3() {
        rule.setRemoteContent(modifier = Modifier.size(200.dp).testTag("buttonMaterial3Parent")) {
            RemoteButton(onClick = Action.Empty) {
                RemoteText("Click".rs)
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("buttonMaterial3Parent").assertExists()

        try {
            val bounds = rule.onNodeWithText("Click").getUnclippedBoundsInRoot()
        } catch (e: Throwable) {}
    }

    @Test
    fun testChartWithNamedFloatArrayUpdate() {
        val initialData = floatArrayOf(20f, 50f, 80f)
        val document =
            rule.setRemoteContent(modifier = Modifier.size(100.dp).testTag("chartRoot")) {
                val chartData = remember {
                    createNamedRemoteFloatArray(
                        name = "chartData",
                        defaultValue = initialData,
                    )
                }
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val paintRed = RemotePaint { color = RemoteColor(Color.Red) }
                    val paintGreen = RemotePaint { color = RemoteColor(Color.Green) }
                    val paintBlue = RemotePaint { color = RemoteColor(Color.Blue) }
                    drawRect(
                        paint = paintRed,
                        topLeft = RemoteOffset(10f.rf, 100f.rf - chartData[0]),
                        size = RemoteSize(20f.rf, chartData[0]),
                    )
                    drawRect(
                        paint = paintGreen,
                        topLeft = RemoteOffset(40f.rf, 100f.rf - chartData[1]),
                        size = RemoteSize(20f.rf, chartData[1]),
                    )
                    drawRect(
                        paint = paintBlue,
                        topLeft = RemoteOffset(70f.rf, 100f.rf - chartData[2]),
                        size = RemoteSize(20f.rf, chartData[2]),
                    )
                }
            }

        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("chartRoot").assertExists()

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

        val namedVar = findNamedVariable(document.getOperationsList(), "USER:chartData")
        assertNotNull(namedVar)
        assertEquals(NamedVariable.FLOAT_ARRAY_TYPE, namedVar!!.mVarType)
        val arrayId = namedVar.mVarId

        val initialArray = document.getArray(arrayId)
        assertNotNull(initialArray)
        assertArrayEquals(initialData, initialArray!!.getFloats(), 0.001f)
        assertEquals(20f, document.remoteComposeState.getFloatValue(arrayId, 0), 0.001f)
        assertEquals(50f, document.remoteComposeState.getFloatValue(arrayId, 1), 0.001f)
        assertEquals(80f, document.remoteComposeState.getFloatValue(arrayId, 2), 0.001f)

        // Update named float array state via delta document
        val updatedData = floatArrayOf(80f, 20f, 40f)
        val deltaBuffer = RemoteComposeBuffer()
        deltaBuffer.addFloatArray(arrayId, updatedData)
        val deltaDoc =
            CoreDocument(RemoteClock.SYSTEM).apply {
                initFromBuffer(deltaBuffer)
            }
        document.applyUpdate(deltaDoc)

        rule.mainClock.advanceTimeBy(100)

        val updatedArray = document.getArray(arrayId)
        assertNotNull(updatedArray)
        assertArrayEquals(updatedData, updatedArray!!.getFloats(), 0.001f)
        assertEquals(80f, document.remoteComposeState.getFloatValue(arrayId, 0), 0.001f)
        assertEquals(20f, document.remoteComposeState.getFloatValue(arrayId, 1), 0.001f)
        assertEquals(40f, document.remoteComposeState.getFloatValue(arrayId, 2), 0.001f)
        rule.onNodeWithTag("chartRoot").assertExists()
    }

    @OptIn(ExperimentalRemotePlayerApi::class)
    @Test
    fun testRcPlayerFailsWhenEmbeddedPlayerDisabled() {
        RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = false
        val document = CoreDocument(RemoteClock.SYSTEM)
        assertFailsWith<IllegalStateException> { rule.setContent { RcPlayer(document = document) } }
    }
}
