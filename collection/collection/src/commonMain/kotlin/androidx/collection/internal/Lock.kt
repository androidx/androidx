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

package androidx.collection.internal

/**
 * A simple reentrant lock. On JVM it is implemented via `synchronized {}`, `ReentrantLock` is
 * avoided for performance reasons. On Native it is implemented via POSIX mutex with
 * PTHREAD_MUTEX_RECURSIVE flag.
 */
internal expect class Lock() {

    /**
     * It's not possible to specify a `contract` for an expect function, see
     * https://youtrack.jetbrains.com/issue/KT-29963.
     *
     * Please use [Lock.synchronized] function, where the `contract` is actually specified.
     */
    inline fun <T> synchronizedImpl(block: () -> T): T
}
