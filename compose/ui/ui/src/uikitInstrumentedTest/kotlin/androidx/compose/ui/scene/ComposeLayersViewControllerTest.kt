/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.scene

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.MockAppDelegate
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.findLayersWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.uikit.utils.CMPComposeContainerLifecycleDelegateProtocol
import androidx.compose.ui.uikit.utils.CMPViewController
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UIInterfaceOrientationMaskAll
import platform.UIKit.UIInterfaceOrientationMaskPortraitUpsideDown
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleDefault
import platform.UIKit.addChildViewController
import platform.UIKit.childViewControllerForStatusBarHidden
import platform.UIKit.childViewControllerForStatusBarStyle
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.preferredInterfaceOrientationForPresentation
import platform.UIKit.shouldAutorotate
import platform.UIKit.supportedInterfaceOrientations
import platform.darwin.NSObject

class ComposeLayersViewControllerTest {
    @Test
    fun testOrientationAndStatusBarValues() {
        val appDelegate = MockAppDelegate()
        val viewController = TestComposeContainerViewController { Dialog(onDismissRequest = {}) {} }
        appDelegate.setUpWindow(viewController = viewController)
        viewController.waitForIdle()

        viewController.overrideShouldAutorotate = true
        viewController.overridePreferredInterfaceOrientationForPresentation =
            UIInterfaceOrientationPortrait
        viewController.overrideSupportedInterfaceOrientations = UIInterfaceOrientationMaskAll

        viewController.overridePreferredStatusBarStyle = UIStatusBarStyleDefault
        viewController.overridePrefersStatusBarHidden = false

        val layersWindow = appDelegate.findLayersWindow()

        assertEquals(true, layersWindow.rootViewController?.shouldAutorotate)
        assertEquals(
            UIInterfaceOrientationPortrait,
            layersWindow.rootViewController?.preferredInterfaceOrientationForPresentation
        )
        assertEquals(
            UIInterfaceOrientationMaskAll,
            layersWindow.rootViewController?.supportedInterfaceOrientations
        )
        assertEquals(
            UIStatusBarStyleDefault,
            layersWindow.rootViewController?.childViewControllerForStatusBarStyle?.preferredStatusBarStyle
        )
        assertEquals(
            false,
            layersWindow.rootViewController?.childViewControllerForStatusBarHidden?.prefersStatusBarHidden
        )

        viewController.overrideShouldAutorotate = false
        viewController.overridePreferredInterfaceOrientationForPresentation =
            UIInterfaceOrientationLandscapeRight
        viewController.overrideSupportedInterfaceOrientations =
            UIInterfaceOrientationMaskPortraitUpsideDown
        viewController.overridePreferredStatusBarStyle = UIStatusBarStyleDarkContent
        viewController.overridePrefersStatusBarHidden = true

        assertEquals(false, layersWindow.rootViewController?.shouldAutorotate)
        assertEquals(
            UIInterfaceOrientationLandscapeRight,
            layersWindow.rootViewController?.preferredInterfaceOrientationForPresentation
        )
        assertEquals(
            UIInterfaceOrientationMaskPortraitUpsideDown,
            layersWindow.rootViewController?.supportedInterfaceOrientations
        )

        assertEquals(
            UIStatusBarStyleDarkContent,
            layersWindow.rootViewController?.childViewControllerForStatusBarStyle?.preferredStatusBarStyle
        )
        assertEquals(
            true,
            layersWindow.rootViewController?.childViewControllerForStatusBarHidden?.prefersStatusBarHidden
        )

        appDelegate.cleanUp()
    }

