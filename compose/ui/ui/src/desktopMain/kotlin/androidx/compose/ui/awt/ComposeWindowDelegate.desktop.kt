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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.LocalWindow
import androidx.compose.ui.window.UndecoratedWindowResizer
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.density
import java.awt.*
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelListener
import javax.accessibility.Accessible
import javax.swing.JLayeredPane
import org.jetbrains.skiko.*

internal class ComposeWindowDelegate(
    private val window: Window,
    private val isUndecorated: () -> Boolean,
    skiaLayerAnalytics: SkiaLayerAnalytics,
    layoutDirection: LayoutDirection
) {
    private var isDisposed = false

    // AWT can leak JFrame in some cases
    // (see https://github.com/JetBrains/compose-jb/issues/1688),
    // so we nullify bridge on dispose, to prevent keeping
    // big objects in memory (like the whole LayoutNode tree of the window)
    private var _bridge: WindowComposeBridge? =
        WindowComposeBridge(skiaLayerAnalytics, layoutDirection)
    private val bridge
        get() = requireNotNull(_bridge) {
            "ComposeBridge is disposed"
        }
    internal val scene: ComposeScene
        get() = bridge.scene

    internal val windowAccessible: Accessible
        get() = bridge.sceneAccessible

    internal var rootForTestListener by bridge::rootForTestListener

    val undecoratedWindowResizer = UndecoratedWindowResizer(window)

    var fullscreen: Boolean
        get() = bridge.component.fullscreen
        set(value) {
            bridge.component.fullscreen = value
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
        get() = bridge.component.windowHandle

    val renderApi: GraphicsApi
        get() = bridge.renderApi

    private val _interopBlending: Boolean
        get() = System.getProperty("compose.interop.blending").toBoolean()
    private val interopBlending: Boolean
        get() = _interopBlending && bridge.interopBlendingSupported

    var isWindowTransparent: Boolean = false
        set(value) {
            if (field != value) {
                check(isUndecorated()) { "Transparent window should be undecorated!" }
                check(!window.isDisplayable) {
                    "Cannot change transparency if window is already displayable."
                }
                field = value
                bridge.isWindowTransparent = value
                bridge.transparency = value || interopBlending

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
                    pane.background = Color(0, 0, 0, 1)
                    pane.isOpaque = true
                } else {
                    pane.background = null
                    pane.isOpaque = false
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

    private val _pane = object : JLayeredPane() {
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
        }

        fun dispose() {
            super.remove(bridge.component)
            super.remove(bridge.invisibleComponent)
        }
    }

    val pane get() = _pane

    init {
        pane.focusTraversalPolicy = object : FocusTraversalPolicy() {
            override fun getComponentAfter(aContainer: Container?, aComponent: Component?) = null
            override fun getComponentBefore(aContainer: Container?, aComponent: Component?) = null
            override fun getFirstComponent(aContainer: Container?) = null
            override fun getLastComponent(aContainer: Container?) = null
            override fun getDefaultComponent(aContainer: Container?) = null
        }
        pane.isFocusCycleRoot = true
        bridge.transparency = interopBlending
        setContent {}
    }

    fun add(component: Component): Component {
        return _pane.add(component)
    }

    fun remove(component: Component) {
        _pane.remove(component)
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
                LocalLayerContainer provides _pane
            ) {
                WindowContentLayout(modifier, content)
            }
        }
    }

    @Composable
    private fun WindowContentLayout(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ){
        Layout(
            {
                content()
                undecoratedWindowResizer.Content(
                    modifier = Modifier.layoutId("UndecoratedWindowResizer")
                )
            },
            modifier = modifier,
            measurePolicy = { measurables, constraints ->
                val resizerMeasurable = measurables.lastOrNull()?.let {
                    if (it.layoutId == "UndecoratedWindowResizer") it else null
                }
                val resizerPlaceable = resizerMeasurable?.let {
                    val density = bridge.component.density.density
                    val resizerWidth = (window.width * density).toInt()
                    val resizerHeight = (window.height * density).toInt()
                    it.measure(
                        Constraints(
                            minWidth = resizerWidth,
                            minHeight = resizerHeight,
                            maxWidth = resizerWidth,
                            maxHeight = resizerHeight
                        )
                    )
                }

                val contentPlaceables = buildList(measurables.size){
                    measurables.fastForEach {
                        if (it != resizerMeasurable)
                            add(it.measure(constraints))
                    }
                }

                val contentWidth = contentPlaceables.maxOfOrNull { it.measuredWidth } ?: 0
                val contentHeight = contentPlaceables.maxOfOrNull { it.measuredHeight } ?: 0
                layout(contentWidth, contentHeight) {
                    contentPlaceables.fastForEach { placeable ->
                        placeable.place(0, 0)
                    }
                    resizerPlaceable?.place(0, 0)
                }
            }
        )
    }

    fun dispose() {
        if (!isDisposed) {
            bridge.dispose()
            _pane.dispose()
            _bridge = null
            isDisposed = true
        }
    }

    fun onRenderApiChanged(action: () -> Unit) {
        bridge.component.onStateChanged(SkiaLayer.PropertyKind.Renderer) {
            action()
        }
    }

    fun addMouseListener(listener: MouseListener) {
        bridge.component.addMouseListener(listener)
    }

    fun removeMouseListener(listener: MouseListener) {
        bridge.component.removeMouseListener(listener)
    }

    fun addMouseMotionListener(listener: MouseMotionListener) {
        bridge.component.addMouseMotionListener(listener)
    }

    fun removeMouseMotionListener(listener: MouseMotionListener) {
        bridge.component.removeMouseMotionListener(listener)
    }

    fun addMouseWheelListener(listener: MouseWheelListener) {
        bridge.component.addMouseWheelListener(listener)
    }

    fun removeMouseWheelListener(listener: MouseWheelListener) {
        bridge.component.removeMouseWheelListener(listener)
    }
}
