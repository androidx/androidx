/*
 * Copyright 2025 The Android Open Source Project
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

// "Declaration annotated with '@OptionalExpectation' can only be used in common module sources."
// because of IgnoreJsTarget. https://youtrack.jetbrains.com/issue/KTIJ-22326
@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")

package androidx.compose.ui.platform.a11y

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.currentTimeMillis
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.get

/**
 * These tests were flaky in Firefox when running the k/js target:
 * https://youtrack.jetbrains.com/issue/CMP-9069/CfWA11YTest.-timed-out-in-Firefox
 *
 * Here, I'd like to share my findings:
 * - `awaitIdle` can take an undefined amount of time, that's why I removed it in tests checking the timing of A11Y updates.
 *   Then the tests use `awaitA11YChanges` - awaiting the HTML mutations.
 * - In some tests I used a Boolean state switching it back and forth.
 *   Even though the state changes caused the invalidation, the A11Y tree didn't change if an update produced the same A11Y tree (for the same state).
 *   It led to awaitA11YChanges hanging forever or to a timeout.
 *   That's why it's better to use an Int state in such tests, so the change in HTML will be noticed 100%.
 * - Note: running the k/js tests in FF takes 15% longer than in Chrome. K/Wasm is fast in both cases.
 */
class CfWA11YTest : OnCanvasTests {

    @Test
    fun a11yButtonClick() = runApplicationTest {
        assertEquals(0, getContainer().childElementCount, "No content expected before a composition")
        var clickCounter = 0

        createComposeWindow {
            Button(onClick = {
                clickCounter++
            }) {
                Text("Button1")
            }
        }

        val a11yContainer = getA11YContainer()
        assertNotNull(a11yContainer)
        assertEquals("", a11yContainer.innerHTML, "No A11Y tree expected yet")

        awaitA11YChanges()

        val button1 = a11yContainer.children[0]?.children[0] as? HTMLElement
        assertNotNull(button1)

        assertEquals("button", button1.getAttribute("role"))
        assertEquals("Button1", button1.innerText)

        repeat(3) {
            button1.click()
            assertEquals(it + 1, clickCounter)
        }
    }

    @Test
    fun changesAreApplied() = runApplicationTest {
        assertEquals(0, getContainer().childElementCount, "No content expected before a composition")
        var clickCounter1 = 0
        var clickCounter2 = 0

        var showButton2 by mutableStateOf(false)

        createComposeWindow {
            Button(onClick = {
                clickCounter1++
            }) {
                Text("Button1")
            }

            if (showButton2) {
                Button(onClick = {
                    clickCounter2++
                }) {
                    Text("Button2")
                }
            }
        }

        val a11yContainer = getA11YContainer()
        assertNotNull(a11yContainer)
        assertEquals("", a11yContainer.innerHTML, "No A11Y tree expected yet")

        awaitA11YChanges()
        val buttonsContainer = a11yContainer.children[0] as HTMLDivElement
        assertEquals(1, buttonsContainer.children.length)

        val button1 = buttonsContainer.children[0] as HTMLElement
        assertEquals("button", button1.getAttribute("role"))
        assertEquals("Button1", button1.innerText)

        showButton2 = true
        awaitA11YChanges()

        assertEquals(2, buttonsContainer.children.length)
        assertTrue(button1.isConnected)

        val button2 = buttonsContainer.children[1] as HTMLElement
        assertEquals("button", button2.getAttribute("role"))
        assertEquals("Button2", button2.innerText)
        assertTrue(button2.isConnected)


        repeat(3) {
            button1.click()
            button2.click()
            assertEquals(it + 1, clickCounter1)
            assertEquals(it + 1, clickCounter2)
        }

        showButton2 = false
        awaitA11YChanges()

        assertEquals(1, buttonsContainer.children.length)
        assertFalse(button2.isConnected)
    }

