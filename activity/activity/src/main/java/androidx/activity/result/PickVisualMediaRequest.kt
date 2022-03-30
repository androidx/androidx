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

import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo

/**
 * Creates a request for a
 * [androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia] or
 * [androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia] Activity Contract.
 *
 * @param mediaType type to go into the PickVisualMediaRequest
 *
 * @return a PickVisualMediaRequest that contains the given input
 */
fun PickVisualMediaRequest(
    mediaType: VisualMediaType = ImageAndVideo
) = PickVisualMediaRequest.Builder().setMediaType(mediaType).build()

/**
 * A request for a
 * [androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia] or
 * [androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia] Activity Contract.
 */
class PickVisualMediaRequest internal constructor() {

    var mediaType: VisualMediaType = ImageAndVideo
        private set

    /**
     * A builder for constructing [PickVisualMediaRequest] instances.
     */
    class Builder {

        private var mediaType: VisualMediaType = ImageAndVideo

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
         * Build the PickVisualMediaRequest specified by this builder.
         *
         * @return the newly constructed PickVisualMediaRequest.
         */
        fun build(): PickVisualMediaRequest = PickVisualMediaRequest(mediaType).apply {
            this.mediaType = mediaType
        }
    }
}
