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
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.LayerType
import androidx.compose.ui.Modifier
import androidx.compose.ui.assertThat
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.RenderSettings
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.isEqualTo
import androidx.compose.ui.platform.a11y.AccessibilityController
import androidx.compose.ui.platform.a11y.ComposeAccessible
import androidx.compose.ui.platform.a11y.ComposeSceneAccessible
import androidx.compose.ui.scene.ComposeContainer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
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
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.runInternalSkikoComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.toDpSize
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.runApplicationTest
import java.awt.Dimension
import java.awt.Point
import java.awt.Window
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.accessibility.Accessible
import javax.accessibility.AccessibleComponent
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleContext.ACCESSIBLE_STATE_PROPERTY
import javax.accessibility.AccessibleRole
import javax.accessibility.AccessibleState
import javax.accessibility.AccessibleText
import javax.accessibility.AccessibleValue
import javax.swing.JFrame
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.hostOs
import org.junit.Assert.assertEquals
import org.junit.Test


@OptIn(ExperimentalTestApi::class)
class AccessibilityTest {
    @Test
    fun accessibleText() = runDesktopA11yTest {
        test.setContent {
            Text("Hello world. Hi world.", modifier = Modifier.testTag("text"))
        }

        val accessibleContext = test.onNodeWithTag("text").fetchAccessibleContext()
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
        test.setContent {
            TabRow(selectedTabIndex = 0) {
                Tab(
                    selected = true,
                    onClick = { },
                    modifier = Modifier.testTag("tab"),
                    text = { Text("Tab") }
                )
            }
        }

        test.onNodeWithTag("tab").assertHasAccessibleRole(AccessibleRole.PAGE_TAB)
    }

    @Test
    fun dropDownListRoleTranslatesToComboBoxAccessibleRole() = runDesktopA11yTest {
        test.setContent {
            Button(
                modifier = Modifier
                    .semantics { role = Role.DropdownList }
                    .testTag("button"),
                onClick = { }
            ) {
                Text("Button")
            }
        }

        test.onNodeWithTag("button").assertHasAccessibleRole(AccessibleRole.COMBO_BOX)
    }

