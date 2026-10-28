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

package androidx.compose.ui

import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.util.LongSparseArray
import android.util.SparseArray
import android.view.View
import android.view.ViewStructure
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.view.translation.TranslationRequestValue
import android.view.translation.TranslationResponseValue
import android.view.translation.ViewTranslationRequest
import android.view.translation.ViewTranslationRequest.ID_TEXT
import android.view.translation.ViewTranslationResponse
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.ExperimentalComposeUiHostApi
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.AdjustedSemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearTextSubstitution
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getAllUncoveredSemanticsNodesToIntObjectMap
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.isShowingTextSubstitution
import androidx.compose.ui.semantics.onAutofillText
import androidx.compose.ui.semantics.onFillData
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.semanticsConfiguration
import androidx.compose.ui.semantics.setTextSubstitution
import androidx.compose.ui.semantics.showTextSubstitution
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.text
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, minSdk = 29)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EnableUserInteractionHostTest {
    @get:Rule val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun modifierEqualsAndInspectableValue() {
        val enabledModifier = Modifier.enableUserInteraction(true)
        val disabledModifier = Modifier.enableUserInteraction(false)

        assertThat(enabledModifier).isEqualTo(Modifier.enableUserInteraction(true))
        assertThat(disabledModifier).isEqualTo(Modifier.enableUserInteraction(false))
        assertThat(enabledModifier).isNotEqualTo(disabledModifier)

        val inspectable = disabledModifier as InspectableValue
        assertThat(inspectable.nameFallback).isEqualTo("enableUserInteraction")
        assertThat(inspectable.valueOverride).isNull()
        assertThat(inspectable.inspectableElements.asIterable())
            .containsExactly(ValueElement("enabled", false))
    }

    @Test
    fun pointerInput_disabledBlocksTouchesToSubtree_andPassesThroughToSiblingsBehind() {
        var backgroundClicked = false
        var foregroundClicked = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                SimpleBox(sizePx = 200) {
                    SimpleBox(
                        sizePx = 100,
                        modifier =
                            Modifier.testTag("background").simpleClickable {
                                backgroundClicked = true
                            },
                    )
                    SimpleBox(
                        sizePx = 200,
                        modifier = Modifier.testTag("foreground").enableUserInteraction(false),
                    ) {
                        SimpleBox(
                            sizePx = 100,
                            modifier = Modifier.simpleClickable { foregroundClicked = true },
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag("foreground").performTouchInput { click(Offset(50f, 50f)) }

        rule.runOnIdle {
            assertThat(foregroundClicked).isFalse()
            assertThat(backgroundClicked).isTrue()
        }
    }

    @Test
    fun pointerInput_disabledOnSameNodeBlocksTouchesRegardlessOfOrder() {
        var clickedBefore = false
        var clickedAfter = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                SimpleBox(sizePx = 200) {
                    SimpleBox(
                        sizePx = 50,
                        modifier =
                            Modifier.testTag("clickableBefore")
                                .simpleClickable { clickedBefore = true }
                                .enableUserInteraction(false),
                    )
                    SimpleBox(
                        sizePx = 50,
                        modifier =
                            Modifier.pxOffset(100, 0)
                                .testTag("clickableAfter")
                                .enableUserInteraction(false)
                                .simpleClickable { clickedAfter = true },
                    )
                }
            }
        }

        rule.onNodeWithTag("clickableBefore").performTouchInput { click(Offset(25f, 25f)) }
        rule.onNodeWithTag("clickableAfter").performTouchInput { click(Offset(25f, 25f)) }

        rule.runOnIdle {
            assertThat(clickedBefore).isFalse()
            assertThat(clickedAfter).isFalse()
        }
    }

    @Test
    fun pointerInput_dynamicToggleAndNestedTrueInsideFalse() {
        var clickCount = 0
        var enabled by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                SimpleBox(sizePx = 200, modifier = Modifier.enableUserInteraction(enabled)) {
                    SimpleBox(sizePx = 100, modifier = Modifier.enableUserInteraction(true)) {
                        SimpleBox(
                            sizePx = 50,
                            modifier = Modifier.testTag("child").simpleClickable { clickCount++ },
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag("child").performTouchInput { click(Offset(25f, 25f)) }
        rule.runOnIdle { assertThat(clickCount).isEqualTo(1) }

        rule.runOnIdle { enabled = false }
        rule.onNodeWithTag("child").performTouchInput { click(Offset(25f, 25f)) }
        rule.runOnIdle { assertThat(clickCount).isEqualTo(1) }

        rule.runOnIdle { enabled = true }
        rule.onNodeWithTag("child").performTouchInput { click(Offset(25f, 25f)) }
        rule.runOnIdle { assertThat(clickCount).isEqualTo(2) }
    }

    @Test
    fun pointerInput_disabledMidGesture_cancelsActivePointerAndPreventsClick() {
        var clicked = false
        var downObserved = false
        var cancelObserved = false
        var enabled by mutableStateOf(true)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                SimpleBox(sizePx = 200, modifier = Modifier.enableUserInteraction(enabled)) {
                    SimpleBox(
                        sizePx = 100,
                        modifier =
                            Modifier.testTag("child")
                                .simpleClickable(
                                    onDown = { downObserved = true },
                                    onCancel = { cancelObserved = true },
                                    onClick = { clicked = true },
                                ),
                    )
                }
            }
        }

        rule.onNodeWithTag("child").performTouchInput { down(Offset(50f, 50f)) }
        rule.runOnIdle {
            assertThat(downObserved).isTrue()
            assertThat(cancelObserved).isFalse()
        }

        rule.runOnIdle { enabled = false }
        rule.runOnIdle { assertThat(cancelObserved).isTrue() }

        rule.onNodeWithTag("child").performTouchInput { up() }
        rule.runOnIdle { assertThat(clicked).isFalse() }
    }

    @Test
    fun pointerInput_conditionallyAttachedMidGesture_cancelsActivePointerAndPreventsClick() {
        var clicked = false
        var downObserved = false
        var cancelObserved = false
        var disable by mutableStateOf(false)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                SimpleBox(
                    sizePx = 200,
                    modifier = if (disable) Modifier.enableUserInteraction(false) else Modifier,
                ) {
                    SimpleBox(
                        sizePx = 100,
                        modifier =
                            Modifier.testTag("child")
                                .simpleClickable(
                                    onDown = { downObserved = true },
                                    onCancel = { cancelObserved = true },
                                    onClick = { clicked = true },
                                ),
                    )
                }
            }
        }

        rule.onNodeWithTag("child").performTouchInput { down(Offset(50f, 50f)) }
        rule.runOnIdle {
            assertThat(downObserved).isTrue()
            assertThat(cancelObserved).isFalse()
        }

        // Conditionally attaches UserInteractionNode(enabled = false) onto an already-attached
        // LayoutNode, exercising UserInteractionNode.onAttach() rather than update().
        rule.runOnIdle { disable = true }
        rule.runOnIdle { assertThat(cancelObserved).isTrue() }

        rule.onNodeWithTag("child").performTouchInput { up() }
        rule.runOnIdle { assertThat(clicked).isFalse() }
    }

    @Test
    fun delegatingSemanticsNode_beforeDisabledModifier_stillBlocksInteraction() {
        var clicked = false
        lateinit var semanticsOwner: SemanticsOwner

        rule.setContent {
            semanticsOwner = (LocalView.current as RootForTest).semanticsOwner
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                SimpleBox(
                    sizePx = 100,
                    modifier =
                        Modifier.testTag("target")
                            .then(DelegatingSemanticsElement("delegated"))
                            .enableUserInteraction(false)
                            .simpleClickable { clicked = true },
                )
            }
        }

        rule.onNodeWithTag("target").performTouchInput { click(Offset(50f, 50f)) }

        rule.runOnIdle {
            assertThat(clicked).isFalse()
            val uncovered = semanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap(0) { false }
            assertThat(uncovered.findByTag("target")).isNull()
        }
    }

    @Test
    fun clippedAndUnclippedOutOfBoundsScene_disabledSubtreeBlocksAllGatesWithoutGeometryLeak() {
        // Reproduces the SystemUI multi-scene layout where a child's unclipped bounds extend
        // outside a smaller parent container (e.g., fillMaxSize / translated scene).
        var outOfBoundsClicked = false
        lateinit var semanticsOwner: SemanticsOwner

        rule.setContent {
            semanticsOwner = (LocalView.current as RootForTest).semanticsOwner
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                SimpleBox(sizePx = 200) {
                    SimpleBox(
                        sizePx = 100,
                        modifier =
                            Modifier.semantics {
                                testTag = "activeSibling"
                                contentDescription = "activeSibling"
                            },
                    )
                    SimpleBox(sizePx = 100, modifier = Modifier.enableUserInteraction(false)) {
                        SimpleBox(
                            sizePx = 300,
                            modifier =
                                Modifier.simpleClickable { outOfBoundsClicked = true }
                                    .semantics {
                                        testTag = "overflowingDisabledChild"
                                        contentDescription = "overflowingDisabledChild"
                                    },
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag("overflowingDisabledChild").performTouchInput {
            click(Offset(150f, 150f))
        }

        rule.runOnIdle {
            assertThat(outOfBoundsClicked).isFalse()
            val uncovered = semanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap(0) { false }
            assertThat(uncovered.findByTag("activeSibling")).isNotNull()
            assertThat(uncovered.findByTag("overflowingDisabledChild")).isNull()
        }
        rule.onNodeWithTag("overflowingDisabledChild").assertExists()
    }

    @Test
    fun enableUserInteraction_setsDedicatedNodeKindBitOnlyWhenPresent() {
        var attachModifier by mutableStateOf(true)
        var enabled by mutableStateOf(false)

        rule.setContent {
            SimpleBox(sizePx = 100) {
                SimpleBox(sizePx = 50, modifier = Modifier.testTag("plainSemantics"))
                SimpleBox(
                    sizePx = 50,
                    modifier =
                        Modifier.testTag("gated")
                            .then(
                                if (attachModifier) Modifier.enableUserInteraction(enabled)
                                else Modifier
                            ),
                )
            }
        }

        val plainNode = rule.onNodeWithTag("plainSemantics").fetchSemanticsNode().layoutNode
        val gatedNode = rule.onNodeWithTag("gated").fetchSemanticsNode().layoutNode

        rule.runOnIdle {
            assertThat(plainNode.nodes.has(Nodes.Semantics)).isTrue()
            assertThat(plainNode.nodes.has(Nodes.UserInteraction)).isFalse()
            assertThat(gatedNode.nodes.has(Nodes.UserInteraction)).isTrue()
            assertThat(gatedNode.nodes.has(Nodes.Semantics)).isTrue()
            assertThat(gatedNode.isUserInteractionDisabled()).isTrue()
        }

        rule.runOnIdle { enabled = true }
        rule.runOnIdle {
            assertThat(gatedNode.nodes.has(Nodes.UserInteraction)).isTrue()
            assertThat(gatedNode.isUserInteractionDisabled()).isFalse()
        }

        rule.runOnIdle { attachModifier = false }
        rule.runOnIdle {
            assertThat(gatedNode.nodes.has(Nodes.UserInteraction)).isFalse()
            assertThat(gatedNode.isUserInteractionDisabled()).isFalse()
        }
    }

    @OptIn(ExperimentalComposeUiHostApi::class)
    @Test
    fun semanticsHitTest_skipsDisabledSubtreeAndHitsSiblingBehind() {
        lateinit var semanticsOwner: SemanticsOwner

        rule.setContent {
            semanticsOwner = (LocalView.current as RootForTest).semanticsOwner
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                SimpleBox(sizePx = 200) {
                    SimpleBox(
                        sizePx = 100,
                        modifier =
                            Modifier.semantics {
                                testTag = "sibling"
                                contentDescription = "sibling"
                            },
                    )
                    SimpleBox(sizePx = 100, modifier = Modifier.enableUserInteraction(false)) {
                        SimpleBox(
                            sizePx = 100,
                            modifier =
                                Modifier.semantics {
                                    testTag = "disabledChild"
                                    contentDescription = "disabledChild"
                                },
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            val hitTags = mutableListOf<String>()
            semanticsOwner.hitTest(Offset(25f, 25f)) { layoutInfo ->
                layoutInfo.semanticsConfiguration
                    ?.getOrNull(SemanticsProperties.TestTag)
                    ?.let(hitTags::add)
                false
            }
            assertThat(hitTags).contains("sibling")
            assertThat(hitTags).doesNotContain("disabledChild")
        }
    }

    @OptIn(ExperimentalComposeUiHostApi::class)
    @Test
    fun semanticsBounds_outerEnableUserInteractionBeforePadding_doesNotIncludePadding() {
        var enabled by mutableStateOf(true)
        lateinit var semanticsOwner: SemanticsOwner

        rule.setContent {
            semanticsOwner = (LocalView.current as RootForTest).semanticsOwner
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                SimpleBox(sizePx = 200) {
                    SimpleBox(
                        sizePx = 50,
                        modifier = Modifier.pxPadding(16).testTag("control"),
                    )
                    SimpleBox(
                        sizePx = 50,
                        modifier =
                            Modifier.enableUserInteraction(enabled).pxPadding(16).testTag("gated"),
                    )
                }
            }
        }

        val controlSemantics = rule.onNodeWithTag("control").fetchSemanticsNode()
        val gatedSemantics = rule.onNodeWithTag("gated").fetchSemanticsNode()

        rule.runOnIdle {
            assertThat(controlSemantics.boundsInRoot).isEqualTo(Rect(16f, 16f, 66f, 66f))
            assertThat(gatedSemantics.boundsInRoot).isEqualTo(controlSemantics.boundsInRoot)
            assertThat(gatedSemantics.touchBoundsInRoot)
                .isEqualTo(controlSemantics.touchBoundsInRoot)
            val uncovered = semanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap(0) { false }
            assertThat(uncovered.findByTag("gated")?.adjustedBounds)
                .isEqualTo(IntRect(16, 16, 66, 66))
        }

        rule.runOnIdle { enabled = false }
        rule.runOnIdle {
            val uncovered = semanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap(0) { false }
            assertThat(uncovered.findByTag("gated")).isNull()
        }
    }

    @OptIn(ExperimentalComposeUiHostApi::class)
    @Test
    fun contentCapture_reportsUncoveredDisabledNodes_withoutOccludingSiblingsBehind() {
        lateinit var androidComposeView: AndroidComposeView
        lateinit var semanticsOwner: SemanticsOwner

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            semanticsOwner = (androidComposeView as RootForTest).semanticsOwner
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                SimpleBox(sizePx = 200) {
                    // Active background sibling at (0, 0, 100, 100).
                    SimpleBox(
                        sizePx = 100,
                        modifier =
                            Modifier.semantics {
                                testTag = "activeBehind"
                                contentDescription = "activeBehind"
                            },
                    )
                    // Disabled subtree with one visible node overlapping activeBehind and one node
                    // covered by the active foreground modal below.
                    SimpleBox(sizePx = 200, modifier = Modifier.enableUserInteraction(false)) {
                        SimpleBox(
                            sizePx = 100,
                            modifier =
                                Modifier.semantics {
                                    testTag = "disabledVisible"
                                    contentDescription = "disabledVisible"
                                },
                        )
                        SimpleBox(
                            sizePx = 60,
                            modifier =
                                Modifier.pxOffset(120, 120).semantics {
                                    testTag = "disabledCovered"
                                    contentDescription = "disabledCovered"
                                },
                        )
                    }
                    // Active foreground modal covering (100, 100, 200, 200).
                    SimpleBox(
                        sizePx = 100,
                        modifier =
                            Modifier.pxOffset(100, 100).semantics {
                                testTag = "activeModal"
                                contentDescription = "activeModal"
                            },
                    )
                }
            }
        }

        rule.runOnIdle {
            val a11yNodes = semanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap(0) { false }
            assertThat(a11yNodes.findByTag("activeBehind")).isNotNull()
            assertThat(a11yNodes.findByTag("activeModal")).isNotNull()
            assertThat(a11yNodes.findByTag("disabledVisible")).isNull()
            assertThat(a11yNodes.findByTag("disabledCovered")).isNull()

            val ccNodes = androidComposeView.contentCaptureManager.currentSemanticsNodes
            assertThat(ccNodes.findByTag("activeModal")).isNotNull()
            assertThat(ccNodes.findByTag("disabledVisible")).isNotNull()
            assertThat(ccNodes.findByTag("activeBehind")).isNotNull()
            assertThat(ccNodes.findByTag("disabledCovered")).isNull()
        }
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.S)
    fun contentCaptureTranslation_suppressesSemanticsActionsInDisabledSubtree() {
        lateinit var androidComposeView: AndroidComposeView
        var enabled by mutableStateOf(false)

        var activeTranslated: AnnotatedString? = null
        var activeShowing: Boolean by mutableStateOf(false)
        var activeShowCalls = 0
        var activeClearCalls = 0

        var disabledTranslated: AnnotatedString? = null
        var disabledShowing: Boolean by mutableStateOf(false)
        var disabledShowCalls = 0
        var disabledClearCalls = 0

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                SimpleBox(sizePx = 200) {
                    SimpleBox(
                        sizePx = 50,
                        modifier =
                            Modifier.semantics {
                                testTag = "activeText"
                                text = AnnotatedString("Hello")
                                isShowingTextSubstitution = activeShowing
                                setTextSubstitution {
                                    activeTranslated = it
                                    true
                                }
                                showTextSubstitution {
                                    activeShowing = it
                                    activeShowCalls++
                                    true
                                }
                                clearTextSubstitution {
                                    activeShowing = false
                                    activeClearCalls++
                                    true
                                }
                            },
                    )
                    SimpleBox(
                        sizePx = 50,
                        modifier = Modifier.pxOffset(0, 60).enableUserInteraction(enabled),
                    ) {
                        SimpleBox(
                            sizePx = 50,
                            modifier =
                                Modifier.semantics {
                                    testTag = "disabledText"
                                    text = AnnotatedString("World")
                                    isShowingTextSubstitution = disabledShowing
                                    setTextSubstitution {
                                        disabledTranslated = it
                                        true
                                    }
                                    showTextSubstitution {
                                        disabledShowing = it
                                        disabledShowCalls++
                                        true
                                    }
                                    clearTextSubstitution {
                                        disabledShowing = false
                                        disabledClearCalls++
                                        true
                                    }
                                },
                        )
                    }
                }
            }
        }

        val activeId = rule.onNodeWithTag("activeText").fetchSemanticsNode().id
        val disabledId = rule.onNodeWithTag("disabledText").fetchSemanticsNode().id

        rule.runOnIdle {
            val requests = mutableListOf<ViewTranslationRequest?>()
            androidComposeView.onCreateVirtualViewTranslationRequests(
                longArrayOf(activeId.toLong(), disabledId.toLong()),
                intArrayOf(),
                requests::add,
            )
            assertThat(requests.filterNotNull())
                .containsExactly(
                    ViewTranslationRequest.Builder(androidComposeView.autofillId, activeId.toLong())
                        .setValue(
                            ID_TEXT,
                            TranslationRequestValue.forText(AnnotatedString("Hello")),
                        )
                        .build()
                )

            val responses =
                LongSparseArray<ViewTranslationResponse?>().apply {
                    append(
                        activeId.toLong(),
                        ViewTranslationResponse.Builder(androidComposeView.autofillId)
                            .setValue(
                                ID_TEXT,
                                TranslationResponseValue.Builder(0).setText("Bonjour").build(),
                            )
                            .build(),
                    )
                    append(
                        disabledId.toLong(),
                        ViewTranslationResponse.Builder(androidComposeView.autofillId)
                            .setValue(
                                ID_TEXT,
                                TranslationResponseValue.Builder(0).setText("Monde").build(),
                            )
                            .build(),
                    )
                }
            androidComposeView.onVirtualViewTranslationResponses(responses)
        }

        rule.runOnIdle {
            assertThat(activeTranslated).isEqualTo(AnnotatedString("Bonjour"))
            assertThat(disabledTranslated).isNull()

            androidComposeView.contentCaptureManager.onShowTranslation()
        }

        rule.runOnIdle {
            assertThat(activeShowCalls).isEqualTo(1)
            assertThat(activeShowing).isTrue()
            assertThat(disabledShowCalls).isEqualTo(0)
            assertThat(disabledShowing).isFalse()

            // Even if the disabled node previously held a translation, hide/clear must skip it.
            disabledShowing = true
        }

        rule.runOnIdle { androidComposeView.contentCaptureManager.onHideTranslation() }
        rule.runOnIdle {
            assertThat(activeShowCalls).isEqualTo(2)
            assertThat(activeShowing).isFalse()
            assertThat(disabledShowCalls).isEqualTo(0)
            assertThat(disabledShowing).isTrue()

            androidComposeView.contentCaptureManager.onClearTranslation()
        }

        rule.runOnIdle {
            assertThat(activeClearCalls).isEqualTo(1)
            assertThat(disabledClearCalls).isEqualTo(0)

            // Re-enable interaction and verify translation requests and actions resume.
            enabled = true
            disabledShowing = false
        }

        rule.runOnIdle {
            val requests = mutableListOf<ViewTranslationRequest?>()
            androidComposeView.onCreateVirtualViewTranslationRequests(
                longArrayOf(disabledId.toLong()),
                intArrayOf(),
                requests::add,
            )
            assertThat(requests.filterNotNull())
                .containsExactly(
                    ViewTranslationRequest.Builder(
                            androidComposeView.autofillId,
                            disabledId.toLong(),
                        )
                        .setValue(
                            ID_TEXT,
                            TranslationRequestValue.forText(AnnotatedString("World")),
                        )
                        .build()
                )

            androidComposeView.contentCaptureManager.onShowTranslation()
            assertThat(disabledShowCalls).isEqualTo(1)
            assertThat(disabledShowing).isTrue()
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun autofill_marksDisabledSubtreeViewStructureDisabled_andSkipsPerformAutofill() {
        lateinit var hostView: View
        var enabled by mutableStateOf(false)
        var activeFilled: String? = null
        var disabledFilled: String? = null
        var disabledLegacyFilled: String? = null

        rule.setContent {
            hostView = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                SimpleBox(sizePx = 200) {
                    SimpleBox(
                        sizePx = 50,
                        modifier =
                            Modifier.semantics {
                                testTag = "activeField"
                                contentDataType = ContentDataType.Text
                                onFillData {
                                    activeFilled = it.textValue?.toString()
                                    true
                                }
                            },
                    )
                    SimpleBox(
                        sizePx = 50,
                        modifier = Modifier.pxOffset(0, 60).enableUserInteraction(enabled),
                    ) {
                        SimpleBox(
                            sizePx = 50,
                            modifier =
                                Modifier.semantics {
                                    testTag = "disabledField"
                                    contentDataType = ContentDataType.Text
                                    onFillData {
                                        disabledFilled = it.textValue?.toString()
                                        true
                                    }
                                    onAutofillText {
                                        disabledLegacyFilled = it.text
                                        true
                                    }
                                },
                        )
                    }
                }
            }
        }

        val activeId = rule.onNodeWithTag("activeField").fetchSemanticsNode().id
        val disabledId = rule.onNodeWithTag("disabledField").fetchSemanticsNode().id

        rule.runOnIdle {
            val structure = RecordingViewStructure()
            hostView.onProvideAutofillVirtualStructure(structure, 0)
            val activeStruct = structure.findChildByVirtualId(activeId)
            val disabledStruct = structure.findChildByVirtualId(disabledId)
            assertThat(activeStruct).isNotNull()
            assertThat(disabledStruct).isNotNull()
            assertThat(activeStruct!!.isEnabled).isTrue()
            assertThat(disabledStruct!!.isEnabled).isFalse()

            val values =
                SparseArray<AutofillValue>().apply {
                    put(activeId, AutofillValue.forText("alice"))
                    put(disabledId, AutofillValue.forText("secret"))
                }
            hostView.autofill(values)

            assertThat(activeFilled).isEqualTo("alice")
            assertThat(disabledFilled).isNull()
            assertThat(disabledLegacyFilled).isNull()
        }

        rule.runOnIdle { enabled = true }
        rule.runOnIdle {
            val structure = RecordingViewStructure()
            hostView.onProvideAutofillVirtualStructure(structure, 0)
            assertThat(structure.findChildByVirtualId(disabledId)?.isEnabled).isTrue()

            val values =
                SparseArray<AutofillValue>().apply {
                    put(disabledId, AutofillValue.forText("secret"))
                }
            hostView.autofill(values)
            assertThat(disabledFilled).isEqualTo("secret")
            assertThat(disabledLegacyFilled).isEqualTo("secret")
        }
    }

    @OptIn(ExperimentalComposeUiHostApi::class)
    private fun androidx.collection.IntObjectMap<AdjustedSemanticsNode>.findByTag(
        tag: String
    ): AdjustedSemanticsNode? {
        var found: AdjustedSemanticsNode? = null
        forEachValue { node ->
            if (node.semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.TestTag) == tag) {
                found = node
            }
        }
        return found
    }
}

@Composable
private fun SimpleBox(
    sizePx: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Layout(content = content, modifier = modifier) { measurables, _ ->
        val childConstraints = Constraints()
        val placeables = measurables.map { it.measure(childConstraints) }
        layout(sizePx, sizePx) { placeables.forEach { it.place(0, 0) } }
    }
}

private fun Modifier.pxOffset(x: Int, y: Int): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.place(x, y) }
    }

