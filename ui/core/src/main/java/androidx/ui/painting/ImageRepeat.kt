/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.painting

/** How to paint any portions of a box not covered by an image. */
enum class ImageRepeat {
    /** Repeat the image in both the x and y directions until the box is filled. */
    repeat,

    /** Repeat the image in the x direction until the box is filled horizontally. */
    repeatX,

    /** Repeat the image in the y direction until the box is filled vertically. */
    repeatY,

    /** Leave uncovered portions of the box transparent. */
    noRepeat
}