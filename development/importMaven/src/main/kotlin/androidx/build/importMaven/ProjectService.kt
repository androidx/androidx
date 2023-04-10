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

package androidx.build.importMaven

import org.apache.logging.log4j.kotlin.logger
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.util.UUID

/**
 * Helper class to create Gradle projects
 */
object ProjectService {
    private val logger = logger("ProjectService")
    private val tmpFolder = System.getProperty("java.io.tmpdir").let { File(it) }

    fun createProject(): Project {
        val folder = randomProjectFolder()
        logger.trace {
            "created project at $folder"
        }
        return ProjectBuilder.builder().withProjectDir(
            folder.resolve("project").also {
                it.mkdirs()
            }
        ).withGradleUserHomeDir(
            folder.resolve("gradle-home").also {
                it.mkdirs()
            }
        ).withName("importMaven")
            .build()
    }

    private fun randomProjectFolder(): File {
        while (true) {
            val file = tmpFolder.resolve(randomId())
            if (!file.exists()) {
                file.mkdirs()
                file.deleteOnExit()
                return file
            }
        }
    }

    private fun randomId(): String = UUID.randomUUID().toString().subSequence(0, 6).toString()
}