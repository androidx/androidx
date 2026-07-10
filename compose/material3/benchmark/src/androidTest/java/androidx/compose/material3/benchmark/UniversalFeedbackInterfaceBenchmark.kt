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

package androidx.compose.material3.benchmark

import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toolingGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.util.concurrent.atomic.AtomicReference
import kotlin.isInfinite
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.launch
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class UniversalFeedbackInterfaceBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val ufiTestCaseFactory = { UniversalFeedbackInterfaceTestCase() }

    @Ignore
    @Test
    fun ufi_first_compose() {
        benchmarkRule.benchmarkFirstCompose(ufiTestCaseFactory)
    }

    @Ignore
    @Test
    fun ufi_measure() {
        benchmarkRule.benchmarkFirstMeasure(ufiTestCaseFactory)
    }

    @Ignore
    @Test
    fun ufi_layout() {
        benchmarkRule.benchmarkFirstLayout(ufiTestCaseFactory)
    }

    @Ignore
    @Test
    fun ufi_draw() {
        benchmarkRule.benchmarkFirstDraw(ufiTestCaseFactory)
    }

    @Test
    fun ufi_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(ufiTestCaseFactory)
    }
}

internal class UniversalFeedbackInterfaceTestCase : LayeredComposeTestCase() {

    @Composable
    override fun MeasuredContent() {
        UniversalFeedbackInterface()
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

@Composable
private fun UniversalFeedbackInterface() {
    val stepperDotsState =
        remember(Unit) {
            mutableStateOf(
                StepperDotsState(numDots = 3, index = 0, maxFullSizeDots = ALL_DOTS_FULL_SIZE_LIMIT)
            )
        }

    val carouselUiState =
        UfiCarouselConfig(stepperDotsState = { stepperDotsState.value }, onDragOffset = null)

    val context = LocalContext.current

    ComposeMediaUfiComponent(
        likeButtonUiState =
            UfiIconUiState(
                icon = Icons.Outlined.FavoriteBorder,
                contentDescription = "Like",
                tint = primaryIcon,
                onClick = { Toast.makeText(context, "Like", Toast.LENGTH_SHORT).show() },
                socialUfiCount = "29.1K",
                onSocialUfiCountClick = {
                    Toast.makeText(context, "Like count", Toast.LENGTH_SHORT).show()
                },
                indication = BouncyIndication(),
            ),
        commentButtonUiState =
            UfiIconUiState(
                icon = Icons.Outlined.Email,
                contentDescription = "Comment",
                tint = primaryIcon,
                onClick = { Toast.makeText(context, "Comment", Toast.LENGTH_SHORT).show() },
                socialUfiCount = "624",
            ),
        repostButtonUiState =
            UfiIconUiState(
                icon = Icons.Outlined.Refresh,
                contentDescription = "Repost",
                tint = primaryIcon,
                socialUfiCount = "88",
                onClick = { Toast.makeText(context, "Repost", Toast.LENGTH_SHORT).show() },
                onSocialUfiCountClick = {
                    Toast.makeText(context, "Repost count", Toast.LENGTH_SHORT).show()
                },
                indication = BouncyIndication(),
            ),
        shareButtonUiState =
            UfiIconUiState(
                icon = Icons.AutoMirrored.Outlined.Send,
                contentDescription = "Share",
                tint = primaryIcon,
                onClick = { Toast.makeText(context, "Share click", Toast.LENGTH_SHORT).show() },
                onTouchInterop = {
                    if (it.action == MotionEvent.ACTION_DOWN) {
                        Toast.makeText(context, "Share touch", Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = {
                    Toast.makeText(context, "Share long click", Toast.LENGTH_SHORT).show()
                },
                socialUfiCount = "233",
            ),
        saveButtonUiState =
            UfiIconUiState(
                icon = Icons.Outlined.Star,
                contentDescription = "Save",
                tint = primaryIcon,
                onClick = { Toast.makeText(context, "Save click", Toast.LENGTH_SHORT).show() },
                onLongClick = {
                    Toast.makeText(context, "Save long click", Toast.LENGTH_SHORT).show()
                },
                indication = BouncyIndication(),
                maybeBottomMargin = 0,
            ),
        carouselUiState = carouselUiState,
    )
}

@Composable
private fun ComposeMediaUfiComponent(
    likeButtonUiState: UfiIconUiState,
    commentButtonUiState: UfiIconUiState? = null,
    shareButtonUiState: UfiIconUiState? = null,
    repostButtonUiState: UfiIconUiState? = null,
    carouselUiState: UfiCarouselConfig? = null,
    saveButtonUiState: UfiIconUiState? = null,
) {
    Box(modifier = Modifier.padding(horizontal = 4.dp).fillMaxWidth()) {
        var iconRowModifier: Modifier = Modifier
        if (carouselUiState != null) {
            ComposeMediaCarouselIndicatorComponent(
                carouselUiState,
                Modifier.padding(top = 4.dp).align(Alignment.TopCenter),
            )
            iconRowModifier = Modifier.padding(top = 20.dp)
        }
        IconRow(
            likeButtonUiState,
            commentButtonUiState,
            repostButtonUiState,
            shareButtonUiState,
            saveButtonUiState,
            modifier = iconRowModifier,
        )
    }
}

@Composable
private fun IconRow(
    likeButtonUiState: UfiIconUiState,
    commentButtonUiState: UfiIconUiState?,
    repostButtonUiState: UfiIconUiState?,
    shareButtonUiState: UfiIconUiState?,
    saveButtonUiState: UfiIconUiState?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val iconUiStates =
            listOfNotNull(
                likeButtonUiState,
                commentButtonUiState,
                repostButtonUiState,
                shareButtonUiState,
            )

        val typography = body1Emphasized
        val color = primaryText
        val socialUfiTextStyle = remember { typography.merge(color = color) }

        iconUiStates.forEach { iconUiState ->
            Row(
                modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 46.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BaseUfiIcon(
                    icon = iconUiState.icon,
                    contentDescription = iconUiState.contentDescription,
                    onClick = iconUiState.onClick,
                    onTouchInterop = iconUiState.onTouchInterop,
                    onLongClick = iconUiState.onLongClick,
                    tint = iconUiState.tint,
                    indication = iconUiState.indication,
                )

                if (iconUiState.socialUfiCount != null) {
                    SocialUfiCount(
                        iconUiState.socialUfiCount,
                        iconUiState.onSocialUfiCountClick ?: { iconUiState.onClick(Rect.Zero) },
                        socialUfiTextStyle,
                    )
                }
            }
        }

        if (saveButtonUiState != null) {
            Spacer(modifier = Modifier.weight(1f))

            BaseUfiIcon(
                icon = saveButtonUiState.icon,
                contentDescription = saveButtonUiState.contentDescription,
                onClick = saveButtonUiState.onClick,
                onLongClick = saveButtonUiState.onLongClick,
                tint = saveButtonUiState.tint,
                indication = saveButtonUiState.indication,
                modifier =
                    Modifier.padding(end = 4.dp)
                        .sizeIn(minWidth = 44.dp, minHeight = 46.dp)
                        .padding(bottom = saveButtonUiState.maybeBottomMargin.dp),
            )
        }
    }
}

@Composable
private fun BaseUfiIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: (boundsInParent: Rect) -> Unit,
    modifier: Modifier = Modifier,
    onTouchInterop: ((MotionEvent) -> Unit)? = null,
    onLongClick: ((boundsInWindow: Rect) -> Unit)? = null,
    tint: Color = primaryIcon,
    indication: Indication? = null,
) {
    val bounds = remember { AtomicReference<LayoutCoordinates?>(null) }

    val clickableModifier =
        if (onTouchInterop != null) {
            val context = LocalContext.current
            val gestureDetector =
                GestureDetector(
                    context,
                    object : GestureDetector.SimpleOnGestureListener() {

                        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                            bounds.get()?.let { layoutCoordinates ->
                                if (layoutCoordinates.isAttached) {
                                    onClick(layoutCoordinates.boundsInParent())
                                }
                            }
                            return true
                        }

                        override fun onLongPress(e: MotionEvent) {
                            bounds.get()?.let { layoutCoordinates ->
                                if (layoutCoordinates.isAttached) {
                                    onLongClick?.invoke(layoutCoordinates.boundsInWindow())
                                }
                            }
                        }
                    },
                    mainThreadHandler,
                )

            val requestDisallowInterceptTouchEvent = remember {
                RequestDisallowInterceptTouchEvent()
            }

            Modifier.pointerInteropFilter(requestDisallowInterceptTouchEvent) { motionEvent ->
                requestDisallowInterceptTouchEvent(true)
                gestureDetector.onTouchEvent(motionEvent)
                onTouchInterop(motionEvent)
                true
            }
        } else {
            Modifier.combinedClickable(
                interactionSource = null,
                indication = indication ?: AlphaIndication(),
                role = Role.Button,
                onClick = {
                    bounds.get()?.let { layoutCoordinates ->
                        if (layoutCoordinates.isAttached) {
                            onClick(layoutCoordinates.boundsInParent())
                        }
                    }
                },
                onLongClick = {
                    bounds.get()?.let { layoutCoordinates ->
                        if (layoutCoordinates.isAttached) {
                            onLongClick?.invoke(layoutCoordinates.boundsInWindow())
                        }
                    }
                },
            )
        }

    Box(
        modifier =
            modifier
                .then(clickableModifier)
                .onPlaced { coordinates -> bounds.set(coordinates) }
                .padding(start = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicIcon(
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            tint = tint,
            contentScale = ContentScale.None,
        )
    }
}

@Composable
private fun SocialUfiCount(count: String, onClick: () -> Unit, textStyle: TextStyle) {
    val countModifier =
        Modifier.padding(start = 4.dp)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
    Box(countModifier) { BasicText(text = count, style = textStyle) }
}

@Composable
private fun BasicIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color,
    contentScale: ContentScale = ContentScale.Fit,
    disableDefaultSize: Boolean = false,
) {
    val colorFilter =
        remember(tint) { if (tint == Color.Unspecified) null else ColorFilter.tint(tint) }
    val semantics =
        if (contentDescription != null) {
            Modifier.semantics {
                this.contentDescription = contentDescription
                this.role = Role.Image
            }
        } else {
            Modifier
        }
    Box(
        modifier
            .toolingGraphicsLayer()
            .thenIf(!disableDefaultSize) { Modifier.defaultSizeFor(painter) }
            .paint(painter, colorFilter = colorFilter, contentScale = contentScale)
            .then(semantics)
    )
}

@Composable
private fun StepperDots(
    state: StepperDotsState,
    modifier: Modifier = Modifier,
    dotSize: Dp = 6.dp,
    activeColor: Color = primaryButton,
    inactiveColor: Color = separator,
    gestureHighlightColor: Color = highlightBackground,
    onDragOffset: ((Int) -> Unit)? = null,
) {
    val previousState = remember { AtomicReference(state) }
    val stepperDots = createStepperDots(dotSize, state)
    val stepperDotsPrev = createStepperDots(dotSize, previousState.get())

    val animatable = remember { Animatable(1f) }
    if (state.numDots > state.maxFullSizeDots) {
        val needsRotationAnimation =
            previousState.get().let { prevState ->
                val rotatePastLastActiveFullSizeDot =
                    prevState.activeFullSizeDotIndex == prevState.maxFullSizeDots - 1 &&
                        prevState.index < state.index

                val rotatePastFirstActiveFullSizeDot =
                    prevState.activeFullSizeDotIndex == 0 && prevState.index > state.index

                rotatePastFirstActiveFullSizeDot || rotatePastLastActiveFullSizeDot
            }

        if (needsRotationAnimation) {
            LaunchedEffect(state) {
                animatable.snapTo(0f)
                animatable.animateTo(1f, spring())
            }
        }
        previousState.set(state)
    }
    val rtlParity = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val widthDp = dotSize * (state.dotSlots * 2 - 1)

    val onDragModifier: Modifier? =
        if (onDragOffset != null) {
            val isPressed = remember { mutableStateOf(false) }
            val haptics = LocalHapticFeedback.current
            val density = LocalDensity.current
            val oneSlotDeltaPx = with(density) { (widthDp + padding * 2).toPx() } / state.dotSlots

            val currentOnDragOffset by rememberUpdatedState(onDragOffset)
            Modifier.pointerInput(Unit) {
                    var dragDeltaRemainder = 0f

                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            isPressed.value = true
                            haptics.performHapticFeedback(
                                HapticFeedbackType.GestureThresholdActivate
                            )
                        },
                        onDragEnd = { isPressed.value = false },
                        onDragCancel = { isPressed.value = false },
                        onDrag = { _, dragAmount ->
                            val dragDelta = dragAmount.x + dragDeltaRemainder
                            dragDeltaRemainder = dragDelta % oneSlotDeltaPx
                            if (dragDelta.absoluteValue > oneSlotDeltaPx) {
                                currentOnDragOffset(
                                    (rtlParity * dragDelta / oneSlotDeltaPx).toInt()
                                )
                            }
                        },
                    )
                }
                .drawBehind {
                    if (isPressed.value) {
                        val cornerRadius = this.size.minDimension / 2
                        drawRoundRect(
                            color = gestureHighlightColor,
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                        )
                    }
                }
        } else null

