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

package androidx.room.gradle

import androidx.room.gradle.integration.AndroidPluginIntegration
import androidx.room.gradle.integration.CommonIntegration
import androidx.room.gradle.integration.KotlinMultiplatformPluginIntegration
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ProviderFactory

class RoomGradlePlugin
@Inject
constructor(projectLayout: ProjectLayout, providerFactory: ProviderFactory) : Plugin<Project> {

    private val commonIntegration = CommonIntegration(projectLayout, providerFactory)
    private val androidIntegration by lazy { AndroidPluginIntegration(commonIntegration) }
    private val kmpIntegration by lazy { KotlinMultiplatformPluginIntegration(commonIntegration) }

    override fun apply(project: Project) {
        val roomExtension = project.extensions.create("room", RoomExtension::class.java)
        androidIntegration.withAndroid(project, roomExtension)
        kmpIntegration.withKotlin(project, roomExtension)
    }
}
