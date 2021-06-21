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
package androidx.wear.complications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo

/**
 * Allows complication providers to request update calls from the system. This effectively allows
 * providers to push updates to the system outside of the update request cycle.
 */
public class ProviderUpdateRequester(
    private val context: Context,
    private val providerComponent: ComponentName
) {
    /**
     * Requests that the system call
     * [onComplicationUpdate][ComplicationProviderService.onComplicationRequest] on the specified
     * provider, for all active complications using that provider.
     *
     * This will do nothing if no active complications are configured to use the specified
     * provider.
     *
     * This will also only work if called from the same package as the provider.
     */
    @SuppressLint("PendingIntentMutability")
    public fun requestUpdateAll() {
        val intent = Intent(ACTION_REQUEST_UPDATE_ALL)
        intent.setPackage(UPDATE_REQUEST_RECEIVER_PACKAGE)
        intent.putExtra(EXTRA_PROVIDER_COMPONENT, providerComponent)
        // Add a placeholder PendingIntent to allow the UID to be checked.
        intent.putExtra(
            ProviderUpdateRequesterConstants.EXTRA_PENDING_INTENT,
            PendingIntent.getActivity(context, 0, Intent(""), 0)
        )
        context.sendBroadcast(intent)
    }

    /**
     * Requests that the system call
     * [onComplicationUpdate][ComplicationProviderService.onComplicationRequest] on the specified
     * provider, for the given complication ids. Inactive complications are ignored, as are
     * complications configured to use a different provider.
     *
     * @param complicationInstanceIds The system's IDs for the complications to be updated as provided
     * to [ComplicationProviderService.onComplicationActivated] and
     * [ComplicationProviderService.onComplicationRequest].
     */
    @SuppressLint("PendingIntentMutability")
    public fun requestUpdate(vararg complicationInstanceIds: Int) {
        val intent = Intent(ACTION_REQUEST_UPDATE)
        intent.setPackage(UPDATE_REQUEST_RECEIVER_PACKAGE)
        intent.putExtra(EXTRA_PROVIDER_COMPONENT, providerComponent)
        intent.putExtra(EXTRA_COMPLICATION_IDS, complicationInstanceIds)
        // Add a placeholder PendingIntent to allow the UID to be checked.
        intent.putExtra(
            ProviderUpdateRequesterConstants.EXTRA_PENDING_INTENT,
            PendingIntent.getActivity(context, 0, Intent(""), 0)
        )
        context.sendBroadcast(intent)
    }

    public companion object {
        /** The package of the service that accepts provider requests.  */
        private const val UPDATE_REQUEST_RECEIVER_PACKAGE = "com.google.android.wearable.app"

        /** @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val ACTION_REQUEST_UPDATE: String =
            "android.support.wearable.complications.ACTION_REQUEST_UPDATE"

        /** @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val ACTION_REQUEST_UPDATE_ALL: String =
            "android.support.wearable.complications.ACTION_REQUEST_UPDATE_ALL"

        /** @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val EXTRA_PROVIDER_COMPONENT: String =
            "android.support.wearable.complications.EXTRA_PROVIDER_COMPONENT"

        /** @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val EXTRA_COMPLICATION_IDS: String =
            "android.support.wearable.complications.EXTRA_COMPLICATION_IDS"
    }
}
