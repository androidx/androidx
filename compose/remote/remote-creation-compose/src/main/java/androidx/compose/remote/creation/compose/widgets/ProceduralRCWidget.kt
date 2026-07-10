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
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteComposeBuffer
import java.io.ByteArrayInputStream
import kotlin.jvm.java

/** Basic procedural implementation for a widget */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ProceduralRCWidget(public val Content: (Context, Int) -> RemoteComposeBuffer?) :
    AbstractRCWidget() {

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun createRemoteView(
        context: Context,
        appWidgetManager: AppWidgetManager,
        provider: RemoteComposeWidget,
        widgetId: Int,
    ) {
        val document = Content(context, widgetId)
        if (document != null) {
            val widget = getRemoteViewFromBuffer(context, widgetId, document)
            appWidgetManager.updateAppWidget(widgetId, widget)
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun updateRemoteView(
        context: Context,
        appWidgetManager: AppWidgetManager,
        provider: RemoteComposeWidget,
        widgetId: Int,
        lambdaId: Int,
    ) {
        val document = Content(context, widgetId)
        if (document != null) {
            val widget = getRemoteViewFromBuffer(context, widgetId, document)
            appWidgetManager.updateAppWidget(widgetId, widget)
        }
    }

    @SuppressLint("ResourceType")
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public fun getRemoteViewFromBuffer(
        context: Context,
        id: Int,
        buffer: RemoteComposeBuffer,
    ): RemoteViews {
        val bufferSize = buffer.buffer.size()
        val bytes = ByteArray(bufferSize)
        val b = ByteArrayInputStream(buffer.buffer.buffer, 0, bufferSize)
        b.read(bytes)
        val r = RemoteViews.DrawInstructions.Builder(java.util.List.of<ByteArray?>(bytes))
        val rv = RemoteViews(r.build())
        val intent = Intent(context, javaClass)
        intent.setAction(ACTION)
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
        rv.setOnClickPendingIntent(567, pendingIntent)
        return rv
    }
}
