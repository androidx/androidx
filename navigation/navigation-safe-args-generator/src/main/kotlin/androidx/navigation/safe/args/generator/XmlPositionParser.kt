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

package androidx.navigation.safe.args.generator

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader

internal class XmlPositionParser(private val name: String, reader: Reader, val logger: NavLogger) {
    private var startLine = 0
    private var startColumn = 0
    private val parser: XmlPullParser = XmlPullParserFactory.newInstance().newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        setInput(reader)
    }

    fun name(): String = parser.name

    fun traverseStartTags(onStartTag: () -> Boolean) {
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            val processedLine = parser.lineNumber
            val processedColumn = parser.columnNumber
            if (parser.eventType == XmlPullParser.START_TAG) {
                if (onStartTag()) {
                    return
                }
            }

            if (processedLine == parser.lineNumber && processedColumn == parser.columnNumber) {
                // otherwise onStart already called next() and we need to try to process current node
                nextToken()
            }
        }
    }

    private fun nextToken() {
        startLine = parser.lineNumber
        startColumn = parser.columnNumber
        parser.nextToken()
    }

    fun xmlPosition() = XmlPosition(name, startLine, startColumn - 1)

    fun traverseInnerStartTags(onStartTag: () -> Unit) {
        val innerDepth = parser.depth + 1
        nextToken()
        traverseStartTags {
            if (innerDepth == parser.depth) {
                onStartTag()
            }
            parser.depth < innerDepth
        }
    }

    fun attrValue(namespace: String, name: String): String? =
        (0 until parser.attributeCount).find {
            parser.getAttributeNamespace(it) == namespace && name == parser.getAttributeName(it)
        }?.let { parser.getAttributeValue(it) }

    fun attrValueOrError(namespace: String, attrName: String): String? {
        val value = attrValue(namespace, attrName)
        if (value == null) {
            logger.error(mandatoryAttrMissingError(name(), attrName), xmlPosition())
        }
        return value
    }
}

internal fun mandatoryAttrMissingError(tag: String, attr: String) =
    "Mandatory attribute '$attr' for tag '$tag' is missing."