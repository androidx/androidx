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

import androidx.annotation.RestrictTo
import kotlin.reflect.KClass

/**
 * An interface for wrapper objects that should not normally be accessed directly.
 *
 * This interface indicates that an object or interface wraps a specific Android object or type and
 * provides a way to retrieve the underlying object directly. Accessing the underlying objects can
 * be useful for compatibility and testing, but is extremely risky if the state or lifetime of the
 * of the object is managed by CameraPipe.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface UnsafeWrapper {
    /**
     * Attempt to unwrap this object into an underlying type.
     *
     * This operation is not safe and should be used with caution as it makes no guarantees about
     * the state of the underlying objects. In particular, implementations should assume that fakes,
     * test wrappers will always return null. Finally this method should return null when unwrapping
     * into the provided type is not supported.
     *
     * @return unwrapped object matching T or null
     */
    public fun <T : Any> unwrapAs(type: KClass<T>): T?
}
