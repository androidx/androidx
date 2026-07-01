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

import com.android.build.api.variant.BuiltArtifactsLoader
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Copy single APK (from AGP artifacts) needed for building androidTest.zip */
@DisableCachingByDefault(because = "Only filesystem operations")
abstract class CopyApkFromArtifactsTask @Inject constructor(private val objects: ObjectFactory) :
    DefaultTask() {

    /** File existence check to determine whether to run this task. */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val androidTestSourceCode: ConfigurableFileCollection

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val appFolder: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val appFileCollection: ConfigurableFileCollection

    @get:Internal abstract val appLoader: Property<BuiltArtifactsLoader>

    @get:OutputFile abstract val outputAppApk: RegularFileProperty

    @get:OutputFile abstract val outputAppApksModel: RegularFileProperty

    @TaskAction
    fun createApks() {
        // Decides where to load the app apk from, depending on whether appFolder or
        // appFileCollection has been set.
        val appDir =
            if (appFolder.isPresent && appFileCollection.files.isEmpty()) {
                appFolder.get()
            } else if (!appFolder.isPresent && appFileCollection.files.size == 1) {
                objects.directoryProperty().also { it.set(appFileCollection.files.first()) }.get()
            } else {
                throw IllegalStateException(
                    """
                    App apk not specified or both appFileCollection and appFolder specified.
                """
                        .trimIndent()
                )
            }

        val appApk =
            appLoader.get().load(appDir)
                ?: throw RuntimeException("Cannot load required APK for task: $name")

        val appApkBuiltArtifact = appApk.elements.single()
        val destinationApk = outputAppApk.get().asFile
        File(appApkBuiltArtifact.outputFile).copyTo(destinationApk, overwrite = true)

        val model =
            singleFileAppApksModel(name = destinationApk.name, sha256 = sha256(destinationApk))
        outputAppApksModel.get().asFile.writeText(model.toJson())
    }
}
