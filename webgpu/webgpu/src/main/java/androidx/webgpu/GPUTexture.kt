/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.webgpu

import dalvik.annotation.optimization.FastNative

/** A GPU texture object for image data storage. */
public class GPUTexture private constructor(public val handle: Long) : AutoCloseable {
    /**
     * Creates a view of the texture, specifying its usage and subresource range.
     *
     * @param descriptor A descriptor specifying creation options for the texture view.
     * @return The newly created texture view.
     */
    @FastNative
    @JvmName("createView")
    @JvmOverloads
    public external fun createView(descriptor: GPUTextureViewDescriptor? = null): GPUTextureView

    /** Immediately destroys the texture resource. */
    @FastNative @JvmName("destroy") public external fun destroy(): Unit

    /**
     * Gets the depth or number of array layers of the texture at mip level 0.
     *
     * @return The depth or array layer count.
     */
    @FastNative @JvmName("getDepthOrArrayLayers") public external fun getDepthOrArrayLayers(): Int

    @get:JvmName("depthOrArrayLayers")
    public val depthOrArrayLayers: Int
        get() = getDepthOrArrayLayers()

    /**
     * Gets the dimension of the texture (1D, 2D, or 3D).
     *
     * @return The texture dimension.
     */
    @FastNative @JvmName("getDimension") @TextureDimension public external fun getDimension(): Int

    @get:JvmName("dimension")
    public val dimension: Int
        get() = getDimension()

    /**
     * Gets the texture format.
     *
     * @return The texture format.
     */
    @FastNative @JvmName("getFormat") @TextureFormat public external fun getFormat(): Int

    @get:JvmName("format")
    public val format: Int
        get() = getFormat()

    /**
     * Gets the height of the texture at mip level 0.
     *
     * @return The height in texels.
     */
    @FastNative @JvmName("getHeight") public external fun getHeight(): Int

    @get:JvmName("height")
    public val height: Int
        get() = getHeight()

    /**
     * Gets the number of mipmap levels in the texture.
     *
     * @return The number of mip levels.
     */
    @FastNative @JvmName("getMipLevelCount") public external fun getMipLevelCount(): Int

    @get:JvmName("mipLevelCount")
    public val mipLevelCount: Int
        get() = getMipLevelCount()

    /**
     * Gets the number of samples per texel (for multisampled textures).
     *
     * @return The sample count.
     */
    @FastNative @JvmName("getSampleCount") public external fun getSampleCount(): Int

    @get:JvmName("sampleCount")
    public val sampleCount: Int
        get() = getSampleCount()

    /**
     * Gets the usage flags the texture was created with.
     *
     * @return The texture's usage flags.
     */
    @FastNative @JvmName("getUsage") @TextureUsage public external fun getUsage(): Int

    @get:JvmName("usage")
    public val usage: Int
        get() = getUsage()

    /**
     * Gets the width of the texture at mip level 0.
     *
     * @return The width in texels.
     */
    @FastNative @JvmName("getWidth") public external fun getWidth(): Int

    @get:JvmName("width")
    public val width: Int
        get() = getWidth()

    /**
     * Sets a debug label for the texture.
     *
     * @param label The label to assign to the texture.
     */
    @FastNative @JvmName("setLabel") public external fun setLabel(label: String): Unit

    external override fun close()

    override fun equals(other: Any?): Boolean = other is GPUTexture && other.handle == handle

    override fun hashCode(): Int = handle.hashCode()
}
