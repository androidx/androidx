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
#include <unistd.h>
#include <android/native_activity.h>
#include <android/surface_control.h>
#include <android/api-level.h>
#include <android/native_window_jni.h>
#include <android/hardware_buffer_jni.h>
#include <android/log.h>
#include <android/sync.h>
#include "egl_utils.h"
#include "sync_fence.h"
#include "buffer_transform_hint_resolver.h"

#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static struct {
    jclass clazz{};
    jfieldID left{};
    jfieldID top{};
    jfieldID right{};
    jfieldID bottom{};
} gRectInfo;

jlong JniBindings_nCreate(JNIEnv *env, jclass,
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

jlong JniBindings_nCreateFromSurface(JNIEnv *env,
                                                              jclass,
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

void JniBindings_nRelease(JNIEnv *env, jclass, jlong surfaceControl) {
    if (android_get_device_api_level() >= 29) {
        ASurfaceControl_release(reinterpret_cast<ASurfaceControl *>(surfaceControl));
    } else {
        return;
    }
}

jlong JniBindings_nTransactionCreate(JNIEnv *env, jclass) {
    if (android_get_device_api_level() >= 29) {
        return reinterpret_cast<jlong>(ASurfaceTransaction_create());
    } else {
        return 0;
    }
}

void JniBindings_nTransactionDelete(JNIEnv *env, jclass, jlong surfaceTransaction) {
    if (android_get_device_api_level() >= 29) {
        ASurfaceTransaction_delete(reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction));
    }
}

void JniBindings_nTransactionApply(JNIEnv *env, jclass, jlong surfaceTransaction) {
    if (android_get_device_api_level() >= 29) {
        ASurfaceTransaction_apply(reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction));
    }
}

void JniBindings_nTransactionReparent(JNIEnv *env, jclass, jlong surfaceTransaction,
                                      jlong surfaceControl, jlong newParent) {
    if (android_get_device_api_level() >= 29) {
        auto parent = (newParent != 0L) ? reinterpret_cast<ASurfaceControl *>(newParent) : nullptr;
        ASurfaceTransaction_reparent(reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction),
                                     reinterpret_cast<ASurfaceControl *>(surfaceControl),
                                     parent);
    }
}

static struct {
    bool CLASS_INFO_INITIALIZED = false;
    jclass clazz{};
    jmethodID onComplete{};
} gTransactionCompletedListenerClassInfo;

static struct {
    bool CLASS_INFO_INITIALIZED = false;
    jclass clazz{};
    jmethodID onCommit{};
} gTransactionCommittedListenerClassInfo;

static struct {
    bool CLASS_INFO_INITIALIZED = false;
    jclass clazz{};
    jmethodID dupeFileDescriptor{};
} gSyncFenceClassInfo;

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
    virtual ~CallbackWrapper() = default;

    virtual void callback(ASurfaceTransactionStats *stats) = 0;

    static void transactionCallbackThunk(void *context, ASurfaceTransactionStats *stats) {
        CallbackWrapper *listener = reinterpret_cast<CallbackWrapper *>(context);
        listener->callback(stats);
        delete listener;
    }

protected:
    JavaVM *mVm{};
    jobject mCallbackObject{};

    JNIEnv *getEnv() {
        JNIEnv *env;
        mVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
        return env;
    }
};

class OnCompleteCallbackWrapper : public CallbackWrapper {
public:
    explicit OnCompleteCallbackWrapper(JNIEnv *env, jobject object) {
        env->GetJavaVM(&mVm);
        mCallbackObject = env->NewGlobalRef(object);
    }

    ~OnCompleteCallbackWrapper() override {
        getEnv()->DeleteGlobalRef(mCallbackObject);
    }

    void callback(ASurfaceTransactionStats *stats) override {
        JNIEnv *env = getEnv();
        env->CallVoidMethod(mCallbackObject,
                            gTransactionCompletedListenerClassInfo.onComplete);
    }
};

class OnCommitCallbackWrapper : public CallbackWrapper {
public:
    explicit OnCommitCallbackWrapper(JNIEnv *env, jobject object) {
        env->GetJavaVM(&mVm);
        mCallbackObject = env->NewGlobalRef(object);
    }

    ~OnCommitCallbackWrapper() override {
        getEnv()->DeleteGlobalRef(mCallbackObject);
    }

    void callback(ASurfaceTransactionStats *stats) override {
        JNIEnv *env = getEnv();
        env->CallVoidMethod(mCallbackObject,
                            gTransactionCommittedListenerClassInfo.onCommit);
    }
};

