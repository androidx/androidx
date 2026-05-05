/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.assertThat
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.isEqualTo
import androidx.compose.ui.platform.a11y.AccessibleFocusHelper
import androidx.compose.ui.platform.a11y.SemanticsOwnerAccessibility
import androidx.compose.ui.platform.a11y.ComposeAccessible
import androidx.compose.ui.platform.a11y.ComposeSceneAccessibility
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.awtRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isContainer
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.v2.runInternalSkikoComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.toDpSize
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.awt.Point
import javax.accessibility.AccessibleComponent
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole
import javax.accessibility.AccessibleState
import javax.accessibility.AccessibleText
import javax.accessibility.AccessibleValue
import javax.swing.JPanel
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test


@OptIn(ExperimentalTestApi::class)
class AccessibilityTest {
    @Test
    fun accessibleText() = runDesktopA11yTest {
        setContent {
            Text("Hello world. Hi world.", modifier = Modifier.testTag("text"))
        }

        val accessibleContext = onNodeWithTag("text").fetchAccessibleContext()
        val accessibleText = accessibleContext.accessibleText!!
        assertEquals(22, accessibleText.charCount)

        assertEquals("H", accessibleText.getAtIndex(AccessibleText.CHARACTER, 0))
        assertEquals("Hello", accessibleText.getAtIndex(AccessibleText.WORD, 0))
        assertEquals("Hello world. ", accessibleText.getAtIndex(AccessibleText.SENTENCE, 0))

        assertEquals("e", accessibleText.getAfterIndex(AccessibleText.CHARACTER, 0))
        assertEquals("world", accessibleText.getAfterIndex(AccessibleText.WORD, 0))
        assertEquals("Hi world.", accessibleText.getAfterIndex(AccessibleText.SENTENCE, 0))

        assertEquals("d", accessibleText.getBeforeIndex(AccessibleText.CHARACTER, 21))
        assertEquals("world", accessibleText.getBeforeIndex(AccessibleText.WORD, 21))
        assertEquals("Hi world", accessibleText.getBeforeIndex(AccessibleText.SENTENCE, 21))

        assertEquals(0, accessibleText.getIndexAtPoint(Point(0, 0)))
        assertEquals("Hello world. Hi world.".length, accessibleText.getIndexAtPoint(Point(10000, 10000)))
    }

    @Test
    fun tabHasPageTabAccessibleRole() = runDesktopA11yTest {
        setContent {
            TabRow(selectedTabIndex = 0) {
                Tab(
                    selected = true,
                    onClick = { },
                    modifier = Modifier.testTag("tab"),
                    text = { Text("Tab") }
                )
            }
        }

        onNodeWithTag("tab").assertHasAccessibleRole(AccessibleRole.PAGE_TAB)
    }

    @Test
    fun dropDownListRoleTranslatesToComboBoxAccessibleRole() = runDesktopA11yTest {
        setContent {
            Button(
                modifier = Modifier
                    .semantics { role = Role.DropdownList }
                    .testTag("button"),
                onClick = { }
            ) {
                Text("Button")
            }
        }

        onNodeWithTag("button").assertHasAccessibleRole(AccessibleRole.COMBO_BOX)
    }

    @Test
    fun progressBarHasCorrectRoleAndValues() = runDesktopA11yTest {
        setContent {
            LinearProgressIndicator(
                progress = 0.2f,
                modifier = Modifier.testTag("progressbar")
            )
        }

        onNodeWithTag("progressbar").fetchAccessibleContext().apply {
            val value = accessibleValue
                ?: fail("No accessibleValue on LinearProgressIndicator")

            assertThat(accessibleRole).isEqualTo(AccessibleRole.PROGRESS_BAR)
            assertThat(value.minimumAccessibleValue).isEqualTo(0f)
            assertThat(value.maximumAccessibleValue).isEqualTo(1f)
            assertThat(value.currentAccessibleValue).isEqualTo(0.2f)
        }
    }

    @Test
    fun boxHasUnknownRole() = runDesktopA11yTest{
        setContent {
            Box(Modifier.testTag("box"))
        }

        onNodeWithTag("box").assertHasAccessibleRole(AccessibleRole.UNKNOWN)
    }

    @Suppress("DEPRECATION")
    @Test
    fun containerHasGroupRole() = runDesktopA11yTest {
        setContent {
            Box(Modifier.testTag("box").semantics {
                isContainer = true
            })
        }

        onNodeWithTag("box").assertHasAccessibleRole(AccessibleRole.GROUP_BOX)
    }

