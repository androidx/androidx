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
package androidx.xr.scenecore.spatial.core

import android.util.Log
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.SpatialPointerComponent
import androidx.xr.scenecore.runtime.SpatialPointerIcon
import androidx.xr.scenecore.runtime.SpatialPointerIconType
import androidx.xr.scenecore.spatial.core.RuntimeUtils.convertSpatialPointerIconType
import com.android.extensions.xr.XrExtensions

internal class SpatialPointerComponentImpl(private val xrExtensions: XrExtensions) :
    SpatialPointerComponent {
    private var xrEntity: AndroidXrEntity? = null

    override fun onAttach(entity: Entity): Boolean {
        if (xrEntity != null) {
            Log.e(TAG, "Already attached to entity $xrEntity")
            return false
        }
        if (entity !is AndroidXrEntity) {
            Log.e(TAG, "Entity is not an AndroidXrEntity.")
            return false
        }
        xrEntity = entity
        spatialPointerIcon = SpatialPointerIcon.TYPE_DEFAULT
        return true
    }

    override fun onDetach(entity: Entity) {
        spatialPointerIcon = SpatialPointerIcon.TYPE_DEFAULT
        xrEntity = null
    }

    @SpatialPointerIconType
    override var spatialPointerIcon = SpatialPointerIcon.TYPE_DEFAULT
        set(value) {
            field = value

            xrExtensions.createNodeTransaction().use { transaction ->
                transaction
                    .setPointerIcon(xrEntity!!.getNode(), convertSpatialPointerIconType(value))
                    .apply()
            }
        }

    companion object {
        private const val TAG = "Runtime"
    }
}
