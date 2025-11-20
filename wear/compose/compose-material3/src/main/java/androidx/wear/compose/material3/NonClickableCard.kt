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

package androidx.wear.compose.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.wear.compose.material3.tokens.CardTokens
import androidx.wear.compose.material3.tokens.OutlinedCardTokens

/**
 * Base level Wear Material 3 [Card] that offers a single slot to take any content.
 *
 * This Card does not handle input events - see the other Card overloads if you want a clickable
 * Card.
 *
 * Is used as the container for more opinionated [Card] components that take specific content such
 * as icons, images, titles, subtitles and labels.
 *
 * The [Card] is Rectangle-shaped with rounded corners by default.
 *
 * Example of a non-clickable [Card]:
 *
 * @sample androidx.wear.compose.material3.samples.NonClickableCardSample
 *
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards) Wear OS Material
 * design guide.
 *
 * @param modifier Modifier to be applied to the card
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param colors [CardColors] that will be used to resolve the colors used for this card. See
 *   [CardDefaults.cardColors].
 * @param border A BorderStroke object which is used for drawing outlines.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param transformation Transformation to be used when card appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content The main slot for a content of this card
 */
@Composable
public fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = CardDefaults.ContentPadding,
    transformation: SurfaceTransformation? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardImpl(
        onClick = null,
        containerPainter = null,
        modifier = modifier.cardSizeModifier(),
        onLongClick = null,
        onLongClickLabel = null,
        enabled = false,
        colors = colors,
        border = border,
        interactionSource = null,
        contentPadding = contentPadding,
        shape = shape,
        transformation = transformation,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colors.titleColor,
            LocalTextStyle provides CardTokens.TitleTypography.value,
        ) {
            content()
        }
    }
}

/**
 * Wear Material 3 [Card] that takes a container painter for drawing a background image, and offers
 * a single slot to take any content.
 *
 * This Card does not handle input events - see the other Card overloads if you want a clickable
 * Card.
 *
 * An image background is a means to reinforce the meaning of information in a Card. Cards should
 * have a content color that contrasts with the background image and scrim. This [Card] takes
 * [containerPainter] for the container image background to be drawn (the [CardColors]
 * containerColor property is ignored). It is recommended to use [CardDefaults.containerPainter] to
 * create the painter so that a scrim is drawn on top of the container image, ensuring that any
 * content above the background is legible.
 *
 * The [Card] is Rectangle-shaped with rounded corners by default.
 *
 * Example of a non-clickable [Card] with an image background:
 *
 * @sample androidx.wear.compose.material3.samples.NonClickableImageCardSample
 * @param containerPainter The [Painter] to use to draw the container image of the [Card], such as
 *   returned by [CardDefaults.containerPainter].
 * @param modifier Modifier to be applied to the card
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param colors [CardColors] that will be used to resolve the colors used for this card (the
 *   containerColor is overridden by containerPainter). See
 *   [CardDefaults.cardWithContainerPainterColors].
 * @param border A BorderStroke object which is used for drawing outlines.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param transformation Transformation to be used when card appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content The main slot for a content of this card
 */
@Composable
public fun Card(
    containerPainter: Painter,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardWithContainerPainterColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = CardDefaults.CardWithContainerPainterContentPadding,
    transformation: SurfaceTransformation? = null,
    content: @Composable ColumnScope.() -> Unit,
): Unit =
    CardImpl(
        onClick = null,
        containerPainter = containerPainter,
        modifier = modifier.cardSizeModifier(),
        onLongClick = null,
        onLongClickLabel = null,
        enabled = false,
        colors = colors,
        border = border,
        interactionSource = null,
        contentPadding = contentPadding,
        shape = shape,
        transformation = transformation,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colors.titleColor,
            LocalTextStyle provides CardTokens.TitleTypography.value,
        ) {
            content()
        }
    }