    @Test
    fun traversalGroupHasGroupRole() = runDesktopA11yTest {
        setContent {
            Box(Modifier.testTag("box").semantics {
                isTraversalGroup = true
            })
        }

        onNodeWithTag("box").assertHasAccessibleRole(AccessibleRole.GROUP_BOX)
    }

    @Test
    fun hideFromA11yMakesAccessibleUnavailable() = runDesktopA11yTest {
        setContent {
            Text(
                text = "Hello",
                modifier = Modifier.testTag("text")
                    .semantics {
                        hideFromAccessibility()
                    }
            )
        }

        assertFails("Component should be invisible to accessibility, but isn't") {
            onNodeWithTag("text").fetchAccessible()
        }
    }

    @Test
    fun materialRadioButtonHasCorrectCheckedStates() = runDesktopA11yTest {
        var selected by mutableStateOf(true)
        setContent {
            Column {
                androidx.compose.material.RadioButton(
                    selected = selected,
                    onClick = { },
                    modifier = Modifier
                        .testTag("radioButton")
                )
                androidx.compose.material3.RadioButton(
                    selected = selected,
                    onClick = { },
                    modifier = Modifier
                        .testTag("radioButton3")
                )
            }
        }

        with(onNodeWithTag("radioButton")) {
            assertCurrentAccessibleValueEquals(1)
            assertHasAccessibleState(AccessibleState.CHECKED)
        }
        with(onNodeWithTag("radioButton3")) {
            assertCurrentAccessibleValueEquals(1)
            assertHasAccessibleState(AccessibleState.CHECKED)
        }
        selected = false
        with(onNodeWithTag("radioButton")) {
            assertCurrentAccessibleValueEquals(0)
            assertDoesNotHaveAccessibleState(AccessibleState.CHECKED)
        }
        with(onNodeWithTag("radioButton3")) {
            assertCurrentAccessibleValueEquals(0)
            assertDoesNotHaveAccessibleState(AccessibleState.CHECKED)
        }
    }

    @Test
    fun materialCheckboxHasCorrectCheckedStates() = runDesktopA11yTest {
        var checked by mutableStateOf(true)
        test.setContent {
            Column {
                androidx.compose.material.Checkbox(
                    checked = checked,
                    onCheckedChange = { },
                    modifier = Modifier
                        .testTag("checkBox")
                )
                androidx.compose.material3.Checkbox(
                    checked = checked,
                    onCheckedChange = { },
                    modifier = Modifier
                        .testTag("checkBox3")
                )
            }
        }

        with(onNodeWithTag("checkBox")) {
            assertCurrentAccessibleValueEquals(1)
            assertHasAccessibleState(AccessibleState.CHECKED)
        }
        with(onNodeWithTag("checkBox3")) {
            assertCurrentAccessibleValueEquals(1)
            assertHasAccessibleState(AccessibleState.CHECKED)
        }
        checked = false
        with(onNodeWithTag("checkBox")) {
            assertCurrentAccessibleValueEquals(0)
            assertDoesNotHaveAccessibleState(AccessibleState.CHECKED)
        }
        with(onNodeWithTag("checkBox3")) {
            assertCurrentAccessibleValueEquals(0)
            assertDoesNotHaveAccessibleState(AccessibleState.CHECKED)
        }
    }

    @Test
    fun accessibleComponentBoundsAreUpdated() = runDesktopA11yTest {
        var size by mutableStateOf(DpSize(100.dp, 110.dp))
        var position by mutableStateOf(DpOffset(10.dp, 20.dp))
        test.setContent {
            Box(
                modifier = Modifier
                    .testTag("box")
                    .size(size)
                    .offset(position.x, position.y)
            )
        }

        onNodeWithTag("box").fetchAccessibleComponent().let {
            assertEquals(size, it.size.toDpSize())
            // TODO: Investigate why location is wrong
        }
        size = DpSize(200.dp, 210.dp)
        position = DpOffset(30.dp, 40.dp)
        waitForIdle()

        onNodeWithTag("box").fetchAccessibleComponent().let {
            assertEquals(size, it.size.toDpSize())
        }
    }

    @Test
    fun mergeDescendantsMergesText() = runDesktopA11yTest {
        test.setContent {
            Row(
                Modifier
                    .testTag("text")
                    .semantics(mergeDescendants = true) {}
            ) {
                Text("Hello")
                Text("World")
            }
        }

        onNodeWithTag("text").apply {
            assertTextContains("Hello")
            assertTextContains("World")
        }
    }

