/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Helper class to manage storage of locales in app's persistent files.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class AppLocalesStorageHelper {
    static final String APPLICATION_LOCALES_RECORD_FILE =
            "androidx.appcompat.app.AppCompatDelegate.application_locales_record_file";
    static final String LOCALE_RECORD_ATTRIBUTE_TAG = "application_locales";
    static final String LOCALE_RECORD_FILE_TAG = "locales";

    static final String TAG = "AppLocalesStorageHelper";
    static final boolean DEBUG = false;

    private static final Object sAppLocaleStorageSync = new Object();

    private AppLocalesStorageHelper() {}

    /**
     * Returns app locales after reading from storage, fetched using the application context.
     */
    @NonNull
    public static String readLocales(@NonNull Context context) {
        synchronized (sAppLocaleStorageSync) {
            String appLocales = "";

            FileInputStream fis;
            try {
                fis = context.openFileInput(APPLICATION_LOCALES_RECORD_FILE);
            } catch (FileNotFoundException fnfe) {
                if (DEBUG) {
                    Log.d(TAG, "Reading app Locales : Locales record file not found: "
                            + APPLICATION_LOCALES_RECORD_FILE);
                }
                return appLocales;
            }
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis, "UTF-8");
                int type;
                int outerDepth = parser.getDepth();
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                    if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                        continue;
                    }

                    String tagName = parser.getName();
                    if (tagName.equals(LOCALE_RECORD_FILE_TAG)) {
                        appLocales =  parser.getAttributeValue(/*namespace= */ null,
                                LOCALE_RECORD_ATTRIBUTE_TAG);
                        break;
                    }
                }
            } catch (XmlPullParserException | IOException e) {
                Log.w(TAG,
                        "Reading app Locales : Unable to parse through file :"
                                + APPLICATION_LOCALES_RECORD_FILE);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        /* ignore */
                    }
                }
            }

            if (!appLocales.isEmpty()) {
                if (DEBUG) {
                    Log.d(TAG,
                            "Reading app Locales : Locales read from file: "
                                    + APPLICATION_LOCALES_RECORD_FILE + " ," + " appLocales: "
                                    + appLocales);
                }
            } else {
                context.deleteFile(APPLICATION_LOCALES_RECORD_FILE);
            }
            return appLocales;
        }
    }

    /**
     * Stores the provided locales in internal app file, using the application context.
     */
    public static void persistLocales(@NonNull Context context, @NonNull String locales) {
        synchronized (sAppLocaleStorageSync) {
            if (locales.equals("")) {
                context.deleteFile(APPLICATION_LOCALES_RECORD_FILE);
                return;
            }

            FileOutputStream fos;
            try {
                fos = context.openFileOutput(APPLICATION_LOCALES_RECORD_FILE, Context.MODE_PRIVATE);
            } catch (FileNotFoundException fnfe) {
                Log.w(TAG, String.format("Storing App Locales : FileNotFoundException: Cannot open "
                        + "file %s for writing ", APPLICATION_LOCALES_RECORD_FILE));
                return;
            }
            XmlSerializer serializer = Xml.newSerializer();
            try {
                serializer.setOutput(fos, /* encoding= */ null);
                serializer.startDocument("UTF-8", true);
                serializer.startTag(/* namespace= */ null, LOCALE_RECORD_FILE_TAG);
                serializer.attribute(/* namespace= */ null, LOCALE_RECORD_ATTRIBUTE_TAG, locales);
                serializer.endTag(/* namespace= */ null, LOCALE_RECORD_FILE_TAG);
                serializer.endDocument();
                if (DEBUG) {
                    Log.d(TAG, "Storing App Locales : app-locales: "
                            + locales + " persisted successfully.");
                }
            } catch (Exception e) {
                Log.w(TAG, "Storing App Locales : Failed to persist app-locales in storage ",
                        e);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        /* ignore */
                    }
                }
            }
        }
    }
}
