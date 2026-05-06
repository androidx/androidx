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
import androidx.compose.remote.creation.compose.capture.CapturedDocument
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.runBlocking

/** Widget implementation that takes a composable */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RCWidget(public val content: @Composable (Context, Int) -> Unit) :
    AbstractRCWidget() {

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun createRemoteView(
        context: Context,
        appWidgetManager: AppWidgetManager,
        provider: RemoteComposeWidget,
        widgetId: Int,
    ) {
        WidgetLambdaAction.clear()

        val widgetInformation = WidgetInformation(widgetId)

        runBlocking {
            captureSingleRemoteDocument(
                context = context,
                content = {
                    CompositionLocalProvider(LocalWidget.provides(widgetInformation)) {
                        content(context, widgetId)
                    }
                },
            )
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
        // First, get the document (in order to get the lambdas)
        WidgetLambdaAction.clear()

        val widgetInformation = WidgetInformation(widgetId)

        val doc = runBlocking {
            captureSingleRemoteDocument(
                context = context,
                content = {
                    CompositionLocalProvider(LocalWidget.provides(widgetInformation)) {
                        content(context, widgetId)
                    }
                },
            )
        }

        // then run the lambda...
        WidgetLambdaAction.run(lambdaId)
        // and recreate the document.
        WidgetLambdaAction.clear()

        val widget = getRemoteView(context, doc, provider, widgetId)
        appWidgetManager.updateAppWidget(widgetId, widget)
    }

    @SuppressLint("ResourceType")
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public fun getRemoteView(
        context: Context,
        document: CapturedDocument,
        provider: RemoteComposeWidget,
        widgetId: Int,
    ): RemoteViews {
        val r = RemoteViews.DrawInstructions.Builder(listOf(document.bytes))
        val rv = RemoteViews(r.build())
        for (i in 0 until WidgetLambdaAction.counter) {
            val intentId = 1000 * widgetId + i
            val intent = Intent(context, provider.javaClass)
            intent.setAction(ACTION)
            intent.putExtra("id", intentId)
            intent.putExtra("widgetId", widgetId)
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context.applicationContext,
                    intentId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            rv.setOnClickPendingIntent(intentId, pendingIntent)
        }
        return rv
    }
}
