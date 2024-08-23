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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeContainer
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDate
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSRunLoop
import platform.Foundation.NSValue
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import platform.UIKit.UIKeyboardAnimationCurveUserInfoKey
import platform.UIKit.UIKeyboardAnimationDurationUserInfoKey
import platform.UIKit.UIKeyboardFrameEndUserInfoKey
import platform.UIKit.UIKeyboardWillChangeFrameNotification
import platform.UIKit.UIKeyboardWillShowNotification
import platform.UIKit.UIScreen
import platform.UIKit.UIWindow
import platform.UIKit.valueWithCGRect

internal fun runUIKitInstrumentedTest(
    testBlock: UIKitInstrumentedTest.() -> Unit
) = with(UIKitInstrumentedTest(), testBlock)

@OptIn(ExperimentalForeignApi::class)
internal class UIKitInstrumentedTest {
    private val screen = UIScreen.mainScreen()
    val density = Density(density = screen.scale.toFloat())
    val window = UIWindow(frame = screen.bounds())
    val screenSize: DpSize = screen.bounds().useContents { DpSize(size.width.dp, size.height.dp) }
    var keyboardHeight: Dp = 0.dp
        private set
    lateinit var composeContainer: ComposeContainer
        private set

    private val infiniteAnimationPolicy = object : InfiniteAnimationPolicy {
        override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R {
            throw CancellationException("Infinite animations are disabled on tests")
        }
    }

    private val coroutineContext = Dispatchers.Main + infiniteAnimationPolicy

    fun setContent(
        configure: ComposeUIViewControllerConfiguration.() -> Unit = {},
        content: @Composable () -> Unit
    ) {
        var onDraw = false
        composeContainer = ComposeContainer(
            configuration = ComposeUIViewControllerConfiguration().apply {
                // Current instrumented test environment doesn't allow providing a plist.
                enforceStrictPlistSanityCheck = false
                configure()
            },
            content = {
                Layout(
                    content = content,
                    modifier = Modifier.drawWithContent {
                        onDraw = true
                    },
                    measurePolicy = { measurables, constraints ->
                        val placeable = measurables.firstOrNull()?.measure(constraints)

                        val width = placeable?.width ?: constraints.minWidth
                        val height = placeable?.height ?: constraints.minHeight

                        layout(width, height) {
                            placeable?.placeRelative(0, 0)
                        }
                    }
                )
            },
            coroutineContext = coroutineContext
        )

        window.rootViewController = composeContainer
        window.makeKeyAndVisible()
        waitUntil { onDraw }
        waitForIdle()
    }

    private fun keyboardUserInfo(height: Dp, animated: Boolean): Map<Any?, *> {
        val keyboardHeight = screenSize.height - height
        val keyboardDuration = if (animated) 0.25 else 0.0
        return mapOf(
            UIKeyboardAnimationDurationUserInfoKey to NSNumber(double = keyboardDuration),
            UIKeyboardFrameEndUserInfoKey to NSValue.valueWithCGRect(
                CGRectMake(
                    x = 0.0,
                    y = keyboardHeight.value.toDouble(),
                    width = screenSize.width.value.toDouble(),
                    height = keyboardHeight.value.toDouble()
                )
            ),
            UIKeyboardAnimationCurveUserInfoKey to NSNumber(unsignedInteger = 0uL)
        )
    }

    fun showKeyboard(height: Dp = 216.dp, animated: Boolean = true) {
        NSNotificationCenter.defaultCenter().postNotification(
            NSNotification(
                name = UIKeyboardWillShowNotification,
                `object` = null,
                userInfo = keyboardUserInfo(height, animated)
            )
        )
        keyboardHeight = height
        resizeKeyboard(height, animated)
    }

    fun resizeKeyboard(newHeight: Dp, animated: Boolean = true) {
        NSNotificationCenter.defaultCenter().postNotification(
            NSNotification(
                name = UIKeyboardWillChangeFrameNotification,
                `object` = null,
                userInfo = keyboardUserInfo(newHeight, animated)
            )
        )
        keyboardHeight = newHeight
    }

    fun hideKeyboard(animated: Boolean = true) {
        NSNotificationCenter.defaultCenter().postNotification(
            NSNotification(
                name = UIKeyboardWillChangeFrameNotification,
                `object` = null,
                userInfo = keyboardUserInfo(0.dp, animated)
            )
        )
        keyboardHeight = 0.dp
    }

    private val isIdle: Boolean
        get() {
            val hadSnapshotChanges = Snapshot.current.hasPendingChanges()
            val isApplyObserverNotificationPending = Snapshot.isApplyObserverNotificationPending
            val containerInvalidations = composeContainer.hasInvalidations()

            return !hadSnapshotChanges && !isApplyObserverNotificationPending && !containerInvalidations
        }

    fun waitForIdle(timeoutMillis: Long = 5_000) {
        waitUntil(
            conditionDescription = "waitForIdle: timeout ${timeoutMillis}ms reached.",
            timeoutMillis = timeoutMillis
        ) { isIdle }
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
}
