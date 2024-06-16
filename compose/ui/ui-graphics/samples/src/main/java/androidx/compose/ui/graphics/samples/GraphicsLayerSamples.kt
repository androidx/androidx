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

package androidx.compose.ui.graphics.samples

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.Sampled
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Sampled
fun DrawScope.GraphicsLayerTopLeftSample(layer: GraphicsLayer) {
    // Build the layer with the density, layout direction and size from the DrawScope
    // and position the top left to be 20 pixels from the left and 30 pixels from the top.
    // This will the bounds of the layer with a red rectangle
    layer.apply {
        record { drawRect(Color.Red) }
        this.topLeft = IntOffset(20, 30)
    }

    // Draw the layer into the provided DrawScope
    drawLayer(layer)
}

@Sampled
fun DrawScope.GraphicsLayerSizeSample(layer: GraphicsLayer) {
    // Build the layer with the density, layout direction from the DrawScope that is
    // sized to 200 x 100 pixels and draw a red rectangle that occupies these bounds
    layer.record(size = IntSize(200, 100)) { drawRect(Color.Red) }

    // Draw the layer into the provided DrawScope
    drawLayer(layer)
}

@Sampled
fun DrawScope.GraphicsLayerScaleAndPivotSample(layer: GraphicsLayer) {
    // Create a 200 x 200 pixel layer that has a red rectangle drawn in the lower right
    // corner.
    layer.apply {
        record(size = IntSize(200, 200)) {
            drawRect(Color.Red, topLeft = Offset(size.width / 2f, size.height / 2f))
        }
        // Scale the layer by 1.5x in both the x and y axis relative to the bottom
        // right corner
        scaleX = 1.5f
        scaleY = 1.5f
        pivotOffset = Offset(this.size.width.toFloat(), this.size.height.toFloat())
    }

    // Draw the layer into the provided DrawScope
    drawLayer(layer)
}

@Sampled
fun DrawScope.GraphicsLayerTranslateSample(layer: GraphicsLayer) {
    // Create a 200 x 200 pixel layer that draws a red square
    layer.apply {
        record(size = IntSize(200, 200)) { drawRect(Color.Red) }
        // Configuring the translationX + Y will translate the red square
        // by 100 pixels to the right and 50 pixels from the top when drawn
        // into the destination DrawScope
        translationX = 100f
        translationY = 50f
    }

    // Draw the layer into the provided DrawScope
    drawLayer(layer)
}

@Sampled
fun DrawScope.GraphicsLayerShadowSample(layer: GraphicsLayer) {
    // Create a 200 x 200 pixel layer that draws a red square
    layer.apply {
        record(size = IntSize(200, 200)) { drawRect(Color.Red) }
        // Apply a shadow with specified colors that has an elevation of 20f when this layer is
        // drawn into the destination DrawScope.
        shadowElevation = 20f
        ambientShadowColor = Color.Cyan
        spotShadowColor = Color.Magenta
    }

    // Draw the layer into the provided DrawScope
    drawLayer(layer)
}

@Sampled
fun DrawScope.GraphicsLayerBlendModeSample(layer: GraphicsLayer) {
    val drawScopeSize = size
    val topLeft = IntOffset((drawScopeSize.width / 4).toInt(), (drawScopeSize.height / 4).toInt())
    val layerSize = IntSize((drawScopeSize.width / 2).toInt(), (drawScopeSize.height / 2).toInt())

    // Build the GraphicsLayer with the specified offset and size that is filled
    // with a red rectangle.
    layer.apply {
        record(size = layerSize) { drawRect(Color.Red) }
        this.topLeft = topLeft
        // Specify the Xor blend mode here so that layer contents will be shown in the
        // destination only if it is transparent, otherwise the destination would be cleared
        // by the source pixels. The color of the rect drawn in this layer is irrelevant as only
        // the area shown by the layer is important when using the Xor BlendMode
        blendMode = BlendMode.Xor
    }

    // Fill the destination DrawScope with a green rectangle
    drawRect(Color.Green)
    // Draw the layer into the destination. Due to usage of the xor blend mode on the layer, the
    // region occupied by the red rectangle in the layer will be used to clear contents in the
    // destination
    drawLayer(layer)

    // Draw a blue rectangle *underneath* the current destination pixels. Because the
    // drawing of the layer cleared the region in the center, this will fill the transparent
    // pixels left in this hole to blue while leaving the bordering green pixels alone.
    drawRect(Color.Blue, blendMode = BlendMode.DstOver)
}

