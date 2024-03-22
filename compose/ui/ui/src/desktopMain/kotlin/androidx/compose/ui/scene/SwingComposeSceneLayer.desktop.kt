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

package androidx.compose.ui.scene

import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.awt.toAwtColor
import androidx.compose.ui.awt.toAwtRectangle
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.scene.skia.SwingSkiaLayerComponent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.window.density
import androidx.compose.ui.window.sizeInPx
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import org.jetbrains.skiko.SkiaLayerAnalytics

internal class SwingComposeSceneLayer(
    composeContainer: ComposeContainer,
    private val skiaLayerAnalytics: SkiaLayerAnalytics,
    density: Density,
    layoutDirection: LayoutDirection,
    focusable: Boolean,
    compositionContext: CompositionContext
) : DesktopComposeSceneLayer(composeContainer, density, layoutDirection) {
    private val backgroundMouseListener = object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) = onMouseEventOutside(event)
        override fun mouseReleased(event: MouseEvent) = onMouseEventOutside(event)
    }

    private val container = object : JLayeredPane() {
        override fun addNotify() {
            super.addNotify()
            mediator?.onComponentAttached()
            onUpdateBounds()
        }

        override fun paint(g: Graphics) {
            g.color = background
            g.fillRect(0,0, width, height)

            // Draw content after background
            super.paint(g)
        }
    }.also {
        it.layout = null
        it.isFocusable = focusable
        it.isOpaque = false
        it.background = Color.Transparent.toAwtColor()
        it.size = Dimension(windowContainer.width, windowContainer.height)
        it.addMouseListener(backgroundMouseListener)
    }

    private var containerSize = IntSize.Zero
        set(value) {
            if (field.width != value.width || field.height != value.height) {
                field = value
                container.setBounds(0, 0, value.width, value.height)
                mediator?.contentComponent?.size = container.size
                mediator?.onChangeComponentSize()
            }
        }

    override var mediator: ComposeSceneMediator? = null

    override var focusable: Boolean = focusable
        set(value) {
            field = value
            container.isFocusable = value
        }

    override var scrimColor: Color? = null
        set(value) {
            field = value
            val background = value ?: Color.Transparent
            container.background = background.toAwtColor()
        }

    init {
        val boundsInPx = windowContainer.sizeInPx.toRect()
        drawBounds = boundsInPx.roundToIntRect()
        mediator = ComposeSceneMediator(
            container = container,
            windowContext = composeContainer.windowContext,
            exceptionHandler = {
                composeContainer.exceptionHandler?.onException(it) ?: throw it
            },
            eventListener = eventListener,
            measureDrawLayerBounds = true,
            coroutineContext = compositionContext.effectCoroutineContext,
            skiaLayerComponentFactory = ::createSkiaLayerComponent,
            composeSceneFactory = ::createComposeScene,
        ).also {
            it.onChangeWindowTransparency(true)
            it.contentComponent.size = container.size
        }

        // TODO: Currently it works only with offscreen rendering
        // TODO: Do not clip this from main scene if layersContainer == main container
        windowContainer.add(container, JLayeredPane.POPUP_LAYER, 0)

        composeContainer.attachLayer(this)
    }

    override fun close() {
        super.close()
        composeContainer.detachLayer(this)
        mediator?.dispose()
        mediator = null

        windowContainer.remove(container)
        windowContainer.invalidate()
        windowContainer.repaint()
    }

    override fun onChangeWindowSize() {
        containerSize = IntSize(windowContainer.width, windowContainer.height)
    }

    override fun onUpdateBounds() {
        val scaledRectangle = drawBounds.toAwtRectangle(density)
        val localBounds = SwingUtilities.convertRectangle(
            /* source = */ windowContainer,
            /* aRectangle = */ scaledRectangle,
            /* destination = */ container)
        mediator?.contentComponent?.bounds = localBounds
    }

    private fun createSkiaLayerComponent(mediator: ComposeSceneMediator): SkiaLayerComponent {
        val skikoView = recordDrawBounds(mediator)
        return SwingSkiaLayerComponent(
            mediator = mediator,
            skikoView = skikoView,
            skiaLayerAnalytics = skiaLayerAnalytics
        )
    }

    private fun createComposeScene(mediator: ComposeSceneMediator): ComposeScene {
        val density = container.density
        return SingleLayerComposeScene(
            coroutineContext = mediator.coroutineContext,
            density = density,
            invalidate = mediator::onComposeInvalidation,
            layoutDirection = layoutDirection,
            composeSceneContext = composeContainer.createComposeSceneContext(
                platformContext = mediator.platformContext
            ),
        )
    }
}