    Canvas(
        modifier
            .thenIfNotNull(onDragModifier) { it }
            .padding(padding)
            .size(widthDp, dotSize)
            .scale(rtlParity, 1f)
    ) {
        val dotRadius = (dotSize / 2).toPx()
        val animationProgress = animatable.value
        val centerY = center.y
        val animationDirection = if (state.activeFullSizeDotIndex == 0) -1 else 1

        stepperDots.forEachIndexed { i, dot ->
            val dotPrev = stepperDotsPrev.getOrNull(i + animationDirection)

            val color = if (dot.active) activeColor else inactiveColor
            val scale = lerp(dotPrev?.scale ?: 0f, dot.scale, animationProgress)
            val circleCenter =
                Offset(
                    androidx.compose.ui.unit
                        .lerp(dotPrev?.centerX ?: dot.centerX, dot.centerX, animationProgress)
                        .toPx(),
                    centerY,
                )

            drawCircle(color, dotRadius * scale, circleCenter)
        }
    }
}

@Composable
private fun ComposeMediaCarouselIndicatorComponent(
    uiState: UfiCarouselConfig,
    modifier: Modifier = Modifier,
) {
    val activeDotColor = remember { Color(0xFF5C77FF) }

    val inactiveDotColor = remember { Color(0xFF48505B) }

    StepperDots(
        state = uiState.stepperDotsState(),
        modifier = modifier,
        activeColor = activeDotColor,
        inactiveColor = inactiveDotColor,
        onDragOffset = uiState.onDragOffset,
    )
}

