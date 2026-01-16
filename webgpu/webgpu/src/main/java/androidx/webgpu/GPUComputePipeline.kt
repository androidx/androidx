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

/** A GPU pipeline object responsible for the compute stage. */
public class GPUComputePipeline private constructor(public val handle: Long) : AutoCloseable {
    /**
     * Gets the bind group layout for a specific index in the pipeline's layout.
     *
     * @param groupIndex The index of the bind group to get the layout for.
     * @return The bind group layout.
     */
    @FastNative
    @JvmName("getBindGroupLayout")
    public external fun getBindGroupLayout(groupIndex: Int): GPUBindGroupLayout

    /**
     * Sets a debug label for the compute pipeline.
     *
     * @param label The label to assign to the compute pipeline.
     */
    @FastNative @JvmName("setLabel") public external fun setLabel(label: String): Unit

    external override fun close()

    override fun equals(other: Any?): Boolean =
        other is GPUComputePipeline && other.handle == handle

    override fun hashCode(): Int = handle.hashCode()
}