void setupTransactionCompletedListenerClassInfo(JNIEnv *env) {
    //ensure we only ever initialize class info once
    if (!gTransactionCompletedListenerClassInfo.CLASS_INFO_INITIALIZED) {
        //setup transactionCompleteListenerClassInfo for test usage
        jclass transactionCompletedListenerClazz =
                env->FindClass(
                        "androidx/graphics/surface/"
                        "SurfaceControlCompat$TransactionCompletedListener");
        gTransactionCompletedListenerClassInfo.clazz =
                static_cast<jclass>(env->NewGlobalRef(transactionCompletedListenerClazz));
        gTransactionCompletedListenerClassInfo.onComplete =
                env->GetMethodID(transactionCompletedListenerClazz, "onTransactionCompleted",
                                 "()V");

        gTransactionCompletedListenerClassInfo.CLASS_INFO_INITIALIZED = true;
    }

}

void setupTransactionCommittedListenerClassInfo(JNIEnv *env) {
    //ensure we only ever initialize class info once
    if (!gTransactionCommittedListenerClassInfo.CLASS_INFO_INITIALIZED) {
        //setup transactionCommittedListenerClassInfo for test usage
        jclass transactionCommittedListenerClazz =
                env->FindClass(
                        "androidx/graphics/surface/"
                        "SurfaceControlCompat$TransactionCommittedListener");
        gTransactionCommittedListenerClassInfo.clazz =
                static_cast<jclass>(env->NewGlobalRef(transactionCommittedListenerClazz));
        gTransactionCommittedListenerClassInfo.onCommit =
                env->GetMethodID(transactionCommittedListenerClazz, "onTransactionCommitted",
                                 "()V");

        gTransactionCommittedListenerClassInfo.CLASS_INFO_INITIALIZED = true;
    }
}

void JniBindings_nTransactionSetOnComplete(JNIEnv *env, jclass, jlong surfaceTransaction,
                                           jobject callback) {
    if (android_get_device_api_level() >= 29) {
        setupTransactionCompletedListenerClassInfo(env);
        void *context = new OnCompleteCallbackWrapper(env, callback);
        ASurfaceTransaction_setOnComplete(
                reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction),
                reinterpret_cast<void *>(context),
                CallbackWrapper::transactionCallbackThunk);
    }
}

void JniBindings_nTransactionSetOnCommit(JNIEnv *env, jclass, jlong surfaceTransaction,
                                         jobject listener) {
    if (android_get_device_api_level() >= 31) {
        setupTransactionCommittedListenerClassInfo(env);
        void *context = new OnCommitCallbackWrapper(env, listener);
        ASurfaceTransaction_setOnCommit(
                reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction),
                reinterpret_cast<void *>(context),
                CallbackWrapper::transactionCallbackThunk);
    }
}

void setupSyncFenceClassInfo(JNIEnv *env) {
    if (!gSyncFenceClassInfo.CLASS_INFO_INITIALIZED) {
        jclass syncFenceClazz = env->FindClass("androidx/hardware/SyncFenceV19");
        gSyncFenceClassInfo.clazz = static_cast<jclass>(env->NewGlobalRef(syncFenceClazz));
        gSyncFenceClassInfo.dupeFileDescriptor =
                env->GetMethodID(gSyncFenceClassInfo.clazz, "dupeFileDescriptor", "()I");
        gSyncFenceClassInfo.CLASS_INFO_INITIALIZED = true;
    }
}

int dup_fence_fd(JNIEnv *env, jobject syncFence) {
    setupSyncFenceClassInfo(env);
    return env->CallIntMethod(syncFence, gSyncFenceClassInfo.dupeFileDescriptor);
}

/* Helper method to extract the SyncFence file descriptor
 */
jint JniBindings_nDupFenceFd(JNIEnv *env, jclass, jobject syncFence) {
    return dup_fence_fd(env, syncFence);
}

void JniBindings_nSetBuffer(JNIEnv *env, jclass, jlong surfaceTransaction,
                            jlong surfaceControl, jobject hBuffer,
                            jobject syncFence) {
    if (android_get_device_api_level() >= 29) {
        auto transaction = reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction);
        auto sc = reinterpret_cast<ASurfaceControl *>(surfaceControl);
        AHardwareBuffer* hardwareBuffer;
        auto fence_fd = -1;
        if (hBuffer) {
            hardwareBuffer = AHardwareBuffer_fromHardwareBuffer(env, hBuffer);
            fence_fd = dup_fence_fd(env, syncFence);
        }
        ASurfaceTransaction_setBuffer(transaction, sc, hardwareBuffer, fence_fd);
    }
}

void JniBindings_nSetVisibility(
        JNIEnv *env, jclass,
        jlong surfaceTransaction, jlong surfaceControl, jbyte jVisibility) {
    if (android_get_device_api_level() >= 29) {
        auto st = reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction);
        auto sc = reinterpret_cast<ASurfaceControl *>(surfaceControl);
        ASurfaceTransaction_setVisibility(st, sc, jVisibility);
    }
}

