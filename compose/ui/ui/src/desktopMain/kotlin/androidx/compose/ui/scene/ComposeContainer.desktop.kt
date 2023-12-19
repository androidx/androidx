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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.awt.LocalLayerContainer
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.scene.skia.SwingSkiaLayerComponent
import androidx.compose.ui.scene.skia.WindowSkiaLayerComponent
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.density
import androidx.compose.ui.window.layoutDirectionFor
import androidx.compose.ui.window.sizeInPx
import java.awt.Component
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.JFrame
import javax.swing.JLayeredPane
import javax.swing.RootPaneContainer
import javax.swing.SwingUtilities
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import org.jetbrains.skiko.MainUIDispatcher
import org.jetbrains.skiko.SkiaLayerAnalytics

/**
 * Internal entry point to Compose.
 *
 * It binds Skia canvas and [ComposeScene] to [container].
 * It also configures compose based on [ComposeFeatureFlags].
 */
internal class ComposeContainer(
    val container: JLayeredPane,
    private val skiaLayerAnalytics: SkiaLayerAnalytics,
    window: Window? = null,

    private val useSwingGraphics: Boolean = ComposeFeatureFlags.useSwingGraphics,
) : ComponentListener, WindowFocusListener {
    val windowContext = PlatformWindowContext()
    var window: Window? = null
        private set
    private var layoutDirection = layoutDirectionFor(window ?: container)

    private val coroutineExceptionHandler = DesktopCoroutineExceptionHandler()
    private val coroutineContext = MainUIDispatcher + coroutineExceptionHandler

    private val mediator = ComposeSceneMediator(
        container = container,
        windowContext = windowContext,
        exceptionHandler = {
            exceptionHandler?.onException(it) ?: throw it
        },
        coroutineContext = coroutineContext,
        skiaLayerComponentFactory = ::createSkiaLayerComponent,
        composeSceneFactory = ::createComposeScene,
    )

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

    init {
        setWindow(window)
    }

    fun dispose() {
        mediator.dispose()
    }

    override fun componentResized(e: ComponentEvent?) = onChangeWindowBounds()
    override fun componentMoved(e: ComponentEvent?) = onChangeWindowBounds()
    override fun componentShown(e: ComponentEvent?) = Unit
    override fun componentHidden(e: ComponentEvent?) = Unit

    override fun windowGainedFocus(event: WindowEvent) = onChangeWindowFocus()
    override fun windowLostFocus(event: WindowEvent) = onChangeWindowFocus()

    private fun onChangeWindowFocus() {
        windowContext.setWindowFocused(window?.isFocused ?: false)
        mediator.onChangeWindowFocus()
    }

    private fun onChangeWindowBounds() {
        val container = (window as? RootPaneContainer)?.contentPane ?: window ?: return
        windowContext.setContainerSize(container.sizeInPx)
    }

    fun onChangeWindowTransparency(value: Boolean) {
        windowContext.isWindowTransparent = value
        mediator.onChangeWindowTransparency(value)
    }

    fun onChangeLayoutDirection(component: Component) {
        // ComposeWindow and ComposeDialog relies on self orientation, not on container's one
        layoutDirection = layoutDirectionFor(component)
        mediator.onChangeLayoutDirection(layoutDirection)
    }

    fun onRenderApiChanged(action: () -> Unit) {
        mediator.onRenderApiChanged(action)
    }

    fun addNotify() {
        mediator.onComponentAttached()
        setWindow(SwingUtilities.getWindowAncestor(container))
    }

    fun removeNotify() {
        setWindow(null)
    }

    fun addToComponentLayer(component: Component) {
        mediator.addToComponentLayer(component)
    }

    fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        mediator.contentComponent.setSize(width, height)

        // In case of preferred size there is no separate event for changing window size,
        // so re-checking the actual size on container resize too.
        onChangeWindowBounds()
    }

    private fun setWindow(window: Window?) {
        if (this.window == window) {
            return
        }

        this.window?.removeWindowFocusListener(this)
        this.window?.removeComponentListener(this)

        window?.addComponentListener(this)
        window?.addWindowFocusListener(this)
        this.window = window

        onChangeWindowFocus()
        onChangeWindowBounds()
    }

    fun setKeyEventListeners(
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
    ) {
        mediator.setKeyEventListeners(onPreviewKeyEvent, onKeyEvent)
    }

    fun setContent(content: @Composable () -> Unit) {
        mediator.setContent {
            ProvideContainerCompositionLocals(this) {
                content()
            }
        }
    }

    private fun createSkiaLayerComponent(mediator: ComposeSceneMediator): SkiaLayerComponent {
        return if (useSwingGraphics) {
            SwingSkiaLayerComponent(mediator, skiaLayerAnalytics)
        } else {
            WindowSkiaLayerComponent(mediator, windowContext, skiaLayerAnalytics)
        }
    }

    private fun createComposeScene(mediator: ComposeSceneMediator): ComposeScene {
        val density = container.density
        return MultiLayerComposeScene(
            coroutineContext = mediator.coroutineContext,
            composeSceneContext = createComposeSceneContext(
                platformContext = mediator.platformContext
            ),
            density = density,
            invalidate = mediator::onComposeInvalidation,
            layoutDirection = layoutDirection,
        )
    }

    fun createComposeSceneContext(platformContext: PlatformContext): ComposeSceneContext =
        ComposeSceneContextImpl(platformContext)

    private inner class ComposeSceneContextImpl(
        override val platformContext: PlatformContext,
    ) : ComposeSceneContext

    private inner class DesktopCoroutineExceptionHandler :
        AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {
        override fun handleException(context: CoroutineContext, exception: Throwable) {
            exceptionHandler?.onException(exception) ?: throw exception
        }
    }
}

@Composable
private fun ProvideContainerCompositionLocals(
    composeContainer: ComposeContainer,
    content: @Composable () -> Unit,
) = CompositionLocalProvider(
    LocalLayerContainer provides composeContainer.container,
    content = content
)
