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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.layout.EmptyLayout
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.uikit.toUIColor
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toDpOffset
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.unit.width
import androidx.compose.ui.viewinterop.InteropContainer
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.viewinterop.InteropViewHolder
import androidx.compose.ui.viewinterop.InteropViewUpdater
import androidx.compose.ui.viewinterop.LocalInteropContainer
import androidx.compose.ui.viewinterop.UIKitInteropViewGroup
import androidx.compose.ui.viewinterop.interopViewSemantics
import androidx.compose.ui.viewinterop.pointerInteropFilter
import androidx.compose.ui.viewinterop.trackInteropPlacement
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.removeFromParentViewController
import platform.UIKit.willMoveToParentViewController

private val STUB_CALLBACK_WITH_RECEIVER: Any.() -> Unit = {}
private val DefaultViewResize: UIView.(CValue<CGRect>) -> Unit = { rect -> this.setFrame(rect) }
private val DefaultViewControllerResize: UIViewController.(CValue<CGRect>) -> Unit =
    { rect -> this.view.setFrame(rect) }

/**
 * Internal common part of custom layout emitting a node associated with UIKit interop for [UIView] and [UIViewController].
 */
@Composable
private fun <T : Any> UIKitInteropLayout(
    modifier: Modifier,
    update: (T) -> Unit,
    background: Color,
    interopViewHolder: UIKitInteropViewHolder<T>,
    interactive: Boolean,
    accessibilityEnabled: Boolean,
) {
    val density = LocalDensity.current
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

            interopViewHolder.updateRect(
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
        .trackInteropPlacement(interopViewHolder)
        .pointerInteropFilter(interactive, interopViewHolder)
        .interopViewSemantics(accessibilityEnabled, interopViewHolder)

    EmptyLayout(
        finalModifier
    )

    DisposableEffect(Unit) {
        interopViewHolder.onStart(initialUpdateBlock = update)

        onDispose {
            interopViewHolder.onStop()
        }
    }

    LaunchedEffect(background) {
        interopViewHolder.onBackgroundColorChange(background)
    }

    SideEffect {
        interopViewHolder.setUpdate(update)
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
    val interopContainer = LocalInteropContainer.current
    val interopViewHolder = remember {
        UIKitViewHolder(
            container = interopContainer,
            createView = factory,
            onResize = onResize,
            onRelease = onRelease
        )
    }

    UIKitInteropLayout(
        modifier = modifier,
        update = update,
        background = background,
        interopViewHolder = interopViewHolder,
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
    val interopContainer = LocalInteropContainer.current
    val rootViewController = LocalUIViewController.current
    val interopViewHolder = remember {
        UIKitViewControllerHolder(
            container = interopContainer,
            createViewController = factory,
            rootViewController = rootViewController,
            onResize = onResize,
            onRelease = onRelease
        )
    }

    UIKitInteropLayout(
        modifier = modifier,
        update = update,
        background = background,
        interopViewHolder = interopViewHolder,
        interactive = interactive,
        accessibilityEnabled = accessibilityEnabled
    )
}

/**
 * An abstract class responsible for hierarchy updates and state management of interop components
 * like [UIView] and [UIViewController].
 */
private abstract class UIKitInteropViewHolder<T : Any>(
    container: InteropContainer,

    // TODO: reuse an object created makeComponent inside LazyColumn like in AndroidView:
    //  https://developer.android.com/reference/kotlin/androidx/compose/ui/viewinterop/package-summary#AndroidView(kotlin.Function1,kotlin.Function1,androidx.compose.ui.Modifier,kotlin.Function1,kotlin.Function1)
    val createUserComponent: () -> T,
    val onResize: (T, rect: CValue<CGRect>) -> Unit,
    val onRelease: (T) -> Unit,
) : InteropViewHolder(container, group = UIKitInteropViewGroup()) {
    private var currentUnclippedRect: IntRect? = null
    private var currentClippedRect: IntRect? = null
    lateinit var userComponent: T
    private lateinit var updater: InteropViewUpdater<T>

    /**
     * Set the [InteropViewUpdater.update] lambda.
     * Lambda is immediately executed after setting.
     * @see InteropViewUpdater.performUpdate
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
            container.changeInteropViewLayout {
                group.setFrame(clippedDpRect.asCGRect())
            }
        }

        // Only call onResize if the actual size changes.
        if (currentUnclippedRect != unclippedRect || currentClippedRect != clippedRect) {
            // offset to move the component to the correct position inside the wrapping view, so
            // its global unclipped frame stays the same
            val offset = unclippedRect.topLeft - clippedRect.topLeft
            val dpOffset = offset.toOffset().toDpOffset(density)

            container.changeInteropViewLayout {
                // The actual component created by the user is resized here using the provided callback.
                onResize(
                    userComponent,
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
        userComponent = createUserComponent()
        updater = InteropViewUpdater(userComponent, initialUpdateBlock) {
            container.changeInteropViewLayout(action = it)
        }

        container.changeInteropViewLayout {
            setupViewHierarchy()
        }
    }

    fun onStop() {
        container.changeInteropViewLayout {
            destroyViewHierarchy()
        }

        onRelease(userComponent)
        updater.dispose()
    }

    fun onBackgroundColorChange(color: Color) = container.changeInteropViewLayout {
        if (color == Color.Unspecified) {
            group.backgroundColor = container.root.backgroundColor
        } else {
            group.backgroundColor = color.toUIColor()
        }
    }

    override fun dispatchToView(pointerEvent: PointerEvent) {
        // Do nothing - iOS uses hit-testing instead of redispatching
    }

    abstract fun setupViewHierarchy()
    abstract fun destroyViewHierarchy()
}

private class UIKitViewHolder<T : UIView>(
    container: InteropContainer,
    createView: () -> T,
    onResize: (T, rect: CValue<CGRect>) -> Unit,
    onRelease: (T) -> Unit
) : UIKitInteropViewHolder<T>(container, createView, onResize, onRelease) {
    override fun getInteropView(): InteropView? =
        userComponent

    override fun setupViewHierarchy() {
        group.addSubview(userComponent)
    }

    override fun destroyViewHierarchy() {
    }
}

private class UIKitViewControllerHolder<T : UIViewController>(
    container: InteropContainer,
    createViewController: () -> T,
    private val rootViewController: UIViewController,
    onResize: (T, rect: CValue<CGRect>) -> Unit,
    onRelease: (T) -> Unit
) : UIKitInteropViewHolder<T>(container, createViewController, onResize, onRelease) {
    override fun getInteropView(): InteropView? =
        userComponent.view

    override fun setupViewHierarchy() {
        rootViewController.addChildViewController(userComponent)
        group.addSubview(userComponent.view)
        userComponent.didMoveToParentViewController(rootViewController)
    }

    override fun destroyViewHierarchy() {
        userComponent.willMoveToParentViewController(null)
        userComponent.removeFromParentViewController()
    }
}
