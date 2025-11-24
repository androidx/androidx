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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.LayerType
import androidx.compose.ui.awt.AwtEventFilter
import androidx.compose.ui.awt.AwtEventListener
import androidx.compose.ui.awt.AwtEventListeners
import androidx.compose.ui.awt.RenderSettings
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.DefaultArchitectureComponentsOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.skiko.OverlayRenderDecorator
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.density
import androidx.compose.ui.window.layoutDirectionFor
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.enableSavedStateHandles
import androidx.savedstate.SavedState
import java.awt.Component
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent as AwtKeyEvent
import java.awt.event.MouseEvent as AwtMouseEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.event.WindowListener
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.MainUIDispatcher
import org.jetbrains.skiko.SkiaLayerAnalytics

/**
 * Internal entry point to Compose.
 *
 * It binds Skia canvas and [ComposeScene] to [container].
 *
 * @property container A container for the [ComposeScene].
 * @param skiaLayerAnalytics The analytics for the Skia layer.
 * @param window The window ancestor of the [container].
 * @param windowContainer A container used for additional layers and as a reference
 *  for window coordinate space.
 * @param savedState The saved state to restore the UI state from a previous instance.
 * @param layerType The type of layer used for Popup/Dialog.
 * @param renderSettings The settings for rendering.
 */
