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
import androidx.compose.ui.awt.ComposeBridge
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
import java.awt.Component
import java.awt.Window
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.JLayeredPane
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
) : WindowFocusListener {
    val windowContext = PlatformWindowContext()
    var window: Window? = null
        private set
    private var layoutDirection = layoutDirectionFor(window ?: container)

    private val coroutineExceptionHandler = DesktopCoroutineExceptionHandler()
    private val coroutineContext = MainUIDispatcher + coroutineExceptionHandler

    private val bridge = ComposeBridge(
        container = container,
        windowContext = windowContext,
        exceptionHandler = {
            exceptionHandler?.onException(it) ?: throw it
        },
        coroutineContext = coroutineContext,
        skiaLayerComponentFactory = ::createSkiaLayerComponent,
        composeSceneFactory = ::createComposeScene,
    )

    val contentComponent by bridge::contentComponent
    val focusManager by bridge::focusManager
    val accessible by bridge::accessible
    var rootForTestListener by bridge::rootForTestListener
    // TODO: Changing fullscreen probably will require recreate our layers
    //  It will require add this flag as remember parameters in rememberComposeSceneLayer
    var fullscreen by bridge.skiaLayerComponent::fullscreen
    var compositionLocalContext by bridge::compositionLocalContext
    var exceptionHandler: WindowExceptionHandler? = null
    val windowHandle by bridge.skiaLayerComponent::windowHandle
    val renderApi by bridge.skiaLayerComponent::renderApi
    val preferredSize by bridge::preferredSize

    init {
        setWindow(window)
    }

    fun dispose() {
        bridge.dispose()
    }

    override fun windowGainedFocus(event: WindowEvent) = onChangeWindowFocus()
    override fun windowLostFocus(event: WindowEvent) = onChangeWindowFocus()

    private fun onChangeWindowFocus() {
        windowContext.setWindowFocused(window?.isFocused ?: false)
        bridge.onChangeWindowFocus()
    }

    fun onChangeWindowTransparency(value: Boolean) {
        windowContext.isWindowTransparent = value
        bridge.skiaLayerComponent.transparency = value
    }

    fun onChangeLayoutDirection(component: Component) {
        // ComposeWindow and ComposeDialog relies on self orientation, not on container's one
        layoutDirection = layoutDirectionFor(component)
        bridge.onChangeLayoutDirection(layoutDirection)
    }

    fun onRenderApiChanged(action: () -> Unit) {
        bridge.skiaLayerComponent.onRenderApiChanged(action)
    }

    fun addNotify() {
        bridge.onComponentAttached()
        setWindow(SwingUtilities.getWindowAncestor(container))
    }

    fun removeNotify() {
        setWindow(null)
    }

    fun addToComponentLayer(component: Component) {
        bridge.addToComponentLayer(component)
    }

    fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        bridge.contentComponent.setSize(width, height)
    }

    private fun setWindow(window: Window?) {
        if (this.window == window) {
            return
        }

        this.window?.removeWindowFocusListener(this)
        window?.addWindowFocusListener(this)

        this.window = window

        onChangeWindowFocus()
    }

    fun setKeyEventListeners(
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
    ) {
        bridge.setKeyEventListeners(onPreviewKeyEvent, onKeyEvent)
    }

    fun setContent(content: @Composable () -> Unit) {
        bridge.setContent {
            ProvideContainerCompositionLocals(this) {
                content()
            }
        }
    }

    private fun createSkiaLayerComponent(bridge: ComposeBridge): SkiaLayerComponent {
        return if (useSwingGraphics) {
            SwingSkiaLayerComponent(skiaLayerAnalytics, bridge)
        } else {
            WindowSkiaLayerComponent(skiaLayerAnalytics, windowContext, bridge)
        }
    }

    private fun createComposeScene(bridge: ComposeBridge): ComposeScene {
        val density = container.density
        return MultiLayerComposeScene(
            coroutineContext = bridge.coroutineContext,
            composeSceneContext = createComposeSceneContext(
                platformContext = bridge.platformContext
            ),
            density = density,
            invalidate = bridge::onComposeInvalidation,
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