private fun StepperDotsState(
    numDots: Int,
    index: Int = 0,
    maxVisibleDots: Int = 7,
    maxFullSizeDots: Int = 3,
): StepperDotsState {
    val maxFullSizeDots = maxFullSizeDots.coerceIn(0, maxVisibleDots)

    return StepperDotsState(numDots, 0, 0, maxVisibleDots, maxFullSizeDots).cloneWithIndex(index)
}

private fun createStepperDots(dotSize: Dp, state: StepperDotsState): List<StepperDot> {
    val dotRadius = dotSize / 2

    val dotIndexOffset =
        if (state.numDots > state.maxFullSizeDots) {
            state.index -
                state.activeFullSizeDotIndex -
                (state.dotSlots - state.maxFullSizeDots) / 2
        } else 0

    return (0..<state.dotSlots).map { i ->
        val dotIndex = dotIndexOffset + i
        val scale = state.scaleOfDotAt(dotIndex)
        val active = dotIndex == state.index
        val circleCenterX = dotSize * i * 2 + dotRadius

        StepperDot(active, scale, circleCenterX)
    }
}

private data class StepperDot(val active: Boolean, val scale: Float, val centerX: Dp)

@Immutable
private data class UfiCarouselConfig(
    val stepperDotsState: () -> StepperDotsState,
    val onDragOffset: ((Int) -> Unit)?,
)

