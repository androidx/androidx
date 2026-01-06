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

package com.android.compose.animation.scene.demo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.Back
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.Swipe
import com.android.compose.animation.scene.UserAction
import com.android.compose.animation.scene.UserActionResult
import com.android.compose.animation.scene.ValueKey
import com.android.compose.animation.scene.animateContentFloatAsState
import com.android.compose.animation.scene.animateElementFloatAsState
import com.android.compose.modifiers.thenIf
import com.android.compose.nestedscroll.LargeTopAppBarNestedScrollConnection
import com.android.compose.nestedscroll.PriorityNestedScrollConnection
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

object Shade {
    fun userActions(
        isLockscreenDismissed: Boolean,
        lockscreenScene: SceneKey,
    ): Map<UserAction, UserActionResult> {
        val previousScreen =
            if (isLockscreenDismissed) {
                Scenes.Launcher
            } else {
                lockscreenScene
            }

        return mapOf(
            Back to previousScreen,
            Swipe.Up to previousScreen,
            Swipe.Down to Scenes.QuickSettings,
        )
    }

    object Elements {
        val Time = ElementKey("ShadeTime")
        val Date = ElementKey("ShadeDate")
        val BatteryPercentage = ElementKey("ShadeBatteryPercentage")
        val CollapsedGrid = ElementKey("QuickSettingsCollapsedGrid")
        val Scrim = ElementKey("ShadeScrim")
        val ScrimBackground = ElementKey("ShadeScrimBackground")
    }

    object Values {
        val TimeScale = ValueKey("ShadeTimeScale")
    }

    object Dimensions {
        val ScrimCornerSize = 30.dp
        val StatusBarHeight = 40.dp
    }

    object Colors {
        val Scrim: Color
            @Composable get() = MaterialTheme.colorScheme.surfaceContainer
    }

    object Shapes {
        val Scrim =
            RoundedCornerShape(
                topStart = Dimensions.ScrimCornerSize,
                topEnd = Dimensions.ScrimCornerSize,
            )
    }
}

@Composable
fun ContentScope.Shade(
    notificationList: @Composable ContentScope.(OverscrollEffect?) -> Unit,
    mediaPlayer: (@Composable ContentScope.() -> Unit)?,
    quickSettingsTiles: List<QuickSettingsTileViewModel>,
    nQuickSettingsColumns: Int,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        val shouldPunchHoleBehindScrim =
            layoutState.isTransitioning() &&
                !layoutState.isTransitioningBetween(Scenes.QuickSettings, Scenes.Shade)

        val scrimMinTopPadding = Shade.Dimensions.StatusBarHeight
        ShadeLayout(
            modifier =
                modifier.thenIf(shouldPunchHoleBehindScrim) {
                    // Use the Offscreen composition strategy so that Scrim() can leverage blending
                    // algorithms against background and underScrim.
                    Modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                },
            scrimMinTopPadding = scrimMinTopPadding,
            background = { QuickSettingsBackground(Modifier.fillMaxSize()) },
            underScrim = {
                UnderScrim(
                    mediaPlayer = mediaPlayer,
                    quickSettingsTiles = quickSettingsTiles,
                    nQuickSettingsColumns = nQuickSettingsColumns,
                )
            },
            scrim = {
                Scrim(
                    notificationList = notificationList,
                    shouldPunchHoleBehindScrim = shouldPunchHoleBehindScrim,
                    scrimMinTopPadding = scrimMinTopPadding,
                )
            },
        )
    }
}