void JniBindings_nSetZOrder(
        JNIEnv *env, jclass,
        jlong surfaceTransaction, jlong surfaceControl, jint z_order) {
    if (android_get_device_api_level() >= 29) {
        auto st = reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction);
        auto sc = reinterpret_cast<ASurfaceControl *>(surfaceControl);
        ASurfaceTransaction_setZOrder(st, sc, z_order);
    }
}

ARect extract_arect(JNIEnv *env, jobject rect) {
    ARect result;
    result.left = env->GetIntField(rect, gRectInfo.left);
    result.top = env->GetIntField(rect, gRectInfo.top);
    result.right = env->GetIntField(rect, gRectInfo.right);
    result.bottom = env->GetIntField(rect, gRectInfo.bottom);
    return result;
}

void JniBindings_nSetDamageRegion(
        JNIEnv *env, jclass,
        jlong surfaceTransaction, jlong surfaceControl,
        jobject rect) {
    if (android_get_device_api_level() >= 29) {
        auto st = reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction);
        auto sc = reinterpret_cast<ASurfaceControl *>(surfaceControl);

        if (rect == nullptr) {
            ASurfaceTransaction_setDamageRegion(st, sc, nullptr, 0);
            return;
        }

        ARect result = extract_arect(env, rect);

        ASurfaceTransaction_setDamageRegion(st, sc, &result, 1);
    }
}

void JniBindings_nSetDesiredPresentTime(
        JNIEnv *env, jclass,
        jlong surfaceTransaction, int64_t desiredPresentTimeNano) {
    if (android_get_device_api_level() >= 29) {
        ASurfaceTransaction_setDesiredPresentTime(
                reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction),
                desiredPresentTimeNano);
    }
}

void JniBindings_nSetBufferTransparency(
        JNIEnv *env, jclass,
        jlong surfaceTransaction, jlong surfaceControl, jbyte transparency) {
    if (android_get_device_api_level() >= 29) {
        ASurfaceTransaction_setBufferTransparency(
                reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction),
                reinterpret_cast<ASurfaceControl *>(surfaceControl),
                transparency);
    }
}

void JniBindings_nSetBufferAlpha(
        JNIEnv *env, jclass,
        jlong surfaceTransaction, jlong surfaceControl, jfloat alpha) {
    if (android_get_device_api_level() >= 29) {
        ASurfaceTransaction_setBufferAlpha(
                reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction),
                reinterpret_cast<ASurfaceControl *>(surfaceControl),
                alpha);
    }
}

void JniBindings_nSetCrop(JNIEnv *env, jclass,
                                                    jlong surfaceTransaction,
                                                    jlong surfaceControl,
                                                    jint left,
                                                    jint top,
                                                    jint right,
                                                    jint bottom) {
    auto st = reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction);
    auto sc = reinterpret_cast<ASurfaceControl *>(surfaceControl);

    ARect result = ARect{left, top, right, bottom};

    ASurfaceTransaction_setCrop(st, sc, result);
}

void JniBindings_nSetPosition(JNIEnv *env, jclass,
                                                        jlong surfaceTransaction,
                                                        jlong surfaceControl,
                                                        jfloat x, jfloat y) {
    auto st = reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction);
    auto sc = reinterpret_cast<ASurfaceControl *>(surfaceControl);
    ASurfaceTransaction_setPosition(st, sc, x, y);
}

void JniBindings_nSetScale(JNIEnv *env, jclass,
                                                     jlong surfaceTransaction,
                                                     jlong surfaceControl,
                                                     jfloat scale_x,
                                                     jfloat scale_y) {
    auto st = reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction);
    auto sc = reinterpret_cast<ASurfaceControl *>(surfaceControl);
    ASurfaceTransaction_setScale(st, sc, scale_x, scale_y);
}

void JniBindings_nSetBufferTransform(JNIEnv *env,
                                                               jclass,
                                                               jlong
                                                               surfaceTransaction,
                                                               jlong surfaceControl,
                                                               jint transformation) {
    auto st = reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction);
    auto sc = reinterpret_cast<ASurfaceControl *>(surfaceControl);
    ASurfaceTransaction_setBufferTransform(st, sc, transformation);
}


void JniBindings_nSetGeometry(JNIEnv *env, jclass,
                                                                       jlong surfaceTransaction,
                                                                       jlong surfaceControl,
                                                                       jint bufferWidth,
                                                                       jint bufferHeight,
                                                                       jint dstWidth,
                                                                       jint dstHeight,
                                                                       jint transformation) {
    auto st = reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction);
    auto sc = reinterpret_cast<ASurfaceControl *>(surfaceControl);
    auto src = ARect{0, 0, bufferWidth, bufferHeight};
    auto dest = ARect{0, 0, dstWidth, dstHeight};
    ASurfaceTransaction_setGeometry(st, sc, src, dest, transformation);
}

