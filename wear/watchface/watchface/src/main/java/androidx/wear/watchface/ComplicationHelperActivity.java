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

package androidx.wear.watchface;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationProviderInfo;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.app.ActivityCompat;
import androidx.wear.watchface.complications.ComplicationDataSourceUpdateRequesterConstants;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.style.UserStyleData;

import java.util.Collection;
import java.util.Objects;

/**
 * Activity to handle permission requests for complications.
 *
 * <p>This can be used to start the complication data source chooser, making a permission request if
 * necessary, or to just make a permission request, and update all active complications if the
 * permission is granted.
 *
 * <p>To use, add this activity to your app, and also add the {@code
 * com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA} permission.
 *
 * <p>Then, to start the complication data source chooser chooser, use {@link
 * #createComplicationDataSourceChooserHelperIntent} to obtain an intent. If the permission has not
 * yet been granted, the permission will be requested and the complication data source chooser
 * chooser will only be started if the request is accepted by the user.
 *
 * <p>Or, to request the permission, for instance if {@link ComplicationData} of {@link
 * ComplicationData#TYPE_NO_PERMISSION TYPE_NO_PERMISSION} has been received and tapped on, use
 * {@link #createPermissionRequestHelperIntent}.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ComplicationHelperActivity extends ComponentActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Whether to invoke a specified activity instead of the system's complication data source
     * chooser.
     *
     * <p>To be used in tests.
     */
    public static boolean useTestComplicationDataSourceChooserActivity = false;

    /**
     * Whether to skip th permission check and directly attempt to invoke the complication data
     * source chooser.
     *
     * <p>To be used in tests.
     */
    public static boolean skipPermissionCheck = false;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String ACTION_REQUEST_UPDATE_ALL_ACTIVE =
            "android.support.wearable.complications.ACTION_REQUEST_UPDATE_ALL_ACTIVE";

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String EXTRA_WATCH_FACE_COMPONENT =
            "android.support.wearable.complications.EXTRA_WATCH_FACE_COMPONENT";

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String ACTION_START_PROVIDER_CHOOSER =
            "android.support.wearable.complications.ACTION_START_PROVIDER_CHOOSER";

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String ACTION_PERMISSION_REQUEST_ONLY =
            "android.support.wearable.complications.ACTION_PERMISSION_REQUEST_ONLY";

    /** The package of the service that accepts complication data source requests. */
    private static final String UPDATE_REQUEST_RECEIVER_PACKAGE = "com.google.android.wearable.app";

    static final int PERMISSION_REQUEST_CODE_PROVIDER_CHOOSER = 1;
    static final int PERMISSION_REQUEST_CODE_PROVIDER_CHOOSER_NO_DENIED_INTENT = 2;
    static final int PERMISSION_REQUEST_CODE_REQUEST_ONLY = 3;
    static final int PERMISSION_REQUEST_CODE_REQUEST_ONLY_NO_DENIED_INTENT = 4;
    static final int START_PERMISSION_RATIONALE = 5;
    static final int START_REQUEST_CODE_PROVIDER_CHOOSER = 6;

    private static final String COMPLICATIONS_PERMISSION =
            "com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA";

    private static final String COMPLICATIONS_PERMISSION_PRIVILEGED =
            "com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA_PRIVILEGED";

    @Nullable ComponentName mWatchFace;
    int mWfComplicationId;
    @Nullable Bundle mAdditionalExtras;
    @Nullable @ComplicationData.ComplicationType int[] mTypes;
    Delegate mDelegate = new DelegateImpl(this);

    /** Allows the logic to be tested. */
    interface Delegate {
        boolean shouldShowRequestPermissionRationale();

        /** Returns true if permissions have been granted. */
        boolean checkPermission();

        void requestPermissions(int requestCode);

        /** Returns `true` if the activity was launched. */
        boolean launchComplicationRationaleActivity();

        void launchComplicationDeniedActivity();

        void startComplicationDataSourceChooser();

        /** Requests that the system update all active complications on the watch face. */
        void requestUpdateAll();
    }

    private static class DelegateImpl implements Delegate {
        final ComplicationHelperActivity mActivity;

        DelegateImpl(ComplicationHelperActivity activity) {
            this.mActivity = activity;
        }

        @Override
        public boolean shouldShowRequestPermissionRationale() {
            return mActivity.shouldShowRequestPermissionRationale(COMPLICATIONS_PERMISSION);
        }

        @Override
        public boolean checkPermission() {
            return ActivityCompat.checkSelfPermission(
                                    mActivity, COMPLICATIONS_PERMISSION_PRIVILEGED)
                            == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(mActivity, COMPLICATIONS_PERMISSION)
                            == PackageManager.PERMISSION_GRANTED
                    || skipPermissionCheck;
        }

        @Override
        public void requestPermissions(int requestCode) {
            ActivityCompat.requestPermissions(
                    mActivity, new String[] {COMPLICATIONS_PERMISSION}, requestCode);
        }

        @Override
        @SuppressWarnings("deprecation") // startActivityForResult
        public boolean launchComplicationRationaleActivity() {
            Intent complicationRationalIntent =
                    mActivity
                            .getIntent()
                            .getParcelableExtra(
                                    ComplicationDataSourceChooserIntent
                                            .EXTRA_COMPLICATION_RATIONALE);

            if (complicationRationalIntent != null) {
                mActivity.startActivityForResult(
                        complicationRationalIntent, START_PERMISSION_RATIONALE);
                return true;
            } else {
                return false;
            }
        }

        @Override
        @SuppressWarnings("deprecation")
        public void launchComplicationDeniedActivity() {
            Intent complicationDeniedIntent =
                    mActivity
                            .getIntent()
                            .getParcelableExtra(
                                    ComplicationDataSourceChooserIntent.EXTRA_COMPLICATION_DENIED);

            if (complicationDeniedIntent != null) {
                mActivity.startActivity(complicationDeniedIntent);
            }
        }

        @Override
        @SuppressWarnings("deprecation") // startActivityForResult
        public void startComplicationDataSourceChooser() {
            Intent intent =
                    ComplicationDataSourceChooserIntent.createComplicationDataSourceChooserIntent(
                            mActivity.mWatchFace, mActivity.mWfComplicationId, mActivity.mTypes);
            // Add the extras that were provided to the ComplicationHelperActivity. This is done by
            // first taking the additional extras and adding to that anything that was set in the
            // chooser intent, and setting them back on the intent itself to avoid the additional
            // extras being able to override anything that was set by the chooser intent.
            Bundle extras = new Bundle(mActivity.mAdditionalExtras);
            extras.putAll(intent.getExtras());
            intent.replaceExtras(extras);
            if (useTestComplicationDataSourceChooserActivity) {
                intent.setComponent(
                        new ComponentName(
                                "androidx.wear.watchface.editor.test",
                                "androidx.wear.watchface.editor"
                                        + ".TestComplicationDataSourceChooserActivity"));
            }
            mActivity.startActivityForResult(intent, START_REQUEST_CODE_PROVIDER_CHOOSER);
        }

        @Override
        public void requestUpdateAll() {
            Intent intent = new Intent(ACTION_REQUEST_UPDATE_ALL_ACTIVE);
            intent.setPackage(UPDATE_REQUEST_RECEIVER_PACKAGE);
            intent.putExtra(EXTRA_WATCH_FACE_COMPONENT, mActivity.mWatchFace);
            // Add a placeholder PendingIntent to allow the UID to be checked.
            intent.putExtra(
                    ComplicationDataSourceUpdateRequesterConstants.EXTRA_PENDING_INTENT,
                    PendingIntent.getActivity(
                            mActivity, 0, new Intent(""), PendingIntent.FLAG_IMMUTABLE));
            mActivity.sendBroadcast(intent);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        start(true);
    }

    @SuppressWarnings("deprecation")
    void start(boolean shouldShowRequestPermissionRationale) {
        if (shouldShowRequestPermissionRationale
                && mDelegate.shouldShowRequestPermissionRationale()) {
            if (mDelegate.launchComplicationRationaleActivity()) {
                return;
            }
        }

        Intent intent = getIntent();

        switch (Objects.requireNonNull(intent.getAction())) {
            case ACTION_START_PROVIDER_CHOOSER:
                mWatchFace =
                        intent.getParcelableExtra(
                                ComplicationDataSourceChooserIntent
                                        .EXTRA_WATCH_FACE_COMPONENT_NAME);
                mWfComplicationId =
                        intent.getIntExtra(
                                ComplicationDataSourceChooserIntent.EXTRA_COMPLICATION_ID, 0);
                mTypes =
                        intent.getIntArrayExtra(
                                ComplicationDataSourceChooserIntent.EXTRA_SUPPORTED_TYPES);
                mAdditionalExtras = getAdditionalExtras(intent);
                if (mDelegate.checkPermission()) {
                    mDelegate.startComplicationDataSourceChooser();
                } else {
                    int requestCode =
                            shouldShowRequestPermissionRationale
                                    ? PERMISSION_REQUEST_CODE_PROVIDER_CHOOSER
                                    : PERMISSION_REQUEST_CODE_PROVIDER_CHOOSER_NO_DENIED_INTENT;
                    mDelegate.requestPermissions(requestCode);
                }
                break;
            case ACTION_PERMISSION_REQUEST_ONLY:
                mWatchFace =
                        intent.getParcelableExtra(
                                ComplicationDataSourceChooserIntent
                                        .EXTRA_WATCH_FACE_COMPONENT_NAME);
                if (mDelegate.checkPermission()) {
                    finish();
                } else {
                    mDelegate.requestPermissions(
                            shouldShowRequestPermissionRationale
                                    ? PERMISSION_REQUEST_CODE_REQUEST_ONLY
                                    : PERMISSION_REQUEST_CODE_REQUEST_ONLY_NO_DENIED_INTENT);
                }
                break;
            default:
                throw new IllegalStateException("Unrecognised intent action.");
        }
    }

    @Override
    @SuppressWarnings("deprecation") //TODO: Use ActivityResultContract
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0) {
            // Request was cancelled.
            finish();
            return;
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == PERMISSION_REQUEST_CODE_PROVIDER_CHOOSER
                    || requestCode == PERMISSION_REQUEST_CODE_PROVIDER_CHOOSER_NO_DENIED_INTENT) {
                mDelegate.startComplicationDataSourceChooser();
            } else {
                finish();
            }
            mDelegate.requestUpdateAll();
        } else {
            if (requestCode == PERMISSION_REQUEST_CODE_PROVIDER_CHOOSER
                    || requestCode == PERMISSION_REQUEST_CODE_REQUEST_ONLY) {
                mDelegate.launchComplicationDeniedActivity();
            }
            finish();
        }
    }

    @Override
    @SuppressWarnings("deprecation") //TODO: Use ActivityResultContract
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case START_REQUEST_CODE_PROVIDER_CHOOSER:
                setResult(resultCode, data);
                finish();
                break;

            case START_PERMISSION_RATIONALE:
                start(false);
                break;
        }
    }

    /**
     * Returns an intent that may be used to start the complication data source chooser activity via
     * the ComplicationHelperActivity. This allows the required permission to be checked before the
     * complication data source chooser is displayed.
     *
     * <p>To use this, the ComplicationHelperActivity must be added to your app, and your app must
     * include the {@code com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA}
     * permission in its manifest.
     *
     * <p>The complication data source chooser activity will show a list of all complication data
     * sources that can supply data of at least one of the {@code supportedTypes}.
     *
     * <p>When the user chooses a complication data source, the configuration will be set up in the
     * complications system - the watch face does not need to do anything else.
     *
     * <p>The activity may be started using {@link Activity#startActivityForResult}. The result
     * delivered back to your activity will have a result code of {@link Activity#RESULT_OK
     * RESULT_OK} if a complication data source was successfully set, or a result code of {@link
     * Activity#RESULT_CANCELED RESULT_CANCELED} if no complication data source was set. In the case
     * where a complication data source was set, {@link ComplicationProviderInfo} for the chosen
     * complication data source will be included in the data intent of the result, as an extra with
     * the key android.support.wearable.complications.EXTRA_PROVIDER_INFO.
     *
     * <p>The package of the calling app must match the package of the watch face, or this will not
     * work.
     *
     * <p>From android R onwards this API can only be called during an editing session.
     *
     * @param context context for the current app, that must contain a ComplicationHelperActivity
     * @param watchFace the ComponentName of the WatchFaceService being configured.
     * @param watchFaceComplicationId the watch face's id for the complication being configured.
     *     This must match the id passed in when the watch face calls
     *     WatchFaceService.Engine#setActiveComplications.
     * @param supportedTypes the types supported by the complication, in decreasing order of
     *     preference. If a complication data source can supply data for more than one of these
     *     types, the type chosen will be whichever was specified first.
     * @param watchFaceInstanceId The ID of the watchface being edited.
     * @param complicationDenied Intent to launch the complication permission denied dialog.
     * @param complicationRationale Intent to launch the complication permission rationale dialog.
     */
    @NonNull
    public static Intent createComplicationDataSourceChooserHelperIntent(
            @NonNull Context context,
            @NonNull ComponentName watchFace,
            int watchFaceComplicationId,
            @NonNull Collection<ComplicationType> supportedTypes,
            @Nullable String watchFaceInstanceId,
            @Nullable Intent complicationDenied,
            @Nullable Intent complicationRationale,
            @Nullable UserStyleData userStyleData) {
        Intent intent = new Intent(context, ComplicationHelperActivity.class);
        intent.setAction(ACTION_START_PROVIDER_CHOOSER);
        intent.putExtra(
                ComplicationDataSourceChooserIntent.EXTRA_WATCH_FACE_COMPONENT_NAME, watchFace);
        intent.putExtra(
                ComplicationDataSourceChooserIntent.EXTRA_COMPLICATION_ID, watchFaceComplicationId);
        if (watchFaceInstanceId != null) {
            intent.putExtra(
                    ComplicationDataSourceChooserIntent.EXTRA_WATCHFACE_INSTANCE_ID,
                    watchFaceInstanceId);
        }
        if (complicationDenied != null) {
            intent.putExtra(
                    ComplicationDataSourceChooserIntent.EXTRA_COMPLICATION_DENIED,
                    complicationDenied);
        }
        if (complicationRationale != null) {
            intent.putExtra(
                    ComplicationDataSourceChooserIntent.EXTRA_COMPLICATION_RATIONALE,
                    complicationRationale);
        }
        if (userStyleData != null) {
            intent.putExtra(
                    ComplicationDataSourceChooserIntent.EXTRA_USER_STYLE,
                    userStyleData.toWireFormat());
        }
        int[] wireSupportedTypes = new int[supportedTypes.size()];
        int i = 0;
        for (ComplicationType supportedType : supportedTypes) {
            wireSupportedTypes[i++] = supportedType.toWireComplicationType();
        }
        intent.putExtra(
                ComplicationDataSourceChooserIntent.EXTRA_SUPPORTED_TYPES, wireSupportedTypes);
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
     * @param context context for the current app, that must contain a ComplicationHelperActivity
     * @param watchFace the ComponentName of the WatchFaceService for the current watch face
     * @param complicationDenied Intent to launch the complication permission denied dialog.
     * @param complicationRationale Intent to launch the complication permission rationale dialog.
     */
    @NonNull
    public static Intent createPermissionRequestHelperIntent(
            @NonNull Context context,
            @NonNull ComponentName watchFace,
            @Nullable Intent complicationDenied,
            @Nullable Intent complicationRationale) {
        Intent intent = new Intent(context, ComplicationHelperActivity.class);
        intent.setAction(ACTION_PERMISSION_REQUEST_ONLY);
        intent.putExtra(
                ComplicationDataSourceChooserIntent.EXTRA_WATCH_FACE_COMPONENT_NAME, watchFace);
        if (complicationDenied != null) {
            intent.putExtra(
                    ComplicationDataSourceChooserIntent.EXTRA_COMPLICATION_DENIED,
                    complicationDenied);
        }
        if (complicationRationale != null) {
            intent.putExtra(
                    ComplicationDataSourceChooserIntent.EXTRA_COMPLICATION_RATIONALE,
                    complicationRationale);
        }
        return intent;
    }

    /**
     * Returns any extras that were not handled by the activity itself.
     *
     * <p>These will be forwarded to the chooser activity.
     */
    private Bundle getAdditionalExtras(Intent intent) {
        Bundle extras = intent.getExtras();
        extras.remove(ComplicationDataSourceChooserIntent.EXTRA_WATCH_FACE_COMPONENT_NAME);
        extras.remove(ComplicationDataSourceChooserIntent.EXTRA_COMPLICATION_ID);
        extras.remove(ComplicationDataSourceChooserIntent.EXTRA_SUPPORTED_TYPES);
        return extras;
    }
}
