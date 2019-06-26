/*
 * Copyright 2017 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform.metainf

import com.android.tools.build.jetifier.core.pom.PomDependency
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.transform.TransformationContext
import com.android.tools.build.jetifier.processor.transform.Transformer
import java.nio.file.Paths

/**
 * Transformer for META-INF/(.*).version files.
 *
 * Replaces version files from the META-INF directory. This should be used only for processing
 * of the support library itself.
 */
class MetaInfTransformer internal constructor(
    private val context: TransformationContext
) : Transformer {

    companion object {
        const val META_INF_DIR = "meta-inf"

        const val VERSION_FILE_SUFFIX = ".version"

        val FILES_TO_IGNORE = setOf(
            "androidx.car_car-cluster.version",
            "androidx.car_car-moderator.version",
            "androidx.activity_activity-ktx.version",
            "androidx.lifecycle_lifecycle-runtime-ktx.version",
            "androidx.dynamicanimation_dynamicanimation-ktx.version",
            "androidx.annotation_annotation-experimental.version"
        )
    }

    // Does not support single proguard file transformation, file has to be within archive.
    override fun canTransform(file: ArchiveFile): Boolean {
        return context.rewritingSupportLib &&
            file.relativePath.toString().contains(META_INF_DIR, ignoreCase = true) &&
            file.fileName.endsWith(VERSION_FILE_SUFFIX, ignoreCase = true) &&
            !file.isSingleFile
    }

    override fun runTransform(file: ArchiveFile) {
        if (FILES_TO_IGNORE.contains(file.fileName)) {
            return
        }

        val tokens = file.fileName.removeSuffix(VERSION_FILE_SUFFIX).split("_")
        if (tokens.size != 2 || tokens.any { it.isNullOrEmpty() }) {
            return
        }

        val dependency = PomDependency(groupId = tokens[0], artifactId = tokens[1])
        val rule = context.config.pomRewriteRules.firstOrNull { it.matches(dependency) }
        if (rule == null) {
            // This class runs only during de-jetification so we can be strict here
            throw IllegalArgumentException("Unsupported version file '${file.relativePath}'")
        }

        // Replace with new dependencies
        val result = rule.to.rewrite(dependency, context.versions)

        // Update the file content
        file.setNewData(result.version!!.toByteArray())

        // Update the file path
        val dirPath = file.relativePath.toString().removeSuffix(file.fileName)
        val newFileName = result.groupId + "_" + result.artifactId + VERSION_FILE_SUFFIX
        val newPath = Paths.get(dirPath, newFileName)
        file.updateRelativePath(newPath)
    }
}
