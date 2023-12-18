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

package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.skia.WindowSkiaLayerComponent
import androidx.compose.ui.window.LocalWindow
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.layoutDirectionFor
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.FocusTraversalPolicy
import java.awt.Window
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelListener
import javax.accessibility.Accessible
import javax.swing.JLayeredPane
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.hostOs

/**
 * A panel used as a main view in [ComposeWindow] and [ComposeDialog].
 */
internal class ComposeWindowPanel(
    private val window: Window,
    private val isUndecorated: () -> Boolean,
    skiaLayerAnalytics: SkiaLayerAnalytics,
) : JLayeredPane() {
    private var isDisposed = false

    // AWT can leak JFrame in some cases
    // (see https://github.com/JetBrains/compose-jb/issues/1688),
    // so we nullify bridge on dispose, to prevent keeping
    // big objects in memory (like the whole LayoutNode tree of the window)
    private var _bridge: ComposeBridge? = ComposeBridge(
        layoutDirectionFor(window)
    ) {
        WindowSkiaLayerComponent(skiaLayerAnalytics, it)
    }
    private val bridge
        get() = requireNotNull(_bridge) {
            "ComposeBridge is disposed"
        }
    internal val scene: ComposeScene
        get() = bridge.scene

    internal val windowAccessible: Accessible
        get() = bridge.sceneAccessible

    internal var rootForTestListener by bridge::rootForTestListener

    var fullscreen: Boolean
        get() = bridge.skiaLayerComponent.fullscreen
        set(value) {
            bridge.skiaLayerComponent.fullscreen = value
        }

    var compositionLocalContext: CompositionLocalContext?
        get() = bridge.compositionLocalContext
        set(value) {
            bridge.compositionLocalContext = value
        }

    @ExperimentalComposeUiApi
    var exceptionHandler: WindowExceptionHandler?
        get() = bridge.exceptionHandler
        set(value) {
            bridge.exceptionHandler = value
        }

    val windowHandle: Long
        get() = bridge.skiaLayerComponent.windowHandle

    val renderApi: GraphicsApi
        get() = bridge.skiaLayerComponent.renderApi

    private val interopBlending: Boolean
        get() = ComposeFeatureFlags.useInteropBlending &&
            bridge.skiaLayerComponent.interopBlendingSupported

    var isWindowTransparent: Boolean = false
        set(value) {
            if (field != value) {
                check(isUndecorated()) { "Transparent window should be undecorated!" }
                check(!window.isDisplayable) {
                    "Cannot change transparency if window is already displayable."
                }
                field = value
                bridge.isWindowTransparent = value
                bridge.skiaLayerComponent.transparency = value || interopBlending

                /*
                 * Windows makes clicks on transparent pixels fall through, but it doesn't work
                 * with GPU accelerated rendering since this check requires having access to pixels from CPU.
                 *
                 * JVM doesn't allow override this behaviour with low-level windows methods, so hack this in this way.
                 * Based on tests, it doesn't affect resulting pixel color.
                 *
                 * Note: Do not set isOpaque = false for this container
                 */
                if (value && hostOs == OS.Windows) {
                    background = Color(0, 0, 0, 1)
                    isOpaque = true
                } else {
                    background = null
                    isOpaque = false
                }

                window.background = if (value && !skikoTransparentWindowHack) Color(0, 0, 0, 0) else null
            }
        }

    /**
     * There is a hack inside skiko OpenGL and Software redrawers for Windows that makes current
     * window transparent without setting `background` to JDK's window. It's done by getting native
     * component parent and calling `DwmEnableBlurBehindWindow`.
     *
     * FIXME: Make OpenGL work inside transparent window (background == Color(0, 0, 0, 0)) without this hack.
     *
     * See `enableTransparentWindow` (skiko/src/awtMain/cpp/windows/window_util.cc)
     */
    private val skikoTransparentWindowHack: Boolean
        get() = hostOs == OS.Windows && renderApi != GraphicsApi.DIRECT3D

    init {
        focusTraversalPolicy = object : FocusTraversalPolicy() {
            override fun getComponentAfter(aContainer: Container?, aComponent: Component?) = null
            override fun getComponentBefore(aContainer: Container?, aComponent: Component?) = null
            override fun getFirstComponent(aContainer: Container?) = null
            override fun getLastComponent(aContainer: Container?) = null
            override fun getDefaultComponent(aContainer: Container?) = null
        }
        isFocusCycleRoot = true
        bridge.skiaLayerComponent.transparency = interopBlending
        setContent {}
    }

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        bridge.component.setBounds(0, 0, width, height)
        super.setBounds(x, y, width, height)
    }

    override fun add(component: Component): Component {
        addToLayer(component, componentLayer)
        if (!interopBlending) {
            bridge.addClipComponent(component)
        }
        return component
    }

    override fun remove(component: Component) {
        bridge.removeClipComponent(component)
        super.remove(component)
    }

    private fun addToLayer(component: Component, layer: Int) {
        if (renderApi == GraphicsApi.METAL) {
            // Applying layer on macOS makes our bridge non-transparent
            // But it draws always on top, so we can just add it as-is
            // TODO: Figure out why it makes difference in transparency
            super.add(component, 0)
        } else {
            super.setLayer(component, layer)
            super.add(component)
        }
    }

    private val bridgeLayer: Int get() = 10
    private val componentLayer: Int
        get() = if (interopBlending) 0 else 20

    override fun addNotify() {
        super.addNotify()
        bridge.component.requestFocus()
    }

    override fun getPreferredSize() =
        if (isPreferredSizeSet) super.getPreferredSize() else bridge.component.preferredSize

    init {
        layout = null
        addToLayer(bridge.invisibleComponent, bridgeLayer)
        addToLayer(bridge.component, bridgeLayer)
        bridge.setParentWindow(window)
    }

    fun setContent(
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        bridge.setKeyEventListeners(
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent
        )
        bridge.setContent {
            CompositionLocalProvider(
                LocalWindow provides window,
                LocalLayerContainer provides this
            ) {
                WindowContentLayout(modifier, content)
            }
        }
    }

    fun dispose() {
        if (!isDisposed) {
            bridge.dispose()

            super.remove(bridge.component)
            super.remove(bridge.invisibleComponent)

            _bridge = null
            isDisposed = true
        }
    }

    fun onChangeLayoutDirection(component: Component) {
        bridge.scene.layoutDirection = layoutDirectionFor(component)
    }

    fun onRenderApiChanged(action: () -> Unit) {
        bridge.skiaLayerComponent.onRenderApiChanged(action)
    }

    // We need overridden listeners because we mix Swing and AWT components in the
    // org.jetbrains.skiko.SkiaLayer, they don't work well together.
    // TODO(demin): is it possible to fix that without overriding?

    override fun addMouseListener(listener: MouseListener) {
        bridge.component.addMouseListener(listener)
    }

    override fun removeMouseListener(listener: MouseListener) {
        bridge.component.removeMouseListener(listener)
    }

    override fun addMouseMotionListener(listener: MouseMotionListener) {
        bridge.component.addMouseMotionListener(listener)
    }

    override fun removeMouseMotionListener(listener: MouseMotionListener) {
        bridge.component.removeMouseMotionListener(listener)
    }

    override fun addMouseWheelListener(listener: MouseWheelListener) {
        bridge.component.addMouseWheelListener(listener)
    }

    override fun removeMouseWheelListener(listener: MouseWheelListener) {
        bridge.component.removeMouseWheelListener(listener)
    }
}
