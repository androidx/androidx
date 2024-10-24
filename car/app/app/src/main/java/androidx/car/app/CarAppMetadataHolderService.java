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

package androidx.car.app;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A placeholder service to avoid adding application-level metadata.
 *
 * <p>The service is only used to expose metadata defined in the library's manifest.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CarAppMetadataHolderService extends Service {
    @Override
    public @Nullable IBinder onBind(@NonNull Intent intent) {
        throw new UnsupportedOperationException();
    }

    private CarAppMetadataHolderService() {
    }

    /**
     * Returns the {@link ServiceInfo} for the declared {@link CarAppMetadataHolderService}.
     */
    @SuppressWarnings("deprecation") // GET_DISABLED_COMPONENTS, getServiceInfo
    public static @NonNull ServiceInfo getServiceInfo(@NonNull Context context) throws
            PackageManager.NameNotFoundException {
        int flags = PackageManager.GET_META_DATA;
        // The service is marked as disabled so we need to include the following flags.
        if (Build.VERSION.SDK_INT >= 24) {
            flags |= Api24Impl.getDisabledComponentFlag();
        } else {
            flags |= PackageManager.GET_DISABLED_COMPONENTS;
        }

        return context.getPackageManager().getServiceInfo(
                new ComponentName(context, CarAppMetadataHolderService.class), flags);
    }

    @RequiresApi(24)
    private static class Api24Impl {
        static int getDisabledComponentFlag() {
            return PackageManager.MATCH_DISABLED_COMPONENTS;
        }
    }
}
