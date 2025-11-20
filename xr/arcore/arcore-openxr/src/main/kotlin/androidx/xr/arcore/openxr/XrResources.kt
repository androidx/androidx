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
@file:Suppress("BanConcurrentHashMap")

package androidx.xr.arcore.openxr

import androidx.xr.arcore.runtime.Trackable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/** Object that holds resources that are used in the XR session. */
internal class XrResources {
    /** Map of native trackable pointer to [androidx.xr.arcore.runtime.Trackable]. */
    private val _trackablesMap = ConcurrentHashMap<Long, Trackable>()
    val trackablesMap: Map<Long, Trackable> = _trackablesMap

    /** List of [Updatable]s that are updated every frame. */
    private val _updatables = CopyOnWriteArrayList<Updatable>()
    val updatables: List<Updatable> = _updatables

    /** The data of eyes */
    val leftEye: OpenXrEye
    val rightEye: OpenXrEye

    /** The data of hands */
    val leftHand: OpenXrHand
    val rightHand: OpenXrHand

    /** The Device tracking data */
    val arDevice: OpenXrDevice

    /** The view camera data */
    val leftRenderViewpoint: OpenXrRenderViewpoint
    val rightRenderViewpoint: OpenXrRenderViewpoint

    /** The data of face */
    val userFace: OpenXrFace

    /** The data of Geospatial */
    val geospatial: OpenXrGeospatial = OpenXrGeospatial(this)

    val leftDepthMap: OpenXrDepthMap
    val rightDepthMap: OpenXrDepthMap

    init {
        this.leftEye = OpenXrEye()
        this.rightEye = OpenXrEye()
        this.leftHand = OpenXrHand(isLeftHand = true)
        this.rightHand = OpenXrHand(isLeftHand = false)
        this.arDevice = OpenXrDevice()
        this.leftRenderViewpoint = OpenXrRenderViewpoint()
        this.rightRenderViewpoint = OpenXrRenderViewpoint()
        this.leftDepthMap = OpenXrDepthMap(/* viewIndex= */ 0)
        this.rightDepthMap = OpenXrDepthMap(/* viewIndex= */ 1)
        this.userFace = OpenXrFace()
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
