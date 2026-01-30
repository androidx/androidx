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
@file:Suppress("RestrictedApiAndroidX")

package androidx.glance.appwidget.remotecompose

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.GlanceComponents

internal typealias TranslationActionMap = MutableList<Pair<Int, PendingIntent>>

internal class TranslationContext(
    val context: Context,
    val remoteComposeContext: RemoteComposeContext,
    val appWidgetId: Int,
    val layoutSize: DpSize,
    val actionMap: TranslationActionMap,
    val glanceComponents: GlanceComponents,
    val actionBroadcastReceiver: ComponentName?,
) {
    private var nextId = 0
    private var nextActionId = Int.MIN_VALUE

    fun nextId(): Int {
        return (nextId++)
    }

    fun nextActionId(): Int {
        return (nextActionId++)
    }
}

/**
 * Represents a remote compose document that has been created from glance
 *
 * a Single is one document. A SizeMap is a map of documents to widget sizes.
 */
internal sealed interface GlanceToRemoteComposeTranslation {
    class Single(
        val remoteComposeContext: RemoteComposeContext,
        val actionMap: TranslationActionMap,
    ) : GlanceToRemoteComposeTranslation

    class SizeMap(val results: List<Pair<DpSize, Single>>) : GlanceToRemoteComposeTranslation
}
