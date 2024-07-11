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
import androidx.compose.ui.node.countInteropComponentsBelow
import androidx.compose.ui.scene.ComposeSceneMediator
import androidx.compose.ui.unit.IntRect
import java.awt.Component
import java.awt.Container
import org.jetbrains.skiko.ClipRectangle

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
): InteropContainer<InteropComponent> {
    /**
     * @see SwingInteropContainer.placeInteropView
     * @see SwingInteropContainer.removeInteropView
     */
    private var interopComponents = mutableMapOf<Component, InteropComponent>()

    override var rootModifier: TrackInteropModifierNode<InteropComponent>? = null
    override val interopViews: Set<InteropComponent>
        get() = interopComponents.values.toSet()

    /**
     * Index of last interop component in [container].
     *
     * [ComposeSceneMediator] might keep extra components in the same container.
     * So based on [placeInteropAbove] they should go below or under all interop views.
     *
     * @see ComposeSceneMediator.contentComponent
     * @see ComposeSceneMediator.invisibleComponent
     */
    private val lastInteropIndex: Int
        get() {
            var lastInteropIndex = interopComponents.size - 1
            if (!placeInteropAbove) {
                val nonInteropComponents = container.componentCount - interopComponents.size
                lastInteropIndex += nonInteropComponents
            }
            return lastInteropIndex
        }

    override fun placeInteropView(nativeView: InteropComponent) {
        val component = nativeView.container

        // Add this component to [interopComponents] to track count and clip rects
        val alreadyAdded = component in interopComponents
        if (!alreadyAdded) {
            interopComponents[component] = nativeView
        }

        // Iterate through a Compose layout tree in draw order and count interop view below this one
        val countBelow = countInteropComponentsBelow(nativeView)

        // AWT/Swing uses the **REVERSE ORDER** for drawing and events
        val awtIndex = lastInteropIndex - countBelow

        // Update AWT/Swing hierarchy
        if (alreadyAdded) {
            container.setComponentZOrder(component, awtIndex)
        } else {
            container.add(component, awtIndex)
        }

        // Sometimes Swing displays the rest of interop views in incorrect order after adding,
        // so we need to force re-validate it.
        container.validate()
        container.repaint()
    }

    override fun unplaceInteropView(nativeView: InteropComponent) {
        val component = nativeView.container
        container.remove(component)
        interopComponents.remove(component)

        // Sometimes Swing displays the rest of interop views in incorrect order after removing,
        // so we need to force re-validate it.
        container.validate()
        container.repaint()
    }

    fun validateComponentsOrder() {
        container.validate()
        container.repaint()
    }

    fun getClipRectForComponent(component: Component): ClipRectangle =
        requireNotNull(interopComponents[component])

    @Composable
    operator fun invoke(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalSwingInteropContainer provides this,
        ) {
            TrackInteropContainer(
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
    container: SwingInteropContainer,
    component: InteropComponent
): Modifier = this then TrackInteropModifierElement(
    container = container,
    nativeView = component
)

/**
 * Provides clipping bounds for skia canvas.
 *
 * @param container The container that holds the component.
 * @param clipBounds The rectangular region to clip skia canvas. It's relative to Compose root
 */
internal open class InteropComponent(
    val container: Container,
    protected var clipBounds: IntRect? = null
) : ClipRectangle {
    override val x: Float
        get() = (clipBounds?.left ?: container.x).toFloat()
    override val y: Float
        get() = (clipBounds?.top ?: container.y).toFloat()
    override val width: Float
        get() = (clipBounds?.width ?: container.width).toFloat()
    override val height: Float
        get() = (clipBounds?.height ?: container.height).toFloat()
}
