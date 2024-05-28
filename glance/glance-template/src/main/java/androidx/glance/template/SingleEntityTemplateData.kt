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

package androidx.glance.template

/**
 * The semantic data required to build Single Entity Template layouts. The template allows for a
 * header, text section with up to three text items, main image, and single action button.
 *
 * @param headerBlock The header block of the entity by [HeaderBlock].
 * @param textBlock The text block for up to three types of texts for the entity.
 * @param imageBlock The image block for the entity main image by [ImageBlock].
 * @param actionBlock The entity single action button by [ActionBlock].
 */
class SingleEntityTemplateData(
    val headerBlock: HeaderBlock? = null,
    val textBlock: TextBlock? = null,
    val imageBlock: ImageBlock? = null,
    val actionBlock: ActionBlock? = null,
) {
    override fun hashCode(): Int {
        var result = headerBlock?.hashCode() ?: 0
        result = 31 * result + (textBlock?.hashCode() ?: 0)
        result = 31 * result + (imageBlock?.hashCode() ?: 0)
        result = 31 * result + (actionBlock?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SingleEntityTemplateData

        if (headerBlock != other.headerBlock) return false
        if (textBlock != other.textBlock) return false
        if (imageBlock != other.imageBlock) return false
        if (actionBlock != other.actionBlock) return false

        return true
    }
}