private fun Modifier.pxPadding(paddingPx: Int): Modifier =
    this.layout { measurable, _ ->
        val placeable = measurable.measure(Constraints())
        layout(placeable.width + paddingPx * 2, placeable.height + paddingPx * 2) {
            placeable.place(paddingPx, paddingPx)
        }
    }

private fun Modifier.simpleClickable(
    onDown: () -> Unit = {},
    onCancel: () -> Unit = {},
    onClick: () -> Unit,
): Modifier = this then SimpleClickableElement(onDown, onCancel, onClick)

private class SimpleClickableElement(
    val onDown: () -> Unit,
    val onCancel: () -> Unit,
    val onClick: () -> Unit,
) : ModifierNodeElement<SimpleClickableNode>() {
    override fun create(): SimpleClickableNode = SimpleClickableNode(onDown, onCancel, onClick)

    override fun update(node: SimpleClickableNode) {
        node.onDown = onDown
        node.onCancel = onCancel
        node.onClick = onClick
    }

    override fun hashCode(): Int =
        31 * (31 * onDown.hashCode() + onCancel.hashCode()) + onClick.hashCode()

    override fun equals(other: Any?): Boolean =
        other is SimpleClickableElement &&
            other.onDown === onDown &&
            other.onCancel === onCancel &&
            other.onClick === onClick
}

