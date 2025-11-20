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

package androidx.wear.compose.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.tokens.CardTokens
import androidx.wear.compose.material3.tokens.ImageCardTokens
import androidx.wear.compose.material3.tokens.OutlinedCardTokens
import androidx.wear.compose.materialcore.Text

/**
 * Base level Wear Material 3 [Card] that offers a single slot to take any content.
 *
 * Is used as the container for more opinionated [Card] components that take specific content such
 * as icons, images, titles, subtitles and labels.
 *
 * The [Card] is Rectangle-shaped with rounded corners by default.
 *
 * Cards can be enabled or disabled. A disabled card will not respond to click events.
 *
 * Example of a [Card] with onClick:
 *
 * @sample androidx.wear.compose.material3.samples.CardSample
 *
 * Example of [Card] with onLongClick:
 *
 * @sample androidx.wear.compose.material3.samples.CardWithOnLongClickSample
 *
 * [Card] does not constrain the height of its contents by default. Where necessary to do that, use
 * `Modifier.height(IntrinsicSize.Min)` as shown in this example:
 *
 * @sample androidx.wear.compose.material3.samples.CardFillContentSample
 *
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards) Wear OS Material
 * design guide.
 *
 * @param onClick Will be called when the user clicks the card
 * @param modifier Modifier to be applied to the card
 * @param onLongClick Called when this card is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param enabled Controls the enabled state of the card. When false, this card will not be
 *   clickable and there will be no ripple effect on click. Wear cards do not have any specific
 *   elevation or alpha differences when not enabled - they are simply not clickable.
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param colors [CardColors] that will be used to resolve the colors used for this card in
 *   different states. See [CardDefaults.cardColors].
 * @param border A BorderStroke object which is used for drawing outlines.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param transformation Transformation to be used when card appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content The main slot for a content of this card
 */
@Composable
public fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = CardDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardImpl(
        onClick = onClick,
        containerPainter = null,
        modifier = modifier.cardSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        colors = colors,
        border = border,
        interactionSource = interactionSource,
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
 * An image background is a means to reinforce the meaning of information in a Card. Cards should
 * have a content color that contrasts with the background image and scrim. This [Card] takes
 * [containerPainter] for the container image background to be drawn (the [CardColors]
 * containerColor property is ignored). It is recommended to use [CardDefaults.containerPainter] to
 * create the painter so that a scrim is drawn on top of the container image, ensuring that any
 * content above the background is legible.
 *
 * The [Card] is Rectangle-shaped with rounded corners by default.
 *
 * Cards can be enabled or disabled. A disabled card will not respond to click events.
 *
 * Example of a [Card] with an image background:
 *
 * @sample androidx.wear.compose.material3.samples.ImageCardSample
 * @param onClick Will be called when the user clicks the card
 * @param containerPainter The [Painter] to use to draw the container image of the [Card], such as
 *   returned by [CardDefaults.containerPainter].
 * @param modifier Modifier to be applied to the card
 * @param onLongClick Called when this card is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param enabled Controls the enabled state of the card. When false, this card will not be
 *   clickable and there will be no ripple effect on click. Wear cards do not have any specific
 *   elevation or alpha differences when not enabled - they are simply not clickable.
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param colors [CardColors] that will be used to resolve the colors used for this card in
 *   different states (the containerColor is overridden by containerPainter). See
 *   [CardDefaults.cardWithContainerPainterColors].
 * @param border A BorderStroke object which is used for drawing outlines.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param transformation Transformation to be used when card appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content The main slot for a content of this card
 */
@Composable
public fun Card(
    onClick: () -> Unit,
    containerPainter: Painter,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardWithContainerPainterColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = CardDefaults.CardWithContainerPainterContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    content: @Composable ColumnScope.() -> Unit,
): Unit =
    CardImpl(
        onClick = onClick,
        containerPainter = containerPainter,
        modifier = modifier.cardSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        colors = colors,
        border = border,
        interactionSource = interactionSource,
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
 * Example of an [AppCard] with onClick:
 *
 * @sample androidx.wear.compose.material3.samples.AppCardSample
 *
 * Example of an [AppCard] with icon:
 *
 * @sample androidx.wear.compose.material3.samples.AppCardWithIconSample
 *
 * Example of an [AppCard] with image [content]:
 *
 * @sample androidx.wear.compose.material3.samples.AppCardWithImageSample
 *
 * Example of an outlined [AppCard]:
 *
 * @sample androidx.wear.compose.material3.samples.OutlinedAppCardSample
 *
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards) guide.
 *
 * @param onClick Will be called when the user clicks the card
 * @param appName A slot for displaying the application name, expected to be a single line of start
 *   aligned text of [Typography.labelSmall]
 * @param title A slot for displaying the title of the card, expected to be one or two lines of
 *   start aligned text of [Typography.titleMedium]
 * @param modifier Modifier to be applied to the card
 * @param onLongClick Called when this card is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param enabled Controls the enabled state of the card. When false, this card will not be
 *   clickable and there will be no ripple effect on click. Wear cards do not have any specific
 *   elevation or alpha differences when not enabled - they are simply not clickable.
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param colors [CardColors] that will be used to resolve the colors used for this card in
 *   different states. See [CardDefaults.cardColors].
 * @param border A BorderStroke object which is used for drawing outlines.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
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
    onClick: () -> Unit,
    appName: @Composable RowScope.() -> Unit,
    title: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = CardDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    appImage: @Composable (RowScope.() -> Unit)? = null,
    time: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
): Unit =
    AppCardImpl(
        onClick = onClick,
        appName = appName,
        title = title,
        modifier = modifier,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        transformation = transformation,
        appImage = appImage,
        time = time,
        content = content,
    )

