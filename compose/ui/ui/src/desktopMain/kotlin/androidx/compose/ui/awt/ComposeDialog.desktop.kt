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
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.layout.MeasurableRootContent
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.UndecoratedWindowResizer
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.savedstate.SavedState
import java.awt.Component
import java.awt.ComponentOrientation
import java.awt.Frame
import java.awt.GraphicsConfiguration
import java.awt.Window
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelListener
import java.util.Locale
import javax.swing.JDialog
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayerAnalytics

/**
 * System dialog for displaying Compose UI, inheriting [javax.swing.JDialog].
 */
class ComposeDialog : JDialog {
    private val composePanel: ComposeWindowPanel

    private fun createComposePanel(
        skiaLayerAnalytics: SkiaLayerAnalytics,
        savedState: SavedState?,
        coroutineContext: CoroutineContext
    ) = ComposeWindowPanel(
        window = this,
        isUndecorated = ::isUndecorated,
        skiaLayerAnalytics = skiaLayerAnalytics,
        savedState = savedState,
        coroutineContext = coroutineContext
    )

    /**
     * System dialog for displaying Compose UI, inheriting [javax.swing.JDialog].
     *
     * @param owner the [java.awt.Window] from which the dialog is displayed or `null` if this dialog has no owner.
     * @param modalityType specifies whether dialog blocks input to other windows when shown.
     * @param graphicsConfiguration [GraphicsConfiguration] of the target screen device; if `null`, the default system
     * [GraphicsConfiguration] is assumed.
     * @param skiaLayerAnalytics Allows receiving notifications about the underlying Skia layer behavior.
     * @param savedState The saved state to restore the UI state from a previous instance.
     * @param coroutineContext The coroutine context for Compose content rendering and effects.
     */
    // All constructors that want to call JDialog(Window?) should call this constructor.
    // On Windows, it will show a taskbar icon if the owner window is null
    @ExperimentalComposeUiApi
    constructor(
        owner: Window?,
        modalityType: ModalityType = ModalityType.MODELESS,
        graphicsConfiguration: GraphicsConfiguration? = null,
        skiaLayerAnalytics: SkiaLayerAnalytics = SkiaLayerAnalytics.Empty,
        savedState: SavedState? = null,
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
    ) : super(owner, "", modalityType, graphicsConfiguration) {
        composePanel = createComposePanel(skiaLayerAnalytics, savedState, coroutineContext)
        contentPane.add(composePanel)
    }

    /**
     * System dialog for displaying Compose UI, inheriting [javax.swing.JDialog].
     *
     * @param owner the [java.awt.Frame] from which the dialog is displayed or `null` if this dialog has no owner.
     * @param modal Whether the dialog blocks input to other windows of the app.
     * @param graphicsConfiguration [GraphicsConfiguration] of the target screen device; if `null`, the default system
     * [GraphicsConfiguration] is assumed.
     * @param skiaLayerAnalytics Allows receiving notifications about the underlying Skia layer behavior.
     * @param savedState The saved state to restore the UI state from a previous instance.
     * @param coroutineContext The coroutine context for Compose content rendering and effects.
     */
    // All constructors that want to call JDialog(Frame?) should call this constructor first.
    // It will not show a taskbar icon if the owner frame is null
    @ExperimentalComposeUiApi
    constructor(
        owner: Frame?,
        modal: Boolean = false,
        graphicsConfiguration: GraphicsConfiguration? = null,
        skiaLayerAnalytics: SkiaLayerAnalytics = SkiaLayerAnalytics.Empty,
        savedState: SavedState? = null,
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
    ) : super(owner, "", modal, graphicsConfiguration) {
        composePanel = createComposePanel(skiaLayerAnalytics, savedState, coroutineContext)
        contentPane.add(composePanel)
    }

    /**
     * System dialog for displaying Compose UI, inheriting [javax.swing.JDialog].
     *
     * @param skiaLayerAnalytics Allows receiving notifications about the underlying Skia layer behavior.
     * @param savedState The saved state to restore the UI state from a previous instance.
     * @param coroutineContext The coroutine context for Compose content rendering and effects.
     */
    @ExperimentalComposeUiApi
    constructor(
        skiaLayerAnalytics: SkiaLayerAnalytics = SkiaLayerAnalytics.Empty,
        savedState: SavedState? = null,
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
    ): this(
        owner = null as Frame?,
        skiaLayerAnalytics = skiaLayerAnalytics,
        savedState = savedState,
        coroutineContext = coroutineContext
    )

