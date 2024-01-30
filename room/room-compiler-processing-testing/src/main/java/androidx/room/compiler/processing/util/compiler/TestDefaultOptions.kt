/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.compiler.processing.util.compiler

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion

/** Default argument / options across the steps and test compiler infra. */
internal object TestDefaultOptions {
    internal val kotlinLanguageVersion =
        LanguageVersion.fromFullVersionString(KotlinVersion.CURRENT.toString())!!
    internal val kotlinApiVersion = ApiVersion.createByLanguageVersion(kotlinLanguageVersion)
    internal val jvmTarget = JvmTarget.JVM_1_8
    internal val jvmDefaultMode = JvmDefaultMode.ALL_COMPATIBILITY
}
