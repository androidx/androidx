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

package androidx.compose.ui.interop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.Modifier
import androidx.compose.runtime.State
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.EmptyLayout
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.AccessibilityKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.uikit.toUIColor
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.unit.width
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSThread
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.removeFromParentViewController
import platform.UIKit.willMoveToParentViewController
import androidx.compose.ui.uikit.utils.CMPInteropWrappingView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toDpOffset
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.viewinterop.interopViewAnchor
import androidx.compose.ui.viewinterop.InteropView
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero

private val STUB_CALLBACK_WITH_RECEIVER: Any.() -> Unit = {}
private val DefaultViewResize: UIView.(CValue<CGRect>) -> Unit = { rect -> this.setFrame(rect) }
private val DefaultViewControllerResize: UIViewController.(CValue<CGRect>) -> Unit =
    { rect -> this.view.setFrame(rect) }

/**
 * A [UIView] that contains underlying interop element, such as an independent [UIView]
 * or [UIViewController]'s root [UIView].
 */
internal class InteropWrappingView : CMPInteropWrappingView(frame = CGRectZero.readValue()) {
    var actualAccessibilityContainer: Any? = null

    init {
        // required to properly clip the content of the wrapping view in case interop unclipped
        // bounds are larger than clipped bounds
        clipsToBounds = true
    }

    override fun accessibilityContainer(): Any? {
        return actualAccessibilityContainer
    }
}

internal val InteropViewSemanticsKey = AccessibilityKey<InteropWrappingView>(
    name = "InteropView",
    mergePolicy = { parentValue, childValue ->
        if (parentValue == null) {
            childValue
        } else {
            println(
                "Warning: Merging accessibility for multiple interop views is not supported. " +
                    "Multiple [UIKitView] are grouped under one node that should be represented as a single accessibility element." +
                    "It isn't recommended because the accessibility system can only recognize the first one. " +
                    "If you need multiple native views for accessibility, make sure to place them inside a single [UIKitView]."
            )

            parentValue
        }
    }
)

private var SemanticsPropertyReceiver.interopView by InteropViewSemanticsKey

/**
 * Chain [this] with [Modifier.semantics] that sets the [interopViewAnchor] of the node if [enabled] is true.
 * If [enabled] is false, [this] is returned as is.
 */
private fun Modifier.interopSemantics(
    enabled: Boolean,
    wrappingView: InteropWrappingView
): Modifier =
    if (enabled) {
        this.semantics {
            interopView = wrappingView
        }
    } else {
        this
    }

/**
 * Add an association with [InteropView] to the modified element.
 * Allows hit testing and custom pointer input handling for the [InteropView].
 *
 * @param isInteractive If `true`, the modifier will be applied. If `false`, returns the original modifier.
 * @param wrappingView The [InteropWrappingView] to associate with the modified element.
 */
private fun Modifier.interopViewAnchor(isInteractive: Boolean, wrappingView: InteropWrappingView): Modifier =
    if (isInteractive) {
        this.interopViewAnchor(wrappingView)
    } else {
        this
    }

/**
 * Internal common part of custom layout emitting a node associated with UIKit interop for [UIView] and [UIViewController].
 */
