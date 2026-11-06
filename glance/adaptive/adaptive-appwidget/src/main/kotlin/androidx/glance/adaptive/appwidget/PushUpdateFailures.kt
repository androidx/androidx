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

import androidx.collection.IntObjectMap
import androidx.glance.adaptive.core.GlanceAdaptiveUpdateException

/**
 * Collects the placements that one [BaseWidgetDelegate.pushUpdate] call fails to update, so that
 * the call completes the healthy work first and then throws a single
 * [GlanceAdaptiveUpdateException].
 *
 * A failed placement is reported under its widgetId only when that widgetId is known. Otherwise it
 * is reported as unidentified, and never under a guessed widgetId.
 */
internal class PushUpdateFailures(private val widgetName: String) {
    private val failedWidgetIds = LinkedHashSet<String>()
    private val errors = ArrayList<Exception>()
    private val summaries = ArrayList<String>()
    private var failedPlacementCount = 0
    private var unreadablePlacementCount = 0
    private var allFailedWidgetIdsKnown = true

    /**
     * Records that the options of [appWidgetId] could not be read, so that the placement could not
     * be resolved or sized. [widgetId] is its widgetId if already known without those options.
     */
    fun addUnreadableOptions(appWidgetId: Int, widgetId: String?, error: Exception) {
        addFailedPlacement(widgetId)
        unreadablePlacementCount++
        errors.add(error)
    }

    /**
     * Records that composing or delivering the update for [target] failed with [error], so that
     * none of its [appWidgetIds] received it. [intIdsToStringIds] maps each grouped `appWidgetId`
     * to the `widgetId` read from its options, if any.
     */
    fun addFailedRenderedTarget(
        target: RenderTarget,
        appWidgetIds: IntArray,
        intIdsToStringIds: IntObjectMap<String>,
        error: Exception,
    ) {
        for (i in appWidgetIds.indices) {
            val appWidgetId = appWidgetIds[i]
            val widgetId =
                intIdsToStringIds[appWidgetId] ?: WidgetOptionsCache.getCachedWidgetId(appWidgetId)
            addFailedPlacement(widgetId)
        }
        summaries.add(
            "${appWidgetIds.size} on surface ${target.surface} at ${target.dimensions.widthDp}x" +
                "${target.dimensions.heightDp}dp (${error.javaClass.simpleName})"
        )
        errors.add(error)
    }

    /** Throws the collected failures as one [GlanceAdaptiveUpdateException], if there are any. */
    fun throwIfAny() {
        if (errors.isEmpty()) return
        val message = buildString {
            append("pushUpdate failed for ")
            append(failedPlacementCount)
            append(" placement(s) of widgetName ")
            append(widgetName)
            append(": ")
            if (unreadablePlacementCount > 0) {
                append(unreadablePlacementCount)
                append(" whose options could not be read")
                if (summaries.isNotEmpty()) append("; ")
            }
            for (i in summaries.indices) {
                if (i > 0) append("; ")
                append(summaries[i])
            }
        }
        val exception =
            GlanceAdaptiveUpdateException(
                message = message,
                failedWidgetIds = failedWidgetIds.toSet(),
                allFailedWidgetIdsKnown = allFailedWidgetIdsKnown,
                cause = errors[0],
            )
        for (i in 1 until errors.size) {
            exception.addSuppressed(errors[i])
        }
        throw exception
    }

    private fun addFailedPlacement(widgetId: String?) {
        failedPlacementCount++
        if (widgetId != null) {
            failedWidgetIds.add(widgetId)
        } else {
            allFailedWidgetIdsKnown = false
        }
    }
}
