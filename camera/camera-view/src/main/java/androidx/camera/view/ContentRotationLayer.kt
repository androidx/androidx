/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.view

import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Surface
import android.view.SurfaceControl
import android.view.SurfaceView
import android.view.View
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting

/**
 * A pair of [SurfaceControl] layers under a [SurfaceView] that rotate the content of [surface].
 *
 * A [SurfaceView] only applies the transform that the producer sets on the buffers, e.g. the camera
 * transform, and View transformations can't rotate it. For a stream without the camera transform,
 * e.g. one produced by a `CameraEffect` or stream sharing, the remaining rotation is applied to the
 * geometry of an intermediate layer between the [SurfaceView] and the layer that receives the
 * frames, the same way a `TextureView` applies it with a transform matrix.
 *
 * The layers are released once the owner and every user of [surface] released their references, so
 * the [Surface] stays valid until the camera no longer uses it.
 */
@RequiresApi(29)
@MainThread
internal class ContentRotationLayer(parent: SurfaceView, width: Int, height: Int) {

    private val rotationLayer: SurfaceControl =
        SurfaceControl.Builder()
            .setName("PreviewView-ContentRotation")
            .setFormat(PixelFormat.RGBX_8888)
            .setBufferSize(width, height)
            .setParent(parent.surfaceControl)
            .build()

    private val contentLayer: SurfaceControl =
        SurfaceControl.Builder()
            .setName("PreviewView-Content")
            .setFormat(PixelFormat.RGBX_8888)
            .setBufferSize(width, height)
            .setParent(rotationLayer)
            .build()

    /** The [Surface] that receives the frames. */
    val surface: Surface = Surface(contentLayer)

    private var refCount = 1

    init {
        SurfaceControl.Transaction().use {
            it.setVisibility(rotationLayer, true).setVisibility(contentLayer, true).apply()
        }
    }

    /** Sets the size of the content, e.g. when the size of the [SurfaceView] changes. */
    fun setBufferSize(width: Int, height: Int) {
        SurfaceControl.Transaction().use {
            it.setBufferSize(rotationLayer, width, height)
                .setBufferSize(contentLayer, width, height)
                .apply()
        }
    }

    /**
     * Rotates the content clockwise by [rotationDegrees] and scales the rotated content to fit the
     * original `width x height` bounds, matching [PreviewTransformation]'s `TextureView`
     * correction.
     *
     * When [syncView] is provided on API 31+, the change is applied with the next frame drawn by
     * the root of [syncView], so it lands on the same frame as the View transformations of the
     * [SurfaceView]. Otherwise, the change is applied immediately.
     */
    fun setContentRotation(rotationDegrees: Int, width: Int, height: Int, syncView: View?) {
        val geometry = Geometry.create(rotationDegrees, width, height)
        val transaction = SurfaceControl.Transaction()
        // setGeometry() is used because setBufferTransform() only transforms the buffer of the
        // layer itself, and this layer has no buffer. The geometry of a layer applies to its
        // children and is preserved across frames. It is deprecated on API 33+, but its
        // replacements are not available on API 29-32.
        @Suppress("DEPRECATION")
        transaction.setGeometry(
            rotationLayer,
            geometry.sourceCrop,
            geometry.destinationFrame,
            geometry.transform,
        )
        if (
            Build.VERSION.SDK_INT >= 31 &&
                syncView != null &&
                Api31Impl.applyTransactionOnDraw(syncView, transaction)
        ) {
            // On API 31-33, the transaction is merged asynchronously on the RenderThread, so it
            // must not be closed here. It's released by its finalizer instead.
            if (Build.VERSION.SDK_INT >= 34) {
                transaction.close()
            }
            syncView.invalidate()
        } else {
            transaction.apply()
            transaction.close()
        }
    }

    /** Hides the content, e.g. when the [SurfaceView]'s surface is destroyed. */
    fun detach() {
        SurfaceControl.Transaction().use { it.reparent(rotationLayer, null).apply() }
    }

    /** Acquires a reference to [surface], e.g. when it's provided to the camera. */
    fun acquire() {
        check(refCount > 0) { "Already released" }
        refCount++
    }

    /** Releases a reference to [surface]. The layers are released with the last reference. */
    fun release() {
        check(refCount > 0) { "Already released" }
        if (--refCount == 0) {
            surface.release()
            contentLayer.release()
            rotationLayer.release()
        }
    }

