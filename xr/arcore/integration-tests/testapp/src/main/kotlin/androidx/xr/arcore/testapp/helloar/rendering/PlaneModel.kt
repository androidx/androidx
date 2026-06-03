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
@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package androidx.xr.arcore.testapp.helloar.rendering

import android.annotation.SuppressLint
import androidx.xr.arcore.Plane
import androidx.xr.arcore.PlaneType
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModelEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

data class PlaneModel(
    val id: Int,
    val planeType: PlaneType,
    val stateFlow: StateFlow<Plane.State>,
    internal val modelEntity: GltfModelEntity,
    internal val renderJob: Job?,
) {
    init {
        @SuppressLint("RestrictedApiAndroidX") modelEntity.setScale(Vector3(0f, 0f, MODEL_DEPTH))
    }

    companion object {
        const val MODEL_DEPTH = .001f
    }
}
