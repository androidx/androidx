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

/** Removes the KT class from the public API */
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.watchface.complications

import android.content.res.Resources
import android.content.res.XmlResourceParser
import androidx.annotation.RestrictTo
import org.xmlpull.v1.XmlPullParser

/**
 * Exception to be thrown if an incorrect node is reached during parsing.
 */
/** @hide */
class IllegalNodeException(parser: XmlResourceParser) :
    IllegalArgumentException("Unexpected node ${parser.name} at line ${parser.lineNumber}")

/**
 * Iterate through inner nodes of the current node.
 *
 * @param block called on each node.
 * @hide
 */
fun XmlResourceParser.iterate(block: () -> Unit) {
    val outerDepth = this.depth
    var type = this.next()

    while (type != XmlPullParser.END_DOCUMENT && this.depth > outerDepth) {
        if (type == XmlPullParser.START_TAG) {
            block()
        }
        type = this.next()
    }
}

/**
 * Move to the beginning of the expectedNode.
 *
 * @param expectedNode called on each node.
 * @hide
 */
fun XmlPullParser.moveToStart(expectedNode: String) {
    var type: Int
    do {
        type = next()
    } while (type != XmlPullParser.END_DOCUMENT && type != XmlPullParser.START_TAG)

    require(name == expectedNode) {
        "Expected a $expectedNode node but is $name"
    }
}

/**
 * Gets the value of a string attribute from resource value, if not found, return the value itself
 * or null if there is no value
 *
 * @param resources the [Resources] from which the value is loaded.
 * @param parser The [XmlResourceParser] instance.
 * @param name the name of the attribute.
 * @hide
 */
fun getStringRefAttribute(
    resources: Resources,
    parser: XmlResourceParser,
    name: String
): String? {
    return if (parser.hasValue(name)) {
        val resId = parser.getAttributeResourceValue(NAMESPACE_APP, name, 0)
        if (resId == 0) {
            parser.getAttributeValue(NAMESPACE_APP, name)
        } else {
            resources.getString(resId)
        }
    } else null
}

/**
 * Gets the value of a integer attribute from resource value, if not found, return the value itself
 * or null if there is no value
 *
 * @param resources the [Resources] from which the value is loaded.
 * @param parser The [XmlResourceParser] instance.
 * @param name the name of the attribute.
 * @hide
 */
fun getIntRefAttribute(
    resources: Resources,
    parser: XmlResourceParser,
    name: String
): Int? {
    return if (parser.hasValue(name)) {
        val resId = parser.getAttributeResourceValue(NAMESPACE_APP, name, 0)
        if (resId == 0) {
            parser.getAttributeValue(NAMESPACE_APP, name).toInt()
        } else {
            resources.getInteger(resId)
        }
    } else null
}