internal class ComposeContainer(
    val container: JLayeredPane,
    private val skiaLayerAnalytics: SkiaLayerAnalytics,

    window: Window? = null,
    windowContainer: JLayeredPane = container,

    savedState: SavedState? = null,

    private val layerType: LayerType = ComposeFeatureFlags.layerType.value,
    private val renderSettings: RenderSettings = RenderSettings.SkiaSurface(),
) : WindowFocusListener,
    WindowListener {
    val windowContext = PlatformWindowContext()
    var window: Window? = null
        private set

    private var layoutDirection = layoutDirectionFor(window ?: container)

    /**
     * A list of additional layers. Layers are used to render [Popup]s and [Dialog]s.
     */
    private val layers = mutableListOf<DesktopComposeSceneLayer>()

    private var _windowContainer: JLayeredPane? = null
    var windowContainer: JLayeredPane
        get() = requireNotNull(_windowContainer)
        set(value) {
            if (_windowContainer == value) {
                return
            }
            if (layerType == LayerType.OnSameCanvas && value != container) {
                error("Customizing windowContainer cannot be used with LayerType.OnSameCanvas")
            }

            _windowContainer?.removeComponentListener(windowContainerComponentListener)
            value.addComponentListener(windowContainerComponentListener)

            _windowContainer = value

            windowContext.setWindowContainer(value)
            onWindowContainerSizeChanged()
            onWindowContainerPositionChanged()
        }

    @VisibleForTesting
    val architectureComponentsOwner = DefaultArchitectureComponentsOwner(savedState)

    private val coroutineExceptionHandler = DesktopCoroutineExceptionHandler()
    private val coroutineContext = MainUIDispatcher + coroutineExceptionHandler

    private val mediator = ComposeSceneMediator(
        container = container,
        windowContext = windowContext,
        exceptionHandler = {
            exceptionHandler?.onException(it) ?: throw it
        },
        eventListener = AwtEventListeners(
            DetectEventOutsideLayer(),
            FocusableLayerEventFilter()
        ),
        architectureComponentsOwner = architectureComponentsOwner,
        coroutineContext = coroutineContext,
        skiaLayerComponentFactory = ::createSkiaLayerComponent,
        composeSceneFactory = ::createComposeScene,
    )

    /**
     * The listener to [windowContainer] size and position changes.
     */
    private val windowContainerComponentListener = object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) = onWindowContainerSizeChanged()
        override fun componentMoved(e: ComponentEvent?) = onWindowContainerPositionChanged()
    }

    /**
     * The listener to [window] size and position changes.
     */
    private val windowBoundsListener = object : ComponentAdapter() {
        // When the window is moved to a display with a different density, componentResized is
        // called on the window, but not on `windowContainer`, so we need to update the size
        // here too.
        override fun componentResized(e: ComponentEvent?) = onWindowContainerSizeChanged()
        override fun componentMoved(e: ComponentEvent?) = onWindowPositionChanged()
    }

    val contentComponent by mediator::contentComponent
    val focusManager by mediator::focusManager
    val accessible by mediator::accessible
    var rootForTestListener by mediator::rootForTestListener
    // TODO: Changing fullscreen probably will require recreate our layers
    //  It will require add this flag as remember parameters in rememberComposeSceneLayer
    var fullscreen by mediator::fullscreen
    var compositionLocalContext by mediator::compositionLocalContext
    var exceptionHandler: WindowExceptionHandler? = null
    val windowHandle by mediator::windowHandle
    val renderApi by mediator::renderApi
    val preferredSize by mediator::preferredSize
    val semanticsOwners by mediator::semanticsOwners

    private var isDisposed = false
    private var isDetached = true
    private var isMinimized = false
    private var isFocused = false

    var isClearFocusOnMouseDownEnabled by mediator::isClearFocusOnMouseDownEnabled

    init {
        architectureComponentsOwner.enableSavedStateHandles()
        setWindow(window)
        this.windowContainer = windowContainer

        if (layerType == LayerType.OnComponent && renderSettings !is RenderSettings.SwingGraphics) {
            error("LayerType.OnComponent can only be used with rendering via Swing graphics")
        }
    }

    /**
     * Saves the current UI state into a [SavedState] object. The returned state can be used
     * to restore the UI state later by passing it to the constructor's [savedState] parameter.
     *
     * @return A [SavedState] object containing the current UI state.
     */
    fun saveState(): SavedState {
        return architectureComponentsOwner.saveState()
    }

    fun dispose() {
        isDisposed = true
        updateLifecycleState()

        _windowContainer?.removeComponentListener(windowContainerComponentListener)
        mediator.dispose()
        layers.fastForEach(DesktopComposeSceneLayer::close)
    }

    override fun windowGainedFocus(event: WindowEvent) = onWindowFocusChanged()
    override fun windowLostFocus(event: WindowEvent) = onWindowFocusChanged()

    override fun windowOpened(e: WindowEvent) {
        onWindowPositionChanged()
    }
    override fun windowClosing(e: WindowEvent) = Unit
    override fun windowClosed(e: WindowEvent) = Unit
    override fun windowIconified(e: WindowEvent) {
        isMinimized = true
        updateLifecycleState()
        onWindowPositionChanged()
    }
    override fun windowDeiconified(e: WindowEvent) {
        isMinimized = false
        updateLifecycleState()
        onWindowPositionChanged()
    }
    override fun windowActivated(e: WindowEvent) = Unit
    override fun windowDeactivated(e: WindowEvent) = Unit

    private fun onWindowFocusChanged() {
        val isNowFocused = window?.isFocused ?: false
        isFocused = isNowFocused
        windowContext.setWindowFocused(isNowFocused)
        mediator.onWindowFocusChanged()
        layers.fastForEach(DesktopComposeSceneLayer::onWindowFocusChanged)
        updateLifecycleState()
    }

    private fun onWindowContainerPositionChanged() {
        if (!container.isDisplayable) return

        mediator.onComponentPositionChanged()
        layers.fastForEach(DesktopComposeSceneLayer::onWindowContainerPositionChanged)
    }

    private fun onWindowContainerSizeChanged() {
        if (!container.isDisplayable) return

        windowContext.setContainerSizeFromComponent(windowContainer)
        mediator.onContainerSizeChanged()
        layers.fastForEach(DesktopComposeSceneLayer::onWindowContainerSizeChanged)

        // Sometimes Swing displays interop views in incorrect order after resizing,
        // so we need to force re-validate it.
        container.validate()
        container.repaint()
    }

    private fun onWindowPositionChanged() {
        if (!isDisposed) {
            mediator.onWindowPositionChanged()
        }
    }

    /**
     * Callback to let layers draw overlay on main [mediator].
     */
    private fun onRenderOverlay(canvas: Canvas, width: Int, height: Int) {
        layers.fastForEach {
            it.onRenderOverlay(canvas, width, height, windowContext.isWindowTransparent)
        }
    }

    fun onWindowTransparencyChanged(value: Boolean) {
        windowContext.isWindowTransparent = value
        mediator.onWindowTransparencyChanged(value)
    }

    fun onLayoutDirectionChanged(component: Component) {
        // ComposeWindow and ComposeDialog relies on self orientation, not on container's one
        layoutDirection = layoutDirectionFor(component)
        mediator.onLayoutDirectionChanged(layoutDirection)
    }

    fun onRenderApiChanged(action: () -> Unit) {
        mediator.onRenderApiChanged(action)
    }

    fun renderImmediately() {
        mediator.renderImmediately()
    }

    fun addNotify() {
        mediator.onComponentAttached()
        setWindow(SwingUtilities.getWindowAncestor(container))

        // Re-checking the actual size if it wasn't available during init.
        onWindowContainerSizeChanged()
        onWindowContainerPositionChanged()

        isDetached = false
        updateLifecycleState()
    }

    fun removeNotify() {
        mediator.onComponentDetached()
        isDetached = true
        updateLifecycleState()
        setWindow(null)
    }

    fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        mediator.contentComponent.setSize(width, height)

        // In case of preferred size there is no separate event for changing window size,
        // so re-checking the actual size on container resize too.
        onWindowContainerSizeChanged()
        onWindowContainerPositionChanged()
    }

    private fun setWindow(window: Window?) {
        if (this.window == window) {
            return
        }

        this.window?.let {
            it.removeWindowFocusListener(this)
            it.removeWindowListener(this)
            it.removeComponentListener(windowBoundsListener)
        }
        window?.let {
            it.addWindowFocusListener(this)
            it.addWindowListener(this)
            it.addComponentListener(windowBoundsListener)
        }
        this.window = window

        onWindowFocusChanged()
    }

    fun setKeyEventListeners(
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
    ) {
        mediator.setKeyEventListeners(
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent
        )
    }

    fun setContent(content: @Composable () -> Unit) {
        mediator.setContent(content)
    }

    private fun createSkiaLayerComponent(mediator: ComposeSceneMediator): SkiaLayerComponent {
        val renderDelegate = when (layerType) {
            // Use overlay decorator to allow window layers draw scrim on the main window
            LayerType.OnWindow -> OverlayRenderDecorator(mediator, ::onRenderOverlay)
            else -> mediator
        }
        return SkiaLayerComponent(
            mediator = mediator,
            windowContext = windowContext,
            renderDelegate = renderDelegate,
            skiaLayerAnalytics = skiaLayerAnalytics,
            renderSettings = renderSettings
        )
    }

    private fun createComposeScene(mediator: ComposeSceneMediator): ComposeScene {
        val density = container.density
        return when (layerType) {
            LayerType.OnSameCanvas ->
                CanvasLayersComposeScene(
                    density = density,
                    layoutDirection = layoutDirection,
                    coroutineContext = mediator.coroutineContext,
                    platformContext = mediator.platformContext,
                    invalidate = mediator::onComposeInvalidation,
                )
            else -> PlatformLayersComposeScene(
                density = density,
                layoutDirection = layoutDirection,
                coroutineContext = mediator.coroutineContext,
                composeSceneContext = createComposeSceneContext(
                    platformContext = mediator.platformContext
                ),
                invalidate = mediator::onComposeInvalidation,
            )
        }
    }

    private fun createPlatformLayer(
        density: Density,
        layoutDirection: LayoutDirection,
        focusable: Boolean,
        compositionContext: CompositionContext
    ): ComposeSceneLayer {
        return when (layerType) {
            LayerType.OnWindow -> WindowComposeSceneLayer(
                composeContainer = this,
                skiaLayerAnalytics = skiaLayerAnalytics,
                transparent = true, // TODO: Consider allowing opaque window layers
                density = density,
                layoutDirection = layoutDirection,
                focusable = focusable,
                compositionContext = compositionContext,
                renderSettings = renderSettings
            )
            LayerType.OnComponent -> SwingComposeSceneLayer(
                composeContainer = this,
                skiaLayerAnalytics = skiaLayerAnalytics,
                density = density,
                layoutDirection = layoutDirection,
                focusable = focusable,
                compositionContext = compositionContext
            )
            else -> error("Unexpected LayerType")
        }
    }

    /**
     * Generates a sequence of layers that are positioned above the given layer in the layers list.
     *
     * @param layer the layer to find layers above
     * @return a sequence of layers positioned above the given layer
     */
    fun layersAbove(layer: DesktopComposeSceneLayer) = sequence {
        var isAbove = false
        for (i in layers) {
            if (i == layer) {
                isAbove = true
            } else if (isAbove) {
                yield(i)
            }
        }
    }

    /**
     * Notify layers about change in layers list. Required for additional invalidation and
     * re-drawing if needed.
     *
     * @param layer the layer that triggered the change
     */
    private fun onLayersChange(layer: DesktopComposeSceneLayer) {
        layers.fastForEach {
            if (it != layer) {
                it.onLayersChange()
            }
        }
    }

    /**
     * Attaches a [DesktopComposeSceneLayer] to the list of layers.
     *
     * @param layer the layer to attach
     */
    fun attachLayer(layer: DesktopComposeSceneLayer) {
        layers.add(layer)
        onLayersChange(layer)
    }

    /**
     * Detaches a [DesktopComposeSceneLayer] from the list of layers.
     *
     * @param layer the layer to detach
     */
    fun detachLayer(layer: DesktopComposeSceneLayer) {
        layers.remove(layer)
        onLayersChange(layer)
    }

    fun createComposeSceneContext(platformContext: PlatformContext): ComposeSceneContext =
        ComposeSceneContextImpl(platformContext)

    private fun updateLifecycleState() {
        architectureComponentsOwner.setLifecycleState(
            when {
                isDisposed -> State.DESTROYED
                isDetached || isMinimized -> State.CREATED
                !isDetached && !isMinimized && isFocused -> State.RESUMED
                else -> State.STARTED
            }
        )
    }

    private inner class ComposeSceneContextImpl(
        override val platformContext: PlatformContext,
    ) : ComposeSceneContext {
        override fun createLayer(
            density: Density,
            layoutDirection: LayoutDirection,
            focusable: Boolean,
            compositionContext: CompositionContext
        ): ComposeSceneLayer = createPlatformLayer(
            density = density,
            layoutDirection = layoutDirection,
            focusable = focusable,
            compositionContext = compositionContext
        )
    }

    private inner class DesktopCoroutineExceptionHandler :
        AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {
        override fun handleException(context: CoroutineContext, exception: Throwable) {
            exceptionHandler?.onException(exception) ?: throw exception
        }
    }

    /**
     * Detect and trigger [DesktopComposeSceneLayer.onMouseEventOutside] if event happened below
     * focused layer.
     */
    private inner class DetectEventOutsideLayer : AwtEventListener {
        override fun onMouseEvent(event: AwtMouseEvent): Boolean {
            layers.fastForEachReversed {
                it.onMouseEventOutside(event)
                if (it.focusable) {
                    return false
                }
            }
            return false
        }
    }

    private inner class FocusableLayerEventFilter : AwtEventFilter() {
        private val noFocusableLayers get() = layers.fastAll { !it.focusable }

        override fun shouldSendMouseEvent(event: AwtMouseEvent): Boolean = noFocusableLayers
        override fun shouldSendKeyEvent(event: AwtKeyEvent): Boolean = noFocusableLayers
    }
}
