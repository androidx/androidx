/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.glance.wear

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.collection.intSetOf
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.glance.wear.cache.WearWidgetCache
import androidx.glance.wear.cache.WearWidgetCache.WidgetCacheMissException
import androidx.glance.wear.core.ActiveWearWidgetHandle
import androidx.glance.wear.core.RendererVersion
import androidx.glance.wear.core.WearWidgetEvent
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WearWidgetRawContent
import androidx.glance.wear.core.WearWidgetUpdateRequest
import androidx.glance.wear.core.WidgetInstanceId
import androidx.glance.wear.parcel.WearWidgetCapture
import androidx.glance.wear.parcel.WearWidgetCapture.addCreationUptimeMillis
import androidx.glance.wear.parcel.WidgetUpdateClient
import androidx.glance.wear.parcel.WidgetUpdateClientImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Object that handles providing the contents of a Wear Widget.
 *
 * The widget UI is defined by an instance of [WearWidgetData] such as [WearWidgetDocument],
 * provided in the implementation of [provideWidgetData].
 *
 * An implementation of this class can be associated with a [GlanceWearWidgetService] for receiving
 * content requests and events from the Host.
 */
public abstract class GlanceWearWidget
internal constructor(
    private val updateClient: WidgetUpdateClient,
    private val widgetCache: WearWidgetCache? = null,
) {

    public constructor() : this(WidgetUpdateClientImpl())

    /**
     * Override this method to provide data for this Widget.
     *
     * This method is called from the main thread.
     *
     * @param context the context from which this method is called
     * @param params the parameters that describe the widget for which the data is being provided.
     * @return the widget data for this Widget.
     */
    @MainThread
    public abstract suspend fun provideWidgetData(
        context: Context,
        params: WearWidgetParams,
    ): WearWidgetData

    /**
     * Provides widget data as raw content, based on [provideWidgetData] and
     * [WearWidgetData.captureRawContent], and checks that it can be sent to the Host.
     *
     * @param context the context from which this method is called.
     * @param params the parameters that describe the widget for which the data is being provided.
     * @param isInspectionMode whether the capture is in inspection mode (e.g., for IDE previews).
     * @return the widget data as raw content.
     * @throws WearWidgetContentTooLargeException if the raw content exceeds the maximum size that
     *   can be sent to the Host in a single Binder transaction.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    public suspend fun provideWidgetDataAsRawContent(
        context: Context,
        params: WearWidgetParams,
        isInspectionMode: Boolean = false,
    ): WearWidgetRawContent {
        if (!isInspectionMode) {
            // We need this flag to be false (meaning empty axis won't be send and default normal
            // weight would be used, for the 1.6 renderer and the player that has a bug in it.
            RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis =
                RendererVersion.fromPlHostPackage(context) >
                    RendererVersion(1, 6, 0, /* not important */ intSetOf())
        }
        val widgetContent = provideWidgetData(context, params)
        val rawContent = widgetContent.captureRawContent(context, params, isInspectionMode)
        WearWidgetCapture.checkContentSize(rawContent.rcDocument.size)
        return rawContent
    }

    /**
     * Called when a widget provider linked to this widget class is added to the host.
     *
     * This occurs when a widget is added to the carousel.
     *
     * This method is called from the main thread.
     *
     * @param context the context from which this method is called
     * @param widgetHandle the handle of the active widget.
     */
    @MainThread
    public open suspend fun onAdded(context: Context, widgetHandle: ActiveWearWidgetHandle) {}

    /**
     * Called when a widget provider linked to this widget class is removed from the host.
     *
     * This occurs when a widget is removed from the carousel.
     *
     * This method is called from the main thread.
     *
     * @param context the context from which this method is called
     * @param widgetHandle the handle of the active widget.
     */
    @MainThread
    public open suspend fun onRemoved(context: Context, widgetHandle: ActiveWearWidgetHandle) {}

    /**
     * Called when the system sends a batch of interaction events. The time between calls to this
     * method may vary; do not depend on it for time-sensitive or critical tasks.
     *
     * Interaction events represent user direct interaction with a widget, for example when a widget
     * was visible.
     *
     * This function must complete within 10 seconds of being called. If the timeout is exceeded,
     * the operation will be canceled.
     *
     * This method is called from the main thread.
     *
     * @param context the context from which this method is called
     * @param events A list of [WearWidgetEvent] representing interactions that occurred.
     */
    @MainThread public open suspend fun onEvents(context: Context, events: List<WearWidgetEvent>) {}

    /**
     * Triggers a content update for the given widget [instanceId], resulting in a call to
     * [provideWidgetData], after which the results are pushed to the Host.
     *
     * All [WidgetInstanceId] for active widgets associated with this class can be retrieved from
     * [GlanceWearWidgetManager.fetchActiveWidgets(KClass)].
     *
     * The coroutine will be canceled if it doesn't complete within 10 seconds of being called.
     *
     * @param context the context from which this method is called.
     * @param instanceId the ID of the widget instance to update.
     * @throws [IllegalArgumentException] if the provided [WidgetInstanceId] is invalid or not owned
     *   by the calling application.
     * @throws [WearWidgetContentTooLargeException] if the widget content exceeds the maximum size
     *   that can be sent to the Host in a single Binder transaction.
     */
    public suspend fun triggerUpdate(context: Context, instanceId: WidgetInstanceId) {
        if (context.isDebuggableBuild()) {
            updateClient.sendUpdateBroadcast(context, instanceId = instanceId)
        }
        triggerUpdateInternal(context, instanceId, cachedHandle = null)
    }

    /**
     * Triggers a content update for all active widget instances associated with this class,
     * resulting in calls to [provideWidgetData], after which the results are pushed to the Host.
     *
     * Each individual update coroutine will be canceled if it doesn't complete within 10 seconds of
     * being called.
     *
     * @param context the context from which this method is called.
     * @throws [WearWidgetContentTooLargeException] if the content of any widget instance exceeds
     *   the maximum size that can be sent to the Host in a single Binder transaction. Other widget
     *   instances are still updated. If several instances fail, the exceptions for the others are
     *   attached to the thrown one as [suppressed exceptions][Throwable.suppressedExceptions].
     */
    @SuppressLint("ListIterator") // Not running inside Compose code.
    public suspend fun triggerUpdateAll(context: Context) {
        val isDebuggable = context.isDebuggableBuild()
        val isDeveloperMode = context.isDeveloperModeEnabled()
        if (isDebuggable || isDeveloperMode) {
            GlanceWearWidgetManager(context).getProviderForWidget(this::class)?.let { provider ->
                if (isDebuggable) {
                    updateClient.sendUpdateBroadcast(context, provider = provider)
                }
                // In developer mode (such as Emulator), we would always trigger a pull update
                // instead of trying to use push mechanism.
                if (isDeveloperMode) {
                    triggerPullUpdate(context, provider, instanceId = null)
                    return@triggerUpdateAll
                }
            }
        }

        val activeWidgets = fetchActiveWidgets(context)
        if (activeWidgets.isEmpty()) {
            Log.i(TAG, "No active instances found in the system to update.")
            return
        }
        val contentTooLargeExceptions = coroutineScope {
            activeWidgets
                .map { handle ->
                    async {
                        try {
                            triggerUpdateInternal(context, handle.instanceId, cachedHandle = handle)
                            null
                        } catch (ex: IllegalArgumentException) {
                            Log.i(
                                TAG,
                                "WidgetInstanceId is no longer valid. It may have been removed after the update was triggered: ${handle.instanceId}",
                            )
                            null
                        } catch (ex: WearWidgetContentTooLargeException) {
                            // Don't cancel the updates of other instances. This is rethrown below.
                            ex
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
        contentTooLargeExceptions.firstOrNull()?.let { first ->
            contentTooLargeExceptions.drop(1).forEach { first.addSuppressed(it) }
            throw first
        }
    }

    private suspend fun triggerUpdateInternal(
        context: Context,
        instanceId: WidgetInstanceId,
        cachedHandle: ActiveWearWidgetHandle? = null,
    ) {
        if (context.isDeveloperModeEnabled()) {
            GlanceWearWidgetManager(context).getProviderForWidget(this::class)?.let {
                triggerPullUpdate(context, it, instanceId)
                return@triggerUpdateInternal
            }
        }

        if (Build.VERSION.SDK_INT >= 37) {
            try {
                pushUpdate(context, instanceId)
                return
            } catch (ex: WidgetCacheMissException) {
                Log.d(TAG, "Error in push update. Falling back to pull update request.", ex)
            }
        }

        val widgetHandle =
            cachedHandle
                ?: findActiveWidgetById(context, instanceId)
                ?: throw IllegalArgumentException("Invalid WidgetInstanceId=$instanceId")
        triggerPullUpdate(context, widgetHandle.provider, instanceId)
    }

    /**
     * Trigger a content update for all widgets associated with the [provider] service component.
     *
     * @param context the context from which this method is called.
     * @param provider the component name of the widget provider service to request an update for.
     * @param instanceId the optional ID of the widget instance to update.
     */
    internal fun triggerPullUpdate(
        context: Context,
        provider: ComponentName,
        instanceId: WidgetInstanceId? = null,
    ) {
        updateClient.requestUpdate(context, provider, instanceId)
    }

    /**
     * Generates widget data and pushes the update to the host.
     *
     * The coroutine will be canceled if it doesn't complete within 10 seconds of being called.
     *
     * @param context the context from which this method is called.
     * @param instanceId the ID of the widget instance to update.
     * @throws [IllegalArgumentException] if the provided [WidgetInstanceId] is invalid or not owned
     *   by the calling application.
     * @throws [WearWidgetCache.WidgetCacheMissException] if any required cache entry cannot be
     *   found.
     */
    @RequiresApi(37)
    private suspend fun pushUpdate(context: Context, instanceId: WidgetInstanceId) {
        val creationUptimeMillis = SystemClock.uptimeMillis()
        val cache = widgetCache ?: WearWidgetCache(context)
        val containerType = cache.getContainerTypeForInstance(instanceId)
        val params = cache.getWidgetParams(containerType, instanceId)

        val rawContent =
            withContext(Dispatchers.Main.immediate) {
                provideWidgetDataAsRawContent(context, params)
            }
        rawContent.extras.addCreationUptimeMillis(creationUptimeMillis)

        updateClient.pushUpdate(context, WearWidgetUpdateRequest(instanceId), rawContent)
    }

    @VisibleForTesting
    @SuppressLint("ListIterator") // Not running inside Compose code.
    internal open suspend fun findActiveWidgetById(
        context: Context,
        instanceId: WidgetInstanceId,
    ): ActiveWearWidgetHandle? = fetchActiveWidgets(context).find { it.instanceId == instanceId }

    @VisibleForTesting
    internal open suspend fun fetchActiveWidgets(context: Context): List<ActiveWearWidgetHandle> =
        GlanceWearWidgetManager(context).fetchActiveWidgets(this::class)

    internal companion object {
        private const val TAG = "GlanceWearWidget"

        private fun Context.isDebuggableBuild(): Boolean =
            (this.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        /**
         * Returns whether developer mode is enabled for the device.
         *
         * Developer mode is enabled if the device is an emulator or if the developer settings are
         * enabled.
         */
        internal fun Context.isDeveloperModeEnabled(): Boolean =
            isEmulator() ||
                Settings.Global.getInt(
                    contentResolver,
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                    0,
                ) == 1

        private fun isEmulator(): Boolean =
            Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.HARDWARE.contains("cutf_cvm") ||
                Build.HARDWARE.contains("starfish")
    }
}
