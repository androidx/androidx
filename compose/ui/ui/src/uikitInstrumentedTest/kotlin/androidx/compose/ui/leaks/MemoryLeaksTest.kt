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
import androidx.compose.ui.window.ComposeUIView
import androidx.compose.ui.test.MockAppDelegate
import androidx.compose.ui.uikit.embedSubview
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.window.IntermediateTextInputUIView
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
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
    companion object {
        private val KeyboardAnimationDelay = 600.milliseconds
    }

    @Test
    fun testComposeUIViewControllerDisposal() = runRepeatingBlocking {
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
            }
            composeViewControllerRef = WeakReference(controller)
            appDelegate.setUpWindow(controller)
        }

        // Allow run loop to start the application
        runApplicationLoop(1.milliseconds)

        assertTrue(composeLoaded)
        assertNotNull(composeViewControllerRef?.get())

        appDelegate.cleanUp()
        cleanupMemory()

        assertNull(composeViewControllerRef.get())
    }

    @Test
    fun testComposeUIViewControllerSubviewsDisposal() = runRepeatingBlocking {
        val appDelegate = MockAppDelegate()
        val subviewsReferences = mutableListOf<WeakReference<UIView>>()

        run {
            val controller = ComposeUIViewController({
                enforceStrictPlistSanityCheck = false
            }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Blue))
            }
            appDelegate.setUpWindow(controller)
        }

        // Allow run loop to start the application
        runApplicationLoop(1.milliseconds)

        collectSubviewsRecursively(
            appDelegate.window?.rootViewController?.view!!,
            subviewsReferences
        )

        assertEquals(
            expected = 4,
            actual = subviewsReferences.count(),
            message = "Expected 4 subviews: [ComposeView, UserInputView, MetalView, UIKitTransparentContainerView]" +
                ", but given: ${
                    subviewsReferences.mapNotNull {
                        it.get()?.let { it::class.simpleName }
                    }
                }"
        )

        appDelegate.cleanUp()
        cleanupMemory()

        assertEquals(emptyList(), subviewsReferences.mapNotNull { it.get() })
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
            }

            composeViewControllerRef = WeakReference(controller)
            appDelegate.setUpWindow(controller)
        }

        // Allow run loop to start the application
        runApplicationLoop(KeyboardAnimationDelay)

        assertNotNull(composeViewControllerRef?.get())

        appDelegate.cleanUp()
        cleanupMemory()

        assertNull(composeViewControllerRef.get())
    }

    @Test
    fun testComposeUIViewControllerSubviewsWithTextInputDisposalAndOldContextMenu() =
        runRepeatingBlocking(newContextMenuEnabled = false) {
            val appDelegate = MockAppDelegate()
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
                }

                appDelegate.setUpWindow(controller)
            }

            // Allow run loop to start the application
            runApplicationLoop(KeyboardAnimationDelay)

            collectSubviewsRecursively(
                appDelegate.window?.rootViewController?.view!!,
                subviewsReferences
            )

            assertEquals(
                expected = 5,
                actual = subviewsReferences.count(),
                message = "Expected 5 subviews: [ComposeView, UserInputView, MetalView, UIKitTransparentContainerView, IntermediateTextInputUIView]" +
                    ", but given: ${
                        subviewsReferences.mapNotNull {
                            it.get()?.let { it::class.simpleName }
                        }
                    }"
            )

            appDelegate.cleanUp()
            // In Kotlin, when UITextInput view becomes a first responder, UIKit captures
            // strong references on this view. For test purposes, staring another text input session
            // to let UIKit release reference to the previous text input view.
            startFakeTextInputSession()

            cleanupMemory()

            assertEquals(emptyList(), subviewsReferences.mapNotNull { it.get() })
        }

    @Test
    fun testComposeUIViewControllerSubviewsWithTextInputDisposalAndNewContextMenu() =
        runRepeatingBlocking(newContextMenuEnabled = true) {
            val appDelegate = MockAppDelegate()
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
                }

                appDelegate.setUpWindow(controller)
            }

            // Allow run loop to start the application
            runApplicationLoop(KeyboardAnimationDelay)

            collectSubviewsRecursively(
                appDelegate.window?.rootViewController?.view!!,
                subviewsReferences
            )

            assertEquals(
                expected = 6,
                actual = subviewsReferences.count(),
                message = "Expected 6 subviews: [ComposeView, UserInputView, MetalView, UIKitTransparentContainerView, CMPEditMenuView, IntermediateTextInputUIView]" +
                    ", but given: ${
                        subviewsReferences.mapNotNull {
                            it.get()?.let { it::class.simpleName }
                        }
                    }"
            )

            appDelegate.cleanUp()
            // In Kotlin, when UITextInput view becomes a first responder, UIKit captures
            // strong references on this view. For test purposes, staring another text input session
            // to let UIKit release reference to the previous text input view.
            startFakeTextInputSession()

            cleanupMemory()

            assertEquals(emptyList(), subviewsReferences.mapNotNull { it.get() })
        }

    @Test
    fun testComposeUIViewDisposal() = runRepeatingBlocking {
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
            }
            composeViewRef = WeakReference(view)
            val controller = UIViewController()
            controller.view.embedSubview(view)
            appDelegate.setUpWindow(controller)
        }

        // Allow run loop to start the application
        runApplicationLoop(1.milliseconds)

        assertTrue(composeLoaded)
        assertNotNull(composeViewRef?.get())

        appDelegate.cleanUp()
        cleanupMemory()

        assertNull(composeViewRef.get())
    }

    @Test
    fun testComposeUIViewSubviewsDisposal() = runRepeatingBlocking {
        val appDelegate = MockAppDelegate()
        val subviewsReferences = mutableListOf<WeakReference<UIView>>()

        run {
            val view = ComposeUIView({
                enforceStrictPlistSanityCheck = false
            }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Blue))
            }
            val controller = UIViewController()
            controller.view.embedSubview(view)
            appDelegate.setUpWindow(controller)
        }

        // Allow run loop to start the application
        runApplicationLoop(1.milliseconds)

        collectSubviewsRecursively(
            appDelegate.window?.rootViewController?.view!!,
            subviewsReferences
        )

        assertEquals(
            expected = 6,
            actual = subviewsReferences.count(),
            message = "Expected 6 subviews: [UIView, ComposeHostingView, ComposeView, UserInputView, MetalView, UIKitTransparentContainerView]" +
                ", but given: ${
                    subviewsReferences.mapNotNull {
                        it.get()?.let { it::class.simpleName }
                    }
                }"
        )

        appDelegate.cleanUp()
        cleanupMemory()

        assertEquals(emptyList(), subviewsReferences.mapNotNull { it.get() })
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
            }
            composeViewRef = WeakReference(view)
            val controller = UIViewController()
            controller.view.embedSubview(view)
            appDelegate.setUpWindow(controller)
        }

        // Allow run loop to start the application
        runApplicationLoop(KeyboardAnimationDelay)

        assertNotNull(composeViewRef?.get())

        appDelegate.cleanUp()
        cleanupMemory()

        assertNull(composeViewRef.get())
    }

    @Test
    fun testComposeUIViewSubviewsWithTextInputDisposalAndNewContextMenu() =
        runRepeatingBlocking(newContextMenuEnabled = true) {
            val appDelegate = MockAppDelegate()
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
                }
                val controller = UIViewController()
                controller.view.embedSubview(view)
                appDelegate.setUpWindow(controller)
            }

            // Allow run loop to start the application
            runApplicationLoop(KeyboardAnimationDelay)

            collectSubviewsRecursively(
                appDelegate.window?.rootViewController?.view!!,
                subviewsReferences
            )

            assertEquals(
                expected = 8,
                actual = subviewsReferences.count(),
                message = "Expected 8 subviews: [UIView, ComposeHostingView, ComposeView, UserInputView, MetalView, UIKitTransparentContainerView, CMPEditMenuView, IntermediateTextInputUIView]" +
                    ", but given: ${
                        subviewsReferences.mapNotNull {
                            it.get()?.let { it::class.simpleName }
                        }
                    }"
            )

            appDelegate.cleanUp()
            // In Kotlin, when UITextInput view becomes a first responder, UIKit captures
            // strong references on this view. For test purposes, staring another text input session
            // to let UIKit release reference to the previous text input view.
            startFakeTextInputSession()

            cleanupMemory()

            assertEquals(emptyList(), subviewsReferences.mapNotNull { it.get() })
        }

    private fun collectSubviewsRecursively(
        view: UIView,
        result: MutableList<WeakReference<UIView>>
    ) {
        result.add(WeakReference(view))
        for (subview in view.subviews) {
            collectSubviewsRecursively(subview as UIView, result)
        }
    }

    @OptIn(NativeRuntimeApi::class)
    private suspend fun cleanupMemory() {
        // Wait until the Compose view controller leaves the view hierarchy.
        repeat(6) {
            runApplicationLoop(100.milliseconds)
            GC.collect()
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
    private suspend fun startFakeTextInputSession() {
        val input = IntermediateTextInputUIView(0)
        UIApplication.sharedApplication.keyWindow?.rootViewController?.view?.addSubview(input)
        input.setFrame(CGRectMake(0.0, 0.0, 100.0, 100.0))
        input.becomeFirstResponder()
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun runRepeatingBlocking(newContextMenuEnabled: Boolean, block: suspend () -> Unit) {
        val defaultValue = ComposeFoundationFlags.isNewContextMenuEnabled
        try {
            ComposeFoundationFlags.isNewContextMenuEnabled = newContextMenuEnabled
            runRepeatingBlocking { block() }
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = defaultValue
        }
    }

    private fun runRepeatingBlocking(
        total: Int = 10,
        successRequired: Int = 2,
        testBlock: suspend CoroutineScope.(Int) -> Unit
    ) = runBlocking {
        var successCount = 0
        var failureCount = 0
        repeat(total) {
            try {
                testBlock(successCount + failureCount)
                successCount++
                if (successCount >= successRequired) {
                    return@runBlocking
                }
            } catch (e: Throwable) {
                failureCount++
                if (failureCount > total - successRequired) {
                    throw e
                }
                cleanupMemory()
            }
        }
    }
}