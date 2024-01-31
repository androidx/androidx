/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import android.content.ComponentCallbacks2
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalMaterial3Api::class)
class ModalBottomSheetTest(private val edgeToEdgeWrapper: EdgeToEdgeWrapper) {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val sheetHeight = 256.dp
    private val dragHandleSize = 44.dp

    private val sheetTag = "sheetContentTag"
    private val dragHandleTag = "dragHandleTag"
    private val BackTestTag = "Back"

    @Test
    fun modalBottomSheet_isDismissedOnTapOutside() {
        var showBottomSheet by mutableStateOf(true)
        val sheetState = SheetState(skipPartiallyExpanded = false, density = rule.density)

        rule.setContent {
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                    windowInsets = windowInsets
                ) {
                    Box(
                        Modifier
                            .size(sheetHeight)
                            .testTag(sheetTag)
                    )
                }
            }
        }

        assertThat(sheetState.isVisible).isTrue()

        // Tap Scrim
        val outsideY = with(rule.density) {
            rule.onAllNodes(isPopup()).onFirst().getUnclippedBoundsInRoot().height.roundToPx() / 4
        }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(0, outsideY)
        rule.waitForIdle()

        // Bottom sheet should not exist
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_isDismissedOnSwipeDown() {
        var showBottomSheet by mutableStateOf(true)
        val sheetState = SheetState(skipPartiallyExpanded = false, density = rule.density)

        rule.setContent {
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                    windowInsets = windowInsets
                ) {
                    Box(
                        Modifier
                            .size(sheetHeight)
                            .testTag(sheetTag)
                    )
                }
            }
        }

        assertThat(sheetState.isVisible).isTrue()

        // Swipe Down
        rule.onNodeWithTag(sheetTag).performTouchInput {
            swipeDown()
        }
        rule.waitForIdle()

        // Bottom sheet should not exist
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_fillsScreenWidth() {
        var boxWidth = 0
        var screenWidth by mutableStateOf(0)

        rule.setContent {
            val context = LocalContext.current
            screenWidth = context.resources.displayMetrics.widthPixels
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            ModalBottomSheet(
                onDismissRequest = {},
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(sheetHeight)
                        .onSizeChanged { boxWidth = it.width }
                )
            }
        }
        assertThat(boxWidth).isEqualTo(screenWidth)
    }

    @Test
    fun modalBottomSheet_wideScreen_fixedMaxWidth_sheetRespectsMaxWidthAndIsCentered() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val latch = CountDownLatch(1)

        rule.activity.application.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onConfigurationChanged(p0: Configuration) {
                latch.countDown()
            }

            override fun onLowMemory() {
                // NO-OP
            }

            override fun onTrimMemory(p0: Int) {
                // NO-OP
            }
        })

        try {
            latch.await(1500, TimeUnit.MILLISECONDS)
            rule.setContent {
                val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                    WindowInsets(0) else BottomSheetDefaults.windowInsets
                ModalBottomSheet(
                    onDismissRequest = {},
                    windowInsets = windowInsets
                ) {
                    Box(
                        Modifier
                            .testTag(sheetTag)
                            .fillMaxHeight(0.4f)
                    )
                }
            }

            val simulatedRootWidth = rule.onNode(isPopup()).getUnclippedBoundsInRoot().width
            val maxSheetWidth = 640.dp
            val expectedSheetWidth = maxSheetWidth.coerceAtMost(simulatedRootWidth)
            // Our sheet should be max 640 dp but fill the width if the container is less wide
            val expectedSheetLeft = if (simulatedRootWidth <= expectedSheetWidth) {
                0.dp
            } else {
                (simulatedRootWidth - expectedSheetWidth) / 2
            }

            rule.onNodeWithTag(sheetTag)
                .onParent()
                .assertLeftPositionInRootIsEqualTo(
                    expectedLeft = expectedSheetLeft
                )
                .assertWidthIsEqualTo(expectedSheetWidth)
        } catch (e: InterruptedException) {
            fail("Unable to verify sheet width in landscape orientation")
        } finally {
            rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    @Test
    fun modalBottomSheet_wideScreen_filledWidth_sheetFillsEntireWidth() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val latch = CountDownLatch(1)

        rule.activity.application.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onConfigurationChanged(p0: Configuration) {
                latch.countDown()
            }

            override fun onLowMemory() {
                // NO-OP
            }

            override fun onTrimMemory(p0: Int) {
                // NO-OP
            }
        })

        try {
            latch.await(1500, TimeUnit.MILLISECONDS)
            var screenWidthPx by mutableStateOf(0)
            rule.setContent {
                val context = LocalContext.current
                screenWidthPx = context.resources.displayMetrics.widthPixels
                val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                    WindowInsets(0) else BottomSheetDefaults.windowInsets
                ModalBottomSheet(
                    onDismissRequest = {},
                    sheetMaxWidth = Dp.Unspecified,
                    windowInsets = windowInsets
                ) {
                    Box(
                        Modifier
                            .testTag(sheetTag)
                            .fillMaxHeight(0.4f)
                    )
                }
            }

            val sheet = rule.onNodeWithTag(sheetTag).onParent().getUnclippedBoundsInRoot()
            val sheetWidthPx = with(rule.density) { sheet.width.roundToPx() }
            assertThat(sheetWidthPx).isEqualTo(screenWidthPx)
        } catch (e: InterruptedException) {
            fail("Unable to verify sheet width in landscape orientation")
        } finally {
            rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    @Test
    fun modalBottomSheet_defaultStateForSmallContentIsFullExpanded() {
        lateinit var sheetState: SheetState
        var height by mutableStateOf(0.dp)

        rule.setContent {
            val config = LocalContext.current.resources.configuration
            height = config.screenHeightDp.dp
            sheetState = rememberModalBottomSheetState()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = null,
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .testTag(sheetTag)
                        .height(sheetHeight)
                )
            }
        }

        height = rule.onNode(isPopup()).getUnclippedBoundsInRoot().height
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        rule.onNodeWithTag(sheetTag).assertTopPositionInRootIsEqualTo(height - sheetHeight)
    }

    @Test
    fun modalBottomSheet_defaultStateForLargeContentIsHalfExpanded() {
        lateinit var sheetState: SheetState
        var screenHeightPx by mutableStateOf(0f)

        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        // Deliberately use fraction != 1f
                        .fillMaxSize(0.6f)
                        .testTag(sheetTag)
                )
            }
        }

        screenHeightPx = with(rule.density) {
            rule.onNode(isPopup()).getUnclippedBoundsInRoot().height.toPx()
        }
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        assertThat(sheetState.requireOffset())
            .isWithin(1f)
            .of(screenHeightPx / 2f)
    }

    @Test
    fun modalBottomSheet_shortSheet_isDismissedOnBackPress() {
        var showBottomSheet by mutableStateOf(true)
        val sheetState = SheetState(skipPartiallyExpanded = true, density = rule.density)

        rule.setContent {
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets
            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                    windowInsets = windowInsets
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight(0.4f)
                            .testTag(sheetTag)
                    ) {
                        Button(
                            onClick = { dispatcher.onBackPressed() },
                            modifier = Modifier.testTag(BackTestTag),
                            content = { Text("Content") },
                        )
                    }
                }
            }
        }

        assertThat(sheetState.isVisible).isTrue()

        rule.onNodeWithTag(BackTestTag).performClick()

        rule.onNodeWithTag(BackTestTag).assertDoesNotExist()

        // Popup should not exist
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_tallSheet_isDismissedOnBackPress() {
        var showBottomSheet by mutableStateOf(true)
        val sheetState = SheetState(skipPartiallyExpanded = false, density = rule.density)

        rule.setContent {
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets
            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                    windowInsets = windowInsets
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight(0.6f)
                            .testTag(sheetTag)
                    ) {
                        Button(
                            onClick = { dispatcher.onBackPressed() },
                            modifier = Modifier.testTag(BackTestTag),
                            content = { Text("Content") },
                        )
                    }
                }
            }
        }
        assertThat(sheetState.isVisible).isTrue()

        rule.onNodeWithTag(BackTestTag).performClick()
        rule.onNodeWithTag(BackTestTag).assertDoesNotExist()

        // Popup should not exist
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_shortSheet_sizeChanges_snapsToNewTarget() {
        lateinit var state: SheetState
        var size by mutableStateOf(56.dp)
        var screenHeight by mutableStateOf(0.dp)
        val expectedExpandedAnchor by derivedStateOf {
            with(rule.density) {
                (screenHeight - size).toPx()
            }
        }

        rule.setContent {
            val context = LocalContext.current
            screenHeight = context.resources.configuration.screenHeightDp.dp
            state = rememberModalBottomSheetState()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
                dragHandle = null,
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        .height(size)
                        .fillMaxWidth()
                )
            }
        }
        screenHeight = rule.onNode(isPopup()).getUnclippedBoundsInRoot().height
        assertThat(state.requireOffset()).isWithin(1f).of(expectedExpandedAnchor)

        size = 100.dp
        rule.waitForIdle()
        assertThat(state.requireOffset()).isWithin(1f).of(expectedExpandedAnchor)

        size = 30.dp
        rule.waitForIdle()
        assertThat(state.requireOffset()).isWithin(1f).of(expectedExpandedAnchor)
    }

    @Test
    fun modalBottomSheet_sheetMaxWidth_sizeChanges_snapsToNewTarget() {
        lateinit var sheetMaxWidth: MutableState<Dp>
        var screenWidth by mutableStateOf(0.dp)
        rule.setContent {
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets
            sheetMaxWidth = remember { mutableStateOf(0.dp) }
            val context = LocalContext.current
            val density = LocalDensity.current
            screenWidth = with(density) { context.resources.displayMetrics.widthPixels.toDp() }
            ModalBottomSheet(
                onDismissRequest = {},
                sheetMaxWidth = sheetMaxWidth.value,
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .testTag(sheetTag)
                )
            }
        }

        for (dp in listOf(0.dp, 200.dp, 400.dp)) {
            sheetMaxWidth.value = dp
            val sheetWidth = rule.onNodeWithTag(sheetTag).getUnclippedBoundsInRoot().width
            val expectedSheetWidth = minOf(sheetMaxWidth.value, screenWidth)
            assertThat(sheetWidth).isEqualTo(expectedSheetWidth)
        }
    }

    @Test
    fun modalBottomSheet_emptySheet_expandDoesNotAnimate() {
        lateinit var state: SheetState
        lateinit var scope: CoroutineScope
        rule.setContent {
            state = rememberModalBottomSheetState()
            scope = rememberCoroutineScope()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
                dragHandle = null,
                windowInsets = windowInsets
            ) {}
        }
        assertThat(state.anchoredDraggableState.currentValue).isEqualTo(SheetValue.Hidden)
        val hiddenOffset = state.requireOffset()
        scope.launch { state.show() }
        rule.waitForIdle()

        assertThat(state.anchoredDraggableState.currentValue).isEqualTo(SheetValue.Expanded)
        val expandedOffset = state.requireOffset()

        assertThat(hiddenOffset).isEqualTo(expandedOffset)
    }

    @Test
    fun modalBottomSheet_anchorsChange_retainsCurrentValue() {
        lateinit var state: SheetState
        var amountOfItems by mutableStateOf(0)
        lateinit var scope: CoroutineScope
        rule.setContent {
            state = rememberModalBottomSheetState()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
                dragHandle = null,
                windowInsets = windowInsets
            ) {
                scope = rememberCoroutineScope()
                LazyColumn {
                    items(amountOfItems) {
                        ListItem(headlineContent = { Text("$it") })
                    }
                }
            }
        }

        assertThat(state.currentValue).isEqualTo(SheetValue.Hidden)

        amountOfItems = 50
        rule.waitForIdle()
        scope.launch {
            state.show()
        }
        // The anchors should now be {Hidden, PartiallyExpanded, Expanded}

        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        amountOfItems = 100 // The anchors should now be {Hidden, PartiallyExpanded, Expanded}

        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded) // We should
        // retain the current value if possible
        assertTrue(state.anchoredDraggableState.anchors.hasAnchorFor(SheetValue.Hidden))
        assertTrue(state.anchoredDraggableState.anchors.hasAnchorFor(SheetValue.PartiallyExpanded))
        assertTrue(state.anchoredDraggableState.anchors.hasAnchorFor(SheetValue.Expanded))

        amountOfItems = 0 // When the sheet height is 0, we should only have a hidden anchor
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.Hidden)
        assertTrue(state.anchoredDraggableState.anchors.hasAnchorFor(SheetValue.Hidden))
        assertFalse(state.anchoredDraggableState.anchors.hasAnchorFor(SheetValue.PartiallyExpanded))
        assertFalse(state.anchoredDraggableState.anchors.hasAnchorFor(SheetValue.Expanded))
    }

    @Test
    fun modalBottomSheet_nestedScroll_consumesWithinBounds_scrollsOutsideBounds() {
        lateinit var sheetState: SheetState
        lateinit var scrollState: ScrollState
        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState
            ) {
                scrollState = rememberScrollState()
                Column(
                    Modifier
                        .verticalScroll(scrollState)
                        .testTag(sheetTag)
                ) {
                    repeat(100) {
                        Text(it.toString(), Modifier.requiredHeight(50.dp))
                    }
                }
            }
        }

        rule.waitForIdle()

        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        rule.onNodeWithTag(sheetTag)
            .performTouchInput {
                swipeUp(startY = bottom, endY = bottom / 2)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)

        rule.onNodeWithTag(sheetTag)
            .performTouchInput {
                swipeUp(startY = bottom, endY = top)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isGreaterThan(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)

        rule.onNodeWithTag(sheetTag)
            .performTouchInput {
                swipeDown(startY = top, endY = bottom)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)

        rule.onNodeWithTag(sheetTag)
            .performTouchInput {
                swipeDown(startY = top, endY = bottom / 2)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        rule.onNodeWithTag(sheetTag)
            .performTouchInput {
                swipeDown(startY = bottom / 2, endY = bottom)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
    }

    @Test
    fun modalBottomSheet_missingAnchors_findsClosest() {
        val topTag = "ModalBottomSheetLayout"
        var showShortContent by mutableStateOf(false)
        val sheetState = SheetState(skipPartiallyExpanded = false, density = rule.density)
        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            ModalBottomSheet(
                onDismissRequest = {},
                modifier = Modifier.testTag(topTag),
                sheetState = sheetState,
                windowInsets = windowInsets
            ) {
                if (showShortContent) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(topTag).performTouchInput {
            swipeDown()
            swipeDown()
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
        }

        showShortContent = true
        scope.launch { sheetState.show() } // We can't use LaunchedEffect with Swipeable in tests
        // yet, so we're invoking this outside of composition. See b/254115946.

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        }
    }

    @Test
    fun modalBottomSheet_expandBySwiping() {
        lateinit var sheetState: SheetState
        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeUp() }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        }
    }

    @Test
    fun modalBottomSheet_respectsConfirmValueChange() {
        lateinit var sheetState: SheetState
        rule.setContent {
            sheetState = rememberModalBottomSheetState(
                confirmValueChange = { newState ->
                    newState != SheetValue.Hidden
                }
            )
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = {
                    Box(
                        Modifier
                            .testTag(dragHandleTag)
                            .size(dragHandleSize)
                    )
                },
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeDown() }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true).onParent()
            .performSemanticsAction(SemanticsActions.Dismiss)

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        // Tap Scrim
        val outsideY = with(rule.density) {
            rule.onAllNodes(isPopup()).onFirst().getUnclippedBoundsInRoot().height.roundToPx() / 4
        }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(0, outsideY)
        rule.waitForIdle()
        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }
    }

    @Test
    fun modalBottomSheet_hideBySwiping_tallBottomSheet() {
        lateinit var sheetState: SheetState
        lateinit var scope: CoroutineScope
        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            scope = rememberCoroutineScope()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        scope.launch { sheetState.expand() }
        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeDown() }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
        }
    }

    @Test
    fun modalBottomSheet_hideBySwiping_skipPartiallyExpanded() {
        lateinit var sheetState: SheetState
        rule.setContent {
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(sheetHeight)
                        .testTag(sheetTag)
                )
            }
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeDown() }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
        }
    }

    @Test
    fun modalBottomSheet_hideManually_skipPartiallyExpanded(): Unit = runBlocking(
        AutoTestFrameClock()
    ) {
        lateinit var sheetState: SheetState
        rule.setContent {
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }
        assertThat(sheetState.currentValue == SheetValue.Expanded)

        sheetState.hide()

        assertThat(sheetState.currentValue == SheetValue.Hidden)
    }

    @Test
    fun modalBottomSheet_testParialExpandReturnsIllegalStateException_whenSkipPartialExpanded() {
        lateinit var scope: CoroutineScope
        val bottomSheetState = SheetState(
            skipPartiallyExpanded = true,
            density = rule.density
        )
        rule.setContent {
            scope = rememberCoroutineScope()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = bottomSheetState,
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }
        scope.launch {
            val exception =
                kotlin.runCatching { bottomSheetState.partialExpand() }.exceptionOrNull()
            assertThat(exception).isNotNull()
            assertThat(exception).isInstanceOf(IllegalStateException::class.java)
            assertThat(exception).hasMessageThat().containsMatch(
                "Attempted to animate to partial expanded when skipPartiallyExpanded was " +
                    "enabled. Set skipPartiallyExpanded to false to use this function."
            )
        }
    }

    @Test
    fun modalBottomSheet_testDismissAction_tallBottomSheet_whenPartiallyExpanded() {
        rule.setContent {
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets
            ModalBottomSheet(
                onDismissRequest = {},
                dragHandle = {
                    Box(
                        Modifier
                            .testTag(dragHandleTag)
                            .size(dragHandleSize)
                    )
                },
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss)
    }

    @Test
    fun modalBottomSheet_testExpandAction_tallBottomSheet_whenHalfExpanded() {
        lateinit var sheetState: SheetState
        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = {
                    Box(
                        Modifier
                            .testTag(dragHandleTag)
                            .size(dragHandleSize)
                    )
                },
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Expand)

        rule.runOnIdle {
            assertThat(sheetState.requireOffset()).isEqualTo(0f)
        }
    }

    @Test
    fun modalBottomSheet_testDismissAction_tallBottomSheet_whenExpanded() {
        lateinit var sheetState: SheetState
        lateinit var scope: CoroutineScope

        var screenHeightPx by mutableStateOf(0f)

        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            scope = rememberCoroutineScope()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = {
                    Box(
                        Modifier
                            .testTag(dragHandleTag)
                            .size(dragHandleSize)
                    )
                },
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }
        screenHeightPx = with(rule.density) {
            rule.onNode(isPopup()).getUnclippedBoundsInRoot().height.toPx()
        }
        scope.launch {
            sheetState.expand()
        }
        rule.waitForIdle()

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss)

        rule.runOnIdle {
            assertThat(sheetState.requireOffset()).isWithin(1f).of(screenHeightPx)
        }
    }

    @Test
    fun modalBottomSheet_testCollapseAction_tallBottomSheet_whenExpanded() {
        lateinit var sheetState: SheetState
        lateinit var scope: CoroutineScope

        var screenHeightPx by mutableStateOf(0f)

        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            scope = rememberCoroutineScope()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = {
                    Box(
                        Modifier
                            .testTag(dragHandleTag)
                            .size(dragHandleSize)
                    )
                },
                windowInsets = windowInsets
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }
        screenHeightPx = with(rule.density) {
            rule.onNode(isPopup()).getUnclippedBoundsInRoot().height.toPx()
        }
        scope.launch {
            sheetState.expand()
        }
        rule.waitForIdle()

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Collapse)

        rule.runOnIdle {
            assertThat(sheetState.requireOffset()).isWithin(1f).of(screenHeightPx / 2)
        }
    }

    @Test
    fun modalBottomSheet_shortSheet_anchorChangeHandler_previousTargetNotInAnchors_reconciles() {
        val sheetState = SheetState(skipPartiallyExpanded = false, density = rule.density)
        var hasSheetContent by mutableStateOf(false) // Start out with empty sheet content
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = null,
                windowInsets = windowInsets
            ) {
                if (hasSheetContent) {
                    Box(Modifier.fillMaxHeight(0.4f))
                }
            }
        }

        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
        assertFalse(
            sheetState.anchoredDraggableState.anchors.hasAnchorFor(SheetValue.PartiallyExpanded)
        )
        assertFalse(sheetState.anchoredDraggableState.anchors.hasAnchorFor(SheetValue.Expanded))

        scope.launch { sheetState.show() }
        rule.waitForIdle()

        assertThat(sheetState.isVisible).isTrue()
        assertThat(sheetState.currentValue).isEqualTo(sheetState.targetValue)

        hasSheetContent = true // Recompose with sheet content
        rule.waitForIdle()
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
    }

    @Test
    fun modalBottomSheet_tallSheet_anchorChangeHandler_previousTargetNotInAnchors_reconciles() {
        val sheetState = SheetState(skipPartiallyExpanded = false, density = rule.density)
        var hasSheetContent by mutableStateOf(false) // Start out with empty sheet content
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = null,
                windowInsets = windowInsets
            ) {
                if (hasSheetContent) {
                    Box(Modifier.fillMaxHeight(0.6f))
                }
            }
        }

        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
        assertFalse(
            sheetState.anchoredDraggableState.anchors.hasAnchorFor(SheetValue.PartiallyExpanded)
        )
        assertFalse(sheetState.anchoredDraggableState.anchors.hasAnchorFor(SheetValue.Expanded))

        scope.launch { sheetState.show() }
        rule.waitForIdle()

        assertThat(sheetState.isVisible).isTrue()
        assertThat(sheetState.currentValue).isEqualTo(sheetState.targetValue)

        hasSheetContent = true // Recompose with sheet content
        rule.waitForIdle()
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
    }

    @Test
    fun modalBottomSheet_callsOnDismissRequest_onNestedScrollFling() {
        var callCount by mutableStateOf(0)
        val expectedCallCount = 1
        val sheetState = SheetState(skipPartiallyExpanded = true, density = rule.density)

        val nestedScrollDispatcher = NestedScrollDispatcher()
        val nestedScrollConnection = object : NestedScrollConnection {
            // No-Op
        }
        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            ModalBottomSheet(
                onDismissRequest = { callCount += 1 },
                sheetState = sheetState,
                windowInsets = windowInsets
            ) {
                Column(
                    Modifier
                        .testTag(sheetTag)
                        .nestedScroll(nestedScrollConnection, nestedScrollDispatcher)
                ) {
                    (0..50).forEach {
                        Text(text = "$it")
                    }
                }
            }
        }

        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        val scrollableContentHeight = rule.onNodeWithTag(sheetTag).fetchSemanticsNode().size.height
        // Simulate a drag + fling
        nestedScrollDispatcher.dispatchPostScroll(
            consumed = Offset.Zero,
            available = Offset(x = 0f, y = scrollableContentHeight / 2f),
            source = NestedScrollSource.Drag
        )
        scope.launch {
            nestedScrollDispatcher.dispatchPostFling(
                consumed = Velocity.Zero,
                available = Velocity(x = 0f, y = with(rule.density) { 200.dp.toPx() })
            )
        }

        rule.waitForIdle()
        assertThat(sheetState.isVisible).isFalse()
        assertThat(callCount).isEqualTo(expectedCallCount)
    }

    @Test
    fun modalBottomSheet_screenWidthConfigurationChange_matchWidthSize() {
        var boxWidth = 0
        var screenWidth by mutableStateOf(0)
        lateinit var configuration: MutableState<Configuration>
        val initialScreenWidth = 100
        val finalScreenWidth = 500

        rule.setContent {
            val localConfig = LocalConfiguration.current
            configuration = remember { mutableStateOf(Configuration(localConfig)) }

            configuration.value.screenWidthDp = initialScreenWidth

            val windowInsets = if (edgeToEdgeWrapper.edgeToEdgeEnabled)
                WindowInsets(0) else BottomSheetDefaults.windowInsets

            CompositionLocalProvider(
                LocalConfiguration provides configuration.value
            ) {
                val context = LocalContext.current
                screenWidth = context.resources.displayMetrics.widthPixels
                ModalBottomSheet(
                    onDismissRequest = {},
                    windowInsets = windowInsets
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(sheetHeight)
                            .onSizeChanged { boxWidth = it.width }
                    )
                }
            }
        }

        // Make sure that the BottomSheet's width is the same as the configuration's screen width
        assertThat(boxWidth).isEqualTo(screenWidth)

        // Change the screen width
        configuration.value.screenWidthDp = finalScreenWidth

        // Make sure that BottomSheet is updating and resizing to the new screen width
        assertThat(boxWidth).isEqualTo(screenWidth)
    }

    @Test
    fun modalBottomSheet_preservesLayoutDirection() {
        var value = LayoutDirection.Ltr
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ModalBottomSheet(onDismissRequest = { /*TODO*/ }) {
                    value = LocalLayoutDirection.current
                }
            }
        }
        rule.runOnIdle {
            assertThat(value).isEqualTo(LayoutDirection.Rtl)
        }
    }

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = arrayOf(
            EdgeToEdgeWrapper("EdgeToEdge", true),
            EdgeToEdgeWrapper("NonEdgeToEdge", false)
        )
    }

    class EdgeToEdgeWrapper(val name: String, val edgeToEdgeEnabled: Boolean) {
        override fun toString(): String {
            return name
        }
    }
}
