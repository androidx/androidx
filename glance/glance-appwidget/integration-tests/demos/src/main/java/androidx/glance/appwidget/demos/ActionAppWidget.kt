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

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionLaunchActivity
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionLaunchActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import java.util.concurrent.atomic.AtomicBoolean

class ActionAppWidget : GlanceAppWidget() {

    @Composable
    override fun Content() {
        Column(
            modifier = GlanceModifier.padding(R.dimen.external_padding).fillMaxSize()
                .appWidgetBackground().cornerRadius(R.dimen.corner_radius),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = "Tap to change me",
                style = TextStyle(
                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    textDecoration = if (shouldChangeView.get()) {
                        TextDecoration.Underline
                    } else {
                        TextDecoration.None
                    }
                ),
                modifier = GlanceModifier
                    .padding(8.dp)
                    .clickable(
                        actionRunCallback<UpdateAction>(
                            actionParametersOf(
                                KEY_USE_UNDERLINE to shouldChangeView.get()
                            )
                        )
                    )
            )
            if (shouldChangeView.get()) {
                Image(
                    ImageProvider(R.drawable.compose),
                    "Compose",
                    modifier = GlanceModifier.size(40.dp)
                )
            }
            Button(
                text = "Intent",
                onClick = actionLaunchActivity(
                    Intent(LocalContext.current, ActionDemoActivity::class.java)
                )
            )
            Button(
                text = "Target class",
                onClick = actionLaunchActivity<ActionDemoActivity>()
            )
            Button(
                text = "Component name",
                onClick = actionLaunchActivity(
                    ComponentName(LocalContext.current, ActionDemoActivity::class.java)
                )
            )
        }
    }
}

private val KEY_USE_UNDERLINE = ActionParameters.Key<Boolean>("underline")
private var shouldChangeView = AtomicBoolean(false)

/**
 * Action to update the [shouldChangeView] value whenever users clicks on text
 */
class UpdateAction : ActionCallback {
    override suspend fun onRun(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        shouldChangeView.set(!parameters.getOrDefault(KEY_USE_UNDERLINE, false))
        ActionAppWidget().update(context, glanceId)
    }
}

/**
 * Placeholder activity to launch via [actionLaunchActivity]
 */
class ActionDemoActivity : Activity()

class ActionAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ActionAppWidget()
}