/**
 * Opinionated Wear Material 3 [Card] that offers a specific layout to show interactive information
 * about an application, e.g. a message. TitleCards are designed for use within an application.
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
 * Example of a [TitleCard] with [onClick], [time], [title] and [content]:
 *
 * @sample androidx.wear.compose.material3.samples.TitleCardSample
 *
 * Example of a [TitleCard] with [time], [title] and [subtitle]:
 *
 * @sample androidx.wear.compose.material3.samples.TitleCardWithSubtitleAndTimeSample
 *
 * Example of a [TitleCard] with images [content]:
 *
 * @sample androidx.wear.compose.material3.samples.TitleCardWithMultipleImagesSample
 *
 * Example of an outlined [TitleCard]:
 *
 * @sample androidx.wear.compose.material3.samples.OutlinedTitleCardSample
 *
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards) guide.
 *
 * @param onClick Will be called when the user clicks the card
 * @param title A slot for displaying the title of the card, expected to be one or two lines of
 *   text.
 * @param modifier Modifier to be applied to the card
 * @param onLongClick Called when this card is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param time An optional slot for displaying the time relevant to the contents of the card,
 *   expected to be a short piece of text. Depending on whether we have a [content] or not, can be
 *   placed at the end of the [title] line or above it.
 * @param subtitle An optional slot for displaying the subtitle of the card, expected to be one line
 *   of text.
 * @param enabled Controls the enabled state of the card. When false, this card will not be
 *   clickable and there will be no ripple effect on click. Wear cards do not have any specific
 *   elevation or alpha differences when not enabled - they are simply not clickable.
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param colors [CardColors] that will be used to resolve the colors used for this card in
 *   different states. See [CardDefaults.cardColors].
 * @param border A BorderStroke object which is used for drawing outlines.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param transformation Transformation to be used when card appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content The optional body content of the card. If not provided then title and subtitle are
 *   expected to be provided
 */
