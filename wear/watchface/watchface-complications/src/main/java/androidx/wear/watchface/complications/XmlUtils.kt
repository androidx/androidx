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