    /**
     * System dialog for displaying Compose UI, inheriting [javax.swing.JDialog].
     *
     * @param owner the [java.awt.Window] from which the dialog is displayed or `null` if this dialog has no owner.
     * @param modalityType specifies whether dialog blocks input to other windows when shown.
     * @param graphicsConfiguration [GraphicsConfiguration] of the target screen device; if `null`, the default system
     * [GraphicsConfiguration] is assumed.
     */
    constructor(
        owner: Window?,
        modalityType: ModalityType = ModalityType.MODELESS,
        graphicsConfiguration: GraphicsConfiguration? = null,
    ) : this(
        owner = owner,
        modalityType = modalityType,
        graphicsConfiguration = graphicsConfiguration,
        skiaLayerAnalytics = SkiaLayerAnalytics.Empty,
        savedState = null,
    )

    /**
     * System dialog for displaying Compose UI, inheriting [javax.swing.JDialog].
     *
     * @param graphicsConfiguration [GraphicsConfiguration] of the target screen device; if `null`, the default system
     * [GraphicsConfiguration] is assumed.
     * @param coroutineContext The coroutine context for Compose content rendering and effects.
     */
    @ExperimentalComposeUiApi
    constructor(
        graphicsConfiguration: GraphicsConfiguration? = null,
        coroutineContext: CoroutineContext = EmptyCoroutineContext
    ) : this(
        owner = null as Frame?,
        graphicsConfiguration = graphicsConfiguration,
        skiaLayerAnalytics = SkiaLayerAnalytics.Empty,
        savedState = null,
        coroutineContext = coroutineContext
    )

    /**
     * System dialog for displaying Compose UI, inheriting [javax.swing.JDialog].
     *
     * @param graphicsConfiguration [GraphicsConfiguration] of the target screen device; if `null`, the default system
     * [GraphicsConfiguration] is assumed.
     */
    constructor(
        graphicsConfiguration: GraphicsConfiguration? = null,
    ) : this(
        owner = null as Frame?,
        graphicsConfiguration = graphicsConfiguration,
        skiaLayerAnalytics = SkiaLayerAnalytics.Empty,
        savedState = null,
    )

    /**
     * System dialog for displaying Compose UI, inheriting [javax.swing.JDialog].
     */
    constructor() : this(
        owner = null as Frame?,
    )

    internal var rootForTestListener
        get() = composePanel.rootForTestListener
        set(value) { composePanel.rootForTestListener = value }

    /**
     * Controls whether mouse-down on an unfocusable element clears focus.
     */
    @ExperimentalComposeUiApi
    var isClearFocusOnMouseDownEnabled: Boolean
        get() = composePanel.isClearFocusOnMouseDownEnabled
        set(value) { composePanel.isClearFocusOnMouseDownEnabled = value }

    private val undecoratedWindowResizer = UndecoratedWindowResizer(this)

    override fun add(component: Component) = composePanel.add(component)

    override fun remove(component: Component) = composePanel.remove(component)

    override fun setComponentOrientation(o: ComponentOrientation?) {
        super.setComponentOrientation(o)

        composePanel.onChangeLayoutDirection(this)
    }

    override fun setLocale(l: Locale?) {
        super.setLocale(l)

        // setLocale is called from JFrame constructor, before ComposeDialog has been initialized
        @Suppress("UNNECESSARY_SAFE_CALL")
        composePanel?.onChangeLayoutDirection(this)
    }

    /**
     * Composes the given composable into the ComposeDialog.
     *
     * @param content Composable content of the ComposeDialog.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun setContent(
        content: @Composable DialogWindowScope.() -> Unit
    ) = setContent(
        onPreviewKeyEvent = { false },
        onKeyEvent = { false },
        content = content
    )

    /**
     * Handler to catch uncaught exceptions during rendering frames, handling events,
     * or processing background Compose operations. If null, then exceptions throw
     * further up the call stack.
     */
    @ExperimentalComposeUiApi
    var exceptionHandler: WindowExceptionHandler?
        get() = composePanel.exceptionHandler
        set(value) {
            composePanel.exceptionHandler = value
        }

    /**
     * Top-level composition locals, which will be provided for the Composable content, which is set by [setContent].
     *
     * `null` if no composition locals should be provided.
     */
    var compositionLocalContext: CompositionLocalContext?
        get() = composePanel.compositionLocalContext
        set(value) {
            composePanel.compositionLocalContext = value
        }

