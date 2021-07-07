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

JNIEXPORT jint Java_androidx_camera_core_ImageYuvToRgbConverter_convertAndroid420ToABGR(
        JNIEnv* env,
        jclass,
        jobject src_y,
        jint src_stride_y,
        jobject src_u,
        jint src_stride_u,
        jobject src_v,
        jint src_stride_v,
        jint src_pixel_stride_uv,
        jobject surface,
        jint width,
        jint height,
        jint start_offset) {

    uint8_t* src_y_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_y));
    uint8_t* src_u_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_u));
    uint8_t* src_v_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_v));
    src_y_ptr += start_offset;
    src_u_ptr += start_offset;
    src_v_ptr += start_offset;

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
    int result = libyuv::Android420ToABGR(src_y_ptr,
                                    src_stride_y,
                                    src_u_ptr,
                                    src_stride_u,
                                    src_v_ptr,
                                    src_stride_v,
                                    src_pixel_stride_uv,
                                    dst_abgr_ptr,
                                    buffer.stride * 4,
                                    width,
                                    height);
    ANativeWindow_unlockAndPost(window);
    ANativeWindow_release(window);
    return result;
}

}  // extern "C"