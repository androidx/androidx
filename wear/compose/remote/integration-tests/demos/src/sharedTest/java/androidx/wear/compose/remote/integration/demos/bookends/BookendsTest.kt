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

package androidx.wear.compose.remote.integration.demos.bookends

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.style.TextAlign
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.remote.integration.demos.bookends.material3.BookendsImplementation
import androidx.wear.compose.remote.integration.demos.bookends.material3.LocalBookendsImplementation
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteAppScaffold
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteButton
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteIcon
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteScreenScaffold
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteText
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteTimeText
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteTransformingLazyColumn
import androidx.wear.compose.remote.integration.demos.bookends.player.WearMaterial3Plugins
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Functional and UI tests for the bookends components using Compose testing APIs and
 * [RcPlayerTestRule], run against both [BookendsImplementation] modes.
 */
abstract class BookendsTest(protected val implementation: BookendsImplementation) {
    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun button_rendersLabelAndDispatchesClick() {
        var clickedName: String? = null
        var clickedValue: Any? = null
        setBookendsContent(
            onNamedAction = { name, value ->
                clickedName = name
                clickedValue = value
            }
        ) {
            RemoteButton(onClick = hostAction("buttonClick".rs, 42.rf)) {
                RemoteText("Button".rs)
            }
        }

        rule
            .onNodeWithText("Button")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        rule.composeRule.runOnIdle {
            assertThat(clickedName).isEqualTo("buttonClick")
            assertThat(clickedValue).isEqualTo(42f)
        }
    }

    @Test
    fun button_clickUpdatesRemoteState() {
        setBookendsContent {
            val clicks = rememberMutableRemoteInt(0)
            RemoteButton(
                onClick = valueChange(clicks, clicks + 1),
                secondaryLabel = { RemoteText("Clicks: ".rs + clicks.toRemoteString()) },
            ) {
                RemoteText("Increment".rs)
            }
        }

        rule.onNodeWithText("Increment").assertIsDisplayed()
        rule.onNodeWithText("Clicks: 0").assertIsDisplayed()

        rule.onNodeWithText("Increment").performClick()
        rule.onNodeWithText("Clicks: 1").assertIsDisplayed()

        rule.onNodeWithText("Increment").performClick()
        rule.onNodeWithText("Clicks: 2").assertIsDisplayed()
    }

    @Test
    fun button_secondaryLabelAndIcon() {
        setBookendsContent {
            RemoteButton(
                onClick = hostAction("noop".rs, 1.rf),
                secondaryLabel = { RemoteText("Secondary label".rs) },
                icon = {
                    RemoteIcon(
                        bitmap = RemoteImageBitmap(bookendsIconBitmap()),
                        contentDescription = "Favorite".rs,
                    )
                },
            ) {
                RemoteText("Primary label".rs)
            }
        }

        rule.onNodeWithText("Primary label").assertIsDisplayed()
        rule.onNodeWithText("Secondary label").assertIsDisplayed()
        rule.onNodeWithContentDescription("Favorite").assertIsDisplayed()
    }

    @Test
    fun button_disabledDoesNotDispatchClick() {
        var clicked = false
        setBookendsContent(onNamedAction = { _, _ -> clicked = true }) {
            RemoteButton(
                onClick = hostAction("disabledClick".rs, 1.rf),
                enabled = false.rb,
                secondaryLabel = { RemoteText("Secondary label".rs) },
            ) {
                RemoteText("Disabled".rs)
            }
        }

        rule.onNodeWithText("Disabled").assertIsDisplayed().performClick()
        rule.composeRule.runOnIdle { assertThat(clicked).isFalse() }
    }

