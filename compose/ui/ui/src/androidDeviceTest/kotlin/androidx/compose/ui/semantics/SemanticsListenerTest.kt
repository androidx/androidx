/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.semantics

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.isExactly
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.Rule

@MediumTest
class SemanticsListenerTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var semanticsOwner: SemanticsOwner

    // Initial layout does not trigger listeners. Users have to detect the initial semantics
    //  values by detecting first layout (You can get the bounds from RectManager.RectList).
    @Test
    fun initialComposition_doesNotTriggerListeners() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            Text(text = "text")
        }

        // Assert.
        rule.runOnIdle { assertThat(events).isEmpty() }
    }

    @Test
    fun addingNonSemanticsModifier() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        var addModifier by mutableStateOf(false)
        val text = AnnotatedString("text")
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            Box(
                modifier =
                    Modifier.then(if (addModifier) Modifier.size(1000.dp) else Modifier)
                        .semantics { this.text = text }
                        .testTag("item")
            )
        }

        // Act.
        rule.runOnIdle { addModifier = true }

        // Assert.
        rule.runOnIdle { assertThat(events).isEmpty() }
    }

    @Test
    fun removingNonSemanticsModifier() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        var removeModifier by mutableStateOf(false)
        val text = AnnotatedString("text")
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            Box(
                modifier =
                    Modifier.then(if (removeModifier) Modifier else Modifier.size(1000.dp))
                        .semantics { this.text = text }
                        .testTag("item")
            )
        }

        // Act.
        rule.runOnIdle { removeModifier = true }

        // Assert.
        rule.runOnIdle { assertThat(events).isEmpty() }
    }

    @Test
    fun addingSemanticsModifier() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        var addModifier by mutableStateOf(false)
        val text = AnnotatedString("text")
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            Box(
                modifier =
                    Modifier.size(100.dp)
                        .then(
                            if (addModifier) Modifier.semantics { this.text = text } else Modifier
                        )
                        .testTag("item")
            )
        }

        // Act.
        rule.runOnIdle { addModifier = true }

        // Assert.
        val semanticsId = rule.onNodeWithTag("item").semanticsId()
        rule.runOnIdle {
            assertThat(events)
                .isExactly(Event(semanticsId, prevSemantics = null, newSemantics = "text"))
        }
    }

    @Test
    fun removingSemanticsModifier() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        var removeModifier by mutableStateOf(false)
        val text = AnnotatedString("text")
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            Box(
                modifier =
                    Modifier.size(1000.dp)
                        .then(
                            if (removeModifier) Modifier
                            else Modifier.semantics { this.text = text }
                        )
                        .testTag("item")
            )
        }

        // Act.
        rule.runOnIdle { removeModifier = true }

        // Assert.
        val semanticsId = rule.onNodeWithTag("item").semanticsId()
        rule.runOnIdle {
            assertThat(events)
                .isExactly(Event(semanticsId, prevSemantics = "text", newSemantics = null))
        }
    }

    @Test
    fun changingMutableSemanticsProperty() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        var text by mutableStateOf(AnnotatedString("text1"))
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            Box(modifier = Modifier.semantics { this.text = text }.testTag("item"))
        }

        // Act.
        rule.runOnIdle { text = AnnotatedString("text2") }

        // Assert.
        val semanticsId = rule.onNodeWithTag("item").semanticsId()
        rule.runOnIdle {
            assertThat(events)
                .isExactly(Event(semanticsId, prevSemantics = "text1", newSemantics = "text2"))
        }
    }

    @Test
    fun changingMutableSemanticsProperty_alongWithRecomposition() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        var text by mutableStateOf(AnnotatedString("text1"))
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            Box(
                modifier =
                    Modifier.border(2.dp, if (text.text == "text1") Red else Black)
                        .semantics { this.text = text }
                        .testTag("item")
            )
        }

        // Act.
        rule.runOnIdle { text = AnnotatedString("text2") }

        // Assert.
        val semanticsId = rule.onNodeWithTag("item").semanticsId()
        rule.runOnIdle {
            assertThat(events)
                .isExactly(Event(semanticsId, prevSemantics = "text1", newSemantics = "text2"))
        }
    }

    @Test
    fun changingSemanticsProperty_andCallingInvalidateSemantics() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        val modifierNode =
            object : SemanticsModifierNode, Modifier.Node() {
                override fun SemanticsPropertyReceiver.applySemantics() {}
            }
        var text = AnnotatedString("text1")
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            Box(
                modifier =
                    Modifier.elementFor(modifierNode).semantics { this.text = text }.testTag("item")
            )
        }

        // Act.
        rule.runOnIdle {
            text = AnnotatedString("text2")
            modifierNode.invalidateSemantics()
        }

        // Assert.
        val semanticsId = rule.onNodeWithTag("item").semanticsId()
        rule.runOnIdle {
            assertThat(events)
                .isExactly(Event(semanticsId, prevSemantics = "text1", newSemantics = "text2"))
        }
    }

    @Test
    fun textChange() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        var text by mutableStateOf("text1")
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            Text(text = text, modifier = Modifier.testTag("item"))
        }

        // Act.
        rule.runOnIdle { text = "text2" }

        // Assert.
        val semanticsId = rule.onNodeWithTag("item").semanticsId()
        rule.runOnIdle {
            assertThat(events)
                .isExactly(Event(semanticsId, prevSemantics = "text1", newSemantics = "text2"))
        }
    }

    @Test
    fun multipleTextChanges() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        var text by mutableStateOf("text1")
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            Text(text = text, modifier = Modifier.testTag("item"))
        }

        // Act.
        rule.runOnIdle { text = "text2" }
        rule.runOnIdle { text = "text3" }

        // Assert.
        val semanticsId = rule.onNodeWithTag("item").semanticsId()
        rule.runOnIdle {
            assertThat(events)
                .isExactly(
                    Event(semanticsId, prevSemantics = "text1", newSemantics = "text2"),
                    Event(semanticsId, prevSemantics = "text2", newSemantics = "text3"),
                )
        }
    }

    @Test
    fun EditTextChange() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        val textFieldState = TextFieldState("text1")
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(
                    Event(
                        info.semanticsId,
                        prev?.EditableText,
                        info.semanticsConfiguration?.EditableText,
                    )
                )
            }
        ) {
            TextField(state = textFieldState, modifier = Modifier.testTag("item"))
        }

        // Act.
        rule.runOnIdle { textFieldState.setTextAndPlaceCursorAtEnd("text2") }

        // Assert.
        val semanticsId = rule.onNodeWithTag("item").semanticsId()
        rule.runOnIdle {
            assertThat(events)
                .isExactly(Event(semanticsId, prevSemantics = "text1", newSemantics = "text2"))
        }
    }

    @Test
    fun FocusChange_withNoRecomposition() {
        // Arrange.
        val events = mutableListOf<Event<Boolean>>()
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(
                    Event(
                        info.semanticsId,
                        prev?.getOrNull(SemanticsProperties.Focused),
                        info.semanticsConfiguration?.getOrNull(SemanticsProperties.Focused),
                    )
                )
            }
        ) {
            Column {
                Box(Modifier.testTag("item1").size(100.dp).focusable())
                Box(Modifier.testTag("item2").size(100.dp).focusable())
            }
        }
        rule.onNodeWithTag("item1").requestFocus()
        rule.runOnIdle { events.clear() }

        // Act.
        rule.onNodeWithTag("item2").requestFocus()

        // Assert.
        val item1 = rule.onNodeWithTag("item1").semanticsId()
        val item2 = rule.onNodeWithTag("item2").semanticsId()
        rule.runOnIdle {
            assertThat(events)
                .isExactly(
                    Event(item1, prevSemantics = true, newSemantics = false),
                    Event(item2, prevSemantics = false, newSemantics = true),
                )
        }
    }

    @Test
    fun FocusChange_thatCausesRecomposition() {
        // Arrange.
        val events = mutableListOf<Event<Boolean>>()
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(
                    Event(
                        info.semanticsId,
                        prev?.getOrNull(SemanticsProperties.Focused),
                        info.semanticsConfiguration?.getOrNull(SemanticsProperties.Focused),
                    )
                )
            }
        ) {
            Column {
                FocusableBox(Modifier.testTag("item1"))
                FocusableBox(Modifier.testTag("item2"))
            }
        }
        rule.onNodeWithTag("item1").requestFocus()
        rule.runOnIdle { events.clear() }

        // Act.
        rule.onNodeWithTag("item2").requestFocus()

        // Assert.
        val item1 = rule.onNodeWithTag("item1").semanticsId()
        val item2 = rule.onNodeWithTag("item2").semanticsId()
        rule.runOnIdle {
            assertThat(events)
                .isExactly(
                    Event(item1, prevSemantics = true, newSemantics = false),
                    Event(item2, prevSemantics = false, newSemantics = true),
                )
        }
    }

    /**
     * Checks that removing a node with semantics from composition notifies listeners with the old
     * semantics and `null` for the new semantics.
     */
    @Test
    fun removingNodeFromComposition_notifiesWithNullNewSemantics() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        var show by mutableStateOf(true)
        val text = AnnotatedString("text")
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            if (show) {
                Box(modifier = Modifier.semantics { this.text = text }.testTag("item"))
            }
        }
        val semanticsId = rule.onNodeWithTag("item").semanticsId()

        // Act.
        rule.runOnIdle { show = false }

        // Assert.
        rule.runOnIdle {
            assertThat(events)
                .isExactly(Event(semanticsId, prevSemantics = "text", newSemantics = null))
        }
    }

    /**
     * Checks that removing a plain node without any `semantics` modifier does not trigger semantics
     * listener notifications.
     */
    @Test
    fun removingNodeWithoutSemantics_doesNotNotifyListeners() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        var show by mutableStateOf(true)
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            if (show) {
                Box(modifier = Modifier.size(100.dp))
            }
        }

        // Act.
        rule.runOnIdle { show = false }

        // Assert.
        rule.runOnIdle { assertThat(events).isEmpty() }
    }

    /**
     * Checks that adding and removing `Modifier.clearAndSetSemantics` on an existing node notifies
     * listeners with the new and old semantics.
     */
    @Test
    fun addingAndRemovingClearAndSetSemantics() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        var clear by mutableStateOf(false)
        val text = AnnotatedString("cleared")
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            Box(
                modifier =
                    Modifier.size(100.dp)
                        .then(
                            if (clear) Modifier.clearAndSetSemantics { this.text = text }
                            else Modifier
                        )
                        .testTag("item")
            )
        }
        val semanticsId = rule.onNodeWithTag("item").semanticsId()

        // Act & Assert: add clearAndSetSemantics.
        rule.runOnIdle { clear = true }
        rule.runOnIdle {
            assertThat(events)
                .isExactly(Event(semanticsId, prevSemantics = null, newSemantics = "cleared"))
            events.clear()
        }

        // Act & Assert: remove clearAndSetSemantics.
        rule.runOnIdle { clear = false }
        rule.runOnIdle {
            assertThat(events)
                .isExactly(Event(semanticsId, prevSemantics = "cleared", newSemantics = null))
        }
    }

    /**
     * Checks that removing one of two `semantics` modifiers on a node notifies listeners, and
     * removing the remaining one notifies listeners again.
     */
    @Test
    fun addingAndRemovingSecondSemanticsModifier() {
        // Arrange.
        val events = mutableListOf<Event<Pair<String?, String?>>>()
        var hasSecond by mutableStateOf(true)
        var hasFirst by mutableStateOf(true)
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(
                    Event(
                        info.semanticsId,
                        Pair(prev?.Text, prev?.ContentDescription),
                        Pair(
                            info.semanticsConfiguration?.Text,
                            info.semanticsConfiguration?.ContentDescription,
                        ),
                    )
                )
            }
        ) {
            Box(
                modifier =
                    Modifier.size(100.dp)
                        .then(
                            if (hasFirst) Modifier.semantics { contentDescription = "first" }
                            else Modifier
                        )
                        .then(
                            if (hasSecond) Modifier.semantics { text = AnnotatedString("second") }
                            else Modifier
                        )
                        .testTag("item")
            )
        }
        val semanticsId = rule.onNodeWithTag("item").semanticsId()

        // Act & Assert: remove second modifier while first remains.
        rule.runOnIdle { hasSecond = false }
        rule.runOnIdle {
            assertThat(events)
                .isExactly(
                    Event(
                        semanticsId,
                        prevSemantics = Pair("second", "first"),
                        newSemantics = Pair(null, "first"),
                    )
                )
            events.clear()
        }

        // Act & Assert: remove last remaining semantics modifier.
        rule.runOnIdle { hasFirst = false }
        rule.runOnIdle {
            assertThat(events)
                .isExactly(
                    Event(
                        semanticsId,
                        prevSemantics = Pair(null, "first"),
                        newSemantics = Pair(null, null),
                    )
                )
        }
    }

    /**
     * Checks that calling `invalidateSemantics()` inside `applySemantics()` does not cause an
     * infinite loop or duplicate events, and subsequent invalidations still notify normally.
     */
    @Test
    fun invalidateSemanticsDuringApplySemantics_isIgnored() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        var value = "text1"
        val modifierNode =
            object : SemanticsModifierNode, Modifier.Node() {
                override fun SemanticsPropertyReceiver.applySemantics() {
                    text = AnnotatedString(value)
                    invalidateSemantics()
                }
            }
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            Box(modifier = Modifier.elementFor(modifierNode).testTag("item"))
        }
        val semanticsId = rule.onNodeWithTag("item").semanticsId()

        // Act.
        rule.runOnIdle {
            value = "text2"
            modifierNode.invalidateSemantics()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(events)
                .isExactly(Event(semanticsId, prevSemantics = "text1", newSemantics = "text2"))
        }
    }

    /**
     * Checks that attaching and detaching a deprecated `SemanticsModifier` still notifies semantics
     * listeners.
     */
    @Test
    fun legacySemanticsModifier_attachAndDetach_notifiesListeners() {
        // Arrange.
        val events = mutableListOf<Int>()
        var attachLegacy by mutableStateOf(false)
        val legacyModifier =
            @Suppress("DEPRECATION")
            object : SemanticsModifier {
                override val semanticsConfiguration: SemanticsConfiguration =
                    SemanticsConfiguration().apply { text = AnnotatedString("legacy") }
            }
        rule.setTestContent(onSemanticsChange = { info, _ -> events.add(info.semanticsId) }) {
            Box(
                modifier =
                    Modifier.size(100.dp)
                        .testTag("item")
                        .then(if (attachLegacy) legacyModifier else Modifier)
            )
        }
        val semanticsId = rule.onNodeWithTag("item").semanticsId()

        // Act & Assert: attach legacy SemanticsModifier.
        rule.runOnIdle { attachLegacy = true }
        rule.runOnIdle {
            assertThat(events).contains(semanticsId)
            events.clear()
        }

        // Act & Assert: detach legacy SemanticsModifier.
        rule.runOnIdle { attachLegacy = false }
        rule.runOnIdle { assertThat(events).contains(semanticsId) }
    }

    /**
     * Checks that after a node is deactivated into a `ReusableContentHost` reuse pool and
     * reactivated, subsequent semantics changes on that node still notify listeners.
     */
    @Test
    fun reusableContentHost_deactivateAndReuse_notifiesListeners() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        var active by mutableStateOf(true)
        var text by mutableStateOf("text1")
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(Event(info.semanticsId, prev?.Text, info.semanticsConfiguration?.Text))
            }
        ) {
            ReusableContentHost(active = active) {
                Box(
                    modifier =
                        Modifier.size(100.dp)
                            .semantics { this.text = AnnotatedString(text) }
                            .testTag("item")
                )
            }
        }
        rule.waitForIdle()

        // Act & Assert: deactivate into reuse pool, then reactivate and update semantics.
        rule.runOnIdle { active = false }
        rule.runOnIdle {
            assertThat(events).isEmpty()
            active = true
        }
        rule.runOnIdle {
            val reactivatedId = rule.onNodeWithTag("item").semanticsId()
            assertThat(events)
                .contains(Event(reactivatedId, prevSemantics = null, newSemantics = "text1"))
            events.clear()
            text = "text2"
        }

        rule.runOnIdle {
            val reactivatedId = rule.onNodeWithTag("item").semanticsId()
            assertThat(events).isExactly(Event(reactivatedId, "text1", "text2"))
        }
    }

    private val SemanticsConfiguration.Text
        get() = getOrNull(SemanticsProperties.Text)?.fastJoinToString()

    private val SemanticsConfiguration.ContentDescription
        get() = getOrNull(SemanticsProperties.ContentDescription)?.fastJoinToString()

    private val SemanticsConfiguration.EditableText
        get() = getOrNull(SemanticsProperties.EditableText)?.toString()

    private fun ComposeContentTestRule.setTestContent(
        onSemanticsChange: (SemanticsInfo, SemanticsConfiguration?) -> Unit,
        composable: @Composable () -> Unit,
    ) {
        val semanticsListener =
            object : SemanticsListener {
                override fun onSemanticsChanged(
                    semanticsInfo: SemanticsInfo,
                    previousSemanticsConfiguration: SemanticsConfiguration?,
                ) {
                    onSemanticsChange(semanticsInfo, previousSemanticsConfiguration)
                }
            }
        setContent {
            semanticsOwner = (LocalView.current as RootForTest).semanticsOwner
            DisposableEffect(semanticsOwner) {
                semanticsOwner.listeners.add(semanticsListener)
                onDispose { semanticsOwner.listeners.remove(semanticsListener) }
            }
            composable()
        }
    }

    data class Event<T>(val semanticsId: Int, val prevSemantics: T?, val newSemantics: T?)

    @Composable
    private fun FocusableBox(
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit = {},
    ) {
        var borderColor by remember { mutableStateOf(Black) }
        Box(
            modifier =
                modifier
                    .size(100.dp)
                    .onFocusChanged { borderColor = if (it.isFocused) Red else Black }
                    .border(2.dp, borderColor)
                    .focusable(),
            content = content,
        )
    }
}