/**
 * Opinionated Wear Material 3 [Card] that offers a specific 5 slot layout to show information about
 * an application, e.g. a notification. AppCards are designed to show interactive elements from
 * multiple applications. They will typically be used by the system UI, e.g. for showing a list of
 * notifications from different applications. However it could also be adapted by individual
 * application developers to show information about different parts of their application.
 *
 * This AppCard does not handle input events - see the other AppCard overloads if you want a
 * clickable AppCard.
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
 * Example of a non-clickable [AppCard]:
 *
 * @sample androidx.wear.compose.material3.samples.NonClickableAppCardSample
 *
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards) guide.
 *
 * @param appName A slot for displaying the application name, expected to be a single line of start
 *   aligned text of [Typography.labelSmall]
 * @param title A slot for displaying the title of the card, expected to be one or two lines of
 *   start aligned text of [Typography.titleMedium]
 * @param modifier Modifier to be applied to the card
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param colors [CardColors] that will be used to resolve the colors used for this card. See
 *   [CardDefaults.cardColors].
 * @param border A BorderStroke object which is used for drawing outlines.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param transformation Transformation to be used when card appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param appImage A slot for a small ([CardDefaults.AppImageSize]x[CardDefaults.AppImageSize] )
 *   [Image] associated with the application.
 * @param time A slot for displaying the time relevant to the contents of the card, expected to be a
 *   short piece of end aligned text of [Typography.labelSmall].
 * @param content The main slot for a content of this card
 */
@Composable
public fun AppCard(
    appName: @Composable RowScope.() -> Unit,
    title: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = CardDefaults.ContentPadding,
    transformation: SurfaceTransformation? = null,
    appImage: @Composable (RowScope.() -> Unit)? = null,
    time: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
): Unit =
    AppCardImpl(
        enabled = false,
        onClick = null,
        appName = appName,
        title = title,
        modifier = modifier,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = null,
        transformation = transformation,
        appImage = appImage,
        time = time,
        content = content,
    )

/**
 * Opinionated Wear Material 3 [Card] that offers a specific layout to show interactive information
 * about an application, e.g. a message. TitleCards are designed for use within an application.
 *
 * This TitleCard does not handle input events - see the other TitleCard overloads if you want a
 * clickable TitleCard.
 *
 * The [time], [subtitle] and [content] fields are optional, but it is expected that at least one of
 * these is provided. The layout will vary according to which fields are supplied - see samples.
 *
 * If the [content] is text it can be single or multiple line and is expected to be Top and Start
 * aligned. When [subtitle] is used [content] shouldn't exceed 2 lines height. Overall the [title],
 * [content] and [subtitle] text should be no more than 5 rows of text combined.
 *
 * If more than one composable is provided in the [content] slot it is the responsibility of the
 * caller to determine how to layout the contents, e.g. provide either a row or a column.
 *
 * Example of a non-clickable [TitleCard] with [time], [title] and [content]:
 *
 * @sample androidx.wear.compose.material3.samples.NonClickableTitleCardSample
 *
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards) guide.
 *
 * @param title A slot for displaying the title of the card, expected to be one or two lines of
 *   text.
 * @param modifier Modifier to be applied to the card
 * @param time An optional slot for displaying the time relevant to the contents of the card,
 *   expected to be a short piece of text. Depending on whether we have a [content] or not, can be
 *   placed at the end of the [title] line or above it.
 * @param subtitle An optional slot for displaying the subtitle of the card, expected to be one line
 *   of text.
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param colors [CardColors] that will be used to resolve the colors used for this card. See
 *   [CardDefaults.cardColors].
 * @param border A BorderStroke object which is used for drawing outlines.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param transformation Transformation to be used when card appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content The optional body content of the card. If not provided then title and subtitle are
 *   expected to be provided
 */
@Composable
public fun TitleCard(
    title: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    time: @Composable (() -> Unit)? = null,
    subtitle: @Composable (ColumnScope.() -> Unit)? = null,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = CardDefaults.ContentPadding,
    transformation: SurfaceTransformation? = null,
    content: @Composable (() -> Unit)? = null,
): Unit =
    CardImpl(
        onClick = null,
        containerPainter = null,
        title = title,
        modifier = modifier,
        onLongClick = null,
        onLongClickLabel = null,
        time = time,
        subtitle = subtitle,
        enabled = false,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = null,
        transformation = transformation,
        content = content,
    )

