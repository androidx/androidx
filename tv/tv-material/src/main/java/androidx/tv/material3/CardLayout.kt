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

package androidx.tv.material3

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * [StandardCardLayout] is an opinionated TV Material Card layout with an image and text content
 * to show information about a subject.
 *
 * It provides a vertical layout with an image card slot at the top. And below that, there are
 * slots for the title, subtitle and description.
 *
 * @param imageCard defines the [Composable] to be used for the image card. See
 * [CardLayoutDefaults.ImageCard] to create an image card. The `interactionSource` param provided
 * in the lambda function should be forwarded and used with the image card composable.
 * @param title defines the [Composable] title placed below the image card in the CardLayout.
 * @param modifier the [Modifier] to be applied to this CardLayout.
 * @param subtitle defines the [Composable] supporting text placed below the title in CardLayout.
 * @param description defines the [Composable] description placed below the subtitle in CardLayout.
 * @param contentColor [CardLayoutColors] defines the content color used in the CardLayout
 * for different interaction states. See [CardLayoutDefaults.contentColor].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this CardLayout. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this card layout in different states.
 * This interaction source param would also be forwarded to be used with the `imageCard` composable.
 */
@ExperimentalTvMaterial3Api
@Composable
fun StandardCardLayout(
    imageCard: @Composable (interactionSource: MutableInteractionSource) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: @Composable () -> Unit = {},
    description: @Composable () -> Unit = {},
    contentColor: CardLayoutColors = CardLayoutDefaults.contentColor(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    Column(
        modifier = modifier
    ) {
        Box(
            contentAlignment = CardDefaults.ContentImageAlignment,
        ) {
            imageCard(interactionSource)
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CardLayoutContent(
                title = title,
                subtitle = subtitle,
                description = description,
                contentColor = contentColor.color(
                    focused = focused,
                    pressed = pressed
                )
            )
        }
    }
}

/**
 * [WideCardLayout] is an opinionated TV Material Card layout with an image and text content
 * to show information about a subject.
 *
 * It provides a horizontal layout with an image card slot at the start, followed by the title,
 * subtitle and description at the end.
 *
 * @param imageCard defines the [Composable] to be used for the image card. See
 * [CardLayoutDefaults.ImageCard] to create an image card. The `interactionSource` param provided
 * in the lambda function should to be forwarded and used with the image card composable.
 * @param title defines the [Composable] title placed below the image card in the CardLayout.
 * @param modifier the [Modifier] to be applied to this CardLayout.
 * @param subtitle defines the [Composable] supporting text placed below the title in CardLayout.
 * @param description defines the [Composable] description placed below the subtitle in CardLayout.
 * @param contentColor [CardLayoutColors] defines the content color used in the CardLayout
 * for different interaction states. See [CardLayoutDefaults.contentColor].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this CardLayout. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this card layout in different states.
 * This interaction source param would also be forwarded to be used with the `imageCard` composable.
 */
@ExperimentalTvMaterial3Api
@Composable
fun WideCardLayout(
    imageCard: @Composable (interactionSource: MutableInteractionSource) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: @Composable () -> Unit = {},
    description: @Composable () -> Unit = {},
    contentColor: CardLayoutColors = CardLayoutDefaults.contentColor(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    Row(
        modifier = modifier
    ) {
        Box(
            contentAlignment = CardDefaults.ContentImageAlignment
        ) {
            imageCard(interactionSource)
        }
        Column {
            CardLayoutContent(
                title = title,
                subtitle = subtitle,
                description = description,
                contentColor = contentColor.color(
                    focused = focused,
                    pressed = pressed
                )
            )
        }
    }
}

@Composable
internal fun CardLayoutContent(
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit = {},
    description: @Composable () -> Unit = {},
    contentColor: Color
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        CardContent(title, subtitle, description)
    }
}

@ExperimentalTvMaterial3Api
object CardLayoutDefaults {
    /**
     * Creates [CardLayoutColors] that represents the default content colors used in a
     * CardLayout.
     *
     * @param contentColor the default content color of this CardLayout.
     * @param focusedContentColor the content color of this CardLayout when focused.
     * @param pressedContentColor the content color of this CardLayout when pressed.
     */
    @ReadOnlyComposable
    @Composable
    fun contentColor(
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedContentColor: Color = contentColor,
        pressedContentColor: Color = focusedContentColor
    ) = CardLayoutColors(
        contentColor = contentColor,
        focusedContentColor = focusedContentColor,
        pressedContentColor = pressedContentColor
    )

    /**
     * [ImageCard] is basically a [Card] composable with an image as the content. It is recommended
     * to be used with the different CardLayout(s).
     *
     * This Card handles click events, calling its [onClick] lambda.
     *
     * @param onClick called when this card is clicked
     * @param interactionSource the [MutableInteractionSource] representing the stream of
     * [Interaction]s for this card. When using with the CardLayout(s), it is recommended to
     * pass in the interaction state obtained from the parent lambda.
     * @param modifier the [Modifier] to be applied to this card
     * @param shape [CardShape] defines the shape of this card's container in different interaction
     * states. See [CardDefaults.shape].
     * @param colors [CardColors] defines the background & content colors used in this card for
     * different interaction states. See [CardDefaults.colors].
     * @param scale [CardScale] defines size of the card relative to its original size for different
     * interaction states. See [CardDefaults.scale].
     * @param border [CardBorder] defines a border around the card for different interaction states.
     * See [CardDefaults.border].
     * @param glow [CardGlow] defines a shadow to be shown behind the card for different interaction
     * states. See [CardDefaults.glow].
     * @param content defines the image content [Composable] to be displayed inside the Card.
     */
    @Composable
    fun ImageCard(
        onClick: () -> Unit,
        interactionSource: MutableInteractionSource,
        modifier: Modifier = Modifier,
        shape: CardShape = CardDefaults.shape(),
        colors: CardColors = CardDefaults.colors(),
        scale: CardScale = CardDefaults.scale(),
        border: CardBorder = CardDefaults.border(),
        glow: CardGlow = CardDefaults.glow(),
        content: @Composable () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            scale = scale,
            border = border,
            glow = glow,
            interactionSource = interactionSource
        ) {
            content()
        }
    }
}

/**
 * Represents the [Color] of content in a CardLayout for different interaction states.
 */
@ExperimentalTvMaterial3Api
@Immutable
class CardLayoutColors internal constructor(
    internal val contentColor: Color,
    internal val focusedContentColor: Color,
    internal val pressedContentColor: Color,
) {
    /**
     * Returns the content color [Color] for different interaction states.
     */
    internal fun color(
        focused: Boolean,
        pressed: Boolean
    ): Color {
        return when {
            focused -> focusedContentColor
            pressed -> pressedContentColor
            else -> contentColor
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CardLayoutColors

        if (contentColor != other.contentColor) return false
        if (focusedContentColor != other.focusedContentColor) return false
        if (pressedContentColor != other.pressedContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentColor.hashCode()
        result = 31 * result + focusedContentColor.hashCode()
        result = 31 * result + pressedContentColor.hashCode()
        return result
    }

    override fun toString(): String {
        return "CardLayoutContentColor(" +
            "contentColor=$contentColor, " +
            "focusedContentColor=$focusedContentColor, " +
            "pressedContentColor=$pressedContentColor)"
    }
}
