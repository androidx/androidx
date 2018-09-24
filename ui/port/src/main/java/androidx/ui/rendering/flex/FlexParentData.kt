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

package androidx.ui.rendering.flex

import androidx.ui.rendering.box.RenderBox

/** Parent data for use with [RenderFlex]. */
class FlexParentData : ContainerBoxParentData<RenderBox>() {
    /**
     * The flex factor to use for this child
     *
     * If null or zero, the child is inflexible and determines its own size. If
     * non-zero, the amount of space the child's can occupy in the main axis is
     * determined by dividing the free space (after placing the inflexible
     * children) according to the flex factors of the flexible children.
     */
    // TODO(Migration/Mihai): document what happens when flex values are negative
    //              or switch to UInt in Kotlin 1.3 if they do not have significance
    var flex: Int = 0

    /**
     * How a flexible child is inscribed into the available space.
     *
     * If [flex] is non-zero, the [fit] determines whether the child fills the
     * space the parent makes available during layout. If the fit is
     * [FlexFit.TIGHT], the child is required to fill the available space. If the
     * fit is [FlexFit.LOOSE], the child can be at most as large as the available
     * space (but is allowed to be smaller).
     */
    var fit: FlexFit = FlexFit.TIGHT

    override fun toString() = "${super.toString()}; flex=$flex; fit=$fit"
}