@Composable
private fun ContentScope.ShadeLayout(
    background: @Composable () -> Unit,
    underScrim: @Composable () -> Unit,
    scrim: @Composable () -> Unit,
    scrimMinTopPadding: Dp,
    modifier: Modifier = Modifier,
) {
    // The offset of the scrim compared to its position at rest. When this is equal to 0 (its
    // maximum value), then the scrim is at its bottom-most position, right below [underScrim]
    // vertically. When it is equal to its minimum (negative) value, then the scrim is at its
    // top-most position (at y = scrimMinTopPadding).
    val scrimOffset = remember { mutableStateOf(0f) }

    // The additional scrim offset so that the notifications in the shade are below the
    // notifications in the lockscreen when the Lockscreen => Shade transition starts and as long as
    // the user finger is down.
    val additionalScrimOffset = additionalScrimOffset()

    // The last height of [underScrim].
    val underScrimHeight = remember {
        object {
            var value = 0f
        }
    }

    val density = LocalDensity.current
    Layout(
        modifier = modifier,
        contents =
            listOf(
                { background() },
                { underScrim() },
                {
                    val flingBehavior = ScrollableDefaults.flingBehavior()
                    Box(
                        Modifier.disableSwipesWhenScrolling()
                            .nestedScroll(
                                remember(
                                    scrimOffset,
                                    underScrimHeight,
                                    density,
                                    scrimMinTopPadding,
                                    flingBehavior,
                                ) {
                                    scrimNestedScrollConnection(
                                        scrimOffset = { scrimOffset.value },
                                        onScrimOffsetChange = { scrimOffset.value = it },
                                        underScrimHeight = { underScrimHeight.value },
                                        density = density,
                                        scrimMinTopPadding = scrimMinTopPadding,
                                        flingBehavior = flingBehavior,
                                    )
                                }
                            )
                    ) {
                        scrim()
                    }
                },
            ),
    ) { measurables, constraints ->
        check(measurables.size == 3)
        check(measurables[0].size == 1) { "background should compose only top-level composable" }
        check(measurables[1].size == 1) { "underScrim should compose only top-level composable" }
        check(measurables[2].size == 1)

        val background = measurables[0][0].measure(constraints)
        val underScrim = measurables[1][0].measure(constraints)

        val additionalScrimOffset = additionalScrimOffset?.value?.toPx() ?: 0f
        val scrimOffsetY =
            underScrim.height + (scrimOffset.value + additionalScrimOffset).roundToInt()
        val constrainedScrimHeight =
            constraints.constrainHeight(constraints.maxHeight - scrimOffsetY)
        val scrim =
            measurables[2][0].measure(
                constraints.copy(
                    minHeight = constrainedScrimHeight,
                    maxHeight = constrainedScrimHeight,
                )
            )

        // Update the last height of underScrim.
        underScrimHeight.value = underScrim.height.toFloat()

        layout(
            width = maxOf(background.width, underScrim.width, scrim.width),
            height = maxOf(background.height, underScrim.height, scrim.height),
        ) {
            background.place(0, 0)
            underScrim.place(0, 0)
            scrim.placeWithLayer(0, scrimOffsetY)
        }
    }
}

/**
 * Returns the additional offset that we should add to the scrim to ensure that notifications move
 * downwards (and not upwards) during the Lockscreen => Shade transition, even if they have a
 * smaller y position (i.e. they are visually higher) in the Shade scene than in the Lockscreen
 * scene.
 */
@Composable
private fun ContentScope.additionalScrimOffset(): Animatable<Dp, AnimationVector1D>? {
    fun shouldHaveAdditionalScrimOffset(): Boolean {
        val currentTransition = layoutState.currentTransition ?: return false
        return currentTransition.isInitiatedByUserInput &&
            layoutState.isTransitioning(from = Scenes.Lockscreen, to = Scenes.Shade)
    }

    // Important: Make sure that we always return the same Animatable and that
    // shouldStartWithAdditionalScrimOffset() is checked the first time the Shade scene is composed.
    val animatable =
        remember {
            if (!shouldHaveAdditionalScrimOffset()) {
                return@remember null
            }

            Animatable(30.dp, Dp.VectorConverter, Dp.VisibilityThreshold)
        } ?: return null

    // Animate the offset to 0.dp as soon as the user releases their finger.
    val currentTransition = layoutState.currentTransition
    val shouldKeepAdditionalScrimOffset =
        shouldHaveAdditionalScrimOffset() &&
            currentTransition != null &&
            (currentTransition.isUserInputOngoing ||
                currentTransition.currentScene == Scenes.Lockscreen)

    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(shouldKeepAdditionalScrimOffset) {
        if (!shouldKeepAdditionalScrimOffset) {
            // Important: We animate using coroutineScope and not the scope of this LaunchedEffect
            // because we want to cancel the animation only if additionalScrimOffset() is removed
            // from composition.
            coroutineScope.launch { animatable.animateTo(0.dp) }
        }
    }

    return animatable
}

/**
 * The scroll connection used to offset the scrim depending on the current offset and the scroll
 * state of the scrim content.
 */
private fun scrimNestedScrollConnection(
    scrimOffset: () -> Float,
    onScrimOffsetChange: (Float) -> Unit,
    underScrimHeight: () -> Float,
    density: Density,
    scrimMinTopPadding: Dp,
    flingBehavior: FlingBehavior,
): PriorityNestedScrollConnection {
    return LargeTopAppBarNestedScrollConnection(
        height = scrimOffset,
        onHeightChanged = onScrimOffsetChange,
        minHeight = { minScrimOffset(density, underScrimHeight(), scrimMinTopPadding) },
        maxHeight = { 0f },
        flingBehavior = flingBehavior,
    )
}

/**
 * The minimum value of the scrim offset relative to its original position at rest, i.e. when it is
 * visually right below the underScrim content (header, quick settings, media player, etc).
 */
private fun minScrimOffset(
    density: Density,
    underScrimHeight: Float,
    scrimMinTopPadding: Dp,
): Float {
    return -underScrimHeight + with(density) { scrimMinTopPadding.toPx() }
}

