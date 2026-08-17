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

package androidx.compose.ui.platform

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TaskDispatchersCompositionLocalTest {
    @get:Rule val rule = createComposeRule(ComposeUiTestConfig(StandardTestDispatcher()))

    @Test
    fun localTaskDispatchers_returnsAndroidTaskDispatchersByDefault() {
        lateinit var taskDispatchers: TaskDispatchers
        rule.setContent {
            BasicText("Test")
            taskDispatchers = LocalTaskDispatchers.current
        }
        rule.waitForIdle()

        assertThat(taskDispatchers.Default).isEqualTo(Dispatchers.Default)
        assertThat(taskDispatchers.IO).isEqualTo(Dispatchers.IO)
    }

    @Test
    fun localTaskDispatchers_canBeOverridden() {
        val customContexts =
            object : TaskDispatchers {
                override val Default: CoroutineDispatcher
                    get() = Dispatchers.Unconfined

                @get:Suppress("AcronymName")
                override val IO: CoroutineDispatcher
                    get() = Dispatchers.Unconfined
            }

        lateinit var taskDispatchers: TaskDispatchers
        rule.setContent {
            CompositionLocalProvider(LocalTaskDispatchers provides customContexts) {
                BasicText("Test")
                taskDispatchers = LocalTaskDispatchers.current
            }
        }
        rule.waitForIdle()

        assertThat(taskDispatchers).isSameInstanceAs(customContexts)
    }
}
