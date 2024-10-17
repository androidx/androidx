/*
 * Copyright 2021 The Android Open Source Project
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

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.testutils.forGivenTestCase
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScaffoldTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val scaffoldTag = "Scaffold"
    private val roundingError = 0.5.dp
    private val fabSpacing = 16.dp

    @Test
    fun scaffold_onlyContent_takesWholeScreen() {
        rule
            .setMaterialContentForSizeAssertions(
                parentMaxWidth = 100.dp,
                parentMaxHeight = 100.dp
            ) {
                Scaffold { Text("Scaffold body") }
            }
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun scaffold_onlyContent_stackSlot() {
        var child1: Offset = Offset.Zero
        var child2: Offset = Offset.Zero
        rule.setMaterialContent(lightColorScheme()) {
            Scaffold {
                Text("One", Modifier.onGloballyPositioned { child1 = it.positionInParent() })
                Text("Two", Modifier.onGloballyPositioned { child2 = it.positionInParent() })
            }
        }
        assertThat(child1.y).isEqualTo(child2.y)
        assertThat(child1.x).isEqualTo(child2.x)
    }

    @Test
    fun scaffold_AppbarAndContent_inColumn() {
        var scaffoldSize: IntSize = IntSize.Zero
        var appbarPosition: Offset = Offset.Zero
        var contentPosition: Offset = Offset.Zero
        var contentSize: IntSize = IntSize.Zero
        rule.setMaterialContent(lightColorScheme()) {
            Scaffold(
                topBar = {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(50.dp)
                            .background(color = Color.Red)
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                appbarPosition = positioned.localToWindow(Offset.Zero)
                            }
                    )
                },
                modifier =
                    Modifier.onGloballyPositioned { positioned: LayoutCoordinates ->
                        scaffoldSize = positioned.size
                    }
            ) {
                Box(
                    Modifier.fillMaxSize().background(Color.Blue).onGloballyPositioned {
                        positioned: LayoutCoordinates ->
                        contentPosition = positioned.positionInParent()
                        contentSize = positioned.size
                    }
                )
            }
        }
        assertThat(appbarPosition.y).isEqualTo(contentPosition.y)
        assertThat(scaffoldSize).isEqualTo(contentSize)
    }

    @Test
    fun scaffold_bottomBarAndContent_inStack() {
        var scaffoldSize: IntSize = IntSize.Zero
        var appbarPosition: Offset = Offset.Zero
        var appbarSize: IntSize = IntSize.Zero
        var contentPosition: Offset = Offset.Zero
        var contentSize: IntSize = IntSize.Zero
        rule.setMaterialContent(lightColorScheme()) {
            Scaffold(
                bottomBar = {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(50.dp)
                            .background(color = Color.Red)
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                appbarPosition = positioned.positionInWindow()
                                appbarSize = positioned.size
                            }
                    )
                },
                modifier =
                    Modifier.onGloballyPositioned { positioned: LayoutCoordinates ->
                        scaffoldSize = positioned.size
                    }
            ) {
                Box(
                    Modifier.fillMaxSize().background(color = Color.Blue).onGloballyPositioned {
                        positioned: LayoutCoordinates ->
                        contentPosition = positioned.positionInWindow()
                        contentSize = positioned.size
                    }
                )
            }
        }
        val appBarBottom = appbarPosition.y + appbarSize.height
        val contentBottom = contentPosition.y + contentSize.height
        assertThat(appBarBottom).isEqualTo(contentBottom)
        assertThat(scaffoldSize).isEqualTo(contentSize)
    }

    @Test
    fun scaffold_innerPadding_lambdaParam() {
        var topBarSize: IntSize = IntSize.Zero
        var bottomBarSize: IntSize = IntSize.Zero
        lateinit var innerPadding: PaddingValues

        rule.setContent {
            Scaffold(
                topBar = {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(50.dp)
                            .background(color = Color.Red)
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                topBarSize = positioned.size
                            }
                    )
                },
                bottomBar = {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(100.dp)
                            .background(color = Color.Red)
                            .onGloballyPositioned { positioned: LayoutCoordinates ->
                                bottomBarSize = positioned.size
                            }
                    )
                }
            ) {
                innerPadding = it
                Text("body")
            }
        }
        rule.runOnIdle {
            with(rule.density) {
                assertThat(innerPadding.calculateTopPadding())
                    .isEqualTo(topBarSize.toSize().height.toDp())
                assertThat(innerPadding.calculateBottomPadding())
                    .isEqualTo(bottomBarSize.toSize().height.toDp())
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_topAppBarIsDrawnOnTopOfContent() {
        rule.setContent {
            Box(
                Modifier.requiredSize(10.dp, 20.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag(scaffoldTag)
            ) {
                Scaffold(
                    topBar = {
                        Box(
                            Modifier.requiredSize(10.dp)
                                .shadow(4.dp)
                                .zIndex(4f)
                                .background(color = Color.White)
                        )
                    }
                ) {
                    Box(Modifier.requiredSize(10.dp).background(color = Color.White))
                }
            }
        }

        rule.onNodeWithTag(scaffoldTag).captureToImage().asAndroidBitmap().apply {
            // asserts the appbar(top half part) has the shadow
            val yPos = height / 2 + 2
            assertThat(Color(getPixel(0, yPos))).isNotEqualTo(Color.White)
            assertThat(Color(getPixel(width / 2, yPos))).isNotEqualTo(Color.White)
            assertThat(Color(getPixel(width - 1, yPos))).isNotEqualTo(Color.White)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_providesInsets_respectTopAppBar() {
        rule.setContent {
            Box(Modifier.requiredSize(10.dp, 40.dp)) {
                Scaffold(
                    contentWindowInsets = WindowInsets(top = 5.dp, bottom = 3.dp),
                    topBar = { Box(Modifier.requiredSize(10.dp)) }
                ) { paddingValues ->
                    // top is like top app bar + rounding error
                    assertDpIsWithinThreshold(
                        actual = paddingValues.calculateTopPadding(),
                        expected = 10.dp,
                        threshold = roundingError
                    )
                    // bottom is like the insets
                    assertDpIsWithinThreshold(
                        actual = paddingValues.calculateBottomPadding(),
                        expected = 3.dp,
                        threshold = roundingError
                    )
                    Box(Modifier.requiredSize(10.dp).background(color = Color.White))
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_respectsProvidedInsets() {
        rule.setContent {
            Box(Modifier.requiredSize(10.dp, 40.dp)) {
                Scaffold(
                    contentWindowInsets = WindowInsets(top = 15.dp, bottom = 10.dp),
                ) { paddingValues ->
                    // topPadding is equal to provided top window inset
                    assertDpIsWithinThreshold(
                        actual = paddingValues.calculateTopPadding(),
                        expected = 15.dp,
                        threshold = roundingError
                    )
                    // bottomPadding is equal to provided bottom window inset
                    assertDpIsWithinThreshold(
                        actual = paddingValues.calculateBottomPadding(),
                        expected = 10.dp,
                        threshold = roundingError
                    )
                    Box(Modifier.requiredSize(10.dp).background(color = Color.White))
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_respectsConsumedWindowInsets() {
        rule.setContent {
            Box(
                Modifier.requiredSize(10.dp, 40.dp)
                    .windowInsetsPadding(WindowInsets(top = 10.dp, bottom = 10.dp))
            ) {
                Scaffold(contentWindowInsets = WindowInsets(top = 15.dp, bottom = 15.dp)) {
                    paddingValues ->
                    // Consumed windowInsetsPadding is omitted. This replicates behavior from
                    // Modifier.windowInsetsPadding. (15.dp contentPadding - 10.dp consumedPadding)
                    assertDpIsWithinThreshold(
                        actual = paddingValues.calculateTopPadding(),
                        expected = 5.dp,
                        threshold = roundingError
                    )
                    assertDpIsWithinThreshold(
                        actual = paddingValues.calculateBottomPadding(),
                        expected = 5.dp,
                        threshold = roundingError
                    )
                    Box(Modifier.requiredSize(10.dp).background(color = Color.White))
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_providesInsets_respectCollapsedTopAppBar() {
        rule.setContent {
            Box(Modifier.requiredSize(10.dp, 40.dp)) {
                Scaffold(
                    contentWindowInsets = WindowInsets(top = 5.dp, bottom = 3.dp),
                    topBar = { Box(Modifier.requiredHeight(0.dp).fillMaxWidth()) }
                ) { paddingValues ->
                    // top is like the collapsed top app bar (i.e. 0dp) + rounding error
                    assertDpIsWithinThreshold(
                        actual = paddingValues.calculateTopPadding(),
                        expected = 0.dp,
                        threshold = roundingError
                    )
                    // bottom is like the insets
                    assertDpIsWithinThreshold(
                        actual = paddingValues.calculateBottomPadding(),
                        expected = 3.dp,
                        threshold = roundingError
                    )
                    Box(Modifier.requiredSize(10.dp).background(color = Color.White))
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_providesInsets_respectsBottomAppBar() {
        rule.setContent {
            Box(Modifier.requiredSize(10.dp, 40.dp)) {
                Scaffold(
                    contentWindowInsets = WindowInsets(top = 5.dp, bottom = 3.dp),
                    bottomBar = { Box(Modifier.requiredSize(10.dp)) }
                ) { paddingValues ->
                    // bottom is like bottom app bar + rounding error
                    assertDpIsWithinThreshold(
                        actual = paddingValues.calculateBottomPadding(),
                        expected = 10.dp,
                        threshold = roundingError
                    )
                    // top is like the insets
                    assertDpIsWithinThreshold(
                        actual = paddingValues.calculateTopPadding(),
                        expected = 5.dp,
                        threshold = roundingError
                    )
                    Box(Modifier.requiredSize(10.dp).background(color = Color.White))
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_insetsTests_snackbarRespectsInsets() {
        val hostState = SnackbarHostState()
        var snackbarSize: IntSize? = null
        var snackbarPosition: Offset? = null
        var density: Density? = null
        rule.setContent {
            Box(Modifier.requiredSize(10.dp, 40.dp)) {
                density = LocalDensity.current
                Scaffold(
                    contentWindowInsets = WindowInsets(top = 5.dp, bottom = 3.dp),
                    snackbarHost = {
                        SnackbarHost(
                            hostState = hostState,
                            modifier =
                                Modifier.onGloballyPositioned {
                                    snackbarSize = it.size
                                    snackbarPosition = it.positionInRoot()
                                }
                        )
                    }
                ) {
                    Box(Modifier.requiredSize(10.dp).background(color = Color.White))
                }
            }
        }
        val snackbarBottomOffsetDp =
            with(density!!) { (snackbarPosition!!.y.roundToInt() + snackbarSize!!.height).toDp() }
        assertThat(rule.rootHeight() - snackbarBottomOffsetDp - 3.dp).isLessThan(1.dp)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_insetsTests_FabRespectsInsets() {
        var fabSize: IntSize? = null
        var fabPosition: Offset? = null
        var density: Density? = null
        rule.setContent {
            Box(Modifier.requiredSize(10.dp, 20.dp)) {
                density = LocalDensity.current
                Scaffold(
                    contentWindowInsets = WindowInsets(top = 5.dp, bottom = 3.dp),
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {},
                            modifier =
                                Modifier.onGloballyPositioned {
                                    fabSize = it.size
                                    fabPosition = it.positionInRoot()
                                }
                        ) {
                            Text("Fab")
                        }
                    },
                ) {
                    Box(Modifier.requiredSize(10.dp).background(color = Color.White))
                }
            }
        }
        val fabBottomOffsetDp =
            with(density!!) { (fabPosition!!.y.roundToInt() + fabSize!!.height).toDp() }
        assertThat(rule.rootHeight() - fabBottomOffsetDp - 3.dp).isLessThan(1.dp)
    }

    @Test
    fun scaffold_fabPosition_start() {
        var fabSize: IntSize? = null
        var fabPosition: Offset? = null
        rule.setContent {
            Box(Modifier.requiredSize(200.dp, 200.dp)) {
                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {},
                            modifier =
                                Modifier.onGloballyPositioned {
                                    fabSize = it.size
                                    fabPosition = it.positionInRoot()
                                }
                        ) {
                            Text("Fab")
                        }
                    },
                    floatingActionButtonPosition = FabPosition.Start,
                ) {
                    Box(Modifier.requiredSize(10.dp).background(color = Color.White))
                }
            }
        }
        with(rule.density) {
            assertThat(fabPosition!!.x).isWithin(1f).of(fabSpacing.toPx())
            assertThat(fabPosition!!.y)
                .isWithin(1f)
                .of(200.dp.toPx() - fabSize!!.height - fabSpacing.toPx())
        }
    }

    @Test
    fun scaffold_fabPosition_center() {
        var fabSize: IntSize? = null
        var fabPosition: Offset? = null
        rule.setContent {
            Box(Modifier.requiredSize(200.dp, 200.dp)) {
                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {},
                            modifier =
                                Modifier.onGloballyPositioned {
                                    fabSize = it.size
                                    fabPosition = it.positionInRoot()
                                }
                        ) {
                            Text("Fab")
                        }
                    },
                    floatingActionButtonPosition = FabPosition.Center,
                ) {
                    Box(Modifier.requiredSize(10.dp).background(color = Color.White))
                }
            }
        }
        with(rule.density) {
            assertThat(fabPosition!!.x).isWithin(1f).of((200.dp.toPx() - fabSize!!.width) / 2f)
            assertThat(fabPosition!!.y)
                .isWithin(1f)
                .of(200.dp.toPx() - fabSize!!.height - fabSpacing.toPx())
        }
    }

    @Test
    fun scaffold_fabPosition_end() {
        var fabSize: IntSize? = null
        var fabPosition: Offset? = null
        rule.setContent {
            Box(Modifier.requiredSize(200.dp, 200.dp)) {
                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {},
                            modifier =
                                Modifier.onGloballyPositioned {
                                    fabSize = it.size
                                    fabPosition = it.positionInRoot()
                                }
                        ) {
                            Text("Fab")
                        }
                    },
                    floatingActionButtonPosition = FabPosition.End,
                ) {
                    Box(Modifier.requiredSize(10.dp).background(color = Color.White))
                }
            }
        }
        with(rule.density) {
            assertThat(fabPosition!!.x)
                .isWithin(1f)
                .of(200.dp.toPx() - fabSize!!.width - fabSpacing.toPx())
            assertThat(fabPosition!!.y)
                .isWithin(1f)
                .of(200.dp.toPx() - fabSize!!.height - fabSpacing.toPx())
        }
    }

    // Regression test for b/295536718
    @Test
    fun scaffold_onSizeChanged_calledBeforeLookaheadPlace() {
        var size: IntSize? = null
        var onSizeChangedCount = 0
        var onPlaceCount = 0

        rule.setContent {
            LookaheadScope {
                Scaffold {
                    SubcomposeLayout { constraints ->
                        val measurables =
                            subcompose("second") {
                                Box(
                                    Modifier.size(45.dp).onSizeChanged {
                                        onSizeChangedCount++
                                        size = it
                                    }
                                )
                            }
                        val placeables = measurables.map { it.measure(constraints) }

                        layout(constraints.maxWidth, constraints.maxHeight) {
                            onPlaceCount++
                            assertWithMessage("Expected onSizeChangedCount to be >= 1")
                                .that(onSizeChangedCount)
                                .isAtLeast(1)
                            assertThat(size).isNotNull()
                            placeables.forEach { it.place(0, 0) }
                        }
                    }
                }
            }
        }

        assertWithMessage("Expected placeCount to be >= 1").that(onPlaceCount).isAtLeast(1)
    }

    // Regression test for b/373904168
    @Test
    fun scaffold_topBarHeightChanging_noRecompositionInBody() {
        val testCase = TopBarHeightChangingScaffoldTestCase()
        rule.forGivenTestCase(testCase).performTestWithEventsControl {
            doFrame()
            assertNoPendingChanges()

            assertEquals(1, testCase.tracker.compositions)

            testCase.toggleState()

            doFramesUntilNoChangesPending(maxAmountOfFrames = 1)

            assertEquals(1, testCase.tracker.compositions)
        }
    }

    private fun assertDpIsWithinThreshold(actual: Dp, expected: Dp, threshold: Dp) {
        assertThat(actual.value).isWithin(threshold.value).of(expected.value)
    }
}

private class TopBarHeightChangingScaffoldTestCase : LayeredComposeTestCase(), ToggleableTestCase {

    private lateinit var state: MutableState<Dp>

    val tracker = CompositionTracker()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun MeasuredContent() {
        state = remember { mutableStateOf(0.dp) }
        val paddingValues = remember {
            object : PaddingValues {
                override fun calculateBottomPadding(): Dp = state.value

                override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = 0.dp

                override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = 0.dp

                override fun calculateTopPadding(): Dp = 0.dp
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Title") }, modifier = Modifier.padding(paddingValues))
            },
        ) { contentPadding ->
            tracker.compositions++
            Box(Modifier.padding(contentPadding).fillMaxSize())
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }

    override fun toggleState() {
        state.value = if (state.value == 0.dp) 10.dp else 0.dp
    }
}

/**
 * Immutable as we want to ensure that we always skip recomposition unless the CompositionLocal
 * value inside the function changes.
 */
@Immutable private class CompositionTracker(var compositions: Int = 0)