@Composable
public fun TitleCard(
    onClick: () -> Unit,
    title: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    time: @Composable (() -> Unit)? = null,
    subtitle: @Composable (ColumnScope.() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = CardDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    content: @Composable (() -> Unit)? = null,
): Unit =
    CardImpl(
        onClick = onClick,
        containerPainter = null,
        title = title,
        modifier = modifier,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        time = time,
        subtitle = subtitle,
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        transformation = transformation,
        content = content,
    )

/**
 * This [TitleCard] overload supports an image container background and provides an opinionated Wear
 * Material 3 [Card] with a specific layout to show interactive information about an application,
 * similar to [TitleCard].
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
 * @sample androidx.wear.compose.material3.samples.TitleCardWithImageWithTimeAndTitleSample
 *
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards) guide.
 *
 * @param onClick Will be called when the user clicks the card
 * @param containerPainter The [Painter] to use to draw the container image of the [TitleCard], such
 *   as returned by [CardDefaults.containerPainter].
 * @param title A slot for displaying the title of the card, expected to be one or two lines of
 *   text.
 * @param modifier Modifier to be applied to the card
 * @param onLongClick Called when this card is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param time An optional slot for displaying the time relevant to the contents of the card,
 *   expected to be a short piece of text. Depending on whether we have a [content] or not, can be
 *   placed at the end of the [title] line or above it.
 * @param subtitle An optional slot for displaying the subtitle of the card, expected to be one line
 *   of text.
 * @param enabled Controls the enabled state of the card. When false, this card will not be
 *   clickable and there will be no ripple effect on click. Wear cards do not have any specific
 *   elevation or alpha differences when not enabled - they are simply not clickable.
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param colors [CardColors] that will be used to resolve the colors used for this card in
 *   different states (the containerColor is overridden by containerPainter). See
 *   [CardDefaults.cardWithContainerPainterColors].
 * @param border A BorderStroke object which is used for drawing outlines.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param transformation Transformation to be used when card appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content The optional body content of the card. If not provided then title and subtitle are
 *   expected to be provided
 */
@Composable
public fun TitleCard(
    onClick: () -> Unit,
    containerPainter: Painter,
    title: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    time: @Composable (() -> Unit)? = null,
    subtitle: @Composable (ColumnScope.() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardWithContainerPainterColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = CardDefaults.CardWithContainerPainterContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    content: @Composable (() -> Unit)? = null,
): Unit =
    CardImpl(
        onClick = onClick,
        containerPainter = containerPainter,
        title = title,
        modifier = modifier,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        time = time,
        subtitle = subtitle,
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        transformation = transformation,
        content = content,
    )

/**
 * Outlined Wear Material 3 [Card] that offers a single slot to take any content.
 *
 * Outlined [Card] components that take specific content such as icons, images, titles, subtitles
 * and labels. Outlined Cards have a visual boundary around the container. This can emphasise the
 * content of this card.
 *
 * The [OutlinedCard] is Rectangle-shaped with rounded corners by default.
 *
 * Cards can be enabled or disabled. A disabled card will not respond to click events.
 *
 * Example of an [OutlinedCard]:
 *
 * @sample androidx.wear.compose.material3.samples.OutlinedCardSample
 *
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards) Wear OS Material
 * design guide.
 *
 * @param onClick Will be called when the user clicks the card
 * @param modifier Modifier to be applied to the card
 * @param onLongClick Called when this card is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param enabled Controls the enabled state of the card. When false, this card will not be
 *   clickable and there will be no ripple effect on click. Wear cards do not have any specific
 *   elevation or alpha differences when not enabled - they are simply not clickable.
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param colors [CardColors] that will be used to resolve the colors used for this card in
 *   different states. See [CardDefaults.cardColors].
 * @param border A BorderStroke object which is used for the outline drawing.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param transformation Transformation to be used when card appears inside a container that needs
 *   to dynamically change its content separately from the background.
 * @param content The main slot for a content of this card
 */
@Composable
public fun OutlinedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    border: BorderStroke = CardDefaults.outlinedCardBorder(),
    contentPadding: PaddingValues = CardDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardImpl(
        onClick = onClick,
        containerPainter = null,
        modifier = modifier.cardSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        colors = colors,
        border = border,
        interactionSource = interactionSource,
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

/** Contains the default values used by [Card] */
public object CardDefaults {
    /**
     * Creates a [CardColors] that represents the default container and content colors used in a
     * [Card], [AppCard] or [TitleCard].
     */
    @Composable public fun cardColors(): CardColors = MaterialTheme.colorScheme.defaultCardColors

    /**
     * Creates a [CardColors] that represents the default container and content colors used in a
     * [Card], [AppCard] or [TitleCard].
     *
     * @param containerColor the container color of this [Card].
     * @param contentColor the content color of this [Card].
     * @param appNameColor the color used for appName, only applies to [AppCard].
     * @param timeColor the color used for time, applies to [AppCard] and [TitleCard].
     * @param titleColor the color used for title, applies to [AppCard] and [TitleCard].
     * @param subtitleColor the color used for subtitle, applies to [TitleCard].
     */
    @Composable
    public fun cardColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        appNameColor: Color = Color.Unspecified,
        timeColor: Color = Color.Unspecified,
        titleColor: Color = Color.Unspecified,
        subtitleColor: Color = Color.Unspecified,
    ): CardColors =
        MaterialTheme.colorScheme.defaultCardColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            appNameColor = appNameColor,
            timeColor = timeColor,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
        )

    /**
     * Creates a [CardColors] that represents the default container and content colors used in an
     * [OutlinedCard], outlined [AppCard] or [TitleCard].
     */
    @Composable
    public fun outlinedCardColors(): CardColors =
        MaterialTheme.colorScheme.defaultOutlinedCardColors

    /**
     * Creates a [CardColors] that represents the default container and content colors used in an
     * [OutlinedCard], outlined [AppCard] or [TitleCard].
     *
     * @param contentColor the content color of this [OutlinedCard].
     * @param appNameColor the color used for appName, only applies to [AppCard].
     * @param timeColor the color used for time, applies to [AppCard] and [TitleCard].
     * @param titleColor the color used for title, applies to [AppCard] and [TitleCard].
     * @param subtitleColor the color used for subtitle, applies to [TitleCard].
     */
    @Composable
    public fun outlinedCardColors(
        contentColor: Color = Color.Unspecified,
        appNameColor: Color = Color.Unspecified,
        timeColor: Color = Color.Unspecified,
        titleColor: Color = Color.Unspecified,
        subtitleColor: Color = Color.Unspecified,
    ): CardColors =
        MaterialTheme.colorScheme.defaultOutlinedCardColors.copy(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            appNameColor = appNameColor,
            timeColor = timeColor,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
        )

    /**
     * Creates a [CardColors] that represents the default container and content colors used in a
     * [Card] with image container painter.
     */
    @Composable
    public fun cardWithContainerPainterColors(): CardColors =
        MaterialTheme.colorScheme.defaultCardWithContainerPainterColors

    /**
     * Creates a [CardColors] that represents the default container and content colors used in a
     * [TitleCard] with Image set as a background.
     *
     * @param contentColor the content color of this [Card].
     * @param appNameColor the color used for appName, only applies to [AppCard].
     * @param timeColor the color used for time.
     * @param titleColor the color used for title.
     * @param subtitleColor the color used for subtitle.
     */
    @Composable
    public fun cardWithContainerPainterColors(
        contentColor: Color = Color.Unspecified,
        appNameColor: Color = Color.Unspecified,
        timeColor: Color = Color.Unspecified,
        titleColor: Color = Color.Unspecified,
        subtitleColor: Color = Color.Unspecified,
    ): CardColors =
        MaterialTheme.colorScheme.defaultCardWithContainerPainterColors.copy(
            contentColor = contentColor,
            appNameColor = appNameColor,
            timeColor = timeColor,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
        )

    /**
     * Creates a [Painter] for the background of an [Card] that displays an image with a scrim on
     * top to make sure that any content above the background will be legible.
     *
     * An Image background is a means to reinforce the meaning of information in a Card, e.g. to
     * help to contextualize the information. Cards should have a content color that contrasts with
     * the background image and scrim.
     *
     * @param image The [Painter] to use to draw the container background of the [Card]
     * @param scrim The [Brush] to use to paint a scrim over the container image to ensure that any
     *   text drawn over the image is legible
     * @param sizeToIntrinsics When false (the default), fills the available space within the
     *   container. Pass true to retain the size of the image.
     * @param alignment Specifies alignment of the container image painter relative to the
     *   container.
     * @param contentScale Strategy for scaling the painter if its size does not match the
     *   container.
     * @param alpha Opacity of the container image painter and scrim.
     */
    @Composable
    public fun containerPainter(
        image: Painter,
        scrim: Brush = scrimBrush(),
        sizeToIntrinsics: Boolean = false,
        alignment: Alignment = Alignment.Center,
        contentScale: ContentScale = ContentScale.Fit,
        alpha: Float = DefaultAlpha,
    ): Painter {
        return remember(image, scrim, sizeToIntrinsics, alignment, contentScale, alpha) {
            containerPainter(
                painter = image,
                scrim = scrim,
                sizeToIntrinsics = sizeToIntrinsics,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
            )
        }
    }

    /** Creates a [Brush] for the recommended scrim drawn on top of image container backgrounds. */
    @Composable
    public fun scrimBrush(): Brush {
        val color = scrimColor
        return remember(color) { SolidColor(color) }
    }

    /**
     * Creates a [BorderStroke] that represents the default border used in Outlined Cards.
     *
     * @param outlineColor The color to be used for drawing an outline.
     * @param borderWidth width of the border in [Dp].
     */
    @Composable
    public fun outlinedCardBorder(
        outlineColor: Color = OutlinedCardTokens.ContainerBorderColor.value,
        borderWidth: Dp = OutlinedCardTokens.BorderWidth,
    ): BorderStroke = BorderStroke(borderWidth, outlineColor)

    private val CardHorizontalPadding = 12.dp
    private val CardVerticalPadding = 12.dp

    /**
     * Returns a scrim color that can be used to draw a scrim on top of an image to ensure that any
     * text drawn over the image is legible.
     */
    public val scrimColor: Color
        @ReadOnlyComposable
        @Composable
        get() =
            ImageCardTokens.OverlayScrimColor.value.copy(
                alpha = ImageCardTokens.OverlayScrimOpacity
            )

    /** The default content padding used by [Card] */
    public val ContentPadding: PaddingValues =
        PaddingValues(
            start = CardHorizontalPadding,
            top = CardVerticalPadding,
            end = CardHorizontalPadding,
            bottom = CardVerticalPadding,
        )

    /** Additional bottom padding added for TitleCard with an image background */
    public val ImageBottomPadding: Dp = 12.dp

    /**
     * ContentPadding for use cards that have an image background in order to show more of the image
     */
    public val CardWithContainerPainterContentPadding: PaddingValues =
        PaddingValues(
            start = CardHorizontalPadding,
            top = CardVerticalPadding,
            end = CardHorizontalPadding,
            bottom = CardVerticalPadding + ImageBottomPadding,
        )

    /** The default size of the app icon/image when used inside a [AppCard]. */
    public val AppImageSize: Dp = CardTokens.AppImageSize

    /** The default shape of [Card], which determines its corner radius. */
    public val shape: Shape
        @Composable get() = CardTokens.Shape.value

    /**
     * The default height of [Card], [AppCard] and [TitleCard]. The card will increase its height to
     * accommodate the contents, if necessary.
     */
    public val Height: Dp = CardTokens.ContainerMinHeight

    private val ColorScheme.defaultCardColors: CardColors
        get() {
            return defaultCardColorsCached
                ?: CardColors(
                        containerColor = fromToken(CardTokens.ContainerColor),
                        contentColor = fromToken(CardTokens.ContentColor),
                        appNameColor = fromToken(CardTokens.AppNameColor),
                        timeColor = fromToken(CardTokens.TimeColor),
                        titleColor = fromToken(CardTokens.TitleColor),
                        subtitleColor = fromToken(CardTokens.SubtitleColor),
                    )
                    .also { defaultCardColorsCached = it }
        }

    private val ColorScheme.defaultOutlinedCardColors: CardColors
        get() {
            return defaultOutlinedCardColorsCached
                ?: CardColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(OutlinedCardTokens.ContentColor),
                        appNameColor = fromToken(OutlinedCardTokens.AppNameColor),
                        timeColor = fromToken(OutlinedCardTokens.TimeColor),
                        titleColor = fromToken(OutlinedCardTokens.TitleColor),
                        subtitleColor = fromToken(OutlinedCardTokens.SubtitleColor),
                    )
                    .also { defaultOutlinedCardColorsCached = it }
        }

    private val ColorScheme.defaultCardWithContainerPainterColors: CardColors
        get() {
            return defaultCardWithContainerPainterColorsCached
                ?: CardColors(
                        containerColor = Color.Unspecified,
                        contentColor = fromToken(ImageCardTokens.ContentColor),
                        appNameColor = fromToken(ImageCardTokens.AppNameColor),
                        timeColor = fromToken(ImageCardTokens.TimeColor),
                        titleColor = fromToken(ImageCardTokens.TitleColor),
                        subtitleColor = fromToken(ImageCardTokens.SubtitleColor),
                    )
                    .also { defaultCardWithContainerPainterColorsCached = it }
        }
}

