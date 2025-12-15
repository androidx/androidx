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

import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIViewController

/**
 * Configuration of [androidx.compose.ui.window.ComposeUIViewController] behavior.
 */
class ComposeUIViewControllerConfiguration: ComposeContainerConfiguration() {
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