private class SimpleClickableNode(
    var onDown: () -> Unit,
    var onCancel: () -> Unit,
    var onClick: () -> Unit,
) : Modifier.Node(), PointerInputModifierNode {
    private var pressed = false

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pass != PointerEventPass.Main) return
        val change = pointerEvent.changes.firstOrNull() ?: return
        if (!pressed && change.pressed) {
            pressed = true
            onDown()
            change.consume()
        } else if (pressed && !change.pressed) {
            pressed = false
            change.consume()
            onClick()
        }
    }

    override fun onCancelPointerInput() {
        pressed = false
        onCancel()
    }
}

private data class DelegatingSemanticsElement(val description: String) :
    ModifierNodeElement<DelegatingSemanticsWrapperNode>() {
    override fun create(): DelegatingSemanticsWrapperNode =
        DelegatingSemanticsWrapperNode(description)

    override fun update(node: DelegatingSemanticsWrapperNode) {
        node.description = description
    }
}

private class DelegatingSemanticsWrapperNode(var description: String) : DelegatingNode() {
    init {
        delegate(
            object : Modifier.Node(), SemanticsModifierNode {
                override fun SemanticsPropertyReceiver.applySemantics() {
                    stateDescription = description
                }
            }
        )
    }
}

private class RecordingViewStructure : ViewStructure() {
    @JvmField var virtualId: Int = 0
    @JvmField var isEnabled: Boolean = true
    val children = mutableListOf<RecordingViewStructure>()
    private val extrasBundle = Bundle()
    private var autofillIdValue: AutofillId? = null
    private var textValue: CharSequence = ""
    private var hintValue: CharSequence = ""

