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

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Allows complication providers to request update calls from the system. This effectively allows
 * providers to push updates to the system outside of the update request cycle.
 */
public class ProviderUpdateRequester {

    /** The package of the service that accepts provider requests. */
    private static final String UPDATE_REQUEST_RECEIVER_PACKAGE = "com.google.android.wearable.app";

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String ACTION_REQUEST_UPDATE =
            "android.support.wearable.complications.ACTION_REQUEST_UPDATE";

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String ACTION_REQUEST_UPDATE_ALL =
            "android.support.wearable.complications.ACTION_REQUEST_UPDATE_ALL";

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String EXTRA_PROVIDER_COMPONENT =
            "android.support.wearable.complications.EXTRA_PROVIDER_COMPONENT";

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String EXTRA_COMPLICATION_IDS =
            "android.support.wearable.complications.EXTRA_COMPLICATION_IDS";

    @NonNull private final Context mContext;
    @NonNull private final ComponentName mProviderComponent;

    /**
     * @param context The provider's context
     * @param providerComponent the component name of the {@link ComplicationProviderService} that
     *     this will request updates for
     */
    public ProviderUpdateRequester(
            @NonNull Context context, @NonNull ComponentName providerComponent) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (providerComponent == null) {
            throw new IllegalArgumentException("ProviderComponent cannot be null");
        }
        mContext = context;
        mProviderComponent = providerComponent;
    }

    /**
     * Requests that the system call {@link ComplicationProviderService#onComplicationUpdate
     * onComplicationUpdate} on the specified provider, for all active complications using that
     * provider.
     *
     * <p>This will do nothing if no active complications are configured to use the specified
     * provider.
     *
     * <p>This will also only work if called from the same package as the provider.
     */
    @SuppressLint("PendingIntentMutability")
    public void requestUpdateAll() {
        Intent intent = new Intent(ACTION_REQUEST_UPDATE_ALL);
        intent.setPackage(UPDATE_REQUEST_RECEIVER_PACKAGE);
        intent.putExtra(EXTRA_PROVIDER_COMPONENT, mProviderComponent);
        // Add a placeholder PendingIntent to allow the UID to be checked.
        intent.putExtra(
                ProviderUpdateRequesterConstants.EXTRA_PENDING_INTENT,
                PendingIntent.getActivity(mContext, 0, new Intent(""), 0));
        mContext.sendBroadcast(intent);
    }

    /**
     * Requests that the system call {@link ComplicationProviderService#onComplicationUpdate
     * onComplicationUpdate} on the specified provider, for the given complication ids. Inactive
     * complications are ignored, as are complications configured to use a different provider.
     *
     * @param complicationIds the ids of the complications to be updated, as provided in calls to
     *     {@link ComplicationProviderService#onComplicationActivated} and
     *     {@link ComplicationProviderService#onComplicationUpdate}.
     */
    @SuppressLint("PendingIntentMutability")
    public void requestUpdate(@NonNull int... complicationIds) {
        Intent intent = new Intent(ACTION_REQUEST_UPDATE);
        intent.setPackage(UPDATE_REQUEST_RECEIVER_PACKAGE);
        intent.putExtra(EXTRA_PROVIDER_COMPONENT, mProviderComponent);
        intent.putExtra(EXTRA_COMPLICATION_IDS, complicationIds);
        // Add a placeholder PendingIntent to allow the UID to be checked.
        intent.putExtra(
                ProviderUpdateRequesterConstants.EXTRA_PENDING_INTENT,
                PendingIntent.getActivity(mContext, 0, new Intent(""), 0));

        mContext.sendBroadcast(intent);
    }
}
