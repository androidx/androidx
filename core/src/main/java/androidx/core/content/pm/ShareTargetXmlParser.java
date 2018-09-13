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

package androidx.core.content.pm;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

/**
 * Utility class to parse the list of {@link ShareTargetCompat} from app's Xml resource.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
class ShareTargetXmlParser {

    static final String TAG = "ShareTargetXmlParser";

    private static final String TAG_SHARE_TARGET = "share-target";
    private static final String ATTR_TARGET_CLASS = "targetClass";

    private static final String TAG_DATA = "data";
    private static final String ATTR_SCHEME = "scheme";
    private static final String ATTR_HOST = "host";
    private static final String ATTR_PORT = "port";
    private static final String ATTR_PATH = "path";
    private static final String ATTR_PATH_PATTERN = "pathPattern";
    private static final String ATTR_PATH_PREFIX = "pathPrefix";
    private static final String ATTR_MIME_TYPE = "mimeType";

    private static final String TAG_CATEGORY = "category";
    private static final String ATTR_NAME = "name";

    // List of share targets loaded from app's manifest. Will not change while the app is running.
    private static ArrayList<ShareTargetCompat> sShareTargets;

    @WorkerThread
    static ArrayList<ShareTargetCompat> getShareTargets(Context context) {
        if (sShareTargets == null) {
            sShareTargets = parseShareTargets(context);
        }
        return sShareTargets;
    }

    private ShareTargetXmlParser() {
        /* Hide the constructor */
    }

    private static ArrayList<ShareTargetCompat> parseShareTargets(Context context) {
        ArrayList<ShareTargetCompat> targets = new ArrayList<>();
        XmlResourceParser parser = getXmlResourceParser(context);

        try {
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG && parser.getName().equals(TAG_SHARE_TARGET)) {
                    ShareTargetCompat target = parseShareTarget(parser);
                    if (target != null) {
                        targets.add(target);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse the Xml resource: ", e);
        }

        parser.close();
        return targets;
    }

    private static XmlResourceParser getXmlResourceParser(Context context) {
        // TODO: Parse the main manifest to find the right Xml resource for share targets
        Resources res = context.getResources();
        return res.getXml(res.getIdentifier("shortcuts", "xml", context.getPackageName()));
    }

    private static ShareTargetCompat parseShareTarget(XmlResourceParser parser) throws Exception {
        String targetClass = getAttributeValue(parser, ATTR_TARGET_CLASS);
        ArrayList<ShareTargetCompat.TargetData> targetData = new ArrayList<>();
        ArrayList<String> categories = new ArrayList<>();

        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case TAG_DATA:
                        targetData.add(parseTargetData(parser));
                        break;
                    case TAG_CATEGORY:
                        categories.add(getAttributeValue(parser, ATTR_NAME));
                        break;
                }
            } else if (type == XmlPullParser.END_TAG && parser.getName().equals(TAG_SHARE_TARGET)) {
                break;
            }
        }
        if (targetData.isEmpty() || targetClass == null || categories.isEmpty()) {
            return null;
        }
        return new ShareTargetCompat(
                targetData.toArray(new ShareTargetCompat.TargetData[targetData.size()]),
                targetClass, categories.toArray(new String[categories.size()]));
    }

    private static ShareTargetCompat.TargetData parseTargetData(XmlResourceParser parser) {
        String scheme = getAttributeValue(parser, ATTR_SCHEME);
        String host = getAttributeValue(parser, ATTR_HOST);
        String port = getAttributeValue(parser, ATTR_PORT);
        String path = getAttributeValue(parser, ATTR_PATH);
        String pathPattern = getAttributeValue(parser, ATTR_PATH_PATTERN);
        String pathPrefix = getAttributeValue(parser, ATTR_PATH_PREFIX);
        String mimeType = getAttributeValue(parser, ATTR_MIME_TYPE);

        return new ShareTargetCompat.TargetData(scheme, host, port, path, pathPattern, pathPrefix,
                mimeType);
    }

    private static String getAttributeValue(XmlResourceParser parser, String attribute) {
        String value = parser.getAttributeValue("http://schemas.android.com/apk/res/android",
                attribute);
        if (value == null) {
            value = parser.getAttributeValue(null, attribute);
        }
        return value;
    }
}
