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

package androidx.compose.ui.layers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.scene.ComposeHostingViewController
import androidx.compose.ui.test.captureScreenshot
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.forEachPixel
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.center
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import platform.UIKit.UIViewController

class ComposeLayersViewControllerTest {
    @Test
    fun testFullScreenPopupFromModalCoversScreenAndIsClickable() = runUIKitInstrumentedTest(
        params = listOf(true, false)
    ) { animated ->
        setContent { Box(Modifier.fillMaxSize()) }

        var clickCount = 0
        val showPopup = mutableStateOf(!animated)

        val modalVC = ComposeUIViewController(configure = {
            enforceStrictPlistSanityCheck = false
        }) {
            if (showPopup.value) {
                Popup(
                    properties = PopupProperties(
                        focusable = true,
                        usePlatformDefaultWidth = false,
                        usePlatformInsets = false
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Red).clickable {
                        clickCount++
                    })
                }
            }
        }

        var presented = false
        viewController.presentViewController(modalVC, animated = animated) { presented = true }
        waitUntil { presented }

        showPopup.value = true

        waitForIdle()
        waitUntil { !(modalVC as ComposeHostingViewController).hasInvalidations() }

        captureScreenshot()!!.forEachPixel(step = 4) { _, _, actualColor ->
            assertEquals(
                Color.Red,
                actualColor,
                "Expected to draw ${Color.Red} background"
            )
        }

        tap(screenSize.center)
        waitForIdle()
        waitUntil { !(modalVC as ComposeHostingViewController).hasInvalidations() }
        assertEquals(1, clickCount)
    }

    @Test
    fun testPopupPresentsModalViewController() = runUIKitInstrumentedTest(
        params = listOf(true, false)
    ) { animated ->
        var clickCount = 0
        var presented = false

        val modalVC = ComposeUIViewController(configure = {
            enforceStrictPlistSanityCheck = false
        }) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Cyan).clickable { clickCount++ })
        }

        val showPopup = mutableStateOf(!animated)

        setContent {
            if (showPopup.value) {
                Popup(properties = PopupProperties(focusable = true)) {
                    val currentVC = LocalUIViewController.current
                    LaunchedEffect(Unit) {
                        currentVC.presentViewController(modalVC, animated = animated) {
                            presented = true
                        }
                    }
                    Box(Modifier.fillMaxSize().background(Color.Gray))
                }
            }
        }

        showPopup.value = true
        waitUntil { presented }
        assertNotNull(modalVC.view.window)

        tap(screenSize.center)
        waitForIdle()
        waitUntil { !(modalVC as ComposeHostingViewController).hasInvalidations() }
        assertEquals(1, clickCount)
    }

    @Test
    fun testDialogWithUIKitViewControllerCallsLifecycleMethods() = runUIKitInstrumentedTest {
        val trackingVC = LifecycleTrackingViewController()
        var showDialog by mutableStateOf(true)

        setContent {
            if (showDialog) {
                Dialog(
                    onDismissRequest = {},
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    UIKitViewController(
                        factory = { trackingVC },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        waitForIdle()
        assertEquals(1, trackingVC.viewDidLoadCount)
        assertEquals(1, trackingVC.viewWillAppearCount)
        assertEquals(1, trackingVC.viewDidAppearCount)
        assertEquals(0, trackingVC.viewWillDisappearCount)
        assertEquals(0, trackingVC.viewDidDisappearCount)

        showDialog = false
        waitForIdle()
        assertEquals(1, trackingVC.viewDidLoadCount)
        assertEquals(1, trackingVC.viewWillAppearCount)
        assertEquals(1, trackingVC.viewDidAppearCount)
        assertEquals(1, trackingVC.viewWillDisappearCount)
        assertEquals(1, trackingVC.viewDidDisappearCount)
    }
}

private class LifecycleTrackingViewController : UIViewController(nibName = null, bundle = null) {
    var viewDidLoadCount = 0
    var viewWillAppearCount = 0
    var viewDidAppearCount = 0
    var viewWillDisappearCount = 0
    var viewDidDisappearCount = 0

    override fun viewDidLoad() {
        super.viewDidLoad()

        viewDidLoadCount++
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)

        viewWillAppearCount++
    }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)

        viewDidAppearCount++
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)

        viewWillDisappearCount++
    }

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)

        viewDidDisappearCount++
    }
}
