/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "PathIterator.h"

#include <jni.h>

#include <sys/system_properties.h>

#include <mutex>

#define JNI_CLASS_NAME "androidx/graphics/path/PathIteratorPreApi34Impl"
#define JNI_CLASS_NAME_CONVERTER "androidx/graphics/path/ConicConverter"

#if !defined(NDEBUG)
#include <android/log.h>
#define ANDROID_LOG_TAG "PathIterator"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, ANDROID_LOG_TAG, __VA_ARGS__)
#endif

struct {
    jclass jniClass;
    jfieldID nativePath;
} sPath{};

uint32_t sApiLevel = 0;
std::once_flag sApiLevelOnceFlag;

static uint32_t api_level() {
    std::call_once(sApiLevelOnceFlag, []() {
        char sdkVersion[PROP_VALUE_MAX];
        __system_property_get("ro.build.version.sdk", sdkVersion);
        sApiLevel = atoi(sdkVersion); // NOLINT(cert-err34-c)
    });
    return sApiLevel;
}

static jlong createPathIterator(JNIEnv* env, jobject,
        jobject path_, jint conicEvaluation_, jfloat tolerance_) {

    auto nativePath = static_cast<intptr_t>(env->GetLongField(path_, sPath.nativePath));
    auto* path = reinterpret_cast<Path*>(nativePath);

    Point* points;
    Verb* verbs;
    float* conicWeights;
    int count;
    PathIterator::VerbDirection direction;

    const uint32_t apiLevel = api_level();
    if (apiLevel >= 30) {
        auto* ref = reinterpret_cast<PathRef30*>(path->pathRef);
        points = ref->points;
        verbs = ref->verbs;
        conicWeights = ref->conicWeights;
        count = ref->verbCount;
        direction = PathIterator::VerbDirection::Forward;
    } else if (apiLevel >= 26) {
        auto* ref = reinterpret_cast<PathRef26*>(path->pathRef);
        points = ref->points;
        verbs = ref->verbs;
        conicWeights = ref->conicWeights;
        count = ref->verbCount;
        direction = PathIterator::VerbDirection::Backward;
    } else if (apiLevel >= 24) {
        auto* ref = reinterpret_cast<PathRef24*>(path->pathRef);
        points = ref->points;
        verbs = ref->verbs;
        conicWeights = ref->conicWeights;
        count = ref->verbCount;
        direction = PathIterator::VerbDirection::Backward;
    } else {
        auto* ref = path->pathRef;
        points = ref->points;
        verbs = ref->verbs;
        conicWeights = ref->conicWeights;
        count = ref->verbCount;
        direction = PathIterator::VerbDirection::Backward;
    }

    return jlong(new PathIterator(points, verbs, conicWeights, count, direction));
}

static void destroyPathIterator(JNIEnv*, jobject, jlong pathIterator_) {
    delete reinterpret_cast<PathIterator*>(pathIterator_);
}

static jboolean pathIteratorHasNext(JNIEnv*, jobject, jlong pathIterator_) {
    return reinterpret_cast<PathIterator*>(pathIterator_)->hasNext();
}

static jint conicToQuadraticsWrapper(JNIEnv* env, jobject,
                                      jfloatArray conicPoints, jfloatArray quadraticPoints,
                                      jfloat weight, jfloat tolerance, jint offset) {
    float *conicData1 = env->GetFloatArrayElements(conicPoints, JNI_FALSE);
    float *quadData1 = env->GetFloatArrayElements(quadraticPoints, JNI_FALSE);
    int quadDataSize = env->GetArrayLength(quadraticPoints);

    int count = conicToQuadratics(reinterpret_cast<Point *>(conicData1 + offset),
                                  reinterpret_cast<Point *>(quadData1),
                                  env->GetArrayLength(quadraticPoints),
                                  weight, tolerance);

    env->ReleaseFloatArrayElements(conicPoints, conicData1, 0);
    env->ReleaseFloatArrayElements(quadraticPoints, quadData1, 0);

    return count;
}

static jint pathIteratorNext(JNIEnv* env, jobject,
                             jlong pathIterator_, jfloatArray points_, jint offset_) {
    auto pathIterator = reinterpret_cast<PathIterator*>(pathIterator_);
    Point pointsData[4];
    Verb verb = pathIterator->next(pointsData);

    if (verb != Verb::Done && verb != Verb::Close) {
        auto* floatsData = reinterpret_cast<jfloat*>(pointsData);
        env->SetFloatArrayRegion(points_, offset_, 8, floatsData);
    }

    return static_cast<jint>(verb);
}

static jint pathIteratorPeek(JNIEnv*, jobject, jlong pathIterator_) {
    return static_cast<jint>(reinterpret_cast<PathIterator *>(pathIterator_)->peek());
}

static jint pathIteratorRawSize(JNIEnv*, jobject, jlong pathIterator_) {
    return static_cast<jint>(reinterpret_cast<PathIterator *>(pathIterator_)->rawCount());
}

static jint pathIteratorSize(JNIEnv*, jobject, jlong pathIterator_) {
    return static_cast<jint>(reinterpret_cast<PathIterator *>(pathIterator_)->count());
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    sPath.jniClass = env->FindClass("android/graphics/Path");
    if (sPath.jniClass == nullptr) return JNI_ERR;

    sPath.nativePath = env->GetFieldID(sPath.jniClass, "mNativePath", "J");
    if (sPath.nativePath == nullptr) return JNI_ERR;

    {
        jclass pathsClass = env->FindClass(JNI_CLASS_NAME);
        if (pathsClass == nullptr) return JNI_ERR;

        static const JNINativeMethod methods[] = {
                {
                    (char*) "createInternalPathIterator",
                    (char*) "(Landroid/graphics/Path;IF)J",
                    reinterpret_cast<void*>(createPathIterator)
                },
                {
                    (char*) "destroyInternalPathIterator",
                    (char*) "(J)V",
                    reinterpret_cast<void*>(destroyPathIterator)
                },
                {
                    (char*) "internalPathIteratorHasNext",
                    (char*) "(J)Z",
                    reinterpret_cast<void*>(pathIteratorHasNext)
                },
                {
                    (char*) "internalPathIteratorNext",
                    (char*) "(J[FI)I",
                    reinterpret_cast<void*>(pathIteratorNext)
                },
                {
                    (char*) "internalPathIteratorPeek",
                    (char*) "(J)I",
                    reinterpret_cast<void*>(pathIteratorPeek)
                },
                {
                    (char*) "internalPathIteratorRawSize",
                    (char*) "(J)I",
                    reinterpret_cast<void*>(pathIteratorRawSize)
                },
                {
                    (char*) "internalPathIteratorSize",
                    (char*) "(J)I",
                    reinterpret_cast<void*>(pathIteratorSize)
                },
        };

        int result = env->RegisterNatives(
                pathsClass, methods, sizeof(methods) / sizeof(JNINativeMethod)
        );
        if (result != JNI_OK) return result;

        env->DeleteLocalRef(pathsClass);

        jclass converterClass = env->FindClass(JNI_CLASS_NAME_CONVERTER);
        if (converterClass == nullptr) return JNI_ERR;
        static const JNINativeMethod methods2[] = {
                {
                    (char *) "internalConicToQuadratics",
                    (char *) "([F[FFFI)I",
                    reinterpret_cast<void *>(conicToQuadraticsWrapper)
                },
        };

        result = env->RegisterNatives(
                converterClass, methods2, sizeof(methods2) / sizeof(JNINativeMethod)
        );
        if (result != JNI_OK) return result;

        env->DeleteLocalRef(converterClass);
    }

    return JNI_VERSION_1_6;
}
