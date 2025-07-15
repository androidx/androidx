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

package androidx.compose.ui.node

// TODO: https://youtrack.jetbrains.com/issue/COMPOSE-1286/Properly-implement-WeakReference-on-Web
internal actual class WeakReference<T : Any> actual constructor(referent: T) {
    private var instance: T? = null

    actual fun clear() {
        instance = null
    }

    actual fun get(): T? = instance
}
