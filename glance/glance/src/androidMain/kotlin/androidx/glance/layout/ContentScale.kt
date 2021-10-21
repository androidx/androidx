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

package androidx.glance.layout

/**
 * Scaling to be used when an element is smaller than its available bounds. Mainly used by
 * [Image] to dictate how the image should be drawn within the [Image] element's bounding box.
 */
@Suppress("INLINE_CLASS_DEPRECATED")
public inline class ContentScale(private val value: Int) {
    public companion object {
        /**
         * Scale the source uniformly (maintaining the source's aspect ratio) so that both
         * dimensions (width and height) of the source will be equal to or larger than the
         * corresponding dimension of the destination.
         */
        public val Crop: ContentScale = ContentScale(0)

        /**
         * Scale the source uniformly (maintaining the source's aspect ratio) so that both
         * dimensions (width and height) of the source will be equal to or less than the
         * corresponding dimension of the destination
         */
        public val Fit: ContentScale = ContentScale(1)

        /**
         * Scale horizontal and vertically non-uniformly to fill the destination bounds.
         */
        public val FillBounds: ContentScale = ContentScale(2)
    }
}
