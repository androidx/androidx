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
    val componentInfo = remember {
        ComponentInfo<T>(
            container = SwingPanelContainer(
                key = compositeKey,
                focusComponent = interopContainer.container,
            )
        )
    }

    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val focusSwitcher = remember { FocusSwitcher(componentInfo, focusManager) }

    OverlayLayout(
        modifier = modifier.onGloballyPositioned { coordinates ->
            val rootCoordinates = coordinates.findRootCoordinates()
            val clipedBounds = rootCoordinates
                .localBoundingBoxOf(coordinates, clipBounds = true).round(density)
            val bounds = rootCoordinates
                .localBoundingBoxOf(coordinates, clipBounds = false).round(density)

            // Take care about clipped bounds
            componentInfo.clipBounds = clipedBounds // Clipping area for skia canvas
            componentInfo.container.isVisible = !clipedBounds.isEmpty // Hide if it's fully clipped
            // Swing clips children based on parent's bounds, so use our container for clipping
            componentInfo.container.setBounds(
                /* x = */ clipedBounds.left,
                /* y = */ clipedBounds.top,
                /* width = */ clipedBounds.width,
                /* height = */ clipedBounds.height
            )

            // The real size and position should be based on not-clipped bounds
            componentInfo.component.setBounds(
                /* x = */ bounds.left - clipedBounds.left, // Local position relative to container
                /* y = */ bounds.top - clipedBounds.top,
                /* width = */ bounds.width,
                /* height = */ bounds.height
            )
            componentInfo.container.validate()
            componentInfo.container.repaint()
        }.drawBehind {
            // Clear interop area to make visible the component under our canvas.
            drawRect(Color.Transparent, blendMode = BlendMode.Clear)
        }.trackSwingInterop(componentInfo)
            .then(InteropPointerInputModifier(componentInfo))
    ) {
        focusSwitcher.Content()
    }

    DisposableEffect(Unit) {
        val focusListener = object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                if (componentInfo.container.isParentOf(e.oppositeComponent)) {
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
        interopContainer.addInteropView(componentInfo)
        onDispose {
            interopContainer.removeInteropView(componentInfo)
            interopContainer.container.removeFocusListener(focusListener)
        }
    }

    DisposableEffect(factory) {
        componentInfo.component = factory()
        componentInfo.container.add(componentInfo.component)
        componentInfo.updater = Updater(componentInfo.component, update)
        onDispose {
            componentInfo.container.remove(componentInfo.component)
            componentInfo.updater.dispose()
        }
    }

    SideEffect {
        componentInfo.container.background = background.toAwtColor()
        componentInfo.updater.update = update
    }
}

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
    private val info: ComponentInfo<T>,
    private val focusManager: FocusManager,
) {
    private val backwardRequester = FocusRequester()
    private val forwardRequester = FocusRequester()
    private var isRequesting = false

    fun moveBackward() {
        try {
            isRequesting = true
            backwardRequester.requestFocus()
        } finally {
            isRequesting = false
        }
        focusManager.moveFocus(FocusDirection.Previous)
    }

    fun moveForward() {
        try {
            isRequesting = true
            forwardRequester.requestFocus()
        } finally {
            isRequesting = false
        }
        focusManager.moveFocus(FocusDirection.Next)
    }

    @Composable
    fun Content() {
        EmptyLayout(
            Modifier
                .focusRequester(backwardRequester)
                .onFocusEvent {
                    if (it.isFocused && !isRequesting) {
                        focusManager.clearFocus(force = true)

                        val component = info.container.focusTraversalPolicy.getFirstComponent(info.container)
                        if (component != null) {
                            component.requestFocus(FocusEvent.Cause.TRAVERSAL_FORWARD)
                        } else {
                            moveForward()
                        }
                    }
                }
                .focusTarget()
        )
        EmptyLayout(
            Modifier
                .focusRequester(forwardRequester)
                .onFocusEvent {
                    if (it.isFocused && !isRequesting) {
                        focusManager.clearFocus(force = true)

                        val component = info.container.focusTraversalPolicy.getLastComponent(info.container)
                        if (component != null) {
                            component.requestFocus(FocusEvent.Cause.TRAVERSAL_BACKWARD)
                        } else {
                            moveBackward()
                        }
                    }
                }
                .focusTarget()
        )
    }
}

private class ComponentInfo<T : Component>(
    container: SwingPanelContainer
): InteropComponent(container) {
    lateinit var component: T
    lateinit var updater: Updater<T>
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
    private val componentInfo: ComponentInfo<T>,
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
        if (SwingUtilities.isDescendingFrom(e.component, componentInfo.container)) {
            // Do not redispatch the event if it originally from this interop view.
            return
        }
        val component = getDeepestComponentForEvent(componentInfo.component, e)
        if (component != null) {
            component.dispatchEvent(SwingUtilities.convertMouseEvent(e.component, e, component))
            pointerEvent.changes.fastForEach {
                it.consume()
            }
        }
    }

    private fun getDeepestComponentForEvent(parent: Component, event: MouseEvent): Component? {
        val point = SwingUtilities.convertPoint(event.component, event.point, parent)
        return SwingUtilities.getDeepestComponentAt(parent, point.x, point.y)
    }
}

/**
 * The maximum radix available for conversion to and from strings.
 */
private val MaxSupportedRadix = 36
