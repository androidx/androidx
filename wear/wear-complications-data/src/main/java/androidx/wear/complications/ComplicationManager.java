/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.complications;

import android.os.RemoteException;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.IComplicationManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Allows providers to interact with the complications system.
 *
 * <p>Providers will receive an instance of this class in calls to {@link
 * ComplicationProviderService#onComplicationActivated onComplicationActivated} and {@link
 * ComplicationProviderService#onComplicationUpdate onComplicationUpdate}.
 */
public class ComplicationManager {

    private static final String TAG = "ComplicationManager";

    @NonNull private final IComplicationManager mService;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ComplicationManager(@NonNull IComplicationManager service) {
        mService = service;
    }

    /**
     * Updates complication data for the given {@code complicationId}. The type of data provided
     * must either match the configured type for the given complication, or should be of {@link
     * ComplicationData#TYPE_NO_DATA TYPE_NO_DATA}.
     *
     * <p>If no update is required (meaning that the complication will continue to display the
     * previous value, which may be TYPE_NO_DATA if no data has ever been sent), then null may be
     * passed as the data ({@link #noUpdateRequired} may be called to have the same effect).
     *
     * <p>A provider service should always call either this method or {@link #noUpdateRequired} in
     * response to an update request from the system.
     *
     * <p>This method is often called in response to {@link
     * ComplicationProviderService#onComplicationUpdate} but it can also be called when you have new
     * data to push.
     *
     * <p> If {@link ComplicationProviderService#inRetailMode} is true then representative mock data
     * should be returned rather than the real data.
     *
     * @param complicationId The ID of the complication to update
     * @param data The {@link ComplicationData} to send to the watch face
     */
    public void updateComplicationData(int complicationId, @NonNull ComplicationData data)
            throws RemoteException {
        if (data.getType() == ComplicationData.TYPE_NOT_CONFIGURED
                || data.getType() == ComplicationData.TYPE_EMPTY) {
            throw new IllegalArgumentException(
                    "Cannot send data of TYPE_NOT_CONFIGURED or "
                            + "TYPE_EMPTY. Use TYPE_NO_DATA instead.");
        }
        try {
            mService.updateComplicationData(complicationId, data);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to send complication data.", e);
        }
    }

    /**
     * Informs the system that no update is required after an update request. This means that the
     * complication will continue to display the previous value, which may be TYPE_NO_DATA if no
     * data has ever been sent.
     *
     * <p>A provider service should always call either this method or {@link
     * #updateComplicationData} in response to an update request from the system.
     *
     * @param complicationId The ID of the complication
     * @throws RemoteException If it failed to send complication data
     */
    public void noUpdateRequired(int complicationId) throws RemoteException {
        mService.updateComplicationData(complicationId, null);
    }
}
