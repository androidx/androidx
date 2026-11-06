/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.arcore.testapp.nativedata

/**
 * Calls into the OpenXR runtime through the `xrGetInstanceProcAddr` returned by
 * `XrDevice.getNativeInstanceData()`.
 */
internal object OpenXrProcAddrChecker {
    /** `XR_SUCCESS`. */
    const val XR_SUCCESS: Int = 0

    /** `XR_ERROR_FUNCTION_UNSUPPORTED`: the function's extension is not enabled on the instance. */
    const val XR_ERROR_FUNCTION_UNSUPPORTED: Int = -7

    init {
        System.loadLibrary("nativedata_jni")
    }

    /**
     * Returns the `XrResult` of `xrGetInstanceProcAddr(instance, functionName)`, or [Int.MIN_VALUE]
     * if it succeeded with a null function pointer.
     */
    fun resolveFunction(getInstanceProcAddr: Long, instance: Long, functionName: String): Int =
        nativeResolveFunction(getInstanceProcAddr, instance, functionName)

    /**
     * Resolves `xrSetDebugUtilsObjectNameEXT` (`XR_EXT_debug_utils`) and calls it to name
     * [session]. Returns its `XrResult`, or the `xrGetInstanceProcAddr` result if the function
     * can't be resolved.
     */
    fun setSessionDebugName(
        getInstanceProcAddr: Long,
        instance: Long,
        session: Long,
        name: String,
    ): Int = nativeSetSessionDebugName(getInstanceProcAddr, instance, session, name)

    @JvmStatic
    private external fun nativeResolveFunction(
        getInstanceProcAddr: Long,
        instance: Long,
        functionName: String,
    ): Int

    @JvmStatic
    private external fun nativeSetSessionDebugName(
        getInstanceProcAddr: Long,
        instance: Long,
        session: Long,
        name: String,
    ): Int
}
