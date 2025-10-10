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

package androidx.compose.ui.platform.a11y

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.currentTimeMillis
import androidx.compose.ui.platform.testTag
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.get

class CfWA11YTest : OnCanvasTests {

    @Test
    fun a11yButtonClick() = runApplicationTest {
        var clickCounter = 0

        createComposeWindow {
            Button(onClick = {
                clickCounter++
            }) {
                Text("Button1")
            }
        }

        awaitIdle()

        val a11yContainer = getA11YContainer()
        assertNotNull(a11yContainer)

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

        awaitIdle()

        val a11yContainer = getA11YContainer()
        assertNotNull(a11yContainer)

        awaitA11YChanges()

        val buttonsContainer = a11yContainer.children[0] as HTMLDivElement
        assertEquals(1, buttonsContainer.children.length)

        val button1 = buttonsContainer.children[0] as HTMLElement
        assertEquals("button", button1.getAttribute("role"))
        assertEquals("Button1", button1.innerText)

        showButton2 = true
        awaitIdle()
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
        awaitIdle()
        awaitA11YChanges()

        assertEquals(1, buttonsContainer.children.length)
        assertFalse(button2.isConnected)
    }

    @Test
    fun orderOfElements() = runApplicationTest {
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

        awaitIdle()

        val a11yContainer = getA11YContainer()
        assertNotNull(a11yContainer)

        awaitA11YChanges()

        val buttonsContainer = a11yContainer.children[0] as HTMLDivElement
        assertEquals(1, buttonsContainer.children.length)

        show2 = true
        show3 = true

        awaitIdle()
        awaitA11YChanges()

        assertEquals(3, buttonsContainer.children.length)

        assertEquals("Button1", buttonsContainer.children[0]!!.innerHTML)
        assertEquals("Button2", buttonsContainer.children[1]!!.innerHTML)
        assertEquals("Button3", buttonsContainer.children[2]!!.innerHTML)

        show1 = false
        awaitIdle()
        awaitA11YChanges()

        assertEquals(2, buttonsContainer.children.length)
        assertEquals("Button2", buttonsContainer.children[0]!!.innerHTML)
        assertEquals("Button3", buttonsContainer.children[1]!!.innerHTML)

        show1 = true
        awaitIdle()
        awaitA11YChanges()

        assertEquals(3, buttonsContainer.children.length)
        assertEquals("Button1", buttonsContainer.children[0]!!.innerHTML)
        assertEquals("Button2", buttonsContainer.children[1]!!.innerHTML)
        assertEquals("Button3", buttonsContainer.children[2]!!.innerHTML)
    }

    @Ignore // Sometimes fails on latest firefox FIXME: https://youtrack.jetbrains.com/issue/CMP-8955
    @Test
    fun changesMustBeBatched() = runApplicationTest {
        var show1 by mutableStateOf(true)

        createComposeWindow {
            if (show1) {
                Button(onClick = {}) {
                    Text("Text in Button")
                }
            }
        }

        awaitIdle()
        val a11yContainer = getA11YContainer()!!

        assertEquals("",a11yContainer.innerHTML)
        assertEquals(0,a11yContainer.childElementCount)

        suspend fun realDelay(timeMs: Long) {
            withContext(Dispatchers.Default) {
                delay(timeMs)
            }
        }

        repeat(20) {
            show1 = !show1
            realDelay(10)

            // No changes expected yet due to debounce
            assertEquals("",a11yContainer.innerHTML)
            assertEquals(0,a11yContainer.childElementCount)
        }

        val startTime = currentTimeMillis()
        awaitA11YChanges()
        val waitedForChangesMs = currentTimeMillis() - startTime

        val buttonsContainer = a11yContainer.children[0] as HTMLDivElement
        assertEquals(1, buttonsContainer.children.length)

        (buttonsContainer.children[0] as HTMLElement).let { button ->
            assertEquals("button", button.getAttribute("role"))
            assertEquals("Text in Button", button.innerHTML)
        }

        // The tolerance is quite large, but it's so to reduce the flakiness.
        // The idea is that the change is not expected to happen immediately, but with debounce.
        assertTrue(waitedForChangesMs in 50..150, "Changes must be batched, waited for $waitedForChangesMs ms. Allowed tolerance 50ms was exceeded")
    }

