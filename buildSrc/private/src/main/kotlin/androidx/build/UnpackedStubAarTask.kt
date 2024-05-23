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

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.intellij.lang.annotations.Language

/**
 * Creates a directory representing a stub (essentially empty) .aar This directory can be zipped to
 * make an actual .aar
 */
@DisableCachingByDefault(because = "Doesn't benefit from caching")
abstract class UnpackedStubAarTask : DefaultTask() {
    @get:Input abstract val aarPackage: Property<String>
    @get:Input abstract val minSdkVersion: Property<Int>
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        // setup
        val outputDir = outputDir.getAsFile().get()
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        // write AndroidManifest.xml
        val manifestFile = File("$outputDir/AndroidManifest.xml")
        val aarPackage = aarPackage.get()
        @Language("xml")
        val manifestText =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="$aarPackage">
                <uses-sdk android:minSdkVersion="${minSdkVersion.get()}"/>
            </manifest>"""
                .trimIndent()
        manifestFile.writeText(manifestText)
    }
}
