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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.tokens.AppBarLargeTokens
import androidx.compose.material3.tokens.AppBarMediumTokens
import androidx.compose.material3.tokens.AppBarSmallTokens
import androidx.compose.material3.tokens.AppBarTokens
import androidx.compose.material3.tokens.BottomAppBarTokens
import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowInsets
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.width
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class AppBarTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun smallTopAppBar_expandsToScreen() {
        rule
            .setMaterialContentForSizeAssertions { TopAppBar(title = { Text("Title") }) }
            .assertHeightIsEqualTo(AppBarSmallTokens.ContainerHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun smallTopAppBar_withTitle() {
        val title = "Title"
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) { TopAppBar(title = { Text(title) }) }
        }
        rule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun smallTopAppBar_withSubtitle() {
        val subtitle = "Subtitle"
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TopAppBar(title = { Text("Title") }, subtitle = { Text(subtitle) })
            }
        }
        rule.onNodeWithText(subtitle).assertIsDisplayed()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun smallTopAppBar_withSubtitleAndCustomColors() {
        val subtitle = "Subtitle"
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TopAppBar(
                    title = { Text("Title", modifier = Modifier.testTag(TitleTestTag)) },
                    subtitle = { Text(subtitle, modifier = Modifier.testTag(SubtitleTestTag)) },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            titleContentColor = Color.Red,
                            subtitleContentColor = Color.Green,
                        ),
                )
            }
        }
        rule.onNodeWithTag(TitleTestTag).captureToImage().assertContainsColor(Color.Red)
        rule.onNodeWithTag(SubtitleTestTag).captureToImage().assertContainsColor(Color.Green)
    }

    @Test
    fun smallTopAppBar_default_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TopAppBar(
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                )
            }
        }
        assertSmallDefaultPositioning()
    }

    @Test
    fun smallTopAppBar_noNavigationIcon_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TopAppBar(
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                )
            }
        }
        assertSmallPositioningWithoutNavigation()
    }

    @Test
    fun smallTopAppBar_centeredWithSubtitle_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TopAppBar(
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    subtitle = { Text("Subtitle", Modifier.testTag(SubtitleTestTag)) },
                    titleHorizontalAlignment = Alignment.CenterHorizontally,
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                )
            }
        }
        assertSmallDefaultPositioning(isCenteredTitle = true)
    }

    // Ensure that the titles are still centered even when there are more actions.
    @Test
    fun smallTopAppBar_centeredWithSubtitle_multiActions_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TopAppBar(
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    subtitle = { Text("Subtitle", Modifier.testTag(SubtitleTestTag)) },
                    titleHorizontalAlignment = Alignment.CenterHorizontally,
                    actions = {
                        FakeIcon(Modifier)
                        // Apply the test tag just to the action at the end. We will test its
                        // position at the assertSmallDefaultPositioning.
                        FakeIcon(Modifier.testTag(ActionsTestTag))
                    },
                )
            }
        }
        assertSmallDefaultPositioning(isCenteredTitle = true)
    }

    @Test
    fun smallTopAppBar_titleDefaultStyle() {
        var textStyle: TextStyle? = null
        var expectedTextStyle: TextStyle? = null
        rule.setMaterialContent(lightColorScheme()) {
            TopAppBar(
                title = {
                    Text("Title")
                    textStyle = LocalTextStyle.current
                    expectedTextStyle = AppBarSmallTokens.TitleFont.value
                }
            )
        }
        assertThat(textStyle).isNotNull()
        assertThat(textStyle).isEqualTo(expectedTextStyle)
    }

    @Test
    fun smallTopAppBar_subtitleDefaultStyle() {
        var textStyle: TextStyle? = null
        var expectedTextStyle: TextStyle? = null
        rule.setMaterialContent(lightColorScheme()) {
            TopAppBar(
                title = { Text("Title") },
                subtitle = {
                    Text("Subtitle")
                    textStyle = LocalTextStyle.current
                    expectedTextStyle = TypographyKeyTokens.LabelMedium.value // TODO tokens
                },
            )
        }
        assertThat(textStyle).isNotNull()
        assertThat(textStyle).isEqualTo(expectedTextStyle)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun smallTopAppBar_contentColor() {
        var titleColor: Color = Color.Unspecified
        var navigationIconColor: Color = Color.Unspecified
        var actionsColor: Color = Color.Unspecified
        var expectedTitleColor: Color = Color.Unspecified
        var expectedNavigationIconColor: Color = Color.Unspecified
        var expectedActionsColor: Color = Color.Unspecified
        var expectedContainerColor: Color = Color.Unspecified

        rule.setMaterialContent(lightColorScheme()) {
            TopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                navigationIcon = {
                    FakeIcon(Modifier.testTag(NavigationIconTestTag))
                    navigationIconColor = LocalContentColor.current
                    expectedNavigationIconColor =
                        TopAppBarDefaults.topAppBarColors().navigationIconContentColor
                    // fraction = 0f to indicate no scroll.
                    expectedContainerColor =
                        TopAppBarDefaults.topAppBarColors()
                            .containerColor(colorTransitionFraction = 0f)
                },
                title = {
                    Text("Title", Modifier.testTag(TitleTestTag))
                    titleColor = LocalContentColor.current
                    expectedTitleColor = TopAppBarDefaults.topAppBarColors().titleContentColor
                },
                actions = {
                    FakeIcon(Modifier.testTag(ActionsTestTag))
                    actionsColor = LocalContentColor.current
                    expectedActionsColor =
                        TopAppBarDefaults.topAppBarColors().actionIconContentColor
                },
            )
        }
        assertThat(navigationIconColor).isNotNull()
        assertThat(titleColor).isNotNull()
        assertThat(actionsColor).isNotNull()
        assertThat(navigationIconColor).isEqualTo(expectedNavigationIconColor)
        assertThat(titleColor).isEqualTo(expectedTitleColor)
        assertThat(actionsColor).isEqualTo(expectedActionsColor)

        rule
            .onNodeWithTag(TopAppBarTestTag)
            .captureToImage()
            .assertContainsColor(expectedContainerColor)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun smallTopAppBar_scrolledContentColor() {
        var expectedScrolledContainerColor: Color = Color.Unspecified
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            TopAppBar(
                title = {
                    Text("Title", Modifier.testTag(TitleTestTag))
                    // fraction = 1f to indicate a scroll.
                    expectedScrolledContainerColor =
                        TopAppBarDefaults.topAppBarColors()
                            .containerColor(colorTransitionFraction = 1f)
                },
                modifier = Modifier.testTag(TopAppBarTestTag),
                scrollBehavior = scrollBehavior,
            )
        }

        // Simulate scrolled content.
        rule.runOnIdle { scrollBehavior.state.contentOffset = -100f }
        rule.waitForIdle()
        rule
            .onNodeWithTag(TopAppBarTestTag)
            .captureToImage()
            .assertContainsColor(expectedScrolledContainerColor)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun smallTopAppBar_scrolledPositioning() {
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        val scrollHeightOffsetDp = 20.dp
        var scrollHeightOffsetPx = 0f

        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            scrollHeightOffsetPx = with(LocalDensity.current) { scrollHeightOffsetDp.toPx() }
            TopAppBar(
                title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                modifier = Modifier.testTag(TopAppBarTestTag),
                scrollBehavior = scrollBehavior,
            )
        }

        // Simulate scrolled content.
        rule.runOnIdle {
            scrollBehavior.state.heightOffset = -scrollHeightOffsetPx
            scrollBehavior.state.contentOffset = -scrollHeightOffsetPx
        }
        rule.waitForIdle()
        rule
            .onNodeWithTag(TopAppBarTestTag)
            .assertHeightIsEqualTo(AppBarSmallTokens.ContainerHeight - scrollHeightOffsetDp)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun smallTopAppBar_customHeight() {
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        val expandedHeightDp = 50.dp
        var expandedHeightPx = 0

        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            expandedHeightPx = with(LocalDensity.current) { expandedHeightDp.roundToPx() }
            TopAppBar(
                title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                modifier = Modifier.testTag(TopAppBarTestTag),
                expandedHeight = expandedHeightDp,
                scrollBehavior = scrollBehavior,
            )
        }

        assertThat(scrollBehavior.state.heightOffsetLimit).isEqualTo(-expandedHeightPx)
        rule.onNodeWithTag(TopAppBarTestTag).assertHeightIsEqualTo(expandedHeightDp)
    }

    @Test
    fun smallTopAppBar_fitsTextIfHeightTooSmall() {
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        val expandedHeightDp = 0.dp

        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            TopAppBar(
                title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                subtitle = { Text("Subtitle", Modifier.testTag(SubtitleTestTag)) },
                modifier = Modifier.testTag(TopAppBarTestTag),
                expandedHeight = expandedHeightDp,
                scrollBehavior = scrollBehavior,
            )
        }

        val titleBounds = rule.onNodeWithTag(TitleTestTag).getBoundsInRoot()
        val subtitleBounds = rule.onNodeWithTag(SubtitleTestTag).getBoundsInRoot()
        val totalHeight = titleBounds.height + subtitleBounds.height
        val totalHeightPx = with(rule.density) { totalHeight.roundToPx() }

        assertThat(scrollBehavior.state.heightOffsetLimit).isEqualTo(-totalHeightPx)
        rule.onNodeWithTag(TopAppBarTestTag).assertHeightIsEqualTo(totalHeight)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun smallTopAppBar_transparentContainerColor() {
        val expectedColorBehindTopAppBar: Color = Color.Red
        rule.setMaterialContent(lightColorScheme()) {
            Box(
                modifier =
                    Modifier.wrapContentHeight()
                        .fillMaxWidth()
                        .background(color = expectedColorBehindTopAppBar)
            ) {
                TopAppBar(
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                )
            }
        }
        rule
            .onNodeWithTag(TopAppBarTestTag)
            .captureToImage()
            .assertContainsColor(expectedColorBehindTopAppBar)
    }

    @Test
    fun centerAlignedTopAppBar_expandsToScreen() {
        rule
            .setMaterialContentForSizeAssertions {
                CenterAlignedTopAppBar(title = { Text("Title") })
            }
            .assertHeightIsEqualTo(AppBarSmallTokens.ContainerHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun centerAlignedTopAppBar_withTitle() {
        val title = "Title"
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                CenterAlignedTopAppBar(title = { Text(title) })
            }
        }
        rule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun centerAlignedTopAppBar_withSubtitle() {
        val subtitle = "Subtitle"
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TopAppBar(title = { Text("Title") }, subtitle = { Text(subtitle) })
            }
        }
        rule.onNodeWithText(subtitle).assertIsDisplayed()
    }

    @Test
    fun centerAlignedTopAppBar_default_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                CenterAlignedTopAppBar(
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                )
            }
        }
        assertSmallDefaultPositioning(isCenteredTitle = true)
    }

    @Test
    fun centerAlignedTopAppBar_default_positioning_respectsWindowInsets() {
        val padding = 10.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                CenterAlignedTopAppBar(
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                    windowInsets = WindowInsets(padding, padding, padding, padding),
                )
            }
        }
        val appBarBounds = rule.onNodeWithTag(TopAppBarTestTag).getUnclippedBoundsInRoot()
        val appBarBottomEdgeY = appBarBounds.top + appBarBounds.height

        rule
            .onNodeWithTag(NavigationIconTestTag)
            // Navigation icon should be 4.dp from the start
            .assertLeftPositionInRootIsEqualTo(AppBarStartAndEndPadding + padding)
            // Navigation icon should be centered within the height of the app bar.
            .assertTopPositionInRootIsEqualTo(
                appBarBottomEdgeY - DefaultAppBarTopAndBottomPadding - padding - FakeIconSize
            )
    }

    @Test
    fun centerAlignedTopAppBar_noNavigationIcon_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                CenterAlignedTopAppBar(
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                )
            }
        }
        assertSmallPositioningWithoutNavigation(isCenteredTitle = true)
    }

    @Test
    fun centerAlignedTopAppBar_longTextDoesNotOverflowToActions() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "This is a very very very very long title",
                            modifier = Modifier.testTag(TitleTestTag),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                )
            }
        }
        val actionsBounds = rule.onNodeWithTag(ActionsTestTag).getUnclippedBoundsInRoot()
        val titleBounds = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot()

        // Check that the title does not render over the actions.
        assertThat(titleBounds.right).isLessThan(actionsBounds.left)
    }

    @Test
    fun centerAlignedTopAppBar_longTextDoesNotOverflowToNavigation() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                CenterAlignedTopAppBar(
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    title = {
                        Text(
                            text = "This is a very very very very long title",
                            modifier = Modifier.testTag(TitleTestTag),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    },
                )
            }
        }
        val navigationIconBounds =
            rule.onNodeWithTag(NavigationIconTestTag).getUnclippedBoundsInRoot()
        val titleBounds = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot()

        // Check that the title does not render over the navigation icon.
        assertThat(titleBounds.left).isGreaterThan(navigationIconBounds.right)
    }

    @Test
    fun centerAlignedTopAppBar_titleDefaultStyle() {
        var textStyle: TextStyle? = null
        var expectedTextStyle: TextStyle? = null
        rule.setMaterialContent(lightColorScheme()) {
            CenterAlignedTopAppBar(
                title = {
                    Text("Title")
                    textStyle = LocalTextStyle.current
                    expectedTextStyle = AppBarSmallTokens.TitleFont.value
                }
            )
        }
        assertThat(textStyle).isNotNull()
        assertThat(textStyle).isEqualTo(expectedTextStyle)
    }

    @Test
    fun centerAlignedTopAppBar_subtitleDefaultStyle() {
        var textStyle: TextStyle? = null
        var expectedTextStyle: TextStyle? = null
        rule.setMaterialContent(lightColorScheme()) {
            TopAppBar(
                title = { Text("Title") },
                subtitle = {
                    Text("Subtitle")
                    textStyle = LocalTextStyle.current
                    expectedTextStyle = TypographyKeyTokens.LabelMedium.value // TODO tokens
                },
            )
        }
        assertThat(textStyle).isNotNull()
        assertThat(textStyle).isEqualTo(expectedTextStyle)
    }

    @Test
    fun centerAlignedTopAppBar_measureWithNonZeroMinWidth() {
        var appBarSize = IntSize.Zero
        rule.setMaterialContent(lightColorScheme()) {
            CenterAlignedTopAppBar(
                modifier =
                    Modifier.layout { measurable, constraints ->
                        val placeable =
                            measurable.measure(constraints.copy(minWidth = constraints.maxWidth))
                        appBarSize = IntSize(placeable.width, placeable.height)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    },
                title = { Text("Title") },
            )
        }

        assertThat(appBarSize).isNotEqualTo(IntSize.Zero)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun centerAlignedTopAppBar_contentColor() {
        var titleColor: Color = Color.Unspecified
        var navigationIconColor: Color = Color.Unspecified
        var actionsColor: Color = Color.Unspecified
        var expectedTitleColor: Color = Color.Unspecified
        var expectedNavigationIconColor: Color = Color.Unspecified
        var expectedActionsColor: Color = Color.Unspecified
        var expectedContainerColor: Color = Color.Unspecified

        rule.setMaterialContent(lightColorScheme()) {
            CenterAlignedTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                navigationIcon = {
                    FakeIcon(Modifier.testTag(NavigationIconTestTag))
                    navigationIconColor = LocalContentColor.current
                    expectedNavigationIconColor =
                        TopAppBarDefaults.topAppBarColors().navigationIconContentColor
                    // fraction = 0f to indicate no scroll.
                    expectedContainerColor =
                        TopAppBarDefaults.topAppBarColors()
                            .containerColor(colorTransitionFraction = 0f)
                },
                title = {
                    Text("Title", Modifier.testTag(TitleTestTag))
                    titleColor = LocalContentColor.current
                    expectedTitleColor = TopAppBarDefaults.topAppBarColors().titleContentColor
                },
                actions = {
                    FakeIcon(Modifier.testTag(ActionsTestTag))
                    actionsColor = LocalContentColor.current
                    expectedActionsColor =
                        TopAppBarDefaults.topAppBarColors().actionIconContentColor
                },
            )
        }
        assertThat(navigationIconColor).isNotNull()
        assertThat(titleColor).isNotNull()
        assertThat(actionsColor).isNotNull()
        assertThat(navigationIconColor).isEqualTo(expectedNavigationIconColor)
        assertThat(titleColor).isEqualTo(expectedTitleColor)
        assertThat(actionsColor).isEqualTo(expectedActionsColor)

        rule
            .onNodeWithTag(TopAppBarTestTag)
            .captureToImage()
            .assertContainsColor(expectedContainerColor)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun centerAlignedTopAppBar_scrolledContentColor() {
        var expectedScrolledContainerColor: Color = Color.Unspecified
        lateinit var scrollBehavior: TopAppBarScrollBehavior

        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            CenterAlignedTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                title = {
                    Text("Title", Modifier.testTag(TitleTestTag))
                    // fraction = 1f to indicate a scroll.
                    expectedScrolledContainerColor =
                        TopAppBarDefaults.topAppBarColors()
                            .containerColor(colorTransitionFraction = 1f)
                },
                scrollBehavior = scrollBehavior,
            )
        }

        // Simulate scrolled content.
        rule.runOnIdle { scrollBehavior.state.contentOffset = -100f }
        rule.waitForIdle()
        rule
            .onNodeWithTag(TopAppBarTestTag)
            .captureToImage()
            .assertContainsColor(expectedScrolledContainerColor)
    }

    @Test
    fun mediumTopAppBar_expandsToScreen() {
        rule
            .setMaterialContentForSizeAssertions {
                MediumTopAppBar(title = { Text("Medium Title") })
            }
            .assertHeightIsEqualTo(AppBarMediumTokens.ContainerHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun mediumTopAppBar_expanded_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                MediumTopAppBar(
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                )
            }
        }

        // The bottom text baseline should be 24.dp from the bottom of the app bar.
        assertMediumOrLargeDefaultPositioning(
            appBarCollapsedHeight = AppBarSmallTokens.ContainerHeight,
            appBarExpandedHeight = AppBarMediumTokens.ContainerHeight,
            bottomTextPadding = 24.dp,
        )
    }

    @Test
    fun mediumTopAppBar_customHeight_expanded_positioning() {
        val collapsedHeightDp = 36.dp
        val expandedHeightDp = 112.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                MediumTopAppBar(
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                    collapsedHeight = collapsedHeightDp,
                    expandedHeight = expandedHeightDp,
                )
            }
        }

        // The bottom text baseline should be 24.dp from the bottom of the app bar.
        assertMediumOrLargeDefaultPositioning(
            appBarCollapsedHeight = collapsedHeightDp,
            appBarExpandedHeight = expandedHeightDp,
            bottomTextPadding = 24.dp,
        )
    }

    @Test
    fun mediumFlexibleTopAppBar_fitsTextIfHeightTooSmall_expanded() {
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        val collapsedHeightDp = TopAppBarDefaults.MediumAppBarCollapsedHeight
        val expandedHeightDp = TopAppBarDefaults.MediumAppBarCollapsedHeight

        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            Box(Modifier.testTag(TopAppBarTestTag)) {
                MediumFlexibleTopAppBar(
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    subtitle = { Text("Subtitle", Modifier.testTag(SubtitleTestTag)) },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                    collapsedHeight = collapsedHeightDp,
                    expandedHeight = expandedHeightDp,
                    scrollBehavior = scrollBehavior,
                )
            }
        }

        val titleBounds = rule.onNodeWithTag(TitleTestTag).getBoundsInRoot()
        val subtitleBounds = rule.onNodeWithTag(SubtitleTestTag).getBoundsInRoot()
        val titlesHeight = titleBounds.height + subtitleBounds.height
        val titlesHeightPx = with(rule.density) { titlesHeight.roundToPx() }

        assertThat(scrollBehavior.state.heightOffsetLimit).isEqualTo(-titlesHeightPx)
        rule.onNodeWithTag(TopAppBarTestTag).assertHeightIsEqualTo(collapsedHeightDp + titlesHeight)
    }

    @Test
    fun mediumFlexibleTopAppBar_fitsTextIfHeightTooSmall_collapsed() {
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        val collapsedHeightDp = 0.dp
        val expandedHeightDp = TopAppBarDefaults.MediumFlexibleAppBarWithSubtitleExpandedHeight
        var expandedHeightPx = 0

        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            expandedHeightPx = with(LocalDensity.current) { expandedHeightDp.roundToPx() }
            Box(Modifier.testTag(TopAppBarTestTag)) {
                MediumFlexibleTopAppBar(
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    subtitle = { Text("Subtitle", Modifier.testTag(SubtitleTestTag)) },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                    collapsedHeight = collapsedHeightDp,
                    expandedHeight = expandedHeightDp,
                    scrollBehavior = scrollBehavior,
                )
            }
        }

        // Simulate a partially collapsed app bar.
        rule.runOnIdle {
            scrollBehavior.state.heightOffset = -expandedHeightPx.toFloat()
            scrollBehavior.state.contentOffset = -expandedHeightPx.toFloat()
        }
        rule.waitForIdle()

        val titleBounds = rule.onNodeWithTag(TitleTestTag).getBoundsInRoot()
        val subtitleBounds = rule.onNodeWithTag(SubtitleTestTag).getBoundsInRoot()
        val titlesHeight = titleBounds.height + subtitleBounds.height

        assertThat(scrollBehavior.state.heightOffsetLimit).isEqualTo(-expandedHeightPx)
        rule.onNodeWithTag(TopAppBarTestTag).assertHeightIsEqualTo(titlesHeight)
    }

    @Test
    fun mediumTopAppBar_scrolled_positioning() {
        val windowInsets = WindowInsets(13.dp, 13.dp, 13.dp, 13.dp)
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                Box(Modifier.testTag(TopAppBarTestTag)) {
                    MediumTopAppBar(
                        navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                        title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                        actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                        scrollBehavior = scrollBehavior,
                        windowInsets = windowInsets,
                    )
                }
            }
        assertMediumOrLargeScrolledHeight(
            AppBarMediumTokens.ContainerHeight,
            AppBarSmallTokens.ContainerHeight,
            windowInsets,
            content,
        )
    }

    @Test
    fun mediumTopAppBar_customHeight_scrolled_positioning() {
        val collapsedHeightDp = 40.dp
        val expandedHeightDp = 120.dp
        val windowInsets = WindowInsets(13.dp, 13.dp, 13.dp, 13.dp)
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                Box(Modifier.testTag(TopAppBarTestTag)) {
                    MediumTopAppBar(
                        navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                        title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                        actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                        collapsedHeight = collapsedHeightDp,
                        expandedHeight = expandedHeightDp,
                        windowInsets = windowInsets,
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        assertMediumOrLargeScrolledHeight(
            appBarMaxHeight = expandedHeightDp,
            appBarMinHeight = collapsedHeightDp,
            windowInsets,
            content,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun mediumTopAppBar_scrolledContainerColor() {
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                MediumTopAppBar(
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    scrollBehavior = scrollBehavior,
                )
            }

        assertMediumOrLargeScrolledColors(
            appBarMaxHeight = AppBarMediumTokens.ContainerHeight,
            appBarMinHeight = AppBarSmallTokens.ContainerHeight,
            titleContentColor = Color.Unspecified,
            subtitleContentColor = Color.Unspecified,
            content = content,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun mediumTopAppBar_scrolledColorsWithCustomTitleTextColor() {
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                MediumTopAppBar(
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    title = {
                        Text(text = "Title", Modifier.testTag(TitleTestTag), color = Color.Green)
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        assertMediumOrLargeScrolledColors(
            appBarMaxHeight = AppBarMediumTokens.ContainerHeight,
            appBarMinHeight = AppBarSmallTokens.ContainerHeight,
            titleContentColor = Color.Green,
            subtitleContentColor = Color.Unspecified,
            content = content,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun mediumFlexibleTopAppBar_scrolledColorsWithCustomTitleAndSubtitleTextColor() {
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                MediumFlexibleTopAppBar(
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    title = {
                        Text(text = "Title", Modifier.testTag(TitleTestTag), color = Color.Green)
                    },
                    subtitle = {
                        Text(
                            text = "Subtitle",
                            Modifier.testTag(SubtitleTestTag),
                            color = Color.Green,
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        assertMediumOrLargeScrolledColors(
            appBarMaxHeight = TopAppBarDefaults.MediumFlexibleAppBarWithSubtitleExpandedHeight,
            appBarMinHeight = AppBarSmallTokens.ContainerHeight,
            titleContentColor = Color.Green,
            subtitleContentColor = Color.Green,
            content = content,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun mediumFlexibleTopAppBar_scrolledColorsWithCustomTitleAndWithoutSubtitleTextColor() {
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                MediumFlexibleTopAppBar(
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    title = {
                        Text(text = "Title", Modifier.testTag(TitleTestTag), color = Color.Green)
                    },
                    subtitle = null,
                    scrollBehavior = scrollBehavior,
                )
            }
        assertMediumOrLargeScrolledColors(
            appBarMaxHeight = TopAppBarDefaults.MediumFlexibleAppBarWithoutSubtitleExpandedHeight,
            appBarMinHeight = AppBarSmallTokens.ContainerHeight,
            titleContentColor = Color.Green,
            subtitleContentColor = Color.Unspecified,
            content = content,
        )
    }

    @Test
    fun topAppBarColors_noNameParams() {
        rule.setContent {
            val colors =
                TopAppBarDefaults.topAppBarColors(
                    Color.Blue,
                    Color.Green,
                    Color.Red,
                    Color.Yellow,
                    Color.Cyan,
                    Color.Magenta,
                )
            assert(colors.containerColor == Color.Blue)
            assert(colors.scrolledContainerColor == Color.Green)
            assert(colors.navigationIconContentColor == Color.Red)
            assert(colors.titleContentColor == Color.Yellow)
            assert(colors.actionIconContentColor == Color.Cyan)
            assert(colors.subtitleContentColor == Color.Magenta)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun mediumTopAppBar_semantics() {
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                MediumTopAppBar(
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    scrollBehavior = scrollBehavior,
                )
            }

        assertMediumOrLargeScrolledSemantics(
            AppBarMediumTokens.ContainerHeight,
            AppBarSmallTokens.ContainerHeight,
            content,
            withSubtitle = false,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun mediumFlexibleTopAppBar_withSubtitle_semantics() {
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                MediumFlexibleTopAppBar(
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    subtitle = { Text("Subtitle", Modifier.testTag(SubtitleTestTag)) },
                    scrollBehavior = scrollBehavior,
                )
            }

        assertMediumOrLargeScrolledSemantics(
            AppBarMediumTokens.ContainerHeight,
            AppBarSmallTokens.ContainerHeight,
            content,
            withSubtitle = true,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun largeTopAppBar_semantics() {
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                LargeTopAppBar(
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    scrollBehavior = scrollBehavior,
                )
            }
        assertMediumOrLargeScrolledSemantics(
            AppBarLargeTokens.ContainerHeight,
            AppBarSmallTokens.ContainerHeight,
            content,
            withSubtitle = false,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun largeFlexibleTopAppBar_withSubtitle_semantics() {
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                LargeFlexibleTopAppBar(
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    subtitle = { Text("Subtitle", Modifier.testTag(SubtitleTestTag)) },
                    scrollBehavior = scrollBehavior,
                )
            }
        assertMediumOrLargeScrolledSemantics(
            AppBarLargeTokens.ContainerHeight,
            AppBarSmallTokens.ContainerHeight,
            content,
            withSubtitle = true,
        )
    }

    @Test
    fun largeTopAppBar_expandsToScreen() {
        rule
            .setMaterialContentForSizeAssertions { LargeTopAppBar(title = { Text("Large Title") }) }
            .assertHeightIsEqualTo(AppBarLargeTokens.ContainerHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun largeTopAppBar_expanded_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                LargeTopAppBar(
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                )
            }
        }

        // The bottom text baseline should be 28.dp from the bottom of the app bar.
        assertMediumOrLargeDefaultPositioning(
            appBarCollapsedHeight = AppBarSmallTokens.ContainerHeight,
            appBarExpandedHeight = AppBarLargeTokens.ContainerHeight,
            bottomTextPadding = 28.dp,
        )
    }

    @Test
    fun largeTopAppBar_customHeight_expanded_positioning() {
        val collapsedHeightDp = 30.dp
        val expandedHeightDp = 130.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                LargeTopAppBar(
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                    collapsedHeight = collapsedHeightDp,
                    expandedHeight = expandedHeightDp,
                )
            }
        }

        // The bottom text baseline should be 28.dp from the bottom of the app bar.
        assertMediumOrLargeDefaultPositioning(
            appBarCollapsedHeight = collapsedHeightDp,
            appBarExpandedHeight = expandedHeightDp,
            bottomTextPadding = 28.dp,
        )
    }

    @Test
    fun largeFlexibleTopAppBar_fitsTextIfHeightTooSmall_expanded() {
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        val collapsedHeightDp = TopAppBarDefaults.LargeAppBarCollapsedHeight
        val expandedHeightDp = TopAppBarDefaults.LargeAppBarCollapsedHeight

        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            Box(Modifier.testTag(TopAppBarTestTag)) {
                LargeFlexibleTopAppBar(
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    subtitle = { Text("Subtitle", Modifier.testTag(SubtitleTestTag)) },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                    collapsedHeight = collapsedHeightDp,
                    expandedHeight = expandedHeightDp,
                    scrollBehavior = scrollBehavior,
                )
            }
        }

        val titleBounds = rule.onNodeWithTag(TitleTestTag).getBoundsInRoot()
        val subtitleBounds = rule.onNodeWithTag(SubtitleTestTag).getBoundsInRoot()
        val titlesHeight = titleBounds.height + subtitleBounds.height
        val titlesHeightPx = with(rule.density) { titlesHeight.roundToPx() }

        assertThat(scrollBehavior.state.heightOffsetLimit).isEqualTo(-titlesHeightPx)
        rule.onNodeWithTag(TopAppBarTestTag).assertHeightIsEqualTo(collapsedHeightDp + titlesHeight)
    }

    @Test
    fun largeFlexibleTopAppBar_fitsTextIfHeightTooSmall_collapsed() {
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        val collapsedHeightDp = 0.dp
        val expandedHeightDp = TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight
        var expandedHeightPx = 0

        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            expandedHeightPx = with(LocalDensity.current) { expandedHeightDp.roundToPx() }
            Box(Modifier.testTag(TopAppBarTestTag)) {
                LargeFlexibleTopAppBar(
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    subtitle = { Text("Subtitle", Modifier.testTag(SubtitleTestTag)) },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                    collapsedHeight = collapsedHeightDp,
                    expandedHeight = expandedHeightDp,
                    scrollBehavior = scrollBehavior,
                )
            }
        }

        // Simulate a partially collapsed app bar.
        rule.runOnIdle {
            scrollBehavior.state.heightOffset = -expandedHeightPx.toFloat()
            scrollBehavior.state.contentOffset = -expandedHeightPx.toFloat()
        }
        rule.waitForIdle()

        val titleBounds = rule.onNodeWithTag(TitleTestTag).getBoundsInRoot()
        val subtitleBounds = rule.onNodeWithTag(SubtitleTestTag).getBoundsInRoot()
        val titlesHeight = titleBounds.height + subtitleBounds.height

        assertThat(scrollBehavior.state.heightOffsetLimit).isEqualTo(-expandedHeightPx)
        rule.onNodeWithTag(TopAppBarTestTag).assertHeightIsEqualTo(titlesHeight)
    }

    @Test
    fun largeTopAppBar_scrolled_positioning() {
        val windowInsets = WindowInsets(4.dp, 4.dp, 4.dp, 4.dp)
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                Box(Modifier.testTag(TopAppBarTestTag)) {
                    LargeTopAppBar(
                        navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                        title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                        actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                        scrollBehavior = scrollBehavior,
                        windowInsets = windowInsets,
                    )
                }
            }
        assertMediumOrLargeScrolledHeight(
            AppBarLargeTokens.ContainerHeight,
            AppBarSmallTokens.ContainerHeight,
            windowInsets,
            content,
        )
    }

    @Test
    fun largeTopAppBar_customHeight_scrolled_positioning() {
        val collapsedHeightDp = 30.dp
        val expandedHeightDp = 130.dp
        val windowInsets = WindowInsets(4.dp, 4.dp, 4.dp, 4.dp)
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                Box(Modifier.testTag(TopAppBarTestTag)) {
                    LargeTopAppBar(
                        navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                        title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                        actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                        collapsedHeight = collapsedHeightDp,
                        expandedHeight = expandedHeightDp,
                        windowInsets = windowInsets,
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        assertMediumOrLargeScrolledHeight(
            appBarMaxHeight = expandedHeightDp,
            appBarMinHeight = collapsedHeightDp,
            windowInsets,
            content,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun largeTopAppBar_scrolledContainerColor() {
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                LargeTopAppBar(
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                    scrollBehavior = scrollBehavior,
                )
            }
        assertMediumOrLargeScrolledColors(
            appBarMaxHeight = AppBarLargeTokens.ContainerHeight,
            appBarMinHeight = AppBarSmallTokens.ContainerHeight,
            titleContentColor = Color.Unspecified,
            subtitleContentColor = Color.Unspecified,
            content = content,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun largeTopAppBar_scrolledColorsWithCustomTitleTextColor() {
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                LargeTopAppBar(
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    title = {
                        Text(text = "Title", Modifier.testTag(TitleTestTag), color = Color.Red)
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        assertMediumOrLargeScrolledColors(
            appBarMaxHeight = AppBarLargeTokens.ContainerHeight,
            appBarMinHeight = AppBarSmallTokens.ContainerHeight,
            titleContentColor = Color.Red,
            subtitleContentColor = Color.Unspecified,
            content = content,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun largeFlexibleTopAppBar_scrolledColorsWithCustomTitleAndSubtitleTextColor() {
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                LargeFlexibleTopAppBar(
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    title = {
                        Text(text = "Title", Modifier.testTag(TitleTestTag), color = Color.Red)
                    },
                    subtitle = {
                        Text(
                            text = "Subtitle",
                            Modifier.testTag(SubtitleTestTag),
                            color = Color.Red,
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        assertMediumOrLargeScrolledColors(
            appBarMaxHeight = TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight,
            appBarMinHeight = AppBarSmallTokens.ContainerHeight,
            titleContentColor = Color.Red,
            subtitleContentColor = Color.Red,
            content = content,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun largeFlexibleTopAppBar_scrolledColorsWithCustomTitleAndWithoutSubtitleTextColor() {
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                LargeFlexibleTopAppBar(
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    title = {
                        Text(text = "Title", Modifier.testTag(TitleTestTag), color = Color.Red)
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        assertMediumOrLargeScrolledColors(
            appBarMaxHeight = TopAppBarDefaults.LargeFlexibleAppBarWithoutSubtitleExpandedHeight,
            appBarMinHeight = AppBarSmallTokens.ContainerHeight,
            titleContentColor = Color.Red,
            subtitleContentColor = Color.Unspecified,
            content = content,
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun topAppBar_enterAlways_allowHorizontalScroll() {
        lateinit var state: LazyListState
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberLazyListState()
            MultiPageContent(TopAppBarDefaults.enterAlwaysScrollBehavior(), state)
        }

        rule.onNodeWithTag(LazyListTag).performTouchInput { swipeLeft() }
        rule.runOnIdle { assertThat(state.firstVisibleItemIndex).isEqualTo(1) }

        rule.onNodeWithTag(LazyListTag).performTouchInput { swipeRight() }
        rule.runOnIdle { assertThat(state.firstVisibleItemIndex).isEqualTo(0) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun topAppBar_exitUntilCollapsed_allowHorizontalScroll() {
        lateinit var state: LazyListState
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberLazyListState()
            MultiPageContent(TopAppBarDefaults.exitUntilCollapsedScrollBehavior(), state)
        }

        rule.onNodeWithTag(LazyListTag).performTouchInput { swipeLeft() }
        rule.runOnIdle { assertThat(state.firstVisibleItemIndex).isEqualTo(1) }

        rule.onNodeWithTag(LazyListTag).performTouchInput { swipeRight() }
        rule.runOnIdle { assertThat(state.firstVisibleItemIndex).isEqualTo(0) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun topAppBar_pinned_allowHorizontalScroll() {
        lateinit var state: LazyListState
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberLazyListState()
            MultiPageContent(TopAppBarDefaults.pinnedScrollBehavior(), state)
        }

        rule.onNodeWithTag(LazyListTag).performTouchInput { swipeLeft() }
        rule.runOnIdle { assertThat(state.firstVisibleItemIndex).isEqualTo(1) }

        rule.onNodeWithTag(LazyListTag).performTouchInput { swipeRight() }
        rule.runOnIdle { assertThat(state.firstVisibleItemIndex).isEqualTo(0) }
    }

    @Test
    fun topAppBar_smallPinnedDraggedAppBar() {
        rule.setMaterialContentForSizeAssertions {
            TopAppBar(
                title = { Text("Title") },
                modifier = Modifier.testTag(TopAppBarTestTag),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            )
        }

        rule
            .onNodeWithTag(TopAppBarTestTag)
            .assertHeightIsEqualTo(AppBarSmallTokens.ContainerHeight)

        // Drag the app bar up half its height.
        rule.onNodeWithTag(TopAppBarTestTag).performTouchInput {
            down(Offset(x = 0f, y = height / 2f))
            moveTo(Offset(x = 0f, y = 0f))
        }
        rule.waitForIdle()
        // Check that the app bar did not collapse.
        rule
            .onNodeWithTag(TopAppBarTestTag)
            .assertHeightIsEqualTo(AppBarSmallTokens.ContainerHeight)
    }

    @Test
    fun topAppBar_mediumDraggedAppBar() {
        rule.setMaterialContentForSizeAssertions {
            MediumTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                title = { Text("Title") },
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
            )
        }

        rule
            .onNodeWithTag(TopAppBarTestTag)
            .assertHeightIsEqualTo(AppBarMediumTokens.ContainerHeight)

        // Drag up the app bar.
        rule.onNodeWithTag(TopAppBarTestTag).performTouchInput {
            down(Offset(x = 0f, y = height - 20f))
            moveTo(Offset(x = 0f, y = 0f))
        }
        rule.waitForIdle()
        // Check that the app bar collapsed to its small size constraints (i.e.
        // AppBarSmallTokens.ContainerHeight).
        rule
            .onNodeWithTag(TopAppBarTestTag)
            .assertHeightIsEqualTo(AppBarSmallTokens.ContainerHeight)
    }

    @Test
    fun topAppBar_dragSnapToCollapsed() {
        rule.setMaterialContentForSizeAssertions {
            LargeTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                title = { Text("Title") },
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
            )
        }

        rule
            .onNodeWithTag(TopAppBarTestTag)
            .assertHeightIsEqualTo(AppBarLargeTokens.ContainerHeight)

        // Slightly drag up the app bar.
        rule.onNodeWithTag(TopAppBarTestTag).performTouchInput {
            down(Offset(x = 0f, y = height - 20f))
            moveTo(Offset(x = 0f, y = height - 40f))
            up()
        }
        rule.waitForIdle()

        // Check that the app bar returned to its expanded size (i.e. fully expanded).
        rule
            .onNodeWithTag(TopAppBarTestTag)
            .assertHeightIsEqualTo(AppBarLargeTokens.ContainerHeight)

        // Drag up the app bar to the point it should continue to collapse after.
        rule.onNodeWithTag(TopAppBarTestTag).performTouchInput {
            down(Offset(x = 0f, y = height - 20f))
            moveTo(Offset(x = 0f, y = 40f))
            up()
        }
        rule.waitForIdle()

        // Check that the app bar collapsed to its small size constraints (i.e.
        // AppBarSmallTokens.ContainerHeight).
        rule
            .onNodeWithTag(TopAppBarTestTag)
            .assertHeightIsEqualTo(AppBarSmallTokens.ContainerHeight)
    }

    @Test
    fun topAppBar_dragWithSnapDisabled() {
        rule.setMaterialContentForSizeAssertions {
            LargeTopAppBar(
                modifier = Modifier.testTag(TopAppBarTestTag),
                title = { Text("Title") },
                scrollBehavior =
                    TopAppBarDefaults.exitUntilCollapsedScrollBehavior(snapAnimationSpec = null),
            )
        }

        // Check that the app bar stayed at its position (i.e. its bounds are with a smaller height)
        val boundsBefore = rule.onNodeWithTag(TopAppBarTestTag).getBoundsInRoot()
        AppBarLargeTokens.ContainerHeight.assertIsEqualTo(
            expected = boundsBefore.height,
            subject = "container height",
        )
        // Slightly drag up the app bar.
        rule.onNodeWithTag(TopAppBarTestTag).performTouchInput {
            down(Offset(x = 100f, y = height - 20f))
            moveTo(Offset(x = 100f, y = height - 100f))
            up()
        }
        rule.waitForIdle()

        // Check that the app bar did not snap back to its fully expanded height, or collapsed to
        // its collapsed height.
        val boundsAfter = rule.onNodeWithTag(TopAppBarTestTag).getBoundsInRoot()
        assertThat(AppBarLargeTokens.ContainerHeight).isGreaterThan(boundsAfter.height)
        assertThat(AppBarSmallTokens.ContainerHeight).isLessThan(boundsAfter.height)
    }

    @Test
    fun topAppBar_enterAlways_scrollingAndContentMovement() {
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        lateinit var state: LazyListState
        var appBarHeightPx = 0f
        rule.setMaterialContentForSizeAssertions {
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            state = rememberLazyListState()
            appBarHeightPx = with(rule.density) { AppBarSmallTokens.ContainerHeight.toPx() }
            Scaffold(
                modifier = Modifier.fillMaxSize().consumeWindowInsets(WindowInsets.systemBars),
                topBar = { TopAppBar(title = { Text("Title") }, scrollBehavior = scrollBehavior) },
            ) { paddingValues ->
                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .padding(paddingValues)
                            .testTag(LazyListTag),
                    state = state,
                ) {
                    items(100) { i ->
                        Text(
                            modifier =
                                Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp),
                            text = "Item $i",
                        )
                    }
                }
            }
        }

        // Swipe up to scroll the content and collapse the top app bar.
        rule.onNodeWithTag(LazyListTag).performTouchInput {
            swipeUp(startY = height - 200f, endY = height - 1000f)
        }
        rule.waitForIdle()

        // Store a tracked visible item's top offset. We set the tracked item to be the third
        // visible one (which helps deflake the test on smaller devices).
        val trackedItemIndex = state.layoutInfo.visibleItemsInfo.first().index + 2
        val trackedItemTopBeforeExpansion =
            rule.onNodeWithText("Item $trackedItemIndex").getBoundsInRoot().top

        // Swipe down to trigger a top app bar expansion without scrolling much the content.
        // Do a slow swipe so the list doesn't fling.
        rule.onNodeWithTag(LazyListTag).performTouchInput {
            swipeDown(
                startY = height - 1000f,
                endY = height - (1000f - appBarHeightPx / 1.5f),
                durationMillis = 1000L,
            )
        }
        rule.waitForIdle()

        // Asserts that the tracked item has moved along with the expansion of the top app bar.
        rule
            .onNodeWithText("Item $trackedItemIndex")
            .assertTopPositionInRootIsEqualTo(
                trackedItemTopBeforeExpansion + AppBarSmallTokens.ContainerHeight
            )
    }

    @Test
    fun topAppBar_enterAlways_reverseLayout_scrollingAndContentMovement() {
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        lateinit var state: LazyListState
        var appBarHeightPx = 0f

        rule.setMaterialContentForSizeAssertions {
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(reverseLayout = true)
            state = rememberLazyListState()
            appBarHeightPx = with(rule.density) { AppBarSmallTokens.ContainerHeight.toPx() }
            Scaffold(
                modifier = Modifier.fillMaxSize().consumeWindowInsets(WindowInsets.systemBars),
                topBar = {
                    TopAppBar(
                        title = { Text("Title") },
                        modifier = Modifier.testTag(TopAppBarTestTag),
                        scrollBehavior = scrollBehavior,
                    )
                },
            ) { paddingValues ->
                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .padding(paddingValues)
                            .testTag(LazyListTag),
                    state = state,
                    reverseLayout = true,
                ) {
                    items(100) { i ->
                        Text(
                            modifier =
                                Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp),
                            text = "Item $i",
                        )
                    }
                }
            }
        }

        // Swipe down to scroll the content in the reverse layout.
        rule.onNodeWithTag(LazyListTag).performTouchInput {
            swipeDown(startY = height - 1000f, endY = height - 300f)
        }
        rule.waitForIdle()

        // Swipe up to trigger a top app bar collapse without scrolling much the content.
        rule.onNodeWithTag(LazyListTag).performTouchInput {
            down(Offset(x = width / 2f, y = height / 2f))
            moveTo(Offset(x = width / 2f, y = height / 2f - appBarHeightPx + 50))
        }
        rule.waitForIdle()

        // Asserts that the first item has moved along with the collapsing of the top app bar.
        val newTopVisibleItemIndex = state.layoutInfo.visibleItemsInfo.last().index
        val bottomAppBarWhileCollapsing =
            rule.onNodeWithTag(TopAppBarTestTag).getBoundsInRoot().bottom
        val topVisibleItemTopWhileCollapsing =
            rule.onNodeWithText("Item $newTopVisibleItemIndex").getBoundsInRoot().top
        topVisibleItemTopWhileCollapsing.assertIsEqualTo(
            expected = bottomAppBarWhileCollapsing,
            subject = "Top item comparison to bottom app bar",
        )
    }

    @Test
    fun state_restoresTopAppBarState() {
        val restorationTester = StateRestorationTester(rule)
        var topAppBarState: TopAppBarState? = null
        restorationTester.setContent { topAppBarState = rememberTopAppBarState() }

        rule.runOnIdle {
            topAppBarState!!.heightOffsetLimit = -350f
            topAppBarState!!.heightOffset = -300f
            topAppBarState!!.contentOffset = -550f
        }

        topAppBarState = null

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(topAppBarState!!.heightOffsetLimit).isEqualTo(-350f)
            assertThat(topAppBarState!!.heightOffset).isEqualTo(-300f)
            assertThat(topAppBarState!!.contentOffset).isEqualTo(-550f)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun topAppBar_intrinsicHeight() {
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        val expandedHeightDp = 50.dp
        var expandedHeightPx = 0

        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            expandedHeightPx = with(LocalDensity.current) { expandedHeightDp.roundToPx() }
            Column {
                Row(modifier = Modifier.height(IntrinsicSize.Min).testTag(RowTestTag + 0)) {
                    TopAppBar(
                        title = { Text("Title") },
                        expandedHeight = expandedHeightDp,
                        scrollBehavior = scrollBehavior,
                    )
                }
                Row(modifier = Modifier.height(IntrinsicSize.Max).testTag(RowTestTag + 1)) {
                    TopAppBar(
                        title = { Text("Title") },
                        expandedHeight = expandedHeightDp,
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        }

        assertThat(scrollBehavior.state.heightOffsetLimit).isEqualTo(-expandedHeightPx)
        // Expecting the row's intrinsic height to be the same as the expanded height whether a
        // height(IntrinsicSize.Max) or height(IntrinsicSize.Min) is used.
        rule.onNodeWithTag(RowTestTag + 0).assertHeightIsEqualTo(expandedHeightDp)
        rule.onNodeWithTag(RowTestTag + 1).assertHeightIsEqualTo(expandedHeightDp)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun topAppBar_intrinsicWidth() {
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        // totalHorizontalPadding = 4 at navigation start, 4 before and after the title, and 4 at
        // the actions end.
        val totalHorizontalPadding = 16.dp
        val expandedHeightDp = 50.dp
        var expandedHeightPx = 0
        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            expandedHeightPx = with(LocalDensity.current) { expandedHeightDp.roundToPx() }
            Column {
                Row(modifier = Modifier.width(IntrinsicSize.Min).testTag(RowTestTag + 0)) {
                    TopAppBar(
                        modifier = Modifier.fillMaxWidth(),
                        navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                        title = { Text("Title", Modifier.testTag(TitleTestTag)) },
                        actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                        expandedHeight = expandedHeightDp,
                        scrollBehavior = scrollBehavior,
                    )
                }
                Row(modifier = Modifier.width(IntrinsicSize.Max).testTag(RowTestTag + 1)) {
                    TopAppBar(
                        modifier = Modifier.fillMaxWidth(),
                        navigationIcon = { FakeIcon(Modifier) },
                        title = { Text("Title") },
                        actions = { FakeIcon(Modifier) },
                        expandedHeight = expandedHeightDp,
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        }

        assertThat(scrollBehavior.state.heightOffsetLimit).isEqualTo(-expandedHeightPx)
        // Expecting the row's intrinsic width to be the same as the expanded width whether a
        // width(IntrinsicSize.Max) or width(IntrinsicSize.Min) is used.
        val intrinsicWidth =
            totalHorizontalPadding +
                rule.onNodeWithTag(NavigationIconTestTag).getBoundsInRoot().width +
                rule.onNodeWithTag(TitleTestTag).getBoundsInRoot().width +
                rule.onNodeWithTag(ActionsTestTag).getBoundsInRoot().width
        rule
            .onNodeWithTag(RowTestTag + 0)
            .getBoundsInRoot()
            .width
            .assertIsEqualTo(
                expected = intrinsicWidth,
                subject = "intrinsic width",
                tolerance = 1.dp,
            )
        rule
            .onNodeWithTag(RowTestTag + 1)
            .getBoundsInRoot()
            .width
            .assertIsEqualTo(
                expected = intrinsicWidth,
                subject = "intrinsic width",
                tolerance = 1.dp,
            )
    }

    @Test
    fun topAppBar_customTwoRows_expanded() {
        val collapsedHeightDp = 36.dp
        val expandedHeightDp = 112.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TwoRowsTopAppBar(
                    title = { expanded ->
                        if (expanded) {
                            Text("Expanded Title", Modifier.testTag(TitleTestTag))
                        } else {
                            Text("Collapsed Title", Modifier.testTag(TitleTestTag))
                        }
                    },
                    navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                    actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                    collapsedHeight = collapsedHeightDp,
                    expandedHeight = expandedHeightDp,
                )
            }
        }

        assertMediumOrLargeDefaultPositioning(
            appBarCollapsedHeight = collapsedHeightDp,
            appBarExpandedHeight = expandedHeightDp,
        )
    }

    @Test
    fun topAppBar_customTwoRows_scrolled_positioning() {
        val collapsedHeightDp = 40.dp
        val expandedHeightDp = 120.dp
        val windowInsets = WindowInsets(13.dp, 13.dp, 13.dp, 13.dp)
        val content =
            @Composable { scrollBehavior: TopAppBarScrollBehavior? ->
                Box(Modifier.testTag(TopAppBarTestTag)) {
                    TwoRowsTopAppBar(
                        title = { expanded ->
                            if (expanded) {
                                Text("Expanded Title", Modifier.testTag(TitleTestTag))
                            } else {
                                Text("Collapsed Title", Modifier.testTag(TitleTestTag))
                            }
                        },
                        navigationIcon = { FakeIcon(Modifier.testTag(NavigationIconTestTag)) },
                        actions = { FakeIcon(Modifier.testTag(ActionsTestTag)) },
                        collapsedHeight = collapsedHeightDp,
                        expandedHeight = expandedHeightDp,
                        windowInsets = windowInsets,
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        assertMediumOrLargeScrolledHeight(
            appBarMaxHeight = expandedHeightDp,
            appBarMinHeight = collapsedHeightDp,
            windowInsets,
            content,
        )
    }

    @Test
    fun topAppBar_correctlyPadsWhenParentHandlesInsetsAndContentPaddingIsUsed() {
        val appBarHeightDp = AppBarSmallTokens.ContainerHeight
        val siblingBoxHeightDp = 40.dp
        val topAppBarContentPaddingTopDp = 20.dp
        val statusBarHeightDp = 10.dp

        // The test illustrates "sibling problem": inset consumption isn't shared between sibling
        // components. If one uses an inset, its siblings aren't aware. In the example  the parent
        // consumes insets.
        rule.setMaterialContent(lightColorScheme()) {
            val statusBarInsets =
                Insets.of(0, with(LocalDensity.current) { statusBarHeightDp.roundToPx() }, 0, 0)
            val windowInsets =
                WindowInsetsCompat.Builder().setInsets(Type.statusBars(), statusBarInsets).build()
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowInsets(windowInsets)) {
                Column(
                    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    TopAppBar(
                        modifier = Modifier.testTag(TopAppBarTestTag),
                        title = { Text("Title") },
                        contentPadding = PaddingValues(top = topAppBarContentPaddingTopDp),
                    )
                    Box(
                        modifier =
                            Modifier.testTag(BoxTestTag)
                                .fillMaxWidth()
                                .height(siblingBoxHeightDp)
                                .windowInsetsPadding(WindowInsets.statusBars),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Sibling Box")
                    }
                }
            }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(TopAppBarTestTag)
            .assertTopPositionInRootIsEqualTo(statusBarHeightDp)
            .assertHeightIsEqualTo(appBarHeightDp + topAppBarContentPaddingTopDp)
        rule
            .onNodeWithTag(BoxTestTag)
            .assertHeightIsEqualTo(siblingBoxHeightDp)
            .assertTopPositionInRootIsEqualTo(
                topAppBarContentPaddingTopDp + appBarHeightDp + statusBarHeightDp
            )
    }

    @Test
    @Ignore("b/422735600")
    fun bottomAppBarWithFAB_heightIsFromSpec() {
        rule
            .setMaterialContentForSizeAssertions {
                BottomAppBar(
                    actions = {},
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { /* do something */ },
                            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        ) {
                            Icon(Icons.Filled.Add, "Localized description")
                        }
                    },
                )
            }
            .assertHeightIsEqualTo(BottomAppBarTokens.ContainerHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    @Ignore("b/422735600")
    fun bottomAppBarWithCustomArrangement_heightIsFromSpec() {
        rule
            .setMaterialContentForSizeAssertions {
                FlexibleBottomAppBar(
                    horizontalArrangement = BottomAppBarDefaults.FlexibleFixedHorizontalArrangement,
                    content = {},
                )
            }
            .assertHeightIsEqualTo(BottomAppBarDefaults.FlexibleBottomAppBarHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    @Ignore("b/422735600")
    fun bottomAppBarWithCustomHeight() {
        val height = 128.dp
        rule
            .setMaterialContentForSizeAssertions {
                FlexibleBottomAppBar(
                    horizontalArrangement = BottomAppBarDefaults.FlexibleFixedHorizontalArrangement,
                    expandedHeight = height,
                    content = {},
                )
            }
            .assertHeightIsEqualTo(height)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun bottomAppBarWithFAB_respectsWindowInsets() {
        rule
            .setMaterialContentForSizeAssertions {
                BottomAppBar(
                    actions = {},
                    windowInsets = WindowInsets(10.dp, 10.dp, 10.dp, 10.dp),
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { /* do something */ },
                            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        ) {
                            Icon(Icons.Filled.Add, "Localized description")
                        }
                    },
                )
            }
            .assertHeightIsEqualTo(BottomAppBarTokens.ContainerHeight + 20.dp)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun bottomAppBar_FABshown_whenActionsOverflowRow() {
        rule.setMaterialContent(lightColorScheme()) {
            BottomAppBar(
                actions = { repeat(20) { FakeIcon(Modifier) } },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { /* do something */ },
                        modifier = Modifier.testTag("FAB"),
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                    ) {
                        Icon(Icons.Filled.Add, "Localized description")
                    }
                },
            )
        }
        rule.onNodeWithTag("FAB").assertIsDisplayed()
    }

    @Test
    @Ignore("b/422735600")
    fun bottomAppBar_widthExpandsToScreen() {
        rule
            .setMaterialContentForSizeAssertions { BottomAppBar {} }
            .assertHeightIsEqualTo(BottomAppBarTokens.ContainerHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    @Ignore("b/422746273")
    fun bottomAppBar_default_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            BottomAppBar(Modifier.testTag("bar")) { FakeIcon(Modifier.testTag("icon")) }
        }

        val appBarBounds = rule.onNodeWithTag("bar").getUnclippedBoundsInRoot()
        val appBarBottomEdgeY = appBarBounds.top + appBarBounds.height

        val defaultPadding = BottomAppBarDefaults.ContentPadding
        rule
            .onNodeWithTag("icon")
            // Child icon should be 4.dp from the start
            .assertLeftPositionInRootIsEqualTo(AppBarStartAndEndPadding)
            // Child icon should be 10.dp from the top
            .assertTopPositionInRootIsEqualTo(
                defaultPadding.calculateTopPadding() +
                    (appBarBottomEdgeY - defaultPadding.calculateTopPadding() - FakeIconSize) / 2
            )
    }

    @Test
    @Ignore("b/422746273")
    fun bottomAppBar_default_positioning_respectsContentPadding() {
        val topPadding = 5.dp
        rule.setMaterialContent(lightColorScheme()) {
            BottomAppBar(
                Modifier.testTag("bar"),
                contentPadding = PaddingValues(top = topPadding, start = 3.dp),
            ) {
                FakeIcon(Modifier.testTag("icon"))
            }
        }

        val appBarBounds = rule.onNodeWithTag("bar").getUnclippedBoundsInRoot()
        val appBarBottomEdgeY = appBarBounds.top + appBarBounds.height

        rule
            .onNodeWithTag("icon")
            // Child icon should be 4.dp from the start
            .assertLeftPositionInRootIsEqualTo(3.dp)
            // Child icon should be 10.dp from the top
            .assertTopPositionInRootIsEqualTo(
                (appBarBottomEdgeY - topPadding - FakeIconSize) / 2 + 5.dp
            )
    }

    @Test
    fun bottomAppBarWithFAB_default_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            BottomAppBar(
                actions = {},
                Modifier.testTag("bar"),
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { /* do something */ },
                        modifier = Modifier.testTag("FAB"),
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                    ) {
                        Icon(Icons.Filled.Add, "Localized description")
                    }
                },
            )
        }

        val appBarBounds = rule.onNodeWithTag("bar").getUnclippedBoundsInRoot()

        val fabBounds = rule.onNodeWithTag("FAB").getUnclippedBoundsInRoot()

        rule
            .onNodeWithTag("FAB")
            // FAB should be 16.dp from the end
            .assertLeftPositionInRootIsEqualTo(appBarBounds.width - 16.dp - fabBounds.width)
            // FAB should be 12.dp from the top
            .assertTopPositionInRootIsEqualTo(12.dp)
    }

    @Test
    @Ignore("b/422746273")
    fun bottomAppBar_exitAlways_scaffoldWithFAB_default_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            val scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                bottomBar = {
                    BottomAppBar(
                        modifier = Modifier.testTag(BottomAppBarTestTag),
                        scrollBehavior = scrollBehavior,
                    ) {}
                },
                floatingActionButton = {
                    FloatingActionButton(
                        modifier = Modifier.testTag("FAB").offset(y = 4.dp),
                        onClick = { /* do something */ },
                    ) {}
                },
                floatingActionButtonPosition = FabPosition.EndOverlay,
            ) {}
        }

        val appBarBounds = rule.onNodeWithTag(BottomAppBarTestTag).getUnclippedBoundsInRoot()
        val fabBounds = rule.onNodeWithTag("FAB").getUnclippedBoundsInRoot()
        rule
            .onNodeWithTag("FAB")
            // FAB should be 16.dp from the end
            .assertLeftPositionInRootIsEqualTo(appBarBounds.width - 16.dp - fabBounds.width)
            // FAB should be 12.dp from the bottom
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - 12.dp - fabBounds.height)
    }

    @Test
    @Ignore("b/422735600")
    fun bottomAppBar_exitAlways_scaffoldWithFAB_scrolled_positioning() {
        lateinit var scrollBehavior: BottomAppBarScrollBehavior
        val scrollHeightOffsetDp = 20.dp
        var scrollHeightOffsetPx = 0f

        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
            scrollHeightOffsetPx = with(LocalDensity.current) { scrollHeightOffsetDp.toPx() }
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                bottomBar = {
                    BottomAppBar(
                        modifier = Modifier.testTag(BottomAppBarTestTag),
                        scrollBehavior = scrollBehavior,
                    ) {}
                },
                floatingActionButton = {
                    FloatingActionButton(
                        modifier = Modifier.testTag("FAB").offset(y = 4.dp),
                        onClick = { /* do something */ },
                    ) {}
                },
                floatingActionButtonPosition = FabPosition.EndOverlay,
            ) {}
        }

        // Simulate scrolled content.
        rule.runOnIdle {
            scrollBehavior.state.heightOffset = -scrollHeightOffsetPx
            scrollBehavior.state.contentOffset = -scrollHeightOffsetPx
        }
        rule.waitForIdle()
        rule
            .onNodeWithTag(BottomAppBarTestTag)
            .assertHeightIsEqualTo(BottomAppBarTokens.ContainerHeight - scrollHeightOffsetDp)

        val appBarBounds = rule.onNodeWithTag(BottomAppBarTestTag).getUnclippedBoundsInRoot()
        val fabBounds = rule.onNodeWithTag("FAB").getUnclippedBoundsInRoot()
        rule
            .onNodeWithTag("FAB")
            // FAB should be 16.dp from the end
            .assertLeftPositionInRootIsEqualTo(appBarBounds.width - 16.dp - fabBounds.width)
            // FAB should be 12.dp from the bottom
            .assertTopPositionInRootIsEqualTo(rule.rootHeight() - 12.dp - fabBounds.height)
    }

    @Test
    fun bottomAppBar_exitAlways_allowHorizontalScroll() {
        lateinit var state: LazyListState
        rule.setMaterialContent(lightColorScheme()) {
            state = rememberLazyListState()
            MultiPageContent(BottomAppBarDefaults.exitAlwaysScrollBehavior(), state)
        }

        rule.onNodeWithTag(LazyListTag).performTouchInput { swipeLeft() }
        rule.runOnIdle { assertThat(state.firstVisibleItemIndex).isEqualTo(1) }

        rule.onNodeWithTag(LazyListTag).performTouchInput { swipeRight() }
        rule.runOnIdle { assertThat(state.firstVisibleItemIndex).isEqualTo(0) }
    }

    @Test
    fun bottomAppBar_exitAlways_outOfRangeOffsetHandled() {
        lateinit var scrollBehavior: BottomAppBarScrollBehavior

        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
            // Set negative initial height offset to emulate out of range exception.
            scrollBehavior.state.heightOffset = -1000f
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                bottomBar = {
                    BottomAppBar(
                        modifier = Modifier.testTag(BottomAppBarTestTag),
                        scrollBehavior = scrollBehavior,
                    ) {}
                },
            ) { contentPadding ->
                Box(modifier = Modifier.padding(contentPadding))
            }
        }

        rule.onNodeWithTag(BottomAppBarTestTag).assertHeightIsEqualTo(0.dp)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MultiPageContent(scrollBehavior: TopAppBarScrollBehavior, state: LazyListState) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = { Text(text = "Title") },
                    modifier = Modifier.testTag(TopAppBarTestTag),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            LazyRow(Modifier.fillMaxSize().testTag(LazyListTag), state) {
                items(2) { page ->
                    LazyColumn(
                        modifier = Modifier.fillParentMaxSize(),
                        contentPadding = contentPadding,
                    ) {
                        items(50) {
                            Text(
                                modifier = Modifier.fillParentMaxWidth(),
                                text = "Item #$page x $it",
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MultiPageContent(scrollBehavior: BottomAppBarScrollBehavior, state: LazyListState) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            bottomBar = {
                BottomAppBar(
                    modifier = Modifier.testTag(BottomAppBarTestTag),
                    scrollBehavior = scrollBehavior,
                ) {}
            },
        ) { contentPadding ->
            LazyRow(Modifier.fillMaxSize().testTag(LazyListTag), state) {
                items(2) { page ->
                    LazyColumn(
                        modifier = Modifier.fillParentMaxSize(),
                        contentPadding = contentPadding,
                    ) {
                        items(50) {
                            Text(
                                modifier = Modifier.fillParentMaxWidth(),
                                text = "Item #$page x $it",
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks the app bar's components positioning when it's a [TopAppBar], a
     * [CenterAlignedTopAppBar], or a larger app bar that is scrolled up and collapsed into a small
     * configuration and there is no navigation icon.
     */
    private fun assertSmallPositioningWithoutNavigation(isCenteredTitle: Boolean = false) {
        val appBarBounds = rule.onNodeWithTag(TopAppBarTestTag).getUnclippedBoundsInRoot()
        val titleBounds = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot()

        val titleNode = rule.onNodeWithTag(TitleTestTag)
        // Title should be vertically centered
        titleNode.assertTopPositionInRootIsEqualTo((appBarBounds.height - titleBounds.height) / 2)
        if (isCenteredTitle) {
            // Title should be horizontally centered
            titleNode.assertLeftPositionInRootIsEqualTo(
                (appBarBounds.width - titleBounds.width) / 2
            )
        } else {
            // Title should now be placed 16.dp from the start, as there is no navigation icon
            // 4.dp padding for the whole app bar + 12.dp inset
            titleNode.assertLeftPositionInRootIsEqualTo(4.dp + 12.dp)
        }

        rule
            .onNodeWithTag(ActionsTestTag)
            // Action should still be placed at the end
            .assertLeftPositionInRootIsEqualTo(expectedActionPosition(appBarBounds.width))
    }

    /**
     * Checks the app bar's components positioning when it's a [TopAppBar] or a
     * [CenterAlignedTopAppBar].
     */
    private fun assertSmallDefaultPositioning(isCenteredTitle: Boolean = false) {
        val appBarBounds = rule.onNodeWithTag(TopAppBarTestTag).getUnclippedBoundsInRoot()
        val titleBounds = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot()
        val appBarBottomEdgeY = appBarBounds.top + appBarBounds.height

        rule
            .onNodeWithTag(NavigationIconTestTag)
            // Navigation icon should be 4.dp from the start
            .assertLeftPositionInRootIsEqualTo(AppBarStartAndEndPadding)
            // Navigation icon should be centered within the height of the app bar.
            .assertTopPositionInRootIsEqualTo(
                appBarBottomEdgeY - DefaultAppBarTopAndBottomPadding - FakeIconSize
            )

        val titleNode = rule.onNodeWithTag(TitleTestTag)
        val subtitleNode = rule.onNodeWithTag(SubtitleTestTag)
        val subtitleBounds =
            with(subtitleNode) {
                if (isDisplayed()) {
                    getUnclippedBoundsInRoot()
                } else {
                    DpRect(0.dp, 0.dp, 0.dp, 0.dp)
                }
            }
        // Title should be vertically centered
        titleNode.assertTopPositionInRootIsEqualTo(
            (appBarBounds.height - titleBounds.height - subtitleBounds.height) / 2
        )
        if (isCenteredTitle) {
            // Title should be horizontally centered
            titleNode.assertLeftPositionInRootIsEqualTo(
                (appBarBounds.width - titleBounds.width) / 2
            )
            if (subtitleNode.isDisplayed()) {
                // Subtitle should be horizontally centered
                subtitleNode.assertLeftPositionInRootIsEqualTo(
                    (appBarBounds.width - subtitleBounds.width) / 2
                )
            }
        } else {
            // Title should be 56.dp from the start
            // 4.dp padding for the whole app bar + 48.dp icon size + 4.dp title padding.
            titleNode.assertLeftPositionInRootIsEqualTo(4.dp + FakeIconSize + 4.dp)
        }

        rule
            .onNodeWithTag(ActionsTestTag)
            // Action should be placed at the end
            .assertLeftPositionInRootIsEqualTo(expectedActionPosition(appBarBounds.width))
            // Action should be 8.dp from the top
            .assertTopPositionInRootIsEqualTo(
                appBarBottomEdgeY - DefaultAppBarTopAndBottomPadding - FakeIconSize
            )
    }

    /**
     * Checks the app bar's components positioning when it's a [MediumTopAppBar], [LargeTopAppBar],
     * or a [TwoRowsTopAppBar].
     */
    private fun assertMediumOrLargeDefaultPositioning(
        appBarCollapsedHeight: Dp,
        appBarExpandedHeight: Dp,
        bottomTextPadding: Dp = Dp.Unspecified,
    ) {
        val appBarBounds = rule.onNodeWithTag(TopAppBarTestTag).getUnclippedBoundsInRoot()
        appBarBounds.height.assertIsEqualTo(appBarExpandedHeight, "top app bar height")

        // Expecting the title composable to be reused for the top and bottom rows of the top app
        // bar, so obtaining the node with the title tag should return two nodes, one for each row.
        val allTitleNodes = rule.onAllNodesWithTag(TitleTestTag, true)
        allTitleNodes.assertCountEquals(2)
        val topTitleNode = allTitleNodes.onFirst()
        val bottomTitleNode = allTitleNodes.onLast()

        val topTitleBounds = topTitleNode.getUnclippedBoundsInRoot()
        val bottomTitleBounds = bottomTitleNode.getUnclippedBoundsInRoot()
        val topAppBarBottomEdgeY = appBarBounds.top + appBarCollapsedHeight
        val bottomAppBarBottomEdgeY = appBarBounds.top + appBarBounds.height

        val topAndBottomPadding = (appBarCollapsedHeight - FakeIconSize) / 2
        rule
            .onNodeWithTag(NavigationIconTestTag)
            // Navigation icon should be 4.dp from the start
            .assertLeftPositionInRootIsEqualTo(AppBarStartAndEndPadding)
            // Navigation icon should be centered within the height of the top part of the app bar.
            .assertTopPositionInRootIsEqualTo(
                topAppBarBottomEdgeY - topAndBottomPadding - FakeIconSize
            )

        rule
            .onNodeWithTag(ActionsTestTag)
            // Action should be placed at the end
            .assertLeftPositionInRootIsEqualTo(expectedActionPosition(appBarBounds.width))
            // Action should be 8.dp from the top
            .assertTopPositionInRootIsEqualTo(
                topAppBarBottomEdgeY - topAndBottomPadding - FakeIconSize
            )

        topTitleNode
            // Top title should be 56.dp from the start
            // 4.dp padding for the whole app bar + 48.dp icon size + 4.dp title padding.
            .assertLeftPositionInRootIsEqualTo(4.dp + FakeIconSize + 4.dp)
            // Title should be vertically centered in the top part, which has a height of a small
            // app bar.
            .assertTopPositionInRootIsEqualTo((topAppBarBottomEdgeY - topTitleBounds.height) / 2)

        bottomTitleNode
            // Bottom title should be 16.dp from the start.
            .assertLeftPositionInRootIsEqualTo(16.dp)

        if (bottomTextPadding.isSpecified) {
            // Check if the bottom text baseline is at the expected distance from the bottom of the
            // app bar.
            val bottomTextBaselineY =
                bottomTitleBounds.top + bottomTitleNode.getLastBaselinePosition()
            (bottomAppBarBottomEdgeY - bottomTextBaselineY).assertIsEqualTo(
                bottomTextPadding,
                "text baseline distance from the bottom",
            )
        }
    }

    /**
     * Checks that changing values at a [MediumTopAppBar], [LargeTopAppBar], or [TwoRowsTopAppBar]
     * scroll behavior affects the height of the app bar.
     *
     * This check partially and fully collapses the app bar to test its height.
     *
     * @param appBarMaxHeight the max height of the app bar [content]
     * @param appBarMinHeight the min height of the app bar [content]
     * @param content a Composable that adds a MediumTopAppBar, a LargeTopAppBar, or their flexible
     *   variations
     */
    @OptIn(ExperimentalMaterial3Api::class)
    private fun assertMediumOrLargeScrolledHeight(
        appBarMaxHeight: Dp,
        appBarMinHeight: Dp,
        windowInsets: WindowInsets,
        content: @Composable (TopAppBarScrollBehavior?) -> Unit,
    ) {
        val (topInset, bottomInset) =
            with(rule.density) {
                windowInsets.getTop(this).toDp() to windowInsets.getBottom(this).toDp()
            }
        val fullyCollapsedOffsetDp = appBarMaxHeight - appBarMinHeight
        val partiallyCollapsedOffsetDp = fullyCollapsedOffsetDp / 3
        var partiallyCollapsedHeightOffsetPx = 0f
        var fullyCollapsedHeightOffsetPx = 0f
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            with(LocalDensity.current) {
                partiallyCollapsedHeightOffsetPx = partiallyCollapsedOffsetDp.toPx()
                fullyCollapsedHeightOffsetPx = fullyCollapsedOffsetDp.toPx()
            }

            content(scrollBehavior)
        }

        // Simulate a partially collapsed app bar.
        rule.runOnIdle {
            scrollBehavior.state.heightOffset = -partiallyCollapsedHeightOffsetPx
            scrollBehavior.state.contentOffset = -partiallyCollapsedHeightOffsetPx
        }
        rule.waitForIdle()
        rule
            .onNodeWithTag(TopAppBarTestTag)
            .assertHeightIsEqualTo(
                appBarMaxHeight - partiallyCollapsedOffsetDp + topInset + bottomInset
            )

        // Simulate a fully collapsed app bar.
        rule.runOnIdle {
            scrollBehavior.state.heightOffset = -fullyCollapsedHeightOffsetPx
            // Simulate additional content scroll beyond the max offset scroll.
            scrollBehavior.state.contentOffset =
                -fullyCollapsedHeightOffsetPx - partiallyCollapsedHeightOffsetPx
        }
        rule.waitForIdle()
        // Check that the app bar collapsed to its min height.
        rule
            .onNodeWithTag(TopAppBarTestTag)
            .assertHeightIsEqualTo(appBarMinHeight + topInset + bottomInset)
    }

    /**
     * Checks that changing values at a [MediumTopAppBar] or a [LargeTopAppBar] scroll behavior
     * affects the container color and the title's content color of the app bar.
     *
     * @param appBarMaxHeight the max height of the app bar [content]
     * @param appBarMinHeight the min height of the app bar [content]
     * @param titleContentColor text content color expected for the app bar's title.
     * @param subtitleContentColor text content color expected for the app bar's subtitle.
     * @param content a Composable that adds a MediumTopAppBar, a LargeTopAppBar, or their flexible
     *   variations
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    private fun assertMediumOrLargeScrolledColors(
        appBarMaxHeight: Dp,
        appBarMinHeight: Dp,
        titleContentColor: Color,
        subtitleContentColor: Color,
        content: @Composable (TopAppBarScrollBehavior?) -> Unit,
    ) {
        // Note: This value is specifically picked to avoid precision issues when asserting the
        // color values further down this test.
        val fullyCollapsedOffsetDp = appBarMaxHeight - appBarMinHeight
        var fullyCollapsedHeightOffsetPx = 0
        var fullyCollapsedContainerColor: Color = Color.Unspecified
        var expandedAppBarBackgroundColor: Color = Color.Unspecified
        var titleColor = titleContentColor
        var subtitleColor = subtitleContentColor
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            // Using the topAppBarColors for both Medium and Large top app bars, as the
            // content color is the same.
            expandedAppBarBackgroundColor = AppBarTokens.ContainerColor.value
            fullyCollapsedContainerColor =
                TopAppBarDefaults.topAppBarColors().containerColor(colorTransitionFraction = 1f)

            // Resolve the title's content color. The default implementation returns the same color
            // regardless of the fraction, and the color is applied later with alpha.
            if (titleColor == Color.Unspecified) {
                titleColor = TopAppBarDefaults.topAppBarColors().titleContentColor
            }
            if (subtitleColor == Color.Unspecified) {
                subtitleColor = TopAppBarDefaults.topAppBarColors().subtitleContentColor
            }

            with(LocalDensity.current) {
                fullyCollapsedHeightOffsetPx = fullyCollapsedOffsetDp.roundToPx()
            }

            content(scrollBehavior)
        }

        // Expecting the title composable to be reused for the top and bottom rows of the top app
        // bar, so obtaining the node with the title tag should return two nodes, one for each row.
        val allTitleNodes = rule.onAllNodesWithTag(TitleTestTag, true)
        allTitleNodes.assertCountEquals(2)
        val topTitleNode = allTitleNodes.onFirst()
        val bottomTitleNode = allTitleNodes.onLast()

        rule
            .onNodeWithTag(TopAppBarTestTag)
            .captureToImage()
            .assertContainsColor(expandedAppBarBackgroundColor)

        // Assert the content color at the top and bottom parts of the expanded app bar.
        topTitleNode
            .captureToImage()
            .assertContainsColor(
                titleColor
                    .copy(alpha = TopTitleAlphaEasing.transform(0f))
                    .compositeOver(expandedAppBarBackgroundColor)
            )
        bottomTitleNode
            .captureToImage()
            .assertContainsColor(titleColor.compositeOver(expandedAppBarBackgroundColor))

        // Simulate fully collapsed content.
        rule.runOnIdle {
            scrollBehavior.state.heightOffset = -fullyCollapsedHeightOffsetPx.toFloat()
            scrollBehavior.state.contentOffset = -fullyCollapsedHeightOffsetPx.toFloat()
        }
        rule.waitForIdle()
        rule
            .onNodeWithTag(TopAppBarTestTag)
            .captureToImage()
            .assertContainsColor(fullyCollapsedContainerColor)
        topTitleNode
            .captureToImage()
            .assertContainsColor(
                titleColor
                    .copy(alpha = TopTitleAlphaEasing.transform(1f))
                    .compositeOver(fullyCollapsedContainerColor)
            )
        // Only the top title should be visible in the collapsed form.
        bottomTitleNode.assertIsNotDisplayed()
    }

    /**
     * Checks that changing values at a [MediumTopAppBar] or a [LargeTopAppBar] scroll behavior
     * affects the title's semantics.
     *
     * This check partially and fully collapses the app bar to test the semantics.
     *
     * @param appBarMaxHeight the max height of the app bar [content]
     * @param appBarMinHeight the min height of the app bar [content]
     * @param content a Composable that adds a MediumTopAppBar or a LargeTopAppBar
     * @param withSubtitle whether a subtitle is present
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    private fun assertMediumOrLargeScrolledSemantics(
        appBarMaxHeight: Dp,
        appBarMinHeight: Dp,
        content: @Composable (TopAppBarScrollBehavior?) -> Unit,
        withSubtitle: Boolean,
    ) {
        val fullyCollapsedOffsetDp = appBarMaxHeight - appBarMinHeight
        val oneThirdCollapsedOffsetDp = fullyCollapsedOffsetDp / 3
        var fullyCollapsedHeightOffsetPx = 0f
        var oneThirdCollapsedHeightOffsetPx = 0f
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        rule.setMaterialContent(lightColorScheme()) {
            scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            with(LocalDensity.current) {
                oneThirdCollapsedHeightOffsetPx = oneThirdCollapsedOffsetDp.toPx()
                fullyCollapsedHeightOffsetPx = fullyCollapsedOffsetDp.toPx()
            }

            content(scrollBehavior)
        }

        // Asserting that only one semantic title node is returned after the clearAndSetSemantics is
        // applied to the merged tree according to the alpha values of the titles.
        assertSingleTitleSemanticNode()
        if (withSubtitle) {
            assertSingleSubtitleSemanticNode()
        }

        // Simulate 1/3 collapsed content.
        rule.runOnIdle {
            scrollBehavior.state.heightOffset = -oneThirdCollapsedHeightOffsetPx
            scrollBehavior.state.contentOffset = -oneThirdCollapsedHeightOffsetPx
        }
        rule.waitForIdle()

        // Assert that only one semantic title node is available while scrolling the app bar.
        assertSingleTitleSemanticNode()
        if (withSubtitle) {
            assertSingleSubtitleSemanticNode()
        }

        // Simulate fully collapsed content.
        rule.runOnIdle {
            scrollBehavior.state.heightOffset = -fullyCollapsedHeightOffsetPx
            scrollBehavior.state.contentOffset = -fullyCollapsedHeightOffsetPx
        }
        rule.waitForIdle()

        // Assert that only one semantic title node is available.
        assertSingleTitleSemanticNode()
        if (withSubtitle) {
            assertSingleSubtitleSemanticNode()
        }
    }

    /** Asserts that only one semantic node exists at app bar title when the tree is merged. */
    private fun assertSingleTitleSemanticNode() {
        val unmergedTitleNodes = rule.onAllNodesWithTag(TitleTestTag, useUnmergedTree = true)
        unmergedTitleNodes.assertCountEquals(2)

        val mergedTitleNodes = rule.onAllNodesWithTag(TitleTestTag, useUnmergedTree = false)
        mergedTitleNodes.assertCountEquals(1)
    }

    /** Asserts that only one semantic node exists at app bar subtitle when the tree is merged. */
    private fun assertSingleSubtitleSemanticNode() {
        val unmergedSubtitleNodes = rule.onAllNodesWithTag(SubtitleTestTag, useUnmergedTree = true)
        unmergedSubtitleNodes.assertCountEquals(2)

        val mergedSubtitleNodes = rule.onAllNodesWithTag(SubtitleTestTag, useUnmergedTree = false)
        mergedSubtitleNodes.assertCountEquals(1)
    }

    /**
     * An [IconButton] with an [Icon] inside for testing positions.
     *
     * An [IconButton] is defaulted to be 48X48dp, while its child [Icon] is defaulted to 24x24dp.
     */
    private val FakeIcon =
        @Composable { modifier: Modifier ->
            IconButton(
                onClick = { /* doSomething() */ },
                modifier = modifier.semantics(mergeDescendants = true) {},
            ) {
                Icon(ColorPainter(Color.Red), null)
            }
        }

    private fun expectedActionPosition(appBarWidth: Dp): Dp =
        appBarWidth - AppBarStartAndEndPadding - FakeIconSize

    private val FakeIconSize = 48.dp
    private val AppBarStartAndEndPadding = 4.dp

    /**
     * Top and bottom padding for all the built-in app bars that have a top part (or only part) with
     * a height of AppBarSmallTokens.ContainerHeight
     */
    private val DefaultAppBarTopAndBottomPadding =
        (AppBarSmallTokens.ContainerHeight - FakeIconSize) / 2

    private val LazyListTag = "lazyList"
    private val TopAppBarTestTag = "topAppBar"
    private val BottomAppBarTestTag = "bottomAppBar"
    private val NavigationIconTestTag = "navigationIcon"
    private val TitleTestTag = "title"
    private val SubtitleTestTag = "subtitle"
    private val ActionsTestTag = "actions"
    private val RowTestTag = "row"
    private val BoxTestTag = "BoxTestTag"
}
