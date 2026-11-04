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

package androidx.camera.core.processing

import android.graphics.Color
import android.hardware.DataSpace
import android.hardware.HardwareBuffer
import android.media.Image
import android.media.ImageWriter
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLExt
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.annotation.RequiresApi
import com.google.common.truth.Truth.assertThat
import java.nio.ByteOrder
import kotlin.math.abs
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

/**
 * Utilities for testing OpenGL processing of 10-bit HDR YUV input without a camera.
 *
 * It provides:
 * - [isHdrYuvSamplingSupported] to skip tests on devices that can't render HDR YUV input.
 * - [createP010ImageWriter] and [queueP010Frame] to write P010 frames with a known data space and
 *   known code values to the input [Surface], instead of a camera stream.
 * - Expected values and assertions for checking the YUV to RGB conversion of the output, e.g.
 *   [fullRange8BitLevel] and [assertNeutralGray].
 */
@RequiresApi(33) // HDR, P010 HardwareBuffer and ImageWriter data spaces are supported from API 33.
internal object HdrYuvInputTestUtil {
    /** Tolerance for 8-bit color levels, e.g. for GPU precision and rounding. */
    const val COLOR_TOLERANCE = 3

    // 10-bit code values.
    const val Y_LIMITED_BLACK = 64
    const val Y_LIMITED_WHITE = 940
    const val UV_NEUTRAL = 512

    /**
     * Returns whether this device can render HDR YUV input with OpenGL, i.e. it has a 10-bit EGL
     * config for OpenGL ES 3 and supports GL_EXT_YUV_target.
     *
     * Without them, [OpenGlRenderer] can't use the HDR YUV shader (it falls back to SDR without
     * GL_EXT_YUV_target), so tests of the HDR YUV shader must be skipped. This is checked with EGL
     * and OpenGL ES directly rather than with [OpenGlRenderer], so a bug in [OpenGlRenderer] makes
     * the tests fail instead of being skipped.
     */
    fun isHdrYuvSamplingSupported(): Boolean {
        val glThread = HandlerThread("HdrYuvSamplingCheck").apply { start() }
        try {
            return runBlocking(Handler(glThread.looper).asCoroutineDispatcher()) {
                queryHdrYuvSamplingSupport()
            }
        } finally {
            glThread.quitSafely()
        }
    }

    private fun queryHdrYuvSamplingSupport(): Boolean {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
            return false
        }
        var context = EGL14.EGL_NO_CONTEXT
        var surface = EGL14.EGL_NO_SURFACE
        try {
            val configAttribs =
                intArrayOf(
                    EGL14.EGL_RED_SIZE,
                    10,
                    EGL14.EGL_GREEN_SIZE,
                    10,
                    EGL14.EGL_BLUE_SIZE,
                    10,
                    EGL14.EGL_ALPHA_SIZE,
                    2,
                    EGL14.EGL_RENDERABLE_TYPE,
                    EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                    EGL14.EGL_SURFACE_TYPE,
                    EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE,
                )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (
                !EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0) ||
                    numConfigs[0] == 0
            ) {
                return false
            }
            // GL_EXT_YUV_target is an OpenGL ES 3 extension, so query it in an ES 3 context.
            context =
                EGL14.eglCreateContext(
                    display,
                    configs[0],
                    EGL14.EGL_NO_CONTEXT,
                    intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE),
                    0,
                )
            surface =
                EGL14.eglCreatePbufferSurface(
                    display,
                    configs[0],
                    intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE),
                    0,
                )
            if (
                context == EGL14.EGL_NO_CONTEXT ||
                    surface == EGL14.EGL_NO_SURFACE ||
                    !EGL14.eglMakeCurrent(display, surface, surface, context)
            ) {
                return false
            }
            return GLES20.glGetString(GLES20.GL_EXTENSIONS)?.contains("GL_EXT_YUV_target") == true
        } finally {
            EGL14.eglMakeCurrent(
                display,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )
            if (surface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(display, surface)
            }
            if (context != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(display, context)
            }
            EGL14.eglTerminate(display)
        }
    }

    /** Returns the BT.2020 HLG data space with the given [DataSpace] range. */
    fun bt2020HlgDataSpace(range: Int): Int =
        DataSpace.pack(DataSpace.STANDARD_BT2020, DataSpace.TRANSFER_HLG, range)

    /** Returns the expected 8-bit level when a full range 10-bit code value isn't stretched. */
    fun fullRange8BitLevel(value10Bit: Int): Int = Math.round(value10Bit / 1023f * 255)

    /**
     * Creates an [ImageWriter] that writes P010 frames in [dataSpace] to [surface], or `null` if
     * it's not supported by the device.
     */
    fun createP010ImageWriter(surface: Surface, width: Int, height: Int, dataSpace: Int) =
        try {
            ImageWriter.Builder(surface)
                .setWidthAndHeight(width, height)
                // Use setHardwareBufferFormat() instead of setImageFormat(), because
                // setDataSpace() resets the image format and the writer would then allocate the
                // default RGBA_8888 buffers.
                .setHardwareBufferFormat(HardwareBuffer.YCBCR_P010)
                .setDataSpace(dataSpace)
                .setUsage(
                    HardwareBuffer.USAGE_CPU_WRITE_OFTEN or HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
                )
                .setMaxImages(1)
                .build()
        } catch (e: RuntimeException) {
            null
        }

    /**
     * Queues a P010 frame filled with [yValue] and neutral chroma to [imageWriter].
     *
     * Neutral chroma means a correct YUV to RGB conversion outputs gray. See [assertNeutralGray].
     *
     * @return `false` if the device doesn't support writing the P010 planes by CPU.
     */
    fun queueP010Frame(imageWriter: ImageWriter, width: Int, height: Int, yValue: Int): Boolean {
        val image =
            try {
                imageWriter.dequeueInputImage()
            } catch (e: RuntimeException) {
                return false
            }
        if (image.planes.size != 3) {
            image.close()
            return false
        }
        image.timestamp = 1L
        fillP010Plane(image.planes[0], width, height, yValue)
        // Chroma is subsampled by 2 in both directions.
        fillP010Plane(image.planes[1], width / 2, height / 2, UV_NEUTRAL)
        fillP010Plane(image.planes[2], width / 2, height / 2, UV_NEUTRAL)
        imageWriter.queueInputImage(image)
        return true
    }

    /**
     * Fills [width] x [height] samples of a P010 [plane] with [value10Bit].
     *
     * P010 stores each 10-bit value in the upper bits of a 16-bit little endian sample. Only the
     * samples are written, according to the row and pixel strides. Filling the whole plane buffer
     * isn't safe: for the interleaved chroma planes, the buffer of the second plane starts 2 bytes
     * after the first one, and on some devices its capacity extends beyond the allocation.
     */
    private fun fillP010Plane(plane: Image.Plane, width: Int, height: Int, value10Bit: Int) {
        val sample = (value10Bit shl 6).toShort()
        val buffer = plane.buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        for (row in 0 until height) {
            for (col in 0 until width) {
                buffer.putShort(row * plane.rowStride + col * plane.pixelStride, sample)
            }
        }
    }

    /** Asserts that [actual] is within [COLOR_TOLERANCE] of [expected], e.g. an 8-bit level. */
    fun assertWithinTolerance(actual: Int, expected: Int) {
        assertThat(abs(actual - expected)).isAtMost(COLOR_TOLERANCE)
    }

    /**
     * Asserts that [pixel] is a neutral gray, i.e. its red, green and blue are equal within
     * [COLOR_TOLERANCE].
     *
     * The frames from [queueP010Frame] have neutral chroma (U = V = 512, the middle of the 10-bit
     * range). The shader subtracts 0.5 from the normalized U and V, so both chroma components
     * become 0. In the YUV to RGB matrix, only the luma column is then used, and it has the same
     * coefficient for R, G and B:
     * ```
     * R = k * Y' + 0      * Cb + c_r  * Cr = k * Y'
     * G = k * Y' - c_gb   * Cb - c_gr * Cr = k * Y'
     * B = k * Y' + c_b    * Cb + 0    * Cr = k * Y'
     * ```
     *
     * where k is 1.1689 for limited range and 1.0 for full range. So a correct conversion always
     * produces a gray pixel, whatever the luma and range are.
     *
     * A non-gray pixel means the chroma path is broken rather than the range handling, e.g. the
     * chroma plane is sampled as 0 instead of 512 (a strong green or purple tint), the channels are
     * swapped, or the P010 chroma plane was written incorrectly. Checking this first makes sure the
     * gray level used by the range assertions is meaningful.
     */
    fun assertNeutralGray(pixel: Int) {
        assertWithinTolerance(Color.red(pixel), expected = Color.green(pixel))
        assertWithinTolerance(Color.blue(pixel), expected = Color.green(pixel))
    }
}