@Composable
internal fun Modifier.cardSizeModifier(): Modifier = defaultMinSize(minHeight = CardDefaults.Height)

/**
 * Represents Colors used in [Card]. Unlike other Material 3 components, Cards do not change their
 * color appearance when they are disabled. All colors remain the same in enabled and disabled
 * states.
 *
 * @param containerColor the background color of this [Card] (overridden by the containerPainter
 *   parameter on Cards with image backgrounds).
 * @param contentColor the content color of this [Card].
 * @param appNameColor the color used for appName, only applies to [AppCard].
 * @param timeColor the color used for time, applies to [AppCard] and [TitleCard].
 * @param titleColor the color used for title, applies to [AppCard] and [TitleCard].
 * @param subtitleColor the color used for subtitle, applies to [TitleCard].
 */
@Immutable
public class CardColors(
    public val containerColor: Color,
    public val contentColor: Color,
    public val appNameColor: Color,
    public val timeColor: Color,
    public val titleColor: Color,
    public val subtitleColor: Color,
) {
    /**
     * Returns a copy of this CardColors, optionally overriding some of the values.
     *
     * @param containerColor The container color of this [Card].
     * @param contentColor The content color of this [Card].
     * @param appNameColor The color used for appName, only applies to [AppCard].
     * @param timeColor The color used for time, applies to [AppCard] and [TitleCard].
     * @param titleColor The color used for title, applies to [AppCard] and [TitleCard].
     * @param subtitleColor The color used for subtitle, applies to [TitleCard].
     */
    public fun copy(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        appNameColor: Color = Color.Unspecified,
        timeColor: Color = Color.Unspecified,
        titleColor: Color = Color.Unspecified,
        subtitleColor: Color = Color.Unspecified,
    ): CardColors =
        CardColors(
            containerColor = containerColor.takeOrElse { this.containerColor },
            contentColor = contentColor.takeOrElse { this.contentColor },
            appNameColor = appNameColor.takeOrElse { this.appNameColor },
            timeColor = timeColor.takeOrElse { this.timeColor },
            titleColor = titleColor.takeOrElse { this.titleColor },
            subtitleColor = subtitleColor.takeOrElse { this.subtitleColor },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CardColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (appNameColor != other.appNameColor) return false
        if (timeColor != other.timeColor) return false
        if (titleColor != other.titleColor) return false
        if (subtitleColor != other.subtitleColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + appNameColor.hashCode()
        result = 31 * result + timeColor.hashCode()
        result = 31 * result + titleColor.hashCode()
        result = 31 * result + subtitleColor.hashCode()
        return result
    }
}

@Composable
internal fun CardImpl(
    onClick: (() -> Unit)?,
    containerPainter: Painter?,
    modifier: Modifier,
    onLongClick: (() -> Unit)?,
    onLongClickLabel: String?,
    enabled: Boolean,
    shape: Shape,
    colors: CardColors,
    border: BorderStroke?,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource?,
    transformation: SurfaceTransformation?,
    content: @Composable ColumnScope.() -> Unit,
) =
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .surface(
                    transformation = transformation,
                    painter = containerPainter ?: ColorPainter(colors.containerColor),
                    shape = shape,
                    border = border,
                )
                .then(
                    if (onClick != null)
                        Modifier.combinedClickable(
                            enabled = enabled,
                            onClick = onClick,
                            onLongClick =
                                onLongClick, // NB combinedClickable calls LongPress haptic
                            onLongClickLabel = onLongClickLabel,
                            role = null,
                            indication = ripple(),
                            interactionSource = interactionSource,
                        )
                    else
                        Modifier.focusable(enabled = true, interactionSource = interactionSource)
                            .semantics(mergeDescendants = true) {}
                )
                .padding(contentPadding),
        content = content,
    )

