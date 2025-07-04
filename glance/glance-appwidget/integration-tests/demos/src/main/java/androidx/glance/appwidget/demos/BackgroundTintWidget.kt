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
import androidx.collection.floatListOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackgroundTintWidgetBroadcastReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = BackgroundTintWidget()
}

/** Demonstrates tinting background drawables with [ColorFilter] and applying opacity with alpha. */
class BackgroundTintWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode
        get() = SizeMode.Exact

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = AlphaRepo(context, id)
        val alphaFlow = withContext(Dispatchers.IO) { repo.load() }

        provideContent {
            val alpha by alphaFlow.collectAsState(0.5f)

            Content(
                alpha = alpha,
                alphaToggleAction = {
                    GlobalScope.launch { withContext(Dispatchers.IO) { repo.changeToNextAlpha() } }
                },
            )
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { Content(alpha = 0.5f, alphaToggleAction = {}) }
    }

    @Composable
    private fun Content(alpha: Float, alphaToggleAction: () -> Unit) {
        GlanceTheme {
            Scaffold(titleBar = { DemoTitleBar(alphaToggleAction) }) {
                LazyColumn(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        // Tint a <shape> background
                        DemoText(
                            text = "Shape drawable bg",
                            modifier =
                                GlanceModifier.background(
                                    ImageProvider(R.drawable.shape_btn_demo),
                                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                                ),
                        )
                    }
                    item {
                        // tint an AVD background
                        DemoText(
                            text = "AVD bg",
                            modifier =
                                GlanceModifier.background(
                                    ImageProvider(R.drawable.ic_android),
                                    colorFilter = ColorFilter.tint(ColorProvider(Color.Cyan)),
                                ),
                        )
                    }
                    item { DemoText(text = "with alpha = $alpha") }
                    item {
                        // Tint a <shape> background and apply alpha
                        DemoText(
                            text = "Shape drawable bg",
                            modifier =
                                GlanceModifier.background(
                                    ImageProvider(R.drawable.shape_btn_demo),
                                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                                    alpha = alpha,
                                ),
                        )
                    }
                    item {
                        // tint an AVD background and apply alpha
                        DemoText(
                            text = "AVD bg",
                            modifier =
                                GlanceModifier.background(
                                    ImageProvider(R.drawable.ic_android),
                                    colorFilter = ColorFilter.tint(ColorProvider(Color.Cyan)),
                                    alpha = alpha,
                                ),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun DemoText(text: String, modifier: GlanceModifier = GlanceModifier) {
        Text(
            text = text,
            style = TextStyle(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            modifier = modifier.padding(top = 20.dp).height(60.dp).fillMaxWidth(),
        )
    }

    @Composable
    private fun DemoTitleBar(alphaToggleAction: () -> Unit) {
        TitleBar(
            startIcon = ImageProvider(R.drawable.ic_demo_app),
            title = "",
            actions = {
                CircleIconButton(
                    imageProvider = ImageProvider(R.drawable.ic_opacity),
                    backgroundColor = null,
                    contentColor = GlanceTheme.colors.secondary,
                    contentDescription = "toggle alpha",
                    onClick = alphaToggleAction,
                    key = "${LocalSize.current.width} ${LocalSize.current.height}",
                )
            },
        )
    }

    /** A datastore backed repository that produces alpha value for the demo */
    private class AlphaRepo(val context: Context, glanceId: GlanceId) {
        private val preferencesKey = intPreferencesKey("ALPHA_VALUES_$glanceId")

        /** Mimics a backend load and returns a flow to listen to updates */
        suspend fun load() =
            context.datastore.data.map { preferences ->
                AlphaValues[coerceToValidAlphaIndex(preferences[preferencesKey])]
            }

        /** Updates the repository to select next alpha */
        suspend fun changeToNextAlpha() {
            context.datastore.edit { preferences ->
                val currentIndex = coerceToValidAlphaIndex(preferences[preferencesKey])
                val newIndex = (currentIndex + 1) % AlphaValues.size
                preferences[preferencesKey] = newIndex
            }
        }

        companion object {
            private val AlphaValues = floatListOf(0.1f, 0.2f, 0.5f, 0.6f, 0.9f, 1f, 0f)
            private val Context.datastore by preferencesDataStore("alphaRepo")

            private fun coerceToValidAlphaIndex(index: Int?) =
                index?.coerceIn(0, AlphaValues.size) ?: 0
        }
    }
}
