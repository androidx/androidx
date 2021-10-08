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
import androidx.glance.Modifier
import androidx.glance.findModifier
import org.junit.Test
import kotlin.test.assertIs

class ActionTest {
    @Test
    fun testLaunch() {
        val modifiers = Modifier.clickable(launchActivityAction(TestActivity::class.java))
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        assertIs<LaunchActivityAction>(modifier.action)
    }

    @Test
    fun testUpdate() {
        val modifiers = Modifier.clickable(updateContentAction<TestRunnable>())
        val modifier = checkNotNull(modifiers.findModifier<ActionModifier>())
        assertIs<UpdateAction>(modifier.action)
    }
}

class TestRunnable : ActionRunnable {
    override suspend fun run(context: Context) { }
}