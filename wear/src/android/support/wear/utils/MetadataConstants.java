/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.support.wear.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Constants for android wear apps which are related to manifest meta-data.
 */
@TargetApi(Build.VERSION_CODES.N)
public class MetadataConstants {
    /**
     * The name of the meta-data element in the Wear app manifest for specifying whether this app
     * does not require a companion phone app. The value should be set to "true" or "false".
     * <p>
     * <p>Wear apps that do not require a phone side companion app to function can declare this in
     * their AndroidManifest.xml file by setting the standalone meta-data element to true as shown
     * in the following example. If this value is true, all users can discover this app regardless
     * of what phone they have. If this value is false (or not set), only users with compatible
     * Android phones can discover this app.
     * <p>
     * <pre class="prettyprint">{@code
     * <meta-data
     * android:name="com.google.android.wearable.standalone"
     * android:value="true" />
     * }</pre>
     */
    public static final String STANDALONE_METADATA_NAME = "com.google.android.wearable.standalone";

    /**
     * Determines whether a given context comes from a standalone app. This can be used as a proxy
     * to check if any given app is compatible with iOS Companion devices.
     *
     * @param context to be evaluated.
     * @return Whether a given context comes from a standalone app
     */
    public static boolean isStandalone(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                return appInfo.metaData.getBoolean(STANDALONE_METADATA_NAME);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Do nothing
        }

        return false;
    }

    private MetadataConstants() {
    }
}
