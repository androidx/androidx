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

package androidx.compose.remote.integration.demos.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.demos.ScrollableList
import androidx.compose.runtime.Composable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@Suppress("RestrictedApiAndroidX")
class ListRCWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, wm: AppWidgetManager, widgetIds: IntArray) {
        widgetIds.forEach { widgetId ->
            goAsync {
                val bytes = listWidget(context.applicationContext, "ListRCWidget")

                val widget = RemoteViews(DrawInstructions(bytes))

                wm.updateAppWidget(widgetId, widget)
            }
        }
    }
}

suspend fun listWidget(context: Context, name: String): ByteArray {
    return record(context.applicationContext) {
        ScrollableList(modifier = RemoteModifier.fillMaxSize(), name = name)
    }
}

@OptIn(ExperimentalRemoteCreationComposeApi::class, ExperimentalRemoteCreationApi::class)
@Suppress("RestrictedApiAndroidX")
suspend fun record(context: Context, content: @RemoteComposable @Composable () -> Unit): ByteArray =
    withContext(Dispatchers.Main) {
        captureSingleRemoteDocument(
                context = context,
                creationDisplayInfo = createCreationDisplayInfo(context),
                profile = RcPlatformProfiles.WIDGETS_V6,
                content = content,
            )
            .bytes
    }

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
fun DrawInstructions(bytes: ByteArray): RemoteViews.DrawInstructions {
    return RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
}
