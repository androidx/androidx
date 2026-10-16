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

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class GlanceWidgetActionPendingIntentsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun callback_createsImmutableBroadcastToReceiver() {
        val pendingIntent =
            GlanceWidgetAction.Callback(NoOpCallback::class.java)
                .toPendingIntent(context, widgetName = "widget", widgetId = "instance_1")

        val shadow = shadowOf(pendingIntent)
        assertThat(shadow.isBroadcast).isTrue()
        assertThat(shadow.isImmutable).isTrue()
        assertThat(shadow.savedIntent.component)
            .isEqualTo(ComponentName(context, ActionCallbackBroadcastReceiver::class.java))
    }

    @Test
    fun callback_keepsInstancesAndParametersApart() {
        val first = callback(item = "first")

        val sameInstanceOtherParams =
            callback(item = "second").toPendingIntent(context, "tasks", widgetId = "alice")
        val otherInstanceSameParams = first.toPendingIntent(context, "tasks", widgetId = "bob")
        val alice = first.toPendingIntent(context, "tasks", widgetId = "alice")

        assertThat(alice).isNotEqualTo(sameInstanceOtherParams)
        assertThat(alice).isNotEqualTo(otherInstanceSameParams)
        // Creating the others did not overwrite Alice's extras.
        val savedIntent = shadowOf(alice).savedIntent
        assertThat(savedIntent.getStringExtra(ActionCallbackBroadcastReceiver.EXTRA_WIDGET_ID))
            .isEqualTo("alice")
        assertThat(
                savedIntent
                    .getBundleExtra(ActionCallbackBroadcastReceiver.EXTRA_PARAMETERS)!!
                    .getString("item")
            )
            .isEqualTo("first")
    }

    @Test
    fun launchActivity_createsImmutableActivityIntentWithExtras() {
        val params = Bundle().apply { putString("screen", "details") }

        val pendingIntent =
            GlanceWidgetAction.LaunchActivity(TargetActivity::class.java, params)
                .toPendingIntent(context, widgetName = "widget", widgetId = "instance_1")

        val shadow = shadowOf(pendingIntent)
        assertThat(shadow.isActivity).isTrue()
        assertThat(shadow.isImmutable).isTrue()
        val savedIntent = shadow.savedIntent
        assertThat(savedIntent.component)
            .isEqualTo(ComponentName(context, TargetActivity::class.java))
        assertThat(savedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0)
        assertThat(savedIntent.getStringExtra("screen")).isEqualTo("details")
        // The intent data belongs to the developer's activity and must not be used for identity.
        assertThat(savedIntent.data).isNull()
    }

    @Test
    fun launchActivity_keepsDifferentParametersApart() {
        val details =
            GlanceWidgetAction.LaunchActivity(
                    TargetActivity::class.java,
                    Bundle().apply { putString("screen", "details") },
                )
                .toPendingIntent(context, widgetName = "widget", widgetId = null)
        val home =
            GlanceWidgetAction.LaunchActivity(
                    TargetActivity::class.java,
                    Bundle().apply { putString("screen", "home") },
                )
                .toPendingIntent(context, widgetName = "widget", widgetId = null)

        assertThat(details).isNotEqualTo(home)
        assertThat(shadowOf(details).savedIntent.getStringExtra("screen")).isEqualTo("details")
    }

    @Test
    fun launchDeeplink_createsImmutableViewActivityIntent() {
        val pendingIntent =
            GlanceWidgetAction.LaunchDeeplink("companion://doses/42/skip")
                .toPendingIntent(context, widgetName = "widget", widgetId = "instance_1")

        val shadow = shadowOf(pendingIntent)
        assertThat(shadow.isActivity).isTrue()
        assertThat(shadow.isImmutable).isTrue()
        val savedIntent = shadow.savedIntent
        assertThat(savedIntent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(savedIntent.data).isEqualTo(Uri.parse("companion://doses/42/skip"))
        assertThat(savedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0)
    }

    private fun callback(item: String) =
        GlanceWidgetAction.Callback(
            NoOpCallback::class.java,
            Bundle().apply { putString("item", item) },
        )

    class NoOpCallback : GlanceAdaptiveActionCallback {
        override suspend fun onAction(
            context: Context,
            widgetName: String,
            widgetId: String?,
            parameters: Bundle,
        ) = Unit
    }

    class TargetActivity : Activity()
}
