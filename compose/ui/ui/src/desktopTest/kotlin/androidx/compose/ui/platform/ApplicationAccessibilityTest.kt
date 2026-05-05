/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.*
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.LayerType
import androidx.compose.ui.Modifier
import androidx.compose.ui.assertThat
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.isEqualTo
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.a11y.SemanticsOwnerAccessibility
import androidx.compose.ui.platform.a11y.ComposeAccessible
import androidx.compose.ui.platform.a11y.ComposeSceneAccessibility.ComposeSceneAccessibleContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import java.awt.Dimension
import java.awt.Point
import java.awt.Window
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.accessibility.Accessible
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleContext.ACCESSIBLE_STATE_PROPERTY
import javax.accessibility.AccessibleState
import javax.swing.JFrame
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

@OptIn(ExperimentalMaterialApi::class)
class ApplicationAccessibilityTest {
    @Test
    fun `single component accessibility`() = runApplicationTest {
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                Button(
                    onClick = {},
                    modifier = Modifier.size(100.dp)
                ) {
                    Text("Accessible button")
                }
            }
        }
        awaitIdle()

        withLeafAccessibleAt(window, 20, 20) {
            assertThat(accessibleName).isEqualTo("Accessible button")
        }
    }

    @Test
    fun `popup accessibility`() = runApplicationTest {
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                // show popup on top of the accessible button
                val position = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset = IntOffset(0, 25)
                }
                Popup(
                    popupPositionProvider = position,
                    properties = PopupProperties(focusable = false)
                ) {
                    Button(
                        onClick = {},
                        modifier = Modifier.size(100.dp)
                    ) {
                        Text("Accessible popup button")
                    }
                }
            }
        }
        awaitIdle()

        withLeafAccessibleAt(window, 5, 50) {
            assertThat(accessibleName).isEqualTo("Accessible popup button")
        }
    }

    @Test
    fun `accessibility of multiple components`() = runApplicationTest {
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                Column {
                    Button(
                        onClick = {},
                        modifier = Modifier.size(20.dp),
                    ) {
                        Text("Accessible button 1")
                    }
                    Button(
                        onClick = {},
                        modifier = Modifier.size(20.dp),
                    ) {
                        Text("Accessible button 2")
                    }
                }
            }
        }
        awaitIdle()

        withLeafAccessibleAt(window, 10, 10) {
            assertThat(accessibleName).isEqualTo("Accessible button 1")
        }

        withLeafAccessibleAt(window, 5, 22) {
            assertThat(accessibleName).isEqualTo("Accessible button 2")
        }
    }

    // TODO: component under popup shouldn't be read by screen reader
    //  but current implementation does it
    //  (see ComposeSceneAccessible.ComposeSceneAccessibleContext.getAccessibleAt)
    @Ignore
    @Test
    fun `hover popup when there is a component under it`() = runApplicationTest {
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                Column {
                    Button(
                        onClick = {},
                        modifier = Modifier.size(20.dp),
                    ) {
                        Text("button under popup")
                    }
                    val popupPosition = object : PopupPositionProvider {
                        override fun calculatePosition(
                            anchorBounds: IntRect,
                            windowSize: IntSize,
                            layoutDirection: LayoutDirection,
                            popupContentSize: IntSize
                        ): IntOffset = IntOffset.Zero
                    }
                    Popup(popupPosition) {
                        Column {
                            Spacer(Modifier.height(30.dp))
                            Button(
                                onClick = {},
                                modifier = Modifier.size(20.dp)
                            ) {
                                Text("popup button")
                            }
                        }
                    }
                }
            }
        }
        awaitIdle()

        withLeafAccessibleAt(window, 5, 32) {
            assertThat(accessibleName).isEqualTo("popup button")
        }

        withLeafAccessibleAt(window, 5, 5) {
            assertNotEquals("button under popup", accessibleName)
        }
    }

    // https://github.com/JetBrains/compose-multiplatform/issues/2185
    @Test
    fun `drop-down menu accessibility`() = runApplicationTest {
        lateinit var window: ComposeWindow
        var firstItemPositionPx: Offset? = null
        var secondItemPositionPx: Offset? = null

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                DropdownMenu(true, onDismissRequest = {}) {
                    DropdownMenuItem(onClick = {}) {
                        Text("item 1", modifier = Modifier.onGloballyPositioned {
                            firstItemPositionPx = it.positionInWindow()
                        })
                    }
                    DropdownMenuItem(onClick = {}) {
                        Text("item 2", modifier = Modifier.onGloballyPositioned {
                            secondItemPositionPx = it.positionInWindow()
                        })
                    }
                }
            }
        }
        awaitIdle()

        val firstItemPosition = firstItemPositionPx!!.toAwtPoint(window)
        val secondItemPosition = secondItemPositionPx!!.toAwtPoint(window)

        withLeafAccessibleAt(window, firstItemPosition.x + 2, firstItemPosition.y + 2) {
            assertThat(accessibleName).isEqualTo("item 1")
        }

        withLeafAccessibleAt(window, secondItemPosition.x + 2, secondItemPosition.y + 2) {
            assertThat(accessibleName).isEqualTo("item 2")
        }
    }

    // https://github.com/JetBrains/compose-multiplatform/issues/2120
    @Test
    fun `alert dialog accessibility`() = runApplicationTest {
        lateinit var window: ComposeWindow
        var buttonTextPositionPx: Offset? = null
        var textPositionPx: Offset? = null

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Alert Dialog") },
                    text = {
                        Text(
                            "Alert Dialog Text",
                            modifier = Modifier
                                .onGloballyPositioned { textPositionPx = it.positionInWindow() }
                        )
                    },
                    confirmButton = {
                        Button(onClick = {}) {
                            Text(
                                "Alert Dialog Button",
                                modifier = Modifier
                                    .onGloballyPositioned {
                                        buttonTextPositionPx = it.positionInWindow()
                                    }
                            )
                        }
                    }
                )
            }
        }
        awaitIdle()

        val textPosition = textPositionPx!!.toAwtPoint(window)
        val buttonTextPosition = buttonTextPositionPx!!.toAwtPoint(window)

        withLeafAccessibleAt(window, textPosition.x + 2, textPosition.y + 2) {
            assertThat(accessibleName).isEqualTo("Alert Dialog Text")
        }

        withLeafAccessibleAt(window, buttonTextPosition.x + 2, buttonTextPosition.y + 2) {
            assertThat(accessibleName).isEqualTo("Alert Dialog Button")
        }
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
        Assert.assertEquals(window, child)
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
        SemanticsOwnerAccessibility.AccessibilityUsage.notifyInUse()

        val deferredWindow = CompletableDeferred<ComposeWindow>()
        launchTestWindowApplication {
            val focusRequester = remember { FocusRequester() }
            TextField(
                state = rememberTextFieldState("Hello, World"),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .semantics {
                        contentDescription = "text"
                    }
            )
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
            // check because the AccessibleFocusHelper.focusedAccessible is reset to null
            // after 100ms, and awaitIdle sometimes takes longer.
            withTimeoutOrNull(1.seconds) {
                while ((window.isFocused != focused) || (textFieldHasFocus != focused)) {
                    receivedFocus.receive()
                }
            }
            val focusStateString = if (focused) "focused" else "unfocused"
            assertEquals(focused, window.isFocused, "Could not make original window $focusStateString")
            assertEquals(focused, textFieldHasFocus, "TextField accessible did not send $focusStateString event")
        }

        waitForTextFieldFocusedState(focused = true)
        assertNotNull(textFieldAccessible)

        fun Accessible.accessibleAncestorChain() = sequence {
            var ancestor: Accessible? = accessibleContext.accessibleParent
            while (ancestor != null) {
                yield(ancestor)
                ancestor = ancestor.accessibleContext.accessibleParent
            }
        }

        // What really causes Java's accessibility to report the correct element (on Windows;
        // possibly on macOS too) is not the property change event, but a trick in
        // AccessibleFocusHelper which reports the focused AccessibleContext following a call to
        // requestFocusOnAccessible
        fun assertSceneAccessibleIsTextField() {
            // On Linux, NativeAccessibleFocusHelper doesn't do its trick with focusedAccessible
            if ((hostOs != OS.Windows) && (hostOs != OS.MacOS)) return

            // Check the Accessible chain's integrity
            assertEquals(
                expected = window,
                actual = textFieldAccessible.accessibleAncestorChain().last()
            )

            // Verify that the TextField's `AccessibleContext' replaced the `ComposeSceneAccessibleContext`
            assertTrue(
                textFieldAccessible.accessibleAncestorChain().none {
                    it.accessibleContext is ComposeSceneAccessibleContext
                },
                message = "TextField Accessible found to be a descendant of the scene"
            )

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
            waitForTextFieldFocusedState(focused = false)
            assertFalse(window.isFocused)
            assertFalse(textFieldHasFocus)
            delay(100.milliseconds)  // Helps test to be more reliable
            window.toFront()
            waitForTextFieldFocusedState(focused = true)
            assertSceneAccessibleIsTextField()
        } finally {
            if (anotherWindow.isShowing) {
                anotherWindow.dispose()
            }
        }
    }

    private inline fun withLeafAccessibleAt(
        window: ComposeWindow,
        x: Int,
        y: Int,
        check: AccessibleContext.() -> Unit
    ) {
        val point = Point(x, y)

        // The position excludes the window decorations (title bar), so we start the search from
        // the root pane.
        var accessible: Accessible = window.rootPane
        while (true) {
            val childAtPoint = accessible.findAccessibleChildAt(point) ?: break

            // Because of the hack in `AccessibleFocusHelper`, `ComposeAccessible`s report their
            // bounds relative to the scene root, not relative to their parent.
            if (childAtPoint !is ComposeAccessible) {
                childAtPoint.accessibleContext.accessibleComponent?.let { c ->
                    point.x -= c.location.x
                    point.y -= c.location.y
                }
            }
            accessible = childAtPoint
        }

        check(accessible.accessibleContext)
    }

    // Can't use `AccessibleComponent.getAccessibleAt(Point)` because it's a hot and buggy mess
    // (see how Container.getAccessibleAt(Point) is implemented)
    private fun Accessible.findAccessibleChildAt(point: Point): Accessible? {
        val context = accessibleContext ?: return null
        val childCount =  context.accessibleChildrenCount
        for (i in 0 until childCount) {
            val child = context.getAccessibleChild(i)
            val accessibleComponent = child.accessibleContext?.accessibleComponent ?: continue
            if (!accessibleComponent.isShowing) continue
            if (accessibleComponent.contains(point)) {
                return child
            }
        }

        return null
    }

    private fun Offset.toAwtPoint(window: ComposeWindow): Point = with(window.density) {
        return Point(x.toDp().value.toInt(), y.toDp().value.toInt())
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
}

