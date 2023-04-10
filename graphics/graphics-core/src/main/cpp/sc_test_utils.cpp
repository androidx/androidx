/*
 * Copyright 2022 The Android Open Source Project
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

#include <jni.h>
#include <android/api-level.h>
#include <android/native_window_jni.h>
#include <android/hardware_buffer_jni.h>

void colorBufferRegion(void *data, int32_t left, int32_t top, int32_t right, int32_t bottom,
                       uint32_t color, uint32_t stride) {
    auto *ptr = static_cast<uint32_t *>(data);
    ptr += stride * top;

    for (uint32_t y = top; y < bottom; y++) {
        for (uint32_t x = left; x < right; x++) {
            ptr[x] = color;
        }
        ptr += stride;
    }
}

static AHardwareBuffer* allocateBuffer(int32_t width, int32_t height) {
    AHardwareBuffer* buffer = nullptr;
    AHardwareBuffer_Desc desc = {};
    desc.width = width;
    desc.height = height;
    desc.layers = 1;
    desc.usage = AHARDWAREBUFFER_USAGE_COMPOSER_OVERLAY | AHARDWAREBUFFER_USAGE_CPU_WRITE_OFTEN |
                 AHARDWAREBUFFER_USAGE_GPU_SAMPLED_IMAGE;
    desc.format = AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM;

    AHardwareBuffer_allocate(&desc, &buffer);

    return buffer;
}

bool createSolidBuffer(JNIEnv *env, jobject thiz, int32_t width, int32_t height, uint32_t color,
                       AHardwareBuffer **outBuffer, int *fence) {
    AHardwareBuffer* buffer = allocateBuffer(width, height);
    if (!buffer) {
        return true;
    }

    AHardwareBuffer_Desc desc = {};
    AHardwareBuffer_describe(buffer, &desc);

    if (!buffer) {
        return true;
    }

    const ARect rect{0, 0, width, height};
    void *data = nullptr;
    AHardwareBuffer_lock(buffer, AHARDWAREBUFFER_USAGE_CPU_WRITE_OFTEN, -1, &rect, &data);

    if (!data) {
        AHardwareBuffer_release(buffer);
        return true;
    }

    colorBufferRegion(data, 0, 0, width, height, color, desc.stride);
    AHardwareBuffer_unlock(buffer, fence);
    *outBuffer = buffer;
    return false;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_androidx_graphics_surface_SurfaceControlUtils_00024Companion_nGetSolidBuffer(JNIEnv *env,
                                                                                        jobject thiz,
                                                                                        jint width,
                                                                                        jint height,
                                                                                        jint color) {
    AHardwareBuffer *tempBuffer;
    if (createSolidBuffer(env, thiz, width, height, static_cast<uint32_t>(color), &tempBuffer,
                          nullptr)) {
        return nullptr;
    }
    jobject hardwareBuffer = AHardwareBuffer_toHardwareBuffer(env, tempBuffer);
    AHardwareBuffer_release(tempBuffer);
    return hardwareBuffer;
}

static bool getQuadrantBuffer(int32_t width, int32_t height, jint colorTopLeft,
                              jint colorTopRight, jint colorBottomRight,
                              jint colorBottomLeft,
                              AHardwareBuffer** outHardwareBuffer,
                              int* outFence) {
    AHardwareBuffer* buffer = allocateBuffer(width, height);
    if (!buffer) {
        return true;
    }

    AHardwareBuffer_Desc desc = {};
    AHardwareBuffer_describe(buffer, &desc);

    void* data = nullptr;
    const ARect rect{0, 0, width, height};
    int error = AHardwareBuffer_lock(buffer, AHARDWAREBUFFER_USAGE_CPU_WRITE_OFTEN, -1, &rect,
                         &data);
    if (!data || error!=0) {
        return true;
    }

    colorBufferRegion(data, 0, 0, width / 2, height / 2, colorTopLeft, desc.stride);
    colorBufferRegion(data, width / 2, 0, width, height / 2, colorTopRight, desc.stride);
    colorBufferRegion(data, 0, height / 2, width / 2, height, colorBottomLeft,
                      desc.stride);
    colorBufferRegion(data, width / 2, height / 2, width, height, colorBottomRight,
                      desc.stride);

    AHardwareBuffer_unlock(buffer, outFence);

    *outHardwareBuffer = buffer;
    return false;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_androidx_graphics_surface_SurfaceControlUtils_00024Companion_nGetQuadrantBuffer(
        JNIEnv* env, jobject thiz,
        jint width, jint height,
        jint colorTopLeft, jint colorTopRight,
        jint colorBottomRight, jint colorBottomLeft) {
    AHardwareBuffer* buffer;
    if (getQuadrantBuffer(width, height, colorTopLeft, colorTopRight, colorBottomRight,
                          colorBottomLeft, &buffer, nullptr)) {
        return nullptr;
    }
    jobject result = AHardwareBuffer_toHardwareBuffer(env, buffer);
    AHardwareBuffer_release(buffer);
    return result;
}