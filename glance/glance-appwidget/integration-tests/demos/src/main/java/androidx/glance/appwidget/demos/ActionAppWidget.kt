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

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionLaunchActivity
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.action.toParametersKey
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionLaunchActivity
import androidx.glance.appwidget.action.actionLaunchBroadcastReceiver
import androidx.glance.appwidget.action.actionLaunchService
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.unit.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle

class ActionAppWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    @Composable
    override fun Content() {
        Column(
            modifier = GlanceModifier.padding(R.dimen.external_padding).fillMaxSize()
                .appWidgetBackground().cornerRadius(R.dimen.corner_radius),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                SelectableActionItem(label = "Activities", index = 0)
                SelectableActionItem(label = "Services", index = 1)
                SelectableActionItem(label = "Broadcasts", index = 2)
            }

            when (currentState<Preferences>()[selectedItemKey] ?: 0) {
                0 -> LaunchActivityActions()
                1 -> LaunchServiceActions()
                2 -> LaunchBroadcastReceiverActions()
                else -> throw IllegalArgumentException("Wrong index selected")
            }
        }
    }
}

private val selectedItemKey = intPreferencesKey("selectedItemKey")
private val launchMessageKey = ActionParameters.Key<String>("launchMessageKey")

@Composable
private fun SelectableActionItem(label: String, index: Int) {
    val style = if (index == currentState<Preferences>()[selectedItemKey] ?: 0) {
        TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline,
        )
    } else {
        TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            textDecoration = TextDecoration.None,
            color = ColorProvider(
                Color.Black.copy(alpha = 0.3f),
                Color.White.copy(alpha = 0.3f)
            )
        )
    }
    Text(
        text = label,
        style = style,
        modifier = GlanceModifier
            .padding(8.dp)
            .clickable(
                actionRunCallback<UpdateAction>(
                    actionParametersOf(
                        selectedItemKey.toParametersKey() to index
                    )
                )
            )
    )
}

@Composable
private fun LaunchActivityActions() {
    Button(
        text = "Intent",
        onClick = actionLaunchActivity(
            Intent(LocalContext.current, ActionDemoActivity::class.java)
        )
    )
    Button(
        text = "Target class",
        onClick = actionLaunchActivity<ActionDemoActivity>(),
    )
    Button(
        text = "Target class with params",
        onClick = actionLaunchActivity<ActionDemoActivity>(
            actionParametersOf(
                launchMessageKey to "Launch activity by target class"
            )
        )
    )
    Button(
        text = "Component name",
        onClick = actionLaunchActivity(
            ComponentName(LocalContext.current, ActionDemoActivity::class.java)
        )
    )
    Button(
        text = "Component name with params",
        onClick = actionLaunchActivity(
            ComponentName(LocalContext.current, ActionDemoActivity::class.java),
            actionParametersOf(
                launchMessageKey to "Launch activity by component name"
            )
        )
    )
}

@Composable
private fun LaunchServiceActions() {
    Button(
        text = "Intent",
        onClick = actionLaunchService(
            Intent(LocalContext.current, ActionDemoService::class.java)
        )
    )
    Button(
        text = "Target class",
        onClick = actionLaunchService<ActionDemoService>()
    )
    Button(
        text = "In foreground",
        onClick = actionLaunchService<ActionDemoService>(isForegroundService = true)
    )
    Button(
        text = "Component name",
        onClick = actionLaunchService(
            ComponentName(LocalContext.current, ActionDemoService::class.java)
        )
    )
}

@Composable
private fun LaunchBroadcastReceiverActions() {
    Button(
        text = "Intent",
        onClick = actionLaunchBroadcastReceiver(
            Intent(LocalContext.current, ActionAppWidgetReceiver::class.java)
        )
    )
    Button(
        text = "Action",
        onClick = actionLaunchBroadcastReceiver(
            AppWidgetManager.ACTION_APPWIDGET_UPDATE
        )
    )
    Button(
        text = "Target class",
        onClick = actionLaunchBroadcastReceiver<ActionAppWidgetReceiver>()
    )
    Button(
        text = "Component name",
        onClick = actionLaunchBroadcastReceiver(
            ComponentName(LocalContext.current, ActionAppWidgetReceiver::class.java)
        )
    )
}

/**
 * Action to update the [selectedItemKey] value whenever users clicks on text
 */
class UpdateAction : ActionCallback {
    override suspend fun onRun(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val actionAppWidget = ActionAppWidget()
        actionAppWidget.updateAppWidgetState<Preferences>(context, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[selectedItemKey] = parameters[selectedItemKey.toParametersKey()] ?: 0
            }
        }
        actionAppWidget.update(context, glanceId)
    }
}

/**
 * Placeholder activity to launch via [actionLaunchActivity]
 */
class ActionDemoActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        setContent {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                val message = intent.getStringExtra(launchMessageKey.name) ?: "Not found"
                androidx.compose.material.Text(message)
            }
        }
        Log.d(this::class.simpleName, "Action Demo Activity: ${intent.extras}")
    }
}

/**
 * Placeholder service to launch via [actionLaunchService]
 */
class ActionDemoService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(this::class.simpleName, "Action Demo Service: $intent")
        return super.onStartCommand(intent, flags, startId)
    }
}

class ActionAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ActionAppWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(this::class.simpleName, "Action Demo Broadcast: $intent")
    }
}