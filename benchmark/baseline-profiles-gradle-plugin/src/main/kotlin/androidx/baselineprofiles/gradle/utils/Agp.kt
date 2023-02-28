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

package androidx.baselineprofiles.gradle.utils

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.GradleException
import org.gradle.api.Project

private val gradleSyncProps by lazy {
    listOf(
        "android.injected.build.model.v2",
        "android.injected.build.model.only",
        "android.injected.build.model.only.advanced",
    )
}

internal fun Project.isGradleSyncRunning() =
    gradleSyncProps.any { it in properties && properties[it].toString().toBoolean() }

internal fun Project.checkAgpVersion() {
    val extension = project.extensions.findByType(AndroidComponentsExtension::class.java)
        ?: if (!isGradleSyncRunning()) {
            throw GradleException(
                """
                    The module $name does not have a registered `AndroidComponentsExtension`. This can
                    only happen if this is not an Android module. Please review your build.gradle to
                    ensure this plugin is applied to the correct module.
                """.trimIndent()
            )
        } else return

    val agpVersion = extension.pluginVersion
    if (agpVersion < MIN_AGP_VERSION_REQUIRED || agpVersion > MAX_AGP_VERSION_REQUIRED) {
        throw GradleException(
            """
            This version of the Baseline Profile Gradle Plugin only works with Android Gradle plugin
            between versions $MIN_AGP_VERSION_REQUIRED and $MAX_AGP_VERSION_REQUIRED. Current version
            is $agpVersion."
            """.trimIndent()
        )
    }
}