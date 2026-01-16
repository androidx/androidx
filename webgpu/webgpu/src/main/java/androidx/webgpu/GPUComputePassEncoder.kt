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

/** Used to record compute pipeline dispatch commands within a command encoder. */
public class GPUComputePassEncoder private constructor(public val handle: Long) : AutoCloseable {
    /**
     * Dispatches compute workgroups with explicitly defined counts.
     *
     * @param workgroupCountX The number of workgroups to dispatch in the X dimension.
     * @param workgroupCountY The number of workgroups to dispatch in the Y dimension.
     * @param workgroupCountZ The number of workgroups to dispatch in the Z dimension.
     */
    @FastNative
    @JvmName("dispatchWorkgroups")
    @JvmOverloads
    public external fun dispatchWorkgroups(
        workgroupCountX: Int,
        workgroupCountY: Int = 1,
        workgroupCountZ: Int = 1,
    ): Unit

    /**
     * Dispatches compute workgroups using arguments from a buffer.
     *
     * @param indirectBuffer The buffer containing the dispatch arguments.
     * @param indirectOffset The offset in the buffer where dispatch arguments start.
     */
    @FastNative
    @JvmName("dispatchWorkgroupsIndirect")
    public external fun dispatchWorkgroupsIndirect(
        indirectBuffer: GPUBuffer,
        indirectOffset: Long,
    ): Unit

    /** Ends the compute pass. */
    @FastNative @JvmName("end") public external fun end(): Unit

    /**
     * Inserts a debug marker command into the pass.
     *
     * @param markerLabel The label for the debug marker.
     */
    @FastNative
    @JvmName("insertDebugMarker")
    public external fun insertDebugMarker(markerLabel: String): Unit

    /** Ends the most recently pushed debug group. */
    @FastNative @JvmName("popDebugGroup") public external fun popDebugGroup(): Unit

    /**
     * Starts a new named debug group.
     *
     * @param groupLabel The label for the debug group.
     */
    @FastNative
    @JvmName("pushDebugGroup")
    public external fun pushDebugGroup(groupLabel: String): Unit

    /**
     * Sets the bind group for a given index.
     *
     * @param groupIndex The index of the bind group to set.
     * @param group The bind group object to set, or {@code null} to unbind.
     * @param dynamicOffsets An array of dynamic offsets for uniform/storage buffers.
     */
    @FastNative
    @JvmName("setBindGroup")
    @JvmOverloads
    public external fun setBindGroup(
        groupIndex: Int,
        group: GPUBindGroup? = null,
        dynamicOffsets: IntArray = intArrayOf(),
    ): Unit

    /**
     * Sets a debug label for the compute pass encoder.
     *
     * @param label The label to assign to the compute pass encoder.
     */
    @FastNative @JvmName("setLabel") public external fun setLabel(label: String): Unit

    /**
     * Sets the active compute pipeline.
     *
     * @param pipeline The compute pipeline to use for subsequent dispatch calls.
     */
    @FastNative
    @JvmName("setPipeline")
    public external fun setPipeline(pipeline: GPUComputePipeline): Unit

    external override fun close()

    override fun equals(other: Any?): Boolean =
        other is GPUComputePassEncoder && other.handle == handle

    override fun hashCode(): Int = handle.hashCode()
}
