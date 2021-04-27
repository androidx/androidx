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

package androidx.car.app.notification;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.car.app.CarContext.EXTRA_START_CAR_APP_BINDER_KEY;

import static java.util.Objects.requireNonNull;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.IStartCarApp;
import androidx.car.app.utils.LogTags;
import androidx.car.app.utils.RemoteUtils;

/**
 * Internal only receiver to handle the broadcast sent when a notification action is taken in
 * order to start the car app.
 *
 * <p>This is only used for Android Auto.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class CarAppNotificationBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = LogTags.TAG + ".NBR";

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        ComponentName appComponent =
                intent.getParcelableExtra(CarPendingIntent.COMPONENT_EXTRA_KEY);
        intent.removeExtra(CarPendingIntent.COMPONENT_EXTRA_KEY);
        intent.setComponent(appComponent);

        IBinder binder = null;
        Bundle extras = intent.getExtras();
        if (extras != null) {
            binder = extras.getBinder(EXTRA_START_CAR_APP_BINDER_KEY);
            extras.remove(EXTRA_START_CAR_APP_BINDER_KEY);
        }

        if (binder == null) {
            Log.e(TAG, "Notification intent missing expected extra: " + intent);
            return;
        }

        IStartCarApp startCarAppInterface = requireNonNull(IStartCarApp.Stub.asInterface(binder));

        RemoteUtils.dispatchCallToHost(
                "startCarApp from notification", () -> {
                    startCarAppInterface.startCarApp(intent);
                    return null;
                });
    }
}
