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
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.tokens.SheetBottomTokens
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import junit.framework.TestCase
import kotlin.IllegalStateException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetScaffoldTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val restorationTester = StateRestorationTester(rule)

    private val sheetHeight = 256.dp
    private val dragHandleSize = 44.dp
    private val peekHeight = 75.dp
    private val sheetTag = "sheetContentTag"
    private val scaffoldContentTag = "scaffoldContentTag"
    private val dragHandleTag = "dragHandleTag"

    @Test
    fun test_stateSavedAndRestored() {
        val initialValue = SheetValue.Expanded
        lateinit var state: BottomSheetScaffoldState
        restorationTester.setContent {
            state = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberStandardBottomSheetState(initialValue),
            )
        }
        assertThat(state.bottomSheetState.currentValue).isEqualTo(initialValue)
        restorationTester.emulateSavedInstanceStateRestore()
        assertThat(state.bottomSheetState.currentValue).isEqualTo(initialValue)
    }

    @Test
    fun bottomSheetScaffold_testOffset_whenCollapsed() {
        rule.setContent {
            BottomSheetScaffold(
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag))
                },
                sheetPeekHeight = peekHeight,
                sheetDragHandle = null
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetTag)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)
    }

    @Test
    fun bottomSheetScaffold_testOffset_whenExpanded() {
        rule.setContent {
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberStandardBottomSheetState(
                        initialValue = SheetValue.Expanded)
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(sheetHeight))
                },
                sheetDragHandle = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(dragHandleSize)
                            .testTag(sheetTag))
                },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - (sheetHeight + dragHandleSize))
    }

    @Test
    fun bottomSheetScaffold_testExpandAction_whenCollapsed() {
        rule.setContent {
            BottomSheetScaffold(
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(sheetHeight)
                            .testTag(sheetTag))
                },
                sheetDragHandle = { Box(
                    Modifier
                        .testTag(dragHandleTag)
                        .size(dragHandleSize)) },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .performSemanticsAction(SemanticsActions.Expand)

        rule.waitForIdle()
        val expectedSheetHeight = sheetHeight + dragHandleSize

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - expectedSheetHeight)
    }

    @Test
    fun bottomSheetScaffold_testDismissAction_whenEnabled() {
        rule.setContent {
            BottomSheetScaffold(
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(sheetHeight)
                            .testTag(sheetTag))
                },
                sheetDragHandle = { Box(
                    Modifier
                        .testTag(dragHandleTag)
                        .size(dragHandleSize)) },
                sheetPeekHeight = peekHeight,
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberStandardBottomSheetState(
                        skipHiddenState = false
                    )
                )
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .performSemanticsAction(SemanticsActions.Dismiss)

        rule.waitForIdle()

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight())
    }

    @Test
    fun bottomSheetScaffold_testHideReturnsIllegalStateException() {
        lateinit var scope: CoroutineScope
        val bottomSheetState = SheetState(
            skipPartiallyExpanded = false,
            skipHiddenState = true,
            initialValue = SheetValue.PartiallyExpanded,
        )
        rule.setContent {
            scope = rememberCoroutineScope()
            BottomSheetScaffold(
                sheetContent = {
                    Box(Modifier.fillMaxWidth().requiredHeight(sheetHeight))
                },
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState
                )
            ) {
                Text("Content")
            }
        }
        scope.launch {
            val exception = kotlin.runCatching { bottomSheetState.hide() }.exceptionOrNull()
            assertThat(exception).isNotNull()
            assertThat(exception).isInstanceOf(IllegalStateException::class.java)
            assertThat(exception).hasMessageThat().containsMatch(
                "Attempted to animate to hidden when skipHiddenState was enabled. Set " +
                    "skipHiddenState to false to use this function."
            )
        }
    }

    @Test
    fun bottomSheetScaffold_testCollapseAction_whenExpanded() {
        rule.setContent {
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberStandardBottomSheetState(
                        initialValue = SheetValue.Expanded)
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(sheetHeight)
                            .testTag(sheetTag))
                },
                sheetDragHandle = { Box(
                    Modifier
                        .testTag(dragHandleTag)
                        .size(dragHandleSize)) },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
            .performSemanticsAction(SemanticsActions.Collapse)

        rule.waitForIdle()

        rule.onNodeWithTag(dragHandleTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)
    }

    @Test
    fun bottomSheetScaffold_testNoCollapseExpandAction_whenPeekHeightIsSheetHeight() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                BottomSheetScaffold(
                    sheetContent = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .requiredHeight(peekHeight)
                                .testTag(sheetTag)
                        )
                    },
                    sheetDragHandle = null,
                    sheetPeekHeight = peekHeight
                ) {
                    Text("Content")
                }
            }
        }

        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
    }

    @Test
    fun backdropScaffold_revealAndConceal_manually(): Unit = runBlocking(AutoTestFrameClock()) {
        lateinit var bottomSheetState: SheetState
        rule.setContent {
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded)
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(sheetHeight))
                },
                sheetDragHandle = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(dragHandleSize)
                            .testTag(sheetTag))
                },
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }
        val expectedHeight = sheetHeight + dragHandleSize

        rule.onNodeWithTag(sheetTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)

        bottomSheetState.expand()
        rule.waitForIdle()

        rule.onNodeWithTag(sheetTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - expectedHeight)

        bottomSheetState.partialExpand()
        rule.waitForIdle()

        rule.onNodeWithTag(sheetTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)
    }

    @Test
    fun bottomSheetScaffold_revealBySwiping() {
        lateinit var bottomSheetState: SheetState
        rule.setContent {
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded)
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(sheetHeight)
                            .testTag(sheetTag))
                },
                sheetDragHandle = null,
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeUp() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(SheetValue.Expanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeDown() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }
    }

    @Test
    fun bottomSheetScaffold_respectsConfirmStateChange() {
        lateinit var bottomSheetState: SheetState
        rule.setContent {
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                confirmValueChange = {
                    it != SheetValue.Expanded
                }
            )
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState,
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(sheetHeight)
                            .testTag(sheetTag))
                },
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeUp() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
    }

    @Test
    fun bottomSheetScaffold_revealBySwiping_gesturesDisabled() {
        lateinit var bottomSheetState: SheetState
        rule.setContent {
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded)
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState
                ),
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(300.dp)
                            .testTag(sheetTag))
                },
                sheetSwipeEnabled = false,
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeUp() }

        rule.runOnIdle {
            assertThat(bottomSheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }
    }

    @Test
    fun bottomSheetScaffold_AppbarAndContent_inColumn() {
        var appbarPosition: Offset = Offset.Zero
        var appbarSize: IntSize = IntSize.Zero
        var contentPosition: Offset = Offset.Zero
        rule.setContent {
            BottomSheetScaffold(
                topBar = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(color = Color.Red)
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                appbarPosition = positioned.localToWindow(Offset.Zero)
                                appbarSize = positioned.size
                            }
                    )
                },
                sheetContent = {
                    Box(Modifier.requiredSize(10.dp))
                }
            ) {
                Box(
                    Modifier
                        .onGloballyPositioned { contentPosition = it.localToWindow(Offset.Zero) }
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color.Blue)
                )
            }
        }
        assertThat(appbarPosition.y + appbarSize.height.toFloat()).isEqualTo(contentPosition.y)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun bottomSheetScaffold_topAppBarIsDrawnOnTopOfContent() {
        rule.setContent {
            Box(
                Modifier
                    .requiredSize(10.dp, 20.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("Scaffold")
            ) {
                BottomSheetScaffold(
                    topBar = {
                        Box(
                            Modifier
                                .requiredSize(10.dp)
                                .shadow(4.dp)
                                .zIndex(4f)
                                .background(color = Color.White)
                        )
                    },
                    sheetContent = {
                        Box(Modifier.requiredSize(0.dp))
                    }
                ) {
                    Box(
                        Modifier
                            .requiredSize(10.dp)
                            .background(color = Color.White)
                    )
                }
            }
        }

        rule.onNodeWithTag("Scaffold")
            .captureToImage().asAndroidBitmap().apply {
                // asserts the appbar(top half part) has the shadow
                val yPos = height / 2 + 2
                assertThat(Color(getPixel(0, yPos))).isNotEqualTo(Color.White)
                assertThat(Color(getPixel(width / 2, yPos))).isNotEqualTo(Color.White)
                assertThat(Color(getPixel(width - 1, yPos))).isNotEqualTo(Color.White)
            }
    }

    @Test
    fun bottomSheetScaffold_innerPadding_lambdaParam() {
        lateinit var innerPadding: PaddingValues

        rule.setContent {
            BottomSheetScaffold(
                sheetContent = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .requiredHeight(100.dp))
                },
                sheetPeekHeight = peekHeight
            ) {
                innerPadding = it
                Text("body")
            }
        }
        rule.runOnIdle {
            assertThat(innerPadding.calculateBottomPadding()).isEqualTo(peekHeight)
        }
    }

    @Test
    fun bottomSheetScaffold_landscape_sheetRespectsMaxWidthAndIsCentered() {
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
                BottomSheetScaffold(sheetContent = {
                    Box(
                        Modifier
                            .testTag(sheetTag)
                            .fillMaxHeight(0.4f)
                    )
                }) {
                    Text("body")
                }
            }
            val rootWidth = rule.rootWidth()
            val maxSheetWidth = 640.dp
            val expectedSheetWidth = maxSheetWidth.coerceAtMost(rootWidth)
            // Our sheet should be max 640 dp but fill the width if the container is less wide
            val expectedSheetLeft = if (rootWidth <= expectedSheetWidth) {
                0.dp
            } else {
                (rootWidth - expectedSheetWidth) / 2
            }

            rule.onNodeWithTag(sheetTag)
                .onParent()
                .assertLeftPositionInRootIsEqualTo(
                    expectedLeft = expectedSheetLeft
                )
                .assertWidthIsEqualTo(expectedSheetWidth)
        } catch (e: InterruptedException) {
            TestCase.fail("Unable to verify sheet width in landscape orientation")
        } finally {
            rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun bottomSheetScaffold_testNestedScrollConnection() {
        lateinit var sheetState: SheetState
        lateinit var sheetScrollState: ScrollState
        lateinit var scaffoldContentScrollState: ScrollState
        lateinit var topAppBarScrollBehavior: TopAppBarScrollBehavior
        var expectedPreScrollContentColor: Color = Color.Unspecified
        var expectedPostScrolledContainerColor: Color = Color.Unspecified

        rule.setContent {
            sheetState = rememberStandardBottomSheetState()
            topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            BottomSheetScaffold(
                modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState),
                sheetContent = {
                    sheetScrollState = rememberScrollState()
                    Column(
                        Modifier
                            .verticalScroll(sheetScrollState)
                            .testTag(sheetTag)
                    ) {
                        repeat(100) {
                            Text(it.toString(), Modifier.requiredHeight(50.dp))
                        }
                    }
                },
                sheetPeekHeight = peekHeight,
                topBar = {
                    TopAppBar(
                        title = {
                            Text("Title")
                            // fraction = 1f to indicate a scroll.
                            expectedPreScrollContentColor = MaterialTheme.colorScheme.surface
                            expectedPostScrolledContainerColor =
                                TopAppBarDefaults.topAppBarColors()
                                    .containerColor(colorTransitionFraction = 1f)
                        },
                    modifier = Modifier.testTag("AppBar"),
                    scrollBehavior = topAppBarScrollBehavior
                )
            }
            ) {
                scaffoldContentScrollState = rememberScrollState()
                Column(
                    Modifier
                        .verticalScroll(scaffoldContentScrollState)
                        .testTag(scaffoldContentTag)
                ) {
                    repeat(100) {
                        Text(it.toString(), Modifier.requiredHeight(50.dp))
                    }
                }
            }
        }

        // Initial sheetScrollStateValue is at 0 and partially expanded
        assertThat(sheetScrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        // At a partial scroll, sheet expands but sheetScrollStateValue is at 0.
        rule.onNodeWithTag(sheetTag)
            .performTouchInput {
                swipeUp(startY = bottom, endY = bottom / 2)
            }
        rule.waitForIdle()
        assertThat(sheetScrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        // Color of TopAppBar has not changed.
        rule.onNodeWithTag("AppBar").captureToImage()
            .assertContainsColor(expectedPreScrollContentColor)

        rule.onNodeWithTag(sheetTag)
            .performTouchInput {
                swipeDown(startY = top, endY = bottom)
            }
        rule.waitForIdle()
        assertThat(sheetScrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        // On content scroll, TopAppBar color updates while sheet state remains PartiallyExpanded
        rule.onNodeWithTag(scaffoldContentTag)
            .performTouchInput {
                swipeUp(startY = bottom / 2, endY = top)
            }
        assertThat(sheetScrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        assertThat(scaffoldContentScrollState.value).isGreaterThan(0)
        rule.onNodeWithTag("AppBar").captureToImage()
            .assertContainsColor(expectedPostScrolledContainerColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun bottomSheetScaffold_slotsPositionedAppropriately() {
        val topBarHeight = 56.dp
        val expectedDragHandleVerticalPadding = 22.dp
        val hostState = SnackbarHostState()
        var snackbarSize: IntSize? = null
        var snackbarPosition: Offset? = null
        var density: Density? = null
        var dragHandleContentDescription = ""
        var dragHandleColor: Color = Color.Unspecified
        var surface: Color = Color.Unspecified
        val dragHandleShape: Shape = RectangleShape

        rule.setContent {
            dragHandleContentDescription = getString(Strings.BottomSheetDragHandleDescription)
            dragHandleColor = SheetBottomTokens.DockedDragHandleColor.toColor()
                .copy(SheetBottomTokens.DockedDragHandleOpacity)
            surface = MaterialTheme.colorScheme.surface
            density = LocalDensity.current
            BottomSheetScaffold(
                sheetContent = {
                    Box(
                        Modifier
                            .height(sheetHeight)
                            .fillMaxWidth()
                            .testTag(sheetTag)
                        )
                    },
                sheetPeekHeight = peekHeight,
                sheetDragHandle = {
                    BottomSheetDefaults.DragHandle(
                        shape = dragHandleShape,
                    )
                },
                topBar = {
                    Box(modifier = Modifier
                        .height(topBarHeight)
                        .fillMaxWidth()
                        .testTag("TopBar")
                    )
                },
                snackbarHost = {
                    SnackbarHost(
                        hostState = hostState,
                        modifier = Modifier
                            .onGloballyPositioned {
                                snackbarSize = it.size
                                snackbarPosition = it.positionInRoot()
                            },
                    )
                },
            ) {
                Box(Modifier.padding(it)) {
                    Text("Scaffold Content", Modifier.testTag("ScaffoldContent"))
                }
            }
        }
        // Assert that the drag handle has vertical padding of 22.dp
        rule
            .onNodeWithContentDescription(dragHandleContentDescription, useUnmergedTree = true)
            .captureToImage()
            .assertShape(
                density = rule.density,
                horizontalPadding = 0.dp,
                verticalPadding = 22.dp,
                backgroundColor = dragHandleColor.compositeOver(surface),
                shapeColor = dragHandleColor.compositeOver(surface),
                shape = dragHandleShape
            )
        // Assert sheet content is positioned at the sheet peek height + drag handle height + 22.dp
        // top and bottom padding.
        rule.onNodeWithTag(sheetTag).assertTopPositionInRootIsEqualTo(
            rule.rootHeight() - peekHeight +
                (expectedDragHandleVerticalPadding * 2) + SheetBottomTokens.DockedDragHandleHeight
        )
        // Assert TopBar is placed at the top of the app.
        rule.onNodeWithTag("TopBar").assertTopPositionInRootIsEqualTo(0.dp)
        // Assert TopBar is sized appropriately.
        rule.onNodeWithTag("TopBar").assertHeightIsEqualTo(topBarHeight)
        rule.onNodeWithTag("TopBar").assertWidthIsEqualTo(rule.rootWidth())
        // Assert scaffold content consumes TopBar height for padding.
        rule.onNodeWithTag("ScaffoldContent").assertTopPositionInRootIsEqualTo(topBarHeight)

        // Assert snackbar is placed above bottom sheet when partially expanded.
        val snackbarBottomOffset = snackbarPosition!!.y + snackbarSize!!.height.toFloat()
        val expectedSnackbarBottomOffset =
            with(density!!) { rule.rootHeight().toPx() - peekHeight.toPx() - snackbarSize!!.height }
        assertThat(snackbarBottomOffset).isWithin(1f).of(expectedSnackbarBottomOffset)
    }
}