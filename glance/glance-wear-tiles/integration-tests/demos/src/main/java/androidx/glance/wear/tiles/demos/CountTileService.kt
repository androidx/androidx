/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.wear.tiles.demos

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.wear.tiles.action.ActionCallback
import androidx.glance.wear.tiles.action.actionRunCallback
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.padding
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.wear.tiles.GlanceTileService
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.wear.tiles.state.updateWearTileState

private val prefsCountKey = intPreferencesKey("count")

class CountTileService : GlanceTileService() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    @Composable
    override fun Content() {
        val currentCount = currentState<Preferences>()[prefsCountKey] ?: 0

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = currentCount.toString(),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
            Spacer(GlanceModifier.height(20.dp))
            Row(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    text = "+2",
                    modifier = GlanceModifier.padding(5.dp).background(Color.Cyan),
                    onClick = actionRunCallback<ClickAddAction>()
                )

                Spacer(GlanceModifier.width(10.dp))

                Button(
                    text = "*2",
                    modifier = GlanceModifier.padding(5.dp).background(Color.Green),
                    onClick = actionRunCallback<ClickMultiplyAction>()
                )

                Spacer(GlanceModifier.width(10.dp))

                Button(
                    text = "/2",
                    modifier = GlanceModifier.padding(5.dp).background(Color.Magenta),
                    onClick = actionRunCallback<ClickDivideAction>()
                )
            }
        }
    }
}

class ClickAddAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId
    ) {
        updateWearTileState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                set(prefsCountKey, (this[prefsCountKey] ?: 0) + 2)
            }
        }
    }
}

class ClickMultiplyAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId
    ) {
        updateWearTileState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                set(prefsCountKey, (this[prefsCountKey] ?: 0) * 2)
            }
        }
    }
}

class ClickDivideAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId
    ) {
        updateWearTileState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                set(prefsCountKey, ((this[prefsCountKey] ?: 0) / 2f).toInt())
            }
        }
    }
}