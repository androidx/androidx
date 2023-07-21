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
package androidx.wear.watchface.complications.datasource

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.ComplicationDataSourceUpdateRequesterConstants

/**
 * Allows complication complication data source to request update calls from the system. This
 * effectively allows complication data source to push updates to the system outside of the update
 * request cycle.
 */
public interface ComplicationDataSourceUpdateRequester {
    /**
     * Requests that the system call
     * [onComplicationUpdate][ComplicationDataSourceService.onComplicationRequest] on the specified
     * complication data source, for all active complications using that complication data source.
     *
     * This will do nothing if no active complications are configured to use the specified
     * complication data source.
     *
     * This will also only work if called from the same package as the omplication data source.
     */
    public fun requestUpdateAll()

    /**
     * Requests that the system call
     * [onComplicationUpdate][ComplicationDataSourceService.onComplicationRequest] on the specified
     * complication data source, for the given complication ids. Inactive complications are ignored,
     * as are complications configured to use a different complication data source.
     *
     * @param complicationInstanceIds The system's IDs for the complications to be updated as
     *   provided to [ComplicationDataSourceService.onComplicationActivated] and
     *   [ComplicationDataSourceService.onComplicationRequest].
     */
    public fun requestUpdate(vararg complicationInstanceIds: Int)

    public companion object {
        /** The package of the service that accepts complication data source requests. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val UPDATE_REQUEST_RECEIVER_PACKAGE = "com.google.android.wearable.app"

        /**
         * Creates a [ComplicationDataSourceUpdateRequester].
         *
         * @param context The [ComplicationDataSourceService]'s [Context]
         * @param complicationDataSourceComponent The [ComponentName] of the
         *   [ComplicationDataSourceService] to reload.
         * @return The constructed [ComplicationDataSourceUpdateRequester].
         */
        @JvmStatic
        public fun create(
            context: Context,
            complicationDataSourceComponent: ComponentName
        ): ComplicationDataSourceUpdateRequester =
            ComplicationDataSourceUpdateRequesterImpl(context, complicationDataSourceComponent)

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val ACTION_REQUEST_UPDATE: String =
            "android.support.wearable.complications.ACTION_REQUEST_UPDATE"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val ACTION_REQUEST_UPDATE_ALL: String =
            "android.support.wearable.complications.ACTION_REQUEST_UPDATE_ALL"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val EXTRA_PROVIDER_COMPONENT: String =
            "android.support.wearable.complications.EXTRA_PROVIDER_COMPONENT"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val EXTRA_COMPLICATION_IDS: String =
            "android.support.wearable.complications.EXTRA_COMPLICATION_IDS"
    }
}

/**
 * @param context The [ComplicationDataSourceService]'s [Context]
 * @param complicationDataSourceComponent The [ComponentName] of the ComplicationDataSourceService]
 *   to reload.
 */
private class ComplicationDataSourceUpdateRequesterImpl(
    private val context: Context,
    private val complicationDataSourceComponent: ComponentName
) : ComplicationDataSourceUpdateRequester {

    override fun requestUpdateAll() {
        val intent = Intent(ComplicationDataSourceUpdateRequester.ACTION_REQUEST_UPDATE_ALL)
        intent.setPackage(ComplicationDataSourceUpdateRequester.UPDATE_REQUEST_RECEIVER_PACKAGE)
        intent.putExtra(
            ComplicationDataSourceUpdateRequester.EXTRA_PROVIDER_COMPONENT,
            complicationDataSourceComponent
        )
        // Add a placeholder PendingIntent to allow the UID to be checked.
        intent.putExtra(
            ComplicationDataSourceUpdateRequesterConstants.EXTRA_PENDING_INTENT,
            PendingIntent.getActivity(context, 0, Intent(""), PendingIntent.FLAG_IMMUTABLE)
        )
        context.sendBroadcast(intent)
    }

    override fun requestUpdate(vararg complicationInstanceIds: Int) {
        val intent = Intent(ComplicationDataSourceUpdateRequester.ACTION_REQUEST_UPDATE)
        intent.setPackage(ComplicationDataSourceUpdateRequester.UPDATE_REQUEST_RECEIVER_PACKAGE)
        intent.putExtra(
            ComplicationDataSourceUpdateRequester.EXTRA_PROVIDER_COMPONENT,
            complicationDataSourceComponent
        )
        intent.putExtra(
            ComplicationDataSourceUpdateRequester.EXTRA_COMPLICATION_IDS,
            complicationInstanceIds
        )
        // Add a placeholder PendingIntent to allow the UID to be checked.
        intent.putExtra(
            ComplicationDataSourceUpdateRequesterConstants.EXTRA_PENDING_INTENT,
            PendingIntent.getActivity(context, 0, Intent(""), PendingIntent.FLAG_IMMUTABLE)
        )
        context.sendBroadcast(intent)
    }
}
