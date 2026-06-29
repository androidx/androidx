/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build.gradle

import org.gradle.api.Project

val Project.isRoot
    get() = this == rootProject

/**
 * Implements project.extensions.extraProperties.getOrNull(key)
 *
 * TODO(https://github.com/gradle/gradle/issues/28857) use simpler replacement when available
 *
 * Note that providers.gradleProperty() might return null in cases where this function can find a
 * value: https://github.com/gradle/gradle/issues/23572
 */
fun Project.extraPropertyOrNull(key: String): Any? {
    val container = project.extensions.extraProperties
    var result: Any? = null
    if (container.has(key)) result = container.get(key)
    return result
}
