/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.lifecycle.viewmodel.internal

/**
 * Provides a custom multiplatform locking mechanism for controlling access to a shared resource by
 * multiple threads.
 *
 * The implementation depends on the platform:
 * - On JVM/ART: uses JDK's synchronization.
 * - On Native: uses posix.
 */
internal expect class Lock() {

    /**
     * Executes the given function [block] while holding the monitor of the current [Lock].
     */
    inline fun <T> withLock(crossinline block: () -> T): T
}
