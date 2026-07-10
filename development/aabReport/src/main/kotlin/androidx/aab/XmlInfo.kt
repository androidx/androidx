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

package androidx.aab

import com.android.aapt.Resources.XmlNode
import com.android.apksig.internal.apk.AndroidBinXmlParser
import com.google.protobuf.InvalidProtocolBufferException
import java.nio.ByteBuffer
import java.util.zip.ZipInputStream

object XmlInfo {
    fun MutableSet<String>.addFiltered(string: String) {
        if (!string.contains(' ') && string.contains('.') && !string.contains(',')) {
            add(string)
        }
    }

    /** APKs should only have Android-binary formatted XML files referring to classes */
    fun collectStringsFromApkResourceFile(bytes: ByteArray, strings: MutableSet<String>) {
        val parser = AndroidBinXmlParser(ByteBuffer.wrap(bytes))

        while (parser.next() != AndroidBinXmlParser.EVENT_END_DOCUMENT) {
            when (parser.eventType) {
                AndroidBinXmlParser.EVENT_START_ELEMENT -> {
                    strings.addFiltered(parser.name)
                    for (i in 0 until parser.attributeCount) {
                        if (
                            parser.getAttributeValueType(i) == AndroidBinXmlParser.VALUE_TYPE_STRING
                        ) {
                            val attrName = parser.getAttributeName(i)
                            val attrValue = parser.getAttributeStringValue(i)

                            strings.addFiltered(attrName)
                            strings.addFiltered(attrValue)
                        }
                    }
                }
            }
        }
    }

    /**
     * Bundles may have both Bundle-Proto and Android-binary formatted XML files referring to
     * classes
     */
    fun collectStringsFromBundleResourceFile(
        zis: ZipInputStream,
        strings: MutableSet<String>,
    ) {
        val bytes = zis.readAllBytes()
        try {
            // first try and parse as a proto
            val root = XmlNode.parseFrom(bytes)
            collectStringsRecursive(root, strings)
        } catch (_: InvalidProtocolBufferException) {
            // next try parse as standard binary resource file
            collectStringsFromApkResourceFile(bytes, strings)
        }
        // TODO: try raw xml string parsing too
    }

    private fun collectStringsRecursive(node: XmlNode, strings: MutableSet<String>) {
        // 1. Extract text from raw text nodes
        if (node.hasText()) {
            strings.addFiltered(node.text)
        }

        // 2. Extract data from elements
        if (node.hasElement()) {
            val element = node.element

            // Tag names
            strings.addFiltered(element.name)

            // Attributes (names and values)
            element.attributeList.forEach { attr ->
                strings.addFiltered(attr.name)
                if (attr.value.isNotEmpty()) {
                    strings.addFiltered(attr.value)
                }
            }

            // Recurse through children
            element.childList.forEach { child -> collectStringsRecursive(child, strings) }
        }
    }
}
