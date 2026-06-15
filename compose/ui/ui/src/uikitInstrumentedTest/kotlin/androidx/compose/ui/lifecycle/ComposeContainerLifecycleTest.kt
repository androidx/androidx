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

package androidx.compose.ui.lifecycle

import androidx.compose.material.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.scene.ComposeHostingView
import androidx.compose.ui.scene.ComposeHostingViewController
import androidx.compose.ui.test.MockAppDelegate
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.waitForIdle
import androidx.compose.ui.uikit.embedSubview
import androidx.compose.ui.window.ComposeUIView
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.autoreleasepool
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.removeFromParentViewController
import platform.UIKit.willMoveToParentViewController

@OptIn(NativeRuntimeApi::class, BetaInteropApi::class)
class ComposeContainerLifecycleTest {
    @OptIn(BetaInteropApi::class)
    @Test
    fun composeViewControllerLifecycleResumed() = runBlocking {
        val testViewController = TestContainerViewController()
        val appDelegate = MockAppDelegate()
        appDelegate.setUpWindow(testViewController)
        var launchesCount = 0
        var disposedCount = 0

        run {
            val compose = ComposeUIViewController({
                enforceStrictPlistSanityCheck = false
            }) {
                DisposableEffect(Unit) {
                    launchesCount++
                    onDispose {
                        disposedCount++
                    }
                }
            } as ComposeHostingViewController

            testViewController.showChildViewController(compose)
            compose.waitForIdle()
            assertEquals(Lifecycle.State.RESUMED, compose.lifecycleState)
            assertEquals(1, launchesCount)

            testViewController.hideChildViewController()
            UIKitInstrumentedTest.waitUntil { disposedCount == 1 }
            assertEquals(Lifecycle.State.CREATED, compose.lifecycleState)

            testViewController.showChildViewController(compose)
            compose.waitForIdle()
            assertEquals(Lifecycle.State.RESUMED, compose.lifecycleState)
            assertEquals(2, launchesCount)

            testViewController.hideChildViewController()
            UIKitInstrumentedTest.waitUntil { disposedCount == 2 }
            assertEquals(Lifecycle.State.CREATED, compose.lifecycleState)
        }

        appDelegate.cleanUp()
    }

    @OptIn(BetaInteropApi::class)
    @Test
    fun composeViewLifecycleResumed() = runBlocking {
        val testViewController = TestContainerViewController()
        val appDelegate = MockAppDelegate()
        appDelegate.setUpWindow(testViewController)
        var launchesCount = 0
        var disposedCount = 0

        run {
            val compose = ComposeUIView({
                enforceStrictPlistSanityCheck = false
            }) {
                DisposableEffect(Unit) {
                    launchesCount++
                    onDispose {
                        disposedCount++
                    }
                }
            } as ComposeHostingView

            testViewController.showChildView(compose)
            compose.waitForIdle()
            assertEquals(Lifecycle.State.RESUMED, compose.lifecycleState)
            assertEquals(1, launchesCount)

            testViewController.hideChildView()
            UIKitInstrumentedTest.waitUntil { disposedCount == 1 }
            assertEquals(Lifecycle.State.CREATED, compose.lifecycleState)

            testViewController.showChildView(compose)
            compose.waitForIdle()
            assertEquals(Lifecycle.State.RESUMED, compose.lifecycleState)
            assertEquals(2, launchesCount)

            testViewController.hideChildView()
            UIKitInstrumentedTest.waitUntil { disposedCount == 2 }
            assertEquals(Lifecycle.State.CREATED, compose.lifecycleState)
        }

        appDelegate.cleanUp()
    }

    @OptIn(BetaInteropApi::class)
    @Test
    fun composeViewControllerViewModelInitialisedAndCleared() = runBlocking {
        val testViewController = TestContainerViewController()
        val appDelegate = MockAppDelegate()
        appDelegate.setUpWindow(testViewController)
        val viewModel = TestViewModel()
        var disposedCount = 0

        run {
            val compose = ComposeUIViewController({
                enforceStrictPlistSanityCheck = false
            }) {
                val vm = viewModel {
                    viewModel.also { it.createdCount++ }
                }
                DisposableEffect(Unit) {
                    onDispose {
                        disposedCount++
                    }
                }
                Text("${vm.hashCode()}")
            } as ComposeHostingViewController

            testViewController.showChildViewController(compose)
            compose.waitForIdle()
            assertEquals(1, viewModel.createdCount, "View models must be initialized")

            testViewController.hideChildViewController()
            UIKitInstrumentedTest.waitUntil { disposedCount == 1 }

            testViewController.showChildViewController(compose)
            compose.waitForIdle()
            assertEquals(1, viewModel.createdCount, "View models should not be re-created")

            testViewController.hideChildViewController()
            UIKitInstrumentedTest.waitUntil { disposedCount == 2 }
        }

        appDelegate.cleanUp()

        awaitTrue {
            GC.collect()
            viewModel.cleared
        }
    }

