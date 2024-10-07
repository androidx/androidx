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
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class SemanticsListenerTest(private val isSemanticAutofillEnabled: Boolean) {

    @get:Rule val rule = createComposeRule()

    private lateinit var semanticsOwner: SemanticsOwner

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "isSemanticAutofillEnabled = {0}")
        fun initParameters() = listOf(false, true)
    }

    @Before
    fun setup() {
        @OptIn(ExperimentalComposeUiApi::class)
        ComposeUiFlags.isSemanticAutofillEnabled = isSemanticAutofillEnabled
    }

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
        val semanticsId = rule.onNodeWithTag("item").semanticsId
        rule.runOnIdle {
            if (isSemanticAutofillEnabled) {
                assertThat(events)
                    .isExactly(Event(semanticsId, prevSemantics = null, newSemantics = "text"))
            } else {
                assertThat(events).isEmpty()
            }
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
        val semanticsId = rule.onNodeWithTag("item").semanticsId
        rule.runOnIdle {
            if (isSemanticAutofillEnabled) {
                assertThat(events)
                    .isExactly(Event(semanticsId, prevSemantics = "text", newSemantics = null))
            } else {
                assertThat(events).isEmpty()
            }
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
        val semanticsId = rule.onNodeWithTag("item").semanticsId
        rule.runOnIdle {
            if (isSemanticAutofillEnabled) {
                assertThat(events)
                    .isExactly(Event(semanticsId, prevSemantics = "text1", newSemantics = "text2"))
            } else {
                assertThat(events).isEmpty()
            }
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
        val semanticsId = rule.onNodeWithTag("item").semanticsId
        rule.runOnIdle {
            if (isSemanticAutofillEnabled) {
                assertThat(events)
                    .isExactly(Event(semanticsId, prevSemantics = "text1", newSemantics = "text2"))
            } else {
                assertThat(events).isEmpty()
            }
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
        val semanticsId = rule.onNodeWithTag("item").semanticsId
        rule.runOnIdle {
            if (isSemanticAutofillEnabled) {
                assertThat(events)
                    .isExactly(Event(semanticsId, prevSemantics = "text1", newSemantics = "text2"))
            } else {
                assertThat(events).isEmpty()
            }
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
        val semanticsId = rule.onNodeWithTag("item").semanticsId
        rule.runOnIdle {
            if (isSemanticAutofillEnabled) {
                assertThat(events)
                    .isExactly(Event(semanticsId, prevSemantics = "text1", newSemantics = "text2"))
            } else {
                assertThat(events).isEmpty()
            }
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
        val semanticsId = rule.onNodeWithTag("item").semanticsId
        rule.runOnIdle {
            if (isSemanticAutofillEnabled) {
                assertThat(events)
                    .isExactly(
                        Event(semanticsId, prevSemantics = "text1", newSemantics = "text2"),
                        Event(semanticsId, prevSemantics = "text2", newSemantics = "text3")
                    )
            } else {
                assertThat(events).isEmpty()
            }
        }
    }

    @Test
    fun EditTextChange() {
        // Arrange.
        val events = mutableListOf<Event<String>>()
        var text by mutableStateOf("text1")
        rule.setTestContent(
            onSemanticsChange = { info, prev ->
                events.add(
                    Event(
                        info.semanticsId,
                        prev?.EditableText,
                        info.semanticsConfiguration?.EditableText
                    )
                )
            }
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.testTag("item")
            )
        }

        // Act.
        rule.runOnIdle { text = "text2" }

        // Assert.
        val semanticsId = rule.onNodeWithTag("item").semanticsId
        rule.runOnIdle {
            if (isSemanticAutofillEnabled) {
                assertThat(events)
                    .isExactly(Event(semanticsId, prevSemantics = "text1", newSemantics = "text2"))
            } else {
                assertThat(events).isEmpty()
            }
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
                        info.semanticsConfiguration?.getOrNull(SemanticsProperties.Focused)
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
        val item1 = rule.onNodeWithTag("item1").semanticsId
        val item2 = rule.onNodeWithTag("item2").semanticsId
        rule.runOnIdle {
            if (isSemanticAutofillEnabled) {
                assertThat(events)
                    .isExactly(
                        Event(item1, prevSemantics = true, newSemantics = false),
                        Event(item2, prevSemantics = false, newSemantics = true)
                    )
            } else {
                assertThat(events).isEmpty()
            }
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
                        info.semanticsConfiguration?.getOrNull(SemanticsProperties.Focused)
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
        val item1 = rule.onNodeWithTag("item1").semanticsId
        val item2 = rule.onNodeWithTag("item2").semanticsId
        rule.runOnIdle {
            if (isSemanticAutofillEnabled) {
                assertThat(events)
                    .isExactly(
                        Event(item1, prevSemantics = true, newSemantics = false),
                        Event(item2, prevSemantics = false, newSemantics = true)
                    )
            } else {
                assertThat(events).isEmpty()
            }
        }
    }

    private val SemanticsConfiguration.Text
        get() = getOrNull(SemanticsProperties.Text)?.fastJoinToString()

    private val SemanticsConfiguration.EditableText
        get() = getOrNull(SemanticsProperties.EditableText)?.toString()

    private fun ComposeContentTestRule.setTestContent(
        onSemanticsChange: (SemanticsInfo, SemanticsConfiguration?) -> Unit,
        composable: @Composable () -> Unit
    ) {
        val semanticsListener =
            object : SemanticsListener {
                override fun onSemanticsChanged(
                    semanticsInfo: SemanticsInfo,
                    previousSemanticsConfiguration: SemanticsConfiguration?
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

    // TODO(b/272068594): Add api to fetch the semantics id from SemanticsNodeInteraction directly.
    private val SemanticsNodeInteraction.semanticsId: Int
        get() = fetchSemanticsNode().id

    @Composable
    private fun FocusableBox(
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit = {}
    ) {
        var borderColor by remember { mutableStateOf(Black) }
        Box(
            modifier =
                modifier
                    .size(100.dp)
                    .onFocusChanged { borderColor = if (it.isFocused) Red else Black }
                    .border(2.dp, borderColor)
                    .focusable(),
            content = content
        )
    }
}
