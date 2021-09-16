#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#include <cinttypes>
#include <cstdlib>

#include "libyuv/convert_argb.h"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "YuvToRgbJni", __VA_ARGS__)

extern "C" {
JNIEXPORT jint Java_androidx_camera_core_ImageProcessingUtil_shiftPixel(
        JNIEnv* env,
        jclass,
        jobject src_y,
        jint src_stride_y,
        jobject src_u,
        jint src_stride_u,
        jobject src_v,
        jint src_stride_v,
        jint src_pixel_stride_y,
        jint src_pixel_stride_uv,
        jint width,
        jint height,
        jint start_offset_y,
        jint start_offset_u,
        jint start_offset_v) {
    uint8_t* src_y_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_y));
    uint8_t* src_u_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_u));
    uint8_t* src_v_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_v));

    // TODO(b/195990691): extend the pixel shift to handle multiple corrupted pixels.
    // We don't support multiple pixel shift now.
    // Y
    for (int i = 0; i < height; i++) {
        memmove(&src_y_ptr[0 + i * src_stride_y],
               &src_y_ptr[start_offset_y + i * src_stride_y],
               width - 1);

        src_y_ptr[width - start_offset_y + i * src_stride_y] =
                src_y_ptr[src_stride_y - start_offset_y + i * src_stride_y];
    }

    // U
    for (int i = 0; i < height / 2; i++) {
        memmove(&src_u_ptr[0 + i * src_stride_u],
               &src_u_ptr[start_offset_u + i * src_stride_u],
               width / 2 - 1);

        src_u_ptr[width / 2 - start_offset_u + i * src_stride_u] =
                src_u_ptr[src_stride_u - start_offset_u + i * src_stride_u];
    }

    // V
    for (int i = 0; i < height / 2; i++) {
        memmove(&src_v_ptr[0 + i * src_stride_v],
               &src_v_ptr[start_offset_v + i * src_stride_v],
               width / 2 - 1);

        src_v_ptr[width / 2 - start_offset_v + i * src_stride_v] =
                src_v_ptr[src_stride_v - start_offset_v + i * src_stride_v];
    }

    return 0;
}

JNIEXPORT jint Java_androidx_camera_core_ImageProcessingUtil_convertAndroid420ToABGR(
        JNIEnv* env,
        jclass,
        jobject src_y,
        jint src_stride_y,
        jobject src_u,
        jint src_stride_u,
        jobject src_v,
        jint src_stride_v,
        jint src_pixel_stride_y,
        jint src_pixel_stride_uv,
        jobject surface,
        jint width,
        jint height,
        jint start_offset_y,
        jint start_offset_u,
        jint start_offset_v) {

    uint8_t* src_y_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_y));
    uint8_t* src_u_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_u));
    uint8_t* src_v_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_v));

    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    if (window == nullptr) {
        return -1;
    }
    ANativeWindow_Buffer buffer;
    int lockResult = ANativeWindow_lock(window, &buffer, NULL);
    if(lockResult != 0 || buffer.format != WINDOW_FORMAT_RGBA_8888) {
        ANativeWindow_release(window);
        return -1;
    }

    uint8_t* dst_abgr_ptr = reinterpret_cast<uint8_t*>(buffer.bits);
    int result = 0;
    // Apply workaround for one pixel shift issue by checking offset.
    if (start_offset_y > 0 || start_offset_u > 0 || start_offset_v > 0) {

        // TODO(b/195990691): extend the pixel shift to handle multiple corrupted pixels.
        // We don't support multiple pixel shift now.
        if (start_offset_y != src_pixel_stride_y
            || start_offset_u != src_pixel_stride_uv
            || start_offset_v != src_pixel_stride_uv) {
            ANativeWindow_unlockAndPost(window);
            ANativeWindow_release(window);
            return -1;
        }

        // Convert yuv to rgb except the last line.
        result = libyuv::Android420ToABGR(src_y_ptr + start_offset_y,
                                          src_stride_y,
                                          src_u_ptr + start_offset_u,
                                          src_stride_u,
                                          src_v_ptr + start_offset_v,
                                          src_stride_v,
                                          src_pixel_stride_uv,
                                          dst_abgr_ptr,
                                          buffer.stride * 4,
                                          width,
                                          height - 1);
        if (result == 0) {
            // Convert the last row with (width - 1) pixels
            // since the last pixel's yuv data is missing.
            result = libyuv::Android420ToABGR(
                    src_y_ptr + start_offset_y + src_stride_y * (height - 1),
                    src_stride_y - 1,
                    src_u_ptr + start_offset_u + src_stride_u * (height - 2) / 2,
                    src_stride_u - 1,
                    src_v_ptr + start_offset_v + src_stride_v * (height - 2) / 2,
                    src_stride_v - 1,
                    src_pixel_stride_uv,
                    dst_abgr_ptr + buffer.stride * 4 * (height - 1),
                    buffer.stride * 4,
                    width - 1,
                    1);
        }

        if (result == 0) {
            // Set the 2x2 pixels on the right bottom by duplicating the 3rd pixel
            // from the right to left in each row.
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    int r_ind = buffer.stride * 4 * (height - 1 - i) + width * 4 - (j * 4 + 1);
                    int g_ind = buffer.stride * 4 * (height - 1 - i) + width * 4 - (j * 4 + 2);
                    int b_ind = buffer.stride * 4 * (height - 1 - i) + width * 4 - (j * 4 + 3);
                    int a_ind = buffer.stride * 4 * (height - 1 - i) + width * 4 - (j * 4 + 4);
                    dst_abgr_ptr[r_ind] = dst_abgr_ptr[r_ind - 8];
                    dst_abgr_ptr[g_ind] = dst_abgr_ptr[g_ind - 8];
                    dst_abgr_ptr[b_ind] = dst_abgr_ptr[b_ind - 8];
                    dst_abgr_ptr[a_ind] = dst_abgr_ptr[a_ind - 8];
                }
            }
        }
    } else {
        result = libyuv::Android420ToABGR(src_y_ptr + start_offset_y,
                                          src_stride_y,
                                          src_u_ptr + start_offset_u,
                                          src_stride_u,
                                          src_v_ptr + start_offset_v,
                                          src_stride_v,
                                          src_pixel_stride_uv,
                                          dst_abgr_ptr,
                                          buffer.stride * 4,
                                          width,
                                          height);
    }

    ANativeWindow_unlockAndPost(window);
    ANativeWindow_release(window);
    return result;
}

}  // extern "C"