    @Test
    fun progressBarHasCorrectRoleAndValues() = runDesktopA11yTest {
        test.setContent {
            LinearProgressIndicator(
                progress = 0.2f,
                modifier = Modifier.testTag("progressbar")
            )
        }

        test.onNodeWithTag("progressbar").fetchAccessibleContext().apply {
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
        test.setContent {
            Box(Modifier.testTag("box"))
        }

        test.onNodeWithTag("box").assertHasAccessibleRole(AccessibleRole.UNKNOWN)
    }

    @Suppress("DEPRECATION")
    @Test
    fun containerHasGroupRole() = runDesktopA11yTest {
        test.setContent {
            Box(Modifier.testTag("box").semantics {
                isContainer = true
            })
        }

        test.onNodeWithTag("box").assertHasAccessibleRole(AccessibleRole.GROUP_BOX)
    }

    @Test
    fun traversalGroupHasGroupRole() = runDesktopA11yTest {
        test.setContent {
            Box(Modifier.testTag("box").semantics {
                isTraversalGroup = true
            })
        }

        test.onNodeWithTag("box").assertHasAccessibleRole(AccessibleRole.GROUP_BOX)
    }

    @Test
    fun hideFromA11yMakesAccessibleUnavailable() = runDesktopA11yTest {
        test.setContent {
            Text(
                text = "Hello",
                modifier = Modifier.testTag("text")
                    .semantics {
                        hideFromAccessibility()
                    }
            )
        }

        assertFails("Component should be invisible to accessibility, but isn't") {
            test.onNodeWithTag("text").fetchAccessible()
        }
    }

    @Test
    fun materialRadioButtonHasCorrectCheckedStates() = runDesktopA11yTest {
        var selected by mutableStateOf(true)
        test.setContent {
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

        with(test.onNodeWithTag("radioButton")) {
            assertCurrentAccessibleValueEquals(1)
            assertHasAccessibleState(AccessibleState.CHECKED)
        }
        with(test.onNodeWithTag("radioButton3")) {
            assertCurrentAccessibleValueEquals(1)
            assertHasAccessibleState(AccessibleState.CHECKED)
        }
        selected = false
        with(test.onNodeWithTag("radioButton")) {
            assertCurrentAccessibleValueEquals(0)
            assertDoesNotHaveAccessibleState(AccessibleState.CHECKED)
        }
        with(test.onNodeWithTag("radioButton3")) {
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

        with(test.onNodeWithTag("checkBox")) {
            assertCurrentAccessibleValueEquals(1)
            assertHasAccessibleState(AccessibleState.CHECKED)
        }
        with(test.onNodeWithTag("checkBox3")) {
            assertCurrentAccessibleValueEquals(1)
            assertHasAccessibleState(AccessibleState.CHECKED)
        }
        checked = false
        with(test.onNodeWithTag("checkBox")) {
            assertCurrentAccessibleValueEquals(0)
            assertDoesNotHaveAccessibleState(AccessibleState.CHECKED)
        }
        with(test.onNodeWithTag("checkBox3")) {
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

        test.onNodeWithTag("box").fetchAccessibleComponent().let {
            assertEquals(size, it.size.toDpSize())
            // TODO: Investigate why location is wrong
        }
        size = DpSize(200.dp, 210.dp)
        position = DpOffset(30.dp, 40.dp)
        test.waitForIdle()

        test.onNodeWithTag("box").fetchAccessibleComponent().let {
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

        test.onNodeWithTag("text").apply {
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
            test.waitForIdle()
            assertTrue(caretChanged)
            assertTrue(selectionChanged)
            // Check new selection
            accessibleText().let { accessibleText ->
                assertNotNull(accessibleText, "AccessibleText is null")
                assertThat(accessibleText.selectedText).isEqualTo("Hello")
            }

            // Check empty selection
            performTextInputSelection(TextRange(3, 3))
            test.waitForIdle()
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

        verifyTextFieldA11y(test.onNodeWithTag("textField"))
    }

    @Test
    fun verifyTextField2A11y() = runDesktopA11yTest {
        test.setContent {
            BasicTextField(
                state = rememberTextFieldState("Hello world"),
                modifier = Modifier.testTag("textField")
            )
        }

        verifyTextFieldA11y(test.onNodeWithTag("textField"))
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

        test.onNodeWithTag("container").fetchAccessible().accessibleContext.let { context ->
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
            assertThat(test.onNodeWithTag(tag).fetchAccessible().accessibleContext?.accessibleIndexInParent)
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

        assertThat(test.onNodeWithTag("button").fetchAccessible().accessibleContext?.accessibleRole)
            .isEqualTo(AccessibleRole.PUSH_BUTTON)
    }

    private fun verifyA11yHierarchyFromAccessible(
        window: Window,
        @Suppress("SameParameterValue") accessibleName: String
    ) {
        val accessible = window.findAccessibleNamed(accessibleName)
        assertNotNull(accessible)

        // Validate the chain from the accessible to the window
        var child: Accessible = accessible
        while (true) {
            val parent = child.accessibleContext.accessibleParent ?: break

            // Check that the index reported by the child matches what the parent says is the
            // child at that index.
            val childIndexInParent = child.accessibleContext.accessibleIndexInParent
            val childAtIndex = parent.accessibleContext.getAccessibleChild(childIndexInParent)
            // Note that we can't compare child with childAtIndex itself because the
            // Accessible instances themselves are different at the seam between Swing and Compose.
            // The child Accessible of SkiaLayer is the Canvas/HardwareLayer inside it, but the
            // Accessible at the root of the scene is ComposeSceneAccessible. The trick is that they
            // both return the same AccessibleContext (HardwareLayer does it via
            // `externalAccessibleFactory`).
            assertEquals(
                expected = child.accessibleContext,
                actual = childAtIndex.accessibleContext,
                message = "Wrong actual child of ${parent.accessibleContext}"
            )

            child = parent
        }
        assertEquals(window, child)
    }

    @Test
    fun verifyA11yHierarchy() = runApplicationTest {
        launchTestWindowApplication {
            Text("text")
        }
        awaitIdle()

        // Relies on ComposeAccessible.getAccessibleName returning the text
        verifyA11yHierarchyFromAccessible(window = window, accessibleName = "text")
    }

    @Test
    fun verifyA11yHierarchyWithComposePanel() = runApplicationTest {
        val composePanel = ComposePanel().apply {
            setContent {
                Text("text")
            }
        }
        val window = ComposeWindow()
        window.contentPane.add(composePanel)
        window.size = Dimension(800, 600)

        try {
            window.isVisible = true
            awaitIdle()
            // Relies on ComposeAccessible.getAccessibleName returning the text
            verifyA11yHierarchyFromAccessible(window = window, accessibleName = "text")
        } finally {
            window.dispose()
        }
    }

    @Test
    fun verifyA11yHierarchyWithComposePanelAndOnComponentLayerType() {
        ComposeFeatureFlags.useSwingGraphicsInComposePanel.withOverride(true) {
            ComposeFeatureFlags.layerType.withOverride(LayerType.OnComponent) {
                verifyA11yHierarchyWithComposePanel()
            }
        }
    }

    @Test
    fun initiallyFocusedElementNotifiesSystemOfFocus() = runApplicationTest {
        AccessibilityController.AccessibilityUsage.notifyInUse()

        val deferredWindow = CompletableDeferred<ComposeWindow>()
        launchTestWindowApplication {
            val focusRequester = remember { FocusRequester() }
            TextField(rememberTextFieldState("text"), Modifier.focusRequester(focusRequester))
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            LaunchedEffect(Unit) { deferredWindow.complete(this@launchTestWindowApplication.window) }
        }

        val window = deferredWindow.await()
        var textFieldAccessible: Accessible? = null
        var textFieldHasFocus = false
        val receivedFocus = Channel<Unit>(CONFLATED)
        window.addHierarchyListener(object : HierarchyListener {
            override fun hierarchyChanged(e: HierarchyEvent?) {
                if (window.isDisplayable) {
                    textFieldAccessible = window.findAccessibleNamed("text")!!
                    textFieldAccessible.accessibleContext.addPropertyChangeListener { evt ->
                        if (evt.propertyName == ACCESSIBLE_STATE_PROPERTY) {
                            if (evt.newValue == AccessibleState.FOCUSED) {
                                textFieldHasFocus = true
                            } else if (evt.oldValue == AccessibleState.FOCUSED) {
                                textFieldHasFocus = false
                            }
                            receivedFocus.trySend(Unit)
                        }
                    }
                    window.removeHierarchyListener(this)
                }
            }
        })

        suspend fun waitForTextFieldFocusedState(focused: Boolean) {
            // If we do awaitIdle here, we'll sometimes fail the assertSceneAccessibleIsTextField
            // check because the NativeAccessibleFocusHelper.focusedAccessible is reset to null
            // after 100ms, and awaitIdle sometimes takes longer.
            withTimeoutOrNull(1000) {
                while ((window.isFocused != focused) || (textFieldHasFocus != focused)) {
                    receivedFocus.receive()
                }
            }
            val focusStateString = if (focused) "focused" else "unfocused"
            assertEquals(focused, window.isFocused, "Could not make original window $focusStateString")
            assertEquals(focused, textFieldHasFocus, "TextField accessible did not send $focusStateString event")
        }

        waitForTextFieldFocusedState(true)
        assertNotNull(textFieldAccessible)

        // What really causes Java's accessibility to report the correct element (on Windows;
        // possibly on macOS too) is not actually the property change event, but a trick in
        // Skiko's NativeAccessibleFocusHelper which reports the focused Accessible following
        // a call to requestNativeFocusOnAccessible
        fun assertSceneAccessibleIsTextField() {
            // On Linux, NativeAccessibleFocusHelper doesn't do its trick with focusedAccessible
            if ((hostOs != OS.Windows) && (hostOs != OS.MacOS)) return

            // Find the ComposeSceneAccessible
            var composeSceneAccessible = textFieldAccessible
            while (composeSceneAccessible != null) {
                if (composeSceneAccessible is ComposeSceneAccessible) break
                composeSceneAccessible = composeSceneAccessible.accessibleContext?.accessibleParent
            }
            assertNotNull(composeSceneAccessible)

            // We want to check the accessibleContext of SkiaLayer.HardwareLayer or SkiaSwingLayer
            val sceneParent = composeSceneAccessible.accessibleContext.accessibleParent
            val childCount = sceneParent.accessibleContext.accessibleChildrenCount
            for (i in 0 until childCount) {
                val child = sceneParent.accessibleContext.getAccessibleChild(i)
                if (child.accessibleContext == textFieldAccessible.accessibleContext) {
                    return  // Found it!
                }
            }
            fail("Did not find ancestor with correct accessible context")
        }
        assertSceneAccessibleIsTextField()

        // The additional check below actually works on Linux, but unfortunately, not on our CI
        if (hostOs == OS.Linux) return@runApplicationTest

        // De-focus, then re-focus the window and check that another focus gained property change
        // event was sent
        val anotherWindow = JFrame()
        try {
            anotherWindow.size = Dimension(800, 600)
            anotherWindow.isVisible = true
            anotherWindow.toFront()
            waitForTextFieldFocusedState(false)
            assertFalse(window.isFocused)
            assertFalse(textFieldHasFocus)
            delay(100)  // Helps test to be more reliable
            window.toFront()
            waitForTextFieldFocusedState(true)
            assertSceneAccessibleIsTextField()
        } finally {
            if (anotherWindow.isShowing) {
                anotherWindow.dispose()
            }
        }
    }

    // This test asserts that the component corresponding to ComposeSceneAccessible when using
    // WindowComposeSceneLayer is a child of ComposeSceneMediator.contentComponent (SkiaLayer),
    // which is assumed by WindowSkiaLayerComponent.sceneAccessibleParent
    // The purpose of this test is to make sure that if this implementation detail of SkiaLayer
    // ever changes, the test will fail and indicate that
    // WindowSkiaLayerComponent.sceneAccessibleParent needs to be updated.
    @Test
    fun assertWindowsSkiaLayerComponentsChildIsAccessibleRoot() = SwingUtilities.invokeAndWait {
        val container = ComposeContainer(
            container = JLayeredPane(),
            skiaLayerAnalytics = SkiaLayerAnalytics.Empty,
            window = null,
            layerType = LayerType.OnWindow
        )

        val composeSceneAccessible = container.accessible
        val matchingChild = container.contentComponent.components.find { // contentComponent is SkiaLayer
            it.accessibleContext == composeSceneAccessible.accessibleContext
        }
        assertNotNull(matchingChild)
    }

    // This test asserts that the component corresponding to ComposeSceneAccessible when using
    // SwingComposeSceneLayer is ComposeSceneMediator.contentComponent itself (SkiaSwingLayer),
    // which is assumed by SwingSkiaLayerComponent.sceneAccessibleParent
    // The purpose of this test is to make sure that if this implementation detail of SkiaLayer
    // ever changes, the test will fail and indicate that
    // SwingSkiaLayerComponent.sceneAccessibleParent needs to be updated.
    @Test
    fun assertSwingSkiaLayerComponentIsAccessibleRoot() = SwingUtilities.invokeAndWait {
        val container = ComposeContainer(
            container = JLayeredPane(),
            skiaLayerAnalytics = SkiaLayerAnalytics.Empty,
            window = null,
            layerType = LayerType.OnComponent,
            renderSettings = RenderSettings.SwingGraphics()
        )

        val composeSceneAccessible = container.accessible
        assertEquals(
            composeSceneAccessible.accessibleContext,
            container.contentComponent.accessibleContext
        )
    }
}


/**
 * Runs a test of accessibility.
 */
@OptIn(ExperimentalTestApi::class, InternalTestApi::class)
private fun runDesktopA11yTest(block: ComposeA11yTestScope.() -> Unit) {

    val parentAccessible = Accessible { null }

    lateinit var composeSceneAccessible: ComposeSceneAccessible

    // A SemanticsOwnerListener to manage the AccessibilityControllers
    val semanticsOwnerListener = object : PlatformContext.SemanticsOwnerListener {
        private val _accessibilityControllers = linkedMapOf<SemanticsOwner, AccessibilityController>()
        val accessibilityControllers get() = _accessibilityControllers.values.reversed()

        override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) {
            check(semanticsOwner !in _accessibilityControllers)
            _accessibilityControllers[semanticsOwner] = AccessibilityController(
                owner = semanticsOwner,
                desktopComponent = PlatformComponent.Empty,
                parentAccessible = composeSceneAccessible,
                onFocusReceived = { }
            )
        }

        override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
            _accessibilityControllers.remove(semanticsOwner)?.dispose()
        }

        override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {
            _accessibilityControllers[semanticsOwner]?.onSemanticsChange()
        }

        override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {
            _accessibilityControllers[semanticsOwner]?.onLayoutChanged(nodeId = semanticsNodeId)
        }
    }

    // The root (scene) accessible
    composeSceneAccessible = ComposeSceneAccessible(
        forceEnableA11y = true,
        parent = { parentAccessible },
        accessibilityControllersProvider = { semanticsOwnerListener.accessibilityControllers }
    )

    // Reset the a11y usage, to avoid having one test affect the next
    AccessibilityController.AccessibilityUsage.reset()

    val testDispatcher = StandardTestDispatcher()
    runInternalSkikoComposeUiTest(
        semanticsOwnerListener = semanticsOwnerListener,
        coroutineDispatcher = testDispatcher
    ) {
        semanticsOwnerListener.accessibilityControllers.forEach {
            it.launchSyncLoop(testDispatcher)
        }

        val scope = ComposeA11yTestScope(
            test = this,
            sceneAccessible = composeSceneAccessible
        )
        block(scope)
    }
}

/**
 * The scope for running accessibility tests.
 */
@OptIn(ExperimentalTestApi::class)
internal class ComposeA11yTestScope(
    val test: SkikoComposeUiTest,
    val sceneAccessible: ComposeSceneAccessible
) {

    @Suppress("MemberVisibilityCanBePrivate")
    val sceneAccessibleContext: ComposeSceneAccessible.ComposeSceneAccessibleContext
        get() = sceneAccessible.accessibleContext!!

    @Suppress("MemberVisibilityCanBePrivate")
    fun SemanticsNodeInteraction.fetchAccessible(): ComposeAccessible =
        fetchSemanticsNode().fetchAccessible()

    @Suppress("MemberVisibilityCanBePrivate")
    fun SemanticsNode.fetchAccessible(): ComposeAccessible {
        for (controller in sceneAccessibleContext.accessibilityControllers) {
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

private fun Accessible.findAccessibleNamed(name: String): Accessible? {
    val accessibleContext = this.accessibleContext
    if (accessibleContext.accessibleName == name) return this
    for (index in 0 until accessibleContext.accessibleChildrenCount) {
        val child = accessibleContext.getAccessibleChild(index)
        child.findAccessibleNamed(name)?.let { return it }
    }
    return null
}