/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.samples.navigation.nav;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.samples.navigation.car.NavigationCarAppService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** A receiver to process notification actions to start the app into a deep linked screen. */
public class DeepLinkNotificationReceiver extends BroadcastReceiver {
    // Intent actions for notification actions in car and phone
    public static final String INTENT_ACTION_NAV_NOTIFICATION_OPEN_APP =
            "com.navigation.INTENT_ACTION_NAV_NOTIFICATION_OPEN_APP";

    private static final Set<String> SUPPORTED_ACTIONS =
            new HashSet<>(Arrays.asList(INTENT_ACTION_NAV_NOTIFICATION_OPEN_APP));

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        String intentAction = intent.getAction();
        if (SUPPORTED_ACTIONS.contains(intentAction)) {
            CarContext.startCarApp(
                    /* notificationIntent= */ intent,

                    // Once the car app opens, process the deep link action.
                    /* appIntent= */ new Intent(Intent.ACTION_VIEW)
                            .setComponent(new ComponentName(context, NavigationCarAppService.class))
                            .setData(NavigationCarAppService.createDeepLinkUri(intentAction)));
        }
    }
}
