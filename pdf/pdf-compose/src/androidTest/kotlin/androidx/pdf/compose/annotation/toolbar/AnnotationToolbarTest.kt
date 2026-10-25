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

package androidx.pdf.compose.annotation.toolbar

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.R
import androidx.pdf.view.annotation.AnnotationToolbarView.Companion.DOCK_STATE_END
import androidx.pdf.view.annotation.AnnotationToolbarView.Companion.DOCK_STATE_START
import androidx.pdf.view.annotation.tool.AnnotationToolInfo
import androidx.pdf.view.annotation.tool.Eraser
import androidx.pdf.view.annotation.tool.Highlighter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPdfApi::class)
@RunWith(AndroidJUnit4::class)
@MediumTest
class AnnotationToolbarTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun annotationToolbarState_defaults() {
        lateinit var state: AnnotationToolbarState
        rule.setContent {
            // Override smallestScreenWidthDp to phone size (< 600dp) so AnnotationToolbarView
            // selects its phone-default dock edge (Bottom).
            WithSmallestScreenWidth(PHONE_SMALLEST_WIDTH_DP) {
                state = rememberAnnotationToolbarState()
                AnnotationToolbar(state = state)
            }
        }

        rule.runOnIdle {
            assertThat(state.canUndo).isFalse()
            assertThat(state.canRedo).isFalse()
            assertThat(state.isConfigPopupVisible).isFalse()
            assertThat(state.isExpanded).isTrue()
            assertThat(state.dockState).isEqualTo(AnnotationToolbarDockState.Bottom)
        }
    }

    @Test
    fun annotationToolbarState_onTablet_adoptsEndDockStateFromView() {
        lateinit var state: AnnotationToolbarState
        rule.setContent {
            // Override smallestScreenWidthDp to tablet size (>= 600dp). AnnotationToolbarView
            // initializes dockState to End on large screens, which AnnotationToolbarState adopts.
            WithSmallestScreenWidth(TABLET_SMALLEST_WIDTH_DP) {
                state = rememberAnnotationToolbarState()
                AnnotationToolbar(state = state)
            }
        }

        rule.runOnIdle {
            assertThat(state.dockState).isEqualTo(AnnotationToolbarDockState.End)
            assertThat(checkNotNull(state.toolbarView).dockState).isEqualTo(DOCK_STATE_END)
        }
    }

    @Test
    fun annotationToolbarState_onTablet_explicitDockStateOverridesViewDefault() {
        lateinit var state: AnnotationToolbarState
        rule.setContent {
            WithSmallestScreenWidth(TABLET_SMALLEST_WIDTH_DP) {
                // An explicit dockState preset on AnnotationToolbarState overrides the tablet
                // form-factor default (End).
                state =
                    rememberAnnotationToolbarState().apply {
                        dockState = AnnotationToolbarDockState.Start
                    }
                AnnotationToolbar(state = state)
            }
        }

        rule.runOnIdle {
            assertThat(state.dockState).isEqualTo(AnnotationToolbarDockState.Start)
            assertThat(checkNotNull(state.toolbarView).dockState).isEqualTo(DOCK_STATE_START)
        }
    }

    @Test
    fun annotationToolbarState_updatesReflectedInView() {
        lateinit var state: AnnotationToolbarState
        rule.setContent {
            state = rememberAnnotationToolbarState()
            AnnotationToolbar(state = state)
        }

        rule.runOnIdle {
            state.canUndo = true
            state.canRedo = true
            state.dockState = AnnotationToolbarDockState.End

            assertThat(state.toolbarView?.canUndo).isTrue()
            assertThat(state.toolbarView?.canRedo).isTrue()
            assertThat(state.toolbarView?.dockState).isEqualTo(DOCK_STATE_END)
        }
    }

    @Test
    fun annotationToolbarState_bufferedBeforeAttachment_syncedOnAttach() {
        // Pre-configure state properties before the toolbar view is inflated and attached to
        // verify buffering and subsequent synchronization on attachment.
        val state =
            AnnotationToolbarState().apply {
                canUndo = true
                canRedo = true
                dockState = AnnotationToolbarDockState.Start
                isExpanded = false
            }

        rule.setContent { AnnotationToolbar(state = state) }

        rule.runOnIdle {
            val toolbarView = checkNotNull(state.toolbarView)
            assertThat(toolbarView.canUndo).isTrue()
            assertThat(toolbarView.canRedo).isTrue()
            assertThat(toolbarView.dockState).isEqualTo(DOCK_STATE_START)
            assertThat(state.isExpanded).isFalse()
            // When collapsed, the tool tray is hidden and only the collapsed pill icon is visible.
            assertThat(toolbarView.findViewById<View>(R.id.collapsed_tool).visibility)
                .isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun rememberAnnotationToolbarState_restoresStateOnRecreation() {
        val restorationTester = StateRestorationTester(rule)
        lateinit var state: AnnotationToolbarState

        restorationTester.setContent {
            state = rememberAnnotationToolbarState()
            AnnotationToolbar(state = state)
        }

        rule.runOnIdle {
            state.canUndo = true
            state.canRedo = true
            state.dockState = AnnotationToolbarDockState.End
            // Select highlighter on the view to verify tool selection survives state restoration.
            checkNotNull(state.toolbarView)
                .findViewById<View>(R.id.highlighter_button)
                .performClick()
            state.isExpanded = false
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            val toolbarView = checkNotNull(state.toolbarView)
            assertThat(state.canUndo).isTrue()
            assertThat(state.canRedo).isTrue()
            assertThat(state.dockState).isEqualTo(AnnotationToolbarDockState.End)
            assertThat(state.isExpanded).isFalse()
            assertThat(toolbarView.findViewById<View>(R.id.highlighter_button).isSelected).isTrue()
        }
    }

    @Test
    fun annotationToolbarState_controlMethodsDelegateToView() {
        lateinit var state: AnnotationToolbarState
        lateinit var toolbarView: View
        lateinit var penButton: View
        rule.setContent {
            state = rememberAnnotationToolbarState()
            AnnotationToolbar(state = state)
        }

        rule.runOnIdle {
            toolbarView = checkNotNull(state.toolbarView)
            penButton = toolbarView.findViewById(R.id.pen_button)
            state.canUndo = true
            state.canRedo = true
            state.isExpanded = false
        }
        rule.runOnIdle {
            assertThat(state.isExpanded).isFalse()
            state.isExpanded = true
        }
        rule.runOnIdle {
            assertThat(state.isExpanded).isTrue()
            // Clicking the active tool button toggles its configuration popup (brush size slider).
            penButton.performClick()
        }
        rule.runOnIdle {
            assertThat(state.isConfigPopupVisible).isTrue()
            state.dismissPopups()
        }
        rule.runOnIdle {
            assertThat(state.isConfigPopupVisible).isFalse()
            assertThat(penButton.isSelected).isTrue()
            state.clearToolSelection()
        }
        rule.runOnIdle {
            assertThat(penButton.isSelected).isFalse()
            state.reset()
        }
        rule.runOnIdle {
            // reset() restores the toolbar to its initial state: default tool selected (Pen),
            // popups dismissed, and undo/redo disabled.
            assertThat(penButton.isSelected).isTrue()
            assertThat(state.isConfigPopupVisible).isFalse()
            assertThat(state.canUndo).isFalse()
            assertThat(state.canRedo).isFalse()
        }
    }

    @Test
    fun annotationToolbarState_observesViewStateChanges() {
        lateinit var state: AnnotationToolbarState
        rule.setContent {
            state = rememberAnnotationToolbarState()
            AnnotationToolbar(state = state)
        }

        rule.runOnIdle {
            checkNotNull(state.toolbarView).findViewById<View>(R.id.pen_button).performClick()
        }
        rule.runOnIdle {
            assertThat(state.isConfigPopupVisible).isTrue()
            checkNotNull(state.toolbarView).dismissPopups()
        }
        rule.runOnIdle {
            assertThat(state.isConfigPopupVisible).isFalse()
            checkNotNull(state.toolbarView).collapseToolbar()
        }
        rule.runOnIdle {
            assertThat(state.isExpanded).isFalse()
            checkNotNull(state.toolbarView).expandToolbar()
        }
        rule.runOnIdle {
            assertThat(state.isExpanded).isTrue()
            checkNotNull(state.toolbarView).dockState = DOCK_STATE_START
        }
        rule.runOnIdle {
            assertThat(state.dockState).isEqualTo(AnnotationToolbarDockState.Start)
        }
    }

    @Test
    fun annotationToolbar_invokesCallbacksOnInteraction() {
        lateinit var state: AnnotationToolbarState
        val selectedTools = mutableListOf<AnnotationToolInfo>()
        var undoCount = 0
        var redoCount = 0
        val visibilityChanges = mutableListOf<Boolean>()

        rule.setContent {
            state = rememberAnnotationToolbarState()
            AnnotationToolbar(
                state = state,
                onToolChanged = { selectedTools.add(it) },
                onUndo = { undoCount++ },
                onRedo = { redoCount++ },
                onAnnotationVisibilityChanged = { visibilityChanges.add(it) },
            )
        }

        rule.runOnIdle {
            state.canUndo = true
            state.canRedo = true
            checkNotNull(state.toolbarView)
                .findViewById<View>(R.id.highlighter_button)
                .performClick()
        }
        rule.runOnIdle {
            assertThat(selectedTools.last()).isInstanceOf(Highlighter::class.java)
            checkNotNull(state.toolbarView).findViewById<View>(R.id.eraser_button).performClick()
        }
        rule.runOnIdle {
            assertThat(selectedTools.last()).isEqualTo(Eraser)
            checkNotNull(state.toolbarView).findViewById<View>(R.id.undo_button).performClick()
            checkNotNull(state.toolbarView).findViewById<View>(R.id.redo_button).performClick()
        }
        rule.runOnIdle {
            assertThat(undoCount).isEqualTo(1)
            assertThat(redoCount).isEqualTo(1)
            // Annotations are visible by default; first click toggles visibility off (false).
            checkNotNull(state.toolbarView)
                .findViewById<View>(R.id.toggle_annotation_button)
                .performClick()
        }
        rule.runOnIdle {
            // Second click toggles annotation visibility back on (true).
            checkNotNull(state.toolbarView)
                .findViewById<View>(R.id.toggle_annotation_button)
                .performClick()
        }
        rule.runOnIdle {
            assertThat(visibilityChanges).containsExactly(false, true).inOrder()
        }
    }

    @Test
    fun annotationToolbar_appliesContentPadding() {
        lateinit var state: AnnotationToolbarState
        lateinit var density: Density
        var padding by
            mutableStateOf(PaddingValues(start = 8.dp, top = 12.dp, end = 16.dp, bottom = 20.dp))

        rule.setContent {
            density = LocalDensity.current
            state = rememberAnnotationToolbarState()
            AnnotationToolbar(state = state, contentPadding = padding)
        }

        rule.runOnIdle {
            val toolbarView = checkNotNull(state.toolbarView)
            with(density) {
                assertThat(toolbarView.paddingLeft).isEqualTo(8.dp.roundToPx())
                assertThat(toolbarView.paddingTop).isEqualTo(12.dp.roundToPx())
                assertThat(toolbarView.paddingRight).isEqualTo(16.dp.roundToPx())
                assertThat(toolbarView.paddingBottom).isEqualTo(20.dp.roundToPx())
            }
            padding = PaddingValues(0.dp)
        }

        rule.runOnIdle {
            val toolbarView = checkNotNull(state.toolbarView)
            assertThat(toolbarView.paddingLeft).isEqualTo(0)
            assertThat(toolbarView.paddingTop).isEqualTo(0)
            assertThat(toolbarView.paddingRight).isEqualTo(0)
            assertThat(toolbarView.paddingBottom).isEqualTo(0)
        }
    }

    @Test
    fun annotationToolbar_addsCustomTools_andHandlesClicks() {
        lateinit var state: AnnotationToolbarState
        var customToolClicked = false

        rule.setContent {
            state = rememberAnnotationToolbarState()
            AnnotationToolbar(state = state) {
                tool(key = "custom_tool_1", index = 0) {
                    BasicText(
                        text = "CustomTool1",
                        modifier = Modifier.clickable { customToolClicked = true },
                    )
                }
                tool(key = "custom_tool_2") { BasicText(text = "CustomTool2") }
            }
        }

        rule.onNodeWithText("CustomTool1").performClick()

        rule.runOnIdle {
            assertThat(state.toolbarView).isNotNull()
            assertThat(customToolClicked).isTrue()
        }
    }

    @Test
    fun annotationToolbar_customTools_updatesWhenObservableStateChanges() {
        val toolsList = mutableStateListOf("ToolA")

        rule.setContent {
            AnnotationToolbar(state = rememberAnnotationToolbarState()) {
                toolsList.forEach { toolKey ->
                    tool(key = toolKey) {
                        BasicText(text = toolKey)
                    }
                }
            }
        }

        rule.onNodeWithText("ToolA").assertExists()
        rule.onNodeWithText("ToolB").assertDoesNotExist()

        rule.runOnIdle { toolsList.add("ToolB") }

        rule.onNodeWithText("ToolA").assertExists()
        rule.onNodeWithText("ToolB").assertExists()

        rule.runOnIdle { toolsList.remove("ToolA") }

        rule.onNodeWithText("ToolA").assertDoesNotExist()
        rule.onNodeWithText("ToolB").assertExists()
    }

    @Test
    fun annotationToolbar_customTools_preservesInternalStateAndMinimizesRecompositions() {
        var padding by mutableStateOf(PaddingValues(0.dp))
        val counters = RecompositionCounters()

        @Composable
        fun TestHarness(capturedValue: Int) {
            AnnotationToolbar(
                state = rememberAnnotationToolbarState(),
                contentPadding = padding,
            ) {
                tool(key = "stateful_tool") {
                    var internalCounter by remember { mutableStateOf(100) }
                    SideEffect { counters.tool1Recompositions++ }
                    DisposableEffect(Unit) { onDispose { counters.tool1DisposeCount++ } }
                    BasicText(
                        text = "Outer: $capturedValue, Internal: $internalCounter",
                        modifier = Modifier.clickable { internalCounter++ },
                    )
                }
                tool(key = "static_tool") {
                    SideEffect { counters.tool2Recompositions++ }
                    BasicText(text = "StaticTool")
                }
            }
        }

        var outerValue by mutableStateOf(0)
        rule.setContent { TestHarness(capturedValue = outerValue) }

        rule.onNodeWithText("Outer: 0, Internal: 100").assertExists()
        rule.runOnIdle {
            assertThat(counters.tool1Recompositions).isEqualTo(1)
            assertThat(counters.tool2Recompositions).isEqualTo(1)
            assertThat(counters.tool1DisposeCount).isEqualTo(0)
        }

        // Mutate internal state inside tool 1 -> only tool 1 recomposes; tool 2 does not
        rule.onNodeWithText("Outer: 0, Internal: 100").performClick()
        rule.onNodeWithText("Outer: 0, Internal: 101").assertExists()
        rule.runOnIdle {
            assertThat(counters.tool1Recompositions).isEqualTo(2)
            assertThat(counters.tool2Recompositions).isEqualTo(1)
            assertThat(counters.tool1DisposeCount).isEqualTo(0)
        }

        // Mutate unrelated toolbar parameter (contentPadding) -> neither custom tool recomposes
        rule.runOnIdle { padding = PaddingValues(16.dp) }
        rule.runOnIdle {
            assertThat(counters.tool1Recompositions).isEqualTo(2)
            assertThat(counters.tool2Recompositions).isEqualTo(1)
            assertThat(counters.tool1DisposeCount).isEqualTo(0)
        }

        // Mutate captured primitive in `content` -> UI updates, internal state is preserved,
        // and the ComposeView is never disposed/recreated
        rule.runOnIdle { outerValue = 1 }
        rule.onNodeWithText("Outer: 1, Internal: 101").assertExists()
        rule.runOnIdle {
            assertThat(counters.tool1Recompositions).isEqualTo(3)
            assertThat(counters.tool1DisposeCount).isEqualTo(0)
        }
    }

    @Test
    fun annotationToolbar_onDispose_clearsToolbarViewReference() {
        lateinit var state: AnnotationToolbarState
        var showToolbar by mutableStateOf(true)

        rule.setContent {
            state = rememberAnnotationToolbarState()
            if (showToolbar) {
                AnnotationToolbar(state = state)
            }
        }

        rule.runOnIdle { assertThat(state.toolbarView).isNotNull() }

        showToolbar = false

        rule.runOnIdle { assertThat(state.toolbarView).isNull() }
    }
}

@Stable
private class RecompositionCounters {
    var tool1Recompositions = 0
    var tool2Recompositions = 0
    var tool1DisposeCount = 0
}

private const val PHONE_SMALLEST_WIDTH_DP = 400
private const val TABLET_SMALLEST_WIDTH_DP = 600

/**
 * Provides a [LocalContext] whose configuration reports [smallestScreenWidthDp], which
 * `AnnotationToolbarView` uses to pick its default dock edge.
 */
@Composable
private fun WithSmallestScreenWidth(smallestScreenWidthDp: Int, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val context =
        remember(baseContext, smallestScreenWidthDp) {
            val configuration =
                Configuration(baseContext.resources.configuration).apply {
                    this.smallestScreenWidthDp = smallestScreenWidthDp
                }
            baseContext.createConfigurationContext(configuration)
        }
    CompositionLocalProvider(LocalContext provides context, content = content)
}
