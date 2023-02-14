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

package androidx.baselineprofiles.gradle.producer

import org.gradle.api.Project

/**
 * Allows specifying settings for the Baseline Profiles Plugin.
 */
open class BaselineProfilesProducerExtension {

    companion object {

        private const val EXTENSION_NAME = "baselineProfilesProfileProducer"

        internal fun registerExtension(project: Project): BaselineProfilesProducerExtension {
            val ext = project
                .extensions.findByType(BaselineProfilesProducerExtension::class.java)
            if (ext != null) {
                return ext
            }
            return project
                .extensions.create(EXTENSION_NAME, BaselineProfilesProducerExtension::class.java)
        }
    }

    /**
     * Allows selecting the managed devices to use for generating baseline profiles.
     * This should be a list of strings contained the names of the devices specified in the
     * configuration for managed devices. For example, in the following configuration, the name
     * is `pixel6Api31`.
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
     * Whether baseline profiles should be generated on connected devices.
     */
    var useConnectedDevices: Boolean = true
}