@Composable
private fun <T : Any> UIKitInteropLayout(
    modifier: Modifier,
    update: (T) -> Unit,
    background: Color,
    componentHandler: InteropComponentHandler<T>,
    interactive: Boolean,
    accessibilityEnabled: Boolean,
) {
    val density = LocalDensity.current
    val interopContainer = LocalUIKitInteropContainer.current

    val finalModifier = modifier
        .onGloballyPositioned { coordinates ->
            val rootCoordinates = coordinates.findRootCoordinates()

            val unclippedBounds = rootCoordinates
                .localBoundingBoxOf(
                    sourceCoordinates = coordinates,
                    clipBounds = false
                )

            val clippedBounds = rootCoordinates
                .localBoundingBoxOf(
                    sourceCoordinates = coordinates,
                    clipBounds = true
                )

            componentHandler.updateRect(
                unclippedRect = unclippedBounds.roundToIntRect(),
                clippedRect = clippedBounds.roundToIntRect(),
                density = density
            )
        }
        .drawBehind {
            // Paint the rectangle behind with transparent color to let our interop shine through
            drawRect(
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )
        }
        .trackUIKitInterop(interopContainer, componentHandler.wrappingView)
        .interopViewAnchor(interactive, componentHandler.wrappingView)
        .interopSemantics(accessibilityEnabled, componentHandler.wrappingView)

    EmptyLayout(
        finalModifier
    )

    DisposableEffect(Unit) {
        componentHandler.onStart(initialUpdateBlock = update)

        onDispose {
            componentHandler.onStop()
        }
    }

    LaunchedEffect(background) {
        componentHandler.onBackgroundColorChange(background)
    }

    SideEffect {
        componentHandler.setUpdate(update)
    }
}

/**
 * @param factory The block creating the [UIView] to be composed.
 * @param modifier The modifier to be applied to the layout. Size should be specified in modifier.
 * Modifier may contains crop() modifier with different shapes.
 * @param update A callback to be invoked after the layout is inflated.
 * @param background A color of [UIView] background wrapping the view created by [factory].
 * @param onRelease A callback invoked as a signal that this view instance has exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * View should be freed at this time.
 * @param onResize May be used to custom resize logic.
 * @param interactive If true, then user touches will be passed to this UIView
 * @param accessibilityEnabled If `true`, then the view will be visible to accessibility services.
 *
 * If this Composable is within a modifier chain that merges
 * the semantics of its children (such as `Modifier.clickable`), the merged subtree data will be ignored in favor of
 * the native UIAccessibility resolution for the view constructed by [factory]. For example, `Button` containing [UIKitView]
 * will be invisible for accessibility services, only the [UIView] created by [factory] will be accessible.
 * To avoid this behavior, set [accessibilityEnabled] to `false` and use custom [Modifier.semantics] for `Button` to
 * make the information associated with this view accessible.
 *
 * If there are multiple [UIKitView] or [UIKitViewController] with [accessibilityEnabled] set to `true` in the merged tree, only the first one will be accessible.
 * Consider using a single [UIKitView] or [UIKitViewController] with multiple views inside it if you need multiple accessible views.
 *
 * In general, [accessibilityEnabled] set to `true` is not recommended to use in such cases.
 * Consider using [Modifier.semantics] on Composable that merges its semantics instead.
 *
 * @see Modifier.semantics
 */
@Composable
fun <T : UIView> UIKitView(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    onResize: (view: T, rect: CValue<CGRect>) -> Unit = DefaultViewResize,
    interactive: Boolean = true,
    accessibilityEnabled: Boolean = true,
) {
    val interopContainer = LocalUIKitInteropContainer.current
    val handler = remember {
        InteropViewHandler(
            createView = factory,
            interopContainer = interopContainer,
            onResize = onResize,
            onRelease = onRelease
        )
    }

    UIKitInteropLayout(
        modifier = modifier,
        update = update,
        background = background,
        componentHandler = handler,
        interactive = interactive,
        accessibilityEnabled = accessibilityEnabled
    )
}