    fun findChildByVirtualId(id: Int): RecordingViewStructure? {
        if (virtualId == id) return this
        for (child in children) {
            child.findChildByVirtualId(id)?.let {
                return it
            }
        }
        return null
    }

    override fun setId(id: Int, packageName: String?, typeName: String?, entryName: String?) {
        virtualId = id
    }

    override fun setDimens(
        left: Int,
        top: Int,
        scrollX: Int,
        scrollY: Int,
        width: Int,
        height: Int,
    ) {}

    override fun setTransformation(matrix: Matrix?) {}

    override fun setElevation(elevation: Float) {}

    override fun setAlpha(alpha: Float) {}

    override fun setVisibility(visibility: Int) {}

    override fun setEnabled(state: Boolean) {
        isEnabled = state
    }

    override fun setClickable(state: Boolean) {}

    override fun setLongClickable(state: Boolean) {}

    override fun setContextClickable(state: Boolean) {}

    override fun setFocusable(state: Boolean) {}

    override fun setFocused(state: Boolean) {}

    override fun setAccessibilityFocused(state: Boolean) {}

    override fun setCheckable(state: Boolean) {}

    override fun setChecked(state: Boolean) {}

    override fun setSelected(state: Boolean) {}

    override fun setActivated(state: Boolean) {}

