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

@file:Suppress("RestrictedApiAndroidX", "DEPRECATION")

package androidx.compose.remote.player.compose.embedded

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.DrawContent
import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.DrawToBitmap
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.ContainerEnd
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.LayoutComponentContent
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ShapeType
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.easing.GeneralEasing
import androidx.compose.remote.core.types.IntegerConstant
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tests for custom visibility animations (`FloatFunctionDefine`), offscreen component bitmap
 * capture (`createOffscreenBitmap` / `DrawToBitmap.MODE_COMPONENT_ID`), and
 * `enterSequence`/`exitSequence` modes (`CONCURRENT`, `BEFORE`, `AFTER`) in the embedded
 * [RcPlayer].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RcPlayerCustomVisibilityAnimationTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun customVisibilityAnimation_enterAndExit_executesCustomFunctionsAndUpdatesVisibility() {
        val buffer = RemoteComposeBuffer()
        val tags = shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES)
        val values =
            arrayOf<Any>(200, 200, RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL)
        buffer.addHeader(tags, values)

        IntegerConstant.apply(buffer.buffer, 550, Component.Visibility.VISIBLE)
        NamedVariable.apply(buffer.buffer, 550, NamedVariable.INT_TYPE, "vis_state")

        val enterFnId = 101
        val exitFnId = 102
        val args = intArrayOf(500, 501, 502, 503, 504, 505) // progress, w, h, x, y, id

        // Enter function: draws green rect and records args into 600..604
        FloatFunctionDefine.apply(buffer.buffer, enterFnId, args)
        val greenPaint = PaintBundle().apply { setColor(0xFF00FF00.toInt()) }
        PaintData.apply(buffer.buffer, greenPaint)
        DrawRect.apply(buffer.buffer, 0f, 0f, 100f, 60f)
        FloatExpression.apply(buffer.buffer, 600, floatArrayOf(Utils.asNan(500)), null)
        FloatExpression.apply(buffer.buffer, 601, floatArrayOf(Utils.asNan(501)), null)
        FloatExpression.apply(buffer.buffer, 602, floatArrayOf(Utils.asNan(502)), null)
        DrawContent.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        // Exit function: draws red rect and records args into 600..602
        FloatFunctionDefine.apply(buffer.buffer, exitFnId, args)
        val redPaint = PaintBundle().apply { setColor(0xFFFF0000.toInt()) }
        PaintData.apply(buffer.buffer, redPaint)
        DrawRect.apply(buffer.buffer, 0f, 0f, 100f, 60f)
        FloatExpression.apply(buffer.buffer, 600, floatArrayOf(Utils.asNan(500)), null)
        FloatExpression.apply(buffer.buffer, 601, floatArrayOf(Utils.asNan(501)), null)
        FloatExpression.apply(buffer.buffer, 602, floatArrayOf(Utils.asNan(502)), null)
        DrawContent.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        RootLayoutComponent.apply(buffer.buffer, -1)
        BoxLayout.apply(buffer.buffer, 10, -1, BoxLayout.START, BoxLayout.TOP)
        WidthModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            100f,
        )
        HeightModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            60f,
        )
        ComponentVisibilityOperation.apply(buffer.buffer, 550)
        AnimationSpec.apply(
            buffer.buffer,
            1,
            200f,
            GeneralEasing.CUBIC_LINEAR,
            200f,
            GeneralEasing.CUBIC_LINEAR,
            AnimationSpec.ANIMATION.CUSTOM.ordinal,
            AnimationSpec.ANIMATION.CUSTOM.ordinal,
            enterFnId,
            exitFnId,
        )
        LayoutComponentContent.apply(buffer.buffer, -1)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        val doc = CoreDocument(RemoteClock.SYSTEM).apply { initFromBuffer(buffer) }
        val playerState = RcPlayerState(doc)
        var visState by playerState.intState("vis_state")

        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(200.dp).testTag("player")) {
                    RcPlayer(state = playerState, trustedContent = true)
                }
            }
        }
        rule.mainClock.advanceTimeBy(32)

        val boxComp = doc.getComponent(10) as LayoutComponent
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.VISIBLE)

        // Trigger exit animation (VISIBLE -> GONE)
        visState = Component.Visibility.GONE
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(100)

        val midExitBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(midExitBitmap.getPixel(10, 10)).isEqualTo(AndroidColor.RED)
        assertThat(playerState.remoteContext.getFloat(600)).isGreaterThan(0.1f)
        assertThat(playerState.remoteContext.getFloat(600)).isLessThan(0.9f)
        assertThat(playerState.remoteContext.getFloat(601)).isEqualTo(100f)
        assertThat(playerState.remoteContext.getFloat(602)).isEqualTo(60f)

        // Finish exit animation
        rule.mainClock.advanceTimeBy(200)
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.GONE)

        // Trigger enter animation (GONE -> VISIBLE)
        visState = Component.Visibility.VISIBLE
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(100)

        val midEnterBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(midEnterBitmap.getPixel(10, 10)).isEqualTo(AndroidColor.GREEN)
        assertThat(playerState.remoteContext.getFloat(600)).isGreaterThan(0.1f)
        assertThat(playerState.remoteContext.getFloat(600)).isLessThan(0.9f)
        assertThat(playerState.remoteContext.getFloat(601)).isEqualTo(100f)
        assertThat(playerState.remoteContext.getFloat(602)).isEqualTo(60f)

        // Finish enter animation
        rule.mainClock.advanceTimeBy(200)
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.VISIBLE)
    }

    @Test
    fun customVisibilityAnimation_offscreenBitmapAndShader_capturesComponentToBitmap() {
        val buffer = RemoteComposeBuffer()
        val tags = shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES)
        val values =
            arrayOf<Any>(200, 200, RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL)
        buffer.addHeader(tags, values)

        IntegerConstant.apply(buffer.buffer, 550, Component.Visibility.VISIBLE)
        NamedVariable.apply(buffer.buffer, 550, NamedVariable.INT_TYPE, "vis_state")

        val shaderTextId = 400
        val shaderSrc =
            """
            uniform shader image;
            uniform float u_progress;
            half4 main(float2 fragCoord) {
                half4 c = image.eval(fragCoord);
                return half4(c.rgb * (1.0 - u_progress * 0.5), c.a);
            }
            """
                .trimIndent()
        TextData.apply(buffer.buffer, shaderTextId, shaderSrc)

        val exitFnId = 102
        val argProgress = 500
        val argW = 501
        val argH = 502
        val argX = 503
        val argY = 504
        val argId = 505
        val offscreenBmpId = 820
        val shaderDataId = 830

        FloatFunctionDefine.apply(
            buffer.buffer,
            exitFnId,
            intArrayOf(argProgress, argW, argH, argX, argY, argId),
        )
        buffer.createOffscreenBitmap(offscreenBmpId)
        DrawToBitmap.apply(
            buffer.buffer,
            offscreenBmpId,
            DrawToBitmap.MODE_COMPONENT_ID,
            argId,
        )
        DrawContent.apply(buffer.buffer)
        DrawToBitmap.apply(buffer.buffer, 0, 0, 0)

        val uniforms = HashMap<String, FloatArray>()
        uniforms["u_progress"] = floatArrayOf(Utils.asNan(argProgress))
        val textures = HashMap<String, Int>()
        textures["image"] = offscreenBmpId
        ShaderData.apply(buffer.buffer, shaderDataId, shaderTextId, uniforms, null, textures)

        val shaderPaint = PaintBundle().apply { setShader(shaderDataId) }
        PaintData.apply(buffer.buffer, shaderPaint)
        DrawRect.apply(buffer.buffer, 0f, 0f, 80f, 80f)
        val clearShaderPaint = PaintBundle().apply { setShader(0) }
        PaintData.apply(buffer.buffer, clearShaderPaint)
        ContainerEnd.apply(buffer.buffer)

        RootLayoutComponent.apply(buffer.buffer, -1)
        BoxLayout.apply(buffer.buffer, 10, -1, BoxLayout.START, BoxLayout.TOP)
        WidthModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            80f,
        )
        HeightModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            80f,
        )
        BackgroundModifierOperation.apply(
            buffer.buffer,
            0,
            0,
            80,
            80,
            0f,
            0f,
            1f,
            1f,
            ShapeType.RECTANGLE,
        )
        ComponentVisibilityOperation.apply(buffer.buffer, 550)
        AnimationSpec.apply(
            buffer.buffer,
            1,
            200f,
            GeneralEasing.CUBIC_LINEAR,
            200f,
            GeneralEasing.CUBIC_LINEAR,
            AnimationSpec.ANIMATION.FADE_IN.ordinal,
            AnimationSpec.ANIMATION.CUSTOM.ordinal,
            0,
            exitFnId,
        )
        LayoutComponentContent.apply(buffer.buffer, -1)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        val doc = CoreDocument(RemoteClock.SYSTEM).apply { initFromBuffer(buffer) }
        val playerState = RcPlayerState(doc)
        var visState by playerState.intState("vis_state")

        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(200.dp).testTag("player")) {
                    RcPlayer(state = playerState, trustedContent = true)
                }
            }
        }
        rule.mainClock.advanceTimeBy(32)

        visState = Component.Visibility.GONE
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(100)

        val midExitBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        val capturedOffscreen =
            playerState.remoteContext.mRemoteComposeState.getFromId(offscreenBmpId) as Bitmap
        assertThat(capturedOffscreen.width).isEqualTo(80)
        assertThat(capturedOffscreen.height).isEqualTo(80)
        assertThat(capturedOffscreen.getPixel(20, 20)).isEqualTo(AndroidColor.BLUE)
        val pixel = midExitBitmap.getPixel(20, 20)
        assertThat(AndroidColor.blue(pixel)).isGreaterThan(50)

        rule.mainClock.advanceTimeBy(200)
        val boxComp = doc.getComponent(10) as LayoutComponent
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.GONE)
    }

    @Test
    fun nestedComponentsSharingSameOffscreenVisibilityAnimation_preservesArgsAndBitmaps() {
        val buffer = RemoteComposeBuffer()
        val tags = shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES)
        val values =
            arrayOf<Any>(400, 400, RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL)
        buffer.addHeader(tags, values)

        IntegerConstant.apply(buffer.buffer, 850, Component.Visibility.VISIBLE)
        NamedVariable.apply(buffer.buffer, 850, NamedVariable.INT_TYPE, "vis_parent")
        IntegerConstant.apply(buffer.buffer, 851, Component.Visibility.VISIBLE)
        NamedVariable.apply(buffer.buffer, 851, NamedVariable.INT_TYPE, "vis_child")

        val sharedFnId = 800
        val argProgress = 810
        val argW = 811
        val argH = 812
        val argX = 813
        val argY = 814
        val argId = 815
        val offscreenBmpId = 820

        FloatFunctionDefine.apply(
            buffer.buffer,
            sharedFnId,
            intArrayOf(argProgress, argW, argH, argX, argY, argId),
        )
        buffer.createOffscreenBitmap(offscreenBmpId)
        DrawToBitmap.apply(
            buffer.buffer,
            offscreenBmpId,
            DrawToBitmap.MODE_COMPONENT_ID,
            argId,
        )
        DrawContent.apply(buffer.buffer)
        DrawToBitmap.apply(buffer.buffer, 0, 0, 0)
        FloatExpression.apply(buffer.buffer, 910, floatArrayOf(Utils.asNan(argW)), null)
        FloatExpression.apply(buffer.buffer, 911, floatArrayOf(Utils.asNan(argH)), null)
        ContainerEnd.apply(buffer.buffer)

        RootLayoutComponent.apply(buffer.buffer, -1)
        // Parent Box (300x160)
        BoxLayout.apply(buffer.buffer, 80, -1, BoxLayout.START, BoxLayout.TOP)
        WidthModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            300f,
        )
        HeightModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            160f,
        )
        ComponentVisibilityOperation.apply(buffer.buffer, 850)
        AnimationSpec.apply(
            buffer.buffer,
            1,
            400f,
            GeneralEasing.CUBIC_STANDARD,
            400f,
            GeneralEasing.CUBIC_STANDARD,
            AnimationSpec.ANIMATION.CUSTOM.ordinal,
            AnimationSpec.ANIMATION.CUSTOM.ordinal,
            sharedFnId,
            sharedFnId,
        )
        LayoutComponentContent.apply(buffer.buffer, -1)

        // Child Box inside Parent (120x50) using the exact same sharedFnId
        BoxLayout.apply(buffer.buffer, 81, -1, BoxLayout.START, BoxLayout.TOP)
        WidthModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            120f,
        )
        HeightModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            50f,
        )
        ComponentVisibilityOperation.apply(buffer.buffer, 851)
        AnimationSpec.apply(
            buffer.buffer,
            1,
            400f,
            GeneralEasing.CUBIC_STANDARD,
            400f,
            GeneralEasing.CUBIC_STANDARD,
            AnimationSpec.ANIMATION.CUSTOM.ordinal,
            AnimationSpec.ANIMATION.CUSTOM.ordinal,
            sharedFnId,
            sharedFnId,
        )
        LayoutComponentContent.apply(buffer.buffer, -1)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer) // End Child Box

        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer) // End Parent Box
        ContainerEnd.apply(buffer.buffer) // End Root

        val doc = CoreDocument(RemoteClock.SYSTEM).apply { initFromBuffer(buffer) }
        val playerState = RcPlayerState(doc)
        var visParent by playerState.intState("vis_parent")
        var visChild by playerState.intState("vis_child")

        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(400.dp).testTag("player")) {
                    RcPlayer(state = playerState)
                }
            }
        }
        rule.mainClock.advanceTimeBy(32)

        // Trigger simultaneous exit animations on BOTH parent and child
        visParent = Component.Visibility.GONE
        visChild = Component.Visibility.GONE
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("player").captureToImage()

        // Parent's post-capture expressions (executed after child's nested animation) must read
        // parent's restored 300x160 bounds, not child's 120x50 bounds!
        assertThat(playerState.remoteContext.getFloat(910)).isWithin(0.01f).of(300f)
        assertThat(playerState.remoteContext.getFloat(911)).isWithin(0.01f).of(160f)
    }

    @Test
    fun visibilityAnimation_beforeAndAfterLayoutSequencing() {
        val buffer = RemoteComposeBuffer()
        val tags = shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES)
        val values =
            arrayOf<Any>(400, 400, RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL)
        buffer.addHeader(tags, values)

        IntegerConstant.apply(buffer.buffer, 750, Component.Visibility.VISIBLE)
        NamedVariable.apply(buffer.buffer, 750, NamedVariable.INT_TYPE, "vis_state")

        // Custom enter (id = 301, records progress into 701) & exit (id = 302, records into 702)
        FloatFunctionDefine.apply(buffer.buffer, 301, intArrayOf(701))
        DrawContent.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        FloatFunctionDefine.apply(buffer.buffer, 302, intArrayOf(702))
        DrawContent.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        RootLayoutComponent.apply(buffer.buffer, -1)
        ColumnLayout.apply(buffer.buffer, 70, -1, ColumnLayout.START, ColumnLayout.TOP, 0f)
        WidthModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            200f,
        )
        HeightModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            300f,
        )
        LayoutComponentContent.apply(buffer.buffer, -1)

        // Box 1 (id = 71): height 100, exitSequence = BEFORE, enterSequence = AFTER
        // motionDuration = 200ms, visibilityDuration = 200ms
        BoxLayout.apply(buffer.buffer, 71, -1, BoxLayout.START, BoxLayout.TOP)
        WidthModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            200f,
        )
        HeightModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            100f,
        )
        ComponentVisibilityOperation.apply(buffer.buffer, 750)
        AnimationSpec.apply(
            buffer.buffer,
            1,
            200f,
            GeneralEasing.CUBIC_LINEAR,
            200f,
            GeneralEasing.CUBIC_LINEAR,
            AnimationSpec.packAnimation(
                AnimationSpec.ANIMATION.CUSTOM,
                AnimationSpec.SEQUENCE.AFTER,
            ),
            AnimationSpec.packAnimation(
                AnimationSpec.ANIMATION.CUSTOM,
                AnimationSpec.SEQUENCE.BEFORE,
            ),
            301,
            302,
        )
        LayoutComponentContent.apply(buffer.buffer, -1)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        // Box 2 (id = 72, sibling below Box 1): height 100, motionDuration = 200ms
        BoxLayout.apply(buffer.buffer, 72, -1, BoxLayout.START, BoxLayout.TOP)
        WidthModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            200f,
        )
        HeightModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            100f,
        )
        AnimationSpec.apply(
            buffer.buffer,
            2,
            200f,
            GeneralEasing.CUBIC_LINEAR,
            200f,
            GeneralEasing.CUBIC_LINEAR,
            AnimationSpec.ANIMATION.FADE_IN.ordinal,
            AnimationSpec.ANIMATION.FADE_OUT.ordinal,
        )
        LayoutComponentContent.apply(buffer.buffer, -1)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        val doc = CoreDocument(RemoteClock.SYSTEM).apply { initFromBuffer(buffer) }
        val playerState = RcPlayerState(doc)
        var visState by playerState.intState("vis_state")

        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(400.dp).testTag("player")) {
                    RcPlayer(state = playerState)
                }
            }
        }
        rule.mainClock.advanceTimeBy(32)

        val box1 = doc.getComponent(71) as LayoutComponent
        val box2 = doc.getComponent(72) as LayoutComponent
        assertThat(box1.y).isWithin(0.5f).of(0f)
        assertThat(box2.y).isWithin(0.5f).of(100f)

        // 1. Trigger EXIT (VISIBLE -> GONE) with exitSequence = BEFORE (200ms vis, 200ms motion)
        visState = Component.Visibility.GONE
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("player").captureToImage()

        // At ~100ms: Box 1 is playing exit animation, while Box 2 STILL holds y = 100
        assertThat(playerState.remoteContext.getFloat(702)).isGreaterThan(0.1f)
        assertThat(playerState.remoteContext.getFloat(702)).isLessThan(0.9f)
        assertThat(box2.y).isWithin(1f).of(100f)

        // At ~300ms: Box 1 visibility fade finished (mVisibility == GONE), Box 2 is collapsing
        rule.mainClock.advanceTimeBy(200)
        assertThat(box1.mVisibility).isEqualTo(Component.Visibility.GONE)
        assertThat(box2.y).isGreaterThan(5f)
        assertThat(box2.y).isLessThan(95f)

        // At ~500ms: Box 2 has reached y = 0
        rule.mainClock.advanceTimeBy(200)
        assertThat(box2.y).isWithin(1f).of(0f)

        // 2. Trigger ENTER (GONE -> VISIBLE) with enterSequence = AFTER (200ms motion, 200ms vis)
        visState = Component.Visibility.VISIBLE
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("player").captureToImage()

        // At ~100ms: Box 2 is moving down (0 < y < 100) while enter visibility hasn't started yet
        assertThat(box2.y).isGreaterThan(5f)
        assertThat(box2.y).isLessThan(95f)

        // At ~300ms: Box 2 has reached y = 100 and Box 1 is halfway through enter visibility
        rule.mainClock.advanceTimeBy(200)
        rule.onNodeWithTag("player").captureToImage()
        assertThat(box2.y).isWithin(1f).of(100f)
        assertThat(playerState.remoteContext.getFloat(701)).isGreaterThan(0.1f)
        assertThat(playerState.remoteContext.getFloat(701)).isLessThan(0.9f)

        // At ~500ms: Enter is complete
        rule.mainClock.advanceTimeBy(200)
        assertThat(box1.mVisibility).isEqualTo(Component.Visibility.VISIBLE)
        assertThat(box2.y).isWithin(1f).of(100f)
    }

    @Test
    fun visibilityAnimation_midFlightToggle_retargetsWithoutRestartAfterCompletion() {
        val buffer = RemoteComposeBuffer()
        val tags = shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES)
        val values =
            arrayOf<Any>(200, 200, RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL)
        buffer.addHeader(tags, values)

        IntegerConstant.apply(buffer.buffer, 960, Component.Visibility.VISIBLE)
        NamedVariable.apply(buffer.buffer, 960, NamedVariable.INT_TYPE, "vis_state")

        FloatFunctionDefine.apply(buffer.buffer, 601, intArrayOf(900))
        DrawContent.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        FloatFunctionDefine.apply(buffer.buffer, 602, intArrayOf(900))
        DrawContent.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        RootLayoutComponent.apply(buffer.buffer, -1)
        BoxLayout.apply(buffer.buffer, 60, -1, BoxLayout.START, BoxLayout.TOP)
        WidthModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            120f,
        )
        HeightModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            80f,
        )
        ComponentVisibilityOperation.apply(buffer.buffer, 960)
        AnimationSpec.apply(
            buffer.buffer,
            1,
            200f,
            GeneralEasing.CUBIC_LINEAR,
            200f,
            GeneralEasing.CUBIC_LINEAR,
            AnimationSpec.ANIMATION.CUSTOM.ordinal,
            AnimationSpec.ANIMATION.CUSTOM.ordinal,
            601,
            602,
        )
        LayoutComponentContent.apply(buffer.buffer, -1)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        val doc = CoreDocument(RemoteClock.SYSTEM).apply { initFromBuffer(buffer) }
        val playerState = RcPlayerState(doc)
        var visState by playerState.intState("vis_state")

        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(200.dp)) { RcPlayer(state = playerState) }
            }
        }
        rule.mainClock.advanceTimeBy(32)

        val boxComp = doc.getComponent(60) as LayoutComponent
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.VISIBLE)

        // Start exit animation (VISIBLE -> GONE)
        visState = Component.Visibility.GONE
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(100)

        // Halfway through exit animation, toggle back GONE -> VISIBLE
        visState = Component.Visibility.VISIBLE
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(50)
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.VISIBLE)

        // Advance past completion
        rule.mainClock.advanceTimeBy(250)
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.VISIBLE)

        // Further frames must not restart an animation
        rule.mainClock.advanceTimeBy(100)
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.VISIBLE)
    }

    @Test
    fun customVisibilityAnimation_densityBehaviorDp_passesScaledDimensions() {
        val buffer = RemoteComposeBuffer()
        val tags =
            shortArrayOf(
                Header.DOC_WIDTH,
                Header.DOC_HEIGHT,
                Header.DOC_PROFILES,
                Header.DOC_DENSITY_BEHAVIOR,
            )
        val values =
            arrayOf<Any>(
                200,
                200,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                CoreDocument.DENSITY_BEHAVIOR_DP,
            )
        buffer.addHeader(tags, values)

        IntegerConstant.apply(buffer.buffer, 960, Component.Visibility.VISIBLE)
        NamedVariable.apply(buffer.buffer, 960, NamedVariable.INT_TYPE, "vis_state")

        val exitFnId = 602
        FloatFunctionDefine.apply(
            buffer.buffer,
            exitFnId,
            intArrayOf(900, 901, 902, 903, 904),
        )
        FloatExpression.apply(buffer.buffer, 951, floatArrayOf(Utils.asNan(901)), null)
        FloatExpression.apply(buffer.buffer, 952, floatArrayOf(Utils.asNan(902)), null)
        DrawContent.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        RootLayoutComponent.apply(buffer.buffer, -1)
        BoxLayout.apply(buffer.buffer, 60, -1, BoxLayout.START, BoxLayout.TOP)
        WidthModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            50f,
        )
        HeightModifierOperation.apply(
            buffer.buffer,
            DimensionModifierOperation.Type.EXACT.ordinal,
            40f,
        )
        ComponentVisibilityOperation.apply(buffer.buffer, 960)
        AnimationSpec.apply(
            buffer.buffer,
            1,
            200f,
            GeneralEasing.CUBIC_LINEAR,
            200f,
            GeneralEasing.CUBIC_LINEAR,
            AnimationSpec.ANIMATION.FADE_IN.ordinal,
            AnimationSpec.ANIMATION.CUSTOM.ordinal,
            0,
            exitFnId,
        )
        LayoutComponentContent.apply(buffer.buffer, -1)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        val doc = CoreDocument(RemoteClock.SYSTEM).apply { initFromBuffer(buffer) }
        val playerState = RcPlayerState(doc)
        var visState by playerState.intState("vis_state")

        rule.mainClock.autoAdvance = false
        rule.setContent {
            // Density = 2.0f -> 50dp x 40dp component measures at 100px x 80px
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Box(modifier = Modifier.size(200.dp).testTag("player")) {
                    RcPlayer(state = playerState)
                }
            }
        }
        rule.mainClock.advanceTimeBy(32)

        visState = Component.Visibility.GONE
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("player").captureToImage()

        assertThat(playerState.remoteContext.getFloat(951)).isWithin(0.01f).of(100f)
        assertThat(playerState.remoteContext.getFloat(952)).isWithin(0.01f).of(80f)
    }
}
