/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Immutable
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.skia.Picture
import org.jetbrains.skia.PictureRecorder

@Immutable
internal actual object DragAndDropSourceDefaults {
    actual val DefaultStartDetector: DragAndDropStartDetector = {
        detectDragStart(::requestDragAndDropTransfer)
    }
}

private suspend fun PointerInputScope.detectDragStart(onDragStart: (Offset) -> Unit) {
    coroutineScope {
        launch {
            detectDragGestures(
                matcher = PointerMatcher.mouse(PointerButton.Primary),
                onDragStart = onDragStart,
                onDrag = { _ -> }
            )
        }
        launch {
            detectTapGestures(
                matcher = PointerMatcher.touch,
                onLongPress = onDragStart
            )
        }
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
                else -> drawIntoCanvas { canvas -> canvas.skiaCanvas.drawPicture(picture) }
            }
        }

    actual fun cachePicture(scope: CacheDrawScope): DrawResult =
        with(scope) {
            val width = this.size.width
            val height = this.size.height
            onDrawWithContent {
                val pictureRecorder = PictureRecorder()
                val pictureCanvas = pictureRecorder
                    .beginRecording(
                        0f,
                        0f,
                        width,
                        height
                    )
                    .asComposeCanvas()
                draw(
                    density = this,
                    layoutDirection = this.layoutDirection,
                    canvas = pictureCanvas,
                    size = this.size
                ) {
                    this@onDrawWithContent.drawContent()
                }
                val picture = pictureRecorder.finishRecordingAsPicture()
                pictureRecorder.close()
                cachedPicture = picture

                drawIntoCanvas { canvas -> canvas.skiaCanvas.drawPicture(picture) }
            }
        }
}
