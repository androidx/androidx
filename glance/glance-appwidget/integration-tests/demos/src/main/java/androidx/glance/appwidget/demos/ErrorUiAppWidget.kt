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

package androidx.glance.appwidget.demos

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.background
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import kotlin.math.roundToInt

/**
 * Demonstrates different methods of handling errors. A widget can use either the default Glance
 * error ui, provide a custom layout, or ignore the error and not make any change to the ui state.
 */
class ErrorUiAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = ErrorUiAppWidgetConfigurationRepo(context = context, glanceId = id)

        provideContent {
            val errorBehavior = repo.getOnErrorBehavior()

            Content(errorBehavior)
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { Content() }
    }

    @Composable
    private fun Content(
        errorBehavior: OnErrorBehavior = OnErrorBehavior.Default,
    ) {
        val size = LocalSize.current
        Column(
            modifier =
                GlanceModifier.fillMaxSize()
                    .background(day = Color.LightGray, night = Color.DarkGray)
                    .padding(8.dp),
        ) {
            Text(
                "Error UI Demo. Method: $errorBehavior",
                modifier = GlanceModifier.fillMaxWidth().wrapContentHeight(),
                style =
                    TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
            )
            Box(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Error UI triggers if width or height reach 400 dp in any orientation.",
                    style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp)
                )
                check(size.width < 400.dp && size.height < 400.dp) { "Too large now!" }
            }
            Text(
                " Current size: ${size.width.value.roundToInt()} dp x " +
                    "${size.height.value.roundToInt()} dp"
            )
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable
    ) {
        fun showCustomError() {
            // Optionally, a custom error view can also be created.
            val rv = RemoteViews(context.packageName, R.layout.error_ui_app_widget_on_error_layout)
            rv.setTextViewText(
                R.id.error_text_view,
                "Error was thrown. \nThis is a custom view \nError Message: `${throwable.message}`"
            )
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, rv)
        }

        val repo = ErrorUiAppWidgetConfigurationRepo(context = context, glanceId = glanceId)

        when (repo.getOnErrorBehavior()) {
            OnErrorBehavior.Default ->
                super.onCompositionError(context, glanceId, appWidgetId, throwable)
            OnErrorBehavior.Custom -> showCustomError()
            OnErrorBehavior.Ignore -> Unit // do nothing beyond the logging
        }

        // onCreateErrorLayout is a good place to perform logging.
        Log.w("Error App", "onCreateErrorLayout called.")
    }
}

class ErrorUiAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ErrorUiAppWidget()
}
