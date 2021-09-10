/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.room.compiler.processing.util.Source
import java.io.File

/**
 * Represents sources that are positioned in the [root] folder.
 * see: [fromExistingFiles]
 */
internal class SourceSet(
    /**
     * The root source folder for the given sources
     */
    root: File,
    /**
     * List of actual sources in the folder
     */
    val sources: List<Source>
) {
    // always use canonical files
    val root = root.canonicalFile

    init {
        check(root.isDirectory) {
            "$root must be a directory"
        }
    }

    val hasJavaSource by lazy {
        javaSources.isNotEmpty()
    }

    val hasKotlinSource by lazy {
        kotlinSources.isNotEmpty()
    }

    val javaSources by lazy {
        sources.filterIsInstance<Source.JavaSource>()
    }

    val kotlinSources by lazy {
        sources.filterIsInstance<Source.KotlinSource>()
    }

    /**
     * Finds the source file matching the given relative path (from root)
     */
    fun findSourceFile(
        path: String
    ): Source? {
        val file = File(path).canonicalFile
        if (!file.path.startsWith(root.path)) {
            return null
        }
        val relativePath = path.substringAfter(root.canonicalPath + "/")
        return sources.firstOrNull {
            it.relativePath == relativePath
        }
    }

    companion object {
        /**
         * Creates a new SourceSet from the given files.
         */
        fun fromExistingFiles(
            root: File
        ) = SourceSet(
            root = root,
            sources = root.collectSources().toList()
        )
    }
}

/**
 * Collects all java/kotlin sources in a given directory.
 * Note that the package name for java sources are inherited from the given relative path.
 */
private fun File.collectSources(): Sequence<Source> {
    val root = this
    return walkTopDown().mapNotNull { file ->
        when (file.extension) {
            "java" -> Source.loadJavaSource(
                file = file,
                qName = file.relativeTo(root).path
                    .replace('/', '.')
                    .substringBeforeLast('.') // drop .java
            )
            "kt" -> Source.loadKotlinSource(
                file = file,
                relativePath = file.relativeTo(root).path
            )
            else -> null
        }
    }
}
/**
 * Converts the file to a [SourceSet] if and only if it is a directory.
 */
internal fun File.toSourceSet() = if (isDirectory) {
    SourceSet.fromExistingFiles(this)
} else {
    null
}