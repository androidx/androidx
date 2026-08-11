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

import androidx.annotation.RestrictTo
import androidx.ink.geometry.AffineTransform
import androidx.ink.nativeloader.InkInternalOnlyApi
import androidx.ink.nativeloader.NativePointer
import androidx.ink.nativeloader.cinterop.MetalRendererNative_create
import androidx.ink.nativeloader.cinterop.MetalRendererNative_drawInProgressStroke
import androidx.ink.nativeloader.cinterop.MetalRendererNative_drawStroke
import androidx.ink.nativeloader.cinterop.MetalRendererNative_free
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import androidx.ink.rendering.ExperimentalInkCrossPlatformRenderingApi
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke
import kotlinx.cinterop.COpaque
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.objcPtr
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
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@ExperimentalInkCrossPlatformRenderingApi
@OptIn(InkInternalOnlyApi::class, ExperimentalForeignApi::class)
class MetalRenderer(
    device: MTLDeviceProtocol,
    colorPixelFormat: MTLPixelFormat,
    stencilPixelFormat: MTLPixelFormat,
    sampleCount: Int? = null,
) {
    private val nativePointer: Long by
        NativePointer(
            {
                MetalRendererNative_create(
                    interpretCPointer<COpaque>(device.objcPtr()),
                    colorPixelFormat,
                    stencilPixelFormat,
                    sampleCount ?: -1,
                    throwForNonOkStatusCallback,
                )
            },
            ::MetalRendererNative_free,
        )

    /**
     * Draws an in-progress stroke using the given render encoder.
     *
     * @param renderEncoder `MTLRenderCommandEncoder` to draw with.
     * @param inProgressStroke The in-progress stroke to draw.
     * @param modelTransform Affine transform from stroke coordinates to world coordinates, an
     *   identity transform by default. Can be omitted if the stroke coordinates are all in the same
     *   world coordinate space.
     * @param viewTransform Affine transform from world coordinates to view coordinates, an identity
     *   transform by default. Can be omitted if the world coordinate space is the same as the view
     *   coordinate space.
     * @param projectionTransform The projection transform to use for drawing, an identity transform
     *   by default. Must transform from the world coordinate space to Y-up normalized device
     *   coordinates (the lower-left corner is (-1, -1) and the upper-right corner is (1, 1)).
     */
    fun draw(
        renderEncoder: MTLRenderCommandEncoderProtocol,
        inProgressStroke: InProgressStroke,
        modelTransform: AffineTransform = AffineTransform.IDENTITY,
        viewTransform: AffineTransform = AffineTransform.IDENTITY,
        projectionTransform: AffineTransform = AffineTransform.IDENTITY,
    ) =
        MetalRendererNative_drawInProgressStroke(
            nativePointer,
            interpretCPointer<COpaque>(renderEncoder.objcPtr()),
            inProgressStroke.nativePointer,
            modelTransform.m00,
            modelTransform.m10,
            modelTransform.m20,
            modelTransform.m01,
            modelTransform.m11,
            modelTransform.m21,
            viewTransform.m00,
            viewTransform.m10,
            viewTransform.m20,
            viewTransform.m01,
            viewTransform.m11,
            viewTransform.m21,
            projectionTransform.m00,
            projectionTransform.m10,
            projectionTransform.m20,
            projectionTransform.m01,
            projectionTransform.m11,
            projectionTransform.m21,
        )

    /**
     * Draws a completed stroke using the given render encoder.
     *
     * @param renderEncoder `MTLRenderCommandEncoder` to draw with.
     * @param stroke The stroke to draw.
     * @param modelTransform Affine transform from stroke coordinates to world coordinates, an
     *   identity transform by default. Can be omitted if the stroke coordinates are all in the same
     *   document coordinate space.
     * @param viewTransform Affine transform from world coordinates to view coordinates, an identity
     *   transform by default. Can be omitted if the coordinate space used for the document is the
     *   same as the view coordinate space.
     * @param projectionTransform The projection transform to use for drawing, an identity transform
     *   by default. Must transform from the world coordinate space to Y-up normalized device
     *   coordinates (the lower-left corner is (-1, -1) and the upper-right corner is (1, 1)).
     */
    fun draw(
        renderEncoder: MTLRenderCommandEncoderProtocol,
        stroke: Stroke,
        modelTransform: AffineTransform = AffineTransform.IDENTITY,
        viewTransform: AffineTransform = AffineTransform.IDENTITY,
        projectionTransform: AffineTransform = AffineTransform.IDENTITY,
    ) =
        MetalRendererNative_drawStroke(
            nativePointer,
            interpretCPointer<COpaque>(renderEncoder.objcPtr()),
            stroke.nativePointer,
            modelTransform.m00,
            modelTransform.m10,
            modelTransform.m20,
            modelTransform.m01,
            modelTransform.m11,
            modelTransform.m21,
            viewTransform.m00,
            viewTransform.m10,
            viewTransform.m20,
            viewTransform.m01,
            viewTransform.m11,
            viewTransform.m21,
            projectionTransform.m00,
            projectionTransform.m10,
            projectionTransform.m20,
            projectionTransform.m01,
            projectionTransform.m11,
            projectionTransform.m21,
        )
}