    /**
     * The arguments to [SurfaceControl.Transaction.setGeometry] that rotate the content clockwise
     * by a multiple of 90 degrees and scale it to fit the original `width x height` bounds.
     *
     * `setGeometry()` maps the [sourceCrop] of the layer to the [destinationFrame] of its parent
     * after applying [transform], which is passed to the native transaction as a buffer transform.
     * For 90 and 270 degree rotations, [sourceCrop] is a square large enough to not crop the
     * content, whose sides are a multiple of the reduced aspect ratio, so that [destinationFrame]
     * can express the exact non-uniform scale with integer coordinates.
     */
    @VisibleForTesting
    internal class Geometry(val sourceCrop: Rect, val destinationFrame: Rect, val transform: Int) {
        companion object {
            // SurfaceControl.BUFFER_TRANSFORM_* constants, which match the native buffer
            // transforms supported since API 29, but were only added to the SDK in API 33.
            const val BUFFER_TRANSFORM_IDENTITY = 0
            const val BUFFER_TRANSFORM_ROTATE_90 = 4
            const val BUFFER_TRANSFORM_ROTATE_180 = 3
            const val BUFFER_TRANSFORM_ROTATE_270 = 7

            // floor(sqrt(Int.MAX_VALUE)), so the square of a reduced dimension fits in an Int.
            private const val MAX_REDUCED_DIMENSION = 46340L

            fun create(rotationDegrees: Int, width: Int, height: Int): Geometry {
                require(width > 0 && height > 0) { "Invalid content size: ${width}x$height" }
                val bounds = Rect(0, 0, width, height)
                return when (rotationDegrees.mod(360)) {
                    0 -> Geometry(bounds, Rect(bounds), BUFFER_TRANSFORM_IDENTITY)
                    180 -> Geometry(bounds, Rect(bounds), BUFFER_TRANSFORM_ROTATE_180)
                    90 -> createQuarterTurn(width, height, clockwise = true)
                    270 -> createQuarterTurn(width, height, clockwise = false)
                    else ->
                        throw IllegalArgumentException(
                            "Rotation degrees must be a multiple of 90: $rotationDegrees"
                        )
                }
            }

            private fun createQuarterTurn(width: Int, height: Int, clockwise: Boolean): Geometry {
                val divisor = gcd(width, height)
                val reducedWidth = (width / divisor).toLong()
                val reducedHeight = (height / divisor).toLong()
                require(
                    reducedWidth <= MAX_REDUCED_DIMENSION && reducedHeight <= MAX_REDUCED_DIMENSION
                ) {
                    "Content size too large to rotate: ${width}x$height"
                }
                val unitSide = reducedWidth * reducedHeight
                val scale = (maxOf(width, height).toLong() - 1L) / unitSide + 1L
                val side = scale * unitSide
                val destWidth = scale * reducedWidth * reducedWidth
                val destHeight = scale * reducedHeight * reducedHeight
                require(
                    side <= Int.MAX_VALUE &&
                        width.toLong() + destWidth <= Int.MAX_VALUE &&
                        height.toLong() + destHeight <= Int.MAX_VALUE
                ) {
                    "Content size too large to rotate: ${width}x$height"
                }
                val sideInt = side.toInt()
                val destinationFrame =
                    if (clockwise) {
                        // (x, y) -> (width - y * width / height, x * height / width)
                        Rect(
                            width,
                            -sideInt,
                            width + destWidth.toInt(),
                            -sideInt + destHeight.toInt(),
                        )
                    } else {
                        // (x, y) -> (y * width / height, height - x * height / width)
                        Rect(
                            -sideInt,
                            height,
                            -sideInt + destWidth.toInt(),
                            height + destHeight.toInt(),
                        )
                    }
                return Geometry(
                    sourceCrop = Rect(0, 0, sideInt, sideInt),
                    destinationFrame = destinationFrame,
                    transform =
                        if (clockwise) BUFFER_TRANSFORM_ROTATE_270 else BUFFER_TRANSFORM_ROTATE_90,
                )
            }

            private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        }
    }

    @RequiresApi(31)
    private object Api31Impl {
        fun applyTransactionOnDraw(view: View, transaction: SurfaceControl.Transaction): Boolean =
            view.rootSurfaceControl?.applyTransactionOnDraw(transaction) == true
    }
}
