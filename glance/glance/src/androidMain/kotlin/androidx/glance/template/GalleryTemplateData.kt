/*
 * Copyright 2022 The Android Open Source Project
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
 * The semantic data required to build Gallery Template layouts
 *
 * @param header The header of the template.
 * @param mainTextBlock The head block for title, body, and other texts of the main gallery object.
 * @param mainImageBlock The head block for an image of the main gallery object.
 * @param mainActionBlock The head block for a list of action buttons for the main gallery object.
 * @param galleryImageBlock The gallery block for a list of gallery images.
 */
class GalleryTemplateData(
    val header: HeaderBlock? = null,
    val mainTextBlock: TextBlock,
    val mainImageBlock: ImageBlock,
    val mainActionBlock: ActionBlock? = null,
    val galleryImageBlock: ImageBlock,
) {
    override fun hashCode(): Int {
        var result = mainTextBlock.hashCode()
        result = 31 * result + (header?.hashCode() ?: 0)
        result = 31 * result + mainImageBlock.hashCode()
        result = 31 * result + (mainActionBlock?.hashCode() ?: 0)
        result = 31 * result + galleryImageBlock.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GalleryTemplateData

        if (header != other.header) return false
        if (mainTextBlock != other.mainTextBlock) return false
        if (mainImageBlock != other.mainImageBlock) return false
        if (mainActionBlock != other.mainActionBlock) return false
        if (galleryImageBlock != other.galleryImageBlock) return false

        return true
    }
}
