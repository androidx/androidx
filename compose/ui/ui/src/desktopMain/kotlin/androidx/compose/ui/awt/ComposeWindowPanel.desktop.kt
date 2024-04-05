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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.LayerType
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.scene.ComposeContainer
import androidx.compose.ui.window.LocalWindow
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.FocusTraversalPolicy
import java.awt.Window
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelListener
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
    private var _composeContainer: ComposeContainer? = ComposeContainer(
        container = this,
        skiaLayerAnalytics = skiaLayerAnalytics,
        window = window,
        useSwingGraphics = false,
        layerType = ComposeFeatureFlags.layerType.let {
            // LayerType.OnComponent might be used only with rendering to Swing graphics,
            // but it's always disabled here. Using fallback instead of [check] to support
            // opening separate windows from [ComposePanel] with such layer type.
            if (it == LayerType.OnComponent) LayerType.OnSameCanvas else it
        }
    )
    private val composeContainer
        get() = requireNotNull(_composeContainer) {
            "ComposeContainer is disposed"
        }
    private val contentComponent by composeContainer::contentComponent

    val windowAccessible by composeContainer::accessible
    val windowContext by composeContainer::windowContext
    var rootForTestListener by composeContainer::rootForTestListener
    var fullscreen by composeContainer::fullscreen
    var compositionLocalContext by composeContainer::compositionLocalContext
    var exceptionHandler by composeContainer::exceptionHandler
    val windowHandle by composeContainer::windowHandle
    val renderApi by composeContainer::renderApi

    var isWindowTransparent: Boolean = false
        set(value) {
            if (field != value) {
                check(isUndecorated()) { "Transparent window should be undecorated!" }
                check(!window.isDisplayable) {
                    "Cannot change transparency if window is already displayable."
                }
                field = value
                composeContainer.onChangeWindowTransparency(value)
                setTransparent(value)
                window.background = getTransparentWindowBackground(value, renderApi)
            }
        }

    init {
        layout = null
        focusTraversalPolicy = object : FocusTraversalPolicy() {
            override fun getComponentAfter(aContainer: Container?, aComponent: Component?) = null
            override fun getComponentBefore(aContainer: Container?, aComponent: Component?) = null
            override fun getFirstComponent(aContainer: Container?) = null
            override fun getLastComponent(aContainer: Container?) = null
            override fun getDefaultComponent(aContainer: Container?) = null
        }
        isFocusCycleRoot = true
    }

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        super.setBounds(x, y, width, height)
        composeContainer.setBounds(0, 0, width, height)
    }

    override fun getPreferredSize(): Dimension? = if (isPreferredSizeSet) {
        super.getPreferredSize()
    } else {
        composeContainer.preferredSize
    }

    override fun addNotify() {
        super.addNotify()
        _composeContainer?.addNotify()
        _composeContainer?.contentComponent?.requestFocus()
    }

    override fun removeNotify() {
        _composeContainer?.removeNotify()
        super.removeNotify()
    }

    fun setContent(
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        composeContainer.setKeyEventListeners(
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent
        )
        composeContainer.setContent {
            CompositionLocalProvider(
                LocalWindow provides window
            ) {
                WindowContentLayout(modifier, content)
            }
        }
    }

    fun dispose() {
        if (isDisposed) {
            return
        }
        _composeContainer?.dispose()
        _composeContainer = null
        isDisposed = true
    }

    fun onChangeLayoutDirection(component: Component) {
        composeContainer.onChangeLayoutDirection(component)
    }

    fun onRenderApiChanged(action: () -> Unit) {
        composeContainer.onRenderApiChanged(action)
    }

    // We need overridden listeners because we mix Swing and AWT components in the
    // org.jetbrains.skiko.SkiaLayer, they don't work well together.
    // TODO(demin): is it possible to fix that without overriding?

    override fun addMouseListener(listener: MouseListener) {
        contentComponent.addMouseListener(listener)
    }

    override fun removeMouseListener(listener: MouseListener) {
        contentComponent.removeMouseListener(listener)
    }

    override fun addMouseMotionListener(listener: MouseMotionListener) {
        contentComponent.addMouseMotionListener(listener)
    }

    override fun removeMouseMotionListener(listener: MouseMotionListener) {
        contentComponent.removeMouseMotionListener(listener)
    }

    override fun addMouseWheelListener(listener: MouseWheelListener) {
        contentComponent.addMouseWheelListener(listener)
    }

    override fun removeMouseWheelListener(listener: MouseWheelListener) {
        contentComponent.removeMouseWheelListener(listener)
    }
}
