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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.ContainerEnd
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.LayoutComponentContent
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.core.operations.utilities.easing.GeneralEasing
import androidx.compose.remote.core.types.IntegerConstant
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
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
 * Tests for built-in visibility animations (`FADE_IN`, `FADE_OUT`, `SLIDE_*`) and mid-flight
 * retargeting in [RcPlayer].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RcPlayerCustomVisibilityAnimationTest {

    @get:Rule val playerRule = RcPlayerTestRule()

    @Test
    fun builtInVisibilityAnimation_enterAndExit_updatesVisibility() {
        val buffer = RemoteComposeBuffer()
        val tags = shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES)
        val values =
            arrayOf<Any>(200, 200, RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL)
        buffer.addHeader(tags, values)

        IntegerConstant.apply(buffer.buffer, 550, Component.Visibility.VISIBLE)
        NamedVariable.apply(buffer.buffer, 550, NamedVariable.INT_TYPE, "vis_state")

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
            AnimationSpec.ANIMATION.FADE_IN.ordinal,
            AnimationSpec.ANIMATION.FADE_OUT.ordinal,
        )
        LayoutComponentContent.apply(buffer.buffer, -1)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        val doc = CoreDocument(RemoteClock.SYSTEM).apply { initFromBuffer(buffer) }
        val playerState = RcPlayerState(doc)
        var visState by playerState.intState("vis_state")

        playerRule.mainClock.autoAdvance = false
        playerRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(200.dp).testTag("player")) {
                    RcPlayer(state = playerState)
                }
            }
        }

        playerRule.mainClock.advanceTimeBy(32)
        val boxComp = doc.getComponent(10) as LayoutComponent
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.VISIBLE)

        // Trigger exit animation (VISIBLE -> GONE)
        visState = Component.Visibility.GONE
        Snapshot.sendApplyNotifications()
        playerRule.mainClock.advanceTimeBy(100)
        playerRule.onNodeWithTag("player").captureToImage()
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.VISIBLE)

        // Advance past completion of exit animation
        playerRule.mainClock.advanceTimeBy(150)
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.GONE)

        // Trigger enter animation (GONE -> VISIBLE)
        visState = Component.Visibility.VISIBLE
        Snapshot.sendApplyNotifications()
        playerRule.mainClock.advanceTimeBy(100)
        playerRule.onNodeWithTag("player").captureToImage()
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.VISIBLE)

        // Advance past completion of enter animation
        playerRule.mainClock.advanceTimeBy(150)
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.VISIBLE)
    }

    @Test
    fun visibilityAnimation_midFlightReversal_retargetsCleanly() {
        val buffer = RemoteComposeBuffer()
        val tags = shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES)
        val values =
            arrayOf<Any>(200, 200, RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL)
        buffer.addHeader(tags, values)

        IntegerConstant.apply(buffer.buffer, 960, Component.Visibility.VISIBLE)
        NamedVariable.apply(buffer.buffer, 960, NamedVariable.INT_TYPE, "vis_state")

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
            AnimationSpec.ANIMATION.FADE_IN.ordinal,
            AnimationSpec.ANIMATION.FADE_OUT.ordinal,
        )
        LayoutComponentContent.apply(buffer.buffer, -1)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)
        ContainerEnd.apply(buffer.buffer)

        val doc = CoreDocument(RemoteClock.SYSTEM).apply { initFromBuffer(buffer) }
        val playerState = RcPlayerState(doc)
        var visState by playerState.intState("vis_state")

        playerRule.mainClock.autoAdvance = false
        playerRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(200.dp)) { RcPlayer(state = playerState) }
            }
        }
        playerRule.mainClock.advanceTimeBy(32)

        val boxComp = doc.getComponent(60) as LayoutComponent
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.VISIBLE)

        // Start exit animation (VISIBLE -> GONE)
        visState = Component.Visibility.GONE
        Snapshot.sendApplyNotifications()
        playerRule.mainClock.advanceTimeBy(100)

        // Halfway through exit animation, toggle back GONE -> VISIBLE
        visState = Component.Visibility.VISIBLE
        Snapshot.sendApplyNotifications()
        playerRule.mainClock.advanceTimeBy(50)
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.VISIBLE)

        // Advance past completion
        playerRule.mainClock.advanceTimeBy(250)
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.VISIBLE)

        // Further frames must not restart an animation
        playerRule.mainClock.advanceTimeBy(100)
        assertThat(boxComp.mVisibility).isEqualTo(Component.Visibility.VISIBLE)
    }
}