/**
 * This [TitleCard] overload supports an image container background and provides an opinionated Wear
 * Material 3 [Card] with a specific layout to show interactive information about an application,
 * similar to [TitleCard].
 *
 * This TitleCard does not handle input events - see the other TitleCard overloads if you want a
 * clickable TitleCard.
 *
 * An image background is a means to reinforce the meaning of information in a Card. Cards should
 * have a content color that contrasts with the background image and scrim. This [TitleCard] takes
 * [containerPainter] for the container image background to be drawn (the [CardColors]
 * containerColor property is ignored). It is recommended to use [CardDefaults.containerPainter] to
 * create the painter so that a scrim is drawn on top of the container image, ensuring that any
 * content above the background is legible.
 *
 * The [time], [subtitle] and [content] fields are optional, but it is expected that at least one of
 * these is provided. The layout will vary according to which fields are supplied - see samples.
 *
 * If the [content] is text it can be single or multiple line and is expected to be Top and Start
 * aligned. When [subtitle] is used [content] shouldn't exceed 2 lines height. Overall the [title],
 * [content] and [subtitle] text should be no more than 5 rows of text combined.
 *
 * If more than one composable is provided in the [content] slot it is the responsibility of the
 * caller to determine how to layout the contents, e.g. provide either a row or a column.
 *
 * Example of a [Card] with a background image:
 *
 * @sample androidx.wear.compose.material3.samples.NonClickableTitleCardWithImageWithTimeAndTitleSample
 *
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards) guide.
 *
 * @param containerPainter The [Painter] to use to draw the container image of the [TitleCard], such
 *   as returned by [CardDefaults.containerPainter].
 * @param title A slot for displaying the title of the card, expected to be one or two lines of
 *   text.
 * @param modifier Modifier to be applied to the card
 * @param time An optional slot for displaying the time relevant to the contents of the card,
 *   expected to be a short piece of text. Depending on whether we have a [content] or not, can be
 *   placed at the end of the [title] line or above it.
 * @param subtitle An optional slot for displaying the subtitle of the card, expected to be one line
 *   of text.
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param colors [CardColors] that will be used to resolve the colors used for this card (the
 *   containerColor is overridden by containerPainter). See
 *   [CardDefaults.cardWithContainerPainterColors].
 * @param border A BorderStroke object which is used for drawing outlines.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param transformation Transformation to be used when card appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content The optional body content of the card. If not provided then title and subtitle are
 *   expected to be provided
 */
@Composable
public fun TitleCard(
    containerPainter: Painter,
    title: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    time: @Composable (() -> Unit)? = null,
    subtitle: @Composable (ColumnScope.() -> Unit)? = null,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardWithContainerPainterColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = CardDefaults.CardWithContainerPainterContentPadding,
    transformation: SurfaceTransformation? = null,
    content: @Composable (() -> Unit)? = null,
): Unit =
    CardImpl(
        onClick = null,
        containerPainter = containerPainter,
        title = title,
        modifier = modifier,
        onLongClick = null,
        onLongClickLabel = null,
        time = time,
        subtitle = subtitle,
        enabled = false,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = null,
        transformation = transformation,
        content = content,
    )

/**
 * Outlined Wear Material 3 [Card] that offers a single slot to take any content.
 *
 * This OutlinedCard does not handle input events - see the other OutlinedCard overloads if you want
 * a clickable OutlinedCard.
 *
 * Outlined [Card] components that take specific content such as icons, images, titles, subtitles
 * and labels. Outlined Cards have a visual boundary around the container. This can emphasise the
 * content of this card.
 *
 * The [OutlinedCard] is Rectangle-shaped with rounded corners by default.
 *
 * Example of a non-clickable [OutlinedCard]:
 *
 * @sample androidx.wear.compose.material3.samples.NonClickableOutlinedCardSample
 *
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards) Wear OS Material
 * design guide.
 *
 * @param modifier Modifier to be applied to the card
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param colors [CardColors] that will be used to resolve the colors used for this card. See
 *   [CardDefaults.cardColors].
 * @param border A BorderStroke object which is used for the outline drawing.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param transformation Transformation to be used when card appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content The main slot for a content of this card
 */
@Composable
public fun OutlinedCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    border: BorderStroke = CardDefaults.outlinedCardBorder(),
    contentPadding: PaddingValues = CardDefaults.ContentPadding,
    transformation: SurfaceTransformation? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardImpl(
        onClick = null,
        containerPainter = null,
        modifier = modifier.cardSizeModifier(),
        onLongClick = null,
        onLongClickLabel = null,
        enabled = false,
        colors = colors,
        border = border,
        interactionSource = null,
        contentPadding = contentPadding,
        shape = shape,
        transformation = transformation,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colors.contentColor,
            LocalTextStyle provides OutlinedCardTokens.ContentTypography.value,
        ) {
            content()
        }
    }
}
