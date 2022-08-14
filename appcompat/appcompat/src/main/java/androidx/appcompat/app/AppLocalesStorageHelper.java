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

package androidx.appcompat.app;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

import static androidx.appcompat.app.AppCompatDelegate.getApplicationLocales;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.core.os.LocaleListCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * Helper class to manage storage of locales in app's persistent files.
 */
class AppLocalesStorageHelper {
    static final String APPLICATION_LOCALES_RECORD_FILE =
            "androidx.appcompat.app.AppCompatDelegate.application_locales_record_file";
    static final String LOCALE_RECORD_ATTRIBUTE_TAG = "application_locales";
    static final String LOCALE_RECORD_FILE_TAG = "locales";
    static final String APP_LOCALES_META_DATA_HOLDER_SERVICE_NAME = "androidx.appcompat.app"
            + ".AppLocalesMetadataHolderService";
    static final String TAG = "AppLocalesStorageHelper";

    private AppLocalesStorageHelper() {}

    /**
     * Returns app locales after reading from storage, fetched using the application context.
     */
    @NonNull
    static String readLocales(@NonNull Context context) {
        String appLocales = "";

        FileInputStream fis;
        try {
            fis = context.openFileInput(APPLICATION_LOCALES_RECORD_FILE);
        } catch (FileNotFoundException fnfe) {
            Log.w(TAG, "Reading app Locales : Locales record file not found: "
                    + APPLICATION_LOCALES_RECORD_FILE);
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
            Log.d(TAG,
                    "Reading app Locales : Locales read from file: "
                            + APPLICATION_LOCALES_RECORD_FILE + " ," + " appLocales: "
                            + appLocales);
        } else {
            context.deleteFile(APPLICATION_LOCALES_RECORD_FILE);
        }
        return appLocales;
    }

    /**
     * Stores the provided locales in internal app file, using the application context.
     */
    static void persistLocales(@NonNull Context context, @NonNull String locales) {
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
            Log.d(TAG, "Storing App Locales : app-locales: "
                    + locales + " persisted successfully.");
        } catch (Exception e) {
            Log.w(TAG, "Storing App Locales : Failed to persist app-locales: "
                    + locales, e);
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

    /**
     * Syncs app-specific locales from androidX to framework. This is used to maintain a smooth
     * transition for a device that updates from pre-T API versions to T.
     *
     * <p><b>NOTE:</b> This should only be called when auto-storage is opted-in. This method
     * uses the meta-data service provided during the opt-in and hence if the service is not found
     * this method will throw an error.</p>
     */
    static void syncLocalesToFramework(Context context) {
        ComponentName app_locales_component = new ComponentName(
                context, APP_LOCALES_META_DATA_HOLDER_SERVICE_NAME);

        if (context.getPackageManager().getComponentEnabledSetting(app_locales_component)
                != COMPONENT_ENABLED_STATE_ENABLED) {
            AppCompatDelegate.setAppContext(context);
            // ComponentEnabledSetting for the app component app_locales_component is used as a
            // marker to represent that the locales has been synced from AndroidX to framework
            // If this marker is found in ENABLED state then we do not need to sync again.
            if (getApplicationLocales().isEmpty()) {
                // We check if some locales are applied by the framework or not (this is done to
                // ensure that we don't overwrite newer locales set by the framework). If no
                // app-locales are found then we need to sync the app-specific locales from androidX
                // to framework.

                String appLocales = readLocales(context);
                // if locales are present in storage, call the setApplicationLocales() API. As the
                // API version is >= 33, this call will be directed to the framework API and the
                // locales will be persisted there.
                AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(appLocales));
            }
            // setting ComponentEnabledSetting for app component using
            // AppLocalesMetadataHolderService (used only for locales, thus minimizing
            // the chances of conflicts). Setting it as ENABLED marks the success of app-locales
            // sync from AndroidX to framework.
            // Flag DONT_KILL_APP indicates that you don't want to kill the app containing the
            // component.
            context.getPackageManager().setComponentEnabledSetting(app_locales_component,
                    COMPONENT_ENABLED_STATE_ENABLED, /* flags= */ DONT_KILL_APP);
        }
    }

    /**
     * Implementation of {@link java.util.concurrent.Executor} that executes each runnable on a
     * new thread.
     */
    static class ThreadPerTaskExecutor implements Executor {
        @Override
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }

    /**
     * Implementation of {@link java.util.concurrent.Executor} that executes runnables serially
     * by synchronizing the {@link Executor#execute(Runnable)} method and maintaining a tasks
     * queue.
     */
    static class SerialExecutor implements Executor {
        private final Object mLock = new Object();
        final Queue<Runnable> mTasks = new ArrayDeque<>();
        final Executor mExecutor;
        Runnable mActive;

        SerialExecutor(Executor executor) {
            this.mExecutor = executor;
        }

        @Override
        public void execute(final Runnable r) {
            synchronized (mLock) {
                mTasks.add(() -> {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                });
                if (mActive == null) {
                    scheduleNext();
                }
            }
        }

        protected void scheduleNext() {
            synchronized (mLock) {
                if ((mActive = mTasks.poll()) != null) {
                    mExecutor.execute(mActive);
                }
            }
        }
    }
}
