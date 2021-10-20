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

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionModifier
import androidx.glance.action.clickable
import androidx.glance.findModifier
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import kotlin.test.assertIs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppWidgetLaunchActionTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testLaunchIntent() {
        val intentActionString = "test_action"
        val intent = Intent(context, TestActivity::class.java).setAction(intentActionString)
        val modifiers = GlanceModifier.clickable(actionLaunchActivity(intent))
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        val action = assertIs<LaunchActivityIntentAction>(modifier.action)
        assertThat(action.intent).isEqualTo(intent)
        assertThat(action.intent.action).isEqualTo(intentActionString)
    }
}

class TestActivity : Activity()
