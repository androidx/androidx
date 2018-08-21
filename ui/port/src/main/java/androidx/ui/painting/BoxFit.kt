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

// / How a box should be inscribed into another box.
// /
// / See also [applyBoxFit], which applies the sizing semantics of these values
// / (though not the alignment semantics).
enum class BoxFit {
    // / Fill the target box by distorting the source's aspect ratio.
    // /
    // / ![](https://flutter.github.io/assets-for-api-docs/assets/painting/box_fit_fill.png)
    fill,

    // / As large as possible while still containing the source entirely within the
    // / target box.
    // /
    // / ![](https://flutter.github.io/assets-for-api-docs/assets/painting/box_fit_contain.png)
    contain,

    // / As small as possible while still covering the entire target box.
    // /
    // / ![](https://flutter.github.io/assets-for-api-docs/assets/painting/box_fit_cover.png)
    cover,

    // / Make sure the full width of the source is shown, regardless of
    // / whether this means the source overflows the target box vertically.
    // /
    // / ![](https://flutter.github.io/assets-for-api-docs/assets/painting/box_fit_fitWidth.png)
    fitWidth,

    // / Make sure the full height of the source is shown, regardless of
    // / whether this means the source overflows the target box horizontally.
    // /
    // / ![](https://flutter.github.io/assets-for-api-docs/assets/painting/box_fit_fitHeight.png)
    fitHeight,

    // / Align the source within the target box (by default, centering) and discard
    // / any portions of the source that lie outside the box.
    // /
    // / The source image is not resized.
    // /
    // / ![](https://flutter.github.io/assets-for-api-docs/assets/painting/box_fit_none.png)
    none,

    // / Align the source within the target box (by default, centering) and, if
    // / necessary, scale the source down to ensure that the source fits within the
    // / box.
    // /
    // / This is the same as `contain` if that would shrink the image, otherwise it
    // / is the same as `none`.
    // /
    // / ![](https://flutter.github.io/assets-for-api-docs/assets/painting/box_fit_scaleDown.png)
    scaleDown
}