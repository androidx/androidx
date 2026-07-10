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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle

private const val TAG = "ActionAppWidget"

class ActionAppWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        Content2()
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent {
        Content2()
    }
}

private val ActionParameterKey = ActionParameters.Key<Int>("ActionParameterKey")

// @androidx.compose.ui.tooling.preview.Preview
@Composable
private fun Content2() {
    Column {
        Box(
            modifier =
                GlanceModifier.background(Color.Red).size(64.dp, 48.dp).clickable {
                    Log.i(TAG, "Red Box was clicked")
                }
        ) {}
        //         Start activity action
        Button(
            text = "Activity",
            onClick =
                actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))),
        )

        // start service action
        Button(
            text = "Service",
            onClick =
                actionStartService(
                    isForegroundService = true,
                    intent = Intent(LocalContext.current, ActionDemoService::class.java),
                ),
        )

        // actionRunCallback
        Button(
            text = "Action",
            onClick =
                actionRunCallback<ActionAppWidgetCallback>(
                    parameters = actionParametersOf(ActionParameterKey to 123456)
                ),
        )

        // lambda button
        Button(text = "Lambda", onClick = { Log.i(TAG, "onClick lambda") })

        // broadcast button
        Button(
            text = "broadcast",
            onClick =
                actionSendBroadcast(
                    Intent(LocalContext.current, ActionAppWidgetReceiver::class.java)
                ),
        )

        Box(
            modifier =
                GlanceModifier.background(Color.Green).size(64.dp, 48.dp).clickable {
                    Log.i(TAG, "Green Box was clicked")
                }
        ) {}

        Text(
            "text w/lambda",
            style = TextStyle(fontSize = 22.sp),
            modifier = GlanceModifier.clickable { Log.i(TAG, "text w/lambda clicked") },
        )

        Box(
            modifier =
                GlanceModifier.background(Color.Cyan).size(64.dp, 48.dp).clickable {
                    Log.i(TAG, "Cyan Box was clicked")
                }
        ) {}
    } // end-column
}

class ActionAppWidgetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        Log.d(
            TAG,
            "onAction() called with: context = $context, glanceId = $glanceId, parameters = $parameters",
        )

        val handler: Handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                    context,
                    "ActionAppWidgetCallback: onAction() executed",
                    Toast.LENGTH_SHORT,
                )
                .show()
        }
    }
}

private val StartMessageKey = ActionParameters.Key<String>("launchMessageKey")

@Composable
private fun SelectableActionItem(label: String, active: Boolean, onClick: () -> Unit) {
    val style =
        if (active) {
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
                color =
                    ColorProvider(Color.Black.copy(alpha = 0.3f), Color.White.copy(alpha = 0.3f)),
            )
        }
    Text(text = label, style = style, modifier = GlanceModifier.padding(8.dp).clickable(onClick))
}

/** Placeholder activity to launch via [actionStartActivity] */
class ActionDemoActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        setContent {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                val message = intent.getStringExtra(StartMessageKey.name) ?: "Not found"
                androidx.compose.material.Text(message)
            }
        }
        Log.d(this::class.simpleName, "Action Demo Activity: ${intent.extras}")
    }
}

/** Placeholder service to launch via [actionStartService] */
class ActionDemoService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(this::class.simpleName, "Action Demo Service: $intent")
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show()
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
