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

import androidx.compose.foundation.gestures.PressGestureScopeImpl
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.processDragGesture
import androidx.compose.foundation.gestures.processTapGesture
import androidx.compose.runtime.Immutable
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import kotlinx.coroutines.coroutineScope

@Immutable
internal actual object DragAndDropSourceDefaults {
    actual val DefaultStartDetector: DragAndDropStartDetector = {
        // special signal to indicate to the sending side that it shouldn't intercept and
        // consume
        // cancel/up events as we're only require down events
        val pressScope = PressGestureScopeImpl(this)

        coroutineScope {
            awaitEachGesture {
                val initialDown =
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (initialDown.type == PointerType.Mouse) {
                    processDragGesture(
                        initialDown = initialDown,
                        shouldAwaitTouchSlop = { true },
                        orientationLock = null,
                        onDragStart = { down, _, _ -> requestDragAndDropTransfer(down.position) },
                        onDrag = { _, _ -> },
                        onDragCancel = {},
                        onDragEnd = {},
                    )
                } else {
                    // Process tap gesture internally doesn't use the initial pass in the initial
                    // suspending code, so we don't need to forward the initialDown on to it.
                    processTapGesture(
                        scope = this@coroutineScope,
                        pressScope = pressScope,
                        onDoubleTap = null,
                        onLongPress = { offset -> requestDragAndDropTransfer(offset) },
                        onPress = {},
                        onTap = null,
                    )
                }
            }
        }
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