void loadRectInfo(JNIEnv *env) {
    gRectInfo.clazz = env->FindClass("android/graphics/Rect");

    gRectInfo.left = env->GetFieldID(gRectInfo.clazz, "left", "I");
    gRectInfo.top = env->GetFieldID(gRectInfo.clazz, "top", "I");
    gRectInfo.right = env->GetFieldID(gRectInfo.clazz, "right", "I");
    gRectInfo.bottom = env->GetFieldID(gRectInfo.clazz, "bottom", "I");
}

static const JNINativeMethod JNI_METHOD_TABLE[] = {
        {
                "nCreate",
                "(JLjava/lang/String;)J",
                (void *) JniBindings_nCreate
        },
        {
                "nCreateFromSurface",
                "(Landroid/view/Surface;Ljava/lang/String;)J",
                (void *) JniBindings_nCreateFromSurface
        },
        {
                "nRelease",
                "(J)V",
                (void *) JniBindings_nRelease
        },
        {
                "nTransactionCreate",
                "()J",
                (void *) JniBindings_nTransactionCreate
        },
        {
                "nTransactionDelete",
                "(J)V",
                (void *) JniBindings_nTransactionDelete
        },
        {
                "nTransactionApply",
                "(J)V",
                (void *) JniBindings_nTransactionApply
        },
        {
                "nTransactionReparent",
                "(JJJ)V",
                (void *) JniBindings_nTransactionReparent
        },
        {

                "nTransactionSetOnComplete",
                "(JLandroidx/graphics/surface/SurfaceControlCompat$TransactionCompletedListener;)V",
                (void *) JniBindings_nTransactionSetOnComplete
        },
        {
                "nTransactionSetOnCommit",
                "(JLandroidx/graphics/surface/SurfaceControlCompat$TransactionCommittedListener;)V",
                (void *) JniBindings_nTransactionSetOnCommit
        },
        {
                "nDupFenceFd",
                "(Landroidx/hardware/SyncFenceV19;)I",
                (void *) JniBindings_nDupFenceFd
        },
        {
                "nSetBuffer",
                "(JJLandroid/hardware/HardwareBuffer;Landroidx/hardware/SyncFenceV19;)V",
                (void *) JniBindings_nSetBuffer
        },
        {
                "nSetVisibility",
                "(JJB)V",
                (void *) JniBindings_nSetVisibility
        },
        {
                "nSetZOrder",
                "(JJI)V",
                (void *) JniBindings_nSetZOrder
        },
        {
                "nSetDamageRegion",
                "(JJLandroid/graphics/Rect;)V",
                (void *) JniBindings_nSetDamageRegion
        },
        {
                "nSetDesiredPresentTime",
                "(JJ)V",
                (void *) JniBindings_nSetDesiredPresentTime
        },
        {
                "nSetBufferTransparency",
                "(JJB)V",
                (void *) JniBindings_nSetBufferTransparency
        },
        {
                "nSetBufferAlpha",
                "(JJF)V",
                (void *) JniBindings_nSetBufferAlpha
        },
        {
                "nSetCrop",
                "(JJIIII)V",
                (void *) JniBindings_nSetCrop
        },
        {
                "nSetPosition",
                "(JJFF)V",
                (void *) JniBindings_nSetPosition
        },
        {
                "nSetScale",
                "(JJFF)V",
                (void *) JniBindings_nSetScale
        },
        {
                "nSetBufferTransform",
                "(JJI)V",
                (void *) JniBindings_nSetBufferTransform
        },
        {
                "nSetGeometry",
                "(JJIIIII)V",
                (void *) JniBindings_nSetGeometry
        }
};

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env;
    ALOGE("GraphicsCore JNI_OnLoad start");
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass clazz = env->FindClass("androidx/graphics/surface/JniBindings");
    if(clazz == nullptr) {
        return JNI_ERR;
    }

    if (env->RegisterNatives(clazz, JNI_METHOD_TABLE,
                             sizeof(JNI_METHOD_TABLE) / sizeof(JNINativeMethod)) != JNI_OK) {
        return JNI_ERR;
    }

    loadRectInfo(env);

    if (loadEGLMethods(env) != JNI_OK) {
        return JNI_ERR;
    }

    if (loadSyncFenceMethods(env) != JNI_OK) {
        return JNI_ERR;
    }

    if (loadBufferTransformHintResolverMethods(env) != JNI_OK) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}