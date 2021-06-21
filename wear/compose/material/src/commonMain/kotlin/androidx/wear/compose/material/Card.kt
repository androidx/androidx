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

package androidx.wear.compose.material

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Base level Wear Material [Card] that offers a single slot to take any content.
 *
 * Is used as the container for more opinionated [Card] components that take specific content such
 * as icons, images, titles, subtitles and labels.
 *
 * The [Card] is Rectangle shaped rounded corners by default.
 *
 * Cards can be enabled or disabled. A disabled card will not respond to click events.
 *
 * @param onClick Will be called when the user clicks the card
 * @param modifier Modifier to be applied to the card
 * @param backgroundPainter A painter used to paint the background of the card. A card will
 * normally have a gradient background. Use [CardDefaults.cardBackgroundPainter()] to obtain an
 * appropriate painter
 * @param contentColor The default color to use for content() unless explicitly set.
 * @param enabled Controls the enabled state of the card. When `false`, this card will not
 * be clickable
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this card. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this card in different [Interaction]s.
 * @param role The type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 */
@Composable
public fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundPainter: Painter = CardDefaults.cardBackgroundPainter(),
    contentColor: Color = MaterialTheme.colors.onSurfaceVariant2,
    enabled: Boolean = true,
    contentPadding: PaddingValues = CardDefaults.ContentPadding,
    shape: Shape = MaterialTheme.shapes.large,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = shape)
    ) {
        // TODO: Due to b/178201337 the paint() modifier on the box doesn't make a call to draw the
        //  box contents. As a result we need to have stacked boxes to enable us to paint the
        //  background
        val painterModifier =
            Modifier
                .matchParentSize()
                .paint(
                    painter = backgroundPainter,
                )

        val contentBoxModifier = Modifier
            .clickable(
                enabled = enabled,
                onClick = onClick,
                role = role,
                indication = rememberRipple(),
                interactionSource = interactionSource,
            )
            .padding(contentPadding)

        Box(
            modifier = painterModifier
        )
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides MaterialTheme.typography.button
        ) {
            Box(
                modifier = contentBoxModifier,
            ) {
                content()
            }
        }
    }
}

/**
 * Opinionated Wear Material [Card] that offers a specific 5 slot layout to show information about
 * an application, e.g. a notification.
 *
 * The first row of the layout has three slots, 1) a small optional application [Image] or [Icon] of
 * size [CardDefaults.AppImageSize]x[CardDefaults.AppImageSize] dp, 2) an application name
 * (emphasised with the [CardColors.appColor()] color), it is expected to be a short start aligned
 * [Text] composable, and 3) the time that the application activity has occurred which will be
 * shown on the top row of the card, this is expected to be an end aligned [Text] composable
 * showing a time relevant to the contents of the [Card].
 *
 * The second row shows a title, this is expected to be a single row of start aligned [Text].
 *
 * The rest of the [Card] contains the body content which can be either [Text] or an [Image].
 *
 * @param onClick Will be called when the user clicks the card
 * @param modifier Modifier to be applied to the card
 * @param appName A slot for displaying the application name, expected to be a single line of text
 * of [Typography.title3]
 * @param time A slot for displaying the time relevant to the contents of the card, expected to be a
 * short piece of right aligned text.
 * @param body A slot for displaying the details of the [Card], expected to be either [Text]
 * (single or multiple-line) or an [Image]
 * @param appImage A slot for a small ([CardDefaults.AppImageSize]x[CardDefaults.AppImageSize] )
 * [Image] or [Icon] associated with the application.
 * @param backgroundPainter A painter used to paint the background of the card. A card will
 * normally have a gradient background. Use [CardDefaults.cardBackgroundPainter()] to obtain an
 * appropriate painter
 * @param appColor The default color to use for appName() and appImage() slots unless explicitly
 * set.
 * @param timeColor The default color to use for time() slot unless explicitly set.
 * @param titleColor The default color to use for title() slot unless explicitly set.
 * @param bodyColor The default color to use for body() slot unless explicitly set.
 */