    @OptIn(BetaInteropApi::class)
    @Test
    fun composeViewViewModelInitialisedAndCleared() = runBlocking {
        val testViewController = TestContainerViewController()
        val appDelegate = MockAppDelegate()
        appDelegate.setUpWindow(testViewController)
        val viewModel = TestViewModel()
        var disposedCount = 0

        run {
            val compose = ComposeUIView({
                enforceStrictPlistSanityCheck = false
            }) {
                val vm = viewModel {
                    viewModel.also { it.createdCount++ }
                }
                DisposableEffect(Unit) {
                    onDispose {
                        disposedCount++
                    }
                }
                Text("${vm.hashCode()}")
            } as ComposeHostingView

            testViewController.showChildView(compose)
            compose.waitForIdle()
            assertEquals(1, viewModel.createdCount, "View models must be initialized")

            testViewController.hideChildView()
            UIKitInstrumentedTest.waitUntil { disposedCount == 1 }

            testViewController.showChildView(compose)
            compose.waitForIdle()
            assertEquals(1, viewModel.createdCount, "View models should not be re-created")

            testViewController.hideChildView()
            UIKitInstrumentedTest.waitUntil { disposedCount == 2 }
        }

        appDelegate.cleanUp()

        awaitTrue {
            GC.collect()
            viewModel.cleared
        }
    }

    @OptIn(BetaInteropApi::class)
    @Test
    fun composeViewControllerSavedStateRestored() = runBlocking {
        val testViewController = TestContainerViewController()
        val appDelegate = MockAppDelegate()
        appDelegate.setUpWindow(testViewController)
        var disposedCount = 0
        var rememberedValue = 0
        var rememberedSavableValue = 0

        run {
            val compose = ComposeUIViewController({
                enforceStrictPlistSanityCheck = false
            }) {
                DisposableEffect(Unit) {
                    onDispose {
                        disposedCount++
                    }
                }
                var value1 by remember { mutableStateOf(0) }
                var value2 by rememberSaveable { mutableStateOf(0) }
                LaunchedEffect(Unit) {
                    value1++
                    value2++

                    rememberedValue = value1
                    rememberedSavableValue = value2
                }
            } as ComposeHostingViewController

            testViewController.showChildViewController(compose)
            compose.waitForIdle()
            assertEquals(1, rememberedValue)
            assertEquals(1, rememberedSavableValue)

            testViewController.hideChildViewController()
            UIKitInstrumentedTest.waitUntil { disposedCount == 1 }

            testViewController.showChildViewController(compose)
            compose.waitForIdle()
            assertEquals(1, rememberedValue)
            assertEquals(2, rememberedSavableValue)

            testViewController.hideChildViewController()
            UIKitInstrumentedTest.waitUntil { disposedCount == 2 }
        }

        appDelegate.cleanUp()
    }


    @OptIn(BetaInteropApi::class)
    @Test
    fun composeViewSavedStateRestored() = runBlocking {
        val testViewController = TestContainerViewController()
        val appDelegate = MockAppDelegate()
        appDelegate.setUpWindow(testViewController)
        var disposedCount = 0
        var rememberedValue = 0
        var rememberedSavableValue = 0

        run {
            val compose = ComposeUIView({
                enforceStrictPlistSanityCheck = false
            }) {
                DisposableEffect(Unit) {
                    onDispose {
                        disposedCount++
                    }
                }
                val value1 = remember { mutableStateOf(0) }
                val value2 = rememberSaveable { mutableStateOf(0) }
                LaunchedEffect(Unit) {
                    value1.value++
                    value2.value++

                    rememberedValue = value1.value
                    rememberedSavableValue = value2.value
                }
            } as ComposeHostingView

            testViewController.showChildView(compose)
            compose.waitForIdle()
            assertEquals(1, rememberedValue)
            assertEquals(1, rememberedSavableValue)

            testViewController.hideChildView()
            UIKitInstrumentedTest.waitUntil { disposedCount == 1 }

            testViewController.showChildView(compose)
            compose.waitForIdle()
            assertEquals(1, rememberedValue)
            assertEquals(2, rememberedSavableValue)

            testViewController.hideChildView()
            UIKitInstrumentedTest.waitUntil { disposedCount == 2 }
        }

        appDelegate.cleanUp()
    }

    private suspend inline fun awaitTrue(statement: () -> Boolean) {
        val duration = 100.milliseconds
        val timeout = 5.seconds
        repeat((timeout / duration).toInt()) {
            NSRunLoop.currentRunLoop().runUntilDate(
                limitDate = NSDate.dateWithTimeIntervalSinceNow(
                    secs = duration.toDouble(DurationUnit.SECONDS)
                )
            )
            delay(duration.inWholeMilliseconds)
            if (statement()) return
        }

        val result = autoreleasepool {
            statement()
        }
        assertTrue(result)
    }
}

private class TestContainerViewController: UIViewController(nibName = null, bundle = null) {

    private var childViewController: UIViewController? = null
    private var childView: UIView? = null

    fun showChildView(child: UIView) {
        if (childView === child) return
        hideChildView()

        view.embedSubview(child)
        childView = child
    }

    fun hideChildView() {
        val child = childView ?: return

        child.removeFromSuperview()
        childView = null
    }

    fun showChildViewController(child: UIViewController) {
        if (childViewController === child) return
        hideChildViewController()

        addChildViewController(child)
        view.embedSubview(child.view)
        child.didMoveToParentViewController(this)
        childViewController = child
    }

    fun hideChildViewController() {
        val child = childViewController ?: return

        child.willMoveToParentViewController(null)
        child.view.removeFromSuperview()
        child.removeFromParentViewController()
        child.didMoveToParentViewController(null)
        childViewController = null
    }
}

class TestViewModel: androidx.lifecycle.ViewModel() {
    var createdCount = 0
    var cleared = false

    override fun onCleared() {
        cleared = true
    }
}

