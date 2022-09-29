/*
 * Copyright (C) 2022 The Android Open Source Project
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

#include <errno.h>
#include <sys/mman.h>
#include <unistd.h>

#include <atomic>
#include <cstdint>
#include <functional>

#include "shared_counter.h"

namespace {
constexpr int NUM_BYTES = 4;
} // namespace

// Allocate 4 bytes from mmap to be used as an atomic integer.
static_assert(sizeof(std::atomic<uint32_t>) == NUM_BYTES,
              "Unexpected atomic<uint32_t> size");
// Atomics are safe to use across processes as they are lock free, because atomic operations on
// the same memory location via two different addresses will communicate atomically. See more
// details at http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2427.html#DiscussLockFree
static_assert(std::atomic<uint32_t>::is_always_lock_free == true,
              "atomic<uint32_t> is not always lock free");

namespace datastore {

int TruncateFile(int fd) {
    return (ftruncate(fd, NUM_BYTES) == 0) ? 0 : errno;
}

/*
 * This function returns non-zero errno if fails to create the counter. Caller should have called
 * "TruncateFile" before calling this method. Caller should use "strerror(errno)" to get error
 * message.
 */
int CreateSharedCounter(int fd, void** counter_address, bool enable_mlock) {
    // Map with MAP_SHARED so the memory region is shared with other processes.
    // MAP_LOCKED may cause memory starvation (b/233902124) so is configurable.
    int map_flags = MAP_SHARED;
    // TODO(b/233902124): the impact of MAP_POPULATE is still unclear, experiment
    // with it when possible.
    map_flags |= enable_mlock ? MAP_LOCKED : MAP_POPULATE;

    void* mmap_result = mmap(nullptr, NUM_BYTES, PROT_READ | PROT_WRITE, map_flags, fd, 0);

    if (mmap_result == MAP_FAILED) {
        return errno;
    }
    *counter_address = mmap_result;
    return 0;
}

uint32_t GetCounterValue(std::atomic<uint32_t>* address) {
    auto counter_atomic =
        reinterpret_cast<volatile std::atomic<uint32_t>*>(address);

    // Note: this read will not be protected by a lock, but is safe since the read is atomic.
    return counter_atomic->load();
}

uint32_t IncrementAndGetCounterValue(std::atomic<uint32_t>* address) {
    // Since other processes may change the value, it's also marked volatile.
    auto counter_atomic =
        reinterpret_cast<volatile std::atomic<uint32_t>*>(address);

    // Note: this increment is protected by an exclusive file lock, though the
    // lock isn't required since the counter is atomic.
    return counter_atomic->fetch_add(1) + 1;
}
} // namespace datastore