/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.appwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.collection.MutableIntList
import androidx.collection.mutableIntListOf
import java.lang.reflect.InvocationTargetException

/**
 * Repository responsible for resolving active platform AppWidget instances matching target widget
 * definitions and target widget instance identifiers.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class WidgetInstanceRepository(
    private val context: Context,
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context),
) {
    /**
     * Resolves the single receiver [ComponentName] declaring [widgetName] in the current package,
     * including disabled receivers.
     *
     * @throws IllegalArgumentException if no receiver, or more than one receiver, declares
     *   [widgetName].
     * @throws IllegalStateException if a [GlanceAdaptiveWidgetReceiver] subclass declared in the
     *   manifest cannot be instantiated to read its [GlanceAdaptiveWidgetReceiver.widgetName].
     */
    fun resolveReceiverComponent(widgetName: String): ComponentName {
        val receivers = allReceiversGroupedByWidgetName()
        val components = receivers[widgetName].orEmpty()
        require(components.isNotEmpty()) {
            "No GlanceAdaptiveWidgetReceiver in ${context.packageName} declares widgetName " +
                "$widgetName. Check for a typo or a missing <receiver> declaration."
        }
        require(components.size == 1) {
            buildString {
                append("widgetName $widgetName must be unique per app, but is declared by ")
                for (i in components.indices) {
                    if (i > 0) append(", ")
                    append(components[i].className)
                }
            }
        }
        return components[0]
    }

    /**
     * Resolves the receiver declaring [widgetName] with [resolveReceiverComponent], then collects
     * its active appWidgetIds matching [widgetIds], or all of them if [widgetIds] is null.
     *
     * @return The matching appWidgetIds keyed by the receiver [ComponentName], or an empty map if
     *   none match.
     * @throws IllegalArgumentException if [widgetName] cannot be resolved, see
     *   [resolveReceiverComponent].
     * @throws IllegalStateException if [widgetName] cannot be resolved, see
     *   [resolveReceiverComponent].
     */
    fun findAppWidgetIdsForWidgetName(
        widgetName: String,
        widgetIds: Set<String>?,
    ): Map<ComponentName, IntArray> {
        // Resolved first, so that an unknown widgetName fails even when nothing is targeted.
        val componentName = resolveReceiverComponent(widgetName)
        if (widgetIds != null && widgetIds.isEmpty()) {
            return emptyMap()
        }

        val matchingIds = getMatchingAppWidgetIds(componentName, widgetIds)
        if (matchingIds.isEmpty()) return emptyMap()
        return mapOf(componentName to IntArray(matchingIds.size) { index -> matchingIds[index] })
    }

    /** Retrieves active appWidgetIds for [componentName] and filters those matching [widgetIds]. */
    private fun getMatchingAppWidgetIds(
        componentName: ComponentName,
        widgetIds: Set<String>?,
    ): MutableIntList {
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        val matchingIds = mutableIntListOf()
        for (appWidgetId in appWidgetIds) {
            if (matchesWidgetId(appWidgetId, widgetIds)) {
                matchingIds.add(appWidgetId)
            }
        }
        return matchingIds
    }

    /**
     * Returns the receivers per [GlanceAdaptiveWidgetReceiver.widgetName] of the current package,
     * read with [readAllReceiversGroupedByWidgetName] on first use and then shared by every
     * repository of the process, see [receiversByPackage].
     */
    private fun allReceiversGroupedByWidgetName(): Map<String, List<ComponentName>> {
        val packageName = context.packageName
        synchronized(lock) { receiversByPackage[packageName] }
            ?.let { cached ->
                return cached
            }
        // Read outside the lock, as it queries PackageManager and instantiates receivers.
        val receivers = readAllReceiversGroupedByWidgetName()
        synchronized(lock) { receiversByPackage[packageName] = receivers }
        return receivers
    }

    /**
     * Reads the [GlanceAdaptiveWidgetReceiver.widgetName] of every receiver of
     * [AppWidgetManager.ACTION_APPWIDGET_UPDATE] declared in the current package, including
     * disabled ones, as an app can disable a widget receiver at runtime.
     */
    private fun readAllReceiversGroupedByWidgetName(): Map<String, List<ComponentName>> {
        val packageManager = context.packageManager
        val updateIntent =
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                setPackage(context.packageName)
            }
        val flags = PackageManager.MATCH_DISABLED_COMPONENTS
        val widgetReceivers =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryBroadcastReceivers(
                    updateIntent,
                    PackageManager.ResolveInfoFlags.of(flags.toLong()),
                )
            } else {
                // The ResolveInfoFlags overload is only available from API 33.
                @Suppress("DEPRECATION") packageManager.queryBroadcastReceivers(updateIntent, flags)
            }

        val result = HashMap<String, MutableList<ComponentName>>()
        for (idx in widgetReceivers.indices) {
            val receiverInfo = widgetReceivers[idx].activityInfo ?: continue
            val widgetName = readWidgetName(receiverInfo) ?: continue
            result
                .getOrPut(widgetName) { mutableListOf() }
                .add(ComponentName(receiverInfo.packageName, receiverInfo.name))
        }
        return result
    }

    /**
     * Reads the [GlanceAdaptiveWidgetReceiver.widgetName] of the receiver represented by
     * [receiverInfo] by instantiating it with its no-argument constructor, the same constructor the
     * platform uses to deliver broadcasts.
     *
     * @return The widgetName, or null if the receiver is not a [GlanceAdaptiveWidgetReceiver].
     * @throws IllegalStateException if the receiver cannot be instantiated or its widgetName cannot
     *   be read.
     */
    private fun readWidgetName(receiverInfo: ActivityInfo): String? {
        val receiverClass =
            try {
                context.classLoader.loadClass(receiverInfo.name)
            } catch (e: ClassNotFoundException) {
                // Not a receiver of this library: the platform reports it when delivering to it.
                return null
            }
        if (!GlanceAdaptiveWidgetReceiver::class.java.isAssignableFrom(receiverClass)) return null

        val receiver =
            try {
                val constructor = receiverClass.getDeclaredConstructor()
                constructor.isAccessible = true
                constructor.newInstance() as GlanceAdaptiveWidgetReceiver
            } catch (e: InvocationTargetException) {
                val cause = e.targetException ?: e
                if (cause is Error) throw cause
                throw IllegalStateException(cannotReadWidgetName(receiverInfo), cause)
            } catch (e: ReflectiveOperationException) {
                throw IllegalStateException(cannotReadWidgetName(receiverInfo), e)
            }
        return try {
            receiver.widgetName
        } catch (e: Exception) {
            throw IllegalStateException(cannotReadWidgetName(receiverInfo), e)
        }
    }

    private fun cannotReadWidgetName(receiverInfo: ActivityInfo): String =
        "Cannot read the widgetName of ${receiverInfo.name}. GlanceAdaptiveWidgetReceiver " +
            "subclasses need a no-argument constructor, and their constructor, initializers and " +
            "widgetName must not throw."

    /**
     * Checks whether an individual platform integer [appWidgetId] matches the requested collection
     * of [widgetIds].
     */
    private fun matchesWidgetId(appWidgetId: Int, widgetIds: Set<String>?): Boolean {
        if (widgetIds == null) return true
        val cachedWidgetId = WidgetOptionsCache.getCachedWidgetId(appWidgetId)
        if (cachedWidgetId != null) {
            return cachedWidgetId in widgetIds
        }

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val storedWidgetId =
            options?.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)?.takeIf {
                it.isNotBlank()
            } ?: return false

        WidgetOptionsCache.cacheWidgetOptionsIfMissing(appWidgetId, options)
        return storedWidgetId in widgetIds
    }

    companion object {
        private val lock = Any()

        /**
         * Receiver [ComponentName]s per [GlanceAdaptiveWidgetReceiver.widgetName], per package,
         * read from the manifest on first use. The set of receivers is fixed in the manifest for
         * the lifetime of the process, so it is read once per process and shared by every
         * repository, and so by every manager. A failure is not cached, so every lookup fails until
         * the receiver is fixed. Concurrent first lookups may each read the manifest, which is
         * harmless.
         */
        private val receiversByPackage = HashMap<String, Map<String, List<ComponentName>>>()

        @VisibleForTesting
        internal fun resetForTesting() {
            synchronized(lock) { receiversByPackage.clear() }
        }
    }
}
