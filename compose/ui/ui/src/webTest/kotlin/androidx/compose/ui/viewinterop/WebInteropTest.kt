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

package androidx.compose.ui.viewinterop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.get

class WebInteropTest : OnCanvasTests {

    @Test
    fun testSimpleDivWithTextState() = runApplicationTest {
        val textState = mutableStateOf("Text1")

        val showDiv = mutableStateOf(false)

        val divId = "divWithText"

        createComposeWindow {
            if (!showDiv.value) return@createComposeWindow
            HtmlElementView(
                modifier = Modifier.size(50.dp),
                factory = {
                    (document.createElement("div") as HTMLDivElement).apply {
                        innerText = textState.value
                        id = divId
                    }
                },
                update = {
                    it.innerText = textState.value
                }
            )
        }


        var div = getShadowRoot().getElementById(divId) as HTMLDivElement?
        assertNull(div)

        showDiv.value = true
        awaitIdle()

        div = getShadowRoot().getElementById(divId) as HTMLDivElement?
        assertNotNull(div)
        assertTrue(div.isConnected)
        assertEquals("Text1", div.innerText)

        textState.value = "Text2"
        awaitIdle()

        assertEquals("Text2", div.innerText)

        showDiv.value = false
        awaitIdle()

        assertFalse(div.isConnected)
    }

    @Test
    fun interopOrder() = runApplicationTest {

        var red by mutableStateOf(true)
        var green by mutableStateOf(true)
        var blue by mutableStateOf(true)

        var interopContainer: InteropContainer? = null
        createComposeWindow {
            interopContainer = LocalInteropContainer.current
            Box {
                if (red) {
                    TestInteropView(
                        Modifier.size(150.dp).offset(0.dp, 0.dp), "red"
                    )
                }
                if (green) {
                    TestInteropView(
                        Modifier.size(150.dp).offset(75.dp, 75.dp), "green"
                    )
                }
                if (blue) {
                    TestInteropView(
                        Modifier.size(150.dp).offset(150.dp, 150.dp), "blue"
                    )
                }
            }
        }

        awaitIdle()
        assertNotNull(interopContainer)

        val interopRoot = interopContainer.root.htmlElement
        assertTrue(interopRoot.isConnected)

        var redDiv =
            (interopRoot.children.get(0) as HTMLDivElement).children.get(0) as HTMLDivElement
        var greenDiv =
            (interopRoot.children.get(1) as HTMLDivElement).children.get(0) as HTMLDivElement
        var blueDiv =
            (interopRoot.children.get(2) as HTMLDivElement).children.get(0) as HTMLDivElement

        assertEquals("red", redDiv.id)
        assertEquals("green", greenDiv.id)
        assertEquals("blue", blueDiv.id)

        red = false
        awaitIdle()

        assertFalse(redDiv.isConnected)
        assertTrue(greenDiv.isConnected)
        assertTrue(blueDiv.isConnected)

        greenDiv =
            (interopRoot.children.get(0) as HTMLDivElement).children.get(0) as HTMLDivElement
        blueDiv =
            (interopRoot.children.get(1) as HTMLDivElement).children.get(0) as HTMLDivElement

        assertEquals("green", greenDiv.id)
        assertEquals("blue", blueDiv.id)

        green = false
        awaitIdle()

        assertFalse(redDiv.isConnected)
        assertFalse(greenDiv.isConnected)
        assertTrue(blueDiv.isConnected)

        blueDiv =
            (interopRoot.children.get(0) as HTMLDivElement).children.get(0) as HTMLDivElement
        assertEquals("blue", blueDiv.id)

        blue = false
        awaitIdle()
        assertFalse(blueDiv.isConnected)
        assertTrue(interopRoot.children.length == 0)
    }

    @Test
    fun hitPath() = runApplicationTest {
        createComposeWindow {
            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                TestInteropView(Modifier.size(30.dp), "div")
            }
        }
        awaitIdle()

        assertEquals("CANVAS", getShadowRoot().elementFromPoint(10.0, 10.0).tagName)
        assertEquals("DIV", getShadowRoot().elementFromPoint(50.0, 50.0).tagName)
        assertEquals("CANVAS", getShadowRoot().elementFromPoint(90.0, 90.0).tagName)
    }
}

@Composable
internal fun TestInteropView(modifier: Modifier, id: String) {
    HtmlElementView(
        modifier = modifier,
        factory = {
            (document.createElement("div") as HTMLDivElement).apply {
                this.id = id
                this.innerText = id
                this.style.apply {
                    backgroundColor = "gray"
                }
            }
        }
    )
}
