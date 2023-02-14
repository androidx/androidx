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

package androidx.baselineprofiles.gradle.consumer

import org.gradle.api.Project

/**
 * Allows specifying settings for the Baseline Profiles Plugin.
 */
open class BaselineProfilesConsumerExtension {

    companion object {

        private const val EXTENSION_NAME = "baselineProfilesProfileConsumer"

        internal fun registerExtension(project: Project): BaselineProfilesConsumerExtension {
            val ext = project.extensions.findByType(BaselineProfilesConsumerExtension::class.java)
            if (ext != null) {
                return ext
            }
            return project
                .extensions.create(EXTENSION_NAME, BaselineProfilesConsumerExtension::class.java)
        }
    }

    /**
     * Specifies what build type should be used to generate baseline profiles. By default this build
     * type is `release`. In general, this should be a build type used for distribution. Note that
     * this will be deprecated when b/265438201 is fixed, as all the build types will be used to
     * generate baseline profiles.
     */
    var buildTypeName: String = "release"
}
