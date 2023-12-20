/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.mediarouter.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.List;

/**
 * A {@link BroadcastReceiver} class for enabling apps to get SystemRoutes using
 * {@link android.media.MediaRouter2}.
 */
@RestrictTo(LIBRARY)
final class SystemRoutingUsingMediaRouter2Receiver extends BroadcastReceiver {
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        // Do nothing for now.
    }

    /**
     * Checks whether the {@link SystemRoutingUsingMediaRouter2Receiver} is declared in the app's
     * manifest.
     */
    @RestrictTo(LIBRARY)
    public static boolean isDeclared(@NonNull Context applicationContext) {
        Intent queryIntent = new Intent(applicationContext,
                SystemRoutingUsingMediaRouter2Receiver.class);
        queryIntent.setPackage(applicationContext.getPackageName());
        PackageManager pm = applicationContext.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryBroadcastReceivers(queryIntent, 0);

        return resolveInfos.size() > 0;
    }
}
