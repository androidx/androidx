/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.core.ui.action

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class ActionCallbackBroadcastReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        RecordingCallback.reset()
        NonCallbackClass.instantiated = false
    }

    @Test
    fun receiver_isDeclaredWithoutBeingExported() {
        val receiverInfo =
            context.packageManager.getReceiverInfo(
                ComponentName(context, ActionCallbackBroadcastReceiver::class.java),
                /* flags= */ 0,
            )

        // Otherwise other apps could trigger this app's callbacks through it.
        assertThat(receiverInfo.exported).isFalse()
    }

    @Test
    fun onReceive_dispatchesCallbackWithWidgetNameWidgetIdAndParameters() = runTest {
        val params =
            Bundle().apply {
                putString("task_id", "task_42")
                putInt("priority", 1)
            }

        receive(
            callbackIntent(
                GlanceWidgetAction.Callback(RecordingCallback::class.java, params),
                widgetName = "obsidian_tasks",
                widgetId = "vault_work",
            )
        )

        val invocation = RecordingCallback.invocation.await()
        assertThat(invocation.widgetName).isEqualTo("obsidian_tasks")
        assertThat(invocation.widgetId).isEqualTo("vault_work")
        assertThat(invocation.parameters.getString("task_id")).isEqualTo("task_42")
        assertThat(invocation.parameters.getInt("priority")).isEqualTo(1)
    }

    @Test
    fun onReceive_withBlankWidgetId_passesNullWidgetId() = runTest {
        receive(
            callbackIntent(
                GlanceWidgetAction.Callback(RecordingCallback::class.java),
                widgetName = "medication_widget",
                widgetId = "   ",
            )
        )

        val invocation = RecordingCallback.invocation.await()
        assertThat(invocation.widgetName).isEqualTo("medication_widget")
        assertThat(invocation.widgetId).isNull()
        assertThat(invocation.parameters.isEmpty).isTrue()
    }

    @Test
    fun onReceive_ignoresUnexpectedActionOrMissingExtras() = runTest {
        val valid =
            callbackIntent(
                GlanceWidgetAction.Callback(RecordingCallback::class.java),
                widgetName = "widget",
                widgetId = null,
            )

        receive(Intent(valid).setAction("com.example.WRONG_ACTION"))
        receive(
            Intent(valid).apply {
                removeExtra(ActionCallbackBroadcastReceiver.EXTRA_CALLBACK_CLASS)
            }
        )
        receive(
            Intent(valid).apply { removeExtra(ActionCallbackBroadcastReceiver.EXTRA_WIDGET_NAME) }
        )

        assertThat(RecordingCallback.invocation.isCompleted).isFalse()
    }

    @Test
    fun onReceive_doesNotInstantiateClassNotImplementingCallback() = runTest {
        receive(
            Intent(ActionCallbackBroadcastReceiver.ACTION_TRIGGER_CALLBACK)
                .putExtra(
                    ActionCallbackBroadcastReceiver.EXTRA_CALLBACK_CLASS,
                    NonCallbackClass::class.java.name,
                )
                .putExtra(ActionCallbackBroadcastReceiver.EXTRA_WIDGET_NAME, "widget")
        )

        assertThat(NonCallbackClass.instantiated).isFalse()
    }

    @Test
    fun onReceive_whenCallbackThrows_doesNotCrash() = runTest {
        receive(
            callbackIntent(
                GlanceWidgetAction.Callback(ThrowingCallback::class.java),
                widgetName = "failing_widget",
                widgetId = null,
            )
        )
    }

    private fun callbackIntent(
        action: GlanceWidgetAction.Callback,
        widgetName: String,
        widgetId: String?,
    ): Intent = ActionCallbackBroadcastReceiver.createIntent(context, action, widgetName, widgetId)

    private fun TestScope.receive(intent: Intent) {
        ActionCallbackBroadcastReceiver(StandardTestDispatcher(testScheduler))
            .onReceive(context, intent)
        testScheduler.advanceUntilIdle()
    }

    data class Invocation(val widgetName: String, val widgetId: String?, val parameters: Bundle)

    class RecordingCallback : GlanceAdaptiveActionCallback {
        override suspend fun onAction(
            context: Context,
            widgetName: String,
            widgetId: String?,
            parameters: Bundle,
        ) {
            invocation.complete(Invocation(widgetName, widgetId, parameters))
        }

        companion object {
            var invocation = CompletableDeferred<Invocation>()

            fun reset() {
                invocation = CompletableDeferred()
            }
        }
    }

    class ThrowingCallback : GlanceAdaptiveActionCallback {
        override suspend fun onAction(
            context: Context,
            widgetName: String,
            widgetId: String?,
            parameters: Bundle,
        ) {
            throw IllegalStateException("Simulated callback failure")
        }
    }

    class NonCallbackClass {
        init {
            instantiated = true
        }

        companion object {
            var instantiated = false
        }
    }
}
