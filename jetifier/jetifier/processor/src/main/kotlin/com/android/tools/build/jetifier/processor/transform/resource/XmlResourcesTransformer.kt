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
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.regex.Pattern
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException

/**
 * Transformer for XML resource files.
 *
 * Searches for any java type reference that is pointing to the support library and rewrites it
 * using the available mappings from [TypesMap].
 */
class XmlResourcesTransformer internal constructor(private val context: TransformationContext) :
        Transformer {

    companion object {
        const val TAG = "XmlResourcesTransformer"

        const val PATTERN_TYPE_GROUP = 1

        /***
         * Matches anything that could be java type or package
         */
        val JAVA_TOKEN_MATCHER = "^[a-zA-Z0-9.\$_]+$".toRegex()
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
        Pattern.compile("</?([a-zA-Z0-9.]+)"), // </{candidate} or <{candidate}
        Pattern.compile("[a-zA-Z0-9:]+=\"([^\"]+)\""), // any="{candidate}"
        Pattern.compile(">\\s*([a-zA-Z0-9.\$_]+)<") // >{candidate}<
    )

    override fun canTransform(file: ArchiveFile) = file.isXmlFile() && !file.isPomFile()

    override fun runTransform(file: ArchiveFile) {
        if (file.isSingleFile) {
            transformSource(file, context)
            return
        }
        if (file.fileName == "maven-metadata.xml") {
            // Dejetification is picking this file and we don't want to deal with it.
            return
        }

        val charset = getCharset(file)
        val sb = StringBuilder(file.data.toString(charset))

        val changesDone = replaceWithPatterns(sb, patterns, file.relativePath)
        if (changesDone) {
            file.setNewData(sb.toString().toByteArray(charset))
        }

        // If we are dealing with linter annotations we need to move the xml files also
        if (context.isInReversedMode &&
                changesDone &&
                file.relativePath.toString().endsWith("annotations.xml")) {
            file.updateRelativePath(rewriteAnnotationsXmlPath(file.relativePath))
        }
    }

    fun getCharset(file: ArchiveFile): Charset {
        try {
            file.data.inputStream().use {
                val xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(it)

                xmlReader.encoding ?: return StandardCharsets.UTF_8 // Encoding was not detected

                val result = Charset.forName(xmlReader.encoding)
                if (result == null) {
                    Log.e(TAG, "Failed to find charset for encoding '%s'", xmlReader.encoding)
                    return StandardCharsets.UTF_8
                }
                return result
            }
        } catch (e: XMLStreamException) {
            // Workaround for b/111814958. A subset of the android.jar xml files has a header that
            // causes our encoding detection to crash. However these files are otherwise valid UTF-8
            // files so we at least try to recover by defaulting to UTF-8.
            Log.w(TAG, "Received malformed sequence exception when trying to detect the encoding " +
                "for '%s'. Defaulting to UTF-8.", file.fileName)
            val tracePrinter = StringWriter()
            e.printStackTrace(PrintWriter(tracePrinter))
            Log.w(TAG, tracePrinter.toString())
            return StandardCharsets.UTF_8
        }
    }

    /**
     * For each pattern in [patterns] matching a portion of the string represented by [sb], applies
     * [mappingFunction] to the match and puts the result back into [sb].
     */
    private fun replaceWithPatterns(
        sb: StringBuilder,
        patterns: List<Pattern>,
        filePath: Path
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

                var replacement =
                    if (toReplace.matches(JAVA_TOKEN_MATCHER)) {
                        if (isPackage(toReplace)) {
                            rewritePackage(toReplace, filePath)
                        } else {
                            rewriteType(toReplace)
                        }
                    } else {
                        toReplace
                    }

                // Try if we are rewriting annotations file and replace symbols there
                if (context.isInReversedMode &&
                        replacement == toReplace &&
                        filePath.toString().endsWith("annotations.xml")) {
                    replacement = tryToRewriteTypesInAnnotationFile(toReplace)
                }

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

    private fun isPackage(token: String): Boolean {
        return !token.any { it.isUpperCase() }
    }

    private fun rewriteType(typeName: String): String {
        if (typeName.contains(" ")) {
            return typeName
        }

        val type = JavaType.fromDotVersion(typeName)
        val result = context.typeRewriter.rewriteType(type)
        if (result != null) {
            return result.toDotNotation()
        }

        context.reportNoMappingFoundFailure(TAG, type)
        return typeName
    }

    private fun rewritePackage(packageName: String, filePath: Path): String {
        if (!packageName.contains('.')) {
            // Single word packages are not something we need or should rewrite
            return packageName
        }

        val pckg = PackageName.fromDotVersion(packageName)

        val result = context.config.packageMap.getPackageFor(pckg)
        if (result != null) {
            return result.toDotNotation()
        }

        if (context.config.isEligibleForRewrite(pckg)) {
            context.reportNoPackageMappingFoundFailure(TAG, packageName, filePath)
        }

        return packageName
    }

    /**
     * This is supposed to be used to rewrite tokens in annotation files. These are special in the
     * way that they contain method declarations. Rewriting these requires to cut them into tokens
     * first.
     */
    private fun tryToRewriteTypesInAnnotationFile(type: String): String {
        // Cut the input into tokens to separate androidx references
        val tokens = type.split(" ", ",", "(", ")", "{", "}", ";")
        var result = type

        tokens.forEach {
            val rewritten = rewriteType(it)
            if (rewritten != it) {
                result = result.replace(it, rewritten)
            }
        }

        return result
    }

    private fun rewriteAnnotationsXmlPath(path: Path): Path {
        val owner = path.toFile().path.replace('\\', '/').removeSuffix(".xml")
        val type = JavaType(owner)

        val result = context.typeRewriter.rewriteType(type)
        if (result == null) {
            context.reportNoMappingFoundFailure("PathRewrite", type)
            return path
        }

        if (result != type) {
            return path.fileSystem.getPath(result.fullName + ".xml")
        }
        return path
    }
}