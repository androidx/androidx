#!/usr/bin/env kotlin
/*
 * Copyright 2026 The Android Open Source Project
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

import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import kotlin.io.readText

// This script updates all package-list files in `docs-public/package-lists`. See the README in that
// directory for details on the expected contents of the `package-lists` subdirectories.

// Navigate up to frameworks/support
val supportDir: File = __FILE__.absoluteFile.parentFile.parentFile.parentFile
updatePackageLists(File(supportDir, "docs-public/package-lists"))

/** Updates the `package-list` file in each subdirectory of [dir]. */
fun updatePackageLists(dir: File) {
    for (packageListDir in dir.listFiles()) {
        if (!packageListDir.isDirectory) continue
        val packageList = PackageListEntry(packageListDir)
        packageList.update()
    }
}

/** Wrapper for functionality of updating a single `package-list` file located in [dir]. */
class PackageListEntry(private val dir: File) {
    val name: String = dir.name

    /** Updates the `package-list` file. */
    fun update(): Boolean {
        // If there is a `download-url` file, use that, otherwise use the base url.
        val downloadUrlFile =
            getFileIfExists("download-url")
                ?: getFileIfExists("url")
                ?: return run {
                    println("ERROR: No `url` file defined for $name")
                    false
                }
        val baseDownloadUrl = downloadUrlFile.readText().trim()

        // Attempt to download the package-list.
        val baseNewContents =
            downloadFile(baseDownloadUrl, "package-list")
                // Newer javadoc calls it element-list instead of package-list.
                ?: downloadFile(baseDownloadUrl, "element-list")
                // Sometimes duplicating the last segment of URL will find the package list.
                ?: downloadFile(duplicateLastSegment(baseDownloadUrl), "package-list")
                ?: return run {
                    println(
                        "ERROR: Could not download `package-list` or `element-list` from " +
                            "$baseDownloadUrl for $name"
                    )
                    false
                }

        // Add the format line, if one is specified, and optionally filter the contents.
        val newContents = formatLine() + filterPackageList(baseNewContents)

        // Update the package-list file.
        getFile("package-list").writeText(newContents)
        println("INFO: Package list updated for $name")
        return true
    }

    /** Returns a [File] with [name] contained in [dir]. */
    private fun getFile(name: String): File {
        return File(dir, name)
    }

    /** Returns a [File] with [name] contained in [dir] if it exists, or null otherwise. */
    private fun getFileIfExists(name: String): File? {
        return getFile(name).takeIf { it.exists() }
    }

    /** Returns the optional line defined in a `format` file to prepend to the `package-list`. */
    private fun formatLine(): String {
        val formatFile = getFileIfExists("format") ?: return ""
        return formatFile.readText().trim() + "\n"
    }

    /**
     * Filters the contents of the downloaded [packageList] based on the `filter` file, if it
     * exists. Returns the new `package-list` contents.
     */
    private fun filterPackageList(packageList: String): String {
        val filterFile = getFileIfExists("filter") ?: return packageList
        val pattern = filterFile.readText().trim().toRegex()
        return packageList.split("\n").filter { pattern.matches(it) }.joinToString("\n")
    }

    companion object {
        /**
         * Downloads the page at [baseUrl]/[page], returning the page contents or `null` if the page
         * does not exist.
         */
        private fun downloadFile(baseUrl: String, page: String): String? {
            // Add a trailing slash if there isn't one already.
            val optionalSlash = "/".takeIf { !baseUrl.endsWith(it) } ?: ""
            val url = "$baseUrl$optionalSlash$page"
            return try {
                URI(url).toURL().readText()
            } catch (_: FileNotFoundException) {
                null
            }
        }

        /**
         * Returns the [url] with the last path segment duplicated. For instance, turns
         * `https://square.github.io/kotlinpoet/1.x/kotlinpoet` into
         * `https://square.github.io/kotlinpoet/1.x/kotlinpoet/kotlinpoet`.
         */
        private fun duplicateLastSegment(url: String): String {
            val withoutSlash = url.removeSuffix("/")
            val lastSegment = withoutSlash.substringAfterLast("/")
            return "$withoutSlash/$lastSegment"
        }
    }
}
