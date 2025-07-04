/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.nativeloader

import androidx.annotation.RestrictTo

/** Native code loader for Android. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
actual public object NativeLoader {
    private var loaded = false

    actual public fun load() {
        // Fast bail-out before grabbing a lock if we don't need to.
        if (loaded) return
        loadSynchronous()
    }

    // JVM synchronized to avoid an extra dependency for Kotlin concurrency.
    @SuppressWarnings("BanSynchronizedMethods")
    @Synchronized
    private fun loadSynchronous() {
        // Double-check in the synchronized block in case something got there after first check.
        if (loaded) return
        System.loadLibrary("ink")
        loaded = true
    }
}
