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

package androidx.benchmark.darwin.gradle.xcode

/**
 * A representation of the output of `xcrun simctl runtimes list --json`.
 *
 * That produces an object that contains a [List] of [SimulatorRuntime].
 */
data class SimulatorRuntimes(val runtimes: List<SimulatorRuntime>)

/**
 * An XCode simulator runtime. The serialized representation looks something like:
 * ```json
 * {
 *   "bundlePath" : "...\/Profiles\/Runtimes\/watchOS.simruntime",
 *   "buildversion" : "19S51",
 *   "runtimeRoot" : "....\/Runtimes\/watchOS.simruntime\/Contents\/Resources\/RuntimeRoot",
 *   "identifier" : "com.apple.CoreSimulator.SimRuntime.watchOS-8-3",
 *   "version" : "8.3",
 *    "isAvailable" : true,
 *    "supportedDeviceTypes" : [
 *      ...
 *    ]
 * }
 * ```
 */
data class SimulatorRuntime(
    val identifier: String,
    val version: String,
    val isAvailable: Boolean,
    val supportedDeviceTypes: List<SupportedDeviceType>
)

/**
 * A serialized supported device type has a representation that looks like:
 * ```json
 * {
 *  "bundlePath" : "...\/CoreSimulator\/Profiles\/DeviceTypes\/iPhone 6s.simdevicetype",
 *  "name" : "iPhone 6s",
 *  "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPhone-6s",
 *  "productFamily" : "iPhone"
 *  }
 * ```
 */
data class SupportedDeviceType(
    val bundlePath: String,
    val name: String,
    val identifier: String,
    val productFamily: String
)
