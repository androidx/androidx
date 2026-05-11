/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.leaks

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.WeakReference
import androidx.compose.ui.scene.ComposeHostingView
import androidx.compose.ui.scene.ComposeHostingViewController
import androidx.compose.ui.window.ComposeUIView
import androidx.compose.ui.test.MockAppDelegate
import androidx.compose.ui.test.findLayersViewController
import androidx.compose.ui.test.waitForIdle
import androidx.compose.ui.uikit.embedSubview
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.ComposeTextInputView
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import platform.UIKit.UIApplication
import platform.UIKit.UIView
import platform.UIKit.UIViewController

class MemoryLeaksTest {
    @Test
    fun testComposeUIViewControllerDisposal() = runBlocking {
        val appDelegate = MockAppDelegate()
        var composeViewControllerRef: WeakReference<UIViewController>? = null
        var composeLoaded = false

        run {
            val controller = ComposeUIViewController({
                enforceStrictPlistSanityCheck = false
            }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Blue))
                SideEffect {
                    composeLoaded = true
                }
            } as ComposeHostingViewController
            composeViewControllerRef = WeakReference(controller)
            appDelegate.setUpWindow(controller)

            controller.waitForIdle()
        }

        assertTrue(composeLoaded)
        assertNotNull(composeViewControllerRef?.get())

        appDelegate.cleanUp()

        assertDeallocated(composeViewControllerRef)
    }

    @Test
    fun testComposeUIViewControllerSubviewsDisposal() = runBlocking {
        val appDelegate = MockAppDelegate()
        val subviewsReferences = mutableListOf<WeakReference<UIView>>()

        run {
            val controller = ComposeUIViewController({
                enforceStrictPlistSanityCheck = false
            }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Blue))
            } as ComposeHostingViewController

            appDelegate.setUpWindow(controller)

            controller.waitForIdle()
        }

        collectComposeSubviewsRecursively(
            appDelegate.window?.rootViewController?.view!!,
            subviewsReferences
        )

        assertEquals(
            expected = 4,
            actual = subviewsReferences.count(),
            message = "Expected 4 subviews: [ComposeView, UserInputView, MetalView, UIKitTransparentContainerView]" +
                ", but given: ${
                    subviewsReferences.mapNotNull { ref ->
                        ref.get()?.let { it::class.simpleName }
                    }
                }"
        )

        appDelegate.cleanUp()

        assertDeallocated(subviewsReferences)
    }

    @Test
    fun testComposeUIViewControllerWithTextInputDisposal() = runBlocking {
        val appDelegate = MockAppDelegate()
        var composeViewControllerRef: WeakReference<UIViewController>? = null

        run {
            val controller = ComposeUIViewController({
                enforceStrictPlistSanityCheck = false
            }) {
                val focusRequester = FocusRequester()
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester)
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            } as ComposeHostingViewController

            composeViewControllerRef = WeakReference(controller)
            appDelegate.setUpWindow(controller)

            controller.waitForIdle()
        }

        assertNotNull(composeViewControllerRef?.get())

        appDelegate.cleanUp()

        assertDeallocated(composeViewControllerRef)
    }

    @Test
    fun testComposeUIViewControllerSubviewsWithTextInputDisposalAndOldContextMenu() {
        val appDelegate = MockAppDelegate()
        runBlocking(newContextMenuEnabled = false) {
            val subviewsReferences = mutableListOf<WeakReference<UIView>>()

            run {
                val controller = ComposeUIViewController({
                    enforceStrictPlistSanityCheck = false
                }) {
                    val focusRequester = FocusRequester()
                    TextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                } as ComposeHostingViewController

                appDelegate.setUpWindow(controller)

                controller.waitForIdle()
            }

            collectComposeSubviewsRecursively(
                appDelegate.window?.rootViewController?.view!!,
                subviewsReferences
            )

            assertEquals(
                expected = 5,
                actual = subviewsReferences.count(),
                message = "Expected 5 subviews: [ComposeView, UserInputView, MetalView, UIKitTransparentContainerView, IntermediateTextInputUIView]" +
                    ", but given: ${
                        subviewsReferences.mapNotNull { ref ->
                            ref.get()?.let { it::class.simpleName }
                        }
                    }"
            )

            appDelegate.cleanUp()
            // In Kotlin, when UITextInput view becomes a first responder, UIKit captures
            // strong references on this view. For test purposes, staring another text input session
            // to let UIKit release reference to the previous text input view.
            startFakeTextInputSession()

            assertDeallocated(subviewsReferences)
        }
    }

    @Test
    fun testComposeUIViewControllerSubviewsWithTextInputDisposalAndNewContextMenu() {
        val appDelegate = MockAppDelegate()
        runBlocking(newContextMenuEnabled = true) {
            val subviewsReferences = mutableListOf<WeakReference<UIView>>()

            run {
                val controller = ComposeUIViewController({
                    enforceStrictPlistSanityCheck = false
                }) {
                    val focusRequester = FocusRequester()
                    TextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                } as ComposeHostingViewController

                appDelegate.setUpWindow(controller)

                controller.waitForIdle()
            }

            collectComposeSubviewsRecursively(
                appDelegate.window?.rootViewController?.view!!,
                subviewsReferences
            )

            assertEquals(
                expected = 6,
                actual = subviewsReferences.count(),
                message = "Expected 6 subviews: [ComposeView, UserInputView, MetalView, UIKitTransparentContainerView, CMPEditMenuView, IntermediateTextInputUIView]" +
                    ", but given: ${
                        subviewsReferences.mapNotNull { ref ->
                            ref.get()?.let { it::class.simpleName }
                        }
                    }"
            )

            appDelegate.cleanUp()
            // In Kotlin, when UITextInput view becomes a first responder, UIKit captures
            // strong references on this view. For test purposes, staring another text input session
            // to let UIKit release reference to the previous text input view.
            startFakeTextInputSession()

            assertDeallocated(subviewsReferences)
        }
    }

    @Test
    fun testComposeUIViewDisposal() = runBlocking {
        val appDelegate = MockAppDelegate()
        var composeViewRef: WeakReference<UIView>? = null
        var composeLoaded = false

        run {
            val view = ComposeUIView({
                enforceStrictPlistSanityCheck = false
            }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Blue))
                SideEffect {
                    composeLoaded = true
                }
            } as ComposeHostingView
            composeViewRef = WeakReference(view)
            val controller = UIViewController()
            controller.view.embedSubview(view)
            appDelegate.setUpWindow(controller)

            view.waitForIdle()
        }

        assertTrue(composeLoaded)
        assertNotNull(composeViewRef?.get())

        appDelegate.cleanUp()

        assertDeallocated(composeViewRef)
    }

    @Test
    fun testComposeUIViewSubviewsDisposal() = runBlocking {
        val appDelegate = MockAppDelegate()
        val subviewsReferences = mutableListOf<WeakReference<UIView>>()

        run {
            val view = ComposeUIView({
                enforceStrictPlistSanityCheck = false
            }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Blue))
            } as ComposeHostingView
            val controller = UIViewController()
            controller.view.embedSubview(view)
            appDelegate.setUpWindow(controller)

            view.waitForIdle()
        }

        collectComposeSubviewsRecursively(
            appDelegate.window?.rootViewController?.view!!,
            subviewsReferences
        )

        assertEquals(
            expected = 5,
            actual = subviewsReferences.count(),
            message = "Expected 5 subviews: [ComposeHostingView, ComposeView, UserInputView, MetalView, UIKitTransparentContainerView]" +
                ", but given: ${
                    subviewsReferences.mapNotNull { ref ->
                        ref.get()?.let { it::class.simpleName }
                    }
                }"
        )

        appDelegate.cleanUp()

        assertDeallocated(subviewsReferences)
    }

    @Test
    fun testComposeUIViewWithTextInputDisposal() = runBlocking {
        val appDelegate = MockAppDelegate()
        var composeViewRef: WeakReference<UIView>? = null

        run {
            val view = ComposeUIView({
                enforceStrictPlistSanityCheck = false
            }) {
                val focusRequester = FocusRequester()
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester)
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            } as ComposeHostingView
            composeViewRef = WeakReference(view)
            val controller = UIViewController()
            controller.view.embedSubview(view)
            appDelegate.setUpWindow(controller)

            view.waitForIdle()
        }

        assertNotNull(composeViewRef?.get())

        appDelegate.cleanUp()

        assertDeallocated(composeViewRef)
    }

    @Test
    fun testComposeUIViewSubviewsWithTextInputDisposalAndNewContextMenu() {
        val appDelegate = MockAppDelegate()
        runBlocking(newContextMenuEnabled = true) {
            val subviewsReferences = mutableListOf<WeakReference<UIView>>()

            run {
                val view = ComposeUIView({
                    enforceStrictPlistSanityCheck = false
                }) {
                    val focusRequester = FocusRequester()
                    TextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                } as ComposeHostingView
                val controller = UIViewController()
                controller.view.embedSubview(view)
                appDelegate.setUpWindow(controller)

                view.waitForIdle()
            }

            collectComposeSubviewsRecursively(
                appDelegate.window?.rootViewController?.view!!,
                subviewsReferences
            )

            assertEquals(
                expected = 7,
                actual = subviewsReferences.count(),
                message = "Expected 7 subviews: [ComposeHostingView, ComposeView, UserInputView, MetalView, UIKitTransparentContainerView, CMPEditMenuView, IntermediateTextInputUIView]" +
                    ", but given: ${
                        subviewsReferences.mapNotNull { ref ->
                            ref.get()?.let { it::class.simpleName }
                        }
                    }"
            )

            appDelegate.cleanUp()
            // In Kotlin, when UITextInput view becomes a first responder, UIKit captures
            // strong references on this view. For test purposes, staring another text input session
            // to let UIKit release reference to the previous text input view.
            startFakeTextInputSession()

            assertDeallocated(subviewsReferences)
        }
    }

    @OptIn(NativeRuntimeApi::class)
    @Test
    fun testComposeLayersViewControllerDisposal() = runBlocking {
        val appDelegate = MockAppDelegate()
        var layersViewControllerRef: WeakReference<UIViewController>? = null
        var dialogLoaded = false

        run {
            val controller = ComposeUIViewController({
                enforceStrictPlistSanityCheck = false
            }) {
                Dialog({}) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Blue))
                    SideEffect {
                        dialogLoaded = true
                    }
                }
            } as ComposeHostingViewController

            appDelegate.setUpWindow(controller)

            controller.waitForIdle()

            val layersViewController = appDelegate.findLayersViewController()
            layersViewControllerRef = WeakReference(layersViewController)
        }

        assertTrue(dialogLoaded)
        assertNotNull(layersViewControllerRef?.get())

        appDelegate.cleanUp()

        assertDeallocated(layersViewControllerRef)
    }

    @Test
    fun testComposeLayersViewControllerSubviewsDisposal() {
        val appDelegate = MockAppDelegate()
        runBlocking(newContextMenuEnabled = true) {
            val subviewsReferences = mutableListOf<WeakReference<UIView>>()

            run {
                val controller = ComposeUIViewController({
                    enforceStrictPlistSanityCheck = false
                }) {
                    Dialog({}) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Blue))
                    }
                } as ComposeHostingViewController

                appDelegate.setUpWindow(controller)

                controller.waitForIdle()
            }

            val layersViewController = appDelegate.findLayersViewController()
            collectComposeSubviewsRecursively(
                layersViewController.view,
                subviewsReferences
            )

            assertEquals(
                expected = 6,
                actual = subviewsReferences.count(),
                message = "Expected 6 subviews: [ComposeLayersView, ComposeContainerView, UIKitComposeSceneLayerView, BackgroundInputView, MetalView, OverlayInputView]" +
                    ", but given: ${
                        subviewsReferences.mapNotNull { ref ->
                            ref.get()?.let { it::class.simpleName }
                        }
                    }"
            )

            appDelegate.cleanUp()

            assertDeallocated(subviewsReferences)
        }
    }

    @OptIn(NativeRuntimeApi::class)
    internal suspend fun assertDeallocated(reference: List<WeakReference<*>>) {
        val duration = 100.milliseconds
        repeat((5.seconds / duration).toInt()) {
            runApplicationLoop(duration)
            GC.collect()
            if (reference.all { it.get() == null }) return
        }

        fail("Memory leak detected: references ${reference.mapNotNull { it.get() }} were not collected")
    }

    @OptIn(NativeRuntimeApi::class)
    internal suspend fun assertDeallocated(reference: WeakReference<*>) {
        assertDeallocated(listOf(reference))
    }

    private fun collectComposeSubviewsRecursively(
        view: UIView,
        result: MutableList<WeakReference<UIView>>
    ) {
        if (view::class != UIView::class) {
            result.add(WeakReference(view))
        }
        for (subview in view.subviews) {
            collectComposeSubviewsRecursively(subview as UIView, result)
        }
    }

    private suspend fun runApplicationLoop(duration: Duration) {
        NSRunLoop.currentRunLoop().runUntilDate(
            limitDate = NSDate.dateWithTimeIntervalSinceNow(
                secs = duration.toDouble(DurationUnit.SECONDS)
            )
        )
        delay(duration.inWholeMilliseconds)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun startFakeTextInputSession(useNativeInput: Boolean = false) {
        val input = ComposeTextInputView(0)
        UIApplication.sharedApplication.keyWindow?.rootViewController?.view?.addSubview(input)
        input.setFrame(CGRectMake(0.0, 0.0, 100.0, 100.0))
        input.becomeFirstResponder()
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun runBlocking(newContextMenuEnabled: Boolean, block: suspend () -> Unit) {
        val defaultValue = ComposeFoundationFlags.isNewContextMenuEnabled
        try {
            ComposeFoundationFlags.isNewContextMenuEnabled = newContextMenuEnabled
            runBlocking { block() }
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = defaultValue
        }
    }
}
