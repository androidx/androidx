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

package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.InteropContainer
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.TrackInteropContainer
import androidx.compose.ui.node.TrackInteropModifierElement
import androidx.compose.ui.node.TrackInteropModifierNode
import androidx.compose.ui.node.countInteropComponentsBefore
import androidx.compose.ui.scene.ComposeSceneMediator
import java.awt.Component
import java.awt.Container

/**
 * Providing interop container as composition local, so [SwingPanel] can use it to add
 * native views to the hierarchy.
 */
internal val LocalSwingInteropContainer = staticCompositionLocalOf<SwingInteropContainer> {
    error("LocalSwingInteropContainer not provided")
}

/**
 * A container that controls interop views/components.
 *
 * It receives [container] native view to use it as parent for all interop views. It should be
 * the same component that is used in [ComposeSceneMediator] to avoid issues with transparency.
 *
 * @property container The Swing container to add the interop views to.
 * @property placeInteropAbove Whether to place interop components above non-interop components.
 */
internal class SwingInteropContainer(
    val container: Container,
    private val placeInteropAbove: Boolean
): InteropContainer<Component> {
    /**
     * Represents the count of interop components in [container].
     *
     * This variable is required to add interop components to right indexes independently of
     * already existing children of [container].
     *
     * @see SwingInteropContainer.addInteropView
     * @see SwingInteropContainer.removeInteropView
     */
    private var interopComponentsCount = 0

    override var rootModifier: TrackInteropModifierNode<Component>? = null

    override fun addInteropView(nativeView: Component) {
        val nonInteropComponents = container.componentCount - interopComponentsCount
        // AWT uses the reverse order for drawing and events, so index = size - count
        val index = interopComponentsCount - countInteropComponentsBefore(nativeView)
        container.add(nativeView, if (placeInteropAbove) {
            index
        } else {
            index + nonInteropComponents
        })
        interopComponentsCount++

        // Sometimes Swing displays the rest of interop views in incorrect order after removing,
        // so we need to force re-validate it.
        container.validate()
        container.repaint()
    }

    override fun removeInteropView(nativeView: Component) {
        interopComponentsCount--
        container.remove(nativeView)

        // Sometimes Swing displays the rest of interop views in incorrect order after removing,
        // so we need to force re-validate it.
        container.validate()
        container.repaint()
    }

    @Composable
    operator fun invoke(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalSwingInteropContainer provides this,
        ) {
            TrackInteropContainer(
                container = container,
                content = content
            )
        }
    }
}

/**
 * Modifier to track interop component inside [LayoutNode] hierarchy.
 *
 * @param component The Swing component that matches the current node.
 */
internal fun Modifier.trackSwingInterop(
    component: Component
): Modifier = this then TrackInteropModifierElement(
    nativeView = component
)
