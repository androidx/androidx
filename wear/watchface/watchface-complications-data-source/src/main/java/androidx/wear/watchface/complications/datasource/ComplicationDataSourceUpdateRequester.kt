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
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.ComplicationDataSourceUpdateRequesterConstants
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService.Companion.ACTION_WEAR_SDK_COMPLICATION_UPDATE_REQUEST
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService.ComplicationDataRequester
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester.Companion.UPDATE_REQUEST_RECEIVER_PACKAGE
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester.Companion.filterRequests
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

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
     * This will also only work if called from the same package as the complication data source.
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

        /** An override to [UPDATE_REQUEST_RECEIVER_PACKAGE] for tests. */
        internal var overrideUpdateRequestsReceiverPackage: String? = null

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
            complicationDataSourceComponent: ComponentName,
        ): ComplicationDataSourceUpdateRequester =
            ComplicationDataSourceUpdateRequesterImpl(
                context.applicationContext,
                complicationDataSourceComponent,
            )

        /**
         * Filters [requests] if they aren't keyed by [requesterComponent] or if their
         * `complicationInstanceId` is not present in [instanceIds].
         *
         * If [instanceIds] is empty, only the [requesterComponent] will be checked.
         */
        internal fun filterRequests(
            requesterComponent: ComponentName,
            instanceIds: IntArray,
            requests: List<Pair<ComponentName, ComplicationRequest>>,
        ): Set<ComplicationRequest> {
            fun isRequestedInstance(id: Int): Boolean = instanceIds.isEmpty() || id in instanceIds
            return requests
                .filter { (component, request) ->
                    component == requesterComponent &&
                        isRequestedInstance(request.complicationInstanceId)
                }
                .map { it.second }
                .toSet()
        }

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
 * @param complicationDataSourceComponent The [ComponentName] of the ComplicationDataSourceService
 *   to reload.
 */
internal class ComplicationDataSourceUpdateRequesterImpl(
    private val context: Context,
    private val complicationDataSourceComponent: ComponentName,
) : ComplicationDataSourceUpdateRequester {

    /**
     * Returns whether the Complication WearSDK APIs used here are available on this device.
     * - Wear SDK is only available on watches.
     * - The Complication APIs are only available on Android B+ devices.
     *
     * If false, we should use the legacy broadcast based system for complication updates.
     */
    fun shouldUseWearSdk(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH) &&
            (Build.VERSION.SDK_INT >= 36)

    private fun updateRequestReceiverPackage() =
        ComplicationDataSourceUpdateRequester.overrideUpdateRequestsReceiverPackage
            ?: UPDATE_REQUEST_RECEIVER_PACKAGE

    override fun requestUpdateAll() {
        if (shouldUseWearSdk()) {
            requestUpdate()
            return
        }
        val intent = Intent(ComplicationDataSourceUpdateRequester.ACTION_REQUEST_UPDATE_ALL)
        intent.setPackage(updateRequestReceiverPackage())
        intent.putExtra(
            ComplicationDataSourceUpdateRequester.EXTRA_PROVIDER_COMPONENT,
            complicationDataSourceComponent,
        )
        // Add a placeholder PendingIntent to allow the UID to be checked.
        intent.putExtra(
            ComplicationDataSourceUpdateRequesterConstants.EXTRA_PENDING_INTENT,
            PendingIntent.getActivity(context, 0, Intent(""), PendingIntent.FLAG_IMMUTABLE),
        )
        context.sendBroadcast(intent)
    }

    override fun requestUpdate(vararg complicationInstanceIds: Int) {
        if (shouldUseWearSdk()) {
            updateComplicationsUsingWearSdk(
                complicationInstanceIds,
                WearSdkComplicationsApi.Impl(context),
            )
            return
        }
        val intent = Intent(ComplicationDataSourceUpdateRequester.ACTION_REQUEST_UPDATE)
        intent.setPackage(updateRequestReceiverPackage())
        intent.putExtra(
            ComplicationDataSourceUpdateRequester.EXTRA_PROVIDER_COMPONENT,
            complicationDataSourceComponent,
        )
        intent.putExtra(
            ComplicationDataSourceUpdateRequester.EXTRA_COMPLICATION_IDS,
            complicationInstanceIds,
        )
        // Add a placeholder PendingIntent to allow the UID to be checked.
        intent.putExtra(
            ComplicationDataSourceUpdateRequesterConstants.EXTRA_PENDING_INTENT,
            PendingIntent.getActivity(context, 0, Intent(""), PendingIntent.FLAG_IMMUTABLE),
        )
        context.sendBroadcast(intent)
    }

    @RequiresApi(36)
    fun updateComplicationsUsingWearSdk(
        complicationInstanceIds: IntArray,
        api: WearSdkComplicationsApi,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        dataSourceProvider:
            suspend () -> Pair<WearSdkComplicationDataRequester, ServiceConnection> =
            this::bindDataSource,
    ) {
        CoroutineScope(dispatcher).launch {
            val executor = dispatcher.asExecutor()
            val updateConfigSet =
                filterRequests(
                    complicationDataSourceComponent,
                    complicationInstanceIds,
                    api.getActiveConfigs(executor).map {
                        it.toComplicationRequestPair(immediateResponseRequired = false)
                    },
                )

            val dataPairs: List<Pair<Int, WearSdkComplicationData>> =
                withDataSource(dataSourceProvider) { dataSource ->
                    updateConfigSet
                        .map { request -> async { dataSource.requestData(request) } }
                        .awaitAll()
                }
            dataPairs.forEach { (id, data) ->
                launch { api.updateComplication(id, data, executor) }
            }
        }
    }

    @RequiresApi(36)
    suspend fun <T> withDataSource(
        provider: suspend () -> Pair<WearSdkComplicationDataRequester, ServiceConnection>,
        block: suspend (WearSdkComplicationDataRequester) -> T,
    ): T {
        val (binder, connection) = provider()
        try {
            return block(binder)
        } finally {
            context.unbindService(connection)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresApi(36)
    suspend fun bindDataSource(): Pair<WearSdkComplicationDataRequester, ServiceConnection> =
        suspendCancellableCoroutine { continuation ->
            val connection =
                object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) =
                        continuation.resume(
                            WearSdkComplicationDataRequester.Impl(
                                binder as ComplicationDataRequester
                            ) to this
                        ) {}

                    override fun onServiceDisconnected(name: ComponentName?) {
                        continuation.cancel()
                    }
                }

            context.bindService(
                Intent(ACTION_WEAR_SDK_COMPLICATION_UPDATE_REQUEST).apply {
                    component = complicationDataSourceComponent
                },
                connection,
                Context.BIND_AUTO_CREATE,
            )

            continuation.invokeOnCancellation { context.unbindService(connection) }
        }
}