    /**
     * Composes the given composable into the ComposeDialog.
     *
     * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
     * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
     * Return true to stop propagation of this event. If you return false, the key event will be
     * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
     * it will be sent back up to the root using the onKeyEvent callback.
     * @param onKeyEvent This callback is invoked when the user interacts with the hardware
     * keyboard. While implementing this callback, return true to stop propagation of this event.
     * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
     * @param content Composable content of the ComposeWindow.
     */
    @ExperimentalComposeUiApi
    fun setContent(
        onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
        onKeyEvent: ((KeyEvent) -> Boolean) = { false },
        content: @Composable DialogWindowScope.() -> Unit
    ) {
        val scope = object : DialogWindowScope {
            override val window: ComposeDialog get() = this@ComposeDialog
        }
        composePanel.setContent(
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent,
            modifier = Modifier.semantics { dialog() },
        ) {
            scope.content()
            undecoratedWindowResizer.Content(
                modifier = Modifier.layoutId("UndecoratedWindowResizer")
            )
        }
    }

    /**
     * The thickness of the resizers used when the dialog is undecorated and resizable.
     */
    var undecoratedResizerThickness: Dp by undecoratedWindowResizer::resizerThickness

    /**
     * Saves the current UI state into a [SavedState] object. The returned state can be used
     * to restore the UI state later by passing it to the constructor's `savedState` parameter.
     *
     * @return A [SavedState] object containing the current UI state.
     */
    @ExperimentalComposeUiApi
    fun saveState(): SavedState? {
        return composePanel.saveState()
    }

    /**
     * Returns an object through which the composable content of the window can be queried for its
     * size preferences, such as its intrinsic size.
     */
    @ExperimentalComposeUiApi
    val measurableContent: MeasurableRootContent
        get() = composePanel.measurableContent

    override fun dispose() {
        super.dispose()
        composePanel.dispose()
    }

    override fun setUndecorated(value: Boolean) {
        super.setUndecorated(value)
        undecoratedWindowResizer.enabled = isUndecorated && isResizable
    }

    override fun setResizable(value: Boolean) {
        super.setResizable(value)
        undecoratedWindowResizer.enabled = isUndecorated && isResizable
    }

    /**
     * `true` if background of the window is transparent, `false` otherwise
     * Transparency should be set only if window is not showing and `isUndecorated` is set to
     * `true`, otherwise AWT will throw an exception.
     */
    var isTransparent: Boolean
        get() = composePanel.isWindowTransparent
        set(value) {
            composePanel.isWindowTransparent = value
            hasMacOsShadow = !value
        }

    /**
     * Registers a task to run when the rendering API changes.
     */
    fun onRenderApiChanged(action: () -> Unit) {
        composePanel.onRenderApiChanged(action)
    }

    /**
     * Renders the dialog's content synchronously.
     *
     * This doesn't need to be used in most cases, as the content will be rendered as needed
     * automatically. It can, however, be used to force the rendering sooner than it normally would
     * occur. Specifically, it allows rendering the content after the dialog has been made
     * displayable, but before it has been shown, to avoid a brief flicker.
     */
    @ExperimentalComposeUiApi
    fun renderImmediately() {
        composePanel.renderImmediately()
    }

    /**
     * Retrieve underlying platform-specific operating system handle for the root window where
     * ComposeDialog is rendered. Currently returns HWND on Windows, Window on X11 and NSWindow
     * on macOS.
     */
    val windowHandle: Long get() = composePanel.windowHandle

    /**
     * Returns low-level rendering API used for rendering in this ComposeDialog. API is
     * automatically selected based on operating system, graphical hardware and `SKIKO_RENDER_API`
     * environment variable.
     */
    val renderApi: GraphicsApi get() = composePanel.renderApi

    // We need overridden listeners because we mix Swing and AWT components in the
    // org.jetbrains.skiko.SkiaLayer, they don't work well together.
    // TODO(demin): is it possible to fix that without overriding?

    override fun addMouseListener(listener: MouseListener) =
        composePanel.addMouseListener(listener)

    override fun removeMouseListener(listener: MouseListener) =
        composePanel.removeMouseListener(listener)

    override fun addMouseMotionListener(listener: MouseMotionListener) =
        composePanel.addMouseMotionListener(listener)

    override fun removeMouseMotionListener(listener: MouseMotionListener) =
        composePanel.removeMouseMotionListener(listener)

    override fun addMouseWheelListener(listener: MouseWheelListener) =
        composePanel.addMouseWheelListener(listener)

    override fun removeMouseWheelListener(listener: MouseWheelListener) =
        composePanel.removeMouseWheelListener(listener)

    /**
     * Set the visual debug option that shows bounds for all nodes in the hierarchy.
     */
    @InternalComposeUiApi
    var showLayoutBounds: Boolean
        get() {
            return composePanel.showLayoutBounds
        }
        set(value) {
            composePanel.showLayoutBounds = value
        }
}
