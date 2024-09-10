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

package androidx.compose.foundation.draganddrop

import android.graphics.Picture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Immutable
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

@Immutable
internal actual object DragAndDropSourceDefaults {
    actual val DefaultStartDetector: DragAndDropStartDetector = {
        detectTapGestures(onLongPress = { offset -> requestDragAndDropTransfer(offset) })
    }
}

internal actual class CacheDrawScopeDragShadowCallback {
    private var cachedPicture: Picture? = null

    actual fun drawDragShadow(drawScope: DrawScope) =
        with(drawScope) {
            when (val picture = cachedPicture) {
                null ->
                    throw IllegalArgumentException(
                        "No cached drag shadow. Check if Modifier.cacheDragShadow(painter) was called."
                    )
                else -> drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPicture(picture) }
            }
        }

    actual fun cachePicture(scope: CacheDrawScope): DrawResult =
        with(scope) {
            val picture = Picture()
            cachedPicture = picture
            val width = this.size.width.toInt()
            val height = this.size.height.toInt()
            onDrawWithContent {
                val pictureCanvas =
                    androidx.compose.ui.graphics.Canvas(picture.beginRecording(width, height))
                draw(
                    density = this,
                    layoutDirection = this.layoutDirection,
                    canvas = pictureCanvas,
                    size = this.size
                ) {
                    this@onDrawWithContent.drawContent()
                }
                picture.endRecording()

                drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPicture(picture) }
            }
        }
}
