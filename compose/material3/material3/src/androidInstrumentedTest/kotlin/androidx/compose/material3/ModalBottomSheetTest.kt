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
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.View
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
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class ModalBottomSheetTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val sheetHeight = 256.dp
    private val dragHandleSize = 44.dp

    private val sheetTag = "sheetContentTag"
    private val dragHandleTag = "dragHandleTag"
    private val BackTestTag = "Back"

    @Test
    fun modalBottomSheet_isDismissedOnTapOutside() {
        var showBottomSheet by mutableStateOf(true)
        lateinit var sheetState: SheetState

        rule.setContent {
            val density = LocalDensity.current
            sheetState =
                SheetState(
                    skipPartiallyExpanded = false,
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                ) {
                    Box(Modifier.size(sheetHeight).testTag(sheetTag))
                }
            }
        }

        assertThat(sheetState.isVisible).isTrue()

        // Tap Scrim
        val outsideY =
            with(rule.density) {
                rule
                    .onAllNodes(isDialog())
                    .onFirst()
                    .getUnclippedBoundsInRoot()
                    .height
                    .roundToPx() / 4
            }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(0, outsideY)
        rule.waitForIdle()

        // Bottom sheet should not exist
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_disabledClickOutside() {
        var showBottomSheet by mutableStateOf(true)
        lateinit var sheetState: SheetState

        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                    properties = ModalBottomSheetProperties(shouldDismissOnClickOutside = false),
                ) {
                    Box(Modifier.size(sheetHeight).testTag(sheetTag))
                }
            }
        }

        assertThat(sheetState.isVisible).isTrue()

        // Tap Scrim
        val outsideY =
            with(rule.density) {
                rule
                    .onAllNodes(isDialog())
                    .onFirst()
                    .getUnclippedBoundsInRoot()
                    .height
                    .roundToPx() / 4
            }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(0, outsideY)
        rule.waitForIdle()

        // Bottom sheet should exist
        assertThat(sheetState.isVisible).isTrue()
        rule.onNodeWithTag(sheetTag).assertExists()
    }

    @Test
    fun modalBottomSheet_isDismissedOnTapOutsideWithPadding() {
        var showBottomSheet by mutableStateOf(true)
        lateinit var sheetState: SheetState

        rule.setContent {
            val density = LocalDensity.current
            val screenHeight = LocalContext.current.resources.configuration.screenHeightDp.dp
            sheetState =
                SheetState(
                    skipPartiallyExpanded = false,
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                    modifier = Modifier.padding(top = screenHeight / 2),
                    // Have top padding account for the top half of the screen
                ) {
                    Box(Modifier.fillMaxSize().testTag(sheetTag))
                }
            }
        }

        assertThat(sheetState.isVisible).isTrue()

        // Tap Scrim
        val outsideY =
            with(rule.density) {
                rule
                    .onAllNodes(isDialog())
                    .onFirst()
                    .getUnclippedBoundsInRoot()
                    .height
                    .roundToPx() / 2
            }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(0, outsideY)
        rule.waitForIdle()

        // Bottom sheet should not exist
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_isDismissedOnSwipeDown() {
        var showBottomSheet by mutableStateOf(true)
        val sheetState =
            SheetState(
                skipPartiallyExpanded = false,
                positionalThreshold = {
                    with(rule.density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                },
                velocityThreshold = {
                    with(rule.density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                },
            )
        rule.setContent {
            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                ) {
                    Box(Modifier.size(sheetHeight).testTag(sheetTag))
                }
            }
        }

        assertThat(sheetState.isVisible).isTrue()

        // Swipe Down
        rule.onNodeWithTag(sheetTag).performTouchInput { swipeDown() }
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

            ModalBottomSheet(onDismissRequest = {}) {
                Box(
                    Modifier.fillMaxWidth().height(sheetHeight).onSizeChanged {
                        boxWidth = it.width
                    }
                )
            }
        }
        assertThat(boxWidth).isEqualTo(screenWidth)
    }

    // TODO(330937081): Update test logic to instead change virtual screen size.
    @Test
    @Ignore
    fun modalBottomSheet_wideScreen_fixedMaxWidth_sheetRespectsMaxWidthAndIsCentered() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val latch = CountDownLatch(1)

        rule.activity.application.registerComponentCallbacks(
            object : ComponentCallbacks2 {
                override fun onConfigurationChanged(p0: Configuration) {
                    latch.countDown()
                }

                @Deprecated("deprecated")
                override fun onLowMemory() {
                    // NO-OP
                }

                override fun onTrimMemory(p0: Int) {
                    // NO-OP
                }
            }
        )

        try {
            latch.await(1500, TimeUnit.MILLISECONDS)
            rule.setContent {
                ModalBottomSheet(onDismissRequest = {}) {
                    Box(Modifier.testTag(sheetTag).fillMaxHeight(0.4f))
                }
            }
            rule.waitForIdle()
            val simulatedRootWidth = rule.onNode(isDialog()).getUnclippedBoundsInRoot().width
            val maxSheetWidth = 640.dp
            val expectedSheetWidth = maxSheetWidth.coerceAtMost(simulatedRootWidth)
            // Our sheet should be max 640 dp but fill the width if the container is less wide
            val expectedSheetLeft =
                if (simulatedRootWidth <= expectedSheetWidth) {
                    0.dp
                } else {
                    (simulatedRootWidth - expectedSheetWidth) / 2
                }

            rule
                .onNodeWithTag(sheetTag)
                .onParent()
                .assertLeftPositionInRootIsEqualTo(expectedLeft = expectedSheetLeft)
                .assertWidthIsEqualTo(expectedSheetWidth)
        } catch (e: InterruptedException) {
            fail("Unable to verify sheet width in landscape orientation")
        } finally {
            rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // TODO(330937081): Update test logic to instead change virtual screen size.
    @Test
    @Ignore
    fun modalBottomSheet_wideScreen_filledWidth_sheetFillsEntireWidth() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val latch = CountDownLatch(1)

        rule.activity.application.registerComponentCallbacks(
            object : ComponentCallbacks2 {
                override fun onConfigurationChanged(p0: Configuration) {
                    latch.countDown()
                }

                @Deprecated("deprecated")
                override fun onLowMemory() {
                    // NO-OP
                }

                override fun onTrimMemory(p0: Int) {
                    // NO-OP
                }
            }
        )

        try {
            latch.await(3000, TimeUnit.MILLISECONDS)
            var screenWidthPx by mutableStateOf(0)
            rule.setContent {
                val context = LocalContext.current
                screenWidthPx = context.resources.displayMetrics.widthPixels
                ModalBottomSheet(onDismissRequest = {}, sheetMaxWidth = Dp.Unspecified) {
                    Box(Modifier.testTag(sheetTag).fillMaxHeight(0.4f))
                }
            }
            rule.waitForIdle()
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
            sheetState = rememberModalBottomSheetState()

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0) },
            ) {
                Box(Modifier.fillMaxWidth().testTag(sheetTag).height(sheetHeight))
            }
        }

        height = rule.onNode(isDialog()).getUnclippedBoundsInRoot().height
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        rule.onNodeWithTag(sheetTag).assertTopPositionInRootIsEqualTo(height - sheetHeight)
    }

    @Test
    fun modalBottomSheet_defaultStateForLargeContentIsHalfExpanded() {
        lateinit var sheetState: SheetState
        var screenHeightPx by mutableStateOf(0f)

        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                Box(
                    Modifier
                        // Deliberately use fraction != 1f
                        .fillMaxSize(0.6f)
                        .testTag(sheetTag)
                )
            }
        }
        rule.waitForIdle()
        screenHeightPx =
            with(rule.density) { rule.onNode(isDialog()).getUnclippedBoundsInRoot().height.toPx() }
        assertThat(sheetState.targetValue).isEqualTo(SheetValue.PartiallyExpanded)
        assertThat(sheetState.requireOffset()).isWithin(1f).of(screenHeightPx / 2f)
    }

    @Test
    fun modalBottomSheet_shortSheet_isDismissedOnBackPress() {
        var showBottomSheet by mutableStateOf(true)
        lateinit var sheetState: SheetState

        rule.setContent {
            val density = LocalDensity.current
            sheetState =
                SheetState(
                    skipPartiallyExpanded = true,
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                ) {
                    Box(Modifier.fillMaxHeight(0.4f).testTag(sheetTag)) {
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
        lateinit var sheetState: SheetState

        rule.setContent {
            val density = LocalDensity.current
            sheetState =
                SheetState(
                    skipPartiallyExpanded = false,
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            if (showBottomSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet = false },
                ) {
                    Box(Modifier.fillMaxHeight(0.6f).testTag(sheetTag)) {
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
            with(rule.density) { (screenHeight - size).toPx() }
        }

        rule.setContent {
            val context = LocalContext.current
            screenHeight = context.resources.configuration.screenHeightDp.dp
            state = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0) },
            ) {
                Box(Modifier.height(size).fillMaxWidth())
            }
        }
        screenHeight = rule.onNode(isDialog()).getUnclippedBoundsInRoot().height
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
            sheetMaxWidth = remember { mutableStateOf(0.dp) }
            val context = LocalContext.current
            val density = LocalDensity.current
            screenWidth = with(density) { context.resources.displayMetrics.widthPixels.toDp() }
            ModalBottomSheet(onDismissRequest = {}, sheetMaxWidth = sheetMaxWidth.value) {
                Box(Modifier.fillMaxWidth().testTag(sheetTag))
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

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0) },
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
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0) },
            ) {
                scope = rememberCoroutineScope()
                LazyColumn { items(amountOfItems) { ListItem(headlineContent = { Text("$it") }) } }
            }
        }

        assertThat(state.currentValue).isEqualTo(SheetValue.Hidden)

        amountOfItems = 50
        rule.waitForIdle()
        scope.launch { state.show() }
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

        scope.launch { state.expand() }
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.Expanded)

        amountOfItems = 50
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.Expanded)
        // We should retain the current value if possible
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
            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                scrollState = rememberScrollState()
                Column(Modifier.verticalScroll(scrollState).testTag(sheetTag)) {
                    repeat(100) { Text(it.toString(), Modifier.requiredHeight(50.dp)) }
                }
            }
        }

        rule.waitForIdle()

        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        rule.onNodeWithTag(sheetTag).performTouchInput {
            swipeUp(startY = bottom, endY = bottom / 2)
        }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)

        rule.onNodeWithTag(sheetTag).performTouchInput { swipeUp(startY = bottom, endY = top) }
        rule.waitForIdle()
        assertThat(scrollState.value).isGreaterThan(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)

        rule.onNodeWithTag(sheetTag).performTouchInput { swipeDown(startY = top, endY = bottom) }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)

        rule.onNodeWithTag(sheetTag).performTouchInput {
            swipeDown(startY = top, endY = bottom / 2)
        }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        rule.onNodeWithTag(sheetTag).performTouchInput {
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
        lateinit var sheetState: SheetState
        lateinit var scope: CoroutineScope

        rule.setContent {
            val density = LocalDensity.current
            sheetState =
                SheetState(
                    skipPartiallyExpanded = false,
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            scope = rememberCoroutineScope()
            ModalBottomSheet(
                onDismissRequest = {},
                modifier = Modifier.testTag(topTag),
                dragHandle = null,
                sheetState = sheetState,
            ) {
                if (showShortContent) {
                    Box(Modifier.fillMaxWidth().height(1.dp))
                } else {
                    Box(Modifier.fillMaxSize().testTag(sheetTag))
                }
            }
        }

        scope.launch { sheetState.hide() }
        rule.runOnIdle { assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden) }

        showShortContent = true
        scope.launch { sheetState.show() } // We can't use LaunchedEffect with Swipeable in tests
        // yet, so we're invoking this outside of composition. See b/254115946.

        rule.runOnIdle { assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded) }
    }

    @Test
    fun modalBottomSheet_expandBySwiping() {
        lateinit var sheetState: SheetState
        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                Box(Modifier.fillMaxSize().testTag(sheetTag))
            }
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(sheetTag).performTouchInput { swipeUp() }

        rule.runOnIdle { assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded) }
    }

    @Test
    fun modalBottomSheet_respectsConfirmValueChange() {
        lateinit var sheetState: SheetState
        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            sheetState =
                rememberModalBottomSheetState(
                    confirmValueChange = { newState -> newState != SheetValue.Hidden }
                )

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = { Box(Modifier.testTag(dragHandleTag).size(dragHandleSize)) },
            ) {
                Box(Modifier.fillMaxSize().testTag(sheetTag))
            }
        }

        val currentOffset = sheetState.requireOffset()
        // Respects on swipe gesture
        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }
        rule.onNodeWithTag(sheetTag).performTouchInput { swipeDown() }
        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }
        assertThat(sheetState.requireOffset()).isEqualTo(currentOffset)

        // Respects on animation call
        rule.runOnIdle { scope.launch { sheetState.hide() } }
        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }
        assertThat(sheetState.requireOffset()).isEqualTo(currentOffset)

        // Respects on semantic action
        rule
            .onNodeWithTag(dragHandleTag, useUnmergedTree = true)
            .onParent()
            .performSemanticsAction(SemanticsActions.Dismiss)

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }
        assertThat(sheetState.requireOffset()).isEqualTo(currentOffset)

        // Tap Scrim
        val outsideY =
            with(rule.density) {
                rule
                    .onAllNodes(isDialog())
                    .onFirst()
                    .getUnclippedBoundsInRoot()
                    .height
                    .roundToPx() / 4
            }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(0, outsideY)
        rule.waitForIdle()
        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }
        assertThat(sheetState.requireOffset()).isEqualTo(currentOffset)
    }

    @Test
    fun modalBottomSheet_hideBySwiping_tallBottomSheet() {
        lateinit var sheetState: SheetState
        lateinit var scope: CoroutineScope
        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            scope = rememberCoroutineScope()
            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                Box(Modifier.fillMaxSize().testTag(sheetTag))
            }
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        scope.launch { sheetState.expand() }
        rule.runOnIdle { assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded) }

        rule.onNodeWithTag(sheetTag).performTouchInput { swipeDown() }

        rule.runOnIdle { assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden) }
    }

    @Test
    fun modalBottomSheet_hideBySwiping_skipPartiallyExpanded() {
        lateinit var sheetState: SheetState
        rule.setContent {
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                Box(Modifier.fillMaxWidth().height(sheetHeight).testTag(sheetTag))
            }
        }

        rule.runOnIdle { assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded) }

        rule.onNodeWithTag(sheetTag).performTouchInput { swipeDown() }

        rule.runOnIdle { assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden) }
    }

    @Test
    fun modalBottomSheet_hideManually_skipPartiallyExpanded(): Unit =
        runBlocking(AutoTestFrameClock()) {
            lateinit var sheetState: SheetState
            rule.setContent {
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                    Box(Modifier.fillMaxSize().testTag(sheetTag))
                }
            }
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)

            sheetState.hide()

            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
        }

    @Test
    fun modalBottomSheet_testParialExpandReturnsIllegalStateException_whenSkipPartialExpanded() {
        lateinit var scope: CoroutineScope
        lateinit var bottomSheetState: SheetState

        rule.setContent {
            val density = LocalDensity.current
            bottomSheetState =
                SheetState(
                    skipPartiallyExpanded = true,
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            scope = rememberCoroutineScope()
            ModalBottomSheet(onDismissRequest = {}, sheetState = bottomSheetState) {
                Box(Modifier.fillMaxSize().testTag(sheetTag))
            }
        }
        scope.launch {
            val exception =
                kotlin.runCatching { bottomSheetState.partialExpand() }.exceptionOrNull()
            assertThat(exception).isNotNull()
            assertThat(exception).isInstanceOf(IllegalStateException::class.java)
            assertThat(exception)
                .hasMessageThat()
                .containsMatch(
                    "Attempted to animate to partial expanded when skipPartiallyExpanded was " +
                        "enabled. Set skipPartiallyExpanded to false to use this function."
                )
        }
    }

    @Test
    fun modalBottomSheet_testDismissAction_tallBottomSheet_whenPartiallyExpanded() {
        rule.setContent {
            ModalBottomSheet(
                onDismissRequest = {},
                dragHandle = { Box(Modifier.testTag(dragHandleTag).size(dragHandleSize)) },
            ) {
                Box(Modifier.fillMaxSize().testTag(sheetTag))
            }
        }

        rule
            .onNodeWithTag(dragHandleTag, useUnmergedTree = true)
            .onParent()
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
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = { Box(Modifier.testTag(dragHandleTag).size(dragHandleSize)) },
            ) {
                Box(Modifier.fillMaxSize().testTag(sheetTag))
            }
        }

        rule
            .onNodeWithTag(dragHandleTag, useUnmergedTree = true)
            .onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Expand)

        rule.runOnIdle { assertThat(sheetState.requireOffset()).isEqualTo(0f) }
    }

    @Test
    fun modalBottomSheet_testDismissAction_tallBottomSheet_whenExpanded() {
        lateinit var sheetState: SheetState
        lateinit var scope: CoroutineScope

        var screenHeightPx by mutableStateOf(0f)

        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            scope = rememberCoroutineScope()
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = { Box(Modifier.testTag(dragHandleTag).size(dragHandleSize)) },
            ) {
                Box(Modifier.fillMaxSize().testTag(sheetTag))
            }
        }
        screenHeightPx =
            with(rule.density) { rule.onNode(isDialog()).getUnclippedBoundsInRoot().height.toPx() }
        scope.launch { sheetState.expand() }
        rule.waitForIdle()

        rule
            .onNodeWithTag(dragHandleTag, useUnmergedTree = true)
            .onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss)

        rule.runOnIdle { assertThat(sheetState.requireOffset()).isWithin(1f).of(screenHeightPx) }
    }

    @Test
    fun modalBottomSheet_testCollapseAction_tallBottomSheet_whenExpanded() {
        lateinit var sheetState: SheetState
        lateinit var scope: CoroutineScope

        var screenHeightPx by mutableStateOf(0f)

        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            scope = rememberCoroutineScope()
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = { Box(Modifier.testTag(dragHandleTag).size(dragHandleSize)) },
            ) {
                Box(Modifier.fillMaxSize().testTag(sheetTag))
            }
        }
        screenHeightPx =
            with(rule.density) { rule.onNode(isDialog()).getUnclippedBoundsInRoot().height.toPx() }
        scope.launch { sheetState.expand() }
        rule.waitForIdle()

        rule
            .onNodeWithTag(dragHandleTag, useUnmergedTree = true)
            .onParent()
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
        var hasSheetContent by mutableStateOf(false) // Start out with empty sheet content
        lateinit var scope: CoroutineScope
        lateinit var sheetState: SheetState

        rule.setContent {
            val density = LocalDensity.current
            sheetState =
                SheetState(
                    skipPartiallyExpanded = false,
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            scope = rememberCoroutineScope()

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0) },
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
        var hasSheetContent by mutableStateOf(false) // Start out with empty sheet content
        lateinit var scope: CoroutineScope
        lateinit var sheetState: SheetState

        rule.setContent {
            val density = LocalDensity.current
            sheetState =
                SheetState(
                    skipPartiallyExpanded = false,
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            scope = rememberCoroutineScope()
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0) },
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
        val nestedScrollDispatcher = NestedScrollDispatcher()
        val nestedScrollConnection =
            object : NestedScrollConnection {
                // No-Op
            }
        lateinit var scope: CoroutineScope

        lateinit var sheetState: SheetState

        rule.setContent {
            val density = LocalDensity.current
            sheetState =
                SheetState(
                    skipPartiallyExpanded = true,
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            scope = rememberCoroutineScope()
            ModalBottomSheet(onDismissRequest = { callCount += 1 }, sheetState = sheetState) {
                Column(
                    Modifier.testTag(sheetTag)
                        .nestedScroll(nestedScrollConnection, nestedScrollDispatcher)
                ) {
                    (0..50).forEach { Text(text = "$it") }
                }
            }
        }

        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        val scrollableContentHeight = rule.onNodeWithTag(sheetTag).fetchSemanticsNode().size.height
        // Simulate a drag + fling
        nestedScrollDispatcher.dispatchPostScroll(
            consumed = Offset.Zero,
            available = Offset(x = 0f, y = scrollableContentHeight / 2f),
            source = NestedScrollSource.UserInput,
        )
        scope.launch {
            nestedScrollDispatcher.dispatchPostFling(
                consumed = Velocity.Zero,
                available = Velocity(x = 0f, y = with(rule.density) { 200.dp.toPx() }),
            )
        }

        rule.waitForIdle()
        assertThat(sheetState.isVisible).isFalse()
        assertThat(callCount).isEqualTo(expectedCallCount)
    }

    @Ignore("b/307313354")
    @Test
    fun modalBottomSheet_imePadding() {
        // TODO: Include APIs < 30  when a solution is found for b/290893168.
        // TODO: 33 > API > 29 does not use imePadding because of b/285746907, include when a better
        // solution is found.
        Assume.assumeTrue(SDK_INT >= 33)

        val imeAnimationDuration = 1000000L
        val textFieldTag = "sheetTextField"

        lateinit var sheetState: SheetState
        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(sheetState = sheetState, onDismissRequest = {}) {
                Box(Modifier.testTag(sheetTag)) {
                    TextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.testTag(textFieldTag),
                    )
                }
            }
        }

        // Stop auto advance for test consistency
        rule.mainClock.autoAdvance = false

        val textFieldNode = rule.onNodeWithTag(textFieldTag)
        val sheetNode = rule.onNodeWithTag(sheetTag)
        val initialTop = sheetNode.getUnclippedBoundsInRoot().top

        // Focus on the text field to force ime visibility.
        textFieldNode.requestFocus()

        // Wait for the ime and bottom sheet to animate after text field is focused.
        rule.mainClock.advanceTimeBy(imeAnimationDuration)
        val finalTop = sheetNode.getUnclippedBoundsInRoot().top
        rule.runOnIdle {
            // The top of the bottom sheet should be higher now due to ime padding.
            assertThat(finalTop).isLessThan(initialTop)
        }
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
        rule.runOnIdle { assertThat(value).isEqualTo(LayoutDirection.Rtl) }
    }

    @Test
    fun modalBottomSheetContent_respectsProvidedInsets() {
        rule.setContent {
            ModalBottomSheet(
                onDismissRequest = { /*TODO*/ },
                dragHandle = {},
                contentWindowInsets = { WindowInsets(bottom = sheetHeight) },
                modifier = Modifier.testTag(sheetTag),
            ) {
                Box {}
            }
        }
        // Size entirely filled by padding provided by WindowInsetPadding
        rule.onNodeWithTag(sheetTag).assertHeightIsEqualTo(sheetHeight)
    }

    @Test
    fun modalBottomSheetContent_halfScreen_consumesSheetOffsetAsTopInsets() {
        var consumedTopInsets = 0
        val providedTopInsets = 10
        lateinit var sheetState: SheetState

        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            val density = LocalDensity.current
            ModalBottomSheet(
                onDismissRequest = {},
                modifier = Modifier,
                sheetState = sheetState,
                contentWindowInsets = { WindowInsets(top = providedTopInsets) },
            ) {
                Box(
                    Modifier.fillMaxSize().onConsumedWindowInsetsChanged {
                        consumedTopInsets = it.getTop(density)
                    }
                )
            }
        }

        // wait for layout
        rule.waitForIdle()
        assertThat(sheetState.requireOffset()).isGreaterThan(providedTopInsets)
        // Consumed insets are the larger of the two provided consumed insets, in this case, the
        // sheets offset as inset 1, and top window insets as inset 2.
        assertThat(consumedTopInsets).isEqualTo(sheetState.requireOffset().toInt())
    }

    @Test
    fun modalBottomSheetContent_fullScreen_consumesOnlyProvidedContentWindowInsets() {
        var top = 0
        lateinit var sheetState: SheetState
        val providedTopInsets = 10

        rule.setContent {
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val density = LocalDensity.current
            ModalBottomSheet(
                onDismissRequest = {},
                modifier = Modifier,
                contentWindowInsets = { WindowInsets(top = providedTopInsets) },
                sheetState = sheetState,
            ) {
                Box(
                    Modifier.fillMaxSize().onConsumedWindowInsetsChanged {
                        top = it.getTop(density)
                    }
                )
            }
        }

        // wait for layout
        rule.waitForIdle()
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        assertThat(sheetState.requireOffset()).isLessThan(providedTopInsets)
        // Consumed insets are the larger of the two provided consumed insets, in this case, the
        // sheets offset as inset 1, and top window insets as inset 2.
        assertThat(top).isEqualTo(providedTopInsets)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun modalBottomSheet_assertSheetContentIsReadBeforeScrim() {
        lateinit var composeView: View
        var closeSheet = ""
        rule.setContent {
            closeSheet = getString(Strings.CloseSheet)
            ModalBottomSheet(onDismissRequest = {}, modifier = Modifier.testTag(sheetTag)) {
                composeView = LocalView.current
                Box(Modifier.fillMaxWidth().height(sheetHeight))
            }
        }

        val scrimViewId = rule.onNodeWithContentDescription(closeSheet).fetchSemanticsNode().id
        val sheetViewId = rule.onNodeWithTag(sheetTag).fetchSemanticsNode().id

        rule.runOnUiThread {
            val accessibilityNodeProvider = composeView.accessibilityNodeProvider
            val sheetViewANI = accessibilityNodeProvider.createAccessibilityNodeInfo(sheetViewId)
            // Ensure that sheet A11y info is read before scrim view.
            assertThat(sheetViewANI?.extras?.traversalBefore).isAtMost(scrimViewId)
        }
    }

    @Test
    fun modalBottomSheet_testDragHandleClick() {
        lateinit var sheetState: SheetState
        var showBottomSheet by mutableStateOf(true)

        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    dragHandle = { Box(Modifier.testTag(dragHandleTag).size(dragHandleSize)) },
                ) {
                    Box(Modifier.fillMaxSize().testTag(sheetTag))
                }
            }
        }

        rule.waitForIdle()
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true).performClick()
        rule.waitForIdle()
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true).performClick()
        rule.waitForIdle()
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_gesturesDisabled_doesNotParticipateInNestedScroll() =
        runBlocking(AutoTestFrameClock()) {
            lateinit var scope: CoroutineScope
            lateinit var sheetState: SheetState
            val scrollConnection = object : NestedScrollConnection {}
            val scrollDispatcher = NestedScrollDispatcher()
            val sheetHeight = 300.dp
            val sheetHeightPx = with(rule.density) { sheetHeight.toPx() }

            rule.setContent {
                scope = rememberCoroutineScope()
                sheetState = rememberModalBottomSheetState()
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = {},
                    sheetGesturesEnabled = false,
                ) {
                    Box(
                        Modifier.fillMaxWidth()
                            .requiredHeight(sheetHeight)
                            .nestedScroll(scrollConnection, scrollDispatcher)
                            .testTag(sheetTag)
                    )
                }
            }
            scope.launch { sheetState.expand() }
            rule.waitForIdle()
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)

            val offsetBeforeScroll = sheetState.requireOffset()
            scrollDispatcher.dispatchPreScroll(
                Offset(x = 0f, y = -sheetHeightPx),
                NestedScrollSource.UserInput,
            )
            rule.waitForIdle()
            assertWithMessage("Offset after scroll is equal to offset before scroll")
                .that(sheetState.requireOffset())
                .isEqualTo(offsetBeforeScroll)

            val highFlingVelocity = Velocity(x = 0f, y = with(rule.density) { 500.dp.toPx() })
            scrollDispatcher.dispatchPreFling(highFlingVelocity)
            rule.waitForIdle()
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        }

    private val Bundle.traversalBefore: Int
        get() = getInt("android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL")
}
