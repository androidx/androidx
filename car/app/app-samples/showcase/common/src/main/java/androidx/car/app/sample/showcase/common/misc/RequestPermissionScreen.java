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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

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

    /**
     * Action which invalidates the template.
     *
     * <p>This can give the user a chance to revoke the permissions and then refresh will pickup
     * the permissions that need to be granted.
     */
    private final Action mRefreshAction = new Action.Builder()
            .setTitle("Refresh")
            .setBackgroundColor(CarColor.BLUE)
            .setOnClickListener(() -> invalidate())
            .build();

    public RequestPermissionScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        List<String> permissions = new ArrayList<>();
        String[] declaredPermissions = null;
        try {
            PackageInfo info =
                    getCarContext().getPackageManager().getPackageInfo(
                            getCarContext().getPackageName(),
                            PackageManager.GET_PERMISSIONS);
            declaredPermissions = info.requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            return new MessageTemplate.Builder("Package Not found.")
                    .setHeaderAction(Action.BACK)
                    .addAction(mRefreshAction)
                    .build();
        }

        if (declaredPermissions != null) {
            for (String declaredPermission : declaredPermissions) {
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
                    .setHeaderAction(Action.BACK)
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
                    (approved, rejected) -> CarToast.makeText(
                            getCarContext(),
                            String.format("Approved: %s Rejected: %s", approved, rejected),
                            CarToast.LENGTH_LONG).show());
            finish();
        });

        Action action = new Action.Builder()
                .setTitle("Grant Access")
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(listener)
                .build();

        return new LongMessageTemplate.Builder(message)
                .setTitle("Required Permissions")
                .addAction(action)
                .setHeaderAction(Action.BACK)
                .build();
    }
}
