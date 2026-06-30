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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.ui.ComposeDesktopEntryPoint
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LayerType
import androidx.compose.ui.awt.RenderSettings.SkiaSurface
import androidx.compose.ui.awt.RenderSettings.SwingGraphics
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.isClearFocusOnMouseDownEnabled
import androidx.compose.ui.scene.ComposeContainer
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.savedstate.SavedState
import java.awt.Color
import java.awt.Component
import java.awt.ComponentOrientation
import java.awt.Container
import java.awt.Dimension
import java.awt.FocusTraversalPolicy
import java.awt.Window
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.image.BufferedImage
import java.util.*
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import javax.swing.SwingUtilities.isEventDispatchThread
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayerAnalytics

/**
 * A component for embedding Compose content into Swing applications.
 *
 * @param skiaLayerAnalytics An object to which analytics about the underlying Skia layer is
 *   reported. Skia layer is the underlying implementation used internally to draw Compose content.
 *   The [SkiaLayerAnalytics] could, for example, send the information to a centralized analytics
 *   gatherer.
 * @param savedState The saved state to restore the UI state from a previous instance.
 * @param renderSettings Configuration class for rendering settings.
 * @param coroutineContext The coroutine context for Compose content rendering and effects.
 */
@OptIn(ComposeToolingApi::class)
class ComposePanel @ExperimentalComposeUiApi constructor(
    private val skiaLayerAnalytics: SkiaLayerAnalytics = SkiaLayerAnalytics.Empty,
    private var savedState: SavedState? = null,
    private val renderSettings: RenderSettings = DefaultRenderSettings,
    private val coroutineContext: CoroutineContext = EmptyCoroutineContext
) : JLayeredPane(), ComposeDesktopEntryPoint {
    constructor() : this(
        savedState = null,
        skiaLayerAnalytics = SkiaLayerAnalytics.Empty,
        renderSettings = DefaultRenderSettings
    )

    companion object {
        private val SwingGraphics = SwingGraphics()

        private val SkiaSurface = SkiaSurface()

        /**
         * [RenderSettings] based on the current environment variable configuration.
         *
         * @see ComposeFeatureFlags.useSwingGraphicsInComposePanel
         */
        @ExperimentalComposeUiApi
        val DefaultRenderSettings: RenderSettings
            get() = if (ComposeFeatureFlags.useSwingGraphicsInComposePanel.value) {
                SwingGraphics
            } else {
                SkiaSurface
            }
    }

    init {
        check(isEventDispatchThread()) {
            "ComposePanel should be created inside AWT Event Dispatch Thread" +
                " (use SwingUtilities.invokeLater).\n" +
                "Creating from another thread isn't supported."
        }
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
        isFocusable = true
    }

    private var windowParent by mutableStateOf<Window?>(null)

    private val _focusListeners = mutableSetOf<FocusListener?>()

    private var _composeContainer: ComposeContainer? = null
    private var _composeContent: (@Composable () -> Unit)? = null

    /**
     * Controls whether mouse-down on an unfocusable element clears focus.
     */
    @ExperimentalComposeUiApi
    var isClearFocusOnMouseDownEnabled: Boolean = ComposeUiFlags.isClearFocusOnMouseDownEnabled
        set(value) {
            field = value
            _composeContainer?.isClearFocusOnMouseDownEnabled = value
        }

    /**
     * Determines whether the Compose state in [ComposePanel] should be disposed
     * when the panel is detached from the Swing hierarchy (when [removeNotify] is called).
     *
     * If it is set to false, it is developer's responsibility to call [dispose] function
     * when Compose state and all related to [ComposePanel] resources are no longer needed.
     * It can be useful for cases when [ComposePanel] can be attached/detached to Swing hierarchy
     * multiple times, so with [isDisposeOnRemove] = `false` state will be preserved.
     *
     * On the other hand, [isDisposeOnRemove] = `true` can be useful for stateless components,
     * that can be recreated for each attaching to Swing hierarchy.
     *
     * @see dispose
     */
    @ExperimentalComposeUiApi
    var isDisposeOnRemove: Boolean = true

    /**
     * Determines whether unconsumed mouse wheel events will be propagated to the parent component.
     *
     * When set to `true`, mouse wheel events that are not consumed by the Compose UI will be passed
     * to Swing for further processing. This is useful when placing [ComposePanel] inside a
     * scrollable Swing container (i.e. [javax.swing.JScrollPane]).
     *
     * When set to `false`, [ComposePanel] will behave like [javax.swing.JScrollPane], consuming all
     * mouse wheel events that occur over it.
     *
     * To configure this behavior globally, set
     * [ComposeFeatureFlags.redispatchUnconsumedMouseWheelEvents].
     *
     * @see ComposeFeatureFlags.redispatchUnconsumedMouseWheelEvents
     */
    @ExperimentalComposeUiApi
    var redispatchUnconsumedMouseWheelEvents: Boolean =
        ComposeFeatureFlags.redispatchUnconsumedMouseWheelEvents.value
        set(value) {
            field = value
            _composeContainer?.redispatchUnconsumedMouseWheelEvents = value
        }

    /**
     * Saves the current UI state into a [SavedState] object. The returned state can be used
     * to restore the UI state later by passing it to the constructor's [savedState] parameter.
     *
     * @return A [SavedState] object containing the current UI state.
     */
    @ExperimentalComposeUiApi
    fun saveState(): SavedState? {
        return _composeContainer?.saveState()
    }

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
        val composeContainer = _composeContainer ?: return
        savedState = composeContainer.saveState()
        composeContainer.dispose()
        _composeContainer = null
    }

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        super.setBounds(x, y, width, height)
        _composeContainer?.setBounds(0, 0, width, height)
    }

    private fun Dimension.actualize(): Dimension {
        return _composeContainer?.actualizeSize(this, insets) ?: this
    }

    /**
     * Sets the minimum size of the composable content.
     *
     * The width and height of [size] can either be a regular, specific, value or
     * [UNSPECIFIED_DIMENSION_VALUE], which means the content will
     * determine the minimum size on the corresponding axis. The content will be measured
     * unconstrained on that axis, and the resulting size will be used.
     */
    override fun setMinimumSize(size: Dimension?) {
        super.setMinimumSize(size)
    }

    @OptIn(ExperimentalUnitApi::class)
    @Suppress("RedundantNullableReturnType")  // https://youtrack.jetbrains.com/issue/KT-31094
    override fun getMinimumSize(): Dimension? {
        return super.getMinimumSize().actualize()
    }

    /**
     * Sets the preferred size of the [ComposePanel].
     *
     * The width and height of [size] can either be a regular, specific, value or
     * [UNSPECIFIED_DIMENSION_VALUE], which means the content will
     * determine the preferred size on the corresponding axis. The content will be measured
     * unconstrained on that axis, and the resulting size will be used.
     */
    override fun setPreferredSize(size: Dimension?) {
        super.setPreferredSize(size)
    }

    @OptIn(ExperimentalUnitApi::class)
    @Suppress("RedundantNullableReturnType")  // https://youtrack.jetbrains.com/issue/KT-31094
    override fun getPreferredSize(): Dimension? {
        val size = if (isPreferredSizeSet) super.getPreferredSize() else UnspecifiedDimension()
        return size.actualize()
    }

    /**
     * Sets the maximum size of the composable content.
     *
     * The width and height of [size] can either be a regular, specific, value or
     * [UNSPECIFIED_DIMENSION_VALUE], which means the content will
     * determine the maximum size on the corresponding axis. The content will be measured
     * unconstrained on that axis, and the resulting size will be used.
     */
    override fun setMaximumSize(size: Dimension?) {
        super.setMaximumSize(size)
    }

    @OptIn(ExperimentalUnitApi::class)
    @Suppress("RedundantNullableReturnType")  // https://youtrack.jetbrains.com/issue/KT-31094
    override fun getMaximumSize(): Dimension? {
        return super.getMaximumSize().actualize()
    }

    override fun setBackground(bg: Color?) {
        // Note that unless `setOpaque(true)` is called, JLayeredPane will not paint the background
        super.setBackground(bg)
        _composeContainer?.contentComponent?.background = bg
    }

    /**
     * Sets Compose content of the ComposePanel.
     *
     * @param content Composable content of the ComposePanel.
     */
    fun setContent(content: @Composable () -> Unit) {
        // The window (or root container) may not be ready to render composable content, so we need
        // to store the composable lambda and set the content only when everything is ready to
        // avoid accidental crashes and memory leaks.
        val wrappedContent = wrapContent(content)
        _composeContent = wrappedContent
        _composeContainer?.setContent(wrappedContent)
    }

    private fun wrapContent(content: @Composable () -> Unit): @Composable () -> Unit {
        return {
            CompositionLocalProvider(LocalAwtWindow provides windowParent) {
                content()
            }
        }
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
     * A container used for additional layers and as the reference for window coordinate space.
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

    // Needed to preserve binary compatibility
    @Suppress("RedundantOverride")
    override fun add(component: Component): Component = super.add(component)

    // Needed to preserve binary compatibility
    @Suppress("RedundantOverride")
    override fun remove(component: Component) = super.remove(component)

    override fun addNotify() {
        super.addNotify()

        // After [super.addNotify] is called, we can safely initialize the bridge and composable
        // content.
        val composeContainer = _composeContainer ?: createComposeContainer().also {
            _composeContainer = it
            windowParent = SwingUtilities.getWindowAncestor(this)
            it.redispatchUnconsumedMouseWheelEvents = redispatchUnconsumedMouseWheelEvents
            it.showLayoutBounds = showLayoutBounds
            val composeContent = _composeContent
            if (composeContent != null) {
                it.setContent(composeContent)
            }
            savedState = null
        }
        composeContainer.addNotify()
    }

    private fun createComposeContainer(): ComposeContainer {
        return ComposeContainer(
            container = this,
            isWindowLevel = false,
            skiaLayerAnalytics = skiaLayerAnalytics,
            savedState = savedState,
            windowContainer = windowContainer,
            renderSettings = renderSettings,
            coroutineContext = coroutineContext,
        ).apply {
            setBounds(0, 0, width, height)
            contentComponent.isFocusable = isFocusable
            contentComponent.isRequestFocusEnabled = isRequestFocusEnabled
            contentComponent.background = background
            exceptionHandler = this@ComposePanel.exceptionHandler

            _focusListeners.forEach(contentComponent::addFocusListener)
            contentComponent.addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent) {
                    if (!e.isTemporary && !e.isFocusGainedHandledBySwingPanel(this@ComposePanel)) {
                        when (e.cause) {
                            FocusEvent.Cause.UNKNOWN, FocusEvent.Cause.ACTIVATION -> {
                                if (!focusManager.hasFocus) {
                                    focusManager.takeFocus(FocusDirection.Next)
                                }
                            }

                            else -> Unit
                        }
                    }
                }

                override fun focusLost(e: FocusEvent) = Unit
            })

            isClearFocusOnMouseDownEnabled = this@ComposePanel.isClearFocusOnMouseDownEnabled
        }
    }

    override fun removeNotify() {
        _composeContainer?.removeNotify()
        if (isDisposeOnRemove) {
            dispose()
        }
        windowParent = null
        super.removeNotify()
    }

    override fun setComponentOrientation(o: ComponentOrientation?) {
        super.setComponentOrientation(o)

        _composeContainer?.onLayoutDirectionChanged(this)
    }

    override fun setLocale(l: Locale?) {
        super.setLocale(l)

        _composeContainer?.onLayoutDirectionChanged(this)
    }

    override fun addFocusListener(l: FocusListener?) {
        _composeContainer?.contentComponent?.addFocusListener(l)
        _focusListeners.add(l)
    }

    override fun removeFocusListener(l: FocusListener?) {
        _composeContainer?.contentComponent?.removeFocusListener(l)
        _focusListeners.remove(l)
    }

    override fun setFocusable(focusable: Boolean) {
        super.setFocusable(focusable)
        _composeContainer?.contentComponent?.isFocusable = focusable
    }

    override fun setRequestFocusEnabled(requestFocusEnabled: Boolean) {
        super.setRequestFocusEnabled(requestFocusEnabled)
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

    /**
     * Renders the panel's content synchronously.
     *
     * This doesn't need to be used in most cases, as the content will be rendered as needed
     * automatically. It can, however, be used to force the rendering sooner than it normally would
     * occur. Specifically, it allows rendering the content after the window has been made
     * displayable, but before it has been shown, to avoid a brief flicker.
     */
    @ExperimentalComposeUiApi
    fun renderImmediately() {
        _composeContainer?.renderImmediately()
    }

    /**
     * Set the visual debug option that shows bounds for all nodes in the hierarchy.
     */
    @InternalComposeUiApi
    var showLayoutBounds: Boolean = _composeContainer?.showLayoutBounds ?: false
        set(value) {
            // We're assuming we own the scene and thus this value, and nobody
            // else will change it from under us, so we never get out of sync.
            field = value
            _composeContainer?.showLayoutBounds = value
        }

    /**
     * Returns the [SemanticsOwner]s corresponding to the roots of the semantics trees in this
     * [ComposePanel].
     *
     * This is backed by Snapshot state, so reading this property in a restartable function (e.g., a
     * composable function) will cause the function to restart when the set of semantics owners
     * changes.
     */
    @ComposeToolingApi
    override val semanticsOwners: Collection<SemanticsOwner>
        get() = _composeContainer?.semanticsOwners ?: emptyList()

    /**
     * Captures the content of this panel into an image.
     *
     * Returns `null` if the panel has not been made visible yet.
     *
     * May be called only on the event dispatching thread.
     */
    @ComposeToolingApi
    override fun captureContentToImage(): BufferedImage? {
        return _composeContainer?.captureContentToImage()
    }
}
