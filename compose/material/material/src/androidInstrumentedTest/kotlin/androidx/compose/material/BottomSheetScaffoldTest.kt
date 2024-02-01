/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.material

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class BottomSheetScaffoldTest {

    @get:Rule
    val rule = createComposeRule()

    private val peekHeight = 75.dp

    private val fabSpacing = 16.dp

    private val sheetContent = "frontLayerTag"

    private fun advanceClock() {
        rule.mainClock.advanceTimeBy(100_000L)
    }

    @Test
    fun bottomSheetScaffold_testOffset_whenCollapsed() {
        rule.setContent {
            BottomSheetScaffold(
                sheetContent = {
                    Box(Modifier.fillMaxSize().testTag(sheetContent))
                },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetContent)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)
    }

    @Test
    fun bottomSheetScaffold_testOffset_whenExpanded() {
        rule.setContent {
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberBottomSheetState(BottomSheetValue.Expanded)
                ),
                sheetContent = {
                    Box(Modifier.fillMaxWidth().requiredHeight(300.dp).testTag(sheetContent))
                },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetContent)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - 300.dp)
    }

    @Test
    fun bottomSheetScaffold_testExpandAction_whenCollapsed() {
        rule.setContent {
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
                ),
                sheetContent = {
                    Box(Modifier.fillMaxWidth().requiredHeight(300.dp).testTag(sheetContent))
                },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetContent).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .performSemanticsAction(SemanticsActions.Expand)

        advanceClock()

        rule.onNodeWithTag(sheetContent)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - 300.dp)
    }

    @Test
    fun bottomSheetScaffold_testCollapseAction_whenExpanded() {
        rule.setContent {
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberBottomSheetState(BottomSheetValue.Expanded)
                ),
                sheetContent = {
                    Box(Modifier.fillMaxWidth().requiredHeight(300.dp).testTag(sheetContent))
                },
                sheetPeekHeight = peekHeight
            ) {
                Text("Content")
            }
        }

        rule.onNodeWithTag(sheetContent).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
            .performSemanticsAction(SemanticsActions.Collapse)

        advanceClock()

        rule.onNodeWithTag(sheetContent)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)
    }

    @Test
    fun bottomSheetScaffold_testNoCollapseExpandAction_whenPeekHeightIsSheetHeight() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                BottomSheetScaffold(
                    scaffoldState = rememberBottomSheetScaffoldState(
                        bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
                    ),
                    sheetContent = {
                        Box(
                            Modifier.fillMaxWidth().requiredHeight(peekHeight).testTag(sheetContent)
                        )
                    },
                    sheetPeekHeight = peekHeight
                ) {
                    Text("Content")
                }
            }
        }

        rule.onNodeWithTag(sheetContent).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
    }

    @Test
    fun bottomSheetScaffold_revealAndConceal_manually(): Unit = runBlocking(AutoTestFrameClock()) {
        lateinit var bottomSheetState: BottomSheetState
        rule.setContent {
            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState
                ),
                sheetContent = {
                    Box(Modifier.fillMaxWidth().requiredHeight(300.dp).testTag(sheetContent))
                },
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }

        rule.onNodeWithTag(sheetContent)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)

        bottomSheetState.expand()

        advanceClock()

        rule.onNodeWithTag(sheetContent)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - 300.dp)

        bottomSheetState.collapse()

        advanceClock()

        rule.onNodeWithTag(sheetContent)
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - peekHeight)
    }

    @Test
    fun bottomSheetScaffold_revealBySwiping() {
        lateinit var bottomSheetState: BottomSheetState
        rule.setContent {
            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState
                ),
                sheetContent = {
                    Box(Modifier.fillMaxWidth().requiredHeight(300.dp).testTag(sheetContent))
                },
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }

        rule.runOnIdle {
            Truth.assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Collapsed)
        }

        rule.onNodeWithTag(sheetContent)
            .performTouchInput { swipeUp() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Expanded)
        }

        rule.onNodeWithTag(sheetContent)
            .performTouchInput { swipeDown() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Collapsed)
        }
    }

    @Test
    fun bottomSheetScaffold_respectsConfirmStateChange() {
        lateinit var bottomSheetState: BottomSheetState
        rule.setContent {
            bottomSheetState = rememberBottomSheetState(
                BottomSheetValue.Collapsed,
                confirmStateChange = {
                    it != BottomSheetValue.Expanded
                }
            )
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState,
                ),
                sheetContent = {
                    Box(Modifier.fillMaxWidth().requiredHeight(300.dp).testTag(sheetContent))
                },
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }

        rule.runOnIdle {
            Truth.assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Collapsed)
        }

        rule.onNodeWithTag(sheetContent)
            .performTouchInput { swipeUp() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Collapsed)
        }

        rule.onNodeWithTag(sheetContent).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .performSemanticsAction(SemanticsActions.Expand)

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Collapsed)
        }
    }

    @Test
    fun bottomSheetScaffold_revealBySwiping_gesturesDisabled() {
        lateinit var bottomSheetState: BottomSheetState
        rule.setContent {
            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
            BottomSheetScaffold(
                scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = bottomSheetState
                ),
                sheetContent = {
                    Box(Modifier.fillMaxWidth().requiredHeight(300.dp).testTag(sheetContent))
                },
                sheetGesturesEnabled = false,
                sheetPeekHeight = peekHeight,
                content = { Text("Content") }
            )
        }

        rule.runOnIdle {
            Truth.assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Collapsed)
        }

        rule.onNodeWithTag(sheetContent)
            .performTouchInput { swipeUp() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Collapsed)
        }
    }

    @Test
    fun bottomSheetScaffold_gesturesDisabled_doesNotParticipateInNestedScroll() =
        runBlocking(AutoTestFrameClock()) {
            lateinit var bottomSheetState: BottomSheetState
            val scrollConnection = object : NestedScrollConnection {}
            val scrollDispatcher = NestedScrollDispatcher()
            val sheetHeight = 300.dp
            val sheetHeightPx = with(rule.density) { sheetHeight.toPx() }

            rule.setContent {
                bottomSheetState = rememberBottomSheetState(BottomSheetValue.Expanded)
                BottomSheetScaffold(
                    scaffoldState = rememberBottomSheetScaffoldState(
                        bottomSheetState = bottomSheetState
                    ),
                    sheetContent = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .requiredHeight(sheetHeight)
                                .nestedScroll(scrollConnection, scrollDispatcher)
                                .testTag(sheetContent)
                        )
                    },
                    sheetGesturesEnabled = false,
                    sheetPeekHeight = peekHeight,
                    content = { Box(Modifier.fillMaxSize()) { Text("Content") } }
                )
            }

            Truth.assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Expanded)

            val offsetBeforeScroll = bottomSheetState.requireOffset()
            scrollDispatcher.dispatchPreScroll(
                Offset(x = 0f, y = -sheetHeightPx),
                NestedScrollSource.Drag
            )
            rule.waitForIdle()
            Truth.assertWithMessage("Offset after scroll is equal to offset before scroll")
                .that(bottomSheetState.requireOffset()).isEqualTo(offsetBeforeScroll)

            val highFlingVelocity = Velocity(x = 0f, y = with(rule.density) { 500.dp.toPx() })
            scrollDispatcher.dispatchPreFling(highFlingVelocity)
            rule.waitForIdle()
            Truth.assertThat(bottomSheetState.currentValue).isEqualTo(BottomSheetValue.Expanded)
        }

    @Test
    fun bottomSheetScaffold_AppbarAndContent_inColumn() {
        var appbarPosition: Offset = Offset.Zero
        var appbarSize: IntSize = IntSize.Zero
        var contentPosition: Offset = Offset.Zero
        rule.setMaterialContent {
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
        Truth.assertThat(appbarPosition.y + appbarSize.height.toFloat())
            .isEqualTo(contentPosition.y)
    }

    @Test
    fun bottomSheetScaffold_fab_startPosition(): Unit = runBlocking(AutoTestFrameClock()) {
        val fabTag = "fab"
        var fabSize: IntSize = IntSize.Zero
        lateinit var scaffoldState: BottomSheetScaffoldState
        rule.setContent {
            scaffoldState = rememberBottomSheetScaffoldState()
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetContent = {
                    Box(Modifier.fillMaxWidth().requiredHeight(300.dp).testTag(sheetContent))
                },
                sheetGesturesEnabled = false,
                sheetPeekHeight = peekHeight,
                floatingActionButton = {
                    FloatingActionButton(
                        modifier = Modifier
                            .onGloballyPositioned { positioned ->
                                fabSize = positioned.size
                            }.testTag(fabTag),
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Favorite, null)
                    }
                },
                floatingActionButtonPosition = FabPosition.Start,
                content = { Text("Content") },
            )
        }
        with(rule.density) {
            rule.onNodeWithTag(fabTag).assertLeftPositionInRootIsEqualTo(
                fabSpacing
            ).assertTopPositionInRootIsEqualTo(
                rule.rootHeight() - peekHeight - fabSize.height.toDp() / 2
            )
        }
        scaffoldState.bottomSheetState.expand()
        advanceClock()

        with(rule.density) {
            rule.onNodeWithTag(fabTag).assertLeftPositionInRootIsEqualTo(
                fabSpacing
            ).assertTopPositionInRootIsEqualTo(
                rule.rootHeight() - 300.dp - fabSize.height.toDp() / 2
            )
        }
    }

    @Test
    fun bottomSheetScaffold_fab_endPosition(): Unit = runBlocking(AutoTestFrameClock()) {
        val fabTag = "fab"
        var fabSize: IntSize = IntSize.Zero
        lateinit var scaffoldState: BottomSheetScaffoldState
        rule.setContent {
            scaffoldState = rememberBottomSheetScaffoldState()
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetContent = {
                    Box(Modifier.fillMaxWidth().requiredHeight(300.dp).testTag(sheetContent))
                },
                sheetGesturesEnabled = false,
                sheetPeekHeight = peekHeight,
                floatingActionButton = {
                    FloatingActionButton(
                        modifier = Modifier
                            .onGloballyPositioned { positioned ->
                                fabSize = positioned.size
                            }.testTag(fabTag),
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Favorite, null)
                    }
                },
                floatingActionButtonPosition = FabPosition.End,
                content = { Text("Content") }
            )
        }
        with(rule.density) {
            rule.onNodeWithTag(fabTag).assertLeftPositionInRootIsEqualTo(
                rule.rootWidth() - fabSize.width.toDp() - fabSpacing
            ).assertTopPositionInRootIsEqualTo(
                rule.rootHeight() - peekHeight - fabSize.height.toDp() / 2
            )
        }
        scaffoldState.bottomSheetState.expand()
        advanceClock()

        with(rule.density) {
            rule.onNodeWithTag(fabTag).assertLeftPositionInRootIsEqualTo(
                rule.rootWidth() - fabSize.width.toDp() - fabSpacing
            ).assertTopPositionInRootIsEqualTo(
                rule.rootHeight() - 300.dp - fabSize.height.toDp() / 2
            )
        }
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
                            Modifier.requiredSize(10.dp)
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
                        Modifier.requiredSize(10.dp)
                            .background(color = Color.White)
                    )
                }
            }
        }

        rule.onNodeWithTag("Scaffold")
            .captureToImage().asAndroidBitmap().apply {
                // asserts the appbar(top half part) has the shadow
                val yPos = height / 2 + 2
                Truth.assertThat(Color(getPixel(0, yPos))).isNotEqualTo(Color.White)
                Truth.assertThat(Color(getPixel(width / 2, yPos))).isNotEqualTo(Color.White)
                Truth.assertThat(Color(getPixel(width - 1, yPos))).isNotEqualTo(Color.White)
            }
    }

    @Test
    fun bottomSheetScaffold_innerPadding_lambdaParam() {
        lateinit var innerPadding: PaddingValues

        rule.setContent {
            BottomSheetScaffold(
                sheetContent = {
                    Box(Modifier.fillMaxWidth().requiredHeight(100.dp))
                },
                sheetPeekHeight = peekHeight
            ) {
                innerPadding = it
                Text("body")
            }
        }
        rule.runOnIdle {
            Truth.assertThat(innerPadding.calculateBottomPadding()).isEqualTo(peekHeight)
        }
    }

    /*
     * This is a validity check for b/235588730. We can not verify actual placement behavior in this
     * test as it would require child composables.
     */
    @Test
    fun bottomSheetScaffold_emptySlots_doesNotCrash() {
        rule.setMaterialContent {
            BottomSheetScaffold(
                sheetContent = { },
                topBar = { },
                snackbarHost = { },
                floatingActionButton = { },
                content = { }
            )
        }
    }
}
