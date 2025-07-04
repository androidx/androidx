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

package androidx.build.testConfiguration

import androidx.build.multiplatformExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.TestVariant
import com.android.build.api.variant.Variant
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

internal fun Project.getTestSourceSetsForAndroid(variant: Variant?): List<FileCollection> {
    val testSourceFileCollections = mutableListOf<FileCollection>()
    when (variant) {
        is TestVariant -> {
            // com.android.test modules keep test code in main sourceset
            variant.sources.java?.all?.let { sourceSet ->
                testSourceFileCollections.add(files(sourceSet))
            }
            // Add kotlin-android main source set
            extensions
                .findByType(KotlinAndroidProjectExtension::class.java)
                ?.sourceSets
                ?.find { it.name == "main" }
                ?.let { testSourceFileCollections.add(it.kotlin.sourceDirectories) }
            // Note, don't have to add kotlin-multiplatform as it is not compatible with
            // com.android.test modules
        }
        is com.android.build.api.variant.HasAndroidTest -> {
            variant.androidTest?.sources?.java?.all?.let {
                testSourceFileCollections.add(files(it))
            }
        }
    }

    // Add kotlin-android androidTest source set
    extensions
        .findByType(KotlinAndroidProjectExtension::class.java)
        ?.sourceSets
        ?.find { it.name == "androidTest" }
        ?.let { testSourceFileCollections.add(it.kotlin.sourceDirectories) }

    // Add kotlin-multiplatform androidInstrumentedTest target source sets when com.android.library
    // plugin is applied
    multiplatformExtension
        ?.targets
        ?.filterIsInstance<KotlinAndroidTarget>()
        ?.mapNotNull {
            it.compilations.find { compilation -> compilation.name == "releaseAndroidTest" }
        }
        ?.flatMap { it.allKotlinSourceSets }
        ?.mapTo(testSourceFileCollections) { it.kotlin.sourceDirectories }

    // Add kotlin-multiplatform androidInstrumentedTest target source sets when AGP KMP plugin is
    // applied
    multiplatformExtension
        ?.targets
        ?.filterIsInstance<KotlinMultiplatformAndroidLibraryTarget>()
        ?.mapNotNull {
            it.compilations.find { compilation -> compilation.name == "instrumentedTest" }
        }
        ?.flatMap { it.allKotlinSourceSets }
        ?.mapTo(testSourceFileCollections) { it.kotlin.sourceDirectories }
    return testSourceFileCollections
}
