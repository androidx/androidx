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

package androidx.glance.wear.tiles.action

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionModifier
import androidx.glance.action.clickable
import androidx.glance.findModifier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RunCallbackActionTest {

    private lateinit var fakeCoroutineScope: TestScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope()
    }

    @Test
    fun testUpdate() {
        val modifiers = GlanceModifier.clickable(actionRunCallback<TestCallback>())
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        assertIs<RunCallbackAction>(modifier.action)
    }
}

class TestCallback : ActionCallback {
    override suspend fun onRun(
        context: Context,
        glanceId: GlanceId
    ) {
        // Nothing
    }
}
