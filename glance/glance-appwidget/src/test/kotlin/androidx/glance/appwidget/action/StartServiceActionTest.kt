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

package androidx.glance.appwidget.action

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionModifier
import androidx.glance.action.clickable
import androidx.glance.findModifier
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StartServiceActionTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testLaunchClass() {
        val modifiers = GlanceModifier.clickable(actionStartService<TestService>())
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        val action = assertIs<StartServiceClassAction>(modifier.action)
        assertThat(action.serviceClass).isEqualTo(TestService::class.java)
        assertThat(action.isForegroundService).isEqualTo(false)
    }

    @Test
    fun testLaunchClassWithForeground() {
        val modifiers =
            GlanceModifier.clickable(actionStartService<TestService>(isForegroundService = true))
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        val action = assertIs<StartServiceClassAction>(modifier.action)
        assertThat(action.serviceClass).isEqualTo(TestService::class.java)
        assertThat(action.isForegroundService).isEqualTo(true)
    }

    @Test
    fun testLaunchIntent() {
        val intentActionString = "test_action"
        val intent = Intent(context, TestService::class.java).setAction(intentActionString)
        val modifiers = GlanceModifier.clickable(actionStartService(intent))
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        val action = assertIs<StartServiceIntentAction>(modifier.action)
        assertThat(action.intent).isEqualTo(intent)
        assertThat(action.intent.action).isEqualTo(intentActionString)
        assertThat(action.isForegroundService).isEqualTo(false)
    }

    @Test
    fun testLaunchComponent() {
        val componentName =
            ComponentName(
                "androidx.glance.appwidget.action",
                "androidx.glance.appwidget.action.TestService"
            )
        val modifiers = GlanceModifier.clickable(actionStartService(componentName))
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        val action = assertIs<StartServiceComponentAction>(modifier.action)
        assertThat(action.componentName).isEqualTo(componentName)
        assertThat(action.isForegroundService).isEqualTo(false)
    }
}

class TestService : Service() {
    override fun onBind(p0: Intent?): IBinder? = null
}