    private fun ComposeA11yTestScope.verifyTextFieldA11y(node: SemanticsNodeInteraction) {
        fun AccessibleText.asString() = buildString {
            for (i in 0 until charCount) {
                append(getAtIndex(AccessibleText.CHARACTER, i))
            }
        }

        fun SemanticsNodeInteraction.accessibleText() =
            fetchAccessible().accessibleContext?.accessibleText

        with(node) {
            // Check role and states
            assertHasAccessibleRole(AccessibleRole.TEXT)
            assertHasAccessibleState(AccessibleState.EDITABLE)

            // Check text
            accessibleText().let { accessibleText ->
                assertNotNull(accessibleText, "AccessibleText is null")
                assertThat(accessibleText.asString()).isEqualTo("Hello world")
                assertThat(accessibleText.getAtIndex(AccessibleText.WORD, 0)).isEqualTo("Hello")
                assertThat(accessibleText.getAtIndex(AccessibleText.WORD, 6)).isEqualTo("world")
                assertThat(accessibleText.selectedText).isEqualTo(null)
            }

            // Check selection change events
            var caretChanged = false
            var selectionChanged = false
            fetchAccessible().accessibleContext!!.addPropertyChangeListener { evt ->
                when (evt.propertyName) {
                    AccessibleContext.ACCESSIBLE_CARET_PROPERTY -> caretChanged = true
                    AccessibleContext.ACCESSIBLE_SELECTION_PROPERTY -> selectionChanged = true
                }
            }
            performTextInputSelection(TextRange(5, 0))
            waitForIdle()
            assertTrue(caretChanged)
            assertTrue(selectionChanged)
            // Check new selection
            accessibleText().let { accessibleText ->
                assertNotNull(accessibleText, "AccessibleText is null")
                assertThat(accessibleText.selectedText).isEqualTo("Hello")
            }

            // Check empty selection
            performTextInputSelection(TextRange(3, 3))
            waitForIdle()
            accessibleText().let { accessibleText ->
                assertNotNull(accessibleText, "AccessibleText is null")
                assertThat(accessibleText.selectedText).isEqualTo(null)
            }
        }
    }

    @Test
    fun verifyTextField1A11y() = runDesktopA11yTest {
        test.setContent {
            BasicTextField(
                value = "Hello world",
                onValueChange = { },
                modifier = Modifier.testTag("textField")
            )
        }

        verifyTextFieldA11y(onNodeWithTag("textField"))
    }

    @Test
    fun verifyTextField2A11y() = runDesktopA11yTest {
        test.setContent {
            BasicTextField(
                state = rememberTextFieldState("Hello world"),
                modifier = Modifier.testTag("textField")
            )
        }

        verifyTextFieldA11y(onNodeWithTag("textField"))
    }

    @Test
    fun traversalIndexIsRespected() = runDesktopA11yTest {
        test.setContent {
            Column(Modifier
                .testTag("container")
                .semantics {
                    isTraversalGroup = true
                }
            ) {
                Text("Item 1",
                    Modifier
                        .semantics {
                            traversalIndex = 0f
                            contentDescription = "Item 1"
                        }
                        .testTag("item1")
                )
                Text("Item 2",
                    Modifier
                        .semantics {
                            traversalIndex = 2f
                            contentDescription = "Item 2"
                        }
                        .testTag("item2")
                )
                Text("Item 3",
                    Modifier
                        .semantics {
                            traversalIndex = 1f
                            contentDescription = "Item 3"
                        }
                        .testTag("item3")
                )
            }
        }

        onNodeWithTag("container").fetchAccessible().accessibleContext.let { context ->
            assertNotNull(context)

            fun assertDescriptionAtIndexIs(index: Int, expectedDescription: String) {
                assertThat(context.getAccessibleChild(index).accessibleContext.accessibleDescription)
                    .isEqualTo(expectedDescription)
            }

            assertThat(context.accessibleChildrenCount).isEqualTo(3)
            assertDescriptionAtIndexIs(0, "Item 1")
            assertDescriptionAtIndexIs(1, "Item 3")
            assertDescriptionAtIndexIs(2, "Item 2")
        }

        fun assertNodeWithTagIndexInParentIs(tag: String, expectedIndex: Int) {
            assertThat(onNodeWithTag(tag).fetchAccessible().accessibleContext?.accessibleIndexInParent)
                .isEqualTo(expectedIndex)
        }
        assertNodeWithTagIndexInParentIs("item1", 0)
        assertNodeWithTagIndexInParentIs("item2", 2)
        assertNodeWithTagIndexInParentIs("item3", 1)
    }

