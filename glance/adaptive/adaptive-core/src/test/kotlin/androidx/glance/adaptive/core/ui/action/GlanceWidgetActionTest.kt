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
import android.content.Context
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class GlanceWidgetActionTest {

    private class FirstCallback : GlanceAdaptiveActionCallback {
        override suspend fun onAction(
            context: Context,
            widgetName: String,
            widgetId: String?,
            parameters: Bundle,
        ) = Unit
    }

    private class SecondCallback : GlanceAdaptiveActionCallback {
        override suspend fun onAction(
            context: Context,
            widgetName: String,
            widgetId: String?,
            parameters: Bundle,
        ) = Unit
    }

    private class FirstActivity : Activity()

    private class SecondActivity : Activity()

    @Test
    fun callback_defaultParameters_isEmptyBundle() {
        val action = GlanceWidgetAction.Callback(FirstCallback::class.java)

        assertThat(action.actionCallbackClass).isEqualTo(FirstCallback::class.java)
        assertThat(action.parameters.isEmpty).isTrue()
    }

    @Test
    fun callback_equalsAndHashCode_usesDeepBundleEquality() {
        val params1 =
            Bundle().apply {
                putString("task_id", "42")
                putIntArray("counts", intArrayOf(1, 2, 3))
            }
        val params2 =
            Bundle().apply {
                putIntArray("counts", intArrayOf(1, 2, 3))
                putString("task_id", "42")
            }
        val differentParams =
            Bundle().apply {
                putString("task_id", "99")
                putIntArray("counts", intArrayOf(1, 2, 3))
            }

        val action1 = GlanceWidgetAction.Callback(FirstCallback::class.java, params1)
        val action2 = GlanceWidgetAction.Callback(FirstCallback::class.java, params2)
        val differentCallback = GlanceWidgetAction.Callback(SecondCallback::class.java, params1)
        val differentParamsAction =
            GlanceWidgetAction.Callback(FirstCallback::class.java, differentParams)

        assertThat(action1).isEqualTo(action2)
        assertThat(action1.hashCode()).isEqualTo(action2.hashCode())
        assertThat(action1).isNotEqualTo(differentCallback)
        assertThat(action1).isNotEqualTo(differentParamsAction)
        assertThat(action1.toString()).contains(FirstCallback::class.java.name)

        params1.putString("task_id", "mutated")
        assertThat(action1.parameters.getString("task_id")).isEqualTo("42")
    }

    @Test
    fun launchActivity_defaultParameters_isEmptyBundle() {
        val action = GlanceWidgetAction.LaunchActivity(FirstActivity::class.java)

        assertThat(action.targetActivityClass).isEqualTo(FirstActivity::class.java)
        assertThat(action.parameters.isEmpty).isTrue()
    }

    @Test
    fun launchActivity_equalsAndHashCode_usesDeepBundleEquality() {
        val params1 = Bundle().apply { putString("tab", "details") }
        val params2 = Bundle().apply { putString("tab", "details") }
        val differentParams = Bundle().apply { putString("tab", "home") }

        val action1 = GlanceWidgetAction.LaunchActivity(FirstActivity::class.java, params1)
        val action2 = GlanceWidgetAction.LaunchActivity(FirstActivity::class.java, params2)
        val differentActivity =
            GlanceWidgetAction.LaunchActivity(SecondActivity::class.java, params1)
        val differentParamsAction =
            GlanceWidgetAction.LaunchActivity(FirstActivity::class.java, differentParams)

        assertThat(action1).isEqualTo(action2)
        assertThat(action1.hashCode()).isEqualTo(action2.hashCode())
        assertThat(action1).isNotEqualTo(differentActivity)
        assertThat(action1).isNotEqualTo(differentParamsAction)
        assertThat(action1.toString()).contains(FirstActivity::class.java.name)

        params1.putString("tab", "mutated")
        assertThat(action1.parameters.getString("tab")).isEqualTo("details")
    }

    @Test
    fun launchDeeplink_equalsAndHashCode() {
        val action1 = GlanceWidgetAction.LaunchDeeplink("companion://doses/123/skip")
        val action2 = GlanceWidgetAction.LaunchDeeplink("companion://doses/123/skip")
        val differentUri = GlanceWidgetAction.LaunchDeeplink("obsidian://new")

        assertThat(action1.uri).isEqualTo("companion://doses/123/skip")
        assertThat(action1).isEqualTo(action2)
        assertThat(action1.hashCode()).isEqualTo(action2.hashCode())
        assertThat(action1).isNotEqualTo(differentUri)
        assertThat(action1.toString()).contains("companion://doses/123/skip")
    }
}
