/*
 * Copyright 2023 The Android Open Source Project
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

@file:JvmMultifileClass
@file:JvmName("AndroidDragAndDropSource_androidKt")

package androidx.compose.foundation.draganddrop

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Immutable
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer

@Immutable
internal actual object DragAndDropSourceDefaults {
    actual val DefaultStartDetector: DragAndDropStartDetector = {
        detectTapGestures(onLongPress = { offset -> requestDragAndDropTransfer(offset) })
    }
}

internal actual class CacheDrawScopeDragShadowCallback {
    private var graphicsLayer: GraphicsLayer? = null

    actual fun drawDragShadow(drawScope: DrawScope) =
        with(drawScope) {
            when (val layer = graphicsLayer) {
                null ->
                    throw IllegalArgumentException(
                        "No cached drag shadow. Check if the drag source node was rendered first"
                    )
                else -> drawLayer(layer)
            }
        }

    actual fun cachePicture(scope: CacheDrawScope): DrawResult =
        with(scope) {
            graphicsLayer = scope.obtainGraphicsLayer().apply { record { drawContent() } }
            onDrawWithContent { drawLayer(graphicsLayer!!) }
        }
}
