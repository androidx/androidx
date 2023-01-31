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

package androidx.glance.appwidget.macrobenchmark.target

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.Tracing
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.session.GlanceSessionManager
import androidx.glance.text.Text
import kotlin.math.roundToInt

open class BasicAppWidget : GlanceAppWidget() {
    init {
        // Ensure tracing is enabled before starting updates.
        Tracing.enabled.set(true)
    }
    override val sizeMode: SizeMode = SizeMode.Single

    @Composable
    override fun Content() {
        val size = LocalSize.current
        // Even though this widget does not use it, accessing state will ensure that this
        // is recomposed every time state updates, which is useful for testing.
        currentState<Preferences>()
        Column(
            modifier = GlanceModifier.fillMaxSize(),
        ) {
            Text(
                " Current size: ${size.width.value.roundToInt()} dp x " +
                    "${size.height.value.roundToInt()} dp"
            )
        }
    }
}

class BasicAppWidgetWithSession : BasicAppWidget() {
    override val sessionManager = GlanceSessionManager
}

class BasicAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BasicAppWidget()
}

class BasicAppWidgetWithSessionReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BasicAppWidgetWithSession()
}
