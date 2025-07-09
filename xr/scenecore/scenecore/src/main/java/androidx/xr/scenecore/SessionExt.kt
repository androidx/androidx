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

@file:JvmName("SessionExt")

package androidx.xr.scenecore

import androidx.xr.runtime.Session
import java.util.Collections
import java.util.WeakHashMap

/**
 * A thread-safe, memory-safe cache to store the Scene for each Session instance.
 *
 * A [WeakHashMap] is used to prevent memory leaks. It allows the garbage collector to remove
 * entries when the [Session] key is no longer in use elsewhere. This is wrapped in a
 * [Collections.synchronizedMap] to ensure thread safety.
 */
private val sceneCache = Collections.synchronizedMap(WeakHashMap<Session, Scene>())

/**
 * Gets the [Scene] associated with this Session.
 *
 * The `Scene` is the primary interface for creating and managing spatial content. There is a single
 * `Scene` instance for each `Session`.
 *
 * @see Scene
 */
public val Session.scene: Scene
    get() =
        sceneCache.getOrPut(this) {
            // This lambda is executed only once per session instance.
            this.sessionConnectors.filterIsInstance<Scene>().single()
        }
