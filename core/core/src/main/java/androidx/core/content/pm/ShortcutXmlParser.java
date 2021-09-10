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

package androidx.core.content.pm;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses information of static shortcuts from shortcuts.xml
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class ShortcutXmlParser {

    private static final String TAG = "ShortcutXmlParser";

    private static final String META_DATA_APP_SHORTCUTS = "android.app.shortcuts";
    private static final String TAG_SHORTCUT = "shortcut";
    private static final String ATTR_SHORTCUT_ID = "shortcutId";

    // List of static shortcuts loaded from app's manifest. Will not change while the app is
    // running.
    private static volatile ArrayList<String> sShortcutIds;
    private static final Object GET_INSTANCE_LOCK = new Object();

    /**
     * Returns a singleton instance of list of ids of static shortcuts parsed from shortcuts.xml
     */
    @WorkerThread
    @NonNull
    public static List<String> getShortcutIds(@NonNull final Context context) {
        if (sShortcutIds == null) {
            synchronized (GET_INSTANCE_LOCK) {
                if (sShortcutIds == null) {
                    sShortcutIds = new ArrayList<>();
                    sShortcutIds.addAll(parseShortcutIds(context));
                }
            }
        }
        return sShortcutIds;
    }

    private ShortcutXmlParser() {
        /* Hide the constructor */
    }

    /**
     * Parses the shortcut ids of static shortcuts from the calling package.
     * Calling package is determined by {@link Context#getPackageName}
     * Returns a set of string which contains the ids of static shortcuts.
     */
    @NonNull
    private static Set<String> parseShortcutIds(@NonNull final Context context) {
        final Set<String> result = new HashSet<>();
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(context.getPackageName());

        final List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(
                mainIntent, PackageManager.GET_META_DATA);
        if (resolveInfos == null || resolveInfos.size() == 0) {
            return result;
        }
        try {
            for (ResolveInfo info : resolveInfos) {
                final ActivityInfo activityInfo = info.activityInfo;
                final Bundle metaData = activityInfo.metaData;
                if (metaData != null && metaData.containsKey(META_DATA_APP_SHORTCUTS)) {
                    try (XmlResourceParser parser = getXmlResourceParser(context, activityInfo)) {
                        result.addAll(parseShortcutIds(parser));
                    }
                }
            }
        } catch (Exception e) {
            // Resource ID mismatch may cause various runtime exceptions when parsing XMLs,
            // But we don't crash the device, so just swallow them.
            Log.e(TAG, "Failed to parse the Xml resource: ", e);
        }
        return result;
    }

    @NonNull
    private static XmlResourceParser getXmlResourceParser(Context context, ActivityInfo info) {
        final XmlResourceParser parser = info.loadXmlMetaData(context.getPackageManager(),
                META_DATA_APP_SHORTCUTS);
        if (parser == null) {
            throw new IllegalArgumentException("Failed to open " + META_DATA_APP_SHORTCUTS
                    + " meta-data resource of " + info.name);
        }

        return parser;
    }

    /**
     * Parses the shortcut ids from given XmlPullParser.
     */
    @VisibleForTesting
    @NonNull
    public static List<String> parseShortcutIds(@NonNull final XmlPullParser parser)
            throws IOException, XmlPullParserException {

        final List<String> result = new ArrayList<>(1);
        int type;

        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > 0)) {
            final int depth = parser.getDepth();
            final String tag = parser.getName();

            if ((type == XmlPullParser.START_TAG) && (depth == 2) && TAG_SHORTCUT.equals(tag)) {
                final String shortcutId = getAttributeValue(
                        parser, ATTR_SHORTCUT_ID);
                if (shortcutId == null) {
                    continue;
                }
                result.add(shortcutId);
            }
        }

        return result;
    }

    private static String getAttributeValue(XmlPullParser parser, String attribute) {
        String value = parser.getAttributeValue("http://schemas.android.com/apk/res/android",
                attribute);
        if (value == null) {
            value = parser.getAttributeValue(null, attribute);
        }
        return value;
    }
}
