/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.glance.appwidget.multiprocess.testapp

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceRemoteViewsService
import androidx.glance.appwidget.MyPackageReplacedReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.ActionCallbackBroadcastReceiver
import androidx.glance.appwidget.action.ActionTrampolineActivity
import androidx.glance.appwidget.action.InvisibleActionTrampolineActivity
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.multiprocess.MultiProcessConfig
import androidx.glance.appwidget.multiprocess.MultiProcessGlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.multiprocess.RemoteWorkerService

/** This widget receiver runs in the default process for this app. */
class MainProcessWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MainProcessWidget()
}

private val currentProcess: String
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            Class.forName("android.app.ActivityThread")
                .getDeclaredMethod("currentProcessName")
                .apply { isAccessible = true }
                .invoke(null) as String
        }

/**
 * This widget uses the default components which run in the default process unless otherwise
 * specified in AndroidManifest.xml
 */
open class MainProcessWidget : MultiProcessGlanceAppWidget() {
    /**
     * If [getMultiProcessConfig] is not overridden, or returns null, the default components are
     * used and the widget runs in the main process.
     */
    override fun getMultiProcessConfig(context: Context): MultiProcessConfig? = null

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            DisposableEffect(true) {
                android.util.Log.e("WIDGET", "started running in $currentProcess")
                onDispose { android.util.Log.e("WIDGET", "finished running in $currentProcess") }
            }
            Column(
                modifier =
                    GlanceModifier.background(GlanceTheme.colors.widgetBackground).fillMaxSize()
            ) {
                Text(
                    "This widget is running in process: $currentProcess",
                    style = TextStyle(GlanceTheme.colors.onBackground),
                    modifier = GlanceModifier.padding(8.dp),
                )
                Button(
                    "Click to log from $currentProcess (lambda)",
                    onClick = {
                        android.util.Log.e("WIDGET", "Running ${this::class} in $currentProcess")
                    },
                )
                Button(
                    "Click to log from $currentProcess (broadcast callback)",
                    onClick = actionRunCallback<LogProcessAction>(),
                )
            }
        }
    }
}

class LogProcessAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        android.util.Log.e("WIDGET", "Running ${this::class} in $currentProcess")
    }
}

/** This widget receiver runs in the ":custom" process, as defined in AndroidManifest.xml */
class CustomProcessWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CustomProcessWidget()
}

/**
 * This widget uses the same UI as [MainProcessWidget], but defines different components for the
 * session to use. Each of these components must run in the same process as the
 * [CustomProcessWidgetReceiver].
 */
class CustomProcessWidget : MainProcessWidget() {
    override fun getMultiProcessConfig(context: Context) =
        MultiProcessConfig(
            remoteWorkerService = ComponentName(context, CustomWorkerService::class.java),
            actionTrampolineActivity =
                ComponentName(context, CustomActionTrampolineActivity::class.java),
            invisibleActionTrampolineActivity =
                ComponentName(context, CustomInvisibleTrampolineActivity::class.java),
            actionCallbackBroadcastReceiver =
                ComponentName(context, CustomActionCallbackBroadcastReceiver::class.java),
            remoteViewsService = ComponentName(context, CustomGlanceRemoteViewsService::class.java),
        )
}

// These all need to be defined in AndroidManifest.xml with a custom android:process value.
class CustomWorkerService : RemoteWorkerService()

class CustomInvisibleTrampolineActivity : InvisibleActionTrampolineActivity()

class CustomActionTrampolineActivity : ActionTrampolineActivity()

class CustomActionCallbackBroadcastReceiver : ActionCallbackBroadcastReceiver()

class CustomGlanceRemoteViewsService : GlanceRemoteViewsService()

class CustomMyPackageReplacedReceiver : MyPackageReplacedReceiver()
