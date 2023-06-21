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

import android.content.Context
import androidx.annotation.Sampled
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.text.Text
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
                    modifier = GlanceModifier.clickable("changeName") {
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