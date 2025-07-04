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

package androidx.xr.scenecore.testapp.common

import android.annotation.SuppressLint
import androidx.xr.runtime.Session
import androidx.xr.scenecore.SpatialCapabilities
import androidx.xr.scenecore.scene
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class SpatialEventLog(val timestamp: String, val eventName: String, val eventLog: String)

enum class SpatialMode {
    FSM,
    HSM,
}

@SuppressLint("SetTextI18n")
enum class EventType(val text: String) {
    MODE_CHANGED_TO_HSM("Switched to HSM"),
    MODE_CHANGED_TO_FSM("Switched to FSM"),
    LOG_CAPABILITIES_CLICKED("Log Capabilities Clicked"),
    CAPABILITIES_CHANGED("Capabilities Changed"),
    BOUNDS_CHANGED("Bounds Changed"),
    OPACITY_CHANGED("Opacity Changed"),
    PASSTHROUGH_CHANGED("Passthrough Changed"),
    SKYBOX_CHANGED("Skybox Changed"),
    GEOMETRY_CHANGED("Geometry Changed"),
    SKYBOX_AND_GEOMETRY_CHANGED("Skybox and geometry Changed"),
}

@SuppressLint("SetTextI18n", "RestrictedApi")
fun logCapabilities(session: Session): String {
    val capsStr: StringBuilder = StringBuilder()
    val caps = session.scene.spatialCapabilities
    capsStr.append(capToStr("UI", caps, SpatialCapabilities.SPATIAL_CAPABILITY_UI))
    capsStr.append(capToStr("3D", caps, SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT))
    capsStr.append(capToStr("PT", caps, SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
    capsStr.append(capToStr("Env", caps, SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
    capsStr.append(capToStr("Audio", caps, SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
    capsStr.append(
        capToStr("Embed Activity", caps, SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)
    )
    return capsStr.toString()
}

@SuppressLint("SetTextI18n", "RestrictedApi")
fun capToStr(name: String, spatialCapabilities: SpatialCapabilities, capability: Int): String {
    val status: String = if (spatialCapabilities.hasCapability(capability)) "Y" else "N"
    return "${name}: $status \t"
}

@SuppressLint("SetTextI18n")
fun currentTimestamp(): String {
    return SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Calendar.getInstance().time)
}

data class Item(var name: String, var value: String)
