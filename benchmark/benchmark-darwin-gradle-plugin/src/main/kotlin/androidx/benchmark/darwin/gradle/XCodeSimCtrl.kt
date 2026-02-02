/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.benchmark.darwin.gradle

import androidx.benchmark.darwin.gradle.xcode.GsonHelpers
import androidx.benchmark.darwin.gradle.xcode.SimulatorRuntimes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.gradle.process.ExecOperations

/** Controls XCode Simulator instances. */
class XCodeSimCtrl(private val execOperations: ExecOperations, private val destination: String) {

    private var destinationDesc: String? = null
    private var deviceId: String? = null

    // A device type looks something like
    // platform=iOS Simulator,name=iPhone 13,OS=15.2

    fun start(block: (destinationDesc: String) -> Unit) {
        try {
            val instance = boot(destination, execOperations)
            destinationDesc = instance.destinationDesc
            deviceId = instance.deviceId
            block(destinationDesc!!)
        } finally {
            val id = deviceId
            if (id != null) {
                shutDownAndDelete(execOperations, id)
            }
        }
    }

    companion object {
        private const val PLATFORM_KEY = "platform"
        private const val NAME_KEY = "name"
        private const val RUNTIME_KEY = "OS"
        private const val IOS_SIMULATOR = "iOS Simulator"
        private const val IPHONE_PRODUCT_FAMILY = "iPhone"

        /** Simulator metadata */
        internal data class SimulatorInstance(
            /* The full device descriptor. */
            val destinationDesc: String,
            /* The unique device UUID if we end up booting the device. */
            val deviceId: String? = null
        )

        internal fun boot(destination: String, execOperations: ExecOperations): SimulatorInstance {
            val parsed = parse(destination)
            return when (platform(parsed)) {
                // Simulator needs to be booted up.
                IOS_SIMULATOR -> bootSimulator(destination, parsed, execOperations)
                // For other destinations, we don't have much to do.
                else -> SimulatorInstance(destinationDesc = destination)
            }
        }

        private fun discoverSimulatorRuntimeVersion(execOperations: ExecOperations): String? {
            val json =
                executeCommand(
                    execOperations,
                    listOf("xcrun", "simctl", "list", "runtimes", "--json")
                )
            val simulatorRuntimes = GsonHelpers.gson().fromJson(json, SimulatorRuntimes::class.java)
            // There is usually one version of the simulator runtime available per xcode version
            val supported =
                simulatorRuntimes.runtimes.firstOrNull { runtime ->
                    runtime.isAvailable &&
                        runtime.supportedDeviceTypes.any { deviceType ->
                            deviceType.productFamily == IPHONE_PRODUCT_FAMILY
                        }
                }
            return supported?.version
        }

        private fun bootSimulator(
            destination: String,
            parsed: Map<String, String>,
            execOperations: ExecOperations
        ): SimulatorInstance {
            val deviceName = deviceName(parsed)
            val supported = discoverSimulatorRuntimeVersion(execOperations)
            // While this is not strictly correct, these versions should be pretty close.
            val runtimeVersion = supported ?: runtimeVersion(parsed)
            check(deviceName != null && runtimeVersion != null) {
                "Invalid destination spec: $destination"
            }
            val deviceId =
                executeCommand(
                    execOperations,
                    listOf(
                        "xcrun",
                        "simctl",
                        "create",
                        deviceName, // Use the deviceName as the name
                        deviceName,
                        "iOS$runtimeVersion"
                    )
                )
            check(deviceId.isNotBlank()) {
                "Invalid device id for simulator: $deviceId (Destination: $destination)"
            }
            executeCommand(execOperations, listOf("xcrun", "simctl", "boot", deviceId))
            // Return a simulator instance with the new descriptor + device id
            return SimulatorInstance(destinationDesc = "id=$deviceId", deviceId = deviceId)
        }

        internal fun shutDownAndDelete(execOperations: ExecOperations, deviceId: String) {
            // Cleans up the instance of the simulator that was booted up.
            executeCommand(execOperations, listOf("xcrun", "simctl", "shutdown", deviceId))
            executeCommand(execOperations, listOf("xcrun", "simctl", "delete", deviceId))
        }

        private fun executeCommand(execOperations: ExecOperations, args: List<String>): String {
            val output = ByteArrayOutputStream()
            output.use {
                execOperations.exec { spec ->
                    spec.commandLine = args
                    spec.standardOutput = output
                }
                val input = ByteArrayInputStream(output.toByteArray())
                return input.use {
                    // Trimming is important here, otherwise ExecOperations encodes the string
                    // with shell specific escape sequences which mangle the device
                    input.reader().readText().trim()
                }
            }
        }

        private fun platform(parsed: Map<String, String>): String? {
            return parsed[PLATFORM_KEY]
        }

        private fun deviceName(parsed: Map<String, String>): String? {
            return parsed[NAME_KEY]
        }

        private fun runtimeVersion(parsed: Map<String, String>): String? {
            return parsed[RUNTIME_KEY]
        }

        private fun parse(destination: String): Map<String, String> {
            return destination
                .splitToSequence(",")
                .map { split ->
                    check(split.contains("=")) { "Invalid destination spec: $destination" }
                    val (key, value) = split.split("=", limit = 2)
                    key.trim() to value.trim()
                }
                .toMap()
        }
    }
}
