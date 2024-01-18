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
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.LayerType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.scene.ComposeContainer
import androidx.compose.ui.window.WindowExceptionHandler
import java.awt.Color
import java.awt.Component
import java.awt.ComponentOrientation
import java.awt.Container
import java.awt.Dimension
import java.awt.FocusTraversalPolicy
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.util.*
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities.isEventDispatchThread
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayerAnalytics

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

    private var _composeContainer: ComposeContainer? = null
    private var _composeContent: (@Composable () -> Unit)? = null

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
        _composeContainer?.dispose()
        _composeContainer = null
    }

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        super.setBounds(x, y, width, height)
        _composeContainer?.setBounds(0, 0, width, height)
    }

    override fun getPreferredSize(): Dimension? = if (isPreferredSizeSet) {
        super.getPreferredSize()
    } else  {
        _composeContainer?.preferredSize
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
        _composeContent = content
        _composeContainer?.setContent(content)
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
            _composeContainer?.exceptionHandler = value
        }

    /**
     * A container used for additional layers and as reference for window coordinate space.
     * It might be customized only with [LayerType.OnComponent].
     *
     * See [ComposeFeatureFlags.layerType]
     */
    @ExperimentalComposeUiApi
    var windowContainer: JLayeredPane = this
        set(value) {
            field = value
            _composeContainer?.windowContainer = value
        }

    override fun add(component: Component): Component {
        _composeContainer?.addToComponentLayer(component)
        return component
    }

    override fun remove(component: Component) {
        super.remove(component)
    }

    override fun addNotify() {
        super.addNotify()

        // After [super.addNotify] is called we can safely initialize the bridge and composable
        // content.
        val composeContainer = _composeContainer ?: createComposeContainer().also {
            _composeContainer = it
            val composeContent = _composeContent
            if (composeContent != null) {
                it.setContent(composeContent)
            }
        }
        composeContainer.addNotify()
    }

    private fun createComposeContainer(): ComposeContainer {
        return ComposeContainer(
            container = this,
            skiaLayerAnalytics = skiaLayerAnalytics,
            windowContainer = windowContainer
        ).apply {
            focusManager.releaseFocus()
            setBounds(0, 0, width, height)
            contentComponent.isFocusable = _isFocusable
            contentComponent.isRequestFocusEnabled = _isRequestFocusEnabled
            exceptionHandler = this@ComposePanel.exceptionHandler

            _focusListeners.forEach(contentComponent::addFocusListener)
            contentComponent.addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent) {
                    // The focus can be switched from the child component inside SwingPanel.
                    // In that case, SwingPanel will take care of it.
                    if (!isParentOf(e.oppositeComponent)) {
                        focusManager.requestFocus()
                        when (e.cause) {
                            FocusEvent.Cause.TRAVERSAL_FORWARD -> {
                                focusManager.moveFocus(FocusDirection.Next)
                            }

                            FocusEvent.Cause.TRAVERSAL_BACKWARD -> {
                                focusManager.moveFocus(FocusDirection.Previous)
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
        _composeContainer?.removeNotify()
        if (isDisposeOnRemove) {
            dispose()
        }
        super.removeNotify()
    }

    override fun setComponentOrientation(o: ComponentOrientation?) {
        super.setComponentOrientation(o)

        _composeContainer?.onChangeLayoutDirection(this)
    }

    override fun setLocale(l: Locale?) {
        super.setLocale(l)

        _composeContainer?.onChangeLayoutDirection(this)
    }

    override fun addFocusListener(l: FocusListener?) {
        _composeContainer?.contentComponent?.addFocusListener(l)
        _focusListeners.add(l)
    }

    override fun removeFocusListener(l: FocusListener?) {
        _composeContainer?.contentComponent?.removeFocusListener(l)
        _focusListeners.remove(l)
    }

    override fun isFocusable() = _isFocusable

    override fun setFocusable(focusable: Boolean) {
        _isFocusable = focusable
        _composeContainer?.contentComponent?.isFocusable = focusable
    }

    override fun isRequestFocusEnabled(): Boolean = _isRequestFocusEnabled

    override fun setRequestFocusEnabled(requestFocusEnabled: Boolean) {
        _isRequestFocusEnabled = requestFocusEnabled
        _composeContainer?.contentComponent?.isRequestFocusEnabled = requestFocusEnabled
    }

    override fun hasFocus(): Boolean {
        return _composeContainer?.contentComponent?.hasFocus() ?: false
    }

    override fun isFocusOwner(): Boolean {
        return _composeContainer?.contentComponent?.isFocusOwner ?: false
    }

    override fun requestFocus() {
        _composeContainer?.contentComponent?.requestFocus()
    }

    override fun requestFocus(temporary: Boolean): Boolean {
        return _composeContainer?.contentComponent?.requestFocus(temporary) ?: false
    }

    override fun requestFocus(cause: FocusEvent.Cause?) {
        _composeContainer?.contentComponent?.requestFocus(cause)
    }

    override fun requestFocusInWindow(): Boolean {
        return _composeContainer?.contentComponent?.requestFocusInWindow() ?: false
    }

    override fun requestFocusInWindow(cause: FocusEvent.Cause?): Boolean {
        return _composeContainer?.contentComponent?.requestFocusInWindow(cause) ?: false
    }

    override fun setFocusTraversalKeysEnabled(focusTraversalKeysEnabled: Boolean) {
        // ignore, traversal keys should always be handled by ComposeContainer
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
        get() = _composeContainer?.renderApi ?: GraphicsApi.UNKNOWN
}
