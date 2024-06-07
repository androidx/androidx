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

package androidx.activity.result

import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.annotation.IntRange

/**
 * Creates a request for a
 * [androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia] or
 * [androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia] Activity Contract.
 *
 * @param mediaType type to go into the PickVisualMediaRequest
 * @return a PickVisualMediaRequest that contains the given input
 */
@Deprecated(
    "Superseded by PickVisualMediaRequest that takes an optional maxItems",
    level = DeprecationLevel.HIDDEN
) // Binary API compatibility.
fun PickVisualMediaRequest(mediaType: VisualMediaType = ImageAndVideo) =
    PickVisualMediaRequest.Builder().setMediaType(mediaType).build()

/**
 * Creates a request for a
 * [androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia] or
 * [androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia] Activity Contract.
 *
 * @param mediaType type to go into the PickVisualMediaRequest
 * @param maxItems limit the number of selectable items when using [PickMultipleVisualMedia]
 * @return a PickVisualMediaRequest that contains the given input
 */
@Suppress("MissingJvmstatic")
fun PickVisualMediaRequest(
    mediaType: VisualMediaType = ImageAndVideo,
    @IntRange(from = 2) maxItems: Int = PickMultipleVisualMedia.getMaxItems()
) = PickVisualMediaRequest.Builder().setMediaType(mediaType).setMaxItems(maxItems).build()

/**
 * A request for a
 * [androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia] or
 * [androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia] Activity Contract.
 */
class PickVisualMediaRequest internal constructor() {

    var mediaType: VisualMediaType = ImageAndVideo
        internal set

    var maxItems: Int = PickMultipleVisualMedia.getMaxItems()
        internal set

    /** A builder for constructing [PickVisualMediaRequest] instances. */
    class Builder {

        private var mediaType: VisualMediaType = ImageAndVideo
        private var maxItems: Int = PickMultipleVisualMedia.getMaxItems()

        /**
         * Set the media type for the [PickVisualMediaRequest].
         *
         * The type is the mime type to filter by, e.g. `PickVisualMedia.ImageOnly`,
         * `PickVisualMedia.ImageAndVideo`, `PickVisualMedia.SingleMimeType("image/gif")`
         *
         * @param mediaType type to go into the PickVisualMediaRequest
         * @return This builder.
         */
        fun setMediaType(mediaType: VisualMediaType): Builder {
            this.mediaType = mediaType
            return this
        }

        /**
         * Limit the number of selectable items in the photo picker when using
         * [PickMultipleVisualMedia]
         *
         * @param maxItems int type limiting the number of selectable items
         * @return This builder.
         */
        fun setMaxItems(@IntRange(from = 2) maxItems: Int): Builder {
            this.maxItems = maxItems
            return this
        }

        /**
         * Build the PickVisualMediaRequest specified by this builder.
         *
         * @return the newly constructed PickVisualMediaRequest.
         */
        fun build(): PickVisualMediaRequest =
            PickVisualMediaRequest().apply {
                this.mediaType = this@Builder.mediaType
                this.maxItems = this@Builder.maxItems
            }
    }
}
