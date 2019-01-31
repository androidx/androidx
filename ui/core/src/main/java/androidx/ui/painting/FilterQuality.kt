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

// Quality levels for image filters.
//
// See [Paint.filterQuality].
enum class FilterQuality {
    // This list comes from Skia's SkFilterQuality.h and the values (order) should
    // be kept in sync.

    // Fastest possible filtering, albeit also the lowest quality.
    //
    // Typically this implies nearest-neighbour filtering.
    none,

    // Better quality than [none], faster than [medium].
    //
    // Typically this implies bilinear interpolation.
    low,

    // Better quality than [low], faster than [high].
    //
    // Typically this implies a combination of bilinear interpolation and
    // pyramidal parametric prefiltering (mipmaps).
    medium,

    // Best possible quality filtering, albeit also the slowest.
    //
    // Typically this implies bicubic interpolation or better.
    high,
}