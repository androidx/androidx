/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.compose.ui.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.isClearFocusOnMouseDownEnabled
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import kotlin.reflect.KProperty
import kotlinx.coroutines.awaitCancellation

/**
 * Platform context that provides platform-specific bindings.
 */
@InternalComposeUiApi
interface PlatformContext {
    /**
     * The value that will be provided to [LocalWindowInfo] by default.
     */
    val windowInfo: WindowInfo

    /**
     * The value that will be provided to [LocalPlatformScreenReader] by default.
     */
    val screenReader: PlatformScreenReader get() = EmptyPlatformScreenReader

    /**
     * Provider of platform owners such as [LifecycleOwner] or [ViewModelStoreOwner].
     */
    val architectureComponentsOwner: PlatformArchitectureComponentsOwner get() = EmptyArchitectureComponentsOwner

    /**
     * Indicates if the Compose view is positioned in a transparent window.
     * This is used when rendering the scrim of a dialog - if set to true, a special blending mode
     * will be used to take into account the existing alpha-channel values.
     *
     * @see CanvasLayersComposeScene
     */
    val isWindowTransparent: Boolean get() = false

    /**
     * Indicates whether the transformation between local and window/screen coordinate spaces
     * includes components other than simple translation (such as rotation, scaling, or skewing).
     *
     * When this property returns `true`:
     * - Position calculations require full matrix transformations rather than simple offsets
     * - Certain optimizations for translation-only transformations cannot be applied
     *
     * @return `true` if the transformation includes rotation, scaling, skewing, or other
     *   non-translation components; `false` if only translation is used.
     *
     * @see convertLocalToWindowPosition
     * @see convertWindowToLocalPosition
     */
    val hasNonTranslationComponents: Boolean get() = false

    /**
     * Converts [localPosition] relative to the [ComposeScene] into an [Offset] relative to
     * the containing window.
     * If the [ComposeScene] is rotated, scaled, or otherwise transformed relative to the window,
     * this will not be a simple translation.
     */
    fun convertLocalToWindowPosition(localPosition: Offset): Offset =
        localPosition

    /**
     * Converts [positionInWindow] relative to the window into an [Offset] relative to
     * the [ComposeScene].
     * If the [ComposeScene] is rotated, scaled, or otherwise transformed relative to the window,
     * this will not be a simple translation.
     */
    fun convertWindowToLocalPosition(positionInWindow: Offset): Offset =
        positionInWindow

    /**
     * Converts [localPosition] relative to the [ComposeScene] into an [Offset] relative to
     * the device's screen.
     */
    fun convertLocalToScreenPosition(localPosition: Offset): Offset =
        convertLocalToWindowPosition(localPosition)

    /**
     * Converts [positionOnScreen] relative to the device's screen into an [Offset] relative to
     * the [ComposeScene].
     */
    fun convertScreenToLocalPosition(positionOnScreen: Offset): Offset =
        convertWindowToLocalPosition(positionOnScreen)

    /**
     * Determines if [OwnedLayer] should measure bounds for all drawings.
     * It's required to determine bounds of any graphics even if it was drawn out of measured
     * layout bounds (for example, shadows). It might be used to resize platform views based on
     * such bounds.
     */
    val measureDrawLayerBounds: Boolean get() = false

    val viewConfiguration: ViewConfiguration get() = DefaultViewConfiguration
    val inputModeManager: InputModeManager
    val textInputService: PlatformTextInputService get() = EmptyPlatformTextInputService

    suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
        awaitCancellation()
    }

    val textToolbar: TextToolbar get() = EmptyTextToolbar
    fun setPointerIcon(pointerIcon: PointerIcon) = Unit

    val parentFocusManager: FocusManager get() = EmptyFocusManager
    fun requestFocus(): Boolean = true

    val dragAndDropManager: PlatformDragAndDropManager get() = EmptyDragAndDropManager

    /**
     * The value that will be provided to [LocalPlatformWindowInsets] by default.
     */
    val windowInsets: PlatformWindowInsets get() = EmptyPlatformWindowInsets

    var isKeepScreenOnEnabled: Boolean
        get() = false
        set(_) {}

    /**
     * Votes for a specific frame rate to be used for rendering.
     *
     * @param frameRate The explicit frame rate value requested
     * @param frameRateCategory The frame rate category value requested as defined in [FrameRateCategory]
     */
    fun voteFrameRate(frameRate: Float, frameRateCategory: Float) = Unit

    /**
     * The listener to track [RootForTest]s.
     *
     * @see RootForTestListener
     */
    val rootForTestListener: RootForTestListener? get() = null

    /**
     * The listener to track [SemanticsOwner]s.
     *
     * @see SemanticsOwnerListener
     */
    val semanticsOwnerListener: SemanticsOwnerListener? get() = null

    /**
     * Returns whether mouse-down on an unfocusable element clears focus.
     */
    val isClearFocusOnMouseDownEnabled: Boolean
        get() = ComposeUiFlags.isClearFocusOnMouseDownEnabled

    interface RootForTestListener {
        fun onRootForTestCreated(root: PlatformRootForTest)
        fun onRootForTestDisposed(root: PlatformRootForTest)
    }

    interface SemanticsOwnerListener {
        /**
         * Callback method that is called when a [SemanticsOwner] is appended to tracking.
         * A new [SemanticsOwner] is always created above existing ones.
         */
        fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner)

        /**
         * Callback method that is called when a [SemanticsOwner] is disposed.
         */
        fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner)

        /**
         * Callback method that is called when a [SemanticsNode] is added to or deleted from
         * the Semantics tree. It will also be called when a [SemanticsNode] in the Semantics tree
         * has some property change.
         *
         * @param semanticsOwner the [SemanticsOwner] whose semantics have changed
         *
         * @see Owner.onSemanticsChange
         */
        fun onSemanticsChange(semanticsOwner: SemanticsOwner)

        /**
         * Callback method that is called when the position and/or size of the [LayoutNode] with
         * the given semantics id changed.
         *
         * Note that the id, rather than the [LayoutNode] itself, is passed here because
         * [LayoutNode] is an internal type, so it can't be exposed in a public method.
         */
        fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int)
    }

    @InternalComposeUiApi
    open class Empty : PlatformContext {
        override val windowInfo: WindowInfo = WindowInfoImpl().apply {
            // true is a better default if the platform doesn't provide WindowInfo.
            // otherwise UI will always be rendered in unfocused mode
            // (hidden text field cursor, gray title bar, etc.)
            isWindowFocused = true
        }

        override val inputModeManager: InputModeManager = DefaultInputModeManager()
    }

    // This object must be immutable because it is used as a delegate in other ViewConfiguration
    // implementations
    @InternalComposeUiApi
    object DefaultViewConfiguration : ViewConfiguration {
        override val longPressTimeoutMillis: Long = 500
        override val doubleTapTimeoutMillis: Long = 300
        override val doubleTapMinTimeMillis: Long = 40
        override val touchSlop: Float = 18f
    }

    // This object must be immutable because it is used directly in several PlatformContext
    // implementations
    @InternalComposeUiApi
    object EmptyFocusManager : FocusManager {
        override fun clearFocus(force: Boolean) = Unit
        override fun moveFocus(focusDirection: FocusDirection) = false
    }
}

private object EmptyPlatformScreenReader : PlatformScreenReader {
    override val isActive: Boolean = false
}

private val EmptyArchitectureComponentsOwner = DefaultArchitectureComponentsOwner(
    enforceMainThread = false
).apply {
    enableSavedStateHandles()
    setLifecycleState(Lifecycle.State.RESUMED)
}

internal class DefaultInputModeManager(
    initialInputMode: InputMode = InputMode.Keyboard
) : InputModeManager {
    override var inputMode: InputMode by mutableStateOf(initialInputMode)

    @ExperimentalComposeUiApi
    override fun requestInputMode(inputMode: InputMode) =
        if (inputMode == InputMode.Touch || inputMode == InputMode.Keyboard) {
            this.inputMode = inputMode
            true
        } else {
            false
        }
}

private object EmptyPlatformTextInputService : PlatformTextInputService {
    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) = Unit

    override fun stopInput() = Unit
    override fun showSoftwareKeyboard() = Unit
    override fun hideSoftwareKeyboard() = Unit
    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) = Unit
}

private object EmptyTextToolbar : TextToolbar {
    override fun hide() = Unit
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) = Unit
}

private object EmptyDragAndDropManager : PlatformDragAndDropManager

/**
 * Helper delegate to re-send missing events to a new listener.
 */
internal class DelegateRootForTestListener : PlatformContext.RootForTestListener {
    private val roots = mutableSetOf<PlatformRootForTest>()
    private var listener: PlatformContext.RootForTestListener? = null

    override fun onRootForTestCreated(root: PlatformRootForTest) {
        roots.add(root)
        listener?.onRootForTestCreated(root)
    }

    override fun onRootForTestDisposed(root: PlatformRootForTest) {
        roots.remove(root)
        listener?.onRootForTestDisposed(root)
    }

    @Suppress("RedundantNullableReturnType")
    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): PlatformContext.RootForTestListener? {
        return this
    }

    operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: PlatformContext.RootForTestListener?
    ) {
        listener = value
        sendMissingEvents()
    }

    private fun sendMissingEvents() {
        for (root in roots) {
            listener?.onRootForTestCreated(root)
        }
    }
}
