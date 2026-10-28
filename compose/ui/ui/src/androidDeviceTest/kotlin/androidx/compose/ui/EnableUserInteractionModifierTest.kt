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

import android.os.Build
import android.util.SparseArray
import android.view.View
import android.view.autofill.AutofillValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.autofill.FakeViewStructure
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
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
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getAllUncoveredSemanticsNodesToIntObjectMap
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.onFillData
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.semanticsConfiguration
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class EnableUserInteractionModifierTest {
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
    fun pointerInput_disabledBlocksTouchesToSubtree() {
        var childClicked = false
        rule.setContent {
            Box(Modifier.size(200.dp)) {
                Box(Modifier.size(200.dp).enableUserInteraction(false)) {
                    Box(Modifier.testTag("child").size(50.dp).clickable { childClicked = true })
                }
            }
        }

        rule.onNodeWithTag("child").performTouchInput { click(Offset(25f, 25f)) }

        rule.runOnIdle { assertThat(childClicked).isFalse() }
    }

    @Test
    fun pointerInput_disabledOnSameNodeBlocksTouchesRegardlessOfOrder() {
        var clickedBefore = false
        var clickedAfter = false
        rule.setContent {
            Box(Modifier.size(200.dp)) {
                Box(
                    Modifier.testTag("clickableBefore")
                        .size(50.dp)
                        .clickable { clickedBefore = true }
                        .enableUserInteraction(false)
                )
                Box(
                    Modifier.offset(100.dp, 0.dp)
                        .testTag("clickableAfter")
                        .size(50.dp)
                        .enableUserInteraction(false)
                        .clickable { clickedAfter = true }
                )
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
    fun pointerInput_disabledPassesThroughToSiblingsBehind() {
        var backgroundClicked = false
        var foregroundClicked = false
        rule.setContent {
            Box(Modifier.size(200.dp)) {
                Box(
                    Modifier.testTag("background").size(100.dp).clickable {
                        backgroundClicked = true
                    }
                )
                Box(Modifier.testTag("foreground").size(200.dp).enableUserInteraction(false)) {
                    Box(Modifier.size(100.dp).clickable { foregroundClicked = true })
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
    fun pointerInput_dynamicToggle_enablesAndDisablesTouches() {
        var clickCount = 0
        var enabled by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.size(200.dp).enableUserInteraction(enabled)) {
                Box(Modifier.testTag("child").size(50.dp).clickable { clickCount++ })
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
    fun pointerInput_childEnabledTrue_cannotOverrideAncestorDisabled() {
        var childClicked = false
        rule.setContent {
            Box(Modifier.size(200.dp).enableUserInteraction(false)) {
                Box(Modifier.size(100.dp).enableUserInteraction(true)) {
                    Box(Modifier.testTag("child").size(50.dp).clickable { childClicked = true })
                }
            }
        }

        rule.onNodeWithTag("child").performTouchInput { click(Offset(25f, 25f)) }

        rule.runOnIdle { assertThat(childClicked).isFalse() }
    }

    @Test
    fun pointerInput_disabledMidGesture_cancelsActivePointerAndPreventsClick() {
        var clicked = false
        var downObserved = false
        var cancelObserved = false
        var enabled by mutableStateOf(true)

        rule.setContent {
            Box(Modifier.size(200.dp).enableUserInteraction(enabled)) {
                Box(
                    Modifier.testTag("child")
                        .size(100.dp)
                        .then(
                            CancelTrackingPointerElement(
                                onDown = { downObserved = true },
                                onCancel = { cancelObserved = true },
                            )
                        )
                        .clickable { clicked = true }
                )
            }
        }

        rule.onNodeWithTag("child").performTouchInput { down(Offset(50f, 50f)) }
        rule.runOnIdle {
            assertThat(downObserved).isTrue()
            assertThat(cancelObserved).isFalse()
        }

        // Disable user interaction while pointer is held down.
        rule.runOnIdle { enabled = false }
        rule.runOnIdle { assertThat(cancelObserved).isTrue() }

        // Release finger; click must not fire.
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
            Box(
                Modifier.size(200.dp)
                    .then(if (disable) Modifier.enableUserInteraction(false) else Modifier)
            ) {
                Box(
                    Modifier.testTag("child")
                        .size(100.dp)
                        .then(
                            CancelTrackingPointerElement(
                                onDown = { downObserved = true },
                                onCancel = { cancelObserved = true },
                            )
                        )
                        .clickable { clicked = true }
                )
            }
        }

        rule.onNodeWithTag("child").performTouchInput { down(Offset(50f, 50f)) }
        rule.runOnIdle {
            assertThat(downObserved).isTrue()
            assertThat(cancelObserved).isFalse()
        }

        // Conditionally attach enableUserInteraction(false) mid-gesture (runs onAttach, not
        // update).
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
                Box(
                    Modifier.testTag("target")
                        .size(100.dp)
                        .then(DelegatingSemanticsElement("delegated"))
                        .enableUserInteraction(false)
                        .clickable { clicked = true }
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
    fun uncoveredSemanticsNodes_disabledSubtreePruned_andRestoredWhenReEnabled() {
        lateinit var semanticsOwner: SemanticsOwner
        var enabled by mutableStateOf(false)

        rule.setContent {
            semanticsOwner = (LocalView.current as RootForTest).semanticsOwner
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                Box(Modifier.size(200.dp)) {
                    // Sibling behind at non-overlapping or overlapping bounds
                    Box(
                        Modifier.size(100.dp).semantics {
                            testTag = "sibling"
                            contentDescription = "sibling"
                        }
                    )

                    // Container covers 200x200, child covers 50x50 at (120, 0)
                    Box(Modifier.size(200.dp).enableUserInteraction(enabled)) {
                        Box(
                            Modifier.offset(120.dp, 0.dp).size(50.dp).semantics {
                                testTag = "child"
                                contentDescription = "child"
                            }
                        )
                    }
                }
            }
        }

        // While disabled: child is pruned from uncovered semantics, sibling is present,
        // and testTag lookup in the unpruned semantics hierarchy still finds child.
        rule.runOnIdle {
            val uncovered = semanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap(0) { false }
            assertThat(uncovered.findByTag("sibling")).isNotNull()
            assertThat(uncovered.findByTag("child")).isNull()
        }
        rule.onNodeWithTag("child").assertExists()

        // Re-enable: child returns to uncovered semantics, and the enabled 200x200 wrapper
        // itself does not occlude the 100x100 sibling behind it.
        rule.runOnIdle { enabled = true }
        rule.runOnIdle {
            val uncovered = semanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap(0) { false }
            assertThat(uncovered.findByTag("sibling")).isNotNull()
            assertThat(uncovered.findByTag("child")).isNotNull()
        }
    }

    @Test
    fun uncoveredSemanticsNodes_childEnabledTrue_cannotOverrideAncestorDisabled() {
        lateinit var semanticsOwner: SemanticsOwner

        rule.setContent {
            semanticsOwner = (LocalView.current as RootForTest).semanticsOwner
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                Box(Modifier.size(200.dp).enableUserInteraction(false)) {
                    Box(Modifier.size(200.dp).enableUserInteraction(true)) {
                        Box(
                            Modifier.size(50.dp).semantics {
                                testTag = "nestedChild"
                                contentDescription = "nestedChild"
                            }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            val uncovered = semanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap(0) { false }
            assertThat(uncovered.findByTag("nestedChild")).isNull()
        }
        rule.onNodeWithTag("nestedChild").assertExists()
    }

    @OptIn(ExperimentalComposeUiHostApi::class)
    @Test
    fun semanticsHitTest_skipsDisabledSubtreeAndHitsSiblingBehind() {
        lateinit var semanticsOwner: SemanticsOwner

        rule.setContent {
            semanticsOwner = (LocalView.current as RootForTest).semanticsOwner
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                Box(Modifier.size(200.dp)) {
                    Box(
                        Modifier.size(100.dp).semantics {
                            testTag = "sibling"
                            contentDescription = "sibling"
                        }
                    )
                    Box(Modifier.size(100.dp).enableUserInteraction(false)) {
                        Box(
                            Modifier.size(100.dp).semantics {
                                testTag = "disabledChild"
                                contentDescription = "disabledChild"
                            }
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun contentCaptureAndAutofill_reportDisabledSubtreeWhileSuppressingAutofillWrite() {
        lateinit var hostView: View
        lateinit var androidComposeView: AndroidComposeView
        var enabled by mutableStateOf(false)
        var filledValue: String? = null

        rule.setContent {
            hostView = LocalView.current
            androidComposeView = hostView as AndroidComposeView
            Box(Modifier.size(200.dp).enableUserInteraction(enabled)) {
                Box(
                    Modifier.size(50.dp).semantics {
                        testTag = "disabledField"
                        contentDescription = "disabledField"
                        contentDataType = ContentDataType.Text
                        onFillData {
                            filledValue = it.textValue?.toString()
                            true
                        }
                    }
                )
            }
        }

        val fieldId = rule.onNodeWithTag("disabledField").semanticsId()

        rule.runOnIdle {
            // Content capture still reports visible uncovered nodes in disabled subtrees.
            assertThat(
                    androidComposeView.contentCaptureManager.currentSemanticsNodes.findByTag(
                        "disabledField"
                    )
                )
                .isNotNull()

            // Autofill ViewStructure marks the node as disabled and performAutofill skips it.
            val structure = FakeViewStructure()
            hostView.onProvideAutofillVirtualStructure(structure, 0)
            val childStructure = structure.children.firstOrNull { it.virtualId == fieldId }
            assertThat(childStructure).isNotNull()
            assertThat(childStructure!!.isEnabled).isFalse()

            val values =
                SparseArray<AutofillValue>().apply {
                    put(fieldId, AutofillValue.forText("secret"))
                }
            hostView.autofill(values)
            assertThat(filledValue).isNull()
        }

        rule.runOnIdle { enabled = true }
        rule.runOnIdle {
            val structure = FakeViewStructure()
            hostView.onProvideAutofillVirtualStructure(structure, 0)
            val childStructure = structure.children.firstOrNull { it.virtualId == fieldId }
            assertThat(childStructure?.isEnabled).isTrue()

            val values =
                SparseArray<AutofillValue>().apply {
                    put(fieldId, AutofillValue.forText("secret"))
                }
            hostView.autofill(values)
            assertThat(filledValue).isEqualTo("secret")
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

    private class CancelTrackingPointerElement(
        val onDown: () -> Unit,
        val onCancel: () -> Unit,
    ) : ModifierNodeElement<CancelTrackingPointerNode>() {
        override fun create(): CancelTrackingPointerNode =
            CancelTrackingPointerNode(onDown, onCancel)

        override fun update(node: CancelTrackingPointerNode) {
            node.onDown = onDown
            node.onCancel = onCancel
        }

        override fun hashCode(): Int = 31 * onDown.hashCode() + onCancel.hashCode()

        override fun equals(other: Any?): Boolean =
            other is CancelTrackingPointerElement &&
                other.onDown === onDown &&
                other.onCancel === onCancel
    }

    private class CancelTrackingPointerNode(
        var onDown: () -> Unit,
        var onCancel: () -> Unit,
    ) : Modifier.Node(), PointerInputModifierNode {
        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize,
        ) {
            if (pass == PointerEventPass.Main && pointerEvent.changes.any { it.pressed }) {
                onDown()
            }
        }

        override fun onCancelPointerInput() {
            onCancel()
        }
    }
}
