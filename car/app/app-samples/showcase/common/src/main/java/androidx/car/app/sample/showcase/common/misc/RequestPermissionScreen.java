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

package androidx.car.app.sample.showcase.common.misc;

import static android.content.pm.PackageManager.FEATURE_AUTOMOTIVE;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppPermission;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.LongMessageTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Template;
import androidx.core.location.LocationManagerCompat;

import java.util.ArrayList;
import java.util.List;

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
    // This field can and should be removed once b/192386096 and/or b/192385602 have been resolved.
    private final boolean mPreSeedMode;

    /**
     * Action which invalidates the template.
     *
     * <p>This can give the user a chance to revoke the permissions and then refresh will pickup
     * the permissions that need to be granted.
     */
    private final Action mRefreshAction = new Action.Builder()
            .setTitle("Refresh")
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

    @NonNull
    @Override
    public Template onGetTemplate() {
        final Action headerAction = mPreSeedMode ? Action.APP_ICON : Action.BACK;
        List<String> permissions = new ArrayList<>();
        String[] declaredPermissions;
        try {
            PackageInfo info =
                    getCarContext().getPackageManager().getPackageInfo(
                            getCarContext().getPackageName(),
                            PackageManager.GET_PERMISSIONS);
            declaredPermissions = info.requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            return new MessageTemplate.Builder("Package Not found.")
                    .setHeaderAction(headerAction)
                    .addAction(mRefreshAction)
                    .build();
        }

        if (declaredPermissions != null) {
            for (String declaredPermission : declaredPermissions) {
                // Don't include permissions against the car app host as they are all normal but
                // show up as ungranted by the system.
                if (declaredPermission.startsWith("androidx.car.app")) {
                    continue;
                }
                try {
                    CarAppPermission.checkHasPermission(getCarContext(), declaredPermission);
                } catch (SecurityException e) {
                    permissions.add(declaredPermission);
                }
            }
        }
        if (permissions.isEmpty()) {
            return new MessageTemplate.Builder("All permissions have been granted. Please "
                    + "revoke permissions from Settings.")
                    .setHeaderAction(headerAction)
                    .addAction(new Action.Builder()
                            .setTitle("Close")
                            .setOnClickListener(this::finish)
                            .build())
                    .build();
        }

        StringBuilder message = new StringBuilder()
                .append("The app needs access to the following permissions:\n");
        for (String permission : permissions) {
            message.append(permission);
            message.append("\n");
        }

        OnClickListener listener = ParkedOnlyOnClickListener.create(() -> {
            getCarContext().requestPermissions(
                    permissions,
                    (approved, rejected) -> {
                        CarToast.makeText(
                                getCarContext(),
                                String.format("Approved: %s Rejected: %s", approved, rejected),
                                CarToast.LENGTH_LONG).show();
                    });
            if (!getCarContext().getPackageManager().hasSystemFeature(FEATURE_AUTOMOTIVE)) {
                CarToast.makeText(getCarContext(), "Grant Permission on the phone screen",
                        CarToast.LENGTH_LONG).show();
            }
        });

        Action action = new Action.Builder()
                .setTitle("Grant Access")
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(listener)
                .build();


        Action action2 = null;
        LocationManager locationManager =
                (LocationManager) getCarContext().getSystemService(Context.LOCATION_SERVICE);
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            message.append("Enable Location Permissions on device\n");
            action2 = new Action.Builder()
                    .setTitle("Enable Location")
                    .setBackgroundColor(CarColor.BLUE)
                    .setOnClickListener(ParkedOnlyOnClickListener.create(() -> {
                        getCarContext().startActivity(
                                new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK));
                        if (!getCarContext().getPackageManager().hasSystemFeature(
                                FEATURE_AUTOMOTIVE)) {
                            CarToast.makeText(getCarContext(), "Enable location on the phone "
                                            + "screen",
                                    CarToast.LENGTH_LONG).show();
                        }
                    }))
                    .build();
        }


        LongMessageTemplate.Builder builder = new LongMessageTemplate.Builder(message)
                .setTitle("Required Permissions")
                .addAction(action)
                .setHeaderAction(headerAction);

        if (action2 != null) {
            builder.addAction(action2);
        }

        return builder.build();
    }
}
