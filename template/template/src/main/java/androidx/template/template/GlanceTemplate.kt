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

package androidx.template.template

import androidx.glance.action.Action
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.glance.ImageProvider

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
 * @param text string to be displayed
 * @param type the [Type] of the item, used for styling
 */
public class TemplateText(val text: String, val type: Type) {

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

    /**
     * The text types that can be used with templates. Types are set on [TemplateText] items and
     * can be used by templates to determine text styling.
     */
    public enum class Type {
        Display,
        Title,
        Label,
        Body
    }
}

/**
 * Contains the information required to display an image on a template.
 *
 * @param image The image to display
 * @param description The image description, usually used as alt text
 * @param cornerRadius The image corner radius in Dp
 */
public class TemplateImageWithDescription(
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
 * Contains the information required to display a button on a template.
 *
 * @param action The onClick action
 */
public sealed class TemplateButton(val action: Action) {

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
public class TemplateTextButton(action: Action, val text: String) : TemplateButton(action) {

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
public class TemplateImageButton(
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