    @Test
    fun orderOfElements() = runApplicationTest {
        assertEquals(0, getContainer().childElementCount, "No content expected before a composition")
        var show1 by mutableStateOf(true)
        var show2 by mutableStateOf(false)
        var show3 by mutableStateOf(false)

        createComposeWindow {
            if (show1) {
                Button(onClick = {}) {
                    Text("Button1")
                }
            }
            if (show2) {
                Button(onClick = {}) {
                    Text("Button2")
                }
            }
            if (show3) {
                Button(onClick = {}) {
                    Text("Button3")
                }
            }
        }

        val a11yContainer = getA11YContainer()
        assertNotNull(a11yContainer)
        assertEquals("", a11yContainer.innerHTML, "No A11Y tree expected yet")

        awaitA11YChanges()
        val buttonsContainer = a11yContainer.children[0] as HTMLDivElement
        assertEquals(1, buttonsContainer.children.length)

        show2 = true
        show3 = true
        awaitA11YChanges()

        assertEquals(3, buttonsContainer.children.length)

        assertEquals("Button1", buttonsContainer.children[0]!!.innerHTML)
        assertEquals("Button2", buttonsContainer.children[1]!!.innerHTML)
        assertEquals("Button3", buttonsContainer.children[2]!!.innerHTML)

        show1 = false
        awaitA11YChanges()

        assertEquals(2, buttonsContainer.children.length)
        assertEquals("Button2", buttonsContainer.children[0]!!.innerHTML)
        assertEquals("Button3", buttonsContainer.children[1]!!.innerHTML)

        show1 = true
        awaitA11YChanges()

        assertEquals(3, buttonsContainer.children.length)
        assertEquals("Button1", buttonsContainer.children[0]!!.innerHTML)
        assertEquals("Button2", buttonsContainer.children[1]!!.innerHTML)
        assertEquals("Button3", buttonsContainer.children[2]!!.innerHTML)
    }

    /**
     * The tests use a TestCoroutineScope with delay skipping.
     * In some cases we need a test to wait for some time, and that's the purpose of this function.
     */
    suspend fun realDelay(timeMs: Long) {
        withContext(Dispatchers.Default) {
            delay(timeMs)
        }
    }

    @Test
    fun changesMustBeBatched() = runApplicationTest {
        assertEquals(0, getContainer().childElementCount, "No content expected before a composition")
        var value by mutableStateOf(0)

        var recompositions = 0
        createComposeWindow {
            Button(onClick = {}) {
                Text("Text in Button - $value")
                recompositions++
            }
        }

        val a11yContainer = getA11YContainer()!!
        assertEquals("",a11yContainer.innerHTML)
        assertEquals(0,a11yContainer.childElementCount)

        awaitA11YChanges()
        assertTrue(a11yContainer.innerHTML.contains("Text in Button - 0"), "Expected the button to be added in HTML")

        recompositions = 0 // resetting because we're interested to count them only after this point

        repeat(20) {
            value++
            awaitAnimationFrame()
            // No changes expected yet due to debounce
            assertTrue(a11yContainer.innerHTML.contains("Text in Button - 0"), "The state is changing but we expect a debounce and no A11Y tree changes")
        }

        assertTrue(recompositions > 0, "The state has been changing, but no recompositions?")

        val startTime = currentTimeMillis()
        awaitA11YChanges()
        val waitedForChangesMs = currentTimeMillis() - startTime

        val buttonsContainer = a11yContainer.children[0] as HTMLDivElement
        assertEquals(1, buttonsContainer.children.length)

        (buttonsContainer.children[0] as HTMLElement).let { button ->
            assertEquals("button", button.getAttribute("role"))
            assertEquals("Text in Button - 20", button.innerHTML)
        }

        // The tolerance is quite large, but it's so to reduce the flakiness.
        // The idea is that the change is not expected to happen immediately, but with debounce.
        assertTrue(waitedForChangesMs in 50..250, "Changes must be batched, waited for $waitedForChangesMs ms. Allowed tolerance was exceeded")
    }

