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

package com.android.tools.build.jetifier.processor.transform.resource

import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.type.PackageName
import com.android.tools.build.jetifier.core.type.TypesMap
import com.android.tools.build.jetifier.core.utils.Log
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.transform.TransformationContext
import com.android.tools.build.jetifier.processor.transform.Transformer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import javax.xml.stream.XMLInputFactory

/**
 * Transformer for XML resource files.
 *
 * Searches for any java type reference that is pointing to the support library and rewrites it
 * using the available mappings from [TypesMap].
 */
class XmlResourcesTransformer internal constructor(private val context: TransformationContext)
        : Transformer {

    companion object {
        const val TAG = "XmlResourcesTransformer"

        const val PATTERN_TYPE_GROUP = 1

        const val MANIFEST_FILE_NAME = "AndroidManifest.xml"
    }

    /**
     * List of regular expression patterns used to find support library types references in XML
     * files.
     *
     * Matches xml tags in form of:
     * 1. '<(/)prefix(SOMETHING)'.
     * 2. <view ... class="prefix(SOMETHING)" ...>
     *
     * Note that this can also rewrite commented blocks of XML. But on a library level we don't care
     * much about comments.
     */
    private val patterns = listOf(
        Pattern.compile("</?([a-zA-Z0-9.]+)"),
        Pattern.compile("<view[^>]*class=\"([a-zA-Z0-9.\$_]+)\"[^>]*>"),
        Pattern.compile("<application[^>]*android:appComponentFactory=\"([a-zA-Z0-9.\$_]+)\"[^>]*>")
    )

    /**
     * List of regular expression patterns used to find support library package references in
     * manifest files.
     *
     * Matches xml tag in form of:
     * 1. <manifest package="package.name" ...>
     */
    private val packagePatterns = listOf(
        Pattern.compile("<manifest[^>]*package=\"([a-zA-Z0-9._]+)\"[^>]*>")
    )

    override fun canTransform(file: ArchiveFile) = file.isXmlFile() && !file.isPomFile()

    override fun runTransform(file: ArchiveFile) {
        val isManifestFile = file.fileName.equals(MANIFEST_FILE_NAME, ignoreCase = true)
        val charset = getCharset(file.data)
        val sb = StringBuilder(file.data.toString(charset))

        var changesDone = replaceWithPatterns(sb, patterns, { rewriteType(it) })

        if (isManifestFile && context.rewritingSupportLib) {
            changesDone = replaceWithPatterns(sb, packagePatterns,
                { rewritePackage(it, context.libraryName) }) || changesDone
        }

        if (changesDone) {
            file.setNewData(sb.toString().toByteArray(charset))
        }
    }

    fun getCharset(data: ByteArray): Charset {
        data.inputStream().use {
            val xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(it)

            xmlReader.encoding ?: return StandardCharsets.UTF_8 // Encoding was not detected

            val result = Charset.forName(xmlReader.encoding)
            if (result == null) {
                Log.e(TAG, "Failed to find charset for encoding '%s'", xmlReader.encoding)
                return StandardCharsets.UTF_8
            }
            return result
        }
    }

    /**
     * For each pattern in [patterns] matching a portion of the string represented by [sb], applies
     * [mappingFunction] to the match and puts the result back into [sb].
     */
    private fun replaceWithPatterns(
        sb: StringBuilder,
        patterns: List<Pattern>,
        mappingFunction: (String) -> String
    ): Boolean {
        var changesDone = false

        for (pattern in patterns) {
            var lastSeenChar = 0
            val processedInput = sb.toString()
            sb.setLength(0)
            val matcher = pattern.matcher(processedInput)

            while (matcher.find()) {
                if (lastSeenChar < matcher.start()) {
                    sb.append(processedInput, lastSeenChar, matcher.start())
                }

                val toReplace = matcher.group(PATTERN_TYPE_GROUP)
                val matched = matcher.group(0)
                val replacement = mappingFunction(toReplace)
                changesDone = changesDone || replacement != toReplace

                val localStart = matcher.start(PATTERN_TYPE_GROUP) - matcher.start()
                val localEnd = matcher.end(PATTERN_TYPE_GROUP) - matcher.start()

                val result = matched.replaceRange(
                    startIndex = localStart,
                    endIndex = localEnd,
                    replacement = replacement)

                sb.append(result)
                lastSeenChar = matcher.end()
            }

            if (lastSeenChar <= processedInput.length - 1) {
                sb.append(processedInput, lastSeenChar, processedInput.length)
            }
        }

        return changesDone
    }

    private fun rewriteType(typeName: String): String {
        val type = JavaType.fromDotVersion(typeName)
        val result = context.typeRewriter.rewriteType(type)
        if (result != null) {
            return result.toDotNotation()
        }

        if (!context.useFallbackIfTypeIsMissing) {
            context.reportNoMappingFoundFailure()
        }

        return typeName
    }

    private fun rewritePackage(packageName: String, archiveName: String): String {
        val pckg = PackageName.fromDotVersion(packageName)
        val result = context.config.packageMap.getPackageFor(pckg, archiveName)
        if (result != null) {
            return result.toDotNotation()
        }
        if (context.useFallbackIfTypeIsMissing) {
            Log.i(TAG, "No mapping for package: '%s' in artifact: '%s' - using identity",
                    pckg, archiveName)
            return packageName
        }

        context.reportNoPackageMappingFoundFailure()
        Log.e(TAG, "No mapping for package: '%s' in artifact: '%s'", pckg, archiveName)
        return packageName
    }
}