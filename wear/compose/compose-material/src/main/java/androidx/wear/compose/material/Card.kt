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

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.materialcore.ImageWithScrimPainter
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
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards) Wear OS Material
 * design guide.
 *
 * Note that the Wear OS design guidance recommends a gradient or image background for Cards which
 * is not the case for Mobile Cards. As a result you will see a backgroundPainter rather than a
 * backgroundColor for Cards. If you want to workaround this recommendation you could use a
 * [ColorPainter] to produce a solid colored background.
 *
 * @param onClick Will be called when the user clicks the card
 * @param modifier Modifier to be applied to the card
 * @param backgroundPainter A painter used to paint the background of the card. A card will normally
 *   have a gradient background. Use [CardDefaults.cardBackgroundPainter()] to obtain an appropriate
 *   painter
 * @param contentColor The default color to use for content() unless explicitly set.
 * @param enabled Controls the enabled state of the card. When false, this card will not be
 *   clickable and there will be no ripple effect on click. Wear cards do not have any specific
 *   elevation or alpha differences when not enabled - they are simply not clickable.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param role The type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param content Slot for composable body content displayed on the Card
 */
@Composable
public fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundPainter: Painter = CardDefaults.cardBackgroundPainter(),
    contentColor: Color = MaterialTheme.colors.onSurfaceVariant,
    enabled: Boolean = true,
    contentPadding: PaddingValues = CardDefaults.ContentPadding,
    shape: Shape = MaterialTheme.shapes.large,
    interactionSource: MutableInteractionSource? = null,
    role: Role? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.wear.compose.materialcore.Card(
        onClick = onClick,
        modifier = modifier,
        border = null,
        containerPainter = backgroundPainter,
        enabled = enabled,
        contentPadding = contentPadding,
        shape = shape,
        interactionSource = interactionSource,
        role = role,
        ripple = ripple(),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides MaterialTheme.typography.button,
        ) {
            content()
        }
    }
}

/**
 * Opinionated Wear Material [Card] that offers a specific 5 slot layout to show information about
 * an application, e.g. a notification. AppCards are designed to show interactive elements from
 * multiple applications. They will typically be used by the system UI, e.g. for showing a list of
 * notifications from different applications. However it could also be adapted by individual
 * application developers to show information about different parts of their application.
 *
 * The first row of the layout has three slots, 1) a small optional application [Image] or [Icon] of
 * size [CardDefaults.AppImageSize]x[CardDefaults.AppImageSize] dp, 2) an application name
 * (emphasised with the [CardColors.appColor()] color), it is expected to be a short start aligned
 * [Text] composable, and 3) the time that the application activity has occurred which will be shown
 * on the top row of the card, this is expected to be an end aligned [Text] composable showing a
 * time relevant to the contents of the [Card].
 *
 * The second row shows a title, this is expected to be a single row of start aligned [Text].
 *
 * The rest of the [Card] contains the content which can be either [Text] or an [Image]. If the
 * content is text it can be single or multiple line and is expected to be Top and Start aligned.
 *
 * If more than one composable is provided in the content slot it is the responsibility of the
 * caller to determine how to layout the contents, e.g. provide either a row or a column.
 *
 * Example of an [AppCard] with icon, title, time and two lines of body text:
 *
 * @sample androidx.wear.compose.material.samples.AppCardWithIcon
 *
 * Example of an [AppCard] with image content:
 *
 * @sample androidx.wear.compose.material.samples.AppCardWithImage
 *
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards) guide.
 *
 * @param onClick Will be called when the user clicks the card
 * @param appName A slot for displaying the application name, expected to be a single line of start
 *   aligned text of [Typography.title3]
 * @param time A slot for displaying the time relevant to the contents of the card, expected to be a
 *   short piece of end aligned text.
 * @param title A slot for displaying the title of the card, expected to be one or two lines of
 *   start aligned text of [Typography.button]
 * @param modifier Modifier to be applied to the card
 * @param enabled Controls the enabled state of the card. When false, this card will not be
 *   clickable and there will be no ripple effect on click. Wear cards do not have any specific
 *   elevation or alpha differences when not enabled - they are simply not clickable.
 * @param appImage A slot for a small ([CardDefaults.AppImageSize]x[CardDefaults.AppImageSize] )
 *   [Image] associated with the application.
 * @param backgroundPainter A painter used to paint the background of the card. A card will normally
 *   have a gradient background. Use [CardDefaults.cardBackgroundPainter()] to obtain an appropriate
 *   painter
 * @param contentColor The default color to use for content() slot unless explicitly set.
 * @param appColor The default color to use for appName() and appImage() slots unless explicitly
 *   set.
 * @param timeColor The default color to use for time() slot unless explicitly set.
 * @param titleColor The default color to use for title() slot unless explicitly set.
 * @param content Slot for composable body content displayed on the Card
 */