    @Test
    fun awtRoleIsCorrect() = runDesktopA11yTest {
        test.setContent {
            Box(
                Modifier
                    .testTag("button")
                    .size(100.dp)
                    .semantics {
                        awtRole = AccessibleRole.PUSH_BUTTON
                    }
            )
        }

        assertThat(onNodeWithTag("button").fetchAccessible().accessibleContext?.accessibleRole)
            .isEqualTo(AccessibleRole.PUSH_BUTTON)
    }

    @Test
    fun textFieldAccessibleNameUsesContentDescription() = runDesktopA11yTest {
        test.setContent {
            BasicTextField(
                value = "typed text",
                onValueChange = { },
                modifier = Modifier
                    .testTag("textFieldWithLabel")
                    .semantics {
                        contentDescription = "Email"
                    }
            )
        }

        val context = onNodeWithTag("textFieldWithLabel").fetchAccessible().accessibleContext
        assertThat(context?.accessibleName).isEqualTo("Email")
        assertThat(context?.accessibleDescription).isEqualTo("Email")
    }

    @Test
    fun textFieldAccessibleNameIsNullWithoutContentDescription() = runDesktopA11yTest {
        test.setContent {
            BasicTextField(
                value = "typed text",
                onValueChange = { },
                modifier = Modifier.testTag("textFieldNoLabel")
            )
        }

        val context = onNodeWithTag("textFieldNoLabel").fetchAccessible().accessibleContext
        // TextField without contentDescription should have null accessibleName.
        // The text content is available through the AccessibleText interface.
        assertThat(context?.accessibleName).isEqualTo(null)
    }

