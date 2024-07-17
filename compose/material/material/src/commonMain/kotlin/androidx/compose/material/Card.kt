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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * <a href="https://material.io/components/cards" class="external" target="_blank">Material Design
 * card</a>.
 *
 * Cards contain content and actions about a single subject.
 *
 * ![Cards
 * image](https://developer.android.com/images/reference/androidx/compose/material/cards.png)
 *
 * This version of Card will block clicks behind it. For clickable card, please use another overload
 * that accepts `onClick` as a parameter.
 *
 * @sample androidx.compose.material.samples.CardSample
 * @param modifier Modifier to be applied to the layout of the card.
 * @param shape Defines the card's shape as well its shadow. A shadow is only displayed if the
 *   [elevation] is greater than zero.
 * @param backgroundColor The background color.
 * @param contentColor The preferred content color provided by this card to its children. Defaults
 *   to either the matching content color for [backgroundColor], or if [backgroundColor] is not a
 *   color from the theme, this will keep the same value set above this card.
 * @param border Optional border to draw on top of the card
 * @param elevation The z-coordinate at which to place this card. This controls the size of the
 *   shadow below the card.
 * @param content The content displayed on the card.
 */
@Composable
@NonRestartableComposable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    border: BorderStroke? = null,
    elevation: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        elevation = elevation,
        border = border,
        content = content
    )
}

/**
 * Cards are [Surface]s that display content and actions on a single topic.
 *
 * This version of Card provides click handling as well. If you do not want Card to handle clicks,
 * consider using another overload.
 *
 * @sample androidx.compose.material.samples.ClickableCardSample
 * @param onClick callback to be called when the card is clicked
 * @param modifier Modifier to be applied to the layout of the card.
 * @param enabled Controls the enabled state of the card. When `false`, this card will not be
 *   clickable
 * @param shape Defines the card's shape as well its shadow. A shadow is only displayed if the
 *   [elevation] is greater than zero.
 * @param backgroundColor The background color.
 * @param contentColor The preferred content color provided by this card to its children. Defaults
 *   to either the matching content color for [backgroundColor], or if [backgroundColor] is not a
 *   color from the theme, this will keep the same value set above this card.
 * @param border Optional border to draw on top of the card
 * @param elevation The z-coordinate at which to place this card. This controls the size of the
 *   shadow below the card.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content The content displayed on the card.
 */
@ExperimentalMaterialApi
@Composable
@NonRestartableComposable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    border: BorderStroke? = null,
    elevation: Dp = 1.dp,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        border = border,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content
    )
}