/**
 * @param factory The block creating the [UIViewController] to be composed.
 * @param modifier The modifier to be applied to the layout. Size should be specified in modifier.
 * Modifier may contains crop() modifier with different shapes.
 * @param update A callback to be invoked after the layout is inflated.
 * @param background A color of [UIView] background wrapping the view of [UIViewController] created by [factory].
 * @param onRelease A callback invoked as a signal that this view controller instance has exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * view controller should be freed at this time.
 * @param onResize May be used to custom resize logic.
 * @param interactive If true, then user touches will be passed to this UIViewController
 * @param accessibilityEnabled If `true`, then the [UIViewController.view] will be visible to accessibility services.
 *
 * If this Composable is within a modifier chain that merges the semantics of its children (such as `Modifier.clickable`),
 * the merged subtree data will be ignored in favor of
 * the native UIAccessibility resolution for the [UIViewController.view] of [UIViewController] constructed by [factory].
 * For example, `Button` containing [UIKitViewController] will be invisible for accessibility services,
 * only the [UIViewController.view] of [UIViewController] created by [factory] will be accessible.
 * To avoid this behavior, set [accessibilityEnabled] to `false` and use custom [Modifier.semantics] for `Button` to
 * make the information associated with the [UIViewController] accessible.
 *
 * If there are multiple [UIKitView] or [UIKitViewController] with [accessibilityEnabled] set to `true` in the merged tree,
 * only the first one will be accessible.
 * Consider using a single [UIKitView] or [UIKitViewController] with multiple views inside it if you need multiple accessible views.
 *
 * In general, [accessibilityEnabled] set to `true` is not recommended to use in such cases.
 * Consider using [Modifier.semantics] on Composable that merges its semantics instead.
 *
 * @see Modifier.semantics
 */
@Composable
fun <T : UIViewController> UIKitViewController(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    onResize: (viewController: T, rect: CValue<CGRect>) -> Unit = DefaultViewControllerResize,
    interactive: Boolean = true,
    accessibilityEnabled: Boolean = true,
) {
    val interopContainer = LocalUIKitInteropContainer.current
    val rootViewController = LocalUIViewController.current
    val handler = remember {
        InteropViewControllerHandler(
            createViewController = factory,
            interopContainer = interopContainer,
            rootViewController = rootViewController,
            onResize = onResize,
            onRelease = onRelease
        )
    }

    UIKitInteropLayout(
        modifier = modifier,
        update = update,
        background = background,
        componentHandler = handler,
        interactive = interactive,
        accessibilityEnabled = accessibilityEnabled
    )
}

/**
 * An abstract class responsible for hiearchy updates and state management of interop components like [UIView] and [UIViewController]
 */
private abstract class InteropComponentHandler<T : Any>(
    // TODO: reuse an object created makeComponent inside LazyColumn like in AndroidView:
    //  https://developer.android.com/reference/kotlin/androidx/compose/ui/viewinterop/package-summary#AndroidView(kotlin.Function1,kotlin.Function1,androidx.compose.ui.Modifier,kotlin.Function1,kotlin.Function1)
    val createComponent: () -> T,
    val interopContainer: UIKitInteropContainer,
    val onResize: (T, rect: CValue<CGRect>) -> Unit,
    val onRelease: (T) -> Unit,
) {
    /**
     * The coordinates
     */
    private var currentUnclippedRect: IntRect? = null
    private var currentClippedRect: IntRect? = null
    val wrappingView = InteropWrappingView()
    lateinit var component: T
    private lateinit var updater: Updater<T>

    /**
     * Set the [Updater.update] lambda.
     * Lambda is immediately executed after setting.
     * @see Updater.performUpdate
     */
    fun setUpdate(block: (T) -> Unit) {
        updater.update = block
    }

    /**
     * Set the frame of the wrapping view.
     */
    fun updateRect(unclippedRect: IntRect, clippedRect: IntRect, density: Density) {
        if (currentUnclippedRect == unclippedRect && currentClippedRect == clippedRect) {
            return
        }

        val clippedDpRect = clippedRect.toRect().toDpRect(density)
        val unclippedDpRect = unclippedRect.toRect().toDpRect(density)

        // wrapping view itself is always using the clipped rect
        if (clippedRect != currentClippedRect) {
            interopContainer.deferAction {
                wrappingView.setFrame(clippedDpRect.asCGRect())
            }
        }

        // Only call onResize if the actual size changes.
        if (currentUnclippedRect != unclippedRect || currentClippedRect != clippedRect) {
            // offset to move the component to the correct position inside the wrapping view, so
            // its global unclipped frame stays the same
            val offset = unclippedRect.topLeft - clippedRect.topLeft
            val dpOffset = offset.toOffset().toDpOffset(density)

            interopContainer.deferAction {
                // The actual component created by the user is resized here using the provided callback.
                onResize(
                    component,
                    CGRectMake(
                        x = dpOffset.x.value.toDouble(),
                        y = dpOffset.y.value.toDouble(),
                        width = unclippedDpRect.width.value.toDouble(),
                        height = unclippedDpRect.height.value.toDouble()
                    ),
                )
            }
        }

        currentUnclippedRect = unclippedRect
        currentClippedRect = clippedRect
    }

    fun onStart(initialUpdateBlock: (T) -> Unit) {
        component = createComponent()
        updater = Updater(component, initialUpdateBlock) {
            interopContainer.deferAction(action = it)
        }

        interopContainer.startTrackingInteropView(wrappingView)
        interopContainer.deferAction {
            setupViewHierarchy()
        }
    }

    fun onStop() {
        interopContainer.stopTrackingInteropView(wrappingView)
        interopContainer.deferAction {
            destroyViewHierarchy()
        }

        onRelease(component)
        updater.dispose()
    }

    fun onBackgroundColorChange(color: Color) = interopContainer.deferAction {
        if (color == Color.Unspecified) {
            wrappingView.backgroundColor = interopContainer.containerView.backgroundColor
        } else {
            wrappingView.backgroundColor = color.toUIColor()
        }
    }

    abstract fun setupViewHierarchy()
    abstract fun destroyViewHierarchy()
}

