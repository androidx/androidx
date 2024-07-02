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

package androidx.compose.ui.graphics.layer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Looper
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.HandlerCompat
import java.lang.reflect.Method
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

internal sealed interface LayerSnapshotImpl {
    suspend fun toBitmap(graphicsLayer: GraphicsLayer): Bitmap
}

@RequiresApi(Build.VERSION_CODES.P)
internal object LayerSnapshotV28 : LayerSnapshotImpl {

    override suspend fun toBitmap(graphicsLayer: GraphicsLayer): Bitmap =
        Bitmap.createBitmap(GraphicsLayerPicture(graphicsLayer))

    /** [Picture] class used to create a hardware bitmap through [Bitmap.createBitmap] */
    private class GraphicsLayerPicture(val graphicsLayer: GraphicsLayer) : Picture() {
        override fun beginRecording(width: Int, height: Int): Canvas {
            // Return placeholder canvas here as we are leveraging a graphicsLayer that is
            // already been built (i.e. has already recorded the drawing commands into its
            // internal canvas). Bitmap.createBitmap(Picture) does not invoke this method as it
            // assumes an already constructed Picture instance as well.
            return Canvas()
        }

        override fun endRecording() {
            // NO-OP. The GraphicsLayer used here already has its drawing commands recorded via
            // GraphicsLayer.record, so there is no additional work to be done here.
        }

        override fun getWidth(): Int = graphicsLayer.size.width

        override fun getHeight(): Int = graphicsLayer.size.height

        override fun requiresHardwareAcceleration(): Boolean = true

        override fun draw(canvas: Canvas) {
            graphicsLayer.draw(androidx.compose.ui.graphics.Canvas(canvas), null)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
internal object LayerSnapshotV22 : LayerSnapshotImpl {

    override suspend fun toBitmap(graphicsLayer: GraphicsLayer): Bitmap {
        val size = graphicsLayer.size
        val looper = Looper.myLooper() ?: Looper.getMainLooper()
        ImageReader.newInstance(
                size.width, /* pixel width */
                size.height, /* pixel height */
                PixelFormat.RGBA_8888, /* format */
                1 /* maxImages */
            )
            .use { reader ->
                val image = suspendCancellableCoroutine { continuation ->
                    reader.setOnImageAvailableListener(
                        { continuation.resume(it.acquireLatestImage()) },
                        HandlerCompat.createAsync(looper)
                    )

                    val surface = reader.surface
                    val canvas = SurfaceUtils.lockCanvas(surface)
                    try {
                        // Clear contents of the buffer before rendering
                        canvas.drawColor(Color.Black.toArgb(), PorterDuff.Mode.CLEAR)
                        graphicsLayer.draw(ComposeCanvas(canvas), null)
                    } finally {
                        surface.unlockCanvasAndPost(canvas)
                    }
                }
                return image.toBitmap()
            }
    }
}

/** Fallback implementation to render into a software bitmap */
internal object LayerSnapshotV21 : LayerSnapshotImpl {
    override suspend fun toBitmap(graphicsLayer: GraphicsLayer): Bitmap {
        val size = graphicsLayer.size
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val canvas = ComposeCanvas(Canvas(bitmap))
        graphicsLayer.draw(canvas, null)
        return bitmap
    }
}

@RequiresApi(Build.VERSION_CODES.M)
private object SurfaceVerificationHelper {

    fun lockHardwareCanvas(surface: Surface): Canvas = surface.lockHardwareCanvas()
}

internal object SurfaceUtils {

    private var lockHardwareCanvasMethod: Method? = null
    private var hasRetrievedMethod = false

    /**
     * Attempts to obtain a hardware accelerated [android.graphics.Canvas] from the provided
     * [Surface]. In certain scenarios it will fallback to returning a software backed [Canvas]. For
     * Android versions M (inclusive) and above this will always return a hardware accelerated
     * [Canvas].
     *
     * For Android L_MR1 (API 22) this will attempt to leverage reflection to obtain a hardware
     * accelerated [Canvas].
     *
     * If the reflective call fails or this method is invoked on Android L (API 21) this will always
     * return a software backed [Canvas]
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun lockCanvas(surface: Surface): Canvas {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            SurfaceVerificationHelper.lockHardwareCanvas(surface)
        } else {
            lockCanvasFallback(surface)
        }
    }

    fun isLockHardwareCanvasAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            true
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1) {
            resolveLockHardwareCanvasMethod() != null
        } else {
            false
        }
    }

    @SuppressLint("BanUncheckedReflection")
    private fun resolveLockHardwareCanvasMethod(): Method? {
        synchronized(this) {
            return try {
                var method: Method? = lockHardwareCanvasMethod
                if (!hasRetrievedMethod) {
                    hasRetrievedMethod = true

                    // getDeclaredMethod as a @NotNull annotation and always returns a non-null
                    // method instance on success and throws on failure. Avoiding usage of the
                    // the safe call operator as it shows warnings in the IDE that it is not
                    // necessary as a result
                    method =
                        Surface::class.java.getDeclaredMethod("lockHardwareCanvas").also {
                            it.isAccessible = true
                            lockHardwareCanvasMethod = it
                        }
                }
                method
            } catch (_: Throwable) {
                lockHardwareCanvasMethod = null
                null
            }
        }
    }

    @SuppressLint("BanUncheckedReflection")
    private fun lockCanvasFallback(surface: Surface): Canvas {
        val method = resolveLockHardwareCanvasMethod()
        return if (method != null) {
            method.invoke(surface) as Canvas
        } else {
            surface.lockCanvas(null)
        }
    }
}

private fun Image.toBitmap(): Bitmap {
    planes!!.let {
        val plane = it[0]
        val pixelCount = width * height
        val colors = IntArray(pixelCount)
        // Do a bulk copy as it is more efficient than buffer.get then rearrange the pixels
        // in memory from RGBA to ARGB for bitmap consumption
        plane.buffer.asIntBuffer().get(colors)
        for (i in 0 until pixelCount) {
            val color = colors[i]
            val red = color and 0xff
            val green = color shr 8 and 0xff
            val blue = color shr 16 and 0xff
            val alpha = color shr 24 and 0xff
            colors[i] = Color(red, green, blue, alpha).toArgb()
        }

        return Bitmap.createBitmap(colors, width, height, android.graphics.Bitmap.Config.ARGB_8888)
    }
}
