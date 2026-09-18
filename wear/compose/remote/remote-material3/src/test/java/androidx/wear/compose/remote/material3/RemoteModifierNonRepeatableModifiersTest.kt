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

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests verifying that Remote Compose components do not emit duplicate
 * [WidthModifierOperation] or [HeightModifierOperation] entries when multiple dimension modifiers
 * are chained (see b/563261712) and respect the outer dimension when rendered in [RcPlayer].
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class RemoteModifierNonRepeatableModifiersTest {

    @get:Rule val playerRule = RcPlayerTestRule()

    @Test
    fun titleCard_withExplicitWidth_emitsSingleWidthModifier() {
        val doc = setRemoteContentWithDisplaySize {
            RemoteTitleCard(
                onClick = Action.Empty,
                title = { RemoteText("Title".rs) },
                modifier =
                    RemoteModifier.width(172.rdp).semantics {
                        contentDescription = "titleCard".rs
                    },
            )
        }

        assertNoDuplicateDimensionModifiers(doc)

        val bounds = playerRule.onNodeWithContentDescription("titleCard").getUnclippedBoundsInRoot()
        assertThat((bounds.right - bounds.left).value).isWithin(1f).of(172f)
    }

    @Test
    fun appCard_withExplicitWidth_emitsSingleWidthModifier() {
        val doc = setRemoteContentWithDisplaySize {
            RemoteAppCard(
                onClick = Action.Empty,
                appName = { RemoteText("App".rs) },
                title = { RemoteText("Title".rs) },
                modifier =
                    RemoteModifier.width(172.rdp).semantics {
                        contentDescription = "appCard".rs
                    },
            ) {
                RemoteText("Body".rs)
            }
        }

        assertNoDuplicateDimensionModifiers(doc)
        val bounds = playerRule.onNodeWithContentDescription("appCard").getUnclippedBoundsInRoot()
        assertThat((bounds.right - bounds.left).value).isWithin(1f).of(172f)
    }

    @Test
    fun cardAndOutlinedCard_withExplicitSize_emitSingleWidthAndHeightModifiers() {
        val doc = setRemoteContentWithDisplaySize {
            RemoteCard(
                onClick = Action.Empty,
                modifier =
                    RemoteModifier.size(172.rdp, 100.rdp).semantics {
                        contentDescription = "card".rs
                    },
            ) {
                RemoteText("Card".rs)
            }
            RemoteOutlinedCard(
                onClick = Action.Empty,
                modifier =
                    RemoteModifier.size(172.rdp, 100.rdp).semantics {
                        contentDescription = "outlinedCard".rs
                    },
            ) {
                RemoteText("Outlined".rs)
            }
        }

        assertNoDuplicateDimensionModifiers(doc)
        val cardBounds = playerRule.onNodeWithContentDescription("card").getUnclippedBoundsInRoot()
        assertThat((cardBounds.right - cardBounds.left).value).isWithin(1f).of(172f)
        assertThat((cardBounds.bottom - cardBounds.top).value).isWithin(1f).of(100f)

        val outlinedBounds =
            playerRule.onNodeWithContentDescription("outlinedCard").getUnclippedBoundsInRoot()
        assertThat((outlinedBounds.right - outlinedBounds.left).value).isWithin(1f).of(172f)
        assertThat((outlinedBounds.bottom - outlinedBounds.top).value).isWithin(1f).of(100f)
    }

    @Test
    fun iconButtonAndSlider_withExplicitSize_emitSingleWidthAndHeightModifiers() {
        val doc = setRemoteContentWithDisplaySize {
            RemoteIconButton(
                onClick = Action.Empty,
                modifier =
                    RemoteModifier.size(60.rdp).semantics {
                        contentDescription = "iconButton".rs
                    },
            ) {
                RemoteText("+".rs)
            }
            RemoteSlider(
                value = 0.5f.rf,
                valueRange = 0f..1f,
                modifier =
                    RemoteModifier.width(160.rdp).semantics {
                        contentDescription = "slider".rs
                    },
            )
        }

        assertNoDuplicateDimensionModifiers(doc)
        val iconBounds =
            playerRule.onNodeWithContentDescription("iconButton").getUnclippedBoundsInRoot()
        assertThat((iconBounds.right - iconBounds.left).value).isWithin(1f).of(60f)
        assertThat((iconBounds.bottom - iconBounds.top).value).isWithin(1f).of(60f)

        val sliderBounds =
            playerRule.onNodeWithContentDescription("slider").getUnclippedBoundsInRoot()
        assertThat((sliderBounds.right - sliderBounds.left).value).isWithin(1f).of(160f)
    }

    @Test
    fun width_then_fillMaxWidth_appliesFirstWidthAndEmitsSingleWidthModifier() {
        val doc = setRemoteContentWithDisplaySize {
            RemoteBox(
                modifier =
                    RemoteModifier.width(120.rdp)
                        .fillMaxWidth()
                        .height(80.rdp)
                        .fillMaxHeight()
                        .semantics { contentDescription = "chainedBox".rs }
            )
        }

        assertNoDuplicateDimensionModifiers(doc)
        val bounds =
            playerRule.onNodeWithContentDescription("chainedBox").getUnclippedBoundsInRoot()
        assertThat((bounds.right - bounds.left).value).isWithin(1f).of(120f)
        assertThat((bounds.bottom - bounds.top).value).isWithin(1f).of(80f)
    }

    @Test
    fun widthIn_then_fillMaxWidth_bothEmittedAndApplied() {
        val doc = setRemoteContentWithDisplaySize {
            RemoteBox(
                modifier =
                    RemoteModifier.widthIn(min = 50.rdp, max = 140.rdp)
                        .fillMaxWidth()
                        .height(60.rdp)
                        .semantics { contentDescription = "widthInThenFill".rs }
            )
        }

        assertNoDuplicateDimensionModifiers(doc)
        val bounds =
            playerRule.onNodeWithContentDescription("widthInThenFill").getUnclippedBoundsInRoot()
        assertThat((bounds.right - bounds.left).value).isWithin(1f).of(140f)
    }

    @Test
    fun width_then_widthIn_bothEmittedAndApplied() {
        val doc = setRemoteContentWithDisplaySize {
            RemoteBox(
                modifier =
                    RemoteModifier.width(110.rdp)
                        .widthIn(min = 50.rdp, max = 200.rdp)
                        .height(60.rdp)
                        .semantics { contentDescription = "widthThenWidthIn".rs }
            )
        }

        assertNoDuplicateDimensionModifiers(doc)
        val bounds =
            playerRule.onNodeWithContentDescription("widthThenWidthIn").getUnclippedBoundsInRoot()
        assertThat((bounds.right - bounds.left).value).isWithin(1f).of(110f)
    }

    private fun setRemoteContentWithDisplaySize(
        content: @Composable @RemoteComposable () -> Unit
    ): CoreDocument {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val displayInfo = createCreationDisplayInfo(context, Size(500f, 500f))
        return playerRule.setRemoteContent(
            remoteCreationDisplayInfo = displayInfo,
            content = content,
        )
    }

    private fun collectLayoutComponents(operations: List<Operation>): List<LayoutComponent> {
        val result = mutableListOf<LayoutComponent>()
        fun visit(op: Any?) {
            if (op is LayoutComponent) {
                result.add(op)
                op.childrenComponents.forEach { visit(it) }
            } else if (op is Component) {
                op.list.forEach { visit(it) }
            }
        }
        operations.forEach { visit(it) }
        return result
    }

    private fun assertNoDuplicateDimensionModifiers(doc: CoreDocument) {
        val layouts =
            collectLayoutComponents(doc.rootLayoutComponent?.let { listOf(it) } ?: emptyList())
        assertThat(layouts).isNotEmpty()
        for (layout in layouts) {
            val widthModifiers =
                layout.componentModifiers.modifiersList.filterIsInstance<WidthModifierOperation>()
            val heightModifiers =
                layout.componentModifiers.modifiersList.filterIsInstance<HeightModifierOperation>()
            assertThat(widthModifiers.size).isAtMost(1)
            assertThat(heightModifiers.size).isAtMost(1)
        }
    }
}