@Composable
public fun AppCard(
    onClick: () -> Unit,
    appName: @Composable RowScope.() -> Unit,
    time: @Composable RowScope.() -> Unit,
    title: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    appImage: @Composable (RowScope.() -> Unit)? = null,
    backgroundPainter: Painter = CardDefaults.cardBackgroundPainter(),
    contentColor: Color = MaterialTheme.colors.onSurfaceVariant,
    appColor: Color = contentColor,
    timeColor: Color = contentColor,
    titleColor: Color = MaterialTheme.colors.onSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.wear.compose.materialcore.AppCard(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        border = null,
        contentPadding = CardDefaults.ContentPadding,
        containerPainter = backgroundPainter,
        interactionSource = null,
        shape = MaterialTheme.shapes.large,
        ripple = ripple(),
        appImage = appImage?.let { { appImage() } },
        appName = {
            CompositionLocalProvider(
                LocalContentColor provides appColor,
                LocalTextStyle provides MaterialTheme.typography.caption1
            ) {
                appName()
            }
        },
        time = {
            CompositionLocalProvider(
                LocalContentColor provides timeColor,
                LocalTextStyle provides MaterialTheme.typography.caption1,
            ) {
                time()
            }
        },
        title = {
            CompositionLocalProvider(
                LocalContentColor provides titleColor,
                LocalTextStyle provides MaterialTheme.typography.title3
            ) {
                title()
            }
        }
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides MaterialTheme.typography.body1,
        ) {
            content()
        }
    }
}

/**
 * Opinionated Wear Material [Card] that offers a specific 3 slot layout to show interactive
 * information about an application, e.g. a message. TitleCards are designed for use within an
 * application.
 *
 * The first row of the layout has two slots. 1. a start aligned title (emphasised with the
 * [titleColor] and expected to be start aligned text). The title text is expected to be a maximum
 * of 2 lines of text. 2. An optional time that the application activity has occurred shown at the
 * end of the row, expected to be an end aligned [Text] composable showing a time relevant to the
 * contents of the [Card].
 *
 * The rest of the [Card] contains the content which is expected to be [Text] or a contained
 * [Image].
 *
 * If the content is text it can be single or multiple line and is expected to be Top and Start
 * aligned and of type of [Typography.body1].
 *
 * Overall the [title] and [content] text should be no more than 5 rows of text combined.
 *
 * If more than one composable is provided in the content slot it is the responsibility of the
 * caller to determine how to layout the contents, e.g. provide either a row or a column.
 *
 * Example of a [TitleCard] with two lines of body text:
 *
 * @sample androidx.wear.compose.material.samples.TitleCardStandard
 *
 * Example of a title card with a background image:
 *
 * @sample androidx.wear.compose.material.samples.TitleCardWithImageBackground
 *
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards) guide.
 *
 * @param onClick Will be called when the user clicks the card
 * @param title A slot for displaying the title of the card, expected to be one or two lines of text
 *   of [Typography.button]
 * @param modifier Modifier to be applied to the card
 * @param enabled Controls the enabled state of the card. When false, this card will not be
 *   clickable and there will be no ripple effect on click. Wear cards do not have any specific
 *   elevation or alpha differences when not enabled - they are simply not clickable.
 * @param time An optional slot for displaying the time relevant to the contents of the card,
 *   expected to be a short piece of end aligned text.
 * @param backgroundPainter A painter used to paint the background of the card. A title card can
 *   have either a gradient background or an image background, use
 *   [CardDefaults.cardBackgroundPainter()] or [CardDefaults.imageBackgroundPainter()] to obtain an
 *   appropriate painter
 * @param contentColor The default color to use for content() slot unless explicitly set.
 * @param titleColor The default color to use for title() slot unless explicitly set.
 * @param timeColor The default color to use for time() slot unless explicitly set.
 * @param content Slot for composable body content displayed on the Card
 */
