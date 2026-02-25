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
@file:JvmName("Exceptions")

package androidx.webgpu

/**
 * Exception for errors originating from the Dawn WebGPU library that do not fit into more specific
 * WebGPU error categories.
 *
 * @param message A detailed message explaining the error.
 */
public class DawnException(message: String) : Exception(message)

/**
 * Exception thrown when a [GPUDevice] is lost and can no longer be used.
 *
 * @param device The [GPUDevice] that was lost.
 * @param reason The reason code indicating why the device was lost.
 * @param message A human-readable message describing the device loss.
 */
public class DeviceLostException(
    public val device: GPUDevice,
    @DeviceLostReason public val reason: Int,
    message: String,
) : Exception(message)

/** Base class for exceptions that can happen at runtime. */
public open class WebGpuRuntimeException(message: String) : Exception(message) {
    public companion object {
        /**
         * Create the exception for the appropriate error type.
         *
         * @param type The [ErrorType].
         * @param message A human-readable message describing the error.
         */
        @JvmStatic
        public fun create(@ErrorType type: Int, message: String): WebGpuRuntimeException =
            when (type) {
                ErrorType.Validation -> ValidationException(message)
                ErrorType.OutOfMemory -> OutOfMemoryException(message)
                ErrorType.Internal -> InternalException(message)
                ErrorType.Unknown -> UnknownException(message)
                else -> UnknownException(message)
            }
    }
}

/**
 * Exception for Validation type errors.
 *
 * @param message A message explaining the error.
 */
public class ValidationException(message: String) : WebGpuRuntimeException(message)

/**
 * Exception for OutOfMemory type errors.
 *
 * @param message A message explaining the error.
 */
public class OutOfMemoryException(message: String) : WebGpuRuntimeException(message)

/**
 * Exception for Internal type errors.
 *
 * @param message A message explaining the error.
 */
public class InternalException(message: String) : WebGpuRuntimeException(message)

/**
 * Exception for Unknown type errors.
 *
 * @param message A message explaining the error.
 */
public class UnknownException(message: String) : WebGpuRuntimeException(message)

public class CompilationInfoRequestException(
    public val reason: String = "",
    @CompilationInfoRequestStatus public val status: Int = CompilationInfoRequestStatus.Success,
) :
    Exception(
        (if (status != CompilationInfoRequestStatus.Success)
            "${ CompilationInfoRequestStatus.toString(status)}: "
        else "") + reason
    ) {}

public class CreatePipelineAsyncException(
    public val reason: String = "",
    @CreatePipelineAsyncStatus public val status: Int = CreatePipelineAsyncStatus.Success,
) :
    Exception(
        (if (status != CreatePipelineAsyncStatus.Success)
            "${ CreatePipelineAsyncStatus.toString(status)}: "
        else "") + reason
    ) {}

public class MapAsyncException(
    public val reason: String = "",
    @MapAsyncStatus public val status: Int = MapAsyncStatus.Success,
) :
    Exception(
        (if (status != MapAsyncStatus.Success) "${ MapAsyncStatus.toString(status)}: " else "") +
            reason
    ) {}

public class PopErrorScopeException(
    public val reason: String = "",
    @PopErrorScopeStatus public val status: Int = PopErrorScopeStatus.Success,
) :
    Exception(
        (if (status != PopErrorScopeStatus.Success) "${ PopErrorScopeStatus.toString(status)}: "
        else "") + reason
    ) {}

public class QueueWorkDoneException(
    public val reason: String = "",
    @QueueWorkDoneStatus public val status: Int = QueueWorkDoneStatus.Success,
) :
    Exception(
        (if (status != QueueWorkDoneStatus.Success) "${ QueueWorkDoneStatus.toString(status)}: "
        else "") + reason
    ) {}

public class RequestAdapterException(
    public val reason: String = "",
    @RequestAdapterStatus public val status: Int = RequestAdapterStatus.Success,
) :
    Exception(
        (if (status != RequestAdapterStatus.Success) "${ RequestAdapterStatus.toString(status)}: "
        else "") + reason
    ) {}

public class RequestDeviceException(
    public val reason: String = "",
    @RequestDeviceStatus public val status: Int = RequestDeviceStatus.Success,
) :
    Exception(
        (if (status != RequestDeviceStatus.Success) "${ RequestDeviceStatus.toString(status)}: "
        else "") + reason
    ) {}

public class WebGpuException(
    public val reason: String = "",
    @Status public val status: Int = Status.Success,
) : Exception((if (status != Status.Success) "${ Status.toString(status)}: " else "") + reason) {}

public class SurfaceGetCurrentTextureException(
    public val reason: String = "",
    @SurfaceGetCurrentTextureStatus
    public val status: Int = SurfaceGetCurrentTextureStatus.SuccessOptimal,
) :
    Exception(
        (if (status != SurfaceGetCurrentTextureStatus.SuccessOptimal)
            "${ SurfaceGetCurrentTextureStatus.toString(status)}: "
        else "") + reason
    ) {}
