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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.layout.EmptyLayout
import androidx.compose.ui.layout.OverlayLayout
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import java.awt.Component
import java.awt.Container
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.LayoutFocusTraversalPolicy
import javax.swing.SwingUtilities
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.atomicfu.atomic

val NoOpUpdate: Component.() -> Unit = {}

/**
 * Composes an AWT/Swing component obtained from [factory]. The [factory] block will be called
 * to obtain the [Component] to be composed.
 *
 * By default, the Swing component is placed on top of the Compose layer (that means that Compose
 * content can't overlap or clip it). It might be changed by `compose.interop.blending` system
 * property. See [ComposeFeatureFlags.useInteropBlending].
 *
 * The [update] block runs due to recomposition, this is the place to set [Component] properties
 * depending on state. When state changes, the block will be re-executed to set the new properties.
 *
 * @param background Background color of SwingPanel
 * @param factory The block creating the [Component] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param update The callback to be invoked after the layout is inflated.
 */
@Composable
public fun <T : Component> SwingPanel(
    background: Color = Color.White,
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate,
) {
    val interopContainer = LocalSwingInteropContainer.current
    val compositeKey = currentCompositeKeyHash
    val interopComponent = remember {
        SwingInteropComponent(
            container = SwingPanelContainer(
                key = compositeKey,
                focusComponent = interopContainer.container,
            ),
            update = update,
        )
    }

    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val focusSwitcher = remember { FocusSwitcher(interopComponent, focusManager) }

    OverlayLayout(
        modifier = modifier.onGloballyPositioned { coordinates ->
            val rootCoordinates = coordinates.findRootCoordinates()
            val clippedBounds = rootCoordinates
                .localBoundingBoxOf(coordinates, clipBounds = true).round(density)
            val bounds = rootCoordinates
                .localBoundingBoxOf(coordinates, clipBounds = false).round(density)

            interopComponent.setBounds(bounds, clippedBounds)
            interopContainer.validateComponentsOrder()
        }.drawBehind {
            // Clear interop area to make visible the component under our canvas.
            drawRect(Color.Transparent, blendMode = BlendMode.Clear)
        }.trackSwingInterop(interopContainer, interopComponent)
            .then(InteropPointerInputModifier(interopComponent))
    ) {
        focusSwitcher.Content()
    }

    DisposableEffect(Unit) {
        val focusListener = object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                if (e.isFocusGainedHandledBySwingPanel(interopComponent.container)) {
                    when (e.cause) {
                        FocusEvent.Cause.TRAVERSAL_FORWARD -> focusSwitcher.moveForward()
                        FocusEvent.Cause.TRAVERSAL_BACKWARD -> focusSwitcher.moveBackward()
                        else -> Unit
                    }
                }
            }

            override fun focusLost(e: FocusEvent) = Unit
        }
        interopContainer.container.addFocusListener(focusListener)
        onDispose {
            interopContainer.container.removeFocusListener(focusListener)
        }
    }

    DisposableEffect(factory) {
        interopComponent.setupUserComponent(factory())
        onDispose {
            interopComponent.cleanUserComponent()
        }
    }

    SideEffect {
        interopComponent.container.background = background.toAwtColor()
        interopComponent.update = update
    }
}

/**
 * Returns whether the event is handled by SwingPanel.
 *
 * The focus can be switched from the child component inside SwingPanel.
 * In that case, SwingPanel will take care of it.
 *
 * The alternative that needs more investigation is to
 * not use ComposePanel as next/previous focus element for SwingPanel children
 * (see [SwingPanelContainer.focusComponent])
 */
internal fun FocusEvent.isFocusGainedHandledBySwingPanel(container: Container) =
    container.isParentOf(oppositeComponent)

/**
 * A container for [SwingPanel]'s component. Takes care about focus and clipping.
 *
 * @param key The unique identifier for the panel container.
 * @param focusComponent The component that should receive focus.
 */