@Composable
public fun TitleCard(
    onClick: () -> Unit,
    title: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    time: @Composable (RowScope.() -> Unit)? = null,
    backgroundPainter: Painter = CardDefaults.cardBackgroundPainter(),
    contentColor: Color = MaterialTheme.colors.onSurfaceVariant,
    titleColor: Color = MaterialTheme.colors.onSurface,
    timeColor: Color = contentColor,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.wear.compose.materialcore.Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerPainter = backgroundPainter,
        border = null,
        contentPadding = CardDefaults.ContentPadding,
        interactionSource = null,
        role = null,
        shape = MaterialTheme.shapes.large,
        ripple = ripple(),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides titleColor,
                    LocalTextStyle provides MaterialTheme.typography.title3,
                ) {
                    title()
                }
                time?.let {
                    Spacer(modifier = Modifier.weight(1.0f))
                    CompositionLocalProvider(
                        LocalContentColor provides timeColor,
                        LocalTextStyle provides MaterialTheme.typography.caption1,
                    ) {
                        time()
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalTextStyle provides MaterialTheme.typography.body1,
            ) {
                content()
            }
        }
    }
}

/** Contains the default values used by [Card] */
public object CardDefaults {
    /**
     * Creates a [Painter] for background colors for a [Card]. Cards typically have a linear
     * gradient for a background. The gradient will be between startBackgroundColor and
     * endBackgroundColor and at an angle of 45 degrees.
     *
     * Cards should have a content color that contrasts with the background gradient.
     *
     * @param startBackgroundColor The background color used at the start of the gradient of this
     *   [Card]
     * @param endBackgroundColor The background color used at the end of the gradient of this [Card]
     * @param gradientDirection Whether the cards gradient should be start to end (indicated by
     *   [LayoutDirection.Ltr]) or end to start (indicated by [LayoutDirection.Rtl]).
     */
    @Composable
    public fun cardBackgroundPainter(
        startBackgroundColor: Color =
            MaterialTheme.colors.primary
                .copy(alpha = 0.30f)
                .compositeOver(MaterialTheme.colors.background),
        endBackgroundColor: Color =
            MaterialTheme.colors.onSurfaceVariant
                .copy(alpha = 0.20f)
                .compositeOver(MaterialTheme.colors.background),
        gradientDirection: LayoutDirection = LocalLayoutDirection.current
    ): Painter {
        return BrushPainter(
            FortyFiveDegreeLinearGradient(
                colors = listOf(startBackgroundColor, endBackgroundColor),
                ltr = gradientDirection == LayoutDirection.Ltr
            )
        )
    }

    /**
     * Creates a [Painter] for the background of a [Card] that displays an Image with a scrim over
     * the image to make sure that any content above the background will be legible.
     *
     * An Image background is a means to reinforce the meaning of information in a Card, e.g. To
     * help to contextualize the information in a TitleCard
     *
     * Cards should have a content color that contrasts with the background image and scrim
     *
     * @param backgroundImagePainter The [Painter] to use to draw the background of the [Card]
     * @param backgroundImageScrimBrush The [Brush] to use to paint a scrim over the background
     *   image to ensure that any text drawn over the image is legible
     */
    @Composable
    public fun imageWithScrimBackgroundPainter(
        backgroundImagePainter: Painter,
        backgroundImageScrimBrush: Brush =
            Brush.linearGradient(
                colors =
                    listOf(
                        MaterialTheme.colors.surface.copy(alpha = 1.0f),
                        MaterialTheme.colors.surface.copy(alpha = 0f)
                    )
            )
    ): Painter {
        return ImageWithScrimPainter(
            imagePainter = backgroundImagePainter,
            brush = backgroundImageScrimBrush
        )
    }

    private val CardHorizontalPadding = 12.dp
    private val CardVerticalPadding = 12.dp

    /** The default content padding used by [Card] */
    public val ContentPadding: PaddingValues =
        PaddingValues(
            start = CardHorizontalPadding,
            top = CardVerticalPadding,
            end = CardHorizontalPadding,
            bottom = CardVerticalPadding
        )

    /** The default size of the app icon/image when used inside a [AppCard]. */
    public val AppImageSize: Dp = 16.dp
}

/** A linear gradient that draws the gradient at 45 degrees from Top|Start. */
@Immutable
internal class FortyFiveDegreeLinearGradient
internal constructor(
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
    private val tileMode: TileMode = TileMode.Clamp,
    private val ltr: Boolean
) : ShaderBrush() {

    override fun createShader(size: Size): Shader {
        val minWidthHeight = min(size.height, size.width)
        val from = if (ltr) Offset(0f, 0f) else Offset(minWidthHeight, 0f)
        val to = if (ltr) Offset(minWidthHeight, minWidthHeight) else Offset(0f, minWidthHeight)
        return LinearGradientShader(
            colors = colors,
            colorStops = stops,
            from = from,
            to = to,
            tileMode = tileMode
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FortyFiveDegreeLinearGradient) return false

        if (colors != other.colors) return false
        if (stops != other.stops) return false
        if (tileMode != other.tileMode) return false
        if (ltr != other.ltr) return false

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
