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

package androidx.compose.ui.integrations

import androidx.compose.ui.backhandler.UIKitBackGestureDispatcher
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.scene.ComposeSceneMediator
import androidx.compose.ui.scene.PlatformLayersComposeScene
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.center
import androidx.compose.ui.viewinterop.UIKitInteropAction
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import androidx.compose.ui.window.MetalRedrawer
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.CAMetalLayer

class ComposeSceneMediatorTest {
    @Test
    fun testDisposedMediatorShouldNotCrash() = runBlocking {
        val mediator = makeMediator()
        mediator.setContent {}
        mediator.dispose()

        mediator.density = Density(2f)
        mediator.layoutDirection = LayoutDirection.Rtl
        mediator.compositionLocalContext = null
        mediator.interactionBounds = IntRect.Companion.Zero
        mediator.isAccessibilityEnabled = true
        mediator.prepareAndGetSizeTransitionAnimation().invoke(10.milliseconds)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testDisposedViewControllerTapNoCrash() = runUIKitInstrumentedTest {
        setContent {}

        hostingViewController.viewControllerDidLeaveWindowHierarchy()

        tap(screenSize.center)

        waitForIdle()
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testDisposedViewControllerResizeNoCrash() = runUIKitInstrumentedTest {
        setContent {}

        hostingViewController.viewControllerDidLeaveWindowHierarchy()

        hostingViewController.view.setFrame(CGRectMake(0.0, 0.0, 100.0, 100.0))
        hostingViewController.view.layoutIfNeeded()

        waitForIdle()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun makeMediator(): ComposeSceneMediator {
        val mediator = ComposeSceneMediator(
            onFocusBehavior = OnFocusBehavior.DoNothing,
            focusedViewsList = null,
            windowContext = PlatformWindowContext(),
            coroutineContext = Dispatchers.Main,
            redrawer = MetalRedrawer(
                metalLayer = CAMetalLayer(),
                retrieveInteropTransaction = {
                    object : UIKitInteropTransaction {
                        override val actions: List<UIKitInteropAction> = emptyList()
                        override val isInteropActive: Boolean = false
                    }
                },
                useSeparateRenderThreadWhenPossible = false,
                render = { _, _ -> }
            ),
            backGestureDispatcher = UIKitBackGestureDispatcher(
                enableBackGesture = false,
                density = Density(1f),
                getTopLeftOffsetInWindow = { IntOffset.Companion.Zero }
            ),
            composeSceneFactory = { invalidate, platformContext ->
                PlatformLayersComposeScene(
                    density = Density(1f)
                )
            },
        )
        return mediator
    }
}