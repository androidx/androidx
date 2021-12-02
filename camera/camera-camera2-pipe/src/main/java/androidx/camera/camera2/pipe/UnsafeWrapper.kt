/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe

import androidx.annotation.RequiresApi

/**
 * This interface indicates that an object or interface wraps a specific Android object or type and
 * provides a way to retrieve the underlying object directly. Accessing the underlying objects can
 * be useful for compatibility and testing, but it is extremely risky if the lifetime of the object
 * is managed by Camera Pipe and the wrapped object is closed, released, or altered.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface UnsafeWrapper<T> {
    public fun unwrap(): T?
}
