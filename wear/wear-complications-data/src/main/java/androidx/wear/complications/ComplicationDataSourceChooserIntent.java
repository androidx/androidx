/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.complications;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationProviderInfo;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Utilities to allow watch faces to launch the complication data source chooser.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ComplicationDataSourceChooserIntent {

    /**
     * The intent action used to open the complication data source chooser activity.
     */
    @SuppressWarnings("ActionValue")
    private static final String ACTION_CHOOSE_DATA_SOURCE =
            "com.google.android.clockwork.home.complications.ACTION_CHOOSE_PROVIDER";

    /**
     * Key for an extra used to provide the watch face component.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @SuppressWarnings("ActionValue")
    public static final String EXTRA_WATCH_FACE_COMPONENT_NAME =
            "android.support.wearable.complications.EXTRA_WATCH_FACE_COMPONENT_NAME";

    /**
     * Key for an extra holding a pending intent used to verify the caller.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @SuppressWarnings("ActionValue")
    public static final String EXTRA_PENDING_INTENT =
            "android.support.wearable.complications.EXTRA_PENDING_INTENT";

    /**
     * Key for an extra used to provide the watch face supported types.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @SuppressWarnings("ActionValue")
    public static final String EXTRA_SUPPORTED_TYPES =
            "android.support.wearable.complications.EXTRA_SUPPORTED_TYPES";

    /**
     * Key for an extra that holds the watch face complication id.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @SuppressWarnings("ActionValue")
    public static final String EXTRA_COMPLICATION_ID =
            "android.support.wearable.complications.EXTRA_COMPLICATION_ID";

    /**
     * Key for an extra that holds the watch face instance id.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String EXTRA_WATCHFACE_INSTANCE_ID =
            "androidx.wear.complications.EXTRA_WATCHFACE_INSTANCE_ID";

    /**
     * Key for an extra used to include details of the chosen complication data source in the
     * activity result returned by the complication data source chooser.
     *
     * @see #createComplicationDataSourceChooserIntent
     */
    @SuppressWarnings("ActionValue")
    public static final String EXTRA_PROVIDER_INFO =
            "android.support.wearable.complications.EXTRA_PROVIDER_INFO";

    /**
     * Returns an intent that may be used to start an activity to allow the user to select a
     * complication data source for the given complication. The activity will show a list of all
     * complication data source that can supply data of at least one of the {@code supportedTypes}.
     *
     * <p>This shouldn't be used by WatchFaces directly. Instead the androidx WatchFaceService calls
     * this as needed on your behalf.
     *
     * <p>This should only be used if the user has already granted the {@code
     * com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA} permission. In most cases
     * it will be easier to use {@link ComplicationHelperActivity} to perform the permission request
     * automatically if it is not already granted.
     *
     * <p>When the user chooses a complication data source, the configuration will be set up in the
     * complications system - the watch face does not need to do anything else.
     *
     * <p>The activity must be started using {@link Activity#startActivityForResult}, or else this
     * will not work. The result delivered back to your activity will have a result code of {@link
     * Activity#RESULT_OK RESULT_OK} if a complication data source was successfully set, or a result
     * code of {@link Activity#RESULT_CANCELED RESULT_CANCELED} if no complication data source was
     * set. In the case where a complication data source was set,
     * {@link ComplicationProviderInfo} for the chosen complication data source will be included
     * in the data intent of the result, as an extra with the key {@link #EXTRA_PROVIDER_INFO}.
     *
     * <p>The package of the calling Activity must match the package of the watch face, or this will
     * not work.
     *
     * @param watchFace the ComponentName of the WatchFaceService being configured.
     * @param watchFaceComplicationId the watch face's id for the complication being configured.
     *     This must match the id passed in when the watch face calls
     *     WatchFaceService.Engine#setActiveComplications.
     * @param supportedTypes the types supported by the complication, in decreasing order of
     *     preference. If a data source can supply data for more than one of these types, the type
     *     chosen will be whichever was specified first.
     * @see ComplicationHelperActivity
     */
    @NonNull
    public static Intent createComplicationDataSourceChooserIntent(
            @NonNull ComponentName watchFace,
            int watchFaceComplicationId,
            @NonNull @ComplicationData.ComplicationType int... supportedTypes) {
        Intent intent = new Intent(ACTION_CHOOSE_DATA_SOURCE);
        intent.putExtra(EXTRA_WATCH_FACE_COMPONENT_NAME, watchFace);
        intent.putExtra(EXTRA_COMPLICATION_ID, watchFaceComplicationId);
        intent.putExtra(EXTRA_SUPPORTED_TYPES, supportedTypes);
        return intent;
    }

    /**
     * Starts an activity to allow the user to select a data source for the given complication. The
     * activity will show a list of all data sources that can supply data of at least one of the
     * {@code supportedTypes}.
     *
     * <p>This should only be used if the user has already granted the {@code
     * com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA} permission. In most cases
     * it will be easier to use {@link ComplicationHelperActivity} to perform the permission request
     * automatically if it is not already granted.
     *
     * <p>This is intended for use when starting the chooser directly from the watch face. If the
     * chooser is being started from an Activity, use
     * {@link #createComplicationDataSourceChooserIntent} instead.
     *
     * <p>The package of the caller must match the package of the watch face, or this will not work.
     *
     * @param context used to start the activity.
     * @param watchFace the ComponentName of the WatchFaceService being configured.
     * @param watchFaceComplicationId the watch face's id for the complication being configured.
     *     This must match the id passed in when the watch face calls
     *     WatchFaceService.Engine#setActiveComplications.
     * @param supportedTypes the types supported by the complication, in decreasing order of
     *     preference. If a data source can supply data for more than one of these types, the type
     *     chosen will be whichever was specified first.
     */
    @SuppressLint("PendingIntentMutability")
    public static void startProviderChooserActivity(
            @NonNull Context context,
            @NonNull ComponentName watchFace,
            int watchFaceComplicationId,
            @NonNull @ComplicationData.ComplicationType int... supportedTypes) {
        Intent intent = createComplicationDataSourceChooserIntent(
                watchFace, watchFaceComplicationId, supportedTypes);
        // Add a placeholder PendingIntent to allow the UID to be checked.
        intent.putExtra(
                EXTRA_PENDING_INTENT, PendingIntent.getActivity(context, 0, new Intent(""), 0));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private ComplicationDataSourceChooserIntent() {}
}
