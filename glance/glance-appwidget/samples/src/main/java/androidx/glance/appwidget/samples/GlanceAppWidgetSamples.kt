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

@file:OptIn(ExperimentalGlanceApi::class)

package androidx.glance.appwidget.samples

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD
import android.content.Context
import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LocalAppWidgetOptions
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.text.Text
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** This sample demonstrates how to do create a simple [GlanceAppWidget] and update the widget. */
@Sampled
fun provideGlanceSample() {
    class MyWidget : GlanceAppWidget() {

        val Context.myWidgetStore by preferencesDataStore("MyWidget")
        val Name = stringPreferencesKey("name")

        override suspend fun provideGlance(context: Context, id: GlanceId) {
            // Load initial data needed to render the AppWidget here. Prefer doing heavy work before
            // provideContent, as the provideGlance function will timeout shortly after
            // provideContent is called.
            val store = context.myWidgetStore
            val initial = store.data.first()

            provideContent {
                // Observe your sources of data, and declare your @Composable layout.
                val data by store.data.collectAsState(initial)
                val scope = rememberCoroutineScope()
                Text(
                    text = "Hello ${data[Name]}",
                    modifier =
                        GlanceModifier.clickable("changeName") {
                            scope.launch {
                                store.updateData {
                                    it.toMutablePreferences().apply { set(Name, "Changed") }
                                }
                            }
                        }
                )
            }
        }

        // Updating the widget from elsewhere in the app:
        suspend fun changeWidgetName(context: Context, newName: String) {
            context.myWidgetStore.updateData {
                it.toMutablePreferences().apply { set(Name, newName) }
            }
            // Call update/updateAll in case a Worker for the widget is not currently running. This
            // is not necessary when updating data from inside of the composition using lambdas,
            // since a Worker will be started to run lambda actions.
            MyWidget().updateAll(context)
        }
    }
}

// Without this declaration, the class reference to WeatherWidgetWorker::class.java below does not
// work because it is defined in that function after it is referenced. This will not show up in the
// sample.
class WeatherWidgetWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    override suspend fun doWork() = Result.success()
}

/**
 * This sample demonstrates how to do periodic updates using a unique periodic [CoroutineWorker].
 */
@Sampled
fun provideGlancePeriodicWorkSample() {
    class WeatherWidget : GlanceAppWidget() {

        val Context.weatherWidgetStore by preferencesDataStore("WeatherWidget")
        val CurrentDegrees = intPreferencesKey("currentDegrees")

        suspend fun DataStore<Preferences>.loadWeather() {
            updateData { prefs ->
                prefs.toMutablePreferences().apply {
                    this[CurrentDegrees] = Random.Default.nextInt()
                }
            }
        }

        override suspend fun provideGlance(context: Context, id: GlanceId) {
            coroutineScope {
                val store = context.weatherWidgetStore
                val currentDegrees =
                    store.data.map { prefs -> prefs[CurrentDegrees] }.stateIn(this@coroutineScope)

                // Load the current weather if there is not a current value present.
                if (currentDegrees.value == null) store.loadWeather()

                // Create unique periodic work to keep this widget updated at a regular interval.
                WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(
                        "weatherWidgetWorker",
                        ExistingPeriodicWorkPolicy.KEEP,
                        PeriodicWorkRequest.Builder(
                                WeatherWidgetWorker::class.java,
                                15.minutes.toJavaDuration()
                            )
                            .setInitialDelay(15.minutes.toJavaDuration())
                            .build()
                    )

                // Note: you can also set `android:updatePeriodMillis` to control how often the
                // launcher requests an update, but this does not support periods less than
                // 30 minutes.

                provideContent {
                    val degrees by currentDegrees.collectAsState()
                    Text("Current weather: $degrees °F")
                }
            }
        }
    }

    class WeatherWidgetWorker(appContext: Context, params: WorkerParameters) :
        CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            WeatherWidget().apply {
                applicationContext.weatherWidgetStore.loadWeather()
                // Call update/updateAll in case a Worker for the widget is not currently running.
                updateAll(applicationContext)
            }
            return Result.success()
        }
    }
}

/** This sample demonstrates how to implement [GlanceAppWidget.providePreview], */
@Sampled
fun providePreviewSample() {
    class MyWidgetWithPreview : GlanceAppWidget() {
        override suspend fun provideGlance(context: Context, id: GlanceId) {
            provideContent {
                val widgetCategory =
                    LocalAppWidgetOptions.current.getInt(
                        AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY
                    )
                Content(isPreview = false, widgetCategory)
            }
        }

        override suspend fun providePreview(context: Context, widgetCategory: Int) {
            provideContent { Content(isPreview = true, widgetCategory) }
        }

        @Composable
        fun Content(
            isPreview: Boolean,
            widgetCategory: Int,
        ) {
            val text = if (isPreview) "preview" else "bound widget"
            Text("This is a $text.")
            // Avoid showing personal information if this preview or widget is showing on the
            // lockscreen/keyguard.
            val isKeyguardWidget = widgetCategory.and(WIDGET_CATEGORY_KEYGUARD) != 0
            if (!isKeyguardWidget) {
                Text("Some personal info.")
            }
        }
    }
}
