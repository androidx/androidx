/*
 * Copyright 2025 The Android Open Source Project
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

import android.hardware.camera2.CaptureRequest
import androidx.annotation.RestrictTo

/**
 * [Parameters] is a Map-like interface that stores the key-value parameter pairs from
 * [android.hardware.camera2.CaptureRequest] and [Metadata] for each [CameraGraph]. Parameter are
 * read/set directly using get/set methods in this interface.
 *
 * During an active [CameraGraph.Session], changes in [Parameters] may not be applied right away.
 * Instead, the change will be applied after [CameraGraph.Session] closes. When there is no active
 * [CameraGraph.Session], the change will be applied without having to wait for the session to
 * close. When applying parameter changes, it will overwrite parameter values that were configured
 * when building the request, and overwrite [CameraGraph.Config.defaultParameters]. It will not
 * overwrite [CameraGraph.Config.requiredParameters].
 *
 * Note that [Parameters] only store values that is a result of methods from this interface. The
 * parameter values that were set from implicit template values, or from building a request directly
 * will not be reflected here.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Parameters {
    /** Get the value correspond to the given [android.hardware.camera2.CaptureRequest.Key]. */
    public operator fun <T> get(key: CaptureRequest.Key<T>): T?

    /** Get the value correspond to the given [Metadata.Key]. */
    public operator fun <T> get(key: Metadata.Key<T>): T?

    /** Store the [CaptureRequest] key value pair in the class. */
    public operator fun <T : Any> set(key: CaptureRequest.Key<T>, value: T?)

    /** Store the [Metadata] key value pair in the class. */
    public operator fun <T : Any> set(key: Metadata.Key<T>, value: T?)

    /**
     * Store the key value pairs in the class. The key is either [CaptureRequest.Key] or
     * [Metadata.Key].
     */
    public fun setAll(newParameters: Map<Any, Any?>)

    /** Clear all [CaptureRequest] and [Metadata] parameters stored in the class. */
    public fun clear()

    /**
     * Remove the [CaptureRequest] key value pair associated with the given key. Returns true if a
     * key was present and removed.
     */
    public fun <T> remove(key: CaptureRequest.Key<T>): Boolean

    /**
     * Remove the [Metadata] key value pair associated with the given key. Returns true if a key was
     * present and removed.
     */
    public fun <T> remove(key: Metadata.Key<T>): Boolean

    /**
     * Remove all parameters that match the given keys. The key is either [CaptureRequest.Key] or
     * [Metadata.Key].
     */
    public fun removeAll(keys: Set<*>): Boolean
}
