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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.camera.camera2.pipe.impl

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
import android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Trace
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraMetadata

/**
 * Internal debug utilities, constants, and checks.
 */
object Debug {
    const val ENABLE_LOGGING = true
    const val ENABLE_TRACING = true

    /**
     * Wrap the specified [block] in calls to [Trace.beginSection] (with the supplied [label])
     * and [Trace.endSection].
     *
     * @param label A name of the code section to appear in the trace.
     * @param block A block of code which is being traced.
     */
    inline fun <T> trace(label: String, crossinline block: () -> T): T {
        try {
            traceStart { label }
            return block()
        } finally {
            traceStop()
        }
    }

    /**
     * Forwarding call to [Trace.beginSection] that can be statically disabled at compile time.
     */
    inline fun traceStart(crossinline label: () -> String) {
        if (ENABLE_TRACING) {
            Trace.beginSection(label())
        }
    }

    /**
     * Forwarding call to [Trace.endSection] that can be statically disabled at compile time.
     */
    inline fun traceStop() {
        if (ENABLE_TRACING) {
            Trace.endSection()
        }
    }

    fun logConfiguration(
        graphId: String,
        metadata: CameraMetadata,
        graphConfig: CameraGraph.Config,
        streamMap: StreamMap
    ) {
        Log.info {
            val lensFacing = when (metadata[LENS_FACING]) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                CameraCharacteristics.LENS_FACING_BACK -> "Back"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                else -> "Unknown"
            }

            val operatingMode = when (graphConfig.operatingMode) {
                CameraGraph.OperatingMode.HIGH_SPEED -> "High Speed"
                CameraGraph.OperatingMode.NORMAL -> "Normal"
            }

            val capabilities = metadata[REQUEST_AVAILABLE_CAPABILITIES]
            val cameraType = if (capabilities != null &&
                capabilities.contains(
                        REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
                    )
            ) {
                "Logical"
            } else {
                "Physical"
            }

            StringBuilder().apply {
                append("$graphId (Camera ${graphConfig.camera.value})\n")
                append("  Facing:    $lensFacing ($cameraType)\n")
                append("  Mode:      $operatingMode\n")
                append("Streams:\n")
                for (stream in streamMap.streamConfigMap) {
                    append("  ")
                    append(stream.value.id.toString().padEnd(12, ' '))
                    append(stream.value.size.toString().padEnd(12, ' '))
                    append(stream.value.format.name.padEnd(16, ' '))
                    append(stream.value.type.toString().padEnd(16, ' '))
                    append("\n")
                }

                if (graphConfig.defaultParameters.isEmpty()) {
                    append("Default Parameters: (None)")
                } else {
                    append("Default Parameters:\n")
                    for (
                        parameter in graphConfig.defaultParameters.filter {
                            it is CaptureRequest.Key<*>
                        }
                    ) {
                        append("  ")
                        append((parameter.key as CaptureRequest.Key<*>).name.padEnd(50, ' '))
                        append(parameter.value)
                    }
                }
            }.toString()
        }
    }
}

/**
 * Asserts that the method was invoked on a specific API version or higher.
 *
 * Example: checkApi(Build.VERSION_CODES.LOLLIPOP, "createCameraDevice")
 */
inline fun checkApi(requiredApi: Int, methodName: String) {
    check(Build.VERSION.SDK_INT >= requiredApi) {
        "$methodName is not supported on API ${Build.VERSION.SDK_INT} (requires API $requiredApi)"
    }
}

/** Asserts that this method was invoked on Android L (API 21) or higher. */
inline fun checkLOrHigher(methodName: String) = checkApi(
    Build.VERSION_CODES.LOLLIPOP, methodName
)

/** Asserts that this method was invoked on Android M (API 23) or higher. */
inline fun checkMOrHigher(methodName: String) = checkApi(
    Build.VERSION_CODES.M, methodName
)

/** Asserts that this method was invoked on Android N (API 24) or higher. */
inline fun checkNOrHigher(methodName: String) = checkApi(
    Build.VERSION_CODES.N, methodName
)

/** Asserts that this method was invoked on Android O (API 26) or higher. */
inline fun checkOOrHigher(methodName: String) = checkApi(
    Build.VERSION_CODES.O, methodName
)

/** Asserts that this method was invoked on Android P (API 28) or higher. */
inline fun checkPOrHigher(methodName: String) = checkApi(
    Build.VERSION_CODES.P, methodName
)

/** Asserts that this method was invoked on Android Q (API 29) or higher. */
inline fun checkQOrHigher(methodName: String) = checkApi(
    Build.VERSION_CODES.Q, methodName
)