    override fun setOpaque(opaque: Boolean) {}

    override fun setClassName(className: String?) {}

    override fun setContentDescription(contentDescription: CharSequence?) {}

    override fun setText(text: CharSequence?) {
        textValue = text ?: ""
    }

    override fun setText(text: CharSequence?, selectionStart: Int, selectionEnd: Int) {
        textValue = text ?: ""
    }

    override fun setTextStyle(size: Float, fgColor: Int, bgColor: Int, style: Int) {}

    override fun setTextLines(charOffsets: IntArray?, baselines: IntArray?) {}

    override fun setHint(hint: CharSequence?) {
        hintValue = hint ?: ""
    }

    override fun getText(): CharSequence = textValue

    override fun getTextSelectionStart(): Int = 0

    override fun getTextSelectionEnd(): Int = 0

    override fun getHint(): CharSequence = hintValue

    override fun getExtras(): Bundle = extrasBundle

    override fun hasExtras(): Boolean = !extrasBundle.isEmpty

    override fun setChildCount(num: Int) {
        children.clear()
        repeat(num) { children.add(RecordingViewStructure()) }
    }

    override fun addChildCount(num: Int): Int {
        val start = children.size
        repeat(num) { children.add(RecordingViewStructure()) }
        return start
    }

    override fun getChildCount(): Int = children.size

    override fun newChild(index: Int): ViewStructure = children[index]

    override fun asyncNewChild(index: Int): ViewStructure = children[index]

    override fun getAutofillId(): AutofillId? = autofillIdValue

    override fun setAutofillId(id: AutofillId) {
        autofillIdValue = id
    }

    override fun setAutofillId(rootId: AutofillId, virtualId: Int) {
        autofillIdValue = rootId
        this.virtualId = virtualId
    }

    override fun setAutofillType(type: Int) {}

    override fun setAutofillHints(hint: Array<out String>?) {}

    override fun setAutofillValue(value: AutofillValue?) {}

    override fun setAutofillOptions(options: Array<out CharSequence>?) {}

    override fun setDataIsSensitive(sensitive: Boolean) {}

    override fun asyncCommit() {}

    override fun setWebDomain(domain: String?) {}

    override fun setLocaleList(localeList: LocaleList?) {}

    override fun newHtmlInfoBuilder(tagName: String): HtmlInfo.Builder {
        throw UnsupportedOperationException()
    }

    override fun setHtmlInfo(htmlInfo: HtmlInfo) {}

    override fun setInputType(inputType: Int) {}
}