@Immutable
private data class StepperDotsState(
    val numDots: Int,
    val index: Int,
    val activeFullSizeDotIndex: Int,
    val maxVisibleDots: Int = 7,
    val maxFullSizeDots: Int = 3,
) {
    val dotSlots: Int =
        if (numDots > maxFullSizeDots) {
            min((numDots - maxFullSizeDots) * 2 + maxFullSizeDots, maxVisibleDots)
        } else numDots

    val firstVisibleIndex: Int =
        if (numDots > maxFullSizeDots) {
            (index - activeFullSizeDotIndex - (dotSlots - maxFullSizeDots) / 2).coerceAtLeast(0)
        } else 0

    val lastVisibleIndex: Int = (firstVisibleIndex + dotSlots - 1).coerceAtMost(numDots - 1)

    fun scaleOfDotAt(index: Int): Float {
        if (index < firstVisibleIndex || index > lastVisibleIndex) return 0f

        val leftBound = this.index - activeFullSizeDotIndex
        val rightBound = leftBound + maxFullSizeDots - 1

        val exponent =
            when {
                index < leftBound -> leftBound - index
                index > rightBound -> index - rightBound
                else -> 0
            }

        return 0.5f.pow(exponent)
    }
}

private data class AlphaIndication(
    private val normalAlpha: Float = IgContentAlpha.normal,
    private val pressedAlpha: Float = IgContentAlpha.pressed,
) :
    BasicAlphaIndication(
        normalAlpha = normalAlpha,
        pressedAlpha = pressedAlpha,
        releaseAnimationSpec = releaseAnimationSpec,
    )

