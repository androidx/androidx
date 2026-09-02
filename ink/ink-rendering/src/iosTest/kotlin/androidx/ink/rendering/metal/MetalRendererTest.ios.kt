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
import androidx.ink.geometry.ImmutableAffineTransform
import androidx.ink.rendering.test.AbstractStrokeRendererTest
import androidx.ink.storage.decode
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInputBatch
import androidx.kruth.assertThat
import kotlin.test.Test
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGImageRelease
import platform.CoreImage.CIContext
import platform.CoreImage.CIImage
import platform.CoreImage.createCGImage
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
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
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

@OptIn(ExperimentalInkCrossPlatformRenderingApi::class, ExperimentalForeignApi::class)
class MetalRendererTest : AbstractStrokeRendererTest() {

    override fun loadCursiveHelloInputs(): ImmutableStrokeInputBatch {
        val path =
            checkNotNull(findPathForResource("cursive_stylus", "inputbatch")) {
                "Could not find cursive_stylus.inputbatch in the test bundle."
            }
        val nsData = checkNotNull(NSData.dataWithContentsOfFile(path))
        val bytes = ByteArray(nsData.length.toInt())
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), nsData.bytes, nsData.length) }
        }
        return StrokeInputBatch.decode(bytes)
    }

    private fun findPathForResource(name: String, type: String?): String? =
        // The resources live in the test bundle, which is the only bundle ending with ".xctest".
        // The
        // more traditional way to access the test bundle is via the test class, but this doesn't
        // work
        // with a KMP test.
        @Suppress("UNCHECKED_CAST") // Kotlin doesn't understand the type of NSBundle.allBundles
        (NSBundle.allBundles as List<NSBundle>)
            .firstOrNull { it.bundlePath.endsWith(".xctest") }
            ?.pathForResource(name, ofType = type)

    private val device =
        checkNotNull(MTLCreateSystemDefaultDevice()) { "Could not create Metal device." }
    private val commandQueue =
        checkNotNull(device.newCommandQueue()) { "Could not create Metal command queue." }
    private val checkerboardUIImage: UIImage? by lazy {
        findPathForResource("checkerboard", "png")?.let { UIImage(contentsOfFile = it) }
    }
    private val textureStore = TextureImageStore { id ->
        when (id) {
            "checkerboard" -> checkerboardUIImage?.CGImage
            else -> null
        }
    }
    private val renderer =
        MetalRenderer(
            device,
            MTLPixelFormatBGRA8Unorm_sRGB,
            MTLPixelFormatDepth32Float_Stencil8,
            sampleCount = null,
            textureImageStore = textureStore,
        )

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
                    clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 0.0)
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

    private val emptyImageData =
        UIImagePNGRepresentation(renderToImage(width = 200, height = 200) {})!!

    @Test
    fun renderToImage_comparisonTest() {
        // Verifies that comparing the results of renderToImage works as expected.
        val otherEmptyImageData =
            UIImagePNGRepresentation(renderToImage(width = 200, height = 200) {})
        assertThat(emptyImageData).isEqualTo(otherEmptyImageData)
        assertThat(emptyImageData).isNotSameInstanceAs(otherEmptyImageData)
    }

    override fun renderAndCompareToGolden(
        stroke: Stroke,
        transform: AffineTransform,
        imageWidth: Int,
        imageHeight: Int,
        goldenName: String,
    ) {
        val image =
            renderToImage(width = imageWidth, height = imageHeight) { renderEncoder ->
                renderer.draw(
                    renderEncoder,
                    stroke,
                    viewTransform = transform,
                    projectionTransform = screenCoordsToNormalized(imageWidth, imageHeight),
                )
            }
        assertImage(image, goldenName)
    }

    override fun renderAndCompareToGolden(
        inProgressStroke: InProgressStroke,
        transform: AffineTransform,
        imageWidth: Int,
        imageHeight: Int,
        goldenName: String,
    ) {
        val image =
            renderToImage(width = imageWidth, height = imageHeight) { renderEncoder ->
                renderer.draw(
                    renderEncoder,
                    inProgressStroke,
                    viewTransform = transform,
                    projectionTransform = screenCoordsToNormalized(imageWidth, imageHeight),
                )
            }
        assertImage(image, goldenName)
    }

    override fun assertLazyAssertsPass() {
        // TODO(b/542290747): Specific image-diff assertion is currently upstream only.
    }

    private fun assertImage(image: UIImage, goldenName: String) {
        // As a smoke-test that can deal with the lack of Jetpack image-diff test infra that works
        // on Kotlin-native / iOS, we compare the rendered image to an empty image.
        assertThat(UIImagePNGRepresentation(image)!!).isNotEqualTo(emptyImageData)

        // TODO(b/542290747): Specific image-diff assertion is currently upstream only.
    }
}
