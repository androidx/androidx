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

package androidx.glance.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.unit.DpSize
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.GlanceId
import kotlinx.coroutines.flow.firstOrNull

/**
 * Manager for Glance App Widgets.
 *
 * This is used to query the app widgets currently installed on the system, and some of their
 * properties.
 */
class GlanceAppWidgetManager(private val context: Context) {

    private data class State(
        val receiverToProviderName: Map<ComponentName, String> = emptyMap(),
        val providerNameToReceivers: Map<String, List<ComponentName>> = emptyMap(),
    ) {
        constructor(receiverToProviderName: Map<ComponentName, String>) : this(
            receiverToProviderName,
            receiverToProviderName.reverseMapping()
        )
    }

    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val dataStore by lazy { getOrCreateDataStore() }

    private fun getOrCreateDataStore(): DataStore<Preferences> {
        synchronized(GlanceAppWidgetManager) {
            return dataStoreSingleton ?: run {
                val newValue = context.appManagerDataStore
                dataStoreSingleton = newValue
                newValue
            }
        }
    }

    internal suspend fun <R : GlanceAppWidgetReceiver, P : GlanceAppWidget> updateReceiver(
        receiver: R,
        provider: P,
    ) {
        val receiverName = requireNotNull(receiver.javaClass.canonicalName) { "no receiver name" }
        val providerName = requireNotNull(provider.javaClass.canonicalName) { "no provider name" }
        dataStore.updateData { pref ->
            pref.toMutablePreferences().also { builder ->
                builder[providersKey] = (pref[providersKey] ?: emptySet()) + receiverName
                builder[providerKey(receiverName)] = providerName
            }.toPreferences()
        }
    }

    private fun createState(prefs: Preferences): State {
        val packageName = context.packageName
        val receivers = prefs[providersKey] ?: return State()
        return State(
            receivers.mapNotNull { receiverName ->
                val comp = ComponentName(packageName, receiverName)
                val providerName = prefs[providerKey(receiverName)] ?: return@mapNotNull null
                comp to providerName
            }.toMap()
        )
    }

    private suspend fun getState() =
        dataStore.data.firstOrNull()?.let { createState(it) } ?: State()

    /**
     * Returns the [GlanceId] of the App Widgets installed for a particular provider.
     */
    suspend fun <T : GlanceAppWidget> getGlanceIds(provider: Class<T>): List<GlanceId> {
        val state = getState()
        val providerName = requireNotNull(provider.canonicalName) { "no canonical provider name" }
        val receivers = state.providerNameToReceivers[providerName] ?: return emptyList()
        return receivers.flatMap { receiver ->
            appWidgetManager.getAppWidgetIds(receiver).map { AppWidgetId(it) }
        }
    }

    /**
     * Retrieve the sizes for a given App Widget, if provided by the host.
     *
     * The list of sizes will be extracted from the App Widget options bundle, using the content of
     * [AppWidgetManager.OPTION_APPWIDGET_SIZES] if provided. If not, and if
     * [AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT] and similar are provided, the landscape and
     * portrait sizes will be estimated from those and returned. Otherwise, the list will contain
     * [DpSize.Zero] only.
     */
    suspend fun getAppWidgetSizes(glanceId: GlanceId): List<DpSize> {
        require(glanceId is AppWidgetId) { "This method only accepts App Widget Glance Id" }
        val bundle = appWidgetManager.getAppWidgetOptions(glanceId.appWidgetId)
        return bundle.extractAllSizes { DpSize.Zero }
    }

    /**
     * Retrieve the platform AppWidget ID from the provided GlanceId
     *
     * Important: Do NOT use appwidget ID as identifier, instead create your own and store them in
     * the GlanceStateDefinition. This method should only be used for compatibility or IPC
     * communication reasons in conjunction with [getGlanceIdBy]
     */
    fun getAppWidgetId(glanceId: GlanceId): Int {
        require(glanceId is AppWidgetId) { "This method only accepts App Widget Glance Id" }
        return glanceId.appWidgetId
    }

    /**
     * Retrieve the GlanceId of the provided AppWidget ID.
     *
     * @throws IllegalArgumentException if the provided id is not associated with an existing
     * GlanceId
     */
    fun getGlanceIdBy(appWidgetId: Int): GlanceId {
        requireNotNull(appWidgetManager.getAppWidgetInfo(appWidgetId)) {
            "Invalid AppWidget ID."
        }
        return AppWidgetId(appWidgetId)
    }