private class SwingPanelContainer(
    key: Int,
    private val focusComponent: Component
): JPanel() {
    init {
        name = "SwingPanel #${key.toString(MaxSupportedRadix)}"
        layout = null
        focusTraversalPolicy = object : LayoutFocusTraversalPolicy() {
            override fun getComponentAfter(aContainer: Container?, aComponent: Component?): Component? {
                return if (aComponent == getLastComponent(aContainer)) {
                    focusComponent
                } else {
                    super.getComponentAfter(aContainer, aComponent)
                }
            }

            override fun getComponentBefore(aContainer: Container?, aComponent: Component?): Component? {
                return if (aComponent == getFirstComponent(aContainer)) {
                    focusComponent
                } else {
                    super.getComponentBefore(aContainer, aComponent)
                }
            }
        }
        isFocusCycleRoot = true
    }
}

private class FocusSwitcher<T : Component>(
    private val interopComponent: SwingInteropComponent<T>,
    private val focusManager: FocusManager,
) {
    private val backwardTracker = FocusTracker {
        val container = interopComponent.container
        val component = container.focusTraversalPolicy.getFirstComponent(container)
        if (component != null) {
            component.requestFocus(FocusEvent.Cause.TRAVERSAL_FORWARD)
        } else {
            moveForward()
        }
    }

    private val forwardTracker = FocusTracker {
        val component = interopComponent.container.focusTraversalPolicy.getLastComponent(interopComponent.container)
        if (component != null) {
            component.requestFocus(FocusEvent.Cause.TRAVERSAL_BACKWARD)
        } else {
            moveBackward()
        }
    }

    fun moveBackward() {
        backwardTracker.requestFocusWithoutEvent()
        focusManager.moveFocus(FocusDirection.Previous)
    }

    fun moveForward() {
        forwardTracker.requestFocusWithoutEvent()
        focusManager.moveFocus(FocusDirection.Next)
    }

    @Composable
    fun Content() {
        EmptyLayout(backwardTracker.modifier)
        EmptyLayout(forwardTracker.modifier)
    }

    /**
     * A helper class that can help:
     * - to prevent recursive focus events
     *   (a case when we focus the same element inside `onFocusEvent`)
     * - to prevent triggering `onFocusEvent` while requesting focus somewhere else
     */
    private class FocusTracker(
        private val onNonRecursiveFocused: () -> Unit
    ) {
        private val requester = FocusRequester()

        private var isRequestingFocus = false
        private var isHandlingFocus = false

        fun requestFocusWithoutEvent() {
            try {
                isRequestingFocus = true
                requester.requestFocus()
            } finally {
                isRequestingFocus = false
            }
        }

        val modifier = Modifier
            .focusRequester(requester)
            .onFocusEvent {
                if (!isRequestingFocus && !isHandlingFocus && it.isFocused) {
                    try {
                        isHandlingFocus = true
                        onNonRecursiveFocused()
                    } finally {
                        isHandlingFocus = false
                    }
                }
            }
            .focusTarget()
    }
}

private class SwingInteropComponent<T : Component>(
    container: SwingPanelContainer,
    var update: (T) -> Unit
): InteropComponent(container) {
    private var userComponent: T? = null
    private var updater: Updater<T>? = null

    fun setupUserComponent(component: T) {
        check(userComponent == null)
        userComponent = component
        container.add(component)
        updater = Updater(component, update)
    }

    fun cleanUserComponent() {
        container.remove(userComponent)
        updater?.dispose()
        userComponent = null
        updater = null
    }

    fun setBounds(
        bounds: IntRect,
        clippedBounds: IntRect = bounds
    ) {
        clipBounds = clippedBounds // Clipping area for skia canvas
        container.isVisible = !clippedBounds.isEmpty // Hide if it's fully clipped
        // Swing clips children based on parent's bounds, so use our container for clipping
        container.setBounds(
            /* x = */ clippedBounds.left,
            /* y = */ clippedBounds.top,
            /* width = */ clippedBounds.width,
            /* height = */ clippedBounds.height
        )

        // The real size and position should be based on not-clipped bounds
        userComponent?.setBounds(
            /* x = */ bounds.left - clippedBounds.left, // Local position relative to container
            /* y = */ bounds.top - clippedBounds.top,
            /* width = */ bounds.width,
            /* height = */ bounds.height
        )
    }

    fun getDeepestComponentForEvent(event: MouseEvent): Component? {
        if (userComponent == null) return null
        val point = SwingUtilities.convertPoint(event.component, event.point, userComponent)
        return SwingUtilities.getDeepestComponentAt(userComponent, point.x, point.y)
    }
}

