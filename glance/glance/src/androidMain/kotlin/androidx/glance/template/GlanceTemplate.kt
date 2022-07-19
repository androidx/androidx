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

package androidx.glance.template

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ImageProvider
import androidx.glance.action.Action

// TODO: Expand display context to include features other than orientation
/** The glanceable display orientation */
enum class TemplateMode {
    Collapsed,
    Vertical,
    Horizontal
}

/**
 * Contains the information required to display a string on a template.
 *
 * @param text The string to be displayed.
 * @param type The [TextType] of the item, used for styling. Default to [TextType.Title].
 */
class TemplateText(val text: String, val type: TextType = TextType.Title) {

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TemplateText

        if (text != other.text) return false
        if (type != other.type) return false

        return true
    }
}

/**
 * Contains the information required to display an image on a template.
 *
 * @param image The image to display
 * @param description The image description, usually used as alt text
 * @param cornerRadius The image corner radius in Dp
 */
class TemplateImageWithDescription(
    val image: ImageProvider,
    val description: String,
    val cornerRadius: Dp = 16.dp
) {

    override fun hashCode(): Int =
        31 * image.hashCode() + description.hashCode() + cornerRadius.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TemplateImageWithDescription
        return image == other.image &&
            description == other.description &&
            cornerRadius == other.cornerRadius
    }
}

/**
 * Base class for a button taking an [Action] without display oriented information.
 *
 * @param action The action to take when this button is clicked.
 */
sealed class TemplateButton(val action: Action) {

    override fun hashCode(): Int = action.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return action == (other as TemplateButton).action
    }
}

/**
 * A text based [TemplateButton].
 *
 * @param action The onClick action
 * @param text The button display text
 */
class TemplateTextButton(action: Action, val text: String) : TemplateButton(action) {

    override fun hashCode(): Int = 31 * super.hashCode() + text.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        return text == (other as TemplateTextButton).text
    }
}

/**
 * An image based [TemplateButton].
 *
 * @param action The onClick action
 * @param image The button image
 */
class TemplateImageButton(
    action: Action,
    val image: TemplateImageWithDescription
) : TemplateButton(action) {

    override fun hashCode(): Int = 31 * super.hashCode() + image.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        return image == (other as TemplateImageButton).image
    }
}

/**
 * A block of text with up to three different [TextType] of text lines that are displayed by the
 * text index order (for example, text1 is displayed first). The block also has a priority number
 * relative to other blocks such as an [ImageBlock]. Lower numbered block has higher priority to be
 * displayed first.
 *
 * @param text1 The text displayed first within the block.
 * @param text2 The text displayed second within the block.
 * @param text3 The text displayed third  within the block.
 * @param priority The display priority number relative to other blocks. Default to the highest: 0.
 */
class TextBlock(
    val text1: TemplateText,
    val text2: TemplateText? = null,
    val text3: TemplateText? = null,
    val priority: Int = 0,
) {
    override fun hashCode(): Int {
        var result = text1.hashCode()
        result = 31 * result + (text2?.hashCode() ?: 0)
        result = 31 * result + (text3?.hashCode() ?: 0)
        result = 31 * result + priority.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextBlock

        if (text1 != other.text1) return false
        if (text2 != other.text2) return false
        if (text3 != other.text3) return false
        if (priority != other.priority) return false

        return true
    }
}

/**
 * A block of image sequence by certain size and aspect ratio preferences and display priority
 * relative to other blocks such as a [TextBlock].
 *
 * @param images The sequence of images or just one image for display. Default to empty list.
 * @param aspectRatio The preferred aspect ratio of the images. Default to [AspectRatio.Ratio1x1].
 * @param size The preferred size type of the images. Default to [ImageSize.Small].
 * @param priority The display priority number relative to other blocks such as a [TextBlock].
 * Default to the highest priority number 0.
 */
class ImageBlock(
    val images: List<TemplateImageWithDescription> = listOf(),
    val aspectRatio: AspectRatio = AspectRatio.Ratio1x1,
    val size: ImageSize = ImageSize.Small,
    val priority: Int = 0,
) {
    override fun hashCode(): Int {
        var result = images.hashCode()
        result = 31 * result + aspectRatio.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + priority.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageBlock

        if (images != other.images) return false
        if (aspectRatio != other.aspectRatio) return false
        if (size != other.size) return false
        if (priority != other.priority) return false

        return true
    }
}

/**
 * Block of action list of text or image buttons.
 *
 * @param actionButtons The list of action buttons. Default to empty list.
 * @param type The type of action buttons. Default to [ButtonType.Icon]
 */
class ActionBlock(
    val actionButtons: List<TemplateButton> = listOf(),
    val type: ButtonType = ButtonType.Icon,
) {
    override fun hashCode(): Int {
        var result = actionButtons.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ActionBlock

        if (actionButtons != other.actionButtons) return false
        if (type != other.type) return false

        return true
    }
}

/**
 * A header for the whole template.
 *
 * @param text The header text.
 * @param icon The header image icon.
 * @param actionBlock The header action buttons.
 */
class HeaderBlock(
    val text: TemplateText,
    val icon: TemplateImageWithDescription? = null,
    val actionBlock: ActionBlock? = null,
) {
    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (icon?.hashCode() ?: 0)
        result = 31 * result + (actionBlock?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HeaderBlock

        if (text != other.text) return false
        if (icon != other.icon) return false
        if (actionBlock != other.actionBlock) return false

        return true
    }
}

/**
 * The aspect ratio of an image.
 */
@JvmInline
value class AspectRatio private constructor(private val value: Int) {
    companion object {
        /**
         * The aspect ratio of 1 x 1.
         */
        val Ratio1x1: AspectRatio = AspectRatio(0)

        /**
         * The aspect ratio of 16 x 9.
         */
        val Ratio16x9: AspectRatio = AspectRatio(1)

        /**
         * The aspect ratio of 2 x 3.
         */
        val Ratio2x3: AspectRatio = AspectRatio(2)
    }
}

/**
 * The relative image size as a hint.
 */
@JvmInline
value class ImageSize private constructor(private val value: Int) {
    companion object {
        /**
         * Relative small sized image.
         */
        val Small: ImageSize = ImageSize(0)

        /**
         * Relative medium sized image.
         */
        val Medium: ImageSize = ImageSize(1)

        /**
         * Relative large sized image.
         */
        val Large: ImageSize = ImageSize(2)
    }
}

/**
 * The type of button such as FAB/Icon/Text/IconText types
 */
@JvmInline
value class ButtonType private constructor(private val value: Int) {
    companion object {
        /**
         * FAB (Floating Action Button) type of image button.
         */
        val Fab: ButtonType = ButtonType(0)

        /**
         * Icon image button type.
         */
        val Icon: ButtonType = ButtonType(1)

        /**
         * Text button type.
         */
        val Text: ButtonType = ButtonType(2)

        /**
         * Button with Text and Icon type.
         */
        val TextIcon: ButtonType = ButtonType(3)
    }
}

/**
 * The text types that can be used with templates such as set in [TemplateText] items to determine
 * text styling.
 */
@JvmInline
value class TextType private constructor(private val value: Int) {
    companion object {
        /**
         * The text is for display with large font size.
         */
        val Display: TextType = TextType(0)

        /**
         * The text is for title content with medium font size.
         */
        val Title: TextType = TextType(1)

        /**
         * The text is for label content with small font size.
         */
        val Label: TextType = TextType(2)

        /**
         * The text is for body content with small font size.
         */
        val Body: TextType = TextType(3)

        /**
         * The text is headline with small font size.
         */
        val Headline: TextType = TextType(4)
    }
}
