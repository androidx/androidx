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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.OPEN
import org.w3c.dom.ShadowRootInit
import org.w3c.dom.ShadowRootMode

/**
 * EXPERIMENTAL! Might be deleted or changed in the future!
 *
 * Creates the composition in HTML canvas created in parent container identified by [viewportContainerId] id.
 * This size of canvas is adjusted with the size of the container
 *
 * @param viewportContainerId - The id of an HTML element which would host the Compose Viewport.
 * If it's null, then `<body>` will be used as a container.
 * @param configure - A lambda for Compose Viewport configuration.
 * See [ComposeViewportConfiguration] for available options.
 * @param content - The Composable content to be rendered on the `<canvas>` element.
 */
@ExperimentalComposeUiApi
fun ComposeViewport(
    viewportContainerId: String? = null,
    configure: ComposeViewportConfiguration.() -> Unit = {},
    content: @Composable () -> Unit = { }
) {
    onDomReady {
        val providedContainer = if (viewportContainerId != null) {
            document.getElementById(viewportContainerId) ?: error("failed to find element by viewportContainerId: '$viewportContainerId'")
        } else {
            document.body ?: error("failed to find <body> element")
        }

        ComposeViewport(providedContainer, configure, content)
    }
}

/**
 * EXPERIMENTAL! Might be deleted or changed in the future!
 *
 * Creates the composition in HTML canvas created in parent container identified by [viewportContainer] Element.
 * This size of canvas is adjusted with the size of the container
 *
 * The current hierarchy:
 * <viewportContainer.shadowDom>
 *     <app root>
 *         <canvas/>
 *         <interop elements container/>
 *         <a11y elements root/>
 *     </app root>
 * </viewportContainer.shadowDom>
 */
@ExperimentalComposeUiApi
fun ComposeViewport(
    viewportContainer: Element,
    configure: ComposeViewportConfiguration.() -> Unit = {},
    content: @Composable () -> Unit = { }
) = onSkikoReady {
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.setAttribute("tabindex", "0")
    canvas.setAttribute("role", "generic")
    canvas.style.outline = "none" // Fixes https://youtrack.jetbrains.com/issue/CMP-9040

    // Create a common container (parent html element) for canvas and the interop container
    // to position at the same place - the interop container is position at 0,0 relative to <canvas>.
    // It simplifies the positioning of the interop views in the container.
    val layerRoot = document.createElement("div") as HTMLElement
    layerRoot.style.apply {
        position = "relative"
    }

    val shadowRoot = viewportContainer.attachShadow(ShadowRootInit(ShadowRootMode.OPEN))
    val shadowRootStyle = document.createElement("style")
    shadowRootStyle.textContent = """
        :host {
            -webkit-touch-callout: none; 
            -webkit-user-select: none; 
            user-select: none;
            
            position: relative;
        }
    """.trimIndent()

    shadowRoot.append(shadowRootStyle, layerRoot)
    layerRoot.appendChild(canvas)

    val interopContainerElement = document.createElement("div") as HTMLDivElement
    layerRoot.appendChild(interopContainerElement)

    interopContainerElement.style.apply {
        position = "absolute"
        top = "0"
        left = "0"
    }

    val configuration = ComposeViewportConfiguration().apply(configure)

    val a11yContainerElement = if (configuration.isA11YEnabled) {
        (document.createElement("div") as HTMLDivElement).also { a11yContainer ->
            layerRoot.appendChild(a11yContainer)
            a11yContainer.style.apply {
                position = "absolute"
                top = "0"
                left = "0"
            }
        }
    } else {
        null
    }

    ComposeWindow(
        canvas = canvas,
        rootNode = shadowRoot,
        interopContainerElement = interopContainerElement,
        a11yContainerElement = a11yContainerElement,
        content = content,
        configuration = configuration,
        state = DefaultWindowState(viewportContainer)
    )
}