    /**
     * Retrieve the GlanceId from the configuration activity intent or null if not valid
     */
    fun getGlanceIdBy(configurationIntent: Intent): GlanceId? {
        val appWidgetId = configurationIntent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return null
        }

        return AppWidgetId(appWidgetId)
    }

    /**
     * Request to pin the [GlanceAppWidget] of the given receiver on the current launcher
     * (if supported).
     *
     * Note: the request is only supported for SDK 26 and beyond, for lower versions this method
     * will be no-op and return false.
     *
     * @param receiver the target [GlanceAppWidgetReceiver] class
     * @param preview the instance of the GlanceAppWidget to compose the preview that will be shown
     * in the request dialog. When not provided the app widget previewImage (as defined in the
     * meta-data) will be used instead, or the app's icon if not available either.
     * @param previewState the state (as defined by the [GlanceAppWidget.stateDefinition] to use for
     * the preview
     * @param successCallback a [PendingIntent] to be invoked if the app widget pinning is accepted
     * by the user
     *
     * @return true if the request was successfully sent to the system, false otherwise
     *
     * @see AppWidgetManager.requestPinAppWidget for more information and limitations
     */
    suspend fun <T : GlanceAppWidgetReceiver> requestPinGlanceAppWidget(
        receiver: Class<T>,
        preview: GlanceAppWidget? = null,
        previewState: Any? = null,
        successCallback: PendingIntent? = null,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }
        if (AppWidgetManagerApi26Impl.isRequestPinAppWidgetSupported(appWidgetManager)) {
            val target = ComponentName(context.packageName, receiver.name)
            val previewBundle = Bundle().apply {
                if (preview != null) {
                    val info = appWidgetManager.installedProviders.first {
                        it.provider == target
                    }
                    val snapshot = preview.compose(
                        context = context,
                        id = AppWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID),
                        state = previewState,
                        options = Bundle.EMPTY,
                        size = info.getMinSize(context.resources.displayMetrics),
                    )
                    putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, snapshot)
                }
            }
            return AppWidgetManagerApi26Impl.requestPinAppWidget(
                appWidgetManager,
                target,
                previewBundle,
                successCallback
            )
        }
        return false
    }

    /** Check which receivers still exist, and clean the data store to only keep those. */
    internal suspend fun cleanReceivers() {
        val packageName = context.packageName
        val receivers = appWidgetManager.installedProviders
            .filter { it.provider.packageName == packageName }
            .map { it.provider.className }
            .toSet()
        dataStore.updateData { prefs ->
            val knownReceivers = prefs[providersKey] ?: return@updateData prefs
            val toRemove = knownReceivers.filter { it !in receivers }
            if (toRemove.isEmpty()) return@updateData prefs
            prefs.toMutablePreferences().apply {
                this[providersKey] = knownReceivers - toRemove
                toRemove.forEach { receiver -> remove(providerKey(receiver)) }
            }.toPreferences()
        }
    }

    @VisibleForTesting
    internal suspend fun listKnownReceivers(): Collection<String>? =
        dataStore.data.firstOrNull()?.let { it[providersKey] }

    private companion object {
        private val Context.appManagerDataStore
            by preferencesDataStore(name = "GlanceAppWidgetManager")
        private var dataStoreSingleton: DataStore<Preferences>? = null
        private val providersKey = stringSetPreferencesKey("list::Providers")

        private fun providerKey(provider: String) = stringPreferencesKey("provider:$provider")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private object AppWidgetManagerApi26Impl {

        @DoNotInline
        fun isRequestPinAppWidgetSupported(manager: AppWidgetManager) =
            manager.isRequestPinAppWidgetSupported

        @DoNotInline
        fun requestPinAppWidget(
            manager: AppWidgetManager,
            target: ComponentName,
            extras: Bundle?,
            successCallback: PendingIntent?,
        ) = manager.requestPinAppWidget(target, extras, successCallback)
    }
}

private fun Map<ComponentName, String>.reverseMapping(): Map<String, List<ComponentName>> =
    entries.groupBy({ it.value }, { it.key })
