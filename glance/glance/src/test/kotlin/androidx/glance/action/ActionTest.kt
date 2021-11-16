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

package androidx.glance.action

import android.content.Context
import com.google.common.truth.Truth.assertThat
import android.content.ComponentName
import androidx.glance.GlanceModifier
import androidx.glance.findModifier
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ActionTest {

    private lateinit var fakeCoroutineScope: TestCoroutineScope
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun testLaunchActivity() {
        val modifiers = GlanceModifier.clickable(actionLaunchActivity(TestActivity::class.java))
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        assertIs<LaunchActivityClassAction>(modifier.action)
    }

    @Test
    fun testUpdate() {
        val modifiers = GlanceModifier.clickable(actionUpdateContent<TestRunnable>())
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        assertIs<UpdateContentAction>(modifier.action)
    }

    @Test
    fun testLaunchFromComponent() = fakeCoroutineScope.runBlockingTest {
        val c = ComponentName("androidx.glance.action", "androidx.glance.action.TestActivity")

        val modifiers = GlanceModifier.clickable(actionLaunchActivity(c))
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        val action = assertIs<LaunchActivityComponentAction>(modifier.action)
        val component = assertNotNull(action.componentName)

        assertThat(component).isEqualTo(c)
    }

    @Test
    fun testLaunchFromComponentWithContext() = fakeCoroutineScope.runBlockingTest {
        val c = ComponentName(context, "androidx.glance.action.TestActivity")

        val modifiers = GlanceModifier.clickable(actionLaunchActivity(c))
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        val action = assertIs<LaunchActivityComponentAction>(modifier.action)
        val component = assertNotNull(action.componentName)

        assertThat(component).isEqualTo(c)
    }
}

class TestRunnable : ActionRunnable {
    override suspend fun run(context: Context, parameters: ActionParameters) { }
}
