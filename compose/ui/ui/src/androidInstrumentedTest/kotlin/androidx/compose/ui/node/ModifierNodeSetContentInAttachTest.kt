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

package androidx.compose.ui.node

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroupOverlay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.DisposableHandle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ModifierNodeSetContentInAttachTest {
    @get:Rule val rule = createComposeRule()

    /**
     * This test ends up calling setContent() of a separate composition during a modifier update in
     * one composition, which ends up causing a _different_ NodeChain from updating inside of this
     * NodeChain's update. This is a valid thing to do and this test ensures that it does not fail.
     *
     * b/374907972
     */
    @Test
    fun setContentInAttach() {
        var isDragging by mutableStateOf(false)

        rule.setContent {
            Box(
                modifier =
                    Modifier.padding(10.dp)
                        .then(
                            if (isDragging) {
                                Modifier.drawInOverlay()
                            } else Modifier
                        )
            )
        }

        rule.runOnIdle { isDragging = true }

        rule.runOnIdle { isDragging = false }

        rule.waitForIdle()
    }
}

@Composable
private fun Modifier.drawInOverlay(): Modifier {
    return drawInOverlay(rememberCompositionContext())
}

private fun Modifier.drawInOverlay(compositionContext: CompositionContext): Modifier {
    return this.then(DrawInOverlayElement(compositionContext))
}

private fun Modifier.container(state: ContainerState): Modifier {
    return layout { measurable, constraints ->
            val p = measurable.measure(constraints)
            layout(p.width, p.height) {
                val coords = coordinates
                if (coords != null && !isLookingAhead) {
                    state.lastCoords = coords
                }

                p.place(0, 0)
            }
        }
        .drawWithContent {
            drawContent()
            state.drawInOverlay(this)
        }
}

private fun Modifier.drawInContainer(
    containerState: ContainerState,
    enabled: () -> Boolean = { true },
    zIndex: Float = 0f,
    clipPath: (LayoutDirection, Density) -> Path? = { _, _ -> null },
): Modifier {
    return this.then(
        DrawInContainerElementNode(
            containerState = containerState,
            enabled = enabled,
            zIndex = zIndex,
            clipPath = clipPath,
        )
    )
}

private data class DrawInOverlayElement(val compositionContext: CompositionContext) :
    ModifierNodeElement<DrawInOverlayNode>() {
    override fun create(): DrawInOverlayNode = DrawInOverlayNode(compositionContext)

    override fun update(node: DrawInOverlayNode) {
        node.compositionContext = compositionContext
    }
}

