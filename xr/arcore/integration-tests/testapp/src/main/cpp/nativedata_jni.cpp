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

// Checks the native OpenXR handles returned by XrDevice.getNativeInstanceData() by calling
// into the OpenXR runtime through the returned xrGetInstanceProcAddr.
//
// Only the few OpenXR types used here are declared, matching the OpenXR 1.x ABI on 64-bit
// targets, so that the test app does not need the OpenXR headers. Jetpack XR only ships 64-bit
// native libraries, and build.gradle only builds this library for 64-bit ABIs.

#include <jni.h>

#include <cstdint>

static_assert(sizeof(void*) == 8, "nativedata_jni only supports 64-bit ABIs");

namespace {

using XrResult = int32_t;
using XrStructureType = int32_t;
// On 64-bit targets, OpenXR dispatchable handles are pointers (XR_DEFINE_HANDLE).
using XrInstance = struct XrInstance_T*;
using PFN_xrVoidFunction = void (*)();
using PFN_xrGetInstanceProcAddr = XrResult (*)(XrInstance, const char*, PFN_xrVoidFunction*);

constexpr XrResult XR_SUCCESS = 0;
// Reported when xrGetInstanceProcAddr succeeds but returns a null function pointer, or when a
// JNI call fails.
constexpr jint kNullFunctionPointer = INT32_MIN;

// XR_EXT_debug_utils
constexpr XrStructureType XR_TYPE_DEBUG_UTILS_OBJECT_NAME_INFO_EXT = 1000019000;
constexpr int32_t XR_OBJECT_TYPE_SESSION = 2;

struct XrDebugUtilsObjectNameInfoEXT {
    XrStructureType type;
    const void* next;
    int32_t objectType;
    uint64_t objectHandle;
    const char* objectName;
};

using PFN_xrSetDebugUtilsObjectNameEXT =
    XrResult (*)(XrInstance, const XrDebugUtilsObjectNameInfoEXT*);

PFN_xrGetInstanceProcAddr ToGetInstanceProcAddr(jlong get_instance_proc_addr) {
    return reinterpret_cast<PFN_xrGetInstanceProcAddr>(
        static_cast<uintptr_t>(get_instance_proc_addr));
}

XrInstance ToInstance(jlong instance) {
    return reinterpret_cast<XrInstance>(static_cast<uintptr_t>(instance));
}

}  // namespace

extern "C" {

// Calls xrGetInstanceProcAddr(instance, function_name) and returns its XrResult, or
// kNullFunctionPointer if it succeeded with a null function pointer.
JNIEXPORT jint JNICALL
Java_androidx_xr_arcore_testapp_nativedata_OpenXrProcAddrChecker_nativeResolveFunction(
    JNIEnv* env, jclass /* clazz */, jlong get_instance_proc_addr, jlong instance,
    jstring function_name) {
    PFN_xrGetInstanceProcAddr gipa = ToGetInstanceProcAddr(get_instance_proc_addr);
    if (gipa == nullptr || function_name == nullptr) return kNullFunctionPointer;
    const char* chars = env->GetStringUTFChars(function_name, /* isCopy= */ nullptr);
    if (chars == nullptr) return kNullFunctionPointer;
    PFN_xrVoidFunction function = nullptr;
    const XrResult result = gipa(ToInstance(instance), chars, &function);
    env->ReleaseStringUTFChars(function_name, chars);
    return (result == XR_SUCCESS && function == nullptr) ? kNullFunctionPointer : result;
}

// Resolves xrSetDebugUtilsObjectNameEXT (XR_EXT_debug_utils) through xrGetInstanceProcAddr and
// calls it to name the session. Returns the XrResult of xrGetInstanceProcAddr if the function
// can't be resolved (XR_ERROR_FUNCTION_UNSUPPORTED if the extension is not enabled on the
// instance), otherwise the XrResult of xrSetDebugUtilsObjectNameEXT.
JNIEXPORT jint JNICALL
Java_androidx_xr_arcore_testapp_nativedata_OpenXrProcAddrChecker_nativeSetSessionDebugName(
    JNIEnv* env, jclass /* clazz */, jlong get_instance_proc_addr, jlong instance, jlong session,
    jstring name) {
    PFN_xrGetInstanceProcAddr gipa = ToGetInstanceProcAddr(get_instance_proc_addr);
    if (gipa == nullptr || name == nullptr) return kNullFunctionPointer;
    PFN_xrSetDebugUtilsObjectNameEXT set_object_name = nullptr;
    XrResult result = gipa(ToInstance(instance), "xrSetDebugUtilsObjectNameEXT",
                           reinterpret_cast<PFN_xrVoidFunction*>(&set_object_name));
    if (result != XR_SUCCESS) return result;
    if (set_object_name == nullptr) return kNullFunctionPointer;

    const char* chars = env->GetStringUTFChars(name, /* isCopy= */ nullptr);
    if (chars == nullptr) return kNullFunctionPointer;
    const XrDebugUtilsObjectNameInfoEXT name_info = {
        .type = XR_TYPE_DEBUG_UTILS_OBJECT_NAME_INFO_EXT,
        .next = nullptr,
        .objectType = XR_OBJECT_TYPE_SESSION,
        .objectHandle = static_cast<uint64_t>(session),
        .objectName = chars,
    };
    result = set_object_name(ToInstance(instance), &name_info);
    env->ReleaseStringUTFChars(name, chars);
    return result;
}

}  // extern "C"
