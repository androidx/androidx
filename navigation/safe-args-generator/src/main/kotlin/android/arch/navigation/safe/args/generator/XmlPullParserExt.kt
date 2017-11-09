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

package android.arch.navigation.safe.args.generator

import org.xmlpull.v1.XmlPullParser

fun XmlPullParser.traverseStartTags(onStartTag: (XmlPullParser) -> Boolean) {
    while (eventType != XmlPullParser.END_DOCUMENT) {
        val processedLine = lineNumber
        val processedColumn = columnNumber
        if (eventType == XmlPullParser.START_TAG) {
            if (onStartTag(this)) {
                return
            }
        }
        if (processedLine == lineNumber && processedColumn == columnNumber) {
            // otherwise onStart already called next() and we need to try to process current node
            next()
        }
    }
}

fun XmlPullParser.traverseInnerStartTags(onStartTag: (XmlPullParser) -> Unit) {
    val innerDepth = depth + 1
    next()
    traverseStartTags {
        if (innerDepth == it.depth) {
            onStartTag(it)
        }
        it.depth < innerDepth
    }
}

fun XmlPullParser.attrValue(namespace: String, name: String): String? =
        (0 until this.attributeCount).find {
            getAttributeNamespace(it) == namespace && name == getAttributeName(it)
        }?.let { getAttributeValue(it) }


fun XmlPullParser.attrValueOrThrow(namespace: String, name: String): String =
        attrValue(namespace, name) ?:
                throw IllegalStateException("attribute $namespace:$name is missing.")
