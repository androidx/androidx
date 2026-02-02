/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.baselineprofile.gradle.producer

import org.gradle.api.Incubating
import org.gradle.api.Project

/** Allows specifying settings for the Baseline Profile Producer Plugin. */
open class BaselineProfileProducerExtension {

    companion object {

        private const val EXTENSION_NAME = "baselineProfile"

        internal fun register(project: Project): BaselineProfileProducerExtension {
            val ext = project.extensions.findByType(BaselineProfileProducerExtension::class.java)
            if (ext != null) {
                return ext
            }
            return project.extensions.create(
                EXTENSION_NAME,
                BaselineProfileProducerExtension::class.java
            )
        }
    }

    /**
     * Allows selecting the managed devices to use for generating baseline profiles. This should be
     * a list of strings contained the names of the devices specified in the configuration for
     * managed devices. For example, in the following configuration, the name is `pixel6Api31`.
     *
     * ```
     *  testOptions.managedDevices.devices {
     *      pixel6Api31(ManagedVirtualDevice) {
     *          device = "Pixel 6"
     *          apiLevel = 31
     *          systemImageSource = "aosp"
     *      }
     *  }
     * ```
     */
    var managedDevices = mutableListOf<String>()

    /**
     * Whether baseline profiles should be generated on connected devices. Note that in order to
     * generate a baseline profile, the device is required to be rooted or api level >= 33.
     */
    var useConnectedDevices = true

    /**
     * Whether tests with Macrobenchmark rule should be skipped when running on emulator. Note that
     * when `automaticGenerationDuringBuild` is `true` and managed devices are used benchmark will
     * always run on emulator, causing an exception if this flag is not enabled.
     */
    var skipBenchmarksOnEmulator = true

    /** Enables the emulator display for GMD devices. This is not a stable api. */
    @Incubating var enableEmulatorDisplay = false
}
