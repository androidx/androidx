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

    //  Constants for standalone apps. //

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

    //  Constants for customizing bridging of notifications from the phone to the wearable. //

    /**
     * We support specifying whether notifications should be bridged from the phone to the wearable
     * in the Wear app manifest file. Simply add a meta-data element to the Wear app manifest with
     * the name "com.google.android.wearable.notificationBridgeMode" and either the value
     * NO_BRIDGING or the value BRIDGING. If you choose not to update your Wear app manifest, then
     * notifications will be bridged by default from the phone to the wearable.
     *
     * <p>NO_BRIDGING means that phone notifications will not be bridged to the wearable if the
     * wearable app is installed.
     *
     * <p>BRIDGING means that phone notifications will be bridged to the wearable, unless they are
     * posted with
     * {@link android.app.Notification.Builder#setLocalOnly(boolean) setLocalOnly(true)}.
     *
     * <p>Example AndroidManifest.xml meta-data element for NO_BRIDGING:
     *
     * <pre class="prettyprint">{@code
     * <meta-data
     *     android:name="com.google.android.wearable.notificationBridgeMode"
     *     android:value="NO_BRIDGING" />
     * }</pre>
     *
     * <p>Example AndroidManifest.xml meta-data element for BRIDGING:
     *
     * <pre class="prettyprint">{@code
     * <meta-data
     *     android:name="com.google.android.wearable.notificationBridgeMode"
     *     android:value="BRIDGING" />
     * }</pre>
     */
    public static final String NOTIFICATION_BRIDGE_MODE_METADATA_NAME =
            "com.google.android.wearable.notificationBridgeMode";

    /**
     * The value of the notification bridge mode meta-data element in the case where the Wear app
     * wants notifications to be bridged from the phone to the wearable.
     */
    public static final String NOTIFICATION_BRIDGE_MODE_BRIDGING = "BRIDGING";

    /**
     * The value of the notification bridge mode meta-data element in the case where the Wear app
     * does not want notifications to be bridged from the phone to the wearable.
     */
    public static final String NOTIFICATION_BRIDGE_MODE_NO_BRIDGING = "NO_BRIDGING";

    //  Constants for watch face preview. //

    /**
     * The name of the meta-data element in the watch face service manifest declaration used
     * to assign a non-circular preview image to the watch face. The value should be set to
     * a drawable reference.
     *
     * <pre class="prettyprint">
     * &lt;meta-data
     *     android:name="com.google.android.wearable.watchface.preview"
     *     android:resource="@drawable/preview_face" /&gt;
     * </pre>
     */
    public static final String WATCH_FACE_PREVIEW_METADATA_NAME =
            "com.google.android.wearable.watchface.preview";

    /**
     * The name of the meta-data element in the watch face service manifest declaration used
     * to assign a circular preview image to the watch face. The value should be set to
     * a drawable reference.
     *
     * <pre class="prettyprint">
     * &lt;meta-data
     *     android:name="com.google.android.wearable.watchface.preview_circular"
     *     android:resource="@drawable/preview_face_circular" /&gt;
     * </pre>
     */
    public static final String WATCH_FACE_PREVIEW_CIRCULAR_METADATA_NAME =
            "com.google.android.wearable.watchface.preview_circular";

    // HELPER METHODS //

    /**
     * Determines whether a given context comes from a standalone app. This can be used as a proxy
     * to check if any given app is compatible with iOS Companion devices.
     *
     * @param context to be evaluated.
     * @return Whether a given context comes from a standalone app.
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

    /**
     * Determines whether a given context has notification bridging enabled.
     *
     * @param context to be evaluated.
     * @return Whether a given context has notification bridging enabled.
     */
    public static boolean isNotificationBridgingEnabled(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                return NOTIFICATION_BRIDGE_MODE_BRIDGING.equals(
                        appInfo.metaData.getString(NOTIFICATION_BRIDGE_MODE_METADATA_NAME));
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Do nothing
        }

        return true;
    }

    /**
     *
     * @param context to be evaluated.
     * @param circular Whether to return the circular or regular preview.
     *
     * @return an integer id representing the resource id of the requested drawable, or 0 if
     * no drawable was found.
     */
    public static int getPreviewDrawableResourceId(Context context, boolean circular) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                return circular
                        ?  appInfo.metaData.getInt(WATCH_FACE_PREVIEW_CIRCULAR_METADATA_NAME, 0)
                        :  appInfo.metaData.getInt(WATCH_FACE_PREVIEW_METADATA_NAME, 0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Do nothing
        }

        return 0;
    }

    private MetadataConstants() {}
}
