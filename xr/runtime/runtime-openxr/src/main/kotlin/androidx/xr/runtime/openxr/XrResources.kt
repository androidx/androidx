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

package androidx.xr.runtime.openxr

import android.annotation.SuppressLint
import androidx.xr.runtime.internal.Trackable
import java.util.concurrent.CopyOnWriteArrayList

/** Object that holds resources that are used in the XR session. */
internal class XrResources {
    /** Map of native trackable pointer to [Trackable]. */
    @SuppressLint("BanConcurrentHashMap")
    private val _trackablesMap = java.util.concurrent.ConcurrentHashMap<Long, Trackable>()
    val trackablesMap: Map<Long, Trackable> = _trackablesMap

    /** List of [Updatable]s that are updated every frame. */
    private val _updatables = CopyOnWriteArrayList<Updatable>()
    val updatables: List<Updatable> = _updatables

    /** The data of hands */
    val leftHand: OpenXrHand
    val rightHand: OpenXrHand

    init {
        this.leftHand = OpenXrHand(isLeftHand = true)
        this.rightHand = OpenXrHand(isLeftHand = false)
        _updatables.add(this.leftHand)
        _updatables.add(this.rightHand)
    }

    internal fun addTrackable(trackableId: Long, trackable: Trackable) {
        _trackablesMap[trackableId] = trackable
    }

    internal fun removeTrackable(trackableId: Long) {
        _trackablesMap.remove(trackableId)
    }

    internal fun addUpdatable(updatable: Updatable) {
        _updatables.add(updatable)
    }

    internal fun removeUpdatable(updatable: Updatable) {
        _updatables.remove(updatable)
    }

    internal fun clear() {
        _trackablesMap.clear()
        _updatables.clear()
    }
}
