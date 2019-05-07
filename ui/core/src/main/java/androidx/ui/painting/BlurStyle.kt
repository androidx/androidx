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

// Styles to use for blurs in [MaskFilter] objects.
// These enum values must be kept in sync with SkBlurStyle.
enum class BlurStyle {
    // These mirror SkBlurStyle and must be kept in sync.

    // Fuzzy inside and outside. This is useful for painting shadows that are
    // offset from the shape that ostensibly is casting the shadow.
    normal,

    // Solid inside, fuzzy outside. This corresponds to drawing the shape, and
    // additionally drawing the blur. This can make objects appear brighter,
    // maybe even as if they were fluorescent.
    solid,

    // Nothing inside, fuzzy outside. This is useful for painting shadows for
    // partially transparent shapes, when they are painted separately but without
    // an offset, so that the shadow doesn't paint below the shape.
    outer,

    // Fuzzy inside, nothing outside. This can make shapes appear to be lit from
    // within.
    inner
}