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

package androidx.core.service.quicksettings;

import static android.os.Build.VERSION.SDK_INT;

import android.app.PendingIntent;
import android.content.Intent;
import android.service.quicksettings.TileService;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * A helper for accessing {@link TileService} API methods.
 */
public class TileServiceCompat {

    private static TileServiceWrapper sTileServiceWrapper;

    /**
     * Calls the correct {@link TileService}#startActivityAndCollapse() method
     * depending on the app's targeted {@link android.os.Build.VERSION_CODES}.
     */
    public static void startActivityAndCollapse(@NonNull TileService tileService,
            @NonNull PendingIntentActivityWrapper wrapper) {
        if (SDK_INT >= 34) {
            if (sTileServiceWrapper != null) {
                sTileServiceWrapper.startActivityAndCollapse(wrapper.getPendingIntent());
            } else {
                Api34Impl.startActivityAndCollapse(tileService, wrapper.getPendingIntent());
            }
        } else if (SDK_INT >= 24) {
            if (sTileServiceWrapper != null) {
                sTileServiceWrapper.startActivityAndCollapse(wrapper.getIntent());
            } else {
                Api24Impl.startActivityAndCollapse(tileService, wrapper.getIntent());
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void setTileServiceWrapper(@NonNull TileServiceWrapper serviceWrapper) {
        sTileServiceWrapper = serviceWrapper;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void clearTileServiceWrapper() {
        sTileServiceWrapper = null;
    }

    @RequiresApi(34)
    private static class Api34Impl {
        @DoNotInline
        static void startActivityAndCollapse(TileService service,
                PendingIntent pendingIntent) {
            service.startActivityAndCollapse(pendingIntent);
        }
    }

    @RequiresApi(24)
    private static class Api24Impl {
        @DoNotInline
        static void startActivityAndCollapse(TileService service, Intent intent) {
            service.startActivityAndCollapse(intent);
        }
    }

    private TileServiceCompat() {
    }

    interface TileServiceWrapper {
        void startActivityAndCollapse(PendingIntent pendingIntent);

        void startActivityAndCollapse(Intent intent);
    }
}
