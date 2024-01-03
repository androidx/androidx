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
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class GlanceAppWidgetDemoActivity : ComponentActivity() {

    lateinit var manager: GlanceAppWidgetManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager = GlanceAppWidgetManager(this)
        updateView()
    }

    override fun onResume() {
        super.onResume()
        updateView()
    }

    private fun updateView() {
        lifecycleScope.launch {
            // Discover the GlanceAppWidget
            val appWidgetManager = AppWidgetManager.getInstance(this@GlanceAppWidgetDemoActivity)
            val receivers = appWidgetManager.installedProviders
                .filter { it.provider.packageName == packageName }
                .map { it.provider.className }
            val data = receivers.mapNotNull { receiverName ->
                val receiverClass = Class.forName(receiverName)
                if (!GlanceAppWidgetReceiver::class.java.isAssignableFrom(receiverClass)) {
                    return@mapNotNull null
                }
                val receiver = receiverClass.getDeclaredConstructor()
                    .newInstance() as GlanceAppWidgetReceiver
                val provider = receiver.glanceAppWidget.javaClass
                ProviderData(
                    provider = provider,
                    receiver = receiver.javaClass,
                    appWidgets = manager.getGlanceIds(provider).map { id ->
                        AppWidgetDesc(appWidgetId = id, sizes = manager.getAppWidgetSizes(id))
                    })
            }

            setContent {
                val scope = rememberCoroutineScope()
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "Installed App Widgets",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(data) {
                            ShowProvider(it) {
                                scope.launch {
                                    manager.requestPinGlanceAppWidget(
                                        receiver = it.receiver,
                                        preview = it.provider.getDeclaredConstructor()
                                            .newInstance(),
                                        previewState = emptyPreferences()
                                    )
                                }
                            }
                            Divider(color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShowProvider(providerData: ProviderData, onProviderClicked: (ProviderData) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onProviderClicked(providerData)
            }
    ) {
        Text(providerData.provider.simpleName, fontWeight = FontWeight.Medium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
                .background(Color.LightGray)
        ) {
            providerData.appWidgets.forEachIndexed { index, widget ->
                ShowAppWidget(index, widget)
                Divider(color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun ShowAppWidget(index: Int, widgetDesc: AppWidgetDesc) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Instance ${index + 1}")
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            widgetDesc.sizes.sortedBy { it.width.value * it.height.value }
                .forEachIndexed { index, size ->
                    Text(
                        String.format(
                            "Size ${index + 1}: %.0f dp x %.0f dp",
                            size.width.value,
                            size.height.value
                        )
                    )
                }
        }
    }
}

data class ProviderData(
    val provider: Class<out GlanceAppWidget>,
    val receiver: Class<out GlanceAppWidgetReceiver>,
    val appWidgets: List<AppWidgetDesc>,
)

data class AppWidgetDesc(
    val appWidgetId: GlanceId,
    val sizes: List<DpSize>,
)
