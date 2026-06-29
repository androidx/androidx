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

package androidx.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.component.AdhocComponentWithVariants

internal fun Project.registerAsComponentForPublishing(gradleVariant: Configuration) =
    components.configureEach {
        // Android Library project 'release' component
        // Java Library project 'java' component
        if (it.name == "release" || it.name == "java") {
            it as AdhocComponentWithVariants
            it.addVariantsFromConfiguration(gradleVariant) {}
        }
    }

internal fun Project.registerAsComponentForKmpPublishing(gradleVariant: Configuration) =
    components.configureEach {
        // Multiplatform library 'adhocKotlin' component
        // https://github.com/JetBrains/kotlin/blob/bf6cb00fa8db7879c323bad863f58a0545c3d655/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/dsl/KotlinMultiplatformPublishing.kt#L20
        if (it.name == "adhocKotlin") {
            it as AdhocComponentWithVariants
            it.addVariantsFromConfiguration(gradleVariant) {}
        }
    }
