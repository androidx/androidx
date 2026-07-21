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

@file:JvmName("UnsafeWrappers")

package androidx.camera.common

import java.lang.Class

/**
 * An interface for wrapper objects that hide underlying platform or implementation-specific types.
 *
 * Classes implementing this interface wrap an underlying object (typically an Android platform
 * type) and provide a mechanism to retrieve that object directly.
 *
 * ### Usage Warning
 * Direct access to the underlying object bypasses the wrapper's management and abstraction. This is
 * **unsafe** and should be done with caution:
 * * **Lifecycle Management:** The library may manage the lifetime of the underlying object (e.g.,
 *   closing or recycling it). Interacting with the unwrapped object outside the wrapper's control
 *   can lead to undefined behavior or crashes.
 * * **State Consistency:** Modifying the state of the underlying object directly may cause the
 *   wrapper to become out of sync or behave unpredictably.
 * * **Test Compatibility:** Test doubles (fakes, mocks) of the wrapper will typically return `null`
 *   when unwrapped. Code that relies on successful unwrapping may be harder to test.
 *
 * Use this interface only when interoperability with other APIs requires the raw platform type, or
 * as a temporary workaround for missing functionality in the wrapper API.
 */
public interface UnsafeWrapper {
    /**
     * Attempts to unwrap this object into the specified underlying [type].
     *
     * Callers should use this method to retrieve the wrapped platform object.
     *
     * ### Implementation Notes
     * * If the requested [type] is not supported by this wrapper, this method must return `null`.
     * * Implementations designed for testing (e.g., fakes, mocks, or no-op wrappers) should return
     *   `null` to signal that no real underlying object is available.
     *
     * @param type The [Class] representing the expected type of the underlying object.
     * @param T The expected type of the underlying object.
     * @return The underlying object cast to [T], or `null` if the object cannot be unwrapped into
     *   the requested [type].
     */
    public fun <T : Any> unwrapAs(type: Class<T>): T?
}

/**
 * Inline extension function to attempt to unwrap this object into the reified type [T].
 *
 * This is a Kotlin-friendly helper for [UnsafeWrapper.unwrapAs].
 *
 * @param T The expected type of the underlying object.
 * @return The underlying object cast to [T], or `null` if the object cannot be unwrapped.
 * @see UnsafeWrapper.unwrapAs
 */
@kotlin.jvm.JvmSynthetic
public inline fun <reified T : Any> UnsafeWrapper.unwrapAs(): T? = unwrapAs(T::class.java)