@Composable
public fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    appName: @Composable () -> Unit,
    time: @Composable () -> Unit,
    title: @Composable () -> Unit,
    body: @Composable () -> Unit,
    appImage: @Composable (() -> Unit)? = null,
    backgroundPainter: Painter = CardDefaults.cardBackgroundPainter(),
    appColor: Color = MaterialTheme.colors.primary,
    timeColor: Color = MaterialTheme.colors.onSurfaceVariant,
    titleColor: Color = MaterialTheme.colors.onSurface,
    bodyColor: Color = MaterialTheme.colors.onSurfaceVariant2,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        backgroundPainter = backgroundPainter,
        enabled = true,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides appColor,
                    LocalTextStyle provides MaterialTheme.typography.caption1
                ) {
                    if (appImage != null) {
                        appImage()
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    appName()
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.weight(1.0f), contentAlignment = Alignment.CenterEnd) {
                    CompositionLocalProvider(
                        LocalContentColor provides timeColor,
                        LocalTextStyle provides MaterialTheme.typography.caption1,
                        content = time
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            CompositionLocalProvider(
                LocalContentColor provides titleColor,
                LocalTextStyle provides MaterialTheme.typography.title3,
                content = title
            )
            CompositionLocalProvider(
                LocalContentColor provides bodyColor,
                LocalTextStyle provides MaterialTheme.typography.body1,
                content = body
            )
        }
    }
}

/**
 * Contains the default values used by [Card]
 */
public object CardDefaults {
    /**
     * Creates a [Painter] for background colors for a [Card]. Cards typically have a linear
     * gradient for a background. The gradient will be between startBackgroundColor
     * and endBackgroundColor and at an angle of 45 degrees.
     *
     * Cards should have a content color that contrasts with the background
     * gradient.
     *
     * @param startBackgroundColor The background color used at the start of the gradient of this
     * [Card]
     * @param endBackgroundColor The background color used at the end of the gradient of this [Card]
     * @param gradientDirection Whether the cards gradient should be start to end (indicated by
     * [LayoutDirection.Ltr]) or end to start (indicated by [LayoutDirection.Rtl]).
     */
    @Composable
    public fun cardBackgroundPainter(
        startBackgroundColor: Color =
            MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.3f)
                .compositeOver(MaterialTheme.colors.surface),
        endBackgroundColor: Color =
            MaterialTheme.colors.onSurfaceVariant2.copy(alpha = 0.2f)
                .compositeOver(MaterialTheme.colors.surface),
        gradientDirection: LayoutDirection = LocalLayoutDirection.current
    ): Painter {
        val backgroundColors: List<Color> = if (gradientDirection == LayoutDirection.Ltr) {
            listOf(
                startBackgroundColor,
                endBackgroundColor
            )
        } else {
            listOf(
                endBackgroundColor,
                startBackgroundColor
            )
        }
        return BrushPainter(FortyFiveDegreeLinearGradient(backgroundColors))
    }

    private val CardHorizontalPadding = 12.dp
    private val CardVerticalPadding = 12.dp

    /**
     * The default content padding used by [Card]
     */
    public val ContentPadding: PaddingValues = PaddingValues(
        start = CardHorizontalPadding,
        top = CardVerticalPadding,
        end = CardHorizontalPadding,
        bottom = CardVerticalPadding
    )

    /**
     * The default size of the app icon/image when used inside a [AppCard].
     */
    public val AppImageSize: Dp = 16.dp
}

/**
 * A linear gradient that draws the gradient at 45 degrees from Top|Start.
 */
@Immutable
internal class FortyFiveDegreeLinearGradient internal constructor(
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
    private val tileMode: TileMode = TileMode.Clamp
) : ShaderBrush() {

    override fun createShader(size: Size): Shader {
        val minWidthHeight = min(size.height, size.width)
        return LinearGradientShader(
            colors = colors,
            colorStops = stops,
            from = Offset(0f, 0f),
            to = Offset(minWidthHeight, minWidthHeight),
            tileMode = tileMode
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FortyFiveDegreeLinearGradient) return false

        if (colors != other.colors) return false
        if (stops != other.stops) return false
        if (tileMode != other.tileMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = colors.hashCode()
        result = 31 * result + (stops?.hashCode() ?: 0)
        result = 31 * result + tileMode.hashCode()
        return result
    }

    override fun toString(): String {
        return "FortyFiveDegreeLinearGradient(colors=$colors, " +
            "stops=$stops, " +
            "tileMode=$tileMode)"
    }
}