private class DrawInOverlayNode(compositionContext: CompositionContext) :
    DelegatingNode(), DrawModifierNode, ObserverModifierNode, CompositionLocalConsumerModifierNode {
    private val containerState = ContainerState()
    private val drawInContainerNode = delegate(DrawInContainerNode(containerState))
    var compositionContext = compositionContext
        set(value) {
            if (value != field) {
                field = value

                if (isAttached) {
                    recreateView(checkNotNull(lastContext), checkNotNull(lastLocalView))
                }
            }
        }

    private var lastContext: Context? = null
    private var lastLocalView: View? = null
    private var lastDisposable: DisposableHandle? = null

    override fun ContentDrawScope.draw() {
        with(drawInContainerNode) { this@draw.draw() }
    }

    override fun onAttach() {
        maybeRecreateView()
    }

    override fun onDetach() {
        removeCurrentViewFromOverlay()
    }

    override fun onObservedReadsChanged() {
        maybeRecreateView()
    }

    private fun maybeRecreateView() {
        if (!isAttached) {
            return
        }

        val context = currentValueOf(LocalContext)
        val localView = currentValueOf(LocalView)
        if (context == lastContext && localView == lastLocalView) {
            return
        }

        lastContext = context
        lastLocalView = localView

        recreateView(context, localView)
    }

    private fun recreateView(context: Context, localView: View) {
        removeCurrentViewFromOverlay()
        lastDisposable = addViewToOverlay(context, localView)
    }

    private fun removeCurrentViewFromOverlay() {
        lastDisposable?.dispose()
        lastDisposable = null
    }

    private fun addViewToOverlay(context: Context, localView: View): DisposableHandle {
        val view =
            ComposeView(context).apply {
                setParentCompositionContext(compositionContext)

                // Set the owners.
                setViewTreeLifecycleOwner(localView.findViewTreeLifecycleOwner())
                setViewTreeViewModelStoreOwner(localView.findViewTreeViewModelStoreOwner())
                setViewTreeSavedStateRegistryOwner(localView.findViewTreeSavedStateRegistryOwner())

                setContent { Box(Modifier.fillMaxSize().container(containerState)) }
            }

        val overlay = localView.rootView.overlay as ViewGroupOverlay
        overlay.add(view)

        // Make the ComposeView as big as the overlay.
        val viewParent = view.parent as ViewGroup
        val size = IntSize(viewParent.width, viewParent.height)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(size.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(size.height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, size.width, size.height)

        return DisposableHandle { overlay.remove(view) }
    }
}

private class ContainerState {
    internal var renderers = mutableStateListOf<LayerRenderer>()
    internal var lastCoords: LayoutCoordinates? = null

    internal fun onLayerRendererAttached(renderer: LayerRenderer) {
        renderers.add(renderer)
        renderers.sortBy { it.zIndex }
    }

    internal fun onLayerRendererDetached(renderer: LayerRenderer) {
        renderers.remove(renderer)
    }

    internal fun drawInOverlay(drawScope: DrawScope) {
        renderers.fastForEach { it.drawInOverlay(drawScope) }
    }
}

private interface LayerRenderer {
    val zIndex: Float

    fun drawInOverlay(drawScope: DrawScope)
}

private data class DrawInContainerElementNode(
    var containerState: ContainerState,
    var enabled: () -> Boolean,
    val zIndex: Float,
    val clipPath: (LayoutDirection, Density) -> Path?,
) : ModifierNodeElement<DrawInContainerNode>() {
    override fun create(): DrawInContainerNode {
        return DrawInContainerNode(containerState, enabled, zIndex, clipPath)
    }

    override fun update(node: DrawInContainerNode) {
        node.containerState = containerState
        node.enabled = enabled
        node.zIndex = zIndex
        node.clipPath = clipPath
    }
}

private class DrawInContainerNode(
    var containerState: ContainerState,
    var enabled: () -> Boolean = { true },
    zIndex: Float = 0f,
    var clipPath: (LayoutDirection, Density) -> Path? = { _, _ -> null },
) : Modifier.Node(), DrawModifierNode, ModifierLocalModifierNode {
    var zIndex by mutableFloatStateOf(zIndex)

    private inner class LayerWithRenderer(val layer: GraphicsLayer) : LayerRenderer {
        override val zIndex: Float
            get() = this@DrawInContainerNode.zIndex

        override fun drawInOverlay(drawScope: DrawScope) {
            if (enabled()) {
                with(drawScope) {
                    val containerCoords =
                        checkNotNull(containerState.lastCoords) { "container is not placed" }
                    val (x, y) =
                        requireLayoutCoordinates().positionInWindow() -
                            containerCoords.positionInWindow()
                    val clipPath = clipPath(layoutDirection, requireDensity())
                    if (clipPath != null) {
                        clipPath(clipPath) { translate(x, y) { drawLayer(layer) } }
                    } else {
                        translate(x, y) { drawLayer(layer) }
                    }
                }
            }
        }
    }

    // Render in-place logic. Depending on the result of `renderInOverlay()`, the content will
    // either render in-place or in the overlay, but never in both places.
    override fun ContentDrawScope.draw() {
        val layer = requireNotNull(layer) { "Error: layer never initialized" }
        layer.record { this@draw.drawContent() }
        if (!enabled()) {
            drawLayer(layer)
        }
    }

    val layer: GraphicsLayer?
        get() = layerWithRenderer?.layer

    private var layerWithRenderer: LayerWithRenderer? = null

    override fun onAttach() {
        LayerWithRenderer(requireGraphicsContext().createGraphicsLayer()).let {
            containerState.onLayerRendererAttached(it)
            layerWithRenderer = it
        }
    }

    override fun onDetach() {
        layerWithRenderer?.let {
            containerState.onLayerRendererDetached(it)
            requireGraphicsContext().releaseGraphicsLayer(it.layer)
        }
    }
}
