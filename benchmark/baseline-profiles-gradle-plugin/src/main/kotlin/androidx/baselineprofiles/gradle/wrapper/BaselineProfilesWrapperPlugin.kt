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

package androidx.baselineprofiles.gradle.wrapper

import androidx.baselineprofiles.gradle.apkprovider.BaselineProfilesApkProviderPlugin
import androidx.baselineprofiles.gradle.consumer.BaselineProfilesConsumerPlugin
import androidx.baselineprofiles.gradle.producer.BaselineProfilesProducerPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The wrapper plugin simplifies the developer workflow detecting which android plugin is applied
 * to the module and applying the correct baseline profile plugin. It uses the following mapping:
 * `com.android.application` can be both [BaselineProfilesConsumerPlugin] and
 * [BaselineProfilesApkProviderPlugin], `com.android.library` can be only
 * [BaselineProfilesConsumerPlugin], `com.android.test` can be only [BaselineProfilesProducerPlugin]
 */
class BaselineProfilesWrapperPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        // If this module is an application module
        project.pluginManager.withPlugin("com.android.application") {

            // Applies profile consumer and apk provider plugins
            project.pluginManager.apply(BaselineProfilesConsumerPlugin::class.java)
            project.pluginManager.apply(BaselineProfilesApkProviderPlugin::class.java)
        }

        // If this module is a library module
        project.pluginManager.withPlugin("com.android.library") {

            // Applies the profile consumer plugin
            project.pluginManager.apply(BaselineProfilesConsumerPlugin::class.java)
        }

        // If this module is a test module
        project.pluginManager.withPlugin("com.android.test") {

            // Applies the profile producer plugin
            project.pluginManager.apply(BaselineProfilesProducerPlugin::class.java)
        }
    }
}
