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

import androidx.annotation.RestrictTo

/**
 * [RequestListeners] is a Set-like interface that stores a collection of [Request.Listener]s.
 * Listeners are read/set directly using add/remove methods in this interface.
 *
 * Listeners in this class would be added on top of the listeners that client includes in a
 * [Request]. During an active [CameraGraph.Session], changes in [RequestListeners] may not be
 * applied right away. Instead, the change will be applied after [CameraGraph.Session] closes. When
 * there is no active [CameraGraph.Session], the change will be applied without having to wait for
 * the session to close.
 *
 * Note that [RequestListeners] only store values that is a result of methods from this interface.
 * The listeners values that were set from building a request directly will not be reflected here.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RequestListeners {

    /** Store the added [Request.Listener] in the class. */
    public fun add(listener: Request.Listener)

    /** Store the list of added [Request.Listener] in the class. */
    public fun addAll(listeners: List<Request.Listener>)

    /** Remove the given [Request.Listener] from the class. */
    public fun remove(listener: Request.Listener)

    /** Remove the given list of [Request.Listener] from the class. */
    public fun removeAll(listeners: List<Request.Listener>)
}
