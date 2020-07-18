/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.graphics

import androidx.compose.InternalComposeApi

// TODO(b/160916667): temporary workaround for ui-desktop. remove when full support of MPP will
//  be in-place
@Deprecated(
    "Temporary workaround. Supposed to be used only in ui-desktop before MPP",
    level = DeprecationLevel.ERROR
)
@InternalComposeApi
object GraphicsFactory {
    var nativeCanvas: (NativeCanvas) -> Canvas = ::AndroidNativeCanvas
    var imageCanvas = ::AndroidImageCanvas
    var canvasHolder: () -> InternalCanvasHolder = { AndroidInternalCanvasHolder() }
    var paint: () -> Paint = { AndroidPaint() }
    var path: () -> Path = { AndroidPath() }

    object Shader {
        var linear = ::AndroidLinearGradientShader
        var radial = ::AndroidRadialGradientShader
        var image = ::AndroidImageShader
    }
}
