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

package androidx.car.app.sample.showcase.common;

import android.content.pm.ApplicationInfo;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.validation.HostValidator;

/**
 * Entry point for the showcase app.
 *
 * <p>{@link CarAppService} is the main interface between the app and the car host. For more
 * details, see the <a href="https://developer.android.com/training/cars/navigation">Android for
 * Cars Library developer guide</a>.
 */
public final class ShowcaseService extends CarAppService {
    public static final String SHARED_PREF_KEY = "ShowcasePrefs";
    public static final String PRE_SEED_KEY = "PreSeed";

    // Intent actions for notification actions in car and phone
    public static final String INTENT_ACTION_NAVIGATE =
            "androidx.car.app.sample.showcase.INTENT_ACTION_PHONE";
    public static final String INTENT_ACTION_CALL =
            "androidx.car.app.sample.showcase.INTENT_ACTION_CANCEL_RESERVATION";
    public static final String INTENT_ACTION_NAV_NOTIFICATION_OPEN_APP =
            "androidx.car.app.sample.showcase.INTENT_ACTION_NAV_NOTIFICATION_OPEN_APP";

    /** Creates a deep link URI with the given deep link action. */
    @NonNull
    public static Uri createDeepLinkUri(@NonNull String deepLinkAction) {
        return Uri.fromParts(ShowcaseSession.URI_SCHEME, ShowcaseSession.URI_HOST, deepLinkAction);
    }

    @Override
    @NonNull
    public Session onCreateSession() {
        return new ShowcaseSession();
    }

    @NonNull
    @Override
    public HostValidator createHostValidator() {
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
        } else {
            return new HostValidator.Builder(getApplicationContext())
                    .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                    .build();
        }
    }
}
