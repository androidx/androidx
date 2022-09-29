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

#include <atomic>
#include <cstdint>

#ifndef DATASTORE_SHARED_COUNTER_H
#define DATASTORE_SHARED_COUNTER_H

namespace datastore {
int TruncateFile(int fd);
int CreateSharedCounter(int fd, void** counter_address, bool enable_mlock);
uint32_t GetCounterValue(std::atomic<uint32_t>* counter);
uint32_t IncrementAndGetCounterValue(std::atomic<uint32_t>* counter);
} // namespace datastore

#endif // DATASTORE_SHARED_COUNTER_H