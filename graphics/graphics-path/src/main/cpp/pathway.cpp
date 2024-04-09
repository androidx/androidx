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

#include <android/api-level.h>

#include <cstdlib>
#include <new>

#define JNI_CLASS_NAME "androidx/graphics/path/PathIteratorPreApi34Impl"
#define JNI_CLASS_NAME_CONVERTER "androidx/graphics/path/ConicConverter"

struct {
    jclass jniClass;
    jfieldID nativePath;
} sPath{};

static jlong createPathIterator(JNIEnv* env, jobject,
        jobject path_, jint conicEvaluation_, jfloat tolerance_) {

    auto nativePath = static_cast<intptr_t>(env->GetLongField(path_, sPath.nativePath));
    auto* path = reinterpret_cast<Path*>(nativePath);

    Point* points;
    Verb* verbs;
    float* conicWeights;
    int count;
    PathIterator::VerbDirection direction;

    const uint32_t apiLevel = android_get_device_api_level();
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

    PathIterator* iterator = static_cast<PathIterator*>(malloc(sizeof(PathIterator)));
    return jlong(new(iterator) PathIterator(
            points, verbs, conicWeights, count, direction,
            PathIterator::ConicEvaluation(conicEvaluation_), tolerance_
    ));
}

static void destroyPathIterator(JNIEnv*, jobject, jlong pathIterator_) {
    PathIterator* iterator = reinterpret_cast<PathIterator*>(pathIterator_);
    iterator->~PathIterator();
    free(iterator);
}

static jboolean pathIteratorHasNext(JNIEnv*, jobject, jlong pathIterator_) {
    return reinterpret_cast<PathIterator*>(pathIterator_)->hasNext();
}

static jint conicToQuadraticsWrapper(JNIEnv* env, jobject,
                                      jfloatArray conicPoints, jint offset,
                                      jfloatArray quadraticPoints,
                                      jfloat weight, jfloat tolerance) {
    float *conicData = env->GetFloatArrayElements(conicPoints, JNI_FALSE);
    float *quadData = env->GetFloatArrayElements(quadraticPoints, JNI_FALSE);

    int count = conicToQuadratics(reinterpret_cast<const Point*>(conicData + offset),
                                  reinterpret_cast<Point*>(quadData),
                                  env->GetArrayLength(quadraticPoints),
                                  weight, tolerance);

    env->ReleaseFloatArrayElements(conicPoints, conicData, JNI_ABORT);
    env->ReleaseFloatArrayElements(quadraticPoints, quadData, 0);

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

        jint result;

        const uint32_t apiLevel = android_get_device_api_level();
        if (apiLevel >= 26) { // Android 8.0
            static const JNINativeMethod methods[] = {
                    {
                            (char *) "createInternalPathIterator",
                            (char *) "(Landroid/graphics/Path;IF)J",
                            reinterpret_cast<void *>(createPathIterator)
                    },
                    {
                            (char *) "destroyInternalPathIterator",
                            (char *) "(J)V",
                            reinterpret_cast<void *>(destroyPathIterator)
                    },
                    {
                            (char *) "internalPathIteratorHasNext",
                            (char *) "(J)Z",
                            reinterpret_cast<void *>(pathIteratorHasNext)
                    },
                    {
                            (char *) "internalPathIteratorNext",
                            (char *) "(J[FI)I",
                            reinterpret_cast<void *>(pathIteratorNext)
                    },
                    {
                            (char *) "internalPathIteratorPeek",
                            (char *) "(J)I",
                            reinterpret_cast<void *>(pathIteratorPeek)
                    },
                    {
                            (char *) "internalPathIteratorRawSize",
                            (char *) "(J)I",
                            reinterpret_cast<void *>(pathIteratorRawSize)
                    },
                    {
                            (char *) "internalPathIteratorSize",
                            (char *) "(J)I",
                            reinterpret_cast<void *>(pathIteratorSize)
                    },
            };

            result = env->RegisterNatives(
                    pathsClass, methods, sizeof(methods) / sizeof(JNINativeMethod)
            );
        } else {
            // Before Android 8, rely on the !bang JNI notation to speed up our JNI calls
            static const JNINativeMethod methods[] = {
                    {
                            (char *) "createInternalPathIterator",
                            (char *) "(Landroid/graphics/Path;IF)J",
                            reinterpret_cast<void *>(createPathIterator)
                    },
                    {
                            (char *) "destroyInternalPathIterator",
                            (char *) "(J)V",
                            reinterpret_cast<void *>(destroyPathIterator)
                    },
                    {
                            (char *) "internalPathIteratorHasNext",
                            (char *) "!(J)Z",
                            reinterpret_cast<void *>(pathIteratorHasNext)
                    },
                    {
                            (char *) "internalPathIteratorNext",
                            (char *) "!(J[FI)I",
                            reinterpret_cast<void *>(pathIteratorNext)
                    },
                    {
                            (char *) "internalPathIteratorPeek",
                            (char *) "!(J)I",
                            reinterpret_cast<void *>(pathIteratorPeek)
                    },
                    {
                            (char *) "internalPathIteratorRawSize",
                            (char *) "!(J)I",
                            reinterpret_cast<void *>(pathIteratorRawSize)
                    },
                    {
                            (char *) "internalPathIteratorSize",
                            (char *) "!(J)I",
                            reinterpret_cast<void *>(pathIteratorSize)
                    },
            };

            result = env->RegisterNatives(
                    pathsClass, methods, sizeof(methods) / sizeof(JNINativeMethod)
            );
        }
        if (result != JNI_OK) return result;

        env->DeleteLocalRef(pathsClass);

        jclass converterClass = env->FindClass(JNI_CLASS_NAME_CONVERTER);
        if (converterClass == nullptr) return JNI_ERR;
        static const JNINativeMethod methods2[] = {
            {
                (char*) "internalConicToQuadratics",
                (char*) "([FI[FFF)I",
                reinterpret_cast<void*>(conicToQuadraticsWrapper)
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