    // https://youtrack.jetbrains.com/issue/CMP-9826
    @Test
    fun removingFocusableElementDoesNotCrashCAccessiblePropertyChangeListener() = runDesktopA11yTest {
        SemanticsOwnerAccessibility.AccessibilityUsage.notifyInUse()

        var showTextField by mutableStateOf(true)
        setContent {
            Column {
                BasicTextField(rememberTextFieldState())
                if (showTextField) {
                    val focusRequester = remember { FocusRequester() }
                    BasicTextField(
                        rememberTextFieldState(),
                        Modifier
                            .focusRequester(focusRequester)
                            .testTag("textField")
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                }
            }
        }

        suspend fun removeTextFieldAndTest(withDelay: Boolean) {
            val textFieldAccessible = onNodeWithTag("textField").fetchAccessible()
            var propertyChangeCalled = false
            textFieldAccessible.accessibleContext!!.addPropertyChangeListener { evt ->
                if (evt.propertyName == AccessibleContext.ACCESSIBLE_STATE_PROPERTY) {
                    // Replicate (partially) what happens in CAccessible.AXChangeNotifier.propertyChange
                    val accessibleContext = textFieldAccessible.accessibleContext!!
                    accessibleContext.accessibleRole
                    val parent = accessibleContext.accessibleParent
                    if (parent != null) {
                        parent.accessibleContext!!.accessibleRole
                    }

                    propertyChangeCalled = true
                }
            }

            showTextField = false
            waitForIdle()
            if (withDelay) {
                // Test after waiting out RESET_FOCUS_ACCESSIBLE_DELAY to validate the scenario
                // when ComposeSceneAccessibility.accessibleParentOverride is not active
                delay(AccessibleFocusHelper.RESET_FOCUS_ACCESSIBLE_DELAY + 100.milliseconds)
                waitForIdle()
            }
            assertTrue(
                propertyChangeCalled,
                "Property change listener not called with `ACCESSIBLE_STATE_PROPERTY`"
            )

            // Reset the state
            showTextField = true
            waitForIdle()
        }

        removeTextFieldAndTest(withDelay = false)
        removeTextFieldAndTest(withDelay = true)
    }
}


/**
 * Runs a test of accessibility.
 */
@OptIn(ExperimentalTestApi::class, InternalTestApi::class)
private fun runDesktopA11yTest(block: suspend ComposeA11yTestScope.() -> Unit) {

    lateinit var sceneAccessibility: ComposeSceneAccessibility

    val sceneComponent = object: JPanel() {
        override fun getAccessibleContext() =
            sceneAccessibility.accessibleContextProvider?.invoke(this)
    }

    // sceneRoot needs to have an Accessible parent for some functionality
    val sceneParent = JPanel()
    sceneParent.add(sceneComponent)

    val testDispatcher = StandardTestDispatcher()

    sceneAccessibility = ComposeSceneAccessibility(
        platformComponent = PlatformComponent.Empty,
        coroutineContext = testDispatcher,
        sceneRoot = { sceneComponent },
    )

    // Reset the a11y usage to avoid having one test affect the next
    SemanticsOwnerAccessibility.AccessibilityUsage.reset()

    runInternalSkikoComposeUiTest(
        semanticsOwnerListener = sceneAccessibility,
        effectContext = testDispatcher
    ) {
        block(
            ComposeA11yTestScope(
                test = this,
                sceneAccessibility = sceneAccessibility
            )
        )
    }
}

/**
 * The scope for running accessibility tests.
 */
@OptIn(ExperimentalTestApi::class)
internal class ComposeA11yTestScope(
    val test: SkikoComposeUiTest,
    val sceneAccessibility: ComposeSceneAccessibility
) : SemanticsNodeInteractionsProvider by test {

    fun setContent(composable: @Composable () -> Unit) {
        test.setContent(composable)
    }

    fun waitForIdle() {
        test.waitForIdle()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun SemanticsNodeInteraction.fetchAccessible(): ComposeAccessible =
        fetchSemanticsNode().fetchAccessible()

    @Suppress("MemberVisibilityCanBePrivate")
    fun SemanticsNode.fetchAccessible(): ComposeAccessible {
        for (controller in sceneAccessibility.ownerAccessibilityList) {
            controller.accessibleByNodeId(id)?.let {
                return it
            }
        }

        throw AssertionError("Failed: Accessible does not exist")
    }

    fun SemanticsNodeInteraction.fetchAccessibleComponent(): AccessibleComponent =
        fetchAccessible().composeAccessibleContext

    @Suppress("unused")
    fun SemanticsNode.fetchAccessibleComponent(): AccessibleComponent =
        fetchAccessible().composeAccessibleContext

    fun SemanticsNodeInteraction.fetchAccessibleContext(): AccessibleContext =
        fetchAccessible().composeAccessibleContext

    @Suppress("unused")
    fun SemanticsNode.fetchAccessibleContext(): AccessibleContext =
        fetchAccessible().composeAccessibleContext

    /**
     * Asserts that the [AccessibleContext] corresponding to the given semantics node has the given
     * role.
     */
    fun SemanticsNodeInteraction.assertHasAccessibleRole(role: AccessibleRole) {
        assertThat(fetchAccessible().accessibleContext!!.accessibleRole).isEqualTo(role)
    }

    /**
     * Asserts that the [AccessibleContext] corresponding to the given semantics node has the given
     * state.
     */
    fun SemanticsNodeInteraction.assertHasAccessibleState(state: AccessibleState) {
        assertTrue("Accessible context expected to, but does not have state: $state") {
            fetchAccessible().accessibleContext!!.accessibleStateSet.contains(state)
        }
    }

    /**
     * Asserts that the [AccessibleContext] corresponding to the given semantics node does not have
     * the given state.
     */
    fun SemanticsNodeInteraction.assertDoesNotHaveAccessibleState(state: AccessibleState) {
        assertFalse("Accessible context expected to not contain, but does have state: $state") {
            fetchAccessible().accessibleContext!!.accessibleStateSet.contains(state)
        }
    }

    /**
     * Asserts that the [AccessibleContext] corresponding to the given semantics node has the given
     * current accessible numeric value ([AccessibleValue.getCurrentAccessibleValue]).
     */
    fun SemanticsNodeInteraction.assertCurrentAccessibleValueEquals(number: Number) {
        assertEquals(
            expected = number,
            actual = fetchAccessible().accessibleContext!!.accessibleValue.currentAccessibleValue,
            message = "Current accessible value expected to, but does not equal: $number",
        )
    }

    /**
     * Asserts that the text of the accessible
     */
    fun SemanticsNodeInteraction.assertTextContains(value: String) {
        val text = fetchAccessible().composeAccessibleContext.text
        assertNotNull(text, "Text is null")
        assertTrue(value in text, "Text does not contain $value")
    }
}