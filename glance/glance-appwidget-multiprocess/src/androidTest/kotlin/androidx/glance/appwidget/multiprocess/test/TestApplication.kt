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

@file:RequiresApi(Build.VERSION_CODES.Q)

package androidx.glance.appwidget.multiprocess.test

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceRemoteViewsService
import androidx.glance.appwidget.MyPackageReplacedReceiver
import androidx.glance.appwidget.action.ActionCallbackBroadcastReceiver
import androidx.glance.appwidget.action.ActionTrampolineActivity
import androidx.glance.appwidget.action.InvisibleActionTrampolineActivity
import androidx.glance.appwidget.multiprocess.MultiProcessConfig
import androidx.glance.appwidget.multiprocess.MultiProcessGlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.work.Configuration
import androidx.work.multiprocess.RemoteWorkerService
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.flow.MutableStateFlow

class TestApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration = Configuration.Builder().build()

    // used to test actionRunCallback
    val actionFlow = MutableStateFlow(0)
}

class TestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        checkCustomProcess()
        super.onCreate(savedInstanceState)
    }
}

class TestWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TestWidget()
}

class TestWidget : MultiProcessGlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val layout by currentLayout.collectAsState()
            layout.invoke()
        }
    }

    override fun getMultiProcessConfig(context: Context) =
        MultiProcessConfig(
            remoteWorkerService = ComponentName(context, CustomWorkerService::class.java),
            remoteViewsService = ComponentName(context, CustomRemoteViewsService::class.java),
            actionTrampolineActivity =
                ComponentName(context, CustomActionTrampolineActivity::class.java),
            invisibleActionTrampolineActivity =
                ComponentName(context, CustomInvisibleTrampolineActivity::class.java),
            actionCallbackBroadcastReceiver =
                ComponentName(context, CustomActionCallbackBroadcastReceiver::class.java),
        )

    companion object {
        private val currentLayout = MutableStateFlow<@Composable (() -> Unit)> {}

        internal fun setContent(content: @Composable () -> Unit) {
            currentLayout.value = content
        }
    }
}

// These all need to be defined in AndroidManifest.xml with a custom android:process value.
class CustomWorkerService : RemoteWorkerService() {
    override fun onCreate() {
        checkCustomProcess()
        super.onCreate()
    }
}

class CustomInvisibleTrampolineActivity : InvisibleActionTrampolineActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        checkCustomProcess()
        super.onCreate(savedInstanceState)
    }
}

class CustomActionTrampolineActivity : ActionTrampolineActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        checkCustomProcess()
        super.onCreate(savedInstanceState)
    }
}

class CustomActionCallbackBroadcastReceiver : ActionCallbackBroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        checkCustomProcess()
        super.onReceive(context, intent)
    }
}

class CustomRemoteViewsService : GlanceRemoteViewsService() {
    override fun onCreate() {
        checkCustomProcess()
        super.onCreate()
    }
}

class CustomMyPackageReplacedReceiver : MyPackageReplacedReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        checkCustomProcess()
        super.onReceive(context, intent)
    }
}

private const val packageName = "androidx.glance.appwidget.multiprocess.test"

private const val customProcess = "$packageName:custom"

internal fun checkCustomProcess() {
    val currentProcess = Application.getProcessName()
    assertWithMessage("Expected to run in ${customProcess}, is in $currentProcess")
        .that(currentProcess)
        .isEqualTo(customProcess)
}
