/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.glance.appwidget.demos

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.wrapContentHeight
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.Text

/** Sample AppWidget that showcase the [AndroidRemoteViews] fallback composable. */
class RemoteViewsWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        Content()
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent {
        Content()
    }

    @Composable
    private fun Content() {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(Color.White),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            // Demonstrates a single item remote view layout
            val remoteViews =
                RemoteViews(LocalContext.current.packageName, R.layout.test_remote_views_single)
            AndroidRemoteViews(
                remoteViews = remoteViews,
                modifier =
                    GlanceModifier.fillMaxWidth()
                        .wrapContentHeight()
                        .cornerRadius(16.dp)
                        .background(Color.Red)
            )

            // Demonstrates a remote view layout being used as a container for regular glance
            // composables
            val flipper =
                RemoteViews(LocalContext.current.packageName, R.layout.test_remote_views_multiple)
            Text(text = "Container RemoteViews")
            Box(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentAlignment = Alignment.Center
            ) {
                AndroidRemoteViews(
                    remoteViews = flipper,
                    containerViewId = R.id.test_flipper_root,
                    modifier = GlanceModifier.wrapContentSize()
                ) {
                    Text(text = "First")
                    Text(text = "Second")
                }
            }
        }
    }
}

class RemoteViewsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RemoteViewsWidget()
}