private val releaseAnimationSpec =
    SpringSpec<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

private open class BasicAlphaIndication(
    private val normalAlpha: Float,
    private val pressedAlpha: Float,
    private val releaseAnimationSpec: SpringSpec<Float>,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return AlphaNode(
            interactionSource = interactionSource,
            normalAlpha = normalAlpha,
            pressedAlpha = pressedAlpha,
            releaseAnimationSpec = releaseAnimationSpec,
        )
    }

    override fun equals(other: Any?): Boolean = super.equals(other)

    override fun hashCode(): Int = super.hashCode()
}

private class AlphaNode(
    private val interactionSource: InteractionSource,
    private val normalAlpha: Float,
    private val pressedAlpha: Float,
    private val releaseAnimationSpec: SpringSpec<Float>,
) : Modifier.Node(), LayoutModifierNode {

    private val alphaAnimation = Animatable(normalAlpha)

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(IntOffset.Zero) { alpha = alphaAnimation.value }
        }
    }

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> alphaAnimation.snapTo(pressedAlpha)
                    is PressInteraction.Release ->
                        alphaAnimation.animateTo(normalAlpha, releaseAnimationSpec)
                    is PressInteraction.Cancel -> alphaAnimation.snapTo(normalAlpha)
                }
            }
        }
    }
}

private object IgContentAlpha {
    const val normal: Float = 1.0f
    const val pressed: Float = 0.7f
}

