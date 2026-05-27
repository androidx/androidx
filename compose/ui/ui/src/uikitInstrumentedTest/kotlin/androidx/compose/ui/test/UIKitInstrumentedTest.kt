/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.AccessibilityNotification
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.scene.ComposeHostingView
import androidx.compose.ui.scene.ComposeHostingViewController
import androidx.compose.ui.scene.ComposeLayersViewController
import androidx.compose.ui.test.utils.center
import androidx.compose.ui.test.utils.getTouchesEvent
import androidx.compose.ui.test.utils.mouseDown
import androidx.compose.ui.test.utils.moveToLocationOnWindow
import androidx.compose.ui.test.utils.resetTouches
import androidx.compose.ui.test.utils.toCGPoint
import androidx.compose.ui.test.utils.touchDown
import androidx.compose.ui.test.utils.up
import androidx.compose.ui.uikit.ComposeContainerConfiguration
import androidx.compose.ui.uikit.ComposeUIViewConfiguration
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.embedSubview
import androidx.compose.ui.uikit.utils.CMPUIWindowSceneUtils
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.toDpOffset
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.size
import androidx.compose.ui.window.KeyboardVisibilityListener
import androidx.compose.ui.window.MetalRedrawer
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertNotNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDelegateProtocol
import platform.UIKit.UIColor
import platform.UIKit.UIDevice
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UIInterfaceOrientationMaskAll
import platform.UIKit.UIInterfaceOrientationMaskLandscapeLeft
import platform.UIKit.UIInterfaceOrientationMaskLandscapeRight
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.UIInterfaceOrientationMaskPortraitUpsideDown
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
import platform.UIKit.UIScreen
import platform.UIKit.UITextInputProtocol
import platform.UIKit.UITouch
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.endEditing
import platform.UIKit.systemBackgroundColor
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Sets up the test environment for iOS instrumented tests, runs the given [test][testBlock] against
 * UIView- and UIViewController-based Compose Container.
 * Then tears down the test environment.
 * Use the methods on [UIKitInstrumentedTest] in the test to find compose content and make
 * assertions on it.
 * @param [testBlock] The test function.
 */
internal fun runUIKitInstrumentedTest(testBlock: UIKitInstrumentedTest.() -> Unit) {
    println("Debug: Running test with ComposeHostingView")
    with(UIKitInstrumentedTest(useHostingView = true)) {
        try {
            testBlock()
        } finally {
            tearDown()
        }
    }

    println("Debug: Running test with ComposeHostingViewController")
    with(UIKitInstrumentedTest(useHostingView = false)) {
        try {
            testBlock()
        } finally {
            tearDown()
        }
    }
}

/**
 * Sets up the test environment for iOS instrumented tests, runs the given [test][testBlock] against
 * UIView- and UIViewController-based Compose Container, passing provided parameters to each test.
 * Then tears down the test environment.
 * Use the methods on [UIKitInstrumentedTest] in the test to find compose content and make
 * assertions on it.
 * @param [ignoreIf] Condition to ignore the test.
 * @param [ignoreNotes] A description of why the test is ignored if it doesn't run.
 * @param [params] The parameters to configure the test run.
 * @param [testBlock] The test function.
 */
internal fun <T> runUIKitInstrumentedTest(
    ignoreIf: Boolean = false,
    ignoreNotes: String = "",
    params: List<T>,
    testBlock: UIKitInstrumentedTest.(T) -> Unit
) {
    if (ignoreIf) {
        println("Debug: Ignored test: $ignoreNotes")
        return
    }

    for (param in params) {
        with(UIKitInstrumentedTest(useHostingView = true)) {
            try {
                testBlock(param)
            } finally {
                tearDown()
            }
        }

        with(UIKitInstrumentedTest(useHostingView = false)) {
            try {
                testBlock(param)
            } finally {
                tearDown()
            }
        }
    }
}

/**
 * Sets up the test environment for iOS instrumented tests, runs the given [test][testBlock]
 * and then tears down the test environment. Use the methods on [UIKitInstrumentedTest]
 * in the test to find compose content and make assertions on it.
 * @param [ignoreIf] Condition to ignore the test.
 * @param [ignoreNotes] A description of why the test is ignored if it doesn't run.
 * @param [testBlock] The test function.
 */
