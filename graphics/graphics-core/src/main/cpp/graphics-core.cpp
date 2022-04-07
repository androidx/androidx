/*
 * Copyright 2021 The Android Open Source Project
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

#define LOG_TAG "ASurfaceControlTest"

#include <jni.h>
#include <string>
#include <poll.h>
#include <unistd.h>
#include <ctime>
#include <android/native_activity.h>
#include <android/surface_control.h>
#include <android/api-level.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <android/sync.h>

#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jlong JNICALL
Java_androidx_graphics_surface_SurfaceControlCompat_nCreate(JNIEnv *env, jobject thiz,
                                                            jlong surfaceControl,
                                                            jstring debug_name) {
    if (android_get_device_api_level() >= 29) {
        auto aSurfaceControl = reinterpret_cast<ASurfaceControl *>(surfaceControl);
        auto debugName = env->GetStringUTFChars(debug_name, nullptr);
        return reinterpret_cast<jlong>(ASurfaceControl_create(aSurfaceControl,
                                                              debugName));

    } else {
        return 0;
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_androidx_graphics_surface_SurfaceControlCompat_nCreateFromWindow(JNIEnv *env, jobject thiz,
                                                                      jobject surface,
                                                                      jstring debug_name) {
    if (android_get_device_api_level() >= 29) {
        auto AWindow = ANativeWindow_fromSurface(env, surface);
        auto debugName = env->GetStringUTFChars(debug_name, nullptr);
        auto surfaceControl = reinterpret_cast<jlong>(ASurfaceControl_createFromWindow(AWindow,
                                                                                       debugName));

        ANativeWindow_release(AWindow);
        return surfaceControl;
    } else {
        return 0;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_androidx_graphics_surface_SurfaceControlCompat_nRelease(JNIEnv *env, jobject thiz,
                                                             jlong surfaceControl) {
    if (android_get_device_api_level() >= 29) {
        ASurfaceControl_release(reinterpret_cast<ASurfaceControl *>(surfaceControl));
    } else {
        return;
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_androidx_graphics_surface_SurfaceControlCompat_00024Transaction_nTransactionCreate(
        JNIEnv *env, jobject thiz) {
    if (android_get_device_api_level() >= 29) {
        return reinterpret_cast<jlong>(ASurfaceTransaction_create());
    } else {
        return 0;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_androidx_graphics_surface_SurfaceControlCompat_00024Transaction_nTransactionDelete(
        JNIEnv *env, jobject thiz,
        jlong surfaceTransaction) {
    if (android_get_device_api_level() >= 29) {
        ASurfaceTransaction_delete(reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction));
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_androidx_graphics_surface_SurfaceControlCompat_00024Transaction_nTransactionApply(
        JNIEnv *env, jobject thiz,
        jlong surfaceTransaction) {
    if (android_get_device_api_level() >= 29) {
        ASurfaceTransaction_apply(reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction));
    }
}

static struct {
    jclass clazz;
    jmethodID onComplete;
} gTransactionCompletedListenerClassInfo;

#define NANO_SECONDS 1000000000LL

int64_t getSystemTime() {
    struct timespec time;
    int result = clock_gettime(CLOCK_MONOTONIC, &time);
    if (result < 0) {
        return -errno;
    }
    return (time.tv_sec * NANO_SECONDS) + time.tv_nsec;
}

/**
 * This wrapper class mimics the one found in CTS tests, specifcally
 * android_view_cts_ASurfaceControlTest.cpp and serves
 * to allow us to set a callback for Transaction onComplete.
 */
class CallbackWrapper {
public:
    explicit CallbackWrapper(JNIEnv *env, jobject object) {
        env->GetJavaVM(&mVm);
        mCallbackObject = env->NewGlobalRef(object);
    }

    ~CallbackWrapper() {
        getEnv()->DeleteGlobalRef(mCallbackObject);
    }

    void callback(ASurfaceTransactionStats *stats) {
        JNIEnv *env = getEnv();
        int64_t latchTime = ASurfaceTransactionStats_getLatchTime(stats);
        uint64_t presentTime = getSystemTime();

        env->CallVoidMethod(mCallbackObject,
                            gTransactionCompletedListenerClassInfo.onComplete, latchTime,
                            presentTime);
    }

    static void transactionCallbackThunk(void *context, ASurfaceTransactionStats *stats) {
        CallbackWrapper *listener = reinterpret_cast<CallbackWrapper *>(context);
        listener->callback(stats);
        delete listener;
    }

private:
    JavaVM *mVm;
    jobject mCallbackObject;

    JNIEnv *getEnv() {
        JNIEnv *env;
        mVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
        return env;
    }
};

void setupTransactionCompletedListenerClassInfo(JNIEnv *env) {
    //setup transactionCompleteListenerClassInfo for test usage
    jclass transactionCompletedListenerClazz =
            env->FindClass(
                    "androidx/graphics/surface/SurfaceControlCompat$TransactionCompletedListener");
    gTransactionCompletedListenerClassInfo.clazz =
            static_cast<jclass>(env->NewGlobalRef(transactionCompletedListenerClazz));
    gTransactionCompletedListenerClassInfo.onComplete =
            env->GetMethodID(transactionCompletedListenerClazz, "onComplete",
                             "(JJ)V");
}

extern "C"
JNIEXPORT void JNICALL
Java_androidx_graphics_surface_SurfaceControlCompat_00024Transaction_nTransactionSetOnComplete(
        JNIEnv *env,
        jobject thiz,
        jlong surfaceTransaction, jobject callback) {
    if (android_get_device_api_level() >= 29) {
        setupTransactionCompletedListenerClassInfo(env);
        void *context = new CallbackWrapper(env, callback);
        ASurfaceTransaction_setOnComplete(
                reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction),
                reinterpret_cast<void *>(context),
                CallbackWrapper::transactionCallbackThunk);
    }
}
