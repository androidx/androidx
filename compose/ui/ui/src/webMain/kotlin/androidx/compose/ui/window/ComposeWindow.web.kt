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
import kotlinx.dom.clear
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
 * <container>
 *   <positioning_container>
 *     <shadow_container.shadow>
 *       <style/>
 *       <app_container>
 *         <canvas/>
 *         <a11y_container/>
 *       </app_container>
 *     </shadow_container>
 *     <interopContainer/>
 *   <positioning_container/>
 * </container>
 *
 * Note: The viewportContainer will be cleared on composition creation.
 */
@ExperimentalComposeUiApi
fun ComposeViewport(
    viewportContainer: Element,
    configure: ComposeViewportConfiguration.() -> Unit = {},
    content: @Composable () -> Unit = { }
) = onSkikoReady {
    viewportContainer.clear()

    // Create a common positioning container (parent html element) for shadow and the interop containers
    // to position at the same place - the interop container is position at 0,0 relative to the shadow.
    // It simplifies the positioning of the interop views in the container.
    val positioningContainer = document.createElement("div") as HTMLDivElement
    positioningContainer.style.apply {
        position = "relative"
    }
    viewportContainer.appendChild(positioningContainer)

    //shadow container
    val shadowContainer = document.createElement("div") as HTMLDivElement
    shadowContainer.style.apply {
        position = "relative"
    }
    positioningContainer.appendChild(shadowContainer)

    //shadow
    val shadowRoot = shadowContainer.attachShadow(ShadowRootInit(ShadowRootMode.OPEN))
    val shadowRootStyle = document.createElement("style")
    shadowRootStyle.textContent = """
        :host {
            -webkit-touch-callout: none; 
            -webkit-user-select: none; 
            user-select: none;
            
            position: relative;
            padding: 0;
        }
        
        canvas {
               display: block;
               width: 100%;
               height: 100%;
        }
    """.trimIndent()
    shadowRoot.appendChild(shadowRootStyle)

    //app container (canvas + a11y)
    val appContainer = document.createElement("div") as HTMLElement
    appContainer.style.apply {
        position = "relative"
    }
    shadowRoot.appendChild(appContainer)

    //canvas
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.setAttribute("tabindex", "0")
    canvas.setAttribute("role", "generic")
    canvas.style.outline = "none" // Fixes https://youtrack.jetbrains.com/issue/CMP-9040
    canvas.style.setProperty("touch-action", "none") //blocks default browser touch handling
    appContainer.appendChild(canvas)

    //a11y container
    val configuration = ComposeViewportConfiguration().apply(configure)
    val a11yContainerElement = if (configuration.isA11YEnabled) {
        (document.createElement("div") as HTMLDivElement).also { a11yContainer ->
            a11yContainer.style.apply {
                position = "absolute"
                top = "0"
                left = "0"
            }
            appContainer.appendChild(a11yContainer)
        }
    } else {
        null
    }

    //interop container
    val interopContainerElement = document.createElement("div") as HTMLDivElement
    interopContainerElement.style.apply {
        position = "absolute"
        top = "0"
        left = "0"
    }
    positioningContainer.appendChild(interopContainerElement)

    ComposeWindow(
        canvas = canvas,
        rootNode = shadowRoot,
        layerRoot = appContainer,
        interopContainerElement = interopContainerElement,
        a11yContainerElement = a11yContainerElement,
        content = content,
        configuration = configuration,
        state = DefaultWindowState(viewportContainer)
    )
}