    @Test
    fun testPopupOrientationAndStatusBarValues() {
        val appDelegate = MockAppDelegate()
        val viewController = TestComposeContainerViewController {
            Popup {
                Box(Modifier.size(10.dp))
            }
        }
        appDelegate.setUpWindow(viewController = viewController)
        viewController.waitForIdle()

        viewController.overrideShouldAutorotate = true
        viewController.overridePreferredInterfaceOrientationForPresentation =
            UIInterfaceOrientationPortrait
        viewController.overrideSupportedInterfaceOrientations = UIInterfaceOrientationMaskAll

        viewController.overridePreferredStatusBarStyle = UIStatusBarStyleDefault
        viewController.overridePrefersStatusBarHidden = false

        val layersWindow = appDelegate.findLayersWindow()

        assertEquals(
            true,
            layersWindow.rootViewController?.shouldAutorotate,
            "Autorotation must be enabled"
        )
        assertEquals(
            UIInterfaceOrientationPortrait,
            layersWindow.rootViewController?.preferredInterfaceOrientationForPresentation
        )
        assertEquals(
            UIInterfaceOrientationMaskAll,
            layersWindow.rootViewController?.supportedInterfaceOrientations
        )
        assertEquals(
            UIStatusBarStyleDefault,
            layersWindow.rootViewController?.childViewControllerForStatusBarStyle?.preferredStatusBarStyle
        )
        assertEquals(
            false,
            layersWindow.rootViewController?.childViewControllerForStatusBarHidden?.prefersStatusBarHidden,
            "Status bar should not be hidden"
        )

        viewController.overrideShouldAutorotate = false
        viewController.overridePreferredInterfaceOrientationForPresentation =
            UIInterfaceOrientationLandscapeRight
        viewController.overrideSupportedInterfaceOrientations =
            UIInterfaceOrientationMaskPortraitUpsideDown
        viewController.overridePreferredStatusBarStyle = UIStatusBarStyleDarkContent
        viewController.overridePrefersStatusBarHidden = true

        assertEquals(
            false,
            layersWindow.rootViewController?.shouldAutorotate,
            "Autorotation must be disabled"
        )
        assertEquals(
            UIInterfaceOrientationLandscapeRight,
            layersWindow.rootViewController?.preferredInterfaceOrientationForPresentation
        )
        assertEquals(
            UIInterfaceOrientationMaskPortraitUpsideDown,
            layersWindow.rootViewController?.supportedInterfaceOrientations
        )

        assertEquals(
            UIStatusBarStyleDarkContent,
            layersWindow.rootViewController?.childViewControllerForStatusBarStyle?.preferredStatusBarStyle
        )
        assertEquals(
            true,
            layersWindow.rootViewController?.childViewControllerForStatusBarHidden?.prefersStatusBarHidden,
            "Status bar should be hidden"
        )

        appDelegate.cleanUp()
    }
}

private class TestComposeContainerViewController(
    private val content: @Composable () -> Unit
): CMPViewController(lifecycleDelegate = TestComposeContainerLifecycleDelegate()) {
    var overrideShouldAutorotate: Boolean = true

    var overridePreferredInterfaceOrientationForPresentation: UIInterfaceOrientation =
        UIInterfaceOrientationPortrait

    var overrideSupportedInterfaceOrientations: UIInterfaceOrientationMask =
        UIInterfaceOrientationMaskAll

    var overridePreferredStatusBarStyle: UIStatusBarStyle =
        UIStatusBarStyleDefault

    var overridePrefersStatusBarHidden: Boolean =
        false

    override fun preferredInterfaceOrientationForPresentation(): UIInterfaceOrientation =
        overridePreferredInterfaceOrientationForPresentation

    override fun supportedInterfaceOrientations(): UIInterfaceOrientationMask =
        overrideSupportedInterfaceOrientations

    override fun shouldAutorotate(): Boolean = overrideShouldAutorotate

    override fun preferredStatusBarStyle(): UIStatusBarStyle = overridePreferredStatusBarStyle

    override fun prefersStatusBarHidden(): Boolean = overridePrefersStatusBarHidden

    val composeViewController = ComposeUIViewController(
        configure = { enforceStrictPlistSanityCheck = false }
    ) { content() } as ComposeHostingViewController

    fun waitForIdle() =
        UIKitInstrumentedTest.waitUntil {
            !composeViewController.hasInvalidations() && !Snapshot.current.hasPendingChanges()
        }

    override fun viewDidLoad() {
        super.viewDidLoad()

        addChildViewController(composeViewController)
        view.addSubview(composeViewController.view)
        composeViewController.didMoveToParentViewController(this)
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        composeViewController.view.setFrame(view.bounds)
    }

    override fun viewControllerDidEnterWindowHierarchy() {}

    override fun viewControllerDidLeaveWindowHierarchy() {}

    override fun userInterfaceStyleDidChange() {}
}

private class TestComposeContainerLifecycleDelegate: NSObject(),
    CMPComposeContainerLifecycleDelegateProtocol {
    override fun composeContainerWillDealloc() {}
    override fun composeContainerWillAppear() {}
    override fun composeContainerDidDisappear() {}
}