    @Ignore // Sometimes fails on latest firefox FIXME: https://youtrack.jetbrains.com/issue/CMP-8955
    @Test
    fun changesMustBeAppliedDespiteConstantDebounceAfter1Second() = runApplicationTest {
        var show1 by mutableStateOf(true)
        var startTime = 0L

        createComposeWindow {
            if (show1) {
                Button(onClick = {}) {
                    Text("Text in Button")
                }
            }

            DisposableEffect(Unit) {
                startTime = currentTimeMillis()
                onDispose {  }
            }
        }

        awaitIdle()
        assertNotEquals(0L, startTime, "The start time must be set")

        val a11yContainer = getA11YContainer()!!

        assertEquals("",a11yContainer.innerHTML)
        assertEquals(0,a11yContainer.childElementCount)

        suspend fun realDelay(timeMs: Long) {
            withContext(Dispatchers.Default) {
                delay(timeMs)
            }
        }

        var changesAppliedTime = 0L

        launch {
            awaitA11YChanges()
            changesAppliedTime = currentTimeMillis()
        }

        var debounceCounter = 0

        repeat(20) {
            // Change the state every 55ms. Such changes must be "debounced", (time delta is less than 100ms)
            show1 = !show1
            realDelay(55)

            if (changesAppliedTime == 0L) {
                // No changes expected yet due to debounce
                assertEquals(0, a11yContainer.childElementCount)
                debounceCounter++
            }
        }

        // To avoid flakiness, we make just a sanity check. The expected value is ~18
        assertTrue(debounceCounter > 1)

        // the "debounce" must be ignored when the changes were waiting for 1 second
        assertEquals(1, a11yContainer.childElementCount)

        // Adding a tolerance of 200ms, just to avoid flakiness
        assertTrue(
            changesAppliedTime - startTime in (1000..1200),
            "Changes must be applied after 1 second, waited for ${changesAppliedTime - startTime} ms"
        )
    }

    @Ignore // Sometimes fails on latest firefox FIXME: https://youtrack.jetbrains.com/issue/CMP-8955
    @Test
    fun noChangesFor1SecondTheDebounceShouldWork() = runApplicationTest {
        var show by mutableStateOf(true)

        createComposeWindow {
            if (show) {
                Text("Test", modifier = Modifier.testTag("testText"))
            }
        }

        awaitIdle()
        awaitA11YChanges()

        val textEl = getShadowRoot().getElementById("testText") as HTMLElement
        assertEquals("Test", textEl.innerText)


        suspend fun realDelay(timeMs: Long) {
            withContext(Dispatchers.Default) {
                delay(timeMs)
            }
        }

        realDelay(1200)

        assertTrue(textEl.isConnected)

        repeat(10) {
            show = !show
            realDelay(10)
            assertTrue(textEl.isConnected)
        }

        show = false
        awaitA11YChanges()

        assertFalse(textEl.isConnected)
    }

    @Test
    fun clickListenerMustBeUpdated() = runApplicationTest {
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


        awaitIdle()
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
        createComposeWindow(
            configure = { isA11YEnabled = false }
        ) {
            Button(onClick = {}) {
                Text("Button1")
            }
        }

        awaitIdle()
        assertNull(getA11YContainer())

        suspend fun realDelay(timeMs: Long) {
            withContext(Dispatchers.Default) {
                delay(timeMs)
            }
        }
        // Wait a bit and make sure the a11y root didn't appear
        realDelay(500)
        assertNull(getA11YContainer())

        assertTrue(getCanvas().isConnected)
        val appContainer = getCanvas().parentElement as HTMLElement

        assertTrue(appContainer.isConnected)
        assertEquals(2, appContainer.children.length)
        assertEquals(getCanvas(), appContainer.children[0])
        assertEquals("DIV", appContainer.children[1]!!.tagName) // interop container
    }

    @Test
    fun modifierTestTagIsSetToId() = runApplicationTest {
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

        awaitIdle()
        awaitA11YChanges()

        val button = getShadowRoot().getElementById("buttonTag") as? HTMLElement
        assertNotNull(button)
        button.click()
        assertEquals(1, clickCounter)

        showButton = false
        awaitIdle()
        awaitA11YChanges()

        assertNull(getShadowRoot().getElementById("buttonTag"))
    }

    @Test
    fun textFieldHasTextBoxRole() = runApplicationTest {
        var text by mutableStateOf("Hello, World!")

        createComposeWindow {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.testTag("textFieldTag")
            )
        }

        awaitIdle()
        awaitA11YChanges()

        val textField = getShadowRoot().getElementById("textFieldTag") as? HTMLElement
        assertNotNull(textField)

        assertEquals("textbox", textField.getAttribute("role"))
        assertEquals("Hello, World!", textField.innerText)
    }
}