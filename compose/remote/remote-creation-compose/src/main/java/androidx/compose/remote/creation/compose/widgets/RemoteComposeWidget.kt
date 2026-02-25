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

package androidx.compose.remote.creation.compose.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.runtime.Composable

/** provider for an app widget */
@OptIn(ExperimentalRemoteCreationComposeApi::class)
@SuppressLint("RestrictedApiAndroidX")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class RemoteComposeWidget(useCompose: Boolean = true) : AppWidgetProvider() {

    public companion object {
        public var sAppWidgetIds: IntArray? = null
        public var provider: RemoteComposeWidget? = null
    }

    public val widget: AbstractRCWidget

    init {
        if (useCompose) {
            RemoteComposeCreationComposeFlags.isRemoteApplierEnabled = false
            widget = RCWidget { context, widgetId -> Content(context, widgetId) }
        } else {
            widget = ProceduralRCWidget { context, widgetId ->
                ProceduralContent(context, widgetId)?.buffer
            }
        }
    }

    @Composable public open fun Content(context: Context, widgetId: Int) {}

    public open fun ProceduralContent(context: Context, widgetId: Int): RemoteComposeContext? {
        return null
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        provider = this
        sAppWidgetIds = appWidgetIds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            for (id in appWidgetIds) {
                widget.createRemoteView(context, appWidgetManager, this, id)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AbstractRCWidget.ACTION) {
            val id = intent.getIntExtra("id", -1)
            val widgetId = intent.getIntExtra("widgetId", -1)
            if (widgetId == -1) {
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                updateWidgets(context, widgetId, id)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public fun updateWidgets(context: Context, widgetId: Int, lambdaToRun: Int) {
        if (sAppWidgetIds == null) {
            return
        }
        val appWidgetManager = AppWidgetManager.getInstance(context)
        widget.updateRemoteView(context, appWidgetManager, this, widgetId, lambdaToRun)
    }
}
