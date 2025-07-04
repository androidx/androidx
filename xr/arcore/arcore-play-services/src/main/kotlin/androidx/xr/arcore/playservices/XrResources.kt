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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.arcore.playservices

import android.annotation.SuppressLint
import androidx.xr.runtime.internal.Trackable
import com.google.ar.core.Trackable as ArCoreTrackable
import java.util.concurrent.ConcurrentHashMap

/**
 * Object that holds shared resources that are used in the ARCore runtime implementation.
 *
 * Currently, the only resource is a map of [ArCoreTrackable] to [Trackable]. This map is used to
 * find the corresponding [Trackable] for a given [ArCoreTrackable].
 */
internal class XrResources() {

    /** Map of [ArCoreTrackable] to [Trackable]. */
    @SuppressLint("BanConcurrentHashMap")
    private val _trackables = ConcurrentHashMap<ArCoreTrackable, Trackable>()
    internal val trackables: Map<ArCoreTrackable, Trackable> = _trackables
    internal val earth: ArCoreEarth = ArCoreEarth(this)

    /**
     * Adds a [Trackable] to the map.
     *
     * This method checks if the map already contains a [Trackable] for the given [ArCoreTrackable].
     * If it does, it does nothing. Otherwise, it adds the [Trackable] to the map.
     *
     * @param arCoreTrackable The [ArCoreTrackable] to add.
     * @param trackable The [Trackable] to add.
     */
    internal fun addTrackable(arCoreTrackable: ArCoreTrackable, trackable: Trackable) {
        _trackables.putIfAbsent(arCoreTrackable, trackable)
    }

    /**
     * Removes a [Trackable] from the map.
     *
     * This method checks if the map contains an [ArCoreTrackable]. If it does, it removes the
     * corresponding [Trackable] from the map. Otherwise, it does nothing.
     *
     * @param arCoreTrackable The [ArCoreTrackable] to remove.
     */
    internal fun removeTrackable(arCoreTrackable: ArCoreTrackable) {
        _trackables.remove(arCoreTrackable)
    }

    /**
     * This method clears the map. It does not perform any additional operations on either the keys
     * or values contained in the map.
     */
    internal fun clear() {
        _trackables.clear()
    }
}