    @Test
    fun changesMustBeAppliedDespiteConstantDebounceAfter1Second() = runApplicationTest {
        assertEquals(0, getContainer().childElementCount, "No content expected before a composition")
        var value by mutableStateOf(0)

        createComposeWindow {
            Button(onClick = {}) {
                Text("Text in Button - $value")
            }
        }

        val a11yContainer = getA11YContainer()!!

        assertEquals("",a11yContainer.innerHTML, "No A11Y tree expected yet")
        assertEquals(0,a11yContainer.childElementCount, "No A11Y tree expected yet")

        awaitA11YChanges()
        assertTrue(a11yContainer.innerHTML.contains("Text in Button - 0"), "Button must be present in the A11Y tree")

        var changesAppliedTime = 0L

        launch {
            awaitA11YChanges()
            changesAppliedTime = currentTimeMillis()
        }

        // Make sure nothing else interferes with the A11Y tree
        realDelay(1200)
        assertEquals(0, changesAppliedTime, "There were no state changes! Why did A11Y tree change?")

        var debounceCounter = 0
        val startTime = currentTimeMillis()

        while(changesAppliedTime == 0L) {
            if (currentTimeMillis() - startTime > 2000) {
                error("Changes must be applied after 1 second, waited for ${currentTimeMillis() - startTime} ms")
            }
            // Change the state every frame. Such changes must be "debounced", (time delta is less than 100ms)
            value += 1
            awaitAnimationFrame()

            if (changesAppliedTime == 0L) {
                debounceCounter++
            }
        }

        // To avoid flakiness, we make just a sanity check. The expected value is ~18
        assertTrue(debounceCounter > 1)

        // Adding a tolerance of 200ms, just to avoid flakiness
        assertTrue(
            changesAppliedTime - startTime in (1000..1200),
            "Changes must be applied after 1 second, waited for ${changesAppliedTime - startTime} ms"
        )
    }

    @Test
    fun noChangesFor1SecondTheDebounceShouldWork() = runApplicationTest {
        assertEquals(0, getContainer().childElementCount, "No content expected before a composition")
        var show by mutableStateOf(true)

        var recompositions = 0
        createComposeWindow {
            if (show) {
                Text("Test", modifier = Modifier.testTag("testText"))
            }
            recompositions++
        }

        awaitA11YChanges()

        val textEl = getShadowRoot().getElementById("testText") as HTMLElement
        assertEquals("Test", textEl.innerText)

        realDelay(1200)

        assertTrue(textEl.isConnected, "textEl must be connected")

        recompositions = 0
        repeat(10) {
            show = !show
            awaitAnimationFrame()
            assertTrue(textEl.isConnected, "textEl must be connected despite value changes - debounce expected. (Iteration $it)")
        }

        assertTrue(recompositions > 0, "The state has been changing, but no recompositions?")

        show = false
        awaitA11YChanges()

        assertFalse(textEl.isConnected, "textEl must be disconnected after debounce ended")
    }

    @Test
    fun clickListenerMustBeUpdated() = runApplicationTest {
        assertEquals(0, getContainer().childElementCount, "No content expected before a composition")
        var clickCounter1 = 0
        var clickCounter2 = 0

        var clickListenerSwitch by mutableStateOf(true)

        createComposeWindow {
            Button(
                onClick = if (clickListenerSwitch) {
                { clickCounter1++ }
            } else {
                { clickCounter2++ }
            }) {
                Text("Click me")
            }
        }

        awaitA11YChanges()

        val a11yContainer = getA11YContainer()!!
        val button = a11yContainer.children[0]!!.children[0] as HTMLElement
        assertEquals("button", button.getAttribute("role"))
        assertEquals("Click me", button.innerText)

        button.click()
        assertEquals(1, clickCounter1)
        assertEquals(0, clickCounter2)

        clickListenerSwitch = false
        awaitIdle()

        button.click()
        assertEquals(1, clickCounter1)
        assertEquals(1, clickCounter2)

        button.click()
        assertEquals(1, clickCounter1)
        assertEquals(2, clickCounter2)

        clickListenerSwitch = true
        awaitIdle()

        button.click()
        assertEquals(2, clickCounter1)
        assertEquals(2, clickCounter2)
    }

    @Test
    fun noA11YRootAndElementsWithDisabledA11Y() = runApplicationTest {
        assertEquals(0, getContainer().childElementCount, "No content expected before a composition")
        createComposeWindow(
            configure = { isA11YEnabled = false }
        ) {
            Button(onClick = {}) {
                Text("Button1")
            }
        }
        assertNull(getA11YContainer())

        // Wait a bit and make sure the a11y root didn't appear
        realDelay(1200)
        assertNull(getA11YContainer())

        assertTrue(getCanvas().isConnected)
        val appContainer = getCanvas().parentElement as HTMLElement

        assertTrue(appContainer.isConnected)
        assertEquals(1, appContainer.children.length)
        assertEquals(getCanvas(), appContainer.children[0])
    }