internal fun runUIKitInstrumentedTest(
    ignoreIf: Boolean,
    ignoreNotes: String,
    testBlock: UIKitInstrumentedTest.() -> Unit
) {
    if (ignoreIf) {
        println("Debug: Ignored test: $ignoreNotes")
        return
    }
    runUIKitInstrumentedTest(testBlock = testBlock)
}

/**
 * A class designed for instrumented testing of UIKit-related functionality. It provides methods for setting
 * content, simulating user interactions, and managing application lifecycle during testing scenarios.
 *
 * This class is primarily intended for internal use within a Compose multiplatform environment that integrates
 * with UIKit APIs on iOS.
 *
 * Constructor properties are initialized with the attributes of the main screen and a mock delegate to simulate
 * the application setup.
 */
@OptIn(ExperimentalForeignApi::class)
internal class UIKitInstrumentedTest(
    private val useHostingView: Boolean
) {
    companion object {
        fun delay(timeoutMillis: Long) {
            val runLoop = NSRunLoop.currentRunLoop()
            runLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(timeoutMillis.toDouble() / 1000.0))
        }

        fun waitUntil(
            conditionDescription: String? = null,
            timeoutMillis: Long = 5_000,
            condition: () -> Boolean
        ) {
            val runLoop = NSRunLoop.currentRunLoop()
            val endTime = TimeSource.Monotonic.markNow() + timeoutMillis.milliseconds
            while (!condition()) {
                if (TimeSource.Monotonic.markNow() > endTime) {
                    throw AssertionError(conditionDescription ?: "Timeout ${timeoutMillis}ms reached.")
                }
                runLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.005))
            }
        }

        val iPadOrientationChangesNotSupported: Boolean get() =
            !(available(OS.Ios to OSVersion(16)) && isRunningOnIPad)

        val isRunningOnIPad: Boolean get() = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad
    }

    private val screen = UIScreen.mainScreen()
    val density = Density(density = screen.scale.toFloat())
    val appDelegate = MockAppDelegate()
    val keyboardHeight: Dp get() =
        KeyboardVisibilityListener.keyboardFrame.useContents { size.height.dp }
    val screenBounds: DpRect get() = screen.bounds().toDpRect()
    val screenSize: DpSize get() = screenBounds.size
    val safeDrawingRect: DpRect get() = screenBounds.let { rect ->
        viewController.view.safeAreaInsets.useContents {
            DpRect(
                left = rect.left + Dp(this.left.toFloat()),
                top = rect.top + Dp(this.top.toFloat()),
                right = rect.right - Dp(this.right.toFloat()),
                bottom = rect.bottom - Dp(this.bottom.toFloat()),
            )
        }
    }
    val accessibilityNotifications = mutableListOf<AccessibilityNotification>()
    val lastAccessibilityNotification: AccessibilityNotification?
        get() = accessibilityNotifications.lastOrNull()
    private var hostingViewController: ComposeHostingViewController? = null
    private var hostingView: ComposeHostingView? = null

    val viewController: UIViewController get() =
        appDelegate.window?.rootViewController ?: error("Cannot find active UIViewController")

    val rootRedrawer: MetalRedrawer? get() =
        hostingView?.rootRedrawer ?: hostingViewController?.rootRedrawer

    private val infiniteAnimationPolicy = object : InfiniteAnimationPolicy {
        override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R {
            throw CancellationException("Infinite animations are disabled on tests")
        }
    }

    private val coroutineContext = Dispatchers.Main + infiniteAnimationPolicy

    fun setContent(
        configure: ComposeContainerConfiguration.() -> Unit = {},
        interfaceOrientation: UIInterfaceOrientation = UIInterfaceOrientationPortrait,
        content: @Composable () -> Unit
    ) {
        accessibilityNotifications.clear()
        AccessibilityNotification.onNotificationPostedForTests = {
            accessibilityNotifications.add(it)
        }
        val innerConfigure: ComposeContainerConfiguration.() -> Unit = {
            enforceStrictPlistSanityCheck = false
            configure()
        }

        val rootViewController: UIViewController = if (useHostingView) {
            hostingView = ComposeHostingView(
                configuration = ComposeUIViewConfiguration().apply(innerConfigure),
                content = content,
                coroutineContext = coroutineContext
            )
            UIViewController().also {
                it.view.embedSubview(hostingView!!)
            }
        } else {
            ComposeHostingViewController(
                configuration = ComposeUIViewControllerConfiguration().apply(innerConfigure),
                content = content,
                coroutineContext = coroutineContext
            ).also {
                hostingViewController = it
            }
        }

        appDelegate.setUpWindow(rootViewController)
        waitForIdle()

        if (appDelegate.requestInterfaceOrientationChangeIfNeeded(interfaceOrientation)) {
            delay(700)
        }
    }

    fun tearDown() {
        // Stop text editing and hide keyboard if any
        viewController.view.endEditing(force = true)
        waitForIdle()

        AccessibilityNotification.onNotificationPostedForTests = null
        appDelegate.cleanUp()
    }

    fun stopComposeScene() {
        hostingView?.viewDidLeaveWindowHierarchy()
        hostingViewController?.viewControllerDidLeaveWindowHierarchy()
    }

    private val isIdle: Boolean
        get() {
            val hadSnapshotChanges = Snapshot.current.hasPendingChanges()
            val isApplyObserverNotificationPending = Snapshot.isApplyObserverNotificationPending
            val containerInvalidations =
                hostingViewController?.hasInvalidations() ?: hostingView?.hasInvalidations()
                ?: false

            return !hadSnapshotChanges && !isApplyObserverNotificationPending && !containerInvalidations
        }

    fun waitForIdle(timeoutMillis: Long = 5_000) {
        waitUntil(
            conditionDescription = "waitForIdle: timeout ${timeoutMillis}ms reached.",
            timeoutMillis = timeoutMillis
        ) { isIdle }
    }

    fun delay(timeoutMillis: Long) = UIKitInstrumentedTest.delay(timeoutMillis)

    fun waitUntil(
        conditionDescription: String? = null,
        timeoutMillis: Long = 5_000,
        condition: () -> Boolean
    ) = UIKitInstrumentedTest.waitUntil(conditionDescription, timeoutMillis, condition)

    // Touches:

    /**
     * Simulates a touch-down event at the specified position on the screen.
     *
     * @param position The position on the window.
     * @param window will be used to handle touches; otherwise,
     * the window hosting the view will be used.
     * @return A UITouch object representing the touch interaction.
     */
    fun touchDown(position: DpOffset, window: UIWindow? = null): UITouch {
        return getTargetWindow(position, window).touchDown(position)
    }

    /**
     * Simulates a mouse-down event at the specified position on the screen.
     *
     * @param position The position on the window.
     * @param window will be used to handle mouse/trackpad click; otherwise,
     * the window hosting the view will be used.
     * @return A UITouch object representing the mouse/trackpad interaction.
     */
    fun mouseDown(position: DpOffset, window: UIWindow? = null): UITouch {
        return getTargetWindow(position, window).mouseDown(position)
    }

    private fun getTargetWindow(position: DpOffset, window: UIWindow? = null): UIWindow {
        return window ?: appDelegate.window()!!
            .windowScene!!
            .windows
            .findLast {
                it as UIWindow
                it.hitTest(position.toCGPoint(), it.getTouchesEvent()) != null
            } as UIWindow
    }

    /**
     * Simulates a tap gesture at the specified position on the screen.
     *
     * @param position The position on the root hosting controller.
     */
    fun tap(position: DpOffset) {
        return touchDown(position).up()
    }

    /**
     * Simulates a click gesture at the specified position on the screen.
     *
     * @param position The position on the root hosting controller.
     */
    fun click(position: DpOffset) {
        return mouseDown(position).up()
    }

    /**
     * Simulates a tap gesture for a given AccessibilityTestNode.
     */
    fun AccessibilityTestNode.tap() {
        val frame = frame ?: error("Internal error. Frame is missing.")
        return tap(frame.center())
    }

    /**
     * Simulates a trackpad click gesture for a given AccessibilityTestNode.
     */
    fun AccessibilityTestNode.click() {
        val frame = frame ?: error("Internal error. Frame is missing.")
        return click(frame.center())
    }

    fun AccessibilityTestNode.doubleTap() {
        val frame = frame ?: error("Internal error. Frame is missing.")
        tap(frame.center())
        delay(50)
        return tap(frame.center())
    }

    /**
     * Simulates a touch-down event at the center of a given AccessibilityTestNode.
     */
    fun AccessibilityTestNode.touchDown(useNodeWindow: Boolean = false): UITouch {
        val frame = frame ?: error("Internal error. Frame is missing.")
        val window = (element as? UIView)?.window?.takeIf { useNodeWindow }
        return touchDown(frame.center(), window)
    }

    /**
     * Simulates a drag gesture on the screen, moving the touch from its current location to a specified position
     * over a given duration.
     *
     * @param location The target position of the drag in DpOffset.
     * @param duration The duration of the drag gesture, defaulting to 0.5 seconds.
     * @return The same UITouch instance after completing the drag gesture.
     */
    private fun UITouch.dragTo(location: DpOffset, duration: Duration = 0.5.seconds): UITouch {
        val startLocation = locationInView(null).toDpOffset()

        val startTime = TimeSource.Monotonic.markNow()
        while (TimeSource.Monotonic.markNow() <= startTime + duration) {
            val progress = ((TimeSource.Monotonic.markNow() - startTime) / duration).coerceIn(0.0, 1.0)
            val touchLocation = lerp(startLocation, location, progress.toFloat())

            this.moveToLocationOnWindow(touchLocation)
            NSRunLoop.currentRunLoop().runUntilDate(NSDate.dateWithTimeIntervalSinceNow(1.0 / 60))
        }
        this.moveToLocationOnWindow(location)
        return this
    }

    /**
     * Simulates a drag gesture on the screen, moving the touch from its current location by a specified offset
     * over a given duration.
     *
     * @param offset The offset by which the touch is moved, specified as a DpOffset.
     * @param duration The duration of the drag gesture, defaulting to 0.5 seconds.
     * @return The same UITouch instance after completing the drag gesture.
     */
    fun UITouch.dragBy(offset: DpOffset, duration: Duration = 0.5.seconds): UITouch {
        return dragTo(locationInView(null).toDpOffset() + offset, duration)
    }

    /**
     * Simulates a drag gesture on the screen, moving the touch from its current location by specified x and y offsets
     * over a given duration.
     *
     * @param dx The horizontal offset by which the touch is moved, specified as a Dp. Defaults to 0.dp.
     * @param dy The vertical offset by which the touch is moved, specified as a Dp. Defaults to 0.dp.
     * @param duration The duration of the drag gesture, specified as a Duration. Defaults to 0.5 seconds.
     * @return The same UITouch instance after completing the drag gesture.
     */
    fun UITouch.dragBy(dx: Dp = 0.dp, dy: Dp = 0.dp, duration: Duration = 0.5.seconds): UITouch {
        return dragBy(DpOffset(dx, dy), duration)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class MockAppDelegate: NSObject(), UIApplicationDelegateProtocol {
    private var _window: UIWindow? = UIWindow(frame = UIScreen.mainScreen.bounds)
    override fun window(): UIWindow? = _window

    private var supportedInterfaceOrientations: UIInterfaceOrientationMask = UIInterfaceOrientationMaskAll

    fun setUpWindow(viewController: UIViewController) {
        UIApplication.sharedApplication().setDelegate(this)

        val scene = UIApplication.sharedApplication().connectedScenes.first() as? UIWindowScene
            ?: error("No window scene found")
        val allWindows = scene.windows - _window

        _window?.backgroundColor = UIColor.systemBackgroundColor
        _window?.windowScene = scene

        _window?.rootViewController = viewController
        _window?.makeKeyAndVisible()

        allWindows.forEach {
            (it as UIWindow).setHidden(true)
        }
    }

    fun cleanUp() {
        val scene = UIApplication.sharedApplication().connectedScenes.first() as? UIWindowScene
        val allWindows = scene?.windows ?: emptyList<UIWindow>()

        UIApplication.sharedApplication().setDelegate(null)
        val window = UIWindow(frame = UIScreen.mainScreen.bounds)
        window.rootViewController = UIViewController()
        window.makeKeyAndVisible()
        window.windowScene = scene
        dispatch_async(dispatch_get_main_queue()) {
            window.windowScene = null
            window.resignKeyWindow()
        }

        _window?.resetTouches()
        _window?.resignKeyWindow()
        _window?.windowScene = null
        _window?.rootViewController = UIViewController()
        _window = null

        allWindows.forEach {
            (it as UIWindow).setHidden(true)
        }
    }

    /**
     * Applies the requested interface orientation if it differs from the current one.
     *
     * @return true if a rotation was requested, false if no change was necessary.
     */
    fun requestInterfaceOrientationChangeIfNeeded(
        requestedInterfaceOrientation: UIInterfaceOrientation
    ): Boolean {
        val currentInterfaceOrientation = window?.windowScene?.interfaceOrientation

        if (currentInterfaceOrientation == requestedInterfaceOrientation) return false

        supportedInterfaceOrientations = requestedInterfaceOrientation.toMask()

        CMPUIWindowSceneUtils.requestOrientationChangeForWindow(
            window,
            requestedInterfaceOrientation
        ) { error ->
            println("Failed to update interface orientation: ${error?.localizedDescription})")
        }

        return true
    }

    override fun application(
        application: UIApplication,
        supportedInterfaceOrientationsForWindow: UIWindow?
    ): UIInterfaceOrientationMask {
        return supportedInterfaceOrientations
    }
}

private fun UIInterfaceOrientation.toMask(): UIInterfaceOrientationMask = when (this) {
    UIInterfaceOrientationPortrait -> UIInterfaceOrientationMaskPortrait
    UIInterfaceOrientationPortraitUpsideDown -> UIInterfaceOrientationMaskPortraitUpsideDown
    UIInterfaceOrientationLandscapeLeft -> UIInterfaceOrientationMaskLandscapeLeft
    UIInterfaceOrientationLandscapeRight -> UIInterfaceOrientationMaskLandscapeRight
    else -> UIInterfaceOrientationMaskAll
}

internal fun MockAppDelegate.findLayersViewController(): ComposeLayersViewController {
    fun UIView.layersViewController(): ComposeLayersViewController? {
        (nextResponder as? ComposeLayersViewController)?.let {
            return it
        }
        for (subview in subviews) {
            subview as UIView
            subview.layersViewController()?.let {
                return it
            }
        }
        return null
    }
    val controller = window?.layersViewController()

    assertNotNull(controller, "${ComposeLayersViewController::class} not found in scene")
    return controller
}

internal fun UIKitInstrumentedTest.findFocusedUITextInput(): UITextInputProtocol? {
    val windowScene = viewController.view.window?.windowScene ?: return null

    fun findFirstResponder(view: UIView): UIView? {
        if (view.isFirstResponder) {
            return view
        }
        view.subviews.forEach { view ->
            findFirstResponder(view as UIView)?.let { return it }
        }
        return null
    }

    return windowScene.windows.reversed().filter {
        it as UIWindow
        it.isKeyWindow() && !it.isHidden()
    }.firstNotNullOfOrNull {
        findFirstResponder(view = it as UIView)
    } as? UITextInputProtocol
}

internal fun ComposeHostingViewController.waitForIdle() {
    UIKitInstrumentedTest.waitUntil { !this.hasInvalidations() }
}

internal fun ComposeHostingView.waitForIdle() {
    UIKitInstrumentedTest.waitUntil { !this.hasInvalidations() }
}

@OptIn(ExperimentalForeignApi::class)
internal fun UIKitInstrumentedTest.captureScreenshot(): UIImage? {
    val scale = UIScreen.mainScreen.scale
    val size = UIScreen.mainScreen.bounds.useContents { CGSizeMake(size.width, size.height) }

    UIGraphicsBeginImageContextWithOptions(size, false, scale)
    UIGraphicsGetCurrentContext() ?: return null

    val scene = appDelegate.window()?.windowScene ?: return null
    scene.windows.mapNotNull { it as? UIWindow }
        .filter { !it.hidden && it.alpha > 0.0 }
        .sortedBy { it.windowLevel }
        .forEach { window ->
            window.drawViewHierarchyInRect(window.bounds, afterScreenUpdates = true)
        }

    val screenshot = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    return screenshot
}