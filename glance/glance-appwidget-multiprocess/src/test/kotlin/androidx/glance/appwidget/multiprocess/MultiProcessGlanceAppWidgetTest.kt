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

package androidx.glance.appwidget.multiprocess

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.glance.GlanceId
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.AppWidgetSession
import androidx.glance.appwidget.GlanceRemoteViewsService
import androidx.glance.appwidget.MyPackageReplacedReceiver
import androidx.glance.appwidget.action.ActionCallbackBroadcastReceiver
import androidx.glance.appwidget.action.ActionTrampolineActivity
import androidx.glance.appwidget.action.InvisibleActionTrampolineActivity
import androidx.test.core.app.ApplicationProvider
import androidx.work.multiprocess.RemoteWorkerService
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MultiProcessGlanceAppWidgetTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testWidget =
        object : MultiProcessGlanceAppWidget() {
            override fun getMultiProcessConfig(context: Context): MultiProcessConfig? {
                return configToUse
            }

            override suspend fun provideGlance(context: Context, id: GlanceId) {}

            fun callCreateAppWidgetSession(
                context: Context,
                id: AppWidgetId,
                options: Bundle?,
            ): AppWidgetSession = createAppWidgetSession(context, id, options)
        }

    private val testConfig =
        MultiProcessConfig(
            remoteWorkerService = ComponentName(context, TestWorkerService::class.java),
            remoteViewsService = ComponentName(context, TestRemoteViewsService::class.java),
            actionTrampolineActivity =
                ComponentName(context, TestActionTrampolineActivity::class.java),
            invisibleActionTrampolineActivity =
                ComponentName(context, TestInvisibleTrampolineActivity::class.java),
            actionCallbackBroadcastReceiver =
                ComponentName(context, TestActionCallbackBroadcastReceiver::class.java),
        )

    var configToUse: MultiProcessConfig? = testConfig

    @Test
    fun getComponents() {
        assertNotNull(testWidget.getComponents(context)).run {
            assertThat(actionTrampolineActivity).isEqualTo(testConfig.actionTrampolineActivity)
            assertThat(invisibleActionTrampolineActivity)
                .isEqualTo(testConfig.invisibleActionTrampolineActivity)
            assertThat(actionCallbackBroadcastReceiver)
                .isEqualTo(testConfig.actionCallbackBroadcastReceiver)
            assertThat(remoteViewsService).isEqualTo(testConfig.remoteViewsService)
        }
    }

    @Test
    fun getSessionManager() {
        assertThat(testWidget.getSessionManager(context)).isEqualTo(RemoteSessionManager)
    }

    @Test
    fun createAppWidgetSession() {
        val session = testWidget.callCreateAppWidgetSession(context, AppWidgetId(0), null)
        assertIs<RemoteAppWidgetSession>(session)
        assertThat(session.remoteWorkService).isEqualTo(testConfig.remoteWorkerService)
    }

    @Test
    fun createAppWidgetSession_null() {
        configToUse = null

        val session = testWidget.callCreateAppWidgetSession(context, AppWidgetId(0), null)
        assertIs<AppWidgetSession>(session)
    }
}

class TestWorkerService : RemoteWorkerService()

class TestInvisibleTrampolineActivity : InvisibleActionTrampolineActivity()

class TestActionTrampolineActivity : ActionTrampolineActivity()

class TestActionCallbackBroadcastReceiver : ActionCallbackBroadcastReceiver()

class TestRemoteViewsService : GlanceRemoteViewsService()

class TestMyPackageReplacedReceiver : MyPackageReplacedReceiver()
