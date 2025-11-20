/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.authoring.internal

import android.graphics.BlendMode
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Use this with [android.graphics.Canvas.drawBitmap] or the combination of
 * [android.graphics.RenderNode.setUseCompositingLayer] and [android.graphics.Canvas.drawRenderNode]
 * to fully replace the contents of one buffer with another buffer. The buffers should be of equal
 * size so that no scaling is needed.
 */
@Suppress("ObsoleteSdkInt") // TODO(b/262911421): Should not need to suppress.
@RequiresApi(Build.VERSION_CODES.Q)
internal fun createPaintForUnscaledBlit() =
    Paint().apply {
        // Ensures that the source buffer content completely replaces the destination buffer
        // content.
        blendMode = BlendMode.SRC

        // No need for AA. The zero-arg Paint constructor enables this by default on API 31 and
        // above.
        isAntiAlias = false

        // Since we know this is always an unscaled blit, avoid any risk of subpixel alignment
        // causing
        // filtering to slightly blur the re-rendered content. The zero-arg Paint constructor
        // enables
        // this by default on API 29 and above.
        isFilterBitmap = false
    }
