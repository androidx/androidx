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

package androidx.compose.ui.interop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.findNodeWithTagOrNull
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.center
import androidx.compose.ui.test.utils.rightCenter
import androidx.compose.ui.test.utils.up
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.UIKit.UINavigationController
import platform.UIKit.UIViewController

@OptIn(ExperimentalForeignApi::class)
internal abstract class UIKitNavigationSwipeBackTest(
    private val runUIKitInstrumentedTest: (UIKitInstrumentedTest.() -> Unit) -> Unit
) {
    @Test
    fun testSwipeRightOnPagerDoesNotPopController() = runUIKitInstrumentedTest {
        val initialPage = 0
        val currentPage = mutableIntStateOf(initialPage)

        setNavigationControllerContent {
            TestContent(currentPage = currentPage)
        }

        findNodeWithTag("pager").swipeRight()

        waitForIdle()

        assertEquals(2, navigationController.viewControllers.size)
        assertNotNull(findNodeWithTagOrNull("pager"))
        assertEquals(initialPage, currentPage.value)
    }

    @Test
    fun testSwipeLeftOnPagerChangesPage() = runUIKitInstrumentedTest {
        val initialPage = 0
        val currentPage = mutableIntStateOf(initialPage)

        setNavigationControllerContent {
            TestContent(currentPage = currentPage)
        }

        findNodeWithTag("pager").swipeLeft()

        waitForIdle()

        assertEquals(initialPage + 1, currentPage.value)
        assertEquals(2, navigationController.viewControllers.size)
    }

    @Test
    fun testSwipeLeftOutsidePagerNoChanges() = runUIKitInstrumentedTest {
        val initialPage = 1
        val currentPage = mutableIntStateOf(initialPage)

        setNavigationControllerContent {
            TestContent(currentPage = currentPage)
        }

        findNodeWithTag("outsideBox").swipeLeft()

        assertEquals(initialPage, currentPage.value)
        assertEquals(2, navigationController.viewControllers.size)
    }

    @Test
    fun testSwipeRightFromEdgePopsController() = runUIKitInstrumentedTest {
        val viewControllerHostingCompose = setNavigationControllerContent {
            TestContent(currentPage = mutableIntStateOf(1))
        }

        swipeRightFromEdge().up()

        waitForPopped(viewControllerHostingCompose)
    }

    @Test
    fun testSwipeRightFromCenterOutsidePagerDoesNotPopController() = runUIKitInstrumentedTest(
        ignoreIf = available(OS.Ios to OSVersion(major = 26)),
        ignoreNotes = "Full-width swipe gesture is not recognized on iOS < 26"
    ){
        val initialPage = 1
        val currentPage = mutableIntStateOf(initialPage)

        setNavigationControllerContent {
            TestContent(currentPage = currentPage)
        }

        findNodeWithTag("outsideBox").swipe(
            fromPosition = { center() },
            toPosition = { rightCenter() },
        )

        waitForIdle()

        assertNotNull(findNodeWithTagOrNull("pager"))
        assertNotNull(findNodeWithTagOrNull("outsideBox"))
        assertEquals(2, navigationController.viewControllers.size)
    }

    @Test
    fun testSwipeRightOutsidePagerPopsControllerOnIos26() = runUIKitInstrumentedTest(
        ignoreIf = !available(OS.Ios to OSVersion(major = 26)),
        ignoreNotes = "Full-width swipe gesture is not recognized on iOS < 26"
    ) {
        val initialPage = 1
        val currentPage = mutableIntStateOf(initialPage)

        val viewControllerHostingCompose = setNavigationControllerContent {
            TestContent(currentPage = currentPage)
        }

        findNodeWithTag("outsideBox").swipeRight()

        waitForPopped(viewControllerHostingCompose)
    }

    @Test
    fun testSwipeRightFromEdgeOutsidePagerPopsControllerOnIos26() = runUIKitInstrumentedTest(
        ignoreIf = !available(OS.Ios to OSVersion(major = 26)),
        ignoreNotes = "Full-width swipe gesture is not recognized on iOS < 26"
    ) {
        val initialPage = 1
        val currentPage = mutableIntStateOf(initialPage)

        val viewControllerHostingCompose = setNavigationControllerContent {
            TestContent(currentPage = currentPage)
        }

        swipeRightFromEdge().up()

        waitForPopped(viewControllerHostingCompose)
    }

    @Test
    fun testSwipeRightFromCenterOutsidePagerPopsControllerOnIos26() = runUIKitInstrumentedTest(
        ignoreIf = !available(OS.Ios to OSVersion(major = 26)),
        ignoreNotes = "Full-width swipe gesture is not recognized on iOS < 26",
    ) {
        val initialPage = 1
        val currentPage = mutableIntStateOf(initialPage)

        val viewControllerHostingCompose = setNavigationControllerContent {
            TestContent(currentPage = currentPage)
        }

        findNodeWithTag("outsideBox").swipe(
            fromPosition = { center() },
            toPosition = { rightCenter() },
        )

        waitForPopped(viewControllerHostingCompose)
    }

    private val UIKitInstrumentedTest.navigationController: UINavigationController get() {
        return assertNotNull(appDelegate.window?.rootViewController as? UINavigationController)
    }

    private fun UIKitInstrumentedTest.setNavigationControllerContent(
        content: @Composable () -> Unit = {}
    ): UIViewController {
        val viewControllerHostingCompose = createViewControllerHostingCompose(content = content)

        setupWindow {
            UINavigationController().also {
                it.setViewControllers(listOf(UIViewController(), viewControllerHostingCompose), false)
            }
        }

        waitUntil {
            viewControllerHostingCompose.view.window != null
        }

        return viewControllerHostingCompose
    }

    private fun UIKitInstrumentedTest.waitForPopped(
        viewController: UIViewController
    ) {
        waitUntil("Waiting for view controller to be popped and detached") {
            navigationController.viewControllers.size == 1 &&
                viewController.view.window == null
        }
    }

    private fun runUIKitInstrumentedTest(
        ignoreIf: Boolean,
        ignoreNotes: String,
        testBlock: UIKitInstrumentedTest.() -> Unit
    ) = if (ignoreIf) {
        println("Debug: Ignored test: $ignoreNotes")
    } else {
        runUIKitInstrumentedTest(testBlock)
    }
}

@Composable
private fun TestContent(
    currentPage: MutableIntState
) {
    val pagerColors = listOf(Color.Red, Color.Green, Color.Blue)
    val pagerState = rememberPagerState(initialPage = currentPage.value) { 3 }

    LaunchedEffect(pagerState.currentPage) {
        currentPage.value = pagerState.currentPage
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("pager")
        ) { page ->
            Box(modifier = Modifier
                .fillMaxSize()
                .background(pagerColors[page])
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .testTag("outsideBox")
        )
    }
}

internal class UIKitNavigationSwipeBackInHostingViewTest : UIKitNavigationSwipeBackTest(
    runUIKitInstrumentedTest = { runUIKitInstrumentedTest(useHostingView = true, it) }
)

internal class UIKitNavigationSwipeBackInHostingViewControllerTest : UIKitNavigationSwipeBackTest(
   runUIKitInstrumentedTest = { runUIKitInstrumentedTest(useHostingView = false, it) }
)