@Composable
private fun ContentScope.UnderScrim(
    mediaPlayer: @Composable (ContentScope.() -> Unit)?,
    quickSettingsTiles: List<QuickSettingsTileViewModel>,
    nQuickSettingsColumns: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        val horizontalPaddingModifier =
            Modifier.padding(horizontal = QuickSettings.Dimensions.Padding)

        StatusBar(showDateAndTime = true, horizontalPaddingModifier)

        Box {
            GridAnchor(isExpanded = false)

            // The grid expansion progress. To make the Tile() implementation more
            // self-contained we could have an animateSharedFloatAsState, but this will
            // create a new shared value for each tile that will all have the same value so
            // we instead create a single shared value here used by all Tiles.
            val expansionProgress by
                animateContentFloatAsState(
                    0f,
                    QuickSettingsGrid.Values.Expansion,
                    canOverflow = false,
                )

            QuickSettingsGrid(
                // Only show the first tiles in a 2 x nQuickSettingsColumns grid.
                tiles = quickSettingsTiles.take(2 * nQuickSettingsColumns),
                nColumns = nQuickSettingsColumns,
                isExpanded = false,
                revealEffect = false,
                expansionProgress = { expansionProgress },
                horizontalPaddingModifier.element(Shade.Elements.CollapsedGrid),
            )
        }

        if (mediaPlayer != null) {
            Box(horizontalPaddingModifier.padding(top = QuickSettingsGrid.Dimensions.Spacing)) {
                mediaPlayer()
            }
        }

        Spacer(Modifier.height(QuickSettings.Dimensions.Padding))
    }
}

@Composable
private fun ContentScope.Scrim(
    notificationList: @Composable ContentScope.(OverscrollEffect?) -> Unit,
    shouldPunchHoleBehindScrim: Boolean,
    scrimMinTopPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val overscrollEffect = rememberOverscrollEffect()
    Box(
        modifier
            .overscroll(verticalOverscrollEffect)
            .overscroll(overscrollEffect)
            .element(Shade.Elements.Scrim)
            .clip(Shade.Shapes.Scrim)
    ) {
        if (shouldPunchHoleBehindScrim) {
            Spacer(
                Modifier.fillMaxSize().drawBehind {
                    // Clear pixels from the destination. The color does not matter here given that
                    // the source (this spacer) is not drawn when using DstOut.
                    drawRect(Color.Black, blendMode = BlendMode.DstOut)
                }
            )
        }

        Box(
            Modifier.element(Shade.Elements.ScrimBackground)
                .fillMaxSize()
                .background(Shade.Colors.Scrim)
        )

        Box(
            Modifier
                // Make sure this list is full size so that shared notifications are not clipped
                // when animating from lockscreen to shade.
                .fillMaxSize(),
            propagateMinConstraints = true,
        ) {
            notificationList(overscrollEffect)
        }
    }
}

@Composable
fun ContentScope.StatusBar(showDateAndTime: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier.height(Shade.Dimensions.StatusBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDateAndTime) {
            ShadeTime(scale = 1f)
            Spacer(Modifier.width(16.dp))
            ShadeDate(Modifier.element(Shade.Elements.Date))
        } else {
            Operator()
        }

        Spacer(Modifier.weight(1f))
        BatteryPercentage()
    }
}

@Composable
fun ContentScope.ShadeTime(scale: Float, modifier: Modifier = Modifier) {
    ElementWithValues(Shade.Elements.Time, modifier) {
        val animatedScale by
            animateElementFloatAsState(scale, Shade.Values.TimeScale, canOverflow = false)

        content {
            val measurer = rememberTextMeasurer()
            val color = LocalContentColor.current
            val style = LocalTextStyle.current
            val layoutResult =
                remember(measurer, style) { measurer.measure("10:36", style = style) }
            val layoutDirection = LocalLayoutDirection.current

            Box {
                Spacer(
                    Modifier.layout { measurable, _ ->
                            // Layout this element with the *target* size/scale of the element in
                            // this
                            // scene.
                            val width = ceil(layoutResult.size.width * scale).roundToInt()
                            val height = ceil(layoutResult.size.height * scale).roundToInt()
                            measurable.measure(Constraints.fixed(width, height)).run {
                                layout(width, height) { place(0, 0) }
                            }
                        }
                        .drawBehind {
                            val topLeft: Offset
                            val pivot: Offset
                            if (layoutDirection == LayoutDirection.Ltr) {
                                topLeft = Offset.Zero
                                pivot = Offset.Zero
                            } else {
                                topLeft = Offset(size.width - layoutResult.size.width, 0f)
                                pivot = Offset(size.width, 0f)
                            }

                            scale(animatedScale, pivot = pivot) {
                                drawText(layoutResult, color = color, topLeft = topLeft)
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun ShadeDate(modifier: Modifier = Modifier) {
    Box(modifier) { Text("Mon, Mar 20") }
}

@Composable
fun ContentScope.BatteryPercentage(modifier: Modifier = Modifier) {
    Text("92%", modifier.element(Shade.Elements.BatteryPercentage))
}