private class Updater<T : Component>(
    private val component: T,
    update: (T) -> Unit,
) {
    private var isDisposed = false
    private val isUpdateScheduled = atomic(false)
    private val snapshotObserver = SnapshotStateObserver { command ->
        command()
    }

    private val scheduleUpdate = { _: T ->
        if (!isUpdateScheduled.getAndSet(true)) {
            SwingUtilities.invokeLater {
                isUpdateScheduled.value = false
                if (!isDisposed) {
                    performUpdate()
                }
            }
        }
    }

    var update: (T) -> Unit = update
        set(value) {
            if (field != value) {
                field = value
                performUpdate()
            }
        }

    private fun performUpdate() {
        // don't replace scheduleUpdate by lambda reference,
        // scheduleUpdate should always be the same instance
        snapshotObserver.observeReads(component, scheduleUpdate) {
            update(component)
        }
    }

    init {
        snapshotObserver.start()
        performUpdate()
    }

    fun dispose() {
        snapshotObserver.stop()
        snapshotObserver.clear()
        isDisposed = true
    }
}

private fun Rect.round(density: Density): IntRect {
    val left = floor(left / density.density).toInt()
    val top = floor(top / density.density).toInt()
    val right = ceil(right / density.density).toInt()
    val bottom = ceil(bottom / density.density).toInt()
    return IntRect(left, top, right, bottom)
}

private class InteropPointerInputModifier<T : Component>(
    private val interopComponent: SwingInteropComponent<T>,
) : PointerInputFilter(), PointerInputModifier {
    override val pointerInputFilter: PointerInputFilter = this

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        /*
         * If the event was a down or up event, we dispatch to platform as early as possible.
         * If the event is a move event, and we can still intercept, we dispatch to platform after
         * we have a chance to intercept due to movement.
         *
         * See Android's PointerInteropFilter as original source for this logic.
         */
        val dispatchDuringInitialTunnel = pointerEvent.changes.fastAny {
            it.changedToDownIgnoreConsumed() || it.changedToUpIgnoreConsumed()
        }
        if (pass == PointerEventPass.Initial && dispatchDuringInitialTunnel) {
            dispatchToView(pointerEvent)
        }
        if (pass == PointerEventPass.Final && !dispatchDuringInitialTunnel) {
            dispatchToView(pointerEvent)
        }
    }

    override fun onCancel() {
    }

    private fun dispatchToView(pointerEvent: PointerEvent) {
        val e = pointerEvent.awtEventOrNull ?: return
        when (e.id) {
            // Do not redispatch Enter/Exit events since they are related exclusively
            // to original component.
            MouseEvent.MOUSE_ENTERED, MouseEvent.MOUSE_EXITED -> return
        }
        if (SwingUtilities.isDescendingFrom(e.component, interopComponent.container)) {
            // Do not redispatch the event if it originally from this interop view.
            return
        }
        val component = interopComponent.getDeepestComponentForEvent(e)
        if (component != null) {
            component.dispatchEvent(SwingUtilities.convertMouseEvent(e.component, e, component))
            pointerEvent.changes.fastForEach {
                it.consume()
            }
        }
    }
}

/**
 * The maximum radix available for conversion to and from strings.
 */
private val MaxSupportedRadix = 36
