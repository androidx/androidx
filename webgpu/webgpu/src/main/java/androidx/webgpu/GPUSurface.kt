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

/** Represents a native window or display surface that can be drawn to. */
public class GPUSurface private constructor(public val handle: Long) : AutoCloseable {
    /**
     * Configures the surface with a device, format, size, and other options.
     *
     * @param config The configuration settings for the surface.
     */
    @FastNative
    @JvmName("configure")
    public external fun configure(config: GPUSurfaceConfiguration): Unit

    /**
     * Gets the presentation capabilities supported by the surface for a given adapter.
     *
     * @param adapter The adapter used to query capabilities.
     * @return Status code of the operation.
     */
    @FastNative
    @JvmName("getCapabilities")
    @Throws(WebGpuException::class)
    public external fun getCapabilities(adapter: GPUAdapter): GPUSurfaceCapabilities

    /** Gets the next available texture from the surface for rendering. */
    @FastNative
    @JvmName("getCurrentTexture")
    public external fun getCurrentTexture(): GPUSurfaceTexture

    /**
     * Presents the currently acquired texture to the screen.
     *
     * @return Status code of the operation.
     */
    @FastNative
    @JvmName("present")
    @Throws(WebGpuException::class)
    public external fun present(): Unit

    /**
     * Sets a debug label for the surface.
     *
     * @param label The label to assign to the surface.
     */
    @FastNative @JvmName("setLabel") public external fun setLabel(label: String): Unit

    /** Unconfigures the surface, releasing the texture resources. */
    @FastNative @JvmName("unconfigure") public external fun unconfigure(): Unit

    external override fun close()

    override fun equals(other: Any?): Boolean = other is GPUSurface && other.handle == handle

    override fun hashCode(): Int = handle.hashCode()
}
