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

package androidx.build

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project

@Suppress("DEPRECATION") // BaseVariant
val Project.agpVariants: DomainObjectSet<out com.android.build.gradle.api.BaseVariant>
    get() {
        val extension =
            checkNotNull(project.extensions.findByType(BaseExtension::class.java)) {
                "${project.name} has no BaseExtension"
            }
        return when (extension) {
            is AppExtension -> extension.applicationVariants
            is LibraryExtension -> extension.libraryVariants
            is TestExtension -> extension.applicationVariants
            else -> error("Unhandled plugin ${extension::class.java}")
        }
    }
