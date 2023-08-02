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

package androidx.hardware

import androidx.graphics.utils.JniVisible

/**
 * Helper class of jni bindings to verify dlopen/dlsym behavior to resolve sync_info_file and
 * sync_info_file_free methods
 */
@JniVisible
internal class SyncFenceBindings private constructor() {
    companion object {

        @JvmStatic
        @JniVisible
        external fun nResolveSyncFileInfo(): Boolean

        @JvmStatic
        @JniVisible
        external fun nResolveSyncFileInfoFree(): Boolean

        init {
            System.loadLibrary("graphics-core")
        }
    }
}