@Composable
internal fun CardImpl(
    onClick: (() -> Unit)?,
    containerPainter: Painter?,
    title: @Composable RowScope.() -> Unit,
    modifier: Modifier,
    onLongClick: (() -> Unit)?,
    onLongClickLabel: String?,
    time: @Composable (() -> Unit)?,
    subtitle: @Composable (ColumnScope.() -> Unit)?,
    enabled: Boolean,
    shape: Shape,
    colors: CardColors,
    border: BorderStroke?,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource?,
    transformation: SurfaceTransformation?,
    content: @Composable (() -> Unit)?,
) {
    val timeWithTextStyle: @Composable () -> Unit = {
        time?.let {
            CompositionLocalProvider(
                values =
                    arrayOf(
                        LocalContentColor provides colors.timeColor,
                        LocalTextStyle provides CardTokens.TimeTypography.value,
                    ),
                content = time,
            )
        }
    }

    CardImpl(
        onClick = onClick,
        containerPainter = containerPainter,
        modifier = modifier.cardSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        colors = colors,
        border = border,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        shape = shape,
        transformation = transformation,
    ) {
        // NB We are in ColumnScope, so spacing between elements will be done with Spacer using
        // Modifier.height().
        if (content == null && time != null) {
            timeWithTextStyle()
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Row(Modifier.weight(1f)) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.titleColor,
                    LocalTextStyle provides CardTokens.TitleTypography.value,
                ) {
                    title()
                }
            }
            if (content != null) {
                Spacer(modifier = Modifier.width(4.dp))
                timeWithTextStyle()
            }
        }
        content?.let {
            Spacer(modifier = Modifier.height(2.dp))
            CompositionLocalProvider(
                values =
                    arrayOf(
                        LocalContentColor provides colors.contentColor,
                        LocalTextStyle provides CardTokens.ContentTypography.value,
                    ),
                content = content,
            )
        }
        subtitle?.let {
            Spacer(modifier = Modifier.height(if (time == null && content == null) 2.dp else 6.dp))
            CompositionLocalProvider(
                LocalContentColor provides colors.subtitleColor,
                LocalTextStyle provides CardTokens.SubtitleTypography.value,
            ) {
                subtitle()
            }
        }
    }
}

