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
@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.core

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
import android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Trace
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraMetadata

/**
 * Internal debug utilities, constants, and checks.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public object Debug {
    public const val ENABLE_LOGGING: Boolean = true
    public const val ENABLE_TRACING: Boolean = true

    /**
     * Wrap the specified [block] in calls to [Trace.beginSection] (with the supplied [label])
     * and [Trace.endSection].
     *
     * @param label A name of the code section to appear in the trace.
     * @param block A block of code which is being traced.
     */
    public inline fun <T> trace(label: String, crossinline block: () -> T): T {
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
    public inline fun traceStart(crossinline label: () -> String) {
        if (ENABLE_TRACING) {
            Trace.beginSection(label())
        }
    }

    /**
     * Forwarding call to [Trace.endSection] that can be statically disabled at compile time.
     */
    public inline fun traceStop() {
        if (ENABLE_TRACING) {
            Trace.endSection()
        }
    }

    public fun formatCameraGraphProperties(
        metadata: CameraMetadata,
        graphConfig: CameraGraph.Config,
        cameraGraph: CameraGraph
    ): String {
        val lensFacing = when (metadata[LENS_FACING]) {
            CameraCharacteristics.LENS_FACING_FRONT -> "Front"
            CameraCharacteristics.LENS_FACING_BACK -> "Back"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
            else -> "Unknown"
        }

        val operatingMode = when (graphConfig.sessionMode) {
            CameraGraph.OperatingMode.HIGH_SPEED -> "High Speed"
            CameraGraph.OperatingMode.NORMAL -> "Normal"
        }

        val capabilities = metadata[REQUEST_AVAILABLE_CAPABILITIES]
        val cameraType = if (capabilities != null &&
            capabilities.contains(REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
        ) {
            "Logical"
        } else {
            "Physical"
        }

        return StringBuilder().apply {
            append("$cameraGraph (Camera ${graphConfig.camera.value})\n")
            append("  Facing:    $lensFacing ($cameraType)\n")
            append("  Mode:      $operatingMode\n")
            append("Outputs:\n")
            for (stream in cameraGraph.streams.streams) {
                stream.outputs.forEachIndexed { i, output ->
                    append("  ")
                    val streamId = if (i == 0) output.stream.id.toString() else ""
                    append(streamId.padEnd(10, ' '))
                    append(output.id.toString().padEnd(10, ' '))
                    append(output.size.toString().padEnd(12, ' '))
                    append(output.format.name.padEnd(16, ' '))
                    if (output.camera != graphConfig.camera) {
                        append(" [")
                        append(output.camera)
                        append("]")
                    }
                    append("\n")
                }
            }

            if (graphConfig.defaultParameters.isEmpty()) {
                append("Session Parameters: (None)")
            } else {
                append("Session Parameters:\n")
                val captureRequestParameters = graphConfig.sessionParameters.filter {
                    it is CaptureRequest.Key<*>
                }
                for (parameter in captureRequestParameters) {
                    append("  ")
                    append((parameter.key).name.padEnd(50, ' '))
                    append(parameter.value)
                }
            }
        }.toString()
    }
}

/**
 * Asserts that the method was invoked on a specific API version or higher.
 *
 * Example: checkApi(Build.VERSION_CODES.LOLLIPOP, "createCameraDevice")
 */
public inline fun checkApi(requiredApi: Int, methodName: String) {
    check(Build.VERSION.SDK_INT >= requiredApi) {
        "$methodName is not supported on API ${Build.VERSION.SDK_INT} (requires API $requiredApi)"
    }
}

/** Asserts that this method was invoked on Android L (API 21) or higher. */
public inline fun checkLOrHigher(methodName: String): Unit = checkApi(
    Build.VERSION_CODES.LOLLIPOP, methodName
)

/** Asserts that this method was invoked on Android M (API 23) or higher. */
public inline fun checkMOrHigher(methodName: String): Unit = checkApi(
    Build.VERSION_CODES.M, methodName
)

/** Asserts that this method was invoked on Android N (API 24) or higher. */
public inline fun checkNOrHigher(methodName: String): Unit = checkApi(
    Build.VERSION_CODES.N, methodName
)

/** Asserts that this method was invoked on Android O (API 26) or higher. */
public inline fun checkOOrHigher(methodName: String): Unit = checkApi(
    Build.VERSION_CODES.O, methodName
)

/** Asserts that this method was invoked on Android P (API 28) or higher. */
public inline fun checkPOrHigher(methodName: String): Unit = checkApi(
    Build.VERSION_CODES.P, methodName
)

/** Asserts that this method was invoked on Android Q (API 29) or higher. */
public inline fun checkQOrHigher(methodName: String): Unit = checkApi(
    Build.VERSION_CODES.Q, methodName
)
