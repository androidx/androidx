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

package androidx.ui.flow.layers

import androidx.ui.engine.geometry.Rect

class PrerollContext(
//    TODO(Migration/Andrey): Not porting RasterCache for now
//    val raster_cache: RasterCache?,
//    TODO(Migration/Andrey): Not porting GrContext for now
//    val gr_context: GrContext?,
//    TODO(Migration/Andrey): Not porting SkColorSpace for now
//    val dst_color_space: SkColorSpace?,
    val child_paint_bounds: Rect
)