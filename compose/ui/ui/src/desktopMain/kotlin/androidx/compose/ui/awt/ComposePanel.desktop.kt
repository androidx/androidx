/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.scene.skia.SwingSkiaLayerComponent
import androidx.compose.ui.scene.skia.WindowSkiaLayerComponent
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.layoutDirectionFor
import java.awt.Color
import java.awt.Component
import java.awt.ComponentOrientation
import java.awt.Container
import java.awt.Dimension
import java.awt.FocusTraversalPolicy
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.util.Locale
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import javax.swing.SwingUtilities.isEventDispatchThread
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.swing.SkiaSwingLayer

/**
 * ComposePanel is a panel for building UI using Compose for Desktop.
 *
 * @param skiaLayerAnalytics Analytics that helps to know more about SkiaLayer behaviour.
 * SkiaLayer is underlying class used internally to draw Compose content.
 * Implementation usually uses third-party solution to send info to some centralized analytics gatherer.
 */
class ComposePanel @ExperimentalComposeUiApi constructor(
    private val skiaLayerAnalytics: SkiaLayerAnalytics,
) : JLayeredPane() {
    constructor() : this(SkiaLayerAnalytics.Empty)

    init {
        check(isEventDispatchThread()) {
            "ComposePanel should be created inside AWT Event Dispatch Thread" +
                " (use SwingUtilities.invokeLater).\n" +
                "Creating from another thread isn't supported."
        }
        background = Color.white
        layout = null
        focusTraversalPolicy = object : FocusTraversalPolicy() {
            override fun getComponentAfter(
                aContainer: Container?,
                aComponent: Component?
            ): Component? {
                val ancestor = focusCycleRootAncestor
                val policy = ancestor.focusTraversalPolicy
                return policy.getComponentAfter(ancestor, this@ComposePanel)
            }

            override fun getComponentBefore(
                aContainer: Container?,
                aComponent: Component?
            ): Component? {
                val ancestor = focusCycleRootAncestor
                val policy = ancestor.focusTraversalPolicy
                return policy.getComponentBefore(ancestor, this@ComposePanel)
            }

            override fun getFirstComponent(aContainer: Container?) = null
            override fun getLastComponent(aContainer: Container?) = null
            override fun getDefaultComponent(aContainer: Container?) = null
        }
        isFocusCycleRoot = true
    }

    private val _focusListeners = mutableSetOf<FocusListener?>()
    private var _isFocusable = true
    private var _isRequestFocusEnabled = false
    private var bridge: ComposeBridge? = null
    private var content: (@Composable () -> Unit)? = null

    /**
     * Determines whether the Compose state in [ComposePanel] should be disposed
     * when panel is detached from Swing hierarchy (when [removeNotify] is called).
     *
     * If it is set to false, it is developer's responsibility to call [dispose] function
     * when Compose state and all related to [ComposePanel] resources are no longer needed.
     * It can be useful for cases when [ComposePanel] can be attached/detached to Swing hierarchy multiple times,
     * so with [isDisposeOnRemove] = `false` state will be preserved.
     *
     * On the other hand, [isDisposeOnRemove] = `true` can be useful for stateless components,
     * that can be recreated for each attaching to Swing hierarchy.
     *
     * @see dispose
     */
    @ExperimentalComposeUiApi
    var isDisposeOnRemove: Boolean = true

    /**
     * Disposes Compose state and rendering resources.
     *
     * Should be called only when [ComposePanel] is detached from Swing hierarchy.
     * Otherwise, nothing will happen.
     *
     * @see isDisposeOnRemove
     */
    @ExperimentalComposeUiApi
    fun dispose() {
        if (bridge != null) {
            bridge!!.dispose()
            super.remove(bridge!!.component)
            super.remove(bridge!!.invisibleComponent)
            bridge = null
        }
    }

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        bridge?.component?.setSize(width, height)
        super.setBounds(x, y, width, height)
    }

    override fun getPreferredSize(): Dimension? {
        return if (isPreferredSizeSet) super.getPreferredSize() else bridge?.component?.preferredSize
    }

    /**
     * Sets Compose content of the ComposePanel.
     *
     * @param content Composable content of the ComposePanel.
     */
    fun setContent(content: @Composable () -> Unit) {
        // The window (or root container) may not be ready to render composable content, so we need
        // to keep the lambda describing composable content and set the content only when
        // everything is ready to avoid accidental crashes and memory leaks on all supported OS
        // types.
        this.content = content
        initContent()
    }

    /**
     * Handler to catch uncaught exceptions during rendering frames, handling events,
     * or processing background Compose operations. If null, then exceptions throw
     * further up the call stack.
     */
    @ExperimentalComposeUiApi
    var exceptionHandler: WindowExceptionHandler? = null
        set(value) {
            field = value
            bridge?.exceptionHandler = value
        }

    private fun initContent() {
        if (bridge != null && content != null) {
            bridge!!.setContent {
                CompositionLocalProvider(
                    LocalLayerContainer provides this,
                    content = content!!
                )
            }
        }
    }

    override fun add(component: Component): Component {
        addToLayer(component, componentLayer)
        if (!interopBlending) {
            bridge?.addClipComponent(component)
        }
        return component
    }

    override fun remove(component: Component) {
        bridge?.removeClipComponent(component)
        super.remove(component)
    }

    @OptIn(ExperimentalSkikoApi::class)
    private fun addToLayer(component: Component, layer: Int) {
        if (renderApi == GraphicsApi.METAL && bridge?.component !is SkiaSwingLayer) {
            // Applying layer on macOS makes our bridge non-transparent
            // But it draws always on top, so we can just add it as-is
            // TODO: Figure out why it makes difference in transparency
            super.add(component, 0)
        } else {
            super.setLayer(component, layer)
            super.add(component)
        }
    }

    private val bridgeLayer: Int = 10
    private val componentLayer: Int
        get() = if (interopBlending) 0 else 20

    private val interopBlending: Boolean
        get() = ComposeFeatureFlags.useInteropBlending &&
            (ComposeFeatureFlags.useSwingGraphics ||
                requireNotNull(bridge).skiaLayerComponent.interopBlendingSupported)

    override fun addNotify() {
        super.addNotify()

        // After [super.addNotify] is called we can safely initialize the bridge and composable
        // content.
        if (this.bridge == null) {
            val bridge = createComposeBridge()
            this.bridge = bridge
            initContent()

            bridge.setParentWindow(SwingUtilities.getWindowAncestor(this))

            addToLayer(bridge.invisibleComponent, bridgeLayer)
            addToLayer(bridge.component, bridgeLayer)
        }
    }

    private fun createComposeBridge(): ComposeBridge {
        val bridge = ComposeBridge(
            layoutDirectionFor(this)
        ) {
            if (ComposeFeatureFlags.useSwingGraphics) {
                SwingSkiaLayerComponent(skiaLayerAnalytics, it)
            } else {
                WindowSkiaLayerComponent(skiaLayerAnalytics, it)
            }
        }
        return bridge.apply {
            focusManager.releaseFocus()
            component.setSize(width, height)
            component.isFocusable = _isFocusable
            component.isRequestFocusEnabled = _isRequestFocusEnabled
            _focusListeners.forEach(component::addFocusListener)
            exceptionHandler = this@ComposePanel.exceptionHandler
            component.addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent) {
                    // The focus can be switched from the child component inside SwingPanel.
                    // In that case, SwingPanel will take care of it.
                    if (!isParentOf(e.oppositeComponent)) {
                        bridge.focusManager.requestFocus()
                        when (e.cause) {
                            FocusEvent.Cause.TRAVERSAL_FORWARD -> {
                                bridge.focusManager.moveFocus(FocusDirection.Next)
                            }

                            FocusEvent.Cause.TRAVERSAL_BACKWARD -> {
                                bridge.focusManager.moveFocus(FocusDirection.Previous)
                            }

                            else -> Unit
                        }
                    }
                }

                override fun focusLost(e: FocusEvent) = Unit
            })
        }
    }

    override fun removeNotify() {
        bridge?.setParentWindow(null)
        if (isDisposeOnRemove) {
            dispose()
        }
        super.removeNotify()
    }

    override fun setComponentOrientation(o: ComponentOrientation?) {
        super.setComponentOrientation(o)

        updateLayoutDirection()
    }

    override fun setLocale(l: Locale?) {
        super.setLocale(l)

        updateLayoutDirection()
    }

    private fun updateLayoutDirection() {
        bridge?.layoutDirection = layoutDirectionFor(this)
    }

    override fun addFocusListener(l: FocusListener?) {
        bridge?.component?.addFocusListener(l)
        _focusListeners.add(l)
    }

    override fun removeFocusListener(l: FocusListener?) {
        bridge?.component?.removeFocusListener(l)
        _focusListeners.remove(l)
    }

    override fun isFocusable() = _isFocusable

    override fun setFocusable(focusable: Boolean) {
        _isFocusable = focusable
        bridge?.component?.isFocusable = focusable
    }

    override fun isRequestFocusEnabled(): Boolean = _isRequestFocusEnabled

    override fun setRequestFocusEnabled(requestFocusEnabled: Boolean) {
        _isRequestFocusEnabled = requestFocusEnabled
        bridge?.component?.isRequestFocusEnabled = requestFocusEnabled
    }

    override fun hasFocus(): Boolean {
        return bridge?.component?.hasFocus() ?: false
    }

    override fun isFocusOwner(): Boolean {
        return bridge?.component?.isFocusOwner ?: false
    }

    override fun requestFocus() {
        bridge?.component?.requestFocus()
    }

    override fun requestFocus(temporary: Boolean): Boolean {
        return bridge?.component?.requestFocus(temporary) ?: false
    }

    override fun requestFocus(cause: FocusEvent.Cause?) {
        bridge?.component?.requestFocus(cause)
    }

    override fun requestFocusInWindow(): Boolean {
        return bridge?.component?.requestFocusInWindow() ?: false
    }

    override fun requestFocusInWindow(cause: FocusEvent.Cause?): Boolean {
        return bridge?.component?.requestFocusInWindow(cause) ?: false
    }

    override fun setFocusTraversalKeysEnabled(focusTraversalKeysEnabled: Boolean) {
        // ignore, traversal keys should always be handled by ComposeBridge
    }

    override fun getFocusTraversalKeysEnabled(): Boolean {
        return false
    }

    /**
     * Returns low-level rendering API used for rendering in this ComposeWindow. API is
     * automatically selected based on operating system, graphical hardware and `SKIKO_RENDER_API`
     * environment variable.
     */
    val renderApi: GraphicsApi
        get() = bridge?.skiaLayerComponent?.renderApi ?: GraphicsApi.UNKNOWN
}
