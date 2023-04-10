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

package androidx.graphics

import android.annotation.SuppressLint
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor

/**
 * Helper class used to draw RenderNode content into a HardwareBuffer instance. The contents of the
 * HardwareBuffer are not persisted across renders.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class MultiBufferedCanvasRenderer(
    private val renderNode: RenderNode,
    width: Int,
    height: Int,
    format: Int = PixelFormat.RGBA_8888,
    usage: Long = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
    maxImages: Int = 2
) {
    // PixelFormat.RGBA_8888 should be accepted here but Android Studio flags as a warning
    @SuppressLint("WrongConstant")
    private val mImageReader = ImageReader.newInstance(width, height, format, maxImages, usage)
    private var mHardwareRenderer: HardwareRenderer? = HardwareRenderer().apply {
        setContentRoot(renderNode)
        setSurface(mImageReader.surface)
        start()
    }

    private var mIsReleased = false

    fun renderFrame(executor: Executor, bufferAvailable: (HardwareBuffer) -> Unit) {
        val renderer = mHardwareRenderer
        if (renderer != null && !mIsReleased) {
            with(renderer) {
                createRenderRequest()
                    .setFrameCommitCallback(executor) {
                        val nextImage = mImageReader.acquireNextImage()
                        nextImage?.let { image ->
                            val buffer = image.hardwareBuffer
                            if (buffer != null) {
                                executor.execute {
                                    bufferAvailable(buffer)
                                }
                            }
                            image.close()
                        }
                    }
                    .syncAndDraw()
            }
        } else {
            Log.v(TAG, "mHardwareRenderer is null")
        }
    }

    fun release() {
        if (!mIsReleased) {
            mImageReader.close()
            mHardwareRenderer?.let { renderer ->
                renderer.stop()
                renderer.destroy()
            }
            mHardwareRenderer = null
            mIsReleased = true
        }
    }

    internal companion object {
        const val TAG = "MultiBufferRenderer"
    }
}