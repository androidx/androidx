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

package androidx.graphics.lowlatency

internal interface SyncFenceImpl {
    /**
     * Waits for a [SyncFenceImpl] to signal for up to the timeout duration
     *
     * @param timeoutNanos time in nanoseconds to wait for before timing out.
     */
    fun await(timeoutNanos: Long): Boolean

    /**
     * Waits forever for a [SyncFenceImpl] to signal
     */
    fun awaitForever(): Boolean

    /**
     * Close the [SyncFenceImpl]
     */
    fun close()
}