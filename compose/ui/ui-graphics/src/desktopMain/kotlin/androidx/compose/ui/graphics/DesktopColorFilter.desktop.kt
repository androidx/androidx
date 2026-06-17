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

package androidx.compose.ui.graphics

import org.jetbrains.skia.ColorFilter as SkColorFilter

/**
 * Obtain a reference to the desktop ColorFilter type
 */
@Deprecated(
    message = "Use asSkiaColorFilter()",
    replaceWith = ReplaceWith("asSkiaColorFilter()"),
    level = DeprecationLevel.HIDDEN,
)
fun ColorFilter.asDesktopColorFilter(): SkColorFilter = nativeColorFilter

/**
 * Obtain a [org.jetbrains.skia.ColorFilter] instance from this [ColorFilter]
 */
@Deprecated(
    message = "Use asComposeColorFilter()",
    replaceWith = ReplaceWith("asComposeColorFilter()"),
    level = DeprecationLevel.HIDDEN,
)
fun SkColorFilter.toComposeColorFilter(): ColorFilter = ColorFilter(this)
