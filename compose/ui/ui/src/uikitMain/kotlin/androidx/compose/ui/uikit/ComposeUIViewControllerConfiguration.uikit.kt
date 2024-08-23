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

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.platform.AccessibilitySyncOptions
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIStatusBarStyle

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
    var delegate: ComposeUIViewControllerDelegate = object : ComposeUIViewControllerDelegate {}

    @ExperimentalComposeApi
    var platformLayers: Boolean = true

    /**
     * @see [AccessibilitySyncOptions]
     *
     * By default, accessibility sync is enabled when required by accessibility services and debug
     * logging is disabled.
     */
    @ExperimentalComposeApi
    var accessibilitySyncOptions: AccessibilitySyncOptions =
        AccessibilitySyncOptions.WhenRequiredByAccessibilityServices(debugLogger = null)
        
    /**
     * Determines whether the Compose view should have an opaque background.
     * Warning: disabling opaque layer may affect performance.
     */
    @ExperimentalComposeApi
    var opaque: Boolean = true

    /**
     * A boolean flag to enable or disable the strict sanity check for the `Info.plist` file.
     * If the flag is set to true, and keys are missing, the app will crash with an
     * explanation on how to fix the issue.
     */
    var enforceStrictPlistSanityCheck: Boolean = true
}

/**
 * Interface for UIViewController to allow injecting logic which otherwise is impossible due to ComposeUIViewController
 * implementation being internal.
 * All of those callbacks are invoked at the very end of overriden function and properties implementation.
 * Default implementations do nothing and return Unit/null (indicating that UIKit default will be used).
 */
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
    object DoNothing : OnFocusBehavior

    /**
     * The Compose view will be panned in "y" coordinates.
     * A focusable element should be displayed above the keyboard.
     */
    object FocusableAboveKeyboard : OnFocusBehavior

    // TODO Better to control OnFocusBehavior with existing WindowInsets.
    // Definition: object: FocusableBetweenInsets(insets: WindowInsets) : OnFocusBehavior
    // Usage: onFocusBehavior = FocusableBetweenInsets(WindowInsets.ime.union(WindowInsets.systemBars))
}
