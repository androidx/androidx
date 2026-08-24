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
import androidx.annotation.RestrictTo
import androidx.collection.MutableIntList
import androidx.collection.mutableIntListOf

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
     * Queries package broadcast receivers matching [AppWidgetManager.ACTION_APPWIDGET_UPDATE],
     * resolves receivers matching [widgetName], and collects active appWidgetIds per
     * [ComponentName].
     */
    fun findAppWidgetIdsForWidgetName(
        widgetName: String,
        widgetIds: Set<String>?,
    ): Map<ComponentName, IntArray> {
        if (widgetIds != null && widgetIds.isEmpty()) {
            return emptyMap()
        }

        val componentToAppWidgetIds = mutableMapOf<ComponentName, MutableIntList>()
        val packageManager = context.packageManager
        val updateIntent =
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                setPackage(context.packageName)
            }
        val widgetReceivers =
            packageManager.queryBroadcastReceivers(updateIntent, PackageManager.GET_META_DATA)
        val seenComponents = hashSetOf<ComponentName>()

        for (idx in widgetReceivers.indices) {
            val resolveInfo = widgetReceivers[idx]
            val receiverInfo = resolveInfo.activityInfo ?: continue
            val componentName = ComponentName(receiverInfo.packageName, receiverInfo.name)
            if (!seenComponents.add(componentName)) continue
            if (!isReceiverForWidgetName(receiverInfo, widgetName)) continue

            val matchingIds = getMatchingAppWidgetIds(componentName, widgetIds)
            if (matchingIds.isNotEmpty()) {
                componentToAppWidgetIds[componentName] = matchingIds
            }
        }

        return componentToAppWidgetIds.mapValues { (_, ids) ->
            IntArray(ids.size) { index -> ids[index] }
        }
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
     * Checks if the broadcast receiver represented by [receiverInfo] matches the target
     * [widgetName] by checking manifest meta-data or reflectively reading
     * [GlanceAdaptiveWidgetReceiver.widgetName].
     */
    private fun isReceiverForWidgetName(receiverInfo: ActivityInfo, widgetName: String): Boolean {
        val metaDataWidgetName =
            receiverInfo.metaData?.getString(GlanceAdaptiveWidgetReceiver.META_DATA_WIDGET_NAME)
        if (metaDataWidgetName != null) {
            return metaDataWidgetName == widgetName
        }

        return runCatching {
                val receiverClass = context.classLoader.loadClass(receiverInfo.name)
                if (GlanceAdaptiveWidgetReceiver::class.java.isAssignableFrom(receiverClass)) {
                    val instance =
                        receiverClass.getDeclaredConstructor().newInstance()
                            as GlanceAdaptiveWidgetReceiver
                    instance.widgetName == widgetName
                } else {
                    false
                }
            }
            .getOrDefault(false)
    }

    /**
     * Checks whether an individual platform integer [appWidgetId] matches the requested collection
     * of [widgetIds].
     */
    private fun matchesWidgetId(appWidgetId: Int, widgetIds: Set<String>?): Boolean {
        if (widgetIds == null) return true
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val storedWidgetId =
            options?.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID) ?: return false

        return storedWidgetId in widgetIds
    }
}