    @Test
    fun modifierTestTagIsSetToId() = runApplicationTest {
        assertEquals(0, getContainer().childElementCount, "No content expected before a composition")
        var showButton by mutableStateOf(true)
        var clickCounter = 0

        createComposeWindow {
            if (showButton) {
                Button(
                    onClick = {
                        clickCounter++
                    },
                    modifier = Modifier.testTag("buttonTag")
                ) {
                    Text("Button with a test tag")
                }
            }
        }

        awaitA11YChanges()

        val button = getShadowRoot().getElementById("buttonTag") as? HTMLElement
        assertNotNull(button)
        button.click()
        assertEquals(1, clickCounter)

        showButton = false
        awaitA11YChanges()

        assertNull(getShadowRoot().getElementById("buttonTag"))
    }

    @Test
    fun textFieldHasTextBoxRole() = runApplicationTest {
        assertEquals(0, getContainer().childElementCount, "No content expected before a composition")
        var text by mutableStateOf("Hello, World!")

        createComposeWindow {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.testTag("textFieldTag")
            )
        }

        awaitA11YChanges()

        val textField = getShadowRoot().getElementById("textFieldTag") as? HTMLElement
        assertNotNull(textField)

        assertEquals("textbox", textField.getAttribute("role"))
        assertEquals("Hello, World!", textField.innerText)
    }

    @Test // A reproducer for https://youtrack.jetbrains.com/issue/CMP-10226
    fun nodeReappearsAfterSemanticsModifierToggle() = runApplicationTest {
        // A LayoutNode that gains/loses a semantic modifier alternates between
        // being a semantic node (in the a11y tree with its own semanticsId)
        // and being "transparent" (fillOneLayerOfSemanticsWrappers recurses
        // through it). The same semanticsId reappears when the modifier is re-added.
        var addSemantics by mutableStateOf(true)

        createComposeWindow {
            Column(
                modifier = if (addSemantics) Modifier.testTag("myColumn") else Modifier
            ) {
                Text("ChildText")
            }
        }

        awaitA11YChanges()
        val a11yContainer = getA11YContainer()!!
        assertTrue(a11yContainer.innerHTML.contains("ChildText"))
        assertNotNull(getShadowRoot().getElementById("myColumn"))

        // Remove semantic modifier: Column becomes transparent in the semantic tree
        addSemantics = false
        awaitA11YChanges()
        assertNull(getShadowRoot().getElementById("myColumn"))

        // Re-add semantic modifier: same LayoutNode, same semanticsId reappears.
        // Without proper cleanup of tracking maps, this would crash with "Node $id not found".
        addSemantics = true
        awaitA11YChanges()
        assertNotNull(getShadowRoot().getElementById("myColumn"))
        assertTrue(a11yContainer.innerHTML.contains("ChildText"))
    }

    @Test // Reproduces the user-reported scenario from CMP-10226
    fun spacerReappearsAfterGraphicsLayerClipToggle() = runApplicationTest {
        // graphicsLayer sets Shape semantics ONLY when clip=true
        // When the clip modifier is conditionally applied, the Spacer's only semantic property
        // (Shape) appears and disappears, causing the Spacer to enter/leave the semantic tree
        // while keeping the same LayoutNode and semanticsId.
        var enableClip by mutableStateOf(true)

        createComposeWindow {
            // Conditionally add/remove the entire graphicsLayer modifier so
            // the modifier chain rebuild triggers semantics invalidation.
            Spacer(
                modifier = if (enableClip)
                    Modifier.graphicsLayer(clip = true, shape = RectangleShape)
                else Modifier
            )
            Text("Sibling")
        }

        awaitA11YChanges()
        val a11yContainer = getA11YContainer()!!
        assertTrue(a11yContainer.innerHTML.contains("Sibling"))

        // Remove clip modifier: Spacer loses Shape semantic → leaves the semantic tree
        enableClip = false
        awaitA11YChanges()

        // Re-add clip modifier: same LayoutNode, same semanticsId reappears with Shape semantic.
        // Without proper cleanup of tracking maps, this would crash with "Node $id not found".
        enableClip = true
        awaitA11YChanges()
        assertTrue(a11yContainer.innerHTML.contains("Sibling"))
    }
}