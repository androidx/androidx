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

package androidx.baselineprofile.gradle.wrapper

import androidx.baselineprofile.gradle.apptarget.BaselineProfileAppTargetPlugin
import androidx.baselineprofile.gradle.consumer.BaselineProfileConsumerPlugin
import androidx.baselineprofile.gradle.producer.BaselineProfileProducerPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The wrapper plugin simplifies the developer workflow detecting which android plugin is applied to
 * the module and applying the correct baseline profile plugin. It uses the following mapping:
 * `com.android.application` can be both [BaselineProfileConsumerPlugin] and
 * [BaselineProfileAppTargetPlugin], `com.android.library` can be only
 * [BaselineProfileConsumerPlugin], `com.android.test` can be only [BaselineProfileProducerPlugin]
 */
class BaselineProfileWrapperPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        // If this module is an application module
        project.pluginManager.withPlugin("com.android.application") {

            // Applies profile consumer and app target plugins
            project.pluginManager.apply(BaselineProfileAppTargetPlugin::class.java)
            project.pluginManager.apply(BaselineProfileConsumerPlugin::class.java)
        }

        // If this module is a library module
        project.pluginManager.withPlugin("com.android.library") {

            // Applies the profile consumer plugin
            project.pluginManager.apply(BaselineProfileConsumerPlugin::class.java)
        }

        // If this module is a test module
        project.pluginManager.withPlugin("com.android.test") {

            // Applies the profile producer plugin
            project.pluginManager.apply(BaselineProfileProducerPlugin::class.java)
        }
    }
}