    @Test
    fun text_rendersAllVariants() {
        setBookendsContent {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxWidth(),
                verticalArrangement = RemoteArrangement.spacedBy(4.rdp),
            ) {
                RemoteText("Default".rs)
                RemoteText(
                    "Start".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                )
                RemoteText(
                    "Center".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                RemoteText(
                    "End".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
                RemoteText("Colored".rs, color = RemoteColor(Color.Cyan))
                RemoteText(
                    "Single line text".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    maxLines = 1,
                )
            }
        }

        rule.onNodeWithText("Default").assertIsDisplayed()
        rule.onNodeWithText("Start").assertIsDisplayed()
        rule.onNodeWithText("Center").assertIsDisplayed()
        rule.onNodeWithText("End").assertIsDisplayed()
        rule.onNodeWithText("Colored").assertIsDisplayed()
        rule.onNodeWithText("Single line text").assertIsDisplayed()
    }

    protected fun setBookendsContent(
        onNamedAction: (name: String, value: Any?) -> Unit = { _, _ -> },
        content: @Composable @RemoteComposable () -> Unit,
    ): CoreDocument {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        return rule.setRemoteContent(
            customPlugins = WearMaterial3Plugins,
            profile = BookendsProfile,
            onNamedAction = { name, value, _ -> onNamedAction(name, value) },
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    context = context,
                    size = Size(200f * density, 200f * density),
                ),
        ) {
            CompositionLocalProvider(LocalBookendsImplementation provides implementation) {
                content()
            }
        }
    }

    protected fun CoreDocument.customComponentCount(): Int {
        var count = 0
        fun visit(ops: Collection<Operation>) {
            for (op in ops) {
                if (op is Custom) {
                    count++
                }
                if (op is Container) {
                    visit(op.list)
                }
            }
        }
        rootLayoutComponent?.let { visit(it.list) }
        return count
    }
}

/** Runs [BookendsTest] using [BookendsImplementation.WearMaterial3] (`Custom` components). */
@RunWith(AndroidJUnit4::class)
class WearMaterial3BookendsTest : BookendsTest(BookendsImplementation.WearMaterial3) {
    @Test
    fun timeText_renders() {
        val doc = setBookendsContent {
            RemoteTimeText(modifier = RemoteModifier.fillMaxSize(), time = "10:09".rs)
        }

        assertThat(doc.customComponentCount()).isEqualTo(1)
    }

    @Test
    fun bookendsDemoContent_rendersAndIncrementsCounterOnClick() {
        setBookendsContent { BookendsDemoContent() }

        rule.onNodeWithText("Bookends").assertIsDisplayed()
        rule.onNodeWithText("Tap me").assertIsDisplayed().assertIsEnabled()
        rule.onNodeWithText("Clicks: 0").assertIsDisplayed()

        // Clicking Tap me increments the remote counter.
        rule.onNodeWithText("Tap me").performClick()
        rule.onNodeWithText("Clicks: 1").assertIsDisplayed()
    }

    @Test
    fun screenScaffold_withTransformingLazyColumn_rendersAndDispatchesItemClicks() {
        var clickedItem = -1
        setBookendsContent(onNamedAction = { _, value -> clickedItem = (value as Float).toInt() }) {
            RemoteAppScaffold(timeText = { RemoteTimeText(time = "10:09".rs) }) {
                RemoteScreenScaffold {
                    RemoteTransformingLazyColumn {
                        item { RemoteText("Header".rs) }
                        items(2) { index ->
                            RemoteButton(onClick = hostAction("item".rs, index.toFloat().rf)) {
                                RemoteText("Row $index".rs)
                            }
                        }
                    }
                }
            }
        }

        rule.onNodeWithText("Header").assertIsDisplayed()
        rule.onNodeWithText("Row 0").assertIsDisplayed().performClick()
        rule.composeRule.runOnIdle { assertThat(clickedItem).isEqualTo(0) }

        rule.onNodeWithText("Row 1").assertIsDisplayed().performClick()
        rule.composeRule.runOnIdle { assertThat(clickedItem).isEqualTo(1) }
    }

    @Test
    fun implementation_emitsCustomOperations() {
        val doc = setBookendsContent {
            RemoteButton(onClick = hostAction("click".rs, 1.rf)) {
                RemoteText("Button".rs)
            }
        }

        assertThat(doc.customComponentCount()).isAtLeast(2)
    }
}

/**
 * Runs [BookendsTest] using [BookendsImplementation.RemoteMaterial3]
 * (`androidx.wear.compose.remote.material3` primitives).
 */
@RunWith(AndroidJUnit4::class)
class RemoteMaterial3BookendsTest : BookendsTest(BookendsImplementation.RemoteMaterial3) {
    @Test
    fun implementation_doesNotEmitCustomOperations() {
        val doc = setBookendsContent {
            RemoteButton(onClick = hostAction("click".rs, 1.rf)) {
                RemoteText("Button".rs)
            }
        }

        assertThat(doc.customComponentCount()).isEqualTo(0)
    }
}