private class InteropViewHandler<T : UIView>(
    createView: () -> T,
    interopContainer: UIKitInteropContainer,
    onResize: (T, rect: CValue<CGRect>) -> Unit,
    onRelease: (T) -> Unit
) : InteropComponentHandler<T>(createView, interopContainer, onResize, onRelease) {
    override fun setupViewHierarchy() {
        interopContainer.containerView.addSubview(wrappingView)
        wrappingView.addSubview(component)
    }

    override fun destroyViewHierarchy() {
        wrappingView.removeFromSuperview()
    }
}

private class InteropViewControllerHandler<T : UIViewController>(
    createViewController: () -> T,
    interopContainer: UIKitInteropContainer,
    private val rootViewController: UIViewController,
    onResize: (T, rect: CValue<CGRect>) -> Unit,
    onRelease: (T) -> Unit
) : InteropComponentHandler<T>(createViewController, interopContainer, onResize, onRelease) {
    override fun setupViewHierarchy() {
        rootViewController.addChildViewController(component)
        interopContainer.containerView.addSubview(wrappingView)
        wrappingView.addSubview(component.view)
        component.didMoveToParentViewController(rootViewController)
    }

    override fun destroyViewHierarchy() {
        component.willMoveToParentViewController(null)
        wrappingView.removeFromSuperview()
        component.removeFromParentViewController()
    }
}

/**
 * A helper class to schedule an update for the interop component whenever the [State] used by the [update]
 * lambda is changed.
 *
 * @param component The interop component to be updated.
 * @param update The lambda to be called whenever the state used by this lambda is changed.
 * @param deferAction The lambda to register [update] execution to defer it in order to sync it with
 * Compose rendering. The aim of this is to make visual changes to UIKit and Compose
 * simultaneously.
 * @see [UIKitInteropContainer] and [UIKitInteropTransaction] for more details.
 */
private class Updater<T : Any>(
    private val component: T,
    update: (T) -> Unit,

    /**
     * Updater will not execute the [update] method by itself, but will pass it to this lambda
     */
    private val deferAction: (() -> Unit) -> Unit,
) {
    private var isDisposed = false
    private val isUpdateScheduled = atomic(false)
    private val snapshotObserver = SnapshotStateObserver { command ->
        command()
    }

    private val scheduleUpdate = { _: T ->
        if (!isUpdateScheduled.getAndSet(true)) {
            deferAction {
                check(NSThread.isMainThread)

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