private data class UfiIconUiState(
    @DrawableRes val icon: ImageVector,
    @StringRes val contentDescription: String,
    val tint: Color,
    val onClick: (boundsInParent: Rect) -> Unit,
    val onTouchInterop: ((MotionEvent) -> Unit)? = null,
    val onLongClick: ((boundsInWindow: Rect) -> Unit)? = null,
    val socialUfiCount: String? = null,
    val onSocialUfiCountClick: (() -> Unit)? = null,
    val indication: Indication = AlphaIndication(),
    val maybeBottomMargin: Int = 0,
)

private data class BouncyIndication(
    private val pressedScale: Float = DefaultBouncyPressScale,
    private val pressStiffness: Float = DefaultBouncyStiffness,
    private val unpressStiffness: Float = DefaultBouncyStiffness,
    private val initialClickedVelocity: Float = 0f,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return BouncyNode(
            interactionSource,
            pressedScale,
            pressStiffness,
            unpressStiffness,
            initialClickedVelocity,
        )
    }
}

private class BouncyNode(
    private val interactionSource: InteractionSource,
    private val pressedScale: Float,
    pressStiffness: Float,
    unpressStiffness: Float,
    private val initialClickedVelocity: Float,
) : Modifier.Node(), DrawModifierNode {

    private val animatedScalePercent = Animatable(1f)
    private val pressedSpring =
        spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = pressStiffness)
    private val unpressSpring =
        spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = unpressStiffness)

    private suspend fun animateToPressed() {
        animatedScalePercent.animateTo(pressedScale, pressedSpring)
    }

    private suspend fun animateToResting(isCancel: Boolean) {
        animatedScalePercent.animateTo(
            1f,
            unpressSpring,
            if (isCancel) 0f else initialClickedVelocity,
        )
    }

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is DragInteraction.Start,
                    is PressInteraction.Press -> animateToPressed()
                    is DragInteraction.Stop,
                    is PressInteraction.Release -> animateToResting(false)
                    is DragInteraction.Cancel,
                    is PressInteraction.Cancel -> animateToResting(true)
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        scale(scale = animatedScalePercent.value) { this@draw.drawContent() }
    }
}

private fun StepperDotsState.cloneWithIndex(index: Int): StepperDotsState {
    val offset = index - this.index
    val activeFullSizeDotIndex = (activeFullSizeDotIndex + offset).coerceIn(0, maxFullSizeDots - 1)

    return copy(
        index = index.coerceIn(0, numDots - 1),
        activeFullSizeDotIndex = activeFullSizeDotIndex,
    )
}

private fun Modifier.defaultSizeFor(painter: Painter): Modifier =
    this.then(
        if (painter.intrinsicSize == Size.Unspecified || painter.intrinsicSize.isInfinite()) {
            DefaultIconSizeModifier
        } else {
            Modifier
        }
    )

private fun Size.isInfinite() = width.isInfinite() && height.isInfinite()

private inline fun Modifier.thenIf(condition: Boolean, otherBlock: () -> Modifier): Modifier =
    if (condition) then(otherBlock()) else this

private inline fun <T> Modifier.thenIfNotNull(value: T?, otherBlock: (T) -> Modifier): Modifier =
    if (value != null) then(otherBlock(value)) else this

private val DefaultIconSizeModifier = Modifier.size(24.dp)

private const val ALL_DOTS_FULL_SIZE_LIMIT = 5

private val padding = 6.dp
private const val DefaultBouncyStiffness: Float = Spring.StiffnessMediumLow
private const val DefaultBouncyPressScale: Float = 0.9f
private val mainThreadHandler = Handler(Looper.getMainLooper())

private val primaryText = Color(0xFF000000)
private val primaryButton = Color(0xFF0095F6)
private val separator = Color(0xFFFFFFFF)
private val highlightBackground = Color(0xFFEFEFEF)
private val primaryIcon = primaryText

private val body1Emphasized =
    TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            textMotion = null,
            lineBreak = LineBreak.Unspecified,
        )
        .copy(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.sp,
            textMotion = null,
            lineBreak = LineBreak.Unspecified,
        )
        .copy(fontWeight = FontWeight.Medium, textMotion = null, lineBreak = LineBreak.Unspecified)
