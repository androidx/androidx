/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core.transform.resource

import android.support.tools.jetifier.core.archive.ArchiveFile
import android.support.tools.jetifier.core.map.TypesMap
import android.support.tools.jetifier.core.rules.JavaTypeXmlRef
import android.support.tools.jetifier.core.transform.TransformationContext
import android.support.tools.jetifier.core.transform.Transformer
import android.support.tools.jetifier.core.utils.Log
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
    }

    /**
     * List of regular expression patterns used to find support library references in XML files.
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
        Pattern.compile("<view[^>]*class=\"([a-zA-Z0-9.\$_]+)\"[^>]*>")
    )

    private val typesMap = context.config.typesMap

    override fun canTransform(file: ArchiveFile) = file.isXmlFile() && !file.isPomFile()

    override fun runTransform(file: ArchiveFile) {
        file.data = transform(file.data)
    }

    fun transform(data: ByteArray) : ByteArray {
        var changesDone = false

        val charset = getCharset(data)
        val sb = StringBuilder(data.toString(charset))
        for (pattern in patterns) {
            var matcher = pattern.matcher(sb)
            while (matcher.find()) {
                val typeToReplace = JavaTypeXmlRef(matcher.group(PATTERN_TYPE_GROUP))
                val result = rewriteType(typeToReplace)
                if (result == typeToReplace) {
                    continue
                }
                sb.replace(matcher.start(PATTERN_TYPE_GROUP), matcher.end(PATTERN_TYPE_GROUP),
                    result.fullName)
                changesDone = true
                matcher = pattern.matcher(sb)
            }
        }

        if (changesDone) {
            return sb.toString().toByteArray(charset)
        }

        return data
    }

    fun getCharset(data: ByteArray) : Charset {
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

    fun rewriteType(type: JavaTypeXmlRef): JavaTypeXmlRef {
        val javaType = type.toJavaType()
        if (!context.isEligibleForRewrite(javaType)) {
            return type
        }

        val result = typesMap.types[javaType]
        if (result != null) {
            Log.i(TAG, "  map: %s -> %s", type, result)
            return JavaTypeXmlRef(result)
        }

        context.reportNoMappingFoundFailure()
        Log.e(TAG, "No mapping for: " + type)
        return type
    }
}