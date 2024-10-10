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

package androidx.car.app.sample.showcase.common.screens.userinteractions;

import static android.content.pm.PackageManager.FEATURE_AUTOMOTIVE;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;

import androidx.car.app.CarAppPermission;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.Header;
import androidx.car.app.model.LongMessageTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.location.LocationManagerCompat;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A screen to show a request for a runtime permission from the user.
 *
 * <p>Scans through the possible dangerous permissions and shows which ones have not been
 * granted in the message. Clicking on the action button will launch the permission request on
 * the phone.
 *
 * <p>If all permissions are granted, corresponding message is displayed with a refresh button which
 * will scan again when clicked.
 */
public class RequestPermissionScreen extends Screen {
    private static final String TAG = "showcase";

    /**
     * This field can and should be removed once b/192386096 and/or b/192385602 have been resolved.
     * Currently it is not possible to know the level of the screen stack and determine the
     * header action according to that. A boolean flag is added to determine that temporarily.
     */
    private final boolean mPreSeedMode;

    /**
     * Action which invalidates the template.
     *
     * <p>This can give the user a chance to revoke the permissions and then refresh will pickup
     * the permissions that need to be granted.
     */
    private final Action mRefreshAction = new Action.Builder()
            .setTitle(getCarContext().getString(R.string.refresh_action_title))
            .setBackgroundColor(CarColor.BLUE)
            .setOnClickListener(this::invalidate)
            .build();

    public RequestPermissionScreen(@NonNull CarContext carContext) {
        this(carContext, false);
    }

    public RequestPermissionScreen(@NonNull CarContext carContext, boolean preSeedMode) {
        super(carContext);
        this.mPreSeedMode = preSeedMode;
    }

    @Override
    public @NonNull Template onGetTemplate() {
        Action headerAction = mPreSeedMode ? Action.APP_ICON : Action.BACK;

        List<String> permissions;
        try {
            permissions = findMissingPermissions();
        } catch (PackageManager.NameNotFoundException e) {
            // Permission lookup failed. Show error.
            return new MessageTemplate.Builder(
                    getCarContext().getString(R.string.package_not_found_error_msg))
                    .setHeader(new Header.Builder().setStartHeaderAction(headerAction)
                            .build())
                    .addAction(mRefreshAction)
                    .build();
        }

        if (permissions.isEmpty()) {
            // No permissions needed. Prompt the user to exit.
            return new MessageTemplate.Builder(
                    getCarContext().getString(R.string.permissions_granted_msg))
                    .setHeader(new Header.Builder().setStartHeaderAction(headerAction)
                            .build())
                    .addAction(
                            new Action.Builder()
                                    .setTitle(
                                            getCarContext().getString(R.string.close_action_title))
                                    .setOnClickListener(this::finish)
                                    .build())
                    .build();
        }
        boolean needsLocationPermission = needsLocationPermission();

        return createPermissionPromptTemplate(permissions, needsLocationPermission, headerAction);
    }

    private Template createPermissionPromptTemplate(
            List<String> permissions, boolean needsLocationPermission, Action headerAction) {
        LongMessageTemplate.Builder builder =
                new LongMessageTemplate.Builder(
                        createRequiredPermissionsMessage(permissions, needsLocationPermission))
                        .setTitle(getCarContext().getString(R.string.required_permissions_title))
                        .addAction(createGrantPermissionsButton(permissions))
                        .setHeaderAction(headerAction);

        if (needsLocationPermission) {
            builder.addAction(createGrantLocationPermissionButton());
        }

        return builder.build();
    }

    private String createRequiredPermissionsMessage(
            List<String> permissions, boolean needsLocationPermission) {
        StringBuilder message = new StringBuilder()
                .append(getCarContext().getString(R.string.needs_access_msg_prefix));
        for (String permission : permissions) {
            message.append(permission);
            message.append("\n");
        }

        if (needsLocationPermission) {
            message.append(
                    getCarContext().getString(R.string.enable_location_permission_on_device_msg));
            message.append("\n");
        }

        return message.toString();
    }

    private List<String> findMissingPermissions() throws PackageManager.NameNotFoundException {
        // Possible NameNotFoundException
        PackageInfo info =
                getCarContext()
                        .getPackageManager()
                        .getPackageInfo(getCarContext().getPackageName(),
                                PackageManager.GET_PERMISSIONS);

        String[] declaredPermissions = info.requestedPermissions;
        if (declaredPermissions == null) {
            Log.d(TAG, "No permissions found in manifest");
            return new ArrayList<>();
        }

        List<String> missingPermissions = new ArrayList<>();
        for (String permission : declaredPermissions) {
            if (isAppHostPermission(permission)) {
                // Don't include permissions against the car app host as they are all normal but
                // show up as ungranted by the system.
                Log.d(
                        TAG, String.format(Locale.US, "Permission ignored (belongs to host): %s",
                                permission));
                continue;
            }

            if (isPermissionGranted(permission)) {
                Log.d(
                        TAG, String.format(Locale.US, "Permission ignored (already granted): %s",
                                permission));
                continue;
            }

            Log.d(TAG, String.format(Locale.US, "Found missing permission: %s", permission));
            missingPermissions.add(permission);
        }

        return missingPermissions;
    }

    private boolean isAppHostPermission(String permission) {
        return permission.startsWith("androidx.car.app");
    }

    private boolean isPermissionGranted(String permission) {
        try {
            CarAppPermission.checkHasPermission(getCarContext(), permission);
        } catch (SecurityException e) {
            // Permission not granted
            return false;
        }

        // Permission already granted
        return true;
    }

    private boolean needsLocationPermission() {
        LocationManager locationManager =
                (LocationManager) getCarContext().getSystemService(Context.LOCATION_SERVICE);
        return !LocationManagerCompat.isLocationEnabled(locationManager);
    }

    private Action createGrantLocationPermissionButton() {
        return new Action.Builder()
                .setTitle(getCarContext().getString(R.string.enable_location_action_title))
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(ParkedOnlyOnClickListener.create(this::grantLocationPermission))
                .build();
    }

    private void grantLocationPermission() {
        getCarContext()
                .startActivity(
                        new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

        invalidate();

        promptAapUsers(getCarContext().getString(R.string.enable_location_permission_on_phone_msg));
    }

    private Action createGrantPermissionsButton(List<String> permissions) {
        OnClickListener listener =
                ParkedOnlyOnClickListener.create(() -> requestPermissions(permissions));

        return new Action.Builder()
                .setTitle(getCarContext().getString(R.string.grant_access_action_title))
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(listener)
                .build();
    }

    private void requestPermissions(List<String> permissions) {
        getCarContext()
                .requestPermissions(
                        permissions,
                        (approved, rejected) -> {
                            // Log debug info
                            CarToast.makeText(
                                            getCarContext(),
                                            String.format(
                                                    Locale.US, "Approved: %d Rejected: %d",
                                                    approved.size(),
                                                    rejected.size()),
                                            CarToast.LENGTH_LONG)
                                    .show();
                            Log.i(TAG,
                                    String.format(Locale.US, "Approved: %s Rejected: %s", approved,
                                            rejected));

                            // Update the template
                            invalidate();
                        });

        // Prompt AAP users to look at their phone, to grant permissions.
        promptAapUsers(getCarContext().getString(R.string.phone_screen_permission_msg));
    }

    private void promptAapUsers(String message) {
        if (getCarContext().getPackageManager().hasSystemFeature(FEATURE_AUTOMOTIVE)) {
            return;
        }

        CarToast.makeText(getCarContext(), message, CarToast.LENGTH_LONG).show();
    }
}