@Composable
internal fun AppCardImpl(
    onClick: (() -> Unit)?,
    appName: @Composable RowScope.() -> Unit,
    title: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = CardDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    transformation: SurfaceTransformation? = null,
    appImage: @Composable (RowScope.() -> Unit)? = null,
    time: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardImpl(
        onClick = onClick,
        containerPainter = null,
        modifier = modifier.cardSizeModifier(),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        colors = colors,
        border = border,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        transformation = transformation,
        shape = shape,
    ) {
        // NB We are in ColumnScope, so spacing between elements will be done with Spacer using
        // Modifier.height().
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                appImage?.let {
                    appImage()
                    Spacer(Modifier.width(4.dp))
                }
                CompositionLocalProvider(
                    LocalContentColor provides colors.appNameColor,
                    LocalTextStyle provides CardTokens.AppNameTypography.value,
                ) {
                    appName()
                }
            }

            time?.let {
                Spacer(Modifier.width(6.dp))
                CompositionLocalProvider(
                    LocalContentColor provides colors.timeColor,
                    LocalTextStyle provides CardTokens.TimeTypography.value,
                ) {
                    time()
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            content = {
                CompositionLocalProvider(
                    LocalContentColor provides colors.titleColor,
                    LocalTextStyle provides CardTokens.TitleTypography.value,
                ) {
                    title()
                }
            },
        )
        Spacer(modifier = Modifier.height(2.dp))
        CompositionLocalProvider(
            LocalContentColor provides colors.contentColor,
            LocalTextStyle provides CardTokens.ContentTypography.value,
        ) {
            content()
        }
    }
}
