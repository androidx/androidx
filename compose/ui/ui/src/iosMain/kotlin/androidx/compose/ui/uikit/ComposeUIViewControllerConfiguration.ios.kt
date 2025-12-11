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

package androidx.compose.ui.uikit

import androidx.compose.ui.ExperimentalComposeUiApi
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIViewController

/**
 * Configuration of ComposeUIViewController behavior.
 */
class ComposeUIViewControllerConfiguration {
    /**
     * Control Compose behaviour on focus changed inside Compose.
     */
    var onFocusBehavior: OnFocusBehavior = OnFocusBehavior.FocusableAboveKeyboard

    /**
     * Reassign this property with an object implementing [ComposeUIViewControllerDelegate] to interact with APIs
     * that otherwise would require subclassing internal implementation of [UIViewController], which is impossible.
     */
    @Deprecated(
        message = "Use parent view controller to override the methods of the UIViewController class." +
            "Read more about child-parent view controller relationships here:" +
            "https://developer.apple.com/documentation/uikit/uiviewcontroller#1652844"
    )
    @Suppress("DEPRECATION")
    var delegate: ComposeUIViewControllerDelegate = object : ComposeUIViewControllerDelegate {}

    /**
     * Determines whether the Compose view should have an opaque background.
     * Warning: disabling opaque layer may affect performance.
     */
    @ExperimentalComposeUiApi
    var opaque: Boolean = true

    /**
     * A boolean flag to enable or disable the strict sanity check for the `Info.plist` file.
     * If the flag is set to true, and keys are missing, the app will crash with an
     * explanation on how to fix the issue.
     */
    var enforceStrictPlistSanityCheck: Boolean = true

    /**
     * If set to true, the Compose will encode the rendering commands on a dedicated render thread,
     * when possible.
     * This can improve the performance when no interop UIKit is used.
     *
     * It's an experimental API, and the effects of enabling it are not considered stable.
     *
     * Changing this setting outside of `ComposeUIViewController` `configure` argument scope has no effect.
     */
    @ExperimentalComposeUiApi
    var parallelRendering: Boolean = false

    /**
     * Determines how the end edge pan gestures will be handled.
     * In LTR layouts, the end edge is the right edge of the screen.
     * In RTL layouts, the end edge is the left edge of the screen.
     *
     * Note: this setting only affects the behavior of the end edge pan gestures.
     * The start edge pan gestures will always be handled as back navigation events.
     *
     * Default value is [EndEdgePanGestureBehavior.Disabled].
     */
    @ExperimentalComposeUiApi
    var endEdgePanGestureBehavior: EndEdgePanGestureBehavior = EndEdgePanGestureBehavior.Disabled
}

/**
 * Interface for UIViewController to allow injecting logic which otherwise is impossible due to ComposeUIViewController
 * implementation being internal.
 * All of those callbacks are invoked at the very end of overridden function and properties implementation.
 * Default implementations do nothing and return Unit/null (indicating that UIKit default will be used).
 */
@Deprecated(
    message = "Use parent view controller to override the methods of the UIViewController class." +
        "Read more about child-parent view controller relationships here:" +
        "https://developer.apple.com/documentation/uikit/uiviewcontroller#1652844"
)
interface ComposeUIViewControllerDelegate {
    /**
     * https://developer.apple.com/documentation/uikit/uiviewcontroller/1621416-preferredstatusbarstyle?language=objc
     * @return null if UIKit default should be used.
     */
    val preferredStatusBarStyle: UIStatusBarStyle?
        get() = null

    /**
     * https://developer.apple.com/documentation/uikit/uiviewcontroller/1621434-preferredstatusbarupdateanimatio?language=objc
     * @return null if UIKit default should be used.
     */
    val preferredStatysBarAnimation: UIStatusBarAnimation?
        get() = null

    /**
     * https://developer.apple.com/documentation/uikit/uiviewcontroller/1621440-prefersstatusbarhidden?language=objc
     * @return null if UIKit default should be used.
     */
    val prefersStatusBarHidden: Boolean?
        get() = null

    fun viewDidLoad() = Unit
    fun viewWillAppear(animated: Boolean) = Unit
    fun viewDidAppear(animated: Boolean) = Unit
    fun viewWillDisappear(animated: Boolean) = Unit
    fun viewDidDisappear(animated: Boolean) = Unit
}

sealed interface OnFocusBehavior {
    /**
     * The Compose view will stay on the current position.
     */
    @Suppress("unused")
    data object DoNothing : OnFocusBehavior

    /**
     * The Compose view will be panned in "y" coordinates.
     * A focusable element should be displayed above the keyboard.
     */
    data object FocusableAboveKeyboard : OnFocusBehavior
}

@ExperimentalComposeUiApi
sealed interface EndEdgePanGestureBehavior {
    /**
     * No navigation events will be sent on the end edge.
     */
    data object Disabled : EndEdgePanGestureBehavior

    /**
     * Back navigation events will be sent on the end edge.
     */
    data object Back : EndEdgePanGestureBehavior

    /**
     * Forward navigation events will be sent on the end edge.
     */
    data object Forward : EndEdgePanGestureBehavior
}
