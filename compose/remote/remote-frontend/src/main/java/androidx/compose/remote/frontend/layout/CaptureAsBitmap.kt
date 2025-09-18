/*
 * Copyright (C) 2024 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.layout

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Picture
import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.remote.frontend.modifier.height
import androidx.compose.remote.frontend.modifier.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

/**
 * Utility function to capture its content as a bitmap. The callback onCapture() is executed when
 * the bitmap is ready.
 */
@Composable
public fun CaptureAsBitmap(
    onCapture: @Composable () -> Unit,
    contentDescription: String = "",
    content: @Composable () -> Unit,
) {
    val picture = remember { Picture() }
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }
    if (bitmap.value == null) {
        androidx.compose.foundation.layout.Box(
            modifier =
                Modifier.drawWithCache {
                    val width = this.size.width.toInt()
                    val height = this.size.height.toInt()
                    onDrawWithContent {
                        val pictureCanvas =
                            androidx.compose.ui.graphics.Canvas(
                                picture.beginRecording(width, height)
                            )
                        draw(this, this.layoutDirection, pictureCanvas, this.size) {
                            this@onDrawWithContent.drawContent()
                        }
                        picture.endRecording()
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawPicture(picture)

                            bitmap.value =
                                Bitmap.createBitmap(
                                    picture,
                                    picture.width,
                                    picture.height,
                                    Config.ARGB_8888,
                                )
                        }
                    }
                }
        ) {
            content()
        }
    } else {
        val bitmapWidth = bitmap.value!!.width
        val bitmapHeight = bitmap.value!!.height
        RemoteBox(modifier = RemoteModifier.width(bitmapWidth).height(bitmapHeight)) {
            Image(
                modifier = androidx.compose.ui.Modifier,
                bitmap = bitmap.value!!.asImageBitmap(),
                contentDescription = "",
            )
        }
        onCapture.invoke()
    }
}
