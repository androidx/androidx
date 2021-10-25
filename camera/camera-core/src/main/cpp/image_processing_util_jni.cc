#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#include <cinttypes>
#include <cstdlib>

#include "libyuv/convert_argb.h"
#include "libyuv/rotate_argb.h"
#include "libyuv/convert.h"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "YuvToRgbJni", __VA_ARGS__)

#define align_buffer_64(var, size)                                           \
  uint8_t* var##_mem = (uint8_t*)(malloc((size) + 63));         /* NOLINT */ \
  uint8_t* var = (uint8_t*)(((intptr_t)(var##_mem) + 63) & ~63) /* NOLINT */

#define free_aligned_buffer_64(var) \
  free(var##_mem);                  \
  var = 0

static void weave_pixels(const uint8_t* src_u,
                        const uint8_t* src_v,
                        int src_pixel_stride_uv,
                        uint8_t* dst_uv,
                        int width) {
    int i;
    for (i = 0; i < width; ++i) {
        dst_uv[0] = *src_u;
        dst_uv[1] = *src_v;
        dst_uv += 2;
        src_u += src_pixel_stride_uv;
        src_v += src_pixel_stride_uv;
    }
}

static libyuv::RotationMode get_rotation_mode(int rotation) {
    libyuv::RotationMode mode = libyuv::kRotate0;
    switch (rotation) {
        case 0:
            mode = libyuv::kRotate0;
            break;
        case 90:
            mode = libyuv::kRotate90;
            break;
        case 180:
            mode = libyuv::kRotate180;
            break;
        case 270:
            mode = libyuv::kRotate270;
            break;
        default:
            break;
    }
    return mode;
}

extern "C" {
JNIEXPORT jint Java_androidx_camera_core_ImageProcessingUtil_nativeShiftPixel(
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

JNIEXPORT jint Java_androidx_camera_core_ImageProcessingUtil_nativeConvertAndroid420ToABGR(
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
        jobject converted_buffer,
        jint width,
        jint height,
        jint start_offset_y,
        jint start_offset_u,
        jint start_offset_v,
        int rotation) {

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

    libyuv::RotationMode mode = get_rotation_mode(rotation);
    bool has_rotation = rotation != 0;

    uint8_t* buffer_ptr = reinterpret_cast<uint8_t*>(buffer.bits);
    uint8_t* converted_buffer_ptr = static_cast<uint8_t*>(env->GetDirectBufferAddress(
            converted_buffer));

    uint8_t* dst_ptr = has_rotation ? converted_buffer_ptr : buffer_ptr;
    int dst_stride_y = has_rotation ? (width * 4) : (buffer.stride * 4);

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
                                          dst_ptr,
                                          dst_stride_y,
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
                    dst_ptr + dst_stride_y * (height - 1),
                    dst_stride_y,
                    width - 1,
                    1);
        }

        if (result == 0) {
            // Set the 2x2 pixels on the right bottom by duplicating the 3rd pixel
            // from the right to left in each row.
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    int r_ind = dst_stride_y * (height - 1 - i) + width * 4 - (j * 4 + 1);
                    int g_ind = dst_stride_y * (height - 1 - i) + width * 4 - (j * 4 + 2);
                    int b_ind = dst_stride_y * (height - 1 - i) + width * 4 - (j * 4 + 3);
                    int a_ind = dst_stride_y * (height - 1 - i) + width * 4 - (j * 4 + 4);
                    dst_ptr[r_ind] = dst_ptr[r_ind - 8];
                    dst_ptr[g_ind] = dst_ptr[g_ind - 8];
                    dst_ptr[b_ind] = dst_ptr[b_ind - 8];
                    dst_ptr[a_ind] = dst_ptr[a_ind - 8];
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
                                          dst_ptr,
                                          dst_stride_y,
                                          width,
                                          height);
    }

    // TODO(b/203141655): avoid unnecessary memory copy by merging libyuv API for rotation.
    if (result == 0 && has_rotation) {
        result = libyuv::ARGBRotate(dst_ptr,
                                    dst_stride_y,
                                    buffer_ptr,
                                    buffer.stride * 4,
                                    width,
                                    height,
                                    mode);
    }

    ANativeWindow_unlockAndPost(window);
    ANativeWindow_release(window);
    return result;
}

JNIEXPORT jint Java_androidx_camera_core_ImageProcessingUtil_nativeRotateYUV(
        JNIEnv* env,
        jclass,
        jobject src_y,
        jint src_stride_y,
        jobject src_u,
        jint src_stride_u,
        jobject src_v,
        jint src_stride_v,
        jint src_pixel_stride_uv,
        jobject dst_y,
        jint dst_stride_y,
        jint dst_pixel_stride_y,
        jobject dst_u,
        jint dst_stride_u,
        jint dst_pixel_stride_u,
        jobject dst_v,
        jint dst_stride_v,
        jint dst_pixel_stride_v,
        jobject rotated_buffer_y,
        jobject rotated_buffer_u,
        jobject rotated_buffer_v,
        jint width,
        jint height,
        jint rotation) {

    uint8_t *src_y_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(src_y));
    uint8_t *src_u_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(src_u));
    uint8_t *src_v_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(src_v));

    uint8_t *dst_y_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(dst_y));
    uint8_t *dst_u_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(dst_u));
    uint8_t *dst_v_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(dst_v));

    int halfwidth = (width + 1) >> 1;
    int halfheight = (height + 1) >> 1;

    // TODO(b/203141655): avoid unnecessary memory copy by merging libyuv API for rotation.
    uint8_t *rotated_y_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(rotated_buffer_y));
    uint8_t *rotated_u_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(rotated_buffer_u));
    uint8_t *rotated_v_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(rotated_buffer_v));

    libyuv::RotationMode mode = get_rotation_mode(rotation);
    bool flip_wh = (mode == libyuv::kRotate90 || mode == libyuv::kRotate270);

    int rotated_stride_y = flip_wh ? height : width;
    int rotated_stride_u = flip_wh ? halfheight : halfwidth;
    int rotated_stride_v = flip_wh ? halfheight : halfwidth;

    int rotated_width = flip_wh ? height : width;
    int rotated_height = flip_wh ? width : height;
    int rotated_halfwidth = flip_wh ? halfheight : halfwidth;
    int rotated_halfheight = flip_wh ? halfwidth : halfheight;

    int result = 0;
    const ptrdiff_t vu_off = src_v_ptr - src_u_ptr;

    if (src_pixel_stride_uv == 1) {
        // I420
        result = libyuv::I420Rotate(src_y_ptr,
                                    src_stride_y,
                                    src_u_ptr,
                                    src_stride_u,
                                    src_v_ptr,
                                    src_stride_v,
                                    rotated_y_ptr,
                                    rotated_stride_y,
                                    rotated_u_ptr,
                                    rotated_stride_u,
                                    rotated_v_ptr,
                                    rotated_stride_v,
                                    width,
                                    height,
                                    mode);
    } else if (src_pixel_stride_uv == 2 && vu_off == -1 &&
               src_stride_u == src_stride_v) {
        // NV21
        result = libyuv::NV12ToI420Rotate(src_y_ptr,
                                          src_stride_y,
                                          src_u_ptr,
                                          src_stride_u,
                                          rotated_y_ptr,
                                          rotated_stride_y,
                                          rotated_u_ptr,
                                          rotated_stride_u,
                                          rotated_v_ptr,
                                          rotated_stride_v,
                                          width,
                                          height,
                                          mode);
    } else if (src_pixel_stride_uv == 2 && vu_off == 1 && src_stride_u == src_stride_v) {
        // NV12
        result = libyuv::NV12ToI420Rotate(src_y_ptr,
                                          src_stride_y,
                                          src_v_ptr,
                                          src_stride_v,
                                          rotated_y_ptr,
                                          rotated_stride_y,
                                          rotated_u_ptr,
                                          rotated_stride_u,
                                          rotated_v_ptr,
                                          rotated_stride_v,
                                          width,
                                          height,
                                          mode);
    } else {
        // General case fallback creates NV12
        align_buffer_64(plane_uv, halfwidth * 2 * halfheight);
        uint8_t* dst_uv = plane_uv;
        for (int y = 0; y < halfheight; y++) {
            weave_pixels(src_v_ptr, src_u_ptr, src_pixel_stride_uv, dst_uv, halfwidth);
            src_u += src_stride_u;
            src_v += src_stride_v;
            dst_uv += halfwidth * 2;
        }

        result = libyuv::NV12ToI420Rotate(src_y_ptr,
                                          src_stride_y,
                                          plane_uv,
                                          halfwidth * 2,
                                          rotated_y_ptr,
                                          rotated_stride_y,
                                          rotated_u_ptr,
                                          rotated_stride_u,
                                          rotated_v_ptr,
                                          rotated_stride_v,
                                          width,
                                          height,
                                          mode);
        free_aligned_buffer_64(plane_uv);
    }

    if (result == 0) {
        // Y
        uint8_t *dst_y = rotated_y_ptr;
        int rotated_pixel_stride_y = 1;
        for (int i = 0; i < rotated_height; i++) {
            for (int j = 0; j < rotated_width; j++) {
                dst_y_ptr[i * dst_stride_y + j * dst_pixel_stride_y] =
                        dst_y[i * rotated_stride_y + j * rotated_pixel_stride_y];
            }
        }

        // U
        uint8_t *dst_u = rotated_u_ptr;
        int rotated_pixel_stride_u = 1;
        for (int i = 0; i < rotated_halfheight; i++) {
            for (int j = 0; j < rotated_halfwidth; j++) {
                dst_u_ptr[i * dst_stride_u + j * dst_pixel_stride_u] =
                        dst_u[i * rotated_stride_u + j * rotated_pixel_stride_u];
            }
        }

        // V
        uint8_t *dst_v = rotated_v_ptr;
        int rotated_pixel_stride_v = 1;
        for (int i = 0; i < rotated_halfheight; i++) {
            for (int j = 0; j < rotated_halfwidth; j++) {
                dst_v_ptr[i * dst_stride_v + j * dst_pixel_stride_v] =
                        dst_v[i * rotated_stride_v + j * rotated_pixel_stride_v];
            }
        }
    }

    return result;
}

}  // extern "C"