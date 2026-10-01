/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.rendering.metal

import androidx.ink.brush.ExperimentalInkCrossPlatformRenderingApi
import androidx.ink.brush.TextureImageStore
import androidx.ink.geometry.AffineTransform
import androidx.ink.nativeloader.InkInternalOnlyApi
import androidx.ink.nativeloader.NativePointer
import androidx.ink.nativeloader.cinterop.MetalRendererNative_create
import androidx.ink.nativeloader.cinterop.MetalRendererNative_drawInProgressStroke
import androidx.ink.nativeloader.cinterop.MetalRendererNative_drawStroke
import androidx.ink.nativeloader.cinterop.MetalRendererNative_free
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaque
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormat
import platform.Metal.MTLRenderCommandEncoderProtocol

/**
 * Ink renderer for iOS, using Metal.
 *
 * @param device `MTLDevice` to use for rendering.
 * @param colorPixelFormat Format of the color texture being rendered.
 * @param stencilPixelFormat Format of the stencil texture being rendered. If a stencil texture is
 *   not being used, this should be `MTLPixelFormatInvalid`.
 * @param sampleCount The number of samples per pixel for MSAA. If unset or null, shader-based
 *   antialiasing will be used instead.
 * @param textureImageStore An optional callback for retrieving texture images by ID.
 */
@ExperimentalInkCrossPlatformRenderingApi
@OptIn(InkInternalOnlyApi::class, ExperimentalForeignApi::class)
public class MetalRenderer(
    device: MTLDeviceProtocol,
    colorPixelFormat: MTLPixelFormat,
    stencilPixelFormat: MTLPixelFormat,
    sampleCount: Int? = null,
    private val textureImageStore: TextureImageStore? = null,
) {

    private val nativePointer: Long by
        NativePointer(
            {
                MetalRendererNative_create(
                        interpretCPointer<COpaque>(device.objcPtr()),
                        colorPixelFormat,
                        stencilPixelFormat,
                        sampleCount ?: -1,
                        if (textureImageStore != null) textureForIdCallback else null,
                        throwForNonOkStatusCallback,
                    )
                    .also { ptr ->
                        if (ptr != 0L && textureImageStore != null) {
                            textureImageStoresByPtr[ptr] = textureImageStore
                        }
                    }
            },
            { ptr ->
                MetalRendererNative_free(ptr)
                textureImageStoresByPtr.remove(ptr)
            },
        )

    private companion object {
        val textureImageStoresByPtr = mutableMapOf<Long, TextureImageStore>()

        @OptIn(ExperimentalForeignApi::class)
        private val textureForIdCallback:
            CPointer<CFunction<(Long, CPointer<ByteVar>?) -> COpaquePointer?>> =
            staticCFunction({ metalRendererNativePtr, textureIdPtr ->
                textureImageStoresByPtr[metalRendererNativePtr]?.let { store ->
                    textureIdPtr
                        ?.toKString()
                        ?.let { (store[it]?.objcPtr()) }
                        ?.let { interpretCPointer<COpaque>(it) }
                }
            })
    }

    /**
     * Draws an in-progress stroke using the given render encoder.
     *
     * @param renderEncoder `MTLRenderCommandEncoder` to draw with.
     * @param inProgressStroke The in-progress stroke to draw.
     * @param textureWidth Width of the texture [renderEncoder] is drawing to, used to compute the
     *   projection transform.
     * @param textureHeight Height of the texture [renderEncoder] is drawing to, used to compute the
     *   projection transform.
     * @param strokeToScreenTransform Affine transform from stroke coordinates to view coordinates.
     */
    public fun draw(
        renderEncoder: MTLRenderCommandEncoderProtocol,
        inProgressStroke: InProgressStroke,
        textureWidth: Double,
        textureHeight: Double,
        strokeToScreenTransform: AffineTransform,
    ) {
        MetalRendererNative_drawInProgressStroke(
            nativePointer,
            interpretCPointer<COpaque>(renderEncoder.objcPtr()),
            inProgressStroke.nativePointer,
            textureWidth,
            textureHeight,
            strokeToScreenTransform.m00,
            strokeToScreenTransform.m10,
            strokeToScreenTransform.m20,
            strokeToScreenTransform.m01,
            strokeToScreenTransform.m11,
            strokeToScreenTransform.m21,
        )
    }

    /**
     * Draws a completed stroke using the given render encoder.
     *
     * @param renderEncoder `MTLRenderCommandEncoder` to draw with.
     * @param stroke The stroke to draw.
     * @param textureWidth Width of the texture [renderEncoder] is drawing to, used to compute the
     *   projection transform.
     * @param textureHeight Height of the texture [renderEncoder] is drawing to, used to compute the
     *   projection transform.
     * @param strokeToScreenTransform Affine transform from stroke coordinates to view coordinates.
     */
    public fun draw(
        renderEncoder: MTLRenderCommandEncoderProtocol,
        stroke: Stroke,
        textureWidth: Double,
        textureHeight: Double,
        strokeToScreenTransform: AffineTransform,
    ) {
        MetalRendererNative_drawStroke(
            nativePointer,
            interpretCPointer<COpaque>(renderEncoder.objcPtr()),
            stroke.nativePointer,
            textureWidth,
            textureHeight,
            strokeToScreenTransform.m00,
            strokeToScreenTransform.m10,
            strokeToScreenTransform.m20,
            strokeToScreenTransform.m01,
            strokeToScreenTransform.m11,
            strokeToScreenTransform.m21,
        )
    }
}
