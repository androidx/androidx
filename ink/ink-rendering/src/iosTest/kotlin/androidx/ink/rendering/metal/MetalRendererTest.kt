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

import androidx.annotation.ColorInt
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.InputToolType
import androidx.ink.geometry.ImmutableAffineTransform
import androidx.ink.rendering.ExperimentalInkCrossPlatformRenderingApi
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.MutableStrokeInputBatch
import kotlin.test.Test
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGImageRelease
import platform.CoreImage.CIContext
import platform.CoreImage.CIImage
import platform.CoreImage.createCGImage
import platform.Metal.MTLClearColorMake
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLLoadActionClear
import platform.Metal.MTLPixelFormatBGRA8Unorm_sRGB
import platform.Metal.MTLPixelFormatDepth32Float_Stencil8
import platform.Metal.MTLRenderCommandEncoderProtocol
import platform.Metal.MTLRenderPassDescriptor
import platform.Metal.MTLStorageModePrivate
import platform.Metal.MTLStoreActionDontCare
import platform.Metal.MTLStoreActionStore
import platform.Metal.MTLTextureDescriptor
import platform.Metal.MTLTextureProtocol
import platform.Metal.MTLTextureUsageRenderTarget
import platform.Metal.MTLTextureUsageShaderRead
import platform.UIKit.UIImage

@OptIn(ExperimentalInkCrossPlatformRenderingApi::class, ExperimentalForeignApi::class)
class MetalRendererTest {

    private val device = checkNotNull(MTLCreateSystemDefaultDevice())
    private val commandQueue = checkNotNull(device.newCommandQueue())
    private val renderer =
        MetalRenderer(device, MTLPixelFormatBGRA8Unorm_sRGB, MTLPixelFormatDepth32Float_Stencil8)

    fun MTLTextureProtocol.toImage(): UIImage {
        val ciImage =
            checkNotNull(
                CIImage.imageWithMTLTexture(
                    this as objcnames.protocols.MTLTextureProtocol,
                    options = null,
                )
            )
        val height = ciImage.extent.useContents { height }.toDouble()
        // Metal is Y-down while Core Image is Y-up, so we need to flip the image vertically.
        val flippedImage = ciImage.imageByApplyingTransform(CGAffineTransformMakeScale(1.0, -1.0))
        val cgImage =
            checkNotNull(
                CIContext.context().createCGImage(flippedImage, fromRect = flippedImage.extent)
            )
        return try {
            UIImage(cgImage)
        } finally {
            CGImageRelease(cgImage)
        }
    }

    fun testInProgressStroke(): InProgressStroke {
        val brush = Brush.createWithColorIntArgb(BrushFamily(), AVOCADO_GREEN, 25F, 0.1F)
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(brush)
        val inputs = MutableStrokeInputBatch()
        inputs.add(type = InputToolType.STYLUS, x = 50.0f, y = 50.0f, elapsedTimeMillis = 0)
        inputs.add(type = InputToolType.STYLUS, x = 150.0f, y = 150.0f, elapsedTimeMillis = 1000)
        inProgressStroke.enqueueInputs(inputs, predictedInputs = ImmutableStrokeInputBatch.EMPTY)
        inProgressStroke.finishInput()
        inProgressStroke.updateShape(currentElapsedTimeMillis = 1000)
        return inProgressStroke
    }

    fun screenCoordsToNormalized(width: Int, height: Int) =
        ImmutableAffineTransform(
            m00 = 2f / width.toFloat(), // Scale x from [0, width] to [0, 2]
            m10 = 0f,
            m20 = -1f, // Translate x from [0, 2] to [-1, 1]
            m01 = 0f,
            m11 = -2f / height.toFloat(), // Scale y from [0, height] to [-2, 0]
            m21 = 1f, // Translate y from [-2, 0] to [1, -1]
        )

    fun renderToImage(
        width: Int,
        height: Int,
        block: (MTLRenderCommandEncoderProtocol) -> Unit,
    ): UIImage {
        val colorTexture =
            checkNotNull(
                device.newTextureWithDescriptor(
                    MTLTextureDescriptor.texture2DDescriptorWithPixelFormat(
                            pixelFormat = MTLPixelFormatBGRA8Unorm_sRGB,
                            width = width.toULong(),
                            height = height.toULong(),
                            mipmapped = false,
                        )
                        .apply { usage = MTLTextureUsageShaderRead or MTLTextureUsageRenderTarget }
                )
            )

        val stencilTexture =
            checkNotNull(
                device.newTextureWithDescriptor(
                    MTLTextureDescriptor.texture2DDescriptorWithPixelFormat(
                            pixelFormat = MTLPixelFormatDepth32Float_Stencil8,
                            width = width.toULong(),
                            height = height.toULong(),
                            mipmapped = false,
                        )
                        .apply {
                            storageMode = MTLStorageModePrivate
                            usage = MTLTextureUsageRenderTarget or MTLTextureUsageShaderRead
                        }
                )
            )

        val renderPassDescriptor =
            MTLRenderPassDescriptor().apply {
                colorAttachments.objectAtIndexedSubscript(0UL).apply {
                    texture = colorTexture
                    loadAction = MTLLoadActionClear
                    storeAction = MTLStoreActionStore
                    clearColor = MTLClearColorMake(red = 1.0, green = 1.0, blue = 1.0, alpha = 1.0)
                }
                stencilAttachment.apply {
                    this.texture = stencilTexture
                    loadAction = MTLLoadActionClear
                    storeAction = MTLStoreActionDontCare
                    clearStencil = 0U
                }
                depthAttachment.apply {
                    this.texture = stencilTexture
                    loadAction = MTLLoadActionClear
                    storeAction = MTLStoreActionDontCare
                }
            }

        val commandBuffer = checkNotNull(commandQueue.commandBuffer())
        val renderEncoder =
            checkNotNull(commandBuffer.renderCommandEncoderWithDescriptor(renderPassDescriptor))

        block(renderEncoder)

        renderEncoder.endEncoding()
        commandBuffer.commit()
        commandBuffer.waitUntilCompleted()

        return colorTexture.toImage()
    }

    @Test
    fun drawInProgressStroke_shouldDraw() {
        val width = 200
        val height = 200
        val image =
            renderToImage(width, height) { renderEncoder ->
                renderer.draw(
                    renderEncoder,
                    testInProgressStroke(),
                    projectionTransform = screenCoordsToNormalized(width, height),
                )
            }
        // TODO(b/542290747): This is currently tested upstream only.
    }

    @Test
    fun drawStroke_shouldDraw() {
        val width = 200
        val height = 200
        val image =
            renderToImage(width, height) { renderEncoder ->
                renderer.draw(
                    renderEncoder,
                    testInProgressStroke().toImmutable(),
                    projectionTransform = screenCoordsToNormalized(width, height),
                )
            }
        // TODO(b/542290747): This is currently tested upstream only.
    }

    private companion object {
        @ColorInt const val AVOCADO_GREEN = 0xff558b2f.toInt()
    }
}
