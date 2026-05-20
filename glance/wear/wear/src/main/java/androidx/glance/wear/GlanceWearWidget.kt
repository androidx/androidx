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
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
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
import androidx.glance.wear.parcel.WidgetUpdateClient
import androidx.glance.wear.parcel.WidgetUpdateClientImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
     */
    @MainThread
    public abstract suspend fun provideWidgetData(
        context: Context,
        params: WearWidgetParams,
    ): WearWidgetData

    /**
     * Internal method to provide widget data as raw content, based on [provideWidgetData] and
     * [WearWidgetData.captureRawContent].
     *
     * @param context the context from which this method is called.
     * @param params the parameters that describe the widget for which the data is being provided.
     * @return the widget data as raw content.
     */
    @SuppressLint("RestrictedApiAndroidX")
    @OptIn(androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi::class)
    internal suspend fun provideWidgetDataAsRawContentInternal(
        context: Context,
        params: WearWidgetParams,
    ): WearWidgetRawContent {
        // We need this flag to be false (meaning empty axis won't be send and default normal weight
        // would be used, for the 1.6 renderer and the player that has a bug in it.
        RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis =
            RendererVersion.fromPlHostPackage(context) > RendererVersion(1, 6, 0)
        val widgetContent = provideWidgetData(context, params)
        return widgetContent.captureRawContent(context, params)
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
     * @param widgetHandle the handle of the widget.
     */
    @MainThread
    public open suspend fun onRemoved(context: Context, widgetHandle: ActiveWearWidgetHandle) {}

    /**
     * Called when the system sends a batch of interaction events. The time between calls to this
     * method may vary, do not depend on it for time-sensitive or critical tasks.
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
     * @param events A list [WearWidgetEvent] representing interactions that occurred.
     */
    @MainThread public open suspend fun onEvents(context: Context, events: List<WearWidgetEvent>) {}

    /**
     * Triggers a content update for the given widget [instanceId], resulting in a call to
     * [provideWidgetData], after which the results to the Host.
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
     */
    public suspend fun triggerUpdate(context: Context, instanceId: WidgetInstanceId) {
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
     */
    @SuppressLint("ListIterator") // Not running inside Compose code.
    public suspend fun triggerUpdateAll(context: Context) {
        val activeWidgets = fetchActiveWidgets(context)
        if (activeWidgets.isEmpty()) {
            Log.i(TAG, "No active instances found to update.")
            return
        }
        coroutineScope {
            for (handle in activeWidgets) {
                launch {
                    try {
                        triggerUpdateInternal(context, handle.instanceId, cachedHandle = handle)
                    } catch (ex: IllegalArgumentException) {
                        Log.i(
                            TAG,
                            "WidgetInstanceId is no longer valid. It may have been removed after the update was triggered: ${handle.instanceId}",
                        )
                    }
                }
            }
        }
    }

    private suspend fun triggerUpdateInternal(
        context: Context,
        instanceId: WidgetInstanceId,
        cachedHandle: ActiveWearWidgetHandle? = null,
    ) {
        if (context.isDebuggable()) {
            updateClient.sendUpdateBroadcast(context, instanceId = instanceId)
        }

        if (isAtLeastC()) {
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
    // TODO: b/446828899 - Add RequiresApi(37) annotation.
    private suspend fun pushUpdate(context: Context, instanceId: WidgetInstanceId) {
        val cache = widgetCache ?: WearWidgetCache(context)
        val containerType = cache.getContainerTypeForInstance(instanceId)
        val params = cache.getWidgetParams(containerType, instanceId)

        val rawContent =
            withContext(Dispatchers.Main.immediate) {
                provideWidgetDataAsRawContentInternal(context, params)
            }

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

        private fun Context.isDebuggable(): Boolean =
            (this.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        /**
         * Robolectric does not support SDK 37 version, so we need to force the SDK version to 37 to
         * test the logic that checks for 37 features.
         */
        // TODO: b/446828899 - Remove once we have 37 in robolectric.
        @VisibleForTesting var forceIsAtLeast37ForTesting: Boolean? = null

        fun isAtLeastC(): Boolean =
            forceIsAtLeast37ForTesting
                ?: (Build.VERSION.SDK_INT >= 37 ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
                        Build.VERSION.CODENAME.startsWith("C", ignoreCase = true)))
    }
}
