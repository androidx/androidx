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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationProviderInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.app.ActivityCompat;
import androidx.wear.complications.data.ComplicationType;

import java.util.Collection;
import java.util.Objects;

/**
 * Activity to handle permission requests for complications.
 *
 * <p>This can be used to start the provider chooser, making a permission request if necessary, or
 * to just make a permission request, and update all active complications if the permission is
 * granted.
 *
 * <p>To use, add this activity to your app, and also add the {@code
 * com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA} permission.
 *
 * <p>Then, to start the provider chooser, use {@link #createProviderChooserHelperIntent} to obtain
 * an intent. If the permission has not yet been granted, the permission will be requested and the
 * provider chooser will only be started if the request is accepted by the user.
 *
 * <p>Or, to request the permission, for instance if {@link ComplicationData} of {@link
 * ComplicationData#TYPE_NO_PERMISSION TYPE_NO_PERMISSION} has been received and tapped on, use
 * {@link #createPermissionRequestHelperIntent}.
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.N)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressWarnings("ForbiddenSuperClass")
public final class ComplicationHelperActivity extends Activity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Whether to invoke a specified activity instead of the system's provider chooser.
     *
     * To be used in tests.
     */
    public static boolean useTestComplicationProviderChooserActivity = false;

    /**
     * Whether to skip th permission check and directly attempt to invoke the provider chooser.
     *
     * To be used in tests.
     */
    public static boolean skipPermissionCheck = false;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String ACTION_REQUEST_UPDATE_ALL_ACTIVE =
            "android.support.wearable.complications.ACTION_REQUEST_UPDATE_ALL_ACTIVE";

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String EXTRA_WATCH_FACE_COMPONENT =
            "android.support.wearable.complications.EXTRA_WATCH_FACE_COMPONENT";

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String ACTION_START_PROVIDER_CHOOSER =
            "android.support.wearable.complications.ACTION_START_PROVIDER_CHOOSER";

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String ACTION_PERMISSION_REQUEST_ONLY =
            "android.support.wearable.complications.ACTION_PERMISSION_REQUEST_ONLY";

    /** The package of the service that accepts provider requests. */
    private static final String UPDATE_REQUEST_RECEIVER_PACKAGE = "com.google.android.wearable.app";

    private static final int START_REQUEST_CODE_PROVIDER_CHOOSER = 1;
    private static final int PERMISSION_REQUEST_CODE_PROVIDER_CHOOSER = 1;
    private static final int PERMISSION_REQUEST_CODE_REQUEST_ONLY = 2;

    private static final String COMPLICATIONS_PERMISSION =
            "com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA";

    private static final String COMPLICATIONS_PERMISSION_PRIVILEGED =
            "com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA_PRIVILEGED";

    @Nullable
    private ComponentName mWatchFace;
    private int mWfComplicationId;
    @Nullable
    private Bundle mAdditionalExtras;
    @Nullable
    @ComplicationData.ComplicationType
    private int[] mTypes;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Translucent_NoTitleBar);

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        switch (Objects.requireNonNull(intent.getAction())) {
            case ACTION_START_PROVIDER_CHOOSER:
                mWatchFace =
                        intent.getParcelableExtra(
                                ProviderChooserIntent.EXTRA_WATCH_FACE_COMPONENT_NAME);
                mWfComplicationId =
                        intent.getIntExtra(ProviderChooserIntent.EXTRA_COMPLICATION_ID, 0);
                mTypes = intent.getIntArrayExtra(ProviderChooserIntent.EXTRA_SUPPORTED_TYPES);
                mAdditionalExtras = getAdditionalExtras(intent);
                if (checkPermission()) {
                    startProviderChooser();
                } else {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{COMPLICATIONS_PERMISSION},
                            PERMISSION_REQUEST_CODE_PROVIDER_CHOOSER);
                }
                break;
            case ACTION_PERMISSION_REQUEST_ONLY:
                mWatchFace =
                        intent.getParcelableExtra(
                                ProviderChooserIntent.EXTRA_WATCH_FACE_COMPONENT_NAME);
                if (checkPermission()) {
                    finish();
                } else {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{COMPLICATIONS_PERMISSION},
                            PERMISSION_REQUEST_CODE_REQUEST_ONLY);
                }
                break;
            default:
                throw new IllegalStateException("Unrecognised intent action.");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length == 0) {
            // Request was cancelled.
            finish();
            return;
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == PERMISSION_REQUEST_CODE_PROVIDER_CHOOSER) {
                startProviderChooser();
            } else {
                finish();
            }
            requestUpdateAll(mWatchFace);
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == START_REQUEST_CODE_PROVIDER_CHOOSER) {
            setResult(resultCode, data);
            finish();
        }
    }

    private boolean checkPermission() {
        return ActivityCompat.checkSelfPermission(this, COMPLICATIONS_PERMISSION_PRIVILEGED)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, COMPLICATIONS_PERMISSION)
                == PackageManager.PERMISSION_GRANTED
                || skipPermissionCheck;
    }

    /**
     * Returns an intent that may be used to start the provider chooser activity via the
     * ComplicationHelperActivity. This allows the required permission to be checked before the
     * provider chooser is displayed.
     *
     * <p>To use this, the ComplicationHelperActivity must be added to your app, and your app must
     * include the {@code com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA}
     * permission in its manifest.
     *
     * <p>The provider chooser activity will show a list of all providers that can supply data of at
     * least one of the {@code supportedTypes}.
     *
     * <p>When the user chooses a provider, the configuration will be set up in the complications
     * system - the watch face does not need to do anything else.
     *
     * <p>The activity may be started using {@link Activity#startActivityForResult}. The result
     * delivered back to your activity will have a result code of {@link Activity#RESULT_OK
     * RESULT_OK} if a provider was successfully set, or a result code of {@link
     * Activity#RESULT_CANCELED RESULT_CANCELED} if no provider was set. In the case where a
     * provider was set, {@link ComplicationProviderInfo} for the chosen provider will be included
     * in the data intent of the result, as an extra with the key
     * android.support.wearable.complications.EXTRA_PROVIDER_INFO.
     *
     * <p>The package of the calling app must match the package of the watch face, or this will not
     * work.
     *
     * <p>From android R onwards this API can only be called during an editing session.
     *
     * @param context                 context for the current app, that must contain a
     *                                ComplicationHelperActivity
     * @param watchFace               the ComponentName of the WatchFaceService being configured.
     * @param watchFaceComplicationId the watch face's id for the complication being configured.
     *                                This must match the id passed in when the watch face calls
     *                                WatchFaceService.Engine#setActiveComplications.
     * @param supportedTypes          the types supported by the complication, in decreasing
     *                                order of
     *                                preference. If a provider can supply data for more than one
     *                                of these types, the type
     *                                chosen will be whichever was specified first.
     * @param watchFaceInstanceId     The ID of the watchface being edited.
     */
    @NonNull
    public static Intent createProviderChooserHelperIntent(
            @NonNull Context context,
            @NonNull ComponentName watchFace,
            int watchFaceComplicationId,
            @NonNull Collection<ComplicationType> supportedTypes,
            @Nullable String watchFaceInstanceId) {
        Intent intent = new Intent(context, ComplicationHelperActivity.class);
        intent.setAction(ACTION_START_PROVIDER_CHOOSER);
        intent.putExtra(ProviderChooserIntent.EXTRA_WATCH_FACE_COMPONENT_NAME, watchFace);
        intent.putExtra(ProviderChooserIntent.EXTRA_COMPLICATION_ID, watchFaceComplicationId);
        if (watchFaceInstanceId != null) {
            intent.putExtra(ProviderChooserIntent.EXTRA_WATCHFACE_INSTANCE_ID, watchFaceInstanceId);
        }
        int[] wireSupportedTypes = new int[supportedTypes.size()];
        int i = 0;
        for (ComplicationType supportedType : supportedTypes) {
            wireSupportedTypes[i++] = supportedType.toWireComplicationType();
        }
        intent.putExtra(ProviderChooserIntent.EXTRA_SUPPORTED_TYPES, wireSupportedTypes);
        return intent;
    }

    /**
     * Returns an intent that may be used to start this activity in order to request the permission
     * required to receive complication data.
     *
     * <p>To use this, the ComplicationHelperActivity must be added to your app, and your app must
     * include the {@code com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA}
     * permission in its manifest.
     *
     * <p>If the current app has already been granted this permission, the activity will finish
     * immediately.
     *
     * <p>If the current app has not been granted this permission, a permission request will be
     * made. If the permission is granted by the user, an update of all complications on the current
     * watch face will be triggered. The provided {@code watchFace} must match the current watch
     * face for this to occur.
     *
     * @param context   context for the current app, that must contain a ComplicationHelperActivity
     * @param watchFace the ComponentName of the WatchFaceService for the current watch face
     */
    @NonNull
    public static Intent createPermissionRequestHelperIntent(
            @NonNull Context context, @NonNull ComponentName watchFace) {
        Intent intent = new Intent(context, ComplicationHelperActivity.class);
        intent.setAction(ACTION_PERMISSION_REQUEST_ONLY);
        intent.putExtra(ProviderChooserIntent.EXTRA_WATCH_FACE_COMPONENT_NAME, watchFace);
        return intent;
    }

    private void startProviderChooser() {
        Intent intent =
                ProviderChooserIntent.createProviderChooserIntent(
                        mWatchFace, mWfComplicationId, mTypes);
        // Add the extras that were provided to the ComplicationHelperActivity. This is done by
        // first taking the additional extras and adding to that anything that was set in the
        // chooser intent, and setting them back on the intent itself to avoid the additional
        // extras being able to override anything that was set by the chooser intent.
        Bundle extras = new Bundle(mAdditionalExtras);
        extras.putAll(intent.getExtras());
        intent.replaceExtras(extras);
        if (useTestComplicationProviderChooserActivity) {
            intent.setComponent(new ComponentName(
                    "androidx.wear.watchface.editor.test",
                    "androidx.wear.watchface.editor.TestComplicationProviderChooserActivity"));
        }
        startActivityForResult(intent, START_REQUEST_CODE_PROVIDER_CHOOSER);
    }

    /** Requests that the system update all active complications on the watch face. */
    private void requestUpdateAll(ComponentName watchFaceComponent) {
        Intent intent = new Intent(ACTION_REQUEST_UPDATE_ALL_ACTIVE);
        intent.setPackage(UPDATE_REQUEST_RECEIVER_PACKAGE);
        intent.putExtra(EXTRA_WATCH_FACE_COMPONENT, watchFaceComponent);
        // Add a placeholder PendingIntent to allow the UID to be checked.
        intent.putExtra(
                ProviderUpdateRequesterConstants.EXTRA_PENDING_INTENT,
                PendingIntent.getActivity(this, 0, new Intent(""), 0));
        sendBroadcast(intent);
    }

    /**
     * Returns any extras that were not handled by the activity itself.
     *
     * <p>These will be forwarded to the chooser activity.
     */
    private Bundle getAdditionalExtras(Intent intent) {
        Bundle extras = intent.getExtras();
        extras.remove(ProviderChooserIntent.EXTRA_WATCH_FACE_COMPONENT_NAME);
        extras.remove(ProviderChooserIntent.EXTRA_COMPLICATION_ID);
        extras.remove(ProviderChooserIntent.EXTRA_SUPPORTED_TYPES);
        return extras;
    }
}