@Sampled
fun DrawScope.GraphicsLayerColorFilterSample(layer: GraphicsLayer) {
    // Create a layer with the same configuration as the destination DrawScope
    // and draw a red rectangle in the layer
    layer.apply {
        record { drawRect(Color.Red) }
        // Apply a ColorFilter that will tint the contents of the layer to blue
        // when it is drawn into the destination DrawScope
        colorFilter = ColorFilter.tint(Color.Blue)
    }

    drawLayer(layer)
}

@RequiresApi(Build.VERSION_CODES.S)
@Sampled
fun DrawScope.GraphicsLayerRenderEffectSample(layer: GraphicsLayer) {
    // Create a layer sized to the destination draw scope that is comprised
    // of an inset red rectangle
    layer.apply {
        record { inset(20f, 20f) { drawRect(Color.Red) } }
        // Configure a blur to the contents of the layer that is applied
        // when drawn to the destination DrawScope
        renderEffect = BlurEffect(20f, 20f, TileMode.Decal)
    }

    drawLayer(layer)
}

@Sampled
fun DrawScope.GraphicsLayerAlphaSample(layer: GraphicsLayer) {
    // Create a layer sized to the destination draw scope that is comprised
    // of an inset red rectangle
    layer.apply {
        record { inset(20f, 20f) { drawRect(Color.Red) } }
        // Renders the content of the layer with 50% alpha when it is drawn
        // into the destination
        alpha = 0.5f
    }

    drawLayer(layer)
}

@Sampled
fun DrawScope.GraphicsLayerOutlineSample(layer: GraphicsLayer) {
    // Create a layer sized to the destination draw scope that is comprised
    // of an inset red rectangle
    layer.apply {
        record { drawRect(Color.Red) }
        // Apply a shadow that is clipped to the specified round rect
        shadowElevation = 20f
        setRoundRectOutline(Offset.Zero, Size(300f, 180f), 30f)
    }

    drawLayer(layer)
}

@Sampled
fun DrawScope.GraphicsLayerRoundRectOutline(layer: GraphicsLayer) {
    // Create a layer sized to the destination draw scope that is comprised
    // of an inset red rectangle
    layer.apply {
        record { drawRect(Color.Red) }
        // Apply a shadow and have the contents of the layer be clipped
        // to the size of the layer with a 20 pixel corner radius
        shadowElevation = 20f
        setRoundRectOutline(cornerRadius = 20f)
    }

    drawLayer(layer)
}

@Sampled
fun DrawScope.GraphicsLayerRectOutline(layer: GraphicsLayer) {
    // Create a layer sized to the destination draw scope that is comprised
    // of an inset red rectangle
    layer.apply {
        record { drawRect(Color.Red) }
        // Apply a shadow and have the contents of the layer be clipped
        // to the size of the layer with a 20 pixel corner radius
        shadowElevation = 20f
        setRectOutline(size = Size(this.size.width / 2f, this.size.height.toFloat()))
    }

    drawLayer(layer)
}

@Sampled
fun DrawScope.GraphicsLayerRotationX(layer: GraphicsLayer) {
    layer.apply {
        record { drawRect(Color.Yellow) }
        // Rotates the yellow rect 45f clockwise relative to the x axis
        rotationX = 45f
    }

    drawLayer(layer)
}

@Sampled
fun DrawScope.GraphicsLayerRotationYWithCameraDistance(layer: GraphicsLayer) {
    layer.apply {
        record { drawRect(Color.Yellow) }
        // Rotates the yellow rect 45f clockwise relative to the y axis
        rotationY = 45f
        cameraDistance = 5.0f
    }

    drawLayer(layer)
}

@OptIn(DelicateCoroutinesApi::class)
@Sampled
fun GraphicsLayerToImageBitmap(context: Context, layer: GraphicsLayer) {
    layer.record(Density(1f), LayoutDirection.Ltr, IntSize(300, 200)) {
        val half = Size(size.width / 2, size.height)
        drawRect(Color.Red, size = half)
        drawRect(Color.Blue, topLeft = Offset(size.width / 2f, 0f), size = half)
    }

    GlobalScope.launch(Dispatchers.IO) {
        val imageBitmap = layer.toImageBitmap()
        val outputStream = context.openFileOutput("MyGraphicsLayerImageBitmap.png", MODE_PRIVATE)
        imageBitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    }
}
