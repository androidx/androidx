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
#include <android/sync.h>
#include <android/log.h>
#include <cstdint>
#include <poll.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <android/file_descriptor_jni.h>
#include <errno.h>
#include <dlfcn.h>
#include <mutex>

static constexpr int64_t SIGNAL_TIME_INVALID = -1;
static constexpr int64_t SIGNAL_TIME_PENDING = INT64_MAX;

#define SYNC_FENCE "SYNC_FENCE"
#define ALOGE(msg, ...) \
    __android_log_print(ANDROID_LOG_ERROR, SYNC_FENCE, (msg), __VA_ARGS__)
#define ALOGW(msg, ...) \
    __android_log_print(ANDROID_LOG_ERROR, SYNC_FENCE, (msg))

extern "C"
JNIEXPORT void JNICALL
Java_androidx_hardware_SyncFence_nClose(JNIEnv *env, jobject thiz, jint fd) {
    close(fd);
}

/**
 * Flag to ensure that we only attempt to load libsync.so once
 */
static std::once_flag load_sync_lib_flag;

/**
 * Function pointer to the dynamically linked sync_file_info method
 */
static decltype(&sync_file_info) sync_file_info_ptr = nullptr;

/**
 * Function pointer to the dynamically linked sync_file_info_free method
 */
static decltype(&sync_file_info_free) sync_file_info_free_ptr = nullptr;

/**
 * Helper method to attempt to load libsync.so. It only attempts to load the library once and the
 * result is cached. On successful load, this method resolves both sync_file_info and
 * sync_file_info_free methods as well.
 */
static void load_libsync() {
    std::call_once(load_sync_lib_flag, []() {
        void* handle = dlopen("libsync.so", RTLD_NOW);
        if (handle) {
            sync_file_info_ptr =
                    reinterpret_cast<decltype(&sync_file_info)>(
                            dlsym(handle, "sync_file_info"));
            if (!sync_file_info_ptr) {
                ALOGW("Unable to resolve sync_file_info from dlsym");
            }
            sync_file_info_free_ptr =
                    reinterpret_cast<decltype(&sync_file_info_free)>(
                            dlsym(handle, "sync_file_info_free"));
            if (!sync_file_info_free_ptr) {
                ALOGW("Unable to resolve sync_file_info_free from dlsym");
            }
        } else  {
            ALOGW("Unable to load libsync.so");
            sync_file_info_ptr = nullptr;
            sync_file_info_free_ptr = nullptr;
        }
    });
}

/**
 * Obtains a sync_file_info object from the provided file descriptor.
 * Internally this attempts to dynamcially resolve the sync_file_info method from libsync.so
 * @param fd File descriptor to resolve the corresponding sync_file_info from
 * @return sync_file_info or nullptr if the method cannot be resolved or there is no sync_file_info
 * associated with the provided file descriptor
 */
static struct sync_file_info* get_sync_file_info(int fd) {
    load_libsync();
    if (sync_file_info_ptr) {
        return sync_file_info_ptr(fd);
    } else {
        return nullptr;
    }
}

/**
 * Helper method that resolves and invokes sync_file_info_free from libsync.so.
 * This attempts to dynamically resolve the method once and caches the function pointer.
 * If the release method cannot be invoked this method call is a no-op.
 * @param info sync_file_info instance to release
 */
static void release_sync_file_info(struct sync_file_info* info) {
    load_libsync();
    if (sync_file_info_free_ptr) {
        sync_file_info_free_ptr(info);
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_androidx_hardware_SyncFence_nGetSignalTime(JNIEnv *env, jobject thiz,
                                                jint fd) {
    // Implementation sampled from Fence::getSignalTime in the framework
    if (fd == -1) {
        return SIGNAL_TIME_INVALID;
    }

    struct sync_file_info* finfo = get_sync_file_info(fd);
    if (finfo == nullptr) {
        ALOGE("sync_file_info returned NULL for fd %d", fd);
        return SIGNAL_TIME_INVALID;
    }

    if (finfo->status != 1) {
        const auto status = finfo->status;
        if (status < 0) {
            ALOGE("nGetSignalTime: sync_file_info contains an error: <%d> for fd: <%d>", status,
                  fd);
        }
        release_sync_file_info(finfo);
        return status < 0 ? SIGNAL_TIME_INVALID : SIGNAL_TIME_PENDING;
    }

    uint64_t timestamp = 0;
    struct sync_fence_info* pinfo = sync_get_fence_info(finfo);
    for (size_t i = 0; i < finfo->num_fences; i++) {
        if (pinfo[i].timestamp_ns > timestamp) {
            timestamp = pinfo[i].timestamp_ns;
        }
    }

    release_sync_file_info(finfo);
    return static_cast<int64_t>(timestamp);
}

// Implementation of sync_wait obtained from libsync/sync.c in the framework
static int sync_wait(int fd, int timeout)
{
    struct pollfd fds{};
    int ret;

    if (fd < 0) {
        errno = EINVAL;
        return -1;
    }

    fds.fd = fd;
    fds.events = POLLIN;

    do {
        ret = poll(&fds, 1, timeout);
        if (ret > 0) {
            if (fds.revents & (POLLERR | POLLNVAL)) {
                errno = EINVAL;
                return -1;
            }
            return 0;
        } else if (ret == 0) {
            errno = ETIME;
            return -1;
        }
    } while (ret == -1 && (errno == EINTR || errno == EAGAIN));

    return ret;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_androidx_hardware_SyncFence_nWait(JNIEnv *env, jobject thiz, jint fd,
                                       jint timeout_millis) {
    if (fd == -1) {
        return static_cast<jboolean>(true);
    }

    // SyncFence#wait takes a timeout as a long in nanoseconds, however, the poll
    // API appears to consume an int. Also the documentation in Fence.cpp seems to indicate
    // that the timeout is consumed as an int in milliseconds
    int err = sync_wait(fd, timeout_millis);
    return static_cast<jboolean>(err == 0);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_androidx_hardware_SyncFenceBindings_00024Companion_nResolveSyncFileInfo(JNIEnv *env,
                                                                             jobject thiz) {
    load_libsync();
    return sync_file_info_ptr != nullptr;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_androidx_hardware_SyncFenceBindings_00024Companion_nResolveSyncFileInfoFree(JNIEnv *env,
                                                                                 jobject thiz) {
    load_libsync();
    return sync_file_info_free_ptr != nullptr;
}