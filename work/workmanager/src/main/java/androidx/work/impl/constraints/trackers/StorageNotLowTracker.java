/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.work.impl.constraints.trackers;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Log;

/**
 * Tracks whether or not the device's storage is low.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class StorageNotLowTracker extends BroadcastReceiverConstraintTracker<Boolean> {

    private static final String TAG = "StorageNotLowTracker";

    /**
     * Create an instance of {@link StorageNotLowTracker}.
     * @param context The application {@link Context}
     */
    public StorageNotLowTracker(Context context) {
        super(context);
    }

    @Override
    public Boolean getInitialState() {
        Intent intent = mAppContext.registerReceiver(null, getIntentFilter());
        if (intent == null || intent.getAction() == null) {
            // ACTION_DEVICE_STORAGE_LOW is a sticky broadcast that is removed when sufficient
            // storage is available again.  ACTION_DEVICE_STORAGE_OK is not sticky.  So if we
            // don't receive anything here, we can assume that the storage state is okay.
            return true;
        } else {
            switch (intent.getAction()) {
                case Intent.ACTION_DEVICE_STORAGE_OK:
                    return true;

                case Intent.ACTION_DEVICE_STORAGE_LOW:
                    return false;

                default:
                    // This should never happen because the intent filter is configured
                    // correctly.
                    return null;
            }
        }
    }

    @Override
    public IntentFilter getIntentFilter() {
        // In API 26+, DEVICE_STORAGE_OK/LOW are deprecated and are no longer sent to
        // manifest-defined BroadcastReceivers. Since we are registering our receiver manually, this
        // is currently okay. This may change in future versions, so this will need to be monitored.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
        return intentFilter;
    }

    @Override
    public void onBroadcastReceive(Context context, @NonNull Intent intent) {
        if (intent.getAction() == null) {
            return; // Should never happen since the IntentFilter was configured.
        }

        Log.d(TAG, String.format("Received %s", intent.getAction()));

        switch (intent.getAction()) {
            case Intent.ACTION_DEVICE_STORAGE_OK:
                setState(true);
                break;

            case Intent.ACTION_DEVICE_STORAGE_LOW:
                setState(false);
                break;
        }
    }
}
