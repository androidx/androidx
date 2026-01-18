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
import java.util.concurrent.Executor

/** Represents the compiled code for one or more shader stages (e.g., WGSL or SPIR-V). */
public class GPUShaderModule private constructor(public val handle: Long) : AutoCloseable {
    /** Asynchronously requests detailed information about the shader module's compilation. */
    @FastNative
    @JvmName("getCompilationInfo")
    public external fun getCompilationInfo(
        callbackExecutor: java.util.concurrent.Executor,
        callback: GPURequestCallback<GPUCompilationInfo>,
    ): Unit

    /** Asynchronously requests detailed information about the shader module's compilation. */
    @Throws(WebGpuException::class)
    public suspend fun getCompilationInfo(): GPUCompilationInfo = awaitGPURequest { callback ->
        getCompilationInfo(Executor(Runnable::run), callback)
    }

    /**
     * Sets a debug label for the shader module.
     *
     * @param label The label to assign to the shader module.
     */
    @FastNative @JvmName("setLabel") public external fun setLabel(label: String): Unit

    external override fun close()

    override fun equals(other: Any?): Boolean = other is GPUShaderModule && other.handle == handle

    override fun hashCode(): Int = handle.hashCode()
}
