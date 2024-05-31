/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.stableaidl.tasks

import java.io.File
import java.security.MessageDigest
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Task for updating the public Android resource surface, e.g. `public.xml`. */
@CacheableTask
abstract class UpdateStableAidlApiTask : DefaultTask() {
    /** Generated resource API file (in build output). */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val apiLocation: DirectoryProperty

    @get:Input abstract val forceUpdate: Property<Boolean>

    /** AIDL API directories to which APIs should be written (in source control). */
    @get:OutputDirectories abstract val outputApiLocations: ListProperty<File>

    @TaskAction
    fun updateAidlApi() {
        // TODO: Work out policy for allowing Stable AIDL overwrites.
        // var permitOverwriting = true
        // for (outputApi in outputApiLocations.get()) {
        //     val version = outputApi.version()
        //     if (version != null && version.isFinalApi() &&
        //         outputApi.publicApiFile.exists() &&
        //         !forceUpdate.get()
        //     ) {
        //         permitOverwriting = false
        //     }
        // }

        val inputApi = apiLocation.get()

        for (outputApi in outputApiLocations.get()) {
            copyDir(inputApi.asFile, outputApi, true, logger)
        }
    }
}

internal fun copyDir(source: File, dest: File, permitOverwriting: Boolean, logger: Logger) {
    val sourceHash =
        if (source.exists()) {
            hashDir(source)
        } else {
            null
        }
    val overwriting = dest.exists() && !sourceHash.contentEquals(hashDir(dest))
    val changing = overwriting || (dest.exists() != source.exists())
    if (changing) {
        if (overwriting && !permitOverwriting) {
            val message =
                "Modifying the API definition for a previously released artifact " +
                    "having a final API version (version not ending in '-alpha') is not " +
                    "allowed.\n\n" +
                    "Previously declared definition is $dest\n" +
                    "Current generated   definition is $source\n\n" +
                    "Did you mean to increment the library version first?\n\n" +
                    "If you have reason to overwrite the API files for the previous release " +
                    "anyway, you can run `./gradlew updateApi -Pforce` to ignore this message"
            throw GradleException(message)
        }
        FileUtils.deleteDirectory(dest)
        if (source.exists()) {
            FileUtils.copyDirectory(source, dest)
            logger.lifecycle("Copied $source to $dest")
        } else {
            logger.lifecycle("Deleted $dest because $source does not exist")
        }
    }
}

fun hashDir(dir: File): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")
    dir.listFiles()?.forEach { file ->
        val fileBytes =
            if (file.isDirectory) {
                hashDir(file)
            } else {
                file.readBytes()
            }
        digest.update(fileBytes)
    }
    return digest.digest()
}
