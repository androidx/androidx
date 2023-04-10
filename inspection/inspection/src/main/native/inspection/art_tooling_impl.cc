/*
 * Copyright (C) 2019 The Android Open Source Project
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
#include "art_tooling_impl.h"

#include <android/log.h>
#include <functional>
#include <unordered_map>
#include <jvmti.h>
#include <android/api-level.h>

#include "art_tooling_transform.h"
#include "hidden_api_silencer.h"
#include "jvmti_helper.h"
#include "slicer/reader.h"
#include "slicer/writer.h"
#include "jni_wrappers.h"

namespace {

    static std::string ConvertClass(JNIEnv* env, jclass cls) {
        jclass classClass = env->FindClass("java/lang/Class");
        jmethodID mid =
                env->GetMethodID(classClass, "getName", "()Ljava/lang/String;");

        jstring strObj = (jstring)env->CallObjectMethod(cls, mid);

        androidx_inspection::JStringWrapper name_wrapped(env, strObj);

        std::string name = "L" + name_wrapped.get() + ";";

        std::replace(name.begin(), name.end(), '.', '/');
        return name;
    }

}  // namespace

namespace androidx_inspection {

ArtToolingImpl* ArtToolingImpl::create(JNIEnv* env) {
    JavaVM* vm;
    int error = env->GetJavaVM(&vm);
    if (error != 0) {
        LOGE("Failed to get JavaVM instance for ArtToolingImpl with error code: %d",
             error);
        return nullptr;
    }
    // This will attach the current thread to the vm, otherwise
    // CreateJvmtiEnv(vm) below will return JNI_EDETACHED error code.
    GetThreadLocalJNI(vm);
    // Create a stand-alone jvmtiEnv to avoid any callback conflicts
    // with other profilers' agents.
    jvmtiEnv* jvmti = CreateJvmtiEnv(vm);
    if (jvmti == nullptr) {
        LOGE("%s", "Failed to initialize JVMTI env for ArtToolingImpl");
        return nullptr;
    }
    auto service = new ArtToolingImpl(jvmti);

    service->Initialize();
    return service;
}

ArtToolingImpl::ArtToolingImpl(jvmtiEnv* jvmti)
        : jvmti_(jvmti), next_tag_(1) {}

// Used on devices with API level < 29
// Names of HeapIterationCallback and HeapObjectCallback are unfortunately
// similar, but they mirror names in jvmti APIs
static jint JNICALL HeapIterationCallback(jlong class_tag, jlong size, jlong *tag_ptr, jint length,
                                          void *user_data) {
    jlong tag = *(reinterpret_cast<jlong*>(user_data));
    *tag_ptr = tag;
    return 0;
}

bool ArtToolingImpl::tagClassInstancesO(JNIEnv* jni, jclass clazz, jlong tag) {
    jvmtiHeapCallbacks heap_callbacks;
    jint count;
    jclass* classes;

    if (CheckJvmtiError(jvmti_, jvmti_->GetLoadedClasses(&count, &classes))) {
        return true;
    }

    memset(&heap_callbacks, 0, sizeof(heap_callbacks));
    heap_callbacks.heap_iteration_callback =
            reinterpret_cast<decltype(heap_callbacks.heap_iteration_callback)>(
                    HeapIterationCallback);
    // unlike IterateOverInstancesOfClass, that is available only on Q and newer,
    // IterateThroughHeap doesn't include subclasses
    // of the specified class, so have to manually search for subclasses.
    bool error = false;
    for (int i = 0; (i < count) && !error; ++i) {
        if (jni->IsAssignableFrom(classes[i], clazz)) {
            error = CheckJvmtiError(
                    jvmti_,
                    jvmti_->IterateThroughHeap(0, classes[i], &heap_callbacks, &tag));
        }
    }
    jvmti_->Deallocate((unsigned char*)classes);
    return error;
}

// Used on devices with API level >= 29
// Names of HeapIterationCallback and HeapObjectCallback are unfortunately
// similar, but they mirror names in jvmti APIs
static jvmtiIterationControl JNICALL HeapObjectCallback(jlong class_tag, jlong size, jlong* tag_ptr,
                                                        void* user_data) {
    jlong tag = *(reinterpret_cast<jlong *>(user_data));
    *tag_ptr = tag;
    return JVMTI_ITERATION_CONTINUE;
}

bool ArtToolingImpl::tagClassInstancesQ(jclass clazz, jlong tag) {
    return CheckJvmtiError(
            jvmti_, jvmti_->IterateOverInstancesOfClass(
                    clazz, JVMTI_HEAP_OBJECT_EITHER, HeapObjectCallback, &tag));
}

jobjectArray ArtToolingImpl::FindInstances(JNIEnv* jni, jclass clazz) {
    if (jni->IsSameObject(clazz, jni->FindClass("java/lang/Class"))) {
        // Special-case handling for the Class object. Internally, ART creates many
        // fake Class objects that we don't care about. Calling GetLoadedClasses
        // returns us the list of the real Class instances.
        jint count;
        jclass* classes;

        if (CheckJvmtiError(jvmti_, jvmti_->GetLoadedClasses(&count, &classes))) {
            return jni->NewObjectArray(0, clazz, NULL);
        }

        auto result = jni->NewObjectArray(count, clazz, NULL);
        for (int i = 0; i < count; ++i) {
            jni->SetObjectArrayElement(result, i, (jobject)classes[i]);
        }
        jvmti_->Deallocate((unsigned char*)classes);

        return result;
    }

    jlong tag = next_tag_++;

    bool error = android_get_device_api_level() < __ANDROID_API_Q__
                 ? tagClassInstancesO(jni, clazz, tag)
                 : tagClassInstancesQ(clazz, tag);

    if (error) {
        return jni->NewObjectArray(0, clazz, NULL);
    }

    jint count;
    jobject* instances;
    if (CheckJvmtiError(jvmti_, jvmti_->GetObjectsWithTags(1, &tag, &count,
                                                           &instances, NULL))) {
        return jni->NewObjectArray(0, clazz, NULL);
    }

    auto result = jni->NewObjectArray(count, clazz, NULL);
    for (int i = 0; i < count; ++i) {
        jni->SetObjectArrayElement(result, i, instances[i]);
    }
    jvmti_->Deallocate((unsigned char*)instances);

    return result;
}

class JvmtiAllocator : public dex::Writer::Allocator {
public:
    JvmtiAllocator(jvmtiEnv* jvmti_env) : jvmti_env_(jvmti_env) {}

    virtual void* Allocate(size_t size) {
        return ::androidx_inspection::Allocate(jvmti_env_, size);
    }

    virtual void Free(void* ptr) { ::androidx_inspection::Deallocate(jvmti_env_, ptr); }

private:
    jvmtiEnv* jvmti_env_;
};

std::unordered_map<std::string, ArtToolingTransform*>*
GetAppInspectionTransforms() {
    static auto* transformations =
            new std::unordered_map<std::string, ArtToolingTransform*>();
    return transformations;
}

void ArtToolingImpl::OnClassFileLoaded(
        jvmtiEnv* jvmti_env, JNIEnv* jni_env, jclass class_being_redefined,
        jobject loader, const char* name, jobject protection_domain,
        jint class_data_len, const unsigned char* class_data,
        jint* new_class_data_len, unsigned char** new_class_data) {
    // The tooling interface will specify class names like "java/net/URL"
    // however, in .dex these classes are stored using the "Ljava/net/URL;"
    // format.
    std::string desc = "L" + std::string(name) + ";";
    auto class_transforms = GetAppInspectionTransforms();
    auto transform = class_transforms->find(desc);
    if (transform == class_transforms->end()) return;

    dex::Reader reader(class_data, class_data_len);
    auto class_index = reader.FindClassIndex(desc.c_str());
    if (class_index == dex::kNoIndex) {
        LOGV("Could not find class index for %s", name);
        return;
    }

    reader.CreateClassIr(class_index);
    auto dex_ir = reader.GetIr();
    transform->second->Apply(dex_ir);

    size_t new_image_size = 0;
    dex::u1* new_image = nullptr;
    dex::Writer writer(dex_ir);

    JvmtiAllocator allocator(jvmti_env);
    new_image = writer.CreateImage(&allocator, &new_image_size);

    *new_class_data_len = new_image_size;
    *new_class_data = new_image;
}

void ArtToolingImpl::Initialize() {
    SetAllCapabilities(jvmti_);

    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassFileLoadHook = OnClassFileLoaded;

    CheckJvmtiError(jvmti_,
                    jvmti_->SetEventCallbacks(&callbacks, sizeof(callbacks)));

    // Before P ClassFileLoadHook has significant performance overhead so we
    // only enable the hook during retransformation (on agent attach and class
    // prepare). For P+ we want to keep the hook events always on to support
    // multiple retransforming agents (and therefore don't need to perform
    // retransformation on class prepare).
    bool filter_class_load_hook = android_get_device_api_level() < __ANDROID_API_P__;
    SetEventNotification(jvmti_,
                         filter_class_load_hook ? JVMTI_DISABLE : JVMTI_ENABLE,
                         JVMTI_EVENT_CLASS_FILE_LOAD_HOOK);
}

void ArtToolingImpl::AddTransform(JNIEnv* jni, const jclass& origin_class,
                                  const std::string& method_name,
                                  const std::string& signature,
                                  bool is_entry) {
    HiddenApiSilencer silencer(jvmti_);
    auto class_transforms = GetAppInspectionTransforms();
    std::string class_name = ConvertClass(jni, origin_class);
    auto transform_iter = class_transforms->find(class_name);
    ArtToolingTransform* app_transform;
    if (transform_iter == class_transforms->end()) {
        app_transform = new ArtToolingTransform(class_name.c_str());
        class_transforms->insert({class_name, app_transform});
    } else {
        app_transform = transform_iter->second;
    }
    app_transform->AddTransform(class_name.c_str(), method_name.c_str(),
                                signature.c_str(), is_entry);

    jthread thread = nullptr;
    jvmti_->GetCurrentThread(&thread);

    // Class file load hooks are automatically managed on P (and newer) devices
    bool manually_toggle_load_hook = android_get_device_api_level() < __ANDROID_API_P__;

    if (manually_toggle_load_hook) {
        CheckJvmtiError(
                jvmti_, jvmti_->SetEventNotificationMode(
                        JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, thread));
    }
    CheckJvmtiError(jvmti_, jvmti_->RetransformClasses(1, &origin_class));
    if (manually_toggle_load_hook) {
        CheckJvmtiError(
                jvmti_, jvmti_->SetEventNotificationMode(
                        JVMTI_DISABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, thread));
    }

    if (thread != nullptr) {
        jni->DeleteLocalRef(thread);
    }
}

}  // namespace androidx_inspection
