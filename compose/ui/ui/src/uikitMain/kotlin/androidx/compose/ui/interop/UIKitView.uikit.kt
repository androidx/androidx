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
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.uikit.toUIColor
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.viewinterop.InteropWrappingView
import androidx.compose.ui.viewinterop.LocalInteropContainer
import androidx.compose.ui.viewinterop.UIKitInteropViewControllerHolder
import androidx.compose.ui.viewinterop.UIKitInteropViewHolder
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGRect
import platform.UIKit.UIView
import platform.UIKit.UIViewController

private val STUB_CALLBACK_WITH_RECEIVER: Any.() -> Unit = {}
private val DefaultViewResize: UIView.(CValue<CGRect>) -> Unit = { rect -> this.setFrame(rect) }
private val DefaultViewControllerResize: UIViewController.(CValue<CGRect>) -> Unit =
    { rect -> this.view.setFrame(rect) }

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
    accessibilityEnabled: Boolean = true
) {
    val compositeKeyHash = currentCompositeKeyHash
    val interopContainer = LocalInteropContainer.current

    val backgroundColor by remember(background) { mutableStateOf(background.toUIColor()) }

    InteropView(
        factory = {
            UIKitInteropViewHolder(
                factory = factory,
                interopContainer = interopContainer,
                group = InteropWrappingView(areTouchesDelayed = true),
                isInteractive = interactive,
                isNativeAccessibilityEnabled = accessibilityEnabled,
                compositeKeyHash = compositeKeyHash,
                resize = onResize
            )
        },
        modifier = modifier,
        onReset = null,
        onRelease = onRelease,
        update = {
            backgroundColor?.let { color ->
                it.backgroundColor = color
            }
            update(it)
        }
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
    accessibilityEnabled: Boolean = true
) {
    val compositeKeyHash = currentCompositeKeyHash
    val interopContainer = LocalInteropContainer.current
    val parentViewController = LocalUIViewController.current

    val backgroundColor by remember(background) { mutableStateOf(background.toUIColor()) }

    InteropView(
        factory = {
            UIKitInteropViewControllerHolder(
                factory = factory,
                parentViewController = parentViewController,
                interopContainer = interopContainer,
                group = InteropWrappingView(areTouchesDelayed = true),
                isInteractive = interactive,
                isNativeAccessibilityEnabled = accessibilityEnabled,
                compositeKeyHash = compositeKeyHash,
                resize = onResize
            )
        },
        modifier = modifier,
        onReset = null,
        onRelease = onRelease,
        update = {
            backgroundColor?.let { color ->
                it.view.backgroundColor = color
            }
            update(it)
        }
    )
}