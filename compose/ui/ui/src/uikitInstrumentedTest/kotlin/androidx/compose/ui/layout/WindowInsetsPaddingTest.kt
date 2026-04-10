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

package androidx.compose.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemGesturesPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.DpRectZero
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.ComposeUIView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIColor
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown

class WindowInsetsPaddingTest {
    @Test
    fun testComposableNotRecomposedOnWindowInsetsImeChange() = runUIKitInstrumentedTest {
        var compositionCount = 0

        setContent {
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Spacer(modifier = Modifier.weight(1f))
                TextField(
                    "",
                    {},
                    Modifier.focusRequester(focusRequester)
                )
                compositionCount++
            }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        assertEquals(1, compositionCount)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testDisplayCutoutPadding_InterfaceOrientationLandscapeLeft() = runUIKitInstrumentedTest(
        ignoreIf = UIKitInstrumentedTest.isRunningOnIPad,
        ignoreNotes = "Run for iPhone only"
    ) {
        var boxRect = DpRectZero()

        setContent(interfaceOrientation = UIInterfaceOrientationLandscapeLeft) {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .background(Color.Red)
                    .onGloballyPositioned({ boxRect = it.boundsInWindow().toDpRect(density) })
            ) {
                Text("TEXT")
            }
        }

        assertEquals(
            DpRect(
                DpOffset.Zero,
                DpSize(
                    screenSize.width - viewController.view.safeAreaInsets.useContents { right }.dp,
                    screenSize.height
                )
            ),
            boxRect
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testDisplayCutoutPadding_InterfaceOrientationLandscapeRight() = runUIKitInstrumentedTest(
        ignoreIf = UIKitInstrumentedTest.isRunningOnIPad,
        ignoreNotes = "Run for iPhone only"
    ) {
        var boxRect = DpRectZero()

        setContent(interfaceOrientation = UIInterfaceOrientationLandscapeRight) {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .background(Color.Red)
                    .onGloballyPositioned({ boxRect = it.boundsInWindow().toDpRect(density) })
            ) {
                Text("TEXT")
            }
        }

        assertEquals(
            DpRect(
                left = viewController.view.safeAreaInsets.useContents { left }.dp,
                top = 0.dp,
                right = screenSize.width,
                bottom = screenSize.height
            ),
            boxRect
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testDisplayCutoutPadding_iPad() = runUIKitInstrumentedTest(
        ignoreIf = UIKitInstrumentedTest.iPadOrientationChangesNotSupported,
        ignoreNotes = "Run for iPad only",
        params = listOf(
            UIInterfaceOrientationPortrait,
            UIInterfaceOrientationLandscapeRight,
            UIInterfaceOrientationLandscapeLeft,
            UIInterfaceOrientationPortraitUpsideDown
        ),
    ) { interfaceOrientation ->
        var boxRect = DpRectZero()

        setContent(interfaceOrientation = interfaceOrientation) {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .background(Color.Red)
                    .onGloballyPositioned({ boxRect = it.boundsInWindow().toDpRect(density) })
            ) {
                Text("TEXT")
            }
        }

        assertEquals(
            DpRect(
                left = 0.dp,
                top = viewController.view.safeAreaInsets.useContents { top }.dp,
                right = screenSize.width,
                bottom = screenSize.height
            ),
            boxRect
        )
    }

    @Test
    fun testContentNotRecomposedWhenContainerRecomposed() = runUIKitInstrumentedTest {
        var forceRecomposition by mutableStateOf(0)
        val recomposed = mutableStateOf(false)

        setContent {
            Box(Modifier.fillMaxSize()) {
                forceRecomposition
                InnerContent(Modifier.systemGesturesPadding(), recomposed)
            }
        }

        recomposed.value = false
        forceRecomposition++
        waitForIdle()

        assertEquals(false, recomposed.value)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testWindowInsetsPaddingAppliedToNonFullscreenContent() = runUIKitInstrumentedTest {
        var innerBoxRect = DpRectZero()
        var outerBoxRect = DpRectZero()

        setContent {
            Box(modifier = Modifier.background(Color.Red).fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Blue)
                        .size(200.dp, 200.dp)
                        .onGloballyPositioned {
                            outerBoxRect = it.boundsInWindow().toDpRect(density)
                        },
                ) {
                    Box(modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .background(Color.Green)
                        .fillMaxSize()
                        .onGloballyPositioned {
                            innerBoxRect = it.boundsInWindow().toDpRect(density)
                        }
                    )
                }
            }
        }

        // WindowInsets.statusBars should only represent the insets at the top in portrait orientation
        val topSafeAreaInsetsDp = viewController.view.safeAreaInsets.useContents { top }.dp

        assertEquals(
            DpRect(
                left = outerBoxRect.left,
                top = outerBoxRect.top + topSafeAreaInsetsDp,
                right = outerBoxRect.right,
                bottom = outerBoxRect.bottom
            ),
            innerBoxRect
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testWindowInsetsPaddingAppliedToNonFullscreenComposeUIViewContent() = runUIKitInstrumentedTest {
        var innerBoxRect = DpRectZero()
        var outerBoxRect = DpRectZero()

        setContent {
            Box(modifier = Modifier.background(Color.Red).fillMaxSize()) {
                UIKitView(
                    factory = {
                        ComposeUIView(
                            configure = {
                                enforceStrictPlistSanityCheck = false
                                opaque = false
                            }
                        ) {
                            Box(modifier = Modifier
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .background(Color.Green)
                                .fillMaxSize()
                                .onGloballyPositioned {
                                    innerBoxRect = it.boundsInWindow().toDpRect(density)
                                }
                            )
                        }.apply {
                            backgroundColor = UIColor.blueColor
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(200.dp, 200.dp)
                        .onGloballyPositioned {
                            outerBoxRect = it.boundsInWindow().toDpRect(density)
                        }
                )
            }
        }

        // WindowInsets.statusBars should only represent the insets at the top in portrait orientation
        val topSafeAreaInsetsDp = viewController.view.safeAreaInsets.useContents { top }.dp

        assertEquals(
            DpRect(
                left = outerBoxRect.left,
                top = outerBoxRect.top + topSafeAreaInsetsDp,
                right = outerBoxRect.right,
                bottom = outerBoxRect.bottom
            ),
            innerBoxRect
        )
    }
}

@Composable
private fun InnerContent(modifier: Modifier, state: MutableState<Boolean>) {
    Box(modifier) { state.value = true }
}