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

package androidx.appcompat.app;

import android.util.AttributeSet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Used on KitKat and below to determine if the currently inflated view is the start of
 * an included layout file. If so, any themed context (android:theme) needs to be manually
 * carried over to preserve it as expected.
 */
class LayoutIncludeDetector {

    private final @NonNull Deque<WeakReference<XmlPullParser>> mXmlParserStack = new ArrayDeque<>();

    /**
     * Returns true if this is the start of an included layout file, otherwise false.
     */
    boolean detect(@NonNull AttributeSet attrs) {
        if (attrs instanceof XmlPullParser) {
            XmlPullParser xmlAttrs = (XmlPullParser) attrs;
            if (xmlAttrs.getDepth() == 1) {
                // This is either beginning of an inflate or an include.
                // Start by popping XmlPullParsers which are no longer valid since we may
                // have returned from any number of sub-includes
                XmlPullParser ancestorXmlAttrs = popOutdatedAttrHolders(mXmlParserStack);
                // Then store current attrs for possible future use
                mXmlParserStack.push(new WeakReference<>(xmlAttrs));
                // Finally check if we need to inherit the parent context based on the
                // current and ancestor attribute set
                if (shouldInheritContext(xmlAttrs, ancestorXmlAttrs)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean shouldInheritContext(@NonNull XmlPullParser parser,
            @Nullable XmlPullParser previousParser) {
        if (previousParser != null && parser != previousParser) {

            // First check event type since that should avoid accessing native side with
            // possibly nulled native ptr. We do this since the previous parser could be
            // either the parent parser for an <include> (in which case it is still active,
            // with event type == START_TAG) or a parser from a separate inflate() call (in
            // which case the parser has been closed and would typically have event type
            // END_TAG or possibly END_DOCUMENT)
            try {
                if (previousParser.getEventType() == XmlPullParser.START_TAG) {
                    // Check if the parent parser is actually on an <include> tag,
                    // if so we need to inherit the parent context
                    return "include".equals(previousParser.getName());
                }
            } catch (XmlPullParserException e) {
            }
        }
        return false;
    }


    /**
     * Pops any outdated {@link XmlPullParser}s from the given stack.
     * @param xmlParserStack stack to purge
     * @return most recent {@link XmlPullParser} that is not outdated
     */
    private static @Nullable XmlPullParser popOutdatedAttrHolders(
            @NonNull Deque<WeakReference<XmlPullParser>> xmlParserStack) {
        while (!xmlParserStack.isEmpty()) {
            XmlPullParser parser = xmlParserStack.peek().get();
            if (isParserOutdated(parser)) {
                xmlParserStack.pop();
            } else {
                return parser;
            }
        }
        return null;
    }

    private static boolean isParserOutdated(@Nullable XmlPullParser parser) {
        try {
            return parser == null || (parser.getEventType() == XmlPullParser.END_TAG
                    || parser.getEventType() == XmlPullParser.END_DOCUMENT);
        } catch (XmlPullParserException e) {
            return true;
        }
    }
}
