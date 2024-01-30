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

package androidx.wear.compose.materialcore

import androidx.annotation.RestrictTo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Base level Wear Material [Card] that offers a single slot to take any content.
 *
 * Is used as the container for more opinionated [Card] components that take specific content such
 * as icons, images, titles, subtitles and labels.
 *
 * Cards can be enabled or disabled. A disabled card will not respond to click events.
 *
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards)
 * Wear OS Material design guide.
 *
 * @param onClick Will be called when the user clicks the card
 * @param modifier Modifier to be applied to the card
 * @param border A BorderStroke object which is used for the outline drawing.
 * Can be null - then outline will not be drawn
 * @param containerPainter A painter used to paint the background of the card. A card will
 * normally have a gradient background. Use [CardDefaults.cardBackgroundPainter()] to obtain an
 * appropriate painter
 * @param containerPainter A Painter which is used for background drawing.
 * @param enabled Controls the enabled state of the card. When false, this card will not
 * be clickable and there will be no ripple effect on click. Wear cards do not have any specific
 * elevation or alpha differences when not enabled - they are simply not clickable.
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
 * @param content A main slot for a content of this card
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun Card(
    onClick: () -> Unit,
    modifier: Modifier,
    border: BorderStroke?,
    containerPainter: Painter,
    enabled: Boolean,
    contentPadding: PaddingValues,
    shape: Shape,
    interactionSource: MutableInteractionSource,
    role: Role?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape = shape)
            .paint(
                painter = containerPainter,
                contentScale = ContentScale.Crop
            )
            .clickable(
                enabled = enabled,
                onClick = onClick,
                role = role,
                indication = rememberRipple(),
                interactionSource = interactionSource,
            )
            .then(
                border?.let { Modifier.border(border = border, shape = shape) } ?: Modifier
            )
            .padding(contentPadding),
        content = content
    )
}

/**
 * Opinionated Wear Material [Card] that offers a specific 5 slot layout to show information about
 * an application, e.g. a notification. AppCards are designed to show interactive elements from
 * multiple applications. They will typically be used by the system UI, e.g. for showing a list of
 * notifications from different applications. However it could also be adapted by individual
 * application developers to show information about different parts of their application.
 *
 * The first row of the layout has three slots, 1) a small optional application [Image] or [Icon],
 * 2) an application name, it is expected to be a short start-aligned [Text] composable,
 * and 3) the optional time that the application activity has occurred which will be
 * shown on the top row of the card, this is expected to be an end aligned [Text] composable
 * showing a time relevant to the contents of the [Card].
 *
 * The second row shows a title, this is expected to be a single row of start aligned [Text].
 *
 * The rest of the [Card] contains the content which can be either [Text] or an [Image].
 * If the content is text it can be single or multiple line and is expected to be Top and Start
 * aligned.
 *
 * If more than one composable is provided in the content slot it is the responsibility of the
 * caller to determine how to layout the contents, e.g. provide either a row or a column.
 *
 * For more information, see the
 * [Cards](https://developer.android.com/training/wearables/components/cards)
 * guide.
 *
 * @param onClick Will be called when the user clicks the card
 * @param modifier Modifier to be applied to the card
 * @param enabled Controls the enabled state of the card. When false, this card will not
 * be clickable and there will be no ripple effect on click. Wear cards do not have any specific
 * elevation or alpha differences when not enabled - they are simply not clickable.
 * @param border A BorderStroke object which is used for the outline drawing.
 * Can be null - then outline will not be drawn
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param containerPainter A Painter which is used for background drawing.
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this card. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this card in different [Interaction]s.
 * @param shape Defines the card's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme
 * @param appImage A slot for a small [Image] associated with the application.
 * @param appName A slot for displaying the application name, expected to be a single line of start
 * aligned text.
 * @param time A slot for displaying the time relevant to the contents of the card, expected to be a
 * short piece of end aligned text.
 * @param title A slot for displaying the title of the card, expected to be one or two lines of
 * start aligned text.
 * @param content A main slot for a content of this card
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    border: BorderStroke?,
    contentPadding: PaddingValues,
    containerPainter: Painter,
    interactionSource: MutableInteractionSource,
    shape: Shape,
    appImage: @Composable (RowScope.() -> Unit)?,
    appName: @Composable RowScope.() -> Unit,
    time: @Composable (RowScope.() -> Unit)?,
    title: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerPainter = containerPainter,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        role = null,
        shape = shape
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                appImage?.let {
                    appImage()
                    Spacer(Modifier.width(6.dp))
                }
                appName()
                Spacer(modifier = Modifier.weight(1.0f))

                time?.let {
                    time()
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                content = title
            )
            content()
        }
    }
}
