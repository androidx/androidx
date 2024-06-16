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

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
class SendBroadcastActionTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testLaunchClass() {
        val modifiers = GlanceModifier.clickable(actionSendBroadcast<TestBroadcastReceiver>())
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        val action = assertIs<SendBroadcastClassAction>(modifier.action)
        assertThat(action.receiverClass).isEqualTo(TestBroadcastReceiver::class.java)
    }

    @Test
    fun testLaunchAction() {
        val intentActionString = "test_action"
        val modifiers = GlanceModifier.clickable(actionSendBroadcast(intentActionString))
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        val action = assertIs<SendBroadcastActionAction>(modifier.action)
        assertThat(action.action).isEqualTo(intentActionString)
        assertThat(action.componentName).isNull()
    }

    @Test
    fun testLaunchActionWithComponentName() {
        val intentActionString = "test_action"
        val componentName =
            ComponentName(
                "androidx.glance.appwidget.action",
                "androidx.glance.appwidget.action.TestBroadcastReceiver"
            )
        val modifiers =
            GlanceModifier.clickable(actionSendBroadcast(intentActionString, componentName))
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        val action = assertIs<SendBroadcastActionAction>(modifier.action)
        assertThat(action.action).isEqualTo(intentActionString)
        assertThat(action.componentName).isEqualTo(componentName)
    }

    @Test
    fun testLaunchIntent() {
        val intentActionString = "test_action"
        val intent =
            Intent(context, TestBroadcastReceiver::class.java).setAction(intentActionString)
        val modifiers = GlanceModifier.clickable(actionSendBroadcast(intent))
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        val action = assertIs<SendBroadcastIntentAction>(modifier.action)
        assertThat(action.intent).isEqualTo(intent)
        assertThat(action.intent.action).isEqualTo(intentActionString)
    }

    @Test
    fun testLaunchComponent() {
        val componentName =
            ComponentName(
                "androidx.glance.appwidget.action",
                "androidx.glance.appwidget.action.TestBroadcastReceiver"
            )
        val modifiers = GlanceModifier.clickable(actionSendBroadcast(componentName))
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        val action = assertIs<SendBroadcastComponentAction>(modifier.action)
        assertThat(action.componentName).isEqualTo(componentName)
    }
}

class TestBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Nothing